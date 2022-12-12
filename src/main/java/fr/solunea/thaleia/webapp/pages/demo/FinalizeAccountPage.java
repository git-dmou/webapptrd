package fr.solunea.thaleia.webapp.pages.demo;

import fr.solunea.thaleia.model.AccountRequest;
import fr.solunea.thaleia.model.User;
import fr.solunea.thaleia.model.dao.ApplicationParameterDao;
import fr.solunea.thaleia.service.utils.Configuration;
import fr.solunea.thaleia.webapp.ThaleiaApplication;
import fr.solunea.thaleia.webapp.api.UserAccountAPI;
import fr.solunea.thaleia.webapp.pages.BasePage;
import fr.solunea.thaleia.webapp.pages.ErrorPage;
import fr.solunea.thaleia.webapp.panels.PanelUtils;
import fr.solunea.thaleia.webapp.panels.ThaleiaFeedbackPanel;
import fr.solunea.thaleia.webapp.panels.ThaleiaLightFeedbackPanel;
import fr.solunea.thaleia.webapp.security.ThaleiaSession;
import fr.solunea.thaleia.webapp.utils.CSSClassRemover;
import fr.solunea.thaleia.webapp.utils.Downloader;
import fr.solunea.thaleia.webapp.utils.MessagesUtils;
import fr.solunea.thaleia.webapp.utils.SeriousPasswordValidator;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.log4j.Logger;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.RestartResponseException;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.ajax.markup.html.IndicatingAjaxButton;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.PasswordTextField;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.model.*;
import org.apache.wicket.request.flow.RedirectToUrlException;
import org.apache.wicket.request.http.WebRequest;
import org.apache.wicket.request.resource.PackageResourceReference;
import org.apache.wicket.util.string.StringValue;
import org.apache.wicket.validation.IValidationError;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class FinalizeAccountPage extends BasePage {

    private static final Logger logger = Logger.getLogger(FinalizeAccountPage.class);
    private final IModel<String> clearPasswordModel = new Model<>("");
    private final String apiToken;
    private final ApplicationParameterDao applicationParameterDao;
    protected ThaleiaFeedbackPanel feedbackPanel;
    protected Form<AccountDetails> form;

    @SuppressWarnings({"unused", "WeakerAccess"})
    public FinalizeAccountPage() {
        super();

        applicationParameterDao = new ApplicationParameterDao(ThaleiaSession.get().getContextService().getContextSingleton());

        // On est identifié, avec un compte déjà finalisé -> page d'accueil des utilisateurs identifiés
        if (ThaleiaSession.get().isSignedIn()) {
            User user = ThaleiaSession.get().getAuthenticatedUser();
            AccountRequest accountRequest = user.getCreatedByRequest();
            if (accountRequest != null) {
                if (getIsAccountFinalized(accountRequest)) {
                    throw new RestartResponseException(ThaleiaApplication.get().getRedirectionPage(Configuration.AUTHENTIFIED_USERS_WELCOME_PAGE, Configuration.DEFAULT_AUTHENTIFIED_USERS_WELCOME_PAGE, Configuration.HOME_MOUNT_POINT));
                }
            }
        }

        // On est identifié, mais l'email du compte n'est pas validé -> message de redirection vers le processus de validation de l'email
        if (ThaleiaSession.get().isSignedIn() && ThaleiaSession.get().getAuthenticatedUser() != null) {
            User user = ThaleiaSession.get().getAuthenticatedUser();
            if (!user.getIsEmailValidated()) {
                error(MessagesUtils.getLocalizedMessage("email.notvalidated", FinalizeAccountPage.class, (Object[]) null));
                throw new RestartResponseException(ErrorPage.class);
            }
        }

        // On recherche les en-têtes :
        // ThaleiaAPIToken = Token d’identification pour l’appel à l’API Thaleia
        // ThaleiaUserEmail = Email du compte utilisateur à compléter
        // ThaleiaInternalSignIn = Si false, alors le compte n’est pas un compte identifié par un tiers (ex : un compte créé avec son compte Google), on ne demande donc pas de fixer un mot de passe. Si true, on demande de saisir un mot de passe.
        WebRequest request = (WebRequest) getRequest();
        StringValue userEmail = request.getPostParameters().getParameterValue(RedirectToFinalizeAccountPageException.THALEIA_USER_EMAIL_HEADER);
        if (userEmail.isNull() || userEmail.toString().isEmpty()) {
            error(MessagesUtils.getLocalizedMessage("email.notfound", FinalizeAccountPage.class, (Object[]) null));
            throw new RestartResponseException(ErrorPage.class);
        }
        StringValue apiToken = request.getPostParameters().getParameterValue(RedirectToFinalizeAccountPageException.THALEIA_API_TOKEN_HEADER);
        if (apiToken.isNull() || apiToken.toString().isEmpty()) {
            error(MessagesUtils.getLocalizedMessage("apiToken.notfound", FinalizeAccountPage.class, (Object[]) null));
            throw new RestartResponseException(ErrorPage.class);
        }
        this.apiToken = apiToken.toString();
        StringValue internalSignin = request.getPostParameters().getParameterValue(RedirectToFinalizeAccountPageException.THALEIA_INTERNAL_SIGNIN_HEADER);
        if (internalSignin.isNull() || internalSignin.toString().isEmpty()) {
            error(MessagesUtils.getLocalizedMessage("internalSignin.notfound", FinalizeAccountPage.class, (Object[]) null));
            throw new RestartResponseException(ErrorPage.class);
        }
        boolean isInternalSignin;
        if ("true".equalsIgnoreCase(internalSignin.toString())) {
            isInternalSignin = true;
        } else if ("false".equalsIgnoreCase(internalSignin.toString())) {
            isInternalSignin = false;
        } else {
            error(MessagesUtils.getLocalizedMessage("internalSignin.invalid", FinalizeAccountPage.class, (Object[]) null));
            throw new RestartResponseException(ErrorPage.class);
        }

        // On fabrique un modèle de compte utilisateur vide SANS PASSER PAR UN DAO ! En effet, on veut passer par l'API Thaleia
        // pour la finalisation du compte, et non un accès direct à Cayenne.
        AccountDetails accountDetails = new AccountDetails();
        accountDetails.setEmail(userEmail.toString());
        accountDetails.setInternalSigning(isInternalSignin);
        addComponents(Model.of(accountDetails));
    }

    /**
     * Fabrique les éléments de la page.
     */
    void addComponents(IModel<AccountDetails> accountDetails) {
        add(new Image("panelIcon", new PackageResourceReference(getClass(), "../img/icon_user.png")));

        // On prépare un compoundPropertyModel pour appel direct à ses membres.
        setDefaultModel(new CompoundPropertyModel<>(accountDetails));

        // Le panneau des messages
        feedbackPanel = new ThaleiaLightFeedbackPanel("feedback");
        feedbackPanel.setOutputMarkupId(true);
        add(feedbackPanel);

        // Le formulaire
        //noinspection unchecked
        form = new Form<>("form", (IModel<AccountDetails>) getDefaultModel());
        form.setOutputMarkupId(true);
        add(form);

        final WebMarkupContainer demoMessage = new WebMarkupContainer("demoMessage");
        demoMessage.setVisible("true".equalsIgnoreCase(applicationParameterDao.getValue("new.account.isdemo", "true")));
        add(demoMessage);

        // Le nom
        form.add(new MyTextField("name").setRequired(true));
        if (accountDetails.getObject().getName().equals(" ")) {
            accountDetails.getObject().setName("");
        }

        // L'entreprise
        form.add(new TextField<>("company", new Model<String>()).setRequired(isCompanyRequired()));
        // Le label de l'entreprise
        String language = ThaleiaSession.get().getLocale().getLanguage();
        String defaultCompanyLabel = MessagesUtils.getLocalizedMessage("companyLabel", this.getClass());
        String companyLabel = applicationParameterDao.getValue("finalize.account.page.company.label." + language, defaultCompanyLabel);
        form.add(new Label("companyLabel", companyLabel));
        // Astérisque rouge ?
        form.add(new WebMarkupContainer("companyRequired") {
            @Override
            public boolean isVisible() {
                return isCompanyRequired();
            }
        });

        // Le numéro de téléphone
        form.add(new TextField<>("phone", new Model<String>()).setRequired(isPhoneRequired()));
        // Astérisque rouge ?
        form.add(new WebMarkupContainer("phoneRequired") {
            @Override
            public boolean isVisible() {
                return isPhoneRequired();
            }
        });

        // Le mot de passe, si ce n'est pas un compte identifié par un tiers
        WebMarkupContainer passwordGroup = new WebMarkupContainer("passwordGroup") {
            @Override
            public boolean isVisible() {
                return accountDetails.getObject().isInternalSigning();
            }
        };
        form.add(passwordGroup);
        final Component password = new PasswordTextField("password", clearPasswordModel) {
            @Override
            public boolean checkRequired() {
                boolean result = super.checkRequired();
                add(new CSSClassRemover("has-error"));
                return result;
            }

            @Override
            public void error(IValidationError error) {
                super.error(error);
                add(AttributeModifier.append("class", "has-error"));
            }

        }.add(new SeriousPasswordValidator()).add(new AttributeModifier("placeholder", new StringResourceModel("password.field.default.text", FinalizeAccountPage.this, null))).setOutputMarkupId(true);
        passwordGroup.add(password);

        // Si demandé par le paramétrage de l'application, on demande de confirmer l'attestation.
        form.add(new WebMarkupContainer("declarationContainer") {
            @Override
            public boolean isVisible() {
                return !applicationParameterDao.getValue("finalize.account.mandatory" + ".declaration.fr", "").isEmpty();
            }
        }.add(new CheckBox("declaration", Model.of(false)).setRequired(true).add(new LegalValidator())).add(new Label("declarationLabel", new AbstractReadOnlyModel<String>() {
            @Override
            public String getObject() {
                String language = ThaleiaSession.get().getLocale().getLanguage();
                return applicationParameterDao.getValue("finalize.account.mandatory" + ".declaration." + language, "");
            }
        })));

        // Le bouton enregistrer
        form.add(new IndicatingAjaxButton("save") {

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                String redirection = null;
                try {
                    // Appel à l'API Thaleia pour l'envoi des informations de mise à jour de l'utilisateur
                    AccountDetails details = (AccountDetails) form.getModelObject();
                    details.setPassword(clearPasswordModel.getObject());
                    redirection = sendUserDetailsAndGetRedirectionToLogin(details);
                } catch (Exception e) {
                    logger.warn("Impossible de finaliser le compte : " + e);
                    MarkupContainer panelContainer = this.getParent().getParent();
                    StringResourceModel errorMessageModel = new StringResourceModel("creation.error", panelContainer,
                            null);
                    // Affiche le message d'erreur
                    PanelUtils.showErrorMessage(errorMessageModel.getString(), e.toString(), feedbackPanel);
                    target.add(feedbackPanel);
                }

                // Redirection vers la page de login reçue dans la réponse JSON
                throw new RedirectToUrlException(redirection);
            }

            private String sendUserDetailsAndGetRedirectionToLogin(AccountDetails accountDetails) throws Exception {
                HttpPut request = new HttpPut(ThaleiaApplication.get().getApplicationRootUrl() + UserAccountAPI.API_END_POINT + "/" + accountDetails.getEmail());
                request.addHeader(HttpHeaders.AUTHORIZATION, FinalizeAccountPage.this.apiToken);

                StringEntity requestEntity = new StringEntity(accountDetails.toJson(), ContentType.APPLICATION_JSON);
                request.setEntity(requestEntity);

                logger.debug("Appel de l'API Thaleia pour la mise à jour d'un compte à finaliser : " + request);
                try (CloseableHttpClient httpClient = Downloader.getHttpClient(); CloseableHttpResponse response = httpClient.execute(request)) {
                    logger.debug("Réponse : " + response.getStatusLine().toString());

                    // Analyse du code de retour
                    int statusCode = response.getStatusLine().getStatusCode();
                    if (statusCode == 200) {
                        // On parse le contenu JSON reçu, pour trouver la redirection vers la page de login
                        HttpEntity entity = response.getEntity();
                        if (entity != null) {
                            try (InputStream is = entity.getContent()) {
                                String result = IOUtils.toString(is, StandardCharsets.UTF_8);
                                JSONParser parse = new JSONParser();
                                JSONObject jsonObject = (JSONObject) parse.parse(result);
                                String loginUrl = jsonObject.get("login_url").toString();
                                logger.debug("Trouvé la redirection " + loginUrl);
                                return loginUrl;
                            } catch (Exception e) {
                                throw new Exception("Impossible de lire le flux de la réponse : ", e);
                            }
                        } else {
                            String msg = "Erreur de mise à jour du compte utilisateur (statusCode = " + statusCode + "), entité vide.";
                            logger.warn(msg);
                            throw new Exception(msg);
                        }
                    } else {
                        String msg = "Erreur de mise à jour du compte utilisateur (statusCode = " + statusCode + ").";
                        logger.warn(msg);
                        throw new Exception(msg);
                    }

                } catch (Exception e) {
                    logger.warn("Erreur lors de l'appel à l'API Thaleia pour la mise à jour d'un compte à finaliser : " + e);
                    logger.debug(ExceptionUtils.getRootCauseMessage(e));
                    logger.debug(ExceptionUtils.getFullStackTrace(e));
                    throw e;
                }
            }

            @Override
            protected void onError(AjaxRequestTarget target, Form<?> form) {
                target.add(feedbackPanel);
            }
        });
    }

    /**
     * @return true si cet utilisateur a été correctement identifié.
     */
    protected boolean authenticate(User user) {
        boolean authenticate = ThaleiaSession.get().signIn(user.getLogin(), clearPasswordModel.getObject());
        logger.debug("Identification de l'utilisateur : " + authenticate);
        return authenticate;
    }

    public boolean isCompanyRequired() {
        String result = applicationParameterDao.getValue("finalize.account.page.company" + ".mandatory", "false");
        return "true".equalsIgnoreCase(result);
    }

    private boolean isPhoneRequired() {
        String result = applicationParameterDao.getValue("finalize.account.page.phone" + ".mandatory", "false");
        return "true".equalsIgnoreCase(result);
    }

    private boolean getIsAccountFinalized(AccountRequest request) {
        // Le nom et mot de passe doivent être saisis, sauf en cas d'association avec un compte externe.
        if (request.getCreatedUser().isInternalSignin()) {
            if (request.getCreatedUser().getPassword() == null || request.getCreatedUser().getPassword().isEmpty()) {
                return false;
            }
            if (request.getCreatedUser().getName() == null || request.getCreatedUser().getName().isEmpty()) {
                return false;
            }
        }
        // Les champ obligatoires doivent être remplis
        if (isPhoneRequired()) {
            if (request.getPhone() == null || request.getPhone().isEmpty()) {
                return false;
            }
        }
        if (isCompanyRequired()) {
            return request.getCompany() != null && !request.getCompany().isEmpty();
        }
        return true;
    }

    public static class MyTextField extends TextField<String> {

        MyTextField(java.lang.String id) {
            super(id);
        }

        @Override
        public boolean checkRequired() {
            boolean result = super.checkRequired();
            add(new CSSClassRemover("has-error"));
            return result;
        }

        @Override
        public void error(IValidationError error) {
            super.error(error);
            add(AttributeModifier.append("class", "has-error"));
        }
    }
}
