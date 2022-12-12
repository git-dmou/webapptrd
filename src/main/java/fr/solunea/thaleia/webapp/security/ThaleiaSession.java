package fr.solunea.thaleia.webapp.security;

import fr.solunea.thaleia.model.ContentType;
import fr.solunea.thaleia.model.Locale;
import fr.solunea.thaleia.model.User;
import fr.solunea.thaleia.model.dao.*;
import fr.solunea.thaleia.service.*;
import fr.solunea.thaleia.service.events.EventNotificationService;
import fr.solunea.thaleia.service.utils.Unique;
import fr.solunea.thaleia.utils.ApplicationEvent;
import fr.solunea.thaleia.utils.DetailedException;
import fr.solunea.thaleia.utils.LogUtils;
import fr.solunea.thaleia.webapp.Analytics;
import fr.solunea.thaleia.webapp.ThaleiaApplication;
import fr.solunea.thaleia.webapp.context.SessionContextService;
import fr.solunea.thaleia.webapp.utils.MessagesUtils;
import org.apache.cayenne.ObjectContext;
import org.apache.log4j.Logger;
import org.apache.wicket.Session;
import org.apache.wicket.authroles.authentication.AuthenticatedWebSession;
import org.apache.wicket.authroles.authorization.strategies.role.Roles;
import org.apache.wicket.request.Request;
import org.apache.wicket.util.cookies.CookieDefaults;
import org.apache.wicket.util.cookies.CookieUtils;

import java.util.Calendar;
import java.util.concurrent.TimeUnit;

@SuppressWarnings("serial")
public class ThaleiaSession extends AuthenticatedWebSession {

    /**
     * Le nom de l'attribut de session qui contient le nom de la classe du dernier plugin lancé par l'utilisateur.
     */
    public static final String LAST_LAUNCHED_PLUGIN_CLASS = "lastLaunchedPluginClass";
    private final static Logger logger = Logger.getLogger(ThaleiaSession.class);
    private final static int NOT_AUTHENTICATED = -1;
    private final ICayenneContextService contextService;
    /**
     * L'id de l'utilisateur qui s'est identifié pour cette session.
     */
    private int authenticatedUserId = NOT_AUTHENTICATED;
    /**
     * L'id du dernier ContentType de module qui a été demandé, lors de la présentation des contents.
     */
    private int lastModuleContentTypeIdBrowsed = 0;
    /**
     * L'id du dernier ContentType d'écran qui a été demandé, lors de la présentation des contents.
     */
    private int lastScreenContentTypeIdBrowsed = 0;
    /**
     * L'id de la dernière locale demandée pour l'édition d'un contenu. Il s'agit de la langue du contenu.
     */
    private int lastContentLocaleId = -1;

    public ThaleiaSession(Request request) {
        super(request);

        // On s'assure que la session n'est pas temporaire.
        this.bind();

        contextService = new SessionContextService();

        // Journalisation de la pile d'appel
        // logger.debug(LogUtils.getStackTrace());

        // logger.debug("Création d'une nouvelle session par " +
        // LogUtils.getCallerInfo(3));
    }

    public static ThaleiaSession get() {
        // logger.debug("Récupération de la session par " +
        // LogUtils.getCallerInfo(3));
        return (ThaleiaSession) Session.get();
    }

    /**
     * Identifie la session pour cet utilisateur (sans vérification de mot de passe), et sans tracer l'identification
     * dans les stats d'accès.
     *
     * @return false si l'identification a échoué, par exemple le compte n'existe pas.
     */
    public boolean authenticateWithNoStats(String username) {
        return authenticate(username, null, false);
    }

    @Override
    public boolean authenticate(String username, String password) {
        return authenticate(username, password, true);
    }

