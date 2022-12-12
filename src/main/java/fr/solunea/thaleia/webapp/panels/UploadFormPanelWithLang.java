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
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.upload.FileUpload;
import org.apache.wicket.markup.html.form.upload.FileUploadField;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
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
public abstract class UploadFormPanelWithLang extends Panel {

    protected static final Logger logger = Logger.getLogger(UploadFormPanelWithLang.class);

    private long maxUploadSizeInKilo;

    private Label maxSizeLabel;

    private FileUploadForm form;

    /**
     * @param model              le fichier temporaire qui sera récupéré sur le serveur
     * @param displayMaxFileSize Doit-on afficher la taille maximale permise des fichiers ?
     */
    public UploadFormPanelWithLang(String id, IModel<File> model, final boolean displayMaxFileSize,
                                   IModel<Locale> selectedLangModel) {
        super(id, model);
        initPanel(model, new StringResourceModel("defaultButtonLabel", this, null), displayMaxFileSize, selectedLangModel);
    }

    /**
     * @param model              le fichier temporaire qui sera récupéré sur le serveur
     * @param buttonLabel        le label du bouton de sélection d'un fichier
     * @param displayMaxFileSize Doit-on afficher la taille maximale permise des fichiers ?
     */
    public UploadFormPanelWithLang(String id, IModel<File> model, IModel<String> buttonLabel, final boolean displayMaxFileSize, IModel<Locale> selectedLocaleModel) {
        super(id, model);
        initPanel(model, buttonLabel, displayMaxFileSize, selectedLocaleModel);
    }

    private void initPanel(IModel<File> model, IModel<String> buttonLabel, final boolean displayMaxFileSize, final IModel<Locale> selectedLocaleModel) {
        this.maxUploadSizeInKilo = ThaleiaApplication.get().getConfiguration().getDatabaseParameterValueAsLong(Configuration.MAX_UPLOAD_SIZE_PARAM, 100L);

        // L'upload de fichier
        form = new FileUploadForm("uploadForm", model);
        add(form);

        // L'indicateur de progression de l'envoi
        add(new ThaleiaUploadProgressBar("progressbar", form, form.fileUploadField, false));

        // Le label de taille max
        String maxUploadSizeReadable = FormatUtils.humanReadableByteCount(maxUploadSizeInKilo * 1000, true);
        maxSizeLabel = new Label("maxSizeLabel",
                new StringResourceModel("maxSizeLabel", this, null, new Object[]{maxUploadSizeReadable})) {
            @Override
            public boolean isVisible() {
                return displayMaxFileSize;
            }
        };
        add(maxSizeLabel.setOutputMarkupPlaceholderTag(true).setOutputMarkupId(true));

        // Le libellé du bouton de sélection de fichier
        add(new Label("buttonLabel", buttonLabel));
        add(new Label("buttonLabelDrop", new StringResourceModel("defaultButtonLabelDrop", this, null)));

        LocaleDao localeDao = new LocaleDao(ThaleiaSession.get().getContextService().getContextSingleton());
        final Label currentSelectedLang = new Label("currentSelectedLang", new Model<String>() {
            @Override
            public String getObject() {
                return localeDao.getDisplayName(selectedLocaleModel.getObject(), ThaleiaSession.get().getLocale());
            }
        });
        currentSelectedLang.setOutputMarkupId(true);
        add(currentSelectedLang);

        final ListView<Locale> listView = new ListView<Locale>("langSelectorRow", localeDao.find()) {
            @Override
            public void populateItem(final ListItem<Locale> item) {
                final AjaxLink<?> lnk = new AjaxLink<>("langSelectorLink", Model.of(item.getModelObject())) {
                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        Locale selectedLocale = localeDao.get(item.getModelObject().getObjectId());
                        selectedLocaleModel.setObject(selectedLocale);
                        target.add(currentSelectedLang);
                        onSelectedLocaleChanged(target);
                    }
                };
                lnk.add(new Label("linktext", new Model<String>() {
                    @Override
                    public String getObject() {
                        return localeDao.getDisplayName(localeDao.get(item.getModelObject().getObjectId()), ThaleiaSession.get().getLocale());
                    }
                }));
                item.add(lnk.setOutputMarkupId(true));
            }
        };
        add(listView.setOutputMarkupId(true));

    }

    /**
     * La méthode appelée après l'upload d'un binaire
     *
     * @param file     le fichier uploadé (binaire dans un emplacement temporaire)
     * @param filename le nom du fichier
     */
    protected abstract void onUpload(File file, String filename, AjaxRequestTarget target);

    /**
     * La méthode appelée après la sélection d'une nouvelle locale.
     */
    protected abstract void onSelectedLocaleChanged(AjaxRequestTarget target);

    private class FileUploadForm extends Form<File> {
        FileUploadField fileUploadField;

        public FileUploadForm(String id, IModel<File> model) {
            super(id, model);

            // nécessaire au fonctionnement
            setMultiPart(true);

            fileUploadField = new FileUploadField("fileInput", new LoadableDetachableModel<>() {
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

        public void onUploaded(AjaxRequestTarget target) {

            // On affiche le label de taille max
            maxSizeLabel.setVisible(true);
            target.add(maxSizeLabel);

            // On affiche le formulaire d'upload
            form.setVisible(true);
            target.add(form);

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
                                new Object[]{upload.getClientFileName()});

                        // On stocke dans les message la réussite de l'upload
                        // logger.debug("On stocke dans les message la réussite
                        // de l'upload");
                        Session.get().info(confirmMessageModel.getString());

                    } catch (Exception e) {
                        logger.warn("Erreur lors de l'upload d'un fichier : " + e);
                        StringResourceModel errorMessageModel = new StringResourceModel("upload.error", this, null,
                                new Object[]{upload.getClientFileName()});
                        Session.get().error(errorMessageModel.getString());
                    }

                    // On apelle la suite du traitement après réussite de
                    // l'upload
                    // logger.debug("On apelle la suite du traitement après
                    // réussite de l'upload");
                    onUpload((File) UploadFormPanelWithLang.this.getDefaultModelObject(), filename, target);

                }
            }
        }
    }

}
