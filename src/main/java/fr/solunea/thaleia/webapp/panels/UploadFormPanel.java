package fr.solunea.thaleia.webapp.panels;

import fr.solunea.thaleia.model.Locale;
import fr.solunea.thaleia.model.dao.LocaleDao;
import fr.solunea.thaleia.service.utils.Configuration;
import fr.solunea.thaleia.utils.FormatUtils;
import fr.solunea.thaleia.webapp.ThaleiaApplication;
import fr.solunea.thaleia.webapp.panels.progress.ThaleiaUploadProgressBar;
import fr.solunea.thaleia.webapp.security.ThaleiaSession;
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

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Réalise l'upload, et enregistre le binaire dans le fichier passé en modèle.
 * Remplace le nom de ce fichier par celui transmis lors de l'upload.
 */
@SuppressWarnings("serial")
public abstract class UploadFormPanel extends Panel {

    protected static final Logger logger = Logger.getLogger(UploadFormPanel.class);

    private long maxUploadSizeInKilo;

    protected Label maxSizeLabel;

    protected FileUploadForm form;

    protected UploadFormPanel(String id, IModel<File> model, IModel<String> uploadButtonLabel, final boolean
            displayMaxFileSize) {
        super(id, model);
        init(model, uploadButtonLabel, displayMaxFileSize);
    }

    /**
     * @param model              le fichier temporaire qui sera récupéré sur le serveur
     * @param displayMaxFileSize Doit-on afficher la taille maximale permise des fichiers ?
     */
    protected UploadFormPanel(String id, IModel<File> model, final boolean displayMaxFileSize) {
        super(id, model);
        IModel<String> buttonLabel = new StringResourceModel("defaultButtonLabel", this, null);
        init(model, buttonLabel, displayMaxFileSize);
    }

    private void init(IModel<File> model, IModel<String> buttonLabel, final boolean displayMaxFileSize) {

        this.maxUploadSizeInKilo = ThaleiaApplication.get().getConfiguration().getDatabaseParameterValueAsLong
                (Configuration.MAX_UPLOAD_SIZE_PARAM, 100L);

        // L'upload de fichier
        form = new FileUploadForm("uploadForm", model);
        add(form);

        // Le label de taille max
        String maxUploadSizeReadable = FormatUtils.humanReadableByteCount(maxUploadSizeInKilo * 1000, true);
        maxSizeLabel = new Label("maxSizeLabel", new StringResourceModel("maxSizeLabel", this, null, new
                Object[]{maxUploadSizeReadable})) {
            @Override
            public boolean isVisible() {
                return displayMaxFileSize;
            }
        };
        add(maxSizeLabel.setOutputMarkupId(true));

        // Le libellé du bouton de sélection de fichier
        add(new Label("buttonLabel", buttonLabel));
        add(new Label("buttonLabelDrop", new StringResourceModel("defaultButtonLabelDrop", this, null)));

        // L'indicateur de progression de l'envoi
        add(new ThaleiaUploadProgressBar("progressbar", form, form.fileUploadField, true));
    }

    /**
     * La méthode appelée après l'upload d'un binaire
     *
     * @param file     le fichier uploadé (binaire dans un emplacement temporaire)
     * @param filename le nom du fichier
     */
    protected abstract void onUpload(File file, String filename, Locale locale, AjaxRequestTarget target);


    private class FileUploadForm extends Form<File> {
        FileUploadField fileUploadField;

        FileUploadForm(String id, IModel<File> model) {
            super(id, model);

            // nécessaire au fonctionnement
            setMultiPart(true);

            fileUploadField = new FileUploadField("fileInput", new LoadableDetachableModel<List<FileUpload>>() {
                @Override
                protected List<FileUpload> load() {
                    return new ArrayList<>();
                }
            });
            AjaxFormSubmitBehavior behavior = new AjaxFormSubmitBehavior(this, "onchange") {
                @Override
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

        void onUploaded(AjaxRequestTarget target) {

            // On affiche le label de taille max
            maxSizeLabel.setVisible(true);
            target.add(maxSizeLabel);

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
                        StringResourceModel confirmMessageModel = new StringResourceModel("upload.confirm",
                                this, null, new Object[]{upload.getClientFileName()});

                        // On stocke dans les message la réussite de l'upload
                        // logger.debug("On stocke dans les message la réussite
                        // de l'upload");
                        Session.get().info(confirmMessageModel.getString());

                    } catch (Exception e) {
                        logger.warn("Erreur lors de l'upload d'un fichier : " + e);
                        StringResourceModel errorMessageModel = new StringResourceModel("upload.error",
                                this, null, new Object[]{upload.getClientFileName()});
                        Session.get().error(errorMessageModel.getString());
                    }

                    // Vu que la locale passée est null et que ça fait une superbe NPE, on va passer la locale de
                    // l'utilisateur courant ! Et si c'est moche je m'en tape !
                    // @Nflo ^^
                    // Locale currentLocale = ThaleiaSession.get().getAuthenticatedUser().getPreferedLocale();

                    LocaleDao localeDao = new LocaleDao(ThaleiaApplication.get().contextService.getContextSingleton());
                    Locale currentLocale = localeDao.getLocale(ThaleiaSession.get().getLocale());

                    // On apelle la suite du traitement après réussite de l'upload
                    // logger.debug("On apelle la suite du traitement après réussite de l'upload");
                    onUpload((File) UploadFormPanel.this.getDefaultModelObject(), filename, currentLocale, target);

                }
            }
        }
    }

}
