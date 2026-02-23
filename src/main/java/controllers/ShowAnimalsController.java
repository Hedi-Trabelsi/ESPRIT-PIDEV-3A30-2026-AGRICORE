package controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import models.Animal;
import services.AnimalService;

import java.util.Comparator;

public class ShowAnimalsController {

    @FXML private ListView<Animal> animalListView;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> triCombo; // ✅ NOUVEAU : ComboBox de tri

    private final AnimalService as = new AnimalService();
    private ObservableList<Animal> observableList;

    @FXML
    void initialize() {
        // ✅ Remplir le ComboBox de tri
        triCombo.setItems(FXCollections.observableArrayList(
                "Aucun tri",
                "Espèce (A→Z)",
                "Espèce (Z→A)",
                "Race (A→Z)",
                "Race (Z→A)",
                "Sexe (A→Z)"
        ));
        triCombo.setValue("Aucun tri");
        triCombo.setOnAction(e -> appliquerTri());

        refreshData();
        setupListView();
    }

    // ✅ NOUVEAU : Applique le tri sélectionné
    private void appliquerTri() {
        if (observableList == null) return;

        String choix = triCombo.getValue();
        ObservableList<Animal> liste = FXCollections.observableArrayList(observableList);

        switch (choix) {
            case "Espèce (A→Z)"  -> liste.sort(Comparator.comparing(Animal::getEspece));
            case "Espèce (Z→A)"  -> liste.sort(Comparator.comparing(Animal::getEspece).reversed());
            case "Race (A→Z)"    -> liste.sort(Comparator.comparing(Animal::getRace));
            case "Race (Z→A)"    -> liste.sort(Comparator.comparing(Animal::getRace).reversed());
            case "Sexe (A→Z)"    -> liste.sort(Comparator.comparing(Animal::getSexe));
            default              -> {} // Aucun tri
        }

        animalListView.setItems(liste);
    }

    @FXML
    private void searchAnimals() {
        String keyword = searchField.getText().toLowerCase();
        if (keyword.isEmpty()) {
            refreshData();
            return;
        }
        ObservableList<Animal> filteredList = FXCollections.observableArrayList();
        for (Animal a : observableList) {
            if (a.getCodeAnimal().toLowerCase().contains(keyword)
                    || a.getEspece().toLowerCase().contains(keyword)
                    || a.getRace().toLowerCase().contains(keyword)) {
                filteredList.add(a);
            }
        }
        animalListView.setItems(filteredList);
    }

    private void refreshData() {
        try {
            observableList = FXCollections.observableArrayList(as.read());
            animalListView.setItems(observableList);
        } catch (Exception e) {
            showAlert(e.getMessage());
        }
    }

    private void setupListView() {
        animalListView.setCellFactory(param -> new ListCell<>() {

            private final Button btnDelete = new Button("Supprimer");
            private final Button btnUpdate = new Button("Modifier");
            private final Button btnSuivi  = new Button("Suivi");
            private final HBox buttonBox   = new HBox(10, btnSuivi, btnUpdate, btnDelete);

            {
                btnDelete.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white;");
                btnUpdate.setStyle("-fx-background-color: #7ca76f; -fx-text-fill: white;");
                btnSuivi.setStyle("-fx-background-color: #3498db; -fx-text-fill: white;");

                btnDelete.setOnAction(event -> {
                    Animal a = getItem();
                    if (a == null) return;
                    try {
                        as.delete(a.getIdAnimal());
                        refreshData();
                    } catch (Exception e) {
                        showAlert(e.getMessage());
                    }
                });

                btnUpdate.setOnAction(event -> {
                    Animal a = getItem();
                    if (a == null) return;
                    try {
                        FXMLLoader loader = new FXMLLoader(getClass().getResource("/UpdateAnimal.fxml"));
                        Parent root = loader.load();
                        UpdateAnimalController controller = loader.getController();
                        controller.setAnimal(a);
                        animalListView.getScene().setRoot(root);
                    } catch (Exception e) {
                        showAlert(e.getMessage());
                    }
                });

                btnSuivi.setOnAction(event -> {
                    if (getItem() == null) return;
                    navigateShowSuiviAnimal();
                });
            }

            @Override
            protected void updateItem(Animal animal, boolean empty) {
                super.updateItem(animal, empty);
                if (empty || animal == null) {
                    setGraphic(null);
                } else {
                    Label code   = new Label("Code : "   + animal.getCodeAnimal());
                    Label espece = new Label("Espèce : " + animal.getEspece());
                    Label race   = new Label("Race : "   + animal.getRace());
                    Label sexe   = new Label("Sexe : "   + animal.getSexe());
                    code.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

                    VBox infoBox = new VBox(5, code, espece, race, sexe);
                    VBox card    = new VBox(15, infoBox, buttonBox);
                    card.setStyle("""
                            -fx-background-color: white;
                            -fx-padding: 15;
                            -fx-background-radius: 15;
                            -fx-border-radius: 15;
                            -fx-border-color: #dddddd;
                            -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 5,0,0,2);
                            """);
                    setGraphic(card);
                }
            }
        });
    }
    @FXML void navigateMeteo() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/MeteoSante.fxml"));
            animalListView.getScene().setRoot(root);
        } catch (Exception e) { showAlert(e.getMessage()); }
    }
    @FXML
    void navigateAddAnimal() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/AddAnimal.fxml"));
            animalListView.getScene().setRoot(root);
        } catch (Exception e) {
            showAlert(e.getMessage());
        }
    }

    @FXML
    void navigateShowSuiviAnimal() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/ShowSuiviAnimal.fxml"));
            animalListView.getScene().setRoot(root);
        } catch (Exception e) {
            showAlert(e.getMessage());
        }
    }


    @FXML
    void navigateStatistiques() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/StatistiquesSuivi.fxml"));
            animalListView.getScene().setRoot(root);
        } catch (Exception e) {
            showAlert(e.getMessage());
        }
    }
    @FXML
    void navigateNutrition() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/RecommandationAlimentaire.fxml"));
            animalListView.getScene().setRoot(root);
        } catch (Exception e) { showAlert(e.getMessage()); }
    }
    @FXML void navigateNotifications() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/NotificationDesktop.fxml"));
            animalListView.getScene().setRoot(root);
        } catch (Exception e) { showAlert(e.getMessage()); }
    }

    private void showAlert(String msg) {
        new Alert(Alert.AlertType.ERROR, msg).show();
    }
}