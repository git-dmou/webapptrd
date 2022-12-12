package fr.solunea.thaleia.webapp.security.saml;

import com.onelogin.saml2.settings.Saml2Settings;
import com.onelogin.saml2.settings.SettingsBuilder;
import fr.solunea.thaleia.utils.DetailedException;
import fr.solunea.thaleia.webapp.ThaleiaApplication;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.opensaml.security.credential.Credential;
import org.opensaml.security.x509.BasicX509Credential;

import java.io.*;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.Security;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Properties;

public class SamlRequest {

    private static final String BEGIN_CERT = "-----BEGIN CERTIFICATE-----";
    private static final String END_CERT = "-----END CERTIFICATE-----";


    public static Saml2Settings getSettings() throws Exception {
        Properties properties = new Properties();

        properties.put("onelogin.saml2.strict", false);
        properties.put("onelogin.saml2.debug", getApplicationParam("saml.sp.request.debug").equalsIgnoreCase("true"));

        properties.put("onelogin.saml2.sp.entityid", SPMetadata.getEntityId());
        properties.put("onelogin.saml2.sp.assertion_consumer_service.url",
                SPMetadata.getConsumerAssertionServiceLocation());
        properties.put("onelogin.saml2.sp.assertion_consumer_service.binding",
                "urn:oasis:names:tc:SAML:2" + ".0:bindings:HTTP-POST");
        properties.put("onelogin.saml2.sp.single_logout_service.url", SPMetadata.getLogoutUrl());
        properties.put("onelogin.saml2.sp.single_logout_service.binding",
                "urn:oasis:names:tc:SAML:2" + ".0:bindings:HTTP-Redirect");
        properties.put("onelogin.saml2.sp.nameidformat", getApplicationParam("saml.sp.request.nameidformat"));
        properties.put("onelogin.saml2.sp.x509cert", BEGIN_CERT + SamlRequest.getCertificateBase64() + END_CERT);
        properties.put("onelogin.saml2.sp.privatekey",
                "-----BEGIN PRIVATE KEY-----" + SamlRequest.getPrivateKeyBase64() + "-----END PRIVATE KEY-----");

        properties.put("onelogin.saml2.idp.entityid", getApplicationParam("saml.sp.idp.entityid"));
        properties.put("onelogin.saml2.idp.single_sign_on_service.url", getApplicationParam("saml.sp.idp.sso.url"));
        properties.put("onelogin.saml2.idp.single_sign_on_service.binding",
                "urn:oasis:names:tc:SAML:2" + ".0:bindings:HTTP-Redirect");
        properties.put("onelogin.saml2.idp.single_logout_service.url", getApplicationParam("saml.sp.idp.slo.url"));
        properties.put("onelogin.saml2.idp.single_logout_service.response.url", "");
        properties.put("onelogin.saml2.idp.single_logout_service.binding",
                "urn:oasis:names:tc:SAML:2" + ".0:bindings:HTTP-Redirect");
        properties.put("onelogin.saml2.idp.x509cert", getApplicationParam("saml.sp.idp.frontchannel.signing.x509cert"));

        properties.put("onelogin.saml2.security.nameid_encrypted", false);
        //        properties.put("onelogin.saml2.security.authnrequest_signed", SPMetadata.getAuthnRequestsSigned());
        properties.put("onelogin.saml2.security.logoutrequest_signed", false);
        properties.put("onelogin.saml2.security.logoutresponse_signed", false);
        properties.put("onelogin.saml2.security.want_messages_signed", false);
        //        properties.put("onelogin.saml2.security.want_assertions_signed", SPMetadata.getWantAssertionsSigned
        //        ());
        properties.put("onelogin.saml2.security.sign_metadata", true);
        properties.put("onelogin.saml2.security.want_assertions_encrypted", false);
        properties.put("onelogin.saml2.security.want_nameid_encrypted", false);

        properties.put("onelogin.saml2.security.requested_authncontext",
                "urn:oasis:names:tc:SAML:2" + ".0:ac:classes:Password");
        properties.put("onelogin.saml2.security.onelogin.saml2.security.requested_authncontextcomparison", "exact");
        properties.put("onelogin.saml2.security.want_xml_validation", true);
        properties.put("onelogin.saml2.security.signature_algorithm", "http://www.w3.org/2000/09/xmldsig#rsa-sha1");

        properties.put("onelogin.saml2.organization.name", "Solunea");
        properties.put("onelogin.saml2.organization.displayname", "Solunea");
        properties.put("onelogin.saml2.organization.url", "http://www.solunea.fr");
        properties.put("onelogin.saml2.organization.lang", "fr");
        properties.put("onelogin.saml2.contacts.technical.given_name", "Support Thaleia");
        properties.put("onelogin.saml2.contacts.technical.email_address", "service.thaleia@solunea.fr");
        properties.put("onelogin.saml2.contacts.support.given_name", "Support Thaleia");
        properties.put("onelogin.saml2.contacts.support.email_address", "service.thaleia@solunea.fr");

        Saml2Settings settings = new SettingsBuilder().fromProperties(properties).build();
        settings.setAuthnRequestsSigned(SPMetadata.getAuthnRequestsSigned());
        settings.setWantAssertionsSigned(SPMetadata.getWantAssertionsSigned());
        return settings;
    }

