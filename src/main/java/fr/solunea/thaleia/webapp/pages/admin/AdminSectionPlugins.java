package fr.solunea.thaleia.webapp.pages.admin;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.authroles.authorization.strategies.role.annotations.AuthorizeInstantiation;
import org.apache.wicket.extensions.ajax.markup.html.IndicatingAjaxLink;
import org.apache.wicket.markup.html.panel.Panel;
import fr.solunea.thaleia.webapp.pages.plugins.PluginsPage;

@SuppressWarnings("serial")
@AuthorizeInstantiation("admin")
public class AdminSectionPlugins extends Panel {

	public AdminSectionPlugins(String id) {
		super(id);

		// Le bouton vers la gestion des plugins
		add(new IndicatingAjaxLink<Void>("pluginsLink") {
			@Override
			public void onClick(AjaxRequestTarget target) {
				setResponsePage(PluginsPage.class);
			}
		});

	}
}
