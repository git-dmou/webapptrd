package fr.solunea.thaleia.webapp;

import fr.solunea.thaleia.model.Event;
import fr.solunea.thaleia.model.dao.*;
import fr.solunea.thaleia.service.*;
import fr.solunea.thaleia.service.events.EventNotificationService;
import fr.solunea.thaleia.service.events.EventTrigerringService;
import fr.solunea.thaleia.service.utils.Configuration;
import fr.solunea.thaleia.utils.DetailedException;
import fr.solunea.thaleia.utils.FormatUtils;
import fr.solunea.thaleia.utils.LogUtils;
import fr.solunea.thaleia.webapp.api.*;
import fr.solunea.thaleia.webapp.api.customization.CustomizationAPI;
import fr.solunea.thaleia.webapp.api.domain.DomainAPI;
import fr.solunea.thaleia.webapp.context.ApplicationContextService;
import fr.solunea.thaleia.webapp.download.DownloadFilePage;
import fr.solunea.thaleia.webapp.pages.*;
import fr.solunea.thaleia.webapp.pages.admin.AdminPage;
import fr.solunea.thaleia.webapp.pages.admin.UserEditPage;
import fr.solunea.thaleia.webapp.pages.admin.locales.NewLocalePage;
import fr.solunea.thaleia.webapp.pages.content.ContentEditPage;
import fr.solunea.thaleia.webapp.pages.content.ContentsPage;
import fr.solunea.thaleia.webapp.pages.demo.ConfirmationPage;
import fr.solunea.thaleia.webapp.pages.demo.CreateAccountWithLicencePage;
import fr.solunea.thaleia.webapp.pages.demo.EmailValidationPage;
import fr.solunea.thaleia.webapp.pages.demo.FinalizeAccountPage;
import fr.solunea.thaleia.webapp.pages.domains.DomainEditPage;
import fr.solunea.thaleia.webapp.pages.modules.ModulesPage;
import fr.solunea.thaleia.webapp.pages.plugins.EditPluginPage;
import fr.solunea.thaleia.webapp.pages.plugins.PluginsPage;
import fr.solunea.thaleia.webapp.preview.PreviewPage;
import fr.solunea.thaleia.webapp.preview.PublishLoginPage;
import fr.solunea.thaleia.webapp.preview.PublishPage;
import fr.solunea.thaleia.webapp.security.ThaleiaAuthorizationStrategy;
import fr.solunea.thaleia.webapp.security.ThaleiaSession;
import fr.solunea.thaleia.webapp.security.jwt.JWTAuthenticationPage;
import fr.solunea.thaleia.webapp.security.saml.SSOAccessPointPage;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.apache.wicket.*;
import org.apache.wicket.application.AbstractClassResolver;
import org.apache.wicket.authroles.authentication.AbstractAuthenticatedWebSession;
import org.apache.wicket.authroles.authentication.AuthenticatedWebApplication;
import org.apache.wicket.core.util.io.SerializableChecker;
import org.apache.wicket.core.util.objects.checker.CheckingObjectOutputStream;
import org.apache.wicket.core.util.resource.UrlResourceStream;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.IPackageResourceGuard;
import org.apache.wicket.markup.html.SecurePackageResourceGuard;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.pageStore.IDataStore;
import org.apache.wicket.pageStore.memory.HttpSessionDataStore;
import org.apache.wicket.pageStore.memory.MemorySizeEvictionStrategy;
import org.apache.wicket.protocol.http.WebApplication;
import org.apache.wicket.request.Request;
import org.apache.wicket.request.Response;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.request.resource.IResource;
import org.apache.wicket.request.resource.PackageResourceReference;
import org.apache.wicket.request.resource.ResourceReference;
import org.apache.wicket.serialize.java.JavaSerializer;
import org.apache.wicket.settings.IExceptionSettings;
import org.apache.wicket.settings.IRequestLoggerSettings;
import org.apache.wicket.settings.def.RequestCycleSettings;
import org.apache.wicket.util.lang.Bytes;

import javax.annotation.Nonnull;
import javax.servlet.http.HttpServletRequest;
import java.io.*;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryPoolMXBean;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;

public class ThaleiaApplication extends AuthenticatedWebApplication {

    /**
     * Dans le web.xml, le nom du paramètre qui décrit la configuration Cayenne à prendre en compte.
     */
    public static final String CAYENNE_CONFIGURATION_LOCATION_PARAM = "cayenne.configuration.location";
    private static final MyThreadFactory myThreadFactory = new MyThreadFactory();
    private static final Logger logger = Logger.getLogger(ThaleiaApplication.class);
    /**
     * Y-a-t-il eu une erreur à l'initialisation de l'application ?
     */
    private static boolean initializationError = false;
    private volatile static ScheduledExecutorService scheduledExecutorService;
    private volatile static Map<String, Future<?>> executorServiceFutures;
    public ICayenneContextService contextService;
    private Configuration configuration;
    private Class<? extends Page> homePageClass;
    private EventTrigerringService eventTrigerringService;

    /**
     * @return le service d'exécution de tâches.
     */
    public static ScheduledExecutorService getScheduledExecutorService() {
        if (scheduledExecutorService == null) {
            // Enregistrement de 10 tâches possibles, avec possibilité de planifier les
            // tâches
            scheduledExecutorService = Executors.newScheduledThreadPool(10, myThreadFactory);
        }

        return scheduledExecutorService;
    }

