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
        card.getStyleClass().add("task-card");
        card.setMinWidth(250); card.setMaxWidth(250);
        card.setMinHeight(280); card.setMaxHeight(280);

        String type = "Inconnu";
        String lieu = "Inconnu";
        String statutMaintenance = "Inconnu";

        // 1. Récupérer la maintenance parente
        try {
            Maintenance m = serviceMaintenance.getMaintenanceById(t.getId_maintenace());
            if (m != null) {
                type = m.getType();
                lieu = m.getLieu();
                statutMaintenance = m.getStatut();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        // --- TON ACTION DE CLIC POUR VOIR LES DÉTAILS ---
        card.setOnMouseClicked(e -> {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/interfaces/ShowMaintenanceDetails.fxml"));
                Parent root = loader.load();
                ShowMaintenanceDetailsController controller = loader.getController();
                Maintenance m = serviceMaintenance.getMaintenanceById(t.getId_maintenace());
                controller.setMaintenance(m);
                card.getScene().setRoot(root);
            } catch (Exception ex) {
                showAlert("Erreur", "Impossible d'ouvrir la maintenance: " + ex.getMessage());
            }
        });

        // 2. Créer le Badge de Statut (selon tes couleurs)
        Label statusBadge = new Label(statutMaintenance.toUpperCase());
        statusBadge.setStyle(getStatusStyle(statutMaintenance));

        Label maintenanceInfoLabel = new Label("📍 Maintenance: " + type + " - " + lieu);
        Label dateLabel = new Label("📅 Date prévue: " + t.getDate_prevue());
        Label coutLabel = new Label("💰 Coût estimé: " + t.getCout_estimee() + " DT");

        Label descLabel = new Label("Description: " + t.getDesciption());
        descLabel.setWrapText(true);
        descLabel.setMinHeight(50);
        descLabel.setMaxHeight(50);

        // 3. Boutons
        Button deleteBtn = new Button("Supprimer");
        deleteBtn.getStyleClass().add("btn-primary");
        Button btnUpdate = new Button("Modifier");
        btnUpdate.getStyleClass().add("btn-primary");

        // --- SÉCURITÉ : VERROUILLAGE SI RÉSOLU OU REFUSÉ ---
        if (statutMaintenance.toLowerCase().contains("resolu") || statutMaintenance.toLowerCase().contains("refuse")) {
            btnUpdate.setDisable(true);
            btnUpdate.setOpacity(0.5);
            btnUpdate.setText("Modifier");
        }

        HBox actions = new HBox(10, btnUpdate, deleteBtn);

        // Actions des boutons
        deleteBtn.setOnAction(e -> {
            try {
                serviceTache.supprimer(t.getId_tache());
                loadTaches();
            } catch (SQLException ex) { showAlert("Erreur", ex.getMessage()); }
        });

        btnUpdate.setOnAction(e -> {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/interfaces/UpdateTache.fxml"));
                Parent root = loader.load();
                UpdateTacheController controller = loader.getController();
                controller.setTache(t);
                btnUpdate.getScene().setRoot(root);
            } catch (Exception ex) { showAlert("Erreur", ex.getMessage()); }
        });

        // On assemble tout
        card.getChildren().addAll(statusBadge, maintenanceInfoLabel, dateLabel, coutLabel, descLabel, actions);
        return card;
    }

    // AJOUTE CETTE MÉTHODE SI ELLE N'EST PAS DÉJÀ LÀ
    private String getStatusStyle(String statut) {
        if (statut == null) return "";
        statut = statut.toLowerCase();

        if (statut.contains("resolu")) {
            return "-fx-background-color:#d4edda; -fx-text-fill:green; -fx-padding:3 8; -fx-background-radius:10; -fx-font-size:10px; -fx-font-weight:bold;";
        } else if (statut.contains("plan")) {
            return "-fx-background-color:#e8e3f5; -fx-text-fill:#6f42c1; -fx-padding:3 8; -fx-background-radius:10; -fx-font-size:10px; -fx-font-weight:bold;";
        }
        return "-fx-background-color:#f1f2f6; -fx-text-fill:#2f3542; -fx-padding:3 8; -fx-background-radius:10; -fx-font-size:10px;";
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
