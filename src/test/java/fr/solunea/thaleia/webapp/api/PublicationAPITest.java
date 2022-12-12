package fr.solunea.thaleia.webapp.api;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import fr.solunea.thaleia.model.ApiToken;
import fr.solunea.thaleia.model.User;
import fr.solunea.thaleia.model.dao.UserDao;
import fr.solunea.thaleia.utils.DetailedException;
import fr.solunea.thaleia.utils.LogUtils;
import fr.solunea.thaleia.webapp.ThaleiaApplication;
import fr.solunea.thaleia.webapp.ThaleiaApplicationTester;
import fr.solunea.thaleia.webapp.api.transform.TransformTreatmentFactory;
import org.apache.commons.io.Charsets;
import org.apache.commons.io.FileUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.client.LaxRedirectStrategy;
import org.apache.http.ssl.SSLContexts;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Calendar;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class PublicationAPITest extends ThaleiaApplicationTester {

    protected static final String XAPI_VERB_INITIALIZED = "initialized";
    protected static final String XAPI_VERB_ATTEMPTED = "attempted";
    protected static final String publicationTitle = "Test publication";
    private static final Logger logger = Logger.getLogger(PublicationAPITest.class);
    private static final String ZIP_MODEL_FILENAME_FR = "modele_excel_classe_virtuelle_fr.zip";
    private static final String USER_ACCOUNT_LOGIN = "admin";
    private static String publicationURL;

    protected static String getPublicationURL() {
        return publicationURL;
    }

    @BeforeAll
    public static void publishCannelleContent() {
        String token = prepareToken(USER_ACCOUNT_LOGIN);
        requiresPluginsExists(token);

        // Récupération de la source Cannelle dans le plugin chargé dans le classLoader.
        File source = retrieveCannelleSource();
        logger.info("Retrouvé la source : " + source.getAbsolutePath());

        // Déclenchement de l'import par l'API
        TransformAPI transformAPI = new TransformAPI(ThaleiaApplication.get().contextService, ThaleiaApplication.get().getConfiguration());
        Object transform = transformAPI.transform(token, TransformTreatmentFactory.CANNELLE_IMPORT, "fr", source.getAbsolutePath());
        assertTrue(TransformAPI.CannelleImportResult.class.isAssignableFrom(transform.getClass()), "Le résultat de l'import n'est pas un objet du type attendu !");
        String contentId = ((TransformAPI.CannelleImportResult) transform).getContentVersionId();
        String contentLocale = ((TransformAPI.CannelleImportResult) transform).getLocale();

        publicationURL = publishContent(token, contentId, contentLocale);
//        publicationURL = "https://prt-rmar/thaleia/publish/1619709335799";
    }

    private static String prepareToken(String login) {
        UserDao userDao = new UserDao(ThaleiaApplication.get().contextService.getContextSingleton());
        User admin = userDao.findUserByLogin(login);
        ApiV1Service apiV1Service = new ApiV1Service(
                ThaleiaApplication.get().contextService,
                ThaleiaApplication.get().getConfiguration());
        return apiV1Service.getToken(admin);
    }

    private static String publishContent(String token, String contentVersionId, String locale) {
        PublicationAPI publicationAPI = new PublicationAPI(ThaleiaApplication.get().contextService, ThaleiaApplication.get().getConfiguration());
        PublicationAPI.PublicationDescription result = (PublicationAPI.PublicationDescription) publicationAPI.create(token, contentVersionId, locale);
        return result.publication_url;
    }

    private static File retrieveCannelleSource() {
        InputStream is = null;
        String filename = ZIP_MODEL_FILENAME_FR;
        try {
            is = ThaleiaApplication.get().getPluginService().getClassLoader().getResourceAsStream(filename);
        } catch (DetailedException e1) {
            logger.warn("Impossible d'accéder aux fichiers du plugin : " + e1);
        }
        assertNotNull(is, "Le fichier '" + filename + "' n'a pas été trouvé !");

        File result = null;
        try {
            result = File.createTempFile(filename, "zip");
            FileUtils.copyInputStreamToFile(is, result);
        } catch (IOException e) {
            logger.warn(e);
            fail(e.getMessage());
        }
        return result;
    }

    private static void requiresPluginsExists(String token) {
        PluginAPI pluginAPI = new PluginAPI(
                ThaleiaApplication.get().contextService,
                ThaleiaApplication.get().getConfiguration(),
                ThaleiaApplication.getScheduledExecutorService(),
                ThaleiaApplication.getExecutorFutures());

        boolean foundCannelle = false, foundPublish = false;

        List<PluginAPI.PluginPOJO> plugins = pluginAPI.listPlugins(token);
        for (PluginAPI.PluginPOJO plugin : plugins) {
            if ("fr.solunea.thaleia.plugins.cannelle.v6.CannelleV6Plugin".equals(plugin.getName())) {
                foundCannelle = true;
            }
            if ("fr.solunea.thaleia.plugins.publish.PublishPlugin".equals(plugin.getName())) {
                foundPublish = true;
            }
        }

        assertTrue(foundCannelle && foundPublish,
                "Les plugins Cannelle et Publish sont-ils bien publiés sur l'instance pour ce test ?");
    }

    /**
     * Vérifie :
     * - Présence des traces de l'accès à l'API CMI : pour cela, on reproduit les échanges avec l'API
     * de la publication, mais on n'exécute pas le code JS d'une vraie publication.
     */
    @Test
    public void publicationsAccessesStored() {
        String apiAnonymousToken = accessPublication(publicationURL);
        checkCMIDataAPI(publicationURL, apiAnonymousToken);
        // TODO tester l'API des suspend_data : /api/v1/cmi/suspend_data/{publicationId}/{email}
    }

    protected void sendXapiStatement(String email, String xapiVerb, String publicationTitle, String activityContext, String completion, String duration) {
        String apiUri = ThaleiaApplication.get().getApplicationRootUrl() + "/lrs";
        String params = "agent={%22objectType%22:%22Agent%22,%22mbox%22:%22mailto:" + email + "%22,%22name%22:%22" + email + "%22}&activity=" + publicationURL;
        HttpPut putRequest = new HttpPut(apiUri + "?" + URLEncoder.encode(params, StandardCharsets.UTF_8));
        putRequest.addHeader("Authorization", getLrsAuthorizationHeader());
        putRequest.addHeader("Content-Type", "application/json");
        String data = getLrsStatement(email, xapiVerb, publicationTitle, activityContext, completion, duration);
        putRequest.setEntity(new StringEntity(data, StandardCharsets.UTF_8));
        String result = "";
        try (CloseableHttpClient httpClient = getHttpClient();
             CloseableHttpResponse response = httpClient.execute(putRequest)) {
            StatusLine statusLine = response.getStatusLine();
            // Analyse du code de retour
            int statusCode = statusLine.getStatusCode();
            if (statusCode == 200) {
                HttpEntity entity = response.getEntity();
                if (entity != null) {
                    result = EntityUtils.toString(entity);
                } else {
                    throw new Exception("La réponse a un contenu vide.");
                }
            } else {
                throw new Exception("Mauvais code de retour : " + statusCode);
            }
        } catch (Exception e) {
            logger.warn("Erreur lors de l'appel pour une réception de données xAPI : " + e + "\n" + LogUtils.getStackTrace(e.getStackTrace()));
            fail();
        }
        // La réponse est du type : <html><head><title>Thaleia LRS</title></head><body>ezLRS1619771103019</body></html>
        assertFalse(result.isEmpty());
        assertTrue(result.trim().startsWith("<html><head><title>Thaleia LRS</title></head><body>ezLRS"), "Réponse reçue : " + result);
        assertTrue(result.trim().endsWith("</body></html>"), "Réponse reçue : " + result);
    }

    private String getLrsStatement(String email, String verb, String publicationTitle, String activityContext, String completion, String duration) {
        // De la forme 2021-04-30T10:25:03
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        String timestamp = formatter.format(Calendar.getInstance().getTime());

        String completionLine = "";
        if (completion != null) {
            completionLine = "        \"completion\": " + completion + ",\n";
        }

        return "{\n" +
                "    \"actor\": {\n" +
                "        \"objectType\": \"Agent\",\n" +
                "        \"mbox\": \"mailto:" + email + "\",\n" +
                "        \"name\": \"" + email + "\"\n" +
                "    },\n" +
                "    \"verb\": {\n" +
                "        \"id\": \"http://adlnet.gov/expapi/verbs/" + verb + "\",\n" +
                "        \"display\": {\n" +
                "            \"en-US\": \"" + verb + "\"\n" +
                "        }\n" +
                "    },\n" +
                "    \"object\": {\n" +
                "        \"id\": \"" + publicationURL + "\",\n" +
                "        \"objectType\": \"Activity\",\n" +
                "        \"definition\": {\n" +
                "            \"name\": {\n" +
                "                \"en-US\": \"" + publicationTitle + "\"\n" +
                "            }\n" +
                "        }\n" +
                "    },\n" +
                "    \"context\": {\n" +
                "        \"contextActivities\": {\n" +
                "            \"parent\": [\n" +
                "                {\n" +
                "                    \"id\": \"http://aero.gocreate-solutions.com/scormXapi/201312/id/\"\n" +
                "                }\n" +
                "            ],\n" +
                "            \"grouping\": [\n" +
                "                {\n" +
                "                    \"id\": \"http://aero.gocreate-solutions.com/scormXapi/201312/grouping/\"\n" +
                "                }\n" +
                "            ],\n" +
                "            \"other\": [\n" +
                "                {\n" +
                "                    \"id\": \"http://aero.gocreate-solutions.com/scormXapi/201312/other/" + activityContext + "\"\n" +
                "                }\n" +
                "            ]\n" +
                "        }\n" +
                "    },\n" +
                "    \"result\": {\n" +
                completionLine +
                "        \"duration\": \"" + duration + "\"\n" +
                "    },\n" +
                "    \"timestamp\": \"" + timestamp + "\",\n" +
                "    \"authority\": {\n" +
                "        \"objectType\": \"Agent\",\n" +
                "        \"mbox\": \"mailto:" + email + "\",\n" +
                "        \"name\": \"" + email + "\"\n" +
                "    }\n" +
                "}";
    }

    protected String getLrsData(String publicationURL, String email) {
        String apiUri = ThaleiaApplication.get().getApplicationRootUrl() + "/lrs";
        String params = "agent={%22objectType%22:%22Agent%22,%22mbox%22:%22mailto:" + email + "%22,%22name%22:%22" + email + "%22}&activity=" + publicationURL;
        HttpGet getRequest = new HttpGet(apiUri + "?" + URLEncoder.encode(params, StandardCharsets.UTF_8));
        getRequest.addHeader("Authorization", getLrsAuthorizationHeader());
        try (CloseableHttpClient httpClient = getHttpClient();
             CloseableHttpResponse response = httpClient.execute(getRequest)) {
            StatusLine statusLine = response.getStatusLine();
            // Analyse du code de retour
            int statusCode = statusLine.getStatusCode();
            if (statusCode == 200) {
                HttpEntity entity = response.getEntity();
                if (entity != null) {
                    return EntityUtils.toString(entity);
                } else {
                    throw new Exception("La réponse a un contenu vide.");
                }
            } else {
                throw new Exception("Mauvais code de retour : " + statusCode);
            }
        } catch (Exception e) {
            logger.warn("Erreur lors de l'appel pour une réception de données xAPI : " + e + "\n" + LogUtils.getStackTrace(e.getStackTrace()));
            fail();
            return "";
        }
    }

    /**
     * @return la valeur de l'entête Authorization pour un appel au LRS (login:mot de passe encodé en Base64)
     */
    private String getLrsAuthorizationHeader() {
        // TODO récupérer les paramètres d'identification au LRS écrits dans le HTML de la publication, et non en base
        String login = ThaleiaApplication.get().getApplicationParameterDao().findByName("lrs.api.account.login").getValue();
        String password = ThaleiaApplication.get().getApplicationParameterDao().findByName("lrs.api.account.password").getValue();
        return new String(Base64.getEncoder().encode((login + ":" + password).getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8);
    }

    private void checkCMIDataAPI(String publicationURL, String apiAnonymousToken) {
        // Les données à placer dans l'API CMI
        String completionStatus = "completionStatus";
        String successStatus = "successStatus";
        String location = "location";
        String email = "test@solunea.fr";
        String entry = "entry";
        String exit = "exit";
        String publicationReference = "publicationReference";
        String scoreRaw = "scoreRaw";
        String sessionTime = "sessionTime";
        String totalTime = "totalTime";
        String suspendData = "suspendData";
        String data = "{\n" +
                "    \"completion_status\": \"" + completionStatus + "\",\n" +
                "    \"success_status\": \"" + successStatus + "\",\n" +
                "    \"location\": \"" + location + "\",\n" +
                "    \"email\": \"" + email + "\",\n" +
                "    \"entry\": \"" + entry + "\",\n" +
                "    \"exit\": \"" + exit + "\",\n" +
                "    \"publication_reference\": \"" + publicationReference + "\",\n" +
                "    \"score_raw\": \"" + scoreRaw + "\",\n" +
                "    \"session_time\": \"" + sessionTime + "\",\n" +
                "    \"total_time\": \"" + totalTime + "\",\n" +
                "    \"suspend_data\": \"" + suspendData + "\"\n" +
                "}";

        // On extrait la référence de la publication depuis son URL, de la forme : https://server/instance/publish/123456
        String publicationRef = publicationURL.substring(publicationURL.lastIndexOf("/") + "/".length());

        String apiUri = ThaleiaApplication.get().getApplicationRootUrl() + "/api/v1/cmi/all/" + publicationRef + "/" + email;

        // On vérifie que le premier appel renvoie bien des données vides (réponse 204).
        cmiDataIsEmpty(apiAnonymousToken, apiUri);

        // Envoi des données CMI dans l'API
        sendCmiData(apiAnonymousToken, data, apiUri);

        // Récupération des données dans l'API CMI
        cmiDataIsFound(apiAnonymousToken, completionStatus, successStatus, location, entry, exit, scoreRaw, sessionTime, totalTime, suspendData, apiUri);
    }

    private void cmiDataIsFound(String apiAnonymousToken, String completionStatus, String successStatus, String location, String entry, String exit, String scoreRaw, String sessionTime, String totalTime, String suspendData, String apiUri) {
        HttpGet getRequest = new HttpGet(apiUri);
        getRequest.addHeader("Authorization", apiAnonymousToken);
        try (CloseableHttpClient httpClient = getHttpClient();
             CloseableHttpResponse response = httpClient.execute(getRequest)) {
            StatusLine statusLine = response.getStatusLine();
            // Analyse du code de retour
            int statusCode = statusLine.getStatusCode();
            if (statusCode == 200) {
                HttpEntity entity = response.getEntity();
                if (entity != null) {
                    // On interprète le contenu du résultat comme du JSON
                    Header encodingHeader = entity.getContentEncoding();
                    Charset encoding = encodingHeader == null ? StandardCharsets.UTF_8 :
                            Charsets.toCharset(encodingHeader.getValue());
                    String jsonString = EntityUtils.toString(entity, encoding);

                    JsonElement jsonElement = JsonParser.parseString(jsonString);
                    assertTrue(jsonElement.isJsonObject());
                    assertEquals(jsonElement.getAsJsonObject().get("completion_status").getAsString(), completionStatus);
                    assertEquals(jsonElement.getAsJsonObject().get("success_status").getAsString(), successStatus);
                    assertEquals(jsonElement.getAsJsonObject().get("location").getAsString(), location);
                    assertEquals(jsonElement.getAsJsonObject().get("entry").getAsString(), entry);
                    assertEquals(jsonElement.getAsJsonObject().get("exit").getAsString(), exit);
                    assertEquals(jsonElement.getAsJsonObject().get("score_raw").getAsString(), scoreRaw);
                    assertEquals(jsonElement.getAsJsonObject().get("session_time").getAsString(), sessionTime);
                    assertEquals(jsonElement.getAsJsonObject().get("total_time").getAsString(), totalTime);
                    assertEquals(jsonElement.getAsJsonObject().get("suspend_data").getAsString(), suspendData);

                } else {
                    throw new Exception("Pas de contenu dans la réponse.");
                }
            } else {
                throw new Exception("Mauvais code de retour : " + statusCode);
            }
        } catch (Exception e) {
            logger.warn("Erreur lors de l'appel à l'API Thaleia pour un envoi de données CMI : " + e + "\n" + LogUtils.getStackTrace(e.getStackTrace()));
            fail();
        }
    }

    private void sendCmiData(String apiAnonymousToken, String data, String apiUri) {
        HttpPut putRequest = new HttpPut(apiUri);
        putRequest.addHeader("Authorization", apiAnonymousToken);
        putRequest.addHeader("Content-Type", "application/json");
        putRequest.setEntity(new StringEntity(data, StandardCharsets.UTF_8));
        try (CloseableHttpClient httpClient = getHttpClient();
             CloseableHttpResponse response = httpClient.execute(putRequest)) {
            StatusLine statusLine = response.getStatusLine();
            // Analyse du code de retour
            int statusCode = statusLine.getStatusCode();
            if (statusCode == 201) { // 201 : created
                HttpEntity entity = response.getEntity();
                if (entity != null) {
                    String result = EntityUtils.toString(entity);
                    logger.debug("Contenu de la réponse : '" + result + "'");
                    assertEquals(result, "{\"code\":0,\"message\":\"Data is saved.\",\"description\":\"\"}");
                } else {
                    throw new Exception("Pas de contenu dans la réponse.");
                }
            } else {
                throw new Exception("Mauvais code de retour : " + statusCode);
            }
        } catch (Exception e) {
            logger.warn("Erreur lors de l'appel à l'API Thaleia pour un envoi de données CMI : " + e);
            fail();
        }
    }

    private void cmiDataIsEmpty(String apiAnonymousToken, String apiUri) {
        HttpGet getRequest = new HttpGet(apiUri);
        getRequest.addHeader("Authorization", apiAnonymousToken);
        try (CloseableHttpClient httpClient = getHttpClient();
             CloseableHttpResponse response = httpClient.execute(getRequest)) {
            StatusLine statusLine = response.getStatusLine();
            // Analyse du code de retour
            int statusCode = statusLine.getStatusCode();
            if (statusCode == 204) {
                HttpEntity entity = response.getEntity();
                if (entity != null) {
                    assertTrue(EntityUtils.toString(entity).trim().isEmpty());
                }  // else = contenu de la réponse nul : on accepte.
            } else {
                throw new Exception("Mauvais code de retour : " + statusCode);
            }
        } catch (Exception e) {
            logger.warn("Erreur lors de l'appel à l'API Thaleia pour une réception de données CMI : " + e + "\n" + LogUtils.getStackTrace(e.getStackTrace()));
            fail();
        }
    }

    private String accessPublication(String publicationURL) {
        HttpGet request = new HttpGet(publicationURL);

        logger.debug("Ouverture de l'URL : " + publicationURL);
        HttpClientContext httpClientContext = HttpClientContext.create();
        try (CloseableHttpClient httpClient = getHttpClient();
             CloseableHttpResponse ignored = httpClient.execute(request, httpClientContext)) {

            // On devrait avoir obtenu une redirection vers une URL de type : https://server/thaleia/publish/1234/launcher.html?token=XYZ
            // Normalement httpClientContext.getRedirectLocations() contient une redirection.
            // On récupère ce token (encodé en Base64 dans l'URL)
            String redirectURL = httpClientContext.getRedirectLocations().get(0).toString();
            String token = redirectURL.substring(redirectURL.lastIndexOf("?token=") + "?token=".length());

            // On vérifie que le token existe bien en base
            assertNotNull(token);
            assertFalse(token.isEmpty());
            String decodedToken = new String(Base64.getDecoder().decode(token), StandardCharsets.UTF_8);

            List<ApiToken> apiTokens = ThaleiaApplication.get().getApiTokenDao().findByValue(decodedToken);
            assertEquals(1, apiTokens.size());
            assertNull(apiTokens.get(0).getUser());
            assertEquals(apiTokens.get(0).getValue(), decodedToken);

            // On renvoie ce token anonyme d'appel à l'API
            return decodedToken;
        } catch (Exception e) {
            logger.warn("Erreur lors de l'appel à l'API Thaleia pour un token d'identification : " + e);
        }
        fail("Erreur lors de l'accès à la publication.");
        return "";
    }

    private CloseableHttpClient getHttpClient() throws NoSuchAlgorithmException, KeyManagementException, KeyStoreException {
        return HttpClients.custom()
                .setRedirectStrategy(new LaxRedirectStrategy()) // support pour HTTP REDIRECT pour GET et POST
                .setSSLSocketFactory(new SSLConnectionSocketFactory(SSLContexts.custom() // Pas de vérification stricte SSL
                        .loadTrustMaterial(null, new TrustSelfSignedStrategy())
                        .build(), NoopHostnameVerifier.INSTANCE
                )).build();
    }

}
