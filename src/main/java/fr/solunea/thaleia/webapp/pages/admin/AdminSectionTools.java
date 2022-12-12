package fr.solunea.thaleia.webapp.pages.admin;

import fr.solunea.thaleia.model.User;
import fr.solunea.thaleia.model.dao.UserDao;
import fr.solunea.thaleia.utils.DateUtils;
import fr.solunea.thaleia.utils.DetailedException;
import fr.solunea.thaleia.webapp.ThaleiaApplication;
import fr.solunea.thaleia.webapp.pages.admin.tools.CannelleScreensUsageReportPanel;
import fr.solunea.thaleia.webapp.pages.admin.tools.CheckUsagePanel;
import fr.solunea.thaleia.webapp.pages.admin.tools.FrontEndsPanel;
import fr.solunea.thaleia.webapp.pages.editcontent.EditContentPage;
import fr.solunea.thaleia.webapp.panels.confirmation.ConfirmationLink;
import fr.solunea.thaleia.webapp.security.ThaleiaSession;
import org.apache.cayenne.ObjectContext;
import org.apache.commons.lang.time.DurationFormatUtils;
import org.apache.log4j.Logger;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.authroles.authorization.strategies.role.annotations.AuthorizeInstantiation;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.PropertyListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.protocol.http.IRequestLogger;
import org.apache.wicket.protocol.http.IRequestLogger.SessionData;

import java.util.Arrays;
import java.util.List;

@SuppressWarnings("serial")
@AuthorizeInstantiation("admin")
public class AdminSectionTools extends Panel {

    private static final Logger logger = Logger.getLogger(AdminSectionTools.class);

    public AdminSectionTools(String id) {
        super(id);

        add(new ResetPanel("resetPanel"));
        add(new VersionPanel("versionPanel"));
        add(new CheckUsagePanel("checkUsagePanel"));
        add(new CannelleScreensUsageReportPanel("cannelleScreensUsageReportPanel"));
        add(new Link<Void>("editContentLink") {

            @Override
            public void onClick() {
                setResponsePage(EditContentPage.class);
            }

        });
        add(new FrontEndsPanel("frontendsPanel"));

        // Le lien "Supprimer les anciennes versions des contenus"
        add(new ConfirmationLink<Void>("cleanVersions", new StringResourceModel("cleanVersions.confirm", this, null,
                (Object) null)) {
            @Override
            public void onClick(AjaxRequestTarget target) {
                try {
                    ObjectContext context = ThaleiaSession.get().getContextService().getNewContext();
                    UserDao userDao = new UserDao(context);
                    for (User user : userDao.find()) {
                        ThaleiaSession.get().getContentService().deleteOldVersions(user);
                    }
                    context.commitChanges();
                    info(new StringResourceModel("cleanVersions.ok", AdminSectionTools.this, null, (Object) null).getString());

                } catch (DetailedException e) {
                    logger.warn("Erreur lors de la suppression des anciennes versions des contenus : " + e);
                    error(new StringResourceModel("cleanVersions.error", AdminSectionTools.this, null, (Object) null).getString());
                }
                setResponsePage(target.getPage());
            }
        });

        // Le tableau de présentation des sessions
        PropertyListView<SessionData> table = new PropertyListView<>("sessions",
                new AbstractReadOnlyModel<List<SessionData>>() {
                    @Override
                    public List<SessionData> getObject() {
                        // Les sessions actives
                        IRequestLogger requestLogger = ThaleiaApplication.get().getRequestLogger();
                        SessionData[] sessions = requestLogger.getLiveSessions();
                        return Arrays.asList(sessions);
                    }
                }) {

            @Override
            protected void populateItem(final ListItem<SessionData> item) {
                item.add(new Label("startDate", new AbstractReadOnlyModel<String>() {
                    @Override
                    public String getObject() {
                        return DateUtils.formatDate(item.getModel().getObject().getStartDate(),
                                ThaleiaSession.get().getLocale());

                    }
                }));
                item.add(new Label("lastActive", new AbstractReadOnlyModel<String>() {
                    @Override
                    public String getObject() {
                        return DateUtils.formatDate(item.getModel().getObject().getLastActive(),
                                ThaleiaSession.get().getLocale());

                    }
                }));
                item.add(new Label("sessionSize"));
                item.add(new Label("totalTimeTaken", new AbstractReadOnlyModel<String>() {
                    @Override
                    public String getObject() {
                        return DurationFormatUtils.formatDurationHMS(item.getModel().getObject().getTotalTimeTaken());
                    }
                }));
                item.add(new Label("sessionInfo"));
            }
        };
        add(table);
    }
}
