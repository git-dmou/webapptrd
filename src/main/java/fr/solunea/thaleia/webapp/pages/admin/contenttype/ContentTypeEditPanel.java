package fr.solunea.thaleia.webapp.pages.admin.contenttype;

import fr.solunea.thaleia.model.ContentType;
import fr.solunea.thaleia.model.dao.ContentTypeDao;
import fr.solunea.thaleia.webapp.panels.PanelUtils;
import fr.solunea.thaleia.webapp.panels.ThaleiaFeedbackPanel;
import fr.solunea.thaleia.webapp.security.ThaleiaSession;
import org.apache.log4j.Logger;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.validation.IValidatable;
import org.apache.wicket.validation.IValidator;
import org.apache.wicket.validation.ValidationError;

@SuppressWarnings("serial")
public abstract class ContentTypeEditPanel extends Panel {

    private static final Logger logger = Logger
            .getLogger(ContentTypeEditPanel.class);

	protected ThaleiaFeedbackPanel feedbackPanel;

    public ContentTypeEditPanel(String id, final IModel<ContentType> model) {
        super(id, model);

        // On fabrique un contexte de session spécifique à cet objet à éditer.
        setDefaultModel(new CompoundPropertyModel<>(Model.of(
                new ContentTypeDao(
                        ThaleiaSession.get().getContextService().getNewContext())
                        .get(model.getObject().getObjectId()))));

        feedbackPanel = new ThaleiaFeedbackPanel("feedback");
        feedbackPanel.setOutputMarkupId(true);
        add(feedbackPanel);

        Form<ContentType> form = new Form<>("form", model);
        form.setOutputMarkupId(true);

        // Le nom, doit être unique
        form.add(new TextField<String>("name").setRequired(true).add(
                new UniqueValidator()));

        // Sélecteur de type de propriété.
        form.add(new CheckBox("isModuleType"));

        // Les boutons enregistrer et annuler
        form.add(getSaveButton(model));
        form.add(getCancelButton());

        add(form);
    }

    private Button getSaveButton(final IModel<ContentType> objectModel) {
        return new Button("save") {
            public void onSubmit() {
                try {
                    // On enregistre les modifications
                    objectModel.getObject().getObjectContext().commitChanges();
                    onOut();
                } catch (Exception e) {
                    logger.debug("Impossible d'enregistrer l'objet : "
                            + e.toString());
                    MarkupContainer panelContainer = this.getParent()
                            .getParent();
                    StringResourceModel errorMessageModel = new StringResourceModel(
                            "save.error", panelContainer, objectModel);
                    // Affiche le message d'erreur
                    PanelUtils.showErrorMessage(errorMessageModel.getString(),
                            e.toString(), feedbackPanel);
                }
            }
        };
    }

    private Button getCancelButton() {
        return new Button("cancel") {
            public void onSubmit() {
                ((ContentType) getDefaultModel().getObject()).getObjectContext().rollbackChangesLocally();
                onOut();
            }
        }.setDefaultFormProcessing(false);
    }

    /**
     * Méthode appelée après un enregistrement ou une annulation de
     * modification. Y placer une éventuelle redirection.
     */
    protected abstract void onOut();

    /**
     * Vérifie que la propriété est unique.
     */
    public class UniqueValidator implements IValidator<String> {
        @Override
        public void validate(IValidatable<String> validatable) {
            // Le nom renseigné dans le formulaire
            final String name = validatable.getValue();
            ContentTypeDao contentTypeDao = new ContentTypeDao(ThaleiaSession.get().getContextService().getContextSingleton());
            if (contentTypeDao.existsWithName(name, (ContentType) ContentTypeEditPanel.this.getDefaultModelObject())) {
                error(validatable, "name.exists");
            }
        }

        private void error(IValidatable<String> validatable, String errorKey) {
            ValidationError error = new ValidationError();
            error.addKey(errorKey);
            validatable.error(error);
        }
    }

}
