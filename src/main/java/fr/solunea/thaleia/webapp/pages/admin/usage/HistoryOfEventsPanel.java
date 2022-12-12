package fr.solunea.thaleia.webapp.pages.admin.usage;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;

import fr.solunea.thaleia.webapp.security.ThaleiaSession;

@SuppressWarnings("serial")
public abstract class HistoryOfEventsPanel extends Panel {

	public HistoryOfEventsPanel(String id, final Filter filter, Calendar firstDay, Calendar lastDay) {
		super(id);
		refreshAccesses(firstDay, lastDay, filter);
	}

	/**
	 * @param day
	 * @param filter
	 * @return le nombre d'événements à cette date, en appliquant le filtre
	 */
	protected abstract int countEvents(Calendar day, Filter filter);

	public void refreshAccesses(Calendar firstDay, Calendar lastDay, final Filter filter) {
		// Présentation des noms des derniers jours
		SimpleDateFormat format;
		if (Locale.ENGLISH.equals(ThaleiaSession.get().getLocale())) {
			format = new SimpleDateFormat("MM/dd");
		} else {
			format = new SimpleDateFormat("dd/MM");
		}

		// Dernier jour de la periode
		Calendar day = lastDay;
		// Nombre de jours à décompter
		int nbDays = getDifferenceDays(firstDay.getTime(), lastDay.getTime());

		ArrayList<AccessesPanelDay> listDays = new ArrayList<AccessesPanelDay>();
		for (int i = 1; i <= nbDays; i++) {
			// Le nom du jour
			final String dayName = format.format(day.getTime());
			// Le nombre de consultations sur ce jour
			final int dayCount = countEvents(day, filter);

			// Ajoute le couple jour/valeur sur la page
			AccessesPanelDay divDay = new AccessesPanelDay();
			divDay.dayLabel = dayName;
			divDay.dayValue = dayCount;
			listDays.add(divDay);

			// Le jour d'avant
			day.add(Calendar.DAY_OF_YEAR, -1);
		}

		ListView<AccessesPanelDay> listDaysView = new ListView<AccessesPanelDay>("listDays", listDays) {
			@Override
			protected void populateItem(ListItem<AccessesPanelDay> item) {
				item.add(new Label("dayLabel", item.getModelObject().dayLabel));
				item.add(new Label("dayValue", item.getModelObject().dayValue));
			}
		};

		addOrReplace(listDaysView);
	}

	private int getDifferenceDays(Date d1, Date d2) {
		long diffTime = d2.getTime() - d1.getTime();
		long diffDays = diffTime / (24 * 60 * 60 * 1000) + 1;
		return (int) diffDays;
	}

	public static class Filter implements Serializable {

	}
}

@SuppressWarnings("serial")
class AccessesPanelDay extends Object implements Serializable {
	public String dayLabel;
	public int dayValue;
}
