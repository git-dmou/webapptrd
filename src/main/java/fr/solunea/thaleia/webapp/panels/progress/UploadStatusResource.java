package fr.solunea.thaleia.webapp.panels.progress;

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import org.apache.wicket.Application;
import org.apache.wicket.Component;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.protocol.http.servlet.MultipartServletWebRequestImpl;
import org.apache.wicket.protocol.http.servlet.UploadInfo;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.request.resource.AbstractResource;
import org.apache.wicket.util.time.Duration;

/**
 * A resource that prints out basic statistics about the current upload. This
 * resource is used to feed the progress bar information by the progress bar
 * javascript which requests this resource through ajax.
 * 
 * For customizing status text see {@link #RESOURCE_STATUS}.
 * 
 * @author Andrew Lombardi
 * @author Igor Vaynberg (ivaynberg)
 */
class UploadStatusResource extends AbstractResource {

	protected static final Logger logger = Logger.getLogger(UploadStatusResource.class);

	private static final long serialVersionUID = 1L;

	private static final String UPLOAD_PARAMETER = "upload";

	/**
	 * Resource key used to retrieve status message for.
	 * 
	 * Example: UploadStatusResource.status=${percentageComplete}% finished,
	 * ${bytesUploadedString} of ${totalBytesString} at ${transferRateString};
	 * ${remainingTimeString}
	 */
	public static final String RESOURCE_STATUS = "UploadStatusResource.status";

	@Override
	public void respond(org.apache.wicket.request.resource.IResource.Attributes attributes) {
		super.respond(attributes);
	};

	@Override
	protected ResourceResponse newResourceResponse(final Attributes attributes) {
		// Determine encoding
		final String encoding = Application.get().getRequestCycleSettings().getResponseRequestEncoding();

		ResourceResponse response = new ResourceResponse();
		response.setContentType("text/html; charset=" + encoding);
		response.setCacheDuration(Duration.NONE);

		final String status = getStatus(attributes);
		response.setWriteCallback(new WriteCallback() {
			@Override
			public void writeData(final Attributes attributes) {
				attributes.getResponse().write("<html><body>|");
				attributes.getResponse().write(status);
				attributes.getResponse().write("|</body></html>");
			}
		});

		return response;
	}

	/**
	 * @param attributes
	 * @return status string with progress data that will feed the
	 *         progressbar.js variables on browser to update the progress bar
	 */
	private String getStatus(final Attributes attributes) {
		final String upload = attributes.getParameters().get(UPLOAD_PARAMETER).toString();

		final HttpServletRequest req = (HttpServletRequest) attributes.getRequest().getContainerRequest();

		UploadInfo info = MultipartServletWebRequestImpl.getUploadInfo(req, upload);

		String status = null;
		if ((info == null) || (info.getTotalBytes() < 1)) {
			status = "100|";
		} else {
			status = info.getPercentageComplete() + "|"
					+ new StringResourceModel(RESOURCE_STATUS, (Component) null, Model.of(info)).getString();
		}

		return status;
	}

	/**
	 * Create a new parameter for the given identifier of a {@link UploadInfo}.
	 * 
	 * @param upload
	 *            identifier
	 * @return page parameter suitable for URLs to this resource
	 */
	public static PageParameters newParameter(String upload) {
		return new PageParameters().add(UPLOAD_PARAMETER, upload);
	}
}