    private static String getApplicationParam(String paramName) throws Exception {
        String result = ThaleiaApplication.get().getApplicationParameterDao().getValue(paramName, "");
        if (result.isEmpty()) {
            throw new Exception("Le paramètre d'application " + paramName + " n'a pas été configuré !");
        }
        return result;
    }

    static String getCertificateBase64() throws DetailedException {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            BASE64Encoder encoder = new BASE64Encoder();
            encoder.encode(getCertificate().getEncoded(), baos);
            return baos.toString();
        } catch (Exception e) {
            throw new DetailedException("Erreur de conversion du certificat en Base64 : " + e);
        }
    }

    private static String getPrivateKeyBase64() throws DetailedException {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            BASE64Encoder encoder = new BASE64Encoder();
            encoder.encode(getPrivateKey().getEncoded(), baos);
            return baos.toString();
        } catch (Exception e) {
            throw new DetailedException("Erreur de conversion de la clé privée en Base64 : " + e);
        }
    }

    public static X509Certificate getCertificate() throws Exception {

        String parameterName = "saml.sp.cert.file";
        String certificateFileName = ThaleiaApplication.get().getApplicationParameterDao().getValue(parameterName, "");
        if (certificateFileName.isEmpty()) {
            throw new Exception(
                    "Aucun emplacement n'a été défini pour le fichier de certificat : paramètre " + "d'application "
                            + parameterName);
        }
        File certificate = new File(certificateFileName);
        if (!certificate.exists() || certificate.isDirectory()) {
            throw new Exception("Il n'y a pas de certificat à l'emplacement '" + certificateFileName + "'");
        }

        return getCertificate(certificateFileName);
    }

    private static PrivateKey getPrivateKey() throws Exception {
        String parameterName = "saml.sp.privatekey.file";
        String privateKeyFilename = ThaleiaApplication.get().getApplicationParameterDao().getValue(parameterName, "");
        if (privateKeyFilename.isEmpty()) {
            throw new Exception("Aucun emplacement n'a été défini pour la clé privée : paramètre " + "d'application "
                    + parameterName);
        }
        File privateKey = new File(privateKeyFilename);
        if (!privateKey.exists() || privateKey.isDirectory()) {
            throw new Exception("Il n'y a pas de clée privée à l'emplacement '" + privateKeyFilename + "'");
        }

        return getPrivateKey(privateKeyFilename);
    }

    private static X509Certificate getCertificate(String certificateFile) throws Exception {
        CertificateFactory fact = CertificateFactory.getInstance("X.509");
        try (FileInputStream is = new FileInputStream(certificateFile)) {
            return (X509Certificate) fact.generateCertificate(is);
        } catch (Exception e) {
            throw new Exception("Impossible d'obtenir le certificat :" + e);
        }
    }

    private static PrivateKey getPrivateKey(String privateKeyFile) throws Exception {
        try {
            BufferedReader br = new BufferedReader(new FileReader(privateKeyFile));
            Security.addProvider(new BouncyCastleProvider());
            PEMParser pp = new PEMParser(br);
            Object parsedObject = pp.readObject();
            PrivateKeyInfo privateKeyInfo;

            if (parsedObject instanceof PEMKeyPair) {
                privateKeyInfo = ((PEMKeyPair) parsedObject).getPrivateKeyInfo();
            } else {
                privateKeyInfo = (PrivateKeyInfo) parsedObject;
            }

            byte[] pemPrivateKeyEncoded = privateKeyInfo.getEncoded();

            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(pemPrivateKeyEncoded);
            KeyFactory kf = KeyFactory.getInstance("RSA");
            return kf.generatePrivate(keySpec);

        } catch (Exception e) {
            throw new Exception("Impossible de lire la clé privée : " + e);
        }
    }

    static Credential getCredential() throws Exception {

        BasicX509Credential credential = new BasicX509Credential(getCertificate());
        credential.setPrivateKey(getPrivateKey());

        return credential;
    }
}
