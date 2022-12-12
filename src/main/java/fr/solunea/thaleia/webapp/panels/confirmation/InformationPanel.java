package fr.solunea.thaleia.webapp.panels.confirmation;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;

@SuppressWarnings("serial")
public abstract class InformationPanel extends Panel {

	/**
	 * La fenêtre modale qui va présenter le texte + bouton fermer.
	 */
	protected ModalWindow modal;

	/**
	 * @param id
	 * @param buttonLabel
	 *            le label du bouton qui va déclencher l'ouverture de la fenêtre
	 *            modale de demande de confirmation.
	 * @param modalMessageText
	 *            le message de confirmation
	 */
	public InformationPanel(final String id, IModel<String> buttonLabel,
			final IModel<String> modalMessageText) {
		super(id);

		// Fenêtre modale vide pour l'instant
		modal = createConfirmModal(id);

		// Le formulaire pour ouvrir la fenêtre modale
		@SuppressWarnings("rawtypes")
		Form<?> form = new Form("form");
		add(form);

		// Le bouton qui peut ouvrir la fenêtre modale
		AjaxButton confirmButton = new AjaxButton("openButton", buttonLabel) {
			@Override
			protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
				// On désactive le message indiquant que la fenêtre tente de
				// fermer la page
				target.prependJavaScript("Wicket.Window.unloadConfirmation = false;");

				// On met à jour le texte de la fenêtre modale, afin de
				// s'assurer que son texte est obtenu d'après son modèle au
				// moment où on clique.
				modal.setContent(new OkPanel(modal.getContentId(),
						modalMessageText, modal));

				// Présentation
				modal.show(target);
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

		form.add(confirmButton);
		form.add(modal);
	}

	/**
	 * Méthode appelée lors de la fermeture de la fenêtre.
	 * 
	 * @param target
	 */
	protected abstract void onExit(AjaxRequestTarget target);

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
						onExit(target);
					}
				});

		return modalWindow;
	}

}
