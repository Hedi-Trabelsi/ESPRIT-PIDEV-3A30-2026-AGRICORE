package controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import models.Animal;
import services.AnimalService;

import java.sql.Date;
import java.sql.SQLException;

public class AddAnimalController {

    @FXML
    private TextField codeAnimalTf;

    @FXML
    private TextField especeTf;

    @FXML
    private TextField raceTf;

    @FXML
    private TextField sexeTf;

    @FXML
    private DatePicker datePicker;

    private AnimalService animalService = new AnimalService();

    @FXML
    public void saveAnimal(ActionEvent event) {

        try {

            // Vérification
            if (codeAnimalTf.getText().isEmpty()
                    || especeTf.getText().isEmpty()
                    || raceTf.getText().isEmpty()
                    || sexeTf.getText().isEmpty()
                    || datePicker.getValue() == null) {

                showAlert("Erreur", "Tous les champs sont obligatoires !");
                return;
            }

            // Conversion LocalDate → SQL Date
            Date sqlDate = Date.valueOf(datePicker.getValue());

            // ⚠️ idAgriculteur = 1 (temporaire)
            Animal a = new Animal(
                    1,
                    codeAnimalTf.getText(),
                    especeTf.getText(),
                    raceTf.getText(),
                    sexeTf.getText(),
                    sqlDate
            );

            animalService.create(a);

            showAlert("Succès", "Animal ajouté avec succès !");

            clearFields();

        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Erreur SQL", e.getMessage());
        }
    }

    @FXML
    public void navigateShowAnimals(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(
                    getClass().getResource("/ShowAnimals.fxml")
            );
            codeAnimalTf.getScene().setRoot(root);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void clearFields() {
        codeAnimalTf.clear();
        especeTf.clear();
        raceTf.clear();
        sexeTf.clear();
        datePicker.setValue(null);
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setContentText(message);
        alert.showAndWait();
    }
}