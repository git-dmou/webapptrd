package fr.solunea.thaleia.webapp.pages.admin;

import fr.solunea.thaleia.model.Domain;
import fr.solunea.thaleia.model.Licence;
import fr.solunea.thaleia.model.User;
import fr.solunea.thaleia.model.dao.LicenceDao;
import fr.solunea.thaleia.model.dao.UserDao;
import fr.solunea.thaleia.utils.DetailedException;
import fr.solunea.thaleia.utils.LogUtils;
import fr.solunea.thaleia.webapp.ThaleiaApplication;
import fr.solunea.thaleia.webapp.pages.ErrorPage;
import fr.solunea.thaleia.webapp.pages.demo.CreateAccountWithLicencePage;
import fr.solunea.thaleia.webapp.panels.confirmation.ConfirmationLink;
import fr.solunea.thaleia.webapp.security.ThaleiaSession;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.time.DateFormatUtils;
import org.apache.commons.lang.time.FastDateFormat;
import org.apache.log4j.Logger;
import org.apache.wicket.Page;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.PropertyListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

class UsersPanel extends Panel {

    private static final Logger logger = Logger.getLogger(UsersPanel.class);
    private final IModel<String> filterModel = Model.of("");
    TextField<String> filter;


    UsersPanel(String id, boolean showDiskUsage) {
        super(id);

        setOutputMarkupId(true);

        UserDao userDao = new UserDao(ThaleiaSession.get().getContextService().getContextSingleton());


        // Le filtre de recherche
        try {
            Form<?> form = new Form<Void>("form");
            add(form);
            filter = new TextField<>("filter", filterModel);

            form.add(filter);
            form.add(new AjaxButton("search") {
                @Override
                protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                }
            });
        } catch (Exception e) {
          logger.warn("Impossible de préparer le panel : " + e + "\n" + LogUtils.getStackTrace(e.getStackTrace()));
          logger.warn(LogUtils.getStackTrace(e.getStackTrace()));
          setResponsePage(ErrorPage.class);
        }

