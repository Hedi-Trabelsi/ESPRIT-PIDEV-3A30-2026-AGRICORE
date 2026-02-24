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
    private Button aiAssistantBtn;
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
                    .filter(m -> m.getStatut().equalsIgnoreCase("en cours"))
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
            // --- 1. VALIDATION ---
            boolean valid = true;
            if (datePrevueDp.getValue() == null || datePrevueDp.getValue().isBefore(LocalDate.now())) valid = false;
            if (descriptionTa.getText().trim().isEmpty()) valid = false;
            if (coutTf.getText().trim().isEmpty() || coutTf.getText().matches(".*[a-zA-Z]+.*")) valid = false;
            if (maintenanceCb.getValue() == null) valid = false;

            if (!valid) {
                showAlert(Alert.AlertType.WARNING, "Validation", "Veuillez corriger les champs.");
                return;
            }

            // --- 2. PRePARATION ---
            int cout = Integer.parseInt(coutTf.getText().trim());
            Maintenance selectedMaintenance = maintenanceCb.getValue();

            // Chemin du fichier PDF uniquement
            String nomFichierPdf = System.getProperty("user.home") + "/Documents/Rapport_Tache_" + selectedMaintenance.getId() + ".pdf";

            Tache tache = new Tache(
                    datePrevueDp.getValue().toString(),
                    descriptionTa.getText(),
                    cout,
                    selectedMaintenance.getId()
            );

            // --- 3. UI : PATIENCE ---
            saveBtn.setDisable(true);
            saveBtn.setText(" Traitement en cours...");

            // --- 4. LE THREAD (TRAVAIL LOURD) ---
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
                            nomFichierPdf
                    );

                    // API Email
                    services.EmailService.envoyerEmailTache(
                            "mrabetzeineb1@gmail.com",
                            selectedMaintenance.getDescription(),
                            descriptionTa.getText(),
                            datePrevueDp.getValue().toString(),
                            String.valueOf(cout),
                            nomFichierPdf
                    );
                    String messageSms = " Nouvelle tâche de maintenance !\n" +
                            "Équipement: " + selectedMaintenance.getEquipement() + "\n" +
                            "Date prévue: " + datePrevueDp.getValue().toString() + "\n" +
                            "Lieu: " + selectedMaintenance.getLieu();

                    // Remplace par le numéro du technicien (ou le tien pour le test)
                    services.SmsService.envoyerSms("+21651042268", messageSms);
                    // --- 5. RETOUR UI ---
                    javafx.application.Platform.runLater(() -> {
                        try {
                            // On ouvre uniquement le PDF
                            java.awt.Desktop.getDesktop().open(new java.io.File(nomFichierPdf));
                        } catch (Exception e) {
                            System.out.println("Erreur ouverture PDF : " + e.getMessage());
                        }

                        showAlert(Alert.AlertType.INFORMATION, "Succes",
                                "Tâche enregistree, Rapport PDF genere et Email envoye !");

                        // Redirection
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
                    javafx.application.Platform.runLater(() -> {
                        saveBtn.setDisable(false);
                        saveBtn.setText("Enregistrer");
                        showAlert(Alert.AlertType.ERROR, "Erreur", e.getMessage());
                    });
                }
            }).start();

        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Le coût doit être un nombre.");
        }
    }


    @FXML
    void helpMeWithAI(ActionEvent event) {
        Maintenance selectedMaint = maintenanceCb.getValue();

        // On garde uniquement la condition sur la sélection de la maintenance
        if (selectedMaint == null) {
            showAlert(Alert.AlertType.WARNING, "Assistant IA", "Veuillez d'abord sélectionner une maintenance dans la liste.");
            return;
        }

        // --- VARIABLES FINALES POUR LE THREAD ---
        final String originalDescription = selectedMaint.getDescription();
        final String equipement = selectedMaint.getEquipement();
        final String type = selectedMaint.getType();
        final String priorite = selectedMaint.getPriorite();

        // On récupère les notes s'il y en a, sinon on envoie juste la description initiale
        String notesText = descriptionTa.getText();
        final String fullContext = "Description initiale de la panne : " + originalDescription +
                ((notesText != null && !notesText.trim().isEmpty()) ? " | Complément tech : " + notesText : "");

        // UI : On lance l'animation de chargement
        aiAssistantBtn.setDisable(true);
        aiAssistantBtn.setText(" IA en cours d'analyse...");

        new Thread(() -> {
            try {
                // L'IA analyse désormais même si 'notesText' est vide
                String diagnosticIA = services.OpenAIService.getAICompletion(type, priorite, equipement, fullContext);

                javafx.application.Platform.runLater(() -> {
                    // On affiche le résultat final dans le TextArea
                    descriptionTa.setText("[RAPPEL PANNE] : " + originalDescription
                            + "\n\n--- DIAGNOSTIC IA PRÉDICTIF ---\n"
                            + diagnosticIA);

                    aiAssistantBtn.setDisable(false);
                    aiAssistantBtn.setText(" Aide Diagnostic IA");
                });
            } catch (Exception e) {
                e.printStackTrace();
                javafx.application.Platform.runLater(() -> {
                    aiAssistantBtn.setDisable(false);
                    aiAssistantBtn.setText(" Aide Diagnostic IA");
                    showAlert(Alert.AlertType.ERROR, "Erreur", "L'IA n'a pas pu analyser la maintenance : " + e.getMessage());
                });
            }
        }).start();
    }
    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(message);
        alert.showAndWait();
    }
}
