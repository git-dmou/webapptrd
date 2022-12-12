package fr.solunea.thaleia.webapp.pages.plugins;

import fr.solunea.thaleia.model.Domain;
import fr.solunea.thaleia.model.Plugin;
import fr.solunea.thaleia.model.User;
import fr.solunea.thaleia.model.dao.DomainDao;
import fr.solunea.thaleia.model.dao.PluginDao;
import fr.solunea.thaleia.plugins.IPluginImplementation;
import fr.solunea.thaleia.service.utils.ClassFactory;
import fr.solunea.thaleia.utils.DetailedException;
import fr.solunea.thaleia.utils.LogUtils;
import fr.solunea.thaleia.webapp.ThaleiaApplication;
import fr.solunea.thaleia.webapp.panels.ThaleiaFeedbackPanel;
import fr.solunea.thaleia.webapp.panels.confirmation.ConfirmationLink;
import fr.solunea.thaleia.webapp.security.ThaleiaSession;
import org.apache.log4j.Logger;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormSubmitBehavior;
import org.apache.wicket.authorization.Action;
import org.apache.wicket.authroles.authorization.strategies.role.annotations.AuthorizeAction;
import org.apache.wicket.extensions.ajax.markup.html.IndicatingAjaxLink;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.markup.html.image.resource.RenderedDynamicImageResource;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.PropertyListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;

import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.*;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

/**
 * La liste des plugins.
 */
@SuppressWarnings("serial")
public class PluginsPanel extends Panel {

    private static final Logger logger = Logger.getLogger(PluginsPanel.class);

    protected ThaleiaFeedbackPanel feedbackPanel;

