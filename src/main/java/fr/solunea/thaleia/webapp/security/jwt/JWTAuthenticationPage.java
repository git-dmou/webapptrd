package fr.solunea.thaleia.webapp.security.jwt;

import fr.solunea.thaleia.webapp.ThaleiaApplication;
import fr.solunea.thaleia.webapp.pages.LoginPage;
import fr.solunea.thaleia.webapp.pages.ThaleiaPage;
import fr.solunea.thaleia.webapp.security.ThaleiaSession;
import org.apache.log4j.Logger;
import org.apache.wicket.request.flow.RedirectToUrlException;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.util.string.StringValue;

public class JWTAuthenticationPage extends ThaleiaPage {

    private final static Logger logger = Logger.getLogger(JWTAuthenticationPage.class);

    public final static String JWT_PARAMETER_NAME = "jwt";

    public JWTAuthenticationPage() {
        super();
        logger.debug("No parameter.");
        ThaleiaSession.get().error("JWT invalid.");
        setResponsePage(LoginPage.class);
    }

    public JWTAuthenticationPage(PageParameters parameters) {
        super();

        try {
            StringValue jwtValue = parameters.get(JWT_PARAMETER_NAME);

            if (jwtValue == null || jwtValue.isEmpty()) {
                throw new Exception("Pas de valeur pour le paramètre "+JWT_PARAMETER_NAME+".");
            } else {
                logger.debug("JWT passé en paramètre : " + jwtValue.toString());
            }
            String jwt = jwtValue.toString("");

            String email = ThaleiaSession.get().getUserService().getEmailFromJwtIfValid(jwt);

            logger.info("Identification par JWT de l'utilisateur " + email);
            if (ThaleiaSession.get().authenticateWithNoStats(email)) {
                ThaleiaSession.get().signIn(email, null);
            }

        } catch (Exception e) {
            logger.warn("Impossible de traiter la requête.", e);
            ThaleiaSession.get().error("JWT invalid.");
        }
        throw new RedirectToUrlException(ThaleiaApplication.get().getApplicationRootUrl());
    }

}
