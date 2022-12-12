package fr.solunea.thaleia.webapp.panels.confirmation;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.basic.MultiLineLabel;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.StringResourceModel;

@SuppressWarnings("serial")
public class YesNoPanel extends Panel {

	public YesNoPanel(String id, IModel<String> message,
			final ModalWindow modalWindow, final ConfirmationAnswer answer) {
		super(id);

		Form<Void> yesNoForm = new Form<Void>("yesNoForm");

		MultiLineLabel messageLabel = new MultiLineLabel("message",
				message.getObject());
		yesNoForm.add(messageLabel);
		modalWindow.setInitialHeight(200);
		modalWindow.setInitialWidth(350);

		AjaxButton yesButton = new AjaxButton("yesButton",
				new StringResourceModel("yes", this, null), yesNoForm) {

			@Override
			protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
				if (target != null) {
					answer.setAnswer(true);
					modalWindow.close(target);
				}
			}
		};

		AjaxButton noButton = new AjaxButton("noButton",
				new StringResourceModel("no", this, null), yesNoForm) {

			@Override
			protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
				if (target != null) {
					answer.setAnswer(false);
					modalWindow.close(target);
				}
			}
		};

		yesNoForm.add(yesButton);
		yesNoForm.add(noButton);

		add(yesNoForm);
	}

}
