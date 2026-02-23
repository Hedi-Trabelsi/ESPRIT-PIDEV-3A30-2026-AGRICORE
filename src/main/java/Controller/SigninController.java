package Controller;

import Model.Utilisateur;
import services.FacePPService;
import services.UserService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import org.mindrot.jbcrypt.BCrypt;

import java.util.List;

public class SigninController {

    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private Hyperlink signupLink;
    @FXML private Label errorLabel;

    @FXML
    public void initialize() {
        errorLabel.setVisible(false);
        // Don't set action here since it's already in FXML
    }

    // =========================================
    // NORMAL LOGIN (EMAIL + PASSWORD)
    // =========================================
    @FXML
    private void handleSignin() {
        errorLabel.setVisible(false);

        String email = emailField.getText().trim();
        String passwordEntered = passwordField.getText();

        if (email.isEmpty() || passwordEntered.isEmpty()) {
            showError("Please fill all fields!");
            return;
        }

        try {
            UserService userService = new UserService();
            List<Utilisateur> users = userService.read();
            Utilisateur matchedUser = null;

            // Find user
            for (Utilisateur u : users) {
                if (u.getEmail().equalsIgnoreCase(email) && BCrypt.checkpw(passwordEntered, u.getPassword())) {
                    matchedUser = u;
                    break;
                }
            }

            if (matchedUser != null) {
                // Check if user is admin (role == 0)
                if (matchedUser.getRole() == 0) {
                    openHomePage(matchedUser);
                } else {
                    showError("Access denied! Only administrators can access this application.");
                }
            } else {
                showError("Invalid email or password!");
            }

        } catch (Exception e) {
            e.printStackTrace();
            showError("Error during login!");
        }
    }

    // =========================================
    // FACE LOGIN
    // =========================================
    @FXML
    private void handleFaceLogin() {
        errorLabel.setStyle("-fx-text-fill: #1e88e5;");
        errorLabel.setText("Opening camera...");

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/FaceScan.fxml"));
            Parent root = loader.load();

            FaceScanController controller = loader.getController();
            controller.setFaceCapturedListener(liveImage -> {
                Platform.runLater(() -> errorLabel.setText("Scanning face..."));

                new Thread(() -> {
                    try {
                        UserService userService = new UserService();
                        List<Utilisateur> users = userService.read();

                        Utilisateur matchedUser = null;

                        for (Utilisateur u : users) {
                            byte[] storedImage = u.getImage();
                            if (storedImage == null) continue;

                            double confidence = FacePPService.compareFaces(liveImage, storedImage);
                            if (confidence >= 80.0) {
                                matchedUser = u;
                                break;
                            }
                        }

                        if (matchedUser != null) {
                            final Utilisateur finalUser = matchedUser;
                            Platform.runLater(() -> {
                                // Check if user is admin (role == 0)
                                if (finalUser.getRole() == 0) {
                                    errorLabel.setStyle("-fx-text-fill: green;");
                                    errorLabel.setText("Face recognized! Welcome " + finalUser.getNom());
                                    openHomePage(finalUser);
                                } else {
                                    errorLabel.setStyle("-fx-text-fill: red;");
                                    errorLabel.setText("Access denied! Only administrators can access this application.");
                                }
                            });
                        } else {
                            Platform.runLater(() -> {
                                errorLabel.setStyle("-fx-text-fill: red;");
                                errorLabel.setText("Face not recognized. Try again.");
                            });
                        }

                    } catch (Exception e) {
                        e.printStackTrace();
                        Platform.runLater(() -> showError("Error during face login: " + e.getMessage()));
                    }
                }).start();
            });

            Stage stage = new Stage();
            stage.setTitle("Face Scan");
            stage.setScene(new Scene(root));
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
            showError("Error opening camera: " + e.getMessage());
        }
    }

    // =========================================
    // OPEN FORGOT PASSWORD PAGE
    // =========================================
    @FXML
    private void openForgotPassword() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/forgotpassword.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) emailField.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Forgot Password");
        } catch (Exception e) {
            e.printStackTrace();
            showError("Cannot load Forgot Password page!");
        }
    }

    // =========================================
    // OPEN SIGNUP PAGE
    // =========================================
    @FXML
    private void openSignupPage() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/gestion_utilisateur.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) signupLink.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Sign Up");
        } catch (Exception e) {
            e.printStackTrace();
            showError("Cannot load Sign Up page!");
        }
    }

    // =========================================
    // OPEN HOMEPAGE (ADMIN ONLY)
    // =========================================
    private void openHomePage(Utilisateur user) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/HomePage.fxml"));
            Parent root = loader.load();

            HomeController controller = loader.getController();
            controller.setLoggedInUser(user);

            Stage stage = (Stage) emailField.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Admin Dashboard - " + user.getNom());
        } catch (Exception e) {
            e.printStackTrace();
            showError("Cannot load Home Page!");
        }
    }

    // =========================================
    // SHOW ERROR
    // =========================================
    private void showError(String message) {
        errorLabel.setStyle("-fx-text-fill: red;");
        errorLabel.setText(message);
        errorLabel.setVisible(true);
    }
}