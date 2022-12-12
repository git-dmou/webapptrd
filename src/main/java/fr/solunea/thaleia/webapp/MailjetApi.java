package fr.solunea.thaleia.webapp;

import com.mailjet.client.ClientOptions;
import com.mailjet.client.MailjetClient;
import com.mailjet.client.MailjetRequest;
import com.mailjet.client.MailjetResponse;
import com.mailjet.client.errors.MailjetException;
import com.mailjet.client.resource.Contact;
import com.mailjet.client.resource.ContactManagecontactslists;
import com.mailjet.client.resource.Contactdata;
import com.mailjet.client.transactional.SendContact;
import com.mailjet.client.transactional.SendEmailsRequest;
import com.mailjet.client.transactional.TrackOpens;
import com.mailjet.client.transactional.TransactionalEmail;
import com.mailjet.client.transactional.response.SendEmailsResponse;
import fr.solunea.thaleia.model.dao.ApplicationParameterDao;
import fr.solunea.thaleia.webapp.security.ThaleiaSession;
import org.apache.cayenne.ObjectContext;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.Serializable;

public class MailjetApi implements Serializable {

    private final static Logger logger = Logger.getLogger(MailjetApi.class);

    private String MJ_APIKEY_PUBLIC;
    private String MJ_APIKEY_PRIVATE;

    private ObjectContext contextSingleton;

    public MailjetApi () {
        contextSingleton = ThaleiaSession.get().getContextService().getContextSingleton();
        MJ_APIKEY_PUBLIC = getApiKey("mailjet.apikey.public");
        MJ_APIKEY_PRIVATE = getApiKey("mailjet.apikey.private");
    }

    /**
     * Récupère une valeur pour l'api Mailjet dans la table application_parameter.
     * @param keyName
     * @return la valeur récupérée.
     * @throws Exception
     */
    private String getApiKey(String keyName) {

        ApplicationParameterDao dao = new ApplicationParameterDao(contextSingleton);
        String value = dao.getValue(keyName, "");
        if(value.equals("")) {
            logger.error("Aucune valeur n'est définie dans la table \"application_parameter\" pour la clé :" + keyName);
        }
        return value;
    }

    /**
     * Appel à l'Api Mailjet pour envoyer un email transactionnel.
     *
     * @param from     Expéditeur
     * @param to       Destinataire
     * @param subject  Sujet
     * @param htmlPart DOM du mail.
     * @return true si success sinon false.
     */
    public SendEmailsResponse sendTransactionalEmail(SendContact from, SendContact to, String subject, String htmlPart) throws MailjetException {
        ClientOptions options = ClientOptions.builder()
                .apiKey(MJ_APIKEY_PUBLIC)
                .apiSecretKey(MJ_APIKEY_PRIVATE)
                .build();

        MailjetClient client = new MailjetClient(options);

        TransactionalEmail message1 = TransactionalEmail
                .builder()
                .to(to)
                .from(from)
                .htmlPart(htmlPart)
                .subject(subject)
                .trackOpens(TrackOpens.ENABLED)
                .header("test-header-key", "test-value")
                .customID("custom-id-value")
                .build();

        SendEmailsRequest request = SendEmailsRequest
                .builder()
                .message(message1) // you can add up to 50 messages per request
                .build();

        return request.sendWith(client);
    }

    /**
     * Appel à l'api mailjet pour créer un contact.
     * @param email Email du contact.
     * @return Réponse de l'API.
     */
    public MailjetResponse createContact(String email) throws MailjetException {
        logger.debug("Création d'un contact mailjet : \"" + email +"\".");
        MailjetRequest request;
        MailjetResponse response;

        ClientOptions options = ClientOptions.builder()
                .apiKey(MJ_APIKEY_PUBLIC)
                .apiSecretKey(MJ_APIKEY_PRIVATE)
                .build();

        MailjetClient client = new MailjetClient(options);

        request = new MailjetRequest(Contact.resource)
                .property(Contact.ISEXCLUDEDFROMCAMPAIGNS, "false")
                .property(Contact.EMAIL, email);

        response = client.post(request);
        logger.debug(response.getStatus());
        logger.debug(response.getData());
        return response;
    }

    /**
     * Mise à jour d'une propriété d'un contact mailjet.
     * @param email Adresse du contact.
     * @param name Nom de la propriété.
     * @param value Valeur de la propriété.
     * @return Réponse de l'API.
     */
    public MailjetResponse updateContactProperty(String email, String name, String value) throws MailjetException {
        MailjetRequest request;
        MailjetResponse response;

        ClientOptions options = ClientOptions.builder()
                .apiKey(MJ_APIKEY_PUBLIC)
                .apiSecretKey(MJ_APIKEY_PRIVATE)
                .build();

        MailjetClient client = new MailjetClient(options);

        request = new MailjetRequest(Contactdata.resource, email)
                .property(Contactdata.DATA, new JSONArray()
                        .put(new JSONObject()
                                .put("Name", name)
                                .put("Value", value)
                        )
                );

        response = client.put(request);
        logger.debug(response.getStatus());
        logger.debug(response.getData());
        return response;
    }

    /**
     * Ajoute un contact mailjet à une liste de contacts.
     * @param email Adresse du contact.
     * @param listId Identifiant de la liste.
     * @return Réponse de l'API.
     */
    public MailjetResponse addContactToList(String email, String listId) throws MailjetException {
        MailjetRequest request;
        MailjetResponse response;

        ClientOptions options = ClientOptions.builder()
                .apiKey(MJ_APIKEY_PUBLIC)
                .apiSecretKey(MJ_APIKEY_PRIVATE)
                .build();

        MailjetClient client = new MailjetClient(options);

        request = new MailjetRequest(ContactManagecontactslists.resource, email)
                .property(ContactManagecontactslists.CONTACTSLISTS, new JSONArray()
                        .put(new JSONObject()
                                .put("Action", "addforce")
                                .put("ListID", listId)
                        ));

        response = client.post(request);
        logger.debug(response.getStatus());
        logger.debug(response.getData());
        return response;
    }

}
