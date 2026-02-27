package Controller;

import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.layout.HBox;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import Model.Maintenance;
import services.ServiceMaintenance;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;
import javafx.event.ActionEvent;
import javafx.application.Platform;

public class NotificationController {
    @FXML private ListView<Maintenance> notifList;
    @FXML private TextField searchField;
    @FXML private ChoiceBox<String> priorityFilter;
    @FXML private ChoiceBox<String> dateSortPicker;

    private final ServiceMaintenance service = new ServiceMaintenance();
    private DashboardController parentController;

    public void setParentController(DashboardController parent) {
        this.parentController = parent;
    }

    @FXML
    public void initialize() {
        // Initialisation des filtres
        if (dateSortPicker != null) {
            dateSortPicker.getItems().setAll("Plus récent", "Plus ancien");
            dateSortPicker.setValue("Plus récent");
            dateSortPicker.valueProperty().addListener((obs, oldVal, newVal) -> filterList());
        }

        loadPendingData();
        setupCustomCells();

        searchField.textProperty().addListener((obs, oldVal, newVal) -> filterList());
        priorityFilter.valueProperty().addListener((obs, oldVal, newVal) -> filterList());
    }

    private void loadPendingData() {
        try {
            List<Maintenance> pending = service.afficher().stream()
                    .filter(m -> "en attente".equalsIgnoreCase(m.getStatut()))
                    .collect(Collectors.toList());
            notifList.getItems().setAll(pending);
            checkCriticalAlerts(pending);
        } catch (SQLException e) { e.printStackTrace(); }
    }

