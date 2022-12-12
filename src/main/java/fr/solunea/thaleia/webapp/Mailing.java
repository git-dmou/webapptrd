package fr.solunea.thaleia.webapp;

import com.mailjet.client.MailjetResponse;
import com.mailjet.client.transactional.SendContact;
import com.mailjet.client.transactional.response.SendEmailsResponse;
import fr.solunea.thaleia.model.dao.ApplicationParameterDao;
import fr.solunea.thaleia.service.utils.UpdateUserRequest;
import fr.solunea.thaleia.utils.DetailedException;
import fr.solunea.thaleia.webapp.security.ThaleiaSession;
import org.apache.cayenne.ObjectContext;
import org.apache.log4j.Logger;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Classe d'interface avec l'API de mailing.
 */
public class Mailing {

    private final static Logger logger = Logger.getLogger(Mailing.class);
//    private final MailjetApi api = new MailjetApi();
    private MailjetApi api;
    private ArrayList<SendContact> debugRecipients;
    private final Boolean sendDebugMails;
    private final ApplicationParameterDao applicationParameterDao;
    private SendContact debugSender;
    private static Mailing singleton;

    private Mailing() {
        ObjectContext contextSingleton = ThaleiaSession.get().getContextService().getContextSingleton();
        applicationParameterDao = new ApplicationParameterDao(contextSingleton);
        sendDebugMails = applicationParameterDao.getValue("mailing.sendDebugMail", "false").equals("true");
        if (sendDebugMails) {
            debugRecipients = getDebugRecipients();
            debugSender = getDebugSender();
        }
        api = new MailjetApi();
    }

    public static Mailing get() throws DetailedException {
        if (singleton == null) {
            try {
                singleton = new Mailing();
            } catch (Exception e) {
                logger.warn(e);
                throw new DetailedException(e).addMessage("Impossible d'initialiser le service d'envoi d'email.");
            }
        }
        return singleton;
    }

    /**
     * Création d'un nouveau contact. Puis mise à jour de sa locale et inscription à une liste de contacts.
     * @param email Adresse du contact.
     * @param listId Liste de contact.
     * @param locale Locale du contact.
     */
    public void registerNewAccount(String email, String listId, String locale) {
        // Création du compte
        if (createContact(email)) {
            // Ajout de la propriété "locale"
            updateContactProperty(email, "locale", locale);
            // Ajout à la liste de contacts
            addContactToList(email, listId);
        }
    }

