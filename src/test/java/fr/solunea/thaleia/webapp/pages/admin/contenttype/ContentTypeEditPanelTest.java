package fr.solunea.thaleia.webapp.pages.admin.contenttype;

import fr.solunea.thaleia.model.ContentType;
import fr.solunea.thaleia.model.dao.ContentTypeDao;
import fr.solunea.thaleia.webapp.ThaleiaApplicationTester;
import fr.solunea.thaleia.webapp.security.ThaleiaSession;
import org.apache.log4j.Logger;
import org.apache.wicket.model.Model;
import org.junit.jupiter.api.Test;

public class ContentTypeEditPanelTest extends ThaleiaApplicationTester {
    private static final Logger logger = Logger.getLogger(ContentTypeEditPanelTest.class);

    @Test
    public void rendersSuccessfully() {
        ThaleiaSession.get().authenticateWithNoStats("admin");
        ContentType contentType = new ContentTypeDao(ThaleiaSession.get().getContextService().getContextSingleton()).getDefaultContentType();
        logger.info("Test de rendu du panneau pour le contentType : " + contentType);
        tester.startComponentInPage(new ContentTypeEditPanel("compId", Model.of(contentType)) {
            @Override
            protected void onOut() {
                // rien.
            }
        });
    }
}
