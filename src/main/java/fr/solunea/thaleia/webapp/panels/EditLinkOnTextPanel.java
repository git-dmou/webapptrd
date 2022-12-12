package fr.solunea.thaleia.webapp.panels;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.extensions.ajax.markup.html.IndicatingAjaxLink;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;

@SuppressWarnings("serial")
public abstract class EditLinkOnTextPanel<T> extends Panel {

    /**
     * Présente un libellé avec un lien d'édition.
     *
     * @param editedObjectModel un modèle sur l'objet à éditer, de classe T.
     * @param displayModel      le modèle du texte à présenter
     */
    public EditLinkOnTextPanel(String id, final IModel<T> editedObjectModel, IModel<?> displayModel) {
        super(id, editedObjectModel);

        add(new IndicatingAjaxLink<T>("edit", editedObjectModel) {

            @Override
            protected void onInitialize() {
                super.onInitialize();
                onItemLinkInitialize(this);
            }

            @Override
            public void onClick(AjaxRequestTarget target) {
                EditLinkOnTextPanel.this.onClick(editedObjectModel, target);
            }
        }.add(new Label("label", displayModel)));
    }

    /**
     * L'appel à traiter suite au clic sur le lien
     */
    protected abstract void onClick(IModel<T> contentModel, AjaxRequestTarget target);

    /**
     * Initialisation du lien de l'item.
     */
    protected abstract void onItemLinkInitialize(AjaxLink<T> link);

}