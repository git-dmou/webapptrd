package fr.solunea.thaleia.webapp.pages;

import fr.solunea.thaleia.model.User;
import fr.solunea.thaleia.model.dao.UserDao;
import fr.solunea.thaleia.service.TemplatedMailsService;
import fr.solunea.thaleia.service.UserService;
import fr.solunea.thaleia.utils.DetailedException;
import fr.solunea.thaleia.webapp.ThaleiaApplication;
import fr.solunea.thaleia.webapp.panels.ThaleiaFeedbackPanel;
import fr.solunea.thaleia.webapp.panels.ThaleiaLightFeedbackPanel;
import fr.solunea.thaleia.webapp.security.ThaleiaSession;
import fr.solunea.thaleia.webapp.utils.CSSClassRemover;
import fr.solunea.thaleia.webapp.utils.MessagesUtils;
import fr.solunea.thaleia.webapp.utils.SeriousPasswordValidator;
import org.apache.cayenne.ObjectContext;
import org.apache.log4j.Logger;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Page;
import org.apache.wicket.RestartResponseException;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.PasswordTextField;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.request.resource.PackageResourceReference;
import org.apache.wicket.util.string.StringValue;
import org.apache.wicket.validation.IValidationError;

import java.lang.reflect.Constructor;

public class ResetPasswordPage extends ThaleiaPageV6 {

    private static final Logger logger = Logger.getLogger(ResetPasswordPage.class);
    private final IModel<String> clearPasswordModel = new Model<>("");
    protected ThaleiaFeedbackPanel feedbackPanel;

    public ResetPasswordPage() {
        super();
        logger.debug("Appel de la page sans paramètre.");
        // Attention, cette méthode ne devrait être appelée que par des sous-classes, qui appelent addComponents().
    }

    public ResetPasswordPage(PageParameters parameters) {
        super(parameters);
        addComponents(parameters);
    }

    private void addComponents(PageParameters parameters) {
        // Un contexte d'édition de ces modifications, isolé des autres modifications en cours
        ObjectContext context = ThaleiaSession.get().getContextService().getNewContext();

        // Tentative de récupération de la requête de création de compte d'après le code de vérification
        IModel<User> userModel;
        try {
            userModel = parsePasswordResetCode(parameters, context);

        } catch (DetailedException e) {
            logger.info("Impossible d'ouvrir la page de réinitialisation du mot de passe : " + e);
            // On redirige sur la page d'accueil de Thaleia.
            error(MessagesUtils.getLocalizedMessage("invalid.code", ResetPasswordPage.class, (Object[]) null));
            throw new RestartResponseException(ThaleiaApplication.get().getHomePage());
        }

        add(new Image("panelIcon", new PackageResourceReference(getClass(), "img/icon_password.png")));

        final Label instructions = new Label("instructions", new StringResourceModel("instructions", this, null));
        add(instructions);

        feedbackPanel = new ThaleiaFeedbackPanel("feedback");
        add(feedbackPanel.setOutputMarkupId(true));

        add(new Label("pageLabel", new StringResourceModel("pageLabel", this, null)));

        final Form<Void> form = new Form<>("form");

        // Le mot de passe à saisir
        final TextField<String> password = new PasswordTextField("password", clearPasswordModel) {
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
        form.add(password.setRequired(true).add(new SeriousPasswordValidator()));
        password.add(new AttributeModifier("placeholder", new StringResourceModel("password.default.text", this,
                null)));
        add(form);

        // Le lien pour ce connecter, si réussite de la réinitilisation du mot de passe
        Link<Void> goToLoginLink = new Link<>("goToLoginLink") {
            @Override
            public void onClick() {
                // On passe par le login de l'application
                Class<? extends Page> loginPageClass = ThaleiaApplication.get().getLoginPage();
                try {
                    // on instancie la page d'accueil sans redirection
                    Constructor<? extends Page> constructor = loginPageClass.getConstructor();
                    Page loginPage = constructor.newInstance();
                    setResponsePage(loginPage);
                } catch (Exception e) {
                    logger.warn(e);
                }
            }
        };
        form.add(goToLoginLink);
        goToLoginLink.setVisible(false);

        Button saveButton = new Button("save") {
            public void onSubmit() {
                try {
                    UserService userService = ThaleiaSession.get().getUserService();

                    // L'utilisateur existe-t-il ?
                    if (userService.isUserNameExists(userModel.getObject().getLogin())) {

                        // On réinitialise le mot de passe
                        userService.setPassword(userModel.getObject().getLogin(), password.getDefaultModelObjectAsString(), context);

                        // Affiche le message d'envoi effectué
                        feedbackPanel.success(ResetPasswordPage.this.getString("password.saved"));

                        // On cache les instructions
                        instructions.setVisible(false);

                        // On désactive le bouton d'enregistrement
                        this.setVisible(false);
                        // Et le champ password
                        password.setVisible(false);
                        // On montre le lien vers la page de login
                        goToLoginLink.setVisible(true);

                        form.add(AttributeModifier.append("class", "has-success"));

                    } else {
                        // Non : on l'indique
                        feedbackPanel.error(ResetPasswordPage.this.getString("login.notfound"));
                    }

                } catch (Exception e) {
                    logger.warn("Impossible d'envoyer un mail de réinitialisation de mot de passe : " + e);
                    feedbackPanel.error(ResetPasswordPage.this.getString("internal.error"));
                }
            }
        };
        saveButton.add(new Label("submitLabel", new StringResourceModel("send.button", this, null)));
        form.add(saveButton.add(new AttributeModifier("value", new StringResourceModel("send.button", this, null))));

    }

    private IModel<User> parsePasswordResetCode(PageParameters parameters, ObjectContext context) throws DetailedException {
        // On récupère le code
        StringValue passwordResetCode = parameters.get(TemplatedMailsService.PASSWORD_RESET_REQUEST_CODE_PARAMETER_NAME);
        if (passwordResetCode == null) {
            throw new DetailedException("Le paramètre " + TemplatedMailsService.PASSWORD_RESET_REQUEST_CODE_PARAMETER_NAME + " est vide !");
        }

        // On recherche le compte utilisateur associé à ce code
        User user = new UserDao(context).findByPasswordResetCode(passwordResetCode.toString());
        if (user == null) {
            throw new DetailedException("Le code de validation " + passwordResetCode.toString() + " n'est pas valide !");
        }

        return Model.of(user);
    }
}
