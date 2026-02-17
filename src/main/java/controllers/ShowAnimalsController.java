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

public class ShowAnimalsController {

    @FXML
    private ListView<Animal> animalListView;

    private final AnimalService as = new AnimalService();
    private ObservableList<Animal> observableList;

    @FXML
    void initialize() {
        refreshData();
        setupListView();
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

            private final VBox content = new VBox();
            private final HBox buttonBox = new HBox(10);

            {
                btnDelete.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white;");
                btnUpdate.setStyle("-fx-background-color: #7ca76f; -fx-text-fill: white;");

                btnDelete.setOnAction(event -> {
                    Animal a = getItem();
                    try {
                        as.delete(a.getIdAnimal());
                        refreshData();
                    } catch (Exception e) {
                        showAlert(e.getMessage());
                    }
                });

                btnUpdate.setOnAction(event -> {
                    try {
                        FXMLLoader loader = new FXMLLoader(getClass().getResource("/UpdateAnimal.fxml"));
                        Parent root = loader.load();

                        UpdateAnimalController controller = loader.getController();
                        controller.setAnimal(getItem());

                        animalListView.getScene().setRoot(root);

                    } catch (Exception e) {
                        showAlert(e.getMessage());
                    }
                });

                buttonBox.getChildren().addAll(btnUpdate, btnDelete);
                content.setSpacing(10);
            }

            @Override
            protected void updateItem(Animal animal, boolean empty) {
                super.updateItem(animal, empty);

                if (empty || animal == null) {
                    setGraphic(null);
                } else {

                    Label code = new Label("Code : " + animal.getCodeAnimal());
                    Label espece = new Label("Espèce : " + animal.getEspece());
                    Label race = new Label("Race : " + animal.getRace());
                    Label sexe = new Label("Sexe : " + animal.getSexe());

                    code.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

                    VBox infoBox = new VBox(5, code, espece, race, sexe);

                    VBox card = new VBox(15, infoBox, buttonBox);
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

    @FXML
    void navigateAddAnimal() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/AddAnimal.fxml"));
            animalListView.getScene().setRoot(root);
        } catch (Exception e) {
            showAlert(e.getMessage());
        }
    }

    private void showAlert(String msg) {
        new Alert(Alert.AlertType.ERROR, msg).show();
    }
}