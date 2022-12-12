package fr.solunea.thaleia.webapp.pages;

import fr.solunea.thaleia.model.Licence;
import fr.solunea.thaleia.model.User;
import fr.solunea.thaleia.model.dao.LicenceDao;
import fr.solunea.thaleia.service.utils.Configuration;
import fr.solunea.thaleia.utils.DetailedException;
import fr.solunea.thaleia.utils.LogUtils;
import fr.solunea.thaleia.webapp.ThaleiaApplication;
import fr.solunea.thaleia.webapp.pages.admin.AdminPage;
import fr.solunea.thaleia.webapp.pages.admin.UserEditPage;
import fr.solunea.thaleia.webapp.pages.content.ContentsPage;
import fr.solunea.thaleia.webapp.pages.modules.ModulesPage;
import fr.solunea.thaleia.webapp.pages.plugins.PluginsPage;
import fr.solunea.thaleia.webapp.security.ThaleiaSession;
import org.apache.log4j.Logger;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.authorization.Action;
import org.apache.wicket.authroles.authorization.strategies.role.annotations.AuthorizeAction;
import org.apache.wicket.authroles.authorization.strategies.role.annotations.AuthorizeInstantiation;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.util.cookies.CookieUtils;
import org.apache.wicket.util.visit.IVisitor;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Page présentant un menu de navigation entre les plugins, et dans l'application (compte, admin...).
 * C'est la page à extends pour les pages voulant présenter le menu de navigation.
 */
@AuthorizeInstantiation("user")
public abstract class ThaleiaV6MenuPage extends ThaleiaPageV6 {

    protected static final Logger logger = Logger.getLogger(ThaleiaV6MenuPage.class);
    private static final String LAST_ITEM_CLICKED_COOKIE_NAME = "lastClickedMenuItem";

    /**
     * Classe CSS à ajouter à la navbar
     */
    protected static String navbarClass;
    private WebMarkupContainer navbar;

    public ThaleiaV6MenuPage() {
        super();
        initPage(true);
    }

    public ThaleiaV6MenuPage(IModel<?> model) {
        super(model);
        initPage(true);
    }

    public ThaleiaV6MenuPage(PageParameters parameters) {
        super(parameters);
        initPage(true);
    }

    public ThaleiaV6MenuPage(Panel mainPanel) {
        super();
        initPage(true);
        addOrReplace(mainPanel.setOutputMarkupId(true).setOutputMarkupPlaceholderTag(true));
    }

    /**
     * @param showMenuActions doit-on présenter les actions dans le menu (liens du menu,
     *                        accès au compte...).
     */
    public ThaleiaV6MenuPage(boolean showMenuActions) {
        super();
        initPage(showMenuActions);
    }

    private void initPage(final boolean showMenuActions) {
        try {
            setNavbarClass();
            checkLocale();
            addNavbar(showMenuActions);
            addLogoutConfirmation(showMenuActions);
            addLinksToPlugins();
        } catch (Exception e) {
            logger.warn("Impossible de préparer la page : " + e + "\n" + LogUtils.getStackTrace(e.getStackTrace()));
        }
    }

    /**
     * Définition de la classe à ajouter à la navbar.
     */
    protected void setNavbarClass() {
        navbarClass = "";
    }

    /**
     * Gestion des locales. En cas de locale inconnue, l'anglais est utilisé.
     */
    private void checkLocale() {
        //Gestion d'une langue inconnue
        ArrayList<Locale> allowedLocales;
        allowedLocales = new ArrayList<>();
        allowedLocales.add(Locale.FRENCH);
        allowedLocales.add(Locale.ENGLISH);
        Locale browserLocale = ThaleiaSession.get().getLocale();
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
    }

    /**
     * Ajout de la navbar (en haut).
     *
     * @param showMenuActions doit-on présenter les actions dans le menu (liens du menu,
     *                        accès au compte...).
     */
    private void addNavbar(final boolean showMenuActions) {
        navbar = new WebMarkupContainer("navbar");
        HomeLink homeLink = new HomeLink("homeLink");
        homeLink.add(getHomeButtonLabel());
        navbar.add(homeLink);
        navbar.add(getMyAccount(showMenuActions));
        navbar.add(new AttributeAppender("class", new Model<>(navbarClass), " "));
        add(navbar);
    }

