package fr.solunea.thaleia.webapp.pages.content;

import fr.solunea.thaleia.model.Content;
import fr.solunea.thaleia.model.ContentType;
import fr.solunea.thaleia.webapp.pages.BasePage;
import fr.solunea.thaleia.webapp.pages.ThaleiaV6MenuPage;
import fr.solunea.thaleia.webapp.panels.ThaleiaFeedbackPanel;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.authroles.authorization.strategies.role.annotations.AuthorizeInstantiation;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.StringResourceModel;

@SuppressWarnings("serial")
@AuthorizeInstantiation("user")
public class ContentsPage extends ThaleiaV6MenuPage {

    public ContentsPage() {
        super();

        add(new Label("pageLabel", new StringResourceModel("pageLabel", this,
                null)));

		// Le panneau des messags
		ThaleiaFeedbackPanel feedbackPanel = new ThaleiaFeedbackPanel("feedback");
		feedbackPanel.setOutputMarkupId(true);
		add(feedbackPanel);

        add(new ContentsPanel("contentsPanel", false) {
            @Override
            protected void onNewContent(ContentType contentType) {
                setResponsePage(new ContentEditPage(contentType, false));
            }

            @Override
            public void onSelected(IModel<Content> model, AjaxRequestTarget target) {
                setResponsePage(new ContentEditPage(model));
            }

            @Override
            protected void onItemLinkInitialize(AjaxLink<Content> link) {
                // Rien
            }
        });
    }

    public ContentsPage(ContentType contentType) {
        super();

        add(new Label("pageLabel", new StringResourceModel("pageLabel", this,
                null)));

		// Le panneau des messages
		ThaleiaFeedbackPanel feedbackPanel = new ThaleiaFeedbackPanel("feedback");
		feedbackPanel.setOutputMarkupId(true);
		add(feedbackPanel);

        add(new ContentsPanel("contentsPanel", contentType.getIsModuleType()) {
            @Override
            protected void onNewContent(ContentType contentType) {
                setResponsePage(new ContentEditPage(contentType, false));
            }

            @Override
            public void onSelected(IModel<Content> model, AjaxRequestTarget target) {
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
