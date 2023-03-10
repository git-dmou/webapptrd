package fr.solunea.thaleia.webapp.pages.content;

import fr.solunea.thaleia.model.*;
import fr.solunea.thaleia.model.dao.*;
import fr.solunea.thaleia.utils.DetailedException;
import fr.solunea.thaleia.utils.LogUtils;
import fr.solunea.thaleia.webapp.ThaleiaApplication;
import fr.solunea.thaleia.webapp.pages.ErrorPage;
import fr.solunea.thaleia.webapp.panels.EditLinkOnTextPanel;
import fr.solunea.thaleia.webapp.panels.confirmation.ConfirmationPanel;
import fr.solunea.thaleia.webapp.security.ThaleiaSession;
import org.apache.cayenne.PersistenceState;
import org.apache.commons.lang.time.DateFormatUtils;
import org.apache.commons.lang.time.FastDateFormat;
import org.apache.log4j.Logger;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.ajax.markup.html.form.AjaxCheckBox;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.SortOrder;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.DefaultDataTable;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.extensions.markup.html.repeater.util.SortableDataProvider;
import org.apache.wicket.extensions.model.AbstractCheckBoxModel;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.*;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

@SuppressWarnings("serial")
public abstract class ContentTypeContentsPanel extends Panel {

    private static final Logger logger = Logger.getLogger(ContentTypeContentsPanel.class);
    /**
     * Le nombre d'items de la liste des contenus pr??sent??s dans une page.
     */
    private static final int ITEMS_PER_PAGE = 20;
    /**
     * Les contenus pr??sent??s
     */
    protected final IModel<List<Content>> contentsModel;
    /**
     * La locale dans laquelle on veut pr??senter les valeurs de propri??t??s.
     */
    protected final IModel<Locale> localeModel;
    /**
     * Le type de contenu pr??sent??
     */
    final IModel<ContentType> contentTypeModel;
    /**
     * Le mod??le de la case "s??lectionner tout" / "d??selectionner tout"
     */
    private final IModel<Boolean> selectAllModel = Model.of(false);
    /**
     * Les contenus s??lectionn??s par les checkBoxes. On ne veut pas conserver
     * cette liste au cours de la navigation : on la perd donc lors des
     * d??tachements li??s ?? l'entr??e et la sortie de la page dans le cache de
     * Wicket.
     */
    private final IModel<List<Content>> selectedContents = new LoadableDetachableModel<>() {
        @Override
        protected List<Content> load() {
            return new ArrayList<>();
        }
    };
    /**
     * Les colonnes du tableau
     */
    private final List<IColumn<Content, String>> columns = new ArrayList<>();
    /**
     * La valeur ?? appliquer comme filtre sur les ??l??ments du tableau.
     */
    private final IModel<String> filterModel = Model.of("");
    /**
     * Le tableau de pr??sentation des ??l??ments.
     */
    private DefaultDataTable<Content, String> datatable;

