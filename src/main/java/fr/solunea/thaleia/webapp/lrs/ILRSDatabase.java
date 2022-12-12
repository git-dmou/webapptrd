package fr.solunea.thaleia.webapp.lrs;

import java.io.File;
import java.util.List;

import fr.solunea.thaleia.model.Publication;
import fr.solunea.thaleia.utils.DetailedException;

/**
 * Un accès à une table contenant les enregistrements du LRS.
 * 
 */
public interface ILRSDatabase {

	/**
	 * Stocke dans ce fichier (en UTF-8) l'ensemble des statements (format JSON)
	 * qui concernent ces publications.
	 * 
	 * @param publications
	 * @param file
	 * @throws DetailedException
	 */
	void writeStatementInFile(List<Publication> publications, File file)
			throws DetailedException;

	/**
	 * @param lrsUserId
	 * @return une chaîne de caractères qui présente en JSON tous les statements
	 *         tincan visibles pour ce user.
	 * @throws DetailedException
	 */
	String exportAllDataAsJson(String lrsUserId) throws DetailedException;

	/**
	 * @param agent
	 *            si non null, alors on filtre sur ce user.
	 * @param activity
	 *            si non null, alors on filtre sur cette activité.
	 * @param verb
	 *            si non null, alors on filtre sur ce verbe.
	 * @return le dernier statement stocké.
	 * @throws DetailedException
	 */
	String selectLastStatementAsString(String agent, String activity,
			String verb) throws DetailedException;

	/**
	 * Insère un nouveau statement en base.
	 * 
	 * @param lrsUserId
	 *            l'identifiant du compte LRS utilisé
	 * @param lrsUserPassword
	 *            le mot de passe du compte LRS utilisé
	 * @param idRef
	 *            l'identifiant unique du statement, de type "ezLRS..."
	 * @param timeRec
	 *            l'horodate de réception de la requête
	 * @param jsonObject
	 *            l'objet JSON reçu en requête, encodé en BASE64
	 * @param mBox
	 *            l'adresse mail de l'acteur
	 * @param verb
	 *            le verbe
	 * @param activity
	 *            l'idenfiant de l'activité
	 */
	void insertStatement(String lrsUserId, String lrsUserPassword,
			String idRef, String timeRec, String jsonObject, String mBox,
			String verb, String activity) throws DetailedException;

}
