package fr.solunea.thaleia.webapp.pages;

import fr.solunea.thaleia.service.utils.Configuration;
import fr.solunea.thaleia.utils.DetailedException;
import fr.solunea.thaleia.webapp.ThaleiaApplication;
import fr.solunea.thaleia.webapp.security.ThaleiaSession;
import fr.solunea.thaleia.webapp.utils.MessagesUtils;
import org.apache.log4j.Logger;
import org.apache.wicket.Page;
import org.apache.wicket.Session;
import org.apache.wicket.markup.head.CssReferenceHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptReferenceHeaderItem;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.component.IRequestablePage;
import org.apache.wicket.request.http.WebResponse;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.request.resource.CssResourceReference;
import org.apache.wicket.request.resource.JavaScriptResourceReference;
import org.apache.wicket.request.resource.PackageResourceReference;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Objects;

public class ThaleiaPageV6 extends WebPage {

    protected static final Logger logger = Logger.getLogger(ThaleiaPageV6.class);

    public ThaleiaPageV6(IModel<?> model) {
        super(model);
        initPage();
    }

    public ThaleiaPageV6() {
        super();
        initPage();
    }

    public ThaleiaPageV6(PageParameters parameters) {
        super(parameters);
        initPage();
    }

    /**
     * @param showMenuActions doit-on présenter les actions dans le menu (liens du menu,
     *                        accès au compte...)
     */
    public ThaleiaPageV6(boolean showMenuActions) {
        super();
        initPage(showMenuActions);
    }

    public static void addRenderHeadCookieConsent(Page page, IHeaderResponse response) {
        // Pour le consentement aux cookies
        // String param = "cookies.consent.legal." + Session.get().getLocale().toString() + ".url";
        //String legalUrl = ThaleiaApplication.get().getApplicationParameterDao().getValue(param, "");

        String legalUrl;
        if(Session.get().getLocale().toString().equals("fr")) {
            legalUrl = ThaleiaApplication.get().getApplicationParameterDao().getValue(
                    Configuration.COOKIES_CONSENT_LEGAL_FR, "http://www.solunea.fr/mentions-legales/");
        } else {
            legalUrl = ThaleiaApplication.get().getApplicationParameterDao().getValue(
                    Configuration.COOKIES_CONSENT_LEGAL_EN, "https://www.solunea.fr/en/legal-notices/");
        }

        String cookieconsent = "window.cookieconsent_options = {\n" + "	\"message\" : \""
                + MessagesUtils.getLocalizedMessage("cookies.message", ThaleiaPageV6.class, (Object[]) null) + "\",\n"
                + "	\"dismiss\" : \""
                + MessagesUtils.getLocalizedMessage("cookies.dismiss", ThaleiaPageV6.class, (Object[]) null) + "\",\n"
                + "	\"learnMore\" : \""
                + MessagesUtils.getLocalizedMessage("cookies.learnMore", ThaleiaPageV6.class, (Object[]) null) + "\",\n"
                + "	\"link\" : \""
                + legalUrl + "\",\n"
                + "	\"theme\" : \"light-bottom\"\n" + "};\n";
        response.render(JavaScriptReferenceHeaderItem.forScript(cookieconsent, null));
        response.render(JavaScriptReferenceHeaderItem.forReference(new JavaScriptResourceReference(ThaleiaPageV6.class,
                "js/cookieconsent.min.js")));
    }

    public static void addFavicons(Page page) {
        page.add(new ExternalLink("favicon1", checkUrl(page.urlFor(new PackageResourceReference(ThaleiaPageV6.class,
                "favicon" + ".ico"), null).toString())));
        page.add(new ExternalLink("favicon2", checkUrl(page.urlFor(new PackageResourceReference(ThaleiaPageV6.class,
                "favicon" + ".ico"), null).toString())));
        page.add(new ExternalLink("favicon3", checkUrl(page.urlFor(new PackageResourceReference(ThaleiaPageV6.class,
                "favicon" + ".png"), null).toString())));
    }

    private static String checkUrl(String string) {
        // Verrue affreuse !

        // Lors de la génération des URL pour les CSS :
        // Page par défaut : "./wicket/resource...." : ok
        // Page de détails d'un plugin : : "../resource/..." :ok
        // Après la page de détails, click sur "mon compte" : "./resource/..." :
        // non ok

        // Donc ce bout de code est fait pour corriger à la hache le troisème
        // cas.

        // Pour bien faire, il faudrait comprendre pourquoi le urlFor réagit de
        // cette manière. Peut-être car la page de détails du plugin n'est pas
        // bookmarkable ?

        String result = string;
        if (result.startsWith("./resource/")) {
            // On supprime le premier "."
            result = result.substring(1, result.length());
            result = "./wicket" + result;
        }

        return result;
    }