    /**
     * @return les ScheduledFuture enregistrées
     */
    public static Map<String, Future<?>> getExecutorFutures() {
        if (executorServiceFutures == null) {
            executorServiceFutures = new HashMap<>();
        }
        return executorServiceFutures;
    }

    public static ThaleiaApplication get() {
        return (ThaleiaApplication) WebApplication.get();
    }

    /**
     * Renvoie le service de fichiers temporaires l'application.
     */
    public static TempFilesService getTempFilesService(Configuration configuration) throws DetailedException {
        // On recherche le nom du répertoire racine local pour les
        // fichiers temporaires
        File localTempDir = configuration.getTempFilesDir();

        // On renvoie un service de publication
        try {
            return new TempFilesService(localTempDir, getScheduledExecutorService(), getExecutorFutures());

        } catch (DetailedException e) {
            throw new DetailedException(e).addMessage("Impossible de préparer le service de fichiers temporaires.");
        }
    }

    public TempDirService getTempDirService() {
        return new TempDirService(contextService, configuration);
    }

    private void initEventTriggeringSchedulerService() {
        eventTrigerringService = EventTrigerringService.getInstance(contextService, configuration, getScheduledExecutorService());
    }

    /**
     * La page à présenter, si la classe associée a été installée dans les paramètres de l'application, sinon sa version
     * par défaut.
     *
     * @param classNameParamName    le nom du paramètre de l'application qui pointe sur la classe de la page que l'on
     *                              veut obtenir
     * @param classNameDefaultValue la valeur par défaut pour ce paramètre (s'il n'existe pas)
     * @param mountUrlPage          le point de montage à associer à cette page. Si null, alors pas de montage
     */
    @SuppressWarnings("unchecked")
    public Class<? extends Page> getRedirectionPage(String classNameParamName, String classNameDefaultValue,
                                                    String mountUrlPage) {

        String pageClassName = classNameDefaultValue;

        try {
            // On recherche le paramètre de l'application qui contient la classe
            // à utiliser pour la page d'accueil.
            // Configuration.AUTHENTIFIED_USERS_WELCOME_PAGE,
            // "fr.solunea.thaleia.webapp.pages.plugins.PluginsPage"
            pageClassName = ThaleiaApplication.get().getConfiguration().getDatabaseParameterValue(classNameParamName,
                    classNameDefaultValue);

            // On charge la classe qui correspond à cette valeur
            // logger.debug("Tentative d'instanciation de la page destination de
            // la redirection : " + pageClassName);
            Class<? extends Page> redirectionClass = (Class<? extends Page>) Class.forName(pageClassName, true,
                    getPluginService().getClassLoader());

            if (mountUrlPage != null) {
                // On monte la page pour avoir une URL plus jolie
                ThaleiaApplication.get().mountPage(mountUrlPage, redirectionClass);
            }

            // On renvoie cette page
            return redirectionClass;

        } catch (Exception e) {
            logger.warn("Impossible de rediriger vers " + classNameParamName + "=" + pageClassName + " : " + e + "\n"
                    + LogUtils.getStackTrace(e.getStackTrace()));

            // On tente de rediriger sur la page par défaut
            try {
                return (Class<? extends Page>) Class.forName(classNameDefaultValue, true,
                        ThaleiaSession.get().getPluginService().getClassLoader());

            } catch (Exception e2) {
                logger.warn("Impossible de rediriger vers " + classNameDefaultValue + " : " + e2 + "\n"
                        + LogUtils.getStackTrace(e2.getStackTrace()));
                // Page d'erreur
                return ErrorPage.class;
            }

        }
    }

    @Override
    public Class<? extends Page> getHomePage() {
        // logger.debug("Demande de la page de l'application
        // (initializationError=" + initializationError + ")");
        if (initializationError) {
            return ErrorPage.class;
        } else {
            if (homePageClass == null) {
                resetHomePage();
            }
            return homePageClass;
        }
    }

    @Override
    protected void onDestroy() {
        logger.info("Destruction de l'exécuteur des tâches planifiées...");
        try {
            getScheduledExecutorService().shutdownNow();
            logger.info("Destruction de l'exécuteur des tâches ok !");
        } catch (Exception e) {
            logger.warn("Impossible de détruire l'exécuteur des tâches planifiées : " + e);
        }
        getEventService().storeEvent("application.destroyed","");
        logger.info("Fermeture du contexte Cayenne...");
        contextService.close();
    }

    /**
     * Indique à l'application de rechercher en base la nouvelle page d'accueil.
     */
    public void resetHomePage() {
        homePageClass = getRedirectionPage(Configuration.PUBLIC_USERS_WELCOME_PAGE,
                Configuration.DEFAULT_PUBLIC_WELCOME_PAGE, Configuration.HOME_PUBLIC_MOUNT_POINT);
    }