    public void updateContactOnAccountCompletion(String email, UpdateUserRequest updateUserRequest) {
        // On extrait les paramètres de la requête dans son corps, du type :
        // {
        //    "name" : "Monsieur Toto",
        //    "password": "123456",
        //    "phone" : "+33466682881",
        //    "company" : "Toto SARL"
        // }

        updateContactProperty(email, "full_name", updateUserRequest.name);
        updateContactProperty(email, "company", updateUserRequest.password);
        updateContactProperty(email, "phone", updateUserRequest.phone);
        updateContactProperty(email, "demo_subscription_date", new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX").format(new Date()));

    }

    /**
     * Création d'un nouveau contact dans l'outil de mailing.
     * @param email Adresse email du contact.
     * @return Boolean
     */
    public Boolean createContact(String email) {
        try {
            api.createContact(email);
            return true;
        } catch (Exception e) {
            String message = "Une erreur est survenue pendant l'enregistrement du nouveau compte mailjet pour l'adresse \"" + email +"\"";
            logger.warn(message, e);

            if (sendDebugMails) {
                String subject = "Mailjet : Erreur création contact "+ email;
                String htmlPart = "Une erreur est survenue lors de la création du contact " + email +
                        "<br><br>" + e;
                sendDebugMails(subject, htmlPart);
            }
            return false;
        }
    }

    /**
     * Mise à jour d'une propriété d'un contact.
     * @param email Adresse du contact.
     * @param name Nom de la propriété.
     * @param value Valeur de la propriété.
     * @return Boolean
     */
    public boolean updateContactProperty(String email, String name, String value) {
        try {
            MailjetResponse response = api.updateContactProperty(email, name, value);
            if (response.getStatus() == 200) {
                return true;
            } else {
                throw new Exception(response.toString());
            }
        } catch (Exception e) {
            String message = "Une erreur est survenue pendant la mise à jour de la propriété \"" + name + "\" du contact \"" + email + "\" pour la valeur \"" + value + "\".";
            logger.error(message, e);

            if (sendDebugMails) {
                String subject = "Mailjet : erreur de mise à jour d'une propriété du contact \"" + email + "\"";
                String htmlPart = message + "<br><br>" + e;
                sendDebugMails(subject, htmlPart);
            }
            return false;
        }
    }

    /**
     * Ajout d'un contact à une liste de contacts
     * @param email Adresse email du contact.
     * @param listId Identifiant de la liste.
     */
    private void addContactToList(String email, String listId) {
        try {
            MailjetResponse response = api.addContactToList(email, listId);
            if(response.getStatus() != 201) {
                throw new Exception(response.toString());
            }
        } catch (Exception e) {
            String message = "Une erreur est survenue pendant l'ajout du contact \"" + email + "\" à la liste de contacts \"" + listId + "\".";
            logger.warn(message, e);

            if (sendDebugMails) {
                String subject = "Mailjet : erreur d'inscription du contact \"" + email + "\" à la liste \"" + listId + "\".";
                String htmlPart = message + "<br><br>" + e;
                sendDebugMails(subject, htmlPart);
            }
        }
    }

    /**
     * Envoi d'un mail de débug.
     * @detail L'expéditeur correspond à "mailing.debugMail.sender" et les destinataires sont dans
     *      "mailing.debugMail.recipients"
     * @param subject Sujet du mail.
     * @param htmlPart Corps HTML du mail.
     */
    public void sendDebugMails (String subject, String htmlPart) {
        for (SendContact recipient : debugRecipients) {
            sendMail(debugSender, recipient, subject, htmlPart);
        }
    }

    /**
     * Envoi d'un email transactionnel.
     * @param sender Expéditeur.
     * @param recipient Destinataire.
     * @param subject Sujet.
     * @param htmlPart Corps du mail en HTML.
     */
    public void sendMail (SendContact sender, SendContact recipient, String subject, String htmlPart) {
        try {
            SendEmailsResponse response = api.sendTransactionalEmail(sender, recipient, subject, htmlPart);
            if ( !response.toString().contains("status=SUCCESS")) {
                throw new Exception("Une erreur est survenue pendant l'envoi d'un mail : <br/>" + response);
            }
        } catch (Exception e) {
            logger.error("Une erreur est survenue pendant l'envoi d'un mail.", e);
        }
    }

    /**
     * Retourne la liste des destinataires des mails de debug.
     * @return Liste des destinataires des mails de debug.
     */
    private ArrayList<SendContact> getDebugRecipients() {
        logger.debug("Récupération de la liste des destinataires des mails de debug.");

        ArrayList<SendContact> recipients = new ArrayList<>();

        final String rawRecipients = applicationParameterDao.getValue("mailing.debugMail.recipients", "");
        final String regex = "<([\\w.@+]+)>([\\w\\s]*)";

        logger.debug("Liste brute : \"" + rawRecipients + "\".");

        final Pattern pattern = Pattern.compile(regex, Pattern.MULTILINE);
        final Matcher matcher = pattern.matcher(rawRecipients);

        while (matcher.find()) {
            System.out.println("Full match: " + matcher.group(0));

            String email = matcher.group(1);
            String name = matcher.group(2);

            logger.debug("Adresse mail trouvée : " + email);
            logger.debug("Nom de contact trouvée : " + name);

            SendContact recipient;

            // Si le nom est renseigné on l'utilise, sinon on utilise l'adresse email en tant que nom.
            if(name.trim().length() > 0) {
                recipient = new SendContact(email, name);
            } else {
                recipient = new SendContact(email, email);
            }

            logger.debug("Objet SendContact créé : " + recipient.toString());
            recipients.add(recipient);

        }

        if (recipients.size() == 0) {
            logger.error("le format des mails trouvés dans la table application_parameter est invalide ");
            logger.error("le format à utiliser est : <adresse@mail.xx> nom ");
            logger.error("veuillez vérifier les informations en base de données ");
        }

        return recipients;
    }

    /**
     * Retourne le contact (email et nom) servant d'expéditeur pour les mails de debug.
     * @return SendContact Objet Mailjet du contact.
     */
    private SendContact getDebugSender() {
        logger.debug("Récupération de la liste des expéditeurs des mails de debug.");
        SendContact sender = null;
        final String rawSender = applicationParameterDao.getValue("mailing.debugMail.sender", "");
        final String regex = "<([\\w.@+]+)>([\\w\\s]*)";

        logger.debug("Expéditeur trouvé : \"" + rawSender + "\".");

        final Pattern pattern = Pattern.compile(regex, Pattern.MULTILINE);
        final Matcher matcher = pattern.matcher(rawSender);

        while (matcher.find()) {
            String email = matcher.group(1);
            String name = matcher.group(2);

            logger.debug("Adresse mail trouvée : " + email);
            logger.debug("Nom de contact trouvée : " + name);

            // Si le nom est renseigné on l'utilise, sinon on utilise l'adresse email en tant que nom.
            if(name.trim().length() > 0) {
                sender = new SendContact(email, name);
            } else {
                sender = new SendContact(email, email);
            }
        }

        if (sender == null) {
            logger.error("le format des mails trouvés dans la table application_parameter est invalide ");
            logger.error("le format à utiliser est : <adresse@mail.xx> nom ");
            logger.error("veuillez vérifier les informations en base de données ");
            sender = new SendContact("");
        }

        logger.debug("Objet SendContact créé : " + sender.toString());

        return sender;
    }
}
