package fr.solunea.thaleia.webapp.fileupload;

import fr.solunea.thaleia.service.utils.ClassFactory;

public abstract class FileUploadTreatment {

    public static IFileUploadTreatment get(String treatmentClassName) throws Exception {
        if (treatmentClassName == null || treatmentClassName.isEmpty()) {
            throw new Exception("Nom de classe de traitement vide.");
        }

        return ClassFactory.getInstanceOf(treatmentClassName, IFileUploadTreatment.class);
    }

}
