package fr.solunea.thaleia.webapp.download;

import fr.solunea.thaleia.model.DownloadableFile;
import fr.solunea.thaleia.model.User;
import fr.solunea.thaleia.model.dao.DownloadableFileDao;
import fr.solunea.thaleia.service.DownloadableFileService;
import fr.solunea.thaleia.service.utils.Configuration;
import fr.solunea.thaleia.utils.DetailedException;
import fr.solunea.thaleia.webapp.ThaleiaApplication;
import fr.solunea.thaleia.webapp.ThaleiaApplicationTester;
import fr.solunea.thaleia.webapp.security.ThaleiaSession;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.junit.jupiter.api.Test;

import java.io.File;

public class DownloadFilePageTest extends ThaleiaApplicationTester {

    private static final Logger logger = Logger.getLogger(DownloadFilePageTest.class);

    /**
     * Vérifie le message d'erreur en cas d'absence de référence de fichier.
     */
    @Test
    public void rendersSuccessfullyIfNoParams() {
        ThaleiaSession.get().authenticateWithNoStats("admin");
        DownloadFilePage downloadFilePage = new DownloadFilePage();
        tester.startPage(downloadFilePage);
        tester.assertRenderedPage(DownloadFilePage.class);
        tester.assertInvisible("downloadLink");
        tester.assertErrorMessages(downloadFilePage.getString("no.reference"));
    }

    /**
     * Vérifie le message d'erreur en cas de mauvaise référence de fichier.
     */
    @Test
    public void rendersSuccessfullyIfWrongParams() {
        ThaleiaSession.get().authenticateWithNoStats("admin");
        PageParameters parameters = new PageParameters();
        parameters.add(Configuration.DOWNLOADABLE_FILES_REFERENCE_PARAMETER, "___wrongReference___");
        DownloadFilePage downloadFilePage = new DownloadFilePage(parameters);
        tester.startPage(downloadFilePage);
        tester.assertRenderedPage(DownloadFilePage.class);
        tester.assertInvisible("downloadLink");
        tester.assertErrorMessages(downloadFilePage.getString("reference.unknown"), downloadFilePage.getString("default.error"));
    }

    /**
     * Fabrique un fichier à télécharger, puis vérifie la bonne présentation et le fonctionnement du lien de téléchargement.
     */
    @Test
    public void rendersSuccessfullyIfParams() throws Exception {
        ThaleiaSession.get().authenticateWithNoStats("admin");

        DownloadableFileDao downloadableFileDao = new DownloadableFileDao(ThaleiaSession.get().getContextService().getContextSingleton());
        User admin = ThaleiaSession.get().getAuthenticatedUser();

        // Création d'un fichier à télécharger
        DownloadableFileService downloadableFileService = ThaleiaApplication.get().getDownloadableFileService();
        File file = File.createTempFile(DownloadFilePageTest.class.getName(), "txt");
        FileUtils.writeStringToFile(file, "Test", "UTF-8");
        DownloadableFile downloadableFile = downloadableFileService.createDownloadableFile(file, "A test file", admin, admin, false);
        downloadableFileDao.save(downloadableFile);

        try {
            PageParameters parameters = new PageParameters();
            parameters.add(Configuration.DOWNLOADABLE_FILES_REFERENCE_PARAMETER, downloadableFile.getReference());
            tester.startPage(new DownloadFilePage(parameters));
            tester.assertRenderedPage(DownloadFilePage.class);
            tester.assertVisible("downloadLink");
            // Test du téléchargement
            tester.clickLink("downloadLink");

            // On vérifie la date de dernier téléchargement, qui a été comitté dans le contexte de l'application
            DownloadableFileDao applicationDownloadableFileDao = new DownloadableFileDao(ThaleiaApplication.get().contextService.getContextSingleton());
            DownloadableFile applicationDownloadableFile = applicationDownloadableFileDao.findByReference(downloadableFile.getReference());
            if (applicationDownloadableFile.getLastDownload() == null) {
                throw new Exception("La date de dernier téléchargement n'a pas été enregsitrée !");
            }

        } catch (Exception e) {
            logger.warn(e);
            throw e;

        } finally {
            // Nettoyage du fichier utilisé pour les tests
            try {
                downloadableFileService.delete(downloadableFile);
            } catch (DetailedException e) {
                logger.warn(e);
            }
        }
    }
}
