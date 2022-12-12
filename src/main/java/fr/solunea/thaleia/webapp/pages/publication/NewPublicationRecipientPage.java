package fr.solunea.thaleia.webapp.pages.publication;

import fr.solunea.thaleia.model.Publication;
import fr.solunea.thaleia.model.dao.PublicationDao;
import fr.solunea.thaleia.service.MailService;
import fr.solunea.thaleia.utils.DetailedException;
import fr.solunea.thaleia.webapp.ThaleiaApplication;
import fr.solunea.thaleia.webapp.pages.ThaleiaV6MenuPage;
import fr.solunea.thaleia.webapp.panels.PanelUtils;
import fr.solunea.thaleia.webapp.panels.ThaleiaFeedbackPanel;
import fr.solunea.thaleia.webapp.panels.ThaleiaLightFeedbackPanel;
import fr.solunea.thaleia.webapp.security.ThaleiaSession;
import org.apache.cayenne.ObjectContext;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Création d'un destinataire pour une publication.
 */
@SuppressWarnings("serial")
public abstract class NewPublicationRecipientPage extends ThaleiaV6MenuPage {

    protected ThaleiaFeedbackPanel feedbackPanel;

    public NewPublicationRecipientPage(final IModel<Publication> model) {
        super(model);

        // On fabrique un contexte d'édition
        ObjectContext context = ThaleiaSession.get().getContextService().getNewContext();
        PublicationDao publicationDao = new PublicationDao(context);
        Publication publication = publicationDao.get(model.getObject().getObjectId());
        setDefaultModelObject(publication);

        // Le panneau des messages
        feedbackPanel = new ThaleiaLightFeedbackPanel("feedback");
        feedbackPanel.setOutputMarkupId(true);
        add(feedbackPanel);

        // Le titre de la page
        add(new Label("title",  ((Publication) getDefaultModelObject()).getName()));

        final IModel<Emails> emailsModel = new CompoundPropertyModel<>(Model.of(new Emails()));

        Form<Emails> form = new Form<>("form", emailsModel);
        add(form);

        form.add(new TextArea<String>("rawContent"));

        form.add(new Button("cancel") {
            @Override
            public void onSubmit() {
                onOut();
            }
        });

        form.add(new Button("save") {
            @Override
            public void onSubmit() {
                try {
                    // L'URL de base de la publication
                    String baseUrl = ThaleiaApplication.get().getPublishUrl() + "/" + publication.getReference();

                    List<String> addedEmails = new ArrayList<>();

                    // On ajoute les emails à la publication
                    for (String email : emailsModel.getObject().parseEmails()) {

                        // Si ce mail n'existe pas déjà pour cette
                        // publication, on l'ajoute
                        if (ThaleiaSession.get().getPublicationService().getRecipient(publication, email) == null) {
                            ThaleiaSession.get().getPublicationService().createRecipient(publication, email, baseUrl);
                            addedEmails.add(email);
                        }
                    }

                    // On enregistre les modifications
                    publicationDao.save(publication, true);

                    // On présente un message avec les mails ajoutés
                    if (addedEmails.isEmpty()) {
                        StringResourceModel message = new StringResourceModel("no.emails.added", NewPublicationRecipientPage.this, null);
                        // Affiche le message
                        ThaleiaSession.get().info(message.getString());
                    } else {
                        StringBuilder addedEmailString = new StringBuilder();
                        for (String addedEmail : addedEmails) {
                            addedEmailString.append(addedEmail).append(" ");
                        }
                        StringResourceModel message = new StringResourceModel("emails.added", NewPublicationRecipientPage.this, null, new Object[]{addedEmailString.toString()});
                        // Affiche le message
                        ThaleiaSession.get().info(message.getString());
                    }

                    onOut();

                } catch (DetailedException e) {
                    logger.warn("Impossible d'ajouter les emails à la publication : " + e);
                    MarkupContainer panelContainer = this.getParent().getParent();
                    StringResourceModel errorMessageModel = new StringResourceModel("save.error", panelContainer, null);
                    // Affiche le message d'erreur
                    PanelUtils.showErrorMessage(errorMessageModel.getString(), e.toString(), feedbackPanel);
                }
            }
        });
    }

    /**
     * La redirection en sortie de cette page.
     */
    public abstract void onOut();

    protected static class Emails implements Serializable {
        private String rawContent;

        public String getRawContent() {
            return rawContent;
        }

        public void setRawContent(String content) {
            this.rawContent = content;
        }

        /**
         * Analyse le contenu brut pour en extraire une liste d'adresses mails
         * valides.
         */
        public List<String> parseEmails() {
            return MailService.parseMultipleAddresses(rawContent);
        }
    }

}
