package fr.solunea.thaleia.webapp.api;

import com.google.gson.JsonObject;
import fr.solunea.thaleia.model.ApiToken;
import fr.solunea.thaleia.model.FileUpload;
import fr.solunea.thaleia.model.Plugin;
import fr.solunea.thaleia.model.dao.FileUploadDao;
import fr.solunea.thaleia.model.dao.ICayenneContextService;
import fr.solunea.thaleia.model.dao.PluginDao;
import fr.solunea.thaleia.service.FileUploadService;
import fr.solunea.thaleia.service.utils.Configuration;
import fr.solunea.thaleia.utils.DetailedException;
import fr.solunea.thaleia.utils.Hash;
import fr.solunea.thaleia.webapp.fileupload.PluginImportTreatment;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.log4j.Logger;
import org.apache.wicket.authorization.AuthorizationException;
import org.wicketstuff.rest.annotations.MethodMapping;
import org.wicketstuff.rest.annotations.ResourcePath;
import org.wicketstuff.rest.annotations.parameters.HeaderParam;
import org.wicketstuff.rest.annotations.parameters.RequestBody;
import org.wicketstuff.rest.utils.http.HttpMethod;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;

@ResourcePath("/api/v1/plugin")
@SuppressWarnings("unused")
public class PluginAPI extends ApiV1Service {

    private final static Logger logger = Logger.getLogger(PluginAPI.class);
    private final ICayenneContextService contextService;

    private fr.solunea.thaleia.service.PluginService pluginService;
    private FileUploadService fileUploadService;

    public PluginAPI(ICayenneContextService contextService, Configuration configuration,
                     ScheduledExecutorService executorService, Map<String, Future<?>> futures) {
        super(contextService, configuration);
        this.contextService = contextService;
        try {
            this.pluginService = new fr.solunea.thaleia.service.PluginService(contextService, executorService,
                    futures, configuration);
            this.fileUploadService = new FileUploadService(configuration);
        } catch (DetailedException e) {
            logger.warn("Impossible d'instancier le pluginService : " + e);
            logger.warn(e.getCause());
        }
    }

    /**
     *
     * @param token le token d'autorisation pour cet appel.
     * @return Liste des plugins dont l'accès est autorisé pour la personne identifiée par le token.
     */
    @MethodMapping(value = "", httpMethod = HttpMethod.GET)
    @SuppressWarnings("unused")
    public List<PluginPOJO> listPlugins(@HeaderParam("Authorization") String token) {

        logger.debug("Appel reçu pour la méthode listPlugins.");

        try {
            // Vérification du token Thaleia
            ApiToken apiToken = getToken(token, false);

            // Liste des plugins dont l'accès est autorisé pour la personne identifiée par le token
            List<Plugin> plugins = pluginService.getPlugins(apiToken.getUser());
            if (plugins == null) {
                plugins = new ArrayList<>();
            }
            List<PluginPOJO> result = new ArrayList<>();
            for (Plugin plugin : plugins) {
                result.add(new PluginPOJO(plugin));
            }
            logger.debug("Nombre de plugins dans la liste renvoyée : " + result.size());
            setResponseStatusCode(200);
            return result;

        } catch (AuthorizationException e) {
            logger.warn(e);
            setResponseStatusCode(403);
            return new ArrayList<>();

        } catch (Exception e) {
            logger.warn(e);
            setResponseStatusCode(500);
            return new ArrayList<>();
        }
    }

