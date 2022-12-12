package fr.solunea.thaleia.webapp.pages.allocations;

import fr.solunea.thaleia.model.Content;
import fr.solunea.thaleia.model.ContentVersion;
import fr.solunea.thaleia.model.dao.ContentDao;
import fr.solunea.thaleia.model.dao.ContentVersionDao;
import fr.solunea.thaleia.utils.DetailedException;
import fr.solunea.thaleia.webapp.security.ThaleiaSession;
import fr.solunea.thaleia.webapp.utils.BootstrapPalette;
import fr.solunea.thaleia.webapp.utils.MessagesUtils;
import org.apache.log4j.Logger;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.extensions.markup.html.form.palette.Palette;
import org.apache.wicket.extensions.markup.html.form.palette.component.Recorder;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.util.ListModel;

import java.util.List;

@SuppressWarnings("serial")
public abstract class AllocationsPanel extends Panel {

    private static final Logger logger = Logger.getLogger(AllocationsPanel.class);

    /**
     * @param model la version du module dont on veut gérer les contenus fils.
     */
    public AllocationsPanel(String id, final IModel<ContentVersion> model, final boolean readOnly) {
        super(id, model);

        Form<?> form = new Form<Void>("form") {
            @Override
            protected void onSubmit() {
                logger.debug("form.onSubmit()");
            }
        };

        add(form);

        IChoiceRenderer<Content> renderer = new ChoiceRenderer<>("lastVersion.contentIdentifier", "lastVersion.contentIdentifier");

        // Les contenus ordonnés pour ce module.
        ContentDao contentDao = new ContentDao(ThaleiaSession.get().getContextService().getContextSingleton());
        final IModel<List<Content>> selection = new ListModel<>(contentDao.getModuleChilds(model.getObject()));

        Palette<Content> palette;
        palette = new BootstrapPalette<>("palette", selection, new ListModel<>(
                ThaleiaSession.get().getContentService().getContents(
                        ThaleiaSession.get().getAuthenticatedUser(), false)), renderer, 10, true) {
            @Override
            public boolean isEnabled() {
                return !readOnly;
            }

            @Override
            protected Recorder<Content> newRecorderComponent() {
                Recorder<Content> recorder = super.newRecorderComponent();
                recorder.add(new AjaxFormComponentUpdatingBehavior(
                        "onchange") {

                    @Override
                    protected void onUpdate(AjaxRequestTarget target) {
                        // On enregistre la nouvelle allocation de contenus pour  ce module
                        try {
                            ThaleiaSession.get().getContentService().setAllocations(model.getObject(), selection.getObject());
                            // On recharge le modèle, pour être sûr que les modifications sont bien prises en compte dans ce contexte
                            model.setObject(new ContentVersionDao(model.getObject().getObjectContext()).get(model.getObject().getObjectId()));
                        } catch (DetailedException e) {
                            AllocationsPanel.this.error(MessagesUtils.getLocalizedMessage("error", AllocationsPanel.class, null));
                            logger.warn("Impossible de mettre à jour les allocations : " + e.toString());
                        }
                        // On remonte l'information de mise à jour
                        onPropertyChanged(target);
                    }
                });
                return recorder;
            }
        };
        form.add(palette);
    }

    /**
     * Méthode appelée lorsque la valeur d'un champ a été mise à jour.
     */
    protected abstract void onPropertyChanged(AjaxRequestTarget target);
}
