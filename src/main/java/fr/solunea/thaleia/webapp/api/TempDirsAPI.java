package fr.solunea.thaleia.webapp.api;

import com.google.gson.JsonObject;
import fr.solunea.thaleia.model.ApiToken;
import fr.solunea.thaleia.model.FileUpload;
import fr.solunea.thaleia.model.TempDir;
import fr.solunea.thaleia.model.TempDirFile;
import fr.solunea.thaleia.model.dao.FileUploadDao;
import fr.solunea.thaleia.model.dao.ICayenneContextService;
import fr.solunea.thaleia.model.dao.TempDirDao;
import fr.solunea.thaleia.model.dao.TempDirFileDao;
import fr.solunea.thaleia.service.FileUploadService;
import fr.solunea.thaleia.service.TempDirService;
import fr.solunea.thaleia.service.TempFilesService;
import fr.solunea.thaleia.service.utils.Configuration;
import fr.solunea.thaleia.service.utils.Unique;
import fr.solunea.thaleia.service.utils.ZipUtils;
import fr.solunea.thaleia.utils.DetailedException;
import fr.solunea.thaleia.webapp.ThaleiaApplication;
import fr.solunea.thaleia.webapp.fileupload.TempDirFileImportTreatment;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.log4j.Logger;
import org.wicketstuff.rest.annotations.MethodMapping;
import org.wicketstuff.rest.annotations.ResourcePath;
import org.wicketstuff.rest.annotations.parameters.HeaderParam;
import org.wicketstuff.rest.annotations.parameters.RequestBody;
import org.wicketstuff.rest.annotations.parameters.RequestParam;
import org.wicketstuff.rest.utils.http.HttpMethod;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

@ResourcePath("/api/v1/tempdirs")
public class TempDirsAPI extends ApiV1Service {

    public final static String API_END_POINT = "/api/v1/tempdirs";

    private final static Logger logger = Logger.getLogger(TempDirsAPI.class);
    private final FileUploadService fileUploadService;
    private final TempDirService tempDirService;
    private final ICayenneContextService contextService;
    private TempFilesService tempFilesService;

    public TempDirsAPI(ICayenneContextService contextService, Configuration configuration) {
        super(contextService, configuration);
        fileUploadService = new FileUploadService(configuration);
        tempDirService = new TempDirService(contextService, configuration);
        this.contextService = contextService;
        try {
            tempFilesService = new TempFilesService(configuration.getTempFilesDir(), ThaleiaApplication.getScheduledExecutorService(), ThaleiaApplication.getExecutorFutures());
        } catch (DetailedException e) {
            logger.warn("Impossible d'initialiser le service des Publications : " + e);
        }
    }

    @MethodMapping(value = "/{tempDirId}/files", httpMethod = HttpMethod.POST)
    @SuppressWarnings("unused")
    public JsonObject addFile(@HeaderParam("Authorization") String token, String tempDirId, @RequestBody FileUploadRequest fileUploadRequest) {
        TempDirDao tempDirDao = new TempDirDao(contextService.getContextSingleton());
        TempDir tempDir = tempDirDao.findByPublicId(tempDirId);
        if (isThereARightsProblemForTempDir(token, tempDir, tempDirId)) return null;

        // On fabrique l'URL d'upload à envoyer en réponse
        FileUpload fileUpload;
        try {
            // Création de l'objet de file_upload
            ApiToken apiToken = getApiTokenAndCheckLicence(token);
            fileUpload = fileUploadService.createFileUpload(apiToken.getUser(), fileUploadRequest.filename,
                    TempDirFileImportTreatment.class.getName());
            fileUpload.setParameters("tempDirId=" + tempDirId);
            FileUploadDao fileUploadDao = new FileUploadDao(contextService.getContextSingleton());
            fileUploadDao.save(fileUpload);
        } catch (Exception e) {
            logger.warn(e);
            logger.debug(ExceptionUtils.getFullStackTrace(e));
            return error(500, "Plugin upload request error", "Internal error during file upload request registration.");
        }

        try {
            // On renvoie une réponse de type :
            // HTTP/1.1 201 OK
            // Location: https://server/application/api/v1/upload/fileupload?upload_id=xa298sdSZThSDf
            // Content-Length: 0
            setResponseStatusCode(201);
            getCurrentWebResponse().setHeader("Location", fileUploadService.getFileUploadUrl(fileUpload));
            getCurrentWebResponse().setHeader("Content-Length", "0");
            return null;
        } catch (Exception e) {
            logger.warn(ExceptionUtils.getStackTrace(e));
            return error(500, "Plugin upload request error", "Internal error during upload URL generation.");
        }
    }

