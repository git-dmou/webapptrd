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
import fr.solunea.thaleia.webapp.utils.TempFileMagicBasket;
import org.apache.cayenne.ObjectContext;
import org.apache.log4j.Logger;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

public class GETCustomizationApiTreatment {

    private final static Logger logger = Logger.getLogger(GETCustomizationApiTreatment.class);
    private final ICayenneContextService contextService;
    private final Configuration configuration;
    private String graphicCustomizationName;
    private CustomizationService customizationService;
    private Domain domaine;
    private ObjectContext tempContext;
    private User user;
    private ApiToken apiToken;

    private TempFileMagicBasket tempFileBasket = new TempFileMagicBasket();


    public GETCustomizationApiTreatment(ICayenneContextService contextService,
                                        Configuration configuration, ApiToken apiToken) {
        super();
        this.contextService = contextService;
        this.configuration = configuration;
        this.apiToken = apiToken;

    }

    public String getCssString(ApiToken apiToken) throws DetailedException {

        try {
            uploadParamSetting(apiToken);

            File tempCustomizationFile = getCustomizationFile();

            File cssFile = getCssFileFromCustomization(tempCustomizationFile);
            String cssContent = Files.readString(Paths.get(cssFile.getAbsolutePath()), StandardCharsets.UTF_8);

            logger.debug("HTTP GET String CSS : " + cssContent);

            tempFileBasket.clean();
            return cssContent;
        } catch (Exception e) {
            logger.warn("Erreur d'enregistrement du fichier de personnalisation :" + e);
            throw new DetailedException(e).addMessage("Erreur d'enregistrement du fichier de personnalisation :");

        }
    }

    private File getCustomizationFile() throws DetailedException {
        File customizationFile = customizationService.getCustomizationFile(graphicCustomizationName,
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

        if (customizationFile == null) {
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


    private File getDefaultCustomizationFile() throws DetailedException {

        File resourcesFile = null;
        resourcesFile = customizationService.getDefaultCustomizationFile();
        return resourcesFile;

    }


    /**
     * retour du nom complet de la classe ActFormatPackager en dur pour éviter la référence circulaire
     * avec le plugin canelle.
     * utilisé comme identifiant dans la requête qui récupère le fichier de perso
     * et pour récupérer la classe de traitement ...
     * actuellement ce nom n'est pas discriminant
     *
     * @return
     */
    private String getGraphicCustomizationName() {
        return "fr.solunea.thaleia.plugins.cannelle.packager.act.ActFormatPackager";
    }

    private File getCssFileFromCustomization(File customizationFile) throws DetailedException {

        String customizationFileAbsolutePath = customizationFile.getAbsolutePath();
        String customizationFileDir = customizationFile.getParent();
        String tempWorkDir = customizationFileDir + "/tempworkdir";
        String cssRelativePath = "/engine/css/";

        tempFileBasket.addFileFromPath(tempWorkDir);

        // unzip perso
        unzipCustomizationFile(customizationFile, customizationFileAbsolutePath, tempWorkDir);

        // get CSS
        File cssFile = returnCssFile(tempWorkDir, cssRelativePath);

         return cssFile;
    }

    private File returnCssFile(String tempWorkDir, String cssRelativePath) throws DetailedException {
        File cssFile = new File(tempWorkDir + cssRelativePath + "customization.css");
        return cssFile;
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

}
