package Controller;

import Model.Utilisateur;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.io.IOException;

public class ProfileController {

    @FXML private TextField nomField;
    @FXML private TextField prenomField;
    @FXML private TextField emailField;
    @FXML private TextField phoneField;
    @FXML private ComboBox<String> genreBox;
    @FXML private DatePicker datePicker;
    @FXML private Label roleLabel;
    @FXML private Button btnEdit;
    @FXML private Button btnUpdate;

    private Utilisateur user;

    /** Called from SigninController to pass logged-in user */
    public void setUser(Utilisateur user) {
        this.user = user;
        fillFields();
    }

    /** Fill all fields with user info */
    private void fillFields() {
        if (user != null) {
            nomField.setText(user.getNom());
            prenomField.setText(user.getPrenom());
            emailField.setText(user.getEmail());
            phoneField.setText(String.valueOf(user.getPhone()));

            genreBox.getItems().clear();
            genreBox.getItems().addAll("Male", "Female", "Other");
            genreBox.setValue(user.getGenre());

            datePicker.setValue(user.getDateNaissance());

            roleLabel.setText(user.getRole() == 0 ? "Admin" : "User");
        }
    }

    /** Edit button clicked */
    @FXML
    private void handleEdit() {
        nomField.setEditable(true);
        prenomField.setEditable(true);
        emailField.setEditable(true);
        phoneField.setEditable(true);
        genreBox.setDisable(false);
        datePicker.setEditable(true);

        btnUpdate.setVisible(true);
        btnEdit.setDisable(true);
    }

    /** Update button clicked */
    @FXML
    private void handleUpdate() {
        nomField.setEditable(false);
        prenomField.setEditable(false);
        emailField.setEditable(false);
        phoneField.setEditable(false);
        genreBox.setDisable(true);
        datePicker.setEditable(false);

        btnUpdate.setVisible(false);
        btnEdit.setDisable(false);

        saveProfileData();
    }

    /** Save the updated info (implement DB logic) */
    private void saveProfileData() {

        if (user != null) {

            user.setNom(nomField.getText());
            user.setPrenom(prenomField.getText());
            user.setEmail(emailField.getText());
            user.setGenre(genreBox.getValue());
            user.setDateNaissance(datePicker.getValue());

            try {
                int phone = Integer.parseInt(phoneField.getText());
                user.setPhone(phone);
            } catch (NumberFormatException e) {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Invalid Phone");
                alert.setHeaderText(null);
                alert.setContentText("Phone number must contain only digits!");
                alert.showAndWait();
                return;
            }

            System.out.println("Profile updated successfully!");
            // TODO: Add database update here
        }
    }


    /** Logout button action */
    @FXML
    private void handleLogout() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/signin.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) nomField.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Login");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
