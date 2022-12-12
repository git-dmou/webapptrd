package fr.solunea.thaleia.webapp.pages.admin;

import fr.solunea.thaleia.model.ApplicationParameter;
import fr.solunea.thaleia.model.auto._ApplicationParameter;
import fr.solunea.thaleia.model.dao.ApplicationParameterDao;
import fr.solunea.thaleia.webapp.pages.admin.parameters.ApplicationParametersPanel;
import fr.solunea.thaleia.webapp.security.ThaleiaSession;
import org.apache.wicket.authroles.authorization.strategies.role.annotations.AuthorizeInstantiation;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.AbstractReadOnlyModel;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@SuppressWarnings("serial")
@AuthorizeInstantiation("admin")
public class AdminSectionParameters extends Panel {

    public AdminSectionParameters(String id) {
        super(id);

        add(new ApplicationParametersPanel("parametersPanel", new AbstractReadOnlyModel<>() {
            @Override
            public List<ApplicationParameter> getObject() {
                // On masque les paramètres qui concernent la
                // localisation, car il vaut mieux les modifier par
                // ailleurs, et ils gênent la lisibilité.
                List<ApplicationParameter> result = new ArrayList<>();
                ApplicationParameterDao applicationParameterDao = new ApplicationParameterDao(ThaleiaSession.get().getContextService().getContextSingleton());
                for (ApplicationParameter parameter : applicationParameterDao.find()) {
                    if (!parameter.getName().startsWith("contenttype.localisation")) {
                        result.add(parameter);
                    }
                }
                // Tri alphabétique
                result.sort(Comparator.comparing(_ApplicationParameter::getName));
                return result;
            }
        }, true, true, true));

    }
}
