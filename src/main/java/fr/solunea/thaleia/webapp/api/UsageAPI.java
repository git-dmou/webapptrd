package fr.solunea.thaleia.webapp.api;

import fr.solunea.thaleia.model.*;
import fr.solunea.thaleia.model.dao.*;
import fr.solunea.thaleia.service.ContentPropertyService;
import fr.solunea.thaleia.service.ContentService;
import fr.solunea.thaleia.service.utils.Configuration;
import fr.solunea.thaleia.utils.DetailedException;
import fr.solunea.thaleia.utils.FormatUtils;
import fr.solunea.thaleia.webapp.ThaleiaApplication;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.wicketstuff.rest.annotations.MethodMapping;
import org.wicketstuff.rest.annotations.ResourcePath;
import org.wicketstuff.rest.annotations.parameters.HeaderParam;
import org.wicketstuff.rest.annotations.parameters.RequestParam;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@ResourcePath("/api/v1/usage")
public class UsageAPI extends ApiV1Service {

    private final static Logger logger = Logger.getLogger(UsageAPI.class);
    private final ICayenneContextService contextService;
    private final Configuration configuration;
    private ContentPropertyService contentPropertyService;
    private ContentService contentService;
    private fr.solunea.thaleia.service.PublicationService publicationService;

    public UsageAPI(ICayenneContextService contextService, Configuration configuration) {
        super(contextService, configuration);
        this.contextService = contextService;
        this.configuration = configuration;
        try {
            contentService = new ContentService(contextService, configuration);
            contentPropertyService = new ContentPropertyService(contextService, configuration);
            publicationService = new fr.solunea.thaleia.service.PublicationService(contextService, configuration);
        } catch (DetailedException e) {
            logger.warn("Impossible d'initialiser le service des Publications : " + e);
        }
    }

