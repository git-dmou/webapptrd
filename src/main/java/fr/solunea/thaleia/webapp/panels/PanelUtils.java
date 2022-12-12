package fr.solunea.thaleia.webapp.panels;

import java.lang.reflect.Constructor;
import java.util.List;

import org.apache.cayenne.BaseDataObject;
import org.apache.cayenne.CayenneDataObject;
import org.apache.log4j.Logger;
import org.apache.wicket.Page;
import org.apache.wicket.RuntimeConfigurationType;
import org.apache.wicket.Session;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import fr.solunea.thaleia.model.dao.CayenneDao;
import fr.solunea.thaleia.model.dao.ICayenneDao;
import fr.solunea.thaleia.webapp.ThaleiaApplication;
import fr.solunea.thaleia.webapp.security.ThaleiaSession;

public abstract class PanelUtils {

	private static final Logger logger = Logger.getLogger(PanelUtils.class);

	/**
	 * Stocke en session le message d'erreur, avec le détail de l'erreur si on
	 * est pas en production.
	 */
	public static void errorMessageInSession(String message, String detail) {
		if (ThaleiaApplication.get().getConfigurationType() == RuntimeConfigurationType.DEPLOYMENT) {
			Session.get().error(message);
		} else {
			Session.get().error(message + detail);
		}
	}

	/**
	 * Affiche le message d'erreur dans le panneau de feedback, avec le détail
	 * de l'erreur si on est pas en production.
	 */
	public static void showErrorMessage(String message, String detail,
										ThaleiaFeedbackPanel feedbackPanel) {
		if (ThaleiaApplication.get().getConfigurationType() == RuntimeConfigurationType.DEPLOYMENT) {
			feedbackPanel.error(message);
		} else {
			feedbackPanel.error(message + detail);
		}
	}

	/**
	 * @return un sélecteur d'objets de la classe T, qui est la classe des
	 *         objets traités par le dao passé en paramètre. Présente tous les
	 *         objets que peut renvoyer ce DAO.
	 */
	@SuppressWarnings("serial")
	public static <T extends BaseDataObject> DropDownChoice<T> getDropDownChoice(
			String id, final ICayenneDao<T> dao) {
		return new DropDownChoice<>(id,
				new LoadableDetachableModel<List<T>>() {
					@Override
					protected List<T> load() {
						return dao.find();
					}
				}, new ChoiceRenderer<>() {
			@Override
			public Object getDisplayValue(T object) {
				return dao.getDisplayName(object, ThaleiaSession.get()
						.getLocale());
			}

			@Override
			public String getIdValue(T object, int index) {
				return Integer.toString(dao.getPK(object));
			}
		});
	}

	/**
	 *            modèle de l'objet sléectionné
	 * @return un sélecteur d'objets de la classe T, qui est la classe des
	 *         objets traités par le dao passé en paramètre. Présente tous les
	 * 	       objets que peut renvoyer ce DAO.
	 */
	@SuppressWarnings("serial")
	public static <T extends BaseDataObject> DropDownChoice<T> getDropDownChoice(
			String id, IModel<T> model, final ICayenneDao<T> dao) {
		return new DropDownChoice<>(id, model,
				new LoadableDetachableModel<List<T>>() {
					@Override
					protected List<T> load() {
						return dao.find();
					}
				}, new ChoiceRenderer<>() {
			@Override
			public Object getDisplayValue(T object) {
				return dao.getDisplayName(object, ThaleiaSession.get()
						.getLocale());
			}

			@Override
			public String getIdValue(T object, int index) {
				return Integer.toString(dao.getPK(object));
			}
		});
	}

	/**
	 * @param item
	 *            l'item
	 * @param dao
	 *            le DAO qui gère les items
	 * @param objectIdParamName
	 *            pour objectEditPageClass, le nom du paramètre qui contient
	 *            l'id de l'item à éditer
	 * @param objectEditPageClass
	 *            La page d'édition de cette classe d'item
	 * @return un lien d'édition pour cet item.
	 */
	public static <T extends BaseDataObject> Link<T> getEditLink(
			final ListItem<T> item, final CayenneDao<T> dao,
			final String objectIdParamName,
			final Class<? extends WebPage> objectEditPageClass) {

		@SuppressWarnings("serial")
		Link<T> link = new Link<>("edit", item.getModel()) {

			public void onClick() {
				int objectID = dao.getPK(item.getModelObject());
				PageParameters params = new PageParameters();
				params.set(objectIdParamName, objectID);
				Constructor<? extends Page> constructor = null;
				try {
					constructor = objectEditPageClass
							.getConstructor(PageParameters.class);
				} catch (Exception e) {
					logger.error("Impossible d'obtenir un constructeur pour la classe '"
							+ objectEditPageClass.getName()
							+ "' :"
							+ e.toString());
				}
				try {
					setResponsePage(constructor.newInstance(params));
				} catch (Exception e) {
					logger.error("Impossible d'instancier la classe '"
							+ objectEditPageClass.getName() + "' :"
							+ e.toString());
				}
			}
		};
		return link;
	}

}
