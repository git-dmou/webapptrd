package fr.solunea.thaleia.utils;

public enum ApplicationEvent implements IEvent {
    /**
     * Ouverture d'une page, dont le nom est donné dans les propriétés de l'événement.
     */
    PageAccess,
    /**
     * Ouverture de la page publique d'accueil.
     */
    PublicPageAccess,
    /**
     * Ouverture de la page des prix
     */
    PricesPageAccess,
    /**
     * Ouverture de la page de création de compte.
     */
    AccountCreationStart,
    /**
     * Un utilisateur s'est créé un compte
     */
    AccountCreationDone,
    /**
     * Ouverture de la page de login
     */
    LoginPageAccess,
    /**
     * Erreur d'identification
     */
    LoginError,
    /**
     * Identification réussie (manuelle ou automatique avec un cookie et la case "Se souvenir de moi").
     */
    LoginOk
}
