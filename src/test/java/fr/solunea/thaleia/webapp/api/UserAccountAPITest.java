package fr.solunea.thaleia.webapp.api;

import com.google.gson.JsonObject;
import fr.solunea.thaleia.model.*;
import fr.solunea.thaleia.model.dao.*;
import fr.solunea.thaleia.service.LicenceService;
import fr.solunea.thaleia.service.UserService;
import fr.solunea.thaleia.service.utils.Configuration;
import fr.solunea.thaleia.service.utils.Unique;
import fr.solunea.thaleia.service.utils.UpdateUserRequest;
import fr.solunea.thaleia.utils.DetailedException;
import fr.solunea.thaleia.webapp.ThaleiaApplication;
import fr.solunea.thaleia.webapp.ThaleiaApplicationTester;
import fr.solunea.thaleia.webapp.pages.demo.AccountDetails;
import fr.solunea.thaleia.webapp.pages.demo.EmailValidationPage;
import fr.solunea.thaleia.webapp.pages.demo.FinalizeAccountPage;
import fr.solunea.thaleia.webapp.pages.demo.RedirectToFinalizeAccountPageException;
import fr.solunea.thaleia.webapp.security.ThaleiaSession;
import fr.solunea.thaleia.webapp.security.jwt.JWTAuthenticationPage;
import org.apache.log4j.Logger;
import org.apache.wicket.Component;
import org.apache.wicket.request.flow.RedirectToUrlException;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.util.tester.FormTester;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

import static fr.solunea.thaleia.webapp.api.UserAccountAPI.API_USER_ACCOUNT_POST_SEND_MAILS_PARAMETER_NAME;
import static org.junit.jupiter.api.Assertions.*;

public class UserAccountAPITest extends ThaleiaApplicationTester {

    private static final Logger logger = Logger.getLogger(UserAccountAPITest.class);

    String userAccountCreationSendMailsInitialValue;

    private void storeApplicationParameters() {
        ApplicationParameterDao applicationParameterDao = ThaleiaApplication.get().getApplicationParameterDao();
        // On stocke cette valeur, pour la rétablir si besoin
        userAccountCreationSendMailsInitialValue = applicationParameterDao.getValue(API_USER_ACCOUNT_POST_SEND_MAILS_PARAMETER_NAME, "");
    }

    private void restoreApplicationParameters() throws DetailedException {
        ApplicationParameterDao applicationParameterDao = ThaleiaApplication.get().getApplicationParameterDao();
        // On stocke cette valeur, pour la rétablir si besoin
        ApplicationParameter parameter = applicationParameterDao.findByName(API_USER_ACCOUNT_POST_SEND_MAILS_PARAMETER_NAME);
        if (parameter == null) {
            parameter = applicationParameterDao.get();
            parameter.setName(API_USER_ACCOUNT_POST_SEND_MAILS_PARAMETER_NAME);
        }
        parameter.setValue(Objects.requireNonNullElse(userAccountCreationSendMailsInitialValue, "true"));
        applicationParameterDao.save(parameter);
    }

    @Test
    public void update() {
    }

    /**
     * Vérifie que la création de compte avec licence est impossible sans token d'identification.
     */
    @Test
    public void createErrorUnauthorized() {
        String newUserEmail = Unique.getUniqueString(12) + "@solunea.fr";
        UserAccountAPI.AttributeLicenceRequest attributeLicenceRequest = getLicenceRequest(newUserEmail, LicenceService.LICENCE_NAME_DEMO_CANNELLE);
        UserAccountAPI userAccountAPI = new UserAccountAPI(ThaleiaApplication.get().contextService, ThaleiaApplication.get().getConfiguration());
        Object result = userAccountAPI.create(null, attributeLicenceRequest);
        assertNull(result);
    }

    private UserAccountAPI.AttributeLicenceRequest getLicenceRequest(String email, String licence) {
        UserAccountAPI.AttributeLicenceRequest attributeLicenceRequest = new UserAccountAPI.AttributeLicenceRequest();
        attributeLicenceRequest.licence = licence;
        attributeLicenceRequest.origin = UserAccountAPITest.class.getName();
        attributeLicenceRequest.email = email;
        attributeLicenceRequest.account_created_locale = "fr";
        attributeLicenceRequest.account_created_subject = "Accès à votre démo";
        attributeLicenceRequest.account_created_body = "Pour accéder à votre démo : ${validationURL}";
        attributeLicenceRequest.account_completion_redirection = "https://site/completion&user=" + email;
        return attributeLicenceRequest;
    }

