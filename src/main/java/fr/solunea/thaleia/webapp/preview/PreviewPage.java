package fr.solunea.thaleia.webapp.preview;

import fr.solunea.thaleia.service.PreviewService;
import fr.solunea.thaleia.utils.DetailedException;
import fr.solunea.thaleia.webapp.security.ThaleiaSession;
import org.apache.wicket.authroles.authorization.strategies.role.annotations.AuthorizeInstantiation;

import java.io.File;
import java.nio.charset.StandardCharsets;

/**
 * Analyse une URL du type
 * http://localhost:8080/thaleia/preview/1385644403694/toto.html pour renvoyer
 * le binaire correspondant, retrouvé dans les fichiers dans le répertoire de
 * prévisualisation : demande au PreviewService le répertoire local où
 * rechercher la resource [répertoire local du
 * PreviewService]/1385644403694/toto.html
 */
@SuppressWarnings("serial")
@AuthorizeInstantiation("user")
public class PreviewPage extends AbstractPublisherPage {

    private PreviewService previewService = null;
    private File resource;
    private String resourceNameWithoutParams;

    @Override
    protected File getResource(String resourceName) throws DetailedException {
        resource = null;
        try {
            // Si la resource a des paramètres, on les ignore pour rechercher le
            // fichier correspondant
            resourceNameWithoutParams = resourceName;
            if (resourceName.contains("?")) {
                resourceNameWithoutParams = resourceNameWithoutParams.substring(0, resourceNameWithoutParams.indexOf
                        ("?"));
            }

            // On reconstruit le chemin complet du fichier demandé
            previewService = ThaleiaSession.get().getPreviewService();
            resource = new File(previewService.getLocalDir().getAbsolutePath() + resourceNameWithoutParams);
            if (!isResourceExists()) {
                resource = tryFileNameWith_ISO_8859_to_UTF8Converison();
            }
            return resource;

        } catch (SecurityException e) {
            throw new DetailedException(e).addMessage(
                    "Impossible d'obtenir l'accès à la ressource '" + resourceName + "'.");
        }
    }

    private boolean isResourceExists() {
        boolean exists = resource.exists();
        logger.debug("resource : " + resource.getAbsolutePath() + " existe ? : " + exists);
        return exists;
    }

    private File tryFileNameWith_ISO_8859_to_UTF8Converison() {
        /* TODO
        * On peut éviter tous ces problèmes en transmettant le nom du fichier dans le corps de la requête GET
        * au lieu de le transmettre dans l'url !
        *
        * car
        * lorsqu'on enregistre une ressource audio dans l'éditeur Dialog/Action
        * un problème apparaît si le nom du fichier de la ressource contient des caractères accentués ou spéciaux,
        * qui se retrouve encodé dans l'url
        * 2 requêtes sont envoyées par l'éditeur :
        * - un POST (cf SaveContentAction.java) qui enregistre le fichier sur le serveur
        * - un GET qui récupère le fichier pour l'utiliser dans l'éditeur
        *   (c'est le problème ici)
        * On remarque que l'url subie des décodages / encodage qui provoquent des modifications :
        * on a donc une transformation qui ressemble à :
        * "è" --------------> %C3 %A8 -->  "Ã¨" ---------------------------> "%C3 %83 %C2 %A8" !!!
        * nom du fichier ---> url   ----> nom du fichier en ISO_8859_1 ? --> nom du fichier ISO_8859_1 traduit en UTF-8
        *
        * à défaut de trouver le paramétrage à ajouter pour conserver l'url correcte,
        * on tente de retrouver le fichier recherché en appliquant la transformation en sens inverse ...
        *
        * */



        logger.debug("conversion ISO_8859_1 ----> utf8");
        logger.debug("le nom du fichier : " + resource.getAbsolutePath());
        resourceNameWithoutParams = decode_ISO_8859_to_utf8(resourceNameWithoutParams);
        resource = new File(previewService.getLocalDir().getAbsolutePath() + resourceNameWithoutParams);
        logger.debug("a été converti en : " + resource.getAbsolutePath());
        return resource;
    }

    private String decode_ISO_8859_to_utf8(String fileName) {
        byte[] decodedByteArray =fileName.getBytes(StandardCharsets.ISO_8859_1);
        String decodedString = new String(decodedByteArray, StandardCharsets.UTF_8);
        return decodedString;
    }

    @Override
    protected CHECK_RENDER_RESULT renderAuthorized() {
        // Pas de contrôle d'identification
        return CHECK_RENDER_RESULT.OK;
    }

}
