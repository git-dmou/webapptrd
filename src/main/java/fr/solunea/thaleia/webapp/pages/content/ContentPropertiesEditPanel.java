package fr.solunea.thaleia.webapp.pages.content;

import fr.solunea.thaleia.model.ContentPropertyValue;
import fr.solunea.thaleia.model.ContentVersion;
import fr.solunea.thaleia.model.Locale;
import fr.solunea.thaleia.model.dao.ContentPropertyValueDao;
import fr.solunea.thaleia.model.dao.LocaleDao;
import fr.solunea.thaleia.utils.DetailedException;
import fr.solunea.thaleia.webapp.ThaleiaApplication;
import fr.solunea.thaleia.webapp.pages.ErrorPage;
import fr.solunea.thaleia.webapp.panels.UploadFormPanel;
import fr.solunea.thaleia.webapp.security.ThaleiaSession;
import org.apache.log4j.Logger;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.Session;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.OnChangeAjaxBehavior;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.link.DownloadLink;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.PropertyListView;
import org.apache.wicket.markup.html.panel.EmptyPanel;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.*;
import org.apache.wicket.util.time.Duration;

import java.io.File;
import java.util.List;

@SuppressWarnings("serial")
public abstract class ContentPropertiesEditPanel extends Panel {

    private static final Logger logger = Logger.getLogger(ContentPropertiesEditPanel.class);

    private final Form<Void> form;

    private final boolean readOnly;

    private DownloadLink downloadLink;
    private WebMarkupContainer colDownload;
    private WebMarkupContainer colUpload;

    public ContentPropertiesEditPanel(String id, final IModel<ContentVersion> contentVersionModel,
                                      final IModel<Locale> languageModel, final boolean readOnly) {

        super(id, contentVersionModel);

        this.readOnly = readOnly;

        // Le formulaire de gestion de l'édition des propriétés du contenu
        form = new Form<>("form") {
            @Override
            protected void onSubmit() {
                // On recharge un panneau de présentation des propriétés du contenu, pour la locale sélectionnée
                ((MarkupContainer) ContentPropertiesEditPanel.this.get("form")).addOrReplace(getPropertiesEditPanel(contentVersionModel, languageModel, readOnly));
            }
        };
        form.setOutputMarkupId(true);
        form.setMultiPart(true);

        // On fabrique un onglet par langue d'édition des propriétés
        add(getLocaleSelector(contentVersionModel, languageModel));

        try {
            // Le modèle récupéré est : toutes les ContentPropertyValue
            // possibles pour cette version : celles définies en base visibles,
            // et celles
            // (vides) qui pourraient être définies au vu de ce type de contenu.
            form.add(getPropertiesEditPanel(contentVersionModel, languageModel, readOnly));

        } catch (Exception e) {
            logger.warn("Erreur lors de la récupération des valeurs de propriétés pour la dernière version du contenu '"
                    + ContentPropertiesEditPanel.this.getDefaultModelObject() + "'" + e);
            setResponsePage(ErrorPage.class);
        }

        add(form);
    }

    private Component getLocaleSelector(final IModel<ContentVersion> contentVersionModel,
                                        final IModel<Locale> languageModel) {

        final IModel<List<Locale>> localesModel = new LoadableDetachableModel<>() {
            @Override
            protected List<Locale> load() {
                return new LocaleDao(ThaleiaSession.get().getContextService().getContextSingleton()).find();
            }
        };

        return new PropertyListView<>("locales", localesModel) {

            @Override
            protected void populateItem(final ListItem<Locale> item) {
                Link<Void> link = new Link<>("link") {

                    @Override
                    public void onClick() {
                        // On enregistre dans le modèle la locale sélectionnée
                        languageModel.setObject(item.getModelObject());

                        // On recharge le sélecteur de locale
                        addOrReplace(getLocaleSelector(contentVersionModel, languageModel));

                        // On recharge le panneau des propriétés
                        form.addOrReplace(getPropertiesEditPanel(contentVersionModel, languageModel, readOnly));
                    }

                };
                item.add(link);

                // Le nom de la locale
                link.add(new Label("name"));

                // L'onglet est actif si cette locale est celle demandée pour ce
                // panneau
                if (item.getModelObject().equals(languageModel.getObject())) {
                    item.add(new AttributeModifier("class", "active"));
                }
            }

            @Override
            public boolean isVisible() {
                // On ne montre le sélecteur de Locale que si l'on en
                // propose plusieurs.
                return localesModel.getObject().size() > 1;
            }

        }.setOutputMarkupId(true);
    }

