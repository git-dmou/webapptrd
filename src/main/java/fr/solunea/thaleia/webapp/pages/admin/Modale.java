package fr.solunea.thaleia.webapp.pages.admin;

import org.apache.wicket.Component;
import org.apache.wicket.WicketRuntimeException;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.AbstractRepeater;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.util.string.AppendingStringBuffer;


public class Modale extends Panel {

    public static final String CONTENT_ID = "content";
    private final IModel<String> title = Model.of("");
    private boolean shown = false;

    public Modale(String id) {
        super(id);

        WebMarkupContainer empty = new WebMarkupContainer(getContentId());
        add(empty.setOutputMarkupId(true));

        add(new Label("title", title).setOutputMarkupId(true));

        setOutputMarkupId(true);
    }

    public String getContentId() {
        return CONTENT_ID;
    }

    public void setTitle(String title) {
        this.title.setObject(title);
    }

    public void show(final AjaxRequestTarget target) {
        if (!shown) {
            getContent().setVisible(true);
            target.add(this);
            target.appendJavaScript(getWindowOpenJavaScript());
            shown = true;
        }
    }

    private String getWindowOpenJavaScript() {
        AppendingStringBuffer buffer = new AppendingStringBuffer();
        buffer.append("$('#modal').modal('show')");
        return buffer.toString();
    }

    @Override
    protected void onBeforeRender() {
        shown = makeContentVisible();

        getContent().setOutputMarkupId(true);
        getContent().setVisible(shown);

        super.onBeforeRender();
    }

    protected boolean makeContentVisible() {
        // if user is refreshing whole page, the window will not be shown
        if (!getWebRequest().isAjax()) {
            return false;
        } else {
            return shown;
        }
    }

    protected final Component getContent() {
        return get(getContentId());
    }

    public Modale setContent(final Component component) {
        if (!component.getId().equals(getContentId())) {
            throw new WicketRuntimeException("Modal window content id is wrong. Component ID:" +
                    component.getId() + "; content ID: " + getContentId());
        } else if (component instanceof AbstractRepeater) {
            throw new WicketRuntimeException(
                    "A repeater component cannot be used as the content of a modal window, please use repeater's parent");
        }

        component.setOutputMarkupPlaceholderTag(true);
        component.setVisible(false);
        replace(component);
        return this;
    }

}
