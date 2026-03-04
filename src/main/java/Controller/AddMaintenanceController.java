package Controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;

import Model.Maintenance;
import services.ServiceMaintenance;
import java.sql.SQLException;
import java.time.LocalDate;


public class AddMaintenanceController {
    ServiceMaintenance ms = new ServiceMaintenance();

    @FXML  private ChoiceBox<String> type;
    @FXML  private ChoiceBox<String> priorite;
    @FXML  private TextField dateDeclarationTf;
    @FXML private TextArea descriptionTf;
    @FXML private TextField lieuTf;
    @FXML private TextField equipementTf;
    @FXML private javafx.scene.control.Label typeStar;
    @FXML private javafx.scene.control.Label lieuStar;
    @FXML private javafx.scene.control.Label equipementStar;
    @FXML private javafx.scene.control.Label prioriteStar;
    @FXML private javafx.scene.control.Label descriptionStar;
    @FXML private TextField nomMaintenanceTf; // Nouveau
    @FXML private Label nomMaintenanceStar;
    private final ServiceMaintenance serviceMaintenance = new ServiceMaintenance();
    @FXML
    private ComboBox<Maintenance> maintenanceCb;
    @FXML
    private javafx.scene.control.Button Save;

    @FXML
    private Label lieuError;

    @FXML
    private Label equipementError;

    public AddMaintenanceController() throws SQLException {
    }

    @FXML  void saveMaintenance(ActionEvent event) {
        try {
            String nom = nomMaintenanceTf.getText();
            String desc = descriptionTf.getText();

            // 1. Validation du Titre (Nom)
            if (nom == null || nom.trim().isEmpty()) {
                showAlert(Alert.AlertType.WARNING, "Validation", "Le titre ne peut pas être vide.");
                return;
            }
            if (isNumericOnly(nom.trim())) {
                showAlert(Alert.AlertType.WARNING, "Validation", "Le titre ne peut pas contenir uniquement des chiffres.");
                return;
            }
            // 2. Validation de la Description
            if (desc == null || desc.trim().isEmpty()) {
                showAlert(Alert.AlertType.WARNING, "Validation", "La description ne peut pas être vide.");
                return;
            }
            if (isNumericOnly(desc.trim())) {
                showAlert(Alert.AlertType.WARNING, "Validation", "La description ne peut pas contenir uniquement des chiffres.");
                return;
            }
            if (nomMaintenanceTf.getText() == null || nomMaintenanceTf.getText().trim().isEmpty()) {
                showAlert(Alert.AlertType.WARNING, "Validation", "Veuillez entrer un titre  pour la maintenance");
                return;
            }
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
                showAlert(Alert.AlertType.WARNING, "Validation", "L'equipement doit contenir au moins 3 lettres et pas uniquement des chiffres");
                return;
            }

            // AU LIEU DE : int idUserConnecte = UserSession.getUserId(); (QUI FAIT L'ERREUR)

// UTILISE ÇA :
            Model.Utilisateur userConnecte = UserSession.getCurrentUser();
            if (userConnecte != null) {
                int idUserConnecte = userConnecte.getId();

                // Ensuite tu crées ta maintenance normalement
                Maintenance maintenance = new Maintenance(
                        nomMaintenanceTf.getText(),
                        type.getValue(),
                        LocalDate.parse(dateDeclarationTf.getText()),
                        descriptionTf.getText(),
                        "En attente",
                        idUserConnecte, // <--- C'est maintenant le bon ID !
                        priorite.getValue(),
                        lieuTf.getText(),
                        equipementTf.getText()
                );

                ms.ajouter(maintenance);
            } else {
                showAlert(Alert.AlertType.ERROR, "Erreur", "Session introuvable");
            }

            // Message succes
            showAlert(Alert.AlertType.INFORMATION, "Succes", "Maintenance enregistree avec succes");

            // Retour a la liste apres 1s
            javafx.animation.PauseTransition pause = new javafx.animation.PauseTransition(javafx.util.Duration.seconds(1));
            pause.setOnFinished(e -> {
                try {
                    FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/ShowMaintenance.fxml"));
                    javafx.scene.Parent root = loader.load();
                    NavigationUtil.loadInContentArea((javafx.scene.Node) event.getSource(), root);
                } catch (Exception ex) {
                    showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de retourner a la liste: " + ex.getMessage());
                }
            });
            pause.play();

        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", e.getMessage());
        }
    }


    @FXML
    void cancel(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/fxml/ShowMaintenance.fxml"));
            NavigationUtil.loadInContentArea((javafx.scene.Node) event.getSource(), root);
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Erreur de navigation", e.getMessage());
        }
    }

    private boolean isNumericOnly(String text) {
        return text != null && text.matches("\\d+");
    }
    @FXML
    void initialize() {
        dateDeclarationTf.setText(LocalDate.now().toString());
        dateDeclarationTf.setEditable(false);
        type.getItems().addAll("Preventive", "Corrective", "Predictive");
        priorite.getItems().addAll("Faible", "Normale", "Urgente");
        LocalDate today = LocalDate.now();
        dateDeclarationTf.setText(today.toString());
        // Validation dynamique TextField
        descriptionTf.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null || newVal.trim().isEmpty()) {
                descriptionStar.setStyle("-fx-text-fill: red;");
            } else {
                descriptionStar.setStyle("-fx-text-fill: green;");
            }
        });

        lieuTf.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null || newVal.trim().isEmpty()) {
                lieuStar.setStyle("-fx-text-fill: red;");
            } else {
                lieuStar.setStyle("-fx-text-fill: green;");
            }
        });

        equipementTf.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null || newVal.trim().isEmpty()) {
                equipementStar.setStyle("-fx-text-fill: red;");
            } else {
                equipementStar.setStyle("-fx-text-fill: green;");
            }
        });

// Validation dynamique ChoiceBox
        type.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null) {
                typeStar.setStyle("-fx-text-fill: red;");
            } else {
                typeStar.setStyle("-fx-text-fill: green;");
            }
        });

        priorite.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null) {
                prioriteStar.setStyle("-fx-text-fill: red;");
            } else {
                prioriteStar.setStyle("-fx-text-fill: green;");
            }
        });
        nomMaintenanceTf.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null || newVal.trim().isEmpty()) {
                nomMaintenanceStar.setStyle("-fx-text-fill: red;");
            } else {
                nomMaintenanceStar.setStyle("-fx-text-fill: green;");
            }
        });

    }


    // Methode utilitaire pour valider texte (au moins 3 lettres et pas que des chiffres)
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
            Parent root = new FXMLLoader(getClass().getResource("/fxml/ShowMaintenance.fxml")).load();
            NavigationUtil.loadInContentArea(Save, root);
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Erreur de navigation", e.getMessage());
        }
    }
}