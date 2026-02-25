package Controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import Model.Maintenance;
import services.ServiceMaintenance;

import java.sql.SQLException;

public class UpdateMaintenanceController {

    private final ServiceMaintenance service = new ServiceMaintenance();
    private Maintenance maintenance; // maintenance a modifier

    @FXML
    private ChoiceBox<String> type;

    @FXML
    private ChoiceBox<String> priorite;

    @FXML
    private DatePicker dateDeclarationDp;

    @FXML
    private TextArea descriptionTf;

    @FXML
    private TextField lieuTf;

    @FXML
    private TextField equipementTf;

    @FXML
    private TextField imageTf;

    // Methode pour recevoir la maintenance selectionnee
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
    void cancelUpdate(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/ShowMaintenance.fxml"));
            javafx.scene.Parent root = loader.load();

            // Remplacer la scene actuelle
            ((javafx.scene.Node) event.getSource()).getScene().setRoot(root);

        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Erreur",
                    "Impossible de retourner a la liste: " + e.getMessage());
        }
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

            // Appel a la methode modifier de ServiceMaintenance
            service.modifier(maintenance);

            // Affiche un message de succes
            showAlert(Alert.AlertType.INFORMATION, "Succes", "Maintenance mise a jour avec succes");

            // Pause de 1 seconde avant de revenir a ShowMaintenance
            javafx.animation.PauseTransition pause = new javafx.animation.PauseTransition(javafx.util.Duration.seconds(1));
            pause.setOnFinished(e -> {
                try {
                    // Charger ShowMaintenance.fxml
                    FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/ShowMaintenance.fxml"));
                    javafx.scene.Parent root = loader.load();

                    // Remplacer la scene actuelle par ShowMaintenance
                    ((javafx.scene.Node) event.getSource()).getScene().setRoot(root);
                } catch (Exception ex) {
                    showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de retourner a la liste: " + ex.getMessage());
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