    /**
     * @return Récupère dans les paramètres de l'application (base de données) la page de login à utiliser.
     */
    public Class<? extends Page> getLoginPage() {
        try {
            String loginPageName =
                    ThaleiaApplication.get().getConfiguration().getDatabaseParameterValue(Configuration.LOGIN_PAGE,
                            "fr.solunea.thaleia.webapp.pages.LoginPage");
            @SuppressWarnings("unchecked") Class<? extends Page> result = (Class<? extends Page>) ThaleiaApplication
                    .this.getApplicationSettings().getClassResolver().getClassLoader().loadClass(loginPageName);
            return result;

        } catch (Exception e) {
            logger.info("Impossible de retrouver la classe de la page de login dans le paramètre "
                    + Configuration.LOGIN_PAGE);
            logger.info(e);
            try {
                @SuppressWarnings("unchecked") Class<? extends Page> result =
                        (Class<? extends Page>) ThaleiaApplication.this.getApplicationSettings().getClassResolver().getClassLoader().loadClass("fr.solunea.thaleia.webapp.pages.LoginPage");
                return result;
            } catch (ClassNotFoundException e1) {
                logger.warn("Impossible de retrouver la classe de la page de login par défaut !");
                logger.warn(e1);
                return ErrorPage.class;
            }
        }
    }

