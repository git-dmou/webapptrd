package fr.solunea.thaleia.webapp.pages.demo;

import fr.solunea.thaleia.model.AccountRequest;
import fr.solunea.thaleia.webapp.pages.ThaleiaPageV6;
import org.apache.log4j.Logger;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptReferenceHeaderItem;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.resource.JavaScriptResourceReference;
import org.apache.wicket.request.resource.PackageResourceReference;

@SuppressWarnings("serial")
public abstract class ConfirmationPage extends ThaleiaPageV6 {

    protected static final Logger logger = Logger.getLogger(ConfirmationPage.class);

    @SuppressWarnings("unused")
    public ConfirmationPage() {
        super();
        logger.debug("Appel de la page sans paramètre.");
    }


    public ConfirmationPage(IModel<AccountRequest> model) {
        super(model);

        add(new Image("panelIcon", new PackageResourceReference(getClass(), "../img/icon_user.png")));

        add(new Label("mailAddress", model.getObject().getMail()));

    }

    abstract protected void onOut();

    @Override
    public void renderHead(IHeaderResponse response) {
        // On ajoute en haut de la balise head le code Google de traçage des conversions
        response.render(JavaScriptReferenceHeaderItem.forReference(new JavaScriptResourceReference(FinalizeAccountPage.class,
                "gtm.js")));

        super.renderHead(response);
    }
}