    /**
     * Ajout du menu "Mon profil", celui-ci n'est affiché qu'aux utilisateurs authentifiés.
     *
     * @param showMenuActions doit-on présenter les actions dans le menu (liens du menu,
     *                        accès au compte...).
     */
    private MarkupContainer getMyAccount(final boolean showMenuActions) {
        // Le menu "Mon compte" n'est présenté qu'aux utilisateurs authentifés.
        MarkupContainer accountMenu = new MarkupContainer("accountMenu") {

            @Override
            protected void onConfigure() {
                super.onConfigure();
                this.setVisible(showMenuActions && (ThaleiaSession.get().isSignedIn()));
            }
        };

        // Lien d'accès au compte utilisateur.
        accountMenu.add(new AjaxLink<Void>("myAccount") {

            @Override
            public void onClick(AjaxRequestTarget target) {
                new CookieUtils().save(LAST_ITEM_CLICKED_COOKIE_NAME, getId());
                setResponsePage(new UserEditPage(Model.of(ThaleiaSession.get().getAuthenticatedUser()),
                        getPageClass()));
            }

            private boolean isLinkVisible() {
                if (!showMenuActions) {
                    return false;
                }
                return ThaleiaSession.get().isSignedIn();
            }


            @Override
            protected void onConfigure() {
                super.onConfigure();
                this.setVisible(isLinkVisible());
            }
        }.setOutputMarkupId(true));

        return accountMenu;
    }

    /**
     * Ajout de la déconnexion.
     *
     * @param showMenuActions doit-on présenter les actions dans le menu (liens du menu,
     *                        accès au compte...).
     */
    private void addLogoutConfirmation(final boolean showMenuActions) {
        MarkupContainer logoutMenu = new MarkupContainer("logoutConfirmation") {

            @Override
            protected void onConfigure() {
                super.onConfigure();
                this.setVisible(showMenuActions && (ThaleiaSession.get().isSignedIn()));
            }
        };

        logoutMenu.add(new AjaxLink("logoutLink") {

            @Override
            public void onClick(AjaxRequestTarget target) {
                ThaleiaSession.get().invalidate();
                setResponsePage(ThaleiaApplication.get().getHomePage());
            }

            private boolean isLinkVisible() {
                if (!showMenuActions) {
                    return false;
                }

                return (ThaleiaSession.get().isSignedIn());
            }


            @Override
            protected void onConfigure() {
                super.onConfigure();
                setVisible(isLinkVisible());
            }
        }.setOutputMarkupId(true));

        add(logoutMenu);
    }

    private void addLinksToPlugins() {
        CannelleLink pluginThaleiaXL = new CannelleLink("pluginThaleiaXL");
        PluginsLink pluginsLink = new PluginsLink("pluginsLink");
        AdminLink adminLink = new AdminLink("adminLink");
        ModulesLink modulesLink = new ModulesLink("modulesLink");
        ContentsLink contentsLink = new ContentsLink("contentsLink");
        DialogueLink pluginThaleiaDialogue = new DialogueLink("pluginThaleiaDialogue");
        DiffusionLink diffusion = new DiffusionLink("diffusion");
        AnalyseLink analyse = new AnalyseLink("analyse");
        List<Link<?>> items = new ArrayList<>();
        items.add(pluginThaleiaXL);
        items.add(pluginsLink);
        items.add(adminLink);
        items.add(modulesLink);
        items.add(contentsLink);
        items.add(pluginThaleiaDialogue);
        items.add(diffusion);
        items.add(analyse);

        // Le bouton qui ouvre le panneau de navigation
        WebMarkupContainer pluginsNavigationPanelOpener = new WebMarkupContainer("pluginsNavigationPanelOpener") {

            @Override
            protected void onConfigure() {
                super.onConfigure();
                setVisible(moreThanOneItemVisible(items));
            }
        };
        navbar.add(pluginsNavigationPanelOpener);

        // Le panneau d'accès aux boutons de navigation
        WebMarkupContainer pluginsNavigationPanel = new WebMarkupContainer("pluginsNavigationPanel") {

            @Override
            protected void onConfigure() {
                super.onConfigure();
                this.setVisible(moreThanOneItemVisible(items));
            }
        };
        add(pluginsNavigationPanel);

        for (Link<?> item : items) {
            pluginsNavigationPanel.add(item);
        }

        // Si pas de menu de navigation, alors on ouvre le seul plugin auquel on a le droit d'accéder
        // On ne simule ce clic QUE si on est sur la page d'arrivée (https://.../instance/home), sinon on va cliquer sans fin.
        logger.debug("Ouverture de la page : " + this.getClass().getName());
        if (this.getClass().getName().equals("fr.solunea.thaleia.plugins.welcomev6.BasePage")) {

            // Présence d'un plugin déjà ouvert ?
            boolean itemClicked = false;
            CookieUtils cookieUtils = new CookieUtils();
            String lastOpenedPage = cookieUtils.load(LAST_ITEM_CLICKED_COOKIE_NAME);
            if (lastOpenedPage != null && !lastOpenedPage.isEmpty()) {
                itemClicked = clickOnItem(lastOpenedPage);
                logger.debug("Cookie de page à ouvrir : " + lastOpenedPage + " ouvert=" + itemClicked);
            }

            // Pas d'ouverture issue de l'historique : ouverture par défaut : le premieritem visible
            if (!itemClicked) {
                for (Link<?> item : items) {
                    if (item.isVisible() && !itemClicked) {
                        logger.debug("item.onClick() sur " + item.getId());
                        itemClicked = true;
                        clickOnItem(item.getId());
                    }
                }
            }
        }
    }

