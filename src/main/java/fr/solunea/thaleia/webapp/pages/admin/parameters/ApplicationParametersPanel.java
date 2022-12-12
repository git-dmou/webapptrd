package fr.solunea.thaleia.webapp.pages.admin.parameters;

import fr.solunea.thaleia.model.ApplicationParameter;
import fr.solunea.thaleia.model.dao.ApplicationParameterDao;
import fr.solunea.thaleia.webapp.pages.admin.Modale;
import fr.solunea.thaleia.webapp.panels.confirmation.ConfirmationLink;
import fr.solunea.thaleia.webapp.security.ThaleiaSession;
import fr.solunea.thaleia.webapp.utils.MessagesUtils;
import org.apache.log4j.Logger;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.PropertyListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;

import java.util.List;

public class ApplicationParametersPanel extends Panel {

    private static final Logger logger = Logger.getLogger(ApplicationParametersPanel.class);

    /**
     * @param canEdit   peut-on modifier une valeur ?
     * @param canDelete peut-on supprimer un paramètre d'application ?
     * @param canAdd    peut-on ajouter un paramètre d'application ?
     */
    public ApplicationParametersPanel(String id, IModel<List<ApplicationParameter>> model, final boolean canEdit,
                                      final boolean canDelete, final boolean canAdd) {
        super(id);

        setOutputMarkupId(true);

        // La popup modale d'édition
        final Modale modal = new Modale("modale");
        add(modal);

        ApplicationParameterDao applicationParameterDao = new ApplicationParameterDao(ThaleiaSession.get().getContextService().getContextSingleton());

        add(new PropertyListView<>("objects", model) {

            @Override
            protected void populateItem(final ListItem<ApplicationParameter> item) {

                item.add(new Label("name"));
                item.add(new Label("value"));

                // Le lien "Modifier".
                item.add(new AjaxLink<Void>("edit") {
                    @Override
                    public void onClick(AjaxRequestTarget ajaxRequestTarget) {
                        // On fabrique cet objet dans un nouveau contexte
                        modal.setTitle(MessagesUtils.getLocalizedMessage("modal.edit", ApplicationParametersPanel.class, (Object) null));
                        modal.setContent(new ApplicationParameterEditPanel(modal.getContentId(),
                                Model.of(ThaleiaSession.get().getContextService().getObjectInNewContext(item.getModel().getObject()))) {
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
                        new Object[]{applicationParameterDao.getDisplayName(item.getModelObject(), ThaleiaSession.get().getLocale())})) {

                    @Override
                    public boolean isVisible() {
                        try {
                            // De toute façon, non si le constructeur ne le permet pas
                            if (!canDelete) {
                                return false;
                            }

                            // Après on vérifie que la suppression est effectivement possible.
                            return applicationParameterDao.canBeDeleted(item.getModelObject());
                        } catch (Exception e) {
                            logger.warn("Erreur durant l'analyse d'un objet : " + e);
                            return false;
                        }
                    }

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        try {
                            ApplicationParameter applicationParameter = item.getModelObject();
                            ThaleiaSession.get().getContextService().safeDelete(applicationParameter);
                        } catch (Exception e) {
                            StringResourceModel errorMessageModel = new StringResourceModel("delete.error", item.getModel());
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
                modal.setTitle(MessagesUtils.getLocalizedMessage("modal.new", ApplicationParametersPanel.class, (Object) null));
                modal.setContent(new ApplicationParameterEditPanel(modal.getContentId(),
                        Model.of(ThaleiaSession.get().getContextService().getNewInNewContext(ApplicationParameter.class))) {
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
