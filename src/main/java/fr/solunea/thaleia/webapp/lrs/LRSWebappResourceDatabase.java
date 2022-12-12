package fr.solunea.thaleia.webapp.lrs;

import fr.solunea.thaleia.model.Publication;
import fr.solunea.thaleia.service.utils.SQLUtils;
import fr.solunea.thaleia.utils.DetailedException;
import fr.solunea.thaleia.webapp.ThaleiaApplication;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.json.JSONObject;

import java.io.File;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Une table aicc_tca_data qui se trouve dans la ressource de la webapp utilisée par Cayenne.
 */
public class LRSWebappResourceDatabase implements ILRSDatabase {

    protected static final Logger logger = Logger.getLogger(LRSWebappResourceDatabase.class);

    @Override
    public String selectLastStatementAsString(String agent, String activity, String verb) throws DetailedException {

        // Préparation de la requête
        // On fait un PreparedStatement pour s'assurer que d'éventuelle
        // injection de SQL soit empêché par le driver.
        String selectStatement = "SELECT jsonobject, statement_ref, keydata FROM aicc_tca_data WHERE visible <>'false' ";

        boolean andAgent = false;
        if (agent != null) {
            selectStatement += "AND actor_mbox = ? ";
            andAgent = true;
        }
        boolean andActivity = false;
        if (activity != null) {
            selectStatement += "AND activity_id = ? ";
            andActivity = true;
        }
        if (verb != null) {
            selectStatement += "AND verb = ? ";
        }

        selectStatement += "ORDER BY keydata DESC LIMIT 1";

        PreparedStatement prepStmt;
        Connection connection = null;
        try {
            connection = getConnection();
            prepStmt = connection.prepareStatement(selectStatement);
        } catch (SQLException e) {
            SQLUtils.closeConnection(connection);
            throw new DetailedException(e).addMessage("Impossible de préparer le statement SQL de sélection du dernier statement xAPI.");
        }

        try {
            if (agent != null) {
                prepStmt.setString(1, agent);
            }
            if (activity != null) {
                // Si pas de condition sur agent, alors activity est la première
                // condition
                if (!andAgent) {
                    prepStmt.setString(1, activity);
                } else {
                    // Sinon, c'est la deuxième condition
                    prepStmt.setString(2, activity);
                }
            }
            if (verb != null) {
                // Si pas de condition sur agent ni activity, alors verb est la
                // première condition
                if (!andAgent && !andActivity) {
                    prepStmt.setString(1, verb);
                } else {
                    // Si agent ET activity, alors verb est la 3ème condition
                    if (andAgent && andActivity) {
                        prepStmt.setString(3, verb);
                    } else {
                        // Si SOIT agent SOIT activity, alors verb est la 2ème
                        // condition
                        prepStmt.setString(2, activity);
                    }
                }
            }
        } catch (SQLException e) {
            throw new DetailedException(e).addMessage("Impossible de fixer les filtres du statement SQL de sélection du dernier statement xAPI.");
        }

        ResultSet resultSet;
        try {
            resultSet = prepStmt.executeQuery();
        } catch (SQLException e) {
            throw new DetailedException(e).addMessage("Impossible d'exécuter la requête SQL de sélection du dernier statement xAPI.");
        }

        StringBuilder result = new StringBuilder();
        try {
            ResultSetMetaData rsmd = resultSet.getMetaData();
            while (resultSet.next()) {
                // Pour toutes les colonnes
                for (int column = 1; column <= rsmd.getColumnCount(); column++) {
                    String cell = resultSet.getString(column);
                    result.append(cell).append(" ");
                }
                result.append("\n");
            }
        } catch (SQLException e) {
            throw new DetailedException(e).addMessage("Impossible d'interpréter le résultat de la requête SQL de sélection du dernier statement xAPI.");
        }

        // Fermeture de la connexion
        try {
            connection.close();
        } catch (SQLException e) {
            throw new DetailedException(e).addMessage("Impossible de fermer la connexion après recherche d'un statement xAPI.");
        }

        return result.toString();
    }

    private Connection getConnection() throws DetailedException {
        return ThaleiaApplication.get().getConfiguration().getNewConnection();
    }

