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
import services.NotificationService; // ✅ AJOUTÉ
import services.SuiviAnimalService;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

public class AddSuiviAnimalController {

    @FXML private ComboBox<Animal> comboAnimal;
    @FXML private TextField        tempTf;
    @FXML private TextField        poidsTf;
    @FXML private TextField        rythmeTf;
    @FXML private ComboBox<String> niveauCombo;
    @FXML private ComboBox<String> etatCombo;
    @FXML private TextArea         remarqueTf;

    private final SuiviAnimalService  suiviService  = new SuiviAnimalService();
    private final AnimalService       animalService = new AnimalService();
    private final NotificationService notifService  = NotificationService.getInstance(); // ✅ AJOUTÉ

    // ════════════════════════════════════════
    //  INITIALIZE
    // ════════════════════════════════════════
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

    // ════════════════════════════════════════
    //  SAVE SUIVI
    // ════════════════════════════════════════
    @FXML
    void saveSuivi() {

        // ── Validations ──
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
        int    rythme;

        try {
            temperature = Double.parseDouble(tempTf.getText().trim().replace(",", "."));
        } catch (NumberFormatException e) {
            showError("Température invalide ! Exemple : 38.5");
            return;
        }
        try {
            poids = Double.parseDouble(poidsTf.getText().trim().replace(",", "."));
        } catch (NumberFormatException e) {
            showError("Poids invalide ! Exemple : 450");
            return;
        }
        try {
            rythme = Integer.parseInt(rythmeTf.getText().trim());
        } catch (NumberFormatException e) {
            showError("Rythme Cardiaque invalide ! Exemple : 70");
            return;
        }

        // ── Sauvegarde ──
        try {
            Animal animal    = comboAnimal.getValue();  // ✅ récupéré ici
            String etatSante = etatCombo.getValue();    // ✅ récupéré ici

            SuiviAnimal s = new SuiviAnimal(
                    animal.getIdAnimal(),
                    Timestamp.valueOf(LocalDateTime.now()),
                    temperature,
                    poids,
                    rythme,
                    etatSante,
                    remarqueTf.getText(),
                    niveauCombo.getValue()
            );

            suiviService.create(s);

            // ════════════════════════════════════════
            //  ✅ NOTIFICATIONS AUTOMATIQUES
            // ════════════════════════════════════════

            // 1. Notification suivi ajouté (toujours)
            notifService.notifierSuiviAjoute(animal.getCodeAnimal(), etatSante);

            // 2. Notification selon état de santé
            if ("Critique".equals(etatSante)) {
                notifService.notifierEtatCritique(animal.getCodeAnimal(), animal.getEspece());
            } else if ("Malade".equals(etatSante)) {
                notifService.notifierAvertissement(animal.getCodeAnimal(),
                        animal.getEspece() + " MALADE — Surveillance médicale nécessaire");
            }

            // 3. Notification selon température anormale
            double[] normes = getNormes(animal.getEspece());
            if (temperature >= normes[1] + 1.5) {
                notifService.notifierTemperatureCritique(animal.getCodeAnimal(), temperature);
            } else if (temperature > normes[1]) {
                notifService.notifierAvertissement(animal.getCodeAnimal(),
                        "Légère fièvre détectée : " + temperature + "°C");
            } else if (temperature < normes[0] - 1.0) {
                notifService.notifierCritique(animal.getCodeAnimal(),
                        "HYPOTHERMIE : " + temperature + "°C ! Urgence vétérinaire !");
            }

            // 4. Notification selon rythme cardiaque anormal
            if (rythme > (int) normes[3] + 20) {
                notifService.notifierRythmeAnormal(animal.getCodeAnimal(), rythme);
            } else if (rythme < (int) normes[2] - 10) {
                notifService.notifierRythmeAnormal(animal.getCodeAnimal(), rythme);
            }

            // ════════════════════════════════════════

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

    // ════════════════════════════════════════
    //  NORMES PAR ESPÈCE
    // ════════════════════════════════════════
    private double[] getNormes(String espece) {
        if (espece == null) return new double[]{38.0, 39.5, 50, 100};
        return switch (espece.toLowerCase().trim()) {
            case "vache", "bovin"     -> new double[]{38.0, 39.5,  48,  84};
            case "cheval", "equin"    -> new double[]{37.5, 38.5,  28,  44};
            case "mouton", "ovin"     -> new double[]{38.5, 39.5,  60, 120};
            case "chèvre", "caprin"   -> new double[]{38.5, 39.5,  70,  80};
            case "porc", "porcin"     -> new double[]{38.0, 39.5,  60,  80};
            case "poulet", "volaille" -> new double[]{40.6, 41.7, 250, 300};
            case "lapin"              -> new double[]{38.5, 39.5, 130, 325};
            default                   -> new double[]{38.0, 39.5,  50, 100};
        };
    }

    // ════════════════════════════════════════
    //  GO BACK
    // ════════════════════════════════════════
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

    // ════════════════════════════════════════
    //  CLEAR + ERROR
    // ════════════════════════════════════════
    private void clearFields() {
        comboAnimal.setValue(null);
        tempTf.clear();
        poidsTf.clear();
        rythmeTf.clear();
        niveauCombo.setValue(null);
        etatCombo.setValue(null);
        remarqueTf.clear();
    }

    private void showError(String msg) {
        new Alert(Alert.AlertType.ERROR, msg).showAndWait();
    }
}