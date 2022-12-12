package fr.solunea.thaleia.webapp.utils;

import fr.solunea.thaleia.webapp.security.ThaleiaSession;
import org.apache.wicket.Application;
import org.apache.wicket.authentication.IAuthenticationStrategy;

public class Signin {

    /**
     * On tente d'identifier l'utilisateur qui aurait déjà été identifié
     * précédemment, et qui dont les informations de connexion seraient chifrées dans un cookie.
     */
    public static void autoSignin(Application application) {

        if (!ThaleiaSession.get().isSignedIn()) {
            IAuthenticationStrategy authenticationStrategy = application.getSecuritySettings()
                    .getAuthenticationStrategy();
            // Récupération des données (login/mot de passe) dans le cookie LoggedIn sur le poste client.
            String[] data = authenticationStrategy.load();
            if ((data != null) && (data.length > 1)) {
                // On tente l'identification. Si ok, alors c'est fait pour la
                // suite, sinon on efface les données stockées pour cet
                // utilisateur.
                if (!ThaleiaSession.get().signIn(data[0], data[1])) {
                    authenticationStrategy.remove();
                }
            }
        }
    }
}
