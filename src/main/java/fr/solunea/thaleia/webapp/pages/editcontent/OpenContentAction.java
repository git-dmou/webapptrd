package fr.solunea.thaleia.webapp.pages.editcontent;

import fr.solunea.thaleia.model.*;
import fr.solunea.thaleia.model.dao.ContentPropertyValueDao;
import fr.solunea.thaleia.model.dao.EditedContentDao;
import fr.solunea.thaleia.model.dao.LocaleDao;
import fr.solunea.thaleia.service.ContentService;
import fr.solunea.thaleia.utils.DetailedException;
import fr.solunea.thaleia.utils.LogUtils;
import fr.solunea.thaleia.webapp.ThaleiaApplication;
import fr.solunea.thaleia.webapp.security.ThaleiaSession;
import org.apache.cayenne.ObjectContext;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;

public class OpenContentAction extends AbstractEditContentAction {

    private static final String CONTENT_ID_PARAM = "contentid";
    private static final String CONTENT_PROPERTY_NAME_PARAM = "editedproperty";
    private static final String LOCALE_NAME_PARAM = "locale";

    @Override
    public void run(HttpServletRequest request, HttpServletResponse response) throws DetailedException {

        try {
            ObjectContext context = ThaleiaSession.get().getContextService().getNewContext();
            LocaleDao localeDao = new LocaleDao(context);
            EditedContentDao editedContentDao =new EditedContentDao(context);
            ContentPropertyValueDao contentPropertyValueDao = new ContentPropertyValueDao(ThaleiaApplication.get().getConfiguration().getLocalDataDir().getAbsolutePath(),
                ThaleiaApplication.get().getConfiguration().getBinaryPropertyType(), context);

            // Recherche du paramètre qui contient le content.id à ouvrir
            String contentIdString = request.getParameter(CONTENT_ID_PARAM);
            exceptionIfNull(contentIdString, CONTENT_ID_PARAM);
            int contentId = Integer.parseInt(contentIdString);

            // Recherche du paramètre qui contient le nom (non localisé :
            // content_property.name) de la propriété qui contient le ZIP qui
            // contient les fichiers à éditer
            String contentPropertyName = request.getParameter(CONTENT_PROPERTY_NAME_PARAM);
            exceptionIfNull(contentPropertyName, CONTENT_PROPERTY_NAME_PARAM);

            // Recherche du paramètre qui contient le nom de la locale
            String localeString = request.getParameter(LOCALE_NAME_PARAM);
            exceptionIfNull(localeString, LOCALE_NAME_PARAM);

            // Recherche de cette locale
            Locale locale = localeDao.findByName(localeString);
            if (locale == null) {
                throw new DetailedException("La locale " + localeString + " n'existe pas.");
            }

            // Recherche d'un content qui porte cet identifiant
            User user = context.localObject(ThaleiaSession.get().getAuthenticatedUser());
            ContentService contentService = ThaleiaSession.get().getContentService();
            Content content = contentService.getContent(user, contentId);
            if (content == null) {
                throw new DetailedException("Le contenu " + contentId + " n'existe pas.");
            }
            // La dernière version de ce contenu
            ContentVersion contentVersion = content.getLastVersion();
            if (contentVersion == null) {
                throw new DetailedException("Le contenu " + contentId + " n'a pas de version.");
            }

            // Recherche de la propriété qui porte le nom demandé
            ContentPropertyValue contentPropertyValue = ThaleiaSession.get().getContentPropertyService()
                    .getContentPropertyValue(contentVersion, contentPropertyName, locale);
            if (contentPropertyValue == null) {
                throw new DetailedException("Le contenu  " + contentId + " n'a pas de valeur pour la propriété '" +
                        contentPropertyName + "' dans la locale '" + localeString + "'.");
            }

            // Si la propriété est déjà en cours d'édition, alors on renvoie la
            // référence vers l'objet en cours d'édition.
            EditedContent existing = editedContentDao.findByContentPropertyValue(contentPropertyValue);
            if (existing != null) {
                // On renvoie son id
                ok(response, ThaleiaSession.get().getEditedContentService().getPk(existing));
            }

            // On vérifie que cette valeur est bien un fichier et non un texte
            if (!contentPropertyValueDao.isValueDescribesAFile(contentPropertyValue)) {
                throw new DetailedException("La propriété '" + contentPropertyName + "' n'est pas un fichier.");
            }

            // On récupère le binaire de cette propriété
            File binary = contentPropertyValueDao.getFile(contentPropertyValue);
            if (!binary.exists()) {
                throw new DetailedException("Le fichier de La propriété '" + contentPropertyName + "' ne peut pas " +
                        "être retrouvé.");
            }

            // On prépare un objet d'édition
            EditedContent editedContent = ThaleiaSession.get().getEditedContentService().open(user, content,
                    contentPropertyName, locale, null);

            // On commite en base
            context.commitChanges();

            // On renvoie son id
            ok(response, ThaleiaSession.get().getEditedContentService().getPk(editedContent));

        } catch (Exception e) {
            AbstractEditContentAction.error(response, "Impossible d'effectuer le traitement : " + e.toString() + "\n"
                    + LogUtils.getStackTrace(e.getStackTrace()));
        }

    }
}
