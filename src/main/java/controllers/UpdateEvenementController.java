package controllers;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import models.EvennementAgricole;
import services.EvennementService;
import javafx.scene.control.TableView;
import javafx.collections.ObservableList;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.LocalTime;

public class UpdateEvenementController {

    @FXML private TextField titreField;
    @FXML private TextField lieuField;
    @FXML private TextField capaciteField;
    @FXML private TextField fraisField;
    @FXML private DatePicker dateDebutPicker;
    @FXML private DatePicker dateFinPicker;

    // Ajout des sélecteurs d'heure pour la précision
    @FXML private ComboBox<Integer> hourDebutCombo;
    @FXML private ComboBox<Integer> minDebutCombo;
    @FXML private ComboBox<Integer> hourFinCombo;
    @FXML private ComboBox<Integer> minFinCombo;

    @FXML private Button saveButton;

    private EvennementAgricole evenement;
    private Stage stage;
    private final EvennementService service = new EvennementService();
    private TableView<EvennementAgricole> table;

    @FXML
    public void initialize() {
        // Initialisation des listes d'heures et minutes
        ObservableList<Integer> hours = FXCollections.observableArrayList();
        for (int i = 0; i < 24; i++) hours.add(i);

        ObservableList<Integer> mins = FXCollections.observableArrayList(0, 15, 30, 45);

        hourDebutCombo.setItems(hours);
        hourFinCombo.setItems(hours);
        minDebutCombo.setItems(mins);
        minFinCombo.setItems(mins);
    }

    public void setEvenement(EvennementAgricole ev, Stage stage, TableView<EvennementAgricole> table) {
        this.evenement = ev;
        this.stage = stage;
        this.table = table;

        // Remplissage des champs texte
        titreField.setText(ev.getTitre());
        lieuField.setText(ev.getLieu());
        capaciteField.setText(String.valueOf(ev.getCapaciteMax()));
        fraisField.setText(String.valueOf(ev.getFraisInscription()));

        // Fix: Séparation Date et Heure pour l'affichage
        dateDebutPicker.setValue(ev.getDateDebut().toLocalDate());
        hourDebutCombo.setValue(ev.getDateDebut().getHour());
        minDebutCombo.setValue(ev.getDateDebut().getMinute());

        dateFinPicker.setValue(ev.getDateFin().toLocalDate());
        hourFinCombo.setValue(ev.getDateFin().getHour());
        minFinCombo.setValue(ev.getDateFin().getMinute());
    }

    @FXML
    private void saveChanges() {
        try {
            evenement.setTitre(titreField.getText());
            evenement.setLieu(lieuField.getText());
            evenement.setCapaciteMax(Integer.parseInt(capaciteField.getText()));
            evenement.setFraisInscription(Integer.parseInt(fraisField.getText()));

            // Fix: Reconstruction du LocalDateTime à partir du Picker + ComboBox
            LocalDateTime debut = LocalDateTime.of(dateDebutPicker.getValue(),
                    LocalTime.of(hourDebutCombo.getValue(), minDebutCombo.getValue()));

            LocalDateTime fin = LocalDateTime.of(dateFinPicker.getValue(),
                    LocalTime.of(hourFinCombo.getValue(), minFinCombo.getValue()));

            evenement.setDateDebut(debut);
            evenement.setDateFin(fin);

            service.update(evenement);

            if (table != null) table.refresh();
            if (stage != null) stage.close();

        } catch (NumberFormatException e) {
            showAlert("Erreur de saisie", "La capacité et les frais doivent être des nombres.");
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Erreur SQL", "Impossible de mettre à jour l'événement !");
        } catch (NullPointerException e) {
            showAlert("Erreur", "Veuillez remplir toutes les dates et heures.");
        }
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}