package controllers;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.stage.Stage;

import models.Animal;
import models.SuiviAnimal;
import services.AnimalService;
import services.SuiviAnimalService;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

public class AddSuiviAnimalController {

    @FXML private ComboBox<Animal> comboAnimal;
    @FXML private TextField tempTf;
    @FXML private TextField poidsTf;
    @FXML private TextField rythmeTf;
    @FXML private ComboBox<String> niveauCombo;
    @FXML private ComboBox<String> etatCombo;
    @FXML private TextArea remarqueTf;

    private final SuiviAnimalService suiviService = new SuiviAnimalService();
    private final AnimalService animalService = new AnimalService();

    // ───────────────── INITIALIZE ─────────────────
    @FXML
    void initialize() {

        try {
            List<Animal> animaux = animalService.read();
            comboAnimal.setItems(FXCollections.observableArrayList(animaux));

            comboAnimal.setCellFactory(param -> new ListCell<>() {
                @Override
                protected void updateItem(Animal item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty || item == null ? null : item.getCodeAnimal());
                }
            });

            comboAnimal.setButtonCell(new ListCell<>() {
                @Override
                protected void updateItem(Animal item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty || item == null ? null : item.getCodeAnimal());
                }
            });

        } catch (Exception e) {
            showError(e.getMessage());
        }

        niveauCombo.setItems(FXCollections.observableArrayList("Faible", "Moyen", "Élevé"));
        etatCombo.setItems(FXCollections.observableArrayList("Bon", "Malade", "Critique"));
    }

    // ───────────────── SAVE ─────────────────
    @FXML
    void saveSuivi() {

        // Vérification champs obligatoires
        if (comboAnimal.getValue() == null) {
            showError("Veuillez choisir un animal !");
            return;
        }
        if (tempTf.getText().trim().isEmpty()) {
            showError("Le champ Température est obligatoire !");
            return;
        }
        if (poidsTf.getText().trim().isEmpty()) {
            showError("Le champ Poids est obligatoire !");
            return;
        }
        if (rythmeTf.getText().trim().isEmpty()) {
            showError("Le champ Rythme Cardiaque est obligatoire !");
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

        double temperature;
        double poids;
        int rythme;

        // Vérification valeurs numériques
        try {
            temperature = Double.parseDouble(tempTf.getText().trim().replace(",", "."));
        } catch (NumberFormatException e) {
            showError("Température invalide ! Exemple valide : 38.5");
            return;
        }

        try {
            poids = Double.parseDouble(poidsTf.getText().trim().replace(",", "."));
        } catch (NumberFormatException e) {
            showError("Poids invalide ! Exemple valide : 450");
            return;
        }

        try {
            rythme = Integer.parseInt(rythmeTf.getText().trim());
        } catch (NumberFormatException e) {
            showError("Rythme Cardiaque invalide ! Exemple valide : 70");
            return;
        }

        // Création objet et insertion en base
        try {

            SuiviAnimal s = new SuiviAnimal(
                    comboAnimal.getValue().getIdAnimal(),
                    Timestamp.valueOf(LocalDateTime.now()),
                    temperature,
                    poids,
                    rythme,
                    etatCombo.getValue(),
                    remarqueTf.getText(),
                    niveauCombo.getValue()
            );

            suiviService.create(s);

            new Alert(Alert.AlertType.INFORMATION,
                    "✅ Suivi ajouté avec succès !").showAndWait();

            clearFields();

            // Retour vers ShowSuiviAnimal
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/ShowSuiviAnimal.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) tempTf.getScene().getWindow();
            stage.getScene().setRoot(root);

        } catch (Exception e) {
            showError("Erreur lors de l'enregistrement :\n" + e.getMessage());
            e.printStackTrace();
        }
    }

    // ───────────────── GO BACK ─────────────────
    @FXML
    private void goBack() {

        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/ShowSuiviAnimal.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) tempTf.getScene().getWindow();
            stage.getScene().setRoot(root);

        } catch (Exception e) {
            showError(e.getMessage());
        }
    }

    // ───────────────── CLEAR ─────────────────
    private void clearFields() {
        comboAnimal.setValue(null);
        tempTf.clear();
        poidsTf.clear();
        rythmeTf.clear();
        niveauCombo.setValue(null);
        etatCombo.setValue(null);
        remarqueTf.clear();
    }

    // ───────────────── ERROR ALERT ─────────────────
    private void showError(String msg) {
        new Alert(Alert.AlertType.ERROR, msg).showAndWait();
    }
}