    @MethodMapping("/")
    @SuppressWarnings("unused")
    public Object getUsage(@HeaderParam("Authorization") String token, @RequestParam(value = "userLogin") String userLogin) {
        try {
            ContentPropertyValueDao contentPropertyValueDao = new ContentPropertyValueDao(configuration.getLocalDataDir().getAbsolutePath(),
                configuration.getBinaryPropertyType(), contextService.getContextSingleton());
            UserDao userDao = new UserDao(contextService.getContextSingleton());
            PublicationDao publicationDao = new PublicationDao(contextService.getContextSingleton());

            if (publicationService == null || contentService == null || contentPropertyValueDao == null || contentPropertyService == null) {
                return new Error("Error", "Internal initialization error occured. Please contact your support.");
            }

            // On vérifie que le token d'accès à l'API est valide
            ApiToken apiToken = getToken(token, false);

            // On vérifie que le compte est administrateur
            User authenticatedUser = apiToken.getUser();
            if (authenticatedUser == null || !authenticatedUser.getIsAdmin()) {
                setResponseStatusCode(403);
                logger.debug("Un utilisateur demande une action réservée aux administrateurs.");
                return "";
            }

            // On vérifie que l'utilisateur pour lequel on veut les informations existe.
            User user = userDao.findUserByLogin(userLogin);
            if (user == null) {
                setResponseStatusCode(404);
                return new Error("Error", "User '" + userLogin + "' not found.");
            }

            long totalSize = 0;

            // On prépare le résultat
            Result result = new Result();
            result.login = user.getLogin();

            // Détails des publications
            for (fr.solunea.thaleia.model.Publication publication : publicationDao.getPublications(user, false)) {
                Publication pub = new Publication();
                pub.title = publication.getName();
                pub.url = ThaleiaApplication.get().getPublishUrl() + "/" + publication.getReference();
                pub.active = publication.getActive().toString();
                File publicationDirectory = publicationService.getPublicationDirectory(publication);
                if (publicationDirectory.exists()) {
                    long bytes = FileUtils.sizeOf(publicationDirectory);
                    totalSize += bytes;
                    pub.totalSize = FormatUtils.humanReadableByteCount(bytes, false);
                } else {
                    pub.totalSize = "0";
                }
                result.publications.add(pub);
            }

            // La liste des versions des modules que cette personne a produites
            List<ContentVersion> modules = contentService.getModulesVersionsWhereAuthor(user);
            // Pour chacune de ces versions de module, on s'assure de créer un objet Content dans la réponse,
            // On recherche la dernière version de ce module, et on donne à l'objet Content de la réponse :
            //  1/ le content_identifier de la dernière version CV du module
            //  2/ le titre de la dernière version CV du module (en cherchant dans toutes les locales la propriété de type Title)
            // Puis, pour chacune des versions du module que l'utilisateur a produite (y compris les intermédiaires) :
            // 1/ On liste les propriétés de type fichier de la version de ce module, et on récupère le poids
            // 2/ On récupère la liste de tous les écrans de la version de ce module, et pour chacun, on liste les propriétés de type fichier de cette version de module, et on récupère le poids
            // 3/ On ajoute les informations de la version dans l'objet de réponse


            for (ContentVersion contentVersion : modules) {
                String contentId = contentVersion.getContentIdentifier();
                String contentType = contentVersion.getContentType().getName();

                Version version = new Version();
                version.revisionNumber = contentVersion.getRevisionNumber().toString();
                long versionSize = 0;
                // Taille des fichiers des propriétés de type fichier associées à cette version (de module)
                long modulePropertiesSize = parseSize(contentVersion, version);
                versionSize = versionSize + modulePropertiesSize;
                // Recherche de tous les écrans de ce module, et ajout du poids de toutes les versions de chacun de ces écrans
                for (Allocation allocation : contentVersion.getChilds()) {
                    fr.solunea.thaleia.model.Content screenContent = allocation.getChild();
                    for (ContentVersion screenVersion : screenContent.getVersions()) {
                        long screensSize = parseSize(screenVersion, version);
                        versionSize = versionSize + screensSize;
                    }
                }
                version.totalSize = FormatUtils.humanReadableByteCount(versionSize, false);
                totalSize = totalSize + versionSize;

                addVersionToContent(result, version, contentVersion.getRevisionNumber(), findTitle(contentVersion), contentVersion.getContentIdentifier(), contentVersion.getContentType().getName());
            }

            // Tri des versions par numéro de révision
            for (Content content : result.contents) {
                Collections.sort(content.versions);
            }

            // Taille totale
            result.totalSize = FormatUtils.humanReadableByteCount(totalSize, false);

//            // Sérialisation JSON de la réponse
//            Gson gson = new GsonBuilder().excludeFieldsWithModifiers(Modifier.TRANSIENT).setPrettyPrinting().addSerializationExclusionStrategy(new ExclusionStrategy() {
//                @Override
//                public boolean shouldSkipField(FieldAttributes field) {
//                    return field.getName().equals("serialVersionUID");
//                }
//
//                @Override
//                public boolean shouldSkipClass(Class<?> clazz) {
//                    return false;
//                }
//            }).create();
//
//            return gson.toJson(result);
            return result;

        } catch (Exception e) {
            logger.warn(e);
            setResponseStatusCode(500);
            return "{ \"error\": \"Internal error occured. Please contact your support.\" }";
        }

    }

    private long parseSize(ContentVersion contentVersion, Version version) {
        int count = 0;
        long addedSize = 0;
        ContentPropertyValueDao contentPropertyValueDao = new ContentPropertyValueDao(configuration.getLocalDataDir().getAbsolutePath(),
                configuration.getBinaryPropertyType(), contextService.getContextSingleton());
        for (ContentPropertyValue contentPropertyValue : contentVersion.getProperties()) {
            if (contentPropertyValueDao.isValueDescribesAFile(contentPropertyValue)) {
                File file = contentPropertyValueDao.getFile(contentPropertyValue);
                if (file.exists()) {
                    count++;
                    long bytes = FileUtils.sizeOf(file);
                    version.totalSize = FormatUtils.humanReadableByteCount(bytes, false);
                    logger.debug("Ajout du poids d'un fichier : " + bytes);
                    addedSize += bytes;
                } else {
                    logger.debug("Le fichier '" + file.getAbsolutePath() + "' n'existe pas.");
                }
            }
        }
        logger.debug(count + " fichiers pris en compte pour un poids total de " + addedSize);
        return addedSize;
    }

