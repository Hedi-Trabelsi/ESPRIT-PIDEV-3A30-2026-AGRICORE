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

import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;

public class ShowMaintenanceController {

    private final ServiceMaintenance serviceMaintenance = new ServiceMaintenance();

    @FXML private javafx.scene.control.TextField searchTf;
    @FXML private GridPane gridPane;
    @FXML private Button addBtn;

    @FXML
    void initialize() {
        applyProfessionalButtonStyle(addBtn);
        loadMaintenances();

        // --- AJOUT DE LA RECHERCHE ---
        searchTf.textProperty().addListener((observable, oldValue, newValue) -> {
            updateFilteredMaintenances(newValue);
        });

        addBtn.setOnAction(e -> navigateAddMaintenance());
    }

    // Nouvelle méthode pour filtrer sans modifier la structure existante
    private void updateFilteredMaintenances(String searchText) {
        try {
            List<Maintenance> allMaintenances = serviceMaintenance.afficher();

            if (searchText == null || searchText.isEmpty()) {
                displayList(allMaintenances);
                return;
            }

            String filter = searchText.toLowerCase();
            List<Maintenance> filtered = allMaintenances.stream()
                    .filter(m -> m.getType().toLowerCase().contains(filter) ||
                            m.getDescription().toLowerCase().contains(filter))
                    .collect(Collectors.toList());

            displayList(filtered);
        } catch (SQLException e) {
            e.printStackTrace();
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
            addBtn.getScene().setRoot(root);
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
        card.setStyle("-fx-background-color: white; " +
                "-fx-padding: 20; " +
                "-fx-background-radius: 25; " +
                "-fx-spacing: 12; " +
                "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.08), 15, 0, 0, 8);");
        card.setMinWidth(260);
        card.setMaxWidth(260);
        card.setMinHeight(320);
        card.setAlignment(javafx.geometry.Pos.TOP_LEFT);
        card.setCursor(javafx.scene.Cursor.HAND);

        card.setOnMouseClicked(event -> {
            if (!(event.getTarget() instanceof Button)) {
                openMaintenanceDetails(m);
            }
        });

        HBox header = new HBox();
        header.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);
        header.setSpacing(10);

        Label editBtn = new Label("✎");
        editBtn.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 18px; -fx-cursor: hand;");
        Label deleteBtn = new Label("🗑");
        deleteBtn.setStyle("-fx-text-fill: #fca5a5; -fx-font-size: 18px; -fx-cursor: hand;");
        header.getChildren().addAll(editBtn, deleteBtn);

        Label titleLabel = new Label(m.getType());
        titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #1e293b;");
        Label descLabel = new Label(m.getDescription());
        descLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #64748b;");
        descLabel.setWrapText(true);
        descLabel.setMinHeight(60);

        Label statutLabel = new Label(m.getStatut().toUpperCase());
        statutLabel.setStyle(getStatusStyle(m.getStatut()));

        Button actionBtn = new Button("Gérer Maintenance");
        actionBtn.setMaxWidth(Double.MAX_VALUE);
        actionBtn.setStyle("-fx-background-color: #f8fafc; -fx-text-fill: #475569; " +
                "-fx-font-weight: bold; -fx-background-radius: 12; -fx-padding: 10; " +
                "-fx-border-color: #e2e8f0; -fx-cursor: hand;");

        editBtn.setOnMouseClicked(e -> {
            e.consume();
            navigateUpdate(m);
        });

        deleteBtn.setOnMouseClicked(e -> {
            e.consume();
            try {
                serviceMaintenance.supprimer(m.getId());
                loadMaintenances();
            } catch (Exception ex) { ex.printStackTrace(); }
        });

        actionBtn.setOnAction(e -> navigatePlanifier(m));

        card.getChildren().addAll(header, titleLabel, descLabel, statutLabel, actionBtn);
        return card;
    }

    private void openMaintenanceDetails(Maintenance m) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/ShowMaintenanceDetails.fxml"));
            Parent root = loader.load();
            ShowMaintenanceDetailsController controller = loader.getController();
            controller.setMaintenance(m);
            gridPane.getScene().setRoot(root);
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void navigatePlanifier(Maintenance m) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/AddTache.fxml"));
            Parent root = loader.load();
            AddTacheController controller = loader.getController();
            controller.setMaintenanceSelectionnee(m);
            gridPane.getScene().setRoot(root);
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void navigateUpdate(Maintenance m) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/UpdateMaintenance.fxml"));
            Parent root = loader.load();
            UpdateMaintenanceController controller = loader.getController();
            controller.setMaintenance(m);
            gridPane.getScene().setRoot(root);
        } catch (Exception ex) { ex.printStackTrace(); }
    }

    private String getStatusStyle(String statut) {
        if (statut == null) return "";
        statut = statut.toLowerCase();
        if (statut.contains("resolu")) return "-fx-background-color:#d4edda; -fx-text-fill:green; -fx-padding:5 10; -fx-background-radius:10;";
        if (statut.contains("cours")) return "-fx-background-color:#d1ecf1; -fx-text-fill:#0c5460; -fx-padding:5 10; -fx-background-radius:10;";
        if (statut.contains("attente")) return "-fx-background-color:#fff3cd; -fx-text-fill:#856404; -fx-padding:5 10; -fx-background-radius:10;";
        return "-fx-background-color:#f1f5f9; -fx-text-fill:#475569; -fx-padding:5 10; -fx-background-radius:10;";
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setContentText(content);
        alert.showAndWait();
    }
}