package controllers;

import javafx.fxml.FXML;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TextField;
import javafx.scene.control.Alert;
import models.EvennementAgricole;
import services.EvennementService;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class AddEvennementController {

    @FXML private TextField tfTitre;
    @FXML private TextField tfDescription;
    @FXML private DatePicker dpDateDebut;
    @FXML private DatePicker dpDateFin;
    @FXML private TextField tfLieu;
    @FXML private TextField tfCapacite;
    @FXML private TextField tfFrais;
    @FXML private TextField tfStatut;

    private EvennementService service = new EvennementService();

    @FXML
    private void addEvennement() {
        try {
            // Récupération des données de base
            String titre = tfTitre.getText();
            String desc = tfDescription.getText();
            String lieu = tfLieu.getText();
            String statut = tfStatut.getText();

            // Conversion sécurisée des nombres
            int capacite = Integer.parseInt(tfCapacite.getText());
            int frais = Integer.parseInt(tfFrais.getText());

            // FIX: Conversion de LocalDate vers LocalDateTime
            // On récupère la date et on y ajoute une heure par défaut pour satisfaire le modèle
            LocalDateTime debut = dpDateDebut.getValue().atStartOfDay();
            LocalDateTime fin = dpDateFin.getValue().atTime(23, 59);

            // Création de l'objet avec les nouveaux types
            EvennementAgricole e = new EvennementAgricole(titre, desc, debut, fin, lieu, capacite, frais, statut);

            service.create(e);

            // Feedback utilisateur
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Succès");
            alert.setContentText("Événement agricole ajouté avec succès !");
            alert.show();

        } catch (NumberFormatException e) {
            showError("La capacité et les frais doivent être des nombres valides.");
        } catch (NullPointerException e) {
            showError("Veuillez remplir toutes les dates.");
        } catch (SQLException ex) {
            ex.printStackTrace();
            showError("Erreur lors de l'insertion en base de données.");
        }
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.show();
    }
}