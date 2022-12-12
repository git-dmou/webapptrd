package fr.solunea.thaleia.webapp.pages;

import fr.solunea.thaleia.service.utils.Configuration;
import fr.solunea.thaleia.utils.ApplicationEvent;
import fr.solunea.thaleia.webapp.Analytics;
import fr.solunea.thaleia.webapp.ThaleiaApplication;
import fr.solunea.thaleia.webapp.panels.PublicPanelHeader;
import fr.solunea.thaleia.webapp.security.ThaleiaSession;
import org.apache.log4j.Logger;
import org.apache.wicket.Page;
import org.apache.wicket.Session;
import org.apache.wicket.request.component.IRequestablePage;
import org.apache.wicket.request.flow.RedirectToUrlException;
import org.apache.wicket.request.mapper.parameter.PageParameters;

@SuppressWarnings("serial")
public class LoginPage extends ThaleiaPageV6 {

    protected static final Logger logger = Logger.getLogger(LoginPage.class.getName());
    public static final String REDIRECTION_PARAMETER = "redirect";

    /**
     * @param destination l'URL de destination en cas de réussite de l'identification.
     */
    public LoginPage(final String destination) {
        Session.get().setLocale(Session.get().getLocale());
        addHeader();
        loginAndRedirect(destination);
    }

    public LoginPage() {
        Session.get().setLocale(Session.get().getLocale());
        addHeader();
        logIn();
    }

    public LoginPage(PageParameters parameters) {
        addHeader();
        Session.get().setLocale(Session.get().getLocale());
        if (parameters.get(REDIRECTION_PARAMETER) != null && !parameters.get(REDIRECTION_PARAMETER).isEmpty()) {
            loginAndRedirect(parameters.get(REDIRECTION_PARAMETER).toString());
        } else {
            logIn();
        }
    }

    public LoginPage(Page destination) {
        Session.get().setLocale(Session.get().getLocale());
        Analytics.getImplementation().logEvent(ApplicationEvent.LoginPageAccess);

        redirectIfLoggedIn(destination);

        addHeader();
        add(new ThaleiaSignInPanel("signInPanel") {
            @Override
            protected void onSignInSucceeded() {
                addWelcomeMessage();

                // On redirige sur la destination demandée
                setResponsePage(destination);
            }
        });
    }

    private void addHeader() {
        add(new PublicPanelHeader("publicPanelHeader"));
    }

    private void loginAndRedirect(String destination) {
        Analytics.getImplementation().logEvent(ApplicationEvent.LoginPageAccess);

        redirectIfLoggedIn(destination);

        add(new ThaleiaSignInPanel("signInPanel") {
            @Override
            protected void onSignInSucceeded() {
                addWelcomeMessage();

                // On redirige sur la destination demandée
                throw new RedirectToUrlException(destination);
            }
        });
    }

    private void logIn() {
        Analytics.getImplementation().logEvent(ApplicationEvent.LoginPageAccess);

        redirectIfLoggedIn();

        add(new ThaleiaSignInPanel("signInPanel") {
            @Override
            protected void onSignInSucceeded() {
                addWelcomeMessage();

                // On redirige sur la page par défaut de l'application
                setResponsePage(ThaleiaApplication.get().getRedirectionPage(Configuration.AUTHENTIFIED_USERS_WELCOME_PAGE,
                        Configuration.DEFAULT_AUTHENTIFIED_USERS_WELCOME_PAGE, Configuration.HOME_MOUNT_POINT));
            }
        });
    }

    /**
     * Si l'utilisateur est identifié, redirige vers la page d'accueil demandée.
     *
     * @param destination La page vers laquelle sera faite la redirection.
     */
    private void redirectIfLoggedIn(Page destination) {
        if (ThaleiaSession.get().isSignedIn()) {
            addWelcomeMessage();
            setResponsePage(destination);
        }
    }

    /**
     * Si l'utilisateur est identifié, redirige vers la page d'acceuil demandée.
     *
     * @param destination le nom de la classe de la page vers laquelle sera faite la
     *                    redirection.
     */
    private void redirectIfLoggedIn(String destination) {
        if (ThaleiaSession.get().isSignedIn()) {
            addWelcomeMessage();

            ClassLoader classLoader =
                    ThaleiaApplication.get().getApplicationSettings().getClassResolver().getClassLoader();

            try {
                @SuppressWarnings("unchecked") Class<? extends IRequestablePage> direction = (Class<?
                        extends IRequestablePage>) classLoader.loadClass(destination);
                setResponsePage(direction);

            } catch (ClassNotFoundException e) {
                logger.warn("Impossible de rediriger vers " + destination + " : " + e);
                redirectIfLoggedIn();
            }

        }
    }

    /**
     * Si l'utilisateur est identifié, redirige vers la page d'acceuil des
     * utilisateurs identifiés.
     */
    public void redirectIfLoggedIn() {
        redirectIfLoggedIn(ThaleiaApplication.get().getConfiguration().getDatabaseParameterValue(
                Configuration.AUTHENTIFIED_USERS_WELCOME_PAGE, Configuration.DEFAULT_AUTHENTIFIED_USERS_WELCOME_PAGE));
    }

    /**
     * Méthode appelée appelée après une identification réussie. Par exemple
     * pour ajouter un message d'accueil dans les messages de session.
     */
    protected void addWelcomeMessage() {
        // Par défaut, rien.
    }

}