    // --- CORRECTION DU FILTRE ET TRI ---
    private void filterList() {
        try {
            String keyword = (searchField.getText() == null) ? "" : searchField.getText().toLowerCase().trim();
            String priority = (priorityFilter.getValue() != null) ? priorityFilter.getValue() : "Toutes les priorités";
            String sortOrder = (dateSortPicker.getValue() != null) ? dateSortPicker.getValue() : "Plus récent";

            List<Maintenance> filtered = service.afficher().stream()
                    .filter(m -> "en attente".equalsIgnoreCase(m.getStatut()))
                    .filter(m -> keyword.isEmpty() ||
                            m.getEquipement().toLowerCase().contains(keyword) ||
                            m.getType().toLowerCase().contains(keyword) ||
                            m.getDescription().toLowerCase().contains(keyword) ||
                            m.getLieu().toLowerCase().contains(keyword))
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

    private void setupCustomCells() {
        notifList.setCellFactory(param -> new ListCell<Maintenance>() {
            @Override
            protected void updateItem(Maintenance m, boolean empty) {
                super.updateItem(m, empty);

                if (empty || m == null) {
                    setGraphic(null);
                    setStyle("-fx-background-color: transparent;");
                } else {
                    // 1. Logique de calcul du retard
                    long joursAttente = java.time.temporal.ChronoUnit.DAYS.between(m.getDateDeclaration(), java.time.LocalDate.now());
                    boolean isCritical = m.getPriorite().equalsIgnoreCase("Urgente") && joursAttente >= 0;

                    // 2. Création des Labels (Style Dashboard)
                    Label equipLabel = new Label(m.getEquipement().toUpperCase());
                    equipLabel.setStyle("-fx-font-size: 17px; -fx-font-weight: bold; -fx-text-fill: #1e293b;");

                    Label descLabel = new Label(m.getDescription());
                    descLabel.setStyle("-fx-text-fill: #64748b; -fx-font-size: 13px;");

                    Label detailsLabel = new Label("📍 " + m.getLieu() + " | 🛠 " + m.getType());
                    detailsLabel.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 12px;");

                    VBox leftBox = new VBox(equipLabel, descLabel, detailsLabel);
                    leftBox.setSpacing(5);

                    // 3. Badges et Temps
                    Label timeLabel = new Label(calculateTimeAgo(m.getDateDeclaration()));
                    timeLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #b2bec3; -fx-font-style: italic;");

                    Label pLabel = new Label(m.getPriorite().toUpperCase());
                    pLabel.setStyle(getPriorityStyle(m.getPriorite()));

                    Label sLabel = new Label(m.getStatut().toUpperCase());
                    sLabel.setStyle("-fx-background-color:#fff3cd; -fx-text-fill:#856404; -fx-padding:5 10; -fx-background-radius:20; -fx-font-weight:bold; -fx-font-size:10px;");

                    HBox badges = new HBox(sLabel, pLabel);
                    badges.setSpacing(8);
                    badges.setAlignment(Pos.TOP_RIGHT);

                    VBox rightBox = new VBox(timeLabel, badges);
                    rightBox.setSpacing(10);
                    rightBox.setAlignment(Pos.TOP_RIGHT);

                    Region spacer = new Region();
                    HBox.setHgrow(spacer, Priority.ALWAYS);

                    Label alertIcon = new Label();
                    if (isCritical) {
                        alertIcon.setText("⚠ RETARD");
                        alertIcon.setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
                    }

                    HBox topRow = new HBox(leftBox, spacer, alertIcon, rightBox);
                    topRow.setAlignment(Pos.TOP_LEFT);

                    // 4. Boutons Accepter/Refuser (Position Centrale)
                    Button accBtn = new Button("Accepter");
                    accBtn.getStyleClass().add("action-button-accept");
                    accBtn.setOnAction(e -> handleAction(m, "en cours"));

                    Button refBtn = new Button("Refuser");
                    refBtn.getStyleClass().add("action-button-refuse");
                    refBtn.setOnAction(e -> handleAction(m, "refusee"));

                    HBox buttonRow = new HBox(accBtn, refBtn);
                    buttonRow.setSpacing(40);
                    buttonRow.setAlignment(Pos.CENTER);
                    buttonRow.setPadding(new javafx.geometry.Insets(15, 0, 0, 0));

                    // 5. Assemblage et Style de la Card
                    VBox cardLayout = new VBox(topRow, buttonRow);
                    cardLayout.setSpacing(10);

                    String baseStyle = "-fx-background-color: white; -fx-padding: 20; -fx-background-radius: 20; " +
                            "-fx-border-color: #f1f5f9; -fx-border-radius: 20; -fx-border-width: 2; " +
                            "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.05), 10, 0, 0, 4);";

                    if (isCritical) {
                        cardLayout.setStyle(baseStyle + "-fx-border-color: #e74c3c; -fx-background-color: #fff5f5;");
                    } else {
                        cardLayout.setStyle(baseStyle);
                        cardLayout.setOnMouseEntered(e -> cardLayout.setStyle(baseStyle + "-fx-background-color: #f8fafc; -fx-border-color: #cbd5e1;"));
                        cardLayout.setOnMouseExited(e -> cardLayout.setStyle(baseStyle));
                    }

                    setGraphic(cardLayout);
                    setStyle("-fx-background-color: transparent; -fx-padding: 10 20;");
                } // Fin du else (m != null)
            } // Fin de updateItem
        }); // Fin de setCellFactory
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

    private void handleAction(Maintenance m, String status) {
        try {
            m.setStatut(status);
            service.modifier(m);
            loadPendingData();
            if (parentController != null) parentController.refreshAll();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    @FXML
    void closeWindow(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/fxml/Dashboard.fxml"));
            ((javafx.scene.Node) event.getSource()).getScene().setRoot(root);
        } catch (Exception e) { e.printStackTrace(); }
    }

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

    public void checkCriticalAlerts(List<Maintenance> maliste) {
        if (maliste == null) return;
        long count = maliste.stream()
                .filter(m -> "Urgente".equalsIgnoreCase(m.getPriorite()))
                .filter(m -> m.getDateDeclaration() != null &&
                        java.time.temporal.ChronoUnit.DAYS.between(m.getDateDeclaration(), LocalDate.now()) >= 2)
                .count();
        if (count > 0) {
            Platform.runLater(() -> {
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("Alertes de Retard");
                alert.setHeaderText(count + " demande(s) urgente(s) critique(s) !");
                alert.setContentText("Ces demandes attendent depuis plus de 48h.");
                alert.show();
            });
        }
    }
}