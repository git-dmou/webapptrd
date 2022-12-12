package fr.solunea.thaleia.webapp.pages.admin;

import fr.solunea.thaleia.utils.DateUtils;
import fr.solunea.thaleia.webapp.ThaleiaApplication;
import fr.solunea.thaleia.webapp.pages.admin.usage.HistoryOfEventsPanel;
import fr.solunea.thaleia.webapp.pages.admin.usage.HistoryOfEventsPanel.Filter;
import fr.solunea.thaleia.webapp.pages.admin.usage.UsersUsagePanel;
import org.apache.wicket.markup.html.panel.Panel;

import java.util.Calendar;

@SuppressWarnings("serial")
class UsagePanel extends Panel {

    UsagePanel(String id) {
        super(id);

        // Par défaut on présente les 7 derniers jours
        Calendar start = Calendar.getInstance();
        start.add(Calendar.DAY_OF_MONTH, -7);
        Calendar end = Calendar.getInstance();

        // L'historique des dernières connexions
        add(new HistoryOfEventsPanel("loginsHistoryPanel", new Filter(), start, end) {
            @Override
            protected int countEvents(Calendar day, Filter filter) {
                Calendar dayStart = Calendar.getInstance();
                dayStart.setTime(DateUtils.getStartOfDay(day.getTime()));
                Calendar dayEnd = Calendar.getInstance();
                dayEnd.setTime(DateUtils.getEndOfDay(day.getTime()));
                return ThaleiaApplication.get().getStatsService().countIdentifications(true, dayStart.getTime(),
                        dayEnd.getTime());
            }
        });

        //        // L'historique des dernières créations de versions de modules Cannelle
        //        add(new HistoryOfEventsPanel("cannelleVersions", new Filter(), start, end) {
        //            @Override
        //            protected int countEvents(Calendar day, Filter filter) {
        //                try {
        //                    Calendar dayStart = Calendar.getInstance();
        //                    dayStart.setTime(DateUtils.getStartOfDay(day.getTime()));
        //                    Calendar dayEnd = Calendar.getInstance();
        //                    dayEnd.setTime(DateUtils.getEndOfDay(day.getTime()));
        //
        //                    List<ContentVersion> modules = ThaleiaSession.get().getContentService()
        // .getContentVersionByPeriod
        //                            (dayStart.getTime(), dayEnd.getTime(), true);
        //                    return modules.size();
        //                } catch (DetailedException e) {
        //                    logger.warn("Impossible d'obtenir le nombre de créations de versions de modules
        // Cannelle : " + e);
        //                    return 0;
        //                }
        //            }
        //        });


        // Le tableau de détail de l'usage par utilisateur
        add(new UsersUsagePanel("usersUsagePanel"));

    }

}
