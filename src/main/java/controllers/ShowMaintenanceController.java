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
import models.Maintenance;
import services.ServiceMaintenance;

import java.sql.SQLException;
import java.util.List;

public class ShowMaintenanceController {

    private final ServiceMaintenance serviceMaintenance = new ServiceMaintenance();
    private final services.ServiceTache serviceTache = new services.ServiceTache(); // Assure-toi que le nom du service est correct
    @FXML
    private javafx.scene.control.TextField searchTf;
    @FXML
    private GridPane gridPane;

   /* @FXML
    void initialize() {
        loadMaintenances();
    }*/
    @FXML
    private Button addBtn;

    @FXML
    void navigateAddMaintenance() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/interfaces/AddMaintenance.fxml"));
            javafx.scene.Parent root = loader.load();
            addBtn.getScene().setRoot(root); // remplacer la scene par AddMaintenance
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Erreur");
            alert.setHeaderText("Impossible d'ouvrir l'ajout");
            alert.setContentText(e.getMessage());
            alert.showAndWait();
        }
    }
    @FXML
    void initialize() {
        loadMaintenances();

        // Ajouter l'action pour le bouton Ajouter
        addBtn.setOnAction(e -> navigateAddMaintenance());

    }

    @FXML
 /*   void navigateAddMaintenance() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/AddMaintenance.fxml"));
            javafx.scene.Parent root = loader.load();
            addBtn.getScene().setRoot(root); // Remplace la scene par AddMaintenance
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Erreur");
            alert.setHeaderText("Impossible d'ouvrir l'ajout");
            alert.setContentText(e.getMessage());
            alert.showAndWait();
        }
    }*/
    private void loadMaintenances() {
        try {
            List<Maintenance> maintenanceList = serviceMaintenance.afficher();
            gridPane.getChildren().clear();

            int column = 0;
            int row = 0;

            for (Maintenance m : maintenanceList) {
                VBox card = createCard(m);
                gridPane.add(card, column, row);
                column++;
                if (column > 2) { // 3 cards per row
                    column = 0;
                    row++;
                }
            }

        } catch (SQLException e) {
            showAlert("Erreur", "Impossible de charger les maintenances: " + e.getMessage());
        }
    }
    @FXML
    private Label planifierLabel; // ✅ correspond au FXML

    @FXML
    void navigatePlanifier() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/interfaces/AddTache.fxml"));
            javafx.scene.Parent root = loader.load();
            // Utilise n'importe quel node existant pour obtenir la scene
            planifierLabel.getScene().setRoot(root);
        } catch (Exception e) {
            showAlert("Erreur", "Impossible d'ouvrir la planification: " + e.getMessage());
        }
    }

    @FXML
    private Label voirListeLabel; // correspond au fx:id du FXML

    @FXML
    void navigateVoirListe() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/interfaces/ShowTache.fxml"));
            Parent root = loader.load();
            voirListeLabel.getScene().setRoot(root); // remplace la scene par ShowTache
        } catch (Exception e) {
            showAlert("Erreur", "Impossible d'ouvrir ShowTache: " + e.getMessage());
        }
    }


    private VBox createCard(Maintenance m) {
        VBox card = new VBox();
        card.setStyle("-fx-background-color: white; -fx-padding: 15; -fx-background-radius: 15; "
                + "-fx-spacing: 10; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 5, 0, 0, 2);");
        card.setMinWidth(250);
        card.setMaxWidth(250);
        card.setMinHeight(300);
        card.setMaxHeight(330);

        javafx.scene.control.Label typeLabel = new javafx.scene.control.Label("Type: " + m.getType());
        javafx.scene.control.Label prioriteLabel = new javafx.scene.control.Label("Priorite: " + m.getPriorite());
        javafx.scene.control.Label dateLabel = new javafx.scene.control.Label("Date: " + m.getDateDeclaration());
        javafx.scene.control.Label statutLabel = new javafx.scene.control.Label(m.getStatut());
        statutLabel.setStyle(getStatusStyle(m.getStatut()));
        javafx.scene.control.Label descLabel = new javafx.scene.control.Label("Description: " + m.getDescription());
        descLabel.setWrapText(true);

        javafx.scene.control.Label lieuLabel = new javafx.scene.control.Label("Lieu: " + m.getLieu());
        javafx.scene.control.Label equipementLabel = new javafx.scene.control.Label("Equipement: " + m.getEquipement());

        // --- CALCUL DU COUT ET DE LA DATE ---
        double totalCout = 0;
        java.time.LocalDate datePlusProche = null;
        try {
            List<models.Tache> tachesLies = serviceTache.afficher().stream()
                    .filter(t -> t.getId_maintenace() == m.getId())
                    .collect(java.util.stream.Collectors.toList());

            for (models.Tache t : tachesLies) {
                totalCout += t.getCout_estimee();
                if (t.getDate_prevue() != null && !t.getDate_prevue().isEmpty()) {
                    try {
                        java.time.LocalDate dateTache = java.time.LocalDate.parse(t.getDate_prevue());
                        if (datePlusProche == null || dateTache.isBefore(datePlusProche)) {
                            datePlusProche = dateTache;
                        }
                    } catch (Exception e) {}
                }
            }
        } catch (Exception e) { e.printStackTrace(); }

        javafx.scene.control.Label infoSupLabel = new javafx.scene.control.Label();
        if ("refusee".equalsIgnoreCase(m.getStatut())) {
            infoSupLabel.setText("❌ Pas de date | Coût: " + String.format("%.2f", totalCout) + " DT");
            infoSupLabel.setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
        } else {
            String dtStr = (datePlusProche != null) ? datePlusProche.toString() : "À définir";
            infoSupLabel.setText("📅 " + dtStr + " | Coût: " + String.format("%.2f", totalCout) + " DT");
            infoSupLabel.setStyle("-fx-text-fill: #2e7d32; -fx-font-weight: bold;");
        }

        // --- BOUTONS ---
        Button deleteBtn = new Button("Supprimer");
        deleteBtn.getStyleClass().add("btn-primary");

        Button btnUpdate = new Button("Modifier");
        btnUpdate.getStyleClass().add("btn-primary");

        // --- SÉCURITÉ : DÉSACTIVER MODIFIER SI RÉSOLU OU REFUSÉ ---
        String currentStatus = m.getStatut().toLowerCase();
        if (currentStatus.contains("resolu") || currentStatus.contains("refuse")) {
            btnUpdate.setDisable(true); // Désactive le bouton
            btnUpdate.setOpacity(0.5);   // Le rend un peu transparent
            btnUpdate.setText("Modifier");
        }

        HBox actions = new HBox(10);
        actions.getChildren().addAll(btnUpdate, deleteBtn);

        // Actions
        deleteBtn.setOnAction(e -> {
            try {
                serviceMaintenance.supprimer(m.getId());
                loadMaintenances();
            } catch (SQLException ex) { showAlert("Erreur", "Impossible de supprimer: " + ex.getMessage()); }
        });

        btnUpdate.setOnAction(e -> {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/interfaces/UpdateMaintenance.fxml"));
                javafx.scene.Parent root = loader.load();
                UpdateMaintenanceController controller = loader.getController();
                controller.setMaintenance(m);
                btnUpdate.getScene().setRoot(root);
            } catch (Exception ex) { showAlert("Erreur", "Impossible d'ouvrir la modification: " + ex.getMessage()); }
        });

        card.getChildren().addAll(
                typeLabel,
                prioriteLabel,
                dateLabel,
                statutLabel,
                descLabel,
                lieuLabel,
                equipementLabel,
                infoSupLabel,
                actions
        );

        return card;
    }


    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setContentText(content);
        alert.showAndWait();
    }
    private String getStatusStyle(String statut) {
        if (statut == null) return "";

        statut = statut.toLowerCase();

        if (statut.contains("resolu")) { // Resolu
            return "-fx-background-color:#d4edda; -fx-text-fill:green; -fx-padding:5 10; -fx-background-radius:10;";
        } else if (statut.contains("cours")) { // En cours
            return "-fx-background-color:#d1ecf1; -fx-text-fill:#0c5460; -fx-padding:5 10; -fx-background-radius:10;";
        } else if (statut.contains("attente")) { // Jaune/Orange (Attention)
            return "-fx-background-color:#fff3cd; -fx-text-fill:#856404; -fx-padding:5 10; -fx-background-radius:10;";
        }
        else if (statut.contains("plan")) { // Mauve/Lilas (Futur/Planifié)
            return "-fx-background-color:#e8e3f5; -fx-text-fill:#6f42c1; -fx-padding:5 10; -fx-background-radius:10;";
        }
        else if (statut.contains("refuse")) { // Rouge (Annulé)
            return "-fx-background-color:#f8d7da; -fx-text-fill:#721c24; -fx-padding:5 10; -fx-background-radius:10;";
        }
        // Par defaut si statut inconnu
        return "-fx-background-color:#d4edda; -fx-text-fill:red; -fx-padding:5 10; -fx-background-radius:10;";
    }


}