    /**
     * @param contentTypeModelParam On pr??sentera dans le tableau tous les contenus du m??me
     *                              ContentType.
     * @param moduleParam           si non nul, alors on ne pr??sentera que les contenus qui sont
     *                              des modules comme lui, ou pas des modules comme lui. Si nul,
     *                              on pr??sentera les contenus en fonction du moduleType du
     *                              ContentType.
     * @param localeModelParam      la langue dans laquelle pr??senter les propri??t??s du tableau.
     */
    public ContentTypeContentsPanel(String id,
                                    final IModel<ContentType> contentTypeModelParam,
                                    final IModel<ContentVersion> moduleParam,
                                    final IModel<Locale> localeModelParam) {
        super(id, contentTypeModelParam);

        this.setOutputMarkupId(true);

        ContentTypeDao contentTypeDao = new ContentTypeDao(ThaleiaSession.get().getContextService().getContextSingleton());
        ContentVersionDao contentVersionDao = new ContentVersionDao(ThaleiaSession.get().getContextService().getContextSingleton());
        LocaleDao localeDao = new LocaleDao(ThaleiaSession.get().getContextService().getContextSingleton());

        // On remplace les mod??les par des versions LoadableDetachable pour
        // ??viter les probl??mes d'objets Cayenne qui deviennent Hollow si on
        // joue avec la navigation dans la pages par l'historique du client web.
        contentTypeModel = new LoadableDetachableModel<>() {
            @Override
            protected ContentType load() {
                return contentTypeDao.get(contentTypeModelParam.getObject().getObjectId());
            }
        };
        final IModel<ContentVersion> moduleModel = new LoadableDetachableModel<>() {
            @Override
            protected ContentVersion load() {
                if (moduleParam.getObject() == null) {
                    return null;
                } else {
                    return contentVersionDao.get(moduleParam.getObject().getObjectId());
                }
            }
        };
        this.localeModel = new LoadableDetachableModel<>() {
            @Override
            protected Locale load() {
                return localeDao.get(localeModelParam.getObject().getObjectId());
            }
        };

        // On r??cup??re la liste des contenus visibles pour ce
        // user, soit des modules, soit des ??crans, uniquement de ce
        // ContentType
        if (moduleModel.getObject() != null) {
            contentsModel = new LoadableDetachableModel<>() {
                @Override
                protected List<Content> load() {
                    return ThaleiaSession.get().getContentService().getContents(ThaleiaSession.get().getAuthenticatedUser(), moduleModel.getObject().getContent().getIsModule(), contentTypeModel.getObject(), null, null);
                }
            };

        } else {
            contentsModel = new LoadableDetachableModel<>() {
                @Override
                protected List<Content> load() {
                    return ThaleiaSession.get().getContentService().getContents(ThaleiaSession.get().getAuthenticatedUser(), contentTypeModel.getObject().getIsModuleType(), contentTypeModel.getObject(), null, null);
                }
            };
//            contentsModel = new IModel<>() {
//                @Override
//                public List<Content> getObject() {
//                    return ThaleiaSession.get().getContentService().getContents(ThaleiaSession.get().getAuthenticatedUser(), moduleModel.getObject().getContent().getIsModule(), contentTypeModel.getObject(), null, null);
//                }
//
//                @Override
//                public void detach() {
//                }                @Override
//                public void setObject(List<Content> object) {
//                }
//
//
//            };
//        } else {
//            contentsModel = new IModel<>() {
//                @Override
//                public void detach() {
//                }
//
//                @Override
//                public List<Content> getObject() {
//                    return ThaleiaSession.get().getContentService().getContents(ThaleiaSession.get().getAuthenticatedUser(), contentTypeModel.getObject().getIsModuleType(), contentTypeModel.getObject(), null, null);
//                }
//
//                @Override
//                public void setObject(List<Content> object) {
//
//                }
//            };
        }

        // Initialisation des colonnes
        initColumns();

        datatable = getTable();
        add(datatable);

        // Le filtre de recherche
        Form<?> form = new Form<Void>("form");
        add(form);
        form.add(new TextField<>("filter", filterModel)).add(
                new AjaxButton("search") {
                    @Override
                    protected void onSubmit(org.apache.wicket.ajax.AjaxRequestTarget target, org.apache.wicket.markup.html.form.Form<?> form) {
                        // On supprime les s??lections ??ventuelles
                        selectAllModel.setObject(false);
                        selectedContents.setObject(new ArrayList<>());

                        // On recharge la table
                        target.add(datatable);
                    }
                }.add(new Label("searchLabel", new StringResourceModel("searchLabel", this, null))));
        // Le bouton Supprimer
        add(getDeleteLink());
    }

