package fr.solunea.thaleia.webapp.fileupload;

import fr.solunea.thaleia.model.FileUpload;

import javax.servlet.http.HttpServletResponse;
import java.io.File;

/**
 * Les classes de traitement à effectuer lors de la réception d'un fichier (fileUpload) doivent
 * implémenter cette interface.
 */
public interface IFileUploadTreatment {

    /**
     *
     * @param fileUpload l'objet FileUpload qui est à traiter
     * @param tempBinary le binaire récupéré, qui est un fichier temporaire.
     * @param response l'objet permettant d'écrire dans la réponse HTTP
     * @throws Exception
     */
    void run(FileUpload fileUpload, File tempBinary, HttpServletResponse response) throws Exception;

}
