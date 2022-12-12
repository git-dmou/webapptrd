package fr.solunea.thaleia.webapp.utils;

import fr.solunea.thaleia.webapp.ThaleiaApplication;
import org.apache.commons.compress.utils.IOUtils;
import org.apache.commons.io.FileUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.client.LaxRedirectStrategy;
import org.apache.http.ssl.SSLContexts;
import org.apache.log4j.Logger;
import org.apache.wicket.RuntimeConfigurationType;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

public class Downloader {

    private final static Logger logger = Logger.getLogger(Downloader.class);

    public static void download(URL url, File destination, Map<String, String> headers) throws IOException, URISyntaxException, KeyStoreException, NoSuchAlgorithmException, KeyManagementException {

        CloseableHttpClient httpclient = getHttpClient();
        try {
            HttpGet get = new HttpGet(url.toURI());
            // Ajout des en-têtes
            for (String header : headers.keySet()) {
                get.addHeader(header, headers.get(header));
            }
            httpclient.execute(get, new FileDownloadResponseHandler(destination));
        } catch (Exception e) {
            logger.warn("Erreur de téléchargement de l'URL '" + url.toString() + "'.", e);
            throw e;
        } finally {
            IOUtils.closeQuietly(httpclient);
        }
    }

    /**
     * @return un client HTTP configuré pour vérifier ou non les certificats SSL selon l'environnement de production ou de développement.
     */
    public static CloseableHttpClient getHttpClient() throws NoSuchAlgorithmException, KeyManagementException, KeyStoreException {
        CloseableHttpClient httpclient;
        if (ThaleiaApplication.get().getConfigurationType().equals(RuntimeConfigurationType.DEVELOPMENT)) {
            // Développement : on accepte les certificats SSL auto-signés
            httpclient = HttpClients.custom()
                    .setRedirectStrategy(new LaxRedirectStrategy()) // support pour HTTP REDIRECT pour GET et POST
                    .setSSLSocketFactory(new SSLConnectionSocketFactory(SSLContexts.custom()
                                    .loadTrustMaterial(null, new TrustSelfSignedStrategy())
                                    .build(), NoopHostnameVerifier.INSTANCE
                            )
                    ).build();
        } else {
            // Production : strict sur les vérifications SSL
            httpclient = HttpClients.custom()
                    .setRedirectStrategy(new LaxRedirectStrategy()) // support pour HTTP REDIRECT pour GET et POST
                    .build();
        }
        return httpclient;
    }

    public static void download(URL url, File destination) throws IOException, URISyntaxException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
        download(url, destination, new HashMap<>());
    }

    static class FileDownloadResponseHandler implements ResponseHandler<File> {

        private final File target;

        public FileDownloadResponseHandler(File target) {
            this.target = target;
        }

        @Override
        public File handleResponse(HttpResponse response) throws IOException {
            InputStream source = response.getEntity().getContent();
            FileUtils.copyInputStreamToFile(source, this.target);
            return this.target;
        }
    }
}
