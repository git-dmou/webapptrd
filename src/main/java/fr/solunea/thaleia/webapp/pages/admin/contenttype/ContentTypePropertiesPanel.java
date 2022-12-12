package fr.solunea.thaleia.webapp.pages.admin.contenttype;

import fr.solunea.thaleia.model.*;
import fr.solunea.thaleia.model.dao.*;
import fr.solunea.thaleia.utils.DetailedException;
import fr.solunea.thaleia.webapp.panels.PanelUtils;
import fr.solunea.thaleia.webapp.panels.ThaleiaFeedbackPanel;
import fr.solunea.thaleia.webapp.security.ThaleiaSession;
import org.apache.cayenne.ObjectContext;
import org.apache.log4j.Logger;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxCheckBox;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.PropertyListView;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.model.util.ListModel;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("serial")
public abstract class ContentTypePropertiesPanel extends Panel {

    private static final Logger logger = Logger.getLogger(ContentTypePropertiesPanel.class);
    private final IModel<List<TableItem>> items;
    protected ThaleiaFeedbackPanel feedbackPanel;

    public ContentTypePropertiesPanel(String id, final IModel<ContentType> model) {
        super(id, model);

        feedbackPanel = new ThaleiaFeedbackPanel("feedback");
        feedbackPanel.setOutputMarkupId(true);
        add(feedbackPanel);

        final Form<?> form = new Form<Void>("form");
        add(form);

        items = new ListModel<>(getTableItems());

        // Le tableau de présentation
        form.add(new PropertyListView<>("objects", items) {

            @Override
            protected void populateItem(final ListItem<TableItem> item) {
                final TableItem tableItem = ((TableItem) (item.getDefaultModelObject()));

                item.add(new Label("name"));

                item.add(new CheckBox("hidden") {
                    @Override
                    public boolean isEnabled() {
                        // "Caché" est désactivé si la ContentProperty n'est pas associée au ContentType.
                        return tableItem.isAssociated();
                    }
                });

                item.add(new AjaxCheckBox("associated") {
                    @Override
                    protected void onUpdate(AjaxRequestTarget target) {
                        // On met à jour l'objet ContentType
                        // logger.debug("Mise à jour de la sélection : "
                        // + tableItem.isAssociated());
                        target.add(form);
                    }
                });
            }
        }.setOutputMarkupId(true));

        // Les libellés
        form.add(new Label("nameLabel", new StringResourceModel("header.name",
                this, null)));
        form.add(new Label("hiddenLabel", new StringResourceModel(
                "header.hidden", this, null)));
        form.add(new Label("associatedLabel", new StringResourceModel(
                "header.associated", this, model, new Object[]{model
                .getObject().getName()})));

        // Les boutons enregistrer et annuler
        form.add(getSaveButton(model));
        form.add(getCancelButton());
    }

    private List<TableItem> getTableItems() {
        List<TableItem> result = new ArrayList<>();

        // Les ContentProperties traitées
        List<ContentProperty> added = new ArrayList<>();

        // On ajoute toutes les ContentProperties actuellement associées au
        // ContentType
        List<ContentTypeProperty> contentTypeProperties = ((ContentType) (this
                .getDefaultModelObject())).getProperties();
        for (ContentTypeProperty contentTypeProperty : contentTypeProperties) {
            TableItem item = new TableItem();
            item.setContentProperty(contentTypeProperty.getContentProperty());
            item.setAssociated(true);
            item.setHidden(contentTypeProperty.getHidden());
            result.add(item);
            added.add(contentTypeProperty.getContentProperty());
        }

        // On ajoute toutes les ContentProperties (sauf celles qu'on a déjà
        // placées dans la liste).
        ContentPropertyDao contentPropertyDao = new ContentPropertyDao(ThaleiaSession.get().getContextService().getContextSingleton());
        for (ContentProperty contentProperty : contentPropertyDao.find()) {
            if (!added.contains(contentProperty)) {
                TableItem item = new TableItem();
                item.setContentProperty(contentProperty);
                item.setAssociated(false);
                item.setHidden(false);
                result.add(item);
            }
        }

        return result;
    }

    private Button getSaveButton(final IModel<ContentType> objectModel) {
        return new Button("save") {

            public void onSubmit() {
                try {
                    ContentType contentType = objectModel.getObject();

                    // On travaille dans un contexte temporaire
                    ObjectContext context = ThaleiaSession.get().getContextService().getNewContext();
                    ContentType localContentType = context.localObject(contentType);
                    ContentTypeDao contentTypeDao = new ContentTypeDao(context);
                    ContentTypePropertyDao contentTypePropertyDao = new ContentTypePropertyDao(context);
                    LocaleDao localeDao = new LocaleDao(context);
                    ContentPropertyNameDao contentPropertyNameDao = new ContentPropertyNameDao(context);

                    // On supprime toutes les ContentProperty du ContentType
                    ThaleiaSession.get().getContentTypeService().deleteContentProperties(localContentType, context);

                    // On parcourt les item du tableau
                    for (TableItem item : items.getObject()) {
                        // On ajoute toutes les ContentProperty associées
                        if (item.associated) {
                            ContentTypeProperty contentTypeProperty = contentTypePropertyDao.get();
                            contentTypeProperty.setContentProperty(item.getContentProperty());
                            contentTypeProperty.setContentType(localContentType);
                            contentTypeProperty.setHidden(item.isHidden());

                            // On ajoute la localisation = le nom de la
                            // ContentProperty, dans toutes les locales.
                            for (Locale locale : localeDao.find()) {
                                ContentPropertyName contentPropertyName = contentPropertyNameDao.get();
                                contentPropertyName.setLocale(locale);
                                contentPropertyName.setContentTypeProperty(contentTypeProperty);
                                contentPropertyName.setName(contentTypeProperty.getContentProperty().getName());
                            }
                        }
                    }

                    // On enregistre les modifications du contexte temporaire en base.
                    contentTypeDao.save(contentType);
                    context.commitChanges();

                    onOut();

                } catch (DetailedException e) {
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
                onOut();
            }
        }.setDefaultFormProcessing(false);
    }

    /**
     * Méthode appelée à la fermeture du panneau.
     */
    protected abstract void onOut();

    private static class TableItem {
        private ContentProperty contentProperty;
        private boolean hidden;
        private boolean associated;

        @SuppressWarnings("unused")
        public String getName() {
            return contentProperty.getName();
        }

        public ContentProperty getContentProperty() {
            return contentProperty;
        }

        public void setContentProperty(ContentProperty contentProperty) {
            this.contentProperty = contentProperty;
        }

        public boolean isHidden() {
            return hidden;
        }

        public void setHidden(boolean hidden) {
            this.hidden = hidden;
        }

        public boolean isAssociated() {
            return associated;
        }

        public void setAssociated(boolean associated) {
            this.associated = associated;
        }
    }

}
