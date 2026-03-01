package Controller;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import Model.Animal;
import services.AnimalService;

import java.sql.Date;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Optional;

public class AdminAnimauxController {

    @FXML private TableView<Animal>            tableAnimaux;
    @FXML private TableColumn<Animal, Integer> colId;
    @FXML private TableColumn<Animal, String>  colCode;
    @FXML private TableColumn<Animal, String>  colEspece;
    @FXML private TableColumn<Animal, String>  colRace;
    @FXML private TableColumn<Animal, String>  colSexe;
    @FXML private TableColumn<Animal, String>  colDate;
    @FXML private TableColumn<Animal, String>  colPoids;
    @FXML private TableColumn<Animal, String>  colActions;

    @FXML private TextField        codeTf;
    @FXML private ComboBox<String> especeCombo;
    @FXML private TextField        raceTf;
    @FXML private ComboBox<String> sexeCombo;
    @FXML private DatePicker       datePicker;
    @FXML private TextField        poidsTf;
    @FXML private TextField        searchTf;
    @FXML private Label            lblFormTitre;
    @FXML private Button           btnSauvegarder;
    @FXML private Label            lblCount;

    private final AnimalService      animalService   = new AnimalService();
    private ObservableList<Animal>   masterList      = FXCollections.observableArrayList();
    private FilteredList<Animal>     filteredList;
    private Animal                   animalEnEdition = null;

    @FXML
    void initialize() {
        configurerColonnes();
        configurerFormulaire();
        chargerDonnees();
        configurerRecherche();
    }

