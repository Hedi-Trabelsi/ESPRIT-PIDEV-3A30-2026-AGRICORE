package Controller;

import Model.Maintenance;
import Model.Tache;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.scene.input.MouseEvent;
import services.ServiceTache;

import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;
import java.io.IOException;

public class MaintenanceDetailController {

    @FXML private Label lblNomMaintenance; // AJOUTÉ : Nom de la maintenance
    @FXML private Label lblEquipement, lblDescription, lblType, lblLieu, lblTotalCout;
    @FXML private Label lblStatut, lblPriorite;
    @FXML private VBox tachesContainer;
    @FXML private Label lblScoreGlobal, lblEtatPerformance, lblTotalLikes, lblTotalNeutres, lblTotalDislikes;

    private Maintenance maintenance;
    private final ServiceTache serviceTache = new ServiceTache();

    private void calculerEvolution(List<Tache> taches) {
        int likes = 0, dislikes = 0, neutres = 0, scoreTotal = 0;

        for (Tache t : taches) {
            int eval = t.getEvaluation();
            scoreTotal += eval;
            if (eval == 1) likes++;
            else if (eval == -1) dislikes++;
            else neutres++;
        }

        lblTotalLikes.setText(String.valueOf(likes));
        lblTotalDislikes.setText(String.valueOf(dislikes));
        lblTotalNeutres.setText(String.valueOf(neutres));
        lblScoreGlobal.setText("Score Global : " + (scoreTotal > 0 ? "+" : "") + scoreTotal);

        String baseEval = "-fx-padding: 4 12; -fx-background-radius: 20; -fx-font-size: 11px; -fx-font-weight: normal;";
        if (scoreTotal > 0) {
            lblEtatPerformance.setText("ÉVOLUTION POSITIVE");
            lblEtatPerformance.setStyle("-fx-background-color: #ecfdf5; -fx-text-fill: #10b981; " + baseEval);
        } else if (scoreTotal < 0) {
            lblEtatPerformance.setText("ÉVOLUTION NÉGATIVE");
            lblEtatPerformance.setStyle("-fx-background-color: #fef2f2; -fx-text-fill: #f87171; " + baseEval);
        } else {
            lblEtatPerformance.setText("STABLE");
            lblEtatPerformance.setStyle("-fx-background-color: #f8fafc; -fx-text-fill: #94a3b8; " + baseEval);
        }
    }

    public void setMaintenance(Maintenance m) {
        this.maintenance = m;
        loadData();
    }

    private void loadData() {
        if (maintenance == null) return;

        // --- NOM DE LA MAINTENANCE (Titre doux) ---
        if (lblNomMaintenance != null) {
            lblNomMaintenance.setText(maintenance.getNom_maintenance().toUpperCase());
            lblNomMaintenance.setStyle("-fx-font-size: 18px; -fx-font-weight: normal; -fx-text-fill: #1e293b;");
        }

        // --- STYLE DE BASE POUR LES DONNÉES ---
        String normalStyle = "-fx-font-weight: normal; -fx-text-fill: #475569; -fx-font-size: 14px;";

        // --- ÉQUIPEMENT ---
        lblEquipement.setText(maintenance.getEquipement().toUpperCase());
        lblEquipement.setStyle(normalStyle);

        // --- STATUT ---
        lblStatut.setText(maintenance.getStatut().toUpperCase());
        lblStatut.setStyle(normalStyle + "-fx-text-fill: #64748b;");

        // --- AUTRES DONNÉES ---
        lblDescription.setText(maintenance.getDescription());
        lblDescription.setStyle(normalStyle);

        lblType.setText(maintenance.getType());
        lblType.setStyle(normalStyle);

        lblLieu.setText( maintenance.getLieu());
        lblLieu.setStyle(normalStyle);

        lblPriorite.setText(maintenance.getPriorite().toUpperCase());
        lblPriorite.setStyle(normalStyle);

        try {
            tachesContainer.getChildren().clear();
            List<Tache> allTaches = serviceTache.afficher();
            List<Tache> filteredTaches = allTaches.stream()
                    .filter(t -> t.getId_maintenace() == maintenance.getId())
                    .collect(Collectors.toList());

            calculerEvolution(filteredTaches);

            double totalBudget = 0;
            for (Tache t : filteredTaches) {
                tachesContainer.getChildren().add(createMiniTacheCard(t));
                totalBudget += t.getCout_estimee();
            }

            lblTotalCout.setText(String.format("%.2f DT", totalBudget));
            lblTotalCout.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #1e293b;");

        } catch (SQLException e) { e.printStackTrace(); }
    }

