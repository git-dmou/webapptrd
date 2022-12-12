package fr.solunea.thaleia.webapp.pages;

import fr.solunea.thaleia.model.User;
import fr.solunea.thaleia.service.utils.Configuration;
import fr.solunea.thaleia.utils.DetailedException;
import fr.solunea.thaleia.utils.LogUtils;
import fr.solunea.thaleia.webapp.ThaleiaApplication;
import fr.solunea.thaleia.webapp.pages.admin.UserEditPage;
import fr.solunea.thaleia.webapp.panels.LocaleSelectorPanel;
import fr.solunea.thaleia.webapp.panels.MenuPanel;
import fr.solunea.thaleia.webapp.security.ThaleiaSession;
import org.apache.log4j.Logger;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.authorization.Action;
import org.apache.wicket.authroles.authorization.strategies.role.annotations.AuthorizeAction;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.request.component.IRequestablePage;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.request.resource.PackageResourceReference;

import java.util.ArrayList;
import java.util.Locale;
import java.util.Objects;

public class BasePage extends ThaleiaPage {

    protected static final Logger logger = Logger.getLogger(BasePage.class);

    public BasePage(IModel<?> model) {
        super(model);
        initPage(true);
    }

    public BasePage(PageParameters parameters) {
        super(parameters);
        initPage(true);
    }

    public BasePage() {
        super();
        initPage(true);
    }

    /**
     * @param showMenuActions doit-on présenter les actions dans le menu (liens du menu,
     *                        accès au compte...)
     */
    public BasePage(boolean showMenuActions) {
        super();
        initPage(showMenuActions);
    }

    /**
     * Ajoute l'image en fond.
     *
     * @param backgroundImage l'URL relative de l'image dans l'application web.
     */
    public BasePage(final String backgroundImage) {
        super();
        initPage(true);

        // On fixe l'image de fond
        AjaxEventBehavior event = new AjaxEventBehavior("onload") {
            @Override
            protected void onEvent(final AjaxRequestTarget target) {
                target.appendJavaScript("document.getElementById(\"wrap\").style.background = \"url('" + backgroundImage
                        + "') no-repeat center center fixed\";");
                target.appendJavaScript(
                        "document.getElementById(\"wrap\").style['background'+'-'+'size'] = " + "\"cover\";");
            }
        };
        add(event);
    }

    /**
     * Effectue une redirection vers la page destination. N'effectue aucune
     * redirection si l'action est impossible. Les classes disponibles pour la
     * redirection sont cherchées dans le classloader de l'application, y
     * compris les jars des plugins.
     *
     * @param destination le nom de la classe de la page qui doit être ouverte. Doit
     *                    hériter de IRequestablePage.
     */
    @SuppressWarnings("unchecked")
    public void setResponsePage(String destination) {
        try {
            ClassLoader classLoader =
                    ThaleiaApplication.get().getApplicationSettings().getClassResolver().getClassLoader();

            Class<?> clazz = classLoader.loadClass(destination);

            if (IRequestablePage.class.isAssignableFrom(clazz)) {
                setResponsePage((Class<? extends IRequestablePage>) clazz);

            } else {
                throw new DetailedException(clazz.getName() + " n'hérite pas de " + IRequestablePage.class.getName());
            }

        } catch (Exception e) {
            logger.warn("La redirection vers " + destination + " n'est pas possible : " + e);
        }
    }

    /**
     * Effectue une redirection vers la page destination. N'effectue aucune
     * redirection si l'action est impossible. Les classes disponibles pour la
     * redirection sont cherchées dans le classloader de l'application, y
     * compris les jars des plugins.
     *
     * @param destination le nom de la classe de la page qui doit être ouverte. Doit
     *                    hériter de IRequestablePage.
     */
    @SuppressWarnings("unchecked")
    public void setResponsePage(String destination, PageParameters parameters) {
        try {
            ClassLoader classLoader =
                    ThaleiaApplication.get().getApplicationSettings().getClassResolver().getClassLoader();

            Class<?> clazz = classLoader.loadClass(destination);

            if (IRequestablePage.class.isAssignableFrom(clazz)) {
                setResponsePage((Class<? extends IRequestablePage>) clazz, parameters);

            } else {
                throw new DetailedException(clazz.getName() + " n'hérite pas de " + IRequestablePage.class.getName());
            }

        } catch (Exception e) {
            logger.warn("La redirection vers " + destination + " n'est pas possible : " + e);
        }
    }

