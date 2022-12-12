package fr.solunea.thaleia.webapp.panels;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.apache.wicket.Session;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormSubmitBehavior;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.upload.FileUpload;
import org.apache.wicket.markup.html.form.upload.FileUploadField;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.util.lang.Bytes;

import fr.solunea.thaleia.service.utils.Configuration;
import fr.solunea.thaleia.webapp.ThaleiaApplication;
import fr.solunea.thaleia.webapp.panels.progress.ThaleiaUploadProgressBar;

/**
 * Réalise l'upload, et enregistre le binaire dans le fichier passé en modèle.
 * Remplace le nom de ce fichier par celui transmis lors de l'upload.
 * 
 */
@SuppressWarnings("serial")
public abstract class UploadFormPanelArchive extends Panel {

	protected static final Logger logger = Logger.getLogger(UploadFormPanelArchive.class);

	protected long maxUploadSizeInKilo;

	protected FileUploadForm form;

	/**
	 * @param id
	 * @param model
	 *            le fichier temporaire qui sera récupéré sur le serveur
	 * @param Doit
	 *            -on afficher la taille maximale permise des fichiers ?
	 */
	public UploadFormPanelArchive(String id, IModel<File> model, final boolean displayMaxFileSize) {
		super(id, model);
		initPanel(model, new StringResourceModel("defaultButtonLabel", this, null),
				new StringResourceModel("progressLabel", this, null), displayMaxFileSize);
	}

	/**
	 * @param id
	 * @param model
	 *            le fichier temporaire qui sera récupéré sur le serveur
	 * @param buttonLabel
	 *            le label du bouton de sélection d'un fichier
	 * @param loadingLabel
	 *            le label à présenter pendant le traitement de l'upload
	 * @param Doit
	 *            -on afficher la taille maximale permise des fichiers ?
	 */
	public UploadFormPanelArchive(String id, IModel<File> model, IModel<String> buttonLabel, IModel<String> loadingLabel,
			final boolean displayMaxFileSize) {
		super(id, model);
		initPanel(model, buttonLabel, loadingLabel, displayMaxFileSize);
	}

	private void initPanel(IModel<File> model, IModel<String> buttonLabel, IModel<String> loadingLabel,
			final boolean displayMaxFileSize) {
		this.maxUploadSizeInKilo = ThaleiaApplication.get().getConfiguration()
				.getDatabaseParameterValueAsLong(Configuration.MAX_UPLOAD_SIZE_PARAM, new Long(100));

		// L'upload de fichier
		form = new FileUploadForm("uploadForm", model);
		add(form);

		// Le libellé du bouton de sélection de fichier
		add(new Label("buttonLabel", buttonLabel));
		add(new Label("buttonLabelDrop", new StringResourceModel("defaultButtonLabelDrop", this, null)));

		// L'indicateur de progression de l'envoi
		add(new ThaleiaUploadProgressBar("progressbar", form, form.fileUploadField, true));
	}

	/**
	 * La méthode appelée après l'upload d'un binaire
	 * 
	 * @param file
	 *            le fichier uploadé (binaire dans un emplacement temporaire)
	 * @param filename
	 *            le nom du fichier
	 * @param target
	 */
	protected abstract void onUpload(File file, String filename, AjaxRequestTarget target);

	private class FileUploadForm extends Form<File> {
		FileUploadField fileUploadField;

		public FileUploadForm(String id, IModel<File> model) {
			super(id, model);

			// nécessaire au fonctionnement
			setMultiPart(true);

			fileUploadField = new FileUploadField("fileInput", new LoadableDetachableModel<List<FileUpload>>() {
				@Override
				protected List<FileUpload> load() {
					return new ArrayList<FileUpload>();
				}
			});
			AjaxFormSubmitBehavior behavior = new AjaxFormSubmitBehavior(this, "onchange") {
				protected void onSubmit(AjaxRequestTarget target) {
					// Le script de la balise "onchange" a déjà été exécuté, et
					// a déclenché l'affichage de l'indicateur de progression.
					// et masqué le label de taille max

					// Dès qu'un fichier est sélectionné, il est envoyé
					// et enregistré.
					FileUploadForm.this.onUploaded(target);
				}
			};
			fileUploadField.setRequired(false).add(behavior);
			add(fileUploadField);

			// Taille maximale permise
			setMaxSize(Bytes.kilobytes(maxUploadSizeInKilo));
		}

		public void onUploaded(AjaxRequestTarget target) {

			// On affiche le formulaire d'upload
			form.setVisible(true);
			target.add(form);

			// On masque l'indicateur de progression
			
			final List<FileUpload> uploads = fileUploadField.getFileUploads();
			if (uploads != null) {
				for (FileUpload upload : uploads) {
					// On créé un fichier temporaire
					String filename = upload.getClientFileName();
					logger.debug("Le fichier est mémorisé sous le nom suivant :" + filename);

					try {
						// On enregistre dans le fichier local contenu dans le
						// modèle
						// logger.debug("On enregistre dans le fichier local
						// contenu dans le modèle");
						File modelFile = (File) getDefaultModelObject();
						// logger.debug("Enregistrement du flux montant dans le
						// fichier "
						// + modelFile.getAbsolutePath());
						upload.writeTo(modelFile);

						// Recherche du message localisé de réussite d'upload
						// logger.debug("Recherche du message localisé de
						// réussite d'upload");
						StringResourceModel confirmMessageModel = new StringResourceModel("upload.confirm", this, null,
								new Object[] { upload.getClientFileName() });

						// On stocke dans les message la réussite de l'upload
						// logger.debug("On stocke dans les message la réussite
						// de l'upload");
						Session.get().info(confirmMessageModel.getString());

					} catch (Exception e) {
						logger.warn("Erreur lors de l'upload d'un fichier : " + e);
						StringResourceModel errorMessageModel = new StringResourceModel("upload.error", this, null,
								new Object[] { upload.getClientFileName() });
						Session.get().error(errorMessageModel.getString());
					}

					// On apelle la suite du traitement après réussite de
					// l'upload
					// logger.debug("On apelle la suite du traitement après
					// réussite de l'upload");
					onUpload((File) UploadFormPanelArchive.this.getDefaultModelObject(), filename, target);

				}
			}
		}
	}

}
