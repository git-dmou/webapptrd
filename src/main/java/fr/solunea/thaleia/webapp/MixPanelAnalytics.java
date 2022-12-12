package fr.solunea.thaleia.webapp;

import com.mixpanel.mixpanelapi.ClientDelivery;
import com.mixpanel.mixpanelapi.MessageBuilder;
import com.mixpanel.mixpanelapi.MixpanelAPI;
import fr.solunea.thaleia.model.User;
import fr.solunea.thaleia.model.dao.ApplicationParameterDao;
import fr.solunea.thaleia.utils.ApplicationEvent;
import fr.solunea.thaleia.utils.DateUtils;
import fr.solunea.thaleia.utils.DetailedException;
import fr.solunea.thaleia.utils.IEvent;
import fr.solunea.thaleia.webapp.security.ThaleiaSession;
import org.apache.log4j.Logger;
import org.json.JSONObject;

import java.io.IOException;
import java.io.Serializable;
import java.util.Objects;

public class MixPanelAnalytics extends Analytics {

    private final static Logger logger = Logger.getLogger(MixPanelAnalytics.class);

    /**
     * Le nom du paramètre de l'application qui contient le token pour l'identification à l'API de MixPanel.
     */
    private static final String MIX_PANEL_TOKEN_PARAM_NAME = "mixpanel.api.project.token";

    /**
     * Recherche dans les paramètres de l'instance la présence d'un paramètre qui contient le token d'identication à
     * l'API MixPanel. Renvoie une chaîne vide si aucune valeur n'est trouvée.
     */
    private String getMixPanelToken() {
        ApplicationParameterDao applicationParameterDao = ThaleiaApplication.get().getApplicationParameterDao();
        return applicationParameterDao.getValue(MIX_PANEL_TOKEN_PARAM_NAME, "");
    }

    @Override
    public void logAuthentication(User user) {
        // On prépare la date de dernière création du mot de passe.
        JSONObject props = new JSONObject();
        String passwordCreationDate = null;
        try {
            if (user.getPasswordCreationDate() != null) {
                passwordCreationDate = DateUtils.formatDateHour(user.getPasswordCreationDate(), java.util.Locale
                        .FRANCE);
            }
        } catch (Exception e) {
            logger.debug(e);
        }
        props.put("PasswordCreated", Objects.requireNonNullElse(passwordCreationDate, ""));

        // On prépare le nom des licences de l'utilisateur
        try {
            props.put("Licence", ThaleiaApplication.get().getLicenceService().getValidLicencesNames(user, java.util.Locale
                    .FRANCE, " - "));
        } catch (DetailedException e) {
            logger.warn(e);
            props.put("Licence", "");
        }


        // L'événement d'identifiation
        // On n'identifie pas l'utilisateur par son login (user.getLogin()), mais par son id anonyme, afin de lier
        // cette action avec le reste de sa navigation.
        logEvent(getUserId(), ApplicationEvent.LoginOk, props);
    }

    @Override
    public void logEvent(Serializable jsessionid, IEvent event) {
        // On journalise l'évènement sans propriétés
        logEvent(jsessionid, event, new JSONObject());
    }


    private void logEvent(Serializable userId, IEvent event, JSONObject properties) {
        if (logUserEvents()) {
            String mixPanelToken = getMixPanelToken();
            if (!mixPanelToken.isEmpty()) {
                logger.debug("Appel pour journaliser l'évènement '" + event + "' pour le user '" + userId + "' chez "
                        + "MixPanel. " + "Properties " + "= " + properties);

                MessageBuilder messageBuilder = new MessageBuilder(mixPanelToken);

                // L'événement
                JSONObject jsonObject = messageBuilder.event(userId.toString(), event.name(), properties);

                ClientDelivery delivery = new ClientDelivery();
                delivery.addMessage(jsonObject);
                MixpanelAPI mixpanel = new MixpanelAPI();
                try {
                    mixpanel.deliver(delivery);
                } catch (IOException e) {
                    logger.warn("Impossible d'appeler l'API MixPanel : " + e);
                }
            }
        }
    }

    /**
     * @return true si les évènements de ce user doivent être tracés.
     */
    private boolean logUserEvents() {
        // On recherche une éventuelle session Wicket
        User loggedUser;
        try {
            loggedUser = ThaleiaSession.get().getAuthenticatedUser();
        } catch (Exception e) {
            // Hors session Wicket : donc on trace
            return true;
        }

        if (loggedUser == null) {
            // On ne sait pas qui c'est : on trace
            return true;
        }

        // On ne trace pas les admins
        if (loggedUser.getIsAdmin()) {
            return false;
        }

        // On ne trace pas les emails *@solunea.fr
        return !loggedUser.getLogin().toLowerCase().endsWith("@solunea.fr");
    }
}
