package tests;

import utils.MyDataBase;
import java.sql.Connection;

public class Main {
    public static void main(String[] args) {
        try {
            // Récupérer la connexion via le Singleton
            Connection conn = MyDataBase.getInstance().getMyConnection();

            if (conn != null && !conn.isClosed()) {
                System.out.println("Connexion à la base de données réussie !");
            } else {
                System.out.println("Échec de la connexion.");
            }

        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }
}
