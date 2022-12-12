package fr.solunea.thaleia.webapp.panels.confirmation;

import org.apache.wicket.ajax.attributes.AjaxCallListener;
import org.apache.wicket.ajax.attributes.AjaxRequestAttributes;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.core.util.string.JavaScriptUtils;
import org.apache.wicket.model.IModel;

/**
 * Ce lien ouvre une pop-up de confirmation, avec ce texte. La méthode onClick
 * du lien ne sera exécutée que si on clique Ok dans la popup (et non Annuler).
 */
@SuppressWarnings("serial")
public abstract class ConfirmationLink<T> extends AjaxLink<T> {

    private final IModel<String> text;

    /**
     * @param text le message de confirmation
     */
    public ConfirmationLink(String id, IModel<T> model, IModel<String> text) {
        super(id, model);
        this.text = text;
    }

    /**
     * @param text le message de confirmation
     */
    public ConfirmationLink(String id, IModel<String> text) {
        super(id);
        // On s'assure d'échapper la chaîne de caractère, pour éviter qu'elle ne
        // perturbe le javascript, par exemple avec des '.
        this.text = text;
    }

    @Override
    protected void updateAjaxAttributes(AjaxRequestAttributes attributes) {
        super.updateAjaxAttributes(attributes);

        AjaxCallListener ajaxCallListener = new AjaxCallListener();
        // On s'assure d'échapper la chaîne de caractère, pour éviter qu'elle ne
        // perturbe le javascript, par exemple avec des '.
        ajaxCallListener.onPrecondition(
                "return confirm('" + JavaScriptUtils.escapeQuotes(text.getObject()).toString() + "');");
        attributes.getAjaxCallListeners().add(ajaxCallListener);
    }
}