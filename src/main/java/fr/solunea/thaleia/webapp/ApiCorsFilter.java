package fr.solunea.thaleia.webapp;

import fr.solunea.thaleia.model.dao.ApplicationParameterDao;
import fr.solunea.thaleia.model.dao.ICayenneContextService;
import fr.solunea.thaleia.service.utils.Configuration;
import fr.solunea.thaleia.webapp.context.ApplicationContextService;
import org.apache.http.HttpHeaders;
import org.apache.log4j.Logger;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Permet d'autoriser les requêtes CORS pour les appels à l'api.
 */
public class ApiCorsFilter implements Filter {

    private static final Logger logger = Logger.getLogger(ApiCorsFilter.class);
    private String accessControlOrigin;

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        logger.info("Instanciation de l'accès au contexte de persistence...");
        String configurationLocation = filterConfig.getServletContext().getInitParameter(ThaleiaApplication.CAYENNE_CONFIGURATION_LOCATION_PARAM);
        // On va chercher le context-param
        // 'CAYENNE_CONFIGURATION_LOCATION_PARAM' dans le web.xml.
        // Ce paramètre contient le nom du fichier XML cayenne à rechercher.
        // Par exemple cayenne-ThaleiaDomain.xml
        // Ce runtime a vocation a être utilisé pour obtenir un contexte
        // Cayenne lié à l'application.
        ICayenneContextService contextService = new ApplicationContextService(configurationLocation);
        ApplicationParameterDao applicationParameterDao = new ApplicationParameterDao(contextService.getContextSingleton());

        // On effectue ces interrogations en base à l'initialisation de la webapp :
        // - avantage : on évite deux requêtes en base à chaque réception de requête HTTP
        // - inconvénient : il faut redémarrer l'application pour prendre en compte un changement de ces paramètres
        // La solution propre serait d'utiliser le cache de Cayenne, mais les premiers essais sont infructueux, car il semble
        // que les invalidations de cache dans le contexte Cayenne de ThaleiaApplication ne sont pas détectés dans ce contexte.
        // Or, du fait de l'ordre des filtres, on ne peut pas placer ce filtre avant celui de Wicket dans web.xml (sinon il intercepte les requête OPTIONS),
        // et donc on n'a pas accès simplement au même contexte Cayenne que l'application.

        String applicationRootHost = applicationParameterDao.getValue(Configuration.APPLICATION_PARAMETER_SERVER_URL, "");
        // applicationRootHost est du style https://server:port/instance ou https://server
        // et on ne veut garder que protocol://server:port/
        applicationRootHost = applicationRootHost.replaceAll("^((http[s]?):/)?/?([^:/\\s]+)(:[0-9]*)?((/\\w+)*/)([\\w\\-.]+[^#?\\s]+)(.*)?(#[\\w\\-]+)?$", "$2://$3$4$5");
        accessControlOrigin = applicationParameterDao.getValue("access.control.allow.origin", applicationRootHost);
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) {

        try {
            // Toutes les requêtes sont augmentées de ces en-têtes
            ((HttpServletResponse) response).addHeader("Access-Control-Allow-Origin", accessControlOrigin);
            ((HttpServletResponse) response).addHeader("Access-Control-Allow-Methods", "GET, OPTIONS, PUT, POST, DELETE");
            ((HttpServletResponse) response).addHeader("Access-Control-Allow-Headers",
                    HttpHeaders.CONTENT_TYPE + "," + HttpHeaders.AUTHORIZATION + ","
                            + "Origin,Access-Control-Allow-Origin,Password,User");

            // On répond systématiquement aux requêtes OPTIONS ainsi, et leur traitement s'arrête là.
            if (((HttpServletRequest) request).getMethod().equalsIgnoreCase("OPTIONS")) {
                ((HttpServletResponse) response).setStatus(HttpServletResponse.SC_ACCEPTED);
                ((HttpServletResponse) response).setHeader("Content-Length", "0");
                return;
            }

            // Pour les autres, on passe à la suite du traitement défini dans les autres filtres de la webapp.
            chain.doFilter(request, response);

        } catch (Exception e) {
            logger.warn(e);
        }
    }

    @Override
    public void destroy() {

    }
}
