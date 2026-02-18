package org.example;

import Model.Utilisateur;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;

public class SignupController implements Initializable {

    @FXML private StackPane rootPane;  // THIS MUST MATCH fx:id in FXML
    @FXML private TextField nomField;
    @FXML private TextField prenomField;
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private DatePicker datePicker;
    @FXML private TextField adresseField;
    @FXML private TextField phoneField;
    @FXML private ComboBox<String> genreBox;
    @FXML private ComboBox<String> roleBox;
    @FXML private Button signupButton;
    @FXML private Hyperlink signInLink;
    @FXML private Label errorLabel;   // Inline error label

    private final Map<String, Integer> roleMap = new HashMap<>();

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        errorLabel.setVisible(false); // hide initially

        // Setup ComboBoxes
        ObservableList<String> genres = FXCollections.observableArrayList("Male", "Female");
        ObservableList<String> roles = FXCollections.observableArrayList("Agriculteur", "Technicien", "Fournisseur");
        genreBox.setItems(genres);
        roleBox.setItems(roles);

        roleMap.put("Agriculteur", 1);
        roleMap.put("Technicien", 2);
        roleMap.put("Fournisseur", 3);

        // Signup button action
        signupButton.setOnAction(event -> handleSignup());

        // Sign In link action
        signInLink.setOnAction(event -> openSignInPage());
    }

    @FXML
    private void handleSignup() {
        errorLabel.setVisible(true);
        errorLabel.setStyle("-fx-text-fill: red; -fx-font-weight: bold;");

        String nom = nomField.getText().trim();
        String prenom = prenomField.getText().trim();
        String email = emailField.getText().trim();
        String password = passwordField.getText();
        String confirmPassword = confirmPasswordField.getText();
        LocalDate dateNaissance = datePicker.getValue();
        String adresse = adresseField.getText().trim();
        String phoneStr = phoneField.getText().trim();
        String genre = genreBox.getValue();
        String roleStr = roleBox.getValue();

        // Validation
        if (nom.isEmpty() || prenom.isEmpty() || email.isEmpty() || password.isEmpty() ||
                confirmPassword.isEmpty() || dateNaissance == null || adresse.isEmpty() ||
                phoneStr.isEmpty() || genre == null || roleStr == null) {
            errorLabel.setText("Veuillez remplir tous les champs !");
            return;
        }

        if (!email.matches("^\\S+@\\S+\\.\\S+$")) {
            errorLabel.setText("Email invalide !");
            return;
        }

        if (!phoneStr.matches("\\d+")) {
            errorLabel.setText("Numéro de téléphone invalide !");
            return;
        }

        if (!password.equals(confirmPassword)) {
            errorLabel.setText("Les mots de passe ne correspondent pas !");
            return;
        }

        int phone = Integer.parseInt(phoneStr);
        int role = roleMap.get(roleStr);

        Utilisateur user = new Utilisateur(nom, prenom, dateNaissance, genre, adresse, phone, role, email, password);

        try {
            insertUserToDB(user);
            errorLabel.setStyle("-fx-text-fill: green; -fx-font-weight: bold;");
            errorLabel.setText("Utilisateur ajouté avec succès !");
            clearFields();
        } catch (SQLException e) {
            e.printStackTrace();
            errorLabel.setText("Erreur lors de l'insertion !");
        }
    }

    private void insertUserToDB(Utilisateur user) throws SQLException {
        String sql = "INSERT INTO user (nom, prenom, date, adresse, numeroT, genre, role, email, password) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, user.getNom());
            ps.setString(2, user.getPrenom());
            ps.setDate(3, java.sql.Date.valueOf(user.getDateNaissance()));
            ps.setString(4, user.getAdresse());
            ps.setInt(5, user.getPhone());
            ps.setString(6, user.getGenre());
            ps.setInt(7, user.getRole());
            ps.setString(8, user.getEmail());
            ps.setString(9, user.getPassword());

            ps.executeUpdate();
        }
    }

    @FXML
    private void openSignInPage() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/signin.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) rootPane.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Sign In");
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            errorLabel.setText("Cannot load Sign In page!");
            errorLabel.setVisible(true);
        }
    }

    private void clearFields() {
        nomField.clear();
        prenomField.clear();
        emailField.clear();
        passwordField.clear();
        confirmPasswordField.clear();
        datePicker.setValue(null);
        adresseField.clear();
        phoneField.clear();
        genreBox.getSelectionModel().clearSelection();
        roleBox.getSelectionModel().clearSelection();
    }
}
