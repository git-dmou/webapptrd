package fr.solunea.thaleia.webapp.pages;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.Model;

/**
 * Panel de fond pour la page login avec une image de background personnalisée.
 */
public class ThaleiaSignInPanelBackgroundCustomImg extends Panel {

    public ThaleiaSignInPanelBackgroundCustomImg(String id) {
        super(id);
    }

    /**
     * Constructeur du panel de background personnalisé pour la page de login.
     * @param id identifiant du panel.
     * @param base64 base64 de l'image
     * @param style css (optionnel).
     * @param clazz classes css (optionnel).
     */
    public ThaleiaSignInPanelBackgroundCustomImg(String id, String base64, String style, String clazz) {
        super(id);

        // Traitement de l'image
        Image img = new Image("img", "");
        img.add(new AttributeModifier("src", base64));

        if(style.trim().length() > 0) {
            img.add(new AttributeAppender("style", new Model(style), " "));
        }

        if(clazz.trim().length() > 0) {
            img.add(new AttributeAppender("class", new Model(clazz), " "));
        }

        add(img);
    }
}
