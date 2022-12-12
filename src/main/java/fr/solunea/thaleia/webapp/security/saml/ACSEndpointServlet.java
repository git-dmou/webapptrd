package fr.solunea.thaleia.webapp.security.saml;

import com.onelogin.saml2.Auth;
import fr.solunea.thaleia.model.Publication;
import fr.solunea.thaleia.model.PublicationSession;
import fr.solunea.thaleia.model.dao.PublicationSessionDao;
import fr.solunea.thaleia.service.PublicationService;
import fr.solunea.thaleia.service.utils.Unique;
import fr.solunea.thaleia.utils.DetailedException;
import fr.solunea.thaleia.utils.LogUtils;
import fr.solunea.thaleia.webapp.ThaleiaApplication;
import fr.solunea.thaleia.webapp.preview.PublishPage;
import fr.solunea.thaleia.webapp.security.ThaleiaSession;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static fr.solunea.thaleia.webapp.utils.MessagesUtils.getLocalizedMessage;

public class ACSEndpointServlet extends HttpServlet {

    private static final Logger logger = Logger.getLogger(ACSEndpointServlet.class);
    public static int PUBLICATION_SESSION_VALIDITY_IN_HOUR = 24;

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) {
        try {
            logger.debug("Réception d'une réponse SAML d'un IdP...");
            Auth auth = new Auth(SamlRequest.getSettings(), request, response);
            auth.processResponse();

            // Si non identifié : retour 403
            if (!auth.isAuthenticated()) {
                logger.debug("Non identifié.");
                if (auth.isDebugActive()) {
                    logger.debug(auth.getLastResponseXML());
                }
                response.setStatus(403);
                return;
            } else {
                logger.debug("Identifié !");
                if (auth.isDebugActive()) {
                    logger.debug(auth.getLastResponseXML());
                }
            }

            // Si erreurs : on les journalise et on renvoie un message d'erreur
            List<String> errors = auth.getErrors();
            if (!errors.isEmpty()) {
                String errorsMessage = StringUtils.join(errors, ", ");
                logger.debug(errorsMessage);
                if (auth.isDebugActive()) {
                    String errorReason = auth.getLastErrorReason();
                    if (errorReason != null && !errorReason.isEmpty()) {
                        logger.debug(auth.getLastErrorReason());
                    }
                }
                throw new DetailedException("Erreur SAML :" + errorsMessage);
            }

            // Journalisation des attributs reçus
            Map<String, List<String>> attributes = auth.getAttributes();
            for (String attribute : attributes.keySet()) {
                logger.debug(attribute + "=" + attributes.get(attribute));
            }

            // La réponse doit contenir l'email
            String nameId = auth.getNameId();
            //                String nameIdFormat = auth.getNameIdFormat();
            //                String sessionIndex = auth.getSessionIndex();
            //                String nameidNameQualifier = auth.getNameIdNameQualifier();
            //                String nameidSPNameQualifier = auth.getNameIdSPNameQualifier();
            if (nameId == null || nameId.isEmpty()) {
                throw new DetailedException("Pas de nameId reçu !");
            }

            // Recherche de l'URL de redirection : relayState
            String relayState = request.getParameter("RelayState");
            if (relayState == null || relayState.isEmpty()) {
                if (attributes.isEmpty()) {
                    logger.debug("Pas d'attributs.");
                } else {
                    Collection<String> keys = attributes.keySet();
                    for (String name : keys) {
                        logger.debug(name);
                        List<String> values = attributes.get(name);
                        for (String value : values) {
                            logger.debug(" - " + value);
                        }
                    }
                }
                throw new DetailedException("Pas de RelayState reçu !");
            }

            // Traitement de la redirection : accès à une publication ou identification d'un auteur ?
            if (isPublicationUrl(relayState)) {
                // C'est une identification pour une consultation de publication
                redirectToPublication(request, response, relayState, nameId);

            } else {
                // C'est une identification pour un utilisateur de Thaleia

                // On identifie l'utilisateur dans Thaleia : a-t-il un compte et une licence valide ?
                boolean authenticated = ThaleiaSession.get().signIn(nameId, null);

                if (!authenticated) {
                    // L'utilisateur identifié par l'IDP n'a pas de compte valide dans Thaleia
                    String errorMessage = getLocalizedMessage("saml.user.identification.error",
                            ACSEndpointServlet.class, nameId);
                    logger.debug(errorMessage);
                    ThaleiaSession.get().addError(errorMessage);
                    response.sendRedirect(ThaleiaApplication.get().getApplicationRootUrl() + "/error");

                } else {
                    // Redirection sur l'URL RelayState
                    response.sendRedirect(relayState);
                }
            }


        } catch (Exception e) {
            logger.warn("Erreur durant le traitement de la réponse de l'IdP : " + e + "\n"
                    + LogUtils.getStackTrace(e.getStackTrace()));

            // Fabrication en envoi d'un message d'erreur en réponse
            try (PrintWriter writer = response.getWriter()) {
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'000'X");
                writer.println(
                        "<html><body><p>" + format.format(Calendar.getInstance().getTime()) + " : SSO access " + "error"
                                + ".</p></body></html>");
            } catch (IOException ex) {
                logger.info("Erreur durant la réponse au user agent : " + ex);
            }
        }
    }

    /**
     * @return true si cette URL est celle d'une publication Thaleia
     */
    private boolean isPublicationUrl(String relayState) throws DetailedException {
        if (relayState == null || relayState.isEmpty()) {
            throw new DetailedException("RelayState ne doit pas être nul !");
        }

        return relayState.substring(ThaleiaApplication.get().getApplicationRootUrl().length()).startsWith("/publish");
    }

    private void redirectToPublication(HttpServletRequest request, HttpServletResponse response, String relayState,
                                       String nameId) throws DetailedException {
        try {
            if (relayState == null || relayState.isEmpty()) {
                throw new DetailedException("RelayState ne doit pas être nul !");
            }
            if (nameId == null || nameId.isEmpty()) {
                throw new DetailedException("nameId ne doit pas être nul !");
            }

            // Recherche de la publication pour cette URL
            PublicationService publicationService = ThaleiaSession.get().getPublicationService();
            Publication publication = publicationService.getPublicationFromUrl(relayState);

            if (publication != null) {
                // Fabrication d'un publicationSession
                PublicationSessionDao publicationSessionDao = new PublicationSessionDao(ThaleiaSession.get().getContextService().getNewContext());
                PublicationSession publicationSession = publicationSessionDao.get();
                publicationSession.setPublication(publication);
                publicationSession.setUsername(nameId);
                publicationSession.setToken(Unique.getUniqueString(32));
                // La date de fin de validité de la publicationSession
                Calendar calendar = Calendar.getInstance();
                calendar.add(Calendar.HOUR_OF_DAY, PUBLICATION_SESSION_VALIDITY_IN_HOUR);
                publicationSession.setValidUntil(calendar.getTime());
                publicationSessionDao.save(publicationSession);

                // Stockage du publicationSession dans un attribut de session
                request.getSession().setAttribute(PublishPage.PUBLICATION_SESSION_ATTRIBUTE_NAME + "_"
                        + publication.getReference(), publicationSession);
                //ServletUtils.logSessionAttributes(request.getSession());

                response.sendRedirect(relayState);

            } else {
                throw new DetailedException("L'URL '" + relayState + "' ne correspond pas à une publication reconnue");
            }

        } catch (Exception e) {
            throw new DetailedException(e).addMessage("Impossible de rediriger la requête vers la publication : ");
        }
    }

}
