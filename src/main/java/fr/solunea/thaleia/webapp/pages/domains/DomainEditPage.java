package fr.solunea.thaleia.webapp.pages.domains;

import fr.solunea.thaleia.model.Domain;
import fr.solunea.thaleia.model.dao.DomainDao;
import fr.solunea.thaleia.utils.DetailedException;
import fr.solunea.thaleia.webapp.pages.ErrorPage;
import fr.solunea.thaleia.webapp.pages.ThaleiaV6MenuPage;
import fr.solunea.thaleia.webapp.pages.admin.AdminPage;
import fr.solunea.thaleia.webapp.security.ThaleiaSession;
import org.apache.cayenne.ObjectContext;
import org.apache.wicket.RestartResponseException;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.authroles.authorization.strategies.role.annotations.AuthorizeInstantiation;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

@AuthorizeInstantiation("admin")
public class DomainEditPage extends ThaleiaV6MenuPage {

    public DomainEditPage(IModel<Domain> model) {
        super(model);
        initPage(model);
    }

    @SuppressWarnings("unused")
    public DomainEditPage() {
        super();
        initPage(null);
    }

    private void initPage(IModel<Domain> model) {
        if (model == null) {
            ObjectContext context = ThaleiaSession.get().getContextService().getNewContext();
            DomainDao domainDao = new DomainDao(context);
            Domain domain = domainDao.get();
            try {
                domainDao.save(domain, false);
            } catch (DetailedException e) {
                throw new RestartResponseException(ErrorPage.class);
            }
            model = Model.of(domain);
        }

        DomainEditPanel editPanel = new DomainEditPanel("editPanel", model) {
            @Override
            protected void onOut() {
                setResponsePage(AdminPage.class);
            }

            @Override
            protected void onPropertyChanged(AjaxRequestTarget target) {
                // Rien. On pourrait utiliser cette méthode pour ne rendre actif
                // le bouton Save que si une des propriétés a été changée.
            }
        };
        editPanel.setOutputMarkupId(true);
        add(editPanel);
    }
}
