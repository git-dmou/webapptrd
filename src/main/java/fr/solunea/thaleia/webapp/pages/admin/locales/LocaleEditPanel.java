package fr.solunea.thaleia.webapp.pages.admin.locales;

import fr.solunea.thaleia.model.Locale;
import fr.solunea.thaleia.model.dao.LocaleDao;
import fr.solunea.thaleia.utils.DetailedException;
import fr.solunea.thaleia.webapp.panels.PanelUtils;
import fr.solunea.thaleia.webapp.panels.ThaleiaFeedbackPanel;
import fr.solunea.thaleia.webapp.security.ThaleiaSession;
import org.apache.cayenne.ObjectContext;
import org.apache.log4j.Logger;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.authorization.Action;
import org.apache.wicket.authroles.authorization.strategies.role.annotations.AuthorizeAction;
import org.apache.wicket.markup.html.form.Button;
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

@AuthorizeAction(action = Action.RENDER, roles = {"admin"})
public abstract class LocaleEditPanel extends Panel {

    private static final Logger logger = Logger.getLogger(LocaleEditPanel.class);

	protected ThaleiaFeedbackPanel feedbackPanel;

    public LocaleEditPanel(String id, final IModel<Locale> model) {
        super(id, model);

        setDefaultModel(new CompoundPropertyModel<>(model));

        feedbackPanel = new ThaleiaFeedbackPanel("feedback");
        feedbackPanel.setOutputMarkupId(true);
        add(feedbackPanel);

        Form<Locale> form = new Form<>("form", model);
        form.setOutputMarkupId(true);

        // Le nom
        form.add(new TextField<String>("name").setRequired(true).add(new UniqueNameValidator()));

        // Les boutons enregistrer et annuler
        form.add(getSaveButton(model));
        form.add(getCancelButton());

        add(form);
    }

    private Button getSaveButton(final IModel<Locale> objectModel) {
        return new Button("save") {

            public void onSubmit() {
                try {
                    objectModel.getObject().getObjectContext().commitChanges();
                    onOut();

                } catch (Exception e) {
                    logger.warn("Impossible d'enregistrer l'objet : " + e.toString());
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
    protected abstract void onOut();

    /**
     * Vérifie que le nom de ce domaine est unique.
     */
    public class UniqueNameValidator implements IValidator<String> {
        @Override
        public void validate(IValidatable<String> validatable) {

            // Le nom renseigné dans le formulaire
            final String name = validatable.getValue();

            LocaleDao localeDao = new LocaleDao(ThaleiaSession.get().getContextService().getContextSingleton());
            if (localeDao.existsWithName(name, (Locale) LocaleEditPanel.this.getDefaultModelObject())) {
                ValidationError error = new ValidationError();
                error.addKey("name.exists");
                validatable.error(error);
            }
        }
    }

}
