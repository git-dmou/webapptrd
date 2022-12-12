package fr.solunea.thaleia.webapp.utils;

import org.apache.wicket.extensions.markup.html.repeater.data.table.*;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.OddEvenItem;
import org.apache.wicket.model.IModel;

import java.util.List;

@SuppressWarnings("serial")
public class BootstrapDataTable<T, S> extends DataTable<T, S> {

    protected BootstrapDataTable(String id, List<? extends IColumn<T, S>> columns, ISortableDataProvider<T, S>
			dataProvider, int rowsPerPage) {
        super(id, columns, dataProvider, rowsPerPage);

        addTopToolbar(new NavigationToolbar(this));
        addTopToolbar(new HeadersToolbar<>(this, dataProvider));
        addBottomToolbar(new NoRecordsToolbarVisibleEmpty(this));
    }

    @Override
    protected Item<T> newRowItem(final String id, final int index, final IModel<T> model) {
        return new OddEvenItem<>(id, index, model);
    }

}
