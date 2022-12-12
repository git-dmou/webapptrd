package fr.solunea.thaleia.webapp.security;

import fr.solunea.thaleia.model.dao.UserDao;
import fr.solunea.thaleia.webapp.ThaleiaApplication;
import org.apache.wicket.authroles.authorization.strategies.role.Roles;

import fr.solunea.thaleia.model.User;

public class ThaleiaRoles {

	/**
	 * @return les rôles d'un anonyme
	 */
	public static Roles getAnonymousRoles() {
		return new Roles(Profiles.ANONYMOUS);
	}

	/**
	 * @return les rôles de cet utilisateur.
	 */
	public Roles getUserRoles(int authenticatedUserId) {

		// Tout le monde est au moins anonyme
		Roles result = new Roles(Profiles.ANONYMOUS);

		User user = new UserDao(ThaleiaApplication.get().contextService.getContextSingleton()).get(authenticatedUserId);
		if (user != null) {

			// S'il est admin du site
			if (user.getIsAdmin()) {
				result.add(Profiles.USER);
				result.add(Profiles.ADMIN);
			} else {
				// Sinon il n'est qye user
				result.add(Profiles.USER);
			}
		}

		return result;

	}

	public static class Profiles {

		public static final String ADMIN = "admin";
		public static final String USER = "user";
		public static final String ANONYMOUS = "anonymous";

	}
}
