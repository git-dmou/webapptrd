package fr.solunea.thaleia.webapp.lrs;

import fr.solunea.thaleia.model.StatementProcessing;
import fr.solunea.thaleia.utils.DetailedException;
import fr.solunea.thaleia.webapp.AbstractServlet;
import fr.solunea.thaleia.webapp.ThaleiaApplication;
import fr.solunea.thaleia.xapi.model.Statement;
import org.apache.cayenne.ObjectContext;
import org.apache.wicket.util.crypt.Base64;
import org.apache.wicket.util.io.IOUtils;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * Implémentation Thaleia du LRS de Gaia.
 */
public class LRSServlet extends AbstractServlet {

    private static final String AUTHENTICATION_HEADER = "Authorization";

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) {
        logger.debug("Requête PUT au LRS !");

        // La lib xAPI passe par des PUT pour transmettre ses statements. On la  traite comme un POST
        doPost(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) {
        logger.debug("Requête POST au LRS !");

        try {
            // On trace les en-têtes de la requête
            //logHeaders(request);

            // On trace le corps de la requête
            // logBody(request);

            // On récupère les données d'identification
            String autorisationHeader = request.getHeader(AUTHENTICATION_HEADER);
            if (autorisationHeader == null) {
                // Pas de paramètre AUTHENTICATION_HEADER
                throw new Exception("Identification impossible. La valeur '" + AUTHENTICATION_HEADER + "' est vide.");
            }

            // La valeur AUTHENTICATION_HEADER existe.
            // Si elle commence par "Basic ", on supprime ce début pour ne
            // garder que la chaîne encodée en base64.
            if (autorisationHeader.startsWith("Basic ")) {
                autorisationHeader = autorisationHeader.substring("Basic ".length());
            }

            // Identification
            if (authentifyToLRS(autorisationHeader)) {
                // Ok : on va traiter la requête

                long now = Calendar.getInstance().getTimeInMillis();

                String idRef = "ezLRS" + now;

                SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                String timeRec = format.format(new Date(now));

                // Interprétation du corps de la requête comme objet JSON.
                JSONObject body;
                String bodyAsString = IOUtils.toString(request.getInputStream(), "UTF-8");
                try {
                    body = new JSONObject(bodyAsString);
                    logger.debug(body.toString(4));
                } catch (Exception e) {
                    throw new DetailedException(e).addMessage("Impossible d'interpréter le corps de " + "la requête "
                            + "comme un objet JSON. Corps reçu :\n" + bodyAsString);
                }

                // On extrait le login et le mot de passe utilisé por la
                // connection au LRS qui sont dans l'en-tête d'idenitification.
                String lrsUserId = getLrsUserId(autorisationHeader);
                String lrsUserPassword = getLrsUserPassword(autorisationHeader);

                // On fabrique un contexte Cayenne pour isoler les traitements et les commiter en base en cas de réussite.
                ObjectContext context = ThaleiaApplication.get().contextService.getNewContext();

                // On récupère les statementsProcessings à traiter, en fonction du compte utilisé pour identifier la
                // requête
                List<StatementProcessing> statementProcessings = ThaleiaApplication.get().getLRSService().getStatementProcessings(lrsUserId, context);

                //Traitement du statement avec les StatementProcessing
                boolean statementProcessed = false;
                for (StatementProcessing statementProcessing : statementProcessings) {
                    try {
                        if (statementProcessing.matchPattern(bodyAsString)) {
                            String consolidatedStatementBody = statementProcessing.consolidate(bodyAsString);
                            if (!statementProcessing.getDestinationUrl().equals("")) {
                                statementProcessing.forwardToDestination(consolidatedStatementBody, bodyAsString);
                            }
                            if (statementProcessing.getStoreResult()) {
                                Statement statement = ThaleiaApplication.get().getLRSService().parseStatement(consolidatedStatementBody);
                                ThaleiaApplication.get().getLRSService().saveForStats(statement, consolidatedStatementBody, context);
                            }
                            if (statementProcessing.getStoreRawResult()) {
                                Statement statement = ThaleiaApplication.get().getLRSService().parseStatement(bodyAsString);
                                ThaleiaApplication.get().getLRSService().saveForStats(statement, bodyAsString, context);
                            }
                            statementProcessed = true;
                        }
                    } catch (DetailedException e) {
                        // L'erreur d'exécution d'un statement n'empêche pas la suite du traitement
                        logger.warn("Erreur lors du traitement du statement : " + e);
                    }
                }

                //Verifie si le statement a déjà reçu un traitement avant de l'envoyer dans la base de données de Thaleia
                if (!statementProcessed) {
                    String mBox = body.getJSONObject("actor").getString("mbox");
                    String verb = body.getJSONObject("verb").getString("id");
                    String activity = body.getJSONObject("object").getString("id");
                    // Stockage du statement dans la table aicc_tca_data
                    LRSDatabaseFactory.getDatabase().insertStatement(lrsUserId, lrsUserPassword, idRef, timeRec, bodyAsString, mBox, verb, activity);

                    Statement statement = ThaleiaApplication.get().getLRSService().parseStatement(bodyAsString);
                    if (statement != null) {
                        ThaleiaApplication.get().getLRSService().saveForStats(statement, bodyAsString, context);
                    } else {
                        logger.info("Pas de stockage du statement en table stat_data : la chaîne n'est pas reconnue comme un statement xAPI.");
                    }
                }
                // Enregistrement en base
                context.commitChanges();

                // En réponse, on renvoie l'id de statement inséré.
                send(response, idRef, true);

            } else {
                logger.debug("Identification refusée : " + AUTHENTICATION_HEADER + "=" + autorisationHeader);
                throw new Exception("Identification refusée.");
            }

        } catch (Exception e) {
            logger.debug("Impossible de traiter la requête au LRS : " + e);

            // Présentation d'une erreur non détaillée
            error(response, "Erreur interne.");
        }
    }

    /**
     * @return le login codé dans le champ d'autorisation, que l'on considère être user:pwd encodé en base64.
     */
    private String getLrsUserPassword(String autorisation) throws DetailedException {
        String decoded;
        decoded = new String(Base64.decodeBase64(autorisation), StandardCharsets.UTF_8);

        if (decoded.contains(":")) {
            // login:password
            String result = decoded.substring(decoded.indexOf(":") + 1);

            // Il peut arriver que des espaces traînent après. Une erreur de
            // décodage Base64 ?
            result = result.trim();

            // logger.debug("Détection d'un mot de passe dans '" + autorisation
            // + "' = " + result);
            return result;

        } else {
            throw new DetailedException("La chaîne d'identification ne contient pas un ':'.");
        }
    }

    /**
     * @return le mot de passe codé dans le champ d'autorisation, que l'on considère être user:pwd encodé en base64.
     */
    private String getLrsUserId(String autorisation) throws DetailedException {
        String decoded;
        decoded = new String(Base64.decodeBase64(autorisation), StandardCharsets.UTF_8);

        if (decoded.contains(":")) {
            // login:password
            return decoded.substring(0, decoded.indexOf(":"));

        } else {
            throw new DetailedException("La chaîne d'identification ne contient pas un ':'.");
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) {
        logger.debug("Requête Get au LRS : ingorée.");
        // On ne répond pas aux requêtes GET, car le mécanisme de sécurisation n'est pas défini.
    }

    /**
     * @return true si le champ correspond à une chaîne "login:mot de passe" encodée en !base64, et que ce compte est
     * autorisé à accéder à l'API du LRS.
     * Les comptes autorisés sont :
     * - le compte défini dans les paramètres de l'application (lrs.api.account.login).
     * - les comptes des LRS externes (table lrs_endpoint).
     */
    private boolean authentifyToLRS(String autorisation) throws DetailedException {
        String login = getLrsUserId(autorisation);
        String password = getLrsUserPassword(autorisation);

        return Identificator.getInstance().loginForApi(login, password);

    }

}
