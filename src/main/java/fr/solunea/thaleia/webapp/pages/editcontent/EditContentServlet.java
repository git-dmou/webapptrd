package fr.solunea.thaleia.webapp.pages.editcontent;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

import fr.solunea.thaleia.model.User;
import fr.solunea.thaleia.utils.DetailedException;
import fr.solunea.thaleia.webapp.security.ThaleiaSession;

@SuppressWarnings("serial")
public class EditContentServlet extends HttpServlet {

	private static final Logger logger = Logger
			.getLogger(EditContentServlet.class);

	@Override
	protected void doPost(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {

		// Cas particulier : on ne peut pas suivre le traitement de la méthode
		// service, car on ne doit pas commencer à consommer la requête. On va
		// donc commencer le traitement Save, puis faire les vérifications
		// d'identification.

		try {
			SaveContentAction action = new SaveContentAction(
					this.getServletContext());
			action.run(request, response);

		} catch (DetailedException e) {
			AbstractEditContentAction.error(response, e.toString());
		}
	}

	@Override
	protected void doGet(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {

		logger.debug("Requête pour le EditContentService !");

		try {
			// Vérification de l'identification de l'appelant
			User user;
			try {
				user = ThaleiaSession.get().getAuthenticatedUser();
				if (user == null) {
					throw new Exception("");
				}

			} catch (Exception e) {
				throw new Exception(
						"L'utilisateur n'est pas identifié. Merci d'ouvrir une session.");
			}

			// Recherche de l'action demandée
			AbstractEditContentAction action = AbstractEditContentAction
					.getInstance(request, this.getServletContext());

			if (action == null) {
				throw new DetailedException(
						"Aucune action n'a été demandée. Vérifiez les paramètres de votre requête.");
			} else {
				logger.debug("Exécution de l'action...");
				action.run(request, response);
			}

			logger.debug("L'action a été exécutée.");

		} catch (Exception e) {
			AbstractEditContentAction.error(response, e.toString());
		}

	}

}