    /**
     * Vérifie que la création de compte avec licence est impossible si on n'est pas admin et pas autorisé à transmettre la licence.
     */
    @Test
    public void createErrorNonDemoUnauthorized() throws DetailedException {
        ApplicationParameterDao applicationParameterDao = ThaleiaApplication.get().getApplicationParameterDao();
        MailTemplateDao mailTemplateDao = new MailTemplateDao(ThaleiaApplication.get().contextService.getContextSingleton());
        UserService userService = new UserService(ThaleiaApplication.get().contextService, ThaleiaApplication.get().getConfiguration());
        UserDao userDao = new UserDao(ThaleiaApplication.get().contextService.getContextSingleton());
        LicenceService licenceService = new LicenceService(ThaleiaApplication.get().contextService, ThaleiaApplication.get().getConfiguration());
        LicenceDao licenceDao = ThaleiaApplication.get().getLicenceDao();

        storeApplicationParameters();

        Licence demoLicenceToAttribute = licenceDao.findByName(LicenceService.LICENCE_NAME_DEMO_CANNELLE);
        Licence licenceOfAttributor = licenceDao.findByName(LicenceService.LICENCE_NAME_DEMO_CANNELLE);
        assertNotNull(demoLicenceToAttribute);

        // On stocke la valeur actuelle du paramètre d'application qui contient les personnes autorisées à attribuer la
        // licence de démo Thaleia XL, pour la remettre en l'état après le test :
        String licenceAttributorsParameterValue = applicationParameterDao.getValue("licence." + demoLicenceToAttribute.getName() + ".attributors", "");

        User licenceAttributor = null;
        String licenceAttributeeEmail = null;
        try {
            // On s'assure que personne n'a le droit d'attibuer cette licence.
            ApplicationParameter applicationParameter = applicationParameterDao.findByName("licence." + demoLicenceToAttribute.getName() + ".attributors");
            if (applicationParameter == null) {
                applicationParameter = applicationParameterDao.get();
                applicationParameter.setName("licence." + demoLicenceToAttribute.getName() + ".attributors");
            }
            applicationParameter.setValue("");

            // On fabrique un compte utilisateur avec une licence valide :

            // Le nom du compte utilisateur qui va attribuer la licence
            String licenceAttributorEmail = Unique.getUniqueString(12) + "@solunea.fr";
            logger.info("Compte utilisateur de tests pour attribuer la licence : " + licenceAttributorEmail);

            // Le contenu du mail de validation
            Locale locale = Locale.ENGLISH;
            MailTemplate mailTemplate = mailTemplateDao.findByName("userAccountCreated", locale);
            String validationEmailSubject = mailTemplate.getObject();
            String validationEmailBody = mailTemplate.getBody();

            // Le lien de finalisation du compte sur cette instance Thaleia
            String accountFinalizationURL = applicationParameterDao.getValue(Configuration.APPLICATION_PARAMETER_SERVER_URL, "") + Configuration.ACCOUNT_FINALIZATION_PAGE;

            // Le point d'accès sur cette instance Thaleia
            String thaleiaLoginUrl = applicationParameterDao.getValue(Configuration.APPLICATION_PARAMETER_SERVER_URL, "");

            try {
                // création du compte de l'utilisateur qui va attribuer la licence (par l'admin)
                User admin = userDao.findUserByLogin("admin");
                userService.attributeVerifiedLicenceToAccountWithException(admin, licenceOfAttributor, licenceAttributorEmail, locale, validationEmailSubject, validationEmailBody, UserAccountAPITest.class.getName(), accountFinalizationURL, thaleiaLoginUrl, false);
            } catch (DetailedException e) {
                logger.warn(e);
                throw e;
            }
            // On vérifie que ce compte a été créé
            licenceAttributor = userDao.findUserByLogin(licenceAttributorEmail);
            assertNotNull(licenceAttributor);
            // On vérifie que son compte est valide :
            if (licenceOfAttributor.getIsDemo()) {
                assertTrue(licenceService.isUserValid(licenceAttributor, false));
            } else {
                assertTrue(licenceService.isUserValid(licenceAttributor, true));
            }
            // On fixe le mot de passe
            // Création de la mise à jour (équivalente au formulaire de la page de finalisation du compte)
            UpdateUserRequest updateUserRequest = new UpdateUserRequest();
            updateUserRequest.company = "Test Company";
            updateUserRequest.name = "Test Username";
            updateUserRequest.phone = "01 02 03 04 05";
            updateUserRequest.password = "test_password";
            // On soumet la finalisation du compte
            userService.updateUserAccountAndGetLoginToken(updateUserRequest, licenceAttributor, false);

            // Préparation d'un token pour appel à l'api
            ApiV1Service apiV1Service = new ApiV1Service(ThaleiaApplication.get().contextService, ThaleiaApplication.get().getConfiguration());
            String token = apiV1Service.createToken(licenceAttributor.getLogin(), updateUserRequest.password);
            assertFalse(token.isEmpty());

            // Cet utilisateur va tenter d'attribuer une licence : erreur attendue
            licenceAttributeeEmail = Unique.getUniqueString(12) + "@solunea.fr";
            UserAccountAPI.AttributeLicenceRequest attributeLicenceRequest = getLicenceRequest(licenceAttributeeEmail, LicenceService.LICENCE_NAME_DEMO_CANNELLE);
            UserAccountAPI userAccountAPI = new UserAccountAPI(ThaleiaApplication.get().contextService, ThaleiaApplication.get().getConfiguration());
            // On récupére un JsonObject en cas de réussite ou d'échec de l'attribution
            logger.info("Appel à l'API pour création du compte de démo : token = " + token + " - email = " + licenceAttributeeEmail);
            Object result = userAccountAPI.create(token, attributeLicenceRequest);
            assertNotNull(result);
            assertTrue(result.getClass().isAssignableFrom(JsonObject.class));
            assertTrue(result.toString().contains("Insufficent rights for requesting this licence."));

        } finally {
            // On remet en place le paramètre de l'application
            ApplicationParameter applicationParameter = applicationParameterDao.findByName("licence." + demoLicenceToAttribute.getName() + ".attributors");
            if (applicationParameter == null) {
                applicationParameter = applicationParameterDao.get();
                applicationParameter.setName("licence." + demoLicenceToAttribute.getName() + ".attributors");
            }
            applicationParameter.setValue(licenceAttributorsParameterValue);

            restoreApplicationParameters();

            // Suppression du compte qui attribue la licence
            userService.deleteUserProfileData(licenceAttributor);
            User licenceAttributee = userDao.findUserByLogin(licenceAttributeeEmail);
            userService.deleteUserProfileData(licenceAttributee);
        }
    }

