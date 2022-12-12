package fr.solunea.thaleia.webapp.panels;

import org.apache.wicket.markup.html.panel.Panel;

public class PublicPanelHeader extends Panel  {

    public PublicPanelHeader(String id) {
        super(id);

        add(new PublicPageLocaleSelectorPanel("localeSelectorPanel"));
    }
}