    private void initColumns() {
        try {
            ContentDao contentDao = new ContentDao(ThaleiaSession.get().getContextService().getContextSingleton());
            ContentTypePropertyDao contentTypePropertyDao = new ContentTypePropertyDao(ThaleiaSession.get().getContextService().getContextSingleton());
            LocaleDao localeDao = new LocaleDao(ThaleiaSession.get().getContextService().getContextSingleton());
            ContentPropertyValueDao contentPropertyValueDao = new ContentPropertyValueDao(ThaleiaApplication.get().getConfiguration().getLocalDataDir().getAbsolutePath(),
                    ThaleiaApplication.get().getConfiguration().getBinaryPropertyType(), ThaleiaSession.get().getContextService().getContextSingleton());

            // On vide les colonnes existantes
            columns.clear();

            // On ajoute en premier la colonne de l'identifiant du contenu
            columns.add(new PropertyColumn<>(
                    new LoadableDetachableModel<String>() {
                        // Le mod??le qui renvoie le libell?? de l'en-t??te de cette colonne
                        @Override
                        public String load() {
                            // Recherche de la cl?? contentIdentifier dans le fichier .properties
                            return new StringResourceModel("contentIdentifier", ContentTypeContentsPanel.this, null).getString();
                        }

                    }, "lastVersion.contentIdentifier", "lastVersion.contentIdentifier") {

                @Override
                public void populateItem(
                        final Item<ICellPopulator<Content>> item,
                        final String componentId, final IModel<Content> rowModel) {
                    // On place un panel qui va porter le texte (identifiant du
                    // contenu pr??sent?? sur cette ligne) et le lien d'??dition
                    item.add(new EditLinkOnTextPanel<>(componentId, rowModel, getDataModel(rowModel)) {
                        @Override
                        protected void onClick(IModel<Content> contentModel, AjaxRequestTarget target) {
                            // On recharge l'objet depuis son Id, afin
                            // d'??viter les objects Hollow apr??s usage de la
                            // navigation dans l'historique du client web.
                            Content content = contentDao.get(rowModel.getObject().getObjectId());

                            // On remonte le traitement de la demande d'??dition
                            // ?? la classe d'impl??mentation de ce panel.
                            ContentTypeContentsPanel.this.onSelected(Model.of(content), target);
                        }

                        @Override
                        protected void onItemLinkInitialize(AjaxLink<Content> link) {
                            ContentTypeContentsPanel.this.onItemLinkInitialize(link);
                        }
                    });
                }

            });

            // On ajoute la colonne de date de derni??re mise ?? jour
            columns.add(new PropertyColumn<>(new LoadableDetachableModel<String>() {
                // Le mod??le qui renvoie le libell?? de l'en-t??te de
                // cette colonne
                @Override
                public String load() {
                    // Recherche de la cl?? lastUpdateDate dans le
                    // fichier .properties
                    return new StringResourceModel("lastUpdateDate", ContentTypeContentsPanel.this, null).getString();
                }
            }, "lastVersion.lastUpdateDate", "lastVersion.lastUpdateDate") {
                @Override
                public IModel<Object> getDataModel(final IModel<Content> rowModel) {
                    // On ne renvoie pas la valeur de la propri??t??
                    // "lastVersion.lastUpdateDate", mais le formattage de cette
                    // date selon la locale de l'utilisateur.

                    return new LoadableDetachableModel<>() {
                        @Override
                        public Object load() {
                            // La date au format de la locale, puis l'heure au
                            // format de la locale. Par exemple :
                            // FR : 29/10/13 16:39
                            // EN : 10/29/13 4:39 PM
                            return DateFormatUtils.format(rowModel.getObject().getLastVersion().getLastUpdateDate(), FastDateFormat.getDateInstance(FastDateFormat.SHORT, ThaleiaSession.get().getLocale()).getPattern() + " " + FastDateFormat.getTimeInstance(FastDateFormat.SHORT, ThaleiaSession.get().getLocale()).getPattern());
                        }

                        @Override
                        public void detach() {
                        }

                        @Override
                        public void setObject(Object object) {
                        }
                    };
                }
            });

            // La liste des des propri??t??s du contentType : on n'obtient que
            // celles qui sont visibles pour l'utilisateur.
            List<ContentTypeProperty> contentTypeProperties = contentTypeModel.getObject().getVisibleProperties(ThaleiaSession.get().getAuthenticatedUser());

            // On parcourt ces propri??t??s pour cr??er une colonne de
            // pr??sentation.
            for (final ContentTypeProperty contentTypeProperty : contentTypeProperties) {

                // On pr??pare le stockage de l'Id de ce ContentTypeProperty,
                // afin d'??viter les objects Hollow apr??s usage de la navigation
                // dans l'historique du client web.
                final int contentTypePropertyId = contentTypePropertyDao.getPK(contentTypeProperty);

                // Le nom de la propri??t?? sur laquelle sera effectu?? le tri = le
                // nom du ContentProperty
                String sortProperty = contentTypeProperty.getContentProperty().getName();

                // Le nom de la propri??t?? appel??e pour afficher la valeur dans
                // la cellule.
                // On utilise une cha??ne vide, car on ne passera pas par ce
                // m??canisme par d??faut de r??cup??ration de la valeur, mais on la
                // calculera avec un appel un peu plus complexe qu'un getXXX() :
                // voir getDataModel de la PropertyColumn plus bas.
                String propertyExpression = "";

                // On ajoute la colonne pour ce ContentTypeProperty
                columns.add(new PropertyColumn<>(new LoadableDetachableModel<String>() {
                    // Le mod??le qui renvoie le libell?? de l'en-t??te de
                    // cette colonne
                    @Override
                    public String load() {
                        // On recharge l'objet ContentTypeProperty
                        // d'apr??s son id
                        ContentTypeProperty contentTypeProperty = contentTypePropertyDao.get(contentTypePropertyId);
                        // On recherche le nom de cette propri??t?? dans
                        // la locale de la session.
                        return contentTypeProperty.getName(localeDao.getLocale(ThaleiaSession.get().getLocale()), "");
                    }

                }, sortProperty, propertyExpression) {
                    @Override
                    public IModel<Object> getDataModel(final IModel<Content> rowModel) {
                        // Le mod??le de la valeur de la cellule ??
                        // pr??senter pour cette colonne
                        return new LoadableDetachableModel<>() {
                            @Override
                            public Object load() {
                                // On recharge l'objet ContentTypeProperty
                                // d'apr??s son id
                                ContentTypeProperty contentTypeProperty = contentTypePropertyDao.get(contentTypePropertyId);
                                // On recherche la valeur de cette propri??t??,
                                // pour l'objet de la ligne, et pour la locale
                                // de la session.
                                return rowModel.getObject().getLastVersion().getPropertyValue(contentTypeProperty.getContentProperty(), localeModel.getObject(), "", contentPropertyValueDao);
                            }
                        };
                    }
                });

            }

            // On ajoute la colonne des chexkboxes pour la s??lection multiple
            columns.add(new CheckBoxColumn<>(Model.of("")) {
                @Override
                protected IModel<Boolean> getCheckBoxModel(final IModel<Content> rowModel) {
                    return new AbstractCheckBoxModel() {
                        @Override
                        public void unselect() {
                            // On supprime l'objet de la liste des Content
                            // s??lectionn??s
                            selectedContents.getObject().remove(rowModel.getObject());
                        }

                        @Override
                        public void select() {
                            // On ajoute l'objet ?? la liste des Content
                            // s??lectionn??s
                            selectedContents.getObject().add(rowModel.getObject());
                        }

                        @Override
                        public boolean isSelected() {
                            // Cet objet est-il dans la liste des Content
                            // s??lectionn??s ?
                            return selectedContents.getObject().contains(rowModel.getObject());
                        }

                        @Override
                        public void detach() {
                            rowModel.detach();
                        }
                    };
                }

            });

        } catch (Exception e) {
            logger.warn("Impossible d'initialiser les colonnes : " + e.toString());
            logger.warn(LogUtils.getStackTrace(e.getStackTrace()));
            setResponsePage(ErrorPage.class);
        }

    }

