package fr.solunea.thaleia.webapp.pages.admin.usage;

import fr.solunea.thaleia.model.Content;
import fr.solunea.thaleia.model.ContentType;
import fr.solunea.thaleia.model.User;
import fr.solunea.thaleia.model.dao.ContentTypeDao;
import fr.solunea.thaleia.model.dao.UserDao;
import fr.solunea.thaleia.webapp.ThaleiaApplication;
import fr.solunea.thaleia.webapp.security.ThaleiaSession;
import org.apache.commons.lang.time.DateFormatUtils;
import org.apache.commons.lang.time.FastDateFormat;
import org.apache.log4j.Logger;
import org.apache.wicket.authroles.authorization.strategies.role.annotations.AuthorizeInstantiation;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.PropertyListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.AbstractReadOnlyModel;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

@AuthorizeInstantiation("admin")
public class UsersUsagePanel extends Panel {

    private static final Logger logger = Logger.getLogger(UsersUsagePanel.class);


    public UsersUsagePanel(String id) {
        super(id);

        UserDao userDao = new UserDao(ThaleiaSession.get().getContextService().getContextSingleton());
        ContentTypeDao contentTypeDao = new ContentTypeDao(ThaleiaSession.get().getContextService().getContextSingleton());

        // Préparation des lignes du tableau
        add(new PropertyListView<>("objects", new AbstractReadOnlyModel<List<User>>() {
            @Override
            public List<User> getObject() {
                // Tous les users, car on suppose que seul un admin peut accéder à ce tableau.
                return userDao.find();
            }
        }) {

            @Override
            protected void populateItem(final ListItem<User> item) {

                item.add(new Label("login"));

                // La date de dernier accès
                item.add(new Label("lastAccess", new AbstractReadOnlyModel<String>() {
                    @Override
                    public String getObject() {
                        // On formatte l'horodate, si elle existe
                        if (item.getModelObject().getLastAccess() == null) {
                            return "";
                        } else {
                            FastDateFormat format = DateFormatUtils.ISO_DATETIME_FORMAT;
                            return format.format(item.getModelObject().getLastAccess());
                        }
                    }
                }));

                // Le nombre de contenus créés
                addLabel(item, "cannelleContents", "module_cannelle");
                addLabel(item, "moustacheContents", "video");
                addLabel(item, "dialogueContents", "Action");

                // Le nombre de connexions
                item.add(new Label("connections", new AbstractReadOnlyModel<String>() {
                    @Override
                    public String getObject() {
                        try {
                            Date start = new Date(0);
                            Date end = Calendar.getInstance().getTime();
                            List<String> username = new ArrayList<>();
                            username.add(item.getModelObject().getLogin());
                            return String.valueOf(ThaleiaApplication.get().getStatDataDao().countIdentifications
                                    (username, true, start, end));
                        } catch (Exception e) {
                            logger.warn(e);
                        }
                        return "-";
                    }
                }));
            }

            private void addLabel(ListItem<User> item, String id, final String contentTypeName) {
                item.add(new Label(id, new AbstractReadOnlyModel<String>() {
                    @Override
                    public String getObject() {
                        ContentType contentType = contentTypeDao.findByName(contentTypeName);
                        if (contentType != null) {
                            List<Content> modules = ThaleiaSession.get().getContentService().getContentsCreatedBy
                                    (item.getModelObject(), contentType);
                            return String.valueOf(modules.size());
                        }
                        return "-";
                    }
                }));
            }
        });

    }
}
