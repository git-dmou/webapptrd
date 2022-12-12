package fr.solunea.thaleia.webapp.api;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import fr.solunea.thaleia.model.ApiToken;
import fr.solunea.thaleia.model.ContentVersion;
import fr.solunea.thaleia.model.dao.ContentVersionDao;
import fr.solunea.thaleia.model.dao.ICayenneContextService;
import fr.solunea.thaleia.service.ContentService;
import fr.solunea.thaleia.service.TempFilesService;
import fr.solunea.thaleia.service.utils.Configuration;
import fr.solunea.thaleia.utils.DetailedException;
import fr.solunea.thaleia.webapp.ThaleiaApplication;
import fr.solunea.thaleia.webapp.api.transform.ITransformTreatment;
import fr.solunea.thaleia.webapp.api.transform.TransformTreatmentFactory;
import fr.solunea.thaleia.webapp.utils.Downloader;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.http.HttpHeaders;
import org.apache.log4j.Logger;
import org.apache.wicket.protocol.http.mock.MockHttpServletRequest;
import org.wicketstuff.rest.annotations.MethodMapping;
import org.wicketstuff.rest.annotations.ResourcePath;
import org.wicketstuff.rest.annotations.parameters.HeaderParam;
import org.wicketstuff.rest.annotations.parameters.RequestParam;
import org.wicketstuff.rest.utils.http.HttpMethod;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@ResourcePath("/api/v1/transform")
public class TransformAPI extends ApiV1Service {

    private final static Logger logger = Logger.getLogger(TransformAPI.class);

    private TempFilesService tempFilesService;

    public TransformAPI(ICayenneContextService contextService, Configuration configuration) {
        super(contextService, configuration);
        try {
            tempFilesService = new TempFilesService(configuration.getTempFilesDir(), ThaleiaApplication.getScheduledExecutorService(), ThaleiaApplication.getExecutorFutures());
        } catch (DetailedException e) {
            logger.warn("Impossible d'instancier le tempFilesService.", e);
        }
    }

    /**
     * @param token         le token d'identification pour le compte qui va effectuer ce traitement.
     * @param type          le type de traitement : {@link fr.solunea.thaleia.webapp.api.transform.TransformTreatmentFactory}
     * @param localeString  : Optionnel. Localisation des éventuels messages d’erreur dans la réponse à cet appel.
     * @param localFilePath : Uniquement pour les tests unitaires Java, afin d'appeler cette méthode sans envoyer une requête POST.
     *                      Ce paramètre n'est pas récupéré dans les requêtes POST, mais permet de faire référence à un fichier source local.
     */
    @MethodMapping(value = "", httpMethod = HttpMethod.POST)
    @SuppressWarnings("unused")
    public Object transform(@HeaderParam("Authorization") String token, @RequestParam(value = "type") String type
            , @RequestParam(value = "locale", required = false) String localeString, @RequestParam(value = "", required = false) String localFilePath) {
        logger.debug("Appel reçu pour à l'API : Transform.");

        ApiToken apiToken;
        try {
            // Vérification du token Thaleia
            apiToken = getToken(token, false);
        } catch (Exception e) {
            logger.warn(e);
            setResponseStatusCode(403);
            return new JsonObject();
        }

        // Analyse du type de transformation demandée
        if (type == null || type.isEmpty()) {
            return error(400, "Missing parameter", "");
        }

        // Récupération de la classe d'implémentation du traitement
        ITransformTreatment<?> transformTreatment;
        try {
            transformTreatment = TransformTreatmentFactory.get(type);
        } catch (DetailedException e) {
            logger.debug("Type " + type + " invalide.");
            return error(400, "Wrong type", type + " type is not valid.");
        }

        // Analyse de la locale demandée
        Locale locale = Locale.ENGLISH;
        if (localeString != null && !localeString.isEmpty()) {
            locale = Locale.forLanguageTag(localeString);
            logger.debug("Locale demandée : '" + localeString + "' -> interprétée : " + locale);
        }

        // Enregistrement du contenu de l'import dans une fichier temporaire
        HttpServletRequest request = (HttpServletRequest) (getCurrentWebRequest().getContainerRequest());
        File tempBinaryFile;
        // Les MockHttpServletRequest sont produites par les classes de test pour simuler des appels à l'API
        if (request != null && !MockHttpServletRequest.class.isAssignableFrom(request.getClass())) {
            // Dans le cas où ce n'est pas un appel de test, on recherche le binaire dans la requête
            try {
                tempBinaryFile = getInputBinary(request);
            } catch (Exception e) {
                logger.warn("Problème de récupération du fichier à traiter : ", e);
                return error(400, "Input file retrieval error.", "");
            }
            if (tempBinaryFile == null) {
                logger.warn("Pas de binaire reçu dans la requête.");
                return error(400, "No input file retrieved.", "");
            }
        } else if (request != null && localFilePath != null && !localFilePath.isEmpty()) {
            // Ce n'est pas une requête HTTP POST, mais un appel direct à la fonction de l'API par un test unitaire
            tempBinaryFile = new File(localFilePath);
        } else {
            logger.warn("Pas de binaire reçu dans la fonction.");
            return error(400, "No input file retrieved.", "");
        }

        // Préparation du résultat
        Object result;
        try {
            logger.debug("Traitement d'une requête de transformation de type '" + type + "'");
            result = transformTreatment.transform(tempBinaryFile, apiToken.getUser(), locale);
        } catch (Exception e) {
            logger.warn("Erreur de génération du contenu de la réponse lors d'une requête de traitement.", e);
            logger.debug(ExceptionUtils.getFullStackTrace(e));
            return error(500, "Response sending error", e.getMessage());
        }

        // Envoi du résultat
        if (result.getClass().isAssignableFrom(File.class)) {
            return sendBinaryToResponse((File) result);
        } else {
            ContentVersion contentVersion = (ContentVersion) result;
            return new CannelleImportResult(contentVersion);
        }
    }

