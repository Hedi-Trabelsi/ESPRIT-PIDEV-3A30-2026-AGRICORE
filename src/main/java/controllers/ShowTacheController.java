package controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import models.Tache;
import models.Maintenance;
import services.ServiceTache;
import services.ServiceMaintenance;

import java.sql.SQLException;
import java.util.List;

public class ShowTacheController {

    private final ServiceTache serviceTache = new ServiceTache();
    private final ServiceMaintenance serviceMaintenance = new ServiceMaintenance();

    @FXML
    private GridPane gridPane;




    @FXML
    void initialize() {
        loadTaches();

    }
    @FXML
    private Label voirListeLabel;

    @FXML
    void navigateVoirListe() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/interfaces/ShowMaintenance.fxml"));
            Parent root = loader.load();
            voirListeLabel.getScene().setRoot(root); // remplace la scene par ShowMaintenance
        } catch (Exception e) {
            showAlert("Erreur", "Impossible d'ouvrir ShowMaintenance: " + e.getMessage());
        }
    }



    private void loadTaches() {
        try {
            List<Tache> tacheList = serviceTache.afficher();
            gridPane.getChildren().clear();

            int column = 0;
            int row = 0;

            for (Tache t : tacheList) {
                VBox card = createCard(t);
                gridPane.add(card, column, row);
                column++;
                if (column > 2) { // 3 cartes par ligne
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
        card.setStyle("-fx-background-color: white; -fx-padding: 15; -fx-background-radius: 15; "
                + "-fx-spacing: 10; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 5, 0, 0, 2);");

        // Infos de la tâche
        Label dateLabel = new Label("Date prevue: " + t.getDate_prevue());
        Label descLabel = new Label("Description: " + t.getDesciption());
        descLabel.setWrapText(true);
        Label coutLabel = new Label("Cout estime: " + t.getCout_estimee());

        // Recuperer la maintenance correspondante
        String type = "Inconnu";
        String lieu = "Inconnu";
        try {
            Maintenance m = serviceMaintenance.getMaintenanceById(t.getId_maintenace()); // methode a creer
            if (m != null) {
                type = m.getType();
                lieu = m.getLieu();
            }
        } catch (SQLException e) {
            // gerer erreur si necessaire
        }

        Label maintenanceInfoLabel = new Label("Maintenance: " + type + " - " + lieu);
        card.setOnMouseClicked(e -> {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/interfaces/ShowMaintenanceDetails.fxml"));
                Parent root = loader.load();

                // Passer la maintenance au contrôleur
                ShowMaintenanceDetailsController controller = loader.getController();
                Maintenance m = serviceMaintenance.getMaintenanceById(t.getId_maintenace());
                controller.setMaintenance(m);

                // Remplacer la scene
                card.getScene().setRoot(root);

            } catch (Exception ex) {
                showAlert("Erreur", "Impossible d'ouvrir la maintenance: " + ex.getMessage());
            }
        });
        // Boutons
        Button deleteBtn = new Button("Supprimer");
        deleteBtn.getStyleClass().add("btn-primary");

        Button btnUpdate = new Button("Modifier");
        btnUpdate.getStyleClass().add("btn-primary");

        HBox actions = new HBox(10);
        actions.getChildren().addAll(btnUpdate, deleteBtn);

        // Action supprimer
        deleteBtn.setOnAction(e -> {
            try {
                serviceTache.supprimer(t.getId_tache());
                loadTaches();
            } catch (SQLException ex) {
                showAlert("Erreur", "Impossible de supprimer: " + ex.getMessage());
            }
        });
        btnUpdate.setOnAction(e -> {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/interfaces/UpdateTache.fxml"));
                Parent root = loader.load();

                UpdateTacheController controller = loader.getController();
                controller.setTache(t); // passer la tâche a modifier

                btnUpdate.getScene().setRoot(root);
            } catch (Exception ex) {
                showAlert("Erreur", "Impossible d'ouvrir la modification: " + ex.getMessage());
            }
        });

        card.getChildren().addAll(dateLabel, descLabel, coutLabel, maintenanceInfoLabel, actions);
        return card;
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
