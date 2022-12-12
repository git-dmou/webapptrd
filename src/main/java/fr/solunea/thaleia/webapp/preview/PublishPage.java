package fr.solunea.thaleia.webapp.preview;

import fr.solunea.thaleia.model.ApiToken;
import fr.solunea.thaleia.model.Publication;
import fr.solunea.thaleia.model.PublicationRecipient;
import fr.solunea.thaleia.model.PublicationSession;
import fr.solunea.thaleia.model.dao.ApiTokenDao;
import fr.solunea.thaleia.service.LicenceService;
import fr.solunea.thaleia.service.PublicationService;
import fr.solunea.thaleia.service.events.EventNotificationService;
import fr.solunea.thaleia.service.utils.Configuration;
import fr.solunea.thaleia.service.utils.UrlUtils;
import fr.solunea.thaleia.utils.DetailedException;
import fr.solunea.thaleia.webapp.ThaleiaApplication;
import fr.solunea.thaleia.webapp.security.ThaleiaSession;
import org.apache.log4j.Logger;
import org.apache.wicket.request.Url.QueryParameter;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.request.flow.RedirectToUrlException;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.StringTokenizer;

/**
 * Analyse une URL du type http://localhost:8080/thaleia/publish/1385644403694/toto.html pour renvoyer le binaire
 * correspondant, retrouvé dans les fichiers dans le répertoire de publication : demande au PublishService le répertoire
 * local où rechercher la resource [répertoire local du PublishService]/1385644403694/toto.html
 */
@SuppressWarnings("serial")
public class PublishPage extends AbstractPublisherPage {

    // Le nom de l'attribut de session dans lequel on va stocker l'objet publicationSession
    public static final String PUBLICATION_SESSION_ATTRIBUTE_NAME = "publicationSession";
    protected static final Logger logger = Logger.getLogger(PublishPage.class);
    /**
     * Dans une requête, le nom du paramètre qui contient l'email associé au recipient utilisé pour l'accès à ce
     * contenu.
     */
    private static final String RECIPIENT_EMAIL_PARAMNAME = "email";
    private static final String RECIPIENT_NAME_PARAMNAME = "name";
    private static final String EMAIL_REQUIRED_PARAMNAME = "emailrequired";
    private static final String TOKEN_PARAMNAME = "token";

    @Override
    protected File getResource(String resourceName) throws DetailedException {
        try {
            // On reconstruit le chemin complet du fichier demandé
            PublicationService previewService = ThaleiaSession.get().getPublicationService();
            return new File(previewService.getLocalDir().getAbsolutePath() + resourceName);

        } catch (SecurityException e) {
            throw new DetailedException(e).addMessage(
                    "Impossible d'obtenir l'accès à la ressource '" + resourceName + "'.");
        }
    }

    /**
     * @return true si l'URL est une demande à la ressource, mais pas à un des fichiers de cette ressource. Par exemple
     * : http://server:port/thaleia/preview/abc/123 renvoie true, et http://server:port/thaleia/preview/abc/123/index
     * .html renvoie false
     */
    private boolean isResourceRequestOnly() {
        // On recherche dans l'URL la ressource qui est à renvoyer
        // L'URI pour http://server:port/thaleia/preview/abc/123
        // est a "preview/abc/123"
        String url = RequestCycle.get().getRequest().getUrl().toString();

        // On supprime un éventuel / final
        if (url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }

        // Si c'est un appel à une ressource, on ne doit avoir qu'un seul / :
        // publish/12345
        // Et donc strictement deux tokens
        StringTokenizer tokenizer = new StringTokenizer(url, "/");
        boolean result;
        result = tokenizer.countTokens() == 2;
        // logger.debug(url + " est une requête pour une ressource ? = " +
        // result);
        return result;
    }

