package fr.solunea.thaleia.webapp.pages.content;

import fr.solunea.thaleia.model.*;
import fr.solunea.thaleia.model.dao.*;
import fr.solunea.thaleia.utils.DetailedException;
import fr.solunea.thaleia.utils.LogUtils;
import fr.solunea.thaleia.webapp.ThaleiaApplication;
import fr.solunea.thaleia.webapp.pages.allocations.AllocationsPanel;
import fr.solunea.thaleia.webapp.panels.PanelUtils;
import fr.solunea.thaleia.webapp.panels.ThaleiaFeedbackPanel;
import fr.solunea.thaleia.webapp.panels.confirmation.ConfirmationLink;
import fr.solunea.thaleia.webapp.security.ThaleiaSession;
import fr.solunea.thaleia.webapp.utils.CSSClassRemover;
import org.apache.cayenne.ObjectContext;
import org.apache.log4j.Logger;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.OnChangeAjaxBehavior;
import org.apache.wicket.extensions.ajax.markup.html.IndicatingAjaxLink;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.EmptyPanel;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.*;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("serial")
public abstract class ContentEditPanel extends Panel {

    private static final Logger logger = Logger.getLogger(Content.class);

    protected ThaleiaFeedbackPanel feedbackPanel;
    protected boolean readOnly;
    private Component saveButton;

    protected ContentEditPanel(String id, final IModel<ContentVersion> model, final boolean readOnly) {
        super(id, model);

        this.readOnly = readOnly;

        setDefaultModel(new CompoundPropertyModel<>(model));

        DomainDao domainDao = new DomainDao(model.getObject().getObjectContext());

        feedbackPanel = new ThaleiaFeedbackPanel("feedback");
        feedbackPanel.setOutputMarkupId(true);
        add(feedbackPanel);

        final Form<ContentVersion> form = new Form<>("form", model);
        form.setOutputMarkupId(true);

        // Les domaines propos??s ?? l'utilisateur dans le s??lecteur de domaine.
        final LoadableDetachableModel<List<Domain>> selectableDomains = new LoadableDetachableModel<>() {
            @Override
            protected List<Domain> load() {
                // On ne pr??sente que la liste des domaines permis pour  cet utilisateur
                List<Domain> authorizedDomains = ThaleiaSession.get().getUserService().getAuthorizedDomains(ThaleiaSession.get()
                        .getAuthenticatedUser());
                // On place ces domaines dans le contexte de la contentVersion
                List<Domain> result = new ArrayList<>();
                for (Domain domain : authorizedDomains) {
                    result.add(domainDao.get(domain.getObjectId()));
                }
                return result;
            }
        };

        // Les libell??s
        form.add(new Label("idLabel", new StringResourceModel("idLabel", this, null)));
        form.add(new Label("domainLabel", new StringResourceModel("domainLabel", this, null)) {
            @Override
            public boolean isVisible() {
                // On ne pr??sente pas ce label si la liste des domaines
                // disponibles ne contient qu'une valeur.
                return selectableDomains.getObject().size() > 1;
            }
        });

        // Le mod??le de la langue d'??dition des propri??t??s
        // Par d??faut la locale de l'IHM
        final IModel<Locale> languageModel = Model.of(new LocaleDao(model.getObject().getObjectContext()).getLocale(ThaleiaSession.get().getLocale()));

        // Le panneau de gestion des propri??t??s
        final Component contentPropertiesEditPanel = new ContentPropertiesEditPanel("propertiesEditPanel", model,
                languageModel, readOnly) {
            @Override
            protected void onPropertyChanged(AjaxRequestTarget target) {
                super.onPropertyChanged(target);
                // Si on peut enregistrer, alors on rend le bouton Save actif.
                onFieldChange(target);
            }
        }.setOutputMarkupId(true);
        form.add(contentPropertiesEditPanel);

        try {
            // Le nom de cette version de contenu
            form.add(new TextField<String>("contentIdentifier") {
                @Override
                public boolean isEnabled() {
                    return !readOnly;
                }
            }.add(new OnChangeAjaxBehavior() {
                @Override
                protected void onUpdate(AjaxRequestTarget target) {
                    logger.debug("L'identifiant a ??t?? chang??.");
                    // Si on peut enregistrer, alors on rend le bouton Save
                    // actif.
                    onFieldChange(target);
                }
            }));
            // On n'utilise pas de validator, car on veut impl??menter notre
            // propre m??canisme de validation, en jouant ??galement sur
            // l'activation du bouton Enregistrer.

        } catch (Exception e) {
            logger.warn("Impossible de r??cup??rer la derni??re version de ce contenu '" + model.getObject() + "' :" + e);
            form.add(new EmptyPanel("lastVersion.contentIdentifier"));
        }

        // S??lecteur de domaine
        final DropDownChoice<Domain> domainChoice = new DropDownChoice<Domain>("content.domain", selectableDomains,
                new ChoiceRenderer<>() {
                    @Override
                    public Object getDisplayValue(Domain object) {
                        return domainDao.getDisplayName(object, ThaleiaSession.get().getLocale());
                    }

                    @Override
                    public String getIdValue(Domain object, int index) {
                        return Integer.toString(domainDao.getPK(object));
                    }
                }) {
            @Override
            public boolean isVisible() {
                // On ne pr??sente pas ce s??lecteur si la liste qu'il pr??sente ne
                // contient qu'une valeur.
                return selectableDomains.getObject().size() > 1;
            }

            @Override
            public boolean isEnabled() {
                return !readOnly;
            }
        };
        form.add(domainChoice.setRequired(true).add(new AjaxEventBehavior("onchange") {
            @SuppressWarnings("unchecked")
            @Override
            protected void onEvent(AjaxRequestTarget target) {

                // On remonte l'??v??nement de changement, afin qu'il soit
                // pris en compte, et que le nouveau domaine soit
                // associ?? au Content.
                ((DropDownChoice<Domain>) getComponent()).onSelectionChanged();

                // On active le bouton Enregistrer, si l'??tat des
                // informations le permet
                onFieldChange(target);
            }
        }));

        // Les boutons enregistrer et annuler
        saveButton = getSaveButton(model).setOutputMarkupId(true).setEnabled(false);
        // Bootstrap : on donne par d??faut l'aspect "disabled"
        saveButton.add(AttributeModifier.append("class", "disabled"));
        form.add(saveButton);
        Component cancelButton = getCancelButton(model).setOutputMarkupId(true);
        form.add(cancelButton);

        // Le bouton "supprimer"
        form.add(getDeleteButton(model));

        // Si on ??dite un module, alors on pr??sente un panneau pour g??rer
        // ses contenus
        if (model.getObject().getContent().getIsModule()) {
            form.add(new AllocationsPanel("allocationsPanel", Model.of(model.getObject()), readOnly) {
                @Override
                protected void onPropertyChanged(AjaxRequestTarget target) {
                    onFieldChange(target);
                }
            }).setOutputMarkupId(true);

        } else {
            form.add(new EmptyPanel("allocationsPanel"));
        }
        add(form);
    }

