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
import java.util.stream.Collectors;

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
    private ComboBox<Maintenance> maintenanceCb;

    @FXML
    private Button saveBtn;

    @FXML
    private Button cancelBtn;


    @FXML
    private Label dateStar, descriptionStar, coutStar, maintenanceStar;
    @FXML
    private Label dateError, coutError, maintenanceError;

    @FXML
    void initialize() {
        // Date prévue par défaut
        datePrevueDp.setValue(LocalDate.now());

        try {
            List<Maintenance> maints = serviceMaintenance.afficher().stream()
                    .filter(m -> !m.getStatut().equalsIgnoreCase("Planifie")
                            && !m.getStatut().equalsIgnoreCase("Resolu")&& !m.getStatut().equalsIgnoreCase("Refuse"))
                    .collect(Collectors.toList());


            maintenanceCb.getItems().addAll(maints);

            maintenanceCb.setConverter(new javafx.util.StringConverter<Maintenance>() {
                @Override
                public String toString(Maintenance m) {
                    if (m == null) return "";
                    return m.getType()
                            + " | Date: " + m.getDateDeclaration()
                            + " | Lieu: " + m.getLieu()
                            + " | Équipement: " + m.getEquipement()
                            + " | Statut: " + m.getStatut()
                            + " | Priorite:" +m.getPriorite();


                }

                @Override
                public Maintenance fromString(String string) {
                    return null; // pas utilisé
                }
            });


            // Validation dynamique
            maintenanceCb.valueProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal == null) {
                    maintenanceStar.setStyle("-fx-text-fill: red;");
                    maintenanceError.setText("Veuillez selectionner une maintenance");
                } else {
                    maintenanceStar.setStyle("-fx-text-fill: green;");
                    maintenanceError.setText("");
                }
            });

        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de charger les maintenances: " + e.getMessage());
        }

        // Autres validations (date, cout, description)
        datePrevueDp.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null || newVal.isBefore(LocalDate.now())) {
                dateStar.setStyle("-fx-text-fill: red;");
                dateError.setText("La date doit etre aujourd'hui ou apres");
            } else {
                dateStar.setStyle("-fx-text-fill: green;");
                dateError.setText("");
            }
        });

        coutTf.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null || newVal.trim().isEmpty() || newVal.matches("[a-zA-Z]+")) {
                coutStar.setStyle("-fx-text-fill: red;");
                coutError.setText("Le cout doit contenir au moins un chiffre");
            } else {
                coutStar.setStyle("-fx-text-fill: green;");
                coutError.setText("");
            }
        });

        descriptionTa.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null || newVal.trim().isEmpty()) {
                descriptionStar.setStyle("-fx-text-fill: red;");
            } else {
                descriptionStar.setStyle("-fx-text-fill: green;");
            }
        });
    }

    @FXML
    void saveTache(ActionEvent event) {
        try {
            boolean valid = true;

            if (datePrevueDp.getValue() == null || datePrevueDp.getValue().isBefore(LocalDate.now())) valid = false;
            if (descriptionTa.getText().trim().isEmpty()) valid = false;
            if (coutTf.getText().trim().isEmpty() || coutTf.getText().matches("[a-zA-Z]+")) valid = false;
            if (maintenanceCb.getValue() == null) valid = false;

            if (!valid) {
                showAlert(Alert.AlertType.WARNING, "Validation", "Veuillez corriger les champs en rouge.");
                return;
            }

            int cout = Integer.parseInt(coutTf.getText().trim());

            Tache tache = new Tache(
                    datePrevueDp.getValue().toString(),
                    descriptionTa.getText(),
                    cout,
                    maintenanceCb.getValue().getId()
            );

            serviceTache.ajouter(tache);
            showAlert(Alert.AlertType.INFORMATION, "Succès", "Tâche enregistrée avec succès");

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
