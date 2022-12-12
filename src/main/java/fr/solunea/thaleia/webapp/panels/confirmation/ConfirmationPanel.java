package fr.solunea.thaleia.webapp.panels.confirmation;

import org.apache.log4j.Logger;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;

@SuppressWarnings("serial")
public abstract class ConfirmationPanel extends Panel {

	protected static final Logger logger = Logger
			.getLogger(ConfirmationPanel.class);

	/**
	 * La fenêtre modale qui va présenter la demande de confirmation : texte +
	 * boutons Oui et Non.
	 */
	protected ModalWindow confirmModal;
	/**
	 * La réponse à la demande de confirmation.
	 */
	protected ConfirmationAnswer answer;

	/**
	 * @param id
	 * @param buttonLabel
	 *            le label du bouton qui va déclencher l'ouverture de la fenêtre
	 *            modale de demande de confirmation.
	 * @param modalMessageText
	 *            le message de confirmation
	 */
	public ConfirmationPanel(final String id, IModel<String> buttonLabel,
			final IModel<String> modalMessageText) {
		super(id);
		answer = new ConfirmationAnswer(false);

		// Fenêtre modale vide pour l'instant
		confirmModal = createConfirmModal(id);

		// Le bouton qui peut ouvrir la fenêtre modale de demande de
		// confirmation.
		AjaxLink<Void> confirmButton = new AjaxLink<Void>("confirmButton") {
			@Override
			public void onClick(AjaxRequestTarget target) {
				logger.debug("Clic sur le bouton de demande de confirmation. Target = "
						+ target);

				// On désactive le message indiquant que la fenêtre tente de
				// fermer la page
				target.prependJavaScript("Wicket.Window.unloadConfirmation = false;");

				// On met à jour le texte de la fenêtre modale, afin de
				// s'assurer que son texte est obtenu d'après son modèle au
				// moment où on clique.
				confirmModal
						.setContent(new YesNoPanel(confirmModal.getContentId(),
								modalMessageText, confirmModal, answer));

				// Présentation
				confirmModal.show(target);
			}
		};

		// On modifie la CSS de ce bouton pour accentuer la mise en forme de
		// l'état d'activation.
		// Si on ne le fait pas, le texte du bouton est bien en italique, mais
		// le bouton garde l'aspect d'un bouton actif.
		if (isEnabled()) {
			confirmButton.add(new AttributeModifier("class",
					"btn enabled pull-right"));
		} else {
			confirmButton.add(new AttributeModifier("class",
					"btn disabled pull-right"));
		}

		add(confirmButton.add(new Label("label", buttonLabel)));
		add(confirmModal);
	}

	/**
	 * Méthode appelée si on sélectionner "Oui".
	 * 
	 * @param target
	 */
	protected abstract void onConfirm(AjaxRequestTarget target);

	/**
	 * Méthode appelée si on sélectionner "Non".
	 * 
	 * @param target
	 */
	protected abstract void onCancel(AjaxRequestTarget target);

	protected ModalWindow createConfirmModal(String cookieName) {

		ModalWindow modalWindow = new ModalWindow("modal");
		// On ne conserve pas la position et la taille entre deux ouvertures :
		modalWindow.setCookieName(null);
		modalWindow.setCssClassName(ModalWindow.CSS_CLASS_GRAY);
		modalWindow.setOutputMarkupId(true);
		modalWindow
				.setWindowClosedCallback(new ModalWindow.WindowClosedCallback() {

					@Override
					public void onClose(AjaxRequestTarget target) {
						if (answer.isAnswer()) {
							onConfirm(target);
						} else {
							onCancel(target);
						}
					}
				});

		return modalWindow;
	}

}
