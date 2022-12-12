package fr.solunea.thaleia.webapp.pages.admin;

import fr.solunea.thaleia.webapp.pages.ThaleiaV6MenuPage;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.authroles.authorization.strategies.role.annotations.AuthorizeInstantiation;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.model.AbstractReadOnlyModel;

import fr.solunea.thaleia.webapp.ThaleiaApplication;
import fr.solunea.thaleia.webapp.panels.ThaleiaFeedbackPanel;
import org.apache.wicket.model.StringResourceModel;

@SuppressWarnings("serial")
@AuthorizeInstantiation("admin")
public class AdminPage extends ThaleiaV6MenuPage {

	public AdminPage() {
		super();

		// Le panneau des messages
		ThaleiaFeedbackPanel feedbackPanel = new ThaleiaFeedbackPanel("feedback");
		feedbackPanel.setOutputMarkupId(true);
		add(feedbackPanel);

		AdminSectionPlugins currentAdminSection = new AdminSectionPlugins("adminPageContent");
		currentAdminSection.setOutputMarkupId(true);
		add(currentAdminSection);

		final WebMarkupContainer pluginsLi = new WebMarkupContainer("pluginsLi");
		Link<Void> pluginsLink = new Link<>("pluginsLink") {
			@Override
			public void onClick() {
				removeClassActive();
				AdminSectionPlugins currentAdminSection = new AdminSectionPlugins("adminPageContent");
				currentAdminSection.setOutputMarkupId(true);
				AdminPage.this.addOrReplace(currentAdminSection);
				pluginsLi.add(new AttributeModifier("class", "active"));
			}
		};
		pluginsLi.add(new AttributeModifier("class", "active"));
		pluginsLi.add(pluginsLink);
		add(pluginsLi);

		final WebMarkupContainer usersSectionLi = new WebMarkupContainer("usersSectionLi");
		Link<Void> usersSectionLink = new Link<>("usersSectionLink") {
			@Override
			public void onClick() {
				removeClassActive();
				AdminSectionUsers currentAdminSection = new AdminSectionUsers("adminPageContent");
				currentAdminSection.setOutputMarkupId(true);
				AdminPage.this.addOrReplace(currentAdminSection);
				usersSectionLi.add(new AttributeModifier("class", "active"));
			}
		};
		usersSectionLi.add(usersSectionLink);
		add(usersSectionLi);

		final WebMarkupContainer propertiesLi = new WebMarkupContainer("propertiesLi");
		Link<Void> propertiesLink = new Link<>("propertiesLink") {
			@Override
			public void onClick() {
				removeClassActive();
				AdminSectionProperties currentAdminSection = new AdminSectionProperties("adminPageContent");
				currentAdminSection.setOutputMarkupId(true);
				AdminPage.this.addOrReplace(currentAdminSection);
				propertiesLi.add(new AttributeModifier("class", "active"));
			}
		};
		propertiesLi.add(propertiesLink);
		add(propertiesLi);

		final WebMarkupContainer localesLi = new WebMarkupContainer("localesLi");
		Link<Void> localesLink = new Link<>("localesLink") {
			@Override
			public void onClick() {
				removeClassActive();
				AdminSectionLocales currentAdminSection = new AdminSectionLocales("adminPageContent");
				currentAdminSection.setOutputMarkupId(true);
				AdminPage.this.addOrReplace(currentAdminSection);
				localesLi.add(new AttributeModifier("class", "active"));
			}
		};
		localesLi.add(localesLink);
		add(localesLi);

		final WebMarkupContainer parametersLi = new WebMarkupContainer("parametersLi");
		Link<Void> parametersLink = new Link<>("parametersLink") {
			@Override
			public void onClick() {
				removeClassActive();
				AdminSectionParameters currentAdminSection = new AdminSectionParameters("adminPageContent");
				currentAdminSection.setOutputMarkupId(true);
				AdminPage.this.addOrReplace(currentAdminSection);
				parametersLi.add(new AttributeModifier("class", "active"));
			}
		};
		parametersLi.add(parametersLink);
		add(parametersLi);

		final WebMarkupContainer toolsLi = new WebMarkupContainer("toolsLi");
		Link<Void> toolsLink = new Link<>("toolsLink") {
			@Override
			public void onClick() {
				removeClassActive();
				AdminSectionTools currentAdminSection = new AdminSectionTools("adminPageContent");
				currentAdminSection.setOutputMarkupId(true);
				AdminPage.this.addOrReplace(currentAdminSection);
				toolsLi.add(new AttributeModifier("class", "active"));
			}
		};
		toolsLi.add(toolsLink);
		add(toolsLi);

		final WebMarkupContainer monitorLi = new WebMarkupContainer("monitorLi");
		ExternalLink monitorLink = new ExternalLink("monitorLink", new AbstractReadOnlyModel<String>() {
			@Override
			public String getObject() {
				return ThaleiaApplication.get().getApplicationRootUrl() + "/monitoring";
			}
		});
		monitorLi.add(monitorLink);
		add(monitorLi);
		
		final WebMarkupContainer usageLi = new WebMarkupContainer("usageLi");
		Link<Void> usageLink = new Link<>("usageLink") {
			@Override
			public void onClick() {
				removeClassActive();
				UsagePanel panel = new UsagePanel("adminPageContent");
				currentAdminSection.setOutputMarkupId(true);
				AdminPage.this.addOrReplace(panel);
				usageLi.add(new AttributeModifier("class", "active"));
			}
		};
		usageLi.add(usageLink);
		add(usageLi);

		final WebMarkupContainer apiLi = new WebMarkupContainer("apiLi");
		Link<Void> apiLink = new Link<>("apiLink") {
			@Override
			public void onClick() {
				removeClassActive();
				RestApiPanel panel = new RestApiPanel("adminPageContent");
				currentAdminSection.setOutputMarkupId(true);
				AdminPage.this.addOrReplace(panel);
				apiLi.add(new AttributeModifier("class", "active"));
			}
		};
		apiLi.add(apiLink);
		add(apiLi);


		final WebMarkupContainer mailjetLi = new WebMarkupContainer("mailjetLi");
		Link<Void> mailjetLink = new Link<>("mailjetLink") {
			@Override
			public void onClick() {
				removeClassActive();
				MailjetPanel panel = new MailjetPanel("adminPageContent");
				currentAdminSection.setOutputMarkupId(true);
				AdminPage.this.addOrReplace(panel);
				mailjetLi.add(new AttributeModifier("class", "active"));
			}
		};
		mailjetLi.add(mailjetLink);
		add(mailjetLi);
	}

	/**
	 * Définition de la classe à ajouter à la navbar.
	 */
	@Override
	protected void setNavbarClass() {
		navbarClass = "thaleia-admin";
	}

	public void removeClassActive() {
		get("pluginsLi").add(new AttributeModifier("class", ""));
		get("usersSectionLi").add(new AttributeModifier("class", ""));
		get("propertiesLi").add(new AttributeModifier("class", ""));
		get("localesLi").add(new AttributeModifier("class", ""));
		get("parametersLi").add(new AttributeModifier("class", ""));
		get("toolsLi").add(new AttributeModifier("class", ""));
	}

	protected Label getHomeButtonLabel() {
		return (Label) new Label("homeLinkLabel",
				new StringResourceModel("homeLinkLabel", this, null))
				.setEscapeModelStrings(false);
	}
}
