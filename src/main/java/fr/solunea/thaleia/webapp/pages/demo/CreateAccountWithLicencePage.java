package fr.solunea.thaleia.webapp.pages.demo;

import fr.solunea.thaleia.model.AccountRequest;
import fr.solunea.thaleia.model.Licence;
import fr.solunea.thaleia.utils.ApplicationEvent;
import fr.solunea.thaleia.webapp.Analytics;
import fr.solunea.thaleia.webapp.ThaleiaApplication;
import fr.solunea.thaleia.webapp.pages.BasePage;
import fr.solunea.thaleia.webapp.pages.ThaleiaPageV6;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

@SuppressWarnings("serial")
public class CreateAccountWithLicencePage extends ThaleiaPageV6 {

    public CreateAccountWithLicencePage(Licence licence) {
        super();
        initialize("", licence);
    }

    public CreateAccountWithLicencePage(String email, Licence licence) {
        super();
        initialize(email, licence);
    }

    private void initialize(String email, Licence licence) {
        // On fabrique une nouvelle requête de création de compte
        AccountRequest accountRequest = new AccountRequest();
        accountRequest.setMail(email);
        IModel<AccountRequest> accountRequestModel = Model.of(accountRequest);

        Analytics.getImplementation().logEvent(ApplicationEvent.AccountCreationStart);

        // Le panneau d'édition
        add(new CreateAccountWithLicencePanel("createAccountPanel", accountRequestModel, Model.of(licence)) {

            @Override
            protected void onOut() {
                CreateAccountWithLicencePage.this.onOut();
            }

            // Magouille pas propre. On devrait appeler une méthode de
            // sortie au niveau de la classe (qui serait abstraite),
            // surdéfinie au moment de l'instanciation de la page. Mais cela
            // fonctionne mal (perte des CSS)

            // Donc, si admin, on envoie sur la page d'admin, sinon sur la
            // page d'accueil.
        });
    }

    protected void onOut() {
        setResponsePage(ThaleiaApplication.get().getHomePage());
    }
}
