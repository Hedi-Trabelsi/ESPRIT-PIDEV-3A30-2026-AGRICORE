package Controller;

import javafx.animation.PauseTransition;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.util.Duration;
import Model.Tache;
import Model.Maintenance;
import services.ServiceTache;
import services.ServiceMaintenance;

import java.sql.SQLException;
import java.time.LocalDate;

public class UpdateTacheController {

    private final ServiceTache serviceTache = new ServiceTache();
    private final ServiceMaintenance serviceMaintenance = new ServiceMaintenance();
    private Tache tache;

    @FXML private TextField nomTacheTf;
    @FXML private DatePicker datePrevueDp;
    @FXML private TextArea descriptionTa;
    @FXML private TextField coutTf;
    @FXML private Button saveBtn;

    // Rectangle de rappel Maintenance
    @FXML private Label maintenanceInfoLabel;
    @FXML private Label maintenanceDetailsLabel;
    @FXML private Label maintenanceDateLabel;

    public void setTache(Tache tache) {
        this.tache = tache;
        populateFields();
    }

    private void populateFields() {
        if (tache == null) return;

        // Remplissage des champs de la tâche
        nomTacheTf.setText(tache.getNomTache());
        datePrevueDp.setValue(LocalDate.parse(tache.getDate_prevue()));
        descriptionTa.setText(tache.getDesciption());
        coutTf.setText(String.valueOf(tache.getCout_estimee()));

        // Remplissage du rectangle de rappel (Maintenance)
        try {
            Maintenance m = serviceMaintenance.getMaintenanceById(tache.getId_maintenace());
            if (m != null) {
                maintenanceInfoLabel.setText(m.getNom_maintenance().toUpperCase());
                maintenanceDateLabel.setText("📅 " + m.getDateDeclaration().toString());
                maintenanceDetailsLabel.setText(m.getLieu());
            }
        } catch (SQLException e) {
            maintenanceInfoLabel.setText("Erreur de chargement");
        }
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

            // 4. Conversion du coût
            int cout = Integer.parseInt(coutStr);

            // --- Si tout est valide, on met à jour l'objet ---
            tache.setNomTache(nom.trim());
            tache.setDate_prevue(datePrevueDp.getValue().toString());
            tache.setDesciption(desc.trim());
            tache.setCout_estimee(cout);

            // Appel au service
            serviceTache.modifier(tache);

            saveBtn.setDisable(true);
            saveBtn.setText(" Mise à jour...");

            showAlert(Alert.AlertType.INFORMATION, "Succès", "Tâche mise à jour !");

            PauseTransition pause = new PauseTransition(Duration.seconds(1));
            pause.setOnFinished(e -> returnToList());
            pause.play();

        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", e.getMessage());
        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Le coût doit être un nombre entier.");
        }
    }

    // Méthode utilitaire à ajouter dans ton contrôleur si elle n'y est pas déjà
    private boolean isNumericOnly(String str) {
        return str.matches("\\d+");
    }

    @FXML
    void cancelAction(MouseEvent event) {
        returnToList();
    }

    private void returnToList() {
        try {
            // 1. Charger le FXML
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/ShowMaintenanceDetails.fxml"));
            Parent root = loader.load();

            // 2. Récupérer le contrôleur de la page de détails
            // Assure-toi que le nom de la classe est bien ShowMaintenanceDetailsController
            ShowMaintenanceDetailsController controller = loader.getController();

            // 3. Récupérer l'objet Maintenance complet via le service
            Maintenance m = serviceMaintenance.getMaintenanceById(tache.getId_maintenace());

            // 4. Envoyer la maintenance au contrôleur pour qu'il affiche les bonnes infos
            if (m != null) {
                controller.setMaintenance(m);
            }

            // 5. Changer la scène
            saveBtn.getScene().setRoot(root);

        } catch (Exception ex) {
            ex.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur de navigation", "Impossible de charger les détails.");
        }
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(message);
        alert.showAndWait();
    }
}