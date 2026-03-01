package Controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import Model.Maintenance;
import services.ServiceMaintenance;
import javafx.collections.FXCollections;
import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;

public class ShowMaintenanceController {

    private final ServiceMaintenance serviceMaintenance;

    {
        try {
            serviceMaintenance = new ServiceMaintenance();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @FXML private javafx.scene.control.TextField searchTf;
    @FXML private GridPane gridPane;
    @FXML private Button addBtn;
    @FXML private ChoiceBox<String> statusFilter;
    @FXML


    void initialize() {
        applyProfessionalButtonStyle(addBtn);

        // Role-based visibility: Technicien (role 2) cannot create maintenance
        int role = UserSession.getRole();
        System.out.println("[DEBUG] ShowMaintenance - UserSession role: " + role);
        if (role == 2) {
            addBtn.setVisible(false);
            addBtn.setManaged(false);
        }

        // --- INITIALISATION DU FILTRE STATUT ---
        if (statusFilter != null) {
            statusFilter.setItems(FXCollections.observableArrayList(
                    "Tous les statuts", "Accepter", "Planifier", "Resolu", "Refusee"
            ));
            statusFilter.setValue("Tous les statuts");

            // Changement ici : on appelle la méthode sans paramètre
            statusFilter.valueProperty().addListener((obs, oldVal, newVal) -> {
                updateFilteredMaintenances();
            });
        }

        loadMaintenances();

        // Changement ici aussi
        searchTf.textProperty().addListener((observable, oldValue, newValue) -> {
            updateFilteredMaintenances();
        });

        addBtn.setOnAction(e -> navigateAddMaintenance());
    }

    // Nouvelle méthode pour filtrer sans modifier la structure existante
    private void updateFilteredMaintenances() { // On ne passe plus de paramètre
        try {
            // 1. On récupère TOUT
            List<Maintenance> allMaintenances = serviceMaintenance.afficher();

            // 2. On récupère les DEUX valeurs de filtre
            String filterText = (searchTf.getText() == null) ? "" : searchTf.getText().toLowerCase().trim();
            String selectedStatus = (statusFilter != null) ? statusFilter.getValue() : "Tous les statuts";

            // 3. On filtre avec les deux critères en même temps
            List<Maintenance> filtered = allMaintenances.stream()
                    .filter(m -> {
                        // Vérification du TEXTE
                        boolean matchesText = filterText.isEmpty() ||
                                (m.getNom_maintenance() != null && m.getNom_maintenance().toLowerCase().contains(filterText)) ||
                                (m.getEquipement() != null && m.getEquipement().toLowerCase().contains(filterText)) ||
                                (m.getDescription() != null && m.getDescription().toLowerCase().contains(filterText)) ||
                                (m.getType() != null && m.getType().toLowerCase().contains(filterText));

                        // Vérification du STATUT (La ChoiceBox)
                        boolean matchesStatus = selectedStatus.equals("Tous les statuts") ||
                                (m.getStatut() != null && m.getStatut().equalsIgnoreCase(selectedStatus));

                        // Il faut que les DEUX soient vrais
                        return matchesText && matchesStatus;
                    })
                    .collect(Collectors.toList());

            // 4. On affiche le résultat
            displayList(filtered);

        } catch (SQLException e) {
            System.err.println("Erreur de filtrage : " + e.getMessage());
        }
    }

    // On sépare l'affichage pour pouvoir l'appeler lors de la recherche
    private void displayList(List<Maintenance> list) {
        gridPane.getChildren().clear();
        int column = 0;
        int row = 0;
        for (Maintenance m : list) {
            VBox card = createCard(m);
            gridPane.add(card, column, row);
            column++;
            if (column > 2) {
                column = 0;
                row++;
            }
        }
    }

    private void applyProfessionalButtonStyle(Button btn) {
        btn.setStyle("-fx-background-color: #2e7d32; " +
                "-fx-text-fill: white; " +
                "-fx-font-weight: bold; " +
                "-fx-padding: 10 25; " +
                "-fx-background-radius: 12; " +
                "-fx-cursor: hand; " +
                "-fx-effect: dropshadow(three-pass-box, rgba(46,125,50,0.3), 10, 0, 0, 5);");
    }

    @FXML
    void navigateAddMaintenance() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/AddMaintenance.fxml"));
            Parent root = loader.load();
            NavigationUtil.loadInContentArea(addBtn, root);
        } catch (Exception e) {
            showAlert("Erreur", "Impossible d'ouvrir l'ajout : " + e.getMessage());
        }
    }

