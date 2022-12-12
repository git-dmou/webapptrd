package fr.solunea.thaleia.webapp.pages.demo;

import fr.solunea.thaleia.model.AccountRequest;
import fr.solunea.thaleia.model.Licence;
import fr.solunea.thaleia.model.MailTemplate;
import fr.solunea.thaleia.model.dao.ApplicationParameterDao;
import fr.solunea.thaleia.model.dao.MailTemplateDao;
import fr.solunea.thaleia.service.LicenceService;
import fr.solunea.thaleia.service.utils.Configuration;
import fr.solunea.thaleia.utils.DetailedException;
import fr.solunea.thaleia.utils.LogUtils;
import fr.solunea.thaleia.webapp.ThaleiaApplication;
import fr.solunea.thaleia.webapp.panels.PanelUtils;
import fr.solunea.thaleia.webapp.panels.ThaleiaFeedbackPanel;
import fr.solunea.thaleia.webapp.panels.ThaleiaLightFeedbackPanel;
import fr.solunea.thaleia.webapp.security.ThaleiaSession;
import fr.solunea.thaleia.webapp.utils.*;
import org.apache.cayenne.ObjectContext;
import org.apache.log4j.Logger;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.ajax.markup.html.IndicatingAjaxButton;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.request.resource.PackageResourceReference;
import org.apache.wicket.validation.IValidationError;

import java.util.Locale;

public abstract class CreateAccountWithLicencePanel extends Panel {

    private static final Logger logger = Logger.getLogger(CreateAccountWithLicencePanel.class);

    protected ThaleiaFeedbackPanel feedbackPanel;
    protected Form<AccountRequest> form;