    @MethodMapping(value = "/{tempDirId}", httpMethod = HttpMethod.DELETE)
    @SuppressWarnings("unused")
    public JsonObject delete(@HeaderParam("Authorization") String token, String tempDirId) {
        TempDirDao tempDirDao = new TempDirDao(contextService.getContextSingleton());
        TempDir tempDir = tempDirDao.findByPublicId(tempDirId);
        if (isThereARightsProblemForTempDir(token, tempDir, tempDirId)) return null;

        // On supprime
        try {
            tempDirDao.delete(tempDir);
        } catch (DetailedException e) {
            logger.warn(e);
            logger.debug(ExceptionUtils.getFullStackTrace(e));
            setResponseStatusCode(500);
            return null;
        }

        setResponseStatusCode(200);
        return null;
    }

    @MethodMapping(value = "/{tempDirId}/files/{fileId}", httpMethod = HttpMethod.GET)
    @SuppressWarnings("unused")
    public FileDetails fileDetails(@HeaderParam("Authorization") String token, String tempDirId, String fileId) {
        TempDirFile tempDirFile = getCheckedTempDirFile(token, tempDirId, fileId);
        if (tempDirFile == null) return null;

        // On présente les détails du fichier
        FileDetails fileDetails = new FileDetails();
        fileDetails.id = tempDirFile.getPublicId();
        fileDetails.name = tempDirFile.getFileName();
        fileDetails.size = tempDirFile.getSize().toString();

        setResponseStatusCode(200);
        return fileDetails;
    }

    @MethodMapping(value = "/{tempDirId}/files/{fileId}", httpMethod = HttpMethod.DELETE)
    @SuppressWarnings("unused")
    public JsonObject deleteFile(@HeaderParam("Authorization") String token, String tempDirId, String fileId) {
        TempDirFile tempDirFile = getCheckedTempDirFile(token, tempDirId, fileId);
        if (tempDirFile == null) return null;

        // On supprime le fichier
        try {
            File file = tempDirService.getFile(tempDirFile);
            FileUtils.deleteQuietly(file);
            TempDirFileDao tempDirFileDao = new TempDirFileDao(contextService.getContextSingleton());
            tempDirFileDao.delete(tempDirFile);
        } catch (DetailedException e) {
            logger.warn("Le TempDirFile au fileId = '" + fileId + "' ne peut pas être supprimé.", e);
            setResponseStatusCode(500);
            return null;
        }

        setResponseStatusCode(200);
        return null;
    }

    /**
     * @return le TempDirFile demandé, en ayant vérifié les droits d'accès et l'existence des objets concernés. Si erreur, alors le code de retour est placé dans la réponse, et on renvoie null.
     */
    private TempDirFile getCheckedTempDirFile(@HeaderParam("Authorization") String token, String tempDirId, String fileId) {
        TempDirDao tempDirDao = new TempDirDao(contextService.getContextSingleton());
        TempDir tempDir = tempDirDao.findByPublicId(tempDirId);
        if (isThereARightsProblemForTempDir(token, tempDir, tempDirId)) return null;

        // Ce file existe dans ce tempDir ?
        TempDirFileDao tempDirFileDao = new TempDirFileDao(contextService.getContextSingleton());
        TempDirFile tempDirFile = tempDirFileDao.findByPublicId(fileId);
        if (tempDirFile == null) {
            logger.debug("Le TempDirFile au fileId = '" + fileId + "' n'existe pas.");
            setResponseStatusCode(404);
            return null;
        }
        if (!tempDirFile.getTempDir().getPublicId().equals(tempDirId)) {
            logger.debug("Le TempDirFile au fileId = '" + fileId + "' n'est pas associé à ce tempDirFile.");
            setResponseStatusCode(404);
            return null;
        }
        return tempDirFile;
    }

