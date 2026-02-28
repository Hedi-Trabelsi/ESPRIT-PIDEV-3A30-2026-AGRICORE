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

    void openUserHomePage(Utilisateur user) {
        try {
            System.out.println("Opening User Home Page for: " + user.getEmail());

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/UserHomePage.fxml"));
            Parent root = loader.load();

            UserHomeController controller = loader.getController();
            controller.setLoggedInUser(user);  // This must be called with non-null user

            Stage stage = (Stage) emailField.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("My Farm - " + user.getNom());
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
            showError("Cannot load User Home Page: " + e.getMessage());
        }
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
                int role = matchedUser.getRole();
                if (role == 0 || role == 3) {
                    // Admin or Fournisseur - open backend
                    openHomePage(matchedUser);
                } else {
                    // Agriculteur or Technicien - open frontend
                    openUserHomePage(matchedUser);
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
                                int role = finalUser.getRole();
                                errorLabel.setStyle("-fx-text-fill: green;");
                                errorLabel.setText("Face recognized! Welcome " + finalUser.getNom());
                                if (role == 0 || role == 3) {
                                    // Admin or Fournisseur - open backend
                                    openHomePage(finalUser);
                                } else {
                                    // Agriculteur or Technicien - open frontend
                                    openUserHomePage(finalUser);
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

    @FXML
    private void handleGoogleSignIn() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/GoogleSignIn.fxml"));
            Parent root = loader.load();

            GoogleSignInController controller = loader.getController();
            controller.setSigninController(this);

            Stage stage = new Stage();
            stage.setTitle("Google Sign-In");
            stage.setScene(new Scene(root));
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
            showError("Cannot open Google Sign-In: " + e.getMessage());
        }
    }

    public void showSuccess(String message) {
        errorLabel.setStyle("-fx-text-fill: green;");
        errorLabel.setText(message);
        errorLabel.setVisible(true);
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
    void openHomePage(Utilisateur user) {
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

    public void prefillEmail(String email) {
        emailField.setText(email);
        showSuccess("Compte Google trouvé! Veuillez vous connecter avec votre mot de passe.");
    }
}