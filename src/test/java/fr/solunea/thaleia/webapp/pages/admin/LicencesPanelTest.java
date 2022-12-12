package fr.solunea.thaleia.webapp.pages.admin;

import fr.solunea.thaleia.webapp.ThaleiaApplicationTester;
import org.junit.jupiter.api.Test;

public class LicencesPanelTest extends ThaleiaApplicationTester {

    @Test
    public void rendersSuccessfully() {
        tester.startComponentInPage(LicencesPanel.class);
    }
}
