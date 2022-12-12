package fr.solunea.thaleia.webapp.pages.content;

import fr.solunea.thaleia.model.Content;
import fr.solunea.thaleia.model.ContentType;
import fr.solunea.thaleia.model.ContentVersion;
import fr.solunea.thaleia.model.Locale;
import fr.solunea.thaleia.model.dao.ContentTypeDao;
import fr.solunea.thaleia.model.dao.LocaleDao;
import fr.solunea.thaleia.webapp.security.ThaleiaSession;
import org.apache.log4j.Logger;
import org.apache.wicket.Page;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.AbstractSingleSelectChoice;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;

import java.util.List;

/**
 * La liste des contenus.
 */
@SuppressWarnings("serial")
public abstract class ContentsPanel extends Panel {

    protected static final Logger logger = Logger.getLogger(ContentsPanel.class);

    /**
     * Affiche-t-on uniquement les modules ? Si non, on affiche tous les
     * contenus qui ne sont pas des modules.
     */
    protected boolean modulesOnly;

    /**
     * Doit-on présenter le sélecteur de type et le sélecteur de langue ?
     */
    protected boolean showSelectors = true;

    /**
     * Doit-on présenter le bouton de création ?
     */
    protected boolean showNew = true;
    protected AbstractSingleSelectChoice<ContentType> contentTypeSelector;
    protected AbstractSingleSelectChoice<Locale> languageSelector;

    /**
     * @param modulesOnly si true, on ne présente que des modules. Sinon, on ne présente
     *                    que des contenus.
     */
    public ContentsPanel(String id, boolean modulesOnly) {
        super(id);
        this.modulesOnly = modulesOnly;

        ContentTypeDao contentTypeDao = new ContentTypeDao(ThaleiaSession.get().getContextService().getContextSingleton());
        LocaleDao localeDao = new LocaleDao(ThaleiaSession.get().getContextService().getContextSingleton());

        // On cherche le dernier contentType ouvert stocké par la session
        ContentType contentType = ThaleiaSession.get().getLastContentTypeBrowsed(modulesOnly);

        // Si aucun trouvé, on cherche le premier trouvé en base
        if (contentType == null) {
            List<ContentType> contentTypes = contentTypeDao.find(modulesOnly);
            if (contentTypes.size() > 0) {
                contentType = contentTypes.get(0);
            }
        }

        // Le modèle de la langue des propriétés des contenus que l'on veut
        // présenter, et qui peut
        // varier si on en choisit une autre dans le sélecteur
        // Par défaut, la locale de l'IHM
        IModel<Locale> localeModel = Model.of(localeDao.getLocale(ThaleiaSession.get().getLocale()));
        init(contentType, localeModel, getDefaultContentTypesModel());
    }

    /**
     * @param contentType   le type de contenu pour lequel on veut présenter les contenus.
     * @param showSelectors doit-on présenter le sélecteur de type et le sélecteur de
     *                      langue ?
     * @param showNew       Doit-on présenter le bouton de création ?
     */
    public ContentsPanel(String id, ContentType contentType, boolean showSelectors, boolean showNew, IModel<Locale> localeModel) {
        super(id);
        this.modulesOnly = contentType.getIsModuleType();
        this.showSelectors = showSelectors;
        this.showNew = showNew;
        init(contentType, localeModel, getDefaultContentTypesModel());
    }

    /**
     * @param contentType       le type de contenu pour lequel on veut présenter les contenus.
     * @param showSelectors     doit-on présenter le sélecteur de type et le sélecteur de
     *                          langue ?
     * @param showNew           Doit-on présenter le bouton de création ?
     * @param contentTypesModel : la liste des ContentTypes à proposer dans le sélecteur de
     *                          ContentType.
     */
    public ContentsPanel(String id, ContentType contentType, boolean showSelectors, boolean showNew, IModel<Locale> localeModel,
                         IModel<List<ContentType>> contentTypesModel) {
        super(id);
        this.modulesOnly = contentType.getIsModuleType();
        this.showSelectors = showSelectors;
        this.showNew = showNew;
        init(contentType, localeModel, contentTypesModel);
    }

    /**
     * @return une liste par défaut des ContentTypes : tous ceux qui respectent
     * la condition modulesOnly.
     */
    private IModel<List<ContentType>> getDefaultContentTypesModel() {
        return new LoadableDetachableModel<>() {
            @Override
            protected List<ContentType> load() {
                // Tous les ContentType : soit les modules, soit les
                // écrans.
                ContentTypeDao contentTypeDao = new ContentTypeDao(ThaleiaSession.get().getContextService().getContextSingleton());
                return contentTypeDao.find(modulesOnly);
            }
        };
    }

    /**
     * Méthode appelée lors du clic sur le bouton "Créer un nouveau contenu".
     */
    protected abstract void onNewContent(ContentType contentType);

