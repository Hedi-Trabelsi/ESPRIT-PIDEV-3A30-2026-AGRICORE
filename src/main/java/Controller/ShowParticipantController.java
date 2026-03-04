package Controller;

import Model.Participant;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import services.ParticipantService;

import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
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
    @FXML private TableColumn<Participant, Integer> colNbrPlaces;
    @FXML private TableColumn<Participant, String> colNomParticipant;

    private ParticipantService service;
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public ShowParticipantController() throws SQLException {
        this.service = new ParticipantService();
    }

    @FXML
    public void initialize() {
        colId.setCellValueFactory(new PropertyValueFactory<>("idParticipant"));
        colIdUtilisateur.setCellValueFactory(new PropertyValueFactory<>("idUtilisateur"));
        colIdEvennement.setCellValueFactory(new PropertyValueFactory<>("idEvennement"));
        colDateInscription.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(
                        cellData.getValue().getDateInscription() != null ?
                                cellData.getValue().getDateInscription().format(dateFormatter) : ""
                )
        );
        colStatut.setCellValueFactory(new PropertyValueFactory<>("statutParticipation"));
        colMontant.setCellValueFactory(new PropertyValueFactory<>("montantPayee"));
        colConfirmation.setCellValueFactory(new PropertyValueFactory<>("confirmation"));
        colNbrPlaces.setCellValueFactory(new PropertyValueFactory<>("nbrPlaces"));
        colNomParticipant.setCellValueFactory(new PropertyValueFactory<>("nomParticipant"));

        loadData();
    }

    private void loadData() {
        try {
            List<Participant> list = service.read();
            ObservableList<Participant> data = FXCollections.observableArrayList(list);
            tableParticipants.setItems(data);
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Erreur", "Impossible de charger les participants: " + e.getMessage());
        }
    }

    private void showAlert(String title, String content) {
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}