    @Override
    public void insertStatement(String lrsUserId, String lrsUserPassword, String idRef, String timeRec, String jsonObject, String mBox, String verb, String activity) throws DetailedException {

        if (lrsUserId == null) {
            throw new DetailedException("lrsUserId ne doit pas être nul !");
        }
        if (lrsUserPassword == null) {
            throw new DetailedException("lrsUserPassword ne doit pas être nul !");
        }
        if (idRef == null) {
            throw new DetailedException("idRef ne doit pas être nul !");
        }
        if (timeRec == null) {
            throw new DetailedException("timeRec ne doit pas être nul !");
        }
        if (jsonObject == null) {
            throw new DetailedException("jsonObject ne doit pas être nul !");
        }
        if (mBox == null) {
            throw new DetailedException("mBox ne doit pas être nul !");
        }
        if (verb == null) {
            throw new DetailedException("verb ne doit pas être nul !");
        }
        if (activity == null) {
            throw new DetailedException("activity ne doit pas être nul !");
        }

        // Préparation de la requête
        // On fait un PreparedStatement pour s'assurer que d'éventuelle injection de SQL soit empêché par le driver.
        String statement = "INSERT INTO aicc_tca_data (aerouserid, aerouserpwd, data_type, date_transmission, jsonobject, querystring, statement_ref, actor_mbox, verb_id ,activity_id, visible ) VALUES ( ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'true' )";
        // On ne précise pas keydata, car son incrémentation est gérée automatiquement coté base de données.

        PreparedStatement prepStmt;
        Connection connection = null;
        try {
            connection = getConnection();
            prepStmt = connection.prepareStatement(statement, Statement.RETURN_GENERATED_KEYS);
        } catch (SQLException e) {
            SQLUtils.closeConnection(connection);
            throw new DetailedException(e).addMessage("Impossible de préparer le statement SQL d'insertion d'un statement xAPI.");
        }

        try {
            prepStmt.setString(1, lrsUserId);
            prepStmt.setString(2, lrsUserPassword);
            prepStmt.setString(3, "tincan");
            prepStmt.setString(4, timeRec);
            prepStmt.setString(5, jsonObject);
            prepStmt.setString(6, "");
            prepStmt.setString(7, idRef);
            prepStmt.setString(8, mBox);
            prepStmt.setString(9, verb);
            prepStmt.setString(10, activity);

        } catch (SQLException e) {
            throw new DetailedException(e).addMessage("Impossible de fixer les filtres du statement SQL d'insertion d'un statement xAPI.");
        }

        try {
            prepStmt.executeUpdate();
        } catch (SQLException e) {
            throw new DetailedException(e).addMessage("Impossible d'exécuter la requête SQL d'insertion d'un statement xAPI.");
        }

        // Fermeture de la connexion
        try {
            connection.close();
        } catch (SQLException e) {
            throw new DetailedException(e).addMessage("Impossible de fermer la connexion après insertion d'un statement xAPI.");
        }

    }

    @Override
    public String exportAllDataAsJson(String lrsUserId) throws DetailedException {

        if (lrsUserId == null) {
            throw new DetailedException("lrsUserId ne doit pas être nul !");
        }

        // Préparation de la requête
        // On fait un PreparedStatement pour s'assurer que d'éventuelle
        // injection de SQL soit empêché par le driver.
        String selectStatement = "SELECT statement_ref, date_transmission, jsonobject " + "FROM aicc_tca_data WHERE visible != 'false' AND " + "data_type = 'tincan' AND " + "aerouserid = ? ORDER BY keydata DESC";

        PreparedStatement prepStmt;
        Connection connection = null;
        try {
            connection = getConnection();
            prepStmt = connection.prepareStatement(selectStatement);
        } catch (SQLException e) {
            SQLUtils.closeConnection(connection);
            throw new DetailedException(e).addMessage("Impossible de préparer le statement SQL d'export des statements xAPI.");
        }

        try {
            prepStmt.setString(1, lrsUserId);
        } catch (SQLException e) {
            throw new DetailedException(e).addMessage("Impossible de fixer les filtres du statement SQL d'export des statements xAPI.");
        }

        ResultSet resultSet;
        try {
            resultSet = prepStmt.executeQuery();
        } catch (SQLException e) {
            throw new DetailedException(e).addMessage("Impossible d'exécuter la requête SQL d'export des statements xAPI.");
        }

        StringBuilder result = new StringBuilder("[");
        try {
            while (resultSet.next()) {
                // Pour chaque statement

                // On récupère l'objet JSON stocké en base
                String storedJsonAsString = resultSet.getString("jsonobject");

                // On fabrique un objet JSON
                JSONObject json = new JSONObject(storedJsonAsString);

                // On ajoute des attributs
                // RMAR : c'était dans impex.php de Gaia. Mais à quoi ça sert ?
                // Le problème, c'est qu'ils empêchent l'interpréteur JSON de
                // désérialiser l'objet comme un statement, donc pour l'instant
                // on commente.
                // On récupère l'identifiant
                // String statementRef = resultSet.getString("statement_ref");
                // // On récupère la date
                // String dateTransmission = resultSet
                // .getString("date_transmission");
                // json.append("id", statementRef);
                // json.append("stored", dateTransmission);

                result.append(json.toString(2)).append(",\n");
            }

            // On supprime le dernier ",\n" s'il existe
            if (result.toString().endsWith(",\n")) {
                result = new StringBuilder(result.substring(0, result.length() - 2));
            }

            result.append("]");

        } catch (SQLException e) {
            throw new DetailedException(e).addMessage("Impossible d'interpréter le résultat de la requête SQL d'export des statements xAPI.");
        }

        // Fermeture de la connexion
        try {
            connection.close();
        } catch (SQLException e) {
            throw new DetailedException(e).addMessage("Impossible de fermer la connexion d'export des statements xAPI.");
        }

        return result.toString();
    }

