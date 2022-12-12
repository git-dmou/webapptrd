package fr.solunea.thaleia.webapp.pages.plugins;

import fr.solunea.thaleia.model.Domain;
import fr.solunea.thaleia.model.Plugin;
import fr.solunea.thaleia.model.dao.PluginDao;
import fr.solunea.thaleia.webapp.pages.BasePage;
import fr.solunea.thaleia.webapp.pages.ThaleiaV6MenuPage;
import fr.solunea.thaleia.webapp.security.ThaleiaSession;
import org.apache.wicket.authroles.authorization.strategies.role.annotations.AuthorizeInstantiation;
import org.apache.wicket.model.LoadableDetachableModel;

@SuppressWarnings("serial")
@AuthorizeInstantiation("admin")
public class EditPluginPage extends ThaleiaV6MenuPage {

    public EditPluginPage() {
        super();

        // On créé un nouveau plugin, dans un contexte temporaire
        final Plugin plugin = ThaleiaSession.get().getContextService().getNewInNewContext(Plugin.class);

        // Par défaut, le domaine du plugin est celui de l'utilisateur
        // On effectue cette association avec un domaine placé dans le contexte d'édition du plugin
        plugin.setDomain(plugin.getObjectContext().localObject( ThaleiaSession.get().getAuthenticatedUser().getDomain()));

        add(new EditPluginPanel("pluginPanel",
                new LoadableDetachableModel<>() {
                    @Override
                    protected Plugin load() {
                        return plugin;
                    }
                }) {

            @Override
            protected void onOut() {
                setResponsePage(PluginsPage.class);
            }
        });
    }

}
