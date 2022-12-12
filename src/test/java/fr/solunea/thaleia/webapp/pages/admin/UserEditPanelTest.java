package fr.solunea.thaleia.webapp.pages.admin;

import fr.solunea.thaleia.model.User;
import fr.solunea.thaleia.model.dao.UserDao;
import fr.solunea.thaleia.webapp.ThaleiaApplicationTester;
import fr.solunea.thaleia.webapp.security.ThaleiaSession;
import org.apache.log4j.Logger;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.junit.jupiter.api.Test;

public class UserEditPanelTest extends ThaleiaApplicationTester {

    private static final Logger logger = Logger.getLogger(UserEditPanelTest.class);

    @Test
    public void rendersSuccessfully() {
        ThaleiaSession.get().authenticateWithNoStats("admin");
        User admin = new UserDao(ThaleiaSession.get().getContextService().getContextSingleton()).findUserByLogin("admin");
        logger.debug("Chargement du panneau d'édition de l'utilisateur " + admin.getLogin());
        tester.startComponentInPage(new MyUserEditPanel("compId", Model.of(admin)));
    }

    static class MyUserEditPanel extends UserEditPanel {
        MyUserEditPanel(String id, IModel<User> model) {
            super(id, model);
        }

        @Override
        protected void onOut(AjaxRequestTarget target) {
            logger.info("OnOut appelée.");
        }
    }

}
