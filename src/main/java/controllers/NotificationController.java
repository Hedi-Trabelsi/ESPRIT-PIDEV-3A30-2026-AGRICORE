package controllers;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import models.Animal;
import models.SuiviAnimal;
import services.AnimalService;
import services.NotificationService;
import services.SuiviAnimalService;

import java.sql.SQLException;
import java.util.List;

public class NotificationController {

    @FXML private Label  lblStatut;
    @FXML private VBox   alertesBox;
    @FXML private Label  lblNbAlertes;
    @FXML private ComboBox<Animal> comboAnimal;
    @FXML private ComboBox<String> typeCombo;
    @FXML private TextField        messageTf;
    @FXML private Label            lblSupport;

    private final AnimalService      animalService  = new AnimalService();
    private final SuiviAnimalService suiviService   = new SuiviAnimalService();
    private final NotificationService notifService  = NotificationService.getInstance();

    // ════════════════════════════════════════
    //  INITIALIZE
    // ════════════════════════════════════════
    @FXML
    void initialize() {
        // Vérifier support
        if (notifService.isSupported()) {
            lblSupport.setText("✅ Notifications Desktop actives sur votre système");
            lblSupport.setStyle("-fx-text-fill:#2e7d32;-fx-font-weight:bold;");
        } else {
            lblSupport.setText("⚠️ Notifications non supportées — Mode console activé");
            lblSupport.setStyle("-fx-text-fill:#f57c00;-fx-font-weight:bold;");
        }

        // Charger animaux
        try {
            List<Animal> animaux = animalService.read();
            comboAnimal.setItems(FXCollections.observableArrayList(animaux));
            comboAnimal.setCellFactory(p -> new ListCell<>() {
                @Override protected void updateItem(Animal a, boolean empty) {
                    super.updateItem(a, empty);
                    setText(empty || a == null ? null
                            : a.getCodeAnimal() + " — " + a.getEspece());
                }
            });
            comboAnimal.setButtonCell(new ListCell<>() {
                @Override protected void updateItem(Animal a, boolean empty) {
                    super.updateItem(a, empty);
                    setText(empty || a == null ? null
                            : a.getCodeAnimal() + " — " + a.getEspece());
                }
            });
        } catch (SQLException e) { showAlert(e.getMessage()); }

        typeCombo.setItems(FXCollections.observableArrayList(
                "🚨 Urgence critique",
                "⚠️ Avertissement",
                "ℹ️ Information"
        ));
        typeCombo.setValue("⚠️ Avertissement");

        // Scanner automatiquement les anomalies au démarrage
        scannerAnomalies();
    }

