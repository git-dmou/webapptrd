package fr.solunea.thaleia.webapp.lrs;

import fr.solunea.thaleia.model.LrsEndpoint;
import fr.solunea.thaleia.webapp.ThaleiaApplication;
import org.apache.log4j.Logger;

import java.util.List;

/**
 * Gestion des comptes d'accès au LRS.
 */
public class Identificator {

    @SuppressWarnings("unused")
    private static final Logger logger = Logger.getLogger(Identificator.class);

    public static Identificator getInstance() {
        return new Identificator();
    }

    /**
     * @return true si ce compte est celui qui est défini dans l'application
     * comme le compte d'accès à l'API xAPI, ou dans les comptes des LRS externes
     */
    public boolean loginForApi(String login, String password) {

        // logger.debug("Le compte '" + login + "'/'" + password
        // + "' est-il identique à '" + LRSParameters.getApiAccountLogin()
        // + "'/'" + LRSParameters.getApiAccountPassword() + "' ?");

        // On recherche le compte existant dans les paramètres de l'application.
        if (LRSParameters.getApiAccountLogin().equals(login)
                && LRSParameters.getApiAccountPassword().equals(password)) {
            return true;
        }

        // On recherche dans les comptes des LRS externes
        List<LrsEndpoint> endPoints = ThaleiaApplication.get().getLrsEndPointDao().findByUser(login, password);
        if (!endPoints.isEmpty()) {
            return true;
        }

        return false;
    }

    /**
     * @return true si ce compte est celui qui est défini dans l'application
     * comme le compte d'accès à l'API d'import/export des statements
     * xAPI.
     */
    public boolean loginForImpex(String login, String password) {

        // On recherche le compte existant dans les paramètres de l'application.
        if (LRSParameters.getImportExportAccountLogin().equals(login)
                && LRSParameters.getImportExportAccountPassword().equals(password)) {
            return true;
        }

        return false;
    }
}
