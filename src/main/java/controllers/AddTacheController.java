package controllers;

import javafx.animation.PauseTransition;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.util.Duration;
import models.Tache;
import models.Maintenance;
import services.ServiceTache;
import services.ServiceMaintenance;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

public class AddTacheController {

    private final ServiceTache serviceTache = new ServiceTache();
    private final ServiceMaintenance serviceMaintenance = new ServiceMaintenance();

    @FXML
    private DatePicker datePrevueDp;

    @FXML
    private TextArea descriptionTa;

    @FXML
    private TextField coutTf;

    @FXML
    private ChoiceBox<Maintenance> maintenanceCb;

    @FXML
    private Button saveBtn;

    @FXML
    private Button cancelBtn;


    @FXML
    void initialize() {
        // Date prévue par défaut = aujourd'hui
        datePrevueDp.setValue(LocalDate.now());

        try {
            List<Maintenance> maintenances = serviceMaintenance.afficher();

            // Ajouter les maintenances au ChoiceBox
            maintenanceCb.getItems().addAll(maintenances);

            // Convertir Maintenance en texte lisible avec plus de détails
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
            maintenanceCb.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal != null) {
                    try {
                        // Changer le statut à "Planifié"
                        newVal.setStatut("Planifié");
                        serviceMaintenance.modifier(newVal); // Mettre à jour dans la base
                    } catch (SQLException e) {
                        showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de mettre à jour le statut: " + e.getMessage());
                    }
                }
            });
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de charger les maintenances: " + e.getMessage());
        }
    }



    @FXML
    void saveTache(ActionEvent event) {
        try {
            // Validation des champs
            if (maintenanceCb.getValue() == null) {
                showAlert(Alert.AlertType.WARNING, "Validation", "Veuillez selectionner une maintenance");
                return;
            }
            if (descriptionTa.getText() == null || descriptionTa.getText().trim().isEmpty()) {
                showAlert(Alert.AlertType.WARNING, "Validation", "Veuillez entrer une description");
                return;
            }
            if (coutTf.getText() == null || coutTf.getText().trim().isEmpty()) {
                showAlert(Alert.AlertType.WARNING, "Validation", "Veuillez entrer le coût estime");
                return;
            }
            // Vérifier que la date n'est pas dans le passé
            if (datePrevueDp.getValue() == null || datePrevueDp.getValue().isBefore(LocalDate.now())) {
                showAlert(Alert.AlertType.WARNING, "Validation", "Veuillez choisir une date prévue valide (aujourd'hui ou plus tard).");
                return;
            }


            int cout = Integer.parseInt(coutTf.getText().trim());

            // Création de l'objet Tache
            Tache tache = new Tache(
                    datePrevueDp.getValue().toString(),
                    descriptionTa.getText(),
                    cout,
                    maintenanceCb.getValue().getId() // id_maintenance
            );

            // Sauvegarde
            serviceTache.ajouter(tache);

            // Message succès
            showAlert(Alert.AlertType.INFORMATION, "Succès", "Tache enregistrée avec succes");

            // Pause puis retour à la liste
            PauseTransition pause = new PauseTransition(Duration.seconds(1));
            pause.setOnFinished(e -> {
                try {
                    Parent root = new FXMLLoader(getClass().getResource("/interfaces/ShowTache.fxml")).load();
                    saveBtn.getScene().setRoot(root);
                } catch (Exception ex) {
                    showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de retourner à la liste: " + ex.getMessage());
                }
            });
            pause.play();

        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", e.getMessage());
        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Le coût estimé doit être un nombre entier");
        }
    }

    @FXML
    void cancel(ActionEvent event) {
        try {
            Parent root = new FXMLLoader(getClass().getResource("/interfaces/ShowTache.fxml")).load();
            cancelBtn.getScene().setRoot(root);
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de retourner à la liste: " + e.getMessage());
        }
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(message);
        alert.showAndWait();
    }
}
