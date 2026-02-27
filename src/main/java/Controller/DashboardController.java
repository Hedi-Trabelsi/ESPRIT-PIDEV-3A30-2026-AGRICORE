package Controller;

import java.io.IOException;
import java.util.stream.Collectors;
import javafx.geometry.Pos;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import Model.Maintenance;
import services.ServiceMaintenance;
import java.sql.SQLException;
import java.util.List;
import javafx.scene.input.MouseEvent;
import services.ServiceTache;
import javafx.application.Platform;

public class DashboardController {

    @FXML private Label notificationBadge;
    @FXML private ChoiceBox<String> priorityFilter;
    @FXML private ListView<Maintenance> mainList;
    @FXML private TextField searchField;
    @FXML private ImageView statsIcon;

    private final ServiceMaintenance serviceMaintenance = new ServiceMaintenance();
    private final ServiceTache serviceTache = new ServiceTache();

    @FXML
    public void initialize() {
        refreshAll();
        searchField.textProperty().addListener((obs, oldVal, newVal) -> filterList());
        priorityFilter.valueProperty().addListener((obs, oldVal, newVal) -> filterList());
        setupCustomCells();
    }

    private void filterList() {
        try {
            String keyword = (searchField.getText() == null) ? "" : searchField.getText().toLowerCase().trim();
            String prioritySelected = priorityFilter.getValue();

            List<Maintenance> filtered = serviceMaintenance.afficher().stream()
                    .filter(m -> !"en attente".equalsIgnoreCase(m.getStatut()))
                    .filter(m -> keyword.isEmpty() ||
                            m.getType().toLowerCase().contains(keyword) ||
                            m.getDescription().toLowerCase().contains(keyword) ||
                            m.getLieu().toLowerCase().contains(keyword) ||
                            m.getEquipement().toLowerCase().contains(keyword))
                    .filter(m -> prioritySelected == null ||
                            prioritySelected.equals("Toutes les priorités") ||
                            m.getPriorite().equalsIgnoreCase(prioritySelected))
                    .collect(Collectors.toList());

            mainList.getItems().setAll(filtered);
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private void setupCustomCells() {
        mainList.setCellFactory(param -> new ListCell<Maintenance>() {
            @Override
            protected void updateItem(Maintenance m, boolean empty) {
                super.updateItem(m, empty);

                if (empty || m == null) {
                    setGraphic(null);
                    setStyle("-fx-background-color: transparent;");
                    return;
                }

                // 1. Calcul du coût
                double totalCout = 0;
                try {
                    totalCout = serviceTache.afficher().stream()
                            .filter(t -> t.getId_maintenace() == m.getId())
                            .mapToDouble(Model.Tache::getCout_estimee).sum();
                } catch (SQLException e) { e.printStackTrace(); }

                // 2. CONTENU GAUCHE (Inversion Titre -> Équipement)
                // L'équipement est maintenant le titre principal
                Label equipLabel = new Label(m.getEquipement().toUpperCase());
                equipLabel.setStyle("-fx-font-size: 17px; -fx-font-weight: bold; -fx-text-fill: #1e293b;");

                Label descLabel = new Label(m.getDescription());
                descLabel.setStyle("-fx-text-fill: #64748b; -fx-font-size: 13px;");

                // Le type de maintenance est maintenant ici avec le lieu
                Label detailsLabel = new Label("📍 " + m.getLieu() + " | 🛠 " + m.getType());
                detailsLabel.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 12px;");

                Label costLabel = new Label("💰 " + String.format("%.2f", totalCout) + " DT");
                costLabel.setStyle("-fx-text-fill: #2e7d32; -fx-font-weight: bold;");

                VBox leftBox = new VBox(equipLabel, descLabel, detailsLabel, costLabel);
                leftBox.setSpacing(5);

                // 3. CONTENU DROITE (Inchangé)
                Label statusLabel = new Label(m.getStatut().toUpperCase());
                statusLabel.setStyle(getStatusStyle(m.getStatut()));

                Label priorityLabel = new Label(m.getPriorite().toUpperCase());
                priorityLabel.setStyle(getPriorityStyle(m.getPriorite()));

                HBox badgesBox = new HBox(statusLabel, priorityLabel);
                badgesBox.setSpacing(8);
                badgesBox.setAlignment(Pos.TOP_RIGHT);

                Label deleteNode = new Label("🗑");
                deleteNode.setStyle("-fx-font-size: 22px; -fx-text-fill: #fca5a5; -fx-cursor: hand;");
                deleteNode.setOnMouseEntered(e -> deleteNode.setStyle("-fx-font-size: 22px; -fx-text-fill: #ef4444; -fx-cursor: hand;"));
                deleteNode.setOnMouseExited(e -> deleteNode.setStyle("-fx-font-size: 22px; -fx-text-fill: #fca5a5; -fx-cursor: hand;"));

                deleteNode.setOnMouseClicked(event -> {
                    event.consume();
                    Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Supprimer définitivement ?", ButtonType.YES, ButtonType.NO);
                    confirm.showAndWait().ifPresent(res -> {
                        if (res == ButtonType.YES) {
                            try {
                                serviceMaintenance.supprimer(m.getId());
                                refreshAll();
                            } catch (SQLException ex) { ex.printStackTrace(); }
                        }
                    });
                });

                VBox rightBox = new VBox();
                rightBox.setAlignment(Pos.TOP_RIGHT);
                rightBox.setSpacing(20);
                Region verticalSpacer = new Region();
                VBox.setVgrow(verticalSpacer, Priority.ALWAYS);
                rightBox.getChildren().addAll(badgesBox, verticalSpacer, deleteNode);

                Region horizontalSpacer = new Region();
                HBox.setHgrow(horizontalSpacer, Priority.ALWAYS);

                // 4. ASSEMBLAGE DE LA CARTE
                HBox card = new HBox(leftBox, horizontalSpacer, rightBox);
                card.setAlignment(Pos.CENTER_LEFT);

                String baseStyle = "-fx-background-color: white; -fx-padding: 20; -fx-background-radius: 20; " +
                        "-fx-border-color: #f1f5f9; -fx-border-radius: 20; -fx-border-width: 2; " +
                        "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.05), 10, 0, 0, 4);";
                card.setStyle(baseStyle);

                card.setOnMouseEntered(e -> {
                    card.setStyle(baseStyle + "-fx-background-color: #f8fafc; -fx-border-color: #cbd5e1;");
                });
                card.setOnMouseExited(e -> {
                    card.setStyle(baseStyle);
                });

                card.setOnMouseClicked(event -> {
                    if (event.getClickCount() == 2) openTacheWindow(m);
                });

                setGraphic(card);
                setStyle("-fx-background-color: transparent; -fx-padding: 10 0;");
            }
        });
    }

    // --- MÉTHODE DE NOTIFICATION DESKTOP ---
    private void notifierSurDesktop(String titre, String message) {
        if (!java.awt.SystemTray.isSupported()) {
            System.out.println("Le Desktop ne supporte pas les notifications.");
            return;
        }
        try {
            java.awt.SystemTray tray = java.awt.SystemTray.getSystemTray();
            java.awt.Image image = java.awt.Toolkit.getDefaultToolkit().createImage("");
            java.awt.TrayIcon trayIcon = new java.awt.TrayIcon(image, "GMAO");
            trayIcon.setImageAutoSize(true);
            tray.add(trayIcon);
            trayIcon.displayMessage(titre, message, java.awt.TrayIcon.MessageType.WARNING);

            new Thread(() -> {
                try { Thread.sleep(4000); } catch (InterruptedException e) {}
                tray.remove(trayIcon);
            }).start();
        } catch (java.awt.AWTException e) { e.printStackTrace(); }
    }

    public void refreshAll() { loadData(); updateNotificationCount(); }

    private void loadData() {
        try {
            List<Maintenance> list = serviceMaintenance.afficher().stream()
                    .filter(m -> !"en attente".equalsIgnoreCase(m.getStatut())).collect(Collectors.toList());
            mainList.getItems().setAll(list);
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private void updateNotificationCount() {
        try {
            List<Maintenance> enAttente = serviceMaintenance.afficher().stream()
                    .filter(m -> "en attente".equalsIgnoreCase(m.getStatut())).collect(Collectors.toList());

            int count = enAttente.size();
            boolean alerteUrgente = enAttente.stream().anyMatch(m -> "urgente".equalsIgnoreCase(m.getPriorite()));

            if (count > 0) {
                notificationBadge.setVisible(true);
                notificationBadge.setText(alerteUrgente ? "!" : String.valueOf(count));

                // --- GESTION DE L'ALERTE SUR LE DESKTOP ---
                if (alerteUrgente) {
                    Platform.runLater(() -> {
                        notifierSurDesktop("ALERTE : Demande Urgente",
                                "Attention ! Une ou plusieurs maintenances urgentes sont en attente.");
                    });
                }
            } else {
                notificationBadge.setVisible(false);
            }
        } catch (SQLException e) { e.printStackTrace(); }
    }

    // Styles (Status et Priorité)
    private String getStatusStyle(String statut) {
        if (statut == null) return "";
        switch (statut.toLowerCase()) {
            case "en cours": return "-fx-background-color:#d1ecf1; -fx-text-fill:#0c5460; -fx-padding:5 10; -fx-background-radius:20; -fx-font-weight:bold; -fx-font-size:10px;";
            case "en attente": return "-fx-background-color:#fff3cd; -fx-text-fill:#856404; -fx-padding:5 10; -fx-background-radius:20; -fx-font-weight:bold; -fx-font-size:10px;";
            case "refusee": return "-fx-background-color:#f8d7da; -fx-text-fill:red; -fx-padding:5 10; -fx-background-radius:20; -fx-font-weight:bold; -fx-font-size:10px;";
            default: return "-fx-background-color:#d4edda; -fx-text-fill:green; -fx-padding:5 10; -fx-background-radius:20; -fx-font-weight:bold; -fx-font-size:10px;";
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

    @FXML void showNotifications(MouseEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/NotificationView.fxml"));
            Parent root = loader.load();
            NotificationController controller = loader.getController();
            controller.setParentController(this);
            notificationBadge.getScene().setRoot(root);
        } catch (Exception e) { e.printStackTrace(); }
    }

    @FXML void openStatsWindow(MouseEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/fxml/StatsView.fxml"));
            statsIcon.getScene().setRoot(root);
        } catch (IOException e) { e.printStackTrace(); }
    }

    private void openTacheWindow(Maintenance m) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/ShowMaintenanceDetails.fxml"));
            Parent root = loader.load();
            ShowMaintenanceDetailsController controller = loader.getController();
            controller.setMaintenance(m);
            mainList.getScene().setRoot(root);
        } catch (Exception e) { e.printStackTrace(); }
    }
}