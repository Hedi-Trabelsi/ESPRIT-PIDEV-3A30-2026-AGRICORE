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
import services.PdfService;
import services.ServiceTache;
import services.EmailService;
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
    private Label statusLabel;

    @FXML
    private Label dateStar, descriptionStar, coutStar, maintenanceStar;
    @FXML
    private Label dateError, coutError, maintenanceError;

    @FXML
    void cancel(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/interfaces/ShowMaintenance.fxml"));
            Parent root = loader.load();
            cancelBtn.getScene().setRoot(root);
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de retourner a la liste: " + e.getMessage());
        }
    }

    @FXML
    void initialize() {
        // Date prevue par defaut
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
                            + " | equipement: " + m.getEquipement()
                            + " | Statut: " + m.getStatut()
                            + " | Priorite:" +m.getPriorite();


                }

                @Override
                public Maintenance fromString(String string) {
                    return null; // pas utilise
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
            // --- 1. VALIDATION (Rapide, on reste sur le thread principal) ---
            boolean valid = true;
            if (datePrevueDp.getValue() == null || datePrevueDp.getValue().isBefore(LocalDate.now())) valid = false;
            if (descriptionTa.getText().trim().isEmpty()) valid = false;
            if (coutTf.getText().trim().isEmpty() || coutTf.getText().matches(".*[a-zA-Z]+.*")) valid = false;
            if (maintenanceCb.getValue() == null) valid = false;

            if (!valid) {
                showAlert(Alert.AlertType.WARNING, "Validation", "Veuillez corriger les champs.");
                return;
            }

            // --- 2. PRÉPARATION ---
            int cout = Integer.parseInt(coutTf.getText().trim());
            Maintenance selectedMaintenance = maintenanceCb.getValue();
            String nomFichier = System.getProperty("user.home") + "/Documents/Rapport_Tache_" + selectedMaintenance.getId() + ".pdf";

            Tache tache = new Tache(
                    datePrevueDp.getValue().toString(),
                    descriptionTa.getText(),
                    cout,
                    selectedMaintenance.getId()
            );

            // --- 3. UI : INFORMER LE TECH QU'IL DOIT ATTENDRE ---
            saveBtn.setDisable(true); // On désactive le bouton
            saveBtn.setText(" Envoi en cours..."); // On change le texte du bouton

            // --- 4. LANCER LE TRAVAIL LOURD DANS UN THREAD SÉPARÉ ---
            new Thread(() -> {
                try {
                    // Actions BDD
                    serviceTache.ajouter(tache);
                    selectedMaintenance.setStatut("Planifie");
                    serviceMaintenance.modifier(selectedMaintenance);

                    // API PDF
                    PdfService.genererRapportTache(
                            descriptionTa.getText(),
                            datePrevueDp.getValue().toString(),
                            String.valueOf(cout),
                            nomFichier
                    );

                    // API Email
                    String emailDestinataire = "mrabetzeineb1@gmail.com";
                    services.EmailService.envoyerEmailTache(
                            emailDestinataire,
                            selectedMaintenance.getDescription(),
                            descriptionTa.getText(),
                            datePrevueDp.getValue().toString(),
                            String.valueOf(cout),
                            nomFichier
                    );

                    // --- 5. RETOUR SUR L'INTERFACE (UI) QUAND C'EST FINI ---
                    javafx.application.Platform.runLater(() -> {
                        try {
                            // Ouvrir le PDF
                            java.awt.Desktop.getDesktop().open(new java.io.File(nomFichier));
                        } catch (Exception e) {
                            System.out.println("Erreur ouverture PDF : " + e.getMessage());
                        }

                        showAlert(Alert.AlertType.INFORMATION, "Succes", "Tache enregistree, PDF genere et Email envoye !");

                        // Redirection après succès
                        PauseTransition pause = new PauseTransition(Duration.seconds(1));
                        pause.setOnFinished(ev -> {
                            try {
                                FXMLLoader loader = new FXMLLoader(getClass().getResource("/interfaces/ShowTache.fxml"));
                                Parent root = loader.load();
                                saveBtn.getScene().setRoot(root);
                            } catch (Exception ex) {
                                ex.printStackTrace();
                            }
                        });
                        pause.play();
                    });

                } catch (Exception e) {
                    // En cas d'erreur dans le thread
                    javafx.application.Platform.runLater(() -> {
                        saveBtn.setDisable(false);
                        saveBtn.setText("Enregistrer");
                        showAlert(Alert.AlertType.ERROR, "Erreur", "Une erreur est survenue : " + e.getMessage());
                    });
                }
            }).start();

        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Le coût doit être un nombre.");
        }
    }




    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(message);
        alert.showAndWait();
    }
}
