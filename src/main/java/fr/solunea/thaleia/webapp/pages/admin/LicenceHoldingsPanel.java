package fr.solunea.thaleia.webapp.pages.admin;

import fr.solunea.thaleia.model.Licence;
import fr.solunea.thaleia.model.LicenceHolding;
import fr.solunea.thaleia.model.User;
import fr.solunea.thaleia.model.dao.LicenceDao;
import fr.solunea.thaleia.model.dao.LicenceHoldingDao;
import fr.solunea.thaleia.service.LicenceService;
import fr.solunea.thaleia.utils.DateUtils;
import fr.solunea.thaleia.utils.DetailedException;
import fr.solunea.thaleia.utils.LogUtils;
import fr.solunea.thaleia.webapp.panels.ThaleiaFeedbackPanel;
import fr.solunea.thaleia.webapp.security.ThaleiaSession;
import org.apache.log4j.Logger;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.PropertyListView;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.*;

import java.util.List;
import java.util.Locale;

@SuppressWarnings("serial")
public class LicenceHoldingsPanel extends Panel {

    private static final Logger logger = Logger.getLogger(LicenceHoldingsPanel.class);

    public LicenceHoldingsPanel(String id, final IModel<User> model) {
        super(id, model);

        // On obtient un objet User dans un contexte Cayenne spécifique à ce panel, pour édition sans effets de bord du reste des objets
        setDefaultModel(new CompoundPropertyModel<>(Model.of(ThaleiaSession.get().getContextService().getObjectInNewContext(model.getObject()))));

        final LicenceService licenceService = getLicenceService();
        LicenceHoldingDao licenceHoldingDao = new LicenceHoldingDao(((User) getDefaultModelObject()).getObjectContext());
        LicenceDao licenceDao = new LicenceDao(((User) getDefaultModelObject()).getObjectContext());

        // On ne place pas la locale dans le contexte d'édition, car elle n'est utilisée que pour la présentation des messages de ce panel.
        final Locale locale = ThaleiaSession.get().getLocale();

        final WebMarkupContainer tableContainer = new WebMarkupContainer("tableContainer");
        add(tableContainer.setOutputMarkupId(true));

        // Le panneau des messages
        final ThaleiaFeedbackPanel feedbackPanel = new ThaleiaFeedbackPanel("feedback");
        feedbackPanel.setOutputMarkupId(true);
        add(feedbackPanel);

        // Préparation des lignes du tableau
        PropertyListView<LicenceHolding> table = new PropertyListView<>(
                "objects", new AbstractReadOnlyModel<List<LicenceHolding>>() {
            @Override
            public List<LicenceHolding> getObject() {
                return licenceHoldingDao.getHoldedBy((User) LicenceHoldingsPanel.this.getDefaultModelObject(), false);
            }
        }) {

            @Override
            protected void populateItem(final ListItem<LicenceHolding> item) {

                assert licenceService != null;
                item.add(new Label("name", licenceService.getDisplayName(item.getModelObject().getLicence(), locale)));
                item.add(new Label("start", DateUtils.formatDate(item.getModelObject().getStartDate(), locale)));
                item.add(new Label("end", DateUtils.formatDate(item.getModelObject().getEndDate(), locale)));
                item.add(new CheckBox("canceled") {
                    @Override
                    public void onSelectionChanged(Boolean newSelection) {
                        try {
                            licenceHoldingDao.save(item.getModelObject(), true);
                        } catch (DetailedException e) {
                            logger.warn("Impossible d'enregistrer la modification de l'état de publication : " + e);
                            logger.warn(LogUtils.getStackTrace(e.getStackTrace()));
                        }
                    }

                    @Override
                    protected boolean wantOnSelectionChangedNotifications() {
                        return true;
                    }
                });
            }
        };
        tableContainer.add(table.setOutputMarkupId(true));

        // Bouton d'ajout de licence
        @SuppressWarnings("unchecked") final DropDownChoice<Licence> licence = (DropDownChoice<Licence>) new DropDownChoice<>("licence", Model.of((Licence) null),
                new LoadableDetachableModel<List<Licence>>() {
                    @Override
                    protected List<Licence> load() {
                        return licenceDao.find();
                    }
                }, new ChoiceRenderer<>() {
            @Override
            public Object getDisplayValue(Licence object) {
                try {
                    return ThaleiaSession.get().getLicenceService().getDisplayName(object, locale) + " " + formatDuration(object, locale);
                } catch (DetailedException e) {
                    logger.warn(e);
                    return "";
                }
            }

            private String formatDuration(Licence licence, Locale locale) {
                if (locale.getLanguage().equals("en")) {
                    return "(" + licence.getLicenceDurationDays() + " days)";
                } else {
                    return "(" + licence.getLicenceDurationDays() + " jours)";
                }
            }

            @Override
            public String getIdValue(Licence object, int index) {
                return Integer.toString(licenceDao.getPK(object));
            }
        }).setOutputMarkupId(true);
        Form<Void> form = new Form<>("newLicenceForm") {
            @Override
            public void onError() {
                logger.debug("Erreur !");
                super.onError();
            }
        };
        form.add(new AjaxSubmitLink("addButton") {
            @Override
            protected void onError(AjaxRequestTarget target, Form<?> form) {
                target.add(feedbackPanel);
            }

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form form) {
                try {
                    // Attribution de la licence sélectionnée
                    LicenceHolding holding = licenceHoldingDao.attributeLicence(
                            ((User) LicenceHoldingsPanel.this.getDefaultModelObject()),
                            licence.getModelObject().getSku(),
                            LicenceHoldingsPanel.class.getName());
                    licenceHoldingDao.save(holding, true);
                } catch (DetailedException e) {
                    logger.warn(e);
                    error(e);
                }
                licence.setModelObject(null);
                target.add(tableContainer);
                target.add(form);
                target.add(feedbackPanel);
                target.add(licence);
            }
        });
        form.setOutputMarkupId(true);
        form.add(licence.setRequired(true));
        add(form);
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
