package Controller;

import Model.Maintenance;
import Model.Tache;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
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
        // 1. Détecter le statut une seule fois pour tout le reste de la fonction
        boolean isResolu = "Resolu".equalsIgnoreCase(maintenance.getStatut());

        // 2. Création de la carte principale
        VBox card = new VBox();
        card.setStyle("-fx-background-color: white; -fx-padding: 15; -fx-background-radius: 20; -fx-spacing: 10; " +
                "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.08), 10, 0, 0, 5); " +
                "-fx-border-color: #f1f5f9; -fx-border-width: 2; -fx-border-radius: 20;");
        card.setMinWidth(400);
        card.setMaxWidth(800);
        card.setCursor(javafx.scene.Cursor.HAND);

        // 3. HEADER (Nom + Actions)
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

        // --- CONDITION : Si résolu, on masque les icônes d'action ---
        if (isResolu) {
            editIcon.setVisible(false);
            editIcon.setManaged(false);
            deleteIcon.setVisible(false);
            deleteIcon.setManaged(false);
        }

        header.getChildren().addAll(nomTache, spacer, editIcon, deleteIcon);

        // 4. LIGNE INFOS (Date + Budget)
        HBox infoRow = new HBox();
        infoRow.setSpacing(15);
        infoRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        Label dateTache = new Label("📅 " + t.getDate_prevue());
        dateTache.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748b; -fx-font-weight: bold;");
        Label coutTache = new Label(t.getCout_estimee() + " DT");
        coutTache.setStyle("-fx-background-color: #f0fdf4; -fx-text-fill: #2e7d32; -fx-padding: 2 8; -fx-background-radius: 8; -fx-font-weight: bold; -fx-font-size: 11px;");
        infoRow.getChildren().addAll(dateTache, coutTache);

        // 5. DESCRIPTION
        Label descLabel = new Label(t.getDesciption());
        descLabel.setWrapText(true);
        descLabel.setStyle("-fx-text-fill: #475569; -fx-font-size: 13px; -fx-padding: 10 5 5 5; -fx-border-color: #f1f5f9; -fx-border-width: 1 0 0 0;");
        descLabel.setVisible(false);
        descLabel.setManaged(false);

        // 6. VOTE BAR (Uniquement si Maintenance résolue)
        HBox voteBar = new HBox(15);
        if (isResolu) {
            voteBar.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);
            voteBar.setStyle("-fx-padding: 5 0 0 0;");
            Label labelEval = new Label("Évaluer :");
            labelEval.setStyle("-fx-text-fill: #64748b; -fx-font-size: 12px;");
            Label likeBtn = new Label("👍");
            Label dislikeBtn = new Label("👎");

            String styleNeutre = "-fx-cursor: hand; -fx-font-size: 18px; -fx-padding: 5; -fx-text-fill: #94a3b8;";
            String styleLikeActive = "-fx-background-color: #dcfce7; -fx-background-radius: 50; -fx-font-size: 18px; -fx-padding: 5; -fx-text-fill: #2e7d32; -fx-cursor: hand;";
            String styleDislikeActive = "-fx-background-color: #fee2e2; -fx-background-radius: 50; -fx-font-size: 18px; -fx-padding: 5; -fx-text-fill: #e11d48; -fx-cursor: hand;";

            likeBtn.setStyle(t.getEvaluation() == 1 ? styleLikeActive : styleNeutre);
            dislikeBtn.setStyle(t.getEvaluation() == -1 ? styleDislikeActive : styleNeutre);

            likeBtn.setOnMouseClicked(e -> {
                e.consume();
                try {
                    int nouveauVote = (t.getEvaluation() == 1) ? 0 : 1;
                    serviceTache.voterTache(t.getId_tache(), nouveauVote);
                    t.setEvaluation(nouveauVote);
                    likeBtn.setStyle(nouveauVote == 1 ? styleLikeActive : styleNeutre);
                    dislikeBtn.setStyle(styleNeutre);
                } catch (SQLException ex) { ex.printStackTrace(); }
            });

            dislikeBtn.setOnMouseClicked(e -> {
                e.consume();
                try {
                    int nouveauVote = (t.getEvaluation() == -1) ? 0 : -1;
                    serviceTache.voterTache(t.getId_tache(), nouveauVote);
                    t.setEvaluation(nouveauVote);
                    dislikeBtn.setStyle(nouveauVote == -1 ? styleDislikeActive : styleNeutre);
                    likeBtn.setStyle(styleNeutre);
                } catch (SQLException ex) { ex.printStackTrace(); }
            });
            voteBar.getChildren().addAll(labelEval, likeBtn, dislikeBtn);
        }

        // 7. ASSEMBLAGE
        card.getChildren().addAll(header, infoRow, descLabel);
        if (isResolu) card.getChildren().add(voteBar);

        // 8. EVENTS
        card.setOnMouseClicked(event -> {
            if (!(event.getTarget() instanceof Label && ((Label)event.getTarget()).getCursor() == javafx.scene.Cursor.HAND)) {
                boolean isVisible = descLabel.isVisible();
                descLabel.setVisible(!isVisible);
                descLabel.setManaged(!isVisible);
                if (!isVisible) card.setStyle(card.getStyle() + "-fx-border-color: transparent;");
                else card.setStyle(card.getStyle().replace("-fx-border-color: #2e7d32;", "-fx-border-color: #f1f5f9;"));
            }
        });

        editIcon.setOnMouseClicked(e -> {
            e.consume();
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/UpdateTache.fxml"));
                Parent root = loader.load();
                UpdateTacheController controller = loader.getController();
                controller.setTache(t);
                editIcon.getScene().setRoot(root);
            } catch (Exception ex) { ex.printStackTrace(); }
        });

        deleteIcon.setOnMouseClicked(e -> {
            e.consume();
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Suppression de tâche");
            alert.setHeaderText("Confirmation");
            alert.setContentText("Voulez-vous vraiment supprimer la tâche : " + t.getNomTache() + " ?");
            alert.showAndWait().ifPresent(response -> {
                if (response == ButtonType.OK) {
                    try {
                        serviceTache.supprimer(t.getId_tache());
                        loadTachesAssociees();
                    } catch (SQLException ex) { ex.printStackTrace(); }
                }
            });
        });

        card.setOnMouseEntered(e -> { if (!descLabel.isVisible()) card.setStyle(card.getStyle() + "-fx-background-color: #f8fafc;"); });
        card.setOnMouseExited(e -> { card.setStyle(card.getStyle().replace("-fx-background-color: #f8fafc;", "-fx-background-color: white;")); });

        return card;
    }

    private void showMaintenanceDetails() {
        if (maintenance != null) {
            // --- TITRE PRINCIPAL (Gras pour la hiérarchie) ---
            nomMaintenanceLabel.setText(maintenance.getNom_maintenance().toUpperCase());
            nomMaintenanceLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #1e293b;");

            // --- DONNÉES DE BASE (Poids normal pour le confort) ---
            String labelStyle = "-fx-font-weight: normal; -fx-text-fill: #475569; -fx-font-size: 14px;";

            typeLabel.setText(maintenance.getType());
            typeLabel.setStyle(labelStyle);

            dateLabel.setText(String.valueOf(maintenance.getDateDeclaration()));
            dateLabel.setStyle(labelStyle);

            descriptionLabel.setText(maintenance.getDescription());
            descriptionLabel.setStyle(labelStyle + "-fx-line-spacing: 5;"); // Un peu d'espace entre les lignes

            lieuLabel.setText(maintenance.getLieu());
            lieuLabel.setStyle(labelStyle);

            equipementLabel.setText(maintenance.getEquipement());
            equipementLabel.setStyle(labelStyle);

            // --- BADGES (On garde le gras car c'est du texte très court) ---
            statutLabel.setText(maintenance.getStatut().toUpperCase());
            statutLabel.setStyle(getStatusStyle(maintenance.getStatut()));

            prioriteLabel.setText(maintenance.getPriorite().toUpperCase());
            String p = maintenance.getPriorite().toLowerCase();
            String priorityBase = "-fx-padding: 5 12; -fx-background-radius: 20; -fx-font-size: 11px; -fx-font-weight: bold;";

            if (p.contains("haute") || p.contains("urgent")) {
                prioriteLabel.setStyle("-fx-background-color: #fef2f2; -fx-text-fill: #e11d48; " + priorityBase);
            } else {
                prioriteLabel.setStyle("-fx-background-color: #f0fdf4; -fx-text-fill: #2e7d32; " + priorityBase);
            }

            // Gestion de la visibilité des boutons
            String statutActuel = maintenance.getStatut();
            boolean afficherBoutons = "Accepter".equalsIgnoreCase(statutActuel) || "Planifier".equalsIgnoreCase(statutActuel);

            btnTerminer.setVisible(afficherBoutons);
            btnTerminer.setManaged(afficherBoutons);
            btnPlanifier.setVisible(afficherBoutons);
            btnPlanifier.setManaged(afficherBoutons);
        }
    }

    private String getStatusStyle(String statut) {
        if (statut == null) return "";
        statut = statut.toLowerCase();

        // On utilise exactement ta base : radius 20 et padding 5 10
        String base = "-fx-padding:5 10; -fx-background-radius:20; -fx-font-weight:bold; -fx-font-size:10px;";

        if (statut.contains("resolu")) {

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

    @FXML
    void handleTerminerIntervention() {
        try {
            // 1. Vérifier s'il y a des tâches associées
            // On récupère toutes les tâches et on filtre pour cette maintenance
            List<Tache> toutesLesTaches = serviceTache.afficher();
            boolean aDesTaches = toutesLesTaches.stream()
                    .anyMatch(t -> t.getId_maintenace() == maintenance.getId());

            // 2. Si aucune tâche n'est trouvée, on affiche une alerte et on arrête
            if (!aDesTaches) {
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("Action impossible");
                alert.setHeaderText("Aucune tâche planifiée");
                alert.setContentText("Vous ne pouvez pas terminer une intervention sans avoir ajouté au moins une tâche.");
                alert.showAndWait();
                return; // On sort de la fonction sans modifier le statut
            }

            // 3. Si on a des tâches, on procède normalement
            maintenance.setStatut("Resolu");
            serviceMaintenance.modifier(maintenance);
            showMaintenanceDetails();
            loadTachesAssociees();

        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Erreur", "Une erreur est survenue lors de la mise à jour.");
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