    private void loadMaintenances() {
        try {
            List<Maintenance> maintenanceList = serviceMaintenance.afficher();
            displayList(maintenanceList); // Utilise la méthode d'affichage
        } catch (SQLException e) {
            showAlert("Erreur", "Impossible de charger les maintenances: " + e.getMessage());
        }
    }

    private VBox createCard(Maintenance m) {
        VBox card = new VBox();

        // 1. STYLE DE BASE
        String styleNormal = "-fx-background-color: white; " +
                "-fx-padding: 20; " +
                "-fx-background-radius: 25; " +
                "-fx-spacing: 12; " +
                "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.08), 15, 0, 0, 8);";

        // --- LOGIQUE D'ALERTE ROUGE ---
        boolean estUrgent = m.getPriorite() != null &&
                (m.getPriorite().toLowerCase().contains("urgent") ||
                        m.getPriorite().toLowerCase().contains("haute"));

        boolean estAccepte = m.getStatut() != null && m.getStatut().toLowerCase().contains("accepter");

        // On n'affiche le style rouge QUE SI c'est Urgent ET Accepté
        boolean afficherAlerteRouge = estUrgent && estAccepte;

        if (afficherAlerteRouge) {
            card.setStyle(styleNormal + "-fx-border-color: transparent; -fx-border-width: 2.5; -fx-border-radius: 25; -fx-background-color: #fffafb;");
        } else {
            card.setStyle(styleNormal);
        }

        card.setMinWidth(260);
        card.setMaxWidth(260);
        card.setMinHeight(320);
        card.setAlignment(javafx.geometry.Pos.TOP_LEFT);
        card.setCursor(javafx.scene.Cursor.HAND);

        // --- 2. ANIMATIONS AU SURVOL (HOVER) ---
        javafx.animation.TranslateTransition hoverIn = new javafx.animation.TranslateTransition(javafx.util.Duration.millis(200), card);
        card.setOnMouseEntered(e -> {
            hoverIn.setToY(-10);
            hoverIn.play();
            card.setEffect(new javafx.scene.effect.DropShadow(25, 0, 12, javafx.scene.paint.Color.rgb(0,0,0, 0.15)));
            if (!afficherAlerteRouge) card.setStyle(card.getStyle() + "-fx-background-color: #fcfcfc;");
        });
        card.setOnMouseExited(e -> {
            javafx.animation.TranslateTransition hoverOut = new javafx.animation.TranslateTransition(javafx.util.Duration.millis(200), card);
            hoverOut.setToY(0);
            hoverOut.play();
            card.setEffect(new javafx.scene.effect.DropShadow(15, 0, 8, javafx.scene.paint.Color.rgb(0,0,0, 0.08)));
            if (!afficherAlerteRouge) card.setStyle(card.getStyle().replace("-fx-background-color: #fcfcfc;", "-fx-background-color: white;"));
        });

        // --- 3. CONTENU (HEADER AVEC BOUTONS) ---
        HBox header = new HBox();
        header.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);
        header.setSpacing(10);

        Label editBtn = new Label("✎");
        editBtn.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 18px; -fx-cursor: hand;");

        Label deleteBtn = new Label("🗑");
        deleteBtn.setStyle("-fx-text-fill: #fca5a5; -fx-font-size: 18px; -fx-cursor: hand;");

        header.getChildren().addAll(editBtn, deleteBtn);

