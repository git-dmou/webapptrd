package fr.solunea.thaleia.webapp.utils;

import org.apache.log4j.Logger;
import org.apache.wicket.ajax.IAjaxIndicatorAware;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.extensions.ajax.markup.html.AjaxIndicatorAppender;
import org.apache.wicket.markup.html.form.Form;

@SuppressWarnings("serial")
public class IndicatingAjaxSubmitLink extends AjaxSubmitLink implements IAjaxIndicatorAware {

	private final AjaxIndicatorAppender indicatorAppender = new AjaxIndicatorAppender();

	public static final Logger logger = Logger.getLogger(IndicatingAjaxSubmitLink.class);

	public IndicatingAjaxSubmitLink(String id, Form<?> form) {
		super(id, form);
		add(indicatorAppender);
	}

	/**
	 * @see org.apache.wicket.ajax.IAjaxIndicatorAware#getAjaxIndicatorMarkupId()
	 */
	@Override
	public String getAjaxIndicatorMarkupId() {
		return indicatorAppender.getMarkupId();
	}

}
