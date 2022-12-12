package fr.solunea.thaleia.webapp.pages.admin;

import fr.solunea.thaleia.utils.DetailedException;
import fr.solunea.thaleia.webapp.panels.confirmation.ConfirmationLink;
import fr.solunea.thaleia.webapp.security.ThaleiaSession;
import org.apache.log4j.Logger;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.authorization.Action;
import org.apache.wicket.authroles.authorization.strategies.role.annotations.AuthorizeAction;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.StringResourceModel;

@SuppressWarnings("serial")
@AuthorizeAction(action = Action.RENDER, roles = {"admin"})
public class ResetPanel extends Panel {

    private static final Logger logger = Logger.getLogger(ResetPanel.class);

    private WebMarkupContainer progress;

    public ResetPanel(String id) {
        super(id);

        // Le formulaire
        Form<Void> form = new Form<>("resetForm");
        add(form);

        // Le texte "Remise à zéro des contenus"
        // form.add(new Label("btnResetLabel", new StringResourceModel(
        // "btnResetLabel", ResetPanel.this, null)));

        // Bouton de remise à zéro
        form.add(new ConfirmationLink<Void>("resetBtn", new StringResourceModel("reset.confirm", ResetPanel.this,
				null)) {

            @Override
            public void onClick(AjaxRequestTarget target) {

                try {
                    // Remise à zéro des contenus
                    ThaleiaSession.get().getContentService().deleteAllContents();

                    StringResourceModel messageModel = new StringResourceModel("reset.ok", ResetPanel.this, null);
                    ThaleiaSession.get().info(messageModel.getString());

                } catch (DetailedException e) {
                    StringResourceModel errorMessageModel = new StringResourceModel("reset.error", ResetPanel.this,
							null);
                    ThaleiaSession.get().error(errorMessageModel.getString());

                    // On journalise le détail technique.
                    logger.warn("Impossible de supprimer les contenus de la base : " + e.toString());
                }

                // On masque l'indicateur de progression
                progress.add(new AttributeModifier("style", "display: none;"));
                target.add(progress);

                // On recharge la page.
                setResponsePage(target.getPage());
            }
        }.add(new AttributeModifier("value",
                new StringResourceModel("btnResetLabel", ResetPanel.this, null))));

        // L'indicateur de progression.
        // Au chargement de la page, il est masqué par un attribut
        // style="display: none;". Il est affiché par un morceau de Javascript
        // déclenché lors du changement de valeur du champ "fileipnut" (cf code
        // HTML). Ce sera à la fin du traitement qu'il sera à nouveau masqué par
        // du code Java et le chargement de la réponse.
        add(progress = (WebMarkupContainer) new WebMarkupContainer("progress").setOutputMarkupId(true));
        progress.add(new Label("progressLabel",
                new StringResourceModel("progressLabel", this, null)).setOutputMarkupId(true));
    }
}
