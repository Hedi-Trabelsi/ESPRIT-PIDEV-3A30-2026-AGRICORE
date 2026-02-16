package controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import models.Maintenance;

public class ShowMaintenanceDetailsController {

    private Maintenance maintenance;

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

    /**
     * Méthode pour passer la maintenance depuis un autre contrôleur
     */
    public void setMaintenance(Maintenance maintenance) {
        this.maintenance = maintenance;
        showMaintenanceDetails();
    }

    /**
     * Affiche les détails de la maintenance dans les labels
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
            alert.setHeaderText("Aucune maintenance sélectionnée");
            alert.setContentText("Impossible d'afficher les détails.");
            alert.showAndWait();
        }
    }
}