        // Préparation des lignes du tableau
        add(new PropertyListView<>("objects", new AbstractReadOnlyModel<List<User>>() {
            @Override
            public List<User> getObject() {
                // TODO ne présenter que les utilisateurs sur lesquels cet utilisateur a la visiblité
                String requestText = (String) filter.getDefaultModelObject();

                if (requestText.equals("")) {
                    return null;
                }
                return userDao.findUsersByPartialLogin(requestText);

            }
        }) {

            @Override
            protected void populateItem(final ListItem<User> item) {

                item.add(new Link<>("edit", item.getModel()) {
                    public void onClick() {
                        setResponsePage(new UserEditPage(item.getModel(), this.getPage().getClass()));
                    }
                }.addOrReplace(new Label("edit.text", new StringResourceModel("edit.text", this, null))));

                item.add(new Label("name"));
                item.add(new Label("login"));
                item.add(new Label("lastAccess", new AbstractReadOnlyModel<String>() {
                    @Override
                    public String getObject() {
                        // On formatte l'horodate d'expiration, si elle  existe
                        if (item.getModelObject().getLastAccess() == null) {
                            return "";
                        } else {
                            FastDateFormat format = DateFormatUtils.ISO_DATETIME_FORMAT;
                            return format.format(item.getModelObject().getLastAccess());
                        }
                    }

                }));
                item.add(new Label("expiration", new AbstractReadOnlyModel<String>() {
                    @Override
                    public String getObject() {
                        // On formatte l'horodate d'expiration, si elle existe
                        if (item.getModelObject().getExpiration() == null) {
                            return "";
                        } else {
                            FastDateFormat format = DateFormatUtils.ISO_DATETIME_FORMAT;
                            return format.format(item.getModelObject().getExpiration());
                        }
                    }

                }));
                item.add(new Label("domain.name"));

                item.add(new Label("licence", new AbstractReadOnlyModel<String>() {
                    @Override
                    public String getObject() {
                        try {
                            return ThaleiaSession.get().getLicenceService().getValidLicencesNames(item.getModelObject(),
                                    ThaleiaSession.get().getLocale(), " - ");
                        } catch (DetailedException e) {
                            logger.warn(e);
                            return "";
                        }
                    }
                }));

                // L'icone indiquant l'absence de licence
                item.add(new WebMarkupContainer("noLicenceIcon") {
                    @Override
                    public boolean isVisible() {
                        try {
                            return !ThaleiaSession.get().getLicenceService().isUserValid(item.getModelObject(), false);
                        } catch (DetailedException e) {
                            logger.warn(e);
                            return false;
                        }
                    }
                });

                // Le nombre de contenus dont il est auteur de la dernière version
                item.add(new Label("generatedContents", new AbstractReadOnlyModel<String>() {
                    @Override
                    public String getObject() {
                        try {
                            Date start = Calendar.getInstance().getTime();
                            String modules =
                                    Integer.toString(
                                            ThaleiaSession.get().getContentService().getModulesVersionsWhereAuthor(
                                                    item.getModelObject()).size());
                            logger.debug(
                                    "Nombre de modules dont " + item.getModelObject().getLogin() + " est l'auteur : "
                                            + modules + " (calcul=" + (Calendar.getInstance().getTime().getTime() - start.getTime()) + "ms)");
                            return modules;
                        } catch (Exception e) {
                            return "-";
                        }
                    }
                }));

                final long userSize = getUserSize(item.getModelObject(), showDiskUsage);

                item.add(new Label("size", new AbstractReadOnlyModel<String>() {
                    @Override
                    public String getObject() {
                        if (showDiskUsage) {
                            // Le poids total utilisé par les publications de cet  utilisateur.
                            // Formatage pour lecture plus facile.
                            return FileUtils.byteCountToDisplaySize(userSize);
                        } else {
                            return "-";
                        }
                    }
                }));

                item.add(new Label("totalSize", new AbstractReadOnlyModel<String>() {

                    @Override
                    public String getObject() {
                        if (showDiskUsage) {
                            // Le poids total utilisé par les publications de cet utilisateur + celui des contenus de  son domaine
                            long domainSize;
                            Date start = Calendar.getInstance().getTime();
                            domainSize = ThaleiaSession.get().getContentService().getDiskUsed(
                                    item.getModelObject().getDomain());
                            logger.debug(
                                    "Espace disque utilisé par le domaine de " + item.getModelObject().getLogin() + " : "
                                            + domainSize + " (calcul=" + (Calendar.getInstance().getTime().getTime() - start.getTime()) + "ms)");
                            // Formatage pour lecture plus facile.
                            return FileUtils.byteCountToDisplaySize(userSize + domainSize);
                        } else {
                            return "-";
                        }
                    }
                }));

                item.add(new Label("publicationCredits", new AbstractReadOnlyModel<String>() {
                    @Override
                    public String getObject() {
                        // Le nombre de crédits de publication consommés
                        try {
                            Date start = Calendar.getInstance().getTime();
                            String registrations =
                                    Integer.toString(
                                            ThaleiaSession.get().getLicenceService().countExistingRegistrations(
                                                    item.getModelObject(), false));
                            logger.debug(
                                    "Nombre de crédits de publication consommés par " + item.getModelObject().getLogin() + " : "
                                            + registrations + " (calcul=" + (Calendar.getInstance().getTime().getTime() - start.getTime()) + "ms)");
                            return registrations;
                        } catch (DetailedException e) {
                            logger.warn("Impossible d'obtenir le nombre de crédits de publication consommés : " + e);
                            return "";
                        }
                    }
                }));

                // La liste des domaines sur lesquels l'utilisateur a les droits d'accès, dans une popup.
                final ModalWindow modal = new ModalWindow("accessibleDomainsModal");
                modal.setContent(new Label(modal.getContentId(), new AbstractReadOnlyModel<String>() {
                    @Override
                    public String getObject() {
                        StringBuilder result = new StringBuilder();
                        List<Domain> accessibles;
                        accessibles = ThaleiaSession.get().getUserService().getAuthorizedDomains(item.getModelObject());
                        for (Domain accessible : accessibles) {
                            result.append("\n").append(accessible.getName());
                        }
                        return result.toString();
                    }
                }));
                item.add(modal);

                // Le bouton pour afficher la popup modale
                @SuppressWarnings("rawtypes")
                AjaxLink showModalButton = new AjaxLink("showAccessibleDomainsModal") {
                    @Override
                    public void onClick(AjaxRequestTarget ajaxRequestTarget) {
                        modal.show(ajaxRequestTarget);
                    }
                };
                item.add(showModalButton);

                // L'icone indiquant que l'expiration a échu
                item.add(new WebMarkupContainer("expirationIcon") {
                    @Override
                    public boolean isVisible() {
                        return item.getModelObject().getExpiration() != null
                                && item.getModelObject().getExpiration().before(Calendar.getInstance().getTime());
                    }
                });

                item.add(new ConfirmationLink<Void>("delete", new StringResourceModel("delete.confirm", this, null,
                        new Object[]{userDao.getDisplayName(item.getModelObject(),
                                ThaleiaSession.get().getLocale())})) {
                    @Override
                    public boolean isVisible() {
                        try {
                            return userDao.canBeDeleted(item.getModelObject());
                        } catch (Exception e) {
                            logger.warn("Erreur durant l'analyse d'un objet : " + e);
                            return false;
                        }
                    }

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        try {
                            // Suppression de l'objet
                            ThaleiaSession.get().getUserService().deleteUserProfileData(item.getModelObject());

                        } catch (DetailedException e) {
                            StringResourceModel errorMessageModel = new StringResourceModel("delete.error",
                                    UsersPanel.this, item.getModel());
                            ThaleiaSession.get().error(errorMessageModel.getString());

                            // On journalise le détail technique.
                            logger.warn("Impossible de supprimer l'objet de type '"
                                    + item.getModelObject().getClass().getName() + "' : " + e.toString());
                        }
                        // On recharge la page.
                        setResponsePage(target.getPage());
                    }
                }.addOrReplace(new Label("delete.text", new StringResourceModel("delete.text", this, null))));

                // Le lien "Supprimer les anciennes versions des contenus"
                item.add(new ConfirmationLink<Void>("cleanVersions", new StringResourceModel("cleanVersions.confirm",
                        this, null, new Object[]{userDao.getDisplayName(item.getModelObject(),
                        ThaleiaSession.get().getLocale())})) {
                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        try {
                            ThaleiaSession.get().getContentService().deleteOldVersions(item.getModelObject());
                            item.getModelObject().getObjectContext().commitChanges();
                            info(new StringResourceModel("cleanVersions.ok", UsersPanel.this, null,
                                    new Object[]{userDao.getDisplayName(item.getModelObject(),
                                            ThaleiaSession.get().getLocale())}).getString());
                        } catch (DetailedException e) {
                            logger.warn("Erreur lors de la suppression des anciennes versions des contenus de "
                                    + item.getModelObject() + " : " + e);
                            error(new StringResourceModel("cleanVersions.error", UsersPanel.this, null,
                                    new Object[]{userDao.getDisplayName(item.getModelObject(),
                                            ThaleiaSession.get().getLocale())}).getString());
                        }
                        setResponsePage(target.getPage());
                    }
                });
            }