    /**
     * @return Le tableau des ??l??ments ?? pr??senter
     */
    @SuppressWarnings("unchecked")
    private DefaultDataTable<Content, String> getTable() {
        // Le tableau des ??l??ments ?? pr??senter
        return (DefaultDataTable<Content, String>) new DefaultDataTable<>("datatable", columns, new ContentProvider(), ITEMS_PER_PAGE).setOutputMarkupId(true);
    }

    /**
     * M??thode appel??e lors de la s??lection d'un objet dans la liste.
     */
    public abstract void onSelected(IModel<Content> model, AjaxRequestTarget target);

    /**
     * @return un lien "supprimer" les Content s??lectionn??s.
     */
    private Component getDeleteLink() {

        // Le label du lien
        IModel<String> labelModel = new StringResourceModel("deleteLabel",
                this, null);

        // Le texte de confirmation de suppression
        IModel<String> textModel = new LoadableDetachableModel<>() {
            @Override
            public String load() {
                logger.debug("Chargement du mod??le du texte de confirmation de la suppression.");
                try {
                    // Le message de confirmation d??pend de ce qu'il y a ??
                    // supprimer : si parmi les contenus ?? supprimer, un est
                    // utilis?? comme fils d'un autre contenu, alors on le
                    // pr??vient qu'il sera retir?? des attachements en plus
                    // d'??tre supprim??.

                    // La liste des identifiants qui seront supprim??s
                    String deleted = getIdList(selectedContents.getObject());

                    // On demande la liste des contenus qui r??f??rencent
                    // ces contenus
                    List<Content> parents = ThaleiaSession.get().getContentService().getParents(selectedContents.getObject());

                    String result;
                    if (parents.isEmpty()) {
                        // Le message demande juste confirmation de la
                        // suppression.
                        result = new StringResourceModel("delete.selected.confirm", ContentTypeContentsPanel.this, null, "", deleted).getString();
                    } else {
                        // Le message demande la confirmation, et
                        // pr??sente les modules qui seront affect??s.
                        String concerned = getIdList(parents);
                        result = new StringResourceModel("delete.selected.confirm.screens", ContentTypeContentsPanel.this, null, "", deleted, concerned).getString();
                    }
                    logger.debug("Message de demande de confirmation : " + result);
                    return result;

                } catch (Exception e) {
                    logger.debug("Impossible de construire le message" + " de confirmation de la suppression : " + e);
                    return "";
                }
            }
        };

        return new ConfirmationPanel("delete", labelModel, textModel) {
            @Override
            public boolean isEnabled() {
                // On peut supprimer si au moins un contenu est s??lectionn??
                return !selectedContents.getObject().isEmpty();
            }

            @Override
            public boolean isVisible() {
                // Visible seulement si la liste pr??sente au moins un ??l??ment
                return datatable.getItemCount() > 0;
            }

            @Override
            protected void onConfirm(AjaxRequestTarget target) {
                try {
                    // Suppression des contenus
                    ThaleiaSession.get().getContentService().deleteContentsAndAllVersions(selectedContents.getObject());
                    // On commite
                    if (!selectedContents.getObject().isEmpty()) {
                        selectedContents.getObject().get(0).getObjectContext().commitChanges();
                    }

                    // Message de r??ussite
                    StringResourceModel messageModel = new StringResourceModel("delete.ok", ContentTypeContentsPanel.this, null);
                    info(messageModel.getString());

                    // On s'assure que les contenus de la table seront
                    // recharg??s, pour ne plus pr??senter ceux supprim??s. C'est
                    // l'int??r??t du LoadableDetachableModel.
                    contentsModel.detach();

                    // On vide la liste des contenus s??lectionn??s.
                    selectedContents.getObject().clear();

                    // On met ?? jour l'activation du bouton Supprimer : on le
                    // recharge, et il g??re sa vie.
                    Component deleteLink = getDeleteLink();
                    ContentTypeContentsPanel.this.replace(deleteLink);
                    target.add(deleteLink);

                } catch (DetailedException e) {
                    logger.warn("Impossible de supprimer les contenus s??lectionn??s : "
                            + e.toString());
                    // Roolback des modifications
                    if (!selectedContents.getObject().isEmpty()) {
                        selectedContents.getObject().get(0).getObjectContext().rollbackChanges();
                    }

                    // Message d'erreur
                    StringResourceModel messageModel = new StringResourceModel("delete.error", ContentTypeContentsPanel.this, null);
                    error(messageModel.getString());
                }

                // On recharge la page.
                setResponsePage(target.getPage());
            }

            @Override
            protected void onCancel(AjaxRequestTarget target) {
                // TODO Auto-generated method stub

            }
        }.setOutputMarkupId(true);
    }

