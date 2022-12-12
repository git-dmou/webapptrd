package fr.solunea.thaleia.webapp.pages;

import fr.solunea.thaleia.model.Licence;
import fr.solunea.thaleia.model.User;
import fr.solunea.thaleia.model.dao.ApplicationParameterDao;
import fr.solunea.thaleia.service.LicenceService;
import fr.solunea.thaleia.service.utils.Configuration;
import fr.solunea.thaleia.webapp.ThaleiaApplication;
import fr.solunea.thaleia.webapp.pages.admin.AdminPage;
import fr.solunea.thaleia.webapp.pages.demo.CreateAccountWithLicencePage;
import fr.solunea.thaleia.webapp.panels.LocaleSelectorPanel;
import fr.solunea.thaleia.webapp.panels.ThaleiaLightFeedbackPanel;
import fr.solunea.thaleia.webapp.security.ThaleiaSession;
import fr.solunea.thaleia.webapp.utils.CSSClassRemover;
import org.apache.cayenne.ObjectContext;
import org.apache.log4j.Logger;
import org.apache.wicket.*;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.authroles.authentication.panel.SignInPanel;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.PasswordTextField;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.markup.html.image.InlineImage;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.html.panel.EmptyPanel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.request.resource.PackageResourceReference;
import org.apache.wicket.validation.IValidationError;

@SuppressWarnings("serial")
public class ThaleiaSignInPanel extends SignInPanel {

    protected static final Logger logger = Logger.getLogger(ThaleiaSignInPanel.class.getName());

    public ThaleiaSignInPanel(String id) {
        super(id);

        getForm().add(getLogo());
        add(getBackground());
        setFormPosition();

        ThaleiaLightFeedbackPanel feedbackPanel = new ThaleiaLightFeedbackPanel("feedback");
        feedbackPanel.setOutputMarkupId(true);
        addOrReplace(feedbackPanel);

        getForm().add(new Label("signIn", new StringResourceModel("signIn", this, null)));

        // Le bouton de réinitialisation du mot de passe
        getForm().addOrReplace(new Link<Void>("passwordReset") {
            @Override
            public void onClick() {
                setResponsePage(ResetPasswordRequestPage.class);
            }
        }.addOrReplace(new Label("passwordResetLabel", new StringResourceModel("resetPassword", this, null))));

        // La case à cocher "Se souvenir de moi"
        ((MarkupContainer) get("signInForm:rememberMeRow")).add(new Label("rememberMeLabel", new StringResourceModel(
                "rememberMeLabel", this, null)));

        // Saisie de l'indentifiant
        getForm().add(new Label("usernameLabel", new StringResourceModel("usernameLabel", this, null)));
        getForm().addOrReplace(new TextField<String>("username") {
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
        });

        // Saisie du mot de passe
        getForm().add(new Label("passwordLabel", new StringResourceModel("passwordLabel", this, null)));
        getForm().addOrReplace(new PasswordTextField("password") {
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
        });

        get("signInForm:username").add(new AttributeModifier("placeholder", new StringResourceModel(
                "login.default" + ".text", this, null)));
        get("signInForm:password").add(new AttributeModifier("placeholder", new StringResourceModel(
                "password.default" + ".text", this, null)));

        AjaxButton submitButton = new AjaxButton("submitButton") {
            @Override
            protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                super.onSubmit(target, form);
                target.add(feedbackPanel);
            }
        };
        submitButton.add(new Label("signInButton", new StringResourceModel("signInButton", this, null)));
        getForm().add(submitButton);

