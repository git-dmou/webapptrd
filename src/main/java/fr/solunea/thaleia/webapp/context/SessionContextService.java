package fr.solunea.thaleia.webapp.context;

import fr.solunea.thaleia.model.dao.CayenneDao;
import fr.solunea.thaleia.model.dao.ICayenneContextService;
import fr.solunea.thaleia.webapp.ThaleiaApplication;
import org.apache.cayenne.BaseContext;
import org.apache.cayenne.BaseDataObject;
import org.apache.cayenne.ObjectContext;
import org.apache.log4j.Logger;

import java.io.Serializable;
import java.lang.reflect.Constructor;

/**
 * Permet de donner accès à un contexte Cayenne associé à la session.
 */
@SuppressWarnings("serial")
public class SessionContextService extends AbstractContextService implements  Serializable {

    @SuppressWarnings("unused")
    private static final Logger logger = Logger.getLogger(SessionContextService.class);

    private ObjectContext context;

    @Override
    public ObjectContext getContextSingleton() {
        // C'est le filtre  <filter-class>org.apache.cayenne.configuration.web.CayenneFilter</filter-class>
        // qui associe le contexte à la Session.
        if (this.context == null) {
            ObjectContext context = BaseContext.getThreadObjectContext();
            if (context == null) {
                logger.error("Contexte Cayenne pour l'application non initialisé !");
            } else {
                this.context = context;
            }
        }
        return this.context;
    }

    @Override
    public ObjectContext getNewContext() {
        // On fabrique un nouveau contexte depuis le runtime déclaré dans l'application.
        return ThaleiaApplication.get().contextService.getNewContext();
    }

    @Override
    public void close() {
        // On suppose que c'est le filtre qui gère la clôture.
    }

    @Override
    public ObjectContext getChildContext(ObjectContext parentContext) {
        // On fabrique un contexte fils depuis le runtime déclaré dans l'application.
        return ThaleiaApplication.get().contextService.getChildContext(parentContext);
    }

}
