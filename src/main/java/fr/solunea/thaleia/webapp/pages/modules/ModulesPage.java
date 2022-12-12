package fr.solunea.thaleia.webapp.pages.modules;

import fr.solunea.thaleia.webapp.pages.ThaleiaV6MenuPage;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.authroles.authorization.strategies.role.annotations.AuthorizeInstantiation;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.StringResourceModel;

import fr.solunea.thaleia.model.Content;
import fr.solunea.thaleia.model.ContentType;
import fr.solunea.thaleia.webapp.pages.content.ContentEditPage;
import fr.solunea.thaleia.webapp.pages.content.ContentsPanel;

@SuppressWarnings("serial")
@AuthorizeInstantiation("user")
public class ModulesPage extends ThaleiaV6MenuPage {

	public ModulesPage() {
		super();

		add(new Label("pageLabel", new StringResourceModel("pageLabel", this,
				null)));

		add(new ContentsPanel("modulesPanel", true) {
			@Override
			protected void onNewContent(ContentType contentType) {
				setResponsePage(new ContentEditPage(contentType, true));
			}

			@Override
			public void onSelected(IModel<Content> model,
					AjaxRequestTarget target) {
				setResponsePage(new ContentEditPage(model));
			}

			@Override
			protected void onItemLinkInitialize(AjaxLink<Content> link) {
				// Rien
			}
		});
	}

	public ModulesPage(ContentType contentType) {
		super();

		add(new Label("pageLabel", new StringResourceModel("pageLabel", this,
				null)));

		add(new ContentsPanel("modulesPanel", contentType.getIsModuleType()) {
			@Override
			protected void onNewContent(ContentType contentType) {
				setResponsePage(new ContentEditPage(contentType, true));
			}

			@Override
			public void onSelected(IModel<Content> model,
					AjaxRequestTarget target) {
				setResponsePage(new ContentEditPage(model));
			}

			@Override
			protected void onItemLinkInitialize(AjaxLink<Content> link) {
				// Rien
			}
		});
	}

	/**
	 * Retourne le Label du bouton "home". Cette classe est à surcharger pour obtenir des labels personnalisés (ex: Thaleia XL, Thaleia Dialogue, etc...)
	 *
	 * @return Label
	 */
	protected Label getHomeButtonLabel() {
		return (Label) new Label("homeLinkLabel",
				new StringResourceModel("homeLinkLabel", this, null))
				.setEscapeModelStrings(false);
	}
}
