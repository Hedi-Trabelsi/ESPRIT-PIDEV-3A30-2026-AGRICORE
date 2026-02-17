package controllers;

import javafx.fxml.FXML;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TextField;
import models.EvennementAgricole;
import services.EvennementService;

import java.sql.SQLException;
import java.time.LocalDate;

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
        String titre = tfTitre.getText();
        String desc = tfDescription.getText();
        LocalDate debut = dpDateDebut.getValue();
        LocalDate fin = dpDateFin.getValue();
        String lieu = tfLieu.getText();
        int capacite = Integer.parseInt(tfCapacite.getText());
        int frais = Integer.parseInt(tfFrais.getText());
        String statut = tfStatut.getText();

        EvennementAgricole e = new EvennementAgricole(titre, desc, debut, fin, lieu, capacite, frais, statut);
        try {
            service.create(e);
            System.out.println("Evenement ajouté!");
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }
}
