package fr.solunea.thaleia.webapp.pages.admin.contenttype;

import fr.solunea.thaleia.model.ContentType;
import fr.solunea.thaleia.model.dao.ContentTypeDao;
import fr.solunea.thaleia.utils.DetailedException;
import fr.solunea.thaleia.webapp.ThaleiaApplication;
import fr.solunea.thaleia.webapp.panels.confirmation.ConfirmationLink;
import fr.solunea.thaleia.webapp.security.ThaleiaSession;
import org.apache.log4j.Logger;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.PropertyListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;

import java.util.List;

@SuppressWarnings("serial")
public class ContentTypesPanel extends Panel {

    private static final Logger logger = Logger.getLogger(ContentTypesPanel.class);

    public ContentTypesPanel(String id) {
        super(id);

        ContentTypeDao contentTypeDao = new ContentTypeDao(ThaleiaSession.get().getContextService().getContextSingleton());

        setOutputMarkupId(true);

        // La popup modale de création / gestion des propriétés
        final ModalWindow modal = new ModalWindow("modal");
        ThaleiaApplication.get().configureModalPopup(modal);
        add(modal);

        add(new PropertyListView<>("objects", new AbstractReadOnlyModel<List<ContentType>>() {
            @Override
            public List<ContentType> getObject() {
                // TODO ne présenter que les ContentType sur lesquels cet utilisateur a la visiblité
                return contentTypeDao.find();
            }
        }) {

            @Override
            protected void populateItem(final ListItem<ContentType> item) {

                item.add(new Label("name"));

                item.add(new CheckBox("isModuleType").setEnabled(false));

                item.add(new AjaxLink<Void>("properties") {
                    @Override
                    public void onClick(AjaxRequestTarget ajaxRequestTarget) {
                        // Désactivation du message d'avertissement du
                        // navigateur à lafermeture.
                        ajaxRequestTarget.appendJavaScript("Wicket.Window.unloadConfirmation = false;");
                        modal.setContent(new ContentTypePropertiesPanel(modal.getContentId(), item.getModel()) {

                            @Override
                            protected void onOut() {
                                setResponsePage(this.findPage());
                            }

                        });
                        modal.show(ajaxRequestTarget);
                    }
                });

                item.add(new ConfirmationLink<Void>("delete", new StringResourceModel("delete.confirm", this, null,
                        new Object[]{contentTypeDao.getDisplayName(item.getModelObject(),
                                ThaleiaSession.get().getLocale())})) {

                    @Override
                    public boolean isVisible() {
                        // Ce code permet de ne présenter le bouton de
                        // suppression que si l'objet à supprimer n'est lié à
                        // aucun autre objet. Or, on désire pouvoir supprimer un
                        // type de contenu, et tous ses contenus associés.

                        // try {
                        // if (ThaleiaSession.get().getContentTypeDao()
                        // .canBeDeleted(item.getModelObject())) {
                        // return true;
                        // } else {
                        // return false;
                        // }
                        // } catch (Exception e) {
                        // logger.warn("Erreur durant l'analyse d'un objet : "
                        // + e);
                        // return false;
                        // }

                        return true;
                    }

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        try {
                            // Suppression de l'objet
                            ThaleiaSession.get().getContentTypeService().deleteContentType(item.getModelObject());
                            item.getModelObject().getObjectContext().commitChanges();

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
                // Désactivation du message d'avertissement du navigateur à la
                // fermeture.
                ajaxRequestTarget.appendJavaScript("Wicket.Window.unloadConfirmation = false;");
                modal.setContent(new ContentTypeEditPanel(modal.getContentId(), Model.of(new ContentType())) {
                    @Override
                    protected void onOut() {
                        setResponsePage(this.findPage());
                    }
                });
                modal.show(ajaxRequestTarget);
            }
        });

    }

}