        addAccountCreation();

    }

    /**
     * Retourne le logo Thaliea ou du client pour la page.
     * @detail Le logo peut être personnalisé en stockant la base64 de l'image dans la table application_parameter
     * @return logo Thaliea ou client pour la page.
     */
    private Image getLogo() {
        ObjectContext context = ThaleiaSession.get().getContextService().getNewContext();
        ApplicationParameterDao applicationParameterDao = new ApplicationParameterDao(context);
        String customLogoBase64 = applicationParameterDao.getValue("page.login.logo.base64", "");
        String customLogoStyle = applicationParameterDao.getValue("page.login.logo.style", "");

        Image logo;
        if(customLogoBase64.trim().length() > 0) {
            logo = new Image("thaleia-logo", "");
            logo.add(new AttributeModifier("src", customLogoBase64));
            if(customLogoStyle.trim().length() > 0) {
                logo.add(new AttributeAppender("style", new Model(customLogoStyle), " "));
            }
        } else {
            logo = new Image("thaleia-logo", new PackageResourceReference(BasePage.class,
                    "../img/logo_thaleia.png"));
        }

        return logo;
    }

    /**
     * Retourne le background de la page.
     * @detail Le background peut être personnalisé en stockant la base64 de l'image dans la table application_parameter
     * @return background de la page.
     */
    private Component getBackground() {
        ObjectContext context = ThaleiaSession.get().getContextService().getNewContext();
        ApplicationParameterDao applicationParameterDao = new ApplicationParameterDao(context);
        String backgroundBase64 = applicationParameterDao.getValue("page.login.background.base64", "");
        String backgroundStyle = applicationParameterDao.getValue("page.login.background.style", "");
        String backgroundClass = applicationParameterDao.getValue("page.login.background.class", "");

        Component background;
        if(backgroundBase64.trim().length() > 0) {
            background = new ThaleiaSignInPanelBackgroundCustomImg("background", backgroundBase64, backgroundStyle, backgroundClass);
        } else {
            background = new ThaleiaSignInPanelBackground("background");
        }

        return background;
    }

    /**
     * Ajustement de la position du formulaire.
     * Permet de ne pas masquer certains éléments du background.
     */
    private void setFormPosition() {
        ObjectContext context = ThaleiaSession.get().getContextService().getNewContext();
        ApplicationParameterDao applicationParameterDao = new ApplicationParameterDao(context);
        String style = applicationParameterDao.getValue("page.login.form.style", "");

        if(style.trim().length() > 0) {
            getForm().add(new AttributeModifier("style", style));
        }
    }

    public static Page getAccountCreationPage() {
        // Magouille pas propre. On devrait appeler une méthode de
        // sortie au niveau de la classe (qui serait abstraite),
        // surdéfinie au moment de l'instanciation de la page. Mais cela
        // fonctionne mal (perte des CSS)

        // Donc, si admin, on envoie sur la page d'admin, sinon sur la
        // page d'accueil.

        // Récupération du nom de la licence de démo par défaut pour cette instance.
        String licenceName = ThaleiaApplication.get().getApplicationParameterDao().getValue("self.signin.default.licence", LicenceService.LICENCE_NAME_DEMO_CANNELLE);
        Licence licence = ThaleiaApplication.get().getLicenceDao().findByName(licenceName);
        if (licence == null) {
            logger.warn("Le paramètre définissant la licence par défaut pour les création de compte (self.signin.default.licence) " +
                    "a rendu la valeur '" + licenceName + "', mais ce n'est pas un nom de licence valide.");
            throw new RestartResponseException(ErrorPage.class);
        }

        return new CreateAccountWithLicencePage("", licence) {
            @Override
            protected void onOut() {
                User user = ThaleiaSession.get().getAuthenticatedUser();
                if (user != null && user.getIsAdmin()) {
                    setResponsePage(AdminPage.class);
                } else {
                    setResponsePage(ThaleiaApplication.get().getHomePage());
                }
            }
        };
    }

    @Override
    protected void onSignInSucceeded() {
        // On ne trace pas l'erreur l'identification pour les stats, car elle
        // est déjà tracée par ThaleiaSession.authenticate()
        super.onSignInSucceeded();
    }

    @Override
    protected void onSignInFailed() {
        // On ne trace pas l'erreur l'identification pour les stats, car elle
        // est déjà tracée par ThaleiaSession.authenticate()
        super.onSignInFailed();
    }

    private void addAccountCreation() {
        // Le lien de création de compte
        WebMarkupContainer accountCreation = (WebMarkupContainer) new WebMarkupContainer("accountCreationPanel") {
            @Override
            public boolean isVisible() {
                String authorizeAccountsCreation =
                        new ApplicationParameterDao(ThaleiaApplication.get().contextService.getContextSingleton())
                                .getValue(Configuration.AUTHORIZE_ACCOUNT_CREATION, "");
                return authorizeAccountsCreation.toLowerCase().equals("true");

            }
        };

        String customUrl = ThaleiaApplication.get().getApplicationParameterDao().getValue(
                "pages.login.signInPanel.createAccount", "");

        if (customUrl.length() == 0) {
            accountCreation.add(new Link<Void>("accountCreation") {
                @Override
                public void onClick() {
                    setResponsePage(getAccountCreationPage());
                }
            }.add(new Label("accountCreationLabel", new StringResourceModel("accountCreation",
                    this, null))));
        } else {
            // changer l'url
            accountCreation.add(new ExternalLink("accountCreation", customUrl)
                    .add(new Label("accountCreationLabel", new StringResourceModel("accountCreation",
                    this, null))));
        }


        getForm().add(accountCreation);
    }

}
