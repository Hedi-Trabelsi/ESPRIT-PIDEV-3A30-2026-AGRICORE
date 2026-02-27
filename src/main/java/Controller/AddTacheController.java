package Controller;

import javafx.animation.PauseTransition;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent; // AJOUT : pour le clic sur Label
import javafx.util.Duration;
import Model.Tache;
import Model.Maintenance;
import services.PdfService;
import services.ServiceTache;
import services.ServiceMaintenance;

import java.sql.SQLException;
import java.time.LocalDate;

public class AddTacheController {

    private final ServiceTache serviceTache = new ServiceTache();
    private final ServiceMaintenance serviceMaintenance = new ServiceMaintenance();

    @FXML private Button aiAssistantBtn;
    @FXML private DatePicker datePrevueDp;
    @FXML private TextArea descriptionTa;
    @FXML private TextField coutTf;
    @FXML private TextField nomTacheTf;


    @FXML private Label maintenanceInfoLabel;
    @FXML private Label maintenanceDetailsLabel;

    @FXML private Button saveBtn;
    @FXML private Label dateStar, descriptionStar, coutStar, nomStar;
    @FXML private Label dateError, coutError, nomError;

    private Maintenance maintenanceAutomatique;

    @FXML private Label maintenanceDateLabel; // N'oublie pas de l'ajouter en haut avec @FXML

    public void setMaintenanceSelectionnee(Maintenance m) {
        this.maintenanceAutomatique = m;
        if (m != null) {
            maintenanceInfoLabel.setText(m.getNom_maintenance().toUpperCase());
            //maintenanceInfoLabel.setText(m.getType() + " - " + m.getEquipement());
            maintenanceDateLabel.setText("📅 " + m.getDateDeclaration().toString());

            // Le lieu (en dessous du nom)
            maintenanceDetailsLabel.setText(m.getLieu());
        }
    }

    // --- MODIFICATION : ActionEvent -> MouseEvent pour le Label de retour ---
    @FXML
    void cancel(MouseEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/ShowMaintenance.fxml"));
            Parent root = loader.load();
            ((javafx.scene.Node) event.getSource()).getScene().setRoot(root);
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de retourner : " + e.getMessage());
        }
    }

    @FXML
    void initialize() {
        datePrevueDp.setValue(LocalDate.now());

        nomTacheTf.textProperty().addListener((obs, oldVal, newVal) -> {
            nomStar.setStyle(newVal == null || newVal.trim().isEmpty() ? "-fx-text-fill: red;" : "-fx-text-fill: green;");
        });

        datePrevueDp.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null || newVal.isBefore(LocalDate.now())) {
                dateStar.setStyle("-fx-text-fill: red;");
                dateError.setText("Date invalide");
            } else {
                dateStar.setStyle("-fx-text-fill: green;");
                dateError.setText("");
            }
        });

        coutTf.textProperty().addListener((obs, oldVal, newVal) -> {
            boolean isInvalid = newVal == null || newVal.trim().isEmpty() || !newVal.matches("\\d+");
            coutStar.setStyle(isInvalid ? "-fx-text-fill: red;" : "-fx-text-fill: green;");
            coutError.setText(isInvalid ? "Chiffres uniquement" : "");
        });

        descriptionTa.textProperty().addListener((obs, oldVal, newVal) -> {
            descriptionStar.setStyle(newVal == null || newVal.trim().isEmpty() ? "-fx-text-fill: red;" : "-fx-text-fill: green;");
        });
    }

    @FXML
    void saveTache(ActionEvent event) {
        try {
            if (nomTacheTf.getText().trim().isEmpty() || datePrevueDp.getValue() == null ||
                    descriptionTa.getText().trim().isEmpty() || coutTf.getText().trim().isEmpty()) {
                showAlert(Alert.AlertType.WARNING, "Validation", "Veuillez remplir tous les champs.");
                return;
            }

            Maintenance selectedMaintenance = this.maintenanceAutomatique;
            if (selectedMaintenance == null) {
                showAlert(Alert.AlertType.ERROR, "Erreur", "Aucune maintenance associée.");
                return;
            }

            int cout = Integer.parseInt(coutTf.getText().trim());
            String nomFichierPdf = System.getProperty("user.home") + "/Documents/Rapport_Tache_" + selectedMaintenance.getId() + ".pdf";

            Tache tache = new Tache(
                    nomTacheTf.getText(),
                    datePrevueDp.getValue().toString(),
                    descriptionTa.getText(),
                    cout,
                    selectedMaintenance.getId()
            );

            saveBtn.setDisable(true);
            saveBtn.setText(" Traitement...");

            new Thread(() -> {
                try {
                    serviceTache.ajouter(tache);
                    selectedMaintenance.setStatut("Planifie");
                    serviceMaintenance.modifier(selectedMaintenance);
                    PdfService.genererRapportTache(descriptionTa.getText(), datePrevueDp.getValue().toString(), String.valueOf(cout), nomFichierPdf);

                    javafx.application.Platform.runLater(() -> {
                        showAlert(Alert.AlertType.INFORMATION, "Succès", "Tâche enregistrée !");
                        PauseTransition pause = new PauseTransition(Duration.seconds(1));
                        pause.setOnFinished(ev -> {
                            try {
                                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/ShowMaintenanceDetails.fxml"));
                                Parent root = loader.load();

                                // --- RÉCUPÉRATION DU CONTROLLER ET REPASSE DE LA MAINTENANCE ---
                                ShowMaintenanceDetailsController controller = loader.getController();
                                controller.setMaintenance(this.maintenanceAutomatique);

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

        if (maintenanceAutomatique == null) {
            showAlert(Alert.AlertType.WARNING, "Assistant IA", "Aucune maintenance détectée.");
            return;
        }

        aiAssistantBtn.setDisable(true);
        aiAssistantBtn.setText("✨ Analyse IA...");

        new Thread(() -> {
            try {
                String diagnosticIA = services.OpenAIService.getAICompletion(
                        maintenanceAutomatique.getType(),
                        maintenanceAutomatique.getPriorite(),
                        maintenanceAutomatique.getEquipement(),
                        "Panne: " + maintenanceAutomatique.getDescription()
                );

                javafx.application.Platform.runLater(() -> {
                    descriptionTa.setText("[RAPPEL] : " + maintenanceAutomatique.getDescription() + "\n\n--- IA DIAGNOSTIC ---\n" + diagnosticIA);
                    aiAssistantBtn.setDisable(false);
                    aiAssistantBtn.setText("✨ Aide Diagnostic IA");
                });
            } catch (Exception e) {
                javafx.application.Platform.runLater(() -> {
                    aiAssistantBtn.setDisable(false);
                    aiAssistantBtn.setText("✨ Aide Diagnostic IA");
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