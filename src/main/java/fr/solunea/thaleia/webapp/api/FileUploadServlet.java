package fr.solunea.thaleia.webapp.api;

import fr.solunea.thaleia.model.ApiToken;
import fr.solunea.thaleia.model.FileUpload;
import fr.solunea.thaleia.model.dao.ApiTokenDao;
import fr.solunea.thaleia.model.dao.FileUploadDao;
import fr.solunea.thaleia.service.utils.Configuration;
import fr.solunea.thaleia.utils.DetailedException;
import fr.solunea.thaleia.webapp.ThaleiaApplication;
import fr.solunea.thaleia.webapp.fileupload.FileUploadTreatment;
import fr.solunea.thaleia.webapp.fileupload.IFileUploadTreatment;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.http.HttpHeaders;
import org.apache.log4j.Logger;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.List;

import static fr.solunea.thaleia.webapp.api.ApiV1Service.TOKEN_DURATION_IN_SECONDS;

public class FileUploadServlet extends HttpServlet {

    private final static Logger logger = Logger.getLogger(FileUploadServlet.class);

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) {
        run(req, resp);
    }

    private void run(HttpServletRequest request, HttpServletResponse response) {

        logger.debug("Traitement d'une demande d'upload d'un fichier...");

        // Vérification du token d'identification
        ApiTokenDao apiTokenDao = ThaleiaApplication.get().getApiTokenDao();
        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        ApiToken token = apiTokenDao.getTokenIfValid(authorization, TOKEN_DURATION_IN_SECONDS);
        if (token == null) {
            try {
                response.sendError(403);
                return;
            } catch (IOException e) {
                logger.warn("Impossible de renvoyer une erreur d'identification.", e);
            }
        }

        // Récupération du FileUpload pour cet envoi (dont vérification de l'expiration)
        FileUpload fileUpload;
        try {
            String uploadId = request.getParameter(Configuration.UPLOAD_FILES_REFERENCE_PARAMETER);
            if (uploadId == null || uploadId.isEmpty()) {
                try {
                    response.sendError(400, "Missing parameter.");
                    return;
                } catch (IOException e) {
                    logger.warn("Impossible de renvoyer une erreur de traitement.", e);
                }
            }
            fileUpload = ThaleiaApplication.get().getFileUploadService().getFileUploadIfValid(uploadId,
                    ThaleiaApplication.get().contextService.getContextSingleton());
            if (fileUpload == null) {
                throw new DetailedException("Pas de FileUpload valide trouvé pour l'id " + uploadId);
            }
        } catch (DetailedException e) {
            logger.warn("Erreur de récupération du FileUpload lors d'une requête d'envoi de fichier.", e);
            sendError(response, "FileUpload retrieval error.");
            return;
        }

        // Chargement de la classe de traitement de ce fileUpload
        IFileUploadTreatment treatment;
        try {
            treatment = FileUploadTreatment.get(fileUpload.getTreatmentClass());
        } catch (Exception e) {
            logger.warn("Erreur de récupération de la classe de traitement lors d'une requête d'envoi de fichier.", e);
            sendError(response, "FileUpload treatment error.");
            return;
        }

        // Enregistrement du contenu de l'import dans un fichier temporaire
        File tempBinaryFile = null;
        try {
            if (ServletFileUpload.isMultipartContent(request)) {
                logger.debug("On interprète le corps de la requête comme une Multipart-Form-Data, et on récupère uniquement le premier fichier.");
                int thresholdSize = 1024 * 1024 * 3;  // 3 MB
                long maxUploadSizeInKilo = ThaleiaApplication.get().getConfiguration().getDatabaseParameterValueAsLong(Configuration.MAX_UPLOAD_SIZE_PARAM, 10000L);
//                int maxFileSize = 1024 * 1024 * 300; // 300 MB
//                int maxRequestSize = 1024 * 1024 * 350; // 350 MB
                int maxFileSize = Math.toIntExact(maxUploadSizeInKilo * 1024);
                int maxRequestSize = Math.toIntExact(maxUploadSizeInKilo * 1024);

                DiskFileItemFactory factory = new DiskFileItemFactory();
                factory.setSizeThreshold(thresholdSize);
                factory.setRepository(ThaleiaApplication.get().getTempFilesService().getTempDir());

                ServletFileUpload upload = new ServletFileUpload(factory);
                upload.setFileSizeMax(maxFileSize);
                upload.setSizeMax(maxRequestSize);

                @SuppressWarnings("unchecked")
                List<FileItem> formItems = upload.parseRequest(request);
                Iterator<FileItem> iter = formItems.iterator();

                boolean firstFileFound = false;

                if (!iter.hasNext()) {
                    throw new Exception("Pas de fichier dans le corps de la requête !");
                } else {
                    while (iter.hasNext()) {
                        FileItem item = iter.next();
                        if (!item.isFormField() && !firstFileFound) {
                            String fileName = new File(item.getName()).getName();
                            tempBinaryFile = ThaleiaApplication.get().getTempFilesService().getTempFile(fileName);
                            item.write(tempBinaryFile);
                            firstFileFound = true;
                        }
                    }
                }

            } else {
                logger.debug(" On prend tel quel le corps de la requête comme binaire à enregister.");
                tempBinaryFile = ThaleiaApplication.get().getTempFilesService().getTempFile();
                try (InputStream body = request.getInputStream(); FileOutputStream fos =
                        new FileOutputStream(tempBinaryFile)) {
                    IOUtils.copy(body, fos);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            logger.warn("Erreur de récupération du binaire lors d'une requête d'envoi de fichier.", e);
            sendError(response, "FileUpload storage error.");
            return;
        }

        try {
            // Traitement
            logger.debug("Appel du traitement par " + treatment.getClass().getName());
            treatment.run(fileUpload, tempBinaryFile, response);

            // Suppression de l'objet FileUpload
            logger.debug("Suppression de l'objet FileUpload...");
            FileUploadDao fileUploadDao = new FileUploadDao(ThaleiaApplication.get().contextService.getContextSingleton());
            fileUploadDao.delete(fileUpload);

        } catch (Exception e) {
            logger.warn("Erreur de traitement du fichier reçu lors d'une requête d'envoi de fichier.", e);
            logger.debug(ExceptionUtils.getFullStackTrace(e));
            sendError(response, "FileUpload treatment error : " + e.getMessage());
        }
    }

    // On remplace le response.sendError par une version où on répond sans le formattage HTML des erreurs Tomcat.
    private void sendError(HttpServletResponse response, String message) {
        response.setStatus(500);
        response.setContentType("text/plain;charset=UTF-8");
        try (ServletOutputStream sout = response.getOutputStream()) {
            sout.println(message);
        } catch (IOException e) {
            logger.warn("Impossible d'envoyer un message d'erreur : ", e);
        }
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) {
        run(req, resp);
    }
}
