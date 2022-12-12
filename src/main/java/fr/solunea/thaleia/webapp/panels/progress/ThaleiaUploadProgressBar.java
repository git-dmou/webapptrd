package fr.solunea.thaleia.webapp.panels.progress;

import java.util.Formatter;

import org.apache.log4j.Logger;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.upload.FileUploadField;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.request.resource.CssResourceReference;
import org.apache.wicket.request.resource.JavaScriptResourceReference;
import org.apache.wicket.request.resource.ResourceReference;
import org.apache.wicket.request.resource.SharedResourceReference;
import org.apache.wicket.resource.CoreLibrariesContributor;
import org.apache.wicket.util.lang.Args;
import org.apache.wicket.util.visit.IVisit;
import org.apache.wicket.util.visit.IVisitor;

import fr.solunea.thaleia.webapp.ThaleiaApplication;

public class ThaleiaUploadProgressBar extends Panel {

	protected static final Logger logger = Logger.getLogger(ThaleiaUploadProgressBar.class);

	/**
	 * Resource key used to retrieve starting message for.
	 * 
	 * Example: UploadProgressBar.starting=Upload starting...
	 */
	public static final String RESOURCE_STARTING = "UploadProgressBar.starting";

	public static final String RESOURCE_ENDED = "UploadProgressBar.ended";

	private static final ResourceReference JS = new JavaScriptResourceReference(ThaleiaUploadProgressBar.class,
			"progressbar.js");

	private static final ResourceReference CSS = new CssResourceReference(ThaleiaUploadProgressBar.class,
			"ThaleiaUploadProgressBar.css");

	private static final String RESOURCE_NAME = UploadStatusResource.class.getName();

	private static final long serialVersionUID = 1L;

	private final Form<?> form;

	private MarkupContainer statusDiv;

	private MarkupContainer barDiv;

	private final FileUploadField uploadField;

	private boolean hideAfterUpload;

	/**
	 * Constructor that will display the upload progress bar for submissions of
	 * the given form, that include a file upload in the given file upload
	 * field; i.e. if the user did not select a file in the given file upload
	 * field, the progess bar is not displayed.
	 * 
	 * @param id
	 *            component id (not null)
	 * @param form
	 *            form that is submitted (not null)
	 * @param uploadField
	 *            the file upload field to check for a file upload, or null to
	 *            display the upload field for every submit of the given form
	 * @param hideAfterUpload
	 *            si true, alors masque les barres lorsque 100% de chargement
	 *            est atteint. Sinon, présente le message de fin de chargement
	 *            dans la barre de progression (qui reste affichée).
	 */
	public ThaleiaUploadProgressBar(final String id, final Form<?> form, final FileUploadField uploadField,
			boolean hideAfterUpload) {
		super(id);

		this.uploadField = uploadField;
		if (uploadField != null) {
			uploadField.setOutputMarkupId(true);
		}

		this.form = Args.notNull(form, "form");
		form.setOutputMarkupId(true);

		setRenderBodyOnly(true);
		
		this.hideAfterUpload = hideAfterUpload;
	}

	@Override
	protected void onInitialize() {
		super.onInitialize();
		getCallbackForm().setOutputMarkupId(true);

		// Initialisation de la ressource qui va s'occuper de transmettre l'état
		// du transfert du fichier.
		ThaleiaApplication.get().getSharedResources().add(RESOURCE_NAME, new UploadStatusResource());

		barDiv = newBarComponent("bar");
		add(barDiv);

		statusDiv = newStatusComponent("status");
		add(statusDiv);
	}

	/**
	 * Creates a component for the status text
	 *
	 * @param id
	 *            The component id
	 * @return the status component
	 */
	protected MarkupContainer newStatusComponent(String id) {
		WebMarkupContainer status = new WebMarkupContainer(id);
		status.setOutputMarkupId(true);
		return status;
	}

	/**
	 * Creates a component for the bar
	 *
	 * @param id
	 *            The component id
	 * @return the bar component
	 */
	protected MarkupContainer newBarComponent(String id) {
		WebMarkupContainer bar = new WebMarkupContainer(id);
		bar.setOutputMarkupId(true);
		return bar;
	}

	/**
	 * Override this to provide your own CSS, or return null to avoid including
	 * the default.
	 * 
	 * @return ResourceReference for your CSS.
	 */
	protected ResourceReference getCss() {
		return CSS;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void renderHead(final IHeaderResponse response) {
		super.renderHead(response);

		CoreLibrariesContributor.contributeAjax(getApplication(), response);
		response.render(JavaScriptHeaderItem.forReference(JS));
		ResourceReference css = getCss();
		if (css != null) {
			response.render(CssHeaderItem.forReference(css));
		}

		ResourceReference ref = new SharedResourceReference(RESOURCE_NAME);

		final String uploadFieldId = (uploadField == null) ? "" : uploadField.getMarkupId();

		final String startStatus = new StringResourceModel(RESOURCE_STARTING, this, (IModel<?>) null).getString();
		final String endStatus = new StringResourceModel(RESOURCE_ENDED, this, (IModel<?>) null).getString();

		CharSequence url = urlFor(ref, UploadStatusResource.newParameter(getPage().getId()));

		StringBuilder builder = new StringBuilder(128);
		@SuppressWarnings("resource")
		Formatter formatter = new Formatter(builder);

		formatter.format("new Wicket.WUPB('%s', '%s', '%s', '%s', '%s', '%s', '%s', '%s');", getCallbackForm().getMarkupId(),
				statusDiv.getMarkupId(), barDiv.getMarkupId(), url, uploadFieldId, startStatus, endStatus, hideAfterUpload);
		response.render(OnDomReadyHeaderItem.forScript(builder.toString()));
	}

	/**
	 * Form on where will be installed the JavaScript callback to present the
	 * progress bar. {@link ModalWindow} is designed to hold nested forms and
	 * the progress bar callback JavaScript needs to be add at the form inside
	 * the {@link ModalWindow} if one is used.
	 * 
	 * @return form
	 */
	private Form<?> getCallbackForm() {
		Boolean insideModal = form.visitParents(ModalWindow.class, new IVisitor<ModalWindow, Boolean>() {
			@Override
			public void component(final ModalWindow object, final IVisit<Boolean> visit) {
				visit.stop(true);
			}
		});
		if ((insideModal != null) && insideModal) {
			return form;
		} else {
			return form.getRootForm();
		}
	}
}
