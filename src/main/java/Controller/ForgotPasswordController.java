package Controller;

import Model.Utilisateur;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import services.EmailService;
import services.UserService;

import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

public class ForgotPasswordController {

    @FXML private TextField emailField;
    @FXML private TextField codeField;
    @FXML private Label emailDisplayLabel;
    @FXML private Label messageLabel;

    @FXML private VBox emailStep;
    @FXML private VBox codeStep;

    private UserService userService;
    private Map<String, String> verificationCodes = new HashMap<>(); // email -> code
    private Map<String, Long> codeExpiry = new HashMap<>(); // email -> expiry time
    private Map<String, Utilisateur> userMap = new HashMap<>(); // email -> user
    private String currentEmail;
    private static final long CODE_EXPIRY_TIME = 10 * 60 * 1000; // 10 minutes

    @FXML
    public void initialize() {
        try {
            userService = new UserService();
        } catch (Exception e) {
            e.printStackTrace();
        }
        messageLabel.setVisible(false);
    }

    @FXML
    private void handleSendCode() {
        String email = emailField.getText().trim().toLowerCase();

        if (email.isEmpty()) {
            showError("Please enter your email address");
            return;
        }

        try {
            // Check if email exists in database
            Utilisateur user = userService.findByEmail(email);

            if (user == null) {
                showError("No account found with this email address");
                return;
            }

            // Store user for later
            userMap.put(email, user);

            // Generate verification code
            String code = EmailService.generateVerificationCode();

            // Store code with expiry
            verificationCodes.put(email, code);
            codeExpiry.put(email, System.currentTimeMillis() + CODE_EXPIRY_TIME);

            // Send email using Jakarta Mail through EmailService
            boolean sent = EmailService.sendLoginCode(email, code, user.getNom());

            if (sent) {
                currentEmail = email;
                emailDisplayLabel.setText("Code envoye a : " + maskEmail(email));

                // Show code verification step
                emailStep.setVisible(false);
                emailStep.setManaged(false);
                codeStep.setVisible(true);
                codeStep.setManaged(true);
                codeField.clear();

                showSuccess("Verification code sent to your email");

                // Auto-expire code after 10 minutes
                new Timer().schedule(new TimerTask() {
                    @Override
                    public void run() {
                        codeExpiry.remove(email);
                        verificationCodes.remove(email);
                    }
                }, CODE_EXPIRY_TIME);

            } else {
                showError("Failed to send email. Please check your internet connection and Gmail settings.");
            }

        } catch (Exception e) {
            e.printStackTrace();
            showError("Error: " + e.getMessage());
        }
    }

    @FXML
    private void handleVerifyCode() {
        String enteredCode = codeField.getText().trim();

        if (enteredCode.isEmpty()) {
            showError("Please enter the verification code");
            return;
        }

        // Check if code exists and not expired
        String storedCode = verificationCodes.get(currentEmail);
        Long expiryTime = codeExpiry.get(currentEmail);

        if (storedCode == null || expiryTime == null) {
            showError("Code expired or invalid. Please request a new one.");
            return;
        }

        if (System.currentTimeMillis() > expiryTime) {
            verificationCodes.remove(currentEmail);
            codeExpiry.remove(currentEmail);
            showError("Code expired. Please request a new one.");
            return;
        }

        if (storedCode.equals(enteredCode)) {
            // Code verified - log the user in directly
            Utilisateur user = userMap.get(currentEmail);
            if (user != null) {
                // Clear stored data
                verificationCodes.remove(currentEmail);
                codeExpiry.remove(currentEmail);
                userMap.remove(currentEmail);

                // Log the user in
                openHomePage(user);
            } else {
                showError("User not found. Please try again.");
            }
        } else {
            showError("Invalid verification code");
        }
    }

    @FXML
    private void handleResendCode() {
        // Clear old code
        verificationCodes.remove(currentEmail);
        codeExpiry.remove(currentEmail);

        // Generate and send new code
        handleSendCode();
    }

    @FXML
    private void handleBackToEmail() {
        // Go back to email step
        codeStep.setVisible(false);
        codeStep.setManaged(false);
        emailStep.setVisible(true);
        emailStep.setManaged(true);
        messageLabel.setVisible(false);
    }

    @FXML
    private void openSignInPage() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/signin.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) emailField.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Connexion");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void openHomePage(Utilisateur user) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/HomePage.fxml"));
            Parent root = loader.load();

            HomeController controller = loader.getController();
            controller.setLoggedInUser(user);

            Stage stage = (Stage) emailField.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Page d'accueil - " + user.getNom());
        } catch (Exception e) {
            e.printStackTrace();
            showError("Cannot load Home Page!");
        }
    }

    private String maskEmail(String email) {
        int atIndex = email.indexOf('@');
        if (atIndex > 1) {
            String prefix = email.substring(0, atIndex);
            String domain = email.substring(atIndex);
            if (prefix.length() > 2) {
                prefix = prefix.substring(0, 2) + "****" + prefix.substring(prefix.length() - 1);
            }
            return prefix + domain;
        }
        return email;
    }

    private void showError(String message) {
        messageLabel.getStyleClass().removeAll("success-label");
        messageLabel.getStyleClass().add("error-label");
        messageLabel.setText(message);
        messageLabel.setVisible(true);
    }

    private void showSuccess(String message) {
        messageLabel.getStyleClass().removeAll("error-label");
        messageLabel.getStyleClass().add("success-label");
        messageLabel.setText(message);
        messageLabel.setVisible(true);
    }
}