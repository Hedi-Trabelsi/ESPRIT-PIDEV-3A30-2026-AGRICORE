package Controller;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import Model.Maintenance;
import Model.Tache;

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

        // Recuperer les tâches associees a cette maintenance
        // tacheList.getItems().setAll(serviceTache.getTachesByMaintenance(maintenance.getId()));
    }
}
