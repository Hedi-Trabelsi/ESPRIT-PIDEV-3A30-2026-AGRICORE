package Controller;

import Model.Utilisateur;

public class UserSession {
    private static Utilisateur currentUser;

    public static void setCurrentUser(Utilisateur user) {
        currentUser = user;
    }

    public static Utilisateur getCurrentUser() {
        return currentUser;
    }

    // --- NOUVELLES MÉTHODES AJOUTÉES POUR LE CHAT ---

    public static int getUserId() {
        return currentUser != null ? currentUser.getId() : -1;
    }

    public static String getNom() {
        return currentUser != null ? currentUser.getNom() : "";
    }

    public static String getPrenom() {
        return currentUser != null ? currentUser.getPrenom() : "";
    }

    /**
     * Retourne le rôle sous forme de String pour faciliter la comparaison.
     * Si votre rôle en BDD est un int (ex: 1 pour Admin), adaptez la logique ici.
     */
    public static String getRoleName() {
        if (currentUser == null) return "GUEST";
        // Si votre rôle est un entier, par exemple 1 = ADMIN
        return (currentUser.getRole() == 1) ? "ADMIN" : "USER";
    }

    public static int getRole() {
        return currentUser != null ? currentUser.getRole() : -1;
    }

    public static void cleanUserSession() {
        currentUser = null;
    }
}