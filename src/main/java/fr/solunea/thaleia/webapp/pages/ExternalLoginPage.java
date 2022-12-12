package fr.solunea.thaleia.webapp.pages;

import org.apache.log4j.Logger;
import org.apache.wicket.authroles.authentication.AuthenticatedWebSession;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import fr.solunea.thaleia.service.utils.Configuration;
import fr.solunea.thaleia.webapp.ThaleiaApplication;
import fr.solunea.thaleia.webapp.security.ThaleiaSession;

@SuppressWarnings("serial")
public class ExternalLoginPage extends BasePage {

	protected static final Logger logger = Logger.getLogger(ExternalLoginPage.class.getName());

	public static final String USERNAME_PARAM = "username";
	public static final String PASSWORD_PARAM = "password";

	public ExternalLoginPage(PageParameters params) {

		try {
			// Récupération des valeurs du formulaire dans la requête
			String username = getRequest().getRequestParameters().getParameterValue(USERNAME_PARAM).toString("");
			String password = getRequest().getRequestParameters().getParameterValue(PASSWORD_PARAM).toString("");

			logger.debug("Tentative d'identification pour username=" + username + " password="
					+ password.replaceAll(".", "*"));

			boolean logon = AuthenticatedWebSession.get().signIn(username, password);

			if (logon) {
				// On trace l'identification pour les stats
				try {
					ThaleiaApplication.get().getStatsService().storeIdentificationEvent(
							ThaleiaSession.get().getAuthenticatedUser().getLogin(),
							ThaleiaSession.get().getAuthenticatedUser().getName(), true);
				} catch (Exception e) {
					logger.warn("Impossible de journaliser l'événement d'identification : " + e);
				}

				logger.debug("Identification réussie. Redirection vers la page par défaut de l'application.");
				// On redirige sur la page par défaut de l'application
				setResponsePage(
						ThaleiaApplication.get().getRedirectionPage(Configuration.AUTHENTIFIED_USERS_WELCOME_PAGE,
								Configuration.DEFAULT_AUTHENTIFIED_USERS_WELCOME_PAGE, Configuration.HOME_MOUNT_POINT));
			} else {
				// On trace l'identification pour les stats
				try {
					ThaleiaApplication.get().getStatsService().storeIdentificationEvent(username, "", false);
				} catch (Exception e) {
					logger.warn("Impossible de journaliser l'événement d'identification : " + e);
				}

				logger.debug("Identification refusée. Redirection vers la page de login manuel.");
				// On redirige sur la page de login de l'application
				setResponsePage(LoginPage.class);
			}

		} catch (Exception e) {
			logger.warn("Erreur de traitement d'une identification : " + e);
		}
	}

}
