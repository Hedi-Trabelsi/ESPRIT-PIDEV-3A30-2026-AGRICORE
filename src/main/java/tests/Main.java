package tests;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import services.SmsService;


public class Main extends Application {

    @Override

        public void start(Stage stage) throws Exception {
          FXMLLoader loader = new FXMLLoader(getClass().getResource("/interfaces/ShowMaintenance.fxml"));
        //    FXMLLoader loader = new FXMLLoader(getClass().getResource("/interfaces/Dashboard.fxml"));
            Parent root = loader.load();
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.setTitle(" work shop javafx ");
            stage.show();
    }
}


/*
public class Main extends Application {

    @Override
    public void start(Stage stage) throws Exception {

        // --- TEST SMS AU DÉMARRAGE ---
        System.out.println("🚀 Lancement du test SMS...");

        // Remplace par ton numéro vérifié (ex: +216...)
        // On le met dans un petit Thread pour ne pas ralentir le chargement de la fenêtre
        new Thread(() -> {
            SmsService.envoyerSms("+21651042268", "Test depuis mon application JavaFX ! 🔧");
        }).start();

        // --- CHARGEMENT DE L'INTERFACE ---
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/interfaces/Dashboard.fxml"));
        Parent root = loader.load();
        Scene scene = new Scene(root);
        stage.setScene(scene);
        stage.setTitle("Work Shop JavaFX - Test SMS");
        stage.show();
    }
}
*/