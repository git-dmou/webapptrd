package fr.solunea.thaleia.webapp.preview;

import fr.solunea.thaleia.utils.DetailedException;
import fr.solunea.thaleia.webapp.ThaleiaApplication;
import fr.solunea.thaleia.webapp.pages.ErrorPage;
import fr.solunea.thaleia.webapp.security.ThaleiaSession;
import fr.solunea.thaleia.webapp.utils.MimeType;
import org.apache.catalina.connector.RequestFacade;
import org.apache.catalina.connector.ResponseFacade;
import org.apache.log4j.Logger;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.request.Response;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.request.http.WebResponse;
import org.apache.wicket.util.io.IOUtils;
import org.apache.wicket.util.time.Time;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.net.URLDecoder;
import java.util.StringTokenizer;

@SuppressWarnings("serial")
public abstract class AbstractPublisherPage extends WebPage {

    protected static final Logger logger = Logger.getLogger(AbstractPublisherPage.class);

    @Override
    protected void setHeaders(WebResponse response) {

        // super.setHeaders(response);
        // "no-cache, no-store," est nécessaire pour les WebPage
        // On ajoute "no-transform" pour éviter que Bouygues remplace les images
        // par des versions compressées
        final int CACHE_DURATION_IN_SECOND = 60 * 60 * 2; // 2 hours
        final long CACHE_DURATION_IN_MS = CACHE_DURATION_IN_SECOND * 1000;
        long now = System.currentTimeMillis();
        // res being the HttpServletResponse of the request
        // Mise en cache des ressources pour amélioré les performances
        response.addHeader("Cache-Control", "max-age=" + CACHE_DURATION_IN_SECOND);
        response.addHeader("Cache-Control", "no-transform");// optional
        response.setDateHeader("Last-Modified", Time.millis(now));
        response.setDateHeader("Expires", Time.millis(now + CACHE_DURATION_IN_MS));

        // Attention : si un serveur HTTPD est en frontal avec mod_jk, il peut
        // être nécessaire de lui ajouter en conf :
        // LoadModule headers_module modules/mod_headers.so
        // Header merge Cache-Control no-transform
    }

    @Override
    protected void onRender() {

        setHeaders((WebResponse) getResponse());
        // Journalisation des en-têtes de la requête
        RequestFacade requestFacade = (RequestFacade) this.getRequest().getContainerRequest();
        logger.debug("Réception de la requête pour prévisualisation : URL = "
                + RequestCycle.get().getRequest().getUrl().toString());

        logger.debug("file.encoding : " + System.getProperty("file.encoding"));
        //logger.debug("En-têtes de la requête : ");
//        for (String headerName : Collections.list(requestFacade.getHeaderNames())) {
//            if ("user-agent".equals(headerName) || "referer".equals(headerName)) {
//                logger.trace(headerName + " = " + requestFacade.getHeader(headerName));
//            }
//        }

        // L'URL demandée
        String url = RequestCycle.get().getRequest().getUrl().toString();
        // logger.debug("Url = " + url);

        // Problème de chargement d'un fichier SVG avec IE : IE fait 2 requêtes,
        // et la première peut bloquer le navigateur pendant 120 secondes. Voir
        // http://www.sitepoint.com/ie-contype-request/
        // Pour ces requêtes, on renvoie directement ce qui est attendu par IE :
        // une réponse vide avec uniquement l'en-tête :
        // Content-Type = image/svg+xml
        // On reconnaît ces requêtes car elles n'ont pas de referer, et on
        // demande un SVG.
        if (url.toLowerCase().endsWith(".svg") && requestFacade.getHeader("referer") == null) {
            ResponseFacade response = (ResponseFacade) RequestCycle.get().getResponse().getContainerResponse();
            response.setContentType("image/svg+xml");
            return;
        }

        // Si l'autorisation de rendu a été acceptée :
        CHECK_RENDER_RESULT renderAuthorized = renderAuthorized();
        if (renderAuthorized == CHECK_RENDER_RESULT.OK) {

            // On recherche dans l'URL la ressource qui est à renvoyer
            // L'URI pour http://server:port/thaleia/preview/abc/123
            // est a preview/abc/123

            // On supprime d'éventuels paramètres
            if (url.contains("?")) {
                url = url.substring(0, url.indexOf("?"));
            }
            // logger.debug("Url sans paramètres = " + url);

            try {
                // On cherche à retrouver l'extension du fichier demandé dans
                // l'URL
                String mimeType = MimeType.parseMimeType(url);
//                logger.trace("Type MIME deviné : " + mimeType);

                // On récupère le nom de la resource demandée après le
                // "preview"
                StringBuilder resourceName = new StringBuilder();
                StringTokenizer tokenizer = new StringTokenizer(url, "/");
                if (tokenizer.countTokens() > 1) {
                    // On passe le nom de la servlet de prévisualisation
                    // ("preview"
                    // ou "publish", ou...) :
                    tokenizer.nextToken();
                    // Il nous reste le nom de la ressource
                    while (tokenizer.hasMoreTokens()) {
                        resourceName.append("/").append(tokenizer.nextToken());
                    }
                    // logger.debug("La ressource demandée est '" + resourceName
                    // + "'.");

                    // On supprime les paramètres de la requêtes du nom de la
                    // resource
                    if (resourceName.indexOf("?") != -1) {
                        resourceName = new StringBuilder(resourceName.substring(0, resourceName.lastIndexOf("?")));
                    }

                    // On traduit les caractères URLencodés
                    resourceName = new StringBuilder(URLDecoder.decode(resourceName.toString(), "UTF-8"));

                    respondResourceBinary(getResponse(), resourceName.toString(), mimeType);

                } else {
                    throw new DetailedException(
                            "L'URI de la requête '" + url + "' ne décrit pas une ressource à " + "prévisualiser !");
                }
            } catch (Exception e) {
                logger.warn(
                        "La requête de prévisualisation à l'URI '" + url + "' ne peut être traitée : " + e.toString());
            }
        } else if (renderAuthorized == CHECK_RENDER_RESULT.REQUEST_PASSWORD) {
            // Dans ce cas, il y a déjà eu un setResponsePage vers une
            // page d'identificatio au niveau du renderAuthorized(). Il ne faut
            // donc pas afficher de message d'erreur, mais laisser le traitement
            // normal s'effectuer.

        } else {
            // logger.debug("Le rendu du binaire demandé n'est pas autorisé.");

            try {
                String errorMessage = "prerender.error";
                switch (renderAuthorized) {
                    // Les cas REQUEST_PASSWORD et OK sont traités précédement
                    case DEACTIVATED:
                        errorMessage = "error.deactivated";
                        break;
                    case NO_NEW_REGISTRATION_LEFT:
                        errorMessage = "error.noregistrationleft";
                        break;
                    case NO_LICENCE:
                        errorMessage = "error.nolicence";
                        break;
                    case NO_PUBLICATION:
                        errorMessage = "error.nopublication";
                        break;
                    case PUBLIC_ACCESS_DEACTIVATED:
                        errorMessage = "error.publicaccessdeactivated";
                        break;
                    case ERROR:
                        errorMessage = "prerender.error";
                        break;
                }
                ThaleiaSession.get().error(new StringResourceModel(errorMessage, this, null).getString());
                setResponsePage(ErrorPage.class);

            } catch (Exception e) {
                logger.warn(e);
            }
        }

    }