    private File getInputBinary(HttpServletRequest request) throws Exception {
        File result = null;
        String contentType = request.getHeader(HttpHeaders.CONTENT_TYPE);

        if (contentType == null || contentType.isEmpty()) {
            throw new Exception("No " + HttpHeaders.CONTENT_TYPE + " header.");
        }

        if (contentType.startsWith("multipart/form-data")) {
            try {
                logger.debug("Enregistrement du fichier reçu dans un fichier temporaire...");
                @SuppressWarnings("unchecked")
                List<FileItem> items = new ServletFileUpload(new DiskFileItemFactory()).parseRequest(request);
                for (FileItem item : items) {
                    if (item.isFormField()) {
                        logger.debug("On ignore l'item " + item.getString());
                    } else {
                        logger.debug("On traite l'item " + item.getName());
                        String fileName = FilenameUtils.getName(item.getName());
                        logger.debug("Nom de fichier = " + fileName);
                        String fieldName = item.getFieldName();
                        result = ThaleiaApplication.get().getTempFilesService().getTempFile();
                        try (InputStream body = item.getInputStream(); FileOutputStream fos =
                                new FileOutputStream(result)) {
                            IOUtils.copy(body, fos);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
                // Attention : comme on parse tous les items de la requête multipart, on peut avoir 0 ou plusieurs fichiers.
                // Si 0, alors result sera null
                // Si plusieurs, alors result contiendra uniquement le dernier fichier de la requête.
                return result;
            } catch (Exception e) {
                throw new Exception("Erreur de récupération du binaire lors d'une requête de traitement.", e);
            }

        } else if (contentType.equals("application/json")) {
            // On recherche un corps de requête du type :
            // {
            //	"type": "http_download",
            //	"url": " https://{instance}/api/v1/tempdirs/[tempdirId]/export?format=zip",
            //	"headers": [
            //"Authorization": "[token d’identification]"
            // 	]
            //}
            HttpDownloadRequest httpDownloadRequest;
            try {
                String body = IOUtils.toString(request.getReader());
                GsonBuilder gsonBuilder = new GsonBuilder();
                httpDownloadRequest = gsonBuilder.create().fromJson(body, HttpDownloadRequest.class);
            } catch (IOException e) {
                throw new Exception("Impossible de lire le corps de la requête", e);
            } catch (JsonSyntaxException e) {
                throw new Exception("Le corps de la requête n'est pas au format attendu.", e);
            }

            return downloadFile(httpDownloadRequest);
        }

        return null;
    }

    private File downloadFile(HttpDownloadRequest httpDownloadRequest) throws Exception {
        if (httpDownloadRequest.type == null || httpDownloadRequest.type.isEmpty()) {
            throw new Exception("Pas de type de téléchargement.");
        }
        if (!httpDownloadRequest.type.equals("http_download")) {
            throw new Exception("Le type de téléchargement '" + httpDownloadRequest.type + "' n'est pas reconnu.");
        }

        if (httpDownloadRequest.url == null || httpDownloadRequest.url.isEmpty()) {
            throw new Exception("Pas d'URL de téléchargement.");
        }

        // Préparation des en-têtes
        Map<String, String> headers = new HashMap<>();
        if (httpDownloadRequest.headers != null && !httpDownloadRequest.headers.isEmpty()) {
            for (Header header : httpDownloadRequest.headers) {
                headers.put(header.name, header.value);
            }
        }

        // Téléchargement
        File result = tempFilesService.getTempFile();
        try {
            Downloader.download(new URL(httpDownloadRequest.url), result, headers);
        } catch (Exception e) {
            throw new Exception("Erreur de téléchargement.", e);
        }
        return result;
    }

    static class HttpDownloadRequest {
        String type;
        String url;
        List<Header> headers;
    }

    static class Header {
        String name;
        String value;
    }


    static class TransformResult {
        int code;
        String message;
    }

    public static class CannelleImportResult extends TransformResult {
        private final String content_version_id;
        private final String locale;

        public CannelleImportResult(ContentVersion contentVersion) {
            code = 200;
            message = "Import successful";
            ContentVersionDao contentVersionDao = ThaleiaApplication.get().getContentVersionDao();
            content_version_id = String.valueOf(contentVersionDao.getPK(contentVersion));
            ContentService contentService = ThaleiaApplication.get().getContentService();
            locale = contentService.getVersionLocale(contentVersion).getName();
        }

        public String getContentVersionId() {
            return content_version_id;
        }

        public String getLocale() {
            return locale;
        }
    }
}