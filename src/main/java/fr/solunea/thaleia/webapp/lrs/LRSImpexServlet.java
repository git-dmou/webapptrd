package fr.solunea.thaleia.webapp.lrs;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import fr.solunea.thaleia.utils.DetailedException;
import fr.solunea.thaleia.webapp.AbstractServlet;

@SuppressWarnings("serial")
public class LRSImpexServlet extends AbstractServlet {

	private static final String MODE_PARAM_NAME = "modimpex";
	private static final String USERID_PARAM_NAME = "aerouserid";
	private static final String PASSWORD_PARAM_NAME = "aerouserpwd";

	@Override
	protected void doPost(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {

		logger.debug("Requête Post à l'import/export du LRS !");

		// logBody(request);
		// logHeaders(request);
		// logParameters(request);

		try {

			// Identification
			String login = getMandatoryParam(request, USERID_PARAM_NAME);
			String password = getMandatoryParam(request, PASSWORD_PARAM_NAME);
			if (Identificator.getInstance().loginForImpex(login, password)) {

				// Récupération du mode
				String mode = getMandatoryParam(request, MODE_PARAM_NAME);

				if ("exportAllData".equals(mode)) {

					String result = LRSDatabaseFactory.getDatabase()
							.exportAllDataAsJson(login);
					send(response, result, false);

				} else {
					throw new Exception("Mode d'import/export '" + mode
 + "' non implémenté.");
				}

			} else {
				throw new Exception("Identification refusée.");
			}

		} catch (Exception e) {
			logger.warn("Impossible de traiter la requête au LRS : " + e);

			// Présentation d'une erreur non détaillée
			error(response, "Erreur interne.");
		}

	}

	private String getMandatoryParam(HttpServletRequest request,
			String paramName) throws DetailedException {
		String result = request.getParameter(paramName);

		if (result == null) {
			throw new DetailedException("Le paramètre '" + paramName + "' est absent de la requête !");
		}

		return result;
	}
}
