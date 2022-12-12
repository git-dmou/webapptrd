package fr.solunea.thaleia.webapp.api;

import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.junit.Test;

import java.io.Reader;
import java.io.StringReader;
import java.security.Security;
import java.security.interfaces.RSAPublicKey;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * test de UserService.java / getPublicKey()
 */
public class GetPublicKeyTest {

    Reader publicKeyReader;
    PEMParser pemParser;
    JcaPEMKeyConverter converter;
    Object object;
    RSAPublicKey rSAPublicKey;
    private static final String BOUNCY_CASTLE_SECURITY_PROVIDER_NAME = "BC";

    /**
     * test à fin de comprendre pourquoi on a une erreur
     * java.lang.Exception: Impossible d'interpréter la clé publique.
     * en Production Thaleia NewArch et pas en Préprod
     *
     * le même schéma est reconstitué :
     * - par ce test pour l'erreur
     * - par \thaleia-webapp\src\test\java\fr\solunea\thaleia\webapp\api\UserAccountAPITest.java
     *              \_> loginForDemoCompleteProcess()
     *              pour le fonctionnement correct
     */

    @Test
    public void testGetPublicKeyFromTestDB() {
        String test_sso_jwt_key_pub = get_sso_jwt_key_pubFromTestDB();

        // cette ligne permet d'éviter le plantage !!!
        Security.addProvider(new BouncyCastleProvider());
        try {
            RSAPublicKey rsaPublicKey = getPublicKey(test_sso_jwt_key_pub);
        } catch (Exception e) {

            e.printStackTrace();
        }
    }