    // Si ce nom correspond à un item Wicket visible existant sur la page, alors on l'ouvre.
    protected boolean clickOnItem(String itemId) {
        getPage().visitChildren(Link.class, (IVisitor<Link<?>, Void>) (object, visit) -> {
            if (object.getId().equals(itemId) && object.isVisible()) {
                new CookieUtils().save(LAST_ITEM_CLICKED_COOKIE_NAME, itemId);
                object.onClick();
            }
        });
        return false;
    }

    private boolean moreThanOneItemVisible(List<Link<?>> items) {
        // On masque le menu si on a moins de 2 liens dans ce menu
        int visibleItems = 0;

        for (Link<?> item : items) {
            if (item.isVisible()) {
                visibleItems++;
            }
        }
        return visibleItems > 1;
    }

    /**
     * Est-ce que le plugin Publish est visible pour cet utilisateur ?
     * Le plugin est visible pour les licences "v6.openbar", "v6.publisher" et "v6.demo.publish".
     * @return boolean
     */
    private boolean publishPluginVisible() {
        // Présence du plugin ?
        if (ThaleiaApplication.get().getPluginDao().findByName("fr.solunea.thaleia.plugins.publish.PublishPlugin").isEmpty()) {
            return false;
        }
        // Licence existante pour l'accès à ce plugin ?
        try {
            LicenceDao licenceDao = ThaleiaApplication.get().getLicenceDao();
            List<Licence> licences = new ArrayList<>();
            licences.add(licenceDao.findByName("v6.openbar"));
            licences.add(licenceDao.findByName("v6.publisher"));
            licences.add(licenceDao.findByName("v6.demo.publish"));     // Licence accordant des droits de publication aux comptes de démo
            return (ThaleiaSession.get().getAuthenticatedUser() != null
                    && ThaleiaSession.get().getAuthenticatedUser().getIsAdmin())
                    || ThaleiaApplication.get().getLicenceService().isOneLicenceHolded(ThaleiaSession.get().getAuthenticatedUser(), licences);
        } catch (DetailedException e) {
            logger.warn(e);
            return false;
        }
    }

    protected Label getHomeButtonLabel() {
        return (Label) new Label("homeLinkLabel", new StringResourceModel("homeLinkLabel", this, null)).setEscapeModelStrings(false);
    }

    // Le bouton qui ouvre le panneau de navigation
    @AuthorizeAction(action = Action.RENDER, roles = {"user"})
    private static class HomeLink extends Link<Void> {
        public HomeLink(String id) {
            super(id);
        }

        @Override
        public void onClick() {
            new CookieUtils().save(LAST_ITEM_CLICKED_COOKIE_NAME, getId());
            setResponsePage(ThaleiaApplication.get().getRedirectionPage(
                    Configuration.AUTHENTIFIED_USERS_WELCOME_PAGE,
                    Configuration.DEFAULT_AUTHENTIFIED_USERS_WELCOME_PAGE,
                    Configuration.HOME_MOUNT_POINT
            ));
        }

