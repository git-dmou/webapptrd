package fr.solunea.thaleia.webapp.pages.admin;

import com.mailjet.client.MailjetResponse;
import com.mailjet.client.transactional.SendContact;
import com.mailjet.client.transactional.response.SendEmailsResponse;
import fr.solunea.thaleia.model.ApplicationParameter;
import fr.solunea.thaleia.model.LicenceHolding;
import fr.solunea.thaleia.model.User;
import fr.solunea.thaleia.model.dao.ApplicationParameterDao;
import fr.solunea.thaleia.model.dao.LicenceDao;
import fr.solunea.thaleia.model.dao.LicenceHoldingDao;
import fr.solunea.thaleia.service.utils.FilesUtils;
import fr.solunea.thaleia.utils.DetailedException;
import fr.solunea.thaleia.webapp.Mailing;
import fr.solunea.thaleia.webapp.MailjetApi;
import fr.solunea.thaleia.webapp.ThaleiaApplication;
import fr.solunea.thaleia.webapp.security.ThaleiaSession;
import org.apache.log4j.Logger;
import org.apache.poi.ss.formula.functions.T;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.*;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.Model;

/**
 * Panel de test de l'Api Mailjet
 */
public class MailjetPanel extends Panel {

    private final MailjetApi api = new MailjetApi();
    private final static Logger logger = Logger.getLogger(MailjetPanel.class);

    public MailjetPanel(String id) {
        super(id);
        addConfigurationForm();
        addSendTransactionalEmailForm();
        addCreateContactForm();
        addUpdateContactPropertiesForm();
        addAddContactToListForm();
        addDummyForm();
    }


    private TextField<String> getTextField(String parameterName, String wicketId) {
        String value = ThaleiaApplication.get().getApplicationParameterDao().getValue(parameterName, " ");
        return new TextField<>(wicketId, Model.of(value));
    }

    private CheckBox getCheckBox(String parameterName, String wicketId, String defaultValue) {
        String raw = ThaleiaApplication.get().getApplicationParameterDao().getValue(parameterName, defaultValue);
        boolean value = "true".equalsIgnoreCase(raw);
        return new CheckBox(wicketId, Model.of(value));
    }