    // ════════════════════════════════════════
    //  SCANNER TOUTES LES ANOMALIES
    // ════════════════════════════════════════
    @FXML
    void scannerAnomalies() {
        alertesBox.getChildren().clear();
        int nbAlertes = 0;

        try {
            List<Animal>      animaux = animalService.read();
            List<SuiviAnimal> suivis  = suiviService.read();

            for (Animal animal : animaux) {
                // Dernier suivi de l'animal
                java.util.Optional<SuiviAnimal> opt = suivis.stream()
                        .filter(s -> s.getIdAnimal() == animal.getIdAnimal())
                        .max(java.util.Comparator.comparing(SuiviAnimal::getDateSuivi));

                if (opt.isEmpty()) continue;
                SuiviAnimal s = opt.get();

                // Normes par espèce
                double[] normes = getNormes(animal.getEspece());
                double tempMin = normes[0], tempMax = normes[1];
                int rythmeMin = (int) normes[2], rythmeMax = (int) normes[3];

                // ── Vérifier température ──
                if (s.getTemperature() >= tempMax + 1.5) {
                    nbAlertes++;
                    ajouterAlerteUI(animal, "🔴 FIÈVRE SÉVÈRE : " + s.getTemperature() + "°C", "#c62828");
                    notifService.notifierTemperatureCritique(animal.getCodeAnimal(), s.getTemperature());
                } else if (s.getTemperature() > tempMax) {
                    nbAlertes++;
                    ajouterAlerteUI(animal, "🟠 Fièvre légère : " + s.getTemperature() + "°C", "#f57c00");
                    notifService.notifierAvertissement(animal.getCodeAnimal(),
                            "Légère fièvre : " + s.getTemperature() + "°C");
                } else if (s.getTemperature() < tempMin - 1.0) {
                    nbAlertes++;
                    ajouterAlerteUI(animal, "🔴 HYPOTHERMIE : " + s.getTemperature() + "°C", "#c62828");
                    notifService.notifierTemperatureCritique(animal.getCodeAnimal(), s.getTemperature());
                }

                // ── Vérifier rythme cardiaque ──
                if (s.getRythmeCardiaque() > rythmeMax + 20) {
                    nbAlertes++;
                    ajouterAlerteUI(animal, "🔴 TACHYCARDIE : " + s.getRythmeCardiaque() + " bpm", "#c62828");
                    notifService.notifierRythmeAnormal(animal.getCodeAnimal(), s.getRythmeCardiaque());
                } else if (s.getRythmeCardiaque() < rythmeMin - 10) {
                    nbAlertes++;
                    ajouterAlerteUI(animal, "🔴 BRADYCARDIE : " + s.getRythmeCardiaque() + " bpm", "#c62828");
                    notifService.notifierRythmeAnormal(animal.getCodeAnimal(), s.getRythmeCardiaque());
                }

                // ── Vérifier état santé ──
                if ("Critique".equals(s.getEtatSante())) {
                    nbAlertes++;
                    ajouterAlerteUI(animal, "🚨 ÉTAT CRITIQUE — Intervention urgente !", "#c62828");
                    notifService.notifierEtatCritique(animal.getCodeAnimal(), animal.getEspece());
                } else if ("Malade".equals(s.getEtatSante())) {
                    nbAlertes++;
                    ajouterAlerteUI(animal, "⚠️ Animal MALADE — Surveillance renforcée", "#f57c00");
                    notifService.notifierAvertissement(animal.getCodeAnimal(),
                            animal.getEspece() + " malade — Surveillance nécessaire");
                }
            }

            if (nbAlertes == 0) {
                Label ok = new Label("✅ Aucune anomalie détectée — Tous les animaux sont en bonne santé !");
                ok.setStyle("-fx-text-fill:#2e7d32;-fx-font-size:14px;-fx-font-weight:bold;");
                alertesBox.getChildren().add(ok);
                notifService.notifierInfo("✅ Scan terminé", "Tous les animaux sont en bonne santé !");
            }

            lblNbAlertes.setText(nbAlertes + " alerte(s) détectée(s)");
            lblNbAlertes.setStyle(nbAlertes > 0
                    ? "-fx-text-fill:#c62828;-fx-font-weight:bold;-fx-font-size:16px;"
                    : "-fx-text-fill:#2e7d32;-fx-font-weight:bold;-fx-font-size:16px;");

        } catch (SQLException e) {
            showAlert("Erreur scan : " + e.getMessage());
        }
    }

