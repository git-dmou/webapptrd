package fr.solunea.thaleia.webapp;

import fr.solunea.thaleia.model.User;
import fr.solunea.thaleia.utils.IEvent;
import fr.solunea.thaleia.webapp.security.ThaleiaSession;
import org.apache.log4j.Logger;

import java.io.Serializable;

/**
 * Donne accès aux fonctions de traçage de la navigation dans le site.
 */
public abstract class Analytics {

    private static final Logger logger = Logger.getLogger(Analytics.class);

    static class NoAnalytics extends Analytics {

        @Override
        public void logAuthentication(User user) {
            // pas d'opération
        }

        @Override
        protected void logEvent(Serializable jsessionid, IEvent event) {
            // pas d'opération
        }
    }

    public static Analytics getImplementation() {
        if (isActivated()) {
            return new MixPanelAnalytics();
        } else {
            // On renvoie une implémentation inactive
            return new NoAnalytics();
        }
    }

    private static boolean isActivated() {
        return ThaleiaApplication.get().getApplicationParameterDao().getValue(
                "fr.solunea.thaleia.webapp.Analytics" + ".activated", "false").toLowerCase().trim().equals("true");
    }

    String getUserId() {
        try {
            return ThaleiaSession.get().getAnonymousId();
        } catch (Exception e) {
            return "";
        }
    }

    public abstract void logAuthentication(User user);

    protected abstract void logEvent(Serializable jsessionid, IEvent event);

    public void logEvent(IEvent event) {
        logEvent(getUserId(), event);
    }
}