    @Override
    public void writeStatementInFile(List<Publication> publications, File file) throws DetailedException {

        if (publications == null) {
            throw new DetailedException("publications ne doit pas être nul !");
        }
        if (publications.isEmpty()) {
            // Rien à écrire
            return;
        }
        if (file == null) {
            throw new DetailedException("file ne doit pas être nul !");
        }

        // On récupère la référence des publications
        List<String> publicationReferences = new ArrayList<>();
        for (Publication publication : publications) {
            publicationReferences.add(publication.getReference());
        }

        // On veut fabriquer un paramètre de la forme :
        // values = "'valeur1', 'valeur2', 'valeur3'" (sans les ")
        String publicationWhere = "";
        for (String publicationReference : publicationReferences) {
            publicationWhere = publicationWhere + "activity_id LIKE '%/" + publicationReference + "' OR " +
                    "activity_id LIKE '%/" + publicationReference + "/%' OR ";
        }
        // On enlève le dernier OR
        publicationWhere = publicationWhere.substring(0, publicationWhere.length() - " OR ".length());

        // Préparation de la requête
        // On fait un PreparedStatement pour s'assurer que d'éventuelle
        // injection de SQL soit empêché par le driver.
        String selectStatement = "SELECT jsonobject FROM aicc_tca_data WHERE visible != 'false' AND data_type = 'tincan' AND " + publicationWhere + " ORDER BY keydata DESC";
        // logger.debug(selectStatement);

        PreparedStatement prepStmt;
        Connection connection = null;
        try {
            connection = getConnection();
            prepStmt = connection.prepareStatement(selectStatement);
        } catch (SQLException e) {
            SQLUtils.closeConnection(connection);
            throw new DetailedException(e).addMessage("Impossible de préparer le statement SQL d'export des statements xAPI.");
        }

        ResultSet resultSet;
        try {
            resultSet = prepStmt.executeQuery();
        } catch (SQLException e) {
            throw new DetailedException(e).addMessage("Impossible d'exécuter la requête SQL d'export des statements xAPI.");
        }

        try {
            int count = 0;
            while (resultSet.next()) {
                // Pour chaque statement

                // On récupère l'objet JSON stocké en base
                String storedJsonAsString = resultSet.getString("jsonobject");

                try {
                    // On fabrique un objet JSON
                    JSONObject json = new JSONObject(storedJsonAsString);
                    FileUtils.writeStringToFile(file, json.toString(2), "UTF-8", true);
                    count++;
                } catch (Exception e) {
                    // On n'arrête pas la suite du traitement.
                    logger.info("Impossible d'écrire dans un fichier l'objet JSON : " + storedJsonAsString);
                }
            }
            logger.debug(count + " statements exportés dans le fichier " + file.getAbsolutePath());

        } catch (SQLException e) {
            throw new DetailedException(e).addMessage("Impossible d'interpréter le résultat de la requête SQL d'export des statements xAPI.");
        }

        // Fermeture de la connexion
        try {
            connection.close();
        } catch (SQLException e) {
            throw new DetailedException(e).addMessage("Impossible de fermer la connexion d'export des statements xAPI.");
        }

    }
}