    private void initPage() {
        // logger.debug("Initialisation des ressources CSS.");
        addFavicons(this);
        manageUnallowedLocales();
    }


    private void initPage(final boolean showMenuActions) {
        addFavicons(this);
    }

    /**
     * Thaleia ne gère que les locales FR et EN. Si le client utilise une autre locale
     * pour son navigateur, on force la locale EN pour Thaleia.
     */
    private void manageUnallowedLocales() {
        //Gestion d'une langue inconnue
        ArrayList<Locale> allowedLocales = null;
        Locale browserLocale = null;
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
    }

    /**
     * TODO Suppression du cache navigateur client
     * @param response
     */
    @Override
    protected void setHeaders(WebResponse response) {
        response.setHeader( "Expires", "0" );
        response.setHeader( "Cache-Control", "no-store, no-cache, must-revalidate, max-age=0, private" );
        response.setHeader( "Pragma", "no-cache" );
    }

    @Override
    public void renderHead(IHeaderResponse response) {

        response.render(JavaScriptReferenceHeaderItem.forReference(new JavaScriptResourceReference(ThaleiaPageV6.class,
                "js/v6/jquery.min.js")).setDefer(true));
        response.render(JavaScriptReferenceHeaderItem.forReference(new JavaScriptResourceReference(ThaleiaPageV6.class,
                "js/v6/popper.min.js")).setDefer(true));
        response.render(JavaScriptReferenceHeaderItem.forReference(new JavaScriptResourceReference(ThaleiaPageV6.class,
                "js/v6/bootstrap.min.js")).setDefer(true));
        response.render(JavaScriptReferenceHeaderItem.forReference(new JavaScriptResourceReference(ThaleiaPageV6.class,
                "js/v6/jquery.waypoints.min.js")).setDefer(true));
        response.render(JavaScriptReferenceHeaderItem.forReference(new JavaScriptResourceReference(ThaleiaPageV6.class,
                "js/v6/jquery.mousewheel.min.js")).setDefer(true));
        response.render(JavaScriptReferenceHeaderItem.forReference(new JavaScriptResourceReference(ThaleiaPageV6.class,
                "js/v6/css-vars-ponyfill.min.js")).setDefer(true));
        response.render(JavaScriptReferenceHeaderItem.forReference(new JavaScriptResourceReference(ThaleiaPageV6.class,
                "js/v6/index.js")).setDefer(true));

        response.render(CssReferenceHeaderItem.forReference(new CssResourceReference(ThaleiaPageV6.class, "css/v6/style.css")));

        addRenderHeadCookieConsent(this, response);

    }

    /**
     * @param panelClassName le nom de la classe de Panel à chercher dans le classloader (application + plugins).
     * @param id             l'id du panel pour l'instanciation.
     */
    protected Panel getPanel(String panelClassName, String id) throws DetailedException {
        return getPanel(panelClassName, id, null);
    }

    /**
     * @param panelClassName le nom de la classe de Panel à chercher dans le classloader (application + plugins).
     * @param id             l'id du panel pour l'instanciation.
     * @param model          le model du panel pour l'instanciation.
     */
    protected Panel getPanel(String panelClassName, String id, IModel<?> model) throws DetailedException {
        try {
            // Recherche de la classe
            ClassLoader classLoader = ThaleiaApplication.get().getApplicationSettings().getClassResolver().getClassLoader();
            Class<? extends Panel> clazz = (Class<? extends Panel>) classLoader.loadClass(panelClassName);
            // Instanciation
            Constructor<? extends Panel> constructor;
            if (Panel.class.isAssignableFrom(clazz)) {
                if (model != null) {
                    constructor = clazz.getConstructor(String.class, IModel.class);
                    return constructor.newInstance(id, model);
                } else {
                    constructor = clazz.getConstructor(String.class);
                    return constructor.newInstance(id);
                }
            } else {
                throw new DetailedException(clazz.getName() + " n'hérite pas de " + Panel.class.getName());
            }
        } catch (Exception e) {
            logger.warn("L'instanciation du panel' " + panelClassName + " n'est pas possible : " + e);
            throw new DetailedException(e);
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
}

