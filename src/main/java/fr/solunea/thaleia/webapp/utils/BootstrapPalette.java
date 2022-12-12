package fr.solunea.thaleia.webapp.utils;

import java.util.Collection;
import java.util.List;

import org.apache.wicket.extensions.markup.html.form.palette.Palette;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.resource.ResourceReference;
import org.apache.wicket.resource.JQueryPluginResourceReference;

@SuppressWarnings("serial")
public class BootstrapPalette<T> extends Palette<T> {

	private static final ResourceReference JAVASCRIPT = new JQueryPluginResourceReference(
			Palette.class, "palette.js");

	public BootstrapPalette(String id,
			IModel<? extends Collection<? extends T>> choicesModel,
			IChoiceRenderer<T> choiceRenderer, int rows, boolean allowOrder) {
		super(id, choicesModel, choiceRenderer, rows, allowOrder);
	}

	public BootstrapPalette(String id,
			IModel<? extends List<? extends T>> model,
			IModel<? extends Collection<? extends T>> choicesModel,
			IChoiceRenderer<T> choiceRenderer, int rows, boolean allowOrder,
			boolean allowMoveAll) {
		super(id, model, choicesModel, choiceRenderer, rows, allowOrder,
				allowMoveAll);
	}

	public BootstrapPalette(String id,
			IModel<? extends List<? extends T>> model,
			IModel<? extends Collection<? extends T>> choicesModel,
			IChoiceRenderer<T> choiceRenderer, int rows, boolean allowOrder) {
		super(id, model, choicesModel, choiceRenderer, rows, allowOrder);
	}

	@Override
	public void renderHead(final IHeaderResponse response) {
		response.render(JavaScriptHeaderItem.forReference(JAVASCRIPT));
	}
}