    /**
     * Vérifie si l'utilisateur est correctement identifié par le token, que le tempDir existe, et qu'il a les droits d'accès dessus. Le traitement peut alors continuer. Sinon, le code de réponse a été fixé dans la réponse.
     */
    private boolean isThereARightsProblemForTempDir(@HeaderParam("Authorization") String token, TempDir tempDir, String tempDirId) {
        ApiToken apiToken;
        try {
            apiToken = getApiTokenAndCheckLicence(token);
        } catch (Exception e) {
            logger.warn(e);
            setResponseStatusCode(403);
            return true;
        }

        // Ce tempDir existe ?
        if (tempDir == null) {
            logger.debug("Le TempDir au publicId = '" + tempDirId + "' n'existe pas.");
            setResponseStatusCode(404);
            return true;
        }

        // On vérifie que l'utilisateur a bien le droit d'accès à ce tempDir
        if (tempDir.getOwner() == null || !tempDir.getOwner().getLogin().equals(apiToken.getUser().getLogin())) {
            logger.debug("Le TempDir publicId = '" + tempDirId + "' n'est pas visible pour '" + apiToken.getUser().getLogin() + "'.");
            setResponseStatusCode(403);
            return true;
        }
        return false;
    }

    @MethodMapping(value = "/{tempDirId}/export", httpMethod = HttpMethod.GET)
    @SuppressWarnings("unused")
    public JsonObject export(@HeaderParam("Authorization") String token, String tempDirId, @RequestParam("format") String format) {
        TempDirDao tempDirDao = new TempDirDao(contextService.getContextSingleton());
        TempDir tempDir = tempDirDao.findByPublicId(tempDirId);
        if (isThereARightsProblemForTempDir(token, tempDir, tempDirId)) return null;

        // On vérifie le format demandé
        if (format == null || !format.equals("zip")) {
            setResponseStatusCode(400);
            return null;
        }

        // On fabrique le Zip dans un fichier temporaire
        File zip;
        try {
            zip = tempFilesService.getTempFile(Calendar.getInstance().getTimeInMillis() + ".zip");
            ZipUtils.toZip(tempDirService.getTempDirDirectory(tempDir).getAbsolutePath(), zip.getAbsolutePath());
        } catch (DetailedException e) {
            logger.warn("Impossible de préparer un export du tempDir.", e);
            setResponseStatusCode(500);
            return null;
        }

        // On envoie le ZIP
        return sendBinaryToResponse(zip);
    }

    @MethodMapping(value = "/{tempDirId}", httpMethod = HttpMethod.GET)
    @SuppressWarnings("unused")
    public TempDirDetails details(@HeaderParam("Authorization") String token, String tempDirId) {
        TempDirDao tempDirDao = new TempDirDao(contextService.getContextSingleton());
        TempDir tempDir = tempDirDao.findByPublicId(tempDirId);
        if (isThereARightsProblemForTempDir(token, tempDir, tempDirId)) return null;

        // On présente les détails
        TempDirDetails details = new TempDirDetails();
        details.id = tempDir.getPublicId();
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        details.creationDate = format.format(tempDir.getCreationDate());
        for (TempDirFile file : tempDir.getFiles()) {
            FileDetails fileDetails = new FileDetails();
            fileDetails.id = file.getPublicId();
            fileDetails.name = file.getFileName();
            fileDetails.size = file.getSize().toString();
            details.files.add(fileDetails);
        }

        setResponseStatusCode(200);
        return details;
    }

