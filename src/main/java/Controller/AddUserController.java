package Controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.TextField;
import Model.Utilisateur;
import services.UserService;

import java.sql.SQLException;

public class AddUserController {
    UserService us;
    {
        try {
            us = new UserService();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
    @FXML
    private TextField firstNameTf;
    @FXML
    private TextField lastNameTf;
    @FXML
    private TextField emailTf;

    @FXML
    void saveUser(ActionEvent event) {
        Utilisateur u = new Utilisateur();
        u.setPrenom(this.firstNameTf.getText());
        u.setNom(this.lastNameTf.getText());
        u.setEmail(emailTf != null ? emailTf.getText() : null);
        try {
            us.create(u);
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Succes");
            alert.setHeaderText("Utilisateur enregistre avec succes");
            alert.showAndWait();
        } catch (SQLException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Erreur");
            alert.setHeaderText("Erreur lors de l'enregistrement");
            alert.setContentText(e.getMessage());
            alert.showAndWait();
        }
    }


    @FXML
    void navigateShowUsers(ActionEvent event) {
        try {
            Parent root = new FXMLLoader(getClass().getResource("/fxml/ShowUsers.fxml")).load();
            firstNameTf.getScene().setRoot(root);
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Erreur");
            alert.setHeaderText("Erreur de navigation");
            alert.setContentText(e.getMessage());
            alert.showAndWait();

        }


    }

}
