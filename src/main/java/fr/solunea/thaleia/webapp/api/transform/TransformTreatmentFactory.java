package fr.solunea.thaleia.webapp.api.transform;

import fr.solunea.thaleia.utils.DetailedException;

public class TransformTreatmentFactory {

    public static final String CANNELLE = "cannelle";
    public static final String CANNELLE_IMPORT = "cannelle_import";
    public static final String CANNELLE_PUBLICATION_EXPORT = "cannelle_publication_export";

    public static ITransformTreatment<?> get(String treatementName) throws DetailedException {
        if (treatementName == null || treatementName.isEmpty()) {
            throw new DetailedException("Pas de nom de traitement !");
        }

        if (CANNELLE.equals(treatementName)) {
            return new CannelleImportExportTreatment();
        }

        if (CANNELLE_IMPORT.equals(treatementName)) {
            return new CannelleImportTreatment();
        }

        if (CANNELLE_PUBLICATION_EXPORT.equals(treatementName)) {
            return new CannellePublicationExportTreatment();
        }

        throw new DetailedException("Le traitement '"+treatementName+"' est invalide !");
    }
}
