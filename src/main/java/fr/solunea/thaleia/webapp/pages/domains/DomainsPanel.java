package fr.solunea.thaleia.webapp.pages.domains;

import fr.solunea.thaleia.model.Domain;
import fr.solunea.thaleia.model.dao.DomainDao;
import fr.solunea.thaleia.model.dao.DomainRightDao;
import fr.solunea.thaleia.utils.DetailedException;
import fr.solunea.thaleia.utils.LogUtils;
import fr.solunea.thaleia.webapp.pages.ErrorPage;
import fr.solunea.thaleia.webapp.panels.confirmation.ConfirmationLink;
import fr.solunea.thaleia.webapp.security.ThaleiaSession;
import org.apache.cayenne.ObjectContext;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.apache.wicket.Page;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.authorization.Action;
import org.apache.wicket.authroles.authorization.strategies.role.annotations.AuthorizeAction;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.PropertyListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

@AuthorizeAction(action = Action.RENDER, roles = {"admin"})
public class DomainsPanel extends Panel {

    private static final Logger logger = Logger.getLogger(DomainsPanel.class);
    private final IModel<String> filterModel = Model.of("");
    TextField<String> filter;

    public DomainsPanel(String id, boolean showDiskUsage) {
        super(id);

        setOutputMarkupId(true);

        final ObjectContext context = ThaleiaSession.get().getContextService().getNewContext();
        DomainDao domainDao = new DomainDao(context);
        DomainRightDao domainRightDao = new DomainRightDao(context);


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


        add(new PropertyListView<>("objects", new AbstractReadOnlyModel<List<Domain>>() {
            @Override
            public List<Domain> getObject() {
                String requestText = (String) filter.getDefaultModelObject();

                if (requestText.equals("")) {
                    return null;
                }
                // On présente tous les domaines, car on suppose que l'utilisateur est admin.
                return domainDao.findDomainsByPartialName(requestText);
            }
        }) {

            @Override
            protected void populateItem(final ListItem<Domain> item) {

                item.add(new Link<>("edit", item.getModel()) {
                    public void onClick() {
                        setResponsePage(new DomainEditPage(item.getModel()));
                    }
                });

                item.add(new Label("name"));

                item.add(new Label("parent.name"));

                item.add(new Label("accessibles", new AbstractReadOnlyModel<String>() {

                    @Override
                    public String getObject() {
                        // La liste des domaines sur lesquels le domaine
                        // a les droits d'accès
                        StringBuilder result = new StringBuilder();

                        List<Domain> accessibles = domainRightDao.getAccessibleDomains(item.getModelObject());
                        for (Domain accessible : accessibles) {
                            result.append("\n").append(accessible.getName());
                        }

                        return result.toString();
                    }
                }));

                item.add(new Label("size", new AbstractReadOnlyModel<String>() {

                    @Override
                    public String getObject() {
                        if (showDiskUsage) {
                            // Le poids total utilisé par les contenus de ce
                            // domaine.
                            long size;
                            Date start = Calendar.getInstance().getTime();
                            size = ThaleiaSession.get().getContentService().getDiskUsed(item.getModelObject());
                            logger.debug("Espace disque utilisé par le domaine " + item.getModelObject().getName() + " : " + size + " (calcul=" + (Calendar.getInstance().getTime().getTime() - start.getTime()) + "ms)");
                            // Formatage pour lecture plus facile.
                            return FileUtils.byteCountToDisplaySize(size);
                        } else {
                            return "-";
                        }
                    }
                }));

                item.add(new ConfirmationLink<Void>("delete", new StringResourceModel("delete.confirm", this, null, new Object[]{domainDao.getDisplayName(item.getModelObject(), ThaleiaSession.get().getLocale())})) {

                    @Override
                    public boolean isVisible() {
                        try {
                            return domainDao.canBeDeleted(item.getModelObject());
                        } catch (Exception e) {
                            logger.warn("Erreur durant l'analyse d'un objet : " + e);
                            return false;
                        }
                    }

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        try {
                            // Suppression de l'objet
                            domainDao.delete(item.getModelObject());

                        } catch (DetailedException e) {
                            StringResourceModel errorMessageModel = new StringResourceModel("delete.error", DomainsPanel.this, item.getModel());
                            ThaleiaSession.get().error(errorMessageModel.getString());

                            // On journalise le détail technique.
                            logger.warn("Impossible de supprimer l'objet de type '" + item.getModelObject().getClass().getName() + "' : " + e);
                        }
                        // On recharge la page.
                        setResponsePage(target.getPage());
                    }
                });
            }
        }.setOutputMarkupId(true));

        // Le lien "Nouveau".
        add(new Link<Page>("new") {
            @Override
            public void onClick() {
                setResponsePage(DomainEditPage.class);
            }
        });
    }
}
