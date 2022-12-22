package fr.solunea.thaleia.webapp.api;

import com.google.gson.JsonObject;
import fr.solunea.thaleia.model.ApiToken;
import fr.solunea.thaleia.model.User;
import fr.solunea.thaleia.model.dao.ApiTokenDao;
import fr.solunea.thaleia.model.dao.ICayenneContextService;
import fr.solunea.thaleia.service.CmiDataService;
import fr.solunea.thaleia.service.LicenceService;
import fr.solunea.thaleia.service.UserService;
import fr.solunea.thaleia.service.utils.Configuration;
import fr.solunea.thaleia.utils.DetailedException;
import fr.solunea.thaleia.utils.LogUtils;
import fr.solunea.thaleia.webapp.ThaleiaApplication;
import fr.solunea.thaleia.webapp.security.ThaleiaSession;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.log4j.Logger;
import org.apache.wicket.authorization.AuthorizationException;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.request.http.WebResponse;
import org.wicketstuff.rest.annotations.MethodMapping;
import org.wicketstuff.rest.annotations.ResourcePath;
import org.wicketstuff.rest.annotations.parameters.HeaderParam;
import org.wicketstuff.rest.contenthandling.json.objserialdeserial.GsonObjectSerialDeserial;
import org.wicketstuff.rest.contenthandling.json.webserialdeserial.JsonWebSerialDeserial;
import org.wicketstuff.rest.resource.AbstractRestResource;
import org.wicketstuff.rest.resource.MethodMappingInfo;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.FileInputStream;
import java.io.OutputStream;

/**
 * Notez que pour toutes les méthodes mappées sur des URL qui attendent des paramètres (par exemple @see createToken),
 * si un paramètre attendu est absent de la requête, alors une erreur 400 sera levée avant d'arriver dans le corps de la
 * requête.
 * Avez-vous bien monté cette ressource dans l'application ?
 * Attention à l'ordre du montage des ressources dans l'application : "/api/v1" en premier.
 */
@ResourcePath("/api/v1")
public class ApiV1Service extends AbstractRestResource<JsonWebSerialDeserial> {

    // 86400 s = 24 h
    public static final int TOKEN_DURATION_IN_SECONDS = 86400;
    private final static Logger logger = Logger.getLogger(ApiV1Service.class);
    final CmiDataService cmiDataService;
    private final UserService userService;
    protected final ICayenneContextService contextService;
    private LicenceService licenceService;

    public ApiV1Service(ICayenneContextService contextService, Configuration configuration) {
        super(new JsonWebSerialDeserial(new GsonObjectSerialDeserial()));

        userService = new UserService(contextService, configuration);
        this.contextService = contextService;
        cmiDataService = new CmiDataService(contextService);
        try {
            licenceService = new LicenceService(contextService, configuration);
        } catch (DetailedException e) {
            logger.warn(e);
            licenceService = null;
        }
    }

    @Override
    protected void onBeforeMethodInvoked(MethodMappingInfo mappedMethod, Attributes attribs) {
        logger.debug(mappedMethod.getMethod().getName());
        logger.debug(attribs);
    }

    @Override
    protected void handleException(WebResponse response, Exception exception) {
        logger.warn(exception);
        logger.warn(exception.getCause());
        logger.warn(LogUtils.getStackTrace(exception.getStackTrace()));
        response.setStatus(500);
        response.write("Internal error.");

    }

    /**
     * Si le champ authorization ne contient pas un token valide, alors : 1/ on fixe le statut de la réponse à 401 2/ on
     * lance une exception.
     *
     * @param authorization   la valeur du champ authorization de la requête
     * @param acceptAnonymous si false, alors le token n'est pas considéré comme valide s'il n'est pas asocié à un
     *                        utilisateur.
     */
    protected ApiToken getToken(String authorization, boolean acceptAnonymous) throws AuthorizationException {
        ApiTokenDao apiTokenDao = new ApiTokenDao(contextService.getContextSingleton());
        ApiToken token = apiTokenDao.getTokenIfValid(authorization, TOKEN_DURATION_IN_SECONDS);
        if (token == null) {
            setResponseStatusCode(401);
            logger.debug("Le token '" + authorization + "' est considéré comme invalide car non reconnu.");
            throw new AuthorizationException("Invalid Authorization") {
            };
        } else {
            if (!acceptAnonymous && token.getUser() == null) {
                setResponseStatusCode(401);
                logger.debug("Le token '" + authorization + "' est considéré comme invalide car anonyme.");
                throw new AuthorizationException("Invalid Authorization") {
                };
            }
            if (token.getUser() != null) {
                logger.debug("Utilisateur identifié :" + token.getUser().getLogin());
            } else {
                logger.debug("Token anonyme autorisé.");
            }
            return token;
        }
    }