        @Override
        protected void onConfigure() {
            super.onConfigure();
            User user = ThaleiaSession.get().getAuthenticatedUser();
            this.setVisible(user != null && user.getMenuTools());
        }


    }

    /**
     * Bouton "Plugins"
     */
    @AuthorizeAction(action = Action.RENDER, roles = {"admin"})
    private static class PluginsLink extends Link<Void> {
        public PluginsLink(String id) {
            super(id);
            add(new Label("pluginsLinkLabel",
                    new StringResourceModel("pluginsLinkLabel", this, null)));
        }

        @Override
        public void onClick() {
            setResponsePage(PluginsPage.class);
        }

        @Override
        protected void onConfigure() {
            super.onConfigure();
            User user = ThaleiaSession.get().getAuthenticatedUser();
            this.setVisible(user != null && user.getIsAdmin());
        }

    }

    /**
     * Bouton "Admin" pour retourner à la page d'acceuil des utilisateurs identifiés.
     */
    @AuthorizeAction(action = Action.RENDER, roles = {"admin"})
    private static class AdminLink extends Link<Void> {
        public AdminLink(String id) {
            super(id);
            add(new Label("adminLinkLabel",
                    new StringResourceModel("adminLinkLabel", this, null)));
        }

        @Override
        public void onClick() {
            new CookieUtils().save(LAST_ITEM_CLICKED_COOKIE_NAME, getId());
            setResponsePage(AdminPage.class);
        }

        @Override
        protected void onConfigure() {
            super.onConfigure();
            User user = ThaleiaSession.get().getAuthenticatedUser();
            this.setVisible(user != null && user.getIsAdmin());
        }


    }

    /**
     * Bouton "Modules" du panneau latéral gauche.
     */
    @AuthorizeAction(action = Action.RENDER, roles = {"user"})
    private static class ModulesLink extends Link<Void> {
        public ModulesLink(String id) {
            super(id);
            add(new Label("modulesLinkLabel",
                    new StringResourceModel("modulesLinkLabel", this, null)));
        }

        @Override
        public void onClick() {
            new CookieUtils().save(LAST_ITEM_CLICKED_COOKIE_NAME, getId());
            setResponsePage(ModulesPage.class);
        }

        @Override
        protected void onConfigure() {
            super.onConfigure();
            User user = ThaleiaSession.get().getAuthenticatedUser();
            this.setVisible(user != null && user.getMenuModules());
        }
    }

    /**
     * Bouton "Thaleia XL" du panneau latéral gauche.
     */
    @AuthorizeAction(action = Action.RENDER, roles = {"user"})
    private class CannelleLink extends Link<Void> {
        public CannelleLink(String id) {
            super(id);
            add(new Label("thaleiaXLLabel", "<svg class=\"icon-thaleia\"><use href=\"#icon-thaleia\"></use></svg>")
                    .setEscapeModelStrings(false));
        }

        @Override
        public void onClick() {
            new CookieUtils().save(LAST_ITEM_CLICKED_COOKIE_NAME, getId());
            ThaleiaV6MenuPage.this.setResponsePage("fr.solunea.thaleia.plugins.cannelle.v6.MainPage");
        }

        private boolean isLinkVisible() {
            // Présence du plugin ?
            if (ThaleiaApplication.get().getPluginDao().findByName("fr.solunea.thaleia.plugins.action.ActionPlugin").isEmpty()) {
                return false;
            }
            // Licence existante pour l'accès à ce plugin ?
            try {
                LicenceDao licenceDao = ThaleiaApplication.get().getLicenceDao();
                List<Licence> licences = new ArrayList<>();
                licences.add(licenceDao.findByName("v6.openbar"));
                licences.add(licenceDao.findByName("v6.demo.dialogue"));
                licences.add(licenceDao.findByName("v6.dialogue"));
                licences.add(licenceDao.findByName("v6.openbar"));
                return ThaleiaSession.get().getAuthenticatedUser() != null && (ThaleiaApplication.get().getLicenceService().isOneLicenceHolded(ThaleiaSession.get().getAuthenticatedUser(), licences) || ThaleiaSession.get().getAuthenticatedUser().getIsAdmin());
            } catch (DetailedException e) {
                logger.warn(e);
                return false;
            }
        }

