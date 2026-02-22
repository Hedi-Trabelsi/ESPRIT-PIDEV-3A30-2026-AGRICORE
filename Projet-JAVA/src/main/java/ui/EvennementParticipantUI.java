package ui;

import javafx.application.Application;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import models.EvennementAgricole;
import models.Participant;
import services.EvennementService;
import services.ParticipantService;

import java.sql.SQLException;
import java.time.LocalDate;

public class EvennementParticipantUI extends Application {

    private final EvennementService eventService = new EvennementService();
    private final ParticipantService participantService = new ParticipantService();

    private TableView<EvennementAgricole> eventTable;
    private TableView<Participant> participantTable;

    // Event input fields
    private TextField tfTitle, tfLieu, tfCapacite, tfFrais;
    private DatePicker dpStart, dpEnd;

    // Participant input fields
    private TextField tfUserId, tfEventId, tfMontant;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        stage.setTitle("Gestion des Evennements et Participants");

        VBox root = new VBox(20);
        root.setPadding(new Insets(15));
        root.setStyle("-fx-background-color: #e6f4ea;"); // light green background

        // --- Event Section ---
        Label lblEvent = new Label("Evennements");
        lblEvent.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        eventTable = new TableView<>();
        eventTable.setPrefHeight(200);
        eventTable.setStyle("-fx-border-color: green; -fx-border-width: 2px;");

