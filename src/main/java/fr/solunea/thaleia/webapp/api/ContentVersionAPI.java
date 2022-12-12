package fr.solunea.thaleia.webapp.api;

import fr.solunea.thaleia.model.ApiToken;
import fr.solunea.thaleia.model.ContentVersion;
import fr.solunea.thaleia.model.dao.ContentDao;
import fr.solunea.thaleia.model.dao.ContentVersionDao;
import fr.solunea.thaleia.model.dao.ICayenneContextService;
import fr.solunea.thaleia.model.dao.LocaleDao;
import fr.solunea.thaleia.service.ContentService;
import fr.solunea.thaleia.service.utils.Configuration;
import fr.solunea.thaleia.utils.DetailedException;
import fr.solunea.thaleia.webapp.ThaleiaApplication;
import fr.solunea.thaleia.webapp.api.transform.CannellePreviewTreatment;
import fr.solunea.thaleia.webapp.api.transform.ITransformTreatment;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.commons.lang.time.DateFormatUtils;
import org.apache.log4j.Logger;
import org.wicketstuff.rest.annotations.MethodMapping;
import org.wicketstuff.rest.annotations.ResourcePath;
import org.wicketstuff.rest.annotations.parameters.HeaderParam;
import org.wicketstuff.rest.annotations.parameters.RequestParam;
import org.wicketstuff.rest.utils.http.HttpMethod;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@ResourcePath("/api/v1/contentVersion")
public class ContentVersionAPI extends ApiV1Service {

    private final static Logger logger = Logger.getLogger(TransformAPI.class);
    private final ICayenneContextService contextService;
    private final Configuration configuration;

    public ContentVersionAPI(ICayenneContextService contextService, Configuration configuration) {
        super(contextService, configuration);
        this.contextService = contextService;
        this.configuration = configuration;
    }

    @MethodMapping(value = "/{contentVersionId}/preview", httpMethod = HttpMethod.GET)
    @SuppressWarnings("unused")
    public Object preview(@HeaderParam("Authorization") String token, String contentVersionId
            , @RequestParam(value = "locale", required = false) String localeString) {
        ApiToken apiToken;
        try {
            apiToken = getToken(token, false);
        } catch (Exception e) {
            logger.warn(e);
            setResponseStatusCode(403);
            return null;
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
            return error(500, "Error during previsualisation production.", "");
        }
        ContentDao contentDao = new ContentDao(contextService.getContextSingleton());
        if (!contentService.canAccessContent(apiToken.getUser(), contentDao.getPK(contentVersion.getContent()))) {
            return error(403, "ContentVersion access not permitted", "");
        }

        // Récupération de la classe d'implémentation du traitement en fonction du type de contenu
        ITransformTreatment<?> transformTreatment;
        try {
            if (contentVersion.getContentType().getName().equals("module_cannelle")) {
                transformTreatment = new CannellePreviewTreatment();
            } else {
                return error(403, "Preview is not available for this content type.", "");
            }
        } catch (Exception e) {
            return error(500, "Error during previsualisation production.", "");
        }

        // Analyse de la locale du module à exporter
        LocaleDao localeDao = new LocaleDao(contextService.getContextSingleton());
        Locale moduleLocale;
        if (localeString == null || localeString.isEmpty()) {
            moduleLocale = localeDao.getJavaLocale(contentService.getVersionLocale(contentVersion));
        } else {
            moduleLocale = new java.util.Locale(localeString);
        }

        // Préparation de l'URL de prévisualisation de cette ContentVersion
        Object transformResult;
        try {
            transformResult = transformTreatment.transform(contentVersion, apiToken.getUser(), moduleLocale);
        } catch (DetailedException e) {
            logger.warn("Erreur de transformation : " + e);
            return error(500, "Error during previsualisation production.", "");
        }

        // Si la réponse du traitement est une String, on considère que c'est l'URL de prévisualisation.
        if (transformResult.getClass().isAssignableFrom(String.class)) {
            // Envoi de la réponse avec l'URL de prévisualisation
            ContentVersionPreview response = new ContentVersionPreview();
            response.content_identifier = contentVersion.getContentIdentifier();
            response.content_type_name = contentVersion.getContentType().getName();
            response.content_version_id = String.valueOf(contentVersionDao.getPK(contentVersion));
            DateFormat dateFormat = SimpleDateFormat.getDateInstance(DateFormat.FULL);
            response.last_update_date = DateFormatUtils.format(contentVersion.getLastUpdateDate(), "yyyyMMdd-HH:mm:ss");
            response.revision_number = contentVersion.getRevisionNumber();
            response.preview_url = (String) transformResult;
            response.locale = moduleLocale.toString();
            return response;
        } else {
            logger.warn("Type de retour de transformation inconnu :" + transformResult.getClass().getName());
            return error(500, "Error during previsualisation production.", "");
        }
    }

