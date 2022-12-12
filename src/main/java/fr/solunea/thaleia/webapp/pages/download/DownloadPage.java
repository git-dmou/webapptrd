package fr.solunea.thaleia.webapp.pages.download;

import fr.solunea.thaleia.service.utils.FilenamesUtils;
import fr.solunea.thaleia.utils.DetailedException;
import fr.solunea.thaleia.utils.LogUtils;
import fr.solunea.thaleia.webapp.pages.BasePage;
import fr.solunea.thaleia.webapp.panels.ThaleiaFeedbackPanel;
import fr.solunea.thaleia.webapp.panels.ThaleiaLazyLoadPanel;
import fr.solunea.thaleia.webapp.utils.MessagesUtils;
import org.apache.commons.lang.time.DateFormatUtils;
import org.apache.wicket.Component;
import org.apache.wicket.Page;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.markup.html.link.DownloadLink;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.request.http.WebResponse;
import org.apache.wicket.request.resource.PackageResourceReference;
import org.apache.wicket.util.encoding.UrlEncoder;
import org.apache.wicket.util.time.Duration;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

@SuppressWarnings("serial")
public abstract class DownloadPage extends BasePage {

    private Page backPage;

    transient ThaleiaLazyLoadPanel donePanel;

    /**
     * @param backPage si null, alors pas de bouton retour. Sinon, on présente un
     *                 bouton retour vers cette page.
     */
    public DownloadPage(boolean showMenuActions, Page backPage) {
        super(showMenuActions);
        this.backPage = backPage;
        initDownloadPage(showMenuActions);
    }

    public DownloadPage() {
        super(true);
        this.backPage = null;
        initDownloadPage(true);
    }

    // Pour s'assurer que l'objet à télécharger n'est pas mis en cache.
    @Override
    protected void setHeaders(WebResponse response) {
        response.addHeader("Cache-Control", "no-cache, max-age=0,must-revalidate, no-store");
    }

    private void initDownloadPage(boolean showMenuActions) {
        // On place un panel temporaire qui indique que la préparation de
        // l'aperçu est en cours. Ce panel sera remplacé par la page du contenu
        // temporaire.
        donePanel = new ThaleiaLazyLoadPanel("treatment") {

            @Override
            public Component getLazyLoadComponent(String id) {
                return new DonePanel(id);
            }

            @Override
            protected Component getLoadingComponent(String markupId) {
                return new LoadingPanel(markupId);
            }
        };
        add(donePanel);
    }

    /**
     * @return le fichier à télécharger, ou null s'il n'a pas été possible de le
     * préparer.
     */
    protected abstract File prepareFile() throws DetailedException;

    /**
     * @return le nom du fichier qui doit servir pour préparer le nom du fichier
     * à télécharger.
     */
    protected abstract String getFileName();

    protected class DonePanel extends Panel {

        public DonePanel(String id) {
            super(id);

            // Panneau de feedback
            add(new ThaleiaFeedbackPanel("feedbackPanel").setOutputMarkupId(true));

            // Nom du fichier à télécharger
            String contentTitle = FilenamesUtils.getNormalizeString(getFileName());
            String horodate = DateFormatUtils.format(Calendar.getInstance(), "yyyyMMdd-HHmmss");
            String downloadFilename = contentTitle + "_" + horodate + ".zip";
            String encodedFileName = UrlEncoder.QUERY_INSTANCE.encode(downloadFilename, "UTF-8");

            // Le bouton de retour
            add(new Link<Void>("backLink") {
                @Override
                public void onClick() {
                    setResponsePage(backPage);
                }

                @Override
                public boolean isVisible() {
                    return backPage != null;
                }
            }.add(new Image("ico_backarrow_header", new PackageResourceReference(BasePage.class,
                    "img/ico_backarrow_header.png"))));

            // On déclenche la préparation du fichier maitenant, pour que durant
            // l'instanciation de ce pannel, le pannel de traitement en cours
            // reste présenté.
            List<String> errorMessagesKeys = new ArrayList<String>();
            try {
                final File downloadable = prepareFile();

                // On indique la préparation du package a réussi, et sa
                // taille
                Double size = downloadable.length() / 1000000.0;
                info(MessagesUtils.getLocalizedMessage("module.file.size", DownloadPage.class, size));

                IModel<File> downloadedFile = new LoadableDetachableModel<File>() {
                    @Override
                    public File load() {
                        return downloadable;
                    }
                };

                add(new DownloadLink("download", downloadedFile, encodedFileName) {
                    @Override
                    public boolean isVisible() {
                        // On ne présente pas le bouton si le fichier n'a pas pu
                        // être préparé
                        return downloadable != null;
                    }
                }.setCacheDuration(Duration.ONE_SECOND).setDeleteAfterDownload(false));

            } catch (Exception e) {
                // Pour faire moins peur, on n'envoie pas sur la
                // page d'erreur.
                logger.warn("Erreur de préparation du fichier à télécharger :" + e);
                logger.warn(LogUtils.getStackTrace(e.getStackTrace()));

                // On présente les erreurs transmises par le
                // traitement
                for (String key : errorMessagesKeys) {
                    error(MessagesUtils.getLocalizedMessage(key, DownloadPage.class, (Object[]) null));
                }
                error(MessagesUtils.getLocalizedMessage("export.error", DownloadPage.class, (Object[]) null));

                // On masque le bouton de téléchargement
                addOrReplace(new WebMarkupContainer("download").setVisible(false));
            }
        }
    }

    protected class LoadingPanel extends Panel {
        public LoadingPanel(String id) {
            super(id);
            add(new Label("progressLabel", new StringResourceModel("progressLabel", DownloadPage.this, null)));
        }
    }

}
