package Controller;

import javafx.application.Platform;
import javafx.concurrent.Worker;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import services.GoogleSignInService;

public class GoogleSignInController {

    @FXML private WebView webView;
    @FXML private Label statusLabel;
    @FXML private ProgressIndicator progressIndicator;

    private SigninController signinController;
    private SignupController signupController; // Add this for signup

    // Google OAuth configuration
    private static final String CLIENT_ID = "1093408491388-26ia28ggqehk4o8a2bc9bgnfppd7qevg.apps.googleusercontent.com";
    private static final String REDIRECT_URI = "http://localhost:8080";

    @FXML
    public void initialize() {
        statusLabel.setText("Initialisation...");
        setupWebView();
    }

    public void setSigninController(SigninController controller) {
        this.signinController = controller;
    }

    // Add this method for signup
    public void setSignupController(SignupController controller) {
        this.signupController = controller;
    }

    private void setupWebView() {
        WebEngine webEngine = webView.getEngine();

        // Show loading indicator
        progressIndicator.setVisible(true);

        // Load Google Sign-In page
        String googleAuthUrl = "https://accounts.google.com/o/oauth2/v2/auth?" +
                "client_id=" + CLIENT_ID + "&" +
                "redirect_uri=" + REDIRECT_URI + "&" +
                "response_type=code&" +
                "scope=email%20profile&" +
                "access_type=offline";

        webEngine.load(googleAuthUrl);

        // Handle page loading
        webEngine.getLoadWorker().stateProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue == Worker.State.RUNNING) {
                progressIndicator.setVisible(true);
                statusLabel.setText("Chargement...");
            } else if (newValue == Worker.State.SUCCEEDED) {
                progressIndicator.setVisible(false);
                statusLabel.setText("");

                // Check if we got a code in the URL
                String location = webEngine.getLocation();
                if (location.startsWith(REDIRECT_URI)) {
                    handleRedirect(location);
                }
            } else if (newValue == Worker.State.FAILED) {
                progressIndicator.setVisible(false);
                statusLabel.setText("Erreur de chargement");
            }
        });
    }

    private void handleRedirect(String url) {
        // Extract code from URL
        String code = extractCodeFromUrl(url);
        if (code != null) {
            statusLabel.setText("Authentification réussie!");

            // In a real implementation, you would exchange the code for tokens
            // and get user info from Google
            // For demonstration, we'll simulate getting user info

            new Thread(() -> {
                try {
                    // Simulate getting user info from Google
                    // In production, you would make API calls to Google
                    Thread.sleep(1000); // Simulate network delay

                    // This data would come from Google in reality
                    String userId = "123456789";
                    String email = "user@gmail.com";
                    String firstName = "John";
                    String lastName = "Doe";
                    String pictureUrl = "https://lh3.googleusercontent.com/a-/profile-photo.jpg";

                    GoogleSignInService.GoogleUserInfo userInfo =
                            new GoogleSignInService.GoogleUserInfo(
                                    userId, email, firstName, lastName, pictureUrl, true
                            );

                    Platform.runLater(() -> {
                        // ===== HERE IS WHERE YOU ADD THE CODE =====
                        if (signupController != null) {
                            // This is for signup
                            signupController.handleGoogleSignUpSuccess(userInfo);
                        } else if (signinController != null) {
                            // This is for signin (existing code)
                            // You would handle signin here
                        }

                        // Close the window
                        Stage stage = (Stage) webView.getScene().getWindow();
                        stage.close();
                    });

                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }).start();
        }
    }

    private String extractCodeFromUrl(String url) {
        String[] parts = url.split("code=");
        if (parts.length > 1) {
            String codePart = parts[1];
            int ampIndex = codePart.indexOf('&');
            if (ampIndex != -1) {
                return codePart.substring(0, ampIndex);
            }
            return codePart;
        }
        return null;
    }

    @FXML
    private void handleClose() {
        Stage stage = (Stage) webView.getScene().getWindow();
        stage.close();
    }

    @FXML
    private void handleRetry() {
        setupWebView();
    }
}