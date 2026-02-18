package org.example;

import Controller.ProfileController; // ← correct import
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
import java.sql.ResultSet;

public class SigninController {

    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private Hyperlink signupLink;
    @FXML private Label errorLabel;

    @FXML
    public void initialize() {
        errorLabel.setVisible(false);
    }

    @FXML
    private void handleSignin() {
        errorLabel.setVisible(false);

        String email = emailField.getText().trim();
        String password = passwordField.getText();

        if (email.isEmpty() || password.isEmpty()) {
            showError("Veuillez remplir tous les champs !");
            return;
        }

        String sql = "SELECT * FROM user WHERE email = ? AND password = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, email);
            ps.setString(2, password);

            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                int role = rs.getInt("role"); // get role from DB

                if (role == 0) {
                    // Admin role
                    openAdminPage();
                } else {
                    // Normal user
                    openProfilePage();
                }

            } else {
                showError("Email ou mot de passe incorrect !");
            }

        } catch (Exception e) {
            e.printStackTrace();
            showError("Erreur de connexion à la base !");
        }
    }

    @FXML
    private void openSignupPage() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/gestion_utilisateur.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) signupLink.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Sign Up");

        } catch (IOException e) {
            e.printStackTrace();
            showError("Cannot load Sign Up page!");
        }
    }

    private void openProfilePage() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/profile.fxml"));
            Parent root = loader.load();

            // Get logged-in user info
            String sql = "SELECT * FROM user WHERE email = ?";
            Utilisateur user = null;

            try (Connection conn = DatabaseConnection.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {

                ps.setString(1, emailField.getText());
                ResultSet rs = ps.executeQuery();

                if (rs.next()) {
                    user = new Utilisateur();
                    user.setId(rs.getInt("id"));
                    user.setNom(rs.getString("nom"));
                    user.setPrenom(rs.getString("prenom"));
                    user.setEmail(rs.getString("email"));
                    user.setPhone(rs.getInt("numeroT"));
                    user.setRole(rs.getInt("role"));
                    user.setGenre(rs.getString("genre"));
                    user.setDateNaissance(rs.getDate("date").toLocalDate());
                }
            }

            if (user != null) {
                // Pass user to ProfileController
                ProfileController controller = loader.getController();
                controller.setUser(user);
            }

            Stage stage = (Stage) emailField.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Profile");

        } catch (Exception e) {
            e.printStackTrace();
            showError("Cannot load Profile page!");
        }
    }

    private void openAdminPage() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/AdminManagement.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) emailField.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Admin Management");

        } catch (IOException e) {
            e.printStackTrace();
            showError("Cannot load Admin page!");
        }
    }

    private void showError(String message) {
        errorLabel.setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
        errorLabel.setText(message);
        errorLabel.setVisible(true);
    }
}
