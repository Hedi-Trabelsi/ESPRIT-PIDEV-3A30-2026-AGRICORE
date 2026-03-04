package Controller;

import Model.Utilisateur;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;
import services.GoogleSignInService;
import services.UserService;
import org.mindrot.jbcrypt.BCrypt;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.time.LocalDate;

public class GoogleSignupCompleteController {

    @FXML private Label welcomeLabel;
    @FXML private Label emailLabel;
    @FXML private ImageView profileImageView;
    @FXML private TextField nomField;
    @FXML private TextField prenomField;
    @FXML private DatePicker datePicker;
    @FXML private ComboBox<String> genreBox;
    @FXML private TextField adresseField;
    @FXML private TextField phoneField;
    @FXML private ComboBox<String> roleBox;
    @FXML private Label errorLabel;

    private GoogleSignInService.GoogleUserInfo googleUser;
    private SigninController signinController;
    private byte[] profileImageBytes;

    @FXML
    public void initialize() {
        errorLabel.setVisible(false);
        genreBox.getItems().addAll("Male", "Female");
        roleBox.getItems().addAll("Agriculteur", "Technicien", "Fournisseur");

        // Pre-fill with Google data
        nomField.setText(googleUser != null ? googleUser.getFirstName() : "");
        prenomField.setText(googleUser != null ? googleUser.getLastName() : "");
    }

    public void setGoogleUserInfo(GoogleSignInService.GoogleUserInfo userInfo) {
        this.googleUser = userInfo;

        welcomeLabel.setText("Bienvenue " + userInfo.getFullName() + "!");
        emailLabel.setText("Email: " + userInfo.getEmail());

        // Pre-fill fields
        nomField.setText(userInfo.getFirstName());
        prenomField.setText(userInfo.getLastName());

        // Load profile picture from Google
        if (userInfo.getPictureUrl() != null && !userInfo.getPictureUrl().isEmpty()) {
            try {
                URL url = new URL(userInfo.getPictureUrl());
                try (InputStream in = url.openStream()) {
                    Image image = new Image(in);
                    profileImageView.setImage(image);

                    // Download image bytes for storage
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    byte[] buffer = new byte[1024];
                    int bytesRead;
                    while ((bytesRead = in.read(buffer)) != -1) {
                        baos.write(buffer, 0, bytesRead);
                    }
                    profileImageBytes = baos.toByteArray();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void setSigninController(SigninController controller) {
        this.signinController = controller;
    }

    @FXML
    private void handleCompleteSignup() {
        errorLabel.setVisible(false);

        try {
            String nom = nomField.getText().trim();
            String prenom = prenomField.getText().trim();
            String genre = genreBox.getValue();
            String adresse = adresseField.getText().trim();
            String phoneStr = phoneField.getText().trim();
            String roleValue = roleBox.getValue();
            LocalDate dateNaissance = datePicker.getValue();

            if (nom.isEmpty() || prenom.isEmpty() || genre == null ||
                    roleValue == null || dateNaissance == null) {
                showError("Veuillez remplir tous les champs obligatoires!");
                return;
            }

            int phone = 0;
            if (!phoneStr.isEmpty()) {
                try {
                    phone = Integer.parseInt(phoneStr);
                } catch (NumberFormatException e) {
                    showError("Numéro de téléphone invalide!");
                    return;
                }
            }

            int role;
            switch (roleValue) {
                case "Agriculteur": role = 1; break;
                case "Technicien": role = 2; break;
                case "Fournisseur": role = 3; break;
                default: role = 1;
            }

            // Generate a random password for Google users (they'll use Google Sign-In)
            String randomPassword = BCrypt.hashpw(googleUser.getId() + System.currentTimeMillis(), BCrypt.gensalt());

            // Create user
            Utilisateur user = new Utilisateur(
                    nom,
                    prenom,
                    dateNaissance,
                    genre,
                    adresse,
                    phone,
                    role,
                    googleUser.getEmail(),
                    randomPassword,
                    profileImageBytes
            );

            UserService userService = new UserService();
            userService.create(user);

            showSuccess("Compte créé avec succès!");

            // Log the user in
            if (role == 0) {
                signinController.openHomePage(user);
            } else {
                signinController.openUserHomePage(user);
            }

            // Close completion window
            Stage stage = (Stage) nomField.getScene().getWindow();
            stage.close();

        } catch (Exception e) {
            e.printStackTrace();
            showError("Erreur lors de la création du compte: " + e.getMessage());
        }
    }

    @FXML
    private void handleSkip() {
        try {
            // Create user with minimal info
            String randomPassword = BCrypt.hashpw(googleUser.getId() + System.currentTimeMillis(), BCrypt.gensalt());

            Utilisateur user = new Utilisateur(
                    googleUser.getFirstName(),
                    googleUser.getLastName(),
                    LocalDate.now().minusYears(20), // Default birth date
                    "Non spécifié",
                    "",
                    0,
                    1, // Default role: Agriculteur
                    googleUser.getEmail(),
                    randomPassword,
                    profileImageBytes
            );

            UserService userService = new UserService();
            userService.create(user);

            // Log the user in as Agriculteur
            signinController.openUserHomePage(user);

            Stage stage = (Stage) nomField.getScene().getWindow();
            stage.close();

        } catch (Exception e) {
            e.printStackTrace();
            showError("Erreur: " + e.getMessage());
        }
    }

    private void showError(String message) {
        errorLabel.getStyleClass().removeAll("success-label");
        errorLabel.getStyleClass().add("error-label");
        errorLabel.setText(message);
        errorLabel.setVisible(true);
    }

    private void showSuccess(String message) {
        errorLabel.getStyleClass().removeAll("error-label");
        errorLabel.getStyleClass().add("success-label");
        errorLabel.setText(message);
        errorLabel.setVisible(true);
    }
}