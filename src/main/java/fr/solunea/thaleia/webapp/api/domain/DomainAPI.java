package fr.solunea.thaleia.webapp.api.domain;

import fr.solunea.thaleia.model.ApiToken;
import fr.solunea.thaleia.model.Domain;
import fr.solunea.thaleia.model.dao.DomainDao;
import fr.solunea.thaleia.model.dao.ICayenneContextService;
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

@ResourcePath("/api/v1/domain")
public class DomainAPI extends ApiV1Service {

    private final static Logger logger = Logger.getLogger(DomainAPI.class);
    private final ICayenneContextService contextService;
    private final Configuration configuration;
    private ApiToken apiToken;
    DomainDao domainDao;


    public DomainAPI(ICayenneContextService contextService,
                     Configuration configuration) {
        super(contextService, configuration);
        this.contextService = contextService;
        this.configuration = configuration;
    }


    /**
     * renvoie la liste des domaines contenant le texte '{partialname}' dans leur nom,
     * transmis à la fin de l'url de la requête HTTP
     *
     * pour récupérer la liste complète des domains, on peut utiliser comme clé de recherche :
     *  - "_"   --> correspond au caractère joker "_" de sql
     *  - "%25" --> correspond au caractère joker "%" de sql
     * @param token
     * @param partialname
     * @return
     */
    @MethodMapping(value = "/{partialname}", httpMethod = HttpMethod.GET)
    @SuppressWarnings("unused")
    public Object getDomainsByPartialName(@HeaderParam("Authorization") String token, String partialname) {

        logger.debug("appel API getDomainByPartialName !!");

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

        domainDao = new DomainDao(ThaleiaSession.get().getContextService().getNewContext());
        return requestDomainsByPartialName(partialname);

    }

    private Object requestDomainsByPartialName(String partialname) {

        try {
            DomainResponseBody domainListResponse = new DomainResponseBody();
            domainListResponse.domainsList = domainDao.findDomainsByPartialName(partialname);
            domainListResponse.message = "requête OK";
            if (domainListResponse.domainsList.size() == 0) {
                domainListResponse.message = "aucun domaine ne correspond à la requête : " + partialname;
            }
            setResponseStatusCode(200);
            return domainListResponse;

        } catch (Exception e) {
            logger.warn(e);
            return error(500, "Internal error la liste des domaines ne peut pas être chargée.", "");
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


    static class DomainResponseBody {
        List<Domain> domainsList = new ArrayList<>();
        String message = "";
    }
}