    // --- FONCTIONS DE COULEURS CONSERVÉES ---
    private String getStatusStyle(String statut) {
        if (statut == null) return "";
        statut = statut.toLowerCase();
        if (statut.contains("resolu")) {
            return "-fx-background-color:#dcfce7; -fx-text-fill:#166534; -fx-padding:5 10; -fx-background-radius:10; -fx-font-weight: bold;";
        }
        if (statut.contains("accepter")) {
            return "-fx-background-color:#e0f2fe; -fx-text-fill:#0369a1; -fx-padding:5 10; -fx-background-radius:10; -fx-font-weight: bold;";
        }
        return "-fx-background-color:#f1f5f9; -fx-text-fill:#475569; -fx-padding:5 10; -fx-background-radius:10;";
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

    private VBox createMiniTacheCard(Tache t) {
        // 1. Création de la carte avec le style premium (Ombres, Radius, Bordures)
        VBox card = new VBox();
        card.setStyle("-fx-background-color: white; -fx-padding: 15; -fx-background-radius: 20; -fx-spacing: 8; " +
                "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.08), 10, 0, 0, 5); " +
                "-fx-border-color: #f1f5f9; -fx-border-width: 2; -fx-border-radius: 20;");
        card.setMinWidth(400);
        card.setMaxWidth(800);
        card.setCursor(javafx.scene.Cursor.HAND);

        // 2. HEADER (Nom de la tâche uniquement)
        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);
        Label nomTache = new Label(t.getNomTache() != null ? t.getNomTache().toUpperCase() : "TÂCHE SANS NOM");
        nomTache.setStyle("-fx-font-size: 14px; -fx-font-weight: 800; -fx-text-fill: #1e293b;");
        header.getChildren().add(nomTache);

        // 3. LIGNE INFOS (Date + Budget style badge)
        HBox infoRow = new HBox();
        infoRow.setSpacing(15);
        infoRow.setAlignment(Pos.CENTER_LEFT);

        // Affichage de la date
        Label dateTache = new Label("📅 " + (t.getDate_prevue() != null ? t.getDate_prevue() : "N/A"));
        dateTache.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748b; -fx-font-weight: bold;");

        // Conversion sécurisée du coût pour éviter le crash %f
        double coutVal = 0.0;
        try {
            String s = String.valueOf(t.getCout_estimee());
            if (s != null && !s.equals("null") && !s.trim().isEmpty()) {
                coutVal = Double.parseDouble(s.replace(",", "."));
            }
        } catch (Exception e) { coutVal = 0.0; }

        // Badge de coût style "Vert" comme dans ta carte principale
        Label coutTache = new Label(String.format("%.2f DT", coutVal));
        coutTache.setStyle("-fx-background-color: #f0fdf4; -fx-text-fill: #2e7d32; -fx-padding: 2 8; -fx-background-radius: 8; -fx-font-weight: bold; -fx-font-size: 11px;");

        infoRow.getChildren().addAll(dateTache, coutTache);

        // 4. DESCRIPTION (Cachée par défaut)
        Label descLabel = new Label(t.getDesciption());
        descLabel.setWrapText(true);
        descLabel.setStyle("-fx-text-fill: #475569; -fx-font-size: 13px; -fx-padding: 10 5 5 5; -fx-border-color: #f1f5f9; -fx-border-width: 1 0 0 0;");
        descLabel.setVisible(false);
        descLabel.setManaged(false);

        // Assemblage
        card.getChildren().addAll(header, infoRow, descLabel);

        // 5. ÉVÉNEMENTS (Hover et Clic pour déplier)
        card.setOnMouseClicked(event -> {
            boolean isVisible = descLabel.isVisible();
            descLabel.setVisible(!isVisible);
            descLabel.setManaged(!isVisible);

            // Change la bordure en vert quand c'est ouvert
            if (!isVisible) {
                card.setStyle(card.getStyle().replace("-fx-border-color: #f1f5f9;", "-fx-border-color: #7ca76f;"));
            } else {
                card.setStyle(card.getStyle().replace("-fx-border-color: #7ca76f;", "-fx-border-color: #f1f5f9;"));
            }
        });

        // Effet au survol (changement de couleur de fond)
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

    @FXML
    private void goBackLabel(MouseEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/fxml/Dashboard.fxml"));
            ((Node) event.getSource()).getScene().setRoot(root);
        } catch (IOException e) { e.printStackTrace(); }
    }
}