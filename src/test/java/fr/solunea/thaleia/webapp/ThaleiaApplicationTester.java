package fr.solunea.thaleia.webapp;

import fr.solunea.thaleia.webapp.context.ApplicationContextService;
import fr.solunea.thaleia.webapp.pages.admin.LicencesPanel;
import hthurow.tomcatjndi.TomcatJNDI;
import org.apache.cayenne.BaseContext;
import org.apache.cayenne.configuration.server.ServerRuntime;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.wicket.util.tester.WicketTester;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.mockito.Mockito;

import javax.servlet.ServletContext;
import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Voir la doc du WicketTester : https://ci.apache.org/projects/wicket/guide/6.x/guide/testing.html
 */
public class ThaleiaApplicationTester {

    private static final Logger logger = Logger.getLogger(ThaleiaApplicationTester.class);
    protected static TomcatJNDI tomcatJNDI;
    protected static WicketTester tester;

    @BeforeAll
    public static void setUp() throws Exception {

        logger.getParent().setLevel(Level.ALL);

        // Les paramètres nécessaires aux test sont récupérés dans les propriétés du système. C'est pour cela que les tests fonctionnent
        // avec mvn test (car ces propriétés sont générées depuis le profil Maven), mais pas si on les lance directement depuis Intellij
        // par CTRL-ALT-F10 (à moins de les fixer en dur à la main dans la conf des tests, ou alors d'exécuter avec CTRL-ENTER
        // mvn -Dtest=NomDeLaClasseDeTest#nomDelLaMethode test
        // ).
        logger.info("Récupération des paramètres pour le test...");
        String contextXml = System.getProperty("tests.context.xml.location");
        File contextXmlFile = new File(contextXml);
        if (!contextXmlFile.exists()) {
            throw new Exception("Vérifiez que maven install a été executé sur Thaleia-Parent, et la présence du fichier de contexte de l'application : " + contextXml);
        } else {
            logger.info("Fichier context.xml : " + contextXmlFile.getAbsolutePath());
        }
        String webXml = System.getProperty("tests.web.xml.location");
        File webXmlFile = new File(webXml);
        if (!webXmlFile.exists()) {
            throw new Exception("Vérifiez que maven install a été executé sur thaleia-war, et la présence du fichier de déploiement de l'application : " + webXml);
        } else {
            logger.info("Fichier web.xml : " + webXmlFile.getAbsolutePath());
        }
        String war = System.getProperty("tests.thaleia.war.root.location");
        File warFile = new File(war);
        if (!warFile.exists()) {
            throw new Exception("Vérifiez que maven install a été executé sur thaleia-war, et la présence du répertoire de l'application dans thaleia-war/target : " + warFile.getAbsolutePath());
        } else {
            logger.info("Fichier war : " + warFile.getAbsolutePath());
        }
        logger.info("Ok.");

        logger.info("Préparation des ressources JNDI pour les tests...");
        tomcatJNDI = new TomcatJNDI();
        tomcatJNDI.processContextXml(contextXmlFile);
        tomcatJNDI.processWebXml(webXmlFile);
        tomcatJNDI.start();
        logger.info("Ok.");

        logger.info("Association du contexte Cayenne au thread, pour un appel par la ThaleiaSession (Wicket)...");
        // Le nom du fichier de conf du contexte Cayenne est défini dans le web.xml
        ServerRuntime runtime = ServerRuntime.builder()
                .addConfig("cayenne-ThaleiaDomain.xml")
                .build();
        BaseContext.bindThreadObjectContext(runtime.newContext());
        logger.info("Ok.");

        logger.info("Mise en service d'une imitation du contexte de l'application, pour les paramètres de l'application du web.xml...");
        final ServletContext servletContext = Mockito.mock(ServletContext.class);
        logger.info("Ok.");

        logger.info("Initialisation de l'application Wicket et du testeur...");
        // Le nom du fichier de conf du contexte Cayenne est défini dans le web.xml
        Mockito.when(servletContext.getInitParameter("cayenne.configuration.location")).thenReturn("cayenne-ThaleiaDomain.xml");
        tester = new WicketTester(new ThaleiaApplication() {
            @Override
            public ServletContext getServletContext() {
                return servletContext;
            }
        }, warFile.getAbsolutePath());
        logger.info("Ok.");


        logger.info("Association du contexte Cayenne au thread, pour un appel par la ThaleiaApplication (Wicket)...");
        // Le nom du fichier de conf du contexte Cayenne est défini dans le web.xml
        ThaleiaApplication.get().contextService = new ApplicationContextService("cayenne-ThaleiaDomain.xml");
        logger.info("Ok.");

        //session = (ThaleiaSession) tester.getSession();
    }

    @AfterAll
    public static void tearDown() {
        logger.info("Nettoyage des objets de tests...");
        tomcatJNDI.tearDown();
        tester.destroy();
        logger.info("Ok.");
    }
}