    private synchronized boolean authenticate(String username, String password, boolean traceForStats) {
        // On fabrique un contexte d'édition pour cette identification
        ObjectContext context = getContextService().getNewContext();

        UserDao userDao = new UserDao(context);
        ApplicationParameterDao applicationParameterDao = new ApplicationParameterDao(context);
        LocaleDao localeDao = new LocaleDao(context);

        // On ignore les majuscules dans les identifiants
        String cleanUsername = username.toLowerCase();

        // Si un système de limitation par jeton est configuré, alors on vérifie la présence d'un jeton disponible.
        try {
            User unauthenticatedUser = userDao.findUserByLogin(cleanUsername);
            if (unauthenticatedUser != null && !unauthenticatedUser.getIsAdmin() && !getLicenceService().isTokenAvailable(cleanUsername)) {
                // Plus de token disponible (et le user n'est pas admin) -> message d'erreur
                // On recherche un éventuel message d'erreur personnalisé dans les paramètres de l'application, sinon
                // on prend la version par défaut, dans les fichiers de localisation standards.

                // On journalise dans les évènements internes
                ThaleiaApplication.get().getEventService().storeEvent("application.user.login.error.no.token.available", cleanUsername);

                String defaultMessage = MessagesUtils.getLocalizedMessage("no.token.available", ThaleiaSession.class);

                String language = ThaleiaSession.get().getLocale().getLanguage();
                error(applicationParameterDao.getValue("instance.licence.no.token.available.error." + language, defaultMessage));
                return false;
            }
        } catch (DetailedException e) {
            logger.warn("Impossible de vérifier la présence de token d'identification : " + e);
            // On passe à la suite de l'identification
        }

        logger.debug("Demande d'identification pour login='" + cleanUsername + "' par" + LogUtils.getCallerInfo(3));
        User user = new UserService(contextService, ThaleiaApplication.get().getConfiguration()).loadByLoginAndPassword(cleanUsername, password, context);

        if (user != null) {
            authenticatedUserId = userDao.getPK(user);
            if (traceForStats) {
                user.setLastAccess(Calendar.getInstance().getTime());
                try {
                    userDao.save(user);
                } catch (DetailedException e) {
                    logger.warn("Impossible d'enregistre la date de login pour le compte utilisateur : " + e);
                }
            }

            // On trace l'identification pour les stats
            if (traceForStats) {
                try {
                    ThaleiaApplication.get().getStatsService().storeIdentificationEvent(user.getLogin(), user.getName(), true);

                } catch (Exception e) {
                    logger.warn("Impossible de journaliser l'événement d'identification :" + e);
                }
            }

            // Si demandé dans les paramètres de l'instance, on trace l'identification dans l'API MixPanel
            Analytics.getImplementation().logAuthentication(user);

            // On journalise dans les évènements internes
            ThaleiaApplication.get().getEventService().storeEvent("application.user.login.success", "", user);

            // On fixe la locale de la session avec la locale préférée de l'utilisateur si elle existe.
            Locale preferedLocale = user.getPreferedLocale();
            if (preferedLocale != null) {
                Session.get().setLocale(localeDao.getJavaLocale(preferedLocale));
            }
            return true;

        } else {
            authenticatedUserId = NOT_AUTHENTICATED;

            // On trace l'erreur d'identification pour les stats
            if (traceForStats) {
                try {
                    ThaleiaApplication.get().getStatsService().storeIdentificationEvent(cleanUsername, "", false);
                } catch (Exception e) {
                    logger.warn("Impossible de journaliser l'événement d'identification :" + e);
                }
            }

            // Si demandé dans les paramètres de l'instance, on trace l'identification dans l'API MixPanel
            Analytics.getImplementation().logEvent(ApplicationEvent.LoginError);

            // On journalise dans les évènements internes
            ThaleiaApplication.get().getEventService().storeEvent("application.user.login.error", username);

            // Si le problème est l'absence de licence, on envoie un message explicite
            try {
                // Pour cela, on doit charger l'objet user (sans l'identifier)
                User unauthenticatedUser = userDao.findUserByLogin(cleanUsername);
                if (unauthenticatedUser != null && !getLicenceService().isUserValid(unauthenticatedUser, false)) {
                    error(MessagesUtils.getLocalizedMessage("no.valid.licence", ThaleiaSession.class));
                    // On journalise dans les évènements internes
                    ThaleiaApplication.get().getEventService().storeEvent("application.user.login.error.nolicence", "", unauthenticatedUser);
                }

            } catch (DetailedException e) {
                // pas de message d'erreur
            }
            return false;
        }
    }

    @Override
    public synchronized Roles getRoles() {
        // logger.debug("Recherche des rôles de l'utilisateur courant...");
        Roles result = null;
        try {
            if (isSignedIn()) {
                // On recherche les rôles de l'utilisateur courant
                result = new ThaleiaRoles().getUserRoles(this.authenticatedUserId);
            } else {
                result = ThaleiaRoles.getAnonymousRoles();
            }
        } catch (Exception e) {
            logger.debug("Erreur de récupération des rôles : " + e.toString());
        }
        // logger.debug("Rôles de l'utilisateur courant : " +
        // result.toString());
        return result;
    }

    /**
     * @return l'utilisateur qui a été identifié. Null si non identifié.
     */
    public synchronized User getAuthenticatedUser() {
        if (this.authenticatedUserId != NOT_AUTHENTICATED) {
            // On charge le User à chaque demande pour ne pas stocker cet objet
            // en mémoire sur le serveur, mais juste son id.
            UserDao userDao = new UserDao(contextService.getContextSingleton());
            User user = userDao.get(authenticatedUserId);

            // Si l'utilisateur est identifié, alors on met à jour sa date de
            // dernier accès.
            if (user != null) {
                user.setLastAccess(Calendar.getInstance().getTime());
            }
            return user;

        } else {
            return null;
        }
    }

    public UserService getUserService() {
        return new UserService(contextService, ThaleiaApplication.get().getConfiguration());
    }


    public PluginService getPluginService() throws DetailedException {
        return new PluginService(contextService, ThaleiaApplication.getScheduledExecutorService(),
                ThaleiaApplication.getExecutorFutures(), ThaleiaApplication.get().getConfiguration());
    }

    public ContentService getContentService() {
        return new ContentService(contextService, ThaleiaApplication.get().getConfiguration());
    }

