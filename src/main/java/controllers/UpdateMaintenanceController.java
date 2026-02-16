package controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import models.Maintenance;
import services.ServiceMaintenance;

import java.sql.SQLException;
import java.time.LocalDate;

public class UpdateMaintenanceController {

    private final ServiceMaintenance service = new ServiceMaintenance();
    private Maintenance maintenance; // maintenance à modifier

    @FXML
    private ChoiceBox<String> type;

    @FXML
    private ChoiceBox<String> priorite;

    @FXML
    private DatePicker dateDeclarationDp;

    @FXML
    private TextField descriptionTf;

    @FXML
    private TextField lieuTf;

    @FXML
    private TextField equipementTf;

    @FXML
    private TextField imageTf;

    // Méthode pour recevoir la maintenance sélectionnée
    public void setMaintenance(Maintenance m) {
        this.maintenance = m;
        type.setValue(m.getType());
        priorite.setValue(m.getPriorite());
        dateDeclarationDp.setValue(m.getDateDeclaration());
        dateDeclarationDp.setDisable(true);
        descriptionTf.setText(m.getDescription());
        lieuTf.setText(m.getLieu());
        equipementTf.setText(m.getEquipement());
    }


    @FXML
    void initialize() {
        type.getItems().addAll("Preventive", "Corrective", "Predictive");
        priorite.getItems().addAll("Faible", "Normale", "Urgente");
    }

    @FXML
    void updateMaintenance(ActionEvent event) {
        try {
            // Remplir l'objet maintenance avec les valeurs du formulaire
            maintenance.setType(type.getValue());
            maintenance.setPriorite(priorite.getValue());
            maintenance.setDateDeclaration(dateDeclarationDp.getValue());
            maintenance.setDescription(descriptionTf.getText());
            maintenance.setLieu(lieuTf.getText());
            maintenance.setEquipement(equipementTf.getText());

            // Appel à la méthode modifier de ServiceMaintenance
            service.modifier(maintenance);

            // Affiche un message de succès
            showAlert(Alert.AlertType.INFORMATION, "Succes", "Maintenance mise a jour avec succes");

            // Pause de 1 seconde avant de revenir à ShowMaintenance
            javafx.animation.PauseTransition pause = new javafx.animation.PauseTransition(javafx.util.Duration.seconds(1));
            pause.setOnFinished(e -> {
                try {
                    // Charger ShowMaintenance.fxml
                    FXMLLoader loader = new FXMLLoader(getClass().getResource("/interfaces/ShowMaintenance.fxml"));
                    javafx.scene.Parent root = loader.load();

                    // Remplacer la scène actuelle par ShowMaintenance
                    ((javafx.scene.Node) event.getSource()).getScene().setRoot(root);
                } catch (Exception ex) {
                    showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de retourner à la liste: " + ex.getMessage());
                }
            });
            pause.play();

        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", e.getMessage());
        }
    }


    private void showAlert(Alert.AlertType type, String title, String msg) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(msg);
        alert.showAndWait();
    }
}
