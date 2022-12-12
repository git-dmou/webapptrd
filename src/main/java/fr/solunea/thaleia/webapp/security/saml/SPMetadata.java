package fr.solunea.thaleia.webapp.security.saml;

import com.onelogin.saml2.util.Util;
import fr.solunea.thaleia.service.utils.Unique;
import fr.solunea.thaleia.utils.DetailedException;
import fr.solunea.thaleia.utils.LogUtils;
import fr.solunea.thaleia.webapp.ThaleiaApplication;
import org.opensaml.core.config.InitializationService;
import org.opensaml.core.xml.XMLObjectBuilderFactory;
import org.opensaml.core.xml.config.XMLObjectProviderRegistrySupport;
import org.opensaml.core.xml.io.Marshaller;
import org.opensaml.saml.saml2.metadata.*;
import org.opensaml.security.credential.UsageType;
import org.opensaml.xmlsec.signature.KeyInfo;
import org.opensaml.xmlsec.signature.X509Certificate;
import org.opensaml.xmlsec.signature.X509Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;

import java.util.ArrayList;

public class SPMetadata {

    private final static Logger logger = LoggerFactory.getLogger(SPMetadata.class);

    public static String get() throws DetailedException {

        try {
            try {
                InitializationService.initialize();
            } catch (Exception e) {
                throw new DetailedException("OpenSAML a lancé une exception lors de l'initialisation : " + e);
            }

            XMLObjectBuilderFactory builderFactory = XMLObjectProviderRegistrySupport.getBuilderFactory();

            EntityDescriptor descriptor =
                    (EntityDescriptor) (builderFactory.getBuilder(EntityDescriptor.DEFAULT_ELEMENT_NAME).buildObject(EntityDescriptor.DEFAULT_ELEMENT_NAME));
            descriptor.setEntityID(getEntityId());
            descriptor.setID("_" + Unique.getUniqueString(16));

            SPSSODescriptor spssoDescriptor =
                    (SPSSODescriptor) (builderFactory.getBuilder(SPSSODescriptor.DEFAULT_ELEMENT_NAME).buildObject(SPSSODescriptor.DEFAULT_ELEMENT_NAME));
            spssoDescriptor.setAuthnRequestsSigned(getAuthnRequestsSigned());
            spssoDescriptor.setWantAssertionsSigned(getWantAssertionsSigned());
            spssoDescriptor.addSupportedProtocol("urn:oasis:names:tc:SAML:2.0:protocol");
            descriptor.getRoleDescriptors().add(spssoDescriptor);

            KeyDescriptor keyDescriptor =
                    (KeyDescriptor) (builderFactory.getBuilder(KeyDescriptor.DEFAULT_ELEMENT_NAME).buildObject(KeyDescriptor.DEFAULT_ELEMENT_NAME));
            keyDescriptor.setUse(UsageType.SIGNING);
            spssoDescriptor.getKeyDescriptors().add(keyDescriptor);

            KeyInfo keyInfo =
                    (KeyInfo) (builderFactory.getBuilder(KeyInfo.DEFAULT_ELEMENT_NAME).buildObject(KeyInfo.DEFAULT_ELEMENT_NAME));
            keyDescriptor.setKeyInfo(keyInfo);

            X509Data x509Data =
                    (X509Data) (builderFactory.getBuilder(X509Data.DEFAULT_ELEMENT_NAME).buildObject(X509Data.DEFAULT_ELEMENT_NAME));
            keyInfo.getX509Datas().add(x509Data);

            X509Certificate x509Certificate =
                    (X509Certificate) (builderFactory.getBuilder(X509Certificate.DEFAULT_ELEMENT_NAME).buildObject(X509Certificate.DEFAULT_ELEMENT_NAME));
            x509Certificate.setValue(SamlRequest.getCertificateBase64());
            x509Data.getX509Certificates().add(x509Certificate);

            ArrayList<String> namesIds = new ArrayList<>();
            namesIds.add("urn:oasis:names:tc:SAML:1.1:nameid-format:emailAddress");
            namesIds.add("urn:oasis:names:tc:SAML:2.0:nameid-format:transient");
            namesIds.add("urn:oasis:names:tc:SAML:2.0:nameid-format:persistent");
            namesIds.add("urn:oasis:names:tc:SAML:1.1:nameid-format:unspecified");
            namesIds.add("urn:oasis:names:tc:SAML:1.1:nameid-format:X509SubjectName");
            namesIds.add("urn:oasis:names:tc:SAML:1.1:nameid-format:unspecified");
            for (String nameId : namesIds) {
                NameIDFormat nameIDFormat =
                        (NameIDFormat) (builderFactory.getBuilder(NameIDFormat.DEFAULT_ELEMENT_NAME).buildObject(NameIDFormat.DEFAULT_ELEMENT_NAME));
                nameIDFormat.setFormat(nameId);
                spssoDescriptor.getNameIDFormats().add(nameIDFormat);
            }

            AssertionConsumerService assertionConsumerService =
                    (AssertionConsumerService) (builderFactory.getBuilder(AssertionConsumerService.DEFAULT_ELEMENT_NAME).buildObject(AssertionConsumerService.DEFAULT_ELEMENT_NAME));
            assertionConsumerService.setBinding("urn:oasis:names:tc:SAML:2.0:bindings:HTTP-POST");
            assertionConsumerService.setLocation(getConsumerAssertionServiceLocation());
            assertionConsumerService.setIndex(0);
            assertionConsumerService.setIsDefault(true);
            spssoDescriptor.getAssertionConsumerServices().add(assertionConsumerService);

            AttributeConsumingService attributeConsumingService =
                    (AttributeConsumingService) (builderFactory.getBuilder(AttributeConsumingService.DEFAULT_ELEMENT_NAME).buildObject(AttributeConsumingService.DEFAULT_ELEMENT_NAME));
            attributeConsumingService.setIndex(0);
            attributeConsumingService.setIsDefault(true);
            spssoDescriptor.getAttributeConsumingServices().add(attributeConsumingService);

            ServiceName serviceName =
                    (ServiceName) (builderFactory.getBuilder(ServiceName.DEFAULT_ELEMENT_NAME).buildObject(ServiceName.DEFAULT_ELEMENT_NAME));
            serviceName.setValue("Thaleia");
            serviceName.setXMLLang("x-none");
            attributeConsumingService.getNames().add(serviceName);

            RequestedAttribute requestedAttribute =
                    (RequestedAttribute) (builderFactory.getBuilder(RequestedAttribute.DEFAULT_ELEMENT_NAME).buildObject(RequestedAttribute.DEFAULT_ELEMENT_NAME));
            requestedAttribute.setIsRequired(true);
            requestedAttribute.setName(getMailAttributeName());
            requestedAttribute.setFriendlyName("mail");
            requestedAttribute.setNameFormat("urn:oasis:names:tc:SAML:2.0:attrname-format:uri");
            attributeConsumingService.getRequestAttributes().add(requestedAttribute);

            Marshaller marshaller = XMLObjectProviderRegistrySupport.getMarshallerFactory().getMarshaller(descriptor);
            Element descriptorElement = marshaller.marshall(descriptor);

            String signatureAlgorithm = "http://www.w3.org/2000/09/xmldsig#rsa-sha1";

            return Util.addSign(descriptorElement, SamlRequest.getCredential().getPrivateKey(),
                    SamlRequest.getCertificate(), signatureAlgorithm);

        } catch (Exception e) {
            logger.warn(LogUtils.getStackTrace(e.getStackTrace()));
            throw new DetailedException("Impossible de préparer les métadonnées du SP Thaleia : " + e);
        }
    }

