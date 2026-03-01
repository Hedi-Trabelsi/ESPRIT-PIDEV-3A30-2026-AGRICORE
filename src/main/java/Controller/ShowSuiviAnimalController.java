package Controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import Model.Animal;
import Model.SuiviAnimal;
import services.AnimalService;
import services.SuiviAnimalService;

import java.io.IOException;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Comparator;
import java.util.List;

public class ShowSuiviAnimalController {

    @FXML private ListView<SuiviAnimal> suiviListView;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> triCombo;

    private final SuiviAnimalService suiviService  = new SuiviAnimalService();
    private final AnimalService      animalService = new AnimalService();

    private ObservableList<SuiviAnimal> suiviList = FXCollections.observableArrayList();
    private List<Animal> animals;

    @FXML
    public void initialize() {
        try {
            animals = animalService.read();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        triCombo.setItems(FXCollections.observableArrayList(
                "Aucun tri",
                "Date (Plus recent)",
                "Date (Plus ancien)",
                "Temperature (Croissant)",
                "Temperature (Decroissant)",
                "Poids (Croissant)",
                "Poids (Decroissant)",
                "Etat de sante"
        ));
        triCombo.setValue("Aucun tri");
        triCombo.setOnAction(e -> appliquerTri());

        loadData();
        setupListView();
        setupSearch();
    }

    private void appliquerTri() {
        ObservableList<SuiviAnimal> liste = FXCollections.observableArrayList(suiviList);
        switch (triCombo.getValue()) {
            case "Date (Plus recent)"        -> liste.sort(Comparator.comparing(SuiviAnimal::getDateSuivi).reversed());
            case "Date (Plus ancien)"        -> liste.sort(Comparator.comparing(SuiviAnimal::getDateSuivi));
            case "Temperature (Croissant)"   -> liste.sort(Comparator.comparingDouble(SuiviAnimal::getTemperature));
            case "Temperature (Decroissant)" -> liste.sort(Comparator.comparingDouble(SuiviAnimal::getTemperature).reversed());
            case "Poids (Croissant)"         -> liste.sort(Comparator.comparingDouble(SuiviAnimal::getPoids));
            case "Poids (Decroissant)"       -> liste.sort(Comparator.comparingDouble(SuiviAnimal::getPoids).reversed());
            case "Etat de sante"             -> liste.sort(Comparator.comparing(SuiviAnimal::getEtatSante));
            default -> {}
        }
        suiviListView.setItems(liste);
    }

    private void loadData() {
        try {
            suiviList.clear();
            suiviList.addAll(suiviService.read());
            suiviListView.setItems(suiviList);
        } catch (SQLException e) {
            new Alert(Alert.AlertType.ERROR, e.getMessage()).show();
        }
    }

    private void setupListView() {
        suiviListView.setCellFactory(param -> new ListCell<>() {

            private final Button btnEdit   = new Button("Modifier");
            private final Button btnDelete = new Button("Supprimer");
            private final HBox   btnBox    = new HBox(10, btnEdit, btnDelete);

            {
                btnEdit.setStyle("-fx-background-color: #7ca76f; -fx-text-fill: white; -fx-background-radius: 8;");
                btnDelete.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-background-radius: 8;");

                btnEdit.setOnAction(e -> {
                    SuiviAnimal s = getItem();
                    if (s != null) openEditWindow(s);
                });

                btnDelete.setOnAction(e -> {
                    SuiviAnimal s = getItem();
                    if (s != null) {
                        try {
                            suiviService.delete(s.getIdSuivi());
                            loadData();
                        } catch (SQLException ex) {
                            new Alert(Alert.AlertType.ERROR, ex.getMessage()).show();
                        }
                    }
                });
            }

            @Override
            protected void updateItem(SuiviAnimal s, boolean empty) {
                super.updateItem(s, empty);
                if (empty || s == null) {
                    setGraphic(null);
                    return;
                }

                String nomAnimal = animals == null ? "Inconnu" : animals.stream()
                        .filter(a -> a.getIdAnimal() == s.getIdAnimal())
                        .map(a -> a.getCodeAnimal() + " - " + a.getEspece())
                        .findFirst().orElse("Inconnu");

                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm");

                Label lblAnimal = new Label(nomAnimal);
                lblAnimal.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

                Label lblDate   = new Label("Date: " + sdf.format(s.getDateSuivi()));
                Label lblTemp   = new Label("Temp : " + s.getTemperature() + " C");
                Label lblPoids  = new Label("Poids : " + s.getPoids() + " kg");
                Label lblRythme = new Label("Rythme : " + s.getRythmeCardiaque() + " bpm");
                Label lblNiveau = new Label("Niveau : " + s.getNiveauActivite());
                Label lblRem    = new Label(s.getRemarque());
                lblRem.setWrapText(true);

                Label lblEtat = new Label("Etat : " + s.getEtatSante());
                lblEtat.setStyle(switch (s.getEtatSante()) {
                    case "Bon"      -> "-fx-text-fill: #2e7d32; -fx-font-weight: bold;";
                    case "Malade"   -> "-fx-text-fill: #f57c00; -fx-font-weight: bold;";
                    case "Critique" -> "-fx-text-fill: #c62828; -fx-font-weight: bold;";
                    default         -> "";
                });

                HBox ligne1 = new HBox(20, lblTemp, lblPoids, lblRythme);
                HBox ligne2 = new HBox(20, lblNiveau, lblEtat);

                VBox infoBox = new VBox(6, lblAnimal, lblDate, ligne1, ligne2, lblRem, btnBox);

                infoBox.setStyle("""
                        -fx-background-color: white;
                        -fx-padding: 15;
                        -fx-background-radius: 12;
                        -fx-border-color: #e0e0e0;
                        -fx-border-radius: 12;
                        -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 6,0,0,2);
                        """);

                setGraphic(infoBox);
                setStyle("-fx-background-color: transparent;");
            }
        });
    }

    private void setupSearch() {
        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null || newVal.isEmpty()) {
                suiviListView.setItems(suiviList);
                return;
            }
            String kw = newVal.toLowerCase();
            ObservableList<SuiviAnimal> filtered = FXCollections.observableArrayList();
            for (SuiviAnimal s : suiviList) {
                String niveau   = s.getNiveauActivite() == null ? "" : s.getNiveauActivite().toLowerCase();
                String etat     = s.getEtatSante()      == null ? "" : s.getEtatSante().toLowerCase();
                String remarque = s.getRemarque()        == null ? "" : s.getRemarque().toLowerCase();
                if (niveau.contains(kw) || etat.contains(kw) || remarque.contains(kw)
                        || String.valueOf(s.getTemperature()).contains(kw)
                        || String.valueOf(s.getPoids()).contains(kw)
                        || String.valueOf(s.getRythmeCardiaque()).contains(kw)) {
                    filtered.add(s);
                }
            }
            suiviListView.setItems(filtered);
        });
    }

    private void openEditWindow(SuiviAnimal s) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/UpdateSuiviAnimal.fxml"));
            Parent root = loader.load();
            UpdateSuiviAnimalController controller = loader.getController();
            controller.setSuivi(s);
            NavigationUtil.loadInContentArea(suiviListView, root);
        } catch (IOException e) {
            new Alert(Alert.AlertType.ERROR, e.getMessage()).show();
        }
    }

    @FXML
    private void navigateAddSuivi() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/AddSuiviAnimal.fxml"));
            Parent root = loader.load();
            NavigationUtil.loadInContentArea(suiviListView, root);
        } catch (IOException e) {
            new Alert(Alert.AlertType.ERROR, e.getMessage()).show();
        }
    }

    @FXML
    private void navigateStatistiques() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/fxml/StatistiquesSuivi.fxml"));
            NavigationUtil.loadInContentArea(suiviListView, root);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void navigateBack() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/fxml/ShowAnimals.fxml"));
            NavigationUtil.loadInContentArea(suiviListView, root);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
