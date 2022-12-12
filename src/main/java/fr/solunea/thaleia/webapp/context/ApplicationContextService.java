package fr.solunea.thaleia.webapp.context;

import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.configuration.server.ServerRuntime;
import org.apache.log4j.Logger;

import java.io.Serializable;

/**
 * Permet de donner accès à un contexte Cayenne de type singleton, associé à l'application.
 */
public class ApplicationContextService extends AbstractContextService implements Serializable {

    private static final Logger logger = Logger.getLogger(ApplicationContextService.class);
    private final String configurationLocation;
    // Transient car org.apache.cayenne.configuration.server.ServerRuntime n'est pas sérializable.
    // On en fait donc un singleton transient, pour n'avoire à sérialiser que la String configurationLocation.
    private transient ServerRuntime cayenneRuntime;
    private ObjectContext context;

    public ApplicationContextService(String configurationLocation) {
        logger.debug("Initialisation du contexte Cayenne décrit dans la ressource " + configurationLocation);
        this.configurationLocation = configurationLocation;
    }

    public ServerRuntime getCayenneRuntime() {
        if (cayenneRuntime == null) {
            cayenneRuntime = ServerRuntime.builder().addConfig(configurationLocation).build();
        }
        return cayenneRuntime;
    }

    @Override
    public ObjectContext getContextSingleton() {
        synchronized (this) {
            if (this.context == null) {
                ObjectContext context = getCayenneRuntime().newContext();
                if (context == null) {
                    logger.error("Contexte Cayenne pour l'application non initialisé !");
                } else {
                    this.context = context;
                }
            }
            return this.context;
        }
    }

    /**
     * @return un nouveau contexte Cayenne, vierge.
     */
    @Override
    public ObjectContext getNewContext() {
        return getCayenneRuntime().newContext();
    }

    @Override
    public void close() {
        getCayenneRuntime().shutdown();
    }

    @Override
    public ObjectContext getChildContext(ObjectContext parentContext) {
        return getCayenneRuntime().newContext(parentContext);
    }

}
