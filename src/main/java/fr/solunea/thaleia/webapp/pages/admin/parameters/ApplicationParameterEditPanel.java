package fr.solunea.thaleia.webapp.pages.admin.parameters;

import fr.solunea.thaleia.model.ApplicationParameter;
import fr.solunea.thaleia.webapp.panels.PanelUtils;
import fr.solunea.thaleia.webapp.panels.ThaleiaFeedbackPanel;
import fr.solunea.thaleia.webapp.security.ThaleiaSession;
import org.apache.log4j.Logger;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;

@SuppressWarnings("serial")
public abstract class ApplicationParameterEditPanel extends Panel {

    private static final Logger logger = Logger.getLogger(ApplicationParameterEditPanel.class);

	protected ThaleiaFeedbackPanel feedbackPanel;

    public ApplicationParameterEditPanel(String id, IModel<ApplicationParameter> model) {
        super(id, model);

        // On fabrique un contexte de session spécifique à cet objet à éditer.
        setDefaultModel(new CompoundPropertyModel<>(Model.of(model.getObject())));

        feedbackPanel = new ThaleiaFeedbackPanel("feedback");
        feedbackPanel.setOutputMarkupId(true);
        add(feedbackPanel);

        Form<ApplicationParameter> form = new Form<>("form", model);
        form.setOutputMarkupId(true);

        form.add(new TextField<String>("name"));
        form.add(new TextField<String>("value"));

        // Les boutons enregistrer et annuler
        form.add(getSaveButton(model));
        form.add(getCancelButton());

        add(form);
    }

    private Button getSaveButton(final IModel<ApplicationParameter> objectModel) {
        return new Button("save") {

            public void onSubmit() {
                try {
                    objectModel.getObject().getObjectContext().commitChanges();

                    onOut();
                } catch (Exception e) {
                    logger.debug("Impossible d'enregistrer l'objet : " + e.toString());
                    MarkupContainer panelContainer = this.getParent().getParent();
                    StringResourceModel errorMessageModel = new StringResourceModel("save.error", panelContainer, objectModel);
                    // Affiche le message d'erreur
                    PanelUtils.showErrorMessage(errorMessageModel.getString(), e.toString(), feedbackPanel);
                }
            }
        };
    }

    private Button getCancelButton() {
        return new Button("cancel") {
            public void onSubmit() {
                onOut();
            }
        }.setDefaultFormProcessing(false);
    }

    protected abstract void onOut();
}
