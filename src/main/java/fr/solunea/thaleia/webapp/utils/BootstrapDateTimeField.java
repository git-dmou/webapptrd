package fr.solunea.thaleia.webapp.utils;

import java.util.Date;

import org.apache.wicket.extensions.yui.calendar.DateTimeField;
import org.apache.wicket.model.IModel;

@SuppressWarnings("serial")
public class BootstrapDateTimeField extends DateTimeField {

	public BootstrapDateTimeField(String id) {
		super(id);
	}

	public BootstrapDateTimeField(String id, IModel<Date> model) {
		super(id, model);
	}

}
