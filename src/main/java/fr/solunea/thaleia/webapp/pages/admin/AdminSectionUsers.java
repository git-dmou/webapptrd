package fr.solunea.thaleia.webapp.pages.admin;

import org.apache.log4j.Logger;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.authroles.authorization.strategies.role.annotations.AuthorizeInstantiation;
import org.apache.wicket.extensions.ajax.markup.html.IndicatingAjaxLink;
import org.apache.wicket.markup.html.panel.Panel;

import fr.solunea.thaleia.webapp.pages.domains.DomainsPanel;

@SuppressWarnings("serial")
@AuthorizeInstantiation("admin")
public class AdminSectionUsers extends Panel {

	protected static final Logger logger = Logger.getLogger(AdminSectionUsers.class);

	Panel usersPanel;
	Panel domainsPanel;

	public AdminSectionUsers(String id) {
		super(id);

		// Par défaut, on ne calcule pas l'espace utilisé dans les tableaux.
		// On le fait si on clique sur le bouton correspondant.

		usersPanel = new UsersPanel("usersPanel", false);
		usersPanel.setOutputMarkupId(true);
		add(usersPanel);

		domainsPanel = new DomainsPanel("domainsPanel", false);
		domainsPanel.setOutputMarkupId(true);
		add(domainsPanel);

		add(new IndicatingAjaxLink<Void>("checkDiskUsage") {

			@Override
			public void onClick(AjaxRequestTarget target) {
				Panel newUsersPanel = new UsersPanel("usersPanel", true);
				newUsersPanel.setOutputMarkupId(true);
				usersPanel.replaceWith(newUsersPanel);
				target.add(newUsersPanel);
				usersPanel = newUsersPanel;

				Panel newDomainsPanel = new DomainsPanel("domainsPanel", true);
				newDomainsPanel.setOutputMarkupId(true);
				domainsPanel.replaceWith(newDomainsPanel);
				target.add(newDomainsPanel);
				domainsPanel = newDomainsPanel;
			}
		});
	}
}
