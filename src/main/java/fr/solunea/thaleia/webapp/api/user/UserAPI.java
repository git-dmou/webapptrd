package fr.solunea.thaleia.webapp.api.user;

import fr.solunea.thaleia.model.ApiToken;
import fr.solunea.thaleia.model.User;
import fr.solunea.thaleia.model.dao.ICayenneContextService;
import fr.solunea.thaleia.model.dao.UserDao;
import fr.solunea.thaleia.service.utils.Configuration;
import fr.solunea.thaleia.webapp.api.ApiV1Service;
import fr.solunea.thaleia.webapp.security.ThaleiaSession;
import org.apache.log4j.Logger;
import org.wicketstuff.rest.annotations.MethodMapping;
import org.wicketstuff.rest.annotations.ResourcePath;
import org.wicketstuff.rest.annotations.parameters.HeaderParam;
import org.wicketstuff.rest.utils.http.HttpMethod;

import java.util.ArrayList;
import java.util.List;

@ResourcePath("/api/v1/users")
public class UserAPI  extends ApiV1Service {

    private final static Logger logger = Logger.getLogger(UserAPI.class);
    private final ICayenneContextService contextService;
    private final Configuration configuration;
    private ApiToken apiToken;
    UserDao userDao;


    public UserAPI(ICayenneContextService contextService,
                            Configuration configuration) {
        super(contextService, configuration);
        this.contextService = contextService;
        this.configuration = configuration;
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

        userDao = new UserDao(ThaleiaSession.get().getContextService().getContextSingleton());
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
            return null;
        }
        return apiToken;
    }


    static class UserResponseBody {
        List<User> usersList = new ArrayList<>();
        String message = "coucou";
    }
}