    @SuppressWarnings({"unchecked" , "Convert2Diamond"})
    protected void init(final ContentType contentType,
                        final IModel<Locale> localeModel,
                        final IModel<List<ContentType>> contentTypesModel) {
        ContentTypeDao contentTypeDao = new ContentTypeDao(ThaleiaSession.get().getContextService().getContextSingleton());
        LocaleDao localeDao = new LocaleDao(ThaleiaSession.get().getContextService().getContextSingleton());

        // Le modèle du type de contenu que l'on veut présenter, et qui peut
        // varier si on en choisit un autre dans le sélecteur
        final IModel<ContentType> contentTypeModel = new LoadableDetachableModel<>() {
            @Override
            protected ContentType load() {
                // Au moment où on charge le modèle, il
                // peut avoir été sérialisé, car on le
                // transmet entre pages et panels. Donc
                // il peut avoir été devenu hollow.
                return contentTypeDao.attach(contentType);
            }
        };

        WebMarkupContainer contentTypeContainer = new WebMarkupContainer("typeSelectorContainer") {
            @Override
            public boolean isVisible() {
                // On ne montre le sélecteur de ContentType que si l'on en
                // propose plusieurs.
                return contentTypeSelector.getChoices().size() > 1;
            }
        };
        // Sélecteur de type de contenu
        contentTypeSelector = (AbstractSingleSelectChoice<ContentType>) new DropDownChoice<ContentType>(
                "contentType", contentTypeModel, contentTypesModel,
                new ChoiceRenderer<>() {
                    @Override
                    public Object getDisplayValue(ContentType object) {
                        return contentTypeDao.getDisplayName(object, ThaleiaSession.get().getLocale());
                    }

                    @Override
                    public String getIdValue(ContentType object, int index) {
                        return Integer.toString(contentTypeDao.getPK(object));
                    }
                }) {
        }.setOutputMarkupId(true);
        contentTypeContainer.add(contentTypeSelector);

        // Sélecteur de langue de présentation des propriétés des contenus
        languageSelector = (AbstractSingleSelectChoice<Locale>) new DropDownChoice<Locale>(
                "language", localeModel,
                new LoadableDetachableModel<List<Locale>>() {
                    @Override
                    protected List<Locale> load() {
                        return localeDao.find();
                    }
                }, new ChoiceRenderer<>() {
            @Override
            public Object getDisplayValue(Locale object) {
                return localeDao.getDisplayName(object, ThaleiaSession.get().getLocale());
            }

            @Override
            public String getIdValue(Locale object, int index) {
                return Integer.toString(localeDao.getPK(object));
            }
        }) {
            @Override
            public boolean isVisible() {
                // On ne montre le sélecteur de Locale que si l'on en
                // propose plusieurs.
                return languageSelector.getChoices().size() > 1;
            }
        }.setOutputMarkupId(true);

        // Le panneau qui présente les contenus du ContentType sélectionné
        ContentTypeContentsPanel contentsPanel = getContentTypeContentsPanel(contentTypeModel, localeModel);
        add(contentsPanel.setOutputMarkupId(true));

        Form<ContentType> form = new Form<>("form") {
            @Override
            protected void onSubmit() {
                // On enregistre le contentType demandé en session
                ThaleiaSession.get().setLastContentTypeBrowsed(contentTypeModel.getObject());

                // On recharge un panneau de présentation des contenus pour le
                // ContentType sélectionné, pour la locale sélectionnée
                ContentsPanel.this.addOrReplace(getContentTypeContentsPanel(contentTypeModel, localeModel).setOutputMarkupId(true));
            }

            @Override
            public boolean isVisible() {
                return showSelectors;
            }
        };
        form.add(contentTypeContainer);
        form.add(languageSelector);
        add(form);

        // Les libellés des sélecteurs et filtres
        form.add(new Label("languageLabel", new StringResourceModel("languageLabel", this, null)));
        contentTypeContainer.add(new Label("typeLabel", new StringResourceModel("typeLabel", this, null)));

        // Le lien "Nouveau".
        add(new AjaxLink<Page>("newLink") {
            @Override
            public boolean isVisible() {
                return showNew;
            }

            @Override
            public void onClick(AjaxRequestTarget target) {
                onNewContent((ContentType) contentTypeSelector.getDefaultModelObject());
            }
        }.add(new Label("newLabel", new StringResourceModel("newLabel", this, null))));

    }

    /**
     * @return un panneau qui présente un tableau des contenus du contentType
     * demandé.
     */
    protected ContentTypeContentsPanel getContentTypeContentsPanel(
            IModel<ContentType> contentTypeModel, IModel<Locale> localeModel) {

        return new ContentTypeContentsPanel("contents", contentTypeModel, Model.of((ContentVersion) null), localeModel) {
            @Override
            public void onSelected(IModel<Content> model, AjaxRequestTarget target) {
                ContentsPanel.this.onSelected(model, target);
            }

            @Override
            protected void onItemLinkInitialize(AjaxLink<Content> link) {
                ContentsPanel.this.onItemLinkInitialize(link);
            }
        };
    }

    /**
     * Initialisation du lien de l'item d'un contenu.
     */
    protected abstract void onItemLinkInitialize(AjaxLink<Content> link);

    /**
     * Méthode appelée lors de la sélection d'un objet pour modification.
     */
    public abstract void onSelected(IModel<Content> model, AjaxRequestTarget target);
}