    @MethodMapping("/tokenlogin")
    @SuppressWarnings("unused")
    public String createToken(@HeaderParam("Authorization") String authorization) {
        // Identification par JWT

        // On extrait le token de la chaîne "Authorization: Bearer <token>
        if (!authorization.startsWith("Bearer ")) {
            logger.debug("Le champ Authorization doit être la forme \"Bearer " + "<token>\"");
            setResponseStatusCode(401);
        }
        String token = authorization.substring("Bearer ".length());
        logger.debug("Token transmis : '" + token + "'");

        String email;
        try {
            email = userService.getEmailFromJwtIfValid(token);
            logger.debug("Email extrait du token : " + email);
        } catch (Exception e) {
            logger.debug("Identification avec le token " + token + " impossible : token invalide.");
            setResponseStatusCode(500);
            return "";
        }

        // Pas d'exception : le token est valide. On recherche le compte associé à cet email
        User user = userService.loadByLoginAndPassword(email, null);
        // Création d'un token pour ce user
        return getToken(user);
    }

    /**
     * @return un token valide pour les appels à l'API Thaleia.
     */
    public String getToken(User user) {
        try {
            ApiTokenDao apiTokenDao = new ApiTokenDao(contextService.getContextSingleton());
            ApiToken token = apiTokenDao.generate(user, TOKEN_DURATION_IN_SECONDS);
            apiTokenDao.save(token);

            return token.getValue();
        } catch (DetailedException e) {
            logger.debug("Identification impossible : création du token refusée par le DAO.");
            setResponseStatusCode(500);
            return "";
        }
    }

    @MethodMapping("/login")
    @SuppressWarnings("unused")
    public String createToken(@HeaderParam(value = "user", required = false) String username, @HeaderParam(value = "password", required = false) String password) {

        User user;
        if ((username == null || username.isEmpty()) && (password == null || password.isEmpty())) {
            // pas de login / mot de passe : tentative d'identification par la Session
            User authenticatedUser = ThaleiaSession.get().getAuthenticatedUser();
            if (authenticatedUser != null) {
                // On utilise l'identification de la session Wicket retrouvée
                logger.debug("Identification par la session Wicket : " + authenticatedUser.getLogin());
                user = authenticatedUser;
            } else {
                logger.debug("Identification refusée : pas d'utilisateur identifiée dans la session.");
                setResponseStatusCode(401);
                return "";
            }

        } else if (!(username == null || username.isEmpty()) && (password == null || password.isEmpty())) {
            // login, mais pas de mot de passe : on refuse l'identification,
            // car ça serait un moyen de s'identifer sans vérifier le mot de passe
            logger.debug("Identification refusée : password null.");
            setResponseStatusCode(401);
            return "";

        } else {
            // On tente l'identification par login/mot de passe
            user = userService.loadByLoginAndPassword(username, password);
            if (user == null) {
                logger.debug("Identification pour username='" + username + "' password='" + password.replaceAll(".", "*")
                        + "' refusée : pas de compte " + "correspondant en " + "base.");
                setResponseStatusCode(401);
                return "";
            }
        }

        // Création d'un token pour ce user
        ApiTokenDao apiTokenDao = new ApiTokenDao(contextService.getContextSingleton());
        ApiToken token = apiTokenDao.generate(user, TOKEN_DURATION_IN_SECONDS);
        try {
            token.getObjectContext().commitChanges();
            logger.debug("Token enregistré en base pour " + username + " : " + token.getValue());
            return token.getValue();
        } catch (Exception e) {
            logger.warn(ExceptionUtils.getStackTrace(e));
            String hiddenPassword = "null";
            if (password != null) {
                hiddenPassword = password.replaceAll(".", "*");
            }
            logger.warn("Identification pour username='" + username + "' password='" + hiddenPassword + "' impossible : création du token refusée par le DAO.");
            setResponseStatusCode(500);
            return "";
        }

    }

