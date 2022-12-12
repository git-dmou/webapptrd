package fr.solunea.thaleia.webapp.pages.admin;

import fr.solunea.thaleia.model.Domain;
import fr.solunea.thaleia.model.User;
import fr.solunea.thaleia.model.dao.DomainDao;
import fr.solunea.thaleia.model.dao.UserDao;
import fr.solunea.thaleia.service.utils.Configuration;
import fr.solunea.thaleia.utils.DetailedException;
import fr.solunea.thaleia.utils.LogUtils;
import fr.solunea.thaleia.webapp.ThaleiaApplication;
import fr.solunea.thaleia.webapp.panels.LocaleSelectorPanel;
import fr.solunea.thaleia.webapp.panels.PanelUtils;
import fr.solunea.thaleia.webapp.panels.ThaleiaFeedbackPanel;
import fr.solunea.thaleia.webapp.security.ThaleiaSession;
import fr.solunea.thaleia.webapp.utils.BootstrapDateTimeField;
import org.apache.cayenne.ObjectContext;
import org.apache.log4j.Logger;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.*;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.*;
import org.apache.wicket.request.flow.RedirectToUrlException;

import java.util.List;

public abstract class UserEditPanel extends Panel {

    private static final Logger logger = Logger.getLogger(UserEditPanel.class);

    protected ThaleiaFeedbackPanel feedbackPanel;

