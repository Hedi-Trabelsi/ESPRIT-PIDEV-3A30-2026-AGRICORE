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

        javafx.scene.control.Label typeLabel = new javafx.scene.control.Label("Type: " + m.getType());
        javafx.scene.control.Label prioriteLabel = new javafx.scene.control.Label("Priorite: " + m.getPriorite());
        javafx.scene.control.Label dateLabel = new javafx.scene.control.Label("Date: " + m.getDateDeclaration());
        javafx.scene.control.Label statutLabel = new javafx.scene.control.Label(m.getStatut());
        statutLabel.setStyle(getStatusStyle(m.getStatut()));
        javafx.scene.control.Label descLabel = new javafx.scene.control.Label("Description: " + m.getDescription());
        descLabel.setWrapText(true);

        // Nouveaux champs
        javafx.scene.control.Label lieuLabel = new javafx.scene.control.Label("Lieu: " + m.getLieu());
        javafx.scene.control.Label equipementLabel = new javafx.scene.control.Label("equipement: " + m.getEquipement());



        Button deleteBtn = new Button("Supprimer");
        deleteBtn.getStyleClass().add("btn-primary");

        Button btnUpdate = new Button("Modifier");
        btnUpdate.getStyleClass().add("btn-primary");
        HBox actions = new HBox(10);
        actions.getChildren().addAll(btnUpdate, deleteBtn);




        deleteBtn.setOnAction(e -> {
            try {
                serviceMaintenance.supprimer(m.getId());
                loadMaintenances();
            } catch (SQLException ex) {
                showAlert("Erreur", "Impossible de supprimer: " + ex.getMessage());
            }
        });

        btnUpdate.setOnAction(e -> {
            try {
                // Mettre le chemin exact de ton FXML
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/interfaces/UpdateMaintenance.fxml"));
                javafx.scene.Parent root = loader.load();

                // Recuperer le controller et passer la maintenance
                UpdateMaintenanceController controller = loader.getController();
                controller.setMaintenance(m);

                // Remplacer la scene
                btnUpdate.getScene().setRoot(root);

            } catch (Exception ex) {
                showAlert("Erreur", "Impossible d'ouvrir la modification: " + ex.getMessage());
            }
        });

        // Ajouter tous les labels a la card
        card.getChildren().addAll(
                typeLabel,
                prioriteLabel,
                dateLabel,
                statutLabel,
                descLabel,
                lieuLabel,
                equipementLabel,
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

        if (statut.contains("resol")) { // Resolu
            return "-fx-background-color:#d4edda; -fx-text-fill:green; -fx-padding:5 10; -fx-background-radius:10;";
        } else if (statut.contains("cours")) { // En cours
            return "-fx-background-color:#d1ecf1; -fx-text-fill:#0c5460; -fx-padding:5 10; -fx-background-radius:10;";
        } else if (statut.contains("attente") || statut.contains("plan")) { // En attente / Planifiee
            return "-fx-background-color:#fff3cd; -fx-text-fill:#856404; -fx-padding:5 10; -fx-background-radius:10;";
        }

        // Par defaut si statut inconnu
        return "-fx-background-color:#f8d7da; -fx-text-fill:red; -fx-padding:5 10; -fx-background-radius:10;";
    }


}