    @Override
    protected synchronized void init() {

        // On n'appelle pas le service : getEventService().storeEvent(); car l'application n'est pas encore initialisée.
        // On prépare juste un timestamp pour le stocker plus tard
        LocalDateTime startTime = LocalDateTime.now();

        try {
            logger.info("Initialisation de l'application...");

            // On journalise des informations systèmes
            logSystemConf();

            // On ajoute un traceur de cycle de vie
            if (getDebugSettings().isDevelopmentUtilitiesEnabled()) {
                getRequestCycleListeners().add(new RequestLogger());
            }

            //Pour le développement on augmente les traces
            if (getConfigurationType().equals(RuntimeConfigurationType.DEVELOPMENT)) {
                getDebugSettings().setOutputComponentPath(true);
            }

            // On fixe l'encodage par défaut
            getMarkupSettings().setDefaultMarkupEncoding("UTF-8");

            // Pour le menu
            getMarkupSettings().setStripWicketTags(true);

            // Gain de mémoire
            getMarkupSettings().setCompressWhitespace(true);

            // Progression des uploads
            getApplicationSettings().setUploadProgressUpdatesEnabled(true);

            // On veut s'assurer d'obtenir des informations sur le navigateur
            // client, par exemple pour déclencher des alternatives au
            // lazyloading si javascript est désactivé.
            getRequestCycleSettings().setGatherExtendedBrowserInfo(true);

            // Pour éviter les problèmes en cluster, on modifie la stratégie :
            // Normalement ce n'est pas nécessaire si on active le sticky_session sur le worker mod_jk
            getRequestCycleSettings().setRenderStrategy(RequestCycleSettings.RenderStrategy.ONE_PASS_RENDER);

            // TODO : faire fonctionner ce DataStore : le problème est que des npe "aléatoires" apparaissent lors des désérialisations d'objets
            // By default Wicket uses a file based session page store where the serialized pages are written to.
            // Wicket stores those to support the browser back button and to render older versions of the page when
            // the back button is pressed.
            // In a cluster setup the serialized pages must be stored in the session so that the pages can be
            // synchronized between the cluster nodes.
            setPageManagerProvider(new DefaultPageManagerProvider(this) {
                @Override
                protected IDataStore newDataStore() {
                    // Attention : avec une stratégie d'évication basée sur le nombre de pages (PageNumberEvictionStrategy)
                    // et un pagesNumber à 5, on avait un problème "bizarre" lors de la soumission
                    // du formulaire de login : l'appel POST du loginFrom était bien envoyé, mais n'était pas traité côté serveur :
                    // la page était rechargée, au lieu de déclencher le traitement SignInPanel.SignInForm.onSubmit()
                    // Problème réglé avec cette stratégie d'éviction, mais pourquoi cette valeur ?
                    return new HttpSessionDataStore(getPageManagerContext(), new MemorySizeEvictionStrategy(Bytes.megabytes(5)));
                }
            });

            // Paramétrage des traitements de requêtes
            logger.debug("Stratégie de rendu = " + this.getRequestCycleSettings().getRenderStrategy());
            logger.debug("Encodage de la réponse = " + this.getRequestCycleSettings().getResponseRequestEncoding());
            logger.debug("Timeout = " + this.getRequestCycleSettings().getTimeout());
            logger.debug("Taille maximale des sessions = "
                    + FormatUtils.humanReadableByteCount(getStoreSettings().getMaxSizePerSession().bytes(), true));

            // Permet d'activer l'interprétation des annotations
            // @StatelessComponent
            // getComponentPreOnBeforeRenderListeners()
            // .add(new StatelessChecker());

            // La gestion des erreurs
            getApplicationSettings().setInternalErrorPage(ErrorPage.class);
            if (usesDevelopmentConfig()) {
                // En développement, on présente la trace complète des
                // exceptions inattendues.
                getExceptionSettings().setUnexpectedExceptionDisplay(IExceptionSettings.SHOW_EXCEPTION_PAGE);
            } else {
                // En prod, on ne montre qu'une page "Erreur" vide.
                getExceptionSettings().setUnexpectedExceptionDisplay(IExceptionSettings.SHOW_INTERNAL_ERROR_PAGE);
            }

            logger.info("Instanciation de l'accès au contexte de persistence...");
            String configurationLocation =
                    this.getServletContext().getInitParameter(CAYENNE_CONFIGURATION_LOCATION_PARAM);
            // On va chercher le context-param
            // 'CAYENNE_CONFIGURATION_LOCATION_PARAM' dans le web.xml.
            // Ce paramètre contient le nom du fichier XML cayenne à rechercher.
            // Par exemple cayenne-ThaleiaDomain.xml
            // Ce runtime a vocation a être utilisé pour obtenir un contexte
            // Cayenne lié à l'application ; pour obtenir un contexte Cayenne
            // lié à la session, il faut passer par
            // ThaleiaSession.getContextService()
            contextService = new ApplicationContextService(configurationLocation);

            logger.info("Initialisation de la configuration...");
            // (tomcat_base)/tmp/thaleiaXXX
            File serverTempDir = new File(System.getProperty("java.io.tmpdir") + getServletContext().getContextPath());
            try {
                logger.debug("Création du répertoire des données temporaires : " + serverTempDir.getAbsolutePath() + "("
                        + " existe = " + serverTempDir.exists() + ")");
                FileUtils.forceMkdir(serverTempDir);
            } catch (Exception e) {
                throw new DetailedException(
                        "Impossible de créer le répertoire temporaire local " + serverTempDir.getAbsolutePath());
            }
            configuration = new Configuration(contextService, serverTempDir);

            logger.info("Initialisation de la base de données...");
            getDatabaseService().initializeDatabase();

            // La protection des pages par annotations
            this.getSecuritySettings().setAuthorizationStrategy(new ThaleiaAuthorizationStrategy(this));

            // Montage des pages pour des URL plus jolies
            // Si une page n'est pas montée, elle ne bénéficiera pas de la CSS.
            // mountPackage("/", BasePage.class);
            mountPage("/", getHomePage());
            mountPage(Configuration.HOME_PUBLIC_MOUNT_POINT, LoginPage.class);
            mountPage(Configuration.LOGIN_MOUNT_POINT, LoginPage.class);
            mountPage("/externallogin", ExternalLoginPage.class);
            mountPage("/content", ContentEditPage.class);
            mountPage("/contents", ContentsPage.class);
            mountPage("/modules", ModulesPage.class);
            mountPage("/error", ErrorPage.class);
            mountPage("/admin", AdminPage.class);
            mountPage("/user", UserEditPage.class);
            mountPage("/domain", DomainEditPage.class);
            mountPage("/actions", PluginsPage.class);
            mountPage("/action", EditPluginPage.class);
            mountPage("/locale", NewLocalePage.class);
            mountPage("/resetrequest", ResetPasswordRequestPage.class);
            mountPage("/reset", ResetPasswordPage.class);
            mountPage("/preview", PreviewPage.class);
            mountPage("/" + LRSService.PUBLISH_PAGE_MOUNT_POINT, PublishPage.class);
            mountPage("/publishlogin", PublishLoginPage.class);
            mountPage("/new", CreateAccountWithLicencePage.class);
            mountPage("/success", ConfirmationPage.class);
            mountPage("/" + Configuration.LEGAL_SITE, LegalNoticePage.class);
            mountPage(Configuration.EMAIL_VALIDATION_PAGE, EmailValidationPage.class);
            mountPage(Configuration.ACCOUNT_FINALIZATION_PAGE, FinalizeAccountPage.class);
            mountPage("/sso", SSOAccessPointPage.class);
            mountPage("/Web/SSO", JWTAuthenticationPage.class);
            mountPage(Configuration.DOWNLOADABLE_FILES_MOUNT_POINT, DownloadFilePage.class);
            mountPage("/start", CreateAccountWithLicencePage.class);

            // On autorise l'accès aux extensions .pdf dans les points de
            // montage de ressources
            IPackageResourceGuard packageResourceGuard = getResourceSettings().getPackageResourceGuard();
            if (packageResourceGuard instanceof SecurePackageResourceGuard) {
                SecurePackageResourceGuard guard = (SecurePackageResourceGuard) packageResourceGuard;
                guard.addPattern("+*.pdf");
            }
            // On monte le PDF des Conditions Générales de Vente
            mountResource(configuration.getCgvMountPoint(), new PackageResourceReference(ThaleiaApplication.class,
                    "20150701-CGV-Thaleia.pdf"));
            mountResource(configuration.getCgvMountPointEN(), new PackageResourceReference(ThaleiaApplication.class,
                    "20150701-CGV-Thaleia_en.pdf"));

            // Chiffrage des URL
            // Nécessaire ?
            // IRequestMapper cryptoMapper = new
            // CryptoMapper(getRootRequestMapper(), this);
            // setRootRequestMapper(cryptoMapper);

            // Initialisation du classloader
            setClassLoader();

            // Nettoyage des répertoires temporaires des services de
            // prévisualisation.
            try {
                PreviewService.deleteFiles(ThaleiaApplication.get().getConfiguration().getLocalPreviewDir());
            } catch (Exception e) {
                logger.warn("Impossible de nettoyer les fichiers de prévisualisation : " + e);
            }

            // Nettoyage des fichiers temporaires de l'application
            try {
                TempFilesService.deleteFiles(ThaleiaApplication.get().getConfiguration().getTempFilesDir());
            } catch (Exception e) {
                logger.warn("Impossible de nettoyer les fichiers temporaires : " + e);
            }
            // On nettoie également les répertoires temporaires, qui sont utilisent les fichiers temporaires pour stocker les fichiers : si les fichiers viennent d'être nettoyés, il faut également nettoyuer les objets en base qui y font référence.
            try {
                getTempDirService().cleanAll();
            } catch (Exception e) {
                logger.warn("Impossible de nettoyer les fichiers temporaires : " + e);
            }

            // Préparation des EditedContent
            EditedContentService editedContentService = new EditedContentService(contextService,
                    getScheduledExecutorService(), getExecutorFutures(), ThaleiaApplication.get().getConfiguration());
            editedContentService.check(contextService.getNewContext());

            // On ajoute un finder de resources qui va chercher les ressources
            // dans le classloader où sont chargés les plugins. Ces ressources
            // seront recherchées comme des resources localisées, dans le
            // répertoire des sources Java des plugins.
            getResourceSettings().getResourceFinders().add((clazz, pathname) -> {
                try {
                    // la seule différence avec les ResourcesFinder
                    // par défaut est l'usage de CE classloader, qui
                    // contient les jar des plugins.
                    ClassLoader classLoader =
                            ThaleiaApplication.this.getApplicationSettings().getClassResolver().getClassLoader();

                    if (classLoader == null) {
                        return null;
                    }

                    URL url = classLoader.getResource(pathname);

                    if (url != null) {
                        return new UrlResourceStream(url);
                    }
                } catch (Exception e) {
                    logger.info("Impossible de retrouver la ressource '" + pathname + "' dans le classloader "
                            + "contenant les plugins : " + e);
                }
                return null;
            });


            // Initialisation des licences utilisateur
            logger.info("Initialisation des licences utilisateur...");
            try {
                LicenceService licenceService = new LicenceService(contextService, configuration);
                licenceService.installLicences(contextService.getContextSingleton());
            } catch (DetailedException e) {
                //throw e.addMessage("Impossible d'initialiser les licences utilisateur.");
            }

            // Activation du logger, pour l'analyse des sessions
            IRequestLoggerSettings reqLogger = getRequestLoggerSettings();
            reqLogger.setRequestLoggerEnabled(true);

            // La page à présenter en cas d'erreur de droits
            getApplicationSettings().setAccessDeniedPage(getLoginPage());

            // La page à présenter à l'expiration de la session
            getApplicationSettings().setPageExpiredErrorPage(getLoginPage());

            // Inscription de notre propre classe de
            // sérialisation (par exemple pour journaliser les erreurs de
            // serialisation des pages dans la session).
            JavaSerializer javaSerializer = new JavaSerializer(getApplicationKey()) {
                @Override
                protected ObjectOutputStream newObjectOutputStream(OutputStream out) throws IOException {
                    return new ThaleiaSerializationCheckerObjectOutputStream(out);
                }
            };
            getFrameworkSettings().setSerializer(javaSerializer);

            logger.info("Mise à jour des comptes utilisateurs dans l'application frontend si elle a été définie...");
            try {
                new FrontendService(configuration).updateAllUsers(contextService.getContextSingleton());
            } catch (DetailedException e) {
                throw e.addMessage("Impossible de mettre à jour des comptes utilisateurs dans l'application frontend.");
            }
            logger.info("Mise à jour des comptes utilisateurs dans l'application frontend si elle a été définie ok.");

            logger.info("Initialisation de la récupération des données des LRSEndpoints...");
            if (configuration.getDatabaseParameterValue(Configuration.LRSENDPOINTS_RETRIEVAL_ENABLED, "false").equalsIgnoreCase("true")) {
                LrsEndpointsService.scheduleLrsUpdates(contextService, configuration);
            }
            logger.info("Initialisation de la récupération des données des LRSEndpoints ok !");

            // Initialisation du nettoyeur de tokens périmés
            ApiTokenService.scheduleTokensCleaning(contextService);

            // Initialisation du nettoyeur de fileuploads périmés
            FileUploadCleanerService.scheduleFileUploadsCleaning(contextService);

            // Initialisation du nettoyeur de tempdirs périmés
            TempDirCleanerService.scheduleCleaning(contextService);

            // Initialisation du nettoyeur de publicationSession
            PublicationSessionService.scheduleCleaning(contextService);

            // Initialisation des générateurs d'événements
            logger.info(" Initialisation des générateurs d'événements...");
            initEventTriggeringSchedulerService();
            logger.info(" Initialisation des générateurs d'événements ok !");

            logger.info("Initialisation de la génération des rapports sur des données des LRSEndpoints...");
            rescheduleLrsReports();
            logger.info("Initialisation de la génération des rapports sur des données des LRSEndpoints ok !");

            // Vérification de la présence d'une paire de clés RSA (pour les signatures de JWT et SAML)
            getUserService().checkRSAKeys();

            // Initialisation de l'API
            try {
                initAPI();
            } catch (Exception e) {
                throw new DetailedException(e).addMessage("Impossible d'initialiser l'API !");
            }

            logger.info("Initialisation de l'application ok !");
            getEventService().storeEvent("application.init.started",
                    "init duration=" + (LocalDateTime.now().toEpochSecond(ZoneOffset.UTC) - startTime.toEpochSecond(ZoneOffset.UTC)) + "s");

        } catch (DetailedException e) {
            logger.error("Une erreur a eu lieu lors de l'initialisation de l'application : " + e);
            initializationError = true;

            try {
                // On tente de stocker l'erreur
                getEventService().storeEvent("application.init.error", "");
            } catch (Exception ex) {
                // rien
            }
        }

        // Tentative de stockage de l'information de début de démarrage
        try {
            EventDao eventDao = new EventDao(contextService.getNewContext());
            Event event = eventDao.get();
            event.setName("application.init.starting");
            event.setDetail("");
            event.setDate(startTime);
            eventDao.save(event, true);
        } catch (DetailedException e) {
            logger.warn("Impossible de stocker les events : " + e);
        }
    }

