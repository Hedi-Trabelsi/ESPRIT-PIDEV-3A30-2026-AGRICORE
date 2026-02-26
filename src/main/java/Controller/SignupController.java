package Controller;

import javafx.animation.PauseTransition;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.mindrot.jbcrypt.BCrypt;
import Model.Utilisateur;
import services.GoogleSignInService;
import services.UserService;

import java.io.*;
import java.net.URL;
import java.sql.Connection;
import java.time.LocalDate;

public class SignupController {

    @FXML private TextField nomField;
    @FXML private TextField prenomField;
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private DatePicker datePicker;
    @FXML private ComboBox<String> genreBox;
    @FXML private TextField adresseField;
    @FXML private TextField phoneField;
    @FXML private ComboBox<String> roleBox;
    @FXML private Label errorLabel;
    @FXML private Hyperlink signInLink;
    @FXML private CheckBox termsCheckBox;
    @FXML private ImageView profileImageView;
    @FXML private Button uploadImageButton;

    private byte[] profileImageBytes;

    @FXML
    public void initialize() {
        errorLabel.setVisible(false);
        genreBox.getItems().addAll("Male", "Female");
        roleBox.getItems().addAll("Agriculteur", "Technicien", "Fournisseur");
        signInLink.setOnAction(e -> openSignInPage());
    }

    @FXML
    private void handleImageUpload() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choose Profile Picture");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg")
        );

        File file = fileChooser.showOpenDialog(uploadImageButton.getScene().getWindow());
        if (file != null) {
            try (FileInputStream fis = new FileInputStream(file)) {
                profileImageBytes = fis.readAllBytes();
                Image image = new Image(file.toURI().toString());
                profileImageView.setImage(image);

                showSuccess("Image uploaded successfully!");

            } catch (Exception e) {
                e.printStackTrace();
                showError("Failed to load image.");
            }
        }
    }

    @FXML
    private void handleSignup() {
        errorLabel.setVisible(false);

        try {
            // Check terms agreement
            if (!termsCheckBox.isSelected()) {
                showError("Please accept the Terms of Service and Privacy Policy");
                return;
            }

            String nom = nomField.getText().trim();
            String prenom = prenomField.getText().trim();
            String email = emailField.getText().trim();
            String password = passwordField.getText();
            String confirmPassword = confirmPasswordField.getText();
            String genre = genreBox.getValue();
            String adresse = adresseField.getText().trim();
            String phoneStr = phoneField.getText().trim();
            String roleValue = roleBox.getValue();
            LocalDate dateNaissance = datePicker.getValue();

            if (nom.isEmpty() || prenom.isEmpty() || email.isEmpty()
                    || password.isEmpty() || confirmPassword.isEmpty()
                    || genre == null || roleValue == null || dateNaissance == null) {
                showError("Veuillez remplir tous les champs !");
                return;
            }

            if (!password.equals(confirmPassword)) {
                showError("Les mots de passe ne correspondent pas !");
                return;
            }

            int phone;
            try { phone = Integer.parseInt(phoneStr); }
            catch (NumberFormatException e) { showError("Numéro invalide !"); return; }

            int role;
            switch (roleValue) {
                case "Agriculteur" -> role = 1;
                case "Technicien" -> role = 2;
                case "Fournisseur" -> role = 3;
                default -> role = 1;
            }

            String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt());

            // ✅ Create user
            Utilisateur user = new Utilisateur(nom, prenom, dateNaissance, genre, adresse, phone, role, email, hashedPassword, profileImageBytes);

            UserService userService = new UserService();
            userService.create(user);

            showSuccess("Account created successfully! Please wait...");
            PauseTransition pause = new PauseTransition(Duration.seconds(2));
            pause.setOnFinished(event -> openSignInPage());
            pause.play();

        } catch (Exception e) {
            e.printStackTrace();
            showError("Erreur lors de la création !");
        }
    }

    private void showError(String message) {
        errorLabel.getStyleClass().removeAll("success-label");
        if (!errorLabel.getStyleClass().contains("error-label")) errorLabel.getStyleClass().add("error-label");
        errorLabel.setText(message);
        errorLabel.setVisible(true);
    }

    private void showSuccess(String message) {
        errorLabel.getStyleClass().removeAll("error-label");
        if (!errorLabel.getStyleClass().contains("success-label")) errorLabel.getStyleClass().add("success-label");
        errorLabel.setText(message);
        errorLabel.setVisible(true);
    }

    @FXML
    private void openSignInPage() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/signin.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) signInLink.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Sign In");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleGoogleSignUp() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/GoogleSignIn.fxml"));
            Parent root = loader.load();

            GoogleSignInController controller = loader.getController();

            // Pass the signup controller reference
            controller.setSignupController(this);

            Stage stage = new Stage();
            stage.setTitle("Google Sign-Up");
            stage.setScene(new Scene(root));
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
            showError("Cannot open Google Sign-Up: " + e.getMessage());
        }
    }

    // Add this method to be called from GoogleSignInController after successful Google auth
    public void handleGoogleSignUpSuccess(GoogleSignInService.GoogleUserInfo userInfo) {
        try {
            // Pre-fill the form with Google data
            nomField.setText(userInfo.getFirstName());
            prenomField.setText(userInfo.getLastName());
            emailField.setText(userInfo.getEmail());
            emailField.setDisable(true); // Email comes from Google, cannot change

            // Try to load profile picture from Google
            if (userInfo.getPictureUrl() != null && !userInfo.getPictureUrl().isEmpty()) {
                new Thread(() -> {
                    try {
                        URL url = new URL(userInfo.getPictureUrl());
                        Image image = new Image(url.toString());
                        javafx.application.Platform.runLater(() -> {
                            profileImageView.setImage(image);
                        });

                        // Download image bytes for later storage
                        try (InputStream in = url.openStream()) {
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
                }).start();
            }

            // Disable password fields (will use Google login)
            passwordField.setDisable(true);
            passwordField.setPromptText("Google account");
            confirmPasswordField.setDisable(true);
            confirmPasswordField.setPromptText("Google account");

            showSuccess("Google account connected! Please complete your profile.");

        } catch (Exception e) {
            e.printStackTrace();
            showError("Error loading Google profile: " + e.getMessage());
        }
    }
}