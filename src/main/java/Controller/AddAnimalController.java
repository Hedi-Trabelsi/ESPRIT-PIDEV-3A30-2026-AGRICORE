package Controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import Model.Animal;
import Model.Utilisateur;
import services.AnimalService;

import java.sql.Date;
import java.sql.SQLException;

public class AddAnimalController {

    @FXML private TextField codeAnimalTf;
    @FXML private TextField especeTf;
    @FXML private TextField raceTf;
    @FXML private TextField sexeTf;
    @FXML private DatePicker datePicker;

    private AnimalService animalService = new AnimalService();

    @FXML
    public void saveAnimal(ActionEvent event) {
        try {
            if (codeAnimalTf.getText().isEmpty()
                    || especeTf.getText().isEmpty()
                    || raceTf.getText().isEmpty()
                    || sexeTf.getText().isEmpty()
                    || datePicker.getValue() == null) {
                showAlert("Erreur", "Tous les champs sont obligatoires !");
                return;
            }

            Utilisateur currentUser = UserSession.getCurrentUser();
            int userId = currentUser != null ? currentUser.getId() : 0;
            if (userId == 0) {
                showAlert("Erreur", "Aucun utilisateur connecte !");
                return;
            }

            Date sqlDate = Date.valueOf(datePicker.getValue());

            Animal a = new Animal(
                    userId,
                    codeAnimalTf.getText(),
                    especeTf.getText(),
                    raceTf.getText(),
                    sexeTf.getText(),
                    sqlDate
            );

            animalService.create(a);
            showAlert("Succes", "Animal ajoute avec succes !");
            clearFields();

        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Erreur SQL", e.getMessage());
        }
    }

    @FXML
    public void navigateShowAnimals(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/fxml/ShowAnimals.fxml"));
            NavigationUtil.loadInContentArea(codeAnimalTf, root);
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
