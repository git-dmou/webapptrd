package fr.solunea.thaleia.webapp.pages.admin.property;

import fr.solunea.thaleia.model.ContentProperty;
import fr.solunea.thaleia.model.ContentPropertyName;
import fr.solunea.thaleia.model.dao.ContentPropertyDao;
import fr.solunea.thaleia.model.dao.ValueTypeDao;
import fr.solunea.thaleia.utils.DetailedException;
import fr.solunea.thaleia.webapp.panels.PanelUtils;
import fr.solunea.thaleia.webapp.panels.ThaleiaFeedbackPanel;
import fr.solunea.thaleia.webapp.security.ThaleiaSession;
import org.apache.cayenne.ObjectContext;
import org.apache.log4j.Logger;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.PropertyListView;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.model.util.ListModel;

@SuppressWarnings("serial")
public abstract class ContentPropertyEditPanel extends Panel {

    private static final Logger logger = Logger.getLogger(ContentPropertyEditPanel.class);

    protected ThaleiaFeedbackPanel feedbackPanel;

    public ContentPropertyEditPanel(String id, IModel<ContentProperty> model) {
        super(id, model);

        setDefaultModel(new CompoundPropertyModel<>(model));

        feedbackPanel = new ThaleiaFeedbackPanel("feedback");
        feedbackPanel.setOutputMarkupId(true);
        add(feedbackPanel);

        Form<ContentProperty> form = new Form<>("form", model);
        form.setOutputMarkupId(true);

        // Le nom non localisé de la ContentProperty
        form.add(new TextField<String>("name"));

        // Le type de valeur pour la ContentProperty
        ValueTypeDao valueTypeDao = new ValueTypeDao(ThaleiaSession.get().getContextService().getContextSingleton());
        form.add(PanelUtils.getDropDownChoice("valueType", valueTypeDao));

        // Le tableau de tous les ContentPropertyName, pour associer un nom à
        // cette ContentProperty, pour chacun des ContentTYpe existants, et pour
        // chacune des locales.
        ContentPropertyDao contentPropertyDao = new ContentPropertyDao(ThaleiaSession.get().getContextService().getContextSingleton());
        form.add(new PropertyListView<>("objects",
                new ListModel<>(contentPropertyDao.getContentPropertyNames(model.getObject()))) {

            @Override
            protected void populateItem(final ListItem<ContentPropertyName> item) {
                item.add(new Label("contentTypeProperty.contentType.name"));
                item.add(new Label("locale.name"));
                item.add(new TextField<String>("name"));
                item.add(new CheckBox("contentTypeProperty.hidden"));
            }
        });

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
    protected abstract void onOut();
}
