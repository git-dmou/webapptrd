package fr.solunea.thaleia.webapp.pages.plugins;

import fr.solunea.thaleia.webapp.pages.ThaleiaV6MenuPage;
import org.apache.wicket.authroles.authorization.strategies.role.annotations.AuthorizeInstantiation;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.StringResourceModel;

@SuppressWarnings("serial")
@AuthorizeInstantiation("user")
public class PluginsPage extends ThaleiaV6MenuPage {

	public PluginsPage() {
		super();

		add(new Label("pageLabel", new StringResourceModel("pageLabel", this,
				null)));

		add(new PluginsPanel("pluginsPanel"));
	}

	/**
	 * Définition de la classe à ajouter à la navbar.
	 */
	@Override
	protected void setNavbarClass() {
		navbarClass = "thaleia-plugins";
	}

	@Override
	protected Label getHomeButtonLabel() {
		return (Label) new Label("homeLinkLabel",
				new StringResourceModel("homeLinkLabel", this, null))
				.setEscapeModelStrings(false);
	}
}
