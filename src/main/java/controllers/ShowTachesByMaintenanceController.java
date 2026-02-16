package controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import models.Tache;
import services.ServiceTache;

import java.sql.SQLException;
import java.util.List;

public class ShowTachesByMaintenanceController {

    private final ServiceTache serviceTache = new ServiceTache();
    private int maintenanceId;

    @FXML
    private GridPane gridPane;
    @FXML
    private Button backBtn;

    @FXML
    void initialize() {
        // Action pour revenir à la liste des maintenances
        backBtn.setOnAction(e -> {
            try {
                javafx.scene.Parent root = FXMLLoader.load(getClass().getResource("/interfaces/ShowMaintenance.fxml"));
                backBtn.getScene().setRoot(root);
            } catch (Exception ex) {
                showAlert("Erreur", "Impossible de revenir à la liste: " + ex.getMessage());
            }
        });
    }

    // Méthode pour passer l'ID de la maintenance sélectionnée
    public void setMaintenanceId(int id) {
        this.maintenanceId = id;
        loadTaches();
    }

    private void loadTaches() {
        try {
            List<Tache> tacheList = serviceTache.getTachesByMaintenance(maintenanceId);
            gridPane.getChildren().clear();

            int column = 0;
            int row = 0;

            for (Tache t : tacheList) {
                VBox card = createCard(t);
                gridPane.add(card, column, row);
                column++;
                if (column > 2) {
                    column = 0;
                    row++;
                }
            }

        } catch (SQLException e) {
            showAlert("Erreur", "Impossible de charger les tâches: " + e.getMessage());
        }
    }

    private VBox createCard(Tache t) {
        VBox card = new VBox();
        card.setStyle("-fx-background-color: white; -fx-padding: 15; -fx-background-radius: 15; -fx-spacing: 10;");

        Label dateLabel = new Label("Date prévue: " + t.getDate_prevue());
        Label descLabel = new Label("Description: " + t.getDesciption());
        descLabel.setWrapText(true);
        Label coutLabel = new Label("Coût estimé: " + t.getCout_estimee());

        card.getChildren().addAll(dateLabel, descLabel, coutLabel);
        return card;
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
