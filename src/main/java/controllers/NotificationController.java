package controllers;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.layout.HBox;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Callback;
import models.Maintenance;
import services.ServiceMaintenance;
import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;
import javafx.event.ActionEvent;
import javafx.scene.Node;
public class NotificationController {
    @FXML private ListView<Maintenance> notifList;
    @FXML private TextField searchField;
    @FXML private ChoiceBox<String> priorityFilter;

    private final ServiceMaintenance service = new ServiceMaintenance();
    private DashboardController parentController;

    public void setParentController(DashboardController parent) {
        this.parentController = parent;
    }
    private final ServiceMaintenance serviceMaintenance = new ServiceMaintenance();
    @FXML
    public void initialize() {
        loadPendingData();
        setupCustomCells(); // La même logique que ton dash !

        // Listeners pour filtrer
        searchField.textProperty().addListener((obs, oldVal, newVal) -> filterList(newVal));
        priorityFilter.valueProperty().addListener((obs, oldVal, newVal) -> filterList(searchField.getText()));
    }

    private void loadPendingData() {
        try {
            // On ne prend que les "en attente"
            List<Maintenance> pending = service.afficher().stream()
                    .filter(m -> "en attente".equalsIgnoreCase(m.getStatut()))
                    .collect(Collectors.toList());
            notifList.getItems().setAll(pending);
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private void filterList(String keyword) {
        try {
            final String priority = (priorityFilter.getValue() != null) ? priorityFilter.getValue() : "Toutes";
            List<Maintenance> filtered = service.afficher().stream()
                    .filter(m -> "en attente".equalsIgnoreCase(m.getStatut())) // Filtre de base
                    .filter(m -> (keyword == null || keyword.isEmpty() || m.getType().toLowerCase().contains(keyword.toLowerCase()) || m.getEquipement().toLowerCase().contains(keyword.toLowerCase())))
                    .filter(m -> priority.equals("Toutes") || m.getPriorite().equalsIgnoreCase(priority))
                    .collect(Collectors.toList());
            notifList.getItems().setAll(filtered);
        } catch (SQLException e) { e.printStackTrace(); }
    }


    private void setupCustomCells() {
        notifList.setCellFactory(param -> new ListCell<Maintenance>() {
            @Override
            protected void updateItem(Maintenance m, boolean empty) {
                super.updateItem(m, empty);

                if (empty || m == null) {
                    setGraphic(null);
                } else {
                    // 1. Infos de gauche
                    Label typeLabel = new Label(m.getType());
                    typeLabel.getStyleClass().add("title-label");
                    VBox leftBox = new VBox(typeLabel, new Label(m.getDescription()), new Label("Equip: " + m.getEquipement()));
                    leftBox.setSpacing(5);

                    // 2. LE STATUT (C'est ce qui manquait !)
                    Label statusLabel = new Label(m.getStatut());
                    statusLabel.getStyleClass().add("status");
                    // On applique le style spécifique au statut "en attente"
                    statusLabel.getStyleClass().add("status-en-attente");

                    // 3. LA PRIORITÉ
                    Label priorityLabel = new Label(m.getPriorite());
                    priorityLabel.getStyleClass().add("priority");
                    // On applique la couleur selon la priorité
                    switch (m.getPriorite().toLowerCase()) {
                        case "urgente": priorityLabel.getStyleClass().add("priority-urgente"); break;
                        case "normale": priorityLabel.getStyleClass().add("priority-normale"); break;
                        case "faible": priorityLabel.getStyleClass().add("priority-faible"); break;
                    }

                    HBox badgesBox = new HBox(statusLabel, priorityLabel);
                    badgesBox.setSpacing(10);
                    badgesBox.setAlignment(javafx.geometry.Pos.CENTER);

                    // 4. Spacer pour pousser vers la droite
                    Region spacer = new Region();
                    HBox.setHgrow(spacer, Priority.ALWAYS);

                    // 5. Boutons stylisés
                    Button accBtn = new Button("Accepter");
                    accBtn.getStyleClass().add("action-button-accept");
                    accBtn.setOnAction(e -> handleAction(m, "en cours"));

                    Button refBtn = new Button("Refuser");
                    refBtn.getStyleClass().add("action-button-refuse");
                    refBtn.setOnAction(e -> handleAction(m, "refusee"));

                    // 6. Assemblage final dans le container
                    // VERIFIE BIEN QUE statusBox (badgesBox ici) EST BIEN AJOUTÉ !
                    HBox container = new HBox(leftBox, spacer, badgesBox, accBtn, refBtn);
                    container.setSpacing(20);
                    container.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                    container.getStyleClass().add("card");

                    setGraphic(container);
                }
            }
        });
    }
    private void handleAction(Maintenance m, String status) {
        try {
            m.setStatut(status);
            service.modifier(m);
            loadPendingData(); // Rafraîchir cette liste
            if (parentController != null) parentController.refreshAll(); // Rafraîchir le badge du dash
        } catch (SQLException e) { e.printStackTrace(); }
    }

    @FXML
    void closeWindow(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/interfaces/Dashboard.fxml"));
            ((javafx.scene.Node) event.getSource()).getScene().setRoot(root);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}