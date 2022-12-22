package fr.solunea.thaleia.webapp.api.transform;

import fr.solunea.thaleia.model.User;
import fr.solunea.thaleia.utils.DetailedException;

import java.io.File;
import java.util.Locale;

public class CannelleImportExportTreatment extends AbstractCannelleTreatment<File> {

    /**
     * @param input le File contenant la source du contenu Cannelle à importer puis exporter.
     * @return le contenu de formation exporté sous forme d'une archive ZIP.
     */
    @Override
    public File transform(Object input, User user, Locale locale) throws DetailedException {
        return invokeCannelleMethod(input, user, locale, "generateAndExportFromSource", "","");
    }

}
