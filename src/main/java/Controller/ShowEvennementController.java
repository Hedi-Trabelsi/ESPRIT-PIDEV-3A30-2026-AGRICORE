package Controller;

import Model.EvennementAgricole;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import services.EvennementService;

import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class ShowEvennementController {

    @FXML private TableView<EvennementAgricole> tableEvennements;
    @FXML private TableColumn<EvennementAgricole, Integer> colId;
    @FXML private TableColumn<EvennementAgricole, String> colTitre;
    @FXML private TableColumn<EvennementAgricole, String> colDescription;
    @FXML private TableColumn<EvennementAgricole, String> colLieu;
    @FXML private TableColumn<EvennementAgricole, String> colDateDebut;
    @FXML private TableColumn<EvennementAgricole, String> colDateFin;
    @FXML private TableColumn<EvennementAgricole, Integer> colCapacite;
    @FXML private TableColumn<EvennementAgricole, Integer> colFrais;
    @FXML private TableColumn<EvennementAgricole, String> colStatut;

    private EvennementService service;
    private final DateTimeFormatter dtfDisplay = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public ShowEvennementController() {
        try {
            this.service = new EvennementService();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void initialize() {
        colId.setCellValueFactory(new PropertyValueFactory<>("idEvennement"));
        colTitre.setCellValueFactory(new PropertyValueFactory<>("titre"));
        colDescription.setCellValueFactory(new PropertyValueFactory<>("description"));
        colLieu.setCellValueFactory(new PropertyValueFactory<>("lieu"));
        colDateDebut.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(
                        cellData.getValue().getDateDebut() != null ?
                                cellData.getValue().getDateDebut().format(dtfDisplay) : ""
                )
        );
        colDateFin.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(
                        cellData.getValue().getDateFin() != null ?
                                cellData.getValue().getDateFin().format(dtfDisplay) : ""
                )
        );
        colCapacite.setCellValueFactory(new PropertyValueFactory<>("capaciteMax"));
        colFrais.setCellValueFactory(new PropertyValueFactory<>("fraisInscription"));
        colStatut.setCellValueFactory(new PropertyValueFactory<>("statut"));

        loadData();
    }

    private void loadData() {
        try {
            List<EvennementAgricole> list = service.read();
            ObservableList<EvennementAgricole> data = FXCollections.observableArrayList(list);
            tableEvennements.setItems(data);
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Erreur", "Impossible de charger les événements: " + e.getMessage());
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