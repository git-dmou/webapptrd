package fr.solunea.thaleia.webapp.pages.admin;

import fr.solunea.thaleia.model.User;
import fr.solunea.thaleia.utils.DetailedException;
import fr.solunea.thaleia.webapp.panels.PanelUtils;
import fr.solunea.thaleia.webapp.panels.ThaleiaFeedbackPanel;
import fr.solunea.thaleia.webapp.security.ThaleiaSession;
import org.apache.log4j.Logger;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.PasswordTextField;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;

public class ChangePasswordPanel extends Panel {

    private static final Logger logger = Logger
            .getLogger(ChangePasswordPanel.class);

    public ChangePasswordPanel(String id, final IModel<User> model, final ThaleiaFeedbackPanel feedbackPanel) {
        super(id, model);

        final IModel<String> clearPasswordModel = Model.of("");

        Form<Void> form = new Form<>("form");

        final Component password = new PasswordTextField("newPassword", clearPasswordModel).add(
                new AttributeModifier("placeholder", new StringResourceModel(
                        "password.field.default.text", this, null)))
                .setOutputMarkupId(true);
        form.add(password);

        Button button = new AjaxButton("changePassword") {
            @Override
            protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                try {
                    // On enregistre le changement de mot de passe
                    ThaleiaSession.get().getUserService().setPassword(model.getObject().getLogin(), clearPasswordModel.getObject());

                    StringResourceModel errorMessageModel = new StringResourceModel("save.ok", ChangePasswordPanel.this, null);

                    // Affiche le message de confirmation
                    ThaleiaSession.get().info(errorMessageModel.getString());
                    target.add(feedbackPanel);

                    // Remet à zéro le champ de mise à jour du mot de passe
                    clearPasswordModel.setObject("");
                    target.add(password);

                } catch (DetailedException e) {
                    logger.warn("Impossible de modifier le mot de passe : " + e.toString());
                    MarkupContainer panelContainer = this.getParent().getParent();
                    StringResourceModel errorMessageModel = new StringResourceModel("save.error", panelContainer, model);
                    // Affiche le message d'erreur
                    PanelUtils.showErrorMessage(errorMessageModel.getString(), e.toString(), feedbackPanel);
                }
            }
        };
        button.add(new Label("changePasswordLabel", new StringResourceModel("changePasswordLabel", this, null)));
        form.add(button);

        add(form);
    }
}
