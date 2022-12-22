package fr.solunea.thaleia.webapp.api.transform;

import fr.solunea.thaleia.model.User;
import fr.solunea.thaleia.utils.DetailedException;

import java.util.Locale;

public class CannellePreviewTreatment extends AbstractCannelleTreatment<String> {

    /**
     * @param input la ContentVersion du module Cannelle à prévisualiser.
     * @return l'URL de prévisualisation.
     */
    @Override
    public String transform(Object input, User user, Locale locale) throws DetailedException {
        return invokeCannelleMethod(input, user, locale, "preview", "", "");
    }
}
