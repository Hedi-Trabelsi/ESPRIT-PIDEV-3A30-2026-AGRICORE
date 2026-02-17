package controllers;

import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import models.EvennementAgricole;
import models.Participant;
import services.EvennementService;
import services.ParticipantService;

import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

public class DashboardController {

    @FXML
    private VBox mainContentVBox;

    private final EvennementService evennementService = new EvennementService();
    private final ParticipantService participantService = new ParticipantService();

    @FXML
    public void initialize() {
        showGestionEvenements();
    }

    // ===================== DISPLAY EVENTS =====================
    @FXML
    private void showGestionEvenements() {
        mainContentVBox.getChildren().clear();

        Label header = new Label("Événements Agricoles à Venir");
        header.setFont(Font.font("System", FontWeight.BOLD, 30));
        header.setStyle("-fx-text-fill: #1a3c1a;");
        mainContentVBox.getChildren().add(header);

        FlowPane flowPane = new FlowPane();
        flowPane.setHgap(20);
        flowPane.setVgap(20);

        try {
            List<EvennementAgricole> events = evennementService.read();
            for (EvennementAgricole event : events) {
                flowPane.getChildren().add(createEventCard(event));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        mainContentVBox.getChildren().add(flowPane);
    }

    private VBox createEventCard(EvennementAgricole ev) {
        VBox card = new VBox(10);
        card.setPrefWidth(280);
        card.setPadding(new Insets(15));
        card.setStyle("-fx-background-color: white; -fx-background-radius: 15;");
        card.setEffect(new DropShadow(10, Color.rgb(0,0,0,0.1)));

        // --- TOP BAR (Icons) ---
        HBox topIcons = new HBox(10);
        topIcons.setAlignment(Pos.TOP_RIGHT);

        // Participants Icon (Blue)
        Label viewParticipantsIcon = new Label("👥");
        viewParticipantsIcon.setStyle("-fx-text-fill: #3498db; -fx-font-size: 18px; -fx-cursor: hand;");
        viewParticipantsIcon.setTooltip(new Tooltip("Voir les participants"));
        viewParticipantsIcon.setOnMouseClicked(e -> showParticipantsForEvent(ev));

        // Trash Icon (Red)
        Label trashIcon = new Label("🗑");
        trashIcon.setStyle("-fx-text-fill: #e74c3c; -fx-font-size: 18px; -fx-cursor: hand;");
        trashIcon.setOnMouseClicked(e -> handleDeleteEvent(ev));

        topIcons.getChildren().addAll(viewParticipantsIcon, trashIcon);

        // --- CONTENT ---
        Label lblTitle = new Label(ev.getTitre());
        lblTitle.setFont(Font.font("System", FontWeight.BOLD, 16));
        lblTitle.setWrapText(true);
        lblTitle.setPrefHeight(45);
        lblTitle.setStyle("-fx-text-fill: #2d5a27;");

        Label lblDate = new Label("📅 " + ev.getDateDebut().format(DateTimeFormatter.ofPattern("dd MMM yyyy")));
        lblDate.setStyle("-fx-text-fill: #7ca76f; -fx-font-weight: bold;");

        Label lblLocation = new Label("📍 " + ev.getLieu());
        lblLocation.setStyle("-fx-text-fill: #666666;");

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        Button btnDetails = new Button("Modifier");
        btnDetails.setMaxWidth(Double.MAX_VALUE);
        btnDetails.setStyle("-fx-background-color: #7ca76f; -fx-text-fill: white; -fx-background-radius: 8;");
        btnDetails.setOnAction(e -> showUpdateEvenementForm(ev));

        card.getChildren().addAll(topIcons, lblTitle, lblDate, lblLocation, spacer, btnDetails);
        return card;
    }

    // ===================== DISPLAY PARTICIPANTS =====================
    private void showParticipantsForEvent(EvennementAgricole ev) {
        mainContentVBox.getChildren().clear();

        // Header with Back Button
        HBox headerBox = new HBox(20);
        headerBox.setAlignment(Pos.CENTER_LEFT);

        Button btnBack = new Button("← Retour");
        btnBack.setStyle("-fx-background-color: #f4f4f4; -fx-background-radius: 10;");
        btnBack.setOnAction(e -> showGestionEvenements());

        Label header = new Label("Participants: " + ev.getTitre());
        header.setFont(Font.font("System", FontWeight.BOLD, 24));

        headerBox.getChildren().addAll(btnBack, header);
        mainContentVBox.getChildren().add(headerBox);

        FlowPane flowPane = new FlowPane(20, 20);

        try {
            // Logic: Filter participants where idEvennement matches
            List<Participant> allParticipants = participantService.read();
            List<Participant> eventParticipants = allParticipants.stream()
                    .filter(p -> p.getIdEvennement() == ev.getIdEvennement())
                    .collect(Collectors.toList());

            if (eventParticipants.isEmpty()) {
                mainContentVBox.getChildren().add(new Label("Aucun participant inscrit à cet événement."));
            } else {
                for (Participant p : eventParticipants) {
                    flowPane.getChildren().add(createParticipantCard(p));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        mainContentVBox.getChildren().add(flowPane);
    }

    private VBox createParticipantCard(Participant p) {
        VBox card = new VBox(5);
        card.setPrefWidth(220);
        card.setPadding(new Insets(15));
        card.setStyle("-fx-background-color: #f9f9f9; -fx-background-radius: 10; -fx-border-color: #ddd; -fx-border-radius: 10;");

        Label lblUser = new Label("Utilisateur #" + p.getIdUtilisateur());
        lblUser.setFont(Font.font("System", FontWeight.BOLD, 14));

        Label lblStatus = new Label("Statut: " + p.getStatutParticipation());
        lblStatus.setStyle("-fx-text-fill: " + (p.getStatutParticipation().equalsIgnoreCase("Confirmé") ? "#27ae60" : "#f39c12"));

        Label lblPaid = new Label("Payé: " + p.getMontantPayee() + " DT");
        lblPaid.setStyle("-fx-font-size: 11px;");

        card.getChildren().addAll(lblUser, lblStatus, lblPaid);
        return card;
    }

    // ===================== ACTIONS =====================
    private void handleDeleteEvent(EvennementAgricole ev) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Supprimer cet événement ?", ButtonType.YES, ButtonType.NO);
        alert.showAndWait().ifPresent(res -> {
            if (res == ButtonType.YES) {
                try {
                    evennementService.delete(ev.getIdEvennement());
                    showGestionEvenements();
                } catch (SQLException ex) { ex.printStackTrace(); }
            }
        });
    }

    private void showUpdateEvenementForm(EvennementAgricole ev) {
        // ... (Keep your existing update form logic here) ...
    }
}