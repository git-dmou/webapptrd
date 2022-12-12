package fr.solunea.thaleia.webapp.pages;

import fr.solunea.thaleia.utils.DetailedException;
import fr.solunea.thaleia.webapp.ThaleiaApplication;
import fr.solunea.thaleia.webapp.security.ThaleiaSession;
import fr.solunea.thaleia.webapp.utils.MessagesUtils;
import org.apache.log4j.Logger;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Page;
import org.apache.wicket.markup.head.CssReferenceHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptReferenceHeaderItem;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.component.IRequestablePage;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.request.resource.CssResourceReference;
import org.apache.wicket.request.resource.JavaScriptResourceReference;
import org.apache.wicket.request.resource.PackageResourceReference;

@SuppressWarnings("serial")
public class ThaleiaPage extends WebPage {

    protected static final Logger logger = Logger.getLogger(ThaleiaPage.class);

    public ThaleiaPage(IModel<?> model) {
        super(model);
        initPage();
    }

    public ThaleiaPage() {
        super();
        initPage();
    }

    public ThaleiaPage(PageParameters parameters) {
        super(parameters);
        initPage();
    }

    public static void addRenderHeadCookieConsent(Page page, IHeaderResponse response) {
        response.render(JavaScriptReferenceHeaderItem.forReference(new JavaScriptResourceReference(ThaleiaPage.class,
                "js/datepickerloader.js", page.getLocale(), page.getStyle(), page.getVariation())));
        // Pour le consentement aux cookies
        String cookieconsent = "window.cookieconsent_options = {\n" + "	\"message\" : \""
                + MessagesUtils.getLocalizedMessage("cookies.message", ThaleiaPage.class, (Object[]) null) + "\",\n"
                + "	\"dismiss\" : \""
                + MessagesUtils.getLocalizedMessage("cookies.dismiss", ThaleiaPage.class, (Object[]) null) + "\",\n"
                + "	\"learnMore\" : \""
                + MessagesUtils.getLocalizedMessage("cookies.learnMore", ThaleiaPage.class, (Object[]) null) + "\",\n"
                + "	\"link\" : \""
                + MessagesUtils.getLocalizedMessage("cookies.link", ThaleiaPage.class, (Object[]) null) + "\",\n"
                + "	\"theme\" : \"light-bottom\"\n" + "};\n";
        response.render(JavaScriptReferenceHeaderItem.forScript(cookieconsent, null));
        response.render(JavaScriptReferenceHeaderItem.forReference(new JavaScriptResourceReference(ThaleiaPage.class,
                "js/cookieconsent.min.js")));
    }

    public static void addFavicons(Page page) {
        page.add(new ExternalLink("favicon1", checkUrl(page.urlFor(new PackageResourceReference(ThaleiaPage.class,
                "favicon" + ".ico"), null).toString())));
        page.add(new ExternalLink("favicon2", checkUrl(page.urlFor(new PackageResourceReference(ThaleiaPage.class,
                "favicon" + ".ico"), null).toString())));
        page.add(new ExternalLink("favicon3", checkUrl(page.urlFor(new PackageResourceReference(ThaleiaPage.class,
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
    }

    @Override
    public void renderHead(IHeaderResponse response) {
        response.render(JavaScriptReferenceHeaderItem.forReference(new JavaScriptResourceReference(ThaleiaPage.class, "js/jquery-1.12.0.min.js")));
        response.render(JavaScriptReferenceHeaderItem.forReference(new JavaScriptResourceReference(ThaleiaPage.class, "js/bootstrap.min.js")));
        response.render(JavaScriptReferenceHeaderItem.forReference(new JavaScriptResourceReference(ThaleiaPage.class, "js/jquery.form.min.js")));
        response.render(JavaScriptReferenceHeaderItem.forReference(new JavaScriptResourceReference(ThaleiaPage.class, "js/jquery.joyride-2.1.js")));
        response.render(JavaScriptReferenceHeaderItem.forReference(new JavaScriptResourceReference(ThaleiaPage.class, "js/moment.min.js")));
        response.render(JavaScriptReferenceHeaderItem.forReference(new JavaScriptResourceReference(ThaleiaPage.class, "js/daterangepicker.js")));
        //response.render(JavaScriptReferenceHeaderItem
        //		.forReference(new JavaScriptResourceReference(ThaleiaPage.class, "js/modernizr-2.8.3-respond-1.4.2.min
        // .js")));

        addRenderHeadCookieConsent(this, response);

        response.render(CssReferenceHeaderItem.forReference(new CssResourceReference(ThaleiaPage.class, "css/bootstrap.min.css")));
        response.render(CssReferenceHeaderItem.forReference(new CssResourceReference(ThaleiaPage.class, "css/bootstrap-theme.min.css")));
        response.render(CssReferenceHeaderItem.forReference(new CssResourceReference(ThaleiaPage.class, "css/style.css")));
        response.render(CssReferenceHeaderItem.forReference(new CssResourceReference(ThaleiaPage.class, "css/prettify.css")));
        response.render(CssReferenceHeaderItem.forReference(new CssResourceReference(ThaleiaPage.class, "css/daterangepicker-bs3.css")));
        response.render(CssReferenceHeaderItem.forReference(new CssResourceReference(ThaleiaPage.class, "css/joyride-2.1.css")));

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
