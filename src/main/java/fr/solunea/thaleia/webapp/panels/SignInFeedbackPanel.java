package fr.solunea.thaleia.webapp.panels;

import org.apache.wicket.feedback.FeedbackMessage;
import org.apache.wicket.markup.html.panel.FeedbackPanel;

public class SignInFeedbackPanel extends FeedbackPanel {

    public SignInFeedbackPanel(String id) {
        super(id);
    }

    @Override
    protected String getCSSClass(FeedbackMessage message) {
        String css;

        switch (message.getLevel()) {
            case FeedbackMessage.SUCCESS:
                css = "success";
                break;
            case FeedbackMessage.INFO:
                css = "info";
                break;
            case FeedbackMessage.WARNING:
                css = "warning";
                break;
            case FeedbackMessage.ERROR:
                css = "error";
                break;
            default:
                css = "";
        }

        return css;
    }
}
