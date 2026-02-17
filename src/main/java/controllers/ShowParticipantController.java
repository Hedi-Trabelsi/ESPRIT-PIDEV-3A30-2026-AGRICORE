package controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import models.Participant;
import services.ParticipantService;

import java.sql.SQLException;
import java.util.List;

public class ShowParticipantController {

    @FXML private TableView<Participant> tableParticipants;
    @FXML private TableColumn<Participant, Integer> colId;
    @FXML private TableColumn<Participant, Integer> colIdUtilisateur;
    @FXML private TableColumn<Participant, Integer> colIdEvennement;
    @FXML private TableColumn<Participant, String> colDateInscription;
    @FXML private TableColumn<Participant, String> colStatut;
    @FXML private TableColumn<Participant, String> colMontant;
    @FXML private TableColumn<Participant, String> colConfirmation;

    private ParticipantService service = new ParticipantService();

    @FXML
    public void initialize() {
        colId.setCellValueFactory(new PropertyValueFactory<>("idParticipant"));
        colIdUtilisateur.setCellValueFactory(new PropertyValueFactory<>("idUtilisateur"));
        colIdEvennement.setCellValueFactory(new PropertyValueFactory<>("idEvennement"));
        colDateInscription.setCellValueFactory(new PropertyValueFactory<>("dateInscription"));
        colStatut.setCellValueFactory(new PropertyValueFactory<>("statutParticipation"));
        colMontant.setCellValueFactory(new PropertyValueFactory<>("montantPayee"));
        colConfirmation.setCellValueFactory(new PropertyValueFactory<>("confirmation"));
        loadData();
    }

    private void loadData() {
        try {
            List<Participant> list = service.read();
            ObservableList<Participant> data = FXCollections.observableArrayList(list);
            tableParticipants.setItems(data);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
