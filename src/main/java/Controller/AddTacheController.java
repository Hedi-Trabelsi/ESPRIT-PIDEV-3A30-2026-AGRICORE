package Controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent; // AJOUT : pour le clic sur Label
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

    public AddTacheController() throws SQLException {
    }

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
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/ShowMaintenanceDetails.fxml"));
            Parent root = loader.load();
            if (maintenanceAutomatique != null) {
                ShowMaintenanceDetailsController controller = loader.getController();
                controller.setMaintenance(maintenanceAutomatique);
            }
            NavigationUtil.loadInContentArea((javafx.scene.Node) event.getSource(), root);
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
            String nom = nomTacheTf.getText();
            String desc = descriptionTa.getText();
            String coutStr = coutTf.getText().trim();

            // 1. Validation : Champs vides
            if (nom == null || nom.trim().isEmpty() ||
                    desc == null || desc.trim().isEmpty() ||
                    coutStr.isEmpty() || datePrevueDp.getValue() == null) {
                showAlert(Alert.AlertType.WARNING, "Validation", "Veuillez remplir tous les champs.");
                return;
            }

            // 2. Validation du Titre : Pas uniquement des chiffres
            if (isNumericOnly(nom.trim())) {
                showAlert(Alert.AlertType.WARNING, "Validation", "Le titre ne peut pas contenir uniquement des chiffres.");
                return;
            }

            // 3. Validation de la Description : Pas uniquement des chiffres
            if (isNumericOnly(desc.trim())) {
                showAlert(Alert.AlertType.WARNING, "Validation", "La description ne peut pas contenir uniquement des chiffres.");
                return;
            }

            // --- Fin des validations, début du traitement ---

            // RÉCUPÉRATION DE L'UTILISATEUR CONNECTÉ
            Model.Utilisateur userConnecte = UserSession.getCurrentUser();
            if (userConnecte == null) {
                showAlert(Alert.AlertType.ERROR, "Erreur", "Session expirée. Veuillez vous reconnecter.");
                return;
            }

            Maintenance selectedMaintenance = this.maintenanceAutomatique;
            if (selectedMaintenance == null) {
                showAlert(Alert.AlertType.ERROR, "Erreur", "Aucune maintenance associée.");
                return;
            }

            int cout = Integer.parseInt(coutStr);
            String nomFichierPdf = System.getProperty("user.home") + "/Documents/Rapport_Tache_" + selectedMaintenance.getId() + ".pdf";

            // CRÉATION DE LA TACHE AVEC L'ID DYNAMIQUE
            Tache tache = new Tache(
                    nom.trim(),
                    datePrevueDp.getValue().toString(),
                    desc.trim(),
                    cout,
                    selectedMaintenance.getId(),
                    userConnecte.getId() // <--- Utilise le vrai ID de l'utilisateur connecté
            );

            saveBtn.setDisable(true);
            saveBtn.setText(" Traitement...");

            new Thread(() -> {
                try {
                    // 1. Actions BDD
                    serviceTache.ajouter(tache);
                    selectedMaintenance.setStatut("planifier");
                    serviceMaintenance.modifier(selectedMaintenance);

                    // 2. Génération du PDF
                    PdfService.genererRapportTache(desc.trim(), datePrevueDp.getValue().toString(), String.valueOf(cout), nomFichierPdf);

                    // --- AJOUT : ENVOI EMAIL DYNAMIQUE ---
                    try {
                        // 1. On récupère l'ID de l'agriculteur depuis la maintenance associée
                        int idAgri = selectedMaintenance.getIdAgriculteur();

                        // 2. On utilise le service pour chercher son email en BDD
                        // Note: Assure-toi d'avoir ajouté la méthode getEmailByAgriculteurId dans ServiceMaintenance
                        String emailAgriculteur = serviceMaintenance.getEmailByAgriculteurId(idAgri);

                        if (emailAgriculteur != null && !emailAgriculteur.isEmpty()) {
                            services.EmailService.envoyerEmailTache(
                                    emailAgriculteur, // <--- L'email vient maintenant de la BDD !
                                    nom.trim(),
                                    selectedMaintenance.getDescription(),
                                    datePrevueDp.getValue().toString(),
                                    String.valueOf(cout),
                                    nomFichierPdf
                            );
                            System.out.println("Email envoyé à l'agriculteur : " + emailAgriculteur);
                        } else {
                            System.out.println("Erreur : Aucun email trouvé pour l'agriculteur ID " + idAgri);
                        }
                    } catch (Exception e) {
                        System.out.println("Erreur lors de l'envoi de l'email : " + e.getMessage());
                    }

                    // --- AJOUT : ENVOI SMS DYNAMIQUE ---
                    // --- AJOUT : ENVOI SMS DYNAMIQUE ---
                    try {
                        // 1. On récupère le numéro (int) et on le convertit en String
                        // Note : On utilise userConnecte.getPhone() car c'est ce qui apparaît dans ton ProfileController
                        String telBrut = String.valueOf(userConnecte.getPhone());

                        // 2. On prépare le numéro au format international (ex: +216XXXXXXXX)
                        String numeroComplet = "+216" + telBrut;

                        String messageSms = "Bonjour " + userConnecte.getNom() + ",\n" +
                                "Une nouvelle tâche a été planifiée pour l'équipement : " + selectedMaintenance.getEquipement() + ".\n" +
                                "Date prévue : " + datePrevueDp.getValue().toString();

                        // 3. Appel au service Twilio
                        services.SmsService.envoyerSms(numeroComplet, messageSms);

                    } catch (Exception e) {
                        System.out.println("Erreur SMS : " + e.getMessage());
                    }

                    javafx.application.Platform.runLater(() -> {
                        try {
                            java.awt.Desktop.getDesktop().open(new java.io.File(nomFichierPdf));
                        } catch (Exception e) {
                            System.out.println("Erreur ouverture PDF : " + e.getMessage());
                        }

                        showAlert(Alert.AlertType.INFORMATION, "Succès", "Tâche enregistrée et notifications envoyées !");

                        try {
                            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/ShowMaintenanceDetails.fxml"));
                            Parent root = loader.load();
                            ShowMaintenanceDetailsController controller = loader.getController();
                            controller.setMaintenance(this.maintenanceAutomatique);
                            NavigationUtil.loadInContentArea(saveBtn, root);
                        } catch (Exception ex) { ex.printStackTrace(); }
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
            showAlert(Alert.AlertType.ERROR, "Erreur", "Le coût doit être un nombre entier.");
        }
    }

    // N'oublie pas d'ajouter cette méthode utilitaire dans ton contrôleur
    private boolean isNumericOnly(String str) {
        return str.matches("\\d+");
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