package controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;
import models.Maintenance;
import services.ServiceMaintenance;

import java.io.File;
import java.sql.SQLException;
import java.time.LocalDate;

public class AddMaintenanceController {
    ServiceMaintenance ms = new ServiceMaintenance();

    @FXML
    private ChoiceBox<String> type;

    @FXML
    private ChoiceBox<String> priorite;

    @FXML
    private TextField dateDeclarationTf;

    @FXML
    private TextField descriptionTf;

    @FXML
    private TextField lieuTf;

    @FXML
    private TextField equipementTf;
    @FXML private javafx.scene.control.Label typeStar;
    @FXML private javafx.scene.control.Label lieuStar;
    @FXML private javafx.scene.control.Label equipementStar;
    @FXML private javafx.scene.control.Label prioriteStar;
    @FXML private javafx.scene.control.Label descriptionStar;


    @FXML
    private javafx.scene.control.Button Save;

    @FXML
    void saveMaintenance(ActionEvent event) {
        try {
            // Vérification des champs obligatoires
            if (type.getValue() == null || type.getValue().isEmpty()) {
                showAlert(Alert.AlertType.WARNING, "Validation", "Veuillez selectionner un type");
                return;
            }
            if (priorite.getValue() == null || priorite.getValue().isEmpty()) {
                showAlert(Alert.AlertType.WARNING, "Validation", "Veuillez selectionner une priorite");
                return;
            }

            if (descriptionTf.getText() == null || descriptionTf.getText().trim().isEmpty()) {
                showAlert(Alert.AlertType.WARNING, "Validation", "Veuillez entrer une description");
                return;
            }
            if (lieuTf.getText() == null || lieuTf.getText().trim().isEmpty()) {
                showAlert(Alert.AlertType.WARNING, "Validation", "Veuillez entrer le lieu");
                return;
            }

            if (equipementTf.getText() == null || equipementTf.getText().trim().isEmpty()) {
                showAlert(Alert.AlertType.WARNING, "Validation", "Veuillez entrer l'équipement");
                return;
            }

            // Créer l'objet Maintenance avec les nouveaux champs
            Maintenance maintenance = new Maintenance(
                    LocalDate.parse(dateDeclarationTf.getText()),
                    type.getValue(),
                    descriptionTf.getText(),
                    "En cours",          // Statut par défaut
                    0,                   // idTechnicien par défaut
                    priorite.getValue(),
                    lieuTf.getText(),
                    equipementTf.getText()

            );

            // Sauvegarder
            ms.ajouter(maintenance);

            // Message succès
            showAlert(Alert.AlertType.INFORMATION, "Succes", "Maintenance enregistree avec succes");

            javafx.animation.PauseTransition pause = new javafx.animation.PauseTransition(javafx.util.Duration.seconds(1));
            pause.setOnFinished(e -> {
                try {
                    FXMLLoader loader = new FXMLLoader(getClass().getResource("/interfaces/ShowMaintenance.fxml"));
                    javafx.scene.Parent root = loader.load();

                    // Utiliser event.getSource() pour récupérer la scène actuelle
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

    @FXML
    void initialize() {
        type.getItems().addAll("Preventive", "Corrective", "Predictive");
        priorite.getItems().addAll("Faible", "Normale", "Urgente");
        LocalDate today = LocalDate.now();
        dateDeclarationTf.setText(today.toString());
        // Validation dynamique TextField
        descriptionTf.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null || newVal.trim().isEmpty()) {
                descriptionStar.setStyle("-fx-text-fill: red;");
            } else {
                descriptionStar.setStyle("-fx-text-fill: green;");
            }
        });

        lieuTf.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null || newVal.trim().isEmpty()) {
                lieuStar.setStyle("-fx-text-fill: red;");
            } else {
                lieuStar.setStyle("-fx-text-fill: green;");
            }
        });

        equipementTf.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null || newVal.trim().isEmpty()) {
                equipementStar.setStyle("-fx-text-fill: red;");
            } else {
                equipementStar.setStyle("-fx-text-fill: green;");
            }
        });

// Validation dynamique ChoiceBox
        type.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null) {
                typeStar.setStyle("-fx-text-fill: red;");
            } else {
                typeStar.setStyle("-fx-text-fill: green;");
            }
        });

        priorite.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null) {
                prioriteStar.setStyle("-fx-text-fill: red;");
            } else {
                prioriteStar.setStyle("-fx-text-fill: green;");
            }
        });



    }

    private void clearFields() {
        type.setValue(null);
        priorite.setValue(null);
        LocalDate today = LocalDate.now();
        dateDeclarationTf.setText(today.toString());
        descriptionTf.clear();
        lieuTf.clear();
        equipementTf.clear();

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
            Parent root = new FXMLLoader(getClass().getResource("/interfaces/ShowMaintenance.fxml")).load();
            Save.getScene().setRoot(root);
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Erreur de navigation", e.getMessage());
        }
    }
}
