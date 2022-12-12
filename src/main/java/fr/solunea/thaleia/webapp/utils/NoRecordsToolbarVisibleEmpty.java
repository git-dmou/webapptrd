package fr.solunea.thaleia.webapp.utils;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.extensions.markup.html.repeater.data.table.DataTable;
import org.apache.wicket.extensions.markup.html.repeater.data.table.NoRecordsToolbar;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.ResourceModel;

@SuppressWarnings("serial")
public class NoRecordsToolbarVisibleEmpty extends NoRecordsToolbar {

	private static final IModel<String> DEFAULT_MESSAGE_MODEL = new ResourceModel(
			"datatable.no-records-found");

	public NoRecordsToolbarVisibleEmpty(DataTable<?, ?> table) {
		this(table, DEFAULT_MESSAGE_MODEL);
	}

	public NoRecordsToolbarVisibleEmpty(final DataTable<?, ?> table,
			IModel<String> messageModel) {
		super(table, messageModel);

		removeAll();

		WebMarkupContainer td = new WebMarkupContainer("td");
		add(td);

		td.add(AttributeModifier.replace("colspan",
				new AbstractReadOnlyModel<String>() {
					private static final long serialVersionUID = 1L;

					@Override
					public String getObject() {
						return String.valueOf(table.getColumns().size());
					}
				}));
		if (table.getRowCount() == 0) {
			td.add(new Label("msg", messageModel));
		} else {
			td.add(new Label("msg", ""));
		}
	}

	@Override
	public boolean isVisible() {
		return true;
	}

}
