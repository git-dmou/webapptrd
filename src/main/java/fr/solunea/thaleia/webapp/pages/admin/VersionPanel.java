package fr.solunea.thaleia.webapp.pages.admin;

import fr.solunea.thaleia.webapp.ThaleiaApplication;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;

/**
 * Présente la version de l'application.
 */
@SuppressWarnings("serial")
public class VersionPanel extends Panel {

    public VersionPanel(String id) {
        super(id);

        // Recherche le paramètre "application.version.name" dans web.xml
        add(new Label("version.value", ThaleiaApplication.get().getServletContext().getInitParameter("application" +
                ".version.name")));

        // Recherche le paramètre "application.revision.number" dans web.xml
        add(new Label("revision.value", ThaleiaApplication.get().getServletContext().getInitParameter("application" +
                ".revision.number")));
    }

}
