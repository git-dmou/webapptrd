package fr.solunea.thaleia.webapp.pages.editcontent;

import fr.solunea.thaleia.model.EditedContent;
import fr.solunea.thaleia.utils.DetailedException;
import fr.solunea.thaleia.utils.LogUtils;
import fr.solunea.thaleia.webapp.security.ThaleiaSession;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class DeleteContentAction extends AbstractEditContentAction {

    private static final String EDITED_CONTENT_ID_PARAM = "editedcontentid";
    private static final String FILENAME_PARAM = "filename";

    @Override
    public void run(HttpServletRequest request, HttpServletResponse response) throws DetailedException {
        try {
            // Recherche du paramètre qui contient l'identifiant du
            // EditedContent à fermer
            String editedContentIdString = request.getParameter(EDITED_CONTENT_ID_PARAM);
            exceptionIfNull(editedContentIdString, EDITED_CONTENT_ID_PARAM);

            // Recherche du paramètre qui contient le fichier à supprimer
            String filename = request.getParameter(FILENAME_PARAM);
            exceptionIfNull(editedContentIdString, FILENAME_PARAM);

            // On recherche le EditedContent
            EditedContent editedContent = ThaleiaSession.get().getEditedContentService().getEditedContent(editedContentIdString, ThaleiaSession.get().getContextService().getContextSingleton());

            if (editedContent == null) {
                throw new DetailedException("L'identifiant '" + editedContentIdString + "' ne correspond à aucun EditedContent.");

            } else {
                // On vérifie que l'utilisateur qui demande l'opération sur ce
                // EditedContent est bien celui qui l'a créé.
                if (!editedContent.getAuthor().equals(ThaleiaSession.get().getAuthenticatedUser())) {
                    throw new DetailedException("Le EditedContent '" + editedContentIdString + "' est détenu par un autre utilisateur.");
                }

                // L'action de suppression
                ThaleiaSession.get().getEditedContentService().deleteFile(editedContent, filename);

                // Yeah !
                // On renvoie son id
                ok(response, ThaleiaSession.get().getEditedContentService().getPk(editedContent));
            }

        } catch (Exception e) {
            AbstractEditContentAction.error(response, "Impossible d'effectuer le traitement : " + e.toString() + "\n" + LogUtils.getStackTrace(e.getStackTrace()));
        }

    }

}
