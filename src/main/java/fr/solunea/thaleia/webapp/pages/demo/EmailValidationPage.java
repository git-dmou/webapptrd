package fr.solunea.thaleia.webapp.pages.demo;

import fr.solunea.thaleia.model.AccountRequest;
import fr.solunea.thaleia.model.User;
import fr.solunea.thaleia.model.dao.AccountRequestDao;
import fr.solunea.thaleia.model.dao.UserDao;
import fr.solunea.thaleia.service.utils.Configuration;
import fr.solunea.thaleia.utils.DetailedException;
import fr.solunea.thaleia.webapp.ThaleiaApplication;
import fr.solunea.thaleia.webapp.pages.BasePage;
import fr.solunea.thaleia.webapp.pages.ErrorPage;
import fr.solunea.thaleia.webapp.pages.LoginPage;
import fr.solunea.thaleia.webapp.security.ThaleiaSession;
import fr.solunea.thaleia.webapp.utils.MessagesUtils;
import org.apache.cayenne.ObjectContext;
import org.apache.log4j.Logger;
import org.apache.wicket.RestartResponseException;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.util.string.StringValue;

/**
 * Prend en paramètre un code de validation d'email (account_request.email_validation_code), et s'il est correct, alors valide l'email dans le compte utilisateur, et redirige vers la page de finalisation du compte.
 * Pas de HTML : ne fait que de la redirection.
 */
public class EmailValidationPage extends BasePage {
    private static final Logger logger = Logger.getLogger(EmailValidationPage.class);

    public EmailValidationPage() {
        super();
        setResponsePage(new ErrorPage());
    }

    public EmailValidationPage(PageParameters parameters) {
        super(parameters);

        // Si déjà identifié, on redirige sur la page d'accueil des utilisateurs identifiés
        if (ThaleiaSession.get().isSignedIn()) {
            throw new RestartResponseException(ThaleiaApplication.get().getRedirectionPage(Configuration.AUTHENTIFIED_USERS_WELCOME_PAGE, Configuration.DEFAULT_AUTHENTIFIED_USERS_WELCOME_PAGE, Configuration.HOME_MOUNT_POINT));
        }

        // Tentative de récupération de la requête de création de compte d'après le code de vérification
        AccountRequest accountRequest;
        ObjectContext context = ThaleiaSession.get().getContextService().getNewContext();
        try {
            AccountRequestDao accountRequestDao = new AccountRequestDao(context);

            // On récupère le code
            StringValue code = parameters.get(Configuration.EMAIL_VALIDATION_CODE_PARAMETER_NAME);
            if (code == null || code.isEmpty()) {
                throw new DetailedException("Le paramètre " + Configuration.EMAIL_VALIDATION_CODE_PARAMETER_NAME + " est vide !");
            }

            // On recherche la demande de création de compte utilisateur associé à ce code
            accountRequest = accountRequestDao.findByEmailValidationCode(code.toString(""));
            if (accountRequest == null) {
                throw new DetailedException("Le code de validation " + code.toString() + " n'est pas valide !");
            }

            if (accountRequest.getCreatedUser() == null) {
                throw new DetailedException("Pas de compte utilisateur associé à ce code !");
            }
        } catch (DetailedException e) {
            logger.info("Impossible d'ouvrir la page de validation de l'email : ", e);
            // On redirige sur la page d'accueil de Thaleia.
            error(MessagesUtils.getLocalizedMessage("invalid.code", EmailValidationPage.class, (Object[]) null));
            throw new RestartResponseException(ThaleiaApplication.get().getHomePage());
        }

        try {
            User createdUser = accountRequest.getCreatedUser();
            UserDao userDao = new UserDao(context);

            if (createdUser == null) {
                logger.info("Demande de validation d'un compte utilisateur inexistant.");
                error(MessagesUtils.getLocalizedMessage("no.useraccount.error", EmailValidationPage.class, (Object[]) null));
                throw new RestartResponseException(ThaleiaApplication.get().getHomePage());
            }

            // Si ce compte était déjà validé ET que l enom a été défini ET que le mot de passe a été complété (ou que c'est un mot de passe externe), c'est à dire que tous
            // les champs obligatoires de la page de complétion ont été remplis, alors on redirige directement sur la page de login.
            // = Le lien de validation n'est valable que tant que le compte n'est pas validé.
            // Sinon, il doit rentrer son mot de passe ou demander à le récupérer. On n'identifie pas un utilisateur uniquement avec un lien d'activation de compte déjà utilisé : ça serait une faille de sécurité.
            if (createdUser.getIsEmailValidated() && !createdUser.getName().isEmpty() && ((createdUser.isInternalSignin() && !createdUser.getPassword().isEmpty() || !createdUser.isInternalSignin()))) {
                info(MessagesUtils.getLocalizedMessage("useraccount.already.activated.message", EmailValidationPage.class, (Object[]) null));
                throw new RestartResponseException(new LoginPage());
            }

            // Ce morceau de code doit fonctionner même après une première tentative de validation de compte, qui n'aurait pas abouti à une validation. On ne supprime donc pas le dernier code de vérification.
            createdUser.setIsEmailValidated(true);
            userDao.save(createdUser);

        } catch (DetailedException e) {
            logger.info("Impossible d'enregistrer la validation de l'email : ", e);
            // On redirige sur la page d'accueil de Thaleia.
            error(MessagesUtils.getLocalizedMessage("internal.error", EmailValidationPage.class, (Object[]) null));
            throw new RestartResponseException(ThaleiaApplication.get().getHomePage());
        }

        // Si quelqu'un a ouvert cette page, on considère qu'il a validé son email
        info(MessagesUtils.getLocalizedMessage("email.validated", EmailValidationPage.class, accountRequest.getMail()));

        // On redirige sur la page de finalisation du compte, en ajoutant ce qu'il faut dans l'en-tête (email et token pour l'API)
        throw new RedirectToFinalizeAccountPageException(accountRequest);
    }

}
