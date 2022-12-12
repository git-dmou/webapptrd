package fr.solunea.thaleia.webapp.api.transform;

import fr.solunea.thaleia.model.User;
import fr.solunea.thaleia.utils.DetailedException;

import java.io.File;

public class CannellePublicationExportTreatment extends AbstractCannelleTreatment<File> {

    /**
     * @param input le File contenant la source du contenu Cannelle à exporter.
     * @return le ContentVersion du module qui a été exporté.
     */
    @Override
    public File transform(Object input, User user, java.util.Locale contentLocale) throws DetailedException {
        return invokeCannelleMethod(input, user, contentLocale, "publicationExport");
    }
}
