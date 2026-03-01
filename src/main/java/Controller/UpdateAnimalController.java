package Controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import Model.Animal;
import services.AnimalService;

import java.sql.Date;

public class UpdateAnimalController {

    @FXML private TextField codeAnimalTf;
    @FXML private TextField especeTf;
    @FXML private TextField raceTf;
    @FXML private TextField sexeTf;
    @FXML private DatePicker datePicker;

    private Animal animal;
    private final AnimalService as = new AnimalService();

    public void setAnimal(Animal a) {
        this.animal = a;
        codeAnimalTf.setText(a.getCodeAnimal());
        especeTf.setText(a.getEspece());
        raceTf.setText(a.getRace());
        sexeTf.setText(a.getSexe());
        datePicker.setValue(a.getDateNaissance().toLocalDate());
    }

    @FXML
    void updateAnimal(ActionEvent event) {
        try {
            animal.setCodeAnimal(codeAnimalTf.getText());
            animal.setEspece(especeTf.getText());
            animal.setRace(raceTf.getText());
            animal.setSexe(sexeTf.getText());
            animal.setDateNaissance(Date.valueOf(datePicker.getValue()));

            as.update(animal);
            navigateShowAnimals();
        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR, e.getMessage()).show();
        }
    }

    @FXML
    void navigateShowAnimals() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/fxml/ShowAnimals.fxml"));
            NavigationUtil.loadInContentArea(codeAnimalTf, root);
        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR, e.getMessage()).show();
        }
    }
}
