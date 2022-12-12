package fr.solunea.thaleia.webapp.pages.plugins;

import java.io.File;

import org.apache.cayenne.ObjectContext;
import org.apache.log4j.Logger;
import org.apache.wicket.Component;
import org.apache.wicket.Session;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.ajax.markup.html.IndicatingAjaxLink;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;

import fr.solunea.thaleia.model.Locale;
import fr.solunea.thaleia.model.Plugin;
import fr.solunea.thaleia.utils.DetailedException;
import fr.solunea.thaleia.webapp.ThaleiaApplication;
import fr.solunea.thaleia.webapp.panels.ThaleiaFeedbackPanel;
import fr.solunea.thaleia.webapp.panels.UploadFormPanel;
import fr.solunea.thaleia.webapp.security.ThaleiaSession;

@SuppressWarnings("serial")
public abstract class EditPluginPanel extends Panel {

    private static final Logger logger = Logger.getLogger(EditPluginPanel.class);

    protected ThaleiaFeedbackPanel feedbackPanel;
    protected Component cancelButton;

    public EditPluginPanel(String id, final IModel<Plugin> model) {
        super(id, model);

        // On fabrique un contexte de session spécifique à cet objet à éditer.
        setDefaultModel(new CompoundPropertyModel<>(Model.of(model.getObject())));

        feedbackPanel = new ThaleiaFeedbackPanel("feedback");
        add(feedbackPanel.setOutputMarkupId(true));

        final Form<Plugin> form = new Form<Plugin>("pluginForm", model);
        form.setOutputMarkupId(true);

        // Le modèle du fichier uploadé
        File tempFile = null;
        try {
            tempFile = ThaleiaApplication.get().getTempFilesService().getTempFile();
        } catch (Exception e) {
            logger.warn("Impossible de créer un fichier temporaire : " + e);
        }
        IModel<File> uploadedModel = Model.of(tempFile);

        // On associe un fichier temporaire pour récolter le
        // binaire uploadé
        final UploadFormPanel uploadPanel = new UploadFormPanel("uploadFormPanel", uploadedModel, true) {

            @Override
            public void onUpload(File uploadedFile, String filename, Locale locale, AjaxRequestTarget target) {
                // On enregistre le fichier uploadé comme un JAR de plugin.
                try {
                    Plugin plugin = (Plugin) EditPluginPanel.this.getDefaultModelObject();
                    // logger.debug("Plugin à sauver : " + plugin);
                    // logger.debug("Domaine du plugin à sauver : "
                    // + plugin.getDomain());
                    ThaleiaSession.get().getPluginService().savePlugin(plugin, uploadedFile, filename);

                    onOut();

                } catch (DetailedException e) {
                    logger.warn("Erreur d'enregistrement du fichier uploadé :" + e);
                    StringResourceModel errorMessageModel = new StringResourceModel("upload.error",
                            this, null);
                    Session.get().error(errorMessageModel.getString());

                    // On présente les messages
                    target.add(feedbackPanel);
                }
            }
        };
        form.add(uploadPanel.setOutputMarkupId(true));

        // Les boutons enregistrer et annuler
        cancelButton = getCancelButton(model).setOutputMarkupId(true);
        form.add(cancelButton);

        add(form.setOutputMarkupId(true));
    }

    private IndicatingAjaxLink<Plugin> getCancelButton(final IModel<Plugin> objectModel) {
        return new IndicatingAjaxLink<Plugin>("cancel") {
            @Override
            public void onClick(AjaxRequestTarget target) {
                onOut();
            }

        };
    }

    /**
     * Méthode appelée après un enregistrement ou une annulation de
     * modification. Y placer une éventuelle redirection.
     */
    protected abstract void onOut();

}
