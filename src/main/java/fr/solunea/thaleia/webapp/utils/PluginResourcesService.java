package fr.solunea.thaleia.webapp.utils;

import fr.solunea.thaleia.model.Plugin;
import fr.solunea.thaleia.model.User;
import fr.solunea.thaleia.service.PluginService;
import fr.solunea.thaleia.utils.DetailedException;
import fr.solunea.thaleia.webapp.ThaleiaApplication;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * Permet d'accéder à un fichier dans le dossier src/main/resources d'un plugin Thaleia.
 * Attention, il faut préciser le chemin vers le fichier dans le .jar du plugin.
 * Le contenu du dossier src/main/resources est copié à la racine du .jar.
 * Si le fichier est dans un sous-dossier de src/main/resources, il sera dans le même sous-dossier
 * à la racine du .jar.
 *
 * Le fichier est mis à disposition sous la forme d'un fichier temporaire
 *
 */


public class PluginResourcesService {

    private static PluginResourcesService singleton;
    private final static Logger logger = Logger.getLogger(PluginResourcesService.class);
    PluginService pluginService;


    public static PluginResourcesService get() throws DetailedException {
        if (singleton == null) {
            try {
                singleton = new PluginResourcesService();
            } catch (Exception e) {
                throw new DetailedException(e)
                        .addMessage("impossible d'initialiser le service PluginResourcesService");
            }
        }
        return singleton;
    }

    private PluginResourcesService() throws DetailedException {
        super();

        try {
            this.pluginService = ThaleiaApplication.get().getPluginService();
        } catch (DetailedException e) {
            throw new DetailedException("Probleme d'accès PluginService");
        }
    }

    private Plugin pluginInstalled(PluginsNames pluginName, User user) {

        List<Plugin> plugins = pluginService.getPlugins(user);
        for (Plugin plugin : plugins) {
            if (plugin.getName().equals(pluginName.getFullName()))
                return plugin;
        }
        return null;

    }

    public File getResource(User user, PluginsNames pluginName, String fileRelativePathInResourcesDir) throws DetailedException {
        Plugin plugin = pluginInstalled(pluginName, user);

        if( plugin == null) {
            throw new DetailedException("ressource inaccessible, plugin inconnu : " + plugin.getName());
        }

        File result = null;
        try {
            result = getFile(fileRelativePathInResourcesDir, plugin);
        } catch (IOException e) {
            logger.warn("Impossible d'accéder aux fichiers du plugin : " + e);
            throw new DetailedException("ressource inaccessible");
        }

        return result;
    }

    private File getFile(String fileRelativePathInResourcesDir, Plugin plugin) throws IOException {
        File result;
        File fileRessource = pluginService.getResource(plugin, fileRelativePathInResourcesDir);
        InputStream is = new FileInputStream(fileRessource);

        result = File.createTempFile(fileRelativePathInResourcesDir, "");
        FileUtils.copyInputStreamToFile(is, result);
        return result;
    }
}