    /**
     * @return La liste des identifiants : "'toto1' 'toto2'"
     */
    private String getIdList(List<Content> contents) {
        StringBuilder result = new StringBuilder();

        if (contents == null) {
            return "";
        }

        for (Content content : contents) {
            if (content.getLastVersion() != null) {
                result.append("\"").append(content.getLastVersion().getContentIdentifier()).append("\" ");
            }
        }
        return result.toString();
    }

    /**
     * Initialisation du lien d'un item pr??sent?? dans le tableau. Par exemple
     * pour d??corer avec :
     *
     * <pre>
     * <code>
     * link.add(
     *    new AttributeModifier(
     *        "onclick",
     * 		  "document.getElementById('moduleSelectorform').style['display'] = 'none';"
     * 			 + " document.getElementById('progress').style['display'] = 'inline';"
     * 			 + " return;"));
     * </code>
     * </pre>
     */
    protected abstract void onItemLinkInitialize(AjaxLink<Content> link);

    private abstract static class CheckPanel extends Panel {
        public CheckPanel(String id, IModel<Boolean> checkModel) {
            super(id);
            add(new AjaxCheckBox("check", checkModel) {
                @Override
                protected void onUpdate(AjaxRequestTarget target) {
                    onCheckBoxUpdate(target);
                }
            });
        }

