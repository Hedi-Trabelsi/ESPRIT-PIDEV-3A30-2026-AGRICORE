package Controller;

import javafx.animation.PauseTransition;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.util.Duration;
import Model.Tache;
import Model.Maintenance;
import services.PdfService;
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
    private Button aiAssistantBtn;
    @FXML
    private DatePicker datePrevueDp;

    @FXML
    private TextArea descriptionTa;

    @FXML
    private TextField coutTf;

    @FXML
    private TextField nomTacheTf; // AJOUT : Champ pour le nom

    @FXML
    private ComboBox<Maintenance> maintenanceCb;

    @FXML
    private Button saveBtn;

    @FXML
    private Button cancelBtn;
    @FXML
    private Label statusLabel;

    @FXML
    private Label dateStar, descriptionStar, coutStar, maintenanceStar, nomStar; // AJOUT : nomStar
    @FXML
    private Label dateError, coutError, maintenanceError, nomError; // AJOUT : nomError
    private Maintenance maintenanceAutomatique;

    public void setMaintenanceSelectionnee(Maintenance m) {
        this.maintenanceAutomatique = m;
        if (m != null && maintenanceCb != null) {
            maintenanceCb.getItems().clear();
            maintenanceCb.getItems().add(m);
            maintenanceCb.setValue(m);
            maintenanceCb.setDisable(true);
        }
    }
    @FXML
    void cancel(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/ShowMaintenance.fxml"));
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

        // AJOUT : Écouteur pour le nom de la tâche
        nomTacheTf.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null || newVal.trim().isEmpty()) {
                nomStar.setStyle("-fx-text-fill: red;");
            } else {
                nomStar.setStyle("-fx-text-fill: green;");
            }
        });

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
            if (nomTacheTf.getText().trim().isEmpty()) valid = false; // AJOUT : Validation nom
            if (datePrevueDp.getValue() == null || datePrevueDp.getValue().isBefore(LocalDate.now())) valid = false;
            if (descriptionTa.getText().trim().isEmpty()) valid = false;
            if (coutTf.getText().trim().isEmpty() || coutTf.getText().matches(".*[a-zA-Z]+.*")) valid = false;

            // ON UTILISE UNIQUEMENT LA VARIABLE PASSÉE DEPUIS L'AUTRE PAGE
            Maintenance selectedMaintenance = this.maintenanceAutomatique;

            if (selectedMaintenance == null) {
                showAlert(Alert.AlertType.ERROR, "Erreur", "Aucune maintenance associée.");
                return;
            }

            if (!valid) {
                showAlert(Alert.AlertType.WARNING, "Validation", "Veuillez corriger les champs.");
                return;
            }

            // --- 2. PREPARATION ---
            int cout = Integer.parseInt(coutTf.getText().trim());

            String nomFichierPdf = System.getProperty("user.home") + "/Documents/Rapport_Tache_" + selectedMaintenance.getId() + ".pdf";

            // AJOUT : Inclusion du nomTache dans l'objet Tache
            Tache tache = new Tache(
                    nomTacheTf.getText(),
                    datePrevueDp.getValue().toString(),
                    descriptionTa.getText(),
                    cout,
                    selectedMaintenance.getId()
            );

            // --- 3. UI : PATIENCE ---
            saveBtn.setDisable(true);
            saveBtn.setText(" Traitement en cours...");

            // --- 4. LE THREAD ---
            new Thread(() -> {
                try {
                    serviceTache.ajouter(tache);
                    selectedMaintenance.setStatut("Planifie");
                    serviceMaintenance.modifier(selectedMaintenance);

                    PdfService.genererRapportTache(
                            descriptionTa.getText(),
                            datePrevueDp.getValue().toString(),
                            String.valueOf(cout),
                            nomFichierPdf
                    );

                    /* --- PARTIE EMAIL DÉSACTIVÉE ---
                    services.EmailService.envoyerEmailTache(
                            "mrabetzeineb1@gmail.com",
                            selectedMaintenance.getDescription(),
                            descriptionTa.getText(),
                            datePrevueDp.getValue().toString(),
                            String.valueOf(cout),
                            nomFichierPdf
                    );
                    -------------------------------- */

                    /* --- PARTIE SMS DÉSACTIVÉE ---
                    String messageSms = " Nouvelle tâche !\n" +
                            "Équipement: " + selectedMaintenance.getEquipement() + "\n" +
                            "Date: " + datePrevueDp.getValue().toString();

                    services.SmsService.envoyerSms("+21651042268", messageSms);
                    ------------------------------- */

                    javafx.application.Platform.runLater(() -> {
                        try {
                            java.awt.Desktop.getDesktop().open(new java.io.File(nomFichierPdf));
                        } catch (Exception e) {}

                        showAlert(Alert.AlertType.INFORMATION, "Succès", "Tâche enregistrée !");

                        PauseTransition pause = new PauseTransition(Duration.seconds(1));
                        pause.setOnFinished(ev -> {
                            try {
                                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/ShowTache.fxml"));
                                Parent root = loader.load();
                                saveBtn.getScene().setRoot(root);
                            } catch (Exception ex) { ex.printStackTrace(); }
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

        if (selectedMaint == null) {
            showAlert(Alert.AlertType.WARNING, "Assistant IA", "Veuillez d'abord sélectionner une maintenance dans la liste.");
            return;
        }

        final String originalDescription = selectedMaint.getDescription();
        final String equipement = selectedMaint.getEquipement();
        final String type = selectedMaint.getType();
        final String priorite = selectedMaint.getPriorite();

        String notesText = descriptionTa.getText();
        final String fullContext = "Description initiale de la panne : " + originalDescription +
                ((notesText != null && !notesText.trim().isEmpty()) ? " | Complément tech : " + notesText : "");

        aiAssistantBtn.setDisable(true);
        aiAssistantBtn.setText(" IA en cours d'analyse...");

        new Thread(() -> {
            try {
                String diagnosticIA = services.OpenAIService.getAICompletion(type, priorite, equipement, fullContext);

                javafx.application.Platform.runLater(() -> {
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