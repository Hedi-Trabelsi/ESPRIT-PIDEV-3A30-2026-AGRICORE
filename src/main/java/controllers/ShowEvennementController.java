package controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import models.EvennementAgricole;
import services.EvennementService;

import java.sql.SQLException;
import java.util.List;

public class ShowEvennementController {

    @FXML
    private TableView<EvennementAgricole> tableEvennements;
    @FXML private TableColumn<EvennementAgricole, Integer> colId;
    @FXML private TableColumn<EvennementAgricole, String> colTitre;
    @FXML private TableColumn<EvennementAgricole, String> colLieu;
    @FXML private TableColumn<EvennementAgricole, String> colDateDebut;
    @FXML private TableColumn<EvennementAgricole, String> colDateFin;

    private EvennementService service = new EvennementService();

    @FXML
    public void initialize() {
        colId.setCellValueFactory(new PropertyValueFactory<>("idEvennement"));
        colTitre.setCellValueFactory(new PropertyValueFactory<>("titre"));
        colLieu.setCellValueFactory(new PropertyValueFactory<>("lieu"));
        colDateDebut.setCellValueFactory(new PropertyValueFactory<>("dateDebut"));
        colDateFin.setCellValueFactory(new PropertyValueFactory<>("dateFin"));
        loadData();
    }

    private void loadData() {
        try {
            List<EvennementAgricole> list = service.read();
            ObservableList<EvennementAgricole> data = FXCollections.observableArrayList(list);
            tableEvennements.setItems(data);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
