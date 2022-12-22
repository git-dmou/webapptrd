package fr.solunea.thaleia.webapp.api.transform;

import fr.solunea.thaleia.model.ContentVersion;
import fr.solunea.thaleia.model.User;
import fr.solunea.thaleia.utils.DetailedException;
import org.apache.log4j.Logger;

import java.util.Locale;


public class CannelleTranslationTreatment  extends AbstractCannelleTreatment<ContentVersion> {

    private static final Logger logger = Logger.getLogger(CannelleTranslationTreatment.class);

    String origLanguage;
    String targetLanguage;

    public CannelleTranslationTreatment(String origLanguage, String targetLanguage) {
        this.origLanguage = origLanguage;
        this.targetLanguage =  targetLanguage;
    }

    @Override
    public ContentVersion transform(Object input, User user, Locale locale) throws DetailedException {
        return invokeCannelleMethod(input, user, locale, "translateFromSource", origLanguage, targetLanguage);

//        return null;
    }

}
