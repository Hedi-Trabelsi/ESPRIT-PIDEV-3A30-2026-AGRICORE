package controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TextField;
import models.Maintenance;
import services.ServiceMaintenance;

import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;

public class AddMaintenanceController {
    ServiceMaintenance ms = new ServiceMaintenance();

    @FXML
    private ChoiceBox<String> type;

    @FXML
    private ChoiceBox<String> priorite;

    @FXML
    private DatePicker dateDeclarationDp;

    @FXML
    private TextField descriptionTf;

    @FXML
    private javafx.scene.control.Button Save;

    @FXML
    void saveMaintenance(ActionEvent event) {
        try {
            // Vérifier que tous les champs obligatoires sont remplis
            if (type.getValue() == null || type.getValue().isEmpty()) {
                showAlert(Alert.AlertType.WARNING, "Validation", "Veuillez selectionner un type");
                return;
            }
            if (priorite.getValue() == null || priorite.getValue().isEmpty()) {
                showAlert(Alert.AlertType.WARNING, "Validation", "Veuillez selectionner une priorite");
                return;
            }
            if (dateDeclarationDp.getValue() == null) {
                showAlert(Alert.AlertType.WARNING, "Validation", "Veuillez selectionner une date");
                return;
            }
            if (descriptionTf.getText() == null || descriptionTf.getText().trim().isEmpty()) {
                showAlert(Alert.AlertType.WARNING, "Validation", "Veuillez entrer une description");
                return;
            }

            // Créer l'objet Maintenance
            Maintenance maintenance = new Maintenance(
                    type.getValue(),
                    dateDeclarationDp.getValue(),
                    descriptionTf.getText(),
                    "En cours",  // Statut par défaut
                    0,           // idTechnicien null/par défaut
                    priorite.getValue()
            );

            // Sauvegarder
            ms.ajouter(maintenance);

            // Afficher message de succès
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Succes");
            alert.setHeaderText("Maintenance enregistrée avec succes");
            alert.showAndWait();

            // Réinitialiser les champs
            clearFields();

        } catch (SQLException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Erreur");
            alert.setHeaderText("Erreur lors de l'enregistrement");
            alert.setContentText(e.getMessage());
            alert.showAndWait();
        }
    }

    @FXML
    void initialize() {
        // Initialiser les ChoiceBox avec les options
        type.getItems().addAll("Préventive", "Curative", "Corrective");
        priorite.getItems().addAll("Basse", "Moyenne", "Haute");
    }

    private void clearFields() {
        type.setValue(null);
        priorite.setValue(null);
        dateDeclarationDp.setValue(null);
        descriptionTf.clear();
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(message);
        alert.showAndWait();
    }

    @FXML
    void navigateShowMaintenance(ActionEvent event) {
        try {
            Parent root = new FXMLLoader(getClass().getResource("/ShowMaintenance.fxml")).load();
            Save.getScene().setRoot(root);
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Erreur");
            alert.setHeaderText("Erreur de navigation");
            alert.setContentText(e.getMessage());
            alert.showAndWait();
        }
    }
}