    // ════════════════════════════════════════
    //  AJOUTER ALERTE DANS L'UI
    // ════════════════════════════════════════
    private void ajouterAlerteUI(Animal animal, String message, String couleur) {
        VBox carte = new VBox(5);
        carte.setStyle("-fx-background-color:white;-fx-padding:12;"
                + "-fx-background-radius:10;-fx-border-color:" + couleur + ";"
                + "-fx-border-radius:10;-fx-border-width:2;"
                + "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.08),6,0,0,2);");

        Label nom = new Label("🐾 " + animal.getCodeAnimal()
                + " — " + animal.getEspece() + " (" + animal.getRace() + ")");
        nom.setStyle("-fx-font-weight:bold;-fx-font-size:13px;-fx-text-fill:#333;");

        Label msg = new Label(message);
        msg.setStyle("-fx-font-size:13px;-fx-font-weight:bold;-fx-text-fill:" + couleur + ";");

        // Bouton notification manuelle
        Button btnNotif = new Button("🔔 Notifier maintenant");
        btnNotif.setStyle("-fx-background-color:" + couleur + ";-fx-text-fill:white;"
                + "-fx-font-size:11px;-fx-background-radius:6;-fx-cursor:hand;-fx-padding:5 10;");
        btnNotif.setOnAction(e ->
            notifService.envoyer("🐾 " + animal.getCodeAnimal(), message,
                    couleur.equals("#c62828")
                            ? java.awt.TrayIcon.MessageType.ERROR
                            : java.awt.TrayIcon.MessageType.WARNING)
        );

        carte.getChildren().addAll(nom, msg, btnNotif);
        alertesBox.getChildren().add(carte);
    }

    // ════════════════════════════════════════
    //  ENVOYER NOTIFICATION MANUELLE
    // ════════════════════════════════════════
    @FXML
    void envoyerManuelle() {
        if (comboAnimal.getValue() == null) { showAlert("Choisissez un animal !"); return; }
        if (messageTf.getText().trim().isEmpty()) { showAlert("Entrez un message !"); return; }

        Animal animal = comboAnimal.getValue();
        String msg    = messageTf.getText().trim();
        String type   = typeCombo.getValue();

        if (type.contains("Urgence")) {
            notifService.notifierCritique(animal.getCodeAnimal(), msg);
        } else if (type.contains("Avertissement")) {
            notifService.notifierAvertissement(animal.getCodeAnimal(), msg);
        } else {
            notifService.notifierInfo("ℹ️ " + animal.getCodeAnimal(), msg);
        }

        new Alert(Alert.AlertType.INFORMATION,
                "✅ Notification envoyée pour " + animal.getCodeAnimal()).showAndWait();
        messageTf.clear();
    }

    // ════════════════════════════════════════
    //  TESTER LES NOTIFICATIONS
    // ════════════════════════════════════════
    @FXML
    void testerNotifications() {
        notifService.notifierInfo("🧪 Test Notifications",
                "Les notifications Desktop fonctionnent correctement !");
        notifService.notifierAvertissement("TEST",
                "Ceci est un test d'avertissement");

        new Alert(Alert.AlertType.INFORMATION,
                "✅ Notifications de test envoyées !\nVérifiez la barre des tâches.").showAndWait();
    }

    // ════════════════════════════════════════
    //  NORMES PAR ESPÈCE
    // ════════════════════════════════════════
    private double[] getNormes(String espece) {
        return switch (espece.toLowerCase().trim()) {
            case "vache", "bovin"         -> new double[]{38.0, 39.5, 48, 84};
            case "cheval", "equin"        -> new double[]{37.5, 38.5, 28, 44};
            case "mouton", "ovin"         -> new double[]{38.5, 39.5, 60, 120};
            case "chèvre", "caprin"       -> new double[]{38.5, 39.5, 70, 80};
            case "porc", "porcin"         -> new double[]{38.0, 39.5, 60, 80};
            case "poulet", "volaille"     -> new double[]{40.6, 41.7, 250, 300};
            case "lapin"                  -> new double[]{38.5, 39.5, 130, 325};
            default                       -> new double[]{38.0, 39.5, 50, 100};
        };
    }

    // ════════════════════════════════════════
    //  NAVIGATION
    // ════════════════════════════════════════
    @FXML
    void navigateBack() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/ShowAnimals.fxml"));
            Stage stage = (Stage) lblStatut.getScene().getWindow();
            stage.getScene().setRoot(root);
        } catch (Exception e) { showAlert(e.getMessage()); }
    }

    private void showAlert(String msg) {
        new Alert(Alert.AlertType.ERROR, msg).showAndWait();
    }
}