    @MethodMapping(value = "", httpMethod = HttpMethod.GET)
    @SuppressWarnings("unused")
    public List<TempDirId> list(@HeaderParam("Authorization") String token) {
        logger.debug("Demande de liste des répertoires temporaires.");

        ApiToken apiToken;
        try {
            apiToken = getApiTokenAndCheckLicence(token);
        } catch (Exception e) {
            logger.warn(e);
            setResponseStatusCode(403);
            return null;
        }

        List<TempDirId> result = new ArrayList<>();
        try {
            TempDirDao tempDirDao = new TempDirDao(contextService.getContextSingleton());
            List<TempDir> tempDirs = tempDirDao.findByOwner(apiToken.getUser());
            for (TempDir tempDir : tempDirs) {
                TempDirId tempDirId = new TempDirId();
                tempDirId.id = tempDir.getPublicId();
                result.add(tempDirId);
            }
            setResponseStatusCode(200);
            // On renvoie une réponse de type :
            // {
            //   [
            //   "id":"AE45-FRGND-5762-DF532",
            //   "id":"TFA34-ZER56-21563-DFEZR3",
            //        ...
            //   ]
            //}
            return result;
        } catch (Exception e) {
            logger.warn(e);
            logger.debug(ExceptionUtils.getFullStackTrace(e));
            setResponseStatusCode(500);
            return new ArrayList<>();
        }
    }

    @MethodMapping(value = "", httpMethod = HttpMethod.POST)
    @SuppressWarnings("unused")
    public JsonObject create(@HeaderParam("Authorization") String token) {
        logger.debug("Demande de création de répartoire temporaire.");

        ApiToken apiToken;
        try {
            // Vérification du token Thaleia : le token est bien associé à un utilisateur
            apiToken = getApiTokenAndCheckLicence(token);
        } catch (Exception e) {
            logger.warn(e);
            setResponseStatusCode(403);
            return null;
        }

        TempDir tempDir;
        try {
            // Création de l'objet de temp_dir
            TempDirDao tempDirDao = new TempDirDao(contextService.getContextSingleton());
            tempDir = tempDirDao.get();
            tempDir.setOwner(apiToken.getUser());
            tempDir.setCreationDate(Calendar.getInstance().getTime());
            tempDir.setPublicId(Unique.getUniqueString(32));
            File dir = tempFilesService.getTempDir();
            tempDir.setPath(dir.getName());
            tempDirDao.save(tempDir);
        } catch (DetailedException e) {
            logger.warn(e);
            logger.debug(ExceptionUtils.getFullStackTrace(e));
            return error(500, "TempDir creation error.", "Internal error during tempDir creation.");
        }

        try {
            // On renvoie une réponse de type :
            // HTTP/1.1 201 OK
            // Location: https://server/application/api/v1/tempdirs/xa298sdSZThSDf
            // Content-Length: 0
            setResponseStatusCode(201);
            getCurrentWebResponse().setHeader("Location", ThaleiaApplication.get().getApplicationRootUrl() + API_END_POINT + "/" + tempDir.getPublicId());
            getCurrentWebResponse().setHeader("Content-Length", "0");
            return null;
        } catch (Exception e) {
            logger.warn(ExceptionUtils.getStackTrace(e));
            return error(500, "TempDir creation error.", "Internal error during tempDir URL generation.");
        }
    }

    static class TempDirId {
        String id;
    }

    static class TempDirDetails {
        String id;
        String creationDate;
        List<FileDetails> files = new ArrayList<>();
    }

    static class FileDetails {
        String id;
        String name;
        String size;
    }

    static class FileUploadRequest {
        String filename;

        FileUploadRequest(String filename) {
            this.filename = filename;
        }
    }
}
