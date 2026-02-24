package controllers;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.util.stream.Collectors;

import javafx.scene.Scene;
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
import javafx.stage.Modality;
import javafx.stage.Stage;
import models.Maintenance;
import services.ServiceMaintenance;
import java.sql.SQLException;
import java.util.List;
//------------------------
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.geometry.Side;
import javafx.scene.input.MouseEvent;

//------------------------
public class DashboardController {

    @FXML
    private Label notificationBadge;

    @FXML
    private ChoiceBox<String> priorityFilter;

    @FXML
    private ListView<Maintenance> mainList;

    private final ServiceMaintenance serviceMaintenance = new ServiceMaintenance();

    @FXML
    private TextField searchField;
    @FXML private Label urgencyDot;
    @FXML
    public void initialize() {
        loadData();
        setupCustomCells();
        updateNotificationCount(); // Mise à jour au démarrage
        // Listener pour la recherche
        searchField.textProperty().addListener((observable, oldValue, newValue) -> filterList(newValue));
        searchField.textProperty().addListener((obs, oldVal, newVal) -> filterList(newVal));
        priorityFilter.valueProperty().addListener((obs, oldVal, newVal) -> filterList(searchField.getText()));
        // Lier badge à la taille des notifications

    }
//---------------------------------------------------
 /*   private void updateNotificationCount() {
        try {
            long count = serviceMaintenance.afficher().stream()
                    .filter(m -> "en attente".equalsIgnoreCase(m.getStatut()))
                    .count();
            notificationBadge.setText(String.valueOf(count));
            notificationBadge.setVisible(count > 0);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
*/

/*
    private void updateNotificationCount() {
        try {
            List<Maintenance> toutes = serviceMaintenance.afficher();

            // 1. Badge de DROITE : Le nombre total (Ton code inchangé)
            long count = toutes.stream()
                    .filter(m -> "en attente".equalsIgnoreCase(m.getStatut()))
                    .count();
            notificationBadge.setText(String.valueOf(count));
            notificationBadge.setVisible(count > 0);

            // 2. Badge de GAUCHE : Le point "!" (Urgence ou Retard)
            boolean hasUrgency = toutes.stream()
                    .filter(m -> "en attente".equalsIgnoreCase(m.getStatut()))
                    .anyMatch(m -> {
                        long jours = java.time.temporal.ChronoUnit.DAYS.between(m.getDateDeclaration(), java.time.LocalDate.now());
                        return "urgente".equalsIgnoreCase(m.getPriorite()) || jours >= 2;
                    });

            // On affiche le point rouge à gauche SEULEMENT s'il y a une urgence
            if (urgencyDot != null) {
                urgencyDot.setVisible(hasUrgency);
                if (hasUrgency) {
                    urgencyDot.setText("!");
                    urgencyDot.setStyle(
                            "-fx-background-color: red; " +
                                    "-fx-text-fill: white; " +
                                    "-fx-font-weight: bold; " +
                                    "-fx-background-radius: 50%; " + // Pour faire un rond
                                    "-fx-min-width: 15px; " +
                                    "-fx-min-height: 15px; " +
                                    "-fx-alignment: center;"
                    );
                }
            }

            // 3. Ton Popup d'alerte (Inchangé)
            if (hasUrgency) {
                javafx.application.Platform.runLater(() -> {
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("ALERTE CRITIQUE");
                    alert.setHeaderText("Maintenance Urgente Non Traitee !");
                    alert.setContentText("Il y a des demandes urgentes ou en retard à gauche de la cloche !");
                    alert.show();
                });
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
*/
private void updateNotificationCount() {
    try {
        List<Maintenance> toutes = serviceMaintenance.afficher();

        // 1. Filtrer les demandes en attente
        List<Maintenance> enAttente = toutes.stream()
                .filter(m -> "en attente".equalsIgnoreCase(m.getStatut()))
                .collect(Collectors.toList());

        long count = enAttente.size();

        // 2. Vérifier s'il y a une urgence
        boolean alerteUrgente = enAttente.stream()
                .anyMatch(m -> "urgente".equalsIgnoreCase(m.getPriorite()));

        // --- GESTION DU BADGE VISUEL ---
        if (count > 0) {
            notificationBadge.setVisible(true);

            if (alerteUrgente) {
                // MODE ALERTE : On affiche seulement !
                notificationBadge.setText("!");
                notificationBadge.getStyleClass().add("badge-alerte");
            } else {
                // MODE NORMAL : On affiche le nombre
                notificationBadge.setText(String.valueOf(count));
                notificationBadge.getStyleClass().add("badge-normal");
            }
        } else {
            notificationBadge.setVisible(false);
        }

        // --- AJOUT DE LA PARTIE ALERTE (POPUP) ---
        if (alerteUrgente) {
            // Platform.runLater évite les erreurs de chargement de l'interface
            javafx.application.Platform.runLater(() -> {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("ALERTE CRITIQUE");
                alert.setHeaderText("Maintenance Urgente Non Traitee !");
                alert.setContentText("Il y a des demandes urgentes en attente. " +
                        "Veuillez verifier l'onglet des notifications pour les traiter rapidement.");
                alert.show();
            });
        }

    } catch (SQLException e) {
        e.printStackTrace();
    }
}
    @FXML
    void showNotifications(MouseEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/interfaces/NotificationView.fxml"));
            Parent root = loader.load();

            // On récupère le contrôleur de la vue notification
            NotificationController controller = loader.getController();
            // On lui passe l'instance actuelle pour le refreshAll()
            controller.setParentController(this);

            // ON CHANGE LA SCÈNE ACTUELLE (pas de nouvelle fenêtre)
            notificationBadge.getScene().setRoot(root);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Méthode utilitaire pour tout rafraîchir d'un coup
    public void refreshAll() {
        loadData(); // Rafraîchit la ListView principale
        updateNotificationCount(); // Rafraîchit le badge de la cloche
    }


//-----------------------------------------------------------------
    private void filterList(String keyword) {
        try {
            List<Maintenance> allMaintenances = serviceMaintenance.afficher();

            // Crée une variable finale pour le lambda
            final String priority = (priorityFilter.getValue() != null) ? priorityFilter.getValue() : "Toutes";

            List<Maintenance> filtered = allMaintenances.stream()
                    .filter(m -> (keyword == null || keyword.isEmpty() ||
                            m.getType().toLowerCase().contains(keyword.toLowerCase()) ||
                            m.getDescription().toLowerCase().contains(keyword.toLowerCase()) ||
                            m.getLieu().toLowerCase().contains(keyword.toLowerCase()) ||
                            m.getEquipement().toLowerCase().contains(keyword.toLowerCase())
                    ))
                    .filter(m -> priority.equals("Toutes") || m.getPriorite().equalsIgnoreCase(priority))
                    .collect(Collectors.toList());

            mainList.getItems().setAll(filtered);

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


    @FXML
    private ImageView statsIcon;

    @FXML
    void openStatsWindow(MouseEvent event) {
        try {
            // 1. Charger la vue des statistiques
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/interfaces/StatsView.fxml"));
            Parent root = loader.load();

            // 2. Changer la racine (root) de la scène actuelle
            // On utilise 'statsIcon' car c'est lui qui a reçu le clic
            statsIcon.getScene().setRoot(root);

        } catch (IOException e) {
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Erreur");
            alert.setHeaderText("Navigation impossible");
            alert.setContentText("Erreur lors du chargement des statistiques.");
            alert.show();
        }
    }


    private void loadData() {
        try {
            List<Maintenance> list = serviceMaintenance.afficher().stream()
                    .filter(m -> !"en attente".equalsIgnoreCase(m.getStatut())) // Exclure les "en attente"
                    .collect(Collectors.toList());
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


                Label typeLabel = new Label(m.getType());
                typeLabel.getStyleClass().add("title-label");

                Label descLabel = new Label(m.getDescription());
                descLabel.getStyleClass().add("desc-label");

                Label lieuLabel = new Label("Lieu: " + m.getLieu());
                lieuLabel.getStyleClass().add("sub-label");

                Label equipLabel = new Label("Equipement: " + m.getEquipement());
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

                // ==== Priorité ====
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
                Button deleteBtn = new Button("Supprimer");
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

                // ==== Boutons accepter/refuser si en attente ====
                if ("en attente".equalsIgnoreCase(m.getStatut())) {
                    Button accepterBtn = new Button("Accepter");
                    accepterBtn.getStyleClass().add("btn-success");
                    accepterBtn.setOnAction(ev -> {
                        try {
                            m.setStatut("en cours");
                            serviceMaintenance.modifier(m);
                            loadData(); // recharge la liste
                        } catch (SQLException ex) {
                            ex.printStackTrace();
                        }
                    });

                    Button refuserBtn = new Button("Refuser");
                    refuserBtn.getStyleClass().add("btn-danger");
                    refuserBtn.setOnAction(ev -> {
                        try {
                            m.setStatut("refusee");
                            serviceMaintenance.modifier(m);
                            loadData();
                        } catch (SQLException ex) {
                            ex.printStackTrace();
                        }
                    });

                    container.getChildren().addAll(accepterBtn, refuserBtn);
                }

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