        TableColumn<EvennementAgricole, Integer> colIdE = new TableColumn<>("ID");
        colIdE.setCellValueFactory(data -> new SimpleIntegerProperty(data.getValue().getIdEvennement()).asObject());
        TableColumn<EvennementAgricole, String> colTitle = new TableColumn<>("Titre");
        colTitle.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getTitre()));
        TableColumn<EvennementAgricole, String> colLieu = new TableColumn<>("Lieu");
        colLieu.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getLieu()));

        eventTable.getColumns().addAll(colIdE, colTitle, colLieu);
        refreshEventTable();

        // Event input form
        HBox eventForm = new HBox(10);
        tfTitle = new TextField();
        tfTitle.setPromptText("Titre");
        tfLieu = new TextField();
        tfLieu.setPromptText("Lieu");
        tfCapacite = new TextField();
        tfCapacite.setPromptText("Capacité Max");
        tfFrais = new TextField();
        tfFrais.setPromptText("Frais Inscription");
        dpStart = new DatePicker(LocalDate.now());
        dpEnd = new DatePicker(LocalDate.now().plusDays(1));
        eventForm.getChildren().addAll(tfTitle, tfLieu, tfCapacite, tfFrais, dpStart, dpEnd);

        // Event buttons
        HBox eventButtons = new HBox(10);
        Button addEventBtn = new Button("Ajouter Event");
        addEventBtn.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white;");
        addEventBtn.setOnAction(e -> addEvent());

        Button deleteEventBtn = new Button("Supprimer Event");
        deleteEventBtn.setStyle("-fx-background-color: #f44336; -fx-text-fill: white;");
        deleteEventBtn.setOnAction(e -> deleteEvent());

        eventButtons.getChildren().addAll(addEventBtn, deleteEventBtn);

        // --- Participant Section ---
        Label lblParticipant = new Label("Participants");
        lblParticipant.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        participantTable = new TableView<>();
        participantTable.setPrefHeight(200);
        participantTable.setStyle("-fx-border-color: green; -fx-border-width: 2px;");

        TableColumn<Participant, Integer> colIdP = new TableColumn<>("ID");
        colIdP.setCellValueFactory(data -> new SimpleIntegerProperty(data.getValue().getIdParticipant()).asObject());
        TableColumn<Participant, Integer> colEventId = new TableColumn<>("Event ID");
        colEventId.setCellValueFactory(data -> new SimpleIntegerProperty(data.getValue().getIdEvennement()).asObject());
        TableColumn<Participant, Integer> colUserId = new TableColumn<>("User ID");
        colUserId.setCellValueFactory(data -> new SimpleIntegerProperty(data.getValue().getIdUtilisateur()).asObject());

        participantTable.getColumns().addAll(colIdP, colEventId, colUserId);
        refreshParticipantTable();

        // Participant input form
        HBox participantForm = new HBox(10);
        tfUserId = new TextField();
        tfUserId.setPromptText("User ID");
        tfEventId = new TextField();
        tfEventId.setPromptText("Event ID");
        tfMontant = new TextField();
        tfMontant.setPromptText("Montant payé");
        participantForm.getChildren().addAll(tfUserId, tfEventId, tfMontant);

        // Participant buttons
        HBox participantButtons = new HBox(10);
        Button addParticipantBtn = new Button("Ajouter Participant");
        addParticipantBtn.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white;");
        addParticipantBtn.setOnAction(e -> addParticipant());

        Button deleteParticipantBtn = new Button("Supprimer Participant");
        deleteParticipantBtn.setStyle("-fx-background-color: #f44336; -fx-text-fill: white;");
        deleteParticipantBtn.setOnAction(e -> deleteParticipant());

        participantButtons.getChildren().addAll(addParticipantBtn, deleteParticipantBtn);

        // Combine everything
        root.getChildren().addAll(
                lblEvent, eventForm, eventTable, eventButtons,
                lblParticipant, participantForm, participantTable, participantButtons
        );

        Scene scene = new Scene(root, 1000, 700);
        stage.setScene(scene);
        stage.show();
    }

    private void refreshEventTable() {
        try {
            eventTable.getItems().setAll(eventService.read());
        } catch (SQLException ex) {
            showAlert("Erreur", "Impossible de charger les evennements: " + ex.getMessage());
        }
    }

    private void refreshParticipantTable() {
        try {
            participantTable.getItems().setAll(participantService.read());
        } catch (SQLException ex) {
            showAlert("Erreur", "Impossible de charger les participants: " + ex.getMessage());
        }
    }

    private void addEvent() {
        EvennementAgricole e = new EvennementAgricole();
        e.setTitre(tfTitle.getText());
        e.setLieu(tfLieu.getText());
        e.setCapaciteMax(Integer.parseInt(tfCapacite.getText()));
        e.setFraisInscription(Integer.parseInt(tfFrais.getText()));
        e.setStatut("Actif");
        e.setDescription("Description");
        e.setDateDebut(dpStart.getValue());
        e.setDateFin(dpEnd.getValue());

        try {
            eventService.create(e);
            refreshEventTable();
            clearEventFields();
        } catch (SQLException ex) {
            showAlert("Erreur", "Impossible d'ajouter l'event: " + ex.getMessage());
        }
    }

    private void deleteEvent() {
        EvennementAgricole selected = eventTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            try {
                eventService.delete(selected.getIdEvennement());
                refreshEventTable();
            } catch (SQLException ex) {
                showAlert("Erreur", "Impossible de supprimer l'event: " + ex.getMessage());
            }
        }
    }

    private void addParticipant() {
        try {
            Participant p = new Participant();
            p.setIdUtilisateur(Integer.parseInt(tfUserId.getText()));
            p.setIdEvennement(Integer.parseInt(tfEventId.getText()));
            p.setDateInscription(LocalDate.now());
            p.setStatutParticipation("Inscrit");
            p.setMontantPayee(tfMontant.getText());
            p.setConfirmation("Non");

            participantService.create(p);
            refreshParticipantTable();
            clearParticipantFields();
        } catch (SQLException ex) {
            showAlert("Erreur", "Impossible d'ajouter le participant: " + ex.getMessage());
        } catch (NumberFormatException ex) {
            showAlert("Erreur", "Veuillez entrer des nombres valides pour User ID et Event ID.");
        }
    }

    private void deleteParticipant() {
        Participant selected = participantTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            try {
                participantService.delete(selected.getIdParticipant());
                refreshParticipantTable();
            } catch (SQLException ex) {
                showAlert("Erreur", "Impossible de supprimer le participant: " + ex.getMessage());
            }
        }
    }

    private void clearEventFields() {
        tfTitle.clear();
        tfLieu.clear();
        tfCapacite.clear();
        tfFrais.clear();
        dpStart.setValue(LocalDate.now());
        dpEnd.setValue(LocalDate.now().plusDays(1));
    }

    private void clearParticipantFields() {
        tfUserId.clear();
        tfEventId.clear();
        tfMontant.clear();
    }

    private void showAlert(String title, String msg) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}
