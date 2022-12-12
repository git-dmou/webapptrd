package fr.solunea.thaleia.webapp.api.transform;

import fr.solunea.thaleia.model.User;
import fr.solunea.thaleia.service.LicenceService;
import fr.solunea.thaleia.utils.DetailedException;
import fr.solunea.thaleia.webapp.ThaleiaApplication;
import fr.solunea.thaleia.webapp.security.ThaleiaSession;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.log4j.Logger;
import org.apache.wicket.feedback.FeedbackMessage;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Locale;

public abstract class AbstractCannelleTreatment<T> implements ITransformTreatment<T> {

    private final static Logger logger = Logger.getLogger(AbstractCannelleTreatment.class);

    protected StringBuilder getFeedbackMessages(InvocationTargetException e) {
        logger.debug(e);

        StringBuilder messages = new StringBuilder();

        // On renvoie le message de l'exception qui a levée l'InvocationTargetException
        messages.append(e.getCause().getMessage()).append("\n");

        // S'ils existent, on ajoute les messages d'erreur destinés à la présentation dans l'IHM
        for (FeedbackMessage feedbackMessage : ThaleiaSession.get().getFeedbackMessages()) {
            messages.append(feedbackMessage.getMessage()).append("\n");
        }

        // On supprime les éventuels messages d'erreur
        ThaleiaSession.get().getFeedbackMessages().clear();
        return messages;
    }

    protected void prepareFeedbackMessages(Locale locale) throws DetailedException {
        try {
            ThaleiaSession.get().setLocale(locale);
            // Et on supprime d'éventuels anciens messages d'erreur
            ThaleiaSession.get().getFeedbackMessages().clear();
        } catch (Exception e) {
            String message = "Impossible de fixer la locale : " + e;
            logger.warn(message);
            throw new DetailedException(message);
        }
    }

    /**
     * Sont considérées valides : les licences author, publisher et demo.
     * @param user
     * @throws DetailedException
     */
    protected void checkLicence(User user) throws DetailedException {
        if (user == null) {
            throw new DetailedException("User non identifié !");
        }
        if (ThaleiaApplication.get().getLicenceService().isLicenceHolded(user,
                ThaleiaApplication.get().getLicenceDao().findByName(LicenceService.LICENCE_NAME_AUTHOR))
                || ThaleiaApplication.get().getLicenceService().isLicenceHolded(user,
                ThaleiaApplication.get().getLicenceDao().findByName(LicenceService.LICENCE_NAME_PUBLISHER))
                || ThaleiaApplication.get().getLicenceService().isLicenceHolded(user,
                ThaleiaApplication.get().getLicenceDao().findByName(LicenceService.LICENCE_NAME_DEMO_CANNELLE))
                || user.getIsAdmin()) {
            logger.debug("Licence ok.");
        } else {
            throw new DetailedException("Pas de licence valide pour ce traitement !");
        }
    }

    protected void checkCannellePlugin() throws DetailedException {
        try {
            ThaleiaApplication.get().getPluginService().getImplementation(
                    "fr.solunea.thaleia.plugins.cannelle.v6.CannelleV6Plugin");
        } catch (DetailedException e) {
            throw e.addMessage("Le plugin Cannelle ne semble pas installé !");
        }
    }

    @SuppressWarnings("unchecked")
    protected T invokeCannelleMethod(Object input, User user, Locale locale, String methodName) throws DetailedException {
        // Vérification de la présence du plugin Cannelle
        checkCannellePlugin();

        // Vérification de la licence de l'utilisateur : admin, auteur ou publisher
        checkLicence(user);

        // On fixe la locale de la session Wicket, afin d'obtenir les messages d'erreurs (destinés à la présentation
        // dans les IHM web) dans la locale demandée pour le traitement
        prepareFeedbackMessages(locale);

        T result;
        try {
            Class<?> aClass = ThaleiaApplication.get().getPluginService().getClassLoader().loadClass(
                    "fr.solunea.thaleia.plugins.cannelle.v6.utils.CannelleTreatment");
            Constructor<?> constructor = aClass.getConstructor();
            Object instance = constructor.newInstance();
            Method method = instance.getClass().getMethod(methodName, input.getClass(), User.class, Locale.class);
            result = (T) method.invoke(instance, input, user, locale);
        } catch (ClassNotFoundException e) {
            throw new DetailedException(e).addMessage(
                    "Impossible de retrouver la classe d'implémentation du traitement !");
        } catch (NoSuchMethodException e) {
            throw new DetailedException(e).addMessage(
                    "Impossible de retrouver la méthode d'implémentation du traitement !");
        } catch (IllegalAccessException | InstantiationException e) {
            throw new DetailedException(e).addMessage(
                    "Impossible d'instancier la classe d'implémentation du traitement !");
        } catch (InvocationTargetException e) {
            logger.warn(e);
            logger.warn(ExceptionUtils.getStackTrace(e));
            StringBuilder messages = getFeedbackMessages(e);
            throw new DetailedException(messages.toString());
        } catch (Exception e) {
            logger.warn(e);
            logger.warn(ExceptionUtils.getStackTrace(e));
            throw new DetailedException(e);
        }
        return result;
    }
}
