package fr.solunea.thaleia.webapp.api.customization;

import fr.solunea.thaleia.model.ApiToken;
import fr.solunea.thaleia.model.Domain;
import fr.solunea.thaleia.model.User;
import fr.solunea.thaleia.model.dao.ICayenneContextService;
import fr.solunea.thaleia.service.CustomizationService;
import fr.solunea.thaleia.service.utils.Configuration;
import fr.solunea.thaleia.service.utils.ZipUtils;
import fr.solunea.thaleia.utils.DetailedException;
import fr.solunea.thaleia.webapp.security.ThaleiaSession;
import fr.solunea.thaleia.webapp.utils.PluginResourcesService;
import fr.solunea.thaleia.webapp.utils.PluginsNames;
import fr.solunea.thaleia.webapp.utils.TempFileMagicBasket;
import org.apache.cayenne.ObjectContext;
import org.apache.log4j.Logger;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class POSTCustomizationApiTreatment {

    private final static Logger logger = Logger.getLogger(POSTCustomizationApiTreatment.class);
    private final ICayenneContextService contextService;
    private final Configuration configuration;
    private String graphicCustomizationName;
    private final String defaultGraphicCustomizationFile = "Resources_cannelle/content-customization-html.zip";

    // pour le test de l'acces aux resources du plugin action :
    //    private String defaultGraphicCustomizationFile = "content-customization-html.zip";
    private final String customizationFilename = "Resources_content-customizationAPI-html.zip";
    private CustomizationService customizationService ;
    private Domain domaine;
    private ObjectContext tempContext;
    private User user;
    private ApiToken apiToken;
    private String fileContent;


    private TempFileMagicBasket tempFileBasket = new TempFileMagicBasket();


    public POSTCustomizationApiTreatment(ICayenneContextService contextService,
                            Configuration configuration, ApiToken apiToken, String fileContent) {
        super();
        this.contextService = contextService;
        this.configuration = configuration;
        this.apiToken = apiToken;
        this.fileContent = fileContent;

    }

    public void run() throws DetailedException{
        onUpload(fileContent, apiToken);
    }

    private void onUpload(String fileContent, ApiToken apiToken) throws DetailedException {

        try {
            uploadParamSetting(apiToken);

            File tempCustomizationFile = getCustomizationFile();

            File customizationFileUpdated = updatePersonnalisationFileWithCSS(fileContent, tempCustomizationFile);
            customizationService.setCustomizationFile(
                    graphicCustomizationName,
                    null,
                    domaine,
                    customizationFileUpdated,
                    customizationFilename,
                    tempContext);
            tempContext.commitChanges();

            tempFileBasket.clean();

        } catch (Exception e) {
            logger.warn("Erreur d'enregistrement du fichier de personnalisation :" + e);
            throw new DetailedException(e).addMessage("Erreur d'enregistrement du fichier de personnalisation :");

        }
//        return null;
    }

    private File getCustomizationFile() throws DetailedException {
        File customizationFile =  customizationService.getCustomizationFile(graphicCustomizationName,
                null,
                domaine,
                tempContext);

        File tempCustomizationFile = null;
        if (customizationFile != null) {
            // on cré un fichier temporaire car le fichier customization retourné
            // est celui qui est présent en base de donnée
            tempCustomizationFile = tempFileBasket.makeTempFileFromFile(customizationFile);
            logger.debug("fichier temp customization par defaut : " + tempCustomizationFile);
        }

        if (customizationFile == null){
            logger.debug("fichier customization inexistant");
            // le fichier customization par defaut retourné est déjà un fichier temporaire
            // il pourra être supprimé sans problèmes à la fin des traitements
            tempCustomizationFile = tempFileBasket.addFile(getDefaultCustomizationFile());
            logger.debug("fichier custom cannelle par defaut ? : " + tempCustomizationFile.getAbsolutePath());

        }
        return tempCustomizationFile;
    }

    /**
     * renseignement des données de session
     *
     * @param apiToken
     * @throws DetailedException
     */
    private void uploadParamSetting(ApiToken apiToken) throws DetailedException {
        customizationService = ThaleiaSession.get().getCustomizationFilesService();
        graphicCustomizationName = getGraphicCustomizationName();
        user = apiToken.getUser();
        domaine = user.getDomain();
        tempContext = ThaleiaSession.get().getContextService().getNewContext();
    }


    private File getDefaultCustomizationFile() throws DetailedException  {

        PluginResourcesService pluginResourcesService = PluginResourcesService.get();
        File resourcesFile = null;
        try {
            resourcesFile = pluginResourcesService.getResource(user, PluginsNames.CANNELLE,defaultGraphicCustomizationFile);

//          pour le test de l'acces aux resources du plugin action :
//            resourcesFile = pluginResourcesService.getResource(user, PluginsNames.ACTION,defaultGraphicCustomizationFile);
        } catch (DetailedException e) {
            throw e.addMessage("impossible d'accéder au fichier de customization par défaut");
        }
        return  resourcesFile;

    }


    /**
     *      retour du nom complet de la classe ActFormatPackager en dur pour éviter la référence circulaire
     *      avec le plugin canelle.
     *      utilisé comme identifiant dans la requête qui récupère le fichier de perso
     *      et pour récupérer la classe de traitement ...
     *      actuellement ce nom n'est pas discriminant
     * @return
     */
    private String getGraphicCustomizationName() {
        return "fr.solunea.thaleia.plugins.cannelle.packager.act.ActFormatPackager";
    }

    private File updatePersonnalisationFileWithCSS(String CSSString, File customizationFile) throws DetailedException {

        String customizationFileAbsolutePath = customizationFile.getAbsolutePath();
        String customizationFileDir = customizationFile.getParent();
        String tempWorkDir = customizationFileDir + "/tempworkdir";
        String cssRelativePath = "/engine/css/";

        tempFileBasket.addFileFromPath(tempWorkDir);

        // unzip perso
        unzipCustomizationFile(customizationFile, customizationFileAbsolutePath, tempWorkDir);

        // update CSS
        updateUnzipedCustomizationFile(CSSString, tempWorkDir, cssRelativePath);

        // zip again
        File archive = zipAgainCustomizationFile(customizationFileAbsolutePath, tempWorkDir);

        return archive;
    }

    private File zipAgainCustomizationFile(String customizationFileAbsolutePath, String tempWorkDir) throws DetailedException {

        File archive = ZipUtils.toZip(tempWorkDir, customizationFileAbsolutePath);

        return archive;
    }

    private void updateUnzipedCustomizationFile(String CSSString, String tempWorkDir, String cssRelativePath) throws DetailedException {
        File oldCssFile = new File(tempWorkDir + cssRelativePath + "customization.css");
        if (oldCssFile.exists()) {
            oldCssFile.delete();
        }
        File newCssFile = makeFileFromString(CSSString, "customization.css", tempWorkDir + cssRelativePath);
    }

    private void unzipCustomizationFile(File customizationFile, String customizationFileAbsolutePath,
                                        String tempWorkDir) throws DetailedException {
        try {
            ZipUtils.dezip(customizationFileAbsolutePath, tempWorkDir);
            customizationFile.delete();
        } catch (Exception e) {
            throw new DetailedException(e).addMessage("Probleme traitement dezip Customization File");
        }
    }

    public File makeFileFromString(String fileContent, String filename, String dirPath) throws DetailedException {

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(dirPath + filename, StandardCharsets.UTF_8));) {
            writer.write(fileContent);
            File tempFile = new File(dirPath + filename);
            return tempFile;
        } catch (IOException e) {
            throw new DetailedException(e).addMessage("Probleme creation nouveau fichier css");
        }
    }

}
