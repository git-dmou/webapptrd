package fr.solunea.thaleia.webapp.utils;

public enum PluginsNames {
    CANNELLE ("fr.solunea.thaleia.plugins.cannelle.v6.CannelleV6Plugin"),
    ACTION ("fr.solunea.thaleia.plugins.action.ActionPlugin"),
    WELCOME("fr.solunea.thaleia.plugins.welcomev6.WelcomeV6Plugin"),
    PUBLISH("fr.solunea.thaleia.plugins.publish.PublishPlugin"),
    DEMO_HELPER("fr.solunea.thaleia.plugins.demohelper.Plugin");

    private final String name;

    PluginsNames(String name) {
        this.name = name;
    }

    public String getFullName() {
        return this.name;
    }

}