    private void initAPI() {
        mountResource("/api/v1", new ResourceReference("restReferenceRoot") {
            final ApiV1Service resource = new ApiV1Service(contextService, configuration);

            @Override
            public IResource getResource() {
                return resource;
            }

        });
        mountResource("/api/v1/cmi", new ResourceReference("restReferenceCmi") {
            final CmiV1Service resource = new CmiV1Service(contextService, configuration);

            @Override
            public IResource getResource() {
                return resource;
            }

        });
        mountResource("/api/v1/publication", new ResourceReference("restReferencePublication") {
            final PublicationAPI resource =
                    new PublicationAPI(contextService, configuration);

            @Override
            public IResource getResource() {
                return resource;
            }

        });
        mountResource("/api/v1/transform", new ResourceReference("restReferenceTransform") {
            final TransformAPI resource =
                    new TransformAPI(contextService, configuration);

            @Override
            public IResource getResource() {
                return resource;
            }

        });
        mountResource("/api/v1/usage", new ResourceReference("restReferenceUsage") {
            final UsageAPI resource =
                    new UsageAPI(contextService, configuration);

            @Override
            public IResource getResource() {
                return resource;
            }
        });
        mountResource("/api/v1/plugin", new ResourceReference("restReferencePlugin") {
            final PluginAPI resource =
                    new PluginAPI(contextService, configuration,
                            getScheduledExecutorService(), getExecutorFutures());

            @Override
            public IResource getResource() {
                return resource;
            }
        });
        mountResource("/api/v1/tempdirs", new ResourceReference("restReferenceTempdir") {
            final TempDirsAPI resource =
                    new TempDirsAPI(contextService, configuration);

            @Override
            public IResource getResource() {
                return resource;
            }
        });
        mountResource(UserAccountAPI.API_END_POINT, new ResourceReference("restReferenceUseraccount") {
            final UserAccountAPI resource =
                    new UserAccountAPI(contextService, configuration);

            @Override
            public IResource getResource() {
                return resource;
            }
        });
        mountResource("/api/v1/contentVersion", new ResourceReference("restReferenceContentVersion") {
            final ContentVersionAPI resource =
                    new ContentVersionAPI(contextService, configuration);

            @Override
            public IResource getResource() {
                return resource;
            }

        });
        mountResource("/api/v1/customization", new ResourceReference("restReferenceCustomization") {
            final CustomizationAPI resource =
                    new CustomizationAPI(contextService, configuration);

            @Override
            public IResource getResource() {
                return resource;
            }

        });

        mountResource("/api/v1/domain", new ResourceReference("restReferenceDomains") {
            final DomainAPI resource =
                    new DomainAPI(contextService, configuration);

            @Override
            public IResource getResource() {
                return resource;
            }

        });

    }