    private void configurerColonnes() {
        colId.setCellValueFactory(new PropertyValueFactory<>("idAnimal"));
        colId.setPrefWidth(50);
        colId.setStyle("-fx-alignment:CENTER;");

        colCode.setCellValueFactory(new PropertyValueFactory<>("codeAnimal"));
        colCode.setPrefWidth(90);

        colEspece.setCellValueFactory(new PropertyValueFactory<>("espece"));
        colEspece.setPrefWidth(90);

        colRace.setCellValueFactory(new PropertyValueFactory<>("race"));
        colRace.setPrefWidth(100);

        colSexe.setCellValueFactory(new PropertyValueFactory<>("sexe"));
        colSexe.setPrefWidth(70);
        colSexe.setStyle("-fx-alignment:CENTER;");

        colDate.setCellValueFactory(c -> {
            Date date = c.getValue().getDateNaissance();
            if (date == null) return new SimpleStringProperty("--");
            return new SimpleStringProperty(new SimpleDateFormat("dd/MM/yyyy").format(date));
        });
        colDate.setPrefWidth(100);

        colPoids.setCellValueFactory(c -> {
            try {
                Object poids = c.getValue().getClass()
                        .getMethod("getPoids").invoke(c.getValue());
                return new SimpleStringProperty(poids != null ? poids + " kg" : "--");
            } catch (Exception ex) {
                return new SimpleStringProperty("--");
            }
        });
        colPoids.setPrefWidth(80);
        colPoids.setStyle("-fx-alignment:CENTER;");

        colActions.setPrefWidth(160);
        colActions.setCellFactory(col -> new TableCell<>() {
            private final Button btnEdit   = new Button("Modifier");
            private final Button btnDelete = new Button("Supprimer");
            private final HBox   box       = new HBox(6, btnEdit, btnDelete);
            {
                btnEdit.setStyle("-fx-background-color:#1565c0;-fx-text-fill:white;"
                        + "-fx-font-size:10px;-fx-padding:4 8;-fx-background-radius:6;-fx-cursor:hand;");
                btnDelete.setStyle("-fx-background-color:#c62828;-fx-text-fill:white;"
                        + "-fx-font-size:10px;-fx-padding:4 8;-fx-background-radius:6;-fx-cursor:hand;");
                btnEdit.setOnAction(e -> {
                    Animal a = getTableView().getItems().get(getIndex());
                    remplirFormulaire(a);
                });
                btnDelete.setOnAction(e -> {
                    Animal a = getTableView().getItems().get(getIndex());
                    confirmerSuppression(a);
                });
            }
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : box);
            }
        });

        tableAnimaux.setStyle("-fx-background-color:white;");
        tableAnimaux.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        tableAnimaux.setRowFactory(tv -> new TableRow<>() {
            @Override protected void updateItem(Animal a, boolean empty) {
                super.updateItem(a, empty);
                if (empty || a == null) setStyle("");
                else if (getIndex() % 2 == 0) setStyle("-fx-background-color:#fafafa;");
                else setStyle("-fx-background-color:white;");
            }
        });
    }

    private void configurerFormulaire() {
        especeCombo.setItems(FXCollections.observableArrayList(
                "Vache", "Cheval", "Mouton", "Chevre", "Porc",
                "Poulet", "Lapin", "Chat", "Chien", "Autre"));
        sexeCombo.setItems(FXCollections.observableArrayList("Male", "Femelle"));
    }

    private void chargerDonnees() {
        try {
            masterList.setAll(animalService.read());
            filteredList = new FilteredList<>(masterList, p -> true);
            tableAnimaux.setItems(filteredList);
            lblCount.setText(masterList.size() + " animal(aux)");
        } catch (SQLException e) { showAlert(e.getMessage()); }
    }

    private void configurerRecherche() {
        searchTf.textProperty().addListener((obs, old, kw) -> {
            filteredList.setPredicate(a -> {
                if (kw == null || kw.isEmpty()) return true;
                String lower = kw.toLowerCase();
                return a.getCodeAnimal().toLowerCase().contains(lower)
                        || a.getEspece().toLowerCase().contains(lower)
                        || a.getRace().toLowerCase().contains(lower)
                        || (a.getSexe() != null && a.getSexe().toLowerCase().contains(lower));
            });
            lblCount.setText(filteredList.size() + " / " + masterList.size() + " animal(aux)");
        });
    }

    private void remplirFormulaire(Animal a) {
        animalEnEdition = a;
        codeTf.setText(a.getCodeAnimal());
        especeCombo.setValue(a.getEspece());
        raceTf.setText(a.getRace());
        sexeCombo.setValue(a.getSexe());
        try {
            Object poids = a.getClass().getMethod("getPoids").invoke(a);
            poidsTf.setText(poids != null ? String.valueOf(poids) : "");
        } catch (Exception ex) {
            poidsTf.clear();
        }
        Date date = a.getDateNaissance();
        if (date != null) {
            datePicker.setValue(date.toLocalDate());
        } else {
            datePicker.setValue(null);
        }
        lblFormTitre.setText("Modifier : " + a.getCodeAnimal());
        btnSauvegarder.setText("Mettre a jour");
        btnSauvegarder.setStyle("-fx-background-color:#f57c00;-fx-text-fill:white;"
                + "-fx-font-weight:bold;-fx-padding:12;-fx-background-radius:10;-fx-cursor:hand;");
    }

    @FXML
    void sauvegarder() {
        if (codeTf.getText().trim().isEmpty()) { showAlert("Code animal obligatoire !"); return; }
        if (especeCombo.getValue() == null)     { showAlert("Espece obligatoire !"); return; }
        if (raceTf.getText().trim().isEmpty())  { showAlert("Race obligatoire !"); return; }

        Date dateNaissance = null;
        if (datePicker.getValue() != null) {
            dateNaissance = Date.valueOf(datePicker.getValue());
        }

        String sexe = sexeCombo.getValue() != null ? sexeCombo.getValue() : "Male";

        try {
            if (animalEnEdition == null) {
                Animal newAnimal = new Animal(
                        0,
                        codeTf.getText().trim(),
                        especeCombo.getValue(),
                        raceTf.getText().trim(),
                        sexe,
                        dateNaissance
                );
                animalService.create(newAnimal);
                showSuccess("Animal ajoute avec succes !");
            } else {
                animalEnEdition.setCodeAnimal(codeTf.getText().trim());
                animalEnEdition.setEspece(especeCombo.getValue());
                animalEnEdition.setRace(raceTf.getText().trim());
                animalEnEdition.setSexe(sexe);
                animalEnEdition.setDateNaissance(dateNaissance);
                animalService.update(animalEnEdition);
                showSuccess("Animal modifie avec succes !");
            }
            resetFormulaire();
            chargerDonnees();
        } catch (SQLException e) { showAlert(e.getMessage()); }
    }

    private void confirmerSuppression(Animal a) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmation suppression");
        confirm.setHeaderText("Supprimer : " + a.getCodeAnimal() + " ?");
        confirm.setContentText("Les suivis lies seront aussi supprimes.");
        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                animalService.delete(a.getIdAnimal());
                chargerDonnees();
                showSuccess("Animal supprime !");
            } catch (SQLException e) { showAlert(e.getMessage()); }
        }
    }

    @FXML
    void resetFormulaire() {
        animalEnEdition = null;
        codeTf.clear(); raceTf.clear(); poidsTf.clear();
        especeCombo.setValue(null);
        sexeCombo.setValue(null);
        datePicker.setValue(null);
        lblFormTitre.setText("Ajouter un animal");
        btnSauvegarder.setText("Ajouter");
        btnSauvegarder.setStyle("-fx-background-color:#2e7d32;-fx-text-fill:white;"
                + "-fx-font-weight:bold;-fx-padding:12;-fx-background-radius:10;-fx-cursor:hand;");
    }

    @FXML void navigateDashboard() { naviguer("/fxml/AdminDashboard.fxml"); }
    @FXML void navigateSuivis()    { naviguer("/fxml/AdminSuivis.fxml"); }

    private void naviguer(String fxml) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(fxml));
            NavigationUtil.loadInContentArea(tableAnimaux, root);
        } catch (Exception e) { showAlert("Navigation: " + e.getMessage()); }
    }

    private void showAlert(String msg) {
        new Alert(Alert.AlertType.ERROR, msg).showAndWait();
    }
    private void showSuccess(String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION, msg);
        a.setHeaderText(null); a.showAndWait();
    }
}