    /**
     * Ajout du formulaire de configuration mailjet.
     */
    private void addConfigurationForm() {
        Form form = new Form("configurationForm");
        add(form);

        // Clé privée Mailjet
        TextField<String> mailjetApiPrivateKey = getTextField("mailjet.apikey.private", "mailjetApiPrivateKey");
        form.add(mailjetApiPrivateKey);

        // Clé publique Mailjet
        TextField<String> mailjetApiPublicKey = getTextField("mailjet.apikey.public", "mailjetApiPublicKey");
        form.add(mailjetApiPublicKey);

        // Envoi des mails de debug
        CheckBox sendDebugMail = getCheckBox("mailing.sendDebugMail", "sendDebugMail", "false");
        form.add(sendDebugMail);

        // Destinataires des mails de debug
        TextField<String> debugMailRecipients = getTextField("mailing.debugMail.recipients", "debugMailRecipients");
        form.add(debugMailRecipients);

        // Expéditeur des mails de debug
        TextField<String> debugMailSender = getTextField("mailing.debugMail.sender", "debugMailSender");
        form.add(debugMailSender);

        // Création d'un nouveau contact Mailjet lors de la création d'un compte de démo Thaleia XL
        CheckBox registerNewAccountOnAccountCreation = getCheckBox("mailing.registerNewAccountOnAccountCreation",
                "registerNewAccountOnAccountCreation", "false");
        form.add(registerNewAccountOnAccountCreation);

        // Mise à jour des propriétés du contact lors de la validation du compte de Démo
        CheckBox updateContactOnAccountCompletion = getCheckBox("mailing.updateContactOnAccountCompletion",
                "updateContactOnAccountCompletion", "false");
        form.add(updateContactOnAccountCompletion);

        // Identifiant de la liste de contact `Démo-Thaleia-XL-New-Arch`
        TextField<String> contactListThaleiaXlDemo = getTextField("mailjet.contactList.demo.ThaleiaXl", "contactsList.demoThaleiaXL");
        form.add(contactListThaleiaXlDemo);

        // Identifiant de la liste de contact `newsletter`
        TextField<String> contactListNewsletter = getTextField("mailjet.contactList.newsletter", "contactsList.newsletter");
        form.add(contactListNewsletter);

        // Bouton enregistrer
        AjaxButton sendTransactionalEmailSubmitBtn = new AjaxButton("configurationFormSubmit") {
            @Override
            protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                try {
                    // Clé privée Mailjet
                    ApplicationParameter privateKeyObject = ThaleiaApplication.get().getApplicationParameterDao().findByName("mailjet.apikey.private");
                    privateKeyObject.setValue(mailjetApiPrivateKey.getModelObject());
                    ThaleiaApplication.get().getApplicationParameterDao().save(privateKeyObject, true);

                    // Clé publique Mailjet
                    ApplicationParameter publicKeyObject = ThaleiaApplication.get().getApplicationParameterDao().findByName("mailjet.apikey.public");
                    publicKeyObject.setValue(mailjetApiPublicKey.getModelObject());
                    ThaleiaApplication.get().getApplicationParameterDao().save(publicKeyObject, true);

                    // Envoi des mails de debug
                    ApplicationParameter sendDebugMailObject = ThaleiaApplication.get().getApplicationParameterDao().findByName("mailing.sendDebugMail");
                    sendDebugMailObject.setValue(String.valueOf(sendDebugMail.getModelObject()));
                    ThaleiaApplication.get().getApplicationParameterDao().save(sendDebugMailObject, true);

                    // Destinataires des mails de debug
                    ApplicationParameter debugMailRecipientsObject = ThaleiaApplication.get().getApplicationParameterDao().findByName("mailing.debugMail.recipients");
                    debugMailRecipientsObject.setValue(debugMailRecipients.getModelObject());
                    ThaleiaApplication.get().getApplicationParameterDao().save(debugMailRecipientsObject, true);

                    // Expéditeur des mails de debug
                    ApplicationParameter debugMailSenderObject = ThaleiaApplication.get().getApplicationParameterDao().findByName("mailing.debugMail.sender");
                    debugMailSenderObject.setValue(debugMailSender.getModelObject());
                    ThaleiaApplication.get().getApplicationParameterDao().save(debugMailSenderObject, true);

                    // Création d'un nouveau contact Mailjet lors de la création d'un compte de démo Thaleia XL
                    ApplicationParameter registerNewAccountOnAccountCreationObject = ThaleiaApplication.get()
                            .getApplicationParameterDao().findByName("mailing.registerNewAccountOnAccountCreation");
                    registerNewAccountOnAccountCreationObject.setValue(registerNewAccountOnAccountCreation.getModelObject().toString());
                    ThaleiaApplication.get().getApplicationParameterDao().save(registerNewAccountOnAccountCreationObject, true);

                    // Mise à jour des propriétés du contact lors de la validation du compte de Démo
                    ApplicationParameter updateContactOnAccountCompletionObject = ThaleiaApplication.get()
                            .getApplicationParameterDao().findByName("mailing.updateContactOnAccountCompletion");
                    updateContactOnAccountCompletionObject.setValue(updateContactOnAccountCompletion.getModelObject().toString());
                    ThaleiaApplication.get().getApplicationParameterDao().save(updateContactOnAccountCompletionObject, true);

                    // Identifiant de la liste de contact `Démo-Thaleia-XL-New-Arch`
                    ApplicationParameter contactListThaleiaXlDemoObject = ThaleiaApplication.get().getApplicationParameterDao()
                            .findByName("mailjet.contactList.demo.ThaleiaXl");
                    contactListThaleiaXlDemoObject.setValue(contactListThaleiaXlDemo.getModelObject());
                    ThaleiaApplication.get().getApplicationParameterDao().save(contactListThaleiaXlDemoObject, true);

                    // Identifiant de la liste de contact `newsletter`
                    ApplicationParameter newsletter = ThaleiaApplication.get().getApplicationParameterDao().findByName("mailjet.contactList.newsletter");
                    newsletter.setValue(contactListNewsletter.getModelObject());
                    ThaleiaApplication.get().getApplicationParameterDao().save(newsletter, true);

                } catch (DetailedException e) {
                    throw new RuntimeException(e);
                }
            }
        };
        form.add(sendTransactionalEmailSubmitBtn);

    }

    /**
     * Ajout du formulaire d'envoi d'un mail transactionnel.
     */
    private void addSendTransactionalEmailForm() {
        WebMarkupContainer sendTransactionalEmail = new WebMarkupContainer("sendTransactionalEmail");
        add(sendTransactionalEmail);

        Form form = new Form("sendTransactionalEmailForm");
        sendTransactionalEmail.add(form);

        // Expéditeur
        TextField senderEmail = new TextField("senderEmail", Model.of(""));
        form.add(senderEmail);
        TextField senderName = new TextField("senderName", Model.of(""));
        form.add(senderName);
        // Destinataire
        TextField recipientEmail = new TextField("recipientEmail", Model.of(""));
        form.add(recipientEmail);
        TextField recipientName = new TextField("recipientName", Model.of(""));
        form.add(recipientName);
        // Sujet du mail
        TextField subject = new TextField("subject", Model.of(""));
        form.add(subject);
        // Corps du mail
        TextArea<String> mailBody = new TextArea("mailBody", Model.of(""));
        form.add(mailBody);

        Model<String> strMdl = Model.of("");
        Label result = (Label) new Label("result", strMdl).setOutputMarkupId(true);
        sendTransactionalEmail.add(result);

        AjaxButton sendTransactionalEmailSubmitBtn = new AjaxButton("sendTransactionalEmailSubmitBtn") {
            @Override
            protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                try {
                    // Expéditeur, si le nom n'est pas renseigné, on réutilise l'adresse email.
                    SendContact from;
                    if (senderName.getValue().equals("")) {
                        from = new SendContact(senderEmail.getValue(), senderEmail.getValue());
                    } else {
                        from = new SendContact(senderEmail.getValue(), senderName.getValue());
                    }

                    // Destinataire, si le nom n'est pas renseigné, on réutilise l'adresse email.
                    SendContact to;
                    if (recipientName.getValue().equals("")) {
                        to = new SendContact(recipientEmail.getValue(), recipientEmail.getValue());
                    } else {
                        to = new SendContact(recipientEmail.getValue(), recipientName.getValue());
                    }

                    SendEmailsResponse response = api.sendTransactionalEmail(from, to, subject.getValue(), mailBody.getValue());
                    strMdl.setObject(response.toString());
                    target.add(result);
                } catch (Exception e) {
                    e.printStackTrace();
                    strMdl.setObject(e.toString());
                    target.add(result);
                }
            }
        };
        form.add(sendTransactionalEmailSubmitBtn);
    }

    /**
     * Ajout du formulaire de création d'un contact.
     */
    private void addCreateContactForm() {
        WebMarkupContainer createContact = new WebMarkupContainer("createContact");
        add(createContact);

        Form form = new Form("createContactForm");
        createContact.add(form);

        TextField mail = new TextField("email", Model.of(""));
        form.add(mail);

        Model<String> strMdl = Model.of("");
        Label result = (Label) new Label("result", strMdl).setOutputMarkupId(true);
        createContact.add(result);

        AjaxButton createContactFormSubmitBtn = new AjaxButton("createContactFormSubmitBtn") {
            @Override
            protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                try {
                    MailjetResponse response = api.createContact(mail.getValue());
                    strMdl.setObject(response.getRawResponseContent());
                    target.add(result);
                } catch (Exception e) {
                    e.printStackTrace();
                    strMdl.setObject(e.toString());
                    target.add(result);
                }
            }
        };

        form.add(createContactFormSubmitBtn);
    }

    /**
     * Ajout du formulaire de mise à jour d'une propriété d'un contact.
     */
    private void addUpdateContactPropertiesForm() {
        WebMarkupContainer updateContactProperty = new WebMarkupContainer("updateContactProperty");
        add(updateContactProperty);

        Form form = new Form("updateContactPropertyForm");
        updateContactProperty.add(form);

        TextField email = new TextField("email", Model.of(""));
        form.add(email);

        TextField propertyName = new TextField("propertyName", Model.of(""));
        form.add(propertyName);

        TextField propertyValue = new TextField("propertyValue", Model.of(""));
        form.add(propertyValue);

        Model<String> strMdl = Model.of("");
        Label result = (Label) new Label("result", strMdl).setOutputMarkupId(true);
        updateContactProperty.add(result);

        AjaxButton updateContactPropertiesFormSubmitBtn = new AjaxButton("updateContactPropertyFormSubmitBtn") {
            @Override
            protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                try {
                    MailjetResponse response = api.updateContactProperty(email.getValue(), propertyName.getValue(),
                            propertyValue.getValue());
                    strMdl.setObject(response.getRawResponseContent());
                    target.add(result);
                } catch (Exception e) {
                    e.printStackTrace();
                    strMdl.setObject(e.toString());
                    target.add(result);
                }
            }
        };
        form.add(updateContactPropertiesFormSubmitBtn);
    }

    /**
     * Ajout du formulaire d'ajout d'un contact à une liste de contacts.
     */
    private void addAddContactToListForm() {
        WebMarkupContainer addContactToList = new WebMarkupContainer("addContactToList");
        add(addContactToList);

        Form form = new Form("addContactToListForm");
        addContactToList.add(form);

        TextField email = new TextField("email", Model.of(""));
        form.add(email);

        TextField listID = new TextField("listID", Model.of(""));
        form.add(listID);

        Model<String> strMdl = Model.of("");
        Label result = (Label) new Label("result", strMdl).setOutputMarkupId(true);
        addContactToList.add(result);

        AjaxButton addContactToListFormSubmitBtn = new AjaxButton("addContactToListFormSubmitBtn") {
            @Override
            protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                try {
                    MailjetResponse response = api.addContactToList(email.getValue(), listID.getValue());
                    strMdl.setObject(response.getRawResponseContent());
                    target.add(result);
                } catch (Exception e) {
                    e.printStackTrace();
                    strMdl.setObject(e.toString());
                    target.add(result);
                }
            }
        };
        form.add(addContactToListFormSubmitBtn);
    }

    private void addDummyForm() {
        WebMarkupContainer dummy = new WebMarkupContainer("dummy");
        add(dummy);

        Form form = new Form("dummyForm");
        dummy.add(form);

        Model<String> strMdl = Model.of("");
        Label result = (Label) new Label("result", strMdl).setOutputMarkupId(true);
        dummy.add(result);

        AjaxButton dummyFormSubmitBtn = new AjaxButton("dummyFormSubmitBtn") {
            @Override
            protected void onSubmit(AjaxRequestTarget target, Form<?> form) {

                LicenceHoldingDao licenceHoldingDao = new LicenceHoldingDao(ThaleiaSession.get().getContextService().getContextSingleton());

                try {
                    // Attribution de la licence sélectionnée
                    LicenceHolding holding = licenceHoldingDao.attributeLicence(
                            ThaleiaSession.get().getAuthenticatedUser(),
                            "THA6-DEMO-1",
                            "Démo contenu produit avec succès");
                    licenceHoldingDao.save(holding, true);
                } catch (DetailedException e) {
                    logger.warn(e);
                    error(e);
                }
            }
        };
        form.add(dummyFormSubmitBtn);
    }
}