    public PluginsPanel(String id) {
        super(id);

        final User user = ThaleiaSession.get().getContextService().getObjectInNewContext(ThaleiaSession.get().getAuthenticatedUser());

        feedbackPanel = new ThaleiaFeedbackPanel("feedback");
        add(feedbackPanel.setOutputMarkupId(true));

        // En-tête du tableau
        WebMarkupContainer header = new WebMarkupContainer("header") {
            @Override
            public boolean isVisible() {
                return ThaleiaSession.get().getAuthenticatedUser().getIsAdmin();
            }
        };
        add(header);

        // La localisation de l'en-tête du tableau
        header.add(new Label("pluginLabel", new StringResourceModel("pluginLabel", this, null)));
        header.add(new Label("descriptionLabel", new StringResourceModel("descriptionLabel", this, null)));
        header.add(new Label("domainLabel", new StringResourceModel("domainLabel", this, null)));
        header.add(new Label("actionsLabel", new StringResourceModel("actionsLabel", this, null)));
        header.add(new Label("revisionLabel", new StringResourceModel("revisionLabel", this, null)));

        // Remplissage du tableau
        PropertyListView<Plugin> objects = new PropertyListView<>("objects", new
                LoadableDetachableModel<List<Plugin>>() {
                    @Override
                    protected List<Plugin> load() {
                        try {
                            // On recherche les plugins dans un contexte propre, afin d'isoler les modifications
                            return ThaleiaSession.get().getPluginService().getPlugins(user);
                        } catch (DetailedException e) {
                            logger.warn("Impossible de récupérer la liste des plugins : " + e);
                            return new ArrayList<>();
                        }
                    }
                }) {

            @Override
            protected void populateItem(final ListItem<Plugin> item) {

                final IModel<Plugin> plugin = new LoadableDetachableModel<>() {
                    @Override
                    protected Plugin load() {
                        return item.getModelObject();
                    }
                };
                try {
                    final IPluginImplementation pluginImplementation = ThaleiaSession.get().getPluginService().getImplementation(plugin.getObject().getName());

                    // Image
                    item.add(new Image("image", new PluginImage(item.getModel(), 200, 110)));

                    // Bouton de lancement du plugin
                    item.add(new IndicatingAjaxLink<>("btnExecute", item.getModel()) {

                        @Override
                        public void onClick(AjaxRequestTarget target) {
                            // logger.debug("Lancement de la page d'accueil du plugin "
                            // + pluginImplementation
                            // .getTitleInManifest(Locale.FRENCH));

                            // On place en session le nom de la classe
                            // d'implémentation du plugin
                            ThaleiaSession.get().setAttribute(ThaleiaSession.LAST_LAUNCHED_PLUGIN_CLASS, pluginImplementation.getClass().getName());
                            // logger.debug("Session : "
                            // + ThaleiaSession.LAST_LAUNCHED_PLUGIN_CLASS
                            // + " = "
                            // + pluginImplementation.getClass().getTitleInManifest());

                            // Lancement de la page d'accueil du plugin
                            Class<?> mainPageClass = pluginImplementation.getPage();

                            if (mainPageClass == null) {
                                logger.warn("Le plugin renvoie null comme classe d'exécution !");
                            } else {
                                try {
                                    setResponsePage(ClassFactory.getInstanceOf(mainPageClass.getName(), WebPage.class, ThaleiaSession.get().getPluginService().getClassLoader()));

                                } catch (Exception e) {
                                    logger.warn(
                                            "Impossible de rediriger vers la page '" + mainPageClass.getName() + "' :"
                                                    + " " + e);
                                }

                                // Cette redirection construit une page
                                // bookmarkable, ce qui pose problème dans le cas
                                // d'une session expirée, ou d'une connexion hors
                                // session.
                                // setResponsePage(mainPageClass
                                // .asSubclass(WebPage.class));
                            }
                        }

                    }.add(new Label("btnExecuteLabel", new StringResourceModel("btnExecuteLabel", this, null))));

                    // Bouton d'information du plugin
                    item.add(new Link<>("btnInfo", item.getModel()) {

                        @Override
                        public void onClick() {
                            try {
                                // On place en session le nom de la classe
                                // d'implémentation du plugin
                                ThaleiaSession.get().setAttribute(ThaleiaSession.LAST_LAUNCHED_PLUGIN_CLASS, pluginImplementation.getClass().getName());

                                setResponsePage(ClassFactory.getInstanceOf(pluginImplementation.getDetailsPage().getName(), WebPage.class, ThaleiaSession.get().getPluginService().getClassLoader()));
                            } catch (DetailedException e) {
                                logger.warn("Impossible de rediriger vers la page '" + pluginImplementation.getDetailsPage().getName() + "' : " + e);
                            }
                        }

                        @Override
                        public boolean isVisible() {
                            try {
                                Class<?> detailsPage = pluginImplementation.getDetailsPage();
                                return (detailsPage != null);
                            } catch (Exception e) {
                                logger.error(e);
                                return false;
                            }
                        }

                    }.add(new Label("btnInfoLabel", new StringResourceModel("btnInfoLabel", this, null))));

                    // Nom et description du plugin
                    item.add(new Label("description", new Model<>(pluginImplementation.getDescription(ThaleiaSession.get().getLocale()))));
                    item.add(new Label("name", new Model<>(pluginImplementation.getName(ThaleiaSession.get().getLocale()))));

                    addDomainSelector(item, plugin);
                    addDeleteAction(item, plugin);
                    addPluginRevisionInfo(item, plugin);

                } catch (Exception e) {
                    logger.warn("Erreur durant la présentation de la liste des plugins : " + e);
                    emptyItem(item, plugin);

                } catch (NoClassDefFoundError e) {
                    logger.warn("Erreur durant la présentation de la liste des plugins : " + e + "\n"
                            + LogUtils.getStackTrace());
                    emptyItem(item, plugin);
                }
            }

            /**
             * Remplit cet item de plugin avec aucun élément ne provenant du
             * plugin, mais permettant la suppression.
             */
            private void emptyItem(ListItem<Plugin> item, final IModel<Plugin> plugin) {

                try {
                    item.add(new Image("image", "").setVisible(false));


                    // Bouton de lancement du plugin
                    item.add(new Link<>("btnExecute", item.getModel()) {
                        @Override
                        public void onClick() {
                            // rien
                        }
                    }.add(new Label("btnExecuteLabel", new StringResourceModel("btnExecuteLabel", this, null))).setEnabled(false));

                    // Bouton d'information du plugin
                    item.add(new Link<>("btnInfo", item.getModel()) {
                        @Override
                        public void onClick() {
                            // rien
                        }
                    }.add(new Label("btnInfoLabel", new StringResourceModel("btnInfoLabel", this, null))).setEnabled(false));

                    // Nom et description du plugin
                    item.add(new Label("description", new Model<>("")));
                    item.add(new Label("name", new Model<>("")));

                    addDomainSelector(item, plugin);
                    addDeleteAction(item, plugin);
                    addPluginRevisionInfo(item, plugin);
                } catch (Exception e) {
                    logger.debug(e.toString());
                }

            }

            private void addDomainSelector(ListItem<Plugin> item, final IModel<Plugin> plugin) {
                DomainDao domainDao = new DomainDao(plugin.getObject().getObjectContext());
                // Sélecteur de domaine
                Form<Plugin> form = new Form<>("form") {
                    @Override
                    protected void onSubmit() {
                        try {
                            PluginDao pluginDao = new PluginDao(plugin.getObject().getObjectContext());
                            pluginDao.save(plugin.getObject(), true);
                        } catch (DetailedException e) {
                            logger.warn("Impossible de modifier le domaine du plugin : " + e);
                        }
                    }
                };
                item.add(form);
                DropDownChoice<Domain> select = new DropDownChoice<>("domain", new LoadableDetachableModel<List<Domain>>() {
                    @Override
                    protected List<Domain> load() {
                        // On ne présente que les domaines sur lequel l'utilisateur a la visibilité
                        return ThaleiaSession.get().getUserService().getAuthorizedDomains(user);
                    }
                }, new ChoiceRenderer<Domain>() {
                    @Override
                    public Object getDisplayValue(Domain object) {
                        return domainDao.getDisplayName(object, ThaleiaSession.get()
                                .getLocale());
                    }

                    @Override
                    public String getIdValue(Domain object, int index) {
                        return Integer.toString(domainDao.getPK(object));
                    }
                }) {
                    @Override
                    public boolean isVisible() {
                        return ThaleiaSession.get().getAuthenticatedUser().getIsAdmin();
                    }
                };
                form.add(select.setOutputMarkupId(true));
                select.add(new AjaxFormSubmitBehavior(form, "onchange") {
                    @Override
                    protected void onEvent(AjaxRequestTarget target) {
                        super.onEvent(target);
                        target.add(form);
                    }
                });
            }

            private void addDeleteAction(ListItem<Plugin> item, final IModel<Plugin> plugin) {
                // Lien "Supprimer"
                PluginDao pluginDao = new PluginDao(plugin.getObject().getObjectContext());
                item.add(new DeletePluginLink("delete", new StringResourceModel("delete.confirm", this, null, new Object[]{pluginDao.getDisplayName(item.getModelObject(), ThaleiaSession.get().getLocale())}).getString()) {
                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        try {
                            ThaleiaSession.get().getPluginService().deletePlugin(plugin.getObject());

                            logger.info("Demande de réinitialisation du classloader...");
                            ThaleiaApplication.get().setClassLoader();
                            logger.info("Réinitialisation du classloader ok.");

                        } catch (DetailedException e) {
                            StringResourceModel errorMessageModel = new StringResourceModel("delete.error", PluginsPanel.this, Model.of(plugin));
                            ThaleiaSession.get().error(errorMessageModel.getString());

                            // On journalise le détail technique.
                            logger.warn("Impossible de supprimer l'objet de type '" + plugin.getClass().getName() + "' : " + e.toString());
                        }
                        // On recharge la page.
                        setResponsePage(target.getPage());

                    }
                }.add(new Label("deleteLabel", new StringResourceModel("deleteLabel", this, null))));
            }

        };
        objects.setOutputMarkupId(true);
        add(objects);

