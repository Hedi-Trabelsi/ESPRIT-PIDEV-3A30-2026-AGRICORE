package Controller;

import services.LocalRedirectServer;
import services.GoogleSignInService;
import services.UserService;
import Model.Utilisateur;
import javafx.application.Platform;
import javafx.concurrent.Worker;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import org.json.JSONObject;

public class GoogleSignInController {

    @FXML private WebView webView;
    @FXML private Label statusLabel;
    @FXML private ProgressIndicator progressIndicator;

    private SigninController signinController;
    private SignupController signupController;
    private LocalRedirectServer redirectServer;
    private boolean isProcessing = false;

    // Google OAuth configuration
    private static final String CLIENT_ID = "1093408491388-26ia28ggqehk4o8a2bc9bgnfppd7qevg.apps.googleusercontent.com";
    private static final String CLIENT_SECRET = "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";
    private static final String TOKEN_URL = "https://oauth2.googleapis.com/token";
    private static final String USER_INFO_URL = "https://www.googleapis.com/oauth2/v3/userinfo";

    @FXML
    public void initialize() {
        statusLabel.setText("Initialisation...");
        startRedirectServer();
    }

    private void startRedirectServer() {
        try {
            redirectServer = new LocalRedirectServer();
            redirectServer.startServer();

            int actualPort = redirectServer.getPort();
            System.out.println("Using redirect port: " + actualPort);

            loadGoogleAuthPage(actualPort);

            new Thread(() -> {
                while (!redirectServer.isCodeReceived() && !isProcessing) {
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                if (isProcessing) return;

                String code = redirectServer.getAuthorizationCode();
                if (code != null) {
                    Platform.runLater(() -> {
                        processAuthorizationCode(code);
                    });
                }
            }).start();

        } catch (IOException e) {
            e.printStackTrace();
            statusLabel.setText("Erreur: Impossible de démarrer le serveur local");
        }
    }

    private void loadGoogleAuthPage(int port) {
        WebEngine webEngine = webView.getEngine();
        progressIndicator.setVisible(true);

        try {
            String scope = URLEncoder.encode("email profile", StandardCharsets.UTF_8);
            String redirectUri = "http://localhost:" + port;

            String googleAuthUrl = "https://accounts.google.com/o/oauth2/v2/auth?" +
                    "client_id=" + CLIENT_ID + "&" +
                    "redirect_uri=" + redirectUri + "&" +
                    "response_type=code&" +
                    "scope=" + scope + "&" +
                    "access_type=offline&" +
                    "prompt=consent";

            System.out.println("Loading Google Auth URL: " + googleAuthUrl);
            webEngine.load(googleAuthUrl);

        } catch (Exception e) {
            e.printStackTrace();
            statusLabel.setText("Erreur: " + e.getMessage());
        }

        webEngine.getLoadWorker().stateProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue == Worker.State.RUNNING) {
                progressIndicator.setVisible(true);
                statusLabel.setText("Chargement...");
            } else if (newValue == Worker.State.SUCCEEDED) {
                progressIndicator.setVisible(false);
                statusLabel.setText("");
            } else if (newValue == Worker.State.FAILED) {
                progressIndicator.setVisible(false);
                statusLabel.setText("Erreur de chargement");
            }
        });
    }

    private void processAuthorizationCode(String code) {
        isProcessing = true;
        statusLabel.setText("Authentification en cours...");

        new Thread(() -> {
            try {
                String accessToken = exchangeCodeForToken(code);

                if (accessToken != null) {
                    GoogleSignInService.GoogleUserInfo userInfo = getUserInfo(accessToken);

                    if (userInfo != null) {
                        Platform.runLater(() -> {
                            try {
                                // Vérifier si l'utilisateur existe déjà dans la base de données
                                UserService userService = new UserService();
                                Utilisateur existingUser = userService.findByEmail(userInfo.getEmail());

                                if (existingUser != null) {
                                    // L'utilisateur existe déjà - rediriger vers la page de connexion
                                    System.out.println("User already exists with email: " + userInfo.getEmail());

                                    // Afficher un message de succès
                                    if (signupController != null) {
                                        signupController.showSuccess("Compte Google déjà existant! Connexion en cours...");
                                    }

                                    // Fermer la fenêtre Google
                                    if (redirectServer != null) {
                                        redirectServer.stopServer();
                                    }
                                    Stage stage = (Stage) webView.getScene().getWindow();
                                    stage.close();

                                    // Rediriger vers la page de connexion avec l'email pré-rempli
                                    if (signinController != null) {
                                        signinController.prefillEmail(userInfo.getEmail());
                                    } else if (signupController != null) {
                                        // Si on vient de signup, aller vers signin
                                        signupController.openSignInPageWithEmail(userInfo.getEmail());
                                    }

                                } else {
                                    // Nouvel utilisateur - procéder à l'inscription
                                    System.out.println("New user with email: " + userInfo.getEmail());

                                    if (signupController != null) {
                                        signupController.handleGoogleSignUpAndGoHome(userInfo);
                                    }

                                    if (redirectServer != null) {
                                        redirectServer.stopServer();
                                    }

                                    Stage stage = (Stage) webView.getScene().getWindow();
                                    stage.close();
                                }

                            } catch (Exception e) {
                                e.printStackTrace();
                                statusLabel.setText("Erreur: " + e.getMessage());
                            }
                        });
                    } else {
                        Platform.runLater(() -> statusLabel.setText("Erreur: Impossible d'obtenir les informations utilisateur"));
                    }
                } else {
                    Platform.runLater(() -> statusLabel.setText("Erreur: Impossible d'obtenir le token d'accès"));
                }

            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> {
                    statusLabel.setText("Erreur lors de l'authentification");
                });
            } finally {
                isProcessing = false;
            }
        }).start();
    }

    private String exchangeCodeForToken(String code) throws IOException {
        int port = redirectServer.getPort();
        String redirectUri = "http://localhost:" + port;

        String params = "code=" + code +
                "&client_id=" + CLIENT_ID +
                "&client_secret=" + CLIENT_SECRET +
                "&redirect_uri=" + redirectUri +
                "&grant_type=authorization_code";

        System.out.println("Exchanging code for token...");

        URL url = new URL(TOKEN_URL);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        conn.setDoOutput(true);

        try (java.io.OutputStream os = conn.getOutputStream()) {
            os.write(params.getBytes());
            os.flush();
        }

        int responseCode = conn.getResponseCode();
        System.out.println("Token response code: " + responseCode);

        if (responseCode == 200) {
            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String inputLine;
            StringBuilder response = new StringBuilder();

            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();

            System.out.println("Token response received");
            JSONObject json = new JSONObject(response.toString());
            return json.getString("access_token");
        } else {
            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
            String inputLine;
            StringBuilder error = new StringBuilder();
            while ((inputLine = in.readLine()) != null) {
                error.append(inputLine);
            }
            in.close();
            System.out.println("Error response: " + error.toString());
        }

        return null;
    }

    private GoogleSignInService.GoogleUserInfo getUserInfo(String accessToken) throws IOException {
        URL url = new URL(USER_INFO_URL);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Authorization", "Bearer " + accessToken);

        int responseCode = conn.getResponseCode();
        if (responseCode == 200) {
            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String inputLine;
            StringBuilder response = new StringBuilder();

            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();

            JSONObject json = new JSONObject(response.toString());

            String id = json.getString("sub");
            String email = json.getString("email");
            String firstName = json.optString("given_name", "");
            String lastName = json.optString("family_name", "");
            String pictureUrl = json.optString("picture", null);
            boolean emailVerified = json.getBoolean("email_verified");

            return new GoogleSignInService.GoogleUserInfo(
                    id, email, firstName, lastName, pictureUrl, emailVerified
            );
        }

        return null;
    }

    public void setSigninController(SigninController controller) {
        this.signinController = controller;
    }

    public void setSignupController(SignupController controller) {
        this.signupController = controller;
    }

    @FXML
    private void handleClose() {
        isProcessing = true;
        if (redirectServer != null) {
            redirectServer.stopServer();
        }
        Stage stage = (Stage) webView.getScene().getWindow();
        stage.close();
    }

    @FXML
    private void handleRetry() {
        isProcessing = false;
        if (redirectServer != null) {
            redirectServer.stopServer();
        }
        statusLabel.setText("Nouvelle tentative...");
        startRedirectServer();
    }
}