package fr.solunea.thaleia.webapp.pages.admin.locales;

import fr.solunea.thaleia.model.Locale;
import fr.solunea.thaleia.model.dao.LocaleDao;
import fr.solunea.thaleia.utils.DetailedException;
import fr.solunea.thaleia.webapp.pages.BasePage;
import fr.solunea.thaleia.webapp.pages.ErrorPage;
import fr.solunea.thaleia.webapp.pages.admin.AdminPage;
import fr.solunea.thaleia.webapp.security.ThaleiaSession;
import org.apache.cayenne.ObjectContext;
import org.apache.wicket.RestartResponseException;
import org.apache.wicket.authroles.authorization.strategies.role.annotations.AuthorizeInstantiation;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

@AuthorizeInstantiation("admin")
public class NewLocalePage extends BasePage {

    public NewLocalePage() {
        super();

        ObjectContext context = ThaleiaSession.get().getContextService().getNewContext();
        LocaleDao localeDao = new LocaleDao(context);
        Locale locale = localeDao.get();
        try {
            localeDao.save(locale, false);
        } catch (DetailedException e) {
            throw new RestartResponseException(ErrorPage.class);
        }

        // On en fait le modèle de cette page
        setDefaultModel(Model.of(locale));

        initPage();
    }

    private void initPage() {
        @SuppressWarnings("unchecked")
        LocaleEditPanel editPanel = new LocaleEditPanel("editPanel", (IModel<Locale>) getDefaultModel()) {
            @Override
            protected void onOut() {
                setResponsePage(AdminPage.class);
            }
        };
        editPanel.setOutputMarkupId(true);
        add(editPanel);
    }

}
