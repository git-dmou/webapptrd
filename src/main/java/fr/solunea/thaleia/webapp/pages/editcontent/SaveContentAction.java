package fr.solunea.thaleia.webapp.pages.editcontent;

import fr.solunea.thaleia.model.EditedContent;
import fr.solunea.thaleia.service.DocumentsService;
import fr.solunea.thaleia.service.PreviewService;
import fr.solunea.thaleia.service.utils.IPreviewHelper;
import fr.solunea.thaleia.utils.DetailedException;
import fr.solunea.thaleia.utils.LogUtils;
import fr.solunea.thaleia.webapp.ThaleiaApplication;
import fr.solunea.thaleia.webapp.security.ThaleiaSession;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.io.FileUtils;
import org.apache.wicket.ajax.json.JSONObject;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.UnsupportedEncodingException;
import java.util.*;

/**
 * Ajoute un document dans un EditedContent en cours d'édition. Ce doument peut
 * être déposé tel quel, ou bien après transformation. Renvoie un tableau JSON
 * avec les noms des fichiers stockés dans le editedContent (path/nom de
 * fichier, exprimé en relatif dans le répertoire du editedContent), et une URL
 * de prévisualisation.
 */
public class SaveContentAction extends AbstractEditContentAction {

    /**
     * L'identifiant du contenu en cours d'édition.
     */
    private static final String EDITED_CONTENT_ID_PARAM = "editedcontentid";
    /**
     * Optionnel : le répertoire où stocker les fichiers, en relatif par rapport
     * au répertoire qui contient le contenu édité.
     */
    private static final String PATH_PARAM = "path";
    /**
     * Optionnel : peut demander d'opérer une transformation sur le fichier
     * transmis, avant de l'enregistrer. Dans ce cas, on renvoie un tableau JSON
     * des fichiers générés (path/nom de fichier), et pour chaucun une URL de
     * prévisualisation.
     */
    private static final String TRANSFORM_PARAM = "transform";
    private static final String TRANSFORM_PPT2IMG = "ppt2img";

    private final ServletContext servletContext;