            private long getUserSize(User user, boolean showDiskUsage) {
                if (showDiskUsage) {
                    try {
                        Date start = Calendar.getInstance().getTime();
                        long diskUsed = ThaleiaSession.get().getPublicationService().getDiskUsed(user);
                        logger.debug("Espace disque utilisé par " + user.getLogin() + " : " + diskUsed + " (calcul=" + (
                                Calendar.getInstance().getTime().getTime() - start.getTime()) + "ms)");
                        return diskUsed;
                    } catch (DetailedException e) {
                        logger.warn("Impossible d'obtenir la taille des publications : " + e);
                        return 0;
                    }
                } else {
                    return -1;
                }
            }
        }.setOutputMarkupId(true));

        // Les liste des liens pour créer un compte avec une licence (un lien par licence)
        RepeatingView createAccountWithLicenceLinks = new RepeatingView("createAccountWithLicenceLinks");
        add(createAccountWithLicenceLinks);
        LicenceDao licenceDao = ThaleiaApplication.get().getLicenceDao();
        for (Licence licence : licenceDao.find()) {
            WebMarkupContainer createAccountWithLicenceItem = new WebMarkupContainer(
                    createAccountWithLicenceLinks.newChildId());
            Link<Page> link = new Link<>("createAccountWithLicenceLink") {
                @Override
                public void onClick() {
                    setResponsePage(new CreateAccountWithLicencePage("", licence) {
                        // On renvoie sur la page admin
                        @Override
                        protected void onOut() {
                            setResponsePage(AdminPage.class);
                        }
                    });
                }
            };
            createAccountWithLicenceItem.add(link);
            Label linkLabel = new Label("createAccountWithLicenceText", licence.getName());
            link.add(linkLabel);
            createAccountWithLicenceLinks.add(createAccountWithLicenceItem);
        }

        // les licences existantes
        add(new LicencesPanel("licencesPanel"));
    }
}
