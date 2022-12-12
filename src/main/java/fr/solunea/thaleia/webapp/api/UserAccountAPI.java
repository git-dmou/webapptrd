package fr.solunea.thaleia.webapp.api;

import fr.solunea.thaleia.model.ApiToken;
import fr.solunea.thaleia.model.Licence;
import fr.solunea.thaleia.model.User;
import fr.solunea.thaleia.model.dao.ApplicationParameterDao;
import fr.solunea.thaleia.model.dao.ICayenneContextService;
import fr.solunea.thaleia.model.dao.LicenceDao;
import fr.solunea.thaleia.model.dao.UserDao;
import fr.solunea.thaleia.service.UserService;
import fr.solunea.thaleia.service.utils.ActionResult;
import fr.solunea.thaleia.service.utils.Configuration;
import fr.solunea.thaleia.service.utils.UpdateUserRequest;
import fr.solunea.thaleia.service.utils.UserServiceResultCode;
import fr.solunea.thaleia.utils.DetailedException;
import fr.solunea.thaleia.webapp.Mailing;
import fr.solunea.thaleia.webapp.ThaleiaApplication;
import fr.solunea.thaleia.webapp.security.ThaleiaSession;
import org.apache.log4j.Logger;
import org.wicketstuff.rest.annotations.MethodMapping;
import org.wicketstuff.rest.annotations.parameters.HeaderParam;
import org.wicketstuff.rest.annotations.parameters.RequestBody;
import org.wicketstuff.rest.utils.http.HttpMethod;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class UserAccountAPI extends ApiV1Service {

    public final static String API_END_POINT = "/api/v1/useraccount";
    // Dans les paramètres de l'application, le paramètre qui permet d'activer ou non l'envoi des mails à la création d'un compte utilisateur.
    public static final String API_USER_ACCOUNT_POST_SEND_MAILS_PARAMETER_NAME = "api.userAccount.post.sendMails";
    private final static Logger logger = Logger.getLogger(UserAccountAPI.class);
    private final ICayenneContextService contextService;
    private ApiToken apiToken;
    UserDao userDao;


    public UserAccountAPI(ICayenneContextService contextService, Configuration configuration) {
        super(contextService, configuration);
        this.contextService = contextService;
    }



    /**
     * renvoie la liste des utilisateurs contenant le texte '{partiallogin}' dans leur login,
     * transmis à la fin de l'url de la requête HTTP
     *
     * pour récupérer la liste complète des utilisateurs, on peut utiliser comme clé de recherche :
     * - "_"   --> correspond au caractère joker "_" de sql
     * - "%25" --> correspond au caractère joker "%" de sql
     *
     * @param partiallogin
     * @return
     */
    @MethodMapping(value = "/{partiallogin}", httpMethod = HttpMethod.GET)
    @SuppressWarnings("unused")
    public Object getUsersByPartialLogin(@HeaderParam("Authorization") String token, String partiallogin) {

        logger.debug("appel API getUserByPartialLogin !!");

        apiToken = authentication(token);
        if (apiToken == null) {
            setResponseStatusCode(403);
            logger.debug("Refus d'accès à l'API : token invalide");
            return null;
        }

        if (!apiToken.getUser().getIsAdmin()) {
            logger.debug("L'appelant de la requête '" + apiToken.getUser() + "' n'est pas administrateur.");
            setResponseStatusCode(403);
            return null;
        }

        userDao = new UserDao(ThaleiaSession.get().getContextService().getNewContext());
        return requestUsersByPartialLogin(partiallogin);
    }

    private Object requestUsersByPartialLogin(String partiallogin) {
        try {
            UserResponseBody userListResponse = new UserResponseBody();
            userListResponse.usersList = userDao.findUsersByPartialLogin(partiallogin);
            userListResponse.message = "requête OK";

            if (userListResponse.usersList.size() == 0) {
                userListResponse.message = "aucun utilisateur ne correspond à la requête : " + partiallogin;
            }
            setResponseStatusCode(200);
            return userListResponse;

        } catch (Exception e) {
            logger.warn(e);
            return error(500, "Internal error la liste des users ne peut pas être chargée.", "");
        }
    }

    private ApiToken authentication(String token) {
        ApiToken apiToken;
        try {
            apiToken = getToken(token, false);
        } catch (Exception e) {
            logger.warn(e);
            setResponseStatusCode(403);
            return null;
        }
        return apiToken;
    }


    @MethodMapping(value = "/{userLogin}", httpMethod = HttpMethod.PUT)
    @SuppressWarnings("unused")
    public Object update(@HeaderParam("Authorization") String token, String userLogin, @RequestBody UpdateUserRequest updateUserRequest) {

        ApiToken apiToken;
        try {
            // Vérification du token Thaleia : le token est bien associé à un utilisateur
            apiToken = getApiTokenAndCheckLicence(token);
        } catch (Exception e) {
            logger.debug("Refus d'accès à l'API.", e);
            setResponseStatusCode(403);
            return null;
        }

        User updatingUser = apiToken.getUser();

        // Vérification des droits
        User updatedUser = ThaleiaApplication.get().getUserDao().findUserByLogin(userLogin);
        if (!updatingUser.getIsAdmin()) {
            // Si l'utilisateur qui fait la demande n'est pas administrateur, alors il faut que le compte à mettre à jour soit le même que lui.
            if (!updatingUser.equals(updatedUser)) {
                logger.debug("L'appelant de la requête '" + updatingUser.getLogin() + "' n'est pas autorisé à mettre à jour l'utilisateur '" + updateUserRequest.name + "'.");
                setResponseStatusCode(403);
                return null;
            }
        }

        try {
            // Mise à jour du compte et obtention d'un token JWT d'identification
            UserService userService = ThaleiaApplication.get().getUserService();
            String jwt = userService.updateUserAccountAndGetLoginToken(updateUserRequest, updatedUser, true);

            // Si l'option est activée dans la table application_parameter le contact Mailing sera également mis à jour.
            ApplicationParameterDao applicationParameterDao = new ApplicationParameterDao(ThaleiaSession.get().getContextService().getNewContext());
            if (applicationParameterDao.getValue("mailing.registerNewAccountOnAccountCreation", "false").equals("true")) {
                try {
                    Mailing.get().updateContactOnAccountCompletion(updatedUser.getLogin(), updateUserRequest);
                } catch (DetailedException e) {
                    logger.error(e);
                }
            } else {
                logger.info ("Pas de mise à jour d'un contact mailjet pour l'utilisateur " + updatedUser.getLogin());
            }


            LoginResponse loginResponse = new LoginResponse();
            loginResponse.code = "0";
            loginResponse.message = "User account updated.";
            loginResponse.login_url = ThaleiaApplication.get().getApplicationRootUrl() + "/Web/SSO?jwt=" + jwt;
            return loginResponse;

        } catch (Exception e) {
            logger.info("Impossible de mettre à jour le compte utilisateur : ", e);
            return error(500, "1", "An error occured.",
                    "");
        }
    }

    /**
     * Création d'un compte (s'il n'existe pas) et attribution d'une licence (que le compte existe préalablement ou pas)
     */
    @MethodMapping(value = "", httpMethod = HttpMethod.POST)
    @SuppressWarnings("unused")
    public Object create(@HeaderParam("Authorization") String token, @RequestBody AttributeLicenceRequest attributeLicenceRequest) {
        ApiToken apiToken;
        try {
            // Vérification du token Thaleia : le token est bien associé à un utilisateur
            apiToken = getApiTokenAndCheckLicence(token);
        } catch (Exception e) {
            logger.debug("Refus d'accès à l'API.", e);
            setResponseStatusCode(403);
            return null;
        }

        // On extrait les paramètres de la requête dans son corps, du type :
        // {
        //"licence" : "v5.dialogue.demo",
        //"origin" : "https://www.solunea.fr",
        //"email" : "toto@solunea.fr",
        //"account_created_subject": "Accès à votre démo Dialogue",
        //"account_created_body" : "<html>…</html>",
        //"account_completion_redirection" : "https://site/completion&user=toto@solunea.fr"
        //}
        LicenceDao licenceDao = new LicenceDao(contextService.getContextSingleton());
        Licence licence = licenceDao.findByName(attributeLicenceRequest.licence);
        if (licence == null) {
            return error(500, "500", "An error occured.",
                    "Licence not found.");
        }

        // Vérification de la locale
        Locale locale;
        try {
            locale = Locale.forLanguageTag(attributeLicenceRequest.account_created_locale);
            if (locale == null) {
                throw new Exception("Null locale.");
            }
        } catch (Exception e) {
            logger.info("Impossible d'utiliser la locale '" + attributeLicenceRequest.account_created_locale + "' pour la création d'un compte utilisateur.");
            return error(500, "10", "An error occured.", "Locale not valid.");
        }

        // Doit-on envoyer des mails ? Pas forcément pour un test unitaire.
        boolean sendMails = Boolean.parseBoolean(new ApplicationParameterDao(contextService.getContextSingleton()).getValue(API_USER_ACCOUNT_POST_SEND_MAILS_PARAMETER_NAME, "true"));

        ActionResult result = ThaleiaApplication.get().getUserService().attributeVerifiedLicenceToAccount(apiToken.getUser(), licence, attributeLicenceRequest.email,
                locale, attributeLicenceRequest.account_created_subject, attributeLicenceRequest.account_created_body,
                attributeLicenceRequest.origin, attributeLicenceRequest.account_completion_redirection,
                ThaleiaApplication.get().getApplicationRootUrl(), sendMails);

        if (UserServiceResultCode.LICENSED_REFUSED.getValue().equals(result.getCode())) {
            return error(403, result.getCode(), result.getMessage(), result.getDescription());
        }
        if (result.isError()) {
            return error(500, result.getCode(), result.getMessage(),
                    result.getDescription());
        } else {
            // Réussite avec création de compte
            if (UserServiceResultCode.OK_WITH_CREATION.getValue().equals(result.getCode())) {

                // Si l'option est activée dans la table application_parameter la création de compte sera suivie d'une création de contact pour mailing.
                ApplicationParameterDao applicationParameterDao = new ApplicationParameterDao(ThaleiaSession.get().getContextService().getNewContext());
                if (applicationParameterDao.getValue("mailing.registerNewAccountOnAccountCreation", "false").equals("true")) {
                    try {
                        String listId = applicationParameterDao.getValue("mailjet.contactList.demo.ThaleiaXl", "false");
                        if(listId.equals("")) {
                            throw new Exception("Aucune valeur n'a été trouvée pour la clé \"mailjet.contactList.demo.ThaleiaXl\" dans la table \"application_parameter\". " +
                                    "Le nouveau contact ne peut donc être rattaché à aucune liste de contacts mailjet.");
                        } else {
                            Mailing.get().registerNewAccount(attributeLicenceRequest.email, listId, String.valueOf(locale));
                        }
                    } catch (DetailedException e) {
                        logger.error(e);
                    } catch (Exception e) {
                        logger.error(e);
                    }
                } else {
                    logger.info("Pas d'enregistrement d'un nouveau contact Mailjet pour le profil " + attributeLicenceRequest.email);
                }

                // On renvoie la réponse à l'API
                Response response = new Response();
                response.code = "00";
                response.message = result.getMessage();
                return response;
            } else if (UserServiceResultCode.OK_WITHOUT_CREATION.getValue().equals(result.getCode())) {
                {
                    // Code = 01 : réussite avec attribution de licence à un compte existant
                    RedirectResponse response = new RedirectResponse();
                    response.code = "01";
                    response.message = result.getMessage();
                    response.redirect = ThaleiaApplication.get().getApplicationRootUrl();
                    return response;
                }
            }
        }
        // Si on arrive ici, c'est qu'un code de résultat de traitement OK n'a pas été correctement traité.
        return error(500, result.getCode(), result.getMessage(), result.getDescription());
    }

    static class AttributeLicenceRequest {
        String licence;
        String origin;
        String email;
        String account_created_locale;
        String account_created_subject;
        String account_created_body;
        String account_completion_redirection;
    }

    static class LoginResponse {
        String code;
        String message;
        String login_url;

        public String getCode() {
            return code;
        }

        public String getMessage() {
            return message;
        }

        public String getLoginUrl() {
            return login_url;
        }
    }

    static class RedirectResponse {
        String code;
        String message;
        String redirect;

        public String getMessage() {
            return message;
        }

        public String getCode() {
            return code;
        }

        public String getRedirect() {
            return redirect;
        }
    }

    static class Response {
        String code;
        String message;

        public String getCode() {
            return code;
        }

        public String getMessage() {
            return message;
        }
    }

    static class UserResponseBody {
        List<User> usersList = new ArrayList<>();
        String message = "coucou";
    }
}
