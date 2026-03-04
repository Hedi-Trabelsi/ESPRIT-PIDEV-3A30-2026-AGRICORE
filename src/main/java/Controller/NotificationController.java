package Controller;

import Model.Animal;
import Model.Maintenance;
import Model.SuiviAnimal;
import Model.Utilisateur;
import services.AnimalService;
import services.NotificationService;
import services.ServiceMaintenance;
import services.SuiviAnimalService;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.event.ActionEvent;

import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class NotificationController {

    // ==================== MAINTENANCE NOTIFICATIONS (Your part) ====================
    @FXML private ListView<Maintenance> notifList;
    @FXML private TextField searchField;
    @FXML private ChoiceBox<String> priorityFilter;
    @FXML private ChoiceBox<String> dateSortPicker;

    private final ServiceMaintenance maintenanceService;
    private Object parentController; // Peut être DashboardController ou MaintenancePageController

    // ==================== ANIMAL HEALTH NOTIFICATIONS (Friend's part) ====================
    @FXML private Label lblStatut;
    @FXML private VBox alertesBox;
    @FXML private Label lblNbAlertes;
    @FXML private ComboBox<Animal> comboAnimal;
    @FXML private ComboBox<String> typeCombo;
    @FXML private TextField messageTf;
    @FXML private Label lblSupport;
    @FXML private TabPane notificationTabPane;
    @FXML private Tab maintenanceTab;
    @FXML private Tab animalTab;

    private final AnimalService animalService = new AnimalService();
    private final SuiviAnimalService suiviService = new SuiviAnimalService();
    private final NotificationService notifService = NotificationService.getInstance();

    private Utilisateur loggedInUser;

    // ==================== CONSTRUCTOR ====================
    public NotificationController() throws SQLException {
        this.maintenanceService = new ServiceMaintenance();
    }

    // ==================== INITIALIZATION ====================
    @FXML
    public void initialize() {
        // Initialize Maintenance Tab
        initMaintenanceTab();

        // Initialize Animal Health Tab
        initAnimalHealthTab();
    }

    private void initMaintenanceTab() {
        if (dateSortPicker != null) {
            dateSortPicker.getItems().setAll("Plus récent", "Plus ancien");
            dateSortPicker.setValue("Plus récent");
            dateSortPicker.valueProperty().addListener((obs, oldVal, newVal) -> filterMaintenanceList());
        }

        loadPendingMaintenanceData();
        setupMaintenanceCustomCells();

        if (searchField != null) {
            searchField.textProperty().addListener((obs, oldVal, newVal) -> filterMaintenanceList());
        }

        if (priorityFilter != null) {
            priorityFilter.valueProperty().addListener((obs, oldVal, newVal) -> filterMaintenanceList());
        }
    }

    private void initAnimalHealthTab() {
        if (lblSupport == null) return;

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

    // ==================== SETTERS ====================
    public void setParentController(Object parent) {
        this.parentController = parent;
    }

    public void setLoggedInUser(Utilisateur user) {
        this.loggedInUser = user;
    }

    // ==================== MAINTENANCE METHODS ====================
    private void loadPendingMaintenanceData() {
        try {
            List<Maintenance> pending = maintenanceService.afficher().stream()
                    .filter(m -> "en attente".equalsIgnoreCase(m.getStatut()))
                    .collect(Collectors.toList());
            notifList.getItems().setAll(pending);
            checkCriticalMaintenanceAlerts(pending);
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private void filterMaintenanceList() {
        try {
            String keyword = (searchField.getText() == null) ? "" : searchField.getText().toLowerCase().trim();
            String priority = (priorityFilter.getValue() != null) ? priorityFilter.getValue() : "Toutes les priorités";
            String sortOrder = (dateSortPicker.getValue() != null) ? dateSortPicker.getValue() : "Plus récent";

            List<Maintenance> filtered = maintenanceService.afficher().stream()
                    .filter(m -> "en attente".equalsIgnoreCase(m.getStatut()))
                    .filter(m -> keyword.isEmpty() ||
                            (m.getNom_maintenance() != null && m.getNom_maintenance().toLowerCase().contains(keyword)) ||
                            (m.getEquipement() != null && m.getEquipement().toLowerCase().contains(keyword)) ||
                            (m.getType() != null && m.getType().toLowerCase().contains(keyword)) ||
                            (m.getDescription() != null && m.getDescription().toLowerCase().contains(keyword)) ||
                            (m.getLieu() != null && m.getLieu().toLowerCase().contains(keyword)))
                    .filter(m -> priority.equals("Toutes les priorités") || priority.equals("Toutes") || m.getPriorite().equalsIgnoreCase(priority))
                    .sorted((m1, m2) -> {
                        if (m1.getDateDeclaration() == null || m2.getDateDeclaration() == null) return 0;
                        return sortOrder.equals("Plus récent") ? m2.getDateDeclaration().compareTo(m1.getDateDeclaration())
                                : m1.getDateDeclaration().compareTo(m2.getDateDeclaration());
                    })
                    .collect(Collectors.toList());

            notifList.getItems().setAll(filtered);
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private void setupMaintenanceCustomCells() {
        notifList.setCellFactory(param -> new ListCell<Maintenance>() {
            @Override
            protected void updateItem(Maintenance m, boolean empty) {
                super.updateItem(m, empty);

                if (empty || m == null) {
                    setGraphic(null);
                    setStyle("-fx-background-color: transparent;");
                    return;
                }

                Label nomLabel = new Label(m.getNom_maintenance().toUpperCase());
                nomLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #1e293b;");

                Label descLabel = new Label(m.getDescription());
                descLabel.setStyle("-fx-text-fill: #64748b; -fx-font-size: 13px;");
                descLabel.setWrapText(true);
                descLabel.setMaxWidth(500);

                Label equipLabel = new Label("• Équipement : " + m.getEquipement());
                equipLabel.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 12px;");

                Label lieuLabel = new Label("• Lieu : " + m.getLieu());
                lieuLabel.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 12px;");

                // BOUTONS
                Button accBtn = new Button("Accepter");
                accBtn.setStyle("-fx-background-color: #ecfdf5; -fx-text-fill: #059669; -fx-font-weight: bold; -fx-background-radius: 8; -fx-cursor: hand; -fx-padding: 8 20;");
                accBtn.setOnAction(e -> handleMaintenanceAction(m, "accepter"));

                Button refBtn = new Button("Refuser");
                refBtn.setStyle("-fx-background-color: #fef2f2; -fx-text-fill: #dc2626; -fx-font-weight: bold; -fx-background-radius: 8; -fx-cursor: hand; -fx-padding: 8 20;");
                refBtn.setOnAction(e -> handleMaintenanceAction(m, "refusee"));

                HBox actionsBox = new HBox(accBtn, refBtn);
                actionsBox.setSpacing(15);
                actionsBox.setPadding(new Insets(10, 0, 0, 0));

                VBox leftBox = new VBox(nomLabel, descLabel, equipLabel, lieuLabel, actionsBox);
                leftBox.setSpacing(6);

                // TEMPS ET PRIORITÉ DROITE
                Label timeAgoLabel = new Label(calculateTimeAgo(m.getDateDeclaration()));
                timeAgoLabel.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 11px; -fx-font-style: italic;");

                Label priorityLabel = new Label(m.getPriorite().toUpperCase());
                priorityLabel.setStyle(getPriorityStyle(m.getPriorite()));

                VBox rightBox = new VBox(timeAgoLabel, priorityLabel);
                rightBox.setSpacing(8);
                rightBox.setAlignment(Pos.TOP_RIGHT);

                Region horizontalSpacer = new Region();
                HBox.setHgrow(horizontalSpacer, Priority.ALWAYS);

                // STYLE DYNAMIQUE (URGENT = ROUGE)
                boolean isUrgent = "urgente".equalsIgnoreCase(m.getPriorite());

                String backgroundColor = isUrgent ? "#fff1f2" : "white";
                String borderColor = isUrgent ? "#fecaca" : "#f1f5f9";
                String hoverColor = isUrgent ? "#ffe4e6" : "#f8fafc";
                String hoverBorder = isUrgent ? "#f87171" : "#cbd5e1";

                String baseStyle = String.format(
                        "-fx-background-color: %s; -fx-padding: 20; -fx-background-radius: 18; " +
                                "-fx-border-color: %s; -fx-border-radius: 18; -fx-border-width: 1.5; " +
                                "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.04), 10, 0, 0, 4);",
                        backgroundColor, borderColor
                );

                HBox card = new HBox(leftBox, horizontalSpacer, rightBox);
                card.setAlignment(Pos.TOP_LEFT);
                card.setStyle(baseStyle);

                card.setOnMouseEntered(e -> card.setStyle(baseStyle + String.format("-fx-background-color: %s; -fx-border-color: %s;", hoverColor, hoverBorder)));
                card.setOnMouseExited(e -> card.setStyle(baseStyle));

                card.setOnMouseClicked(e -> {
                    if (e.getTarget() instanceof Button) return;
                    openMaintenanceDetailsWindow(m);
                });

                setGraphic(card);
                setStyle("-fx-background-color: transparent; -fx-padding: 10 0;");
            }
        });
    }

    private void openMaintenanceDetailsWindow(Maintenance m) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/ShowMaintenanceDetails.fxml"));
            Parent root = loader.load();

            ShowMaintenanceDetailsController controller = loader.getController();
            controller.setMaintenance(m);

            // Open in new window or in content area
            Stage stage = new Stage();
            stage.setTitle("Détails Maintenance - " + m.getNom_maintenance());
            stage.setScene(new Scene(root));
            stage.show();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String getPriorityStyle(String priorite) {
        String base = "-fx-padding:5 10; -fx-background-radius:20; -fx-font-weight:bold; -fx-font-size:10px;";
        if (priorite == null) return "-fx-background-color:#b0b0b0; -fx-text-fill:white; " + base;
        switch (priorite.toLowerCase()) {
            case "urgente": return "-fx-background-color:#f5c6cb; -fx-text-fill:#721c24; " + base;
            case "normale": return "-fx-background-color:#ffeeba; -fx-text-fill:#856404; " + base;
            case "faible": return "-fx-background-color:#c3e6cb; -fx-text-fill:#155724; " + base;
            default: return "-fx-background-color:#e2e3e5; -fx-text-fill:#383d41; " + base;
        }
    }

    private void handleMaintenanceAction(Maintenance m, String status) {
        try {
            m.setStatut(status);
            maintenanceService.modifier(m);
            loadPendingMaintenanceData();

            // Rafraîchir le parent selon son type
            if (parentController instanceof DashboardController) {
                ((DashboardController) parentController).refreshAll();
            } else if (parentController instanceof MaintenancePageController) {
                ((MaintenancePageController) parentController).refreshAll();
            } else if (parentController != null) {
                // Try to call refresh method via reflection if available
                try {
                    parentController.getClass().getMethod("refreshAll").invoke(parentController);
                } catch (Exception e) {
                    System.out.println("Parent controller doesn't have refreshAll method");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void checkCriticalMaintenanceAlerts(List<Maintenance> maliste) {
        if (maliste == null) return;
        long count = maliste.stream()
                .filter(m -> "Urgente".equalsIgnoreCase(m.getPriorite()))
                .filter(m -> m.getDateDeclaration() != null &&
                        java.time.temporal.ChronoUnit.DAYS.between(m.getDateDeclaration(), LocalDate.now()) >= 2)
                .count();
    }

    // ==================== ANIMAL HEALTH METHODS ====================
    @FXML
    void scannerAnomalies() {
        alertesBox.getChildren().clear();
        int nbAlertes = 0;

        try {
            List<Animal>      animaux = animalService.read();
            List<SuiviAnimal> suivis  = suiviService.read();

            for (Animal animal : animaux) {
                // Dernier suivi de l'animal
                Optional<SuiviAnimal> opt = suivis.stream()
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

    @FXML
    void testerNotifications() {
        notifService.notifierInfo("🧪 Test Notifications",
                "Les notifications Desktop fonctionnent correctement !");
        notifService.notifierAvertissement("TEST",
                "Ceci est un test d'avertissement");

        new Alert(Alert.AlertType.INFORMATION,
                "✅ Notifications de test envoyées !\nVérifiez la barre des tâches.").showAndWait();
    }

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

    // ==================== NAVIGATION ====================
    @FXML
    void closeWindow(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/fxml/Dashboard.fxml"));
            Stage stage = (Stage) ((javafx.scene.Node) event.getSource()).getScene().getWindow();
            stage.getScene().setRoot(root);
        } catch (Exception e) { e.printStackTrace(); }
    }

    @FXML
    void navigateBack() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/fxml/ShowAnimals.fxml"));
            Stage stage = (Stage) lblStatut.getScene().getWindow();
            stage.getScene().setRoot(root);
        } catch (Exception e) { showAlert(e.getMessage()); }
    }

    // ==================== UTILITY METHODS ====================
    private String calculateTimeAgo(LocalDate date) {
        if (date == null) return "Date inconnue";
        long days = java.time.temporal.ChronoUnit.DAYS.between(date, LocalDate.now());
        if (days == 0) return "Aujourd'hui";
        if (days == 1) return "Hier";
        if (days < 7) return "Il y a " + days + " jours";
        if (days < 30) return "Il y a " + (days / 7) + " semaines";
        long months = days / 30;
        return (months < 12) ? "Il y a " + months + " mois" : "Il y a " + (days / 365) + " ans";
    }

    private void showAlert(String msg) {
        new Alert(Alert.AlertType.ERROR, msg).showAndWait();
    }
}