    /**
     * @return un panneau d'édition de ces propriétés, dans cette langue.
     */
    private Component getPropertiesEditPanel(IModel<ContentVersion> contentVersionModel, IModel<Locale> languageModel
            , final boolean readOnly) {

        // On récupère la liste de toutes les propriétés (existantes ou pouvant
        // être définies et étant visible) pour cette version, dans la langue
        // demandée.
        ContentPropertyValueDao contentPropertyValueDao = new ContentPropertyValueDao(ThaleiaApplication.get().getConfiguration().getLocalDataDir().getAbsolutePath(),
                ThaleiaApplication.get().getConfiguration().getBinaryPropertyType(), ThaleiaSession.get().getContextService().getContextSingleton());
        IModel<List<? extends ContentPropertyValue>> contentPropertyValuesModel =
                Model.ofList(contentPropertyValueDao.getAllVisiblePropertiesValues(contentVersionModel.getObject(), languageModel.getObject(), ThaleiaSession.get().getAuthenticatedUser()));

        LocaleDao localeDao = new LocaleDao(ThaleiaSession.get().getContextService().getContextSingleton());
        return new PropertyListView<>("objects", contentPropertyValuesModel) {

            @Override
            protected void populateItem(final ListItem<ContentPropertyValue> item) {

                // Le nom de la propriété
                item.add(new Label("property.name", new Model<String>() {
                    @Override
                    public String getObject() {
                        // Le nom de la propriété, dans la locale de l'IHM
                        return item.getModelObject().getName(localeDao.getLocale(ThaleiaSession.get().getLocale()));
                    }
                }));

                // La valeur de la propriété
                final Component textField = new TextField<String>("value",
                        new PropertyModel<>(item.getModelObject(), "value") {
                            @Override
                            public String getObject() {
                                // Si la valeur est un fichier, on ne présente
                                // que son nom, et pas le répertoire qui le
                                // contient.
                                return contentPropertyValueDao.getValue(item.getModelObject());
                            }
                        }) {

                    @Override
                    public boolean isEnabled() {
                        if (readOnly) {
                            return false;
                        }
                        try {
                            // Modifiable si la propriété n'est pas du type
                            // "fichier binaire"
                            return !contentPropertyValueDao.isValueDescribesAFile(item.getModelObject());
                        } catch (Exception e) {
                            logger.warn("Erreur durant l'analyse d'un objet : " + e);
                            return false;
                        }
                    }
                };
                textField.add(new OnChangeAjaxBehavior() {
                    @Override
                    protected void onUpdate(AjaxRequestTarget target) {
                        // On met à jour le champ
                        // target.add(textField);

                        // On remonte l'information de mise à jour
                        onPropertyChanged(target);
                    }
                });

                item.add(textField.setOutputMarkupId(true));

                colDownload = new WebMarkupContainer("colDownload");
                colUpload = new WebMarkupContainer("colUpload");

                item.add(colDownload);
                item.add(colUpload);

                colUpload.add(new AttributeModifier("class", "col-md-12"));
                colDownload.add(new AttributeModifier("class", ""));

                // Un lien de téléchargement du contenu, si le contenu est un
                // fichier.
                downloadLink = (DownloadLink) new DownloadLink("downloadBtn", new AbstractReadOnlyModel<>() {

                    @Override
                    public File getObject() {
                        // Le fichier de ce ContentPropertyValue
                        File file = contentPropertyValueDao.getFile(item.getModelObject());

                        if (file == null || !file.exists()) {
                            logger.warn("Le fichier de cette propriété n'a pas été trouvé : " + item.getModelObject());
                            return null;
                        } else {
                            return file;
                        }
                    }

                }) {

                    @Override
                    public boolean isVisible() {

                        // On cache ce lien si la propriété n'est pas un fichier
                        if (!isBinary(item.getModelObject())) {
                            return false;
                        }

                        // Le fichier de ce ContentPropertyValue
                        File file = contentPropertyValueDao.getFile(item.getModelObject());

                        return file != null && file.exists();
                    }

                }.setCacheDuration(Duration.NONE).add(new Label("downloadLabel", new StringResourceModel(
                        "downloadLabel", this, null)));
                colDownload.add(downloadLink);
                try {
                    // On associe un fichier temporaire pour récolter le
                    // binaire uploadé
                    final UploadFormPanel uploadPanel = new UploadFormPanel("uploadForm",
                            Model.of(ThaleiaApplication.get().getTempFilesService().getTempFile()), true) {
                        @Override
                        public boolean isVisible() {
                            // Dans tous les cas, on cache si lecture seule
                            if (readOnly) {
                                return false;
                            }

                            return isBinary(item.getModelObject());
                        }

                        @Override
                        public void onUpload(File uploadedFile, String filename, Locale locale,
                                             AjaxRequestTarget target) {
                            // O fichier uploadé comme la
                            // nouvelle valeur de cette propriété
                            try {
                                // logger.debug("Enregistrement de la
                                // modification de la propriété '"
                                // + item.getModelObject()
                                // + "' associée au fichier '"
                                // + uploadedFile.getAbsolutePath() + "'.");
                                contentPropertyValueDao.setFile(item.getModelObject(), uploadedFile, filename);

                                // On rafraîchit l'item pour présenter la
                                // modification du modèle.
                                target.add(item);

                                // On remonte l'information de mise à jour
                                onPropertyChanged(target);

                            } catch (DetailedException e) {
                                logger.warn("Erreur d'enregistrement du fichier uploadé :" + e);
                                StringResourceModel errorMessageModel = new StringResourceModel("upload.error", this, null);
                                Session.get().error(errorMessageModel.getString());
                            }
                        }
                    };
                    colUpload.add(uploadPanel.setOutputMarkupId(true));

                    adjustDownloadUploadColumns();

                } catch (Exception e) {
                    logger.warn(
                            "Impossible de générer un fichier temporaire pour l'upload d'une propriété binaire : " + e);
                    item.add(new EmptyPanel("uploadForm").setOutputMarkupId(true));
                }
                item.setOutputMarkupId(true);
            }
        }.setReuseItems(true).setOutputMarkupId(true);
    }

    /**
     * @return true si ce contentPropertyValue concerne une propriété de type
     * "fichier binaire".
     */
    private boolean isBinary(ContentPropertyValue contentPropertyValue) {
        ContentPropertyValueDao contentPropertyValueDao = new ContentPropertyValueDao(ThaleiaApplication.get().getConfiguration().getLocalDataDir().getAbsolutePath(),
                ThaleiaApplication.get().getConfiguration().getBinaryPropertyType(), ThaleiaSession.get().getContextService().getContextSingleton());
        return contentPropertyValueDao.isValueDescribesAFile(contentPropertyValue);
    }

    /**
     * Méthode appelée lorsque la valeur d'un champ a été mise à jour.
     */
    protected void onPropertyChanged(AjaxRequestTarget target) {
        adjustDownloadUploadColumns();
    }

    protected void adjustDownloadUploadColumns() {
        if (downloadLink.isVisible()) {
            colUpload.add(new AttributeModifier("class", "col-md-8"));
            colDownload.add(new AttributeModifier("class", "col-md-4"));
        } else {
            colUpload.add(new AttributeModifier("class", "col-md-12"));
            colDownload.add(new AttributeModifier("class", ""));
        }
    }
}
