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
import Model.SuiviAnimal;
import services.AnimalService;
import services.SuiviAnimalService;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public class AdminSuivisController {

    @FXML private TableView<SuiviAnimal>               tableSuivis;
    @FXML private TableColumn<SuiviAnimal, Integer>    colId;
    @FXML private TableColumn<SuiviAnimal, String>     colAnimal;
    @FXML private TableColumn<SuiviAnimal, String>     colDate;
    @FXML private TableColumn<SuiviAnimal, String>     colTemp;
    @FXML private TableColumn<SuiviAnimal, String>     colPoids;
    @FXML private TableColumn<SuiviAnimal, String>     colRythme;
    @FXML private TableColumn<SuiviAnimal, String>     colEtat;
    @FXML private TableColumn<SuiviAnimal, String>     colActivite;
    @FXML private TableColumn<SuiviAnimal, String>     colActions;

    @FXML private ComboBox<Animal>  comboAnimal;
    @FXML private TextField         tempTf;
    @FXML private TextField         poidsTf;
    @FXML private TextField         rythmeTf;
    @FXML private ComboBox<String>  etatCombo;
    @FXML private ComboBox<String>  niveauCombo;
    @FXML private TextArea          remarqueTa;
    @FXML private TextField         searchTf;
    @FXML private ComboBox<String>  filtreEtatCombo;
    @FXML private Label             lblFormTitre;
    @FXML private Button            btnSauvegarder;
    @FXML private Label             lblCount;

    private final AnimalService      animalService = new AnimalService();
    private final SuiviAnimalService suiviService  = new SuiviAnimalService();

    private ObservableList<SuiviAnimal> masterList   = FXCollections.observableArrayList();
    private FilteredList<SuiviAnimal>   filteredList;
    private List<Animal>                animaux;
    private SuiviAnimal                 suiviEnEdition = null;

    @FXML
    void initialize() {
        chargerAnimaux();
        configurerColonnes();
        configurerFormulaire();
        chargerDonnees();
        configurerRecherche();
    }

    private void chargerAnimaux() {
        try {
            animaux = animalService.read();
            comboAnimal.setItems(FXCollections.observableArrayList(animaux));
            comboAnimal.setCellFactory(p -> new ListCell<>() {
                @Override protected void updateItem(Animal a, boolean empty) {
                    super.updateItem(a, empty);
                    setText(empty || a == null ? null
                            : a.getCodeAnimal() + " -- " + a.getEspece());
                }
            });
            comboAnimal.setButtonCell(new ListCell<>() {
                @Override protected void updateItem(Animal a, boolean empty) {
                    super.updateItem(a, empty);
                    setText(empty || a == null ? null
                            : a.getCodeAnimal() + " -- " + a.getEspece());
                }
            });
        } catch (SQLException e) { showAlert(e.getMessage()); }
    }

    private void configurerColonnes() {
        colId.setCellValueFactory(new PropertyValueFactory<>("idSuivi"));
        colId.setPrefWidth(45);
        colId.setStyle("-fx-alignment:CENTER;");

        colAnimal.setCellValueFactory(c -> {
            if (animaux == null) return new SimpleStringProperty("--");
            return new SimpleStringProperty(
                    animaux.stream()
                            .filter(a -> a.getIdAnimal() == c.getValue().getIdAnimal())
                            .map(a -> a.getCodeAnimal() + " / " + a.getEspece())
                            .findFirst().orElse("Inconnu"));
        });
        colAnimal.setPrefWidth(130);

        colDate.setCellValueFactory(c ->
                new SimpleStringProperty(
                        new SimpleDateFormat("dd/MM/yy HH:mm").format(c.getValue().getDateSuivi())));
        colDate.setPrefWidth(110);

        colTemp.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getTemperature() + " C"));
        colTemp.setPrefWidth(65);
        colTemp.setStyle("-fx-alignment:CENTER;");

        colPoids.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getPoids() + " kg"));
        colPoids.setPrefWidth(70);
        colPoids.setStyle("-fx-alignment:CENTER;");

        colRythme.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getRythmeCardiaque() + " bpm"));
        colRythme.setPrefWidth(75);
        colRythme.setStyle("-fx-alignment:CENTER;");

        colEtat.setPrefWidth(85);
        colEtat.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getEtatSante()));
        colEtat.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String etat, boolean empty) {
                super.updateItem(etat, empty);
                if (empty || etat == null) { setText(null); setStyle(""); return; }
                setText(etat);
                setStyle(switch (etat) {
                    case "Bon"      -> "-fx-text-fill:#2e7d32;-fx-font-weight:bold;-fx-alignment:CENTER;";
                    case "Malade"   -> "-fx-text-fill:#f57c00;-fx-font-weight:bold;-fx-alignment:CENTER;";
                    case "Critique" -> "-fx-text-fill:#c62828;-fx-font-weight:bold;-fx-alignment:CENTER;";
                    default         -> "-fx-alignment:CENTER;";
                });
            }
        });

        colActivite.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getNiveauActivite() != null
                        ? c.getValue().getNiveauActivite() : "--"));
        colActivite.setPrefWidth(75);
        colActivite.setStyle("-fx-alignment:CENTER;");

        colActions.setPrefWidth(150);
        colActions.setCellFactory(col -> new TableCell<>() {
            private final Button btnEdit   = new Button("Modifier");
            private final Button btnDelete = new Button("Supprimer");
            private final HBox   box       = new HBox(6, btnEdit, btnDelete);
            {
                btnEdit.setStyle("-fx-background-color:#1565c0;-fx-text-fill:white;"
                        + "-fx-padding:5 10;-fx-background-radius:6;-fx-cursor:hand;-fx-font-size:12px;");
                btnDelete.setStyle("-fx-background-color:#c62828;-fx-text-fill:white;"
                        + "-fx-padding:5 10;-fx-background-radius:6;-fx-cursor:hand;-fx-font-size:12px;");
                box.setAlignment(javafx.geometry.Pos.CENTER);
                btnEdit.setOnAction(e -> {
                    SuiviAnimal s = getTableView().getItems().get(getIndex());
                    remplirFormulaire(s);
                });
                btnDelete.setOnAction(e -> {
                    SuiviAnimal s = getTableView().getItems().get(getIndex());
                    confirmerSuppression(s);
                });
            }
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : box);
            }
        });

        tableSuivis.setRowFactory(tv -> new TableRow<>() {
            @Override protected void updateItem(SuiviAnimal s, boolean empty) {
                super.updateItem(s, empty);
                if (empty || s == null) { setStyle(""); return; }
                if ("Critique".equals(s.getEtatSante()))
                    setStyle("-fx-background-color:#fff3f3;");
                else if ("Malade".equals(s.getEtatSante()))
                    setStyle("-fx-background-color:#fff8f0;");
                else if (getIndex() % 2 == 0)
                    setStyle("-fx-background-color:#f9fafb;");
                else
                    setStyle("-fx-background-color:white;");
            }
        });

        tableSuivis.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
    }

    private void configurerFormulaire() {
        etatCombo.setItems(FXCollections.observableArrayList(
                "Bon", "Malade", "Critique", "Convalescent", "En gestation", "En lactation"));
        niveauCombo.setItems(FXCollections.observableArrayList("Faible", "Moyen", "Eleve"));
        filtreEtatCombo.setItems(FXCollections.observableArrayList(
                "Tous", "Bon", "Malade", "Critique", "Convalescent"));
        filtreEtatCombo.setValue("Tous");
        filtreEtatCombo.setOnAction(e -> appliquerFiltres());
    }

    private void chargerDonnees() {
        try {
            masterList.setAll(suiviService.read());
            masterList.sort((a, b) -> b.getDateSuivi().compareTo(a.getDateSuivi()));
            filteredList = new FilteredList<>(masterList, p -> true);
            tableSuivis.setItems(filteredList);
            lblCount.setText(masterList.size() + " suivi(s)");
        } catch (SQLException e) { showAlert(e.getMessage()); }
    }

    private void configurerRecherche() {
        searchTf.textProperty().addListener((obs, old, kw) -> appliquerFiltres());
    }

    private void appliquerFiltres() {
        String kw    = searchTf.getText() == null ? "" : searchTf.getText().toLowerCase();
        String filtre = filtreEtatCombo.getValue();

        filteredList.setPredicate(s -> {
            boolean etatOk = "Tous".equals(filtre) || filtre == null
                    || filtre.equals(s.getEtatSante());
            if (!etatOk) return false;
            if (kw.isEmpty()) return true;
            String nomAnimal = animaux == null ? "" : animaux.stream()
                    .filter(a -> a.getIdAnimal() == s.getIdAnimal())
                    .map(a -> a.getCodeAnimal() + " " + a.getEspece())
                    .findFirst().orElse("").toLowerCase();
            return nomAnimal.contains(kw)
                    || String.valueOf(s.getTemperature()).contains(kw)
                    || String.valueOf(s.getPoids()).contains(kw)
                    || (s.getRemarque() != null && s.getRemarque().toLowerCase().contains(kw))
                    || (s.getEtatSante() != null && s.getEtatSante().toLowerCase().contains(kw));
        });
        lblCount.setText(filteredList.size() + " / " + masterList.size() + " suivi(s)");
    }

    private void remplirFormulaire(SuiviAnimal s) {
        suiviEnEdition = s;
        if (animaux != null) {
            animaux.stream()
                    .filter(a -> a.getIdAnimal() == s.getIdAnimal())
                    .findFirst()
                    .ifPresent(comboAnimal::setValue);
        }
        tempTf.setText(String.valueOf(s.getTemperature()));
        poidsTf.setText(String.valueOf(s.getPoids()));
        rythmeTf.setText(String.valueOf(s.getRythmeCardiaque()));
        etatCombo.setValue(s.getEtatSante());
        niveauCombo.setValue(s.getNiveauActivite());
        remarqueTa.setText(s.getRemarque() != null ? s.getRemarque() : "");
        lblFormTitre.setText("Modifier le suivi #" + s.getIdSuivi());
        btnSauvegarder.setText("Mettre a jour");
        btnSauvegarder.setStyle("-fx-background-color:#f57c00;-fx-text-fill:white;"
                + "-fx-font-weight:bold;-fx-padding:11;-fx-background-radius:10;-fx-cursor:hand;");
        tempTf.requestFocus();
    }

    @FXML
    void sauvegarder() {
        if (comboAnimal.getValue() == null) { showAlert("Choisissez un animal !"); return; }
        if (tempTf.getText().trim().isEmpty()) { showAlert("Temperature obligatoire !"); return; }
        if (poidsTf.getText().trim().isEmpty()) { showAlert("Poids obligatoire !"); return; }
        if (rythmeTf.getText().trim().isEmpty()) { showAlert("Rythme cardiaque obligatoire !"); return; }
        if (etatCombo.getValue() == null) { showAlert("Etat de sante obligatoire !"); return; }

        double temperature, poids;
        int rythme;
        try {
            temperature = Double.parseDouble(tempTf.getText().trim().replace(",", "."));
        } catch (NumberFormatException e) { showAlert("Temperature invalide !"); return; }
        try {
            poids = Double.parseDouble(poidsTf.getText().trim().replace(",", "."));
        } catch (NumberFormatException e) { showAlert("Poids invalide !"); return; }
        try {
            rythme = Integer.parseInt(rythmeTf.getText().trim());
        } catch (NumberFormatException e) { showAlert("Rythme cardiaque invalide !"); return; }

        try {
            if (suiviEnEdition == null) {
                SuiviAnimal newSuivi = new SuiviAnimal(
                        comboAnimal.getValue().getIdAnimal(),
                        Timestamp.valueOf(LocalDateTime.now()),
                        temperature, poids, rythme,
                        niveauCombo.getValue() != null ? niveauCombo.getValue() : "Moyen",
                        etatCombo.getValue(),
                        remarqueTa.getText()
                );
                suiviService.create(newSuivi);
                showSuccess("Suivi ajoute avec succes !");
            } else {
                suiviEnEdition.setIdAnimal(comboAnimal.getValue().getIdAnimal());
                suiviEnEdition.setTemperature(temperature);
                suiviEnEdition.setPoids(poids);
                suiviEnEdition.setRythmeCardiaque(rythme);
                suiviEnEdition.setEtatSante(etatCombo.getValue());
                suiviEnEdition.setNiveauActivite(niveauCombo.getValue());
                suiviEnEdition.setRemarque(remarqueTa.getText());
                suiviService.update(suiviEnEdition);
                showSuccess("Suivi modifie avec succes !");
            }
            resetFormulaire();
            chargerDonnees();
        } catch (SQLException e) { showAlert(e.getMessage()); }
    }

    private void confirmerSuppression(SuiviAnimal s) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmation suppression");
        confirm.setHeaderText("Supprimer le suivi #" + s.getIdSuivi() + " ?");
        confirm.setContentText("Cette action est irreversible.");
        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                suiviService.delete(s.getIdSuivi());
                chargerDonnees();
                showSuccess("Suivi supprime !");
            } catch (SQLException e) { showAlert(e.getMessage()); }
        }
    }

    @FXML
    void resetFormulaire() {
        suiviEnEdition = null;
        comboAnimal.setValue(null);
        tempTf.clear(); poidsTf.clear(); rythmeTf.clear();
        etatCombo.setValue(null); niveauCombo.setValue(null);
        remarqueTa.clear();
        lblFormTitre.setText("Ajouter un suivi");
        btnSauvegarder.setText("Ajouter");
        btnSauvegarder.setStyle("-fx-background-color:#2e7d32;-fx-text-fill:white;"
                + "-fx-font-weight:bold;-fx-padding:11;-fx-background-radius:10;-fx-cursor:hand;");
    }

    @FXML void navigateDashboard() { naviguer("/fxml/AdminDashboard.fxml"); }
    @FXML void navigateAnimaux()   { naviguer("/fxml/AdminAnimaux.fxml"); }

    private void naviguer(String fxml) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(fxml));
            NavigationUtil.loadInContentArea(tableSuivis, root);
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