    /**
     * Méthode exécutée avant le rendu de la page. Doit-on effectuer le rendu du
     * binaire demandé ?
     */
    protected abstract CHECK_RENDER_RESULT renderAuthorized();

    /**
     * Renvoie le binaire de cette ressource dans cette réponse.
     */
    private void respondResourceBinary(Response response, String resourceName, String mimeType) throws DetailedException {

        // On récupère le fichier demandé en fonction de la chaîne passée en
        // paramètre
        File resource = getResource(resourceName);

        // On fixe le type MIME
        if (mimeType != null) {
            ((WebResponse) response).setContentType(mimeType);
        }

        // Transmission du binaire
        FileInputStream is = null;
        try {
            ((WebResponse) response).setContentLength(resource.length());
            is = new FileInputStream(resource);
            IOUtils.copy(is, response.getOutputStream());
            logger.debug("comparaison : ");
            logger.debug("La ressource demandée '" + resource.getAbsolutePath());
            logger.debug("' pour l'URL '" + RequestCycle.get().getRequest().getUrl().toString() + "' ");

        } catch (FileNotFoundException e) {
            String url = RequestCycle.get().getRequest().getUrl().toString();

            logger.debug("La ressource demandée '" + resource.getAbsolutePath() + "' pour l'URL '" + url + "' "
                    + "n'existe pas !");

            ThaleiaSession.get().error(new StringResourceModel("not.found", this, null, new Object[]{url}).getString());
            setResponsePage(ThaleiaApplication.get().getApplicationSettings().getInternalErrorPage());

        } catch (Exception e) {
            logger.debug("La ressource demandée '" + resource.getAbsolutePath() + "' n'a pas pu être copiée !");

            ThaleiaSession.get().error(new StringResourceModel("copy.error", this, null).getString());
            setResponsePage(ThaleiaApplication.get().getApplicationSettings().getInternalErrorPage());

        } finally {
            IOUtils.closeQuietly(is);
            IOUtils.closeQuietly(response.getOutputStream());
        }

    }

    /**
     * @return le fichier qui correspond à la ressource identifiée par ce nom.
     */
    protected abstract File getResource(String resourceName) throws DetailedException;

    /**
     * Le résultat de la vérification de la possibilité du rendu de la page.
     */
    protected enum CHECK_RENDER_RESULT {
        /**
         * La ressource peut être rendue.
         */
        OK,
        /**
         * La publication demandée n'existe pas.
         */
        NO_PUBLICATION,
        /**
         * La publication a été désactivée.
         */
        DEACTIVATED,
        /**
         * L'accès public a été demandé, mais il est désactivé.
         */
        PUBLIC_ACCESS_DEACTIVATED,
        /**
         * La licence de l'auteur de la publication
         * ne permet plus de nouvelles inscriptions
         * à ses publications.
         */
        NO_NEW_REGISTRATION_LEFT,
        /**
         * La licence de l'auteur de la publication
         * a expiré.
         */
        NO_LICENCE,
        /**
         * L'accès à la ressource demande un mot de passe.
         */
        REQUEST_PASSWORD,
        /**
         * Une erreur interne a empêché la vérification.
         */
        ERROR
    }

}
