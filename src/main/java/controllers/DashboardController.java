package controllers;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import models.EvennementAgricole;
import models.Participant;
import services.EvennementService;
import services.ParticipantService;

import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class DashboardController {

    @FXML
    private VBox mainContentVBox;

    private final EvennementService evennementService = new EvennementService();
    private final ParticipantService participantService = new ParticipantService();

    // ===================== SHOW TABLES =====================
    @FXML
    private void showGestionEvenements() {
        mainContentVBox.getChildren().clear();

        // ---- Evennements Table ----
        TableView<EvennementAgricole> evennementTable = new TableView<>();
        evennementTable.setPrefHeight(250);

        TableColumn<EvennementAgricole, String> titreCol = new TableColumn<>("Titre");
        titreCol.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getTitre()));

        TableColumn<EvennementAgricole, String> dateDebutCol = new TableColumn<>("Date Début");
        dateDebutCol.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getDateDebut().format(DateTimeFormatter.ISO_DATE))
        );

        TableColumn<EvennementAgricole, String> dateFinCol = new TableColumn<>("Date Fin");
        dateFinCol.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getDateFin().format(DateTimeFormatter.ISO_DATE))
        );

        TableColumn<EvennementAgricole, String> lieuCol = new TableColumn<>("Lieu");
        lieuCol.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getLieu()));

        TableColumn<EvennementAgricole, String> capaciteCol = new TableColumn<>("Capacité Max");
        capaciteCol.setCellValueFactory(cellData -> new SimpleStringProperty(String.valueOf(cellData.getValue().getCapaciteMax())));

        TableColumn<EvennementAgricole, String> fraisCol = new TableColumn<>("Frais Inscription");
        fraisCol.setCellValueFactory(cellData -> new SimpleStringProperty(String.valueOf(cellData.getValue().getFraisInscription())));

        TableColumn<EvennementAgricole, Void> actionCol = new TableColumn<>("Actions");
        actionCol.setCellFactory(col -> new TableCell<>() {
            private final Button btnDelete = new Button("Delete");
            private final Button btnUpdate = new Button("Update");
            private final javafx.scene.layout.HBox hbox = new javafx.scene.layout.HBox(10, btnDelete, btnUpdate);

            {
                btnDelete.setOnAction(e -> {
                    EvennementAgricole ev = getTableView().getItems().get(getIndex());
                    try {
                        evennementService.delete(ev.getIdEvennement());
                        evennementTable.getItems().remove(ev);
                    } catch (SQLException ex) {
                        ex.printStackTrace();
                        showAlert("Erreur", "Impossible de supprimer l'événement !");
                    }
                });

                btnUpdate.setOnAction(e -> {
                    EvennementAgricole ev = getTableView().getItems().get(getIndex());
                    showUpdateEvenementForm(ev, evennementTable);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : hbox);
            }
        });

        evennementTable.getColumns().addAll(titreCol, dateDebutCol, dateFinCol, lieuCol, capaciteCol, fraisCol, actionCol);

        try {
            List<EvennementAgricole> events = evennementService.read();
            evennementTable.setItems(FXCollections.observableArrayList(events));
        } catch (SQLException ex) {
            ex.printStackTrace();
        }

        // ---- Participants Table ----
        TableView<Participant> participantTable = new TableView<>();
        participantTable.setPrefHeight(250);

        TableColumn<Participant, String> userIdCol = new TableColumn<>("User ID");
        userIdCol.setCellValueFactory(cellData -> new SimpleStringProperty(String.valueOf(cellData.getValue().getIdUtilisateur())));

        TableColumn<Participant, String> eventIdCol = new TableColumn<>("Event ID");
        eventIdCol.setCellValueFactory(cellData -> new SimpleStringProperty(String.valueOf(cellData.getValue().getIdEvennement())));

        TableColumn<Participant, String> dateInsCol = new TableColumn<>("Date Inscription");
        dateInsCol.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getDateInscription().format(DateTimeFormatter.ISO_DATE))
        );

        TableColumn<Participant, String> statutCol = new TableColumn<>("Statut");
        statutCol.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getStatutParticipation()));

        TableColumn<Participant, String> montantCol = new TableColumn<>("Montant Payé");
        montantCol.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getMontantPayee()));

        TableColumn<Participant, String> confirmCol = new TableColumn<>("Confirmation");
        confirmCol.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getConfirmation()));

        TableColumn<Participant, Void> actionParticipantCol = new TableColumn<>("Actions");
        actionParticipantCol.setCellFactory(col -> new TableCell<>() {
            private final Button btnDelete = new Button("Delete");
            private final Button btnUpdate = new Button("Update");
            private final javafx.scene.layout.HBox hbox = new javafx.scene.layout.HBox(10, btnDelete, btnUpdate);

            {
                btnDelete.setOnAction(e -> {
                    Participant p = getTableView().getItems().get(getIndex());
                    try {
                        participantService.delete(p.getIdParticipant());
                        participantTable.getItems().remove(p);
                    } catch (SQLException ex) {
                        ex.printStackTrace();
                        showAlert("Erreur", "Impossible de supprimer le participant !");
                    }
                });

                btnUpdate.setOnAction(e -> {
                    Participant p = getTableView().getItems().get(getIndex());
                    showUpdateParticipantForm(p);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : hbox);
            }
        });

        participantTable.getColumns().addAll(userIdCol, eventIdCol, dateInsCol, statutCol, montantCol, confirmCol, actionParticipantCol);

        try {
            List<Participant> participants = participantService.read();
            participantTable.setItems(FXCollections.observableArrayList(participants));
        } catch (SQLException ex) {
            ex.printStackTrace();
        }

        mainContentVBox.getChildren().addAll(
                new Label("Événnements Agricoles"), evennementTable,
                new Label("Participants"), participantTable
        );
    }

    // ===================== UPDATE FORMS =====================
    private void showUpdateEvenementForm(EvennementAgricole ev, TableView<EvennementAgricole> table) {
        mainContentVBox.getChildren().clear();

        // VBox with fields
        VBox form = new VBox(15);
        form.setStyle("-fx-padding: 20; -fx-background-color: #f0f8ff;");

        TextField txtTitre = new TextField(ev.getTitre());
        TextField txtLieu = new TextField(ev.getLieu());
        TextField txtCapacite = new TextField(String.valueOf(ev.getCapaciteMax()));
        TextField txtFrais = new TextField(String.valueOf(ev.getFraisInscription()));

        Button btnSave = new Button("Save Changes");
        btnSave.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white;");

        btnSave.setOnAction(e -> {
            ev.setTitre(txtTitre.getText());
            ev.setLieu(txtLieu.getText());
            ev.setCapaciteMax(Integer.parseInt(txtCapacite.getText()));
            ev.setFraisInscription(Integer.parseInt(txtFrais.getText()));

            try {
                evennementService.update(ev);
                showGestionEvenements(); // reload table after update
            } catch (SQLException ex) {
                ex.printStackTrace();
                showAlert("Erreur", "Impossible de mettre à jour l'événement !");
            }
        });

        form.getChildren().addAll(
                new Label("Update Événement"), txtTitre, txtLieu, txtCapacite, txtFrais, btnSave
        );

        mainContentVBox.getChildren().add(form);
    }

    private void showUpdateParticipantForm(Participant p) {
        mainContentVBox.getChildren().clear();

        VBox form = new VBox(15);
        form.setStyle("-fx-padding: 20; -fx-background-color: #fff8dc;");

        TextField txtStatut = new TextField(p.getStatutParticipation());
        TextField txtMontant = new TextField(p.getMontantPayee());
        TextField txtConfirm = new TextField(p.getConfirmation());

        Button btnSave = new Button("Save Changes");
        btnSave.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white;");

        btnSave.setOnAction(e -> {
            p.setStatutParticipation(txtStatut.getText());
            p.setMontantPayee(txtMontant.getText());
            p.setConfirmation(txtConfirm.getText());

            try {
                participantService.update(p); // ensure your service has update method
                showGestionEvenements(); // reload tables
            } catch (SQLException ex) {
                ex.printStackTrace();
                showAlert("Erreur", "Impossible de mettre à jour le participant !");
            }
        });

        form.getChildren().addAll(
                new Label("Update Participant"), txtStatut, txtMontant, txtConfirm, btnSave
        );

        mainContentVBox.getChildren().add(form);
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
