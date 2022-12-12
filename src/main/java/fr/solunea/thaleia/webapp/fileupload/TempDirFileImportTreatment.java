package fr.solunea.thaleia.webapp.fileupload;

import fr.solunea.thaleia.model.FileUpload;
import fr.solunea.thaleia.model.TempDir;
import fr.solunea.thaleia.model.TempDirFile;
import fr.solunea.thaleia.model.dao.FileUploadDao;
import fr.solunea.thaleia.model.dao.TempDirDao;
import fr.solunea.thaleia.model.dao.TempDirFileDao;
import fr.solunea.thaleia.service.TempDirService;
import fr.solunea.thaleia.webapp.ThaleiaApplication;
import fr.solunea.thaleia.webapp.api.TempDirsAPI;
import org.apache.cayenne.ObjectContext;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.log4j.Logger;

import javax.servlet.http.HttpServletResponse;
import java.io.File;

public class TempDirFileImportTreatment implements IFileUploadTreatment {

    private final static Logger logger = Logger.getLogger(TempDirFileImportTreatment.class);

    @Override
    public void run(FileUpload fileUpload, File tempBinary, HttpServletResponse response) throws Exception {
        logger.debug("Traitement de l'import d'un fichier pour un répertoire temporaire depuis un fichier uploadé...");
        // Création de l'objet hors du try, pour le supprimer en cas d'erreur d'installation.
        TempDir tempDir;
        TempDirFile tempDirFile = null;
        // On prépare un contexte fils  de l'objet passé en paramètre, pour isoler les modifications et les commiter en cas de réussite.
        ObjectContext context = ThaleiaApplication.get().contextService.getChildContext(fileUpload.getObjectContext());
        TempDirFileDao tempDirFileDao = new TempDirFileDao(context);
        TempDirDao tempDirDao = new TempDirDao(context);
        FileUpload localFileUpload = context.localObject(fileUpload);
        try {
            try {
                String parameters = localFileUpload.getParameters();
                if (parameters == null || parameters.isEmpty()) {
                    throw new Exception("Pas de tempDirId défini dans les paramètres du fileUpload.");
                }
                if (parameters.length() <= "tempDirId=".length()) {
                    throw new Exception("tempDirId défini dans les paramètres du fileUpload '" + parameters + "' incorrect.");
                }
                String tempDirId = parameters.substring("tempDirId=".length());
                tempDir = tempDirDao.findByPublicId(tempDirId);
                if (tempDir == null) {
                    throw new Exception("tempDirId défini dans les paramètres du fileUpload '" + parameters + "' non trouvé.");
                }
            } catch (Exception e) {
                throw new Exception("Impossible d'obtenir le tempsDir concerné par cet upload.", e);
            }

            TempDirService tempDirService = ThaleiaApplication.get().getTempDirService();
            tempDirFile = tempDirService.addOrReplaceFile(tempDir, localFileUpload.getFilename(), tempBinary, context);

            if (tempDirFile == null) {
                throw new Exception("Impossible d'ajouter le fichier au tempDir.");
            }

            tempDirFileDao.save(tempDirFile, false);

            // Nettoyage de l'objet d'upload
            FileUploadDao fileUploadDao = new FileUploadDao(context);
            fileUploadDao.delete(localFileUpload, false);

            // Commit en base
            context.commitChanges();

            // On renvoie une réponse de type :
            // HTTP/1.1 201 OK
            // Location: https://server/application/api/v1/tempdirs/[tempdirId]/files/[fileId]
            // Content-Length: 0
            response.setStatus(201);
            response.setHeader("Location", ThaleiaApplication.get().getApplicationRootUrl() + TempDirsAPI.API_END_POINT + "/" + tempDir.getPublicId() + "/files/" + tempDirFile.getPublicId());
            response.setHeader("Content-Length", "0");

        } catch (Exception e) {
            logger.warn(ExceptionUtils.getStackTrace(e));
            response.setStatus(500);
        }
    }
}
