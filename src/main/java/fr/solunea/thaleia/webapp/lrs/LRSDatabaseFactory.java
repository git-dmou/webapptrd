package fr.solunea.thaleia.webapp.lrs;

/**
 * Fabrique d'accès à une table contenant les enregistrements du LRS.
 * 
 */
public class LRSDatabaseFactory {

	/**
	 * @return Renvoie un accès à une base de données aicc_tca_data, soit en
	 *         local (c'est à dire incluse dans la base qui contient les tables
	 *         utilsiées par Cayenne), soit ailleurs.
	 */
	public static ILRSDatabase getDatabase() {

		// Pour l'instant, seule la base interne est implémentée.
		return new LRSWebappResourceDatabase();
	}

}
