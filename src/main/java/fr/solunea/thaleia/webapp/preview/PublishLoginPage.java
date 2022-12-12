package fr.solunea.thaleia.webapp.preview;

import fr.solunea.thaleia.model.Publication;
import fr.solunea.thaleia.webapp.pages.ThaleiaPage;
import fr.solunea.thaleia.webapp.panels.ThaleiaFeedbackPanel;
import org.apache.log4j.Logger;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.PasswordTextField;
import org.apache.wicket.markup.html.panel.EmptyPanel;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.request.flow.RedirectToUrlException;

@SuppressWarnings("serial")
public class PublishLoginPage extends ThaleiaPage {

    private static final Logger logger = Logger.getLogger(PublishLoginPage.class);

    public PublishLoginPage() {
        super();
        add(new EmptyPanel("feedback"));
        add(new PasswordTextField("password", Model.of("")));
        add(new Form<Void>("form"));
    }

    /**
     * @param publication la publication
     * @param successUrl  l'URL de redirection en cas de réussite de la vérification du mot de passe
     */
    public PublishLoginPage(final Publication publication, final String successUrl) {
        super();

        logger.debug("Vérification du mot de passe pour l'accès à l'URL " + successUrl);

        // Le panneau des messags
        ThaleiaFeedbackPanel feedbackPanel = new ThaleiaFeedbackPanel("feedback");
        feedbackPanel.setOutputMarkupId(true);
        add(feedbackPanel);

        // Message : "Cette ressource est protégée par un mot de passe."
        info(new StringResourceModel("autorisation.required", PublishLoginPage.this, null).getString());

        final PasswordTextField passwordField = new PasswordTextField("password", Model.of(""));
        passwordField.setRequired(true);

        Form<?> form = new Form<Void>("form") {

            @Override
            protected void onSubmit() {
                final String passwordValue = passwordField.getModelObject();
                if (passwordValue.equals(publication.getPassword())) {
                    // On stocke l'identification en session
                    PublishSecurity.signIn(publication);

                    // On redirige sur la destination demandée
                    throw new RedirectToUrlException(successUrl);
                } else {
                    error(new StringResourceModel("not.permitted", PublishLoginPage.this, null).getString());
                }
            }

        };

        add(form);
        form.add(passwordField);
    }

}
