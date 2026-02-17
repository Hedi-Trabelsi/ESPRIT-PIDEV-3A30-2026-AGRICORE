package controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import models.Maintenance;
import models.Tache;
import services.ServiceTache;

import java.sql.SQLException;
import java.util.List;
public class TacheMaintenanceController {

    private Maintenance maintenance;

    @FXML
    private Label typeLabel;
    @FXML
    private Label descriptionLabel;
    @FXML
    private ListView<Tache> tacheList;

    public void setMaintenance(Maintenance maintenance) {
        this.maintenance = maintenance;
        loadData();
    }

    private void loadData() {
        typeLabel.setText(maintenance.getType());
        descriptionLabel.setText(maintenance.getDescription());

        // Récupérer les tâches associées à cette maintenance
        // tacheList.getItems().setAll(serviceTache.getTachesByMaintenance(maintenance.getId()));
    }
}
