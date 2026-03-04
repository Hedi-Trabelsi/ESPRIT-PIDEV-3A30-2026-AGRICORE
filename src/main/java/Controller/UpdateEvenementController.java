package Controller;

import Model.EvennementAgricole;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import services.EvennementService;

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
    @FXML private TextArea descriptionArea;

    // Ajout des sélecteurs d'heure pour la précision
    @FXML private ComboBox<Integer> hourDebutCombo;
    @FXML private ComboBox<Integer> minDebutCombo;
    @FXML private ComboBox<Integer> hourFinCombo;
    @FXML private ComboBox<Integer> minFinCombo;

    @FXML private Button saveButton;
    @FXML private Button cancelButton;

    private EvennementAgricole evenement;
    private Stage stage;
    private EvennementService service;
    private TableView<EvennementAgricole> table;
    private Runnable onUpdateCallback;

    public UpdateEvenementController() {
        try {
            this.service = new EvennementService();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

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

        // Valeurs par défaut
        if (hourDebutCombo.getValue() == null) hourDebutCombo.setValue(0);
        if (hourFinCombo.getValue() == null) hourFinCombo.setValue(0);
        if (minDebutCombo.getValue() == null) minDebutCombo.setValue(0);
        if (minFinCombo.getValue() == null) minFinCombo.setValue(0);

        cancelButton.setOnAction(e -> closeWindow());
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
        descriptionArea.setText(ev.getDescription());

        // Fix: Séparation Date et Heure pour l'affichage
        if (ev.getDateDebut() != null) {
            dateDebutPicker.setValue(ev.getDateDebut().toLocalDate());
            hourDebutCombo.setValue(ev.getDateDebut().getHour());
            minDebutCombo.setValue(ev.getDateDebut().getMinute());
        }

        if (ev.getDateFin() != null) {
            dateFinPicker.setValue(ev.getDateFin().toLocalDate());
            hourFinCombo.setValue(ev.getDateFin().getHour());
            minFinCombo.setValue(ev.getDateFin().getMinute());
        }
    }

    public void setOnUpdateCallback(Runnable callback) {
        this.onUpdateCallback = callback;
    }

    @FXML
    private void saveChanges() {
        try {
            // Validation des champs
            if (titreField.getText().isEmpty() || lieuField.getText().isEmpty() ||
                    capaciteField.getText().isEmpty() || fraisField.getText().isEmpty() ||
                    dateDebutPicker.getValue() == null || dateFinPicker.getValue() == null) {
                showAlert("Erreur", "Veuillez remplir tous les champs obligatoires.");
                return;
            }

            evenement.setTitre(titreField.getText());
            evenement.setLieu(lieuField.getText());
            evenement.setDescription(descriptionArea.getText());
            evenement.setCapaciteMax(Integer.parseInt(capaciteField.getText()));
            evenement.setFraisInscription(Integer.parseInt(fraisField.getText()));

            // Reconstruction du LocalDateTime à partir du Picker + ComboBox
            LocalDateTime debut = LocalDateTime.of(dateDebutPicker.getValue(),
                    LocalTime.of(hourDebutCombo.getValue(), minDebutCombo.getValue()));

            LocalDateTime fin = LocalDateTime.of(dateFinPicker.getValue(),
                    LocalTime.of(hourFinCombo.getValue(), minFinCombo.getValue()));

            evenement.setDateDebut(debut);
            evenement.setDateFin(fin);
            evenement.setStatut("Actif");

            service.update(evenement);

            if (table != null) table.refresh();
            if (onUpdateCallback != null) onUpdateCallback.run();

            showSuccess("Succès", "Événement mis à jour avec succès!");
            closeWindow();

        } catch (NumberFormatException e) {
            showAlert("Erreur de saisie", "La capacité et les frais doivent être des nombres.");
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Erreur SQL", "Impossible de mettre à jour l'événement: " + e.getMessage());
        } catch (NullPointerException e) {
            showAlert("Erreur", "Veuillez remplir toutes les dates et heures.");
        }
    }

    private void closeWindow() {
        if (stage != null) {
            stage.close();
        }
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private void showSuccess(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}