    /**
     * Vérifie que la création de compte avec licence est possible si on est admin et pas autorisé à transmettre la licence.
     */
    @Test
    public void createNonDemoIfAdmin() throws DetailedException {
        ApplicationParameterDao applicationParameterDao = ThaleiaApplication.get().getApplicationParameterDao();
        MailTemplateDao mailTemplateDao = new MailTemplateDao(ThaleiaApplication.get().contextService.getContextSingleton());
        UserService userService = new UserService(ThaleiaApplication.get().contextService, ThaleiaApplication.get().getConfiguration());
        UserDao userDao = new UserDao(ThaleiaApplication.get().contextService.getContextSingleton());
        LicenceService licenceService = new LicenceService(ThaleiaApplication.get().contextService, ThaleiaApplication.get().getConfiguration());
        LicenceDao licenceDao = ThaleiaApplication.get().getLicenceDao();

        storeApplicationParameters();

        Licence demoLicenceToAttribute = licenceDao.findByName(LicenceService.LICENCE_NAME_DEMO_CANNELLE);
        Licence licenceOfAttributor = licenceDao.findByName(LicenceService.LICENCE_NAME_DEMO_CANNELLE);
        assertNotNull(demoLicenceToAttribute);

        // On stocke la valeur actuelle du paramètre d'application qui contient les personnes autorisées à attribuer la
        // licence de démo Thaleia XL, pour la remettre en l'état après le test :
        String licenceAttributorsParameterValue = applicationParameterDao.getValue("licence." + demoLicenceToAttribute.getName() + ".attributors", "");

        User licenceAttributor = null;
        String licenceAttributeeEmail = null;
        try {
            // On s'assure que personne n'a le droit d'attibuer cette licence.
            ApplicationParameter applicationParameter = applicationParameterDao.findByName("licence." + demoLicenceToAttribute.getName() + ".attributors");
            if (applicationParameter == null) {
                applicationParameter = applicationParameterDao.get();
                applicationParameter.setName("licence." + demoLicenceToAttribute.getName() + ".attributors");
            }
            applicationParameter.setValue("");

            // On fabrique un compte utilisateur avec une licence valide :

            // Le nom du compte utilisateur qui va attribuer la licence
            String licenceAttributorEmail = Unique.getUniqueString(12) + "@solunea.fr";
            logger.info("Compte utilisateur de tests pour attribuer la licence : " + licenceAttributorEmail);

            // Le contenu du mail de validation
            Locale locale = Locale.ENGLISH;
            MailTemplate mailTemplate = mailTemplateDao.findByName("userAccountCreated", locale);
            String validationEmailSubject = mailTemplate.getObject();
            String validationEmailBody = mailTemplate.getBody();

            // Le lien de finalisation du compte sur cette instance Thaleia
            String accountFinalizationURL = applicationParameterDao.getValue(Configuration.APPLICATION_PARAMETER_SERVER_URL, "") + Configuration.ACCOUNT_FINALIZATION_PAGE;

            // Le point d'accès sur cette instance Thaleia
            String thaleiaLoginUrl = applicationParameterDao.getValue(Configuration.APPLICATION_PARAMETER_SERVER_URL, "");

            try {
                // création du compte de l'utilisateur qui va attribuer la licence (par l'admin)
                User admin = userDao.findUserByLogin("admin");
                userService.attributeVerifiedLicenceToAccountWithException(admin, licenceOfAttributor, licenceAttributorEmail, locale, validationEmailSubject, validationEmailBody, UserAccountAPITest.class.getName(), accountFinalizationURL, thaleiaLoginUrl, false);
            } catch (DetailedException e) {
                logger.warn(e);
                throw e;
            }
            // On vérifie que ce compte a été créé
            licenceAttributor = userDao.findUserByLogin(licenceAttributorEmail);
            assertNotNull(licenceAttributor);
            // On vérifie que son compte est valide :
            if (licenceOfAttributor.getIsDemo()) {
                assertTrue(licenceService.isUserValid(licenceAttributor, false));
            } else {
                assertTrue(licenceService.isUserValid(licenceAttributor, true));
            }
            // On fixe le mot de passe
            // Création de la mise à jour (équivalente au formulaire de la page de finalisation du compte)
            UpdateUserRequest updateUserRequest = new UpdateUserRequest();
            updateUserRequest.company = "Test Company";
            updateUserRequest.name = "Test Username";
            updateUserRequest.phone = "01 02 03 04 05";
            updateUserRequest.password = "test_password";
            // On soumet la finalisation du compte
            userService.updateUserAccountAndGetLoginToken(updateUserRequest, licenceAttributor, false);
            // On rend cet utilisateur admin
            licenceAttributor.setIsAdmin(true);

            // Préparation d'un token pour appel à l'api
            ApiV1Service apiV1Service = new ApiV1Service(ThaleiaApplication.get().contextService, ThaleiaApplication.get().getConfiguration());
            String token = apiV1Service.createToken(licenceAttributor.getLogin(), updateUserRequest.password);
            assertFalse(token.isEmpty());
            logger.debug("Vérification du token " + token);
            ApiTokenDao apiTokenDao = new ApiTokenDao(ThaleiaApplication.get().contextService.getContextSingleton());
            assertFalse(apiTokenDao.findByValue(token).isEmpty());

            // Cet utilisateur va tenter d'attribuer une licence : attribution ok attendue
            licenceAttributeeEmail = Unique.getUniqueString(12) + "@solunea.fr";
            UserAccountAPI.AttributeLicenceRequest attributeLicenceRequest = getLicenceRequest(licenceAttributeeEmail, LicenceService.LICENCE_NAME_DEMO_CANNELLE);
            UserAccountAPI userAccountAPI = new UserAccountAPI(ThaleiaApplication.get().contextService, ThaleiaApplication.get().getConfiguration());
            // On récupére un JsonObject en cas de réussite ou d'échec de l'attribution
            Object result = userAccountAPI.create(token, attributeLicenceRequest);
            assertNotNull(result);
            assertTrue(result.getClass().isAssignableFrom(UserAccountAPI.Response.class));
            assertEquals("Account created and licence attributed.", ((UserAccountAPI.Response) result).getMessage());

        } finally {
            // On remet en place le paramètre de l'application
            ApplicationParameter applicationParameter = applicationParameterDao.findByName("licence." + demoLicenceToAttribute.getName() + ".attributors");
            if (applicationParameter == null) {
                applicationParameter = applicationParameterDao.get();
                applicationParameter.setName("licence." + demoLicenceToAttribute.getName() + ".attributors");
            }
            applicationParameter.setValue(licenceAttributorsParameterValue);

            restoreApplicationParameters();

            // Suppression du compte qui attribue la licence
            userService.deleteUserProfileData(licenceAttributor);
            User licenceAttributee = userDao.findUserByLogin(licenceAttributeeEmail);
            userService.deleteUserProfileData(licenceAttributee);
        }
    }

    /**
     * Vérifie que la création de compte avec licence est possible si on est autorisé à transmettre la licence, mais pas admin.
     */
    @Test
    public void createDemo() throws DetailedException {
        ApplicationParameterDao applicationParameterDao = ThaleiaApplication.get().getApplicationParameterDao();
        MailTemplateDao mailTemplateDao = new MailTemplateDao(ThaleiaApplication.get().contextService.getContextSingleton());
        UserService userService = new UserService(ThaleiaApplication.get().contextService, ThaleiaApplication.get().getConfiguration());
        UserDao userDao = new UserDao(ThaleiaApplication.get().contextService.getContextSingleton());
        LicenceService licenceService = new LicenceService(ThaleiaApplication.get().contextService, ThaleiaApplication.get().getConfiguration());
        LicenceDao licenceDao = ThaleiaApplication.get().getLicenceDao();

        storeApplicationParameters();

        Licence demoLicenceToAttribute = licenceDao.findByName(LicenceService.LICENCE_NAME_DEMO_CANNELLE);
        Licence licenceOfAttributor = licenceDao.findByName(LicenceService.LICENCE_NAME_DEMO_CANNELLE);
        assertNotNull(demoLicenceToAttribute);

        // On stocke la valeur actuelle du paramètre d'application qui contient les personnes autorisées à attribuer la
        // licence de démo Thaleia XL, pour la remettre en l'état après le test :
        String licenceAttributorsParameterValue = applicationParameterDao.getValue("licence." + demoLicenceToAttribute.getName() + ".attributors", "");

        User licenceAttributor = null;
        String licenceAttributeeEmail = null;
        try {
            // On s'assure que personne n'a le droit d'attibuer cette licence.
            ApplicationParameter applicationParameter = applicationParameterDao.findByName("licence." + demoLicenceToAttribute.getName() + ".attributors");
            if (applicationParameter == null) {
                applicationParameter = applicationParameterDao.get();
                applicationParameter.setName("licence." + demoLicenceToAttribute.getName() + ".attributors");
            }
            applicationParameter.setValue("");

            // On fabrique un compte utilisateur avec une licence valide :

            // Le nom du compte utilisateur qui va attribuer la licence
            String licenceAttributorEmail = Unique.getUniqueString(12) + "@solunea.fr";
            logger.info("Compte utilisateur de tests pour attribuer la licence : " + licenceAttributorEmail);
            // On donne à l'utilisateur le droit d'attribuer des licences
            applicationParameter.setValue("test@solunea.fr," + licenceAttributorEmail);
            applicationParameterDao.save(applicationParameter);

            // Le contenu du mail de validation
            Locale locale = Locale.ENGLISH;
            MailTemplate mailTemplate = mailTemplateDao.findByName("userAccountCreated", locale);
            String validationEmailSubject = mailTemplate.getObject();
            String validationEmailBody = mailTemplate.getBody();

            // Le lien de finalisation du compte sur cette instance Thaleia
            String accountFinalizationURL = applicationParameterDao.getValue(Configuration.APPLICATION_PARAMETER_SERVER_URL, "") + Configuration.ACCOUNT_FINALIZATION_PAGE;

            // Le point d'accès sur cette instance Thaleia
            String thaleiaLoginUrl = applicationParameterDao.getValue(Configuration.APPLICATION_PARAMETER_SERVER_URL, "");

            try {
                // création du compte de l'utilisateur qui va attribuer la licence (par l'admin)
                User admin = userDao.findUserByLogin("admin");
                userService.attributeVerifiedLicenceToAccountWithException(admin, licenceOfAttributor, licenceAttributorEmail, locale, validationEmailSubject, validationEmailBody, UserAccountAPITest.class.getName(), accountFinalizationURL, thaleiaLoginUrl, false);
            } catch (DetailedException e) {
                logger.warn(e);
                throw e;
            }
            // On vérifie que ce compte a été créé
            licenceAttributor = userDao.findUserByLogin(licenceAttributorEmail);
            assertNotNull(licenceAttributor);
            // On vérifie que son compte est valide :
            if (licenceOfAttributor.getIsDemo()) {
                assertTrue(licenceService.isUserValid(licenceAttributor, false));
            } else {
                assertTrue(licenceService.isUserValid(licenceAttributor, true));
            }
            // On fixe le mot de passe
            // Création de la mise à jour (équivalente au formulaire de la page de finalisation du compte)
            UpdateUserRequest updateUserRequest = new UpdateUserRequest();
            updateUserRequest.company = "Test Company";
            updateUserRequest.name = "Test Username";
            updateUserRequest.phone = "01 02 03 04 05";
            updateUserRequest.password = "test_password";
            // On soumet la finalisation du compte
            userService.updateUserAccountAndGetLoginToken(updateUserRequest, licenceAttributor, false);

            // Préparation d'un token pour appel à l'api
            ApiV1Service apiV1Service = new ApiV1Service(ThaleiaApplication.get().contextService, ThaleiaApplication.get().getConfiguration());
            String token = apiV1Service.createToken(licenceAttributor.getLogin(), updateUserRequest.password);
            assertFalse(token.isEmpty());

            // Cet utilisateur va tenter d'attribuer une licence : attribution ok attendue
            licenceAttributeeEmail = Unique.getUniqueString(12) + "@solunea.fr";
            UserAccountAPI.AttributeLicenceRequest attributeLicenceRequest = getLicenceRequest(licenceAttributeeEmail, LicenceService.LICENCE_NAME_DEMO_CANNELLE);
            UserAccountAPI userAccountAPI = new UserAccountAPI(ThaleiaApplication.get().contextService, ThaleiaApplication.get().getConfiguration());
            // On récupére un JsonObject en cas de réussite ou d'échec de l'attribution
            Object result = userAccountAPI.create(token, attributeLicenceRequest);
            assertNotNull(result);
            assertTrue(result.getClass().isAssignableFrom(UserAccountAPI.Response.class));
            assertEquals("Account created and licence attributed.", ((UserAccountAPI.Response) result).getMessage());

        } finally {
            // On remet en place le paramètre de l'application
            ApplicationParameter applicationParameter = applicationParameterDao.findByName("licence." + demoLicenceToAttribute.getName() + ".attributors");
            if (applicationParameter == null) {
                applicationParameter = applicationParameterDao.get();
                applicationParameter.setName("licence." + demoLicenceToAttribute.getName() + ".attributors");
            }
            applicationParameter.setValue(licenceAttributorsParameterValue);

            restoreApplicationParameters();

            // Suppression du compte qui attribue la licence
            userService.deleteUserProfileData(licenceAttributor);
            User licenceAttributee = userDao.findUserByLogin(licenceAttributeeEmail);
            userService.deleteUserProfileData(licenceAttributee);
        }
    }

