package controllers.admin;

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
import javafx.stage.Stage;
import models.Animal;
import services.AnimalService;

import java.sql.Date;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Optional;

public class AdminAnimauxController {

    // ── TableView ──
    @FXML private TableView<Animal>            tableAnimaux;
    @FXML private TableColumn<Animal, Integer> colId;
    @FXML private TableColumn<Animal, String>  colCode;
    @FXML private TableColumn<Animal, String>  colEspece;
    @FXML private TableColumn<Animal, String>  colRace;
    @FXML private TableColumn<Animal, String>  colSexe;
    @FXML private TableColumn<Animal, String>  colDate;
    @FXML private TableColumn<Animal, String>  colPoids;
    @FXML private TableColumn<Animal, String>  colActions;

    // ── Formulaire ──
    @FXML private TextField        codeTf;
    @FXML private ComboBox<String> especeCombo;
    @FXML private TextField        raceTf;
    @FXML private ComboBox<String> sexeCombo;
    @FXML private DatePicker       datePicker;
    @FXML private TextField        poidsTf;  // affiché mais non lié au modèle
    @FXML private TextField        searchTf;
    @FXML private Label            lblFormTitre;
    @FXML private Button           btnSauvegarder;
    @FXML private Label            lblCount;

    private final AnimalService      animalService   = new AnimalService();
    private ObservableList<Animal>   masterList      = FXCollections.observableArrayList();
    private FilteredList<Animal>     filteredList;
    private Animal                   animalEnEdition = null;

    // ════════════════════════════════════════
    //  INITIALIZE
    // ════════════════════════════════════════
    @FXML
    void initialize() {
        configurerColonnes();
        configurerFormulaire();
        chargerDonnees();
        configurerRecherche();
    }

    // ════════════════════════════════════════
    //  CONFIGURER COLONNES
    // ════════════════════════════════════════
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

        // ✅ Date — via réflexion (java.sql.Date dans le modèle)
        colDate.setCellValueFactory(c -> {
            try {
                Object date = c.getValue().getClass()
                        .getMethod("getDateNaissance").invoke(c.getValue());
                if (date == null) return new SimpleStringProperty("—");
                return new SimpleStringProperty(
                        new SimpleDateFormat("dd/MM/yyyy").format((java.util.Date) date));
            } catch (Exception ex) {
                return new SimpleStringProperty("—");
            }
        });
        colDate.setPrefWidth(100);

        // ✅ Poids — via réflexion (peut ne pas exister dans le modèle)
        colPoids.setCellValueFactory(c -> {
            try {
                Object poids = c.getValue().getClass()
                        .getMethod("getPoids").invoke(c.getValue());
                return new SimpleStringProperty(poids != null ? poids + " kg" : "—");
            } catch (Exception ex) {
                return new SimpleStringProperty("—");
            }
        });
        colPoids.setPrefWidth(80);
        colPoids.setStyle("-fx-alignment:CENTER;");

