package fr.solunea.thaleia.webapp.api;

import fr.solunea.thaleia.model.ApiToken;
import fr.solunea.thaleia.model.ContentVersion;
import fr.solunea.thaleia.model.Publication;
import fr.solunea.thaleia.model.PublicationSession;
import fr.solunea.thaleia.model.dao.*;
import fr.solunea.thaleia.plugins.IPluginImplementation;
import fr.solunea.thaleia.service.ContentService;
import fr.solunea.thaleia.service.LicenceService;
import fr.solunea.thaleia.service.PluginService;
import fr.solunea.thaleia.service.PublicationService;
import fr.solunea.thaleia.service.utils.Configuration;
import fr.solunea.thaleia.service.utils.IPublishHelper;
import fr.solunea.thaleia.utils.DetailedException;
import fr.solunea.thaleia.webapp.ThaleiaApplication;
import fr.solunea.thaleia.webapp.api.transform.ITransformTreatment;
import fr.solunea.thaleia.webapp.api.transform.TransformTreatmentFactory;
import fr.solunea.thaleia.webapp.security.ThaleiaSession;
import org.apache.log4j.Logger;
import org.apache.wicket.authorization.AuthorizationException;
import org.wicketstuff.rest.annotations.MethodMapping;
import org.wicketstuff.rest.annotations.ResourcePath;
import org.wicketstuff.rest.annotations.parameters.HeaderParam;
import org.wicketstuff.rest.annotations.parameters.RequestParam;
import org.wicketstuff.rest.utils.http.HttpMethod;

import java.io.File;
import java.util.Locale;

@ResourcePath("/api/v1/publication")
public class PublicationAPI extends ApiV1Service {

    private final static Logger logger = Logger.getLogger(PublicationAPI.class);
    private final ICayenneContextService contextService;
    private final Configuration configuration;


    public PublicationAPI(ICayenneContextService contextService, Configuration configuration) {
        super(contextService, configuration);
        this.contextService = contextService;
        this.configuration = configuration;
    }

    @MethodMapping("/session/{publicationSessionToken}")
    @SuppressWarnings("unused")
    public String getUsername(@HeaderParam("Authorization") String token, String publicationSessionToken) {
        try {
            // On vérifie que le token d'accès à l'API est valide
            getToken(token, true);

            // On vérifie que la publicationSession existe
            PublicationSessionDao publicationSessionDao = new PublicationSessionDao(contextService.getContextSingleton());
            PublicationSession publicationSession = publicationSessionDao.findByToken(publicationSessionToken);
            if (publicationSession == null) {
                setResponseStatusCode(404);
                return "{}";
            } else if (!publicationSession.isValid()) {
                setResponseStatusCode(403);
                return "{}";
            } else if (publicationSession.getUsername() != null) {
                setResponseStatusCode(200);
                return "{\"username\": \"" + publicationSession.getUsername() + "\"}";
            } else {
                setResponseStatusCode(200);
                return "{\"username\": \"\"h}";
            }

        } catch (AuthorizationException e) {
            logger.warn(e);
            setResponseStatusCode(403);
            return "";
        }

    }

    @MethodMapping(value = "/publication", httpMethod = HttpMethod.POST)
    public Object create(@HeaderParam("Authorization") String token,
                         @RequestParam(value = "content_version_id") String contentVersionId,
                         @RequestParam(value = "locale") String locale) {
        ApiToken apiToken;
        try {
            apiToken = getToken(token, false);
        } catch (Exception e) {
            logger.warn(e);
            setResponseStatusCode(403);
            return null;
        }

        // On vérifie que le plugin Publish est installé
        try {
            PluginService pluginService = ThaleiaApplication.get().getPluginService();
            IPluginImplementation implementation = pluginService.getImplementation("fr.solunea.thaleia.plugins.publish.PublishPlugin");
            if (implementation == null) {
                throw new Exception("Pas de plugin Publish installé.");
            }
        } catch (Exception e) {
            logger.warn(e);
            return error(500, "Internal error.", "");
        }

        // Récupération de la ContentVersion
        ContentVersionDao contentVersionDao = new ContentVersionDao(contextService.getContextSingleton());
        int contentId = Integer.parseInt(contentVersionId);
        ContentVersion contentVersion = contentVersionDao.get(contentId);
        if (contentVersion == null) {
            return error(404, "ContentVersion not found", "");
        }

        // L'utilisateur peut-il consulter cette CV ?
        ContentService contentService;
        try {
            contentService = ThaleiaApplication.get().getContentService();
        } catch (Exception e) {
            logger.warn(e);
            return error(500, "Internal error.", "");
        }
        ContentDao contentDao = new ContentDao(contextService.getContextSingleton());
        if (!contentService.canAccessContent(apiToken.getUser(), contentDao.getPK(contentVersion.getContent()))) {
            return error(403, "ContentVersion access not permitted", "");
        }

        // Vérification des droits de publication
        LicenceService licenceService;
        try {
            licenceService = ThaleiaApplication.get().getLicenceService();
        } catch (DetailedException e) {
            logger.warn(e);
            return error(500, "Internal error.", "");
        }
        if (!licenceService.isLicencePermitsPublications(apiToken.getUser())) {
            return error(403, "Publication not permitted", "");
        }

        // Export de la CV
        ITransformTreatment<?> transformTreatment;
        try {
            // Quel type de CV ?
            if ("module_cannelle".equals(contentVersion.getContentType().getName())) {
                transformTreatment = TransformTreatmentFactory.get(TransformTreatmentFactory.CANNELLE_PUBLICATION_EXPORT);
            } else if ("Action".equals(contentVersion.getContentType().getName())) {
                return error(400, "Publication for this content type is not implemented.", "");
            } else {
                return error(400, "Publication for this content type is not implemented.", "");
            }

            // Export dans un fichier temporaire, les messages d'erreur seront localisés en anglais, mais on fixe la locale dans laquelle exporter le contenu.
            Locale javaLocaleToPublish = Locale.forLanguageTag(locale);
            File export = (File) transformTreatment.transform(contentVersion, apiToken.getUser(), javaLocaleToPublish);

            fr.solunea.thaleia.model.Locale localeToPublish = new LocaleDao(ThaleiaApplication.get().contextService.getContextSingleton()).findByName(locale);

            // On prépare le ScormPackageHelper, qui est dans le plugin publish.
            IPublishHelper scormPackageHelper = (IPublishHelper) ThaleiaApplication.get().getPluginService().getClassLoader().loadClass("fr.solunea.thaleia.plugins.publish.helpers.ScormPackageHelper").getConstructor().newInstance();
            PublicationService publicationService = new PublicationService(contextService, configuration);
            Publication publication = publicationService.publish(export, scormPackageHelper, true, true, apiToken.getUser(), contentVersion.getContent(), localeToPublish);

            PublicationDescription response = new PublicationDescription();
            response.publication_id = publication.getReference();
            response.publication_url = ThaleiaApplication.get().getPublishUrl() + "/" + publication.getReference();
            return response;

        } catch (Exception e) {
            logger.warn(e);
            return error(500, "Internal error.", "");
        }
    }

    public static class PublicationDescription {
        String publication_id;
        String publication_url;

    }
}
