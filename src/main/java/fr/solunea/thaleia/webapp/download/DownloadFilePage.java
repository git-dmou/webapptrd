package fr.solunea.thaleia.webapp.download;

import fr.solunea.thaleia.model.DownloadableFile;
import fr.solunea.thaleia.model.User;
import fr.solunea.thaleia.model.dao.DownloadableFileDao;
import fr.solunea.thaleia.service.DownloadableFileService;
import fr.solunea.thaleia.service.utils.Configuration;
import fr.solunea.thaleia.utils.DetailedException;
import fr.solunea.thaleia.webapp.ThaleiaApplication;
import fr.solunea.thaleia.webapp.pages.BasePage;
import fr.solunea.thaleia.webapp.pages.LoginPage;
import fr.solunea.thaleia.webapp.panels.ThaleiaFeedbackPanel;
import fr.solunea.thaleia.webapp.security.ThaleiaSession;
import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.DownloadLink;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.util.encoding.UrlEncoder;
import org.apache.wicket.util.string.StringValue;
import org.apache.wicket.util.time.Duration;

import java.io.File;
import java.util.Calendar;

public class DownloadFilePage extends BasePage {

    public DownloadFilePage() {
        super();
        checkAuthenticated();
        initPage();
        error(getString("no.reference"));
        add(new DownloadLink("downloadLink", new File("")).setVisible(false));
        add(new Label("filename").setVisible(false));
        add(new Label("description").setVisible(false));
    }

    private void checkAuthenticated() {
        User authenticatedUser = ThaleiaSession.get().getAuthenticatedUser();
        if (authenticatedUser == null) {
            setResponsePage(new LoginPage(RequestCycle.get().getRequest().getClientUrl().toString()));
        }
    }

    public DownloadFilePage(PageParameters parameters) {
        super();
        initPage();
        checkAuthenticated();

        try {
            // Extraction de la référence du fichier à télécharger
            // Le chemin est de type /download/123456
            StringValue referenceSV = parameters.get(Configuration.DOWNLOADABLE_FILES_REFERENCE_PARAMETER);
            if (referenceSV == null) {
                error(getString("no.reference"));
                throw new Exception("Pas de référence.");
            }
            String reference = referenceSV.toString();

            // Recherche d'un downloadable file avec cette référence
            DownloadableFileDao downloadableFileDao = ThaleiaApplication.get().getDownloadableFileDao();
            DownloadableFile downloadableFile = downloadableFileDao.findByReference(reference);
            if (downloadableFile == null) {
                error(getString("reference.unknown"));
                throw new Exception("Référence '" + reference + "' inconnue.");
            }

            // Vérification du compte utilisateur
            User authenticatedUser = ThaleiaSession.get().getAuthenticatedUser();
            if (!downloadableFile.getDownloadableBy().getLogin().equalsIgnoreCase(authenticatedUser.getLogin())) {
                error(getString("rights.error"));
                throw new Exception("Impossible de télécharger la référence '" + reference + "' par l'utilisateur "
                        + authenticatedUser.getLogin());
            }

            // Envoi du binaire
            // On renvoie ce binaire
            String encodedFileName = addDownloadLink(this, downloadableFile);
            add(new Label("filename", encodedFileName));
            add(new Label("description", downloadableFile.getDescription()));


        } catch (Exception e) {
            logger.info(e);
            error(getString("default.error"));
            add(new Label("filename").setVisible(false));
            add(new Label("description").setVisible(false));
            add(new DownloadLink("downloadLink", new File("")).setVisible(false));
        }

    }

    public static String addDownloadLink(MarkupContainer container, DownloadableFile downloadableFile) throws DetailedException {
        DownloadableFileService downloadableFileService = ThaleiaApplication.get().getDownloadableFileService();
        File downloadedFile = downloadableFileService.getFile(downloadableFile);
        String encodedFileName = UrlEncoder.QUERY_INSTANCE.encode(downloadedFile.getName(), "UTF-8");
        container.add(new DownloadLink("downloadLink", downloadedFile, encodedFileName) {
            @Override
            public void onClick() {
                super.onClick();
                downloadableFile.setLastDownload(Calendar.getInstance().getTime());
                try {
                    ThaleiaApplication.get().getDownloadableFileDao().save(downloadableFile);
                } catch (DetailedException e) {
                    logger.warn("Impossible d'enregistrer le téléchargement du fichier ref="
                            + downloadableFile.getReference() + " : " + e);
                    ThaleiaApplication.get().getDownloadableFileDao().rollback();
                }
            }
        }.setCacheDuration(Duration.ONE_SECOND).setDeleteAfterDownload(false));
        return encodedFileName;
    }

    private void initPage() {
        // Panneau de feedback
        final ThaleiaFeedbackPanel feedbackPanel =
                (ThaleiaFeedbackPanel) new ThaleiaFeedbackPanel("feedbackPanel").setOutputMarkupId(true);
        add(feedbackPanel);
    }

}
