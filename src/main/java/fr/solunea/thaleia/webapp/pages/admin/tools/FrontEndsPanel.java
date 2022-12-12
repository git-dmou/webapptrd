package fr.solunea.thaleia.webapp.pages.admin.tools;

import fr.solunea.thaleia.service.FrontendService;
import fr.solunea.thaleia.service.utils.Configuration;
import fr.solunea.thaleia.webapp.ThaleiaApplication;
import org.apache.log4j.Logger;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.panel.Panel;

@SuppressWarnings("serial")
public class FrontEndsPanel extends Panel {

    private static final Logger logger = Logger.getLogger(FrontEndsPanel.class);

    public FrontEndsPanel(String id) {
        super(id);

        AjaxLink<Void> link = new AjaxLink<Void>("updateLink") {

            @Override
            public void onClick(AjaxRequestTarget target) {


                try {
                    FrontendService frontendService = ThaleiaApplication.get().getFrontendService();
                    if (frontendService.frontendIsDefined()) {
                        frontendService.updateAllUsers(ThaleiaApplication.get().contextService.getContextSingleton());
                        target.appendJavaScript("alert('Les frontaux ont été mis à jour.');");
                    } else {
                        target.appendJavaScript("alert('Pas frontaux à mettre à jour : le paramètre " + Configuration
                                .FRONTEND_UPDATE_URL + " est vide.');");
                    }

                } catch (Exception e) {
                    logger.warn("Erreur durant la mise à jour des frontends :" + e);
                }
            }
        };
        add(link);
    }

}