    @Override
    protected CHECK_RENDER_RESULT renderAuthorized() {

        try {
            // L'URL appelée, de type http://server:port/thaleia/preview/123?param=value
            String applicationRootUrl = ThaleiaApplication.get().getApplicationRootUrl();
            if (applicationRootUrl.endsWith("/")) {
                applicationRootUrl = applicationRootUrl.substring(0, applicationRootUrl.length() - 1);
            }
            String clientUrl = applicationRootUrl + "/" + RequestCycle.get().getRequest().getClientUrl().toString();
            //logger.debug("URl client =" + clientUrl);

            // On recherche la Publication demandée.
            PublicationService publicationService = ThaleiaSession.get().getPublicationService();
            Publication publication = publicationService.getPublicationFromUrl(clientUrl);

            if (publication == null) {
                return CHECK_RENDER_RESULT.NO_PUBLICATION;
            }

            // Si la ressource n'est pas active, on ne la présente pas
            if (!publication.getActive()) {
                logger.debug("La publication est désactivée.");
                return CHECK_RENDER_RESULT.DEACTIVATED;
            }

            // On s'assure que l'auteur a une licence valide
            LicenceService licenceService = ThaleiaApplication.get().getLicenceService();
            if (!licenceService.isUserValid(publication.getUser(), false)) {
                logger.debug("L'utilisateur n'a pas de licence valide.");
                return CHECK_RENDER_RESULT.NO_LICENCE;
            }

            // Si un mot de passe est défini pour cette ressource, on fait une
            // vérification de ce mot de passe.
            if (publication.getPassword() != null && publication.getPassword().length() > 0
                    && !PublishSecurity.isSignedIn(publication)) {
                // logger.debug("Un mot de passe est requis, et l'identification
                // n'a pas été faite (isSignedIn = "
                // + PublishSecurity.isSignedIn(publication) + ").");

                // On demande l'identification, puis si elle réussi le
                // retour sur l'URL courante.
                setResponsePage(new PublishLoginPage(publication, RequestCycle.get().getRequest().getUrl().toString()));

                // On ne rendra pas le binaire de la ressource
                return CHECK_RENDER_RESULT.REQUEST_PASSWORD;

            }

            // Si l'identification par SSO est demandée, on vérifie la présence d'un cookie
            boolean ssoActivated = ThaleiaApplication.get().getApplicationParameterDao().getValue(
                    "saml.publications.sso" + ".identification", "false").equalsIgnoreCase("true");
            if (ssoActivated) {
                logger.debug("Recherche d'une publicationSession valide...");
                //                ServletUtils.logSessionAttributes(((HttpServletRequest) getRequestCycle()
                //                .getRequest().getContainerRequest()).getSession());

                // Vérification du cookie : ce token de session existe-t-il pour cette publication, et est-il valide ?
                boolean noValidToken = true;
                //                String publicationSessionToken = (String) ThaleiaSession.get().getAttribute(
                //                        PublishPage.PUBLICATION_SESSION_ATTRIBUTE_NAME + "_" + publication
                //                        .getReference());
                PublicationSession publicationSession =
                        (PublicationSession) ((HttpServletRequest) getRequest().getContainerRequest()).getSession().getAttribute(
                                PublishPage.PUBLICATION_SESSION_ATTRIBUTE_NAME + "_" + publication.getReference());
                if (publicationSession != null && publicationSession.isValid()) {
                    logger.debug("Trouvé la publicationSession valide : token =" + publicationSession.getToken());
                    noValidToken = false;
                } else {
                    logger.debug("Pas de publicationSession valide.");
                }

                if (noValidToken) {
                    logger.debug("Redirection vers le SSO pour création d'une publicationSession.");
                    // On redirige vers le SSO, qui se chargera d'écrire un cookie en cas de succès
                    // On demande une identification par SSO avec un relayState de type :
                    // https://serveur/instance/publication/123456?relayStateToken=XXXX
                    throw new RedirectToUrlException(
                            ThaleiaApplication.get().getApplicationRootUrl() + "/gosso?relayState="
                                    + URLEncoder.encode(clientUrl, StandardCharsets.UTF_8));
                } else {
                    // On écrit un cookie PUBLICATIONSESSION = publicationSession.token
                    Cookie cookie = new Cookie("PublicationSession", publicationSession.getToken());
                    cookie.setSecure(true);
                    cookie.setHttpOnly(false);
                    cookie.setPath("/");
                    ((HttpServletResponse) getResponse().getContainerResponse()).addCookie(cookie);
                }

                // On peut passer à la suite
            }

            // On récupère un éventuel PublicationRecipient qui est associé à
            // cette url : deux étapes

            // 1ere étape : on compare cette URL avec les URL des recipients
            // existants. Par exemple :
            // publish/1402416355073?recipient=24a70f43858b4ce19f21
            PublicationRecipient recipient =
                    ThaleiaApplication.get().getPublicationService().getRecipientByUrlEnding(clientUrl);
            //logger.debug("Trouvé le recipient : " + recipient);

            // 2eme étape, on vérifie si par hasard on n'aurait pas comme
            // paramètre un identifiant d'utilisateur, qui soit associé à un
            // recipient valide. Par exemple :
            // publish/1402416355073/DesignSystems.pdf?user=romain.marion@solunea.fr
            String email = null;
            String name = null;
            if (!ssoActivated) {
                QueryParameter emailParam =
                        RequestCycle.get().getRequest().getClientUrl().getQueryParameter(RECIPIENT_EMAIL_PARAMNAME);
                email = null;
                if (emailParam != null) {
                    email = UrlUtils.decode(emailParam.getValue());
                }
                //logger.debug("Trouvé l'email : " + email);

                QueryParameter nameParam =
                        RequestCycle.get().getRequest().getClientUrl().getQueryParameter(RECIPIENT_NAME_PARAMNAME);
                name = null;
                if (nameParam != null) {
                    name = nameParam.getValue();
                }
                //logger.debug("Trouvé le nom de l'utilisateur : " + name);

                if (email != null && recipient == null) {
                    // On vérifie s'il existe une url privée pour cet email et pour
                    // cette publication
                    recipient = ThaleiaApplication.get().getPublicationService().getRecipient(publication, email);
                    //logger.debug("Trouvé le recipient correspondant à cet email : " + recipient);

                    // Si l'accès public est désactivé ET que cet email ne fait pas
                    // partie de la liste des URL privée, alors on refuse l'accès
                    if (!publication.getPublicAccess() && recipient == null) {
                        logger.debug("L'accès public est désactivé, et le paramètre " + RECIPIENT_EMAIL_PARAMNAME + "="
                                + email + " ne correspond à aucun recipient pour cette publication.");
                        return CHECK_RENDER_RESULT.PUBLIC_ACCESS_DEACTIVATED;
                    }

                    // Si cet email ne fait pas partie des URL privées, cela veut
                    // dire que c'est une URL publique qui a été ouverte, que la
                    // personne a renseigné son email, et que la page s'est
                    // rechargée avec cette information.
                }
            }
            // logger.debug("Recipient trouvé pour cette requête : " +
            // recipient);

            // Si un recipient a été trouvé, et que l’email associé n’a pas
            // encore ouvert cette publication, alors on s’assure que la licence
            // de l’auteur de la publication permet ce nouvel accès d’un
            // utilisateur à la publication.
            if (recipient != null && noStatData(recipient.getEmail(), publication)) {
                if (licenceService.noRegistrationsAvailable(publication.getUser())) {
                    logger.info("La licence de l'utilisateur ne permet plus de nouvelles inscriptions.");
                    ThaleiaApplication.get().getEventNotificationService().notify(EventNotificationService.Event.EVENT_REGISTRATION_CREDIT_REQUIRED.toString(), publication);
                    return CHECK_RENDER_RESULT.NO_NEW_REGISTRATION_LEFT;
                }
            }

            // S’il n’y avait pas de recipient, mais que l’email a été trouvé,
            // cela veut dire qu’il s’agit d’une URL publique qui a été ouverte,
            // et qui s’est ouverte à nouveau après avoir demandé son email à
            // l’utilisateur. Si cet email n’est pas encore associé à une
            // consultation de cette publication, on s’assure alors que la
            // licence de l’auteur de la publication permet ce nouvel accès d’un
            // utilisateur à la publication.
            if (recipient == null && email != null && noStatData(email, publication)) {
                if (licenceService.noRegistrationsAvailable(publication.getUser())) {
                    logger.info("La licence de l'utilisateur ne permet plus de nouvelles inscriptions.");
                    ThaleiaApplication.get().getEventNotificationService().notify(EventNotificationService.Event.EVENT_REGISTRATION_CREDIT_REQUIRED.toString(), publication);
                    return CHECK_RENDER_RESULT.NO_NEW_REGISTRATION_LEFT;
                }
            }

            // Si on a demandé uniquement la ressource, par exemple :
            // https://server:port/application/publish/12345
            // alors on ajoute le point d'entrée par défaut de la resource
            // demandée
            if (isResourceRequestOnly()) {
                // Si l'accès public est désactivé, alors on doit avoir retrouvé
                // un recipient pour autoriser l'accès.
                // On ne fait ce test que pour le point d'entrée principal, car
                // on ne peut pas assurer que tous les appels après le point
                // d'entrée seront bien associés par un paramètre au recipient
                // ou à son email.
                if (!publication.getPublicAccess() && recipient == null) {
                    logger.info("L'accès public est désactivé, et aucun recipient valide n'a été retrouvé dans la "
                            + "requête.");
                    return CHECK_RENDER_RESULT.PUBLIC_ACCESS_DEACTIVATED;
                }

                // String url = RequestCycle.get().getRequest().getUrl()
                // .toString()
                // + "/" + publication.getLauncher();
                String url = publication.getReference() + "/" + publication.getLauncher();

                // Prise en compte de la situation où ne veut pas demander son email au lancement de la publication, sur
                // certaines instances.
                // ATTENTION : ceci peut perturber un décompte correct de la consommation des accès aux publications,
                // par rapport à ce que permet la licence d'un utilisateur Thaleia.
                if ("false".equals(ThaleiaApplication.get().getConfiguration().getDatabaseParameterValue(Configuration.PUBLICATIONS_ACCESS_REQUIRE_EMAIL, "true").toLowerCase())) {
                    // On ajoute l'email "anonymous"
                    email = "anonymous";
                    url = UrlUtils.addParamToUrl(url, EMAIL_REQUIRED_PARAMNAME, "false");
                }

                // Si un email a été trouvé, mais pas de recipient, on l'ajoute
                url = UrlUtils.addEncodedParamToUrl(url, RECIPIENT_EMAIL_PARAMNAME, email);

                // Si un nom a été trouvé, on l'ajoute
                url = UrlUtils.addEncodedParamToUrl(url, RECIPIENT_NAME_PARAMNAME, name);


                // Si un recipient a été trouvé pour cette requête, on ajoute
                // son adresse email
                // Cela permet de transmettre l'email d'un recipient au point
                // d'entrée du contenu. Si c'est un document, tant pis, mais cet
                // email peut être interprété par le lecteur SCORM pour
                // retrouver l'email automatiquement.
                if (recipient != null) {
                    url = UrlUtils.addEncodedParamToUrl(url, RECIPIENT_EMAIL_PARAMNAME, recipient.getEmail());
                }

                // On fabrique un token d'autorisation pour les appels anonymes à l'API Thaleia
                ApiTokenDao apiTokenDao = new ApiTokenDao(ThaleiaSession.get().getContextService().getNewContext());
                // Token anonyme de 24 heures
                ApiToken token = apiTokenDao.generate(null, 86400);
                apiTokenDao.save(token);
                url = UrlUtils.addEncodedParamToUrl(url, TOKEN_PARAMNAME, token.getValue());

                logger.trace("Redirection vers " + url);

                // On redirige sur l'URL avec le point d'entrée
                throw new RedirectToUrlException(ThaleiaApplication.get().getPublishUrl() + "/" + url);
                //                throw new NonResettingRestartException(ThaleiaApplication.get().getPublishUrl() +
                //                "/" + url);
            }

        } catch (DetailedException e) {
            // On ne rendra pas le binaire de la ressource
            logger.info("Impossible d'analyser les droits d'accès à la requête de visualisation '"
                    + RequestCycle.get().getRequest().getUrl().toString() + "' : " + e);
            return CHECK_RENDER_RESULT.ERROR;

        }

        return CHECK_RENDER_RESULT.OK;
    }

    /**
     * @return false s'il existe au moins une donnée d'accès pour cette publication et cet email
     */
    private boolean noStatData(String email, Publication publication) {
        boolean result =
                ThaleiaApplication.get().getStatDataDao().getFirstStatData(email, Arrays.asList(publication)) != null;
        logger.debug("Existe-t-il des données pour l'email '" + email + "' et la publication " + publication + " ? "
                + result);
        return !result;
    }
}
