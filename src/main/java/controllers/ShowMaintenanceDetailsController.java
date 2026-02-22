package controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import models.Maintenance;
import services.ServiceMaintenance;

import java.sql.SQLException;

public class ShowMaintenanceDetailsController {

    private Maintenance maintenance;
    @FXML
    private Button btnTerminer;
    @FXML
    private Label typeLabel;

    @FXML
    private Label statutLabel;

    @FXML
    private Label dateLabel;

    @FXML
    private Label descriptionLabel;

    @FXML
    private Label prioriteLabel;

    @FXML
    private Label lieuLabel;

    @FXML
    private Label equipementLabel;

    @FXML
    void handleTerminerIntervention() {
        if (maintenance == null) return;

        try {
            // 1. Changer le statut dans l'objet
            maintenance.setStatut("Resolu");

            // 2. Appeler le service pour mettre à jour la base de données
            // Assure-toi d'avoir une instance de ServiceMaintenance
            ServiceMaintenance serviceMaintenance = new ServiceMaintenance();
            serviceMaintenance.modifier(maintenance);

            // 3. Feedback visuel
            statutLabel.setText("Resolu");
            statutLabel.setStyle("-fx-text-fill: green; -fx-font-weight: bold;");
            btnTerminer.setDisable(true); // On désactive le bouton car c'est fini

            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Succes");
            alert.setHeaderText(null);
            alert.setContentText("L'intervention a ete marquée comme resolue !");
            alert.show();

        } catch (SQLException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Erreur");
            alert.setContentText("Impossible de modifier le statut : " + e.getMessage());
            alert.show();
        }
    }
    public void setMaintenance(Maintenance maintenance) {
        this.maintenance = maintenance;
        showMaintenanceDetails();
    }
    @FXML
    private Label retourLabel; // correspond au fx:id du FXML

    @FXML
    void navigateRetour() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/interfaces/ShowTache.fxml"));
            javafx.scene.Parent root = loader.load();
            retourLabel.getScene().setRoot(root); // remplace la scene par ShowTache
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Erreur");
            alert.setHeaderText("Impossible de retourner a la liste des tâches");
            alert.setContentText(e.getMessage());
            alert.showAndWait();
        }
    }


    /**
     * Affiche les details de la maintenance dans les labels
     */
    private void showMaintenanceDetails() {
        if (maintenance != null) {
            typeLabel.setText(maintenance.getType());
            statutLabel.setText(maintenance.getStatut());
            dateLabel.setText(String.valueOf(maintenance.getDateDeclaration()));
            descriptionLabel.setText(maintenance.getDescription());
            prioriteLabel.setText(maintenance.getPriorite());
            lieuLabel.setText(maintenance.getLieu());
            equipementLabel.setText(maintenance.getEquipement());
        } else {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Erreur");
            alert.setHeaderText("Aucune maintenance selectionnee");
            alert.setContentText("Impossible d'afficher les details.");
            alert.showAndWait();
        }
    }
}
