package fr.solunea.thaleia.webapp.panels;

import org.apache.wicket.authorization.Action;
import org.apache.wicket.authroles.authorization.strategies.role.annotations.AuthorizeAction;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.html.panel.Panel;

import fr.solunea.thaleia.webapp.pages.admin.AdminPage;
import fr.solunea.thaleia.webapp.pages.content.ContentsPage;
import fr.solunea.thaleia.webapp.pages.modules.ModulesPage;
import fr.solunea.thaleia.webapp.pages.plugins.PluginsPage;
import fr.solunea.thaleia.webapp.security.ThaleiaSession;

@SuppressWarnings("serial")
public class MenuPanel extends Panel {

	public MenuPanel(String id) {
		super(id);

		add(new ContentsLink("contentsLink"));
		add(new ModulesLink("modulesLink"));
		add(new PluginsLink("pluginsLink"));
		add(new AdminLink("adminLink"));
	}

	@AuthorizeAction(action = Action.RENDER, roles = { "user" })
	private class ContentsLink extends Link<Void> {
		public ContentsLink(String id) {
			super(id);
		}

		@Override
		public void onClick() {
			setResponsePage(ContentsPage.class);
		}

		@Override
		public boolean isVisible() {
			return (ThaleiaSession.get().getAuthenticatedUser() != null && ThaleiaSession
					.get().getAuthenticatedUser().getMenuContents());
		}
	}

	@AuthorizeAction(action = Action.RENDER, roles = { "admin" })
	private class AdminLink extends Link<Void> {
		public AdminLink(String id) {
			super(id);
		}

		@Override
		public void onClick() {
			setResponsePage(AdminPage.class);
		}
	}

	@AuthorizeAction(action = Action.RENDER, roles = { "admin" })
	private class PluginsLink extends Link<Void> {
		public PluginsLink(String id) {
			super(id);
		}

		@Override
		public void onClick() {
			setResponsePage(PluginsPage.class);
		}
	}

	@AuthorizeAction(action = Action.RENDER, roles = { "user" })
	private class ModulesLink extends Link<Void> {
		public ModulesLink(String id) {
			super(id);
		}

		@Override
		public void onClick() {
			setResponsePage(ModulesPage.class);
		}

		@Override
		public boolean isVisible() {
			return (ThaleiaSession.get().getAuthenticatedUser() != null && ThaleiaSession
					.get().getAuthenticatedUser().getMenuModules());
		}
	}

}
