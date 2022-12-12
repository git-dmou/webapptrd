package fr.solunea.thaleia.webapp.lrs;

import fr.solunea.thaleia.model.ApplicationParameter;
import fr.solunea.thaleia.model.dao.ApplicationParameterDao;
import fr.solunea.thaleia.service.LRSService;
import fr.solunea.thaleia.utils.DetailedException;
import fr.solunea.thaleia.webapp.ThaleiaApplication;
import org.apache.log4j.Logger;

/**
 * Les paramètres d'accès au LRS.
 */
public class LRSParameters {

    public static final String LRS_API_ACCOUNT_PASSWORD_PARAMETER = "lrs.api.account.password";
    public static final String LRS_API_URL_PARAMETER = "lrs.api.server.url";
    public static final String LRS_IMPORTEXPORT_ACCOUNT_LOGIN_PARAMETER = "lrs.impex.account.login";
    public static final String LRS_IMPORTEXPORT_ACCOUNT_PASSWORD_PARAMETER = "lrs.impex.account.password";
    public static final String LRS_IMPORTEXPORT_URL_PARAMETER = "lrs.impex.server.url";
    protected static final Logger logger = Logger.getLogger(LRSParameters.class);

    /**
     * @return la valeur de l'identifiant du compte d'accès à l'API du LRS.
     */
    public static String getApiAccountLogin() {
        return getValue(LRSService.LRS_API_ACCOUNT_LOGIN_PARAMETER);
    }

    /**
     * @return la valeur du mot de passe du compte d'accès à l'API du LRS.
     */
    public static String getApiAccountPassword() {
        return getValue(LRS_API_ACCOUNT_PASSWORD_PARAMETER);
    }

    /**
     * @return l'url d'accès à l'API du LRS.
     */
    public static String getApiUrl() {
        return getValue(LRS_API_URL_PARAMETER);
    }

    /**
     * @return l'url d'accès à l'import/export du LRS.
     */
    public static String getImportExportUrl() {
        return getValue(LRS_IMPORTEXPORT_URL_PARAMETER);
    }

    /**
     * @return la valeur de l'identifiant du compte d'accès à l'import/export du
     * LRS.
     */
    public static String getImportExportAccountLogin() {
        return getValue(LRS_IMPORTEXPORT_ACCOUNT_LOGIN_PARAMETER);
    }

    /**
     * @return la valeur du mot de passe du compte d'accès à l'import/export du
     * LRS.
     */
    public static String getImportExportAccountPassword() {
        return getValue(LRS_IMPORTEXPORT_ACCOUNT_PASSWORD_PARAMETER);
    }

    /**
     * @return La valeur du paramètre parameterName. Cette valeur est recherchée
     * dans les paramètres de l'application Thaleia. Si ce paramètre
     * n'est pas défini, alors sa valeur est initialisée d'après les valeurs trouvées dans le contexte de l'application.
     */
    private static String getValue(String parameterName) {
        String result;
        result = ThaleiaApplication.get().getConfiguration().getDatabaseParameterValue(parameterName, "");

        if (result.isEmpty()) {

            // On charge la valeur du paramètre
            result = ThaleiaApplication.get().getServletContext().getInitParameter(parameterName);
            if (result == null) {
                result = "";
            }

            // On enregistre la valeur en base
            ApplicationParameterDao applicationParameterDao = new ApplicationParameterDao(ThaleiaApplication.get().contextService.getNewContext());
            ApplicationParameter applicationParameter = applicationParameterDao.get();
            applicationParameter.setName(parameterName);
            applicationParameter.setValue(result);
            try {
                applicationParameterDao.save(applicationParameter, true);
            } catch (DetailedException e) {
                logger.info("Impossible d'enregistrer en base la valeur du paramètre : " + e);
            }
        }

        return result;
    }

}
