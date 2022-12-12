package fr.solunea.thaleia.webapp.fileupload;

import fr.solunea.thaleia.model.FileUpload;
import fr.solunea.thaleia.model.Plugin;
import fr.solunea.thaleia.model.dao.PluginDao;
import fr.solunea.thaleia.utils.DetailedException;
import fr.solunea.thaleia.webapp.ThaleiaApplication;
import org.apache.log4j.Logger;

import javax.servlet.http.HttpServletResponse;
import java.io.File;

public class PluginImportTreatment implements IFileUploadTreatment {

    private final static Logger logger = Logger.getLogger(PluginImportTreatment.class);

    @Override
    public void run(FileUpload fileUpload, File tempBinary, HttpServletResponse response) throws Exception {
        logger.debug("Traitement de l'import d'un plugin depuis un fichier uploadé...");
        // Création de l'objet hors du try, pour le supprimer en cas d'erreur d'installation.
        Plugin plugin = null;
        PluginDao pluginDao;
        try {
            pluginDao = ThaleiaApplication.get().getPluginDao();
        } catch (Exception e) {
            throw new Exception("Impossible d'obtenir un DAO.", e);
        }

        // On vérifie qu'il n'existe pas déjà un plugin portant ce nom
        // de classe principale
        String pluginClassName = fr.solunea.thaleia.service.PluginService.getPluginClassName(tempBinary);
        if (!pluginDao.findByName(pluginClassName).isEmpty()) {
            String message =
                    "Il existe déjà un plugin associé à cette classe de traitement : '" + pluginClassName + "' !";
            logger.info(message);
            throw new Exception(message);
        }

        // Création de l'objet plugin pour la base et enregistrement du binaire
        try {
            plugin = pluginDao.get();
            plugin.setDomain(fileUpload.getUser().getDomain());

            // Enregistrement du JAR
            ThaleiaApplication.get().getPluginService().savePlugin(plugin, tempBinary, fileUpload.getFilename());

            // Suppression du fichier temporaire -> automatique
            logger.debug("Plugin uploadé installé !");

            response.setStatus(200);

        } catch (DetailedException ex) {

            // Si erreur d'import, alors on supprime l'objet plugin qu'on s'apprêtait à installer.
            try {
                ThaleiaApplication.get().getPluginDao().delete(plugin);
            } catch (DetailedException e) {
                logger.warn("Erreur de suppression de l'objet Plugin temporaire : ", ex);
            }

            throw new Exception("Erreur d'enregistrement du plugin.", ex);

        }
    }
}