    @Override
    public Session newSession(Request request, Response response) {
        return new ThaleiaSession(request);
    }

    @Override
    protected Class<? extends AbstractAuthenticatedWebSession> getWebSessionClass() {
        return ThaleiaSession.class;
    }

    @Override
    protected Class<? extends WebPage> getSignInPageClass() {
        return WelcomePublicPage.class;
    }

    public void rescheduleLrsReports() throws DetailedException {
        LrsEndpointsService.scheduleReports(contextService, configuration, getTempFilesService());
    }

    public ContentDao getContentDao() {
        return new ContentDao(contextService.getContextSingleton());
    }

    public LocaleDao getLocaleDao() {
        return new LocaleDao(contextService.getContextSingleton());
    }

    private DatabaseService getDatabaseService() throws DetailedException {
        return new DatabaseService(contextService, configuration, getTempFilesService(), getServletContext());
    }

    public ExportService getExportService() throws DetailedException {
        return new ExportService(contextService, getScheduledExecutorService(), getExecutorFutures(), configuration);
    }

    public LrsEndpointDao getLrsEndPointDao() {
        return new LrsEndpointDao(contextService.getContextSingleton());
    }

    public DownloadableFileDao getDownloadableFileDao() {
        return new DownloadableFileDao(contextService.getContextSingleton());
    }

    /**
     * Journalise des informations du contexte d'exécution : mémoire, processeurs, espace disque...
     */
    private void logSystemConf() {
        /* Total number of processors or cores available to the JVM */
        logger.info("Available processors (cores): " + Runtime.getRuntime().availableProcessors());

        /* Total amount of free memory available to the JVM */
        logger.info("Free memory (bytes): " + Runtime.getRuntime().freeMemory());

        /* This will return Long.MAX_VALUE if there is no preset limit */
        long maxMemory = Runtime.getRuntime().maxMemory();
        /* Maximum amount of memory the JVM will attempt to use */
        logger.info("Maximum memory (bytes): " + (maxMemory == Long.MAX_VALUE ? "no limit" : maxMemory));

        /* Total memory currently in use by the JVM */
        logger.info("Total memory (bytes): " + Runtime.getRuntime().totalMemory());

        /* Get a list of all filesystem roots on this system */
        File[] roots = File.listRoots();

        /* For each filesystem root, print some info */
        for (File root : roots) {
            logger.info("File system root: " + root.getAbsolutePath());
            logger.info("Total space (bytes): " + root.getTotalSpace());
            logger.info("Free space (bytes): " + root.getFreeSpace());
            logger.info("Usable space (bytes): " + root.getUsableSpace());
        }

        List<MemoryPoolMXBean> memoryPoolMXBeans = ManagementFactory.getMemoryPoolMXBeans();
        for (MemoryPoolMXBean pool : memoryPoolMXBeans) {
            logger.info("Memory pool: " + pool.getName());
            logger.info("    type: " + pool.getType());
            logger.info("    usage: " + pool.getUsage());
        }

    }

