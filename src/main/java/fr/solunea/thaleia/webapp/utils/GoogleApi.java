package fr.solunea.thaleia.webapp.utils;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import fr.solunea.thaleia.utils.DetailedException;
import fr.solunea.thaleia.webapp.ThaleiaApplication;
import fr.solunea.thaleia.webapp.pages.demo.FinalizeAccountPage;
import fr.solunea.thaleia.webapp.security.ThaleiaSession;
import org.apache.log4j.Logger;
import org.apache.wicket.RestartResponseException;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.MetaDataHeaderItem;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.util.string.StringValue;

import java.util.Collections;

public class GoogleApi {

    protected static final Logger logger = Logger.getLogger(GoogleApi.class.getName());

    /**
     * Ajoute dans le header de la page une balise de script qui pointe sur l'API Google.
     */
    public static void addGoogleApiScript(IHeaderResponse response) {
        // La ressource Javascript vers l'API Google
        //        response.render(JavaScriptReferenceHeaderItem.forReference(new UrlResourceReference(Url.parse(
        //                "https://apis" + ".google.com/js/platform.js?onload=renderButton")), null, null, true,
        // null, null));

        logger.debug("Connexion à l'API Google : " + isConfigured());
        response.render(MetaDataHeaderItem.forMetaTag("google-signin-client_id", getGoogleSigninClientId()));
    }

    /**
     * Renvoie true si l'API Google est correctement configurée
     */
    public static boolean isConfigured() {
        try {
            // Le paramètre meta qui indique l'ID de client pour l'API Google.
            if (getGoogleSigninClientId().isEmpty()) {
                //                logger.warn("Attention : la valeur du paramètre d'application
                //                'google-signin-client_id' n'a pas été "
                //                        + "fixée : " + "l'identification par Google Id ne sera pas possible !");
                return false;
            } else {
                return true;
            }
        } catch (Exception e) {
            logger.warn("Erreur durant la vérification de la connexion à l'API Google : " + e);
            return false;
        }
    }

    private static String getGoogleSigninClientId() {
        return ThaleiaApplication.get().getApplicationParameterDao().getValue("google" + "-signin-client_id", "");
    }

    /**
     * Analyse les paramètres, se connecte à l'API Google, valide le le token, et renvoie les données
     * correspondantes. Redirection vers la page d'accueil en cas d'erreur.
     */
    public static GoogleIdToken.Payload getPayloadFromParameters(PageParameters pageParameters) {
        // Connexion à l'API Google
        GoogleIdTokenVerifier verifier;
        try {
            HttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
            JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();

            String googleSigninClientId = getGoogleSigninClientId();
            if (googleSigninClientId.isEmpty()) {
                logger.warn("Attention : la valeur du paramètre d'application 'google-signin-client_id' n'a pas été "
                        + "fixée : " + "l'identification par Google Id ne sera pas possible !");
            }

            verifier = new GoogleIdTokenVerifier.Builder(httpTransport, jsonFactory)
                    // Specify the CLIENT_ID of the app that accesses the backend:
                    .setAudience(Collections.singletonList(googleSigninClientId))
                    // Or, if multiple clients access the backend:
                    //.setAudience(Arrays.asList(CLIENT_ID_1, CLIENT_ID_2, CLIENT_ID_3))
                    .build();
        } catch (Exception e) {
            logger.info("Impossible d'obtenir une connexion à l'API Google : " + e);
            // On redirige sur la page d'accueil de Thaleia.
            ThaleiaSession.get().error(MessagesUtils.getLocalizedMessage("invalid.code", FinalizeAccountPage.class,
                    (Object[]) null));
            throw new RestartResponseException(ThaleiaApplication.get().getHomePage());
        }


        // Récupération du token de l'identité Google de la personne concernée
        GoogleIdToken.Payload tokenPayload;
        try {
            // On récupère le code de vérification
            StringValue idTokenString = pageParameters.get("id");
            if (idTokenString == null) {
                throw new DetailedException("Le paramètre id est vide !");
            }

            // Vérification de la validité du token
            GoogleIdToken idToken = verifier.verify(idTokenString.toString());
            if (idToken != null) {
                tokenPayload = idToken.getPayload();
            } else {
                throw new Exception("Le token est nul !");
            }

        } catch (Exception e) {
            logger.info("Impossible d'obtenir un token valide : " + e);
            // On redirige sur la page d'accueil de Thaleia.
            ThaleiaSession.get().error(MessagesUtils.getLocalizedMessage("invalid.token", FinalizeAccountPage.class,
                    (Object[]) null));
            throw new RestartResponseException(ThaleiaApplication.get().getHomePage());
        }
        return tokenPayload;
    }
}
