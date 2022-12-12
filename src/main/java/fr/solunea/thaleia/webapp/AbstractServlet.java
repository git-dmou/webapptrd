package fr.solunea.thaleia.webapp;

import java.io.IOException;
import java.util.Enumeration;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import fr.solunea.thaleia.webapp.lrs.LRSServlet;
import org.apache.log4j.Logger;
import org.apache.wicket.util.io.IOUtils;

@SuppressWarnings("serial")
public abstract class AbstractServlet extends HttpServlet {

	protected static final Logger logger = Logger.getLogger(AbstractServlet.class);

	@Override
	protected void doOptions(HttpServletRequest request, HttpServletResponse response) throws ServletException,
			IOException {
		super.doOptions(request, response);
	}

	/**
	 * Consomme le corps de la requête pour la tracer. Si la requête est de type
	 * POST, alors le corps ne sera plus disponible pour de futurs accès.
	 * 
	 * @param request
	 */
	protected void logBody(HttpServletRequest request) {
		try {
			String body = IOUtils.toString(request.getInputStream(), "UTF-8");
			logger.debug(body);

		} catch (IOException e) {
			logger.warn("Impossible de consommer le corps de la requête pour le journaliser : "
					+ e);
		}
	}

	/**
	 * Trace les paramètres de la requête.
	 * 
	 * @param request
	 */
	protected void logParameters(HttpServletRequest request) {
		logger.debug("Paramètres de la requête : ");
		Enumeration<String> parameterNames = request.getParameterNames();
		while (parameterNames.hasMoreElements()) {
			String parameterName = parameterNames.nextElement();
			String parameterValue = request.getParameter(parameterName);
			logger.debug("'" + parameterName + "'='" + parameterValue + "'");
		}
	}

	/**
	 * Trace les en-têtes de la requête.
	 * 
	 * @param request
	 */
	protected void logHeaders(HttpServletRequest request) {
		logger.debug("En-têtes de la requête : ");
		Enumeration<String> headerNames = request.getHeaderNames();
		while (headerNames.hasMoreElements()) {
			String headerName = headerNames.nextElement();
			String headerValue = request.getHeader(headerName);
			logger.debug("'" + headerName + "'='" + headerValue + "'");
		}
	}

	protected void error(HttpServletResponse response, String message) {
		// 500 error
		try {
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			response.getWriter().print(
					"<html><head><title>Error</title></head>");
			response.getWriter().print("<body>" + message + "</body>");
			response.getWriter().println("</html>");

		} catch (Exception e) {
			logger.warn("Le message d'erreur ne peut pas être renvoyé : " + e);
		}
	}

	/**
	 * @param response
	 *            où écrire la réponse
	 * @param message
	 *            le texte à écrire
	 * @param htmlHeader
	 *            doit-on écrire le texte dans du HTML ? Si oui, alors on ajoute
	 *            lse balises HTML, BODY, bla bla bla...
	 */
	protected void send(HttpServletResponse response, String message,
			boolean htmlHeader) {
		try {
			if (htmlHeader) {
				response.setStatus(HttpServletResponse.SC_OK);
				response.getWriter().print(
						"<html><head><title>Thaleia LRS</title></head>");
				response.getWriter().print("<body>" + message + "</body>");
				response.getWriter().println("</html>");
			} else {
				response.getWriter().print(message);
			}

		} catch (Exception e) {
			logger.warn("Le message ne peut pas être renvoyé : " + e);
		}
	}

}
