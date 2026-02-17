package controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import models.Maintenance;
import services.ServiceMaintenance;

import java.io.File;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

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
    private Label lieuError;

    @FXML
    private Label equipementError;

    @FXML
    void saveMaintenance(ActionEvent event) {
        try {
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

            if (!isValidText(lieuTf.getText())) {
                showAlert(Alert.AlertType.WARNING, "Validation", "Le lieu doit contenir au moins 3 lettres et pas uniquement des chiffres");
                return;
            }

            if (!isValidText(equipementTf.getText())) {
                showAlert(Alert.AlertType.WARNING, "Validation", "L'équipement doit contenir au moins 3 lettres et pas uniquement des chiffres");
                return;
            }

            // Créer l'objet Maintenance avec les nouveaux champs
            Maintenance maintenance = new Maintenance(
                    LocalDate.parse(dateDeclarationTf.getText()),
                    type.getValue(),
                    descriptionTf.getText(),
                    "En attente",
                    0,
                    priorite.getValue(),
                    lieuTf.getText(),
                    equipementTf.getText()
            );

            // Sauvegarder
            ms.ajouter(maintenance);

            // Message succès
            showAlert(Alert.AlertType.INFORMATION, "Succes", "Maintenance enregistree avec succes");

            // Retour à la liste après 1s
            javafx.animation.PauseTransition pause = new javafx.animation.PauseTransition(javafx.util.Duration.seconds(1));
            pause.setOnFinished(e -> {
                try {
                    FXMLLoader loader = new FXMLLoader(getClass().getResource("/interfaces/ShowMaintenance.fxml"));
                    javafx.scene.Parent root = loader.load();
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
    private final ServiceMaintenance serviceMaintenance = new ServiceMaintenance();
    @FXML
    private ComboBox<Maintenance> maintenanceCb;

    @FXML
    private DatePicker datePrevueDp;

    @FXML
    void initialize() {
        datePrevueDp.setValue(LocalDate.now());

        try {
            // Récupérer toutes les maintenances
            List<Maintenance> maintenances = serviceMaintenance.afficher();

            // Ajouter les maintenances au ComboBox
            maintenanceCb.getItems().addAll(maintenances);

            // Affichage lisible pour le client
            maintenanceCb.setConverter(new javafx.util.StringConverter<Maintenance>() {
                @Override
                public String toString(Maintenance m) {
                    if (m == null) return "";
                    return m.getType()
                            + " | Date: " + m.getDateDeclaration()
                            + " | Lieu: " + m.getLieu()
                            + " | Équipement: " + m.getEquipement();
                }

                @Override
                public Maintenance fromString(String string) {
                    return null; // pas utilisé
                }
            });

            // Quand on sélectionne une maintenance, on peut changer le statut à "Planifié"
            maintenanceCb.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal != null) {
                    try {
                        newVal.setStatut("Planifié");
                        serviceMaintenance.modifier(newVal);
                    } catch (SQLException e) {
                        showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de mettre à jour le statut: " + e.getMessage());
                    }
                }
            });

        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de charger les maintenances: " + e.getMessage());
        }
    }


    // Méthode utilitaire pour valider texte (au moins 3 lettres et pas que des chiffres)
    private boolean isValidText(String text) {
        if (text == null) return false;
        String trimmed = text.trim();
        return trimmed.length() >= 3 && !trimmed.matches("\\d+");
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
