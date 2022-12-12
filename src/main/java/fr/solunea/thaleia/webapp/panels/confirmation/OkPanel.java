package fr.solunea.thaleia.webapp.panels.confirmation;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.basic.MultiLineLabel;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

@SuppressWarnings("serial")
public class OkPanel extends Panel {

	public OkPanel(String id, IModel<String> message,
			final ModalWindow modalWindow) {
		super(id);

		Form<Void> form = new Form<Void>("form");

		MultiLineLabel messageLabel = new MultiLineLabel("message",
				message.getObject());
		form.add(messageLabel);
		modalWindow.setInitialHeight(200);
		modalWindow.setInitialWidth(350);

		AjaxButton okButton = new AjaxButton("okButton", Model.of("Ok"), form) {
			@Override
			protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
				if (target != null) {
					modalWindow.close(target);
				}
			}
		};

		form.add(okButton);

		add(form);
	}

}
