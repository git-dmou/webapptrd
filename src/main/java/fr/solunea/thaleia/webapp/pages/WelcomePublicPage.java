package fr.solunea.thaleia.webapp.pages;

import fr.solunea.thaleia.service.utils.Configuration;
import fr.solunea.thaleia.webapp.ThaleiaApplication;
import fr.solunea.thaleia.webapp.security.ThaleiaSession;
import fr.solunea.thaleia.webapp.utils.Signin;
import org.apache.log4j.Logger;
import org.apache.wicket.Page;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.request.flow.RedirectToUrlException;
import org.apache.wicket.request.resource.PackageResourceReference;

import java.lang.reflect.Constructor;

/**
 * Cette page présente un contenu HTML, et un lien vers la page qui contient le
 * panneau de login.
 */
@SuppressWarnings("serial")
public class WelcomePublicPage extends BasePage {

    protected static final Logger logger = Logger.getLogger(WelcomePublicPage.class.getName());

    /**
     * @param destination l'URL de destination en cas de réussite de l'identification.
     */
    @SuppressWarnings("unused")
    public WelcomePublicPage(final String destination) {
        super("medias/background_landingpage.png");


        add(new Link<Void>("link") {
            @Override
            public void onClick() {
                if (ThaleiaSession.get().isSignedIn()) {
                    // On redirige sur la destination demandée
                    throw new RedirectToUrlException(destination);
                } else {
                    // On passe par le login de l'application
                    Class<? extends Page> loginPageClass = ThaleiaApplication.get().getLoginPage();
                    try {
                        // on instancie la page avec comme paramètre la
                        // redirection après login.
                        Constructor<? extends Page> constructor = loginPageClass.getConstructor(String.class);
                        Object loginPage = constructor.newInstance(destination);
                        setResponsePage((Page) loginPage);

                    } catch (Exception e) {
                        logger.warn(e);
                    }
                }
            }
        });

        initPage();
    }

    public WelcomePublicPage() {
        super("medias/background_landingpage.png");


        // logger.debug(LogUtils.getStackTrace(Thread.currentThread()
        // .getStackTrace()));

        add(new Link<Void>("link") {
            @Override
            public void onClick() {
                if (ThaleiaSession.get().isSignedIn()) {
                    // On redirige sur la page privée par défaut
                    setResponsePage(ThaleiaApplication.get().getRedirectionPage(Configuration
                            .AUTHENTIFIED_USERS_WELCOME_PAGE, Configuration.DEFAULT_AUTHENTIFIED_USERS_WELCOME_PAGE,
                            Configuration.HOME_MOUNT_POINT));

                } else {
                    // On passe par le login de l'application
                    Class<? extends Page> loginPageClass = ThaleiaApplication.get().getLoginPage();
                    try {
                        // on instancie la page d'accueil sans redirection
                        Constructor<? extends Page> constructor = loginPageClass.getConstructor();
                        Object loginPage = constructor.newInstance();
                        setResponsePage((Page) loginPage);

                    } catch (Exception e) {
                        logger.warn(e.getMessage());
                        logger.warn(fr.solunea.thaleia.utils.LogUtils.getStackTrace(e.getStackTrace()));
                    }
                }
            }
        });

        initPage();
    }

    private void initPage() {
        add(new Image("logo", new PackageResourceReference(getClass(), "logo_thaleia_landingpage.png")));

        Signin.autoSignin(getApplication());
    }
}
