package fr.solunea.thaleia.webapp.pages.admin;

import fr.solunea.thaleia.model.ContentProperty;
import fr.solunea.thaleia.model.dao.ContentPropertyDao;
import fr.solunea.thaleia.webapp.pages.admin.contenttype.ContentTypesPanel;
import fr.solunea.thaleia.webapp.pages.admin.property.ContentPropertiesPanel;
import fr.solunea.thaleia.webapp.security.ThaleiaSession;
import org.apache.wicket.authroles.authorization.strategies.role.annotations.AuthorizeInstantiation;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.AbstractReadOnlyModel;

import java.util.List;

@SuppressWarnings("serial")
@AuthorizeInstantiation("admin")
public class AdminSectionProperties extends Panel {

    public AdminSectionProperties(String id) {
        super(id);

        add(new ContentPropertiesPanel("propertiesPanel",
                new AbstractReadOnlyModel<>() {
                    @Override
                    public List<ContentProperty> getObject() {
                        ContentPropertyDao contentPropertyDao = new ContentPropertyDao(ThaleiaSession.get().getContextService().getContextSingleton());
                        return contentPropertyDao.find();
                    }
                }, true, true, true));

        add(new ContentTypesPanel("contentTypesPanel"));

    }
}
