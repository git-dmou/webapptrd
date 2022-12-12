package fr.solunea.thaleia.webapp.pages.admin.tools;

import java.io.File;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.core.util.string.JavaScriptUtils;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.StringResourceModel;

import fr.solunea.thaleia.webapp.ThaleiaApplication;

@SuppressWarnings("serial")
public class CheckUsagePanel extends Panel {

	private static final Logger logger = Logger
			.getLogger(CheckUsagePanel.class);

	public CheckUsagePanel(String id) {
		super(id);

		AjaxLink<Void> link = new AjaxLink<Void>("link") {

			@Override
			public void onClick(AjaxRequestTarget target) {

				try {
					File localDataDir = ThaleiaApplication.get()
							.getConfiguration().getLocalDataDir();

					// Les données d'occupation disque
					String used = FileUtils.byteCountToDisplaySize(FileUtils
							.sizeOfDirectory(localDataDir));
					String free = FileUtils.byteCountToDisplaySize(localDataDir
							.getFreeSpace());

					// Le message localisé
					StringResourceModel messageModel = new StringResourceModel(
							"usage", CheckUsagePanel.this, null, new Object[] {
									used, free });
					// On échappe les ' et "
					String message = JavaScriptUtils.escapeQuotes(
							messageModel.getString()).toString();
					// On supprime les \n, car ils sont refusés à l'exécution.
					message = message.replace("\n", " ");

					target.appendJavaScript("alert('" + message + "');");

				} catch (Exception e) {
					logger.warn("Impossible de présenter l'usage des ressources du serveur :"
							+ e);
				}
			}
		};
		add(link);
	}

}
