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

    public void showSuccess(String message) {
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

    // ========== GOOGLE SIGN-UP METHODS ==========

    public void handleGoogleSignUpSuccess(GoogleSignInService.GoogleUserInfo userInfo) {
        try {
            nomField.setText(userInfo.getFirstName());
            prenomField.setText(userInfo.getLastName());
            emailField.setText(userInfo.getEmail());
            emailField.setDisable(true);

            if (userInfo.getPictureUrl() != null && !userInfo.getPictureUrl().isEmpty()) {
                new Thread(() -> {
                    try {
                        byte[] imageBytes = downloadImageFromUrl(userInfo.getPictureUrl());
                        if (imageBytes != null) {
                            profileImageBytes = imageBytes;
                            Image image = new Image(new ByteArrayInputStream(imageBytes));
                            javafx.application.Platform.runLater(() -> {
                                profileImageView.setImage(image);
                            });
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }).start();
            }

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

    public void handleGoogleSignUpAndGoHome(GoogleSignInService.GoogleUserInfo userInfo) {
        try {
            System.out.println("=== Creating Google user account ===");
            System.out.println("First Name: " + userInfo.getFirstName());
            System.out.println("Last Name: " + userInfo.getLastName());
            System.out.println("Email: " + userInfo.getEmail());

            String randomPassword = BCrypt.hashpw(userInfo.getId() + System.currentTimeMillis(), BCrypt.gensalt());

            byte[] imageBytes = downloadImageFromUrl(userInfo.getPictureUrl());

            if (imageBytes == null) {
                imageBytes = createDefaultAvatar(userInfo.getFirstName());
            }

            // FIX: Use default values instead of null
            LocalDate defaultDate = LocalDate.of(2000, 1, 1); // Default date
            String defaultGenre = "Non spécifié"; // Default genre
            String defaultAdresse = ""; // Empty address
            int defaultPhone = 0; // Default phone

            Utilisateur user = new Utilisateur(
                    userInfo.getFirstName(),
                    userInfo.getLastName(),
                    defaultDate,
                    defaultGenre, // Use default genre instead of null
                    defaultAdresse,
                    defaultPhone,
                    1, // Default role (Agriculteur)
                    userInfo.getEmail(),
                    randomPassword,
                    imageBytes
            );

            UserService userService = new UserService();
            int userId = userService.create(user);
            user.setId(userId);

            System.out.println("User created successfully with ID: " + userId);
            System.out.println("Email: " + user.getEmail());

            openUserHomePageWithNotification(user);

        } catch (Exception e) {
            System.err.println("ERROR creating account: " + e.getMessage());
            e.printStackTrace();
            showError("Error creating account: " + e.getMessage());
        }
    }

    private void openUserHomePageWithNotification(Utilisateur user) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/UserHomePage.fxml"));
            Parent root = loader.load();

            UserHomeController controller = loader.getController();
            controller.setLoggedInUser(user);

            // La notification s'affichera automatiquement grâce à checkForMissingInformation()

            Stage stage = (Stage) signInLink.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("My Farm - " + user.getNom());
            stage.show();

            System.out.println("User home page opened successfully");

        } catch (Exception e) {
            e.printStackTrace();
            showError("Cannot load User Home Page: " + e.getMessage());
        }
    }

    private byte[] downloadImageFromUrl(String imageUrl) {
        if (imageUrl == null || imageUrl.isEmpty()) {
            return null;
        }

        try {
            URL url = new URL(imageUrl);
            try (InputStream in = url.openStream();
                 ByteArrayOutputStream out = new ByteArrayOutputStream()) {

                byte[] buffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = in.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                }
                return out.toByteArray();
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private byte[] createDefaultAvatar(String name) {
        try {
            int size = 100;
            java.awt.image.BufferedImage bufferedImage = new java.awt.image.BufferedImage(size, size, java.awt.image.BufferedImage.TYPE_INT_RGB);
            java.awt.Graphics2D g2d = bufferedImage.createGraphics();

            g2d.setColor(new java.awt.Color(46, 125, 50));
            g2d.fillRect(0, 0, size, size);

            g2d.setColor(java.awt.Color.WHITE);
            g2d.setFont(new java.awt.Font("Arial", java.awt.Font.BOLD, 50));
            java.awt.FontMetrics fm = g2d.getFontMetrics();
            String firstLetter = name != null && !name.isEmpty() ? name.substring(0, 1).toUpperCase() : "?";
            int x = (size - fm.stringWidth(firstLetter)) / 2;
            int y = (size - fm.getHeight()) / 2 + fm.getAscent();
            g2d.drawString(firstLetter, x, y);

            g2d.dispose();

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            javax.imageio.ImageIO.write(bufferedImage, "png", baos);
            return baos.toByteArray();

        } catch (Exception e) {
            e.printStackTrace();
            return new byte[]{0};
        }
    }

    public void openSignInPageWithEmail(String email) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/signin.fxml"));
            Parent root = loader.load();

            SigninController controller = loader.getController();
            controller.prefillEmail(email);

            Stage stage = (Stage) signInLink.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Sign In");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


}