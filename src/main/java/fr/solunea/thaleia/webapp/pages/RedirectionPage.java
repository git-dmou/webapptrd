package fr.solunea.thaleia.webapp.pages;

import org.apache.log4j.Logger;
import org.apache.wicket.authroles.authorization.strategies.role.annotations.AuthorizeInstantiation;

/**
 * Cette classe abstraite permet la redirection des pages en fonction des
 * paramètres de Configuration de Thaleia eg : L'utilisateur est identifié =>
 * Redirection
 *
 */
@SuppressWarnings("serial")
@AuthorizeInstantiation("user")
abstract public class RedirectionPage extends BasePage {

	protected static final Logger logger = Logger
			.getLogger(RedirectionPage.class);



}
