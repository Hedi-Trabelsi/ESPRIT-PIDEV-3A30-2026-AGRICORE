package Controller;

import Model.Maintenance;
import Model.Tache;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.Node;
import javafx.scene.input.MouseEvent;
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

    public ShowMaintenanceDetailsController() throws SQLException {
    }

    @FXML
    void initialize() {
        // Early role check: hide buttons immediately for restricted roles
        int role = UserSession.getRole();
        System.out.println("[DEBUG] ShowMaintenanceDetails - UserSession role: " + role);

        if (role == 1) {
            // Agriculteur: hide Clôturer and Planifier
            btnTerminer.setVisible(false);
            btnTerminer.setManaged(false);
            btnPlanifier.setVisible(false);
            btnPlanifier.setManaged(false);
        }
    }

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
            double totalBudget = 0;

            for (Tache t : toutesLesTaches) {
                if (t.getId_maintenace() == maintenance.getId()) {
                    tachesContainer.getChildren().add(createMiniTacheCard(t));
                    totalBudget += t.getCout_estimee();
                }
            }

            if (totalPrixLabel != null) {
                totalPrixLabel.setText(String.format("%.2f DT", totalBudget));
                totalPrixLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #2e7d32;");
            }

            if (tachesContainer.getChildren().isEmpty()) {
                Label info = new Label("Aucune tâche planifiée pour cette maintenance.");
                info.setStyle("-fx-text-fill: #94a3b8; -fx-font-style: italic;");
                tachesContainer.getChildren().add(info);
            }

        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Erreur", "Impossible de charger les tâches: " + e.getMessage());
        }
    }

    @FXML
    void navigatePlanifier() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/AddTache.fxml"));
            Parent root = loader.load();

            AddTacheController controller = loader.getController();
            controller.setMaintenanceSelectionnee(this.maintenance);

            NavigationUtil.loadInContentArea(btnTerminer, root);
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Erreur", "Impossible d'ouvrir la planification.");
        }
    }

    private VBox createMiniTacheCard(Tache t) {
        boolean isResolu = "Resolu".equalsIgnoreCase(maintenance.getStatut());
        // Récupération du rôle pour les conditions
        int role = UserSession.getRole();

        VBox card = new VBox();
        card.setStyle("-fx-background-color: white; -fx-padding: 15; -fx-background-radius: 20; -fx-spacing: 10; " +
                "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.08), 10, 0, 0, 5); " +
                "-fx-border-color: #f1f5f9; -fx-border-width: 2; -fx-border-radius: 20;");
        card.setMinWidth(400);
        card.setMaxWidth(800);
        card.setCursor(javafx.scene.Cursor.HAND);

        // HEADER
        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);
        Label nomTache = new Label(t.getNomTache() != null ? t.getNomTache().toUpperCase() : "TÂCHE SANS NOM");
        nomTache.setStyle("-fx-font-size: 14px; -fx-font-weight: 800; -fx-text-fill: #1e293b;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label editIcon = new Label("✎");
        editIcon.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 18px; -fx-cursor: hand; -fx-padding: 0 10 0 0;");

        Label deleteIcon = new Label("🗑");
        deleteIcon.setStyle("-fx-text-fill: #fca5a5; -fx-font-size: 18px; -fx-cursor: hand;");

        // CONDITION : Masquer Modifier/Supprimer si c'est déjà résolu OU si l'utilisateur est un Agriculteur (Role 1)
        if (isResolu || role == 1) {
            editIcon.setVisible(false);
            editIcon.setManaged(false);
            deleteIcon.setVisible(false);
            deleteIcon.setManaged(false);
        }

        header.getChildren().addAll(nomTache, spacer, editIcon, deleteIcon);

        // INFO ROW
        HBox infoRow = new HBox();
        infoRow.setSpacing(15);
        infoRow.setAlignment(Pos.CENTER_LEFT);
        Label dateTache = new Label("📅 " + t.getDate_prevue());
        dateTache.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748b; -fx-font-weight: bold;");
        Label coutTache = new Label(String.format("%.2f DT", (double) t.getCout_estimee()));
        coutTache.setStyle("-fx-background-color: #f0fdf4; -fx-text-fill: #2e7d32; -fx-padding: 2 8; -fx-background-radius: 8; -fx-font-weight: bold; -fx-font-size: 11px;");
        infoRow.getChildren().addAll(dateTache, coutTache);

        // DESCRIPTION
        Label descLabel = new Label(t.getDesciption());
        descLabel.setWrapText(true);
        descLabel.setStyle("-fx-text-fill: #475569; -fx-font-size: 13px; -fx-padding: 10 5 5 5; -fx-border-color: #f1f5f9; -fx-border-width: 1 0 0 0;");
        descLabel.setVisible(false);
        descLabel.setManaged(false);

        // VOTE BAR
        HBox voteBar = new HBox(15);
        // CONDITION : Afficher l'évaluation seulement si c'est Résolu ET que l'utilisateur est l'Agriculteur (Role 1)
        if (isResolu && role == 1) {
            voteBar.setAlignment(Pos.CENTER_RIGHT);
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
                    loadTachesAssociees();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                    showAlert("Erreur", "Impossible d'enregistrer votre vote.");
                }
            });

            dislikeBtn.setOnMouseClicked(e -> {
                e.consume();
                try {
                    int nouveauVote = (t.getEvaluation() == -1) ? 0 : -1;
                    serviceTache.voterTache(t.getId_tache(), nouveauVote);
                    t.setEvaluation(nouveauVote);
                    dislikeBtn.setStyle(nouveauVote == -1 ? styleDislikeActive : styleNeutre);
                    likeBtn.setStyle(styleNeutre);
                    loadTachesAssociees();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                    showAlert("Erreur", "Impossible d'enregistrer votre vote.");
                }
            });
            voteBar.getChildren().addAll(labelEval, likeBtn, dislikeBtn);
        }

        // ASSEMBLAGE
        card.getChildren().addAll(header, infoRow, descLabel);
        // On ajoute la barre de vote uniquement si elle contient des éléments (Role 1 + Résolu)
        if (!voteBar.getChildren().isEmpty()) {
            card.getChildren().add(voteBar);
        }

        // EVENTS
        card.setOnMouseClicked(event -> {
            if (!(event.getTarget() instanceof Label && ((Label)event.getTarget()).getCursor() == javafx.scene.Cursor.HAND)) {
                boolean isVisible = descLabel.isVisible();
                descLabel.setVisible(!isVisible);
                descLabel.setManaged(!isVisible);
                if (isVisible) {
                    card.setStyle(card.getStyle().replace("-fx-border-color: #2e7d32;", "-fx-border-color: #f1f5f9;"));
                } else {
                    card.setStyle(card.getStyle() + "-fx-border-color: #2e7d32;");
                }
            }
        });

        editIcon.setOnMouseClicked(e -> {
            e.consume();
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/UpdateTache.fxml"));
                Parent root = loader.load();
                UpdateTacheController controller = loader.getController();
                controller.setTache(t);
                NavigationUtil.loadInContentArea(tachesContainer, root);
            } catch (Exception ex) {
                ex.printStackTrace();
                showAlert("Erreur", "Impossible d'ouvrir la modification.");
            }
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
                    } catch (SQLException ex) {
                        ex.printStackTrace();
                        showAlert("Erreur", "Impossible de supprimer la tâche.");
                    }
                }
            });
        });

        card.setOnMouseEntered(e -> {
            if (!descLabel.isVisible()) {
                card.setStyle(card.getStyle().replace("-fx-background-color: white;", "-fx-background-color: #f8fafc;"));
            }
        });
        card.setOnMouseExited(e -> {
            card.setStyle(card.getStyle().replace("-fx-background-color: #f8fafc;", "-fx-background-color: white;"));
        });

        return card;
    }

    private void showMaintenanceDetails() {
        if (maintenance != null) {
            nomMaintenanceLabel.setText(maintenance.getNom_maintenance().toUpperCase());
            nomMaintenanceLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #1e293b;");

            String labelStyle = "-fx-font-weight: normal; -fx-text-fill: #475569; -fx-font-size: 14px;";

            typeLabel.setText(maintenance.getType());
            typeLabel.setStyle(labelStyle);

            dateLabel.setText(String.valueOf(maintenance.getDateDeclaration()));
            dateLabel.setStyle(labelStyle);

            descriptionLabel.setText(maintenance.getDescription());
            descriptionLabel.setStyle(labelStyle + "-fx-line-spacing: 5;");

            lieuLabel.setText(maintenance.getLieu());
            lieuLabel.setStyle(labelStyle);

            equipementLabel.setText(maintenance.getEquipement());
            equipementLabel.setStyle(labelStyle);

            statutLabel.setText(maintenance.getStatut().toUpperCase());
            statutLabel.setStyle(getStatusStyle(maintenance.getStatut()));

            prioriteLabel.setText(maintenance.getPriorite().toUpperCase());
            prioriteLabel.setStyle(getPriorityStyle(maintenance.getPriorite()));

            String statutActuel = maintenance.getStatut();
            boolean afficherBoutons = "Accepter".equalsIgnoreCase(statutActuel) || "Planifier".equalsIgnoreCase(statutActuel);

            // Role-based: Agriculteur (role 1) cannot see Clôturer/Planifier buttons
            if (UserSession.getRole() == 1) {
                afficherBoutons = false;
            }

            btnTerminer.setVisible(afficherBoutons);
            btnTerminer.setManaged(afficherBoutons);
            btnPlanifier.setVisible(afficherBoutons);
            btnPlanifier.setManaged(afficherBoutons);
        }
    }

    private String getPriorityStyle(String priorite) {
        String base = "-fx-padding:5 12; -fx-background-radius:20; -fx-font-size:11px; -fx-font-weight:bold;";
        String p = priorite != null ? priorite.toLowerCase() : "";

        if (p.contains("urgente")) {
            return "-fx-background-color:#fef2f2; -fx-text-fill:#e11d48; " + base;
        } else if (p.contains("normale")) {
            return "-fx-background-color:#fff3e0; -fx-text-fill:#ed6c02; " + base;
        } else if (p.contains("faible")) {
            return "-fx-background-color:#f0fdf4; -fx-text-fill:#2e7d32; " + base;
        }
        return "-fx-background-color:#e2e3e5; -fx-text-fill:#383d41; " + base;
    }

    private String getStatusStyle(String statut) {
        if (statut == null) return "";
        statut = statut.toLowerCase();
        String base = "-fx-padding:5 10; -fx-background-radius:20; -fx-font-weight:bold; -fx-font-size:10px;";

        if (statut.contains("resolu")) {
            return "-fx-background-color:#c3e6cb; -fx-text-fill:#155724; " + base;
        }
        if (statut.contains("accepter") || statut.contains("accepté")) {
            return "-fx-background-color:#e0f2fe; -fx-text-fill:#0369a1; " + base;
        }
        if (statut.contains("planifier")) {
            return "-fx-background-color:#ffeeba; -fx-text-fill:#856404; " + base;
        }
        if (statut.contains("refuse") || statut.contains("refusé")) {
            return "-fx-background-color:#f5c6cb; -fx-text-fill:#721c24; " + base;
        }
        return "-fx-background-color:#e2e3e5; -fx-text-fill:#383d41; " + base;
    }

    @FXML
    void handleTerminerIntervention() {
        try {
            List<Tache> toutesLesTaches = serviceTache.afficher();
            boolean aDesTaches = toutesLesTaches.stream()
                    .anyMatch(t -> t.getId_maintenace() == maintenance.getId());

            if (!aDesTaches) {
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("Action impossible");
                alert.setHeaderText("Aucune tâche planifiée");
                alert.setContentText("Vous ne pouvez pas terminer une intervention sans avoir ajouté au moins une tâche.");
                alert.showAndWait();
                return;
            }

            maintenance.setStatut("Resolu");
            serviceMaintenance.modifier(maintenance);
            showMaintenanceDetails();
            loadTachesAssociees();

        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Erreur", "Une erreur est survenue lors de la mise à jour: " + e.getMessage());
        }
    }

    @FXML
    void navigateRetour(MouseEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/ShowMaintenance.fxml"));
            Parent root = loader.load();
            NavigationUtil.loadInContentArea((Node) event.getSource(), root);
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Erreur", "Impossible de retourner à la liste des maintenances.");
        }
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}