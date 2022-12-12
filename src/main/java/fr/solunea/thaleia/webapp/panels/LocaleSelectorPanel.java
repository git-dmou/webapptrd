package fr.solunea.thaleia.webapp.panels;

import fr.solunea.thaleia.model.User;
import fr.solunea.thaleia.model.dao.LocaleDao;
import fr.solunea.thaleia.model.dao.UserDao;
import fr.solunea.thaleia.webapp.ThaleiaApplication;
import fr.solunea.thaleia.webapp.security.ThaleiaSession;
import org.apache.wicket.authroles.authorization.strategies.role.annotations.AuthorizeInstantiation;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.Model;

import java.util.Locale;

@SuppressWarnings("serial")
public class LocaleSelectorPanel extends Panel {

    public LocaleSelectorPanel(String id) {
        super(id);

        add(new Link<Object>("goEnglish") {
            @Override
            public void onClick() {
                getSession().setLocale(Locale.ENGLISH);

                // Si l'utilisateur est identifié, on met à jour sa locale prédérée
                setPreferedLocale(Locale.ENGLISH);
            }
        });
        add(new Link<Object>("goFrench") {
            @Override
            public void onClick() {
                getSession().setLocale(Locale.FRENCH);

                // Si l'utilisateur est identifié, on met à jour sa locale prédérée
                setPreferedLocale(Locale.FRENCH);
            }
        });
        add(new Label("currentLocale", new Model<String>() {
            @Override
            public String getObject() {
                return getSession().getLocale().getLanguage();
            }
        }));
    }

    private void setPreferedLocale(Locale javaLocale) {
        try {
            User authenticatedUser = ThaleiaSession.get().getAuthenticatedUser();
            UserDao userDao = new UserDao(ThaleiaSession.get().getContextService().getContextSingleton());
            LocaleDao localeDao = new LocaleDao(ThaleiaSession.get().getContextService().getContextSingleton());
            authenticatedUser.setPreferedLocale(localeDao.getLocale(javaLocale));
            userDao.save(authenticatedUser);
        } catch (Exception e) {
            // On ignore les erreurs, car on peut être dans le cas d'un utilisateur non identifié.
        }
    }

}
