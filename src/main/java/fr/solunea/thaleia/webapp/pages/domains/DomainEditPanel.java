package fr.solunea.thaleia.webapp.pages.domains;

import fr.solunea.thaleia.model.Domain;
import fr.solunea.thaleia.model.dao.DomainDao;
import fr.solunea.thaleia.model.dao.DomainRightDao;
import fr.solunea.thaleia.utils.DetailedException;
import fr.solunea.thaleia.webapp.panels.PanelUtils;
import fr.solunea.thaleia.webapp.panels.ThaleiaFeedbackPanel;
import fr.solunea.thaleia.webapp.security.ThaleiaSession;
import org.apache.log4j.Logger;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.extensions.markup.html.form.palette.Palette;
import org.apache.wicket.extensions.markup.html.form.palette.component.Recorder;
import org.apache.wicket.markup.html.form.*;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.model.util.ListModel;
import org.apache.wicket.validation.IValidatable;
import org.apache.wicket.validation.IValidator;
import org.apache.wicket.validation.ValidationError;

import java.util.List;

public abstract class DomainEditPanel extends Panel {

    private static final Logger logger = Logger.getLogger(DomainEditPanel.class);

    protected ThaleiaFeedbackPanel feedbackPanel;

    public DomainEditPanel(String id, final IModel<Domain> model) {
        super(id, model);

        setDefaultModel(new CompoundPropertyModel<>(model.getObject()));

        DomainDao domainDao = new DomainDao(model.getObject().getObjectContext());
        DomainRightDao domainRightDao = new DomainRightDao(model.getObject().getObjectContext());

        feedbackPanel = new ThaleiaFeedbackPanel("feedback");
        feedbackPanel.setOutputMarkupId(true);
        add(feedbackPanel);

        Form<Domain> form = new Form<>("form", model);
        form.setOutputMarkupId(true);

        // Le nom
        form.add(new TextField<String>("name").setRequired(true).add(new UniqueNameValidator()));

        // S??lecteur de domaine parent
        DropDownChoice<Domain> domainChoice = PanelUtils.getDropDownChoice("parent", domainDao);
        domainChoice.setNullValid(true);
        form.add(domainChoice.setRequired(false));

        // La visibilit?? sur d'autres domaines :

        // La liste des domaines qui sont accessibles pour le domaine ??dit??.
        final IModel<List<Domain>> domainRightsOn = new ListModel<>(domainRightDao.getAccessibleDomains(model.getObject()));

        // La pr??sentation d'un domaine
        IChoiceRenderer<Domain> domainRenderer = new ChoiceRenderer<>("name", "name");

        // Le composant d'??dition des domaines accessibles
        final Palette<Domain> palette = new Palette<>("palette", domainRightsOn, new ListModel<>(domainDao.find(model.getObject())), domainRenderer, 10, true) {
            @Override
            protected Recorder<Domain> newRecorderComponent() {
                Recorder<Domain> recorder = super.newRecorderComponent();
                recorder.add(new AjaxFormComponentUpdatingBehavior("onchange") {

                    @Override
                    protected void onUpdate(AjaxRequestTarget target) {
                        // On enregistre la nouvelle liste de domainesaccessibles.
                        try {
                            domainRightDao.setAccessible(model.getObject(), domainRightsOn.getObject());
                            // On ne commite pas le contexte
                        } catch (DetailedException e) {
                            // TODO : pr??senter ce message ?? l'utilisateur.
                            logger.warn("Impossible de mettre ?? jour les visibilit??s des domaines : " + e);
                        }

                        // On remonte l'information de mise ?? jour
                        onPropertyChanged(target);
                    }
                });
                return recorder;
            }
        };

        form.add(palette);

        // Les boutons enregistrer et annuler
        form.add(getSaveButton(model));
        form.add(getCancelButton());

        add(form);
    }

    /**
     * M??thode appel??e lorsque la valeur d'un champ a ??t?? mise ?? jour.
     */
    protected abstract void onPropertyChanged(AjaxRequestTarget target);

    private Button getSaveButton(final IModel<Domain> objectModel) {
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
     * M??thode appel??e apr??s un enregistrement ou une annulation de
     * modification. Y placer une ??ventuelle redirection.
     */
    protected abstract void onOut();

    /**
     * V??rifie que le nom de ce domaine est unique.
     */
    public class UniqueNameValidator implements IValidator<String> {
        @Override
        public void validate(IValidatable<String> validatable) {

            // Le nom renseign?? dans le formulaire
            final String name = validatable.getValue();

            DomainDao domainDao = new DomainDao(ThaleiaSession.get().getContextService().getContextSingleton());
            if (domainDao.existsWithName(name, (Domain) DomainEditPanel.this.getDefaultModelObject())) {
                ValidationError error = new ValidationError();
                error.addKey("name.exists");
                validatable.error(error);
            }
        }
    }

}
