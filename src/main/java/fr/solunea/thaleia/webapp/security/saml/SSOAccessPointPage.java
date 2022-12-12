package fr.solunea.thaleia.webapp.security.saml;

import fr.solunea.thaleia.webapp.ThaleiaApplication;
import fr.solunea.thaleia.webapp.pages.LoginPage;
import fr.solunea.thaleia.webapp.security.ThaleiaSession;
import org.apache.wicket.request.flow.RedirectToUrlException;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

public class SSOAccessPointPage extends LoginPage {

    public SSOAccessPointPage() throws UnsupportedEncodingException {
        super();

        // Si identifié dans Thaleia, redirection vers la page des utilisateurs identifiés.
        if (ThaleiaSession.get().getAuthenticatedUser() != null) {
            redirectIfLoggedIn();
        } else {
            // Redirection hors Wicket pour une redirection vers l'IdP, et un RelayState sur la page d'accueil des
            // utilisateurs identifiés. La redirection en cas d'erreur d'identification est gérée par
            // l'ACSEndpointServlet.
            throw new RedirectToUrlException(
                    ThaleiaApplication.get().getApplicationRootUrl() + "/gosso?relayState=" + URLEncoder.encode(
                            ThaleiaApplication.get().getApplicationRootUrl() + "/home", "UTF-8"));
        }
    }

}