    private void addVersionToContent(Result result, Version version, int revisionNumber, String title, String contentId, String contentType) {
        try {
            if (contentId == null) {
                logger.debug("Pas de contentIdentifier pour affecter à un module.");
            } else {
                // On vérifie que le contenu (module de cette version) existe bien dans le résultat
                if (!result.contentIdExists(contentId)) {
                    // On ajoute un contenu qui porte cet identifiant
                    Content content = new Content();
                    content.contentId = contentId;
                    content.type = contentType;

                    // Le titre est celui de la dernière version de ce contenu
//                    List<fr.solunea.thaleia.model.Content> moduleContents = contentDao.findByName(content.contentId);
//                    if (moduleContents != null && !moduleContents.isEmpty()) {
//                        // On prend le premier de la liste
//                        fr.solunea.thaleia.model.Content moduleContent = moduleContents.get(0);
//                        ContentVersion lastVersion = moduleContent.getLastVersion();
//                        if (lastVersion != null) {
//                            content.type = lastVersion.getContentType().getName();
//                            // On recherche dans toutes les locales le titre de cette version
//                            content.title = findTitle(lastVersion);
//                        }
//                    }

                    result.contents.add(content);
                }
                // On ajoute la version au contenu du résultat
                for (Content content : result.contents) {
                    if (content.contentId.equals(contentId)) {
                        // On change le titre et le type de contenu, si la version est supérieure aux versions déjà existantes
                        int maxRevisionNumber = getMaxRevisionNumber(content.versions);
                        if (revisionNumber > maxRevisionNumber) {
                            content.title = title;
                            content.type = contentType;
                        }

                        // On ajoute la version aux versions du contenu
                        content.versions.add(version);
                    }
                }

            }
        } catch (Exception e) {
            logger.warn("Erreur durant l'ajout de la version " + version + " pour le contentIdentifier " + contentId, e);
        }
    }

    /**
     * @return le numéro de révision le plus élevé de cette liste de versions, sinon inférieur au premier numéro de version possible.
     */
    private int getMaxRevisionNumber(List<Version> versions) {
        int result = ContentVersion.FIRST_VERSION_NUMBER - 1;
        for (Version version : versions) {
            try {
                int revisionNumber = Integer.parseInt(version.revisionNumber);
                if (revisionNumber > result) {
                    result = revisionNumber;
                }
            } catch (NumberFormatException e) {
                // On ignore ce revisionNumber
            }
        }
        return result;
    }

    private String findTitle(ContentVersion version) {
        String result = "";
        LocaleDao localeDao = new LocaleDao(contextService.getContextSingleton());
        for (Locale locale : localeDao.find()) {
            ContentPropertyValue title = contentPropertyService.getContentPropertyValue(version,
                    "Title", locale);
            if (title != null) {
                result = title.getValue();
            }
        }
        return result;
    }

    static class Version implements Comparable<Version> {
        String revisionNumber;
        String totalSize;

        @Override
        public int compareTo(Version o) {
            try {
                Integer revision = Integer.parseInt(revisionNumber);
                return revision.compareTo(Integer.parseInt(o.revisionNumber));
            } catch (NumberFormatException e) {
                return 0;
            }
        }
    }

    static class Publication {
        String title;
        String url;
        String active;
        String totalSize;
    }

    static class Result {
        String login;
        String totalSize;
        List<Content> contents = new ArrayList<>();
        List<Publication> publications = new ArrayList<>();

        boolean contentIdExists(String contentId) {
            for (Content content : contents) {
                if (content.contentId.equals(contentId)) {
                    return true;
                }
            }
            return false;
        }
    }

    static class Content {
        String contentId;
        String title;
        String type;
        List<Version> versions = new ArrayList<>();
    }

    static class Error {
        String message;
        String description;

        public Error(String message, String description) {
            this.message = message;
            this.description = description;
        }
    }
}
