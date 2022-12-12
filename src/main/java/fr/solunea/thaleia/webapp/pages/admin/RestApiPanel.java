package fr.solunea.thaleia.webapp.pages.admin;

import fr.solunea.thaleia.utils.DateUtils;
import fr.solunea.thaleia.webapp.ThaleiaApplication;
import fr.solunea.thaleia.webapp.pages.admin.usage.HistoryOfEventsPanel;
import fr.solunea.thaleia.webapp.pages.admin.usage.HistoryOfEventsPanel.Filter;
import fr.solunea.thaleia.webapp.pages.admin.usage.UsersUsagePanel;
import fr.solunea.thaleia.webapp.security.ThaleiaSession;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;

import java.util.Calendar;

@SuppressWarnings("serial")
class RestApiPanel extends Panel {

    RestApiPanel(String id) {
        super(id);

        add(new Label("rootUrl", ThaleiaApplication.get().getApplicationRootUrl()));
    }

}
