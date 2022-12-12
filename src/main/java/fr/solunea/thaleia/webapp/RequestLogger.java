package fr.solunea.thaleia.webapp;

import org.apache.log4j.Logger;
import org.apache.wicket.request.IRequestHandler;
import org.apache.wicket.request.cycle.AbstractRequestCycleListener;
import org.apache.wicket.request.cycle.RequestCycle;

import fr.solunea.thaleia.utils.FormatUtils;
import fr.solunea.thaleia.webapp.security.ThaleiaSession;

/**
 * Cet écouteur permet d'intercepter et de tracer des exceptions levée lors du
 * cycle de vie de la requête Wicket, et qui ne seraient pas tracées dans le
 * code des pages. Par exemple, un appel javascript qui ne serait pas traité par
 * l'écouteur de la page... Fonctionne s'il est installé dans l'application avec
 * 
 * <pre>
 * <code>getRequestCycleListeners().add(new RequestLogger());</code>
 * </pre>
 *
 */
public class RequestLogger extends AbstractRequestCycleListener {

	private final static Logger logger = Logger.getLogger(RequestLogger.class);

	@Override
	public IRequestHandler onException(RequestCycle cycle, Exception ex) {
		String url = "";
		try {
			url = cycle.getRequest().getUrl().toString();
		} catch (Exception e) {
			// rien
		}
		logger.debug(
				"Exception lors du traitement du cycle de vie de la requête Wicket : " + ex + "\n" + "URL = " + url);
		logger.debug("Session : " + FormatUtils.humanReadableByteCount(ThaleiaSession.get().getSizeInBytes(), true));
		// + "\n"
		// + LogUtils.getStackTrace(ex.getStackTrace()));
		return null;
	}

	// @Override
	// public void onBeginRequest(RequestCycle cycle) {
	// logger.debug("Début de traitement de requête.");
	// cycle.getListeners().add(new IRequestCycleListener() {
	//
	// @Override
	// public void onUrlMapped(RequestCycle cycle,
	// IRequestHandler handler, Url url) {
	// logger.debug("onUrlMapped");
	// }
	//
	// @Override
	// public void onRequestHandlerScheduled(RequestCycle cycle,
	// IRequestHandler handler) {
	// logger.debug("onRequestHandlerScheduled");
	// }
	//
	// @Override
	// public void onRequestHandlerResolved(RequestCycle cycle,
	// IRequestHandler handler) {
	// logger.debug("onRequestHandlerResolved");
	// }
	//
	// @Override
	// public void onRequestHandlerExecuted(RequestCycle cycle,
	// IRequestHandler handler) {
	// logger.debug("onRequestHandlerExecuted");
	// }
	//
	// @Override
	// public void onExceptionRequestHandlerResolved(RequestCycle cycle,
	// IRequestHandler handler, Exception exception) {
	// logger.debug("onExceptionRequestHandlerResolved");
	//
	// }
	//
	// @Override
	// public IRequestHandler onException(RequestCycle cycle, Exception ex)
	// {
	// logger.debug("onException");
	// return null;
	// }
	//
	// @Override
	// public void onEndRequest(RequestCycle cycle) {
	// logger.debug("onEndRequest");
	// }
	//
	// @Override
	// public void onDetach(RequestCycle cycle) {
	// logger.debug("onDetach");
	// }
	//
	// @Override
	// public void onBeginRequest(RequestCycle cycle) {
	// logger.debug("onBeginRequest");
	// }
	// });
	// }

}
