package fr.solunea.thaleia.webapp.pages.admin.locales;

import fr.solunea.thaleia.model.Locale;
import fr.solunea.thaleia.model.dao.LocaleDao;
import fr.solunea.thaleia.utils.DetailedException;
import fr.solunea.thaleia.webapp.panels.confirmation.ConfirmationLink;
import fr.solunea.thaleia.webapp.security.ThaleiaSession;
import org.apache.cayenne.ObjectContext;
import org.apache.log4j.Logger;
import org.apache.wicket.Page;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.authorization.Action;
import org.apache.wicket.authroles.authorization.strategies.role.annotations.AuthorizeAction;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.PropertyListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.StringResourceModel;

import java.util.List;

@SuppressWarnings("serial")
@AuthorizeAction(action = Action.RENDER, roles = {"admin"})
public class LocalesPanel extends Panel {

    private static final Logger logger = Logger.getLogger(LocalesPanel.class);

    public LocalesPanel(String id) {
        super(id);

        setOutputMarkupId(true);

        LocaleDao localeDao = new LocaleDao(ThaleiaSession.get().getContextService().getNewContext());

        add(new PropertyListView<>("objects", new AbstractReadOnlyModel<List<Locale>>() {
            @Override
            public List<Locale> getObject() {
                return localeDao.find();
            }
        }) {

            @Override
            protected void populateItem(final ListItem<Locale> item) {

                item.add(new Label("name"));

                item.add(new ConfirmationLink<Void>("delete", new StringResourceModel("delete.confirm", this, null,
                        new Object[]{localeDao.getDisplayName(item.getModelObject(),
                                ThaleiaSession.get().getLocale())})) {

                    @Override
                    public boolean isVisible() {
                        try {
                            return localeDao.canBeDeleted(item.getModelObject());
                        } catch (Exception e) {
                            logger.warn("Erreur durant l'analyse d'un objet : " + e);
                            return false;
                        }
                    }

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        try {
                            Locale locale = item.getModelObject();
                            // Suppression de l'objet
                            localeDao.delete(locale, true);

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
        add(new Link<Page>("new") {
            @Override
            public boolean isVisible() {
                // TODO vérifier le droit de l'utilisateur à créer des locales
                return true;
            }

            @Override
            public void onClick() {
                setResponsePage(NewLocalePage.class);
            }
        });
    }

}