    static Boolean getWantAssertionsSigned() {
        String result = ThaleiaApplication.get().getApplicationParameterDao().getValue("saml.sp.assertions.must.be"
                        + ".signed",
                "false");
        return Boolean.parseBoolean(result);
    }

    static Boolean getAuthnRequestsSigned() {
        String result = ThaleiaApplication.get().getApplicationParameterDao().getValue("saml.sp.request.authn.is"
                        + ".signed",
                "false");
        return Boolean.parseBoolean(result);
    }

    public static String getConsumerAssertionServiceLocation() {
        String result = ThaleiaApplication.get().getApplicationRootUrl();

        if (!result.endsWith("/")) {
            result = result + "/";
        }

        return result + "SAML2/SSO/POST";
    }

    public static String getLogoutUrl() {
        String result = ThaleiaApplication.get().getApplicationRootUrl();

        if (!result.endsWith("/")) {
            result = result + "/";
        }

        return result + "SAML2/SSO/Logout";
    }

    /**
     * Exemple pour Shibboleth : urn:oid:0.9.2342.19200300.100.1.3
     */
    private static String getMailAttributeName() throws Exception {
        String parameterName = "saml.sp.request.mail.attribute.name";
        String result = ThaleiaApplication.get().getApplicationParameterDao().getValue(parameterName, "");
        if (result.isEmpty()) {
            throw new Exception(
                    "Aucun nom pour l'attribut mail n'a été défini par le paramètre d'application " + parameterName);
        }
        return result;
    }

    /**
     * On reconstruit l'URL d'accès aux métadonnées du SP Thaleia.
     */
    public static String getEntityId() {
        String result = ThaleiaApplication.get().getApplicationRootUrl();

        if (!result.endsWith("/")) {
            result = result + "/";
        }

        return result + "SAML2/SSO/Metadata";
    }

}