    /**
     * Centralise l'aspect des pop-up modales de l'application. Les modifications de l'utilisateur (position, taille,
     * etc.) sont stockées dans un cookie, et seront partagées entre les instances de pop-ups.
     */
    public void configureModalPopup(ModalWindow modal) {
        // Configuration de la popup
        modal.setCssClassName(ModalWindow.CSS_CLASS_GRAY);
        modal.setMaskType(ModalWindow.MaskType.SEMI_TRANSPARENT);
        modal.setInitialWidth(500);
        modal.setWidthUnit("px");
        modal.setResizable(true);
        modal.setUseInitialHeight(false);
        modal.setCookieName("wicket-tips/styledModal");
    }

    /**
     * Initialise le classLoader de l'application.
     */
    public void setClassLoader() {
        this.getApplicationSettings().setClassResolver(new AbstractClassResolver() {
            @Override
            public ClassLoader getClassLoader() {
                // logger.debug("On renvoie le classLoader de l'application.");
                try {
                    // On demande accès au contexte Cayenne ouvert par
                    // l'application, et non celui de la session
                    // utilisateur, car le service demandé ne va pas
                    // agir dans un contexte de session utilisateur.
                    //                    PluginService pluginService = new PluginService(contextService,
                    // getScheduledExecutorService(),
                    //                            getExecutorFutures(), ThaleiaApplication.get().getConfiguration());
                    //                    return pluginService.getClassLoader();
                    return getPluginService().getClassLoader();

                } catch (Exception e) {
                    logger.warn("Impossible de renvoyer le classloader de l'application : " + e);
                    // ClassLoader par défaut
                    return Thread.currentThread().getContextClassLoader();
                }
            }
        });

    }

    /**
     * Renvoie le service de prévisualisation de l'application.
     */
    public PreviewService getPreviewService() throws DetailedException {
        // On recherche le nom du répertoire racine local pour les
        // prévisualisations
        File localRootDir = ThaleiaApplication.get().getConfiguration().getLocalPreviewDir();

        String previewUrl = getRequestContextPath() + "/preview";

        // On renvoie un service de publication
        try {
            return new PreviewService(localRootDir, previewUrl, getScheduledExecutorService(), getExecutorFutures());

        } catch (DetailedException e) {
            throw new DetailedException(e).addMessage("Impossible de préparer le service de prévisualisation.");
        }
    }

    /**
     * @return la configuration de l'application.
     */
    public Configuration getConfiguration() {
        return this.configuration;
    }

    /**
     * @return le contexte d'appel de la requête. Par exemple pour une requête courante à :
     * "https://serveur/application/wicket/page?8" la méthode va renvoyer "application/wicket/page?8"
     */
    private String getRequestContextPath() {
        if (RequestCycle.get() != null) {
            // On est bien dans une requête du cycle de vie de Wicket

            HttpServletRequest req = (HttpServletRequest) RequestCycle.get().getRequest().getContainerRequest();
            try {
                req.setCharacterEncoding("UTF-8");
            } catch (UnsupportedEncodingException e) {
                logger.debug("Problème encoding");
            }
            // ContextPath = le nom du contexte de la webapp
            // Attention, cette méthode peut rendre des résultats hasardeux si
            // on
            // est derrière un frontal web.
            return req.getContextPath();
        } else {
            // On est dans une servlet non Wicket.
            return "";
        }

    }

    /**
     * @return l'URL racine de l'application, sans le dernier /. Par exemple : "http://thaleia.server.fr/thaleia"
     */
    public String getApplicationRootUrl() {
        return configuration.getApplicationRootUrl();
    }

    /**
     * L'URL d'appel pour la prévisualisation. Par exemple : "https://serveur/application/publish" pour que les accès se
     * fasse par : "https://serveur/application/publish/123456"
     */
    public String getPublishUrl() {
        return configuration.getPublishUrl();
    }

    public PublicationService getPublicationService() throws DetailedException {
        return new PublicationService(contextService, configuration);
    }

    public LRSService getLRSService() {
        return new LRSService(contextService, configuration);
    }

    public StatsService getStatsService() {
        return new StatsService(contextService);
    }

    public EventNotificationService getEventNotificationService() {
        return new EventNotificationService(contextService, configuration);
    }

    public PluginService getPluginService() throws DetailedException {
        return new PluginService(contextService, getScheduledExecutorService(), getExecutorFutures(), configuration);
    }

    public StatDataDao getStatDataDao() {
        return new StatDataDao(contextService.getContextSingleton());
    }

    public LicenceService getLicenceService() throws DetailedException {
        return new LicenceService(contextService, configuration);
    }