        @Override
        protected void onConfigure() {
            super.onConfigure();
            this.setVisible(isLinkVisible());
        }
    }

    /**
     * Bouton "Dialogue" du panneau latéral gauche.
     */
    private class DialogueLink extends Link<Void> {
        public DialogueLink(String id) {
            super(id);
            add(new Label("thaleiaDialogueLabel", "<svg class=\"icon-dialogue\"><use href=\"#icon-dialogue\"></use></svg>")
                    .setEscapeModelStrings(false));
        }

        @Override
        public void onClick() {
            new CookieUtils().save(LAST_ITEM_CLICKED_COOKIE_NAME, getId());
            ThaleiaV6MenuPage.this.setResponsePage("fr.solunea.thaleia.plugins.action.MainPage");
        }


        private boolean isLinkVisible() {
            // Présence du plugin ?
            if (ThaleiaApplication.get().getPluginDao().findByName("fr.solunea.thaleia.plugins.action.ActionPlugin").isEmpty()) {
                return false;
            }
            // Licence existante pour l'accès à ce plugin ?
            try {
                LicenceDao licenceDao = ThaleiaApplication.get().getLicenceDao();
                List<Licence> licences = new ArrayList<>();
                licences.add(licenceDao.findByName("v6.openbar"));
                licences.add(licenceDao.findByName("v6.demo.dialogue"));
                licences.add(licenceDao.findByName("v6.dialogue"));
                licences.add(licenceDao.findByName("v6.openbar"));
                return ThaleiaSession.get().getAuthenticatedUser() != null && (ThaleiaApplication.get().getLicenceService().isOneLicenceHolded(ThaleiaSession.get().getAuthenticatedUser(), licences) || ThaleiaSession.get().getAuthenticatedUser().getIsAdmin());
            } catch (DetailedException e) {
                logger.warn(e);
                return false;
            }
        }

        @Override
        protected void onConfigure() {
            super.onConfigure();
            this.setVisible(isLinkVisible());
        }
    }

    /**
     * Bouton "Diffusion" du panneau latéral gauche.
     */
    private class DiffusionLink extends Link<Void> {
        public DiffusionLink(String id) {
            super(id);
            add(new Label("diffusionLabel",
                    new StringResourceModel("diffusionLabel", this, null)
            ).setEscapeModelStrings(false));
        }

        @Override
        public void onClick() {
            new CookieUtils().save(LAST_ITEM_CLICKED_COOKIE_NAME, getId());
            ThaleiaV6MenuPage.this.setResponsePage("fr.solunea.thaleia.plugins.publish.MainPage");
        }

        @Override
        protected void onConfigure() {
            super.onConfigure();
            this.setVisible(publishPluginVisible());
        }
    }

    /**
     * Bouton "Analyser" du panneau latéral gauche.
     */
    private class AnalyseLink extends Link<Void> {
        public AnalyseLink(String id) {
            super(id);
            add(new Label("analyseLabel",
                    new StringResourceModel("analyseLabel", this, null)
            ).setEscapeModelStrings(false));
        }

        @Override
        public void onClick() {
            new CookieUtils().save(LAST_ITEM_CLICKED_COOKIE_NAME, getId());
            ThaleiaV6MenuPage.this.setResponsePage("fr.solunea.thaleia.plugins.analyze.pages.MainStatsPage");
        }

        @Override
        protected void onConfigure() {
            super.onConfigure();
            this.setVisible(publishPluginVisible());
        }
    }

    /**
     * Bouton "Contenus" du panneau latéral gauche.
     */

    @AuthorizeAction(action = Action.RENDER, roles = {"user"})
    private class ContentsLink extends Link<Void> {
        public ContentsLink(String id) {
            super(id);
            add(new Label("contentsLinkLabel",
                    new StringResourceModel("contentsLinkLabel", this, null)));
        }

        @Override
        public void onClick() {
            new CookieUtils().save(LAST_ITEM_CLICKED_COOKIE_NAME, getId());
            ThaleiaV6MenuPage.this.setResponsePage(ContentsPage.class);
        }

        @Override
        protected void onConfigure() {
            super.onConfigure();
            User user = ThaleiaSession.get().getAuthenticatedUser();
            this.setVisible(user != null && user.getMenuModules());
        }
    }
}
