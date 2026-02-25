package controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.TextField;
import models.User;
import services.UserService;

import java.io.IOException;
import java.sql.SQLException;

public class AddUserController {
    UserService us = new UserService();
    @FXML
    private TextField ageTf;

    @FXML
    private TextField firstNameTf;

    @FXML
    private TextField lastNameTf;

    @FXML
    void saveUser(ActionEvent event) {
        User u = new User(Integer.parseInt(this.ageTf.getText()), this.firstNameTf.getText(), this.lastNameTf.getText());
        try {
            us.create(u);
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Success");
            alert.setHeaderText("user saved successfully");
            alert.showAndWait();
        } catch (SQLException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText("Error saving user");
            alert.setContentText(e.getMessage());
            alert.showAndWait();
        }
    }


    @FXML
    void navigateShowUsers(ActionEvent event) {
        try {
            Parent root = new FXMLLoader(getClass().getResource("/ShowUsers.fxml")).load();
            ageTf.getScene().setRoot(root);
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText("Error navigating ");
            alert.setContentText(e.getMessage());
            alert.showAndWait();

        }


    }

}