    public LicenceDao getLicenceDao() {
        return new LicenceDao(contextService.getContextSingleton());
    }

    //    private class MyRejectedExecutionHandler implements RejectedExecutionHandler {
    //        @Override
    //        public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
    //            // Method that may be invoked by a ThreadPoolExecutor when execute cannot accept a task. This may
    // occur
    //            // when no more threads or queue slots are available because their bounds would be exceeded, or upon
    //            // shutdown of the Executor.
    //            logger.warn("Impossible d'exécuter la tâche " + r.toString());
    //            throw new RejectedExecutionException("Task " + r.toString() + " rejected from " + executor.toString
    // ());
    //        }
    //    }

    public UserService getUserService() {
        return new UserService(contextService, configuration);
    }

    public DocumentsService getDocumentsService() {
        return new DocumentsService();
    }

    public ContentPropertyService getContentPropertyService() {
        return new ContentPropertyService(contextService, configuration);
    }

    public AdhocReportService getAdhocReportService() throws DetailedException {
        return new AdhocReportService(getDownloadableFileService(), getUserDao(), getTempFilesService());
    }

    public DownloadableFileService getDownloadableFileService() throws DetailedException {
        return new DownloadableFileService(contextService, configuration);
    }

    public UserDao getUserDao() {
        return new UserDao(contextService.getContextSingleton());
    }

    /**
     * Renvoie le service de fichiers temporaires l'application.
     */
    public TempFilesService getTempFilesService() throws DetailedException {
        return getTempFilesService(configuration);
    }

    public FrontendService getFrontendService() {
        return new FrontendService(configuration);
    }

    public ApplicationParameterDao getApplicationParameterDao() {
        return new ApplicationParameterDao(contextService.getContextSingleton());
    }

    public ContentService getContentService() {
        return new ContentService(contextService, configuration);
    }

    public ContentPropertyDao getContentPropertyDao() {
        return new ContentPropertyDao(contextService.getContextSingleton());
    }

    public ApiTokenDao getApiTokenDao() {
        return new ApiTokenDao(contextService.getContextSingleton());
    }

    public FileUploadService getFileUploadService() {
        return new FileUploadService(configuration);
    }

    public PluginDao getPluginDao() {
        return new PluginDao(contextService.getContextSingleton());
    }

    public ContentTypeDao getContentTypeDao() {
        return new ContentTypeDao(contextService.getContextSingleton());
    }

    public LicenceHoldingDao getLicenceHoldingDao() {
        return new LicenceHoldingDao(contextService.getContextSingleton());
    }

    public ApiV1Service getApiV1Service() {
        return new ApiV1Service(contextService, configuration);
    }

    public ContentVersionDao getContentVersionDao() {
        return new ContentVersionDao(contextService.getContextSingleton());
    }

    public EventTrigerringService getEventTrigerringService() {
        return eventTrigerringService;
    }

    public EventService getEventService() {
        return new EventService(contextService);
    }

    /**
     * Write objects to the wrapped output stream and log a meaningful message for serialization problems. <p> <p> Note:
     * the checking functionality is used only if the serialization fails with NotSerializableException. This is done so
     * to save some CPU time to make the checks for no reason. </p>
     */
    private static class ThaleiaSerializationCheckerObjectOutputStream extends ObjectOutputStream {
        private final OutputStream outputStream;

        private final ObjectOutputStream oos;

        private ThaleiaSerializationCheckerObjectOutputStream(OutputStream outputStream) throws IOException {
            this.outputStream = outputStream;
            oos = new ObjectOutputStream(outputStream);
        }

        @Override
        protected final void writeObjectOverride(Object obj) throws IOException {
            try {
                oos.writeObject(obj);

            } catch (NotSerializableException nsx) {
                logger.debug("Erreur de sérialisation : " + nsx + "\n" + LogUtils.getStackTrace(nsx.getStackTrace()));

                if (CheckingObjectOutputStream.isAvailable()) {
                    try (SerializableChecker serializeChecker = new SerializableChecker(outputStream, nsx)) {
                        // On tente à nouveau la sérialisation, mais en cherchant à obtenir plus d'informations sur l'erreur.
                        serializeChecker.writeObject(obj);
                    } catch (Exception x) {
                        if (x instanceof CheckingObjectOutputStream.ObjectCheckException) {
                            throw (CheckingObjectOutputStream.ObjectCheckException) x;
                        } else {
                            x.initCause(nsx);
                            throw new WicketRuntimeException("Impossible d'obtenir des informations détaillées sur l'erreur de sérialisation.", x);
                        }
                    }

                    // Pas d'erreur de sérialisation, alors qu'il devrait y en avoir une.
                    throw nsx;
                }
                throw nsx;
            } catch (Exception e) {
                logger.error("error writing object " + obj + ": " + e.getMessage(), e);
                throw new WicketRuntimeException(e);
            }
        }

        @Override
        public void flush() throws IOException {
            oos.flush();
        }

        @Override
        public void close() throws IOException {
            oos.close();
        }
    }

    private static class MyThreadFactory implements ThreadFactory, Serializable {
        @Override
        public Thread newThread(@Nonnull Runnable target) {
            final Thread thread = new Thread(target);
            logger.debug("Création d'un thread de traitement de tâche.");
            thread.setUncaughtExceptionHandler((t, e) -> logger.warn("Exception levée par une tâche !", e));
            return thread;
        }
    }
}
