package fr.solunea.thaleia.webapp.pages.editcontent;

import org.apache.wicket.authroles.authorization.strategies.role.annotations.AuthorizeInstantiation;

import fr.solunea.thaleia.webapp.pages.BasePage;

@SuppressWarnings("serial")
@AuthorizeInstantiation("user")
public class EditContentPage extends BasePage {

	public EditContentPage() {
		super();
	}

}
