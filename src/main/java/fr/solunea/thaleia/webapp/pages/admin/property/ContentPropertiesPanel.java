package fr.solunea.thaleia.webapp.pages.admin.property;

import fr.solunea.thaleia.model.ContentProperty;
import fr.solunea.thaleia.model.dao.ContentPropertyDao;
import fr.solunea.thaleia.utils.DetailedException;
import fr.solunea.thaleia.webapp.ThaleiaApplication;
import fr.solunea.thaleia.webapp.panels.confirmation.ConfirmationLink;
import fr.solunea.thaleia.webapp.security.ThaleiaSession;
import org.apache.cayenne.ObjectContext;
import org.apache.log4j.Logger;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.PropertyListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;

import java.util.List;

@SuppressWarnings("serial")
public class ContentPropertiesPanel extends Panel {

    private static final Logger logger = Logger.getLogger(ContentPropertiesPanel.class);

    /**
     * @param properties les propriétés que l'on veut présenter dans le panneau.
     * @param canEdit    peut-on modifier une valeur ?
     * @param canDelete  peut-on supprimer une propriété ?
     * @param canAdd     peut-on ajouter une propriété ?
     */
    public ContentPropertiesPanel(String id, IModel<List<ContentProperty>> properties, final boolean canEdit, final
    boolean canDelete, final boolean canAdd) {
        super(id);

        setOutputMarkupId(true);

        // La popup modale de création
        final ModalWindow modal = new ModalWindow("modal");
        ThaleiaApplication.get().configureModalPopup(modal);
        add(modal);

        ContentPropertyDao contentPropertyDao = new ContentPropertyDao(ThaleiaSession.get().getContextService().getContextSingleton());

        add(new PropertyListView<>("objects", properties) {

            @Override
            protected void populateItem(final ListItem<ContentProperty> item) {

                item.add(new Label("name"));
                item.add(new Label("valueType.name"));

                // Le lien "Modifier".
                item.add(new AjaxLink<Void>("edit") {
                    @Override
                    public void onClick(AjaxRequestTarget ajaxRequestTarget) {
                        // Désactivation du message d'avertissement du
                        // navigateur à la fermeture.
                        ajaxRequestTarget.appendJavaScript("Wicket.Window.unloadConfirmation = false;");
                        modal.setContent(new ContentPropertyEditPanel(modal.getContentId(), item.getModel()) {
                            @Override
                            protected void onOut() {
                                setResponsePage(this.findPage());
                            }
                        });
                        modal.show(ajaxRequestTarget);
                    }
                }.setVisible(canEdit));

                // Le lien supprimer
                item.add(new ConfirmationLink<Void>("delete", new StringResourceModel("delete.confirm", this, null,
                        new Object[]{contentPropertyDao.getDisplayName(item.getModelObject()
                                , ThaleiaSession.get().getLocale())})) {

                    @Override
                    public boolean isVisible() {
                        try {
                            // De toute façon, non si le constructeur ne le
                            // permet pas
                            if (!canDelete) {
                                return false;
                            }

                            // Après on vérifie que la suppression est
                            // effectivement possible.
                            return contentPropertyDao.canBeDeleted(item.getModelObject());
                        } catch (Exception e) {
                            logger.warn("Erreur durant l'analyse d'un objet : " + e);
                            return false;
                        }
                    }

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        try {
                            // Suppression de l'objet
                            ContentProperty contentProperty = item.getModelObject();
                            // Suppression de l'objet
                            ObjectContext context = ThaleiaSession.get().getContextService().getNewContext();
                            ContentPropertyDao contentPropertyDao1 = new ContentPropertyDao(context);
                            contentPropertyDao1.delete(context.localObject(contentProperty), false);
                            context.commitChanges();

                        } catch (DetailedException e) {
                            StringResourceModel errorMessageModel = new StringResourceModel("delete.error", item
                                    .getModel());
                            ThaleiaSession.get().error(errorMessageModel.getString());

                            // On journalise le détail technique.
                            logger.warn("Impossible de supprimer l'objet de type '"
                                    + item.getModelObject().getClass().getName() + "' : " + e.toString());
                        }
                        // On recharge la page.
                        setResponsePage(target.getPage());
                    }
                });
            }
        }.setOutputMarkupId(true));

        // Le lien "Nouveau".
        add(new AjaxLink<Void>("new") {
            @Override
            public void onClick(AjaxRequestTarget ajaxRequestTarget) {
                // Désactivation du message d'avertissement du navigateur à la fermeture.
                ajaxRequestTarget.appendJavaScript("Wicket.Window.unloadConfirmation = false;");
                modal.setContent(new NewContentPropertyPanel(modal.getContentId(), Model.of(new ContentProperty())) {
                    @Override
                    protected void onOut() {
                        setResponsePage(this.findPage());
                    }
                });
                modal.show(ajaxRequestTarget);
            }
        }.setVisible(canAdd));

    }

}