        /**
         * M??thode appel??e lors d'une mise ?? jour de l'??tat de la case ?? cocher.
         */
        abstract void onCheckBoxUpdate(AjaxRequestTarget target);
    }

    protected class ContentProvider extends SortableDataProvider<Content, String> {

        private final SortableDataProviderComparator comparator = new SortableDataProviderComparator();

        public ContentProvider() {
            // On fixe le tri par d??faut, afin qu'il ne soit juste pas nul.
            setSort("lastVersion.lastUpdateDate", SortOrder.DESCENDING);
        }

        @Override
        public Iterator<? extends Content> iterator(long first, long count) {
            // On copie la liste des contenus
            List<Content> contents = new ArrayList<>(getContents());

            // On trie cette liste
            contents.sort(comparator);

            // On pagine le r??sultat
            return contents.subList(Long.valueOf(first).intValue(), Long.valueOf(first + count).intValue()).iterator();
        }

        /**
         * @return les contenus, filtr??s si l'utilsiateur a demand?? d'appliquer
         * un filtre
         */
        private List<Content> getContents() {

            if (filterModel.getObject() != null && filterModel.getObject().length() > 0) {

                // On filtre
                List<Content> filtered = new ArrayList<>();
                for (Content content : contentsModel.getObject()) {
                    if (ThaleiaSession.get().getContentService().containsInPropertyValue(content.getLastVersion(), filterModel.getObject())) {
                        filtered.add(content);
                    }
                }

                // On renvoie les ??l??ments filtr??s
                return filtered;

            } else {
                // Pas de filtre : on renvoie les objets du mod??le
                return contentsModel.getObject();
            }
        }

        @Override
        public long size() {
            return getContents().size();
        }

        @Override
        public IModel<Content> model(final Content object) {
            return new AbstractReadOnlyModel<>() {
                @Override
                public Content getObject() {
                    return object;
                }
            };
        }

        class SortableDataProviderComparator implements Comparator<Content>,
                Serializable {

            @SuppressWarnings({"rawtypes", "unchecked"})
            public int compare(final Content o1, final Content o2) {

                ContentVersion version1 = o1.getLastVersion();
                ContentVersion version2 = o2.getLastVersion();

                if (version1 == null || version2 == null) {
                    logger.debug("Comparaison de " + version1 + " avec "
                            + version2
                            + " : comparaison impossible d'objets nuls.");
                    return 0;
                }

                // La propri??t?? sur laquelle effectuer le tri
                String sortProperty = getSort().getProperty();

                // On rechercher les valeurs ?? comparer pour les deux objets
                // Content
                Comparable value1;
                Comparable value2;
                int result = 0;

                if ("lastVersion.contentIdentifier".equals(sortProperty)) {
                    // Cas particulier : contentIdentifier
                    value1 = version1.getContentIdentifier();
                    value2 = version2.getContentIdentifier();

                } else if ("lastVersion.lastUpdateDate".equals(sortProperty)) {
                    // Cas particulier : on compare des dates
                    value1 = version1.getLastUpdateDate();
                    value2 = version2.getLastUpdateDate();

                } else {
                    // Cas g??n??ral : c'est le nom d'une ContentProperty

                    // La ContentProperty correspondante
                    ContentPropertyDao contentPropertyDao = new ContentPropertyDao(o1.getObjectContext());
                    ContentProperty contentProperty = contentPropertyDao.findByName(sortProperty);

                    // R??cup??ration des valeurs
                    try {
                        ContentPropertyValueDao contentPropertyValueDao = new ContentPropertyValueDao(ThaleiaApplication.get().getConfiguration().getLocalDataDir().getAbsolutePath(),
                                ThaleiaApplication.get().getConfiguration().getBinaryPropertyType(), o1.getObjectContext());
                        value1 = version1.getPropertyValue(contentProperty, localeModel.getObject(), "", contentPropertyValueDao);
                        value2 = version2.getPropertyValue(contentProperty, localeModel.getObject(), "", contentPropertyValueDao);
                    } catch (Exception e) {
                        // On ignore l'erreur
                        value1 = "";
                        value2 = "";
                    }
                }

                // Comparaison n'est pas raison
                try {
                    result = value1.compareTo(value2);
                } catch (Exception e) {
                    // Peut arriver si identifier est nul
                    logger.debug("Probl??me de comparaison ? " + e);
                }

                // Prise en compte de l'ordre demand??
                if (!getSort().isAscending()) {
                    result = -result;
                }

                return result;
            }
        }

    }

