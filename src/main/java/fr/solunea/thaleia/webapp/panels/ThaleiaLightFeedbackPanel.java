package fr.solunea.thaleia.webapp.panels;

import org.apache.wicket.feedback.IFeedbackMessageFilter;

@SuppressWarnings("serial")
public class ThaleiaLightFeedbackPanel extends ThaleiaFeedbackPanel {

	public ThaleiaLightFeedbackPanel(String id) {
		super(id);
	}

	public ThaleiaLightFeedbackPanel(String id, IFeedbackMessageFilter filter) {
		super(id, filter);
	}

}