        // --- 4. LABELS D'INFORMATION ---
        // Titre
        Label titleLabel = new Label(m.getNom_maintenance());
        titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: " + (afficherAlerteRouge ? "#991b1b" : "#1e293b") + ";");

        // Équipement (Style épuré, majuscules)
        Label equipLabel = new Label(m.getEquipement() != null ? m.getEquipement().toUpperCase() : "ÉQUIPEMENT INCONNU");
        equipLabel.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #64748b; -fx-letter-spacing: 1px;");

        // Description
        Label descLabel = new Label(m.getDescription());
        descLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #64748b;");
        descLabel.setWrapText(true);
        descLabel.setMinHeight(50);

        // Badge Statut
        Label statutLabel = new Label(m.getStatut().toUpperCase());
        statutLabel.setStyle(getStatusStyle(m.getStatut()));

        // Bouton Action (Voir Détails)
        Button actionBtn = new Button("Voir Détails");
        actionBtn.setMaxWidth(Double.MAX_VALUE);

        if (afficherAlerteRouge) {
            actionBtn.setStyle("-fx-background-color: #e11d48; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 12; -fx-padding: 10; -fx-cursor: hand;");
        } else {
            actionBtn.setStyle("-fx-background-color: #f8fafc; -fx-text-fill: #475569; -fx-font-weight: bold; -fx-background-radius: 12; -fx-padding: 10; -fx-border-color: #e2e8f0; -fx-cursor: hand;");
        }

        // --- 5. GESTION DES ÉVÉNEMENTS ---

        // Clic sur la carte (hors boutons d'action)
        card.setOnMouseClicked(event -> {
            javafx.scene.Node target = (javafx.scene.Node) event.getTarget();
            while (target != null && target != card) {
                if (target instanceof Button) return;
                if (target == editBtn || target == deleteBtn) return;
                target = target.getParent();
            }
            openMaintenanceDetails(m);
        });

        // Bouton Modifier
        editBtn.setOnMouseClicked(e -> {
            e.consume();
            navigateUpdate(m);
        });

        // Bouton Supprimer (AVEC CONFIRMATION)
        deleteBtn.setOnMouseClicked(e -> {
            e.consume();
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Suppression");
            alert.setHeaderText("Confirmer la suppression");
            alert.setContentText("Voulez-vous vraiment supprimer la maintenance : " + m.getNom_maintenance() + " ?");

            ButtonType btnSuppr = new ButtonType("Supprimer", ButtonBar.ButtonData.OK_DONE);
            ButtonType btnAnnul = new ButtonType("Annuler", ButtonBar.ButtonData.CANCEL_CLOSE);
            alert.getButtonTypes().setAll(btnSuppr, btnAnnul);

            alert.showAndWait().ifPresent(response -> {
                if (response == btnSuppr) {
                    try {
                        serviceMaintenance.supprimer(m.getId());
                        loadMaintenances(); // Rafraîchit l'affichage
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        showAlert("Erreur", "La suppression a échoué.");
                    }
                }
            });
        });

        // Bouton Action
        actionBtn.setOnAction(e -> openMaintenanceDetails(m));

        // ASSEMBLAGE FINAL
        card.getChildren().addAll(header, titleLabel, equipLabel, descLabel, statutLabel, actionBtn);

        return card;
    }

    private void openMaintenanceDetails(Maintenance m) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/ShowMaintenanceDetails.fxml"));
            Parent root = loader.load();
            ShowMaintenanceDetailsController controller = loader.getController();
            controller.setMaintenance(m);
            NavigationUtil.loadInContentArea(gridPane, root);
        } catch (Exception e) { e.printStackTrace(); }
    }



    private void navigateUpdate(Maintenance m) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/UpdateMaintenance.fxml"));
            Parent root = loader.load();
            UpdateMaintenanceController controller = loader.getController();
            controller.setMaintenance(m);
            NavigationUtil.loadInContentArea(gridPane, root);
        } catch (Exception ex) { ex.printStackTrace(); }
    }

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

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setContentText(content);
        alert.showAndWait();
    }
}