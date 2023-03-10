package fr.solunea.thaleia.webapp.pages;

import fr.solunea.thaleia.model.User;
import fr.solunea.thaleia.model.dao.UserDao;
import fr.solunea.thaleia.service.UserService;
import fr.solunea.thaleia.webapp.ThaleiaApplication;
import fr.solunea.thaleia.webapp.panels.ThaleiaFeedbackPanel;
import fr.solunea.thaleia.webapp.panels.ThaleiaLightFeedbackPanel;
import fr.solunea.thaleia.webapp.security.ThaleiaSession;
import fr.solunea.thaleia.webapp.utils.CSSClassRemover;
import org.apache.log4j.Logger;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Page;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.request.resource.PackageResourceReference;
import org.apache.wicket.validation.IValidationError;
import org.apache.wicket.validation.validator.EmailAddressValidator;

import java.lang.reflect.Constructor;
import java.util.Calendar;

@SuppressWarnings("serial")
public class ResetPasswordRequestPage extends ThaleiaPageV6 {

    private static final Logger logger = Logger.getLogger(ResetPasswordRequestPage.class);

    protected ThaleiaFeedbackPanel feedbackPanel;

    public ResetPasswordRequestPage() {
        super();

        add(new Image("panelIcon", new PackageResourceReference(getClass(), "img/icon_password.png")));

        final Label instructions = new Label("instructions", new StringResourceModel("instructions", this, null));
        add(instructions);

        feedbackPanel = new ThaleiaLightFeedbackPanel("feedback");
        add(feedbackPanel.setOutputMarkupId(true));

        add(new Label("pageLabel", new StringResourceModel("pageLabel", this, null)));

        final Form<Void> form = new Form<>("form");

        // L'identifiant : une adresse mail
        final TextField<String> login = new TextField<>("username", Model.of("")) {
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
        form.add(login.setRequired(true).add(EmailAddressValidator.getInstance()));
        login.add(new AttributeModifier("placeholder", new StringResourceModel("login.default.text", this, null)));
        add(form);

        // Le lien pour ce connecter, si r??ussite de la r??initilisation du mot
        // de passe
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

                    // On met l'identifiant demand?? en minuscules
                    String username = login.getDefaultModelObjectAsString().toLowerCase();

                    // L'utilisateur existe-t-il ?
                    if (userService.isUserNameExists(username)) {

                        // Il n'a pas de licence valide : on ajoute un
                        // message d'avertissement
                        User user = new UserDao(ThaleiaSession.get().getContextService().getContextSingleton()).findUserByLogin(username);

                        if (user != null && !ThaleiaSession.get().getLicenceService().isUserValid(user, false)) {
                            error(ResetPasswordRequestPage.this.getString("no.valid.licence"));
                        }

                        // Le compte a expir?? : on avertit
                        if (user != null && (user.getExpiration() != null) && (user.getExpiration().before(Calendar.getInstance().getTime()))) {
                            error(ResetPasswordRequestPage.this.getString("expired.account"));
                        }

                        // On r??initialise le mot de passe
                        userService.passwordResetRequest(username, ThaleiaSession.get().getLocale());

                        // Affiche le message d'envoi effectu??
                        feedbackPanel.success(ResetPasswordRequestPage.this.getString("email.sent"));

                        // On cache les instructions
                        instructions.setVisible(false);

                        // On d??sactive le bouton d'envoi
                        this.setVisible(false);
                        // Et le champ login
                        login.setEnabled(false);
                        // On cache le lien vers la page de login
                        goToLoginLink.setVisible(false);

                        form.add(AttributeModifier.append("class", "has-success"));

                    } else {
                        // Non : on indique malgr?? tout le m??me message afin d'??viter tout scan d'adresse mail.
                        feedbackPanel.error(ResetPasswordRequestPage.this.getString("email.sent"));
                    }

                } catch (Exception e) {
                    logger.warn("Impossible d'envoyer un mail de r??initialisation de mot de passe : " + e);
                    feedbackPanel.error(ResetPasswordRequestPage.this.getString("internal.error"));
                }
            }
        };
        saveButton.add(new Label("submitLabel", new StringResourceModel("send.button", this, null)));
        form.add(saveButton.add(new AttributeModifier("value", new StringResourceModel("send.button", this, null))));

    }
}