    private void initPage(final boolean showMenuActions) {

        try {

            //Gestion d'une langue inconnue
            ArrayList<Locale> allowedLocales;
            Locale browserLocale;
            allowedLocales = new ArrayList<>();
            allowedLocales.add(Locale.FRENCH);
            allowedLocales.add(Locale.ENGLISH);
            browserLocale = ThaleiaSession.get().getLocale();
            boolean found = false;
            for (Locale localeToTest : allowedLocales) {
                if (Objects.equals(localeToTest.getDisplayLanguage(), browserLocale.getDisplayLanguage())) {
                    found = true;
                }
            }
            if (!found) {
                //La langue actuelle du navigateur est inconnue, on passe directement en anglais
                getSession().setLocale(Locale.ENGLISH);
            }

            Link<Void> logoLink = new Link<>("logoLink") {

                @Override
                public void onClick() {
                    // Retour vers la page d'accueil
                    if (ThaleiaSession.get().isSignedIn()) {
                        // Si identifié : page d'accueil des utilisateurs
                        // identifiés
                        setResponsePage(ThaleiaApplication.get().getRedirectionPage(Configuration.AUTHENTIFIED_USERS_WELCOME_PAGE,
                                Configuration.DEFAULT_AUTHENTIFIED_USERS_WELCOME_PAGE, Configuration.HOME_MOUNT_POINT));
                    } else {
                        // page d'accueil publique de Thaleia
                        setResponsePage(ThaleiaApplication.get().getHomePage());
                    }
                }

            };
            add(logoLink);

            logoLink.add(new Image("thaleia-logo", new PackageResourceReference(BasePage.class,
                    "img/thaleia-logo" + ".png")));

            // V5 : on présente pas le menu "Mon compte" tant qu'on n'est pas
            // connecté
            MarkupContainer accountMenu = new MarkupContainer("accountMenu") {
                @Override
                public boolean isVisible() {
                    return showMenuActions && (ThaleiaSession.get().isSignedIn());
                }
            };
            add(accountMenu);

            accountMenu.add(new Label("accountLabel", new StringResourceModel("accountLabel", this, null)));
            accountMenu.add(new Image("myAccountImage", new PackageResourceReference(BasePage.class,
                    "img/icon_user" + ".png")));

            // Bouton "Home" vers la page d'accueil des utilisateurs identifiés
            ActionLink actionslink = new ActionLink("actionsLink") {
                @Override
                public boolean isVisible() {
                    return showMenuActions;
                }
            };
            actionslink.add(new Image("homeImage", new PackageResourceReference(BasePage.class,
                    "img/icon_home_white" + ".png")));
            actionslink.add(new Label("homeLabel", new StringResourceModel("homeLabel", this, null)));
            add(actionslink);

            // Si utilisateur non identifié, on présente un lien de login
            accountMenu.add(new Link<Void>("loginLink") {

                @Override
                public void onClick() {
                    setResponsePage(WelcomePublicPage.class);
                }

                @Override
                public boolean isVisible() {
                    if (!showMenuActions) {
                        return false;
                    }

                    return !ThaleiaSession.get().isSignedIn();

                }
            }.addOrReplace(new Label("loginContent", new StringResourceModel("login", this, null))));

            // Si utilisateur identifié, on présente son nom
            accountMenu.add(new Link<Void>("userNameLink") {

                @Override
                public void onClick() {
                    setResponsePage(new UserEditPage(Model.of(ThaleiaSession.get().getAuthenticatedUser()),
                            getPageClass()));
                }

                @Override
                public boolean isVisible() {
                    if (!showMenuActions) {
                        return false;
                    }

                    return ThaleiaSession.get().isSignedIn();
                }

            }.add(new Label("userName", new Model<String>() {

                @Override
                public String getObject() {
                    User user = ThaleiaSession.get().getAuthenticatedUser();
                    if (user != null) {
                        return user.getName();
                    } else {
                        return "";
                    }
                }
            })));

            // Si utilisateur identifié, on présente un lien de logout
            accountMenu.add(new Link<Void>("logoutLink") {

                @Override
                public void onClick() {
                    ThaleiaSession.get().invalidate();
                    setResponsePage(ThaleiaApplication.get().getHomePage());
                }

                @Override
                public boolean isVisible() {
                    if (!showMenuActions) {
                        return false;
                    }

                    return ThaleiaSession.get().isSignedIn();
                }
            }.addOrReplace(new Label("logoutContent", new StringResourceModel("logout", this, null))));

            addOrReplace(new LocaleSelectorPanel("localeSelectorPanel") {
                @Override
                public boolean isVisible() {
                    return showMenuActions;
                }
            });

            // Ajout du menu
            addOrReplace(new MenuPanel("menu") {
                @Override
                public boolean isVisible() {
                    return showMenuActions;
                }
            }.setOutputMarkupId(true));

        } catch (Exception e) {
            logger.warn("Impossible de préparer la page : " + e + "\n" + LogUtils.getStackTrace(e.getStackTrace()));
        }
    }

    @AuthorizeAction(action = Action.RENDER, roles = {"user"})
    private static class ActionLink extends Link<Void> {
        public ActionLink(String id) {
            super(id);
        }

        @Override
        public void onClick() {
            setResponsePage(ThaleiaApplication.get().getRedirectionPage(Configuration.AUTHENTIFIED_USERS_WELCOME_PAGE
                    , Configuration.DEFAULT_AUTHENTIFIED_USERS_WELCOME_PAGE, Configuration.HOME_MOUNT_POINT));
        }

        @Override
        public boolean isVisible() {
            return (ThaleiaSession.get().getAuthenticatedUser() != null
                    && ThaleiaSession.get().getAuthenticatedUser().getMenuTools());
        }
    }

}
