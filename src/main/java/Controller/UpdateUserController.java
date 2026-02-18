package org.example;

import Model.Utilisateur;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;

public class UpdateUserController {

    @FXML private TextField nomField;
    @FXML private TextField prenomField;
    @FXML private DatePicker dateNaissanceField;
    @FXML private TextField adresseField;
    @FXML private TextField phoneField;
    @FXML private ComboBox<String> genreField;
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;

    @FXML private Button updateButton;
    @FXML private Button cancelButton;
    @FXML private Label statusLabel;

    private Utilisateur currentUser; // the user being updated

    // Call this method from AdminController when opening this page
    public void setUser(Utilisateur user) {
        this.currentUser = user;

        // Fill fields with current user data
        nomField.setText(user.getNom());
        prenomField.setText(user.getPrenom());
        dateNaissanceField.setValue(user.getDateNaissance());
        adresseField.setText(user.getAdresse());
        phoneField.setText(String.valueOf(user.getPhone()));
        emailField.setText(user.getEmail());
        passwordField.setText(user.getPassword());

        // ComboBox
        genreField.getItems().addAll("Male", "Female", "Other");
        genreField.setValue(user.getGenre());
    }

    @FXML
    public void initialize() {
        updateButton.setOnAction(e -> updateUser());
        cancelButton.setOnAction(e -> goBackToAdmin());
    }

    private void updateUser() {
        try {
            String sql = "UPDATE user SET nom=?, prenom=?, date=?, adresse=?, numeroT=?, genre=?, email=?, password=? WHERE id=?";

            try (Connection conn = DatabaseConnection.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {

                ps.setString(1, nomField.getText());
                ps.setString(2, prenomField.getText());
                ps.setDate(3, java.sql.Date.valueOf(dateNaissanceField.getValue()));
                ps.setString(4, adresseField.getText());
                ps.setInt(5, Integer.parseInt(phoneField.getText()));
                ps.setString(6, genreField.getValue());
                ps.setString(7, emailField.getText());
                ps.setString(8, passwordField.getText());
                ps.setInt(9, currentUser.getId());

                ps.executeUpdate();
                statusLabel.setText("User updated successfully!");

                // Go back to admin page
                goBackToAdmin();
            }

        } catch (Exception ex) {
            ex.printStackTrace();
            statusLabel.setText("Failed to update user!");
        }
    }

    private void goBackToAdmin() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/AdminManagement.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) updateButton.getScene().getWindow();
            stage.setScene(new Scene(root));
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
}
