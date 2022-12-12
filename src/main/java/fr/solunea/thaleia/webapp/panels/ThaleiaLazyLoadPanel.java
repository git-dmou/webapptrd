package fr.solunea.thaleia.webapp.panels;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AbstractDefaultAjaxBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.attributes.AjaxRequestAttributes;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;

/**
 * Panneau de lazyloading : il faut définir le panneau
 *
 */
@SuppressWarnings("serial")
public abstract class ThaleiaLazyLoadPanel extends Panel {

	/**
	 * The component id which will be used to load the lazily loaded component.
	 */
	public static final String LAZY_LOAD_COMPONENT_ID = "content";

	// state,
	// 0:add loading component
	// 1:loading component added, waiting for ajax replace
	// 2:ajax replacement completed
	protected byte state = 0;

	/**
	 * Constructor
	 * 
	 * @param id
	 */
	public ThaleiaLazyLoadPanel(final String id) {
		this(id, null);
	}

	/**
	 * Constructor
	 * 
	 * @param id
	 * @param model
	 */
	public ThaleiaLazyLoadPanel(final String id, final IModel<?> model) {
		super(id, model);

		setOutputMarkupId(true);

		add(new AbstractDefaultAjaxBehavior() {
			private static final long serialVersionUID = 1L;

			@Override
			protected void respond(final AjaxRequestTarget target) {
				if (state < 2) {
					Component component = getLazyLoadComponent(LAZY_LOAD_COMPONENT_ID);
					ThaleiaLazyLoadPanel.this.replace(component);
					setState((byte) 2);
					ThaleiaLazyLoadPanel.this.onComponentLoaded(component,
							target);
				}
				target.add(ThaleiaLazyLoadPanel.this);

			}

			@Override
			protected void updateAjaxAttributes(AjaxRequestAttributes attributes) {
				super.updateAjaxAttributes(attributes);
				ThaleiaLazyLoadPanel.this.updateAjaxAttributes(attributes);
			}

			@Override
			public void renderHead(final Component component,
					final IHeaderResponse response) {
				super.renderHead(component, response);
				if (state < 2) {
					CharSequence js = getCallbackScript(component);
					handleCallbackScript(response, js, component);
				}
			}
		});
	}

	// TODO
	public ThaleiaLazyLoadPanel(final String id, final IModel<?> model, String addCSSClass) {
		super(id, model);

		setOutputMarkupId(true);

		add(new AbstractDefaultAjaxBehavior() {
			private static final long serialVersionUID = 1L;

			@Override
			protected void respond(final AjaxRequestTarget target) {
				if (state < 2) {
					Component component = getLazyLoadComponent(LAZY_LOAD_COMPONENT_ID);
					ThaleiaLazyLoadPanel.this.replace(component);
					setState((byte) 2);
					ThaleiaLazyLoadPanel.this.onComponentLoaded(component,
							target);
				}
				target.add(ThaleiaLazyLoadPanel.this);

			}

			@Override
			protected void updateAjaxAttributes(AjaxRequestAttributes attributes) {
				super.updateAjaxAttributes(attributes);
				ThaleiaLazyLoadPanel.this.updateAjaxAttributes(attributes);
			}

			@Override
			public void renderHead(final Component component,
								   final IHeaderResponse response) {
				super.renderHead(component, response);
				if (state < 2) {
					CharSequence js = getCallbackScript(component);
					handleCallbackScript(response, js, component);
				}
			}
		});
	}



	protected void updateAjaxAttributes(AjaxRequestAttributes attributes) {
	}

	/**
	 * Allows subclasses to change the callback script if needed.
	 * 
	 * @param response
	 *            the current response that writes to the header
	 * @param callbackScript
	 *            the JavaScript to write in the header
	 * @param component
	 *            the component which produced the callback script
	 */
	protected void handleCallbackScript(final IHeaderResponse response,
			final CharSequence callbackScript, final Component component) {
		response.render(OnDomReadyHeaderItem.forScript(callbackScript));
	}

	/**
	 * @see org.apache.wicket.Component#onBeforeRender()
	 */
	@Override
	protected void onBeforeRender() {
		if (state == 0) {
			add(getLoadingComponent(LAZY_LOAD_COMPONENT_ID));
			setState((byte) 1);
		}
		super.onBeforeRender();
	}

	/**
	 * 
	 * @param state
	 */
	protected void setState(final byte state) {
		this.state = state;
		getPage().dirty();
	}

	/**
	 * 
	 * @param markupId
	 *            The components markupid.
	 * @return The component that must be lazy created. You may call
	 *         setRenderBodyOnly(true) on this component if you need the body
	 *         only.
	 */
	public abstract Component getLazyLoadComponent(String markupId);

	/**
	 * Called when the placeholder component is replaced with the lazy loaded
	 * one.
	 *
	 * @param component
	 *            The lazy loaded component
	 * @param target
	 *            The Ajax request handler
	 */
	protected void onComponentLoaded(Component component,
			AjaxRequestTarget target) {
	}

	/**
	 * @param markupId
	 *            The components markupid.
	 * @return The component to show while the real component is being created.
	 */
	protected abstract Component getLoadingComponent(final String markupId);

}
