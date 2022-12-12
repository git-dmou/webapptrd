package fr.solunea.thaleia.webapp.pages.admin;

import fr.solunea.thaleia.model.Licence;
import fr.solunea.thaleia.model.dao.LicenceDao;
import fr.solunea.thaleia.service.LicenceService;
import fr.solunea.thaleia.utils.DetailedException;
import fr.solunea.thaleia.webapp.security.ThaleiaSession;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.PropertyListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.AbstractReadOnlyModel;

import java.util.List;
import java.util.Locale;

@SuppressWarnings("serial")
public class LicencesPanel extends Panel {

    private static final Logger logger = Logger.getLogger(LicencesPanel.class);

    public LicencesPanel(String id) {
        super(id);

        LicenceService licenceService = getLicenceService();
        Locale locale = ThaleiaSession.get().getLocale();

        final WebMarkupContainer tableContainer = new WebMarkupContainer("table");
        add(tableContainer.setOutputMarkupId(true));

        // Préparation des lignes du tableau
        PropertyListView<Licence> table = new PropertyListView<>("objects", new AbstractReadOnlyModel<List<Licence>>() {
            @Override
            public List<Licence> getObject() {
                LicenceDao licenceDao = new LicenceDao(ThaleiaSession.get().getContextService().getContextSingleton());
                return licenceDao.find();
            }
        }) {

            @Override
            protected void populateItem(final ListItem<Licence> item) {

                assert licenceService != null;
                item.add(new Label("name", licenceService.getDisplayName(item.getModelObject(), locale)));

                item.add(new Label("includedPublicationCredits"));
                item.add(new Label("maxPublications"));
                item.add(new Label("maxSizeMo", FileUtils.byteCountToDisplaySize(item.getModelObject().getMaxSizeBytes())));
                item.add(new Label("licenceDurationDays"));
                item.add(new Label("catalogPriceEuro"));
                item.add(new Label("isDemo"));
            }
        };
        tableContainer.add(table.setOutputMarkupId(true));

    }

    private LicenceService getLicenceService() {
        try {
            return ThaleiaSession.get().getLicenceService();
        } catch (DetailedException e) {
            logger.warn(e);
            return null;
        }
    }

}