    /**
     * Une case ?? cocher, qui va activer ou non le bouton Supprimer.
     *
     * @param <T> Le type de la valeur pr??sent??e
     * @param <S> Le type de la propri??t?? de tri
     */
    abstract class CheckBoxColumn<T, S> extends AbstractColumn<T, S> {

        public CheckBoxColumn(IModel<String> displayModel) {
            super(displayModel);
        }

        @Override
        public void populateItem(Item<ICellPopulator<T>> cellItem, String componentId, final IModel<T> rowModel) {
            cellItem.add(new CheckPanel(componentId,
                    getCheckBoxModel(new LoadableDetachableModel<>() {
                        @Override
                        @SuppressWarnings("unchecked")
                        protected T load() {
                            T object = rowModel.getObject();
                            // On s'assure que l'objet renvoy?? n'est pas Hollow
                            // On ne traite que les Content, car notre classe
                            // interne n'est utilis??e ici que pour T = Content.
                            if (Content.class.isAssignableFrom(object.getClass()) && ((Content) object).getPersistenceState() == PersistenceState.HOLLOW) {
                                // On recharge l'objet Cayenne dans le contexte
                                // de la session, en se basant sur son id, qui
                                // est bien d??s??rialis??.
                                ContentDao contentDao = new ContentDao(ThaleiaSession.get().getContextService().getContextSingleton());
                                object = (T) contentDao.get(((Content) object).getObjectId());
                            }
                            return object;
                        }
                    })) {
                @Override
                void onCheckBoxUpdate(AjaxRequestTarget target) {
                    // On met ?? jour l'activation du bouton Supprimer : on le
                    // recharge, et lui g??re sa vie.
                    Component deleteLink = getDeleteLink();
                    ContentTypeContentsPanel.this.replace(deleteLink);
                    target.add(deleteLink);
                }
            });
        }

        protected abstract IModel<Boolean> getCheckBoxModel(IModel<T> rowModel);

        @Override
        public Component getHeader(String componentId) {
            // Dans l'en-t??te, on ajoute une checkbox qui va g??rer l'??tat de
            // toutes celles de la colonne
            return new CheckPanel(componentId, selectAllModel) {

                @Override
                public boolean isVisible() {
                    // Visible seulement si la liste pr??sente au moins un
                    // ??l??ment
                    return datatable.getItemCount() > 0;
                }

                @Override
                void onCheckBoxUpdate(AjaxRequestTarget target) {

                    // On met ?? jour le mod??le qui contient l'??tat de s??lection
                    // de toutes les checkbox. Pour cela on demande le bool??en
                    // associ?? ?? cette case ?? cocher : true ou false.
                    if (selectAllModel.getObject()) {
                        // On s??l??ctionne tout
                        @SuppressWarnings("unchecked")
                        Iterator<Content> contentIterator = (Iterator<Content>) datatable.getDataProvider().iterator(0, datatable.getItemCount());
                        while (contentIterator.hasNext()) {
                            Content content = contentIterator.next();
                            selectedContents.getObject().add(content);
                        }
                    } else {
                        // On vide la s??lection.
                        selectedContents.getObject().clear();
                    }

                    // On met ?? jour l'activation du bouton Supprimer : on le
                    // recahrge, et il g??re sa vie.
                    Component deleteLink = getDeleteLink();
                    ContentTypeContentsPanel.this.replace(deleteLink);
                    target.add(deleteLink);

                    // On met ?? jour la table, pour que les checkbox soient
                    // mises ?? jour
                    datatable = getTable();
                    add(datatable);

                    // On recharge la page
                    setResponsePage(this.getPage());
                }
            };
        }
    }
}
