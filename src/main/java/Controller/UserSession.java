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

    public static int getRole() {
        return currentUser != null ? currentUser.getRole() : -1;
    }
}
