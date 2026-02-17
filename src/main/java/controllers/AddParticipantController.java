package controllers;

import javafx.fxml.FXML;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TextField;
import models.Participant;
import services.ParticipantService;

import java.sql.SQLException;
import java.time.LocalDate;

public class AddParticipantController {

    @FXML private TextField tfIdUtilisateur;
    @FXML private TextField tfIdEvennement;
    @FXML private DatePicker dpDateInscription;
    @FXML private TextField tfStatut;
    @FXML private TextField tfMontant;
    @FXML private TextField tfConfirmation;

    private ParticipantService service = new ParticipantService();

    @FXML
    private void addParticipant() {
        int idUser = Integer.parseInt(tfIdUtilisateur.getText());
        int idEven = Integer.parseInt(tfIdEvennement.getText());
        LocalDate date = dpDateInscription.getValue();
        String statut = tfStatut.getText();
        String montant = tfMontant.getText();
        String conf = tfConfirmation.getText();

        Participant p = new Participant(idUser, idEven, date, statut, montant, conf);
        try {
            service.create(p);
            System.out.println("Participant ajouté !");
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }
}
