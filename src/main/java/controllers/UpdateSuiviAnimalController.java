package controllers;

import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import models.SuiviAnimal;
import services.SuiviAnimalService;

public class UpdateSuiviAnimalController {

    @FXML private TextField tempTf;
    @FXML private TextField poidsTf;
    @FXML private TextField rythmeTf;
    @FXML private ComboBox<String> niveauCombo;
    @FXML private ComboBox<String> etatCombo;
    @FXML private TextArea remarqueTf;

    private SuiviAnimal suivi;
    private final SuiviAnimalService service = new SuiviAnimalService();

    @FXML
    void initialize() {
        // ✅ Peupler les ComboBox
        niveauCombo.setItems(FXCollections.observableArrayList("Faible", "Moyen", "Élevé"));
        etatCombo.setItems(FXCollections.observableArrayList("Bon", "Malade", "Critique"));
    }

    public void setSuivi(SuiviAnimal s) {
        this.suivi = s;
        tempTf.setText(String.valueOf(s.getTemperature()));
        poidsTf.setText(String.valueOf(s.getPoids()));
        rythmeTf.setText(String.valueOf(s.getRythmeCardiaque()));
        niveauCombo.setValue(s.getNiveauActivite());
        etatCombo.setValue(s.getEtatSante());
        remarqueTf.setText(s.getRemarque());
    }

    @FXML
    void updateSuivi(ActionEvent event) {

        // ── Vérifications ──
        if (tempTf.getText().trim().isEmpty()) {
            showError("Température obligatoire !");
            return;
        }
        if (poidsTf.getText().trim().isEmpty()) {
            showError("Poids obligatoire !");
            return;
        }
        if (rythmeTf.getText().trim().isEmpty()) {
            showError("Rythme Cardiaque obligatoire !");
            return;
        }
        if (niveauCombo.getValue() == null) {
            showError("Veuillez choisir un niveau d'activité !");
            return;
        }
        if (etatCombo.getValue() == null) {
            showError("Veuillez choisir un état de santé !");
            return;
        }

        try {
            suivi.setTemperature(Double.parseDouble(tempTf.getText().trim().replace(",", ".")));
            suivi.setPoids(Double.parseDouble(poidsTf.getText().trim().replace(",", ".")));
            suivi.setRythmeCardiaque(Integer.parseInt(rythmeTf.getText().trim()));
            suivi.setNiveauActivite(niveauCombo.getValue());
            suivi.setEtatSante(etatCombo.getValue());
            suivi.setRemarque(remarqueTf.getText());

            service.update(suivi);

            new Alert(Alert.AlertType.INFORMATION, "✅ Suivi modifié avec succès !").showAndWait();
            navigateShowSuiviAnimal();

        } catch (NumberFormatException e) {
            showError("Valeur numérique invalide !\nTempérature ex: 38.5 | Poids ex: 450 | Rythme ex: 70");
        } catch (Exception e) {
            showError("Erreur lors de la modification :\n" + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    void navigateShowSuiviAnimal() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ShowSuiviAnimal.fxml"));
            Parent root = loader.load();

            // ✅ Retour dans le MÊME stage — remplace juste le root
            Stage stage = (Stage) tempTf.getScene().getWindow();
            stage.getScene().setRoot(root);

        } catch (Exception e) {
            showError("Erreur de navigation :\n" + e.getMessage());
        }
    }

    private void showError(String msg) {
        new Alert(Alert.AlertType.ERROR, msg).showAndWait();
    }
}