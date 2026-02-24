package controllers;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.layout.HBox;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import models.Maintenance;
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
    @FXML private ChoiceBox<String> dateSortPicker; // Ajoute ceci dans ton fichier FXML aussi

    private final ServiceMaintenance service = new ServiceMaintenance();
    private DashboardController parentController;

    public void setParentController(DashboardController parent) {
        this.parentController = parent;
    }

    @FXML
    public void initialize() {

        if (dateSortPicker != null) {
            dateSortPicker.setValue("Plus récent"); // Par défaut
            dateSortPicker.valueProperty().addListener((obs, oldVal, newVal) -> filterList(searchField.getText()));
        }

        loadPendingData();
        setupCustomCells();

        searchField.textProperty().addListener((obs, oldVal, newVal) -> filterList(newVal));
        priorityFilter.valueProperty().addListener((obs, oldVal, newVal) -> filterList(searchField.getText()));
    }

    private void loadPendingData() {
        try {
            List<Maintenance> pending = service.afficher().stream()
                    .filter(m -> "en attente".equalsIgnoreCase(m.getStatut()))
                    .collect(Collectors.toList());

            notifList.getItems().setAll(pending);

            // --- AJOUT : Vérification de l'alerte à chaque chargement ---
            checkCriticalAlerts(pending);

        } catch (SQLException e) { e.printStackTrace(); }
    }

    private void filterList(String keyword) {
        try {
            final String priority = (priorityFilter.getValue() != null) ? priorityFilter.getValue() : "Toutes";
            final String sortOrder = (dateSortPicker != null && dateSortPicker.getValue() != null) ? dateSortPicker.getValue() : "Plus récent";

            List<Maintenance> filtered = service.afficher().stream()
                    .filter(m -> "en attente".equalsIgnoreCase(m.getStatut()))
                    .filter(m -> (keyword == null || keyword.isEmpty() ||
                            m.getType().toLowerCase().contains(keyword.toLowerCase()) ||
                            m.getEquipement().toLowerCase().contains(keyword.toLowerCase())))
                    .filter(m -> priority.equals("Toutes") || m.getPriorite().equalsIgnoreCase(priority))
                    // --- AJOUT DU TRI ---
                    .sorted((m1, m2) -> {
                        if (m1.getDateDeclaration() == null || m2.getDateDeclaration() == null) return 0;
                        if (sortOrder.equals("Plus récent")) {
                            return m2.getDateDeclaration().compareTo(m1.getDateDeclaration()); // Décroissant
                        } else {
                            return m1.getDateDeclaration().compareTo(m2.getDateDeclaration()); // Croissant
                        }
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
                } else {


                /*    long joursAttente = java.time.temporal.ChronoUnit.DAYS.between(m.getDateDeclaration(), LocalDate.now());
                    boolean isCritical = m.getPriorite().equalsIgnoreCase("Urgente") && joursAttente >= 2;*/
                    // Remplace le 2 par 0 pour que ça s'affiche ROUGE dès aujourd'hui
                    long joursAttente = java.time.temporal.ChronoUnit.DAYS.between(m.getDateDeclaration(), LocalDate.now());
                    boolean isCritical = m.getPriorite().equalsIgnoreCase("Urgente") && joursAttente >= 0;

                    Label typeLabel = new Label(m.getType());
                    typeLabel.getStyleClass().add("title-label");

                    VBox leftInfoBox = new VBox(
                            typeLabel,
                            new Label(m.getDescription()),
                            new Label("Equipement: " + m.getEquipement()),
                            new Label("lieu: " + m.getLieu())
                    );
                    leftInfoBox.setSpacing(5);

                    Label timeLabel = new Label(calculateTimeAgo(m.getDateDeclaration()));
                    timeLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #b2bec3; -fx-font-style: italic;");

                    Label pLabel = new Label(m.getPriorite());
                    pLabel.getStyleClass().addAll("priority", "priority-" + m.getPriorite().toLowerCase());

                    Label sLabel = new Label(m.getStatut());
                    sLabel.getStyleClass().addAll("status", "status-en-attente");

                    HBox badges = new HBox(sLabel, pLabel);
                    badges.setSpacing(8);
                    badges.setAlignment(Pos.CENTER_RIGHT);

                    VBox rightMetaBox = new VBox(timeLabel, badges);
                    rightMetaBox.setSpacing(5);
                    rightMetaBox.setAlignment(Pos.TOP_RIGHT);

                    // --- AJOUT : Icône Warning si critique ---
                    Label alertIcon = new Label();
                    if (isCritical) {
                        alertIcon.setText("\u26A0 RETARD");
                        alertIcon.setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
                    }

                    Region spacer = new Region();
                    HBox.setHgrow(spacer, Priority.ALWAYS);

                    HBox topRow = new HBox(leftInfoBox, spacer, alertIcon, rightMetaBox);
                    topRow.setAlignment(Pos.TOP_LEFT);
                    topRow.setSpacing(10);

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

                    VBox cardLayout = new VBox(topRow, buttonRow);
                    cardLayout.setSpacing(10);
                    cardLayout.getStyleClass().add("card");

                    // --- STYLE DYNAMIQUE ---
                    if (isCritical) {
                        cardLayout.setStyle("-fx-padding: 15; -fx-border-color: #e74c3c; -fx-border-width: 2; -fx-background-color: #fff5f5; -fx-border-radius: 10; -fx-background-radius: 10;");
                    } else {
                        cardLayout.setStyle("-fx-padding: 15;");
                    }

                    setGraphic(cardLayout);
                }
            }
        });
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
            Parent root = FXMLLoader.load(getClass().getResource("/interfaces/Dashboard.fxml"));
            ((javafx.scene.Node) event.getSource()).getScene().setRoot(root);
        } catch (Exception e) { e.printStackTrace(); }
    }

    private String calculateTimeAgo(LocalDate date) {
        if (date == null) return "Date inconnue";
        long days = java.time.temporal.ChronoUnit.DAYS.between(date, LocalDate.now());
        if (days == 0) return "Aujourd'hui";
        if (days == 1) return "Hier";
        return "Il y a " + days + " jours";
    }

    // --- LA NOUVELLE MÉTHODE D'ALERTE POPUP ---
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