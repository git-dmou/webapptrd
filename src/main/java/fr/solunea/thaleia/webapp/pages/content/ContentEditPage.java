package fr.solunea.thaleia.webapp.pages.content;

import fr.solunea.thaleia.model.Content;
import fr.solunea.thaleia.model.ContentType;
import fr.solunea.thaleia.model.ContentVersion;
import fr.solunea.thaleia.model.dao.ContentDao;
import fr.solunea.thaleia.model.dao.ContentTypeDao;
import fr.solunea.thaleia.model.dao.ContentVersionDao;
import fr.solunea.thaleia.model.dao.DomainDao;
import fr.solunea.thaleia.utils.LogUtils;
import fr.solunea.thaleia.webapp.ThaleiaApplication;
import fr.solunea.thaleia.webapp.pages.BasePage;
import fr.solunea.thaleia.webapp.security.ThaleiaSession;
import org.apache.cayenne.ObjectContext;
import org.apache.log4j.Logger;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.authroles.authorization.strategies.role.annotations.AuthorizeInstantiation;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;

@SuppressWarnings("serial")
@AuthorizeInstantiation("user")
public class ContentEditPage extends BasePage {

    private static final Logger logger = Logger.getLogger(ContentEditPage.class);

    /**
     * Page d'édition d'un nouveau contenu.
     */
    public ContentEditPage() {
        // Ce constructeur par défaut doit exister pour que Wicket puisse
        // instancier cette page au besoin, même s'il n'y a pas d'appel
        // explicite dans le reste du code.

        super();

        // On place comme modèle un nouveau contenu, sans type.
        Content content = getNewContent();
        setDefaultModel(Model.of(content));

        // On lui crée une nouvelle version
        ContentVersion version = createNewVersion(content);

        // On édite cette nouvelle version
        initPage(version, false);
    }

    /**
     * Page d'édition de ce contenu, dans une nouvelle version.
     *
     * @param model le modèle du contenu
     */
    public ContentEditPage(IModel<Content> model) {
        super(model);

        // On lui crée une nouvelle version dans un contexte d'édition spécifique
        ContentVersion version = createNewVersion(model.getObject());

        // On édite cette nouvelle version
        initPage(version, false);
    }

    /**
     * Page de présentation en lecture seule de ce contenu, dans la version
     * demandée.
     *
     * @param model le modèle du contenu
     */
    public ContentEditPage(IModel<Content> model, int revisionNumber) {
        super(model);

        // On place le content à éditer dans un contexte spécifique
         ObjectContext newContext = ThaleiaSession.get().getContextService().getNewContext();
         setDefaultModelObject(new ContentDao(newContext).get(model.getObject().getObjectId()));

        // On récupère la version demandée
        ContentVersion version = ((Content) getDefaultModel().getObject()).getVersion(revisionNumber);

        if (version == null) {
            logger.warn("Imposible d'éditer la version " + revisionNumber + " du contenu : la version n'existe pas. "
                    + "Contenu :" + model.getObject());
        }

        // On présente cette version sans pouvoir l'éditer.
        initPage(version, true);
    }

    /**
     * Page d'édition de cette version de contenu.
     *
     * @param model le modèle du contenu
     */
    public ContentEditPage(IModel<Content> model, IModel<ContentVersion> versionModel) {
        super(model);

        // On place le content à éditer dans un contexte spécifique
         ObjectContext newContext = ThaleiaSession.get().getContextService().getNewContext();
         setDefaultModelObject(new ContentDao(newContext).get(model.getObject().getObjectId()));

         ContentVersion version = new ContentVersionDao(newContext).get(versionModel.getObject().getObjectId());

        // On édite cette version
        initPage(version, false);
    }