        // ── Colonne Actions ──
        colActions.setPrefWidth(160);
        colActions.setCellFactory(col -> new TableCell<>() {
            private final Button btnEdit   = new Button("✏️ Modifier");
            private final Button btnDelete = new Button("🗑️ Supprimer");
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
                "Vache", "Cheval", "Mouton", "Chèvre", "Porc",
                "Poulet", "Lapin", "Chat", "Chien", "Autre"));
        sexeCombo.setItems(FXCollections.observableArrayList("Mâle", "Femelle"));
    }

    // ════════════════════════════════════════
    //  CHARGER DONNÉES
    // ════════════════════════════════════════
    private void chargerDonnees() {
        try {
            masterList.setAll(animalService.read());
            filteredList = new FilteredList<>(masterList, p -> true);
            tableAnimaux.setItems(filteredList);
            lblCount.setText(masterList.size() + " animal(aux)");
        } catch (SQLException e) { showAlert(e.getMessage()); }
    }

    // ════════════════════════════════════════
    //  RECHERCHE EN TEMPS RÉEL
    // ════════════════════════════════════════
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

    // ════════════════════════════════════════
    //  REMPLIR FORMULAIRE POUR ÉDITION
    // ════════════════════════════════════════
    private void remplirFormulaire(Animal a) {
        animalEnEdition = a;
        codeTf.setText(a.getCodeAnimal());
        especeCombo.setValue(a.getEspece());
        raceTf.setText(a.getRace());
        sexeCombo.setValue(a.getSexe());

        // Poids via réflexion
        try {
            Object poids = a.getClass().getMethod("getPoids").invoke(a);
            poidsTf.setText(poids != null ? String.valueOf(poids) : "");
        } catch (Exception ex) {
            poidsTf.clear();
        }

        // Date via réflexion (java.sql.Date → LocalDate)
        try {
            Object date = a.getClass().getMethod("getDateNaissance").invoke(a);
            if (date != null) {
                java.time.LocalDate ld = ((java.sql.Date) date).toLocalDate();
                datePicker.setValue(ld);
            } else {
                datePicker.setValue(null);
            }
        } catch (Exception ex) {
            datePicker.setValue(null);
        }

        lblFormTitre.setText("✏️ Modifier : " + a.getCodeAnimal());
        btnSauvegarder.setText("💾 Mettre à jour");
        btnSauvegarder.setStyle("-fx-background-color:#f57c00;-fx-text-fill:white;"
                + "-fx-font-weight:bold;-fx-padding:12;-fx-background-radius:10;-fx-cursor:hand;");
    }

    // ════════════════════════════════════════
    //  SAUVEGARDER (Ajouter OU Modifier)
    // ════════════════════════════════════════
    @FXML
    void sauvegarder() {
        if (codeTf.getText().trim().isEmpty()) { showAlert("Code animal obligatoire !"); return; }
        if (especeCombo.getValue() == null)     { showAlert("Espèce obligatoire !"); return; }
        if (raceTf.getText().trim().isEmpty())  { showAlert("Race obligatoire !"); return; }

        // ✅ Convertir LocalDate → java.sql.Date (type du modèle)
        Date dateNaissance = null;
        if (datePicker.getValue() != null) {
            dateNaissance = Date.valueOf(datePicker.getValue());
        }

        String sexe = sexeCombo.getValue() != null ? sexeCombo.getValue() : "Mâle";

        try {
            if (animalEnEdition == null) {
                // ════════════════════════════════
                //  ✅ AJOUTER — Constructeur réel :
                //  Animal(int, String, String, String, String, java.sql.Date)
                //  → on passe 0 pour l'id (auto-increment)
                // ════════════════════════════════
                Animal newAnimal = new Animal(
                        0,                              // id auto-généré par MySQL
                        codeTf.getText().trim(),        // codeAnimal
                        especeCombo.getValue(),         // espece
                        raceTf.getText().trim(),        // race
                        sexe,                           // sexe
                        dateNaissance                   // java.sql.Date
                );
                animalService.create(newAnimal);
                showSuccess("✅ Animal ajouté avec succès !");

            } else {
                // ════════════════════════════════
                //  ✅ MODIFIER — Setters directs disponibles
                // ════════════════════════════════
                animalEnEdition.setCodeAnimal(codeTf.getText().trim());
                animalEnEdition.setEspece(especeCombo.getValue());
                animalEnEdition.setRace(raceTf.getText().trim());
                animalEnEdition.setSexe(sexe);
                animalEnEdition.setDateNaissance(dateNaissance);

                animalService.update(animalEnEdition);
                showSuccess("✅ Animal modifié avec succès !");
            }

            resetFormulaire();
            chargerDonnees();

        } catch (SQLException e) { showAlert(e.getMessage()); }
    }

    // ════════════════════════════════════════
    //  SUPPRIMER
    // ════════════════════════════════════════
    private void confirmerSuppression(Animal a) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmation suppression");
        confirm.setHeaderText("Supprimer : " + a.getCodeAnimal() + " ?");
        confirm.setContentText("⚠️ Les suivis liés seront aussi supprimés.");

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                animalService.delete(a.getIdAnimal());
                chargerDonnees();
                showSuccess("✅ Animal supprimé !");
            } catch (SQLException e) { showAlert(e.getMessage()); }
        }
    }

    // ════════════════════════════════════════
    //  RESET FORMULAIRE
    // ════════════════════════════════════════
    @FXML
    void resetFormulaire() {
        animalEnEdition = null;
        codeTf.clear(); raceTf.clear(); poidsTf.clear();
        especeCombo.setValue(null);
        sexeCombo.setValue(null);
        datePicker.setValue(null);
        lblFormTitre.setText("➕ Ajouter un animal");
        btnSauvegarder.setText("➕ Ajouter");
        btnSauvegarder.setStyle("-fx-background-color:#2e7d32;-fx-text-fill:white;"
                + "-fx-font-weight:bold;-fx-padding:12;-fx-background-radius:10;-fx-cursor:hand;");
    }

    // ════════════════════════════════════════
    //  NAVIGATION
    // ════════════════════════════════════════
    @FXML void navigateDashboard() { naviguer("/admin/AdminDashboard.fxml"); }
    @FXML void navigateSuivis()    { naviguer("/admin/AdminSuivis.fxml"); }

    private void naviguer(String fxml) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(fxml));
            Stage stage = (Stage) tableAnimaux.getScene().getWindow();
            stage.getScene().setRoot(root);
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