        // Le lien "Nouveau", pour les admins
        add(new NewPluginLink("newLink").add(new Label("newLabel", new StringResourceModel("newLabel", this, null))));
    }

    private void addPluginRevisionInfo(ListItem<Plugin> item, final IModel<Plugin> plugin ) throws DetailedException {
        String revision = ThaleiaSession.get().getPluginService().getPluginRevision(plugin.getObject());
        item.add(new Label("revision", new Model<>(revision)));
    }

    private static final class PluginImage extends RenderedDynamicImageResource {

        private final IModel<Plugin> pluginModel;

        public PluginImage(IModel<Plugin> pluginModel, int width, int height) {
            super(width, height);
            this.pluginModel = pluginModel;
        }

        @Override
        protected boolean render(Graphics2D graphics, Attributes attributes) {

            Iterator<?> readers = ImageIO.getImageReadersByFormatName("png");
            ImageReader reader = (ImageReader) readers.next();

            IPluginImplementation plugin;
            try {
                plugin = ThaleiaSession.get().getPluginService().getImplementation(pluginModel.getObject().getName());
            } catch (DetailedException e) {
                logger.warn("Impossible d'obtenir l'implémentation du plugin " + pluginModel.getObject() + " : " + e);
                return false;
            }

            byte[] png = plugin.getImageAsPng();
            try (ByteArrayInputStream source = new ByteArrayInputStream(png);
                 ImageInputStream iis = ImageIO.createImageInputStream(source)) {

                // logger.debug("Image du plugin : " + png.length
                // + " octets chargés.");

                return drawImage(graphics, reader, iis);

            } catch (Exception e) {
                logger.warn("Impossible de lire le flux d'entrée comme une image : " + e);
                // Impossible de récupérer l'image du plugin, on renvoie une
                // image par défaut
                try (ImageInputStream iis = ImageIO.createImageInputStream(Objects.requireNonNull(ThaleiaApplication.get().getApplicationSettings()
                        .getClassResolver().getClassLoader().getResourceAsStream("images/blank.png")))) {
                    return drawImage(graphics, reader, iis);
                } catch (IOException e2) {
                    logger.error("Impossible de charger l'image de plugin par défaut : " + e2);
                }
            }
            logger.warn("Impossible d'interpréter le flux comme une image.");
            return false;
        }

        private boolean drawImage(Graphics2D graphics, ImageReader reader, ImageInputStream iis) throws IOException {
            reader.setInput(iis, true);
            ImageReadParam param = reader.getDefaultReadParam();
            java.awt.Image image;
            image = reader.read(0, param);
            graphics.drawImage(image, null, null);
            return true;
        }
    }

    @AuthorizeAction(action = Action.RENDER, roles = {"admin"})
    private static class NewPluginLink extends Link<Void> {
        public NewPluginLink(String id) {
            super(id);
        }

        @Override
        public void onClick() {
            setResponsePage(EditPluginPage.class);
        }
    }

    @AuthorizeAction(action = Action.RENDER, roles = {"admin"})
    private abstract static class DeletePluginLink extends ConfirmationLink<Void> {

        public DeletePluginLink(String id, String text) {
            super(id, Model.of(text));
        }
    }

}
