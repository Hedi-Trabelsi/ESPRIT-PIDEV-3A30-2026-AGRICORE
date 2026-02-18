package org.example;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.net.URL;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) {
        try {
            // Load the FXML file
            URL fxmlUrl = getClass().getResource("/fxml/signin.fxml");
            if (fxmlUrl == null) {
                System.err.println("FXML file not found!");
                return;
            }

            Parent root = FXMLLoader.load(fxmlUrl);

            // Set up the primary stage
            primaryStage.setTitle("Gestion d'utilisateur");
            primaryStage.setScene(new Scene(root, 1000, 650));
            primaryStage.show();

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Error loading the FXML file.");
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
