package fr.solunea.thaleia.webapp.security.saml;

import com.onelogin.saml2.Auth;
import fr.solunea.thaleia.utils.DetailedException;
import org.apache.log4j.Logger;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.net.URLDecoder;

public class AuthRequestSenderServlet extends HttpServlet {

    private static final Logger logger = Logger.getLogger(AuthRequestSenderServlet.class);

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) {
        try {
            // Demande d'identification (AuthNRequest) à l'IdP
            Auth auth;
            try {
                // Génération de la demande d'identification SAML
                auth = new Auth(SamlRequest.getSettings(), request, response);
            } catch (Exception e) {
                throw new DetailedException(e).addMessage("Impossible d'instancier l'objet d'identification SAML.");
            }

            try {
                // On recherche le paramètre RelayState dans l'URL de la requête
                String relayState = request.getParameter("relayState");
                if (relayState == null) {
                    relayState = request.getRequestURL().toString().replace("gosso", "login");
                } else {
                    relayState = URLDecoder.decode(relayState, "UTF-8");
                }

                // La page des utilisateurs identifiés est donnée comme RelayState (URL de redirection si succès de
                // l'identification)
                auth.login(relayState);
            } catch (Exception e) {
                throw new DetailedException(e).addMessage(
                        "Erreur durant le traitement de la requête AuthNRequest auprès " + "du IdP");
            }
        } catch (DetailedException e) {
            logger.warn(e);
        }

    }

}
