package Controller;

import javafx.animation.PauseTransition;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.util.Duration;
import Model.Maintenance;
import services.ServiceMaintenance;

import java.sql.SQLException;

public class UpdateMaintenanceController {

    private final ServiceMaintenance service = new ServiceMaintenance();
    private Maintenance maintenance;

    @FXML private ChoiceBox<String> type;
    @FXML private ChoiceBox<String> priorite;
    @FXML private TextField dateDeclarationTf;
    @FXML private TextArea descriptionTf;
    @FXML private TextField lieuTf;
    @FXML private TextField equipementTf;
    @FXML private Button updateBtn;

    @FXML
    void initialize() {
        type.getItems().addAll("Preventive", "Corrective", "Predictive");
        priorite.getItems().addAll("Faible", "Normale", "Urgente");
    }

    public void setMaintenance(Maintenance m) {
        this.maintenance = m;
        if (m != null) {
            type.setValue(m.getType());
            priorite.setValue(m.getPriorite());

            // On remplit le TextField avec la date sous forme de texte
            dateDeclarationTf.setText(m.getDateDeclaration().toString());

            descriptionTf.setText(m.getDescription());
            lieuTf.setText(m.getLieu());
            equipementTf.setText(m.getEquipement());
        }
    }

    @FXML
    void updateMaintenance(ActionEvent event) {
        try {
            if (type.getValue() == null || priorite.getValue() == null ||
                    lieuTf.getText().isEmpty() || equipementTf.getText().isEmpty()) {
                showAlert(Alert.AlertType.WARNING, "Validation", "Veuillez remplir tous les champs.");
                return;
            }

            maintenance.setType(type.getValue());
            maintenance.setPriorite(priorite.getValue());
            maintenance.setDescription(descriptionTf.getText());
            maintenance.setLieu(lieuTf.getText());
            maintenance.setEquipement(equipementTf.getText());

            service.modifier(maintenance);

            updateBtn.setDisable(true);
            updateBtn.setText("Mise à jour...");

            showAlert(Alert.AlertType.INFORMATION, "Succès", "Maintenance mise à jour !");

            PauseTransition pause = new PauseTransition(Duration.seconds(1));
            pause.setOnFinished(e -> returnToList());
            pause.play();

        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", e.getMessage());
        }
    }

    @FXML
    void cancelUpdate(MouseEvent event) {
        returnToList();
    }

    private void returnToList() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/fxml/ShowMaintenance.fxml"));
            updateBtn.getScene().setRoot(root);
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de retourner : " + e.getMessage());
        }
    }

    private void showAlert(Alert.AlertType type, String title, String msg) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(msg);
        alert.showAndWait();
    }
}