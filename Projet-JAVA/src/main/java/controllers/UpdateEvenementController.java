package controllers;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import models.EvennementAgricole;
import services.EvennementService;
import javafx.collections.ObservableList;
import javafx.scene.control.TableView;

import java.sql.SQLException;
import java.time.LocalDate;

public class UpdateEvenementController {

    @FXML private TextField titreField;
    @FXML private TextField lieuField;
    @FXML private TextField capaciteField;
    @FXML private TextField fraisField;
    @FXML private DatePicker dateDebutPicker;
    @FXML private DatePicker dateFinPicker;
    @FXML private Button saveButton;

    private EvennementAgricole evenement;
    private Stage stage;
    private final EvennementService service = new EvennementService();
    private TableView<EvennementAgricole> table;

    public void setEvenement(EvennementAgricole ev, Stage stage, TableView<EvennementAgricole> table) {
        this.evenement = ev;
        this.stage = stage;
        this.table = table;

        // fill fields
        titreField.setText(ev.getTitre());
        lieuField.setText(ev.getLieu());
        capaciteField.setText(String.valueOf(ev.getCapaciteMax()));
        fraisField.setText(String.valueOf(ev.getFraisInscription()));
        dateDebutPicker.setValue(ev.getDateDebut());
        dateFinPicker.setValue(ev.getDateFin());
    }

    @FXML
    private void saveChanges() {
        try {
            evenement.setTitre(titreField.getText());
            evenement.setLieu(lieuField.getText());
            evenement.setCapaciteMax(Integer.parseInt(capaciteField.getText()));
            evenement.setFraisInscription(Integer.parseInt(fraisField.getText()));
            evenement.setDateDebut(dateDebutPicker.getValue());
            evenement.setDateFin(dateFinPicker.getValue());

            service.update(evenement);
            table.refresh();
            stage.close();
        } catch (SQLException e) {
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR, "Impossible de mettre à jour l'événement !");
            alert.showAndWait();
        }
    }
}