    public ContentTypeService getContentTypeService() {
        return new ContentTypeService(contextService, ThaleiaApplication.get().getConfiguration());
    }

    @Override
    public void invalidate() {
        super.invalidate();
        this.authenticatedUserId = NOT_AUTHENTICATED;
    }

    /**
     * Supprime tous les éléments non commités dans le contexte de persistence.
     */
    public void rollbackChanges() {
        contextService.getContextSingleton().rollbackChanges();
    }

    /**
     * @return le dernier contentType pour lequel on a présenté les content. Null si aucune valeur n'est sotckée.
     */
    public ContentType getLastContentTypeBrowsed(boolean modulesOnly) {
        ContentTypeDao contentTypeDao = new ContentTypeDao(contextService.getContextSingleton());
        if (modulesOnly) {
            return contentTypeDao.get(lastModuleContentTypeIdBrowsed);
        } else {
            return contentTypeDao.get(lastScreenContentTypeIdBrowsed);
        }
    }

    public void setLastContentTypeBrowsed(ContentType contentType) {
        ContentTypeDao contentTypeDao = new ContentTypeDao(contextService.getContextSingleton());
        if (contentType != null) {
            if (contentType.getIsModuleType()) {
                lastModuleContentTypeIdBrowsed = contentTypeDao.getPK(contentType);
            } else {
                lastScreenContentTypeIdBrowsed = contentTypeDao.getPK(contentType);
            }
        }
    }

    public ContentPropertyService getContentPropertyService() {
        return new ContentPropertyService(contextService, ThaleiaApplication.get().getConfiguration());
    }

    /**
     * @return le service de prévisualisation, si l'utilisateur est bien identifié.
     */
    public PreviewService getPreviewService() throws DetailedException, SecurityException {
        return ThaleiaApplication.get().getPreviewService();
    }

    /**
     * Ajoute un message d'erreur dans la session.
     *
     * @param message les paramètres à ajouter dans la chaîne de caractères.
     */
    public void addError(String message) {
        logger.debug("Ajout de l'erreur dans la session : " + message);
        error(message);
    }

    /**
     * @return la dernière locale qui a été utilisée pour éditer les contenus. Ce n'est pas la même chose que la locale
     * des IHM de Thaleia.
     */
    public Locale getLastContentLocale() {
        LocaleDao localeDao = new LocaleDao(contextService.getContextSingleton());
        // Par défaut, la locale des contenus est la locale de l'IHM
        if (lastContentLocaleId == -1) {
            // La locale de l'IHM
            Locale result = localeDao.getLocale(getLocale());

            // On stocke son id en session
            lastContentLocaleId = localeDao.getPK(result);

            // on renvoie le résultat
            return result;

        } else {
            // Une locale a déjà été stockée en session

            // On instancie la locale
            Locale result = localeDao.get(lastContentLocaleId);

            if (result == null) {
                // Si la locale demandée n'existe pas, on renvoie celle de l'IHM
                result = localeDao.getLocale(getLocale());
            }

            return result;
        }
    }

    /**
     * Fixe la dernière locale demandée pour l'édition des contenus. Il s'agit de la langue du contenu.
     */
    public void setLastContentLocale(Locale locale) {
        LocaleDao localeDao = new LocaleDao(contextService.getContextSingleton());
        lastContentLocaleId = localeDao.getPK(locale);
    }

    /**
     * Un identifiant unique, anonyme, stocké dans un cookie à longue durée.
     */
    public String getAnonymousId() {
        // On s'assure d'un identifiant anonyme dans un cookie
        CookieDefaults cookieDefaults = new CookieDefaults();
        cookieDefaults.setMaxAge((int) TimeUnit.DAYS.toSeconds(365));
        CookieUtils cookieUtils = new CookieUtils(cookieDefaults);
        String anonid = cookieUtils.load("anonid");
        if (anonid == null || anonid.isEmpty()) {
            anonid = Unique.getUniqueString(12);
            cookieUtils.save("anonid", anonid);
        }
        return anonid;
    }

    public EditedContentService getEditedContentService() throws DetailedException {
        return new EditedContentService(contextService, ThaleiaApplication.getScheduledExecutorService(),
                ThaleiaApplication.getExecutorFutures(), ThaleiaApplication.get().getConfiguration());
    }

    public PublicationService getPublicationService() throws DetailedException {
        return new PublicationService(contextService, ThaleiaApplication.get().getConfiguration());
    }

    public LicenceService getLicenceService() throws DetailedException {
        return new LicenceService(contextService, ThaleiaApplication.get().getConfiguration());
    }

    public CustomizationService getCustomizationFilesService() throws DetailedException {
        return new CustomizationService(contextService, ThaleiaApplication.get().getConfiguration());
    }

    public BuyProcessService getBuyProcessService() throws DetailedException {
        return new BuyProcessService(contextService, ThaleiaApplication.get().getConfiguration());
    }

    public EventNotificationService getEventService() {
        return new EventNotificationService(contextService, ThaleiaApplication.get().getConfiguration());
    }

    public ICayenneContextService getContextService() {
        return contextService;
    }

}
