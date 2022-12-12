package fr.solunea.thaleia.webapp.pages.admin;

import org.apache.wicket.markup.html.basic.MultiLineLabel;
import org.apache.wicket.markup.html.panel.Panel;

public class MailjetApiResultPanel extends Panel {

    private String result;

    public MailjetApiResultPanel(String id) {
        super(id);
        add(new MultiLineLabel("response", result));
    }

    public MailjetApiResultPanel(String id, String result) {
        super(id);
        add(new MultiLineLabel("response", result));
    }

    public void setResult(String result) {
        this.result = result;
    }
}
