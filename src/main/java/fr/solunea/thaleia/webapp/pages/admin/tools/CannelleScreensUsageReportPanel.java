package fr.solunea.thaleia.webapp.pages.admin.tools;

import fr.solunea.thaleia.service.utils.ZipUtils;
import fr.solunea.thaleia.utils.DetailedException;
import fr.solunea.thaleia.webapp.ThaleiaApplication;
import fr.solunea.thaleia.webapp.pages.download.DownloadPage;
import fr.solunea.thaleia.webapp.security.ThaleiaSession;
import org.apache.log4j.Logger;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.panel.Panel;

import java.io.File;
import java.util.Calendar;


/**
 * Le rappport de la répartition des écrans Cannelle utilisés dans les formations
 */
public class CannelleScreensUsageReportPanel extends Panel {

    private static final Logger logger = Logger.getLogger(CannelleScreensUsageReportPanel.class);

    public CannelleScreensUsageReportPanel(String id) {
        super(id);

        AjaxLink<Void> link = new AjaxLink<Void>("screensUsage") {
            @Override
            public void onClick(AjaxRequestTarget target) {
                setResponsePage(new DownloadPage(false, this.getPage()) {

                    @Override
                    protected File prepareFile() throws DetailedException {
                        File report = ThaleiaApplication.get().getTempFilesService().getTempFile("report.csv");
                        ThaleiaSession.get().getContentService().writeScreensUsageReport(report);
                        File archive = ThaleiaApplication.get().getTempFilesService().getTempFile("report.zip");
                        ZipUtils.toZip(report.getParentFile().getAbsolutePath(), archive.getAbsolutePath());
                        return archive;
                    }

                    @Override
                    protected String getFileName() {
                        long timestamp = Calendar.getInstance().getTimeInMillis();
                        return "cannelle_screens_" + timestamp;
                    }

                });
            }
        };
        add(link);
    }

}
