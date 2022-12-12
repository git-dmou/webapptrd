package fr.solunea.thaleia.webapp.security.saml;

import com.onelogin.saml2.settings.Saml2Settings;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
import java.util.List;

public class SPMetadataServlet extends HttpServlet {

    private static final Logger logger = Logger.getLogger(SPMetadataServlet.class);

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) {
        PrintWriter out = null;
        try {
            String metadata = SPMetadata.get();
            logger.debug("Métadonnées générées : \n" + metadata);
            List<String> errors = Saml2Settings.validateMetadata(metadata);
            out = response.getWriter();
            if (errors.isEmpty()) {
                out.println(metadata);
            } else {
                response.setContentType("text/html; charset=UTF-8");
                for (String error : errors) {
                    out.println("<p>" + error + "</p>");
                }
            }
        } catch (Exception e) {
            logger.warn("Erreur de génération des métadonnées SAML : " + e);

        } finally {
            IOUtils.closeQuietly(out);
        }
    }
}