    @MethodMapping(value = "", httpMethod = HttpMethod.POST)
    @SuppressWarnings("unused")
    public JsonObject installPlugin(@HeaderParam("Authorization") String token,
                                    @RequestBody PluginUploadRequest pluginUploadRequest) {
        logger.debug("Appel reçu pour la méthode installPlugin.");

        ApiToken apiToken;
        try {
            // Vérification du token Thaleia
            apiToken = getToken(token, false);
            logger.debug("Filename reçu : " + pluginUploadRequest.filename);
            checkIfAdmin(apiToken);
        } catch (Exception e) {
            logger.warn(e);
            setResponseStatusCode(403);
            return null;
        }

        FileUpload fileUpload = null;
        try {
            // Création de l'objet de file_upload
            FileUploadDao fileUploadDao = new FileUploadDao(contextService.getContextSingleton());
            fileUpload = fileUploadService.createFileUpload(apiToken.getUser(), pluginUploadRequest.filename,
                    PluginImportTreatment.class.getName());
            fileUploadDao.save(fileUpload);
        } catch (DetailedException e) {
            logger.warn(e);
            logger.debug(ExceptionUtils.getFullStackTrace(e));
            return error(500, "Plugin upload request error", "Internal error during file upload request registration.");
        }

        try {
            // On renvoie une réponse de type :
            // HTTP/1.1 200 OK
            // Location: https://server/application/api/v1/upload/fileupload?upload_id=xa298sdSZThSDf
            // Content-Length: 0
            setResponseStatusCode(200);
            getCurrentWebResponse().setHeader("Location", fileUploadService.getFileUploadUrl(fileUpload));
            getCurrentWebResponse().setHeader("Content-Length", "0");
            return null;
        } catch (Exception e) {
            logger.warn(ExceptionUtils.getStackTrace(e));
            return error(500, "Plugin upload request error", "Internal error during upload URL generation.");
        }
    }

    private void checkIfAdmin(ApiToken apiToken) throws Exception {
        // Vérification du droit admin de l'utilisateur
        if (!apiToken.getUser().getIsAdmin()) {
            String message = "Refus d'une demande d'installation de plugin par un utilisateur non admin : "
                    + apiToken.getUser().getLogin();
            logger.debug(message);
            throw new Exception(message);
        }
    }

    @MethodMapping(value = "/{pluginId}", httpMethod = HttpMethod.DELETE)
    public JsonObject deletePlugin(@HeaderParam("Authorization") String token, int pluginId) {

        logger.debug("Appel reçu pour la méthode deletePlugin.");
        ApiToken apiToken;
        try {
            // Vérification du token Thaleia
            apiToken = getToken(token, false);
            checkIfAdmin(apiToken);
        } catch (Exception e) {
            logger.warn(e);
            setResponseStatusCode(403);
            return null;
        }

        PluginDao pluginDao = new PluginDao(contextService.getContextSingleton());
        Plugin plugin = pluginDao.get(pluginId);
        if (plugin == null) {
            logger.debug("Le plugin à supprimer '" + pluginId + "' n'a pas été trouvé en base.");
            return error(404, "Deletion error", "Object not found.");
        }

        // Supprime le plugin si les droits sont suffisants
        try {
            pluginService.deletePlugin(plugin, apiToken.getUser());
        } catch (DetailedException e) {
            logger.debug("L'utilisateur " + apiToken.getUser().getLogin() + " n'a pas les droits pour supprimer "
                    + "le plugin " + pluginId);
            return error(403, "Deletion error", "Insuficent rights.");
        }

        logger.debug("Plugin " + pluginId + " supprimé.");
        setResponseStatusCode(200);
        return ok(200, "Plugin is deleted.");
    }

    // Objets JSON attendus en entrée pour l'import des plugins
    static class PluginUploadRequest {
        private final String filename;

        public PluginUploadRequest(String filename) {
            this.filename = filename;
        }
    }

    class PluginPOJO {
        private final String id;
        private final String name;
        private final String filename;
        private final String hash;
        private final String date;

        PluginPOJO(Plugin plugin) {
            PluginDao pluginDao = new PluginDao(contextService.getContextSingleton());
            this.id = String.valueOf(pluginDao.getPK(plugin));
            this.name = plugin.getName();
            this.filename = plugin.getFilename();
            File jarFile = pluginService.getJarFile(plugin);
            this.hash = Hash.getSHA1Hash(jarFile);
            DateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy - hh:mm:ss");
            this.date = dateFormat.format(jarFile.lastModified());
        }

        public String getName() {
            return name;
        }
    }
}