    @Test
    public void testGetPublicKeyFromVar() {
//        String test_sso_jwt_key_pub = get_sso_jwt_key_pubFromTestDB();

        // !!! attention les \n au début et à la fin sont important !!! ==> clé décodé !!
//        String test_sso_jwt_key_pub = "-----BEGIN RSA PUBLIC KEY-----\nMIIBCgKCAQEArIqGgZgKvB78vd9FGud534on0MFj0Npu20FOnj9lthzBDbUOMe/hqVIWxcS+VPUyNKF/43pgwfZFU1A38701PCCixhOA1rl5GSKDcXcN/vPH5csukp3zy2W9V0M+F34NMQGyLCV9KTlsyAO0u88BoM5CzrB0xzdduDQZl+VX1pzAK0nlsmYNPWIeNU+s98fhL31e5XiOEPcZyamMhbGE5sNSYL/fgabgpjiNpnem9xwyYITiqdMXLZfv3iBeANpHzX/GO9MHg9a9cpg9m5BrUfImS63Q6A1mLzWR6iHm0lu8CeSRhkFTZPRTCjc7H9q6z6DjS6ibnS6xFEZmsnzuqQIDAQAB\n-----END RSA PUBLIC KEY-----" ;

        // sans \n ==> pb !!
        String test_sso_jwt_key_pub = "-----BEGIN RSA PUBLIC KEY-----MIIBCgKCAQEArIqGgZgKvB78vd9FGud534on0MFj0Npu20FOnj9lthzBDbUOMe/hqVIWxcS+VPUyNKF/43pgwfZFU1A38701PCCixhOA1rl5GSKDcXcN/vPH5csukp3zy2W9V0M+F34NMQGyLCV9KTlsyAO0u88BoM5CzrB0xzdduDQZl+VX1pzAK0nlsmYNPWIeNU+s98fhL31e5XiOEPcZyamMhbGE5sNSYL/fgabgpjiNpnem9xwyYITiqdMXLZfv3iBeANpHzX/GO9MHg9a9cpg9m5BrUfImS63Q6A1mLzWR6iHm0lu8CeSRhkFTZPRTCjc7H9q6z6DjS6ibnS6xFEZmsnzuqQIDAQAB\n-----END RSA PUBLIC KEY-----" ;

//        String cleprivee = "-----BEGIN RSA PRIVATE KEY-----\nMIIEpQIBAAKCAQEArIqGgZgKvB78vd9FGud534on0MFj0Npu20FOnj9lthzBDbUOMe/hqVIWxcS+VPUyNKF/43pgwfZFU1A38701PCCixhOA1rl5GSKDcXcN/vPH5csukp3zy2W9V0M+F34NMQGyLCV9KTlsyAO0u88BoM5CzrB0xzdduDQZl+VX1pzAK0nlsmYNPWIeNU+s98fhL31e5XiOEPcZyamMhbGE5sNSYL/fgabgpjiNpnem9xwyYITiqdMXLZfv3iBeANpHzX/GO9MHg9a9cpg9m5BrUfImS63Q6A1mLzWR6iHm0lu8CeSRhkFTZPRTCjc7H9q6z6DjS6ibnS6xFEZmsnzuqQIDAQABAoIBAG1aXmVEN4mDGiw8lU+35UYICbQE3X5A8oGDopApHg2Nq81EMvrzTdJCdKxjRT4TKP6yXJuPtEM6EjX7YXrZMCQriv5+3ek5I8cKWMRQ8E3ls4uwepPyB9GhaZ8kvvWkrjehG4tfNzQpcQhTUPi2+hEwqKbWryyTqpJUcuQSNtmM9MBodCW+HOnTDGD1zNvaewdriRcEZl0Wyw5Z5VjwYs1IolZgEZVmytQ8/Lv6Q2239J6P6WHAewVFU4RtrNfVnk1ho/jCUyVL7c4Zx2P1EEei8y8Ck61aIy0pzgOIE7xYfakJT3+gHvjqnLWOecOCDhRWL8zehBw877UvLZGenQECgYEA16YpJ5RjX9fyjttQgfuK6QXHdtG0ejDOFEBWUoozSuM4GNH5hlNLcnEd7M8CRyWhVgbDnr8yIXkgol+wLtasSMvB0LzdYpfPcJct7FhWweW6I7Gj3hIutOO6fxguJgkFoRn93m+lkoMVUFGV5Jniy/bQCmmMix37cAnF6Cvc6C8CgYEAzNNxrRF4/zJBeCcZUFl7JuTWi11yDCyznyToAtcRxz4MtNSUAa1rAo1pzwk3DeuK0lm6Hb4xJvnjOuT9GECl+HrW5lgRLryArIx6W4hGwFJDezGSaaoym/MLFB5XAwb+LdyHrrG6ugW1ELknKQzlrgTFYe/Txl8+OaHIEgvgCKcCgYEAt7wMyHo3iQuW0HyvxW+qVQvlfKkDmjBHBR4j7kNtnFqoCw2sCfSiDamvE3r0t+Wp5n/w4CqK7dYkJfRdSisBsyxXwB+KiFxME+Pa7sn9cUroI01RDv5y6RA2b98MQr/hGffpQ0Hg0kn5OOuqSJGMmLd7XcO8STOMbVMGWeA1YWUCgYEAn07yAnctkednrmlSsgD23ThtejXzB28ZQfV1kwfuJvam8VrJx0g3i+OrYUE8ldIWxibNsGaGWldPmjYSeHOH42y/iqcCrkQlHI+id2DWDq2tGLtkslSTSmJNwpcafBtLRSZaTdCkeB0KSHiJ8c67MY86akMbhxX7Et42+3pLBM8CgYEAgT5xYm19i6htzOu7kKetzYdP04/ZLri02myjTyXa0+J/vXokwxas+fWmZRqCAbPiakb2zd+d8P9ZUvVfnaMjUlirI1JwrIXFzLAoF+Ir94uHNJ4XczusaqsvGW3uHJ7hWHnIny8acw6VAvavnCruPGL2LWuyN75XfNL0UXpjYpI=\n-----END RSA PRIVATE KEY-----"
        // cette ligne permet d'éviter le plantage !!!
        Security.addProvider(new BouncyCastleProvider());
        try {
            RSAPublicKey rsaPublicKey = getPublicKey(test_sso_jwt_key_pub);
        } catch (Exception e) {

            e.printStackTrace();
        }
    }

