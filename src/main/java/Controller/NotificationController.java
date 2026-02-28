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

import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;
import javafx.event.ActionEvent;

public class NotificationController {
    @FXML private ListView<Maintenance> notifList;
    @FXML private TextField searchField;
    @FXML private ChoiceBox<String> priorityFilter;
    @FXML private ChoiceBox<String> dateSortPicker;

    private final ServiceMaintenance service;

    {
        try {
            service = new ServiceMaintenance();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private Object parentController; // Peut être DashboardController ou MaintenancePageController

    public void setParentController(Object parent) {
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

    private void filterList() {
        try {
            String keyword = (searchField.getText() == null) ? "" : searchField.getText().toLowerCase().trim();
            String priority = (priorityFilter.getValue() != null) ? priorityFilter.getValue() : "Toutes les priorités";
            String sortOrder = (dateSortPicker.getValue() != null) ? dateSortPicker.getValue() : "Plus récent";

            List<Maintenance> filtered = service.afficher().stream()
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

    private void setupCustomCells() {
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

                // --- 2. BOUTONS ---
                Button accBtn = new Button("Accepter");
                accBtn.setStyle("-fx-background-color: #ecfdf5; -fx-text-fill: #059669; -fx-font-weight: bold; -fx-background-radius: 8; -fx-cursor: hand; -fx-padding: 8 20;");
                accBtn.setOnAction(e -> handleAction(m, "accepter"));

                Button refBtn = new Button("Refuser");
                refBtn.setStyle("-fx-background-color: #fef2f2; -fx-text-fill: #dc2626; -fx-font-weight: bold; -fx-background-radius: 8; -fx-cursor: hand; -fx-padding: 8 20;");
                refBtn.setOnAction(e -> handleAction(m, "refusee"));

                HBox actionsBox = new HBox(accBtn, refBtn);
                actionsBox.setSpacing(15);
                actionsBox.setPadding(new javafx.geometry.Insets(10, 0, 0, 0));

                VBox leftBox = new VBox(nomLabel, descLabel, equipLabel, lieuLabel, actionsBox);
                leftBox.setSpacing(6);

                // --- 3. TEMPS ET PRIORITÉ DROITE ---
                Label timeAgoLabel = new Label(calculateTimeAgo(m.getDateDeclaration()));
                timeAgoLabel.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 11px; -fx-font-style: italic;");

                Label priorityLabel = new Label(m.getPriorite().toUpperCase());
                priorityLabel.setStyle(getPriorityStyle(m.getPriorite()));

                VBox rightBox = new VBox(timeAgoLabel, priorityLabel);
                rightBox.setSpacing(8);
                rightBox.setAlignment(Pos.TOP_RIGHT);

                Region horizontalSpacer = new Region();
                HBox.setHgrow(horizontalSpacer, Priority.ALWAYS);

                // --- 4. STYLE DYNAMIQUE (URGENT = ROUGE) ---
                boolean isUrgent = "urgente".equalsIgnoreCase(m.getPriorite());

                // Si urgent : fond très légèrement rouge et bordure rouge
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

                // Effets au survol adaptés à l'état urgent
                card.setOnMouseEntered(e -> card.setStyle(baseStyle + String.format("-fx-background-color: %s; -fx-border-color: %s;", hoverColor, hoverBorder)));
                card.setOnMouseExited(e -> card.setStyle(baseStyle));

                card.setOnMouseClicked(e -> {
                    if (e.getTarget() instanceof Button) return;
                    openTacheWindow(m);
                });

                setGraphic(card);
                setStyle("-fx-background-color: transparent; -fx-padding: 10 0;");
            }
        });
    }

    private void openTacheWindow(Maintenance m) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/ShowMaintenanceDetails.fxml"));
            Parent root = loader.load();

            ShowMaintenanceDetailsController controller = loader.getController();
            controller.setMaintenance(m);

            NavigationUtil.loadInContentArea(notifList, root);
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

    private void handleAction(Maintenance m, String status) {
        try {
            m.setStatut(status);
            service.modifier(m);
            loadPendingData();

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

    @FXML
    void closeWindow(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/fxml/Dashboard.fxml"));
            NavigationUtil.loadInContentArea((javafx.scene.Node) event.getSource(), root);
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
    }
}