    public SaveContentAction(ServletContext context) {
        this.servletContext = context;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void run(HttpServletRequest request, HttpServletResponse response) throws DetailedException {
        try {

            request.setCharacterEncoding("UTF-8");

            // Récupération de tous les paramètres de la requête
            DiskFileItemFactory factory = new DiskFileItemFactory();
            File repository = (File) servletContext.getAttribute("javax.servlet.context.tempdir");
            factory.setRepository(repository);
            ServletFileUpload upload = new ServletFileUpload(factory);
            // logger.debug("Il y a " + request.getParts().size()
            // + " parties dans cette requête.");
            List<FileItem> items = upload.parseRequest(request);

            // Parcours des items de la requête
            Map<String, String> requestParams = new HashMap<>();
            List<File> uploadedFiles = new ArrayList<>();
            for (FileItem item : items) {
                logger.debug("Analyse de l'item : " + item);
                if (item.isFormField()) {
                    // Si l'item est un paramètre, on l'ajoute aux paramètres
                    // récupérés
                    processFormField(item, requestParams);
                } else {
                    // Si l'item est un fichier, on l'ajoute aux fichiers
                    // récupérés
                    processUploadedFile(item, uploadedFiles);
                }
            }

            // Recherche du paramètre qui contient l'identifiant du
            // EditedContent à fermer
            String editedContentIdString = request.getParameter(EDITED_CONTENT_ID_PARAM);
            if (editedContentIdString == null) {
                editedContentIdString = requestParams.get(EDITED_CONTENT_ID_PARAM);
            }
            exceptionIfNull(editedContentIdString, EDITED_CONTENT_ID_PARAM);

            // Recherche du paramètre qui contient le chemin dans lequel
            // enregistrer le fichier.
            String path = request.getParameter(PATH_PARAM);
            if (path == null) {
                path = requestParams.get(PATH_PARAM);
                if (path == null) {
                    path = "";
                }
            }

            // On nettoie le path des caratères qui posent problème
            // à l'OS
            String okPath = path.replaceAll("[:\"*?<>|]+", "_");

            // On remplace tous les \ par des /
            okPath = okPath.replace("\\", "/");

            // On s'assure que path ne commence ni ne termine par /
            if (okPath.startsWith("/")) {
                okPath = okPath.substring(1);
            }
            if (okPath.endsWith("/")) {
                okPath = okPath.substring(0, okPath.length() - 1);
            }

            // Recherche du paramètre d'application d'une transformation
            String transform = requestParams.get(TRANSFORM_PARAM);
            if (transform == null) {
                transform = "";
            }

            logger.debug("Enregistrement du contenu dans le répertoire " + okPath + " du editedContent id=" + editedContentIdString + " transform=" + transform);

            // On recherche le EditedContent
            EditedContent editedContent = ThaleiaSession.get().getEditedContentService().getEditedContent(editedContentIdString, ThaleiaSession.get().getContextService().getContextSingleton());

            if (editedContent == null) {
                throw new DetailedException("L'identifiant '" + editedContentIdString + "' ne correspond à aucun " + "EditedContent.");

            } else {
                // On vérifie que l'utilisateur qui demande l'opération sur ce
                // EditedContent est bien celui qui l'a créé.
                if (!editedContent.getAuthor().equals(ThaleiaSession.get().getAuthenticatedUser())) {
                    throw new DetailedException("Le EditedContent '" + editedContentIdString + "' est détenu par un " + "autre utilisateur.");
                }

                List<File> filesToStore = new ArrayList<>();

                // S'il fallait effectuer une transformation PPT2IMG, on le fait
                if (TRANSFORM_PPT2IMG.equalsIgnoreCase(transform)) {
                    // On recherche le PPT
                    for (File file : uploadedFiles) {
                        if (isPptFile(file)) {
                            // Transformation du PPT en images dans un
                            // répertoire
                            // temporaire
                            File tempDir = ThaleiaApplication.get().getTempFilesService().getTempDir();
                            DocumentsService documentsService = ThaleiaApplication.get().getDocumentsService();
                            documentsService.ppt2Png(file, tempDir);

                            // On stockera les images de ce répertoire
                            // temporaires
                            // dans le editedcontent (mais pas le PPT)
                            File[] files = tempDir.listFiles();
                            if (files != null) {
                                Collections.addAll(filesToStore, files);
                            }
                        } else {
                            // On ajoute tous les fichiers, sauf le PPT, à
                            // enregistrer
                            filesToStore.add(file);
                        }
                    }
                } else {
                    // Tous les fichiers transmis sont à enregistrer
                    filesToStore.addAll(uploadedFiles);
                }

                // Les fichiers stockés, avec leur URL de prévisualisation
                Map<String, String> storedFiles = new HashMap<>();

                // On procède à l'enregistrement de tous les fichiers reçus
                // dans le chemin demandé
                PreviewService previewService = ThaleiaApplication.get().getPreviewService();
                for (File file : filesToStore) {
                    ThaleiaSession.get().getEditedContentService().saveFile(editedContent, file, okPath + File.separator + file.getName(), true);
                    try {
                        if (file.length() < 100) {
                            logger.debug("Contenu textuel du fichier enregistré : \n" + FileUtils.readFileToString(file, "utf-8"));
                        } else {
                            logger.debug("Contenu enregistré : " + file.length() + "octets");
                        }
                    } catch (Exception e) {
                        // rien
                    }

                    // On prépare une URL de prévisualisation pour ce
                    // fichier
                    String previewUrl = previewService.publishFile(file, new IPreviewHelper() {
                        @Override
                        public void adapt(File directory) {
                            // rien
                        }

                        @Override
                        public String guessMainFile(File tempDir) {
                            return file.getName();
                        }
                    });

                    // On stocke le nom de ce fichier
                    storedFiles.put(okPath + "/" + file.getName(), previewUrl);
                }

                // Yeah !
                // On renvoie son id
                logger.debug("Enregistrement ok !");

                // On fabrique un tableau JSON sur la base de storedFiles
                JSONObject jsonResponse = new JSONObject(storedFiles);

                ok(response, jsonResponse.toString());
            }

        } catch (Exception e) {
            logger.info("Erreur de traitement : " + e);
            AbstractEditContentAction.error(response, "Impossible d'effectuer le traitement : " + e.toString() + "\n" + LogUtils.getStackTrace(e.getStackTrace()));
        }

    }

    private boolean isPptFile(File file) {
        return (file.getName().toLowerCase().endsWith(".ppt") || file.getName().toLowerCase().endsWith(".pptx"));
    }

    /**
     * Enregistre le binaire de ce FileItem dans un fichier temporaire, et
     * l'ajoute à la liste passée en paramètre.
     */
    private void processUploadedFile(FileItem item, List<File> uploadedFiles) throws DetailedException {
        String fileName = item.getName();
        long sizeInBytes = item.getSize();
        logger.debug("Réception du fichier '" + fileName + "' de taille " + FileUtils.byteCountToDisplaySize(sizeInBytes));

        File uploadedFile;
        try {
            uploadedFile = ThaleiaApplication.get().getTempFilesService().getTempFile(fileName);
        } catch (DetailedException e) {
            throw e.addMessage(
                    "Impossible d'obtenir un fichier temporaire pour stocker le fichier uploadé " + fileName);
        }

        try {
            item.write(uploadedFile);
            // On stockera le fichier reçu.
            uploadedFiles.add(uploadedFile);

        } catch (Exception e) {
            throw new DetailedException(e).addMessage(
                    "Impossible d'enregistrer le fichier uploadé " + fileName + " dans le fichier temporaire " + uploadedFile.getAbsolutePath());
        }

    }

    /**
     * Ajoute cet item, consiédéré comme couple clé/valeur dans la liste passée
     * en paramètre.
     */
    private void processFormField(FileItem item, Map<String, String> requestParams) {
        String name = item.getFieldName();
        String value = "";
        try {
            value = item.getString("UTF-8");
        } catch (UnsupportedEncodingException e) {
            // On ignore.
            logger.warn("Le paramètre '" + name + "' est ignoré, car son encodage n'est pas reconnu.");
        }
        logger.debug("RequestParam récupéré : " + name + "=" + value);
        requestParams.put(name, value);
    }

}
