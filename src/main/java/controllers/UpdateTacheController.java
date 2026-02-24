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
import services.OpenAIService;
import services.ServiceTache;
import services.ServiceMaintenance;

import java.sql.SQLException;
import java.time.LocalDate;

public class UpdateTacheController {

    private final ServiceTache serviceTache = new ServiceTache();
    private final ServiceMaintenance serviceMaintenance = new ServiceMaintenance();

    private Tache tache; // La tâche a mettre a jour

    @FXML
    private DatePicker datePrevueDp;

    @FXML
    private TextArea descriptionTa;

    @FXML
    private TextField coutTf;

    @FXML
    private Label maintenanceLbl; // Label pour la maintenance associee (non modifiable)

    @FXML
    private Button saveBtn;

    @FXML
    private Button cancelBtn;

    /**
     * Methode pour passer la tâche a modifier
     */
    public void setTache(Tache tache) {
        this.tache = tache;
        populateFields();
    }

    /**
     * Remplit les champs avec les valeurs de la tâche
     */
    private void populateFields() {
        if (tache == null) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Tâche invalide");
            return;
        }

        // Champs modifiables
        datePrevueDp.setValue(LocalDate.parse(tache.getDate_prevue()));
        descriptionTa.setText(tache.getDesciption());
        coutTf.setText(String.valueOf(tache.getCout_estimee()));

        try {
            Maintenance m = serviceMaintenance.getMaintenanceById(tache.getId_maintenace());
            if (m != null) {
                maintenanceLbl.setText(m.getType() + " - " + m.getLieu());
            } else {
                maintenanceLbl.setText("Maintenance inconnue");
            }
        } catch (SQLException e) {
            maintenanceLbl.setText("Erreur chargement");
        }

    }

    /**
     * Sauvegarde les modifications
     */
    @FXML
    void saveTache(ActionEvent event) {
        try {
            // Validation
            if (descriptionTa.getText() == null || descriptionTa.getText().trim().isEmpty()) {
                showAlert(Alert.AlertType.WARNING, "Validation", "Veuillez entrer une description");
                return;
            }
            if (coutTf.getText() == null || coutTf.getText().trim().isEmpty()) {
                showAlert(Alert.AlertType.WARNING, "Validation", "Veuillez entrer le coût estime");
                return;
            }
            if (datePrevueDp.getValue() == null || datePrevueDp.getValue().isBefore(LocalDate.now())) {
                showAlert(Alert.AlertType.WARNING, "Validation", "Veuillez choisir une date prevue valide");
                return;
            }

            int cout = Integer.parseInt(coutTf.getText().trim());

            // Mise a jour de la tâche
            tache.setDate_prevue(datePrevueDp.getValue().toString());
            tache.setDesciption(descriptionTa.getText());
            tache.setCout_estimee(cout);

            serviceTache.modifier(tache);

            showAlert(Alert.AlertType.INFORMATION, "Succes", "Tâche mise a jour avec succes");

            // Retour a la liste apres un court delai
            PauseTransition pause = new PauseTransition(Duration.seconds(1));
            pause.setOnFinished(e -> {
                try {
                    Parent root = new FXMLLoader(getClass().getResource("/interfaces/ShowTache.fxml")).load();
                    saveBtn.getScene().setRoot(root);
                } catch (Exception ex) {
                    showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de retourner a la liste: " + ex.getMessage());
                }
            });
            pause.play();

        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", e.getMessage());
        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Le coût estime doit être un nombre entier");
        }
    }

    /**
     * Annule et retourne a la liste
     */
    @FXML
    void cancel(ActionEvent event) {
        try {
            Parent root = new FXMLLoader(getClass().getResource("/interfaces/ShowTache.fxml")).load();
            cancelBtn.getScene().setRoot(root);
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de retourner a la liste: " + e.getMessage());
        }
    }

    /**
     * Affiche une alerte
     */
    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(message);
        alert.showAndWait();
    }

}
