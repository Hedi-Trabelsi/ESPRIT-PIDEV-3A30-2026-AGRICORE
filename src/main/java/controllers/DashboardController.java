package controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import models.Maintenance;
import services.ServiceMaintenance;

import java.sql.SQLException;
import java.util.List;

public class DashboardController {

    @FXML
    private ListView<Maintenance> mainList;

    private final ServiceMaintenance serviceMaintenance = new ServiceMaintenance();

    @FXML
    public void initialize() {
        loadData();
        setupCustomCells();
    }

    private void loadData() {
        try {
            List<Maintenance> list = serviceMaintenance.afficher();
            mainList.getItems().setAll(list);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void setupCustomCells() {
        mainList.setCellFactory(param -> new ListCell<Maintenance>() {

            @Override
            protected void updateItem(Maintenance m, boolean empty) {
                super.updateItem(m, empty);

                if (empty || m == null) {
                    setGraphic(null);
                    return;
                }

                // ==== Contenu de la carte ====
                Label typeLabel = new Label(m.getType());
                typeLabel.getStyleClass().add("title-label");

                Label descLabel = new Label(m.getDescription());
                descLabel.getStyleClass().add("desc-label");

                Label lieuLabel = new Label("Lieu: " + m.getLieu());
                lieuLabel.getStyleClass().add("sub-label");

                Label equipLabel = new Label("equipement: " + m.getEquipement());
                equipLabel.getStyleClass().add("sub-label");

                VBox leftBox = new VBox(typeLabel, descLabel, lieuLabel, equipLabel);
                leftBox.setSpacing(5);

                // ==== Statut ====
                Label statusLabel = new Label(m.getStatut());
                statusLabel.getStyleClass().add("status");
                switch (m.getStatut().toLowerCase()) {
                    case "en cours": statusLabel.getStyleClass().add("status-en-cours"); break;
                    case "en attente": statusLabel.getStyleClass().add("status-en-attente"); break;
                    case "refusee": statusLabel.getStyleClass().add("status-refusee"); break;
                    default: statusLabel.getStyleClass().add("status-planifiee");
                }

                // ==== Priorite ====
                Label priorityLabel = new Label(m.getPriorite());
                priorityLabel.getStyleClass().add("priority");
                switch (m.getPriorite().toLowerCase()) {
                    case "urgente": priorityLabel.getStyleClass().add("priority-urgente"); break;
                    case "normale": priorityLabel.getStyleClass().add("priority-normale"); break;
                    case "faible": priorityLabel.getStyleClass().add("priority-faible"); break;
                }

                HBox statusBox = new HBox(statusLabel, priorityLabel);
                statusBox.setSpacing(10);

                Region spacer = new Region();
                HBox.setHgrow(spacer, Priority.ALWAYS);

                HBox container = new HBox(leftBox, spacer, statusBox);
                container.setSpacing(15);
                container.getStyleClass().add("card");

                // ==== Bouton supprimer ====
                Button deleteBtn = new Button("supprimer");
                deleteBtn.getStyleClass().add("delete-button");
                deleteBtn.setOnAction(e -> {
                    Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
                    confirm.setContentText("Supprimer cette maintenance ?");
                    confirm.showAndWait().ifPresent(r -> {
                        if (r == ButtonType.OK) {
                            try {
                                serviceMaintenance.supprimer(m.getId());
                                mainList.getItems().remove(m);
                            } catch (SQLException ex) {
                                ex.printStackTrace();
                            }
                        }
                    });
                });

                container.getChildren().add(deleteBtn);

                // ==== Clic sur la carte ====
                container.setOnMouseClicked(e -> {
                    if ("planifiee".equalsIgnoreCase(m.getStatut())) {
                        openTacheWindow(m, container);
                    } else if ("en cours".equalsIgnoreCase(m.getStatut())) {
                        Alert alert = new Alert(Alert.AlertType.INFORMATION);
                        alert.setTitle("Maintenance en cours");
                        alert.setHeaderText(null);
                        alert.setContentText("Cette maintenance est deja en cours !");
                        alert.showAndWait();
                    }
                });

                setGraphic(container);
            }
        });
    }

    private void openTacheWindow(Maintenance maintenance, HBox card) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/interfaces/ShowMaintenanceDetails.fxml"));
            Parent root = loader.load();

            ShowMaintenanceDetailsController controller = loader.getController();
            controller.setMaintenance(maintenance);

            card.getScene().setRoot(root);

        } catch (Exception e) {
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Erreur");
            alert.setHeaderText("Impossible d'ouvrir la maintenance");
            alert.setContentText(e.getMessage());
            alert.showAndWait();
        }
    }

    // ==== Couleur priorites ====
    private String getPriorityColor(String priorite) {
        if (priorite == null) return "#b0b0b0";
        switch (priorite.toLowerCase()) {
            case "urgente": return "#f5c6cb";
            case "normale": return "#ffeeba";
            case "faible": return "#c3e6cb";
            default: return "#b0b0b0";
        }
    }

    // ==== Style statuts ====
    private String getStatusStyle(String statut) {
        if (statut == null) return "";
        switch (statut.toLowerCase()) {
            case "en cours":
                return "-fx-background-color:#d1ecf1; -fx-text-fill:#0c5460; -fx-padding:5 10; -fx-background-radius:20;";
            case "en attente":
                return "-fx-background-color:#fff3cd; -fx-text-fill:#856404; -fx-padding:5 10; -fx-background-radius:20;";
            case "refusee":
                return "-fx-background-color:#f8d7da; -fx-text-fill:red; -fx-padding:5 10; -fx-background-radius:20;";
            default: // planifiee ou resolu
                return "-fx-background-color:#d4edda; -fx-text-fill:green; -fx-padding:5 10; -fx-background-radius:20;";
        }
    }
}
