package Controller;

import java.io.IOException;
import java.util.stream.Collectors;
import javafx.geometry.Pos;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
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
                            (m.getNom_maintenance() != null && m.getNom_maintenance().toLowerCase().contains(keyword)) || // Recherche par titre ajoutée ici
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

                // 1. CONTENU GAUCHE
                Label equipLabel = new Label(m.getNom_maintenance().toUpperCase());
                equipLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #1e293b;");

                Label descLabel = new Label(m.getDescription());
                descLabel.setStyle("-fx-text-fill: #64748b; -fx-font-size: 13px;");
                descLabel.setWrapText(true);
                descLabel.setMaxWidth(400); // Limite la largeur pour éviter que le texte ne pousse tout

                Label detailsLabel = new Label("Lieu: " + m.getLieu());
                detailsLabel.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 12px;");

                // --- CORRECTION : PLUS FIN ET PLUS CLAIR ---
                Label btnDetails = new Label("Voir détails →");

// On utilise un vert plus éclatant (#10b981) et on retire le bold (font-weight: normal)
                String styleClair = "-fx-text-fill: #1e293b; -fx-font-weight: normal; -fx-font-size: 14px; -fx-cursor: hand; -fx-padding: 5 0; -fx-underline: false;";

// Au survol, on garde la même finesse mais on souligne
                String styleSurvol = "-fx-text-fill: #1e293b; -fx-font-weight: normal; -fx-font-size: 14px; -fx-cursor: hand; -fx-padding: 5 0; -fx-underline: true;";

                btnDetails.setStyle(styleClair);
                btnDetails.setOnMouseEntered(e -> btnDetails.setStyle(styleSurvol));
                btnDetails.setOnMouseExited(e -> btnDetails.setStyle(styleClair));

                VBox leftBox = new VBox(equipLabel, descLabel, detailsLabel, btnDetails);
                leftBox.setSpacing(8);

                // 2. CONTENU DROITE (Badges et Poubelle)
                Label statusLabel = new Label(m.getStatut().toUpperCase());
                statusLabel.setStyle(getStatusStyle(m.getStatut()));

                Label priorityLabel = new Label(m.getPriorite().toUpperCase());
                priorityLabel.setStyle(getPriorityStyle(m.getPriorite()));

                HBox badgesBox = new HBox(statusLabel, priorityLabel);
                badgesBox.setSpacing(8);
                badgesBox.setAlignment(Pos.TOP_RIGHT);

                Label deleteNode = new Label("🗑");
                deleteNode.setStyle("-fx-font-size: 20px; -fx-text-fill: #fca5a5; -fx-cursor: hand;");
                deleteNode.setOnMouseEntered(e -> deleteNode.setStyle("-fx-font-size: 20px; -fx-text-fill: #ef4444; -fx-cursor: hand;"));
                deleteNode.setOnMouseExited(e -> deleteNode.setStyle("-fx-font-size: 20px; -fx-text-fill: #fca5a5; -fx-cursor: hand;"));

                deleteNode.setOnMouseClicked(event -> {
                    event.consume(); // Empêche d'ouvrir les détails quand on clique sur supprimer
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
                rightBox.setSpacing(15);
                Region verticalSpacer = new Region();
                VBox.setVgrow(verticalSpacer, Priority.ALWAYS);
                rightBox.getChildren().addAll(badgesBox, verticalSpacer, deleteNode);

                Region horizontalSpacer = new Region();
                HBox.setHgrow(horizontalSpacer, Priority.ALWAYS);

                // 3. ASSEMBLAGE DE LA CARTE
                HBox card = new HBox(leftBox, horizontalSpacer, rightBox);
                card.setAlignment(Pos.CENTER_LEFT);

                String baseStyle = "-fx-background-color: white; -fx-padding: 20; -fx-background-radius: 18; " +
                        "-fx-border-color: #f1f5f9; -fx-border-radius: 18; -fx-border-width: 1.5; " +
                        "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.04), 10, 0, 0, 4);";
                card.setStyle(baseStyle);

                // Effet de survol sur toute la carte
                card.setOnMouseEntered(e -> card.setStyle(baseStyle + "-fx-background-color: #f8fafc; -fx-border-color: #cbd5e1;"));
                card.setOnMouseExited(e -> card.setStyle(baseStyle));

                // Clique sur toute la carte pour ouvrir les détails
                card.setOnMouseClicked(e -> {
                    if (m != null) {
                        openTacheWindow(m);
                    }
                });

                setGraphic(card);
                setStyle("-fx-background-color: transparent; -fx-padding: 8 0;");
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
        statut = statut.toLowerCase();

        // On utilise exactement ta base : radius 20 et padding 5 10
        String base = "-fx-padding:5 10; -fx-background-radius:20; -fx-font-weight:bold; -fx-font-size:10px;";

        if (statut.contains("resolu")) {
            // Même Vert que "faible"
            return "-fx-background-color:#c3e6cb; -fx-text-fill:#155724; " + base;
        }
        if (statut.contains("accepter")) {
            // Un Bleu doux (pour changer du vert/rouge)
            return "-fx-background-color:#e0f2fe; -fx-text-fill:#0369a1; " + base;
        }
        if (statut.contains("planifier")) {
            // Même Jaune que "normale"
            return "-fx-background-color:#ffeeba; -fx-text-fill:#856404; " + base;
        }
        if (statut.contains("refuse")) {
            // Même Rouge que "urgente"
            return "-fx-background-color:#f5c6cb; -fx-text-fill:#721c24; " + base;
        }
        // Gris par défaut (comme ton default)
        return "-fx-background-color:#e2e3e5; -fx-text-fill:#383d41; " + base;
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
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/MaintenanceDetail.fxml"));
            Parent root = loader.load();

            MaintenanceDetailController controller = loader.getController();
            controller.setMaintenance(m); // envoie la maintenance sélectionnée

            mainList.getScene().setRoot(root); // remplace la scène actuelle
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}