    /**
     * Vérifie que lors de l'attribution d'une démo sur un compt existant, on obtient une redirection.
     */
    @Test
    public void getRedirectWhenDemoOnExistingAccount() throws DetailedException {
        ApplicationParameterDao applicationParameterDao = ThaleiaApplication.get().getApplicationParameterDao();
        MailTemplateDao mailTemplateDao = new MailTemplateDao(ThaleiaApplication.get().contextService.getContextSingleton());
        UserService userService = new UserService(ThaleiaApplication.get().contextService, ThaleiaApplication.get().getConfiguration());
        UserDao userDao = new UserDao(ThaleiaApplication.get().contextService.getContextSingleton());
        LicenceService licenceService = new LicenceService(ThaleiaApplication.get().contextService, ThaleiaApplication.get().getConfiguration());
        LicenceDao licenceDao = ThaleiaApplication.get().getLicenceDao();

        storeApplicationParameters();

        Licence firstDemoLicenceToAttribute = licenceDao.findByName(LicenceService.LICENCE_NAME_DEMO_CANNELLE);
        Licence secondDemoLicenceToAttribute = licenceDao.findByName(LicenceService.LICENCE_NAME_DEMO_ACTION);
        Licence licenceOfAttributor = licenceDao.findByName(LicenceService.LICENCE_NAME_DEMO_CANNELLE);
        assertNotNull(firstDemoLicenceToAttribute);

        // On stocke la valeur actuelle du paramètre d'application qui contient les personnes autorisées à attribuer la
        // licence de démo Thaleia XL, pour la remettre en l'état après le test :
        String firstLicenceAttributorsParameterValue = applicationParameterDao.getValue("licence." + firstDemoLicenceToAttribute.getName() + ".attributors", "");
        String secondLicenceAttributorsParameterValue = applicationParameterDao.getValue("licence." + secondDemoLicenceToAttribute.getName() + ".attributors", "");

        User licenceAttributor = null;
        String licenceAttributeeEmail = null;
        try {
            // On s'assure que personne n'a le droit d'attibuer ces licences.
            ApplicationParameter firstLicenceApplicationParameter = applicationParameterDao.findByName("licence." + firstDemoLicenceToAttribute.getName() + ".attributors");
            if (firstLicenceApplicationParameter == null) {
                firstLicenceApplicationParameter = applicationParameterDao.get();
                firstLicenceApplicationParameter.setName("licence." + firstDemoLicenceToAttribute.getName() + ".attributors");
            }
            firstLicenceApplicationParameter.setValue("");
            ApplicationParameter secondLicenceApplicationParameter = applicationParameterDao.findByName("licence." + secondDemoLicenceToAttribute.getName() + ".attributors");
            if (secondLicenceApplicationParameter == null) {
                secondLicenceApplicationParameter = applicationParameterDao.get();
                secondLicenceApplicationParameter.setName("licence." + secondDemoLicenceToAttribute.getName() + ".attributors");
            }
            secondLicenceApplicationParameter.setValue("");

            // On fabrique un compte utilisateur avec une licence valide :

            // Le nom du compte utilisateur qui va attribuer la licence
            String licenceAttributorEmail = Unique.getUniqueString(12) + "@solunea.fr";
            logger.info("Compte utilisateur de tests pour attribuer la licence : " + licenceAttributorEmail);
            // On donne à l'utilisateur le droit d'attribuer des licences
            firstLicenceApplicationParameter.setValue("test@solunea.fr," + licenceAttributorEmail);
            applicationParameterDao.save(firstLicenceApplicationParameter);
            secondLicenceApplicationParameter.setValue(licenceAttributorEmail + ",test@solunea.fr, ");
            applicationParameterDao.save(secondLicenceApplicationParameter);

            // Le contenu du mail de validation
            Locale locale = Locale.ENGLISH;
            MailTemplate mailTemplate = mailTemplateDao.findByName("userAccountCreated", locale);
            String validationEmailSubject = mailTemplate.getObject();
            String validationEmailBody = mailTemplate.getBody();

            // Le lien de finalisation du compte sur cette instance Thaleia
            String accountFinalizationURL = applicationParameterDao.getValue(Configuration.APPLICATION_PARAMETER_SERVER_URL, "") + Configuration.ACCOUNT_FINALIZATION_PAGE;

            // Le point d'accès sur cette instance Thaleia
            String thaleiaLoginUrl = applicationParameterDao.getValue(Configuration.APPLICATION_PARAMETER_SERVER_URL, "");

            try {
                // création du compte de l'utilisateur qui va attribuer la licence (par l'admin)
                User admin = userDao.findUserByLogin("admin");
                userService.attributeVerifiedLicenceToAccountWithException(admin, licenceOfAttributor, licenceAttributorEmail, locale, validationEmailSubject, validationEmailBody, UserAccountAPITest.class.getName(), accountFinalizationURL, thaleiaLoginUrl, false);
            } catch (DetailedException e) {
                logger.warn(e);
                throw e;
            }
            // On vérifie que ce compte a été créé
            licenceAttributor = userDao.findUserByLogin(licenceAttributorEmail);
            assertNotNull(licenceAttributor);
            // On vérifie que son compte est valide :
            if (licenceOfAttributor.getIsDemo()) {
                assertTrue(licenceService.isUserValid(licenceAttributor, false));
            } else {
                assertTrue(licenceService.isUserValid(licenceAttributor, true));
            }
            // On fixe le mot de passe
            // Création de la mise à jour (équivalente au formulaire de la page de finalisation du compte)
            UpdateUserRequest updateUserRequest = new UpdateUserRequest();
            updateUserRequest.company = "Test Company";
            updateUserRequest.name = "Test Username";
            updateUserRequest.phone = "01 02 03 04 05";
            updateUserRequest.password = "test_password";
            // On soumet la finalisation du compte
            userService.updateUserAccountAndGetLoginToken(updateUserRequest, licenceAttributor, false);

            // Préparation d'un token pour appel à l'api
            ApiV1Service apiV1Service = new ApiV1Service(ThaleiaApplication.get().contextService, ThaleiaApplication.get().getConfiguration());
            String token = apiV1Service.createToken(licenceAttributor.getLogin(), updateUserRequest.password);
            assertFalse(token.isEmpty());

            // Cet utilisateur va tenter d'attribuer une licence : attribution ok attendue
            licenceAttributeeEmail = Unique.getUniqueString(12) + "@solunea.fr";
            UserAccountAPI.AttributeLicenceRequest attributeLicenceRequest = getLicenceRequest(licenceAttributeeEmail, LicenceService.LICENCE_NAME_DEMO_CANNELLE);
            UserAccountAPI userAccountAPI = new UserAccountAPI(ThaleiaApplication.get().contextService, ThaleiaApplication.get().getConfiguration());
            // On récupére un JsonObject en cas de réussite ou d'échec de l'attribution
            Object result = userAccountAPI.create(token, attributeLicenceRequest);
            assertNotNull(result);
            assertTrue(result.getClass().isAssignableFrom(UserAccountAPI.Response.class));
            assertEquals("Account created and licence attributed.", ((UserAccountAPI.Response) result).getMessage());

            // On attribue maintenant une autre licence : attribution ok attendue, mais pas de création de compte, donc
            // présence d'une URL de redirection dans la réponse
            Object secondResult = userAccountAPI.create(token, getLicenceRequest(licenceAttributeeEmail, secondDemoLicenceToAttribute.getName()));
            assertNotNull(secondResult);
            assertTrue(secondResult.getClass().isAssignableFrom(UserAccountAPI.RedirectResponse.class));
            assertEquals("Licence attributed to existing account.", ((UserAccountAPI.RedirectResponse) secondResult).getMessage());
            assertFalse(((UserAccountAPI.RedirectResponse) secondResult).getCode().isEmpty());
            assertFalse(((UserAccountAPI.RedirectResponse) secondResult).getRedirect().isEmpty());
            logger.info("Redirection de l'attribution d'une licence à un compte existant : " + ((UserAccountAPI.RedirectResponse) secondResult).getRedirect());

            // On vérifie que cet utilsiateur peut s'identifier, et est bien possesseur des deux licences
            User licenceAttributeeUser = userDao.findUserByLogin(licenceAttributeeEmail);
            assertNotNull(licenceAttributeeUser);
            assertTrue(licenceService.isUserValid(licenceAttributeeUser, false));
            assertTrue(licenceService.isLicenceHolded(licenceAttributeeUser, firstDemoLicenceToAttribute));
            assertTrue(licenceService.isLicenceHolded(licenceAttributeeUser, secondDemoLicenceToAttribute));
            // On teste l'identification sans vérifier le mot de passe, car celui-ci n'est pas encore défini :
            // le nouvel utilisateur n'a pas encore terminé de compléter son compte
            assertTrue(ThaleiaSession.get().authenticate(licenceAttributeeEmail, null));

        } finally {
            // On remet en place les paramètres de l'application
            ApplicationParameter applicationParameter = applicationParameterDao.findByName("licence." + firstDemoLicenceToAttribute.getName() + ".attributors");
            if (applicationParameter == null) {
                applicationParameter = applicationParameterDao.get();
                applicationParameter.setName("licence." + firstDemoLicenceToAttribute.getName() + ".attributors");
            }
            applicationParameter.setValue(firstLicenceAttributorsParameterValue);
            ApplicationParameter secondLicenceApplicationParameter = applicationParameterDao.findByName("licence." + secondDemoLicenceToAttribute.getName() + ".attributors");
            if (secondLicenceApplicationParameter == null) {
                secondLicenceApplicationParameter = applicationParameterDao.get();
                secondLicenceApplicationParameter.setName("licence." + secondDemoLicenceToAttribute.getName() + ".attributors");
            }
            secondLicenceApplicationParameter.setValue(secondLicenceAttributorsParameterValue);

            restoreApplicationParameters();

            // Suppression des comptes utilisateurs créés
            userService.deleteUserProfileData(licenceAttributor);
            User licenceAttributee = userDao.findUserByLogin(licenceAttributeeEmail);
            userService.deleteUserProfileData(licenceAttributee);
        }
    }