    @SuppressWarnings("unchecked")
    CreateAccountWithLicencePanel(String id, IModel<AccountRequest> accountRequestModel, IModel<Licence> licenceModel) {
        super(id);
        ApplicationParameterDao applicationParameterDao = new ApplicationParameterDao(ThaleiaSession.get().getContextService().getContextSingleton());

        // Si déjà identifié, on redirige sur la page d'accueil des utilisateurs identifiés
        if (ThaleiaSession.get().isSignedIn()) {
            setResponsePage(ThaleiaApplication.get().getRedirectionPage(Configuration.AUTHENTIFIED_USERS_WELCOME_PAGE, Configuration.DEFAULT_AUTHENTIFIED_USERS_WELCOME_PAGE, Configuration.HOME_MOUNT_POINT));
        }

        logger.debug("Requête de création de compte : " + accountRequestModel.getObject());
        logger.debug("Email : " + accountRequestModel.getObject().getMail());

        // On prépare un compoundPropertyModel pour appel direct à ses membres.
        setDefaultModel(new CompoundPropertyModel<>(accountRequestModel));

        // Le panneau des messages
        feedbackPanel = new ThaleiaLightFeedbackPanel("feedback");
        feedbackPanel.setOutputMarkupId(true);
        add(feedbackPanel);

        String pageTitleText;
        if (licenceModel.getObject().getName().equals(LicenceService.LICENCE_NAME_DEMO_CANNELLE)) {
            pageTitleText = MessagesUtils.getLocalizedMessage("pageTitle.thaleiaxl", CreateAccountWithLicencePanel.class);
        } else if (licenceModel.getObject().getName().equals(LicenceService.LICENCE_NAME_DEMO_ACTION)) {
            pageTitleText = MessagesUtils.getLocalizedMessage("pageTitle.dialogue", CreateAccountWithLicencePanel.class);
        } else {
            pageTitleText = MessagesUtils.getLocalizedMessage("pageTitle.default", CreateAccountWithLicencePanel.class, licenceModel.getObject().getName());
        }
        Label pageTitle = new Label("pageTitle", pageTitleText);
        add(pageTitle);

        final WebMarkupContainer demoMessageDiv = new WebMarkupContainer("demoMessageDiv");
        demoMessageDiv.setVisible(licenceModel.getObject().getIsDemo());
        add(demoMessageDiv);
        String demoMessageText = "";
        if (licenceModel.getObject().getName().equals(LicenceService.LICENCE_NAME_DEMO_CANNELLE)) {
            demoMessageText = MessagesUtils.getLocalizedMessage("demoMessageDiv.thaleiaxl", CreateAccountWithLicencePanel.class);
        } else if (licenceModel.getObject().getName().equals(LicenceService.LICENCE_NAME_DEMO_ACTION)) {
            demoMessageText = MessagesUtils.getLocalizedMessage("demoMessageDiv.dialogue", CreateAccountWithLicencePanel.class);
        }
        Label demoMessage = new Label("demoMessage", demoMessageText);
        demoMessageDiv.add(demoMessage.setEscapeModelStrings(false));

        // Le formulaire
        form = new Form<>("form", (IModel<AccountRequest>) getDefaultModel());
        form.setOutputMarkupId(true);
        form.setOutputMarkupPlaceholderTag(true);

        // Adresse mail : ne doit pas exister, et doit être acceptée comme
        // valide.
        TextField<String> mail = new TextField<>("mail") {
            @Override
            public void error(IValidationError error) {
                super.error(error);
                add(AttributeModifier.append("class", "has-error"));
            }

            @Override
            public boolean checkRequired() {
                boolean result = super.checkRequired();
                add(new CSSClassRemover("has-error"));
                return result;
            }
        };
        mail.add(new AttributeModifier("placeholder", new StringResourceModel(
                "mailLabel", this, null)));
        form.addOrReplace(mail);
        ((TextField<String>) form.get("mail")).setRequired(true).add(SeriousMailValidator.getInstance()).add(new UniqueLoginValidator()).setLabel(new StringResourceModel("mailLabel", this, null));
        if (ThaleiaSession.get().getUserService().isAccountCreationConstrainedByDomain()) {
            mail.add(new DomainConstraintMailValidator(ThaleiaSession.get().getUserService().getAccountDomainsWhitelist()) {
                @Override
                public String getErrorMessage() {
                    String defaultMessage = MessagesUtils.getLocalizedMessage("mail.DomainConstraintMailValidator", CreateAccountWithLicencePanel.class);
                    String language = ThaleiaSession.get().getLocale().getLanguage();
                    return applicationParameterDao.getValue("new.account.domain.whitelist.error." + language, defaultMessage);
                }
            });
        }

        WebMarkupContainer legalLabel = new WebMarkupContainer("legalLabelContainer") {
            @Override
            public boolean isVisible() {
                return ThaleiaSession.get().getAuthenticatedUser() == null;
            }
        };
        legalLabel.add(getLegalLink());
        form.add(legalLabel);

        // Le bouton d'acceptation des conditions d'utilisation est toujours activé
        IModel<Boolean> legalModel = new AbstractReadOnlyModel<>() {
            @Override
            public Boolean getObject() {
                return true;
            }
        };
        legalLabel.add(new CheckBox("legal", legalModel) {
        }
                .setRequired(true)
                .add(new LegalValidator()).setLabel(new StringResourceModel("legalLabel", this, null)));

        // Le bouton enregistrer
        form.add(new IndicatingAjaxButton("save") {

            @Override
            protected void onError(AjaxRequestTarget target, Form<?> form) {
                target.add(feedbackPanel);
            }

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                try {
                    @SuppressWarnings("unchecked") IModel<AccountRequest> accountRequestModel = (IModel<AccountRequest>) CreateAccountWithLicencePanel.this.getDefaultModel();
                    // On place cet objet dans un contexte propre à l'opération
                    ObjectContext context = ThaleiaSession.get().getContextService().getNewContext();
                    AccountRequest accountRequest = accountRequestModel.getObject();
                    MailTemplateDao mailTemplateDao = new MailTemplateDao(context);
                    ApplicationParameterDao applicationParameterDao = new ApplicationParameterDao(context);

                    logger.debug("Requête de création de compte : " + accountRequest);

                    Licence licence = context.localObject(licenceModel.getObject());

                    // Le mail de création de compte à envoyer à l'utilisateur pour la validation de son email (si besoin)
                    Locale locale = ThaleiaSession.get().getLocale();
                    MailTemplate mailTemplate;
                    if (licence.getName().equals("v6.author")) {
                        // Si la licence est du type auteur on est face à un compte de type Active Login Formateur.
                        // Le mail à envoyer diffère de celui pour les comptes de démo.
                        mailTemplate = mailTemplateDao.findByName("userAccountCreatedAuthor", locale);
                    } else {
                        // Sinon on prend le mail des comptes de démo.
                        mailTemplate = mailTemplateDao.findByName("userAccountCreated", locale);
                    }

                    String validationEmailSubject = mailTemplate.getObject();
                    String validationEmailBody = mailTemplate.getBody();

                    // Le lien de finalisation du compte sur cette instance Thaleia
                    String accountFinalizationURL = applicationParameterDao.getValue(Configuration.APPLICATION_PARAMETER_SERVER_URL, "") +
                            Configuration.ACCOUNT_FINALIZATION_PAGE;

                    ThaleiaSession.get().getUserService().attributeVerifiedLicenceToAccountWithException(
                            ThaleiaApplication.get().getUserDao().findUserByLogin("admin"),
                            licence,
                            accountRequest.getMail(),
                            locale,
                            validationEmailSubject,
                            validationEmailBody,
                            ThaleiaApplication.get().getApplicationRootUrl() + " - CreateAccountPanel",
                            accountFinalizationURL,
                            ThaleiaApplication.get().getApplicationRootUrl());

                    // On renvoie sur la page de confirmation
                    setResponsePage(new ConfirmationPage(accountRequestModel) {
                        @Override
                        protected void onOut() {
                            CreateAccountWithLicencePanel.this.onOut();
                        }
                    });

                } catch (DetailedException e) {
                    logger.warn("Impossible de créer le compte : " + e);
                    MarkupContainer panelContainer = this.getParent().getParent();
                    StringResourceModel errorMessageModel = new StringResourceModel(
                            "creation" + ".error", panelContainer, null);
                    // Affiche le message d'erreur
                    PanelUtils.showErrorMessage(errorMessageModel.getString(), e.toString(), feedbackPanel);
                    target.add(feedbackPanel);

                } catch (Exception e) {
                    logger.warn("Impossible de créer le compte : " + e + "\n" + LogUtils.getStackTrace(e.getStackTrace()));
                    MarkupContainer panelContainer = this.getParent().getParent();
                    StringResourceModel errorMessageModel = new StringResourceModel("creation" + ".error", panelContainer, null);
                    // Affiche le message d'erreur
                    PanelUtils.showErrorMessage(errorMessageModel.getString(), e.toString(), feedbackPanel);
                    target.add(feedbackPanel);
                }
            }
        });

        add(form.setOutputMarkupId(true));

        add(new Image("panelIcon", new PackageResourceReference(getClass(), "../img/icon_user.png")));
    }

    /**
     * Retourne le lien vers la page "Conditions d'utilisation".
     * Ce lien est configurable via la table application_parameter : pages.createAccount.legal
     * @return Component
     */
    private Component getLegalLink() {
        Component link;
        ApplicationParameterDao applicationParameterDao = new ApplicationParameterDao(ThaleiaSession.get().getContextService().getNewContext());

        String legalUrl = applicationParameterDao.getValue(
                "pages.createAccount.legal", "");
        logger.info("Valeur de pages.createAccount.legal : " + legalUrl);

        if (legalUrl.length() == 0) {
            String srv = applicationParameterDao.getValue(
                    "server.url", "");
            legalUrl = srv + "legal";
        } else {
            logger.info("Valeur personnalisée pour les conditions d'utilisation du service thaleia de la page création de compte : " + legalUrl);
        }

        link = new ExternalLink("legalUrl", legalUrl);
        return link;
    }

    abstract protected void onOut();
}
