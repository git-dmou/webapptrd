package fr.solunea.thaleia.webapp.api.customization;

import fr.solunea.thaleia.model.ApiToken;
import fr.solunea.thaleia.model.dao.ICayenneContextService;
import fr.solunea.thaleia.service.utils.Configuration;
import fr.solunea.thaleia.utils.DetailedException;
import fr.solunea.thaleia.webapp.api.ApiV1Service;
import org.apache.log4j.Logger;
import org.wicketstuff.rest.annotations.MethodMapping;
import org.wicketstuff.rest.annotations.ResourcePath;
import org.wicketstuff.rest.annotations.parameters.HeaderParam;
import org.wicketstuff.rest.annotations.parameters.RequestBody;
import org.wicketstuff.rest.utils.http.HttpMethod;


/**
 *  API permettant de remplacer le contenu css du fichier de customization en cours
 */

@ResourcePath("/api/v1/customization")
public class CustomizationAPI extends ApiV1Service {

    private final static Logger logger = Logger.getLogger(CustomizationAPI.class);
    private final ICayenneContextService contextService;
    private final Configuration configuration;
    private ApiToken apiToken;

    public CustomizationAPI(ICayenneContextService contextService,
                            Configuration configuration) {
        super(contextService, configuration);
        this.contextService = contextService;
        this.configuration = configuration;
    }

    @MethodMapping(value = "/css", httpMethod = HttpMethod.POST)
    @SuppressWarnings("unused")
    public Object importCss(@HeaderParam("Authorization") String token, @RequestBody String fileUploadRequest) {

        logger.debug("appel API importCss !!");

        ApiToken apiToken = authentication(token);
        if (apiToken == null) return "token invalide";

        String fileContent = fileUploadRequest;
        try {
            POSTCustomizationApiTreatment treatments = new POSTCustomizationApiTreatment(contextService, configuration, apiToken, fileContent);
            treatments.run();
        } catch (Exception e) {
            logger.warn(e);
            return error(500, "Internal error le fichier ne peut pas être chargé.", "");
        }

        setResponseStatusCode(200);
        return "importCss OK";
    }

    @MethodMapping(value = "/css", httpMethod = HttpMethod.GET)
    @SuppressWarnings("unused")
    public Object getCurrentCustomizationCss(@HeaderParam("Authorization") String token) {

        logger.debug("appel API getCurrentCustomizationCss !!");
        String currentCustomizationCssString ;

        apiToken = authentication(token);
        if (apiToken == null) return "token invalide";

        CssContentsBody cssContentsResponse = new CssContentsBody();
        try {
            buildResponse(cssContentsResponse);

        } catch (Exception e) {
            logger.warn(e);
            return error(500, "Internal error le contenu css ne peut pas être chargé.", "");
        }

        setResponseStatusCode(200);
        return cssContentsResponse;

    }

    private void buildResponse(CssContentsBody cssContentResponse) throws DetailedException {
        String currentCustomizationCssString;
        GETCustomizationApiTreatment treatments = new GETCustomizationApiTreatment(contextService, configuration,
                apiToken);
        currentCustomizationCssString = treatments.getCssString(apiToken);
        cssContentResponse.cssContents = currentCustomizationCssString;
        if (currentCustomizationCssString.equals("")) {
            cssContentResponse.message = "contenu css vide";
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

    static class CssContentsBody {
        String cssContents = "";
        String message = "";
    }




}