    @MethodMapping(value = "", httpMethod = HttpMethod.GET)
    @SuppressWarnings("unused")
    public Object list(@HeaderParam("Authorization") String token,
                       @RequestParam(value = "modules_only", required = false) Boolean modulesOnly,
                       @RequestParam(value = "content_type_name", required = false) String contentTypeName) {
        logger.debug("Demande de liste des contentVersions.");

        ApiToken apiToken;
        try {
            apiToken = getToken(token, false);
        } catch (Exception e) {
            logger.warn(e);
            setResponseStatusCode(403);
            return null;
        }

        ContentService contentService;
        try {
            contentService = ThaleiaApplication.get().getContentService();
        } catch (Exception e) {
            logger.warn(e);
            return error(500, "Error during previsualisation production.", "");
        }

        List<ContentVersionDescription> result = new ArrayList<>();
        try {
            ContentService contentVersionService = new ContentService(contextService, configuration);
            ContentVersionDao contentVersionDao = new ContentVersionDao(contextService.getContextSingleton());
            List<ContentVersion> contentVersions;
            if (modulesOnly != null && modulesOnly) {
                contentVersions = contentVersionService.getModulesVersionsWhereAuthor(apiToken.getUser());
            } else {
                contentVersions = contentVersionService.getContentVersionsWhereAuthor(apiToken.getUser());
            }
            for (ContentVersion contentVersion : contentVersions) {
                // Si demandé, on filtre sur le contentTypeName
                if (contentTypeName == null || contentTypeName.isEmpty() || contentTypeName.equals(contentVersion.getContentType().getName())) {
                    ContentVersionDescription description = new ContentVersionDescription();
                    description.content_identifier = contentVersion.getContentIdentifier();
                    description.content_type_name = contentVersion.getContentType().getName();
                    description.content_version_id = String.valueOf(contentVersionDao.getPK(contentVersion));
                    DateFormat dateFormat = SimpleDateFormat.getDateInstance(DateFormat.FULL);
                    description.last_update_date = DateFormatUtils.format(contentVersion.getLastUpdateDate(), "yyyyMMdd-HH:mm:ss");
                    description.revision_number = contentVersion.getRevisionNumber();
                    description.setExistsInLocales(contentService.getVersionLocales(contentVersion));
                    result.add(description);
                }
            }
            setResponseStatusCode(200);
            return result;
        } catch (Exception e) {
            logger.warn(e);
            logger.debug(ExceptionUtils.getFullStackTrace(e));
            setResponseStatusCode(500);
            return new ArrayList<>();
        }
    }

    private static class ContentVersionDescription {
        String content_version_id;
        String content_identifier;
        String content_type_name;
        String last_update_date;
        int revision_number;
        String exists_in_locales;

        void setExistsInLocales(List<fr.solunea.thaleia.model.Locale> locales) {
            String result = "";
            for (fr.solunea.thaleia.model.Locale locale : locales) {
                result = result + locale.getName() + ",";
            }
            if (result.endsWith(",")) {
                result = result.substring(0, result.length() - 1);
            }
            exists_in_locales = result;
        }
    }

    private static class ContentVersionPreview {
        String content_version_id;
        String content_identifier;
        String content_type_name;
        String last_update_date;
        int revision_number;
        String preview_url;
        String locale;
    }
}