    /**
     * @return true si le bouton Enregistrer peut ??tre activ??.
     */
    private boolean isSaveButtonCanBeActivated() {
        logger.debug("V??rification de l'activation du bouton Enregistrer.");

        if (readOnly) {
            return false;
        }

        try {
            // On v??rifie que l'identifiant du contenu n'est pas nul
            if (((ContentVersion) getDefaultModelObject()).getContentIdentifier() == null
                    || ((ContentVersion) getDefaultModelObject()).getContentIdentifier().isEmpty()) {
                return false;
            }

            // On v??rifie que l'identifiant du contenu n'est pas d??j?? utilis??
            Content content = ((ContentVersion) getDefaultModelObject()).getContent();
            String id = ((ContentVersion) getDefaultModelObject()).getContentIdentifier();
            try {
                if (ThaleiaSession.get().getContentService().isContentVersionNameExists(id, content, content.getDomain())) {
                    // On ajoute un message
                    StringResourceModel messageModel = new StringResourceModel("content.name.exists", ContentEditPanel.this, null);
                    error(messageModel.getString());
                    // On n'active pas le bouton Enregistrer
                    return false;
                }
            } catch (DetailedException e) {
                throw new DetailedException(e).addMessage(
                        "Erreur durant la v??rification de l'unicit?? de l'identifiant du contenu " + content + ".");
            }

            // Pas de probl??me pour enregistrer
            return true;

        } catch (Exception e) {
            logger.warn(e);
            logger.warn(LogUtils.getStackTrace(e.getStackTrace()));
            return false;
        }
    }

    /**
     * M??thode ?? appeler si une valeur a ??t?? modifi??e dans un champ par
     * l'utilisateur, afin d'activer le bouton "Enregistrer".
     */
    @SuppressWarnings("unchecked")
    private void onFieldChange(AjaxRequestTarget target) {
        // Doit-on activer le bouton Enregistrer ?
        boolean isSaveButtonCanBeActivated = isSaveButtonCanBeActivated();
        logger.debug("Le bouton Enregistrer doit-il ??tre activ?? ? " + isSaveButtonCanBeActivated);

        // On active le bouton Enregistrer
        if (isSaveButtonCanBeActivated) {
            saveButton = saveButton.replaceWith(getSaveButton((IModel<ContentVersion>) this.getDefaultModel())
                    .setOutputMarkupId(true).setEnabled(true).add(new CSSClassRemover("disabled")));
        } else {
            saveButton = saveButton.replaceWith(getSaveButton((IModel<ContentVersion>) this.getDefaultModel())
                    .setOutputMarkupId(true).setEnabled(false).add(AttributeModifier.append("class", "disabled")));
        }
        target.add(saveButton);

        // On pr??sente les messages
        target.add(feedbackPanel);
    }

    private MarkupContainer getSaveButton(final IModel<ContentVersion> objectModel) {
        return new IndicatingAjaxLink<>("save", objectModel) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                try {
                    ContentVersion contentVersion = objectModel.getObject();

                    // Normalement, la cr??ation d'un contexte sp??cifique ?? cette ??dition a ??t?? fait au chargement de la page,
                    // et pour une nouvelle version l'id est temporaire. Donc pas de cr??ation de contexte sp??cifique pour
                    // cet enregistrement.

                    // Suppression des ContentPropertyValue vides
                    ContentPropertyValueDao contentPropertyValueDao = new ContentPropertyValueDao(ThaleiaApplication.get().getConfiguration().getLocalDataDir().getAbsolutePath(),
                            ThaleiaApplication.get().getConfiguration().getBinaryPropertyType(), contentVersion.getObjectContext());
                    contentPropertyValueDao.deletePropertiesIfNoValue(contentVersion);

                    // On enregistre les modifications du contenu
                    ContentVersionDao contentVersionDao = new ContentVersionDao(contentVersion.getObjectContext());
                    contentVersionDao.save(contentVersion, true);

                    onOut(contentVersion.getContentType());

                } catch (DetailedException e) {
                    logger.debug("Impossible d'enregistrer l'objet : " + e.toString());
                    MarkupContainer panelContainer = this.getParent().getParent();
                    StringResourceModel errorMessageModel = new StringResourceModel("save.error", panelContainer,
                            objectModel);
                    // Affiche le message d'erreur
                    PanelUtils.showErrorMessage(errorMessageModel.getString(), e.toString(), feedbackPanel);
                }
            }
        }.add(new Label("saveLabel", new StringResourceModel("saveLabel", this, null)));
    }

    private MarkupContainer getCancelButton(final IModel<ContentVersion> objectModel) {
        return new IndicatingAjaxLink<ContentVersion>("cancel") {

            @Override
            public void onClick(AjaxRequestTarget target) {

                ContentVersion currentVersion = objectModel.getObject();
                ContentType contentType = currentVersion.getContentType();

                // Si on ??tait pas en lecture seule, alors il faut supprimer
                // cette version temporaire
                if (!readOnly) {

                    try {
                        // Le contenu de cette version
                        Content content = currentVersion.getContent();

                        // On supprime tous les fichiers upload??s pour cette  version
                        ThaleiaSession.get().getContentService().deleteLocalFiles(currentVersion);

                        // On supprime la nouvelle version (cr????e ??  l'ouverture de cette page) des objets persistants
                        ContentVersionDao contentVersionDao = new ContentVersionDao(currentVersion.getObjectContext());
                        contentVersionDao.delete(currentVersion);

                        // Si le contenu n'a plus de version, on le supprime aussi :
                        // c'est le cas si on annule la cr??ation d'un nouveau contenu.
                        if (content.getVersions().isEmpty()) {
                            ContentDao contentDao = new ContentDao(currentVersion.getObjectContext());
                            contentDao.delete(objectModel.getObject().getContent());
                        }
                    } catch (DetailedException e) {
                        // Il est normal que cette tentative de suppression  provoque des erreurs.
                    }
                }
                // Traitement de la redirection
                onOut(contentType);
            }

        }.add(new Label("cancelLabel", new StringResourceModel("cancelLabel", this, null)));
    }

    private MarkupContainer getDeleteButton(final IModel<ContentVersion> objectModel) {
        return new ConfirmationLink<ContentVersion>("delete", new StringResourceModel("delete.confirm", this, null)) {

            @Override
            public boolean isVisible() {
                try {
                    ContentVersionDao contentVersionDao = new ContentVersionDao(objectModel.getObject().getObjectContext());
                    return contentVersionDao.canBeDeleted(objectModel.getObject());
                } catch (Exception e) {
                    logger.warn("Erreur durant l'analyse d'un objet : " + e);
                    return false;
                }
            }

            @Override
            public void onClick(AjaxRequestTarget target) {

                ContentVersion currentVersion = objectModel.getObject();
                ContentType contentType = currentVersion.getContentType();
                String contentName = currentVersion.getContentIdentifier();

                try {
                    // On supprime le contenu et toutes ses versions
                    ThaleiaSession.get().getContentService().deleteContentAndAllVersions(objectModel.getObject().getContent());
                    // On ne commite pas le contexte : on fait plut??t suivre par un rollback

                    MarkupContainer panelContainer = this.getParent().getParent();
                    StringResourceModel message = new StringResourceModel("delete.ok", panelContainer, null, new Object[]{contentName});
                    ThaleiaSession.get().info(message.getString());

                } catch (DetailedException e) {
                    logger.debug("Impossible de supprimer le contenu : " + e.toString());
                    MarkupContainer panelContainer = this.getParent().getParent();
                    StringResourceModel errorMessageModel = new StringResourceModel("delete.error", panelContainer, objectModel);
                    // Affiche le message d'erreur
                    PanelUtils.showErrorMessage(errorMessageModel.getString(), e.toString(), feedbackPanel);
                }
                // Traitement de la redirection
                onOut(contentType);
            }

        }.add(new Label("deleteLabel", new StringResourceModel("deleteLabel", this, null)));
    }

    /**
     * M??thode appel??e apr??s un enregistrement ou une annulation de
     * modification. Y placer une ??ventuelle redirection.
     */
    protected abstract void onOut(ContentType contentType);

}