    /**
     * avec la clé générée sur le serveur de prod avec :
     *
     */
    @Test
    public void testGetPublicKeyFromServerProdSshKeygen() {


        String test_sso_jwt_key_pub = "-----BEGIN RSA PUBLIC KEY-----\nMIIBCgKCAQEAxOcg/D/CwfEhYtvQxuh97grHCNRqplpNEbXa+Z5Gz9/wDzyNz7otm/vIoueJQ3XWKahvsVuBCbioYqie3Ft9io41oB6V/d9QMVCNpHZ2OzOlYLXzXBHcE8rsFrjWfpyJVBsz4bpEis/PzAqtrg9E+N46mMjLlBcoycXIjFdUymN6ji+8Ke6/zuu7YPZ8hWRtOhy5MofficEwYYz9EYt+yZ817MNTT2aE/ULKUSFX+nq30PE3nAX/sb003kxlWhWce21/ut4SURNGb6us00BmSBNCX5Iq+4v0OOAiiq0X54y0ZAe9KQfGr+jnyK3Twl0QFKwTBMN+zqlMGE5vgcKiCwIDAQAB\n-----END RSA PUBLIC KEY-----" ;

        // cette ligne permet d'éviter le plantage !!!
//        Security.addProvider(new BouncyCastleProvider());
        try {
            RSAPublicKey rsaPublicKey = getPublicKey(test_sso_jwt_key_pub);
        } catch (Exception e) {

            e.printStackTrace();
        }
    }

    public String get_sso_jwt_key_pubFromTestDB() {
//        String url = "jdbc:postgresql://localhost:5432/thaleia_newArch?user=thaleia_newArch&password=dbdb";
        String url = "jdbc:postgresql://localhost:5432/thaleia_newArch";
        String query = "select value from application_parameter where name = 'sso.jwt.key.pub'";
        String cleTableTest = "";

        try {
            Class.forName("org.postgresql.Driver");
            Connection connexion = DriverManager.getConnection(url,"thaleia_newArch","dbdb");
            Statement statement = connexion.createStatement();
            ResultSet result = statement.executeQuery(query);
            result.next();
            System.out.println("sso.jwt.key.pub : ");
            System.out.println(result.getString("value"));
            cleTableTest =  result.getString("value");
            statement.close();
            connexion.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
        return cleTableTest;
    }

    /**
     * methode adapté de :
     * thaleia-service\src\main\java\fr\solunea\thaleia\service\UserService.java
     *                                                             \_> getPublicKey()
     * @param publicKey
     * @return
     * @throws Exception
     */
    public RSAPublicKey getPublicKey(String publicKey) throws Exception {
        try (Reader publicKeyReader = new StringReader(publicKey)) {
            PEMParser pemParser = new PEMParser(publicKeyReader);
            JcaPEMKeyConverter converter = new JcaPEMKeyConverter().setProvider(BOUNCY_CASTLE_SECURITY_PROVIDER_NAME);
            System.out.println("converter : " + converter.toString());
            Object object = pemParser.readObject();
            System.out.println("object : " + object.toString());
            RSAPublicKey rSAPublicKey = (RSAPublicKey) converter.getPublicKey((SubjectPublicKeyInfo) object);
            System.out.println("rSAPublicKey : ");
            System.out.println(rSAPublicKey.toString());

            return rSAPublicKey;
        } catch (Exception e) {
            System.out.println("publicKeyReader : " + publicKeyReader.toString());
            System.out.println("pemParser : " + pemParser.toString());
            System.out.println("converter : " + converter.toString());
            System.out.println("object : " + object.toString());
            System.out.println("rSAPublicKey : " + rSAPublicKey.toString());

            throw new Exception("Impossible d'interpréter la clé publique.", e);
        }
    }
}