    /**
     * Vérifie qu'il est possible de créer un compte et lui attribuer une licence de démo, puis de compléter ce compte,
     * et enfin d'obtenir une redirection vers la page de login, et que ce login fonctionne.
     */
    @Test
    public void loginForDemoCompleteProcess() throws DetailedException {
        ApplicationParameterDao applicationParameterDao = ThaleiaApplication.get().getApplicationParameterDao();
        MailTemplateDao mailTemplateDao = new MailTemplateDao(ThaleiaApplication.get().contextService.getContextSingleton());
        UserService userService = new UserService(ThaleiaApplication.get().contextService, ThaleiaApplication.get().getConfiguration());
        UserDao userDao = new UserDao(ThaleiaApplication.get().contextService.getContextSingleton());
        LicenceService licenceService = new LicenceService(ThaleiaApplication.get().contextService, ThaleiaApplication.get().getConfiguration());
        LicenceDao licenceDao = ThaleiaApplication.get().getLicenceDao();
        AccountRequestDao accountRequestDao = new AccountRequestDao(ThaleiaApplication.get().contextService.getContextSingleton());

        storeApplicationParameters();

        Licence demoLicenceToAttribute = licenceDao.findByName(LicenceService.LICENCE_NAME_DEMO_CANNELLE);
        Licence licenceOfAttributor = licenceDao.findByName(LicenceService.LICENCE_NAME_DEMO_CANNELLE);
        assertNotNull(demoLicenceToAttribute);

        // On stocke la valeur actuelle du paramètre d'application qui contient les personnes autorisées à attribuer la
        // licence de démo Thaleia XL, pour la remettre en l'état après le test :
        String licenceAttributorsParameterValue = applicationParameterDao.getValue("licence." + demoLicenceToAttribute.getName() + ".attributors", "");

        User licenceAttributor = null;
        String licenceAttributeeEmail = null;
        try {
            // On s'assure que personne n'a le droit d'attibuer ces licences.
            ApplicationParameter licenceApplicationParameter = applicationParameterDao.findByName("licence." + demoLicenceToAttribute.getName() + ".attributors");
            if (licenceApplicationParameter == null) {
                licenceApplicationParameter = applicationParameterDao.get();
                licenceApplicationParameter.setName("licence." + demoLicenceToAttribute.getName() + ".attributors");
            }
            licenceApplicationParameter.setValue("");

            // On fabrique un compte utilisateur avec une licence valide :

            // Le nom du compte utilisateur qui va attribuer la licence
            String licenceAttributorEmail = Unique.getUniqueString(12) + "@solunea.fr";
            logger.info("Compte utilisateur de tests pour attribuer la licence : " + licenceAttributorEmail);
            // On donne à l'utilisateur le droit d'attribuer cette licence
            licenceApplicationParameter.setValue("test@solunea.fr," + licenceAttributorEmail);
            applicationParameterDao.save(licenceApplicationParameter);

            // Le contenu du mail de validation
            Locale locale = Locale.ENGLISH;
            MailTemplate mailTemplate = mailTemplateDao.findByName("userAccountCreated", locale);
            String validationEmailSubject = mailTemplate.getObject();
            String validationEmailBody = mailTemplate.getBody();

            // Le lien de finalisation du compte sur cette instance Thaleia
            String accountFinalizationURL = applicationParameterDao.getValue(Configuration.APPLICATION_PARAMETER_SERVER_URL, "") + Configuration.ACCOUNT_FINALIZATION_PAGE;

            // Le point d'accès sur cette instance Thaleia
            String thaleiaLoginUrl = applicationParameterDao.getValue(Configuration.APPLICATION_PARAMETER_SERVER_URL, "");

            try {
                // création du compte de l'utilisateur qui va attribuer la licence (par l'admin)
                User admin = userDao.findUserByLogin("admin");
                userService.attributeVerifiedLicenceToAccountWithException(admin, licenceOfAttributor, licenceAttributorEmail, locale, validationEmailSubject, validationEmailBody, UserAccountAPITest.class.getName(), accountFinalizationURL, thaleiaLoginUrl, false);
            } catch (DetailedException e) {
                logger.warn(e);
                throw e;
            }
            // On vérifie que ce compte a été créé
            licenceAttributor = userDao.findUserByLogin(licenceAttributorEmail);
            assertNotNull(licenceAttributor);
            // On vérifie que son compte est valide :
            if (licenceOfAttributor.getIsDemo()) {
                assertTrue(licenceService.isUserValid(licenceAttributor, false));
            } else {
                assertTrue(licenceService.isUserValid(licenceAttributor, true));
            }
            // On fixe le mot de passe
            // Création de la mise à jour (équivalente au formulaire de la page de finalisation du compte)
            UpdateUserRequest attributorUpdateUserRequest = new UpdateUserRequest();
            attributorUpdateUserRequest.company = "Test Company";
            attributorUpdateUserRequest.name = "Test Username";
            attributorUpdateUserRequest.phone = "01 02 03 04 05";
            attributorUpdateUserRequest.password = "test_password";
            // On soumet la finalisation du compte
            userService.updateUserAccountAndGetLoginToken(attributorUpdateUserRequest, licenceAttributor, false);

            // Préparation d'un token pour appel à l'api
            ApiV1Service apiV1Service = new ApiV1Service(ThaleiaApplication.get().contextService, ThaleiaApplication.get().getConfiguration());
            String token = apiV1Service.createToken(licenceAttributor.getLogin(), attributorUpdateUserRequest.password);
            assertFalse(token.isEmpty());

            // Cet utilisateur va tenter d'attribuer une licence : attribution ok attendue
            licenceAttributeeEmail = Unique.getUniqueString(12) + "@solunea.fr";
            logger.info("Compte utilisateur à créer pour attribuer la licence de démo : " + licenceAttributeeEmail);
            UserAccountAPI.AttributeLicenceRequest attributeLicenceRequest = getLicenceRequest(licenceAttributeeEmail, LicenceService.LICENCE_NAME_DEMO_CANNELLE);
            UserAccountAPI userAccountAPI = new UserAccountAPI(ThaleiaApplication.get().contextService, ThaleiaApplication.get().getConfiguration());
            // On récupére un JsonObject en cas de réussite ou d'échec de l'attribution
            Object result = userAccountAPI.create(token, attributeLicenceRequest);
            assertNotNull(result);
            assertTrue(result.getClass().isAssignableFrom(UserAccountAPI.Response.class));
            assertEquals("Account created and licence attributed.", ((UserAccountAPI.Response) result).getMessage());

            // On vérifie que cet utilisiateur peut s'identifier, et est bien possesseur de la licence
            User licenceAttributeeUser = userDao.findUserByLogin(licenceAttributeeEmail);
            assertNotNull(licenceAttributeeUser);
            assertTrue(licenceService.isUserValid(licenceAttributeeUser, false));
            assertTrue(licenceService.isLicenceHolded(licenceAttributeeUser, demoLicenceToAttribute));
            // On teste l'identification sans vérifier le mot de passe, car celui-ci n'est pas encore défini :
            // le nouvel utilisateur n'a pas encore terminé de compléter son compte
            assertTrue(ThaleiaSession.get().authenticate(licenceAttributeeEmail, null));

            // On récupère l'URL de validation du compte qui est dans le mail envoyé
            List<AccountRequest> accountRequests = accountRequestDao.findByEmail(licenceAttributeeEmail);
            assertFalse(accountRequests.isEmpty());
            assertTrue((accountRequests.size() == 1));
            AccountRequest accountRequest = accountRequests.get(0);
            String accountValidationUrl = userService.getValidationPageUrl(accountRequest);

            // On ouvre cette page
            ThaleiaSession.get().invalidate();
            assertNull(ThaleiaSession.get().getAuthenticatedUser());
            PageParameters pageParameters = new PageParameters();
            String validationCode = accountValidationUrl.substring(accountValidationUrl.lastIndexOf("=") + 1);
            assertFalse(validationCode.isEmpty());
            pageParameters.add(Configuration.EMAIL_VALIDATION_CODE_PARAMETER_NAME, validationCode);
            // L'appel de cette page va valider l'email dans les propriétés du compte, et est censé rediriger vers le
            // formulaire de finalisation du compte
            assertThrows(RedirectToFinalizeAccountPageException.class, () -> tester.startPage(new EmailValidationPage(pageParameters)));

            // On ne suit pas la redirection, mais on instancie la page de finalisation du compte pour le simuler.
            // On ajoute les en-têtes dans le header, comme la RedirectToFinalizeAccountPageException
            tester.getRequest().addHeader(RedirectToFinalizeAccountPageException.THALEIA_USER_EMAIL_HEADER, licenceAttributeeEmail);
            // On fabrique le token pour le user qui obtient la démo (et non pour celui qui a créé ce suer) comme le fait la RedirectToFinalizeAccountPageException
            String attributeeToken = RedirectToFinalizeAccountPageException.generateToken(accountRequest).getValue();
            tester.getRequest().addHeader(RedirectToFinalizeAccountPageException.THALEIA_API_TOKEN_HEADER, attributeeToken);
            tester.getRequest().addHeader(RedirectToFinalizeAccountPageException.THALEIA_INTERNAL_SIGNIN_HEADER, String.valueOf(accountRequest.getCreatedUser().isInternalSignin()));
            // Ouverture de la page
            tester.startPage(new FinalizeAccountPage());
            // On teste le formulaire de complétion du compte
            FormTester formTester = tester.newFormTester("form", false);
            formTester.setValue("name", "Nom du compte de test");
            // Recherche du champ password, dans un passwordGroup
            Component passwordField = formTester.getForm().get("passwordGroup").get("password");
            String licenceAttributeePassword = "solunea1234";
            formTester.setValue(passwordField, licenceAttributeePassword);
            // Normalement, la validation de ce formulaire déclenche un appel à l'API de Thaleia pour la finalisation du compte :
            // formTester.submit("save");
            // tester.assertNoErrorMessage();
            // Or, les appels à l'API Thaleia ne peuvent pas être faits sans instance d'écoute. On va donc simuler cet appel
            // par un appel direct aux objets de l'API, sans requête HTTP.

            // On récupère l'objet édité dans le formulaire de la page de finalsation
            AccountDetails accountDetails = ((AccountDetails) tester.getLastRenderedPage().getDefaultModelObject());

            // Appel de l'API Thaleia pour le traitement de la finalisation du compte
            UpdateUserRequest attributeeUpdateUserRequest = new UpdateUserRequest();
            attributeeUpdateUserRequest.name = accountDetails.getName();
            attributeeUpdateUserRequest.password = licenceAttributeePassword;
            attributeeUpdateUserRequest.phone = accountDetails.getPhone();
            attributeeUpdateUserRequest.company = accountDetails.getCompany();
            Object updateResult = userAccountAPI.update(attributeeToken, licenceAttributeeEmail, attributeeUpdateUserRequest);
            // Vérification de la réponse :
            assertNotNull(updateResult);
            assertTrue(updateResult.getClass().isAssignableFrom(UserAccountAPI.LoginResponse.class));
            assertEquals("User account updated.", ((UserAccountAPI.LoginResponse) updateResult).getMessage());
            String loginUrl = ((UserAccountAPI.LoginResponse) updateResult).getLoginUrl();
            // loginURL du type https://site/instance/Web/SSO?jwt=123456
            logger.info("URL de login avec JWT pour le compte de démo : " + loginUrl);

            // On vérifie que cet utilisateur peut s'identifier, et est bien possesseur de la licence
            User updatedUser = userDao.findUserByLogin(licenceAttributeeEmail);
            assertNotNull(updatedUser);
            assertTrue(licenceService.isUserValid(updatedUser, false));
            assertTrue(licenceService.isLicenceHolded(updatedUser, demoLicenceToAttribute));
            // On teste l'identification avec vérification du mot de passe,
            assertTrue(ThaleiaSession.get().authenticate(licenceAttributeeEmail, licenceAttributeePassword));

            // Vérification de l'identification par le jeton JWT
            // On ne fait pas une redirection sur cette URL, car il faudrait une application Thaleia en cours de fonctionnement.
            // On va plutôt simuler cette redirection en appelant la page Web/SSO et en donnant en paramètre le JWT obtenu précédemment.
            // On commence par vérifier qu'il n'est pas identifié en session :
            ThaleiaSession.get().invalidate();
            assertNull(ThaleiaSession.get().getAuthenticatedUser());
            // Récupération du token dans l'URL de connexion
            String jwt = loginUrl.substring(loginUrl.indexOf("=") + 1);
            PageParameters jwtParameters = new PageParameters();
            jwtParameters.add(JWTAuthenticationPage.JWT_PARAMETER_NAME, jwt);
            // Ouverture de la page d'identification avec le JWT
            assertThrows(RedirectToUrlException.class, () -> tester.startPage(new JWTAuthenticationPage(jwtParameters)));
            // Est-ce que l'utilisateur a bien été identifié par le JWT ?
            assertNotNull(ThaleiaSession.get().getAuthenticatedUser());
            assertEquals(licenceAttributeeEmail, ThaleiaSession.get().getAuthenticatedUser().getLogin());

        } finally {
            // On remet en place les paramètres de l'application
            ApplicationParameter applicationParameter = applicationParameterDao.findByName("licence." + demoLicenceToAttribute.getName() + ".attributors");
            if (applicationParameter == null) {
                applicationParameter = applicationParameterDao.get();
                applicationParameter.setName("licence." + demoLicenceToAttribute.getName() + ".attributors");
            }
            applicationParameter.setValue(licenceAttributorsParameterValue);

            restoreApplicationParameters();

            // Suppression des comptes utilisateurs créés
            userService.deleteUserProfileData(licenceAttributor);
            User licenceAttributee = userDao.findUserByLogin(licenceAttributeeEmail);
            userService.deleteUserProfileData(licenceAttributee);
        }
    }
}
