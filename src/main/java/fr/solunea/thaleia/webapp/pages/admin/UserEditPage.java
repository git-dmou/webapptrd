package fr.solunea.thaleia.webapp.pages.admin;

import fr.solunea.thaleia.model.User;
import fr.solunea.thaleia.webapp.pages.ThaleiaV6MenuPage;
import fr.solunea.thaleia.webapp.panels.ThaleiaFeedbackPanel;
import fr.solunea.thaleia.webapp.security.ThaleiaSession;
import org.apache.wicket.Page;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.authroles.authorization.strategies.role.annotations.AuthorizeInstantiation;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;

@AuthorizeInstantiation("user")
// On permet à un utilisateur de présenter cette page, afin qu'il puisse
// modifier son compte.
public class UserEditPage extends ThaleiaV6MenuPage {

    /**
     * La page appelée en sortie d'édition.
     */
    private final Class<? extends Page> outPageClass;

    /**
     * Panneau d'affichage des feedbacks.
     */
    private ThaleiaFeedbackPanel feedbackPanel;

    public UserEditPage(IModel<User> model, Class<? extends Page> outPageClass) {
        super(model);
        setDefaultModel(model);

        this.outPageClass = outPageClass;

        initPage();
    }

    public UserEditPage() {
        super();
        setDefaultModel(new LoadableDetachableModel<User>() {
            @Override
            protected User load() {
                return ThaleiaSession.get().getAuthenticatedUser();
            }
        });
        this.outPageClass = this.getPageClass();
        initPage();
    }

    @SuppressWarnings("unchecked")
    private void initPage() {
        WebMarkupContainer editPanel = new UserEditPanel("editPanel", (IModel<User>) UserEditPage.this.getDefaultModel()) {
            @Override
            protected void onOut(AjaxRequestTarget target) {
                UserEditPage.this.onOut();
            }
        };
        editPanel.setOutputMarkupId(true);
        add(editPanel);
    }

    /**
     * Redirection appelée à la sortie de l'édition d'un compte utilisateur.
     */
    public void onOut() {
        logger.debug("Sortie vers la page " + outPageClass);
        setResponsePage(outPageClass);
    }

}
