package fr.solunea.thaleia.webapp.pages.demo;

import fr.solunea.thaleia.model.AccountRequest;
import fr.solunea.thaleia.model.ApiToken;
import fr.solunea.thaleia.model.dao.ApiTokenDao;
import fr.solunea.thaleia.utils.DetailedException;
import fr.solunea.thaleia.webapp.ThaleiaApplication;
import fr.solunea.thaleia.webapp.api.ApiV1Service;
import fr.solunea.thaleia.webapp.pages.ErrorPage;
import org.apache.cayenne.ObjectContext;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.apache.wicket.RestartResponseException;
import org.apache.wicket.request.IRequestCycle;
import org.apache.wicket.request.RequestHandlerStack;
import org.apache.wicket.request.http.WebResponse;
import org.apache.wicket.request.http.handler.RedirectRequestHandler;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class RedirectToFinalizeAccountPageException extends RequestHandlerStack.ReplaceHandlerException {
    public static final String THALEIA_USER_EMAIL_HEADER = "ThaleiaUserEmail";
    public static final String THALEIA_API_TOKEN_HEADER = "ThaleiaAPIToken";
    public static final String THALEIA_INTERNAL_SIGNIN_HEADER = "ThaleiaInternalSignIn";
    private static final Logger logger = Logger.getLogger(RedirectToFinalizeAccountPageException.class);

    public RedirectToFinalizeAccountPageException(AccountRequest accountRequest) {
        super(new RedirectToFinalizeAccountPageRequestHandler(accountRequest.getAccountCompletionRedirection(), accountRequest), true);
    }

    public static ApiToken generateToken(AccountRequest accountRequest) throws DetailedException {
        ObjectContext newContext = ThaleiaApplication.get().contextService.getNewContext();
        ApiTokenDao apiTokenDao = new ApiTokenDao(newContext);
        ApiToken token = apiTokenDao.generate(accountRequest.getCreatedUser(), ApiV1Service.TOKEN_DURATION_IN_SECONDS);
        try {
            apiTokenDao.save(token);
            token.getObjectContext().commitChanges();
        } catch (DetailedException e) {
            throw e.addMessage("Impossible d'enregistrer un nouveau ApiToken.");
        }
        return token;
    }

    private static class RedirectToFinalizeAccountPageRequestHandler extends RedirectRequestHandler {

        private final AccountRequest accountRequest;

        public RedirectToFinalizeAccountPageRequestHandler(String redirectUrl, AccountRequest accountRequest) {
            super(redirectUrl);
            this.accountRequest = accountRequest;
        }

        @Override
        public void respond(IRequestCycle requestCycle) {
            String location;
            final String url = getRedirectUrl();

            if (url.charAt(0) == '/') {
                // context-absolute url
                location = requestCycle.getUrlRenderer().renderContextRelativeUrl(url);
            } else {
                // if relative url, servlet container will translate to absolute as
                // per the servlet spec
                // if absolute url still do the same
                location = url;
            }

            WebResponse response = (WebResponse) requestCycle.getResponse();

            // On ne fabrique pas juste une redirection vers location, car on a besoin d'ajouter des en-têtes dans
            // cette redirection.
            // On renvoie donc un script qui va effectuer cette redirection avec ces en-têtes.

            String responseBody = "";
            try (InputStream is = getClass().getResourceAsStream("redirect.html")) {
                responseBody = IOUtils.toString(is, StandardCharsets.UTF_8);
            } catch (Exception e) {
                logger.warn("Erreur de lecture du corps de la requête : " + e);
            }
            responseBody = responseBody.replace("##THALEIA_USER_EMAIL##", accountRequest.getMail());
            responseBody = responseBody.replace("##THALEIA_USER_EMAIL_HEADER##", THALEIA_USER_EMAIL_HEADER);
            responseBody = responseBody.replace("##REDIRECT_URL##", location);
            ApiToken apiToken = null;
            try {
                apiToken = generateToken(accountRequest);
            } catch (DetailedException e) {
                throw new RestartResponseException(ErrorPage.class);
            }
            responseBody = responseBody.replace("##THALEIA_API_TOKEN##", apiToken.getValue());
            responseBody = responseBody.replace("##THALEIA_API_TOKEN_HEADER##", THALEIA_API_TOKEN_HEADER);
            responseBody = responseBody.replace("##THALEIA_INTERNAL_SIGNIN##", String.valueOf(accountRequest.getCreatedUser().isInternalSignin()));
            responseBody = responseBody.replace("##THALEIA_INTERNAL_SIGNIN_HEADER##", THALEIA_INTERNAL_SIGNIN_HEADER);

            // On écrit le corps
            int status = HttpServletResponse.SC_OK;
            response.setStatus(status);
            response.setContentType("text/html; charset=UTF-8");
            response.write(responseBody);
        }

    }
}
