package fr.solunea.thaleia.webapp.pages;

import org.apache.log4j.Logger;
import org.apache.wicket.Session;
import org.apache.wicket.devutils.debugbar.DebugBar;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.EmptyPanel;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import fr.solunea.thaleia.utils.LogUtils;
import fr.solunea.thaleia.webapp.panels.ThaleiaFeedbackPanel;

@SuppressWarnings("serial")
public class ErrorPage extends ThaleiaPage {

	public static final String MESSAGE_PARAM_ID = "message";
	private final static Logger logger = Logger.getLogger(ErrorPage.class);

	public ErrorPage(PageParameters params) {
		super();

		// On récupère le message d'erreur en paramètre, et on le place en
		// session, pour que le traitement de présentation soit le même :
		// initPanels()
		String message = params.get(MESSAGE_PARAM_ID).toString("");
		Session.get().error(message);

		initPanels();
	}

	public ErrorPage() {
		super();
		initPanels();
	}

	private void initPanels() {
		// logger.debug("Messages feedback : "
		// + Session.get().getFeedbackMessages());
		// logger.debug(LogUtils.getStackTrace());

		try {
			ThaleiaFeedbackPanel feedback = new ThaleiaFeedbackPanel("feedback") {
				@Override
				public boolean isVisible() {
					return anyMessage();
				}
			};
			add(feedback);

			addOrReplace(new Label("title", getString("title")));

			// Panel de debug Wicket
			if (getApplication().getDebugSettings().isDevelopmentUtilitiesEnabled()) {
				add(new DebugBar("debug"));
			} else {
				add(new EmptyPanel("debug").setVisible(false));
			}

		} catch (Exception e) {
			logger.warn("Une erreur a eu lieu durant la préparation de la page d'erreur (quelle ironie) : "
					+ e.toString() + "\n" + LogUtils.getStackTrace(e.getStackTrace()));
		}

	}

	@Override
	public boolean isErrorPage() {
		return true;
	}

}
