package fr.solunea.thaleia.webapp.api.transform;

import fr.solunea.thaleia.model.ContentVersion;
import fr.solunea.thaleia.model.User;
import fr.solunea.thaleia.utils.DetailedException;
import org.apache.log4j.Logger;

import java.util.Locale;

public class CannelleImportTreatment extends AbstractCannelleTreatment<ContentVersion> {

    private static final Logger logger = Logger.getLogger(CannelleImportTreatment.class);

    /**
     * @param input le File contenant la source du contenu Cannelle à importer.
     * @return le ContentVersion du module qui a été importé.
     */
    @Override
    public ContentVersion transform(Object input, User user, Locale locale) throws DetailedException {
        return invokeCannelleMethod(input, user, locale, "generateFromSource");
    }
}
