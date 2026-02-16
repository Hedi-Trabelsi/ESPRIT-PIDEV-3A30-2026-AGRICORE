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

public class UpdateTacheController {

    private final ServiceTache serviceTache = new ServiceTache();
    private final ServiceMaintenance serviceMaintenance = new ServiceMaintenance();

    private Tache tache; // La tâche à mettre à jour

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

    // Méthode pour passer la tâche à modifier
    public void setTache(Tache tache) {
        this.tache = tache;
        populateFields();
    }

    private void populateFields() {
        if (tache != null) {
            datePrevueDp.setValue(LocalDate.parse(tache.getDate_prevue()));
            descriptionTa.setText(tache.getDesciption());
            coutTf.setText(String.valueOf(tache.getCout_estimee()));

            try {
                List<Maintenance> maintenances = serviceMaintenance.afficher();
                maintenanceCb.getItems().addAll(maintenances);

                // Sélectionner la maintenance associée
                for (Maintenance m : maintenances) {
                    if (m.getId() == tache.getId_maintenace()) {
                        maintenanceCb.setValue(m);
                        break;
                    }
                }

                // Affichage lisible
                maintenanceCb.setConverter(new javafx.util.StringConverter<Maintenance>() {
                    @Override
                    public String toString(Maintenance m) {
                        if (m == null) return "";
                        return m.getType() + " - " + m.getLieu(); // affichage lisible
                    }

                    @Override
                    public Maintenance fromString(String string) {
                        return null; // pas utilisé, nécessaire pour compiler
                    }
                });


            } catch (SQLException e) {
                showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de charger les maintenances: " + e.getMessage());
            }
        }
    }

    @FXML
    void saveTache(ActionEvent event) {
        try {
            if (maintenanceCb.getValue() == null) {
                showAlert(Alert.AlertType.WARNING, "Validation", "Veuillez sélectionner une maintenance");
                return;
            }
            if (descriptionTa.getText() == null || descriptionTa.getText().trim().isEmpty()) {
                showAlert(Alert.AlertType.WARNING, "Validation", "Veuillez entrer une description");
                return;
            }
            if (coutTf.getText() == null || coutTf.getText().trim().isEmpty()) {
                showAlert(Alert.AlertType.WARNING, "Validation", "Veuillez entrer le coût estimé");
                return;
            }
            if (datePrevueDp.getValue() == null || datePrevueDp.getValue().isBefore(LocalDate.now())) {
                showAlert(Alert.AlertType.WARNING, "Validation", "Veuillez choisir une date prévue valide");
                return;
            }

            int cout = Integer.parseInt(coutTf.getText().trim());

            // Mettre à jour la tâche
            tache.setDate_prevue(datePrevueDp.getValue().toString());
            tache.setDesciption(descriptionTa.getText());
            tache.setCout_estimee(cout);
            tache.setId_maintenace(maintenanceCb.getValue().getId());

            serviceTache.modifier(tache);

            showAlert(Alert.AlertType.INFORMATION, "Succès", "Tâche mise à jour avec succès");

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
