package fr.solunea.thaleia.webapp.pages.admin.property;

import fr.solunea.thaleia.model.ContentProperty;
import fr.solunea.thaleia.model.ValueType;
import fr.solunea.thaleia.model.dao.ContentPropertyDao;
import fr.solunea.thaleia.model.dao.ValueTypeDao;
import fr.solunea.thaleia.utils.DetailedException;
import fr.solunea.thaleia.webapp.pages.admin.AdminPage;
import fr.solunea.thaleia.webapp.panels.PanelUtils;
import fr.solunea.thaleia.webapp.panels.ThaleiaFeedbackPanel;
import fr.solunea.thaleia.webapp.security.ThaleiaSession;
import org.apache.cayenne.ObjectContext;
import org.apache.log4j.Logger;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.validation.IValidatable;
import org.apache.wicket.validation.IValidator;
import org.apache.wicket.validation.ValidationError;

@SuppressWarnings("serial")
public class NewContentPropertyPanel extends Panel {

    private static final Logger logger = Logger.getLogger(NewContentPropertyPanel.class);

    protected ThaleiaFeedbackPanel feedbackPanel;

    public NewContentPropertyPanel(String id, final IModel<ContentProperty> model) {

        super(id, model);

        setDefaultModel(new CompoundPropertyModel<>(model));

        feedbackPanel = new ThaleiaFeedbackPanel("feedback");
        feedbackPanel.setOutputMarkupId(true);
        add(feedbackPanel);

        Form<ContentProperty> form = new Form<>("form", model);
        form.setOutputMarkupId(true);

        // Le nom, doit être unique
        form.add(new TextField<String>("name").setRequired(true).add(new UniquePropertyValidator()));

        // Sélecteur de type de propriété.
        ValueTypeDao valueTypeDao = new ValueTypeDao(ThaleiaSession.get().getContextService().getContextSingleton());
        DropDownChoice<ValueType> typeChoice = PanelUtils.getDropDownChoice("valueType", valueTypeDao);
        form.add(typeChoice.setRequired(true));

        // Les boutons enregistrer et annuler
        form.add(getSaveButton(model));
        form.add(getCancelButton());

        add(form);
    }

    private Button getSaveButton(final IModel<ContentProperty> objectModel) {
        return new Button("save") {

            public void onSubmit() {
                try {
                    ContentProperty contentProperty = objectModel.getObject();

                    // On enregistre les modifications
                    ObjectContext context = ThaleiaSession.get().getContextService().getNewContext();
                    ContentPropertyDao contentPropertyDao = new ContentPropertyDao(context);
                    contentPropertyDao.save(context.localObject(contentProperty), false);
                    context.commitChanges();

                    onOut();

                } catch (DetailedException e) {
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

    /**
     * Méthode appelée après un enregistrement ou une annulation de
     * modification. Y placer une éventuelle redirection.
     */
    protected void onOut() {
        setResponsePage(AdminPage.class);
    }

    /**
     * Vérifie que la propriété est unique.
     */
    public static class UniquePropertyValidator implements IValidator<String> {
        @Override
        public void validate(IValidatable<String> validatable) {

            // L'identifiant renseigné dans le formulaire
            final String name = validatable.getValue();

            ContentPropertyDao contentPropertyDao = new ContentPropertyDao(ThaleiaSession.get().getContextService().getContextSingleton());
            if (contentPropertyDao.findByName(name) != null) {
                ValidationError error = new ValidationError();
                error.addKey("property.name.exists");
                validatable.error(error);
            }

        }
    }

}
