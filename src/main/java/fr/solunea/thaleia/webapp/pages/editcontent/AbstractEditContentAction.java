package fr.solunea.thaleia.webapp.pages.editcontent;

import fr.solunea.thaleia.utils.DetailedException;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.http.HttpHeaders;
import org.apache.log4j.Logger;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public abstract class AbstractEditContentAction {

    protected static final Logger logger = Logger.getLogger(AbstractEditContentAction.class);
    /**
     * Le nom du paramètre qui contient le nom de l'action demandée.
     */
    private static final String ACTION_NAME_PARAM = "do";

    /**
     * Les noms d'action existants
     */
    private static final String OPEN_ACTION = "open";
    private static final String CLOSE_ACTION = "close";
    private static final String DELETE_ACTION = "delete";
    private static final String HEADERS_ACTION = "headers";

    /**
     * @return l'objet de traitement de l'action demandée dans cette requête. Null si aucune action n'est demandée.
     */
    public static AbstractEditContentAction getInstance(HttpServletRequest request, ServletContext context) throws DetailedException {
        AbstractEditContentAction result = null;

        // Avant toute consommation de la requête, on vérifie si c'est une
        // Multipart. Si c'est le cas, on envoie directement sur le traitement
        // d'un upload.
        if (ServletFileUpload.isMultipartContent(request)) {
            logger.debug("Réception d'une requête multipart");
            return new SaveContentAction(context);
        }

        // Recherche du paramètre DO
        String doValue = request.getParameter(ACTION_NAME_PARAM);
        if (doValue != null) {
            logger.debug("Réception d'une requête do=" + doValue);

            switch (doValue) {

                // Pas de recherche de do=save, car ce cas a été traité précédement
                // (c'est une requête Multipart).

                case OPEN_ACTION:
                    result = new OpenContentAction();
                    break;

                case CLOSE_ACTION:
                    result = new CloseContentAction();
                    break;

                case DELETE_ACTION:
                    result = new DeleteContentAction();
                    break;

                case HEADERS_ACTION:
                    result = new HeadersContentAction();
                    break;

                default:
                    logger.debug("Le paramètre " + ACTION_NAME_PARAM + "=" + doValue + " n'est pas reconnu !");
                    break;
            }
        } else {
            throw new DetailedException("Pas d'action demandée : le paramètre '" + ACTION_NAME_PARAM + "' est vide !");
        }

        return result;
    }

    protected void ok(HttpServletResponse response, String message) {
        // 200 ok
        try {
            response.setStatus(HttpServletResponse.SC_OK);
            response.setCharacterEncoding("UTF-8");
            response.addHeader(HttpHeaders.CONTENT_TYPE, "application/json");
            response.getWriter().println(message);

        } catch (Exception e) {
            logger.warn("Le message de retroue ne peut pas être renvoyé : " + e);
        }
    }

    /**
     *
     */
    public abstract void run(HttpServletRequest request, HttpServletResponse response) throws DetailedException;

    /**
     * Vérifie si la valeur est nulle. Si c'est le cas, envoie une exception
     * avec un message indiquant que ce paramètre est nul.
     */
    protected void exceptionIfNull(String value, String paramName) throws DetailedException {
        if (value == null) {
            throw new DetailedException("Le paramètre '" + paramName + "' n'est pas défini !");
        }
    }

    /**
     * Envoie un message d'erreur en réponse.
     */
    public static void error(HttpServletResponse response, String message) {
        // 500 error
        try {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.setCharacterEncoding("UTF-8");
            response.getWriter().print("<html><head><title>Error</title><meta http-equiv=\"Content-Type\" "
                    + "content=\"text/html; charset=utf-8\"/></head>");
            response.getWriter().print("<body>" + message + "</body>");
            response.getWriter().println("</html>");

        } catch (Exception e) {
            logger.warn("Le message d'erreur ne peut pas être renvoyé : " + e);
        }
    }

}