    /**
     * Page d'édition d'un nouveau contenu, de ce type, et module ou non.
     */
    public ContentEditPage(ContentType contentType, boolean isModule) {
        super();

        // Nouveau contenu, module ou non
        Content content = getNewContent();
        content.setIsModule(isModule);

        // On fabrique une nouvelle version, pour uniquement fixer le ContentType
        ContentVersion version = ThaleiaSession.get().getContentService().createFirstVersion(content, ThaleiaSession.get().getAuthenticatedUser());
        version.setContentType(new ContentTypeDao(version.getObjectContext()).get(contentType.getObjectId()));

        // On en fait le modèle de la page
        setDefaultModel(Model.of(content));

        // On édite cette nouvelle version
        initPage(content.getLastVersion(), false);
    }

    /**
     *
     * @return un nouveau Content, dans un contexte d'édition spécifique.
     */
    private Content getNewContent() {
        // On créé un nouveau contenu, dans un contexte spécifique
        ObjectContext newContext = ThaleiaSession.get().getContextService().getNewContext();
        Content content = new ContentDao(newContext).get(true);

        // Par défaut, le domaine du contenu est celui de l'utilisateur
        content.setDomain(new DomainDao(newContext).get(ThaleiaSession.get().getAuthenticatedUser().getDomain().getObjectId()));

        return content;
    }

    /**
     * Fabrique une nouvelle version à éditer pour ce contenu, dans un contexte spécifique.
     */
    private ContentVersion createNewVersion(Content content) {
        // On obtient le contennu dans un contexte spécifique
        ObjectContext newContext = ThaleiaSession.get().getContextService().getNewContext();
        Content localContent = new ContentDao(newContext).get(content.getObjectId());
        // On crée une nouvelle version, basée sur la plus  récente existante (ou une première version si elle n'existe pas
        // encore).
        try {
            return ThaleiaSession.get().getContentService().getNewVersion(localContent, ThaleiaSession
                    .get().getAuthenticatedUser());

        } catch (Exception e) {
            logger.warn("Impossible de créer une nouvelle version du contenu '" + content + "' : " + e);
            logger.warn(LogUtils.getStackTrace(e.getStackTrace()));
            setResponsePage(ThaleiaApplication.get().getHomePage());

            return null;
        }
    }

    /**
     * Initialise l'édition de cette version, en mode lecture uniquement si demandé.
     */
    private void initPage(final ContentVersion contentVersion, final boolean readOnly) {
        Component contentEditPanel = new ContentEditPanel("contentEditPanel", Model.of(contentVersion), readOnly) {
            @Override
            protected void onOut(ContentType contentType) {
                // Après l'édition de ce contenu, on présente la liste des
                // contenus du même type.
                setResponsePage(new ContentsPage(contentType));
            }
        }.setOutputMarkupId(true);
        add(contentEditPanel);

        // Libellé de la page
        String contentTypeName = new ContentTypeDao(contentVersion.getObjectContext()).getDisplayName(((Content) ContentEditPage
                .this.getDefaultModelObject()).getLastVersion().getContentType(), ThaleiaSession.get().getLocale());
        add(new Label("pageLabel", new StringResourceModel("pageLabel", this, null, new Object[]{contentTypeName})));

        // Le sélecteur de version
        add(new VersionSelectorPanel("versionSelectorPanel", new LoadableDetachableModel<>() {
            @Override
            protected ContentVersion load() {
                logger.debug("Chargement de la version " + contentVersion);
                return contentVersion;
            }
        }) {
            @Override
            protected void onSelected(AjaxRequestTarget target, IModel<ContentVersion> version) {
                LoadableDetachableModel<Content> content = new LoadableDetachableModel<>() {
                    @Override
                    protected Content load() {
                        return version.getObject().getContent();
                    }
                };

                if (new ContentVersionDao(version.getObject().getObjectContext()).isNewObject(version.getObject())) {
                    // Si cette version n'existe pas en base, alors on  permet de l'éditer.
                    setResponsePage(new ContentEditPage(content, version));

                } else {
                    // Si cette version existe déjà en base, on ne
                    // permet pas de la modifier, mais on la présente.
                    setResponsePage(new ContentEditPage(content, version.getObject().getRevisionNumber()));
                }
            }
        }.setOutputMarkupId(true));
    }

}