    //    /**
    //     * Prépare une réponse de type Ok, et met en forme le message JSON de description du traitement effectué.
    //     */
    //    private String ok(int code, String okMessage, Object object) {
    //        String jsonObject = new Gson().toJson(object);
    //        logger.info(okMessage + " : " + jsonObject);
    //        setResponseStatusCode(200);
    //        return "{\"code\": " + code + "," + "\"message\": \"" + okMessage + "\"," + "\"object\": \"" +
    // jsonObject + "\""
    //                + "}";
    //    }

    @MethodMapping("/status")
    public String getStatus() {

        String remoteAddr =
                ((HttpServletRequest) RequestCycle.get().getRequest().getContainerRequest()).getRemoteAddr();
        logger.debug("Requête 'status' depuis l'adresse : " + remoteAddr);

        // On n'accepte que les requêtes depuis le serveur local
        //        if ("127.0.0.1".equals(remoteAddr)) {
        setResponseStatusCode(200);
        if (ThaleiaApplication.get().getLocaleDao().find().size() > 0) {
            return "ok";
        } else {
            return "nok";
        }
        //        } else {
        //            setResponseStatusCode(403);
        //            return "";
        //        }
    }

    /**
     * fixe dans la réponse le code HTTP 201, et place un message JSON décrivant la création.
     */
    protected JsonObject ok(int code, String okMessage) {
        logger.debug("Réponse JSON à une requête à l'API : " + okMessage);
        setResponseStatusCode(201);
        JsonObject json = new JsonObject();
        json.addProperty("code", code);
        json.addProperty("message", okMessage);
        json.addProperty("description", "");
        return json;
    }

    protected JsonObject error(int httpCode, String jsonCode, String errorMessage, String description) {
        logger.info(errorMessage + " : " + description);
        setResponseStatusCode(httpCode);

        JsonObject json = new JsonObject();
        json.addProperty("code", jsonCode);
        json.addProperty("message", errorMessage);
        json.addProperty("description", description);
        logger.debug("Réponse :\n" + json.toString());
        return json;
    }

    /**
     * Prépare une réponse de type Erreur, et met en forme le message JSON de description de l'erreur.
     */
    protected JsonObject error(int code, String errorMessage, String description) {
        return error(code, String.valueOf(code), errorMessage, description);
    }

    protected JsonObject sendBinaryToResponse(File result) {
        try (FileInputStream is = new FileInputStream(result); OutputStream os =
                getCurrentWebResponse().getOutputStream()) {
            logger.debug("Envoi du résultat : " + result.getAbsolutePath());
            setResponseStatusCode(200);
            getCurrentWebResponse().setHeader("Content-Length", String.valueOf(result.length()));
            getCurrentWebResponse().setHeader("Content-Type", "application/zip");
            getCurrentWebResponse().setHeader("Content-Disposition",
                    "attachment; filename=\"" + result.getName() + "\"");
            int copied = IOUtils.copy(is, os);
            getCurrentWebResponse().flush();
            logger.debug(copied + " octets transférés.");
            // On renvoie null, car si on renvoie une String alors on fait un second appel à response
            // .getOutputStream (), et celui-ci a déjà été fermé.
            return null;
        } catch (Exception e) {
            logger.warn("Erreur d'envoi de la réponse lors d'une requête de traitement.", e);
            return error(500, "Response sending error", "");
        }
    }

    protected ApiToken getApiTokenAndCheckLicence(@HeaderParam("Authorization") String token) throws Exception {
        ApiToken apiToken;// Vérification du token Thaleia : le token est bien associé à un utilisateur
        apiToken = getToken(token, false);

        // Est-ce que l'utilisateur est admin ?
        boolean isAdmin = false;
        if (apiToken.getUser() != null && apiToken.getUser().getIsAdmin()) {
            isAdmin = true;
        }

        // On vérifie que l'utilisateur a une licence en cours de validité
        if (!isAdmin && !licenceService.isUserValid(apiToken.getUser(), false)) {
            String userLogin = "";
            if (apiToken.getUser() != null) {
                userLogin = apiToken.getUser().getLogin();
            }
            throw new Exception("L'utilisateur '" + userLogin + "' n'a pas de licence valide.");
        }
        return apiToken;
    }
}
