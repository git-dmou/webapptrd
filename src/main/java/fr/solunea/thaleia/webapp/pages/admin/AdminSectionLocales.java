package fr.solunea.thaleia.webapp.pages.admin;

import org.apache.wicket.authroles.authorization.strategies.role.annotations.AuthorizeInstantiation;
import org.apache.wicket.markup.html.panel.Panel;
import fr.solunea.thaleia.webapp.pages.admin.locales.LocalesPanel;

@SuppressWarnings("serial")
@AuthorizeInstantiation("admin")
public class AdminSectionLocales extends Panel {

	public AdminSectionLocales(String id) {
		super(id);

		add(new LocalesPanel("localesPanel"));

	}
}