    @SuppressWarnings("unchecked")
    public UserEditPanel(String id, final IModel<User> model) {
        super(id, model);

        try {
            addBtnBack();

            ObjectContext context = ThaleiaSession.get().getContextService().getNewContext();
            UserDao userDao = new UserDao(context);

            setDefaultModel(new CompoundPropertyModel<>(userDao.get(model.getObject().getObjectId())));

            add(new Label("login", model.getObject().getLogin()));

            feedbackPanel = new ThaleiaFeedbackPanel("feedbackPanel");
            feedbackPanel.setOutputMarkupId(true);
            add(feedbackPanel);

            Form<User> form = new Form<>("form", (IModel<User>) getDefaultModel());
            form.setOutputMarkupId(true);

            // Le nom
            TextField<String> name = (TextField<String>) new TextField<String>("name")
                    .setRequired(true)
                    .add(new AttributeAppender("placeholder",
                            new StringResourceModel("nameLabel", this, null)));
            form.add(name);

            // L'adresse de facturation
            TextField<String> postalAddress = (TextField<String>) new TextField<String>("postalAddress")
                    .setRequired(false)
                    .add(new AttributeAppender("placeholder",
                            new StringResourceModel("addressLabel", this, null)));
            form.add(postalAddress);

            // Envoi des newsletter ?
            // La newsletter est gérée depuis Mailjet. On ne s'en occupe plus dans Thaleia.
            //form.add(new CheckBox("newsletter"));

            // Les champs présentés si on est admin
            WebMarkupContainer adminRows = new WebMarkupContainer("adminRows") {
                @Override
                public boolean isVisible() {
                    // La présentation de cette option n'est possible que pour
                    // les administrateurs
                    return ThaleiaSession.get().getAuthenticatedUser().getIsAdmin();
                }
            };
            form.add(adminRows);

            adminRows.add(new DropDownChoice<>("domain", new LoadableDetachableModel<java.util.List<Domain>>() {
                @Override
                protected List<Domain> load() {
                    return new DomainDao(context).find();
                }
            }, new ChoiceRenderer<>() {
                @Override
                public Object getDisplayValue(Domain object) {
                    return new DomainDao(context).getDisplayName(object, ThaleiaSession.get().getLocale());
                }

                @Override
                public String getIdValue(Domain object, int index) {
                    return Integer.toString(new DomainDao(context).getPK(object));
                }
            }).setRequired(true));

            adminRows.add(new org.apache.wicket.markup.html.basic.Label("currentLicence", new AbstractReadOnlyModel<String>() {
                @Override
                public String getObject() {
                    try {
                        return ThaleiaSession.get().getLicenceService().getValidLicencesNames(model.getObject(), ThaleiaSession.get().getLocale(), " - ");
                    } catch (DetailedException e) {
                        logger.warn(e);
                        return "";
                    }
                }
            }));
            // Un avertissement si l'utilisateur n'a pas de licence
            adminRows.add(new WebMarkupContainer("licenceWarningMessage") {
                @Override
                public boolean isVisible() {
                    try {
                        return !ThaleiaSession.get().getLicenceService().isUserValid(model.getObject(), false);
                    } catch (DetailedException e) {
                        logger.warn(e);
                        return false;
                    }
                }
            }.setOutputMarkupId(true));

            // Administrateur ?
            adminRows.add(new CheckBox("isAdmin"));

            // Accès aux menus
            adminRows.add(new CheckBox("menuContents"));
            adminRows.add(new CheckBox("menuModules"));
            adminRows.add(new CheckBox("menuTools"));

            // Date d'expiration
            // DateTextField df = new DateTextField("expiration");
            // df.add(new DatePicker());
            BootstrapDateTimeField df = new BootstrapDateTimeField("expiration");
            adminRows.add(df);

            // Le bouton de connexion "en tant que"
            adminRows.add(new AjaxButton("proxyLogin") {
                @Override
                protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                    // Le compte utilisateur actuel
                    String initialLogin = ThaleiaSession.get().getAuthenticatedUser().getLogin();
                    if (ThaleiaSession.get().authenticateWithNoStats(((User) UserEditPanel.this.getDefaultModelObject()).getLogin())) {
                        throw new RedirectToUrlException(ThaleiaApplication.get().getApplicationRootUrl());
                    } else {
                        // On se reconnecte correctement
                        ThaleiaSession.get().authenticateWithNoStats(initialLogin);
                        target.add(feedbackPanel);
                    }
                }
            });

            // On indique si un mail sera envoyé avec le mot de passe.
            if (ThaleiaSession.get().getAuthenticatedUser().getIsAdmin() && model.getObject().getPassword() == null) {
                // Si pas de mot de passe en base, on en fixera un à
                // l'enregistrement, et on enverra un mail.
                form.add(new org.apache.wicket.markup.html.basic.Label("passwordCreationMessage", new StringResourceModel("passwordCreationMessage", this, null)));
            } else {
                form.add(new Label("passwordCreationMessage", ""));
            }

            // Les boutons enregistrer et annuler
            form.add(getSaveButton((IModel<User>) getDefaultModel(), form));
            form.add(getCancelButton(form));

            add(form);

            // Le panneau pour changer son mot de passe.
            // On ne le présente que si l'utilisateur édité est celui de la
            // session en cours ET ce n'est pas un compte Google
            add(new ChangePasswordPanel("changePasswordPanel", model, feedbackPanel) {
                @Override
                public boolean isVisible() {
                    User user = ThaleiaSession.get().getAuthenticatedUser();
                    return model.getObject().equals(user) && user.isInternalSignin();
                }
            });

            // Si admin, alors le panneau de gestion des licences
            add(new LicenceHoldingsPanel("licencesPanel", model) {
                @Override
                public boolean isVisible() {
                    // La présentation de cette option n'est possible que pour
                    // les administrateurs
                    return ThaleiaSession.get().getAuthenticatedUser().getIsAdmin();
                }
            });

            // Panneau de présentation des achats effectués
            add(new BuyProcessesPanel("buyProcessesPanel", (IModel<User>) UserEditPanel.this.getDefaultModel()));

            // Sélection de locale de l'IHM
            add(new LocaleSelectorPanel("localeSelector") {
                @Override
                public boolean isVisible() {
                    return true;
                }
            });

        } catch (Exception e) {
            logger.debug("Erreur : " + LogUtils.getStackTrace(e.getStackTrace()));
        }
    }

    private org.apache.wicket.markup.html.form.Button getSaveButton(final IModel<User> objectModel, Form<User> form) {
        return new AjaxButton("save", form) {
            @Override
            public void onSubmit(AjaxRequestTarget target, Form<?> form) {
                try {
                    User user = (User) getForm().getModelObject();
                    new UserDao(user.getObjectContext()).save(user, true);
                    onOut(target);
                } catch (DetailedException e) {
                    logger.warn("Impossible d'enregistrer l'objet : " + e);
                    MarkupContainer panelContainer = this.getParent().getParent();
                    StringResourceModel errorMessageModel = new StringResourceModel("save.error", panelContainer,
                            objectModel);
                    // Affiche le message d'erreur
                    PanelUtils.showErrorMessage(errorMessageModel.getString(), e.toString(), feedbackPanel);
                }
            }
        };
    }

    private Button getCancelButton(Form<User> form) {
        return new AjaxButton("cancel", form) {
            @Override
            public void onSubmit(AjaxRequestTarget target, Form<?> form) {
                onOut(target);
            }
        }.setDefaultFormProcessing(false);
    }

    /**
     * Méthode appelée après un enregistrement ou une annulation de modification. Y placer une éventuelle redirection.
     */
    protected abstract void onOut(AjaxRequestTarget target);

    /**
     * Ajout du bouton Retour.
     */
    private void addBtnBack() {
        add(new Link<>("btnBack") {
            public void onClick() {
                setResponsePage(ThaleiaApplication.get().getRedirectionPage(
                        Configuration.AUTHENTIFIED_USERS_WELCOME_PAGE,
                        Configuration.DEFAULT_AUTHENTIFIED_USERS_WELCOME_PAGE,
                        Configuration.HOME_MOUNT_POINT
                ));
            }
        });
    }
}
