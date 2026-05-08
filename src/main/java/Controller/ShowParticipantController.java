package Controller;

import Model.Participant;
import javafx.beans.property.SimpleStringProperty;
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
    @FXML private TableColumn<Participant, String> colNomParticipant;
    @FXML private TableColumn<Participant, Integer> colNbrPlaces;
    // New Columns for Presence Tracking
    @FXML private TableColumn<Participant, Integer> colNbrPresents;
    @FXML private TableColumn<Participant, String> colPercentage;

    @FXML private TableColumn<Participant, String> colStatut;
    @FXML private TableColumn<Participant, String> colConfirmation;
    @FXML private TableColumn<Participant, String> colDateInscription;

    private ParticipantService service;
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public ShowParticipantController() throws SQLException {
        this.service = new ParticipantService();
    }

    @FXML
    public void initialize() {
        // Standard Columns
        colId.setCellValueFactory(new PropertyValueFactory<>("idParticipant"));
        colNomParticipant.setCellValueFactory(new PropertyValueFactory<>("nomParticipant"));
        colNbrPlaces.setCellValueFactory(new PropertyValueFactory<>("nbrPlaces"));

        // --- MULTI-DAY LOGIC COLUMNS ---

        // 1. Total Presents (from the nbr_presents integer field)
        colNbrPresents.setCellValueFactory(new PropertyValueFactory<>("nbrPresents"));

        // 2. Percentage Calculation: (present_count / reserved_places) × 100
        colPercentage.setCellValueFactory(cellData -> {
            Participant p = cellData.getValue();
            if (p.getNbrPlaces() > 0) {
                double pct = (double) p.getNbrPresents() / p.getNbrPlaces() * 100;
                return new SimpleStringProperty(String.format("%.0f%%", pct));
            }
            return new SimpleStringProperty("0%");
        });

        // Formatting and Other Columns
        colStatut.setCellValueFactory(new PropertyValueFactory<>("statutParticipation"));
        colConfirmation.setCellValueFactory(new PropertyValueFactory<>("confirmation"));
        colDateInscription.setCellValueFactory(cellData ->
                new SimpleStringProperty(
                        cellData.getValue().getDateInscription() != null ?
                                cellData.getValue().getDateInscription().format(dateFormatter) : ""
                )
        );

        loadData();
    }

    private void loadData() {
        try {
            // Service.read() should now return Participants with the nbrPresents field populated
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