package fr.solunea.thaleia.webapp.pages.content;

import fr.solunea.thaleia.model.ContentVersion;
import fr.solunea.thaleia.model.dao.ContentVersionDao;
import fr.solunea.thaleia.webapp.security.ThaleiaSession;
import org.apache.commons.lang.time.DateFormatUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxFallbackLink;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.PropertyListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;

import java.util.List;
import java.util.stream.Collectors;

public abstract class VersionSelectorPanel extends Panel {

    ContentVersionDao contentVersionDao;

    public VersionSelectorPanel(String id, IModel<ContentVersion> contentVersion) {
        super(id, contentVersion);

        // Le modèle des versions du contenu
        IModel<List<ContentVersion>> versionsModel = new LoadableDetachableModel<>() {
            @Override
            protected List<ContentVersion> load() {

                contentVersionDao = new ContentVersionDao(ThaleiaSession.get().getContextService().getContextSingleton());

                // On ne garde que les versions qui sont en base : pas la
                // nouvelle qui a été créée si on est sur un écran de nouvelle
                // version.
                return contentVersion.getObject().getContent().getVersions().stream().filter(contentVersionDao::isCommitedInDatabase).collect(Collectors.toList());
            }
        };

        // Le sélecteur de version
        final Label currentVersionName = new Label("currentVersionName", new LoadableDetachableModel<String>() {
            @Override
            protected String load() {
                return getVersionName(contentVersion.getObject()).getObject();
            }
        });
        currentVersionName.setOutputMarkupId(true);
        add(currentVersionName);
        add(new PropertyListView<>("versions", versionsModel) {
            @Override
            protected void populateItem(final ListItem<ContentVersion> item) {
                item.add(new AjaxFallbackLink<Void>("versionLink") {
                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        currentVersionName.getDefaultModel().detach();
                        target.add(currentVersionName);
                        onSelected(target, item.getModel());
                    }
                }.add(new Label("versionName", getVersionName(item.getModelObject()))).add(new Label("versionDescription", getVersionDescription((item.getModelObject())))));
            }
        });
    }

    /**
     * L'action à effectuer à la sélection d'une version.
     *
     * @param target         pour recharger les éléments
     * @param contentVersion la version sélectionnée
     */
    protected abstract void onSelected(AjaxRequestTarget target, IModel<ContentVersion> contentVersion);

    /**
     * @return le nom de présentation de la version : son numéro, ou bien "Nouvelle version" localisé, si c'est un
     * nouvel objet.
     */
    private IModel<String> getVersionName(ContentVersion contentVersion) {

        if (contentVersion == null) {
            return Model.of("");
        }

        StringBuilder result = new StringBuilder();

        if (contentVersionDao.isNewObject(contentVersion)) {
            // Nouvelle Version
            result.append(new StringResourceModel("newVersionLabel", this, null).getString());

        } else {
            // Version X
            result.append(new StringResourceModel("versionLabelPrefix", this, null).getString()).append(contentVersion.getRevisionNumber().toString());
        }

        return Model.of(result.toString());
    }

    /**
     * @return la date d'enregistrement et l'auteur de cette version, si elle a déjà été enregistrée en base.
     */
    private IModel<String> getVersionDescription(ContentVersion contentVersion) {

        StringBuilder result = new StringBuilder();

        if (!contentVersionDao.isNewObject(contentVersion)) {

            // On ajoute la date
            result.append(DateFormatUtils.format(contentVersion.getLastUpdateDate(), " yyyy-MM-dd HH:mm",
                    ThaleiaSession.get().getLocale()));

            // On ajoute l'auteur
            result.append(" - ");
            result.append(contentVersion.getAuthor().getName());

            return Model.of(result.toString());

        } else {
            return Model.of("");
        }

    }
}
