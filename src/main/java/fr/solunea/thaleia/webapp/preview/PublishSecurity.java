package fr.solunea.thaleia.webapp.preview;

import org.apache.wicket.Session;

import fr.solunea.thaleia.model.Publication;

public class PublishSecurity {

	private static String PARAMETER_PREFIX = "publication_";

	/**
	 * @return true si une autorisation d'accès a été définie pour cette
	 *         session.
	 */
	public static boolean isSignedIn(Publication publication) {
		if (Session.get().getAttribute(
				PARAMETER_PREFIX + publication.getReference()) != null) {
			return true;
		} else {
			return false;
		}
	}

	/**
	 * Enregistre cette autorisation d'accès en session.
	 */
	public static void signIn(Publication publication) {
		Session.get().setAttribute(
				PARAMETER_PREFIX + publication.getReference(), 1);
	}
}
