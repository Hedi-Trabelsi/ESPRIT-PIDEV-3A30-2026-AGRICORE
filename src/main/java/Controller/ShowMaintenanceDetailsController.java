package Controller;

import Model.Maintenance;
import Model.Tache;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import services.ServiceMaintenance;
import services.ServiceTache;

import java.sql.SQLException;
import java.util.List;

public class ShowMaintenanceDetailsController {

    private Maintenance maintenance;
    private final ServiceTache serviceTache = new ServiceTache();
    private final ServiceMaintenance serviceMaintenance = new ServiceMaintenance();
    @FXML private Button btnPlanifier;
    @FXML private Label nomMaintenanceLabel;
    @FXML private Button btnTerminer;
    @FXML private Label typeLabel, statutLabel, dateLabel, descriptionLabel, prioriteLabel, lieuLabel, equipementLabel;
    @FXML private VBox tachesContainer;
    @FXML private Label retourLabel;
    @FXML private Label totalPrixLabel;
    public void setMaintenance(Maintenance maintenance) {
        this.maintenance = maintenance;
        showMaintenanceDetails();
        loadTachesAssociees();
    }

    private void loadTachesAssociees() {
        if (maintenance == null || tachesContainer == null) return;

        try {
            tachesContainer.getChildren().clear();
            tachesContainer.setSpacing(15);

            List<Tache> toutesLesTaches = serviceTache.afficher();
            double totalBudget = 0; // Variable pour calculer le total

            for (Tache t : toutesLesTaches) {
                if (t.getId_maintenace() == maintenance.getId()) {
                    tachesContainer.getChildren().add(createMiniTacheCard(t));
                    totalBudget += t.getCout_estimee(); // On ajoute le coût de la tâche au total
                }
            }

            // --- MISE À JOUR DU PRIX TOTAL ---
            if (totalPrixLabel != null) {
                totalPrixLabel.setText(String.format("%.2f DT", totalBudget));
                // Petit style dynamique pour le prix
                totalPrixLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #2e7d32;");
            }

            if (tachesContainer.getChildren().isEmpty()) {
                Label info = new Label("Aucune tâche planifiée pour cette maintenance.");
                info.setStyle("-fx-text-fill: #94a3b8; -fx-font-style: italic;");
                tachesContainer.getChildren().add(info);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    @FXML
    void navigatePlanifier() { // On garde ton nom habituel
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/AddTache.fxml"));
            Parent root = loader.load();

            // On passe la maintenance actuelle au contrôleur suivant
            AddTacheController controller = loader.getController();
            controller.setMaintenanceSelectionnee(this.maintenance);

            // On utilise un bouton existant pour récupérer la scene
            btnTerminer.getScene().setRoot(root);
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Erreur", "Impossible d'ouvrir la planification.");
        }
    }
    private VBox createMiniTacheCard(Tache t) {
        VBox card = new VBox();
        card.setStyle("-fx-background-color: white; " +
                "-fx-padding: 15; " +
                "-fx-background-radius: 20; " +
                "-fx-spacing: 10; " +
                "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.08), 10, 0, 0, 5); " +
                "-fx-border-color: #f1f5f9; -fx-border-radius: 20;");

        card.setMinWidth(400);
        card.setMaxWidth(800);
        card.setCursor(javafx.scene.Cursor.HAND);

        // --- HEADER (Nom + Boutons) ---
        HBox header = new HBox();
        header.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        Label nomTache = new Label(t.getNomTache() != null ? t.getNomTache().toUpperCase() : "TÂCHE SANS NOM");
        nomTache.setStyle("-fx-font-size: 14px; -fx-font-weight: 800; -fx-text-fill: #1e293b;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label editIcon = new Label("✎");
        editIcon.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 18px; -fx-cursor: hand; -fx-padding: 0 10 0 0;");
        Label deleteIcon = new Label("🗑");
        deleteIcon.setStyle("-fx-text-fill: #fca5a5; -fx-font-size: 18px; -fx-cursor: hand;");

        header.getChildren().addAll(nomTache, spacer, editIcon, deleteIcon);

        // --- LIGNE INFOS (Date + Budget) ---
        HBox infoRow = new HBox();
        infoRow.setSpacing(15);
        infoRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        Label dateTache = new Label("📅 " + t.getDate_prevue());
        dateTache.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748b; -fx-font-weight: bold;");

        Label coutTache = new Label(t.getCout_estimee() + " DT");
        coutTache.setStyle("-fx-background-color: #f0fdf4; -fx-text-fill: #166534; " +
                "-fx-padding: 2 8; -fx-background-radius: 8; -fx-font-weight: bold; -fx-font-size: 11px;");

        infoRow.getChildren().addAll(dateTache, coutTache);

        // --- DESCRIPTION (Cachée par défaut) ---
        Label descLabel = new Label(t.getDesciption());
        descLabel.setWrapText(true);
        descLabel.setStyle("-fx-text-fill: #475569; -fx-font-size: 13px; -fx-padding: 10 5 5 5; -fx-border-color: #f1f5f9; -fx-border-width: 1 0 0 0;");

        // Astuce pour cacher et ne pas prendre de place
        descLabel.setVisible(false);
        descLabel.setManaged(false);

        // --- LOGIQUE DE CLIC POUR DÉPLIER ---
        card.setOnMouseClicked(event -> {
            // On vérifie qu'on ne clique pas sur les boutons d'édition/suppression
            if (!(event.getTarget() instanceof Label && ((Label)event.getTarget()).getCursor() == javafx.scene.Cursor.HAND)) {
                boolean isVisible = descLabel.isVisible();
                descLabel.setVisible(!isVisible);
                descLabel.setManaged(!isVisible);

                if (!isVisible) {
                    card.setStyle(card.getStyle() + "-fx-border-color: #7ca76f;"); // Bordure verte quand c'est ouvert
                } else {
                    card.setStyle(card.getStyle().replace("-fx-border-color: #7ca76f;", "-fx-border-color: #f1f5f9;"));
                }
            }
        });

        // --- LOGIQUE ACTIONS ---
        editIcon.setOnMouseClicked(e -> {
            e.consume(); // Empêche de déclencher le clic de la carte
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/UpdateTache.fxml"));
                Parent root = loader.load();
                UpdateTacheController controller = loader.getController();
                controller.setTache(t);
                editIcon.getScene().setRoot(root);
            } catch (Exception ex) { ex.printStackTrace(); }
        });

        deleteIcon.setOnMouseClicked(e -> {
            e.consume(); // Empêche de déclencher le clic de la carte
            try {
                serviceTache.supprimer(t.getId_tache());
                loadTachesAssociees();
            } catch (SQLException ex) { ex.printStackTrace(); }
        });

        card.getChildren().addAll(header, infoRow, descLabel);

        // --- EFFETS DE SURVOL ---
        card.setOnMouseEntered(e -> {
            if (!descLabel.isVisible()) card.setStyle(card.getStyle() + "-fx-background-color: #f8fafc;");
        });
        card.setOnMouseExited(e -> {
            card.setStyle(card.getStyle().replace("-fx-background-color: #f8fafc;", "-fx-background-color: white;"));
        });

        return card;
    }

    private void showMaintenanceDetails() {
        if (maintenance != null) {
            nomMaintenanceLabel.setText(maintenance.getNom_maintenance().toUpperCase());
            typeLabel.setText(maintenance.getType());
            dateLabel.setText(String.valueOf(maintenance.getDateDeclaration()));
            descriptionLabel.setText(maintenance.getDescription());
            lieuLabel.setText(maintenance.getLieu());
            equipementLabel.setText(maintenance.getEquipement());

            statutLabel.setText(maintenance.getStatut().toUpperCase());
            statutLabel.setStyle(getStatusStyle(maintenance.getStatut()));

            prioriteLabel.setText(maintenance.getPriorite());
            String p = maintenance.getPriorite().toLowerCase();
            if (p.contains("haute") || p.contains("urgent")) {
                prioriteLabel.setStyle("-fx-text-fill: #e11d48; -fx-font-weight: bold;");
            } else {
                prioriteLabel.setStyle("-fx-text-fill: #2e7d32; -fx-font-weight: bold;");
            }

            if ("Resolu".equalsIgnoreCase(maintenance.getStatut())) {
                btnTerminer.setVisible(false);
            } else {
                btnTerminer.setVisible(true);
            }
        }
    }

    private String getStatusStyle(String statut) {
        if (statut == null) return "";
        statut = statut.toLowerCase();
        if (statut.contains("resolu")) return "-fx-background-color:#d4edda; -fx-text-fill:green; -fx-padding:5 10; -fx-background-radius:10;";
        if (statut.contains("cours")) return "-fx-background-color:#d1ecf1; -fx-text-fill:#0c5460; -fx-padding:5 10; -fx-background-radius:10;";
        return "-fx-background-color:#f1f5f9; -fx-text-fill:#475569; -fx-padding:5 10; -fx-background-radius:10;";
    }

    @FXML
    void handleTerminerIntervention() {
        try {
            maintenance.setStatut("Resolu");
            serviceMaintenance.modifier(maintenance);
            showMaintenanceDetails();
            loadTachesAssociees(); // Recharger pour désactiver les boutons de modification
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @FXML
    void navigateRetour() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/ShowMaintenance.fxml"));
            Parent root = loader.load();
            retourLabel.getScene().setRoot(root);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setContentText(content);
        alert.showAndWait();
    }
}