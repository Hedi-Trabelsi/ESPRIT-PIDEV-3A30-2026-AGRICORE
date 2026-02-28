package Controller;

import Model.Utilisateur;
import javafx.animation.ScaleTransition;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.Popup;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.ByteArrayInputStream;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class UserHomeController {

    @FXML private StackPane contentArea;
    @FXML private Button notificationsButton;  // Le bouton de notification
    @FXML private Label count;                  // Le label pour le nombre de notifications

    // Navigation Buttons - 6 main sections
    @FXML private Button profileButton;
    @FXML private Button eventButton;
    @FXML private Button financialButton;
    @FXML private Button equipmentButton;
    @FXML private Button maintenanceButton;
    @FXML private Button animalButton;
    @FXML private Button settingsButton;
    @FXML private Button logoutButton;

    // Sidebar Profile Elements
    @FXML private ImageView profileImageView;
    @FXML private Label sidebarNameLabel;
    @FXML private Label sidebarRoleLabel;
    @FXML private Label userMenuLabel;
    @FXML private Label welcomeLabel;

    // Quick action buttons
    @FXML private Button quickProfileButton;
    @FXML private Button quickSettingsButton;
    @FXML private Button quickHelpButton;

    private Utilisateur loggedInUser;
    private boolean profileCompletionNotificationShown = false;
    private List<String> missingFields = new ArrayList<>();
    private Popup notificationPopup;

    @FXML
    public void initialize() {
        System.out.println("UserHomeController initialized");
        notificationPopup = new Popup();
        notificationPopup.setAutoHide(true);
        setupNavigation();
        setupNotificationHandler();
        showProfilePage();
    }

    private void setupNavigation() {
        setActiveButton(profileButton);

        if (profileButton != null) {
            profileButton.setOnAction(e -> {
                setActiveButton(profileButton);
                showProfilePage();
            });
        }

        if (eventButton != null) {
            eventButton.setOnAction(e -> {
                setActiveButton(eventButton);
                loadPage("/fxml/EventPage.fxml", "Event Management");
            });
        }

        if (financialButton != null) {
            financialButton.setOnAction(e -> {
                setActiveButton(financialButton);
                loadPage("/fxml/FinancialPage.fxml", "Financial Management");
            });
        }

        if (equipmentButton != null) {
            equipmentButton.setOnAction(e -> {
                setActiveButton(equipmentButton);
                loadPage("/fxml/EquipmentPage.fxml", "Equipment Management");
            });
        }

        if (maintenanceButton != null) {
            maintenanceButton.setOnAction(e -> {
                setActiveButton(maintenanceButton);
                loadPage("/fxml/MaintenancePage.fxml", "Maintenance");
            });
        }

        if (animalButton != null) {
            animalButton.setOnAction(e -> {
                setActiveButton(animalButton);
                loadPage("/fxml/AnimalPage.fxml", "Animal Management");
            });
        }

        if (settingsButton != null) {
            settingsButton.setOnAction(e -> showComingSoon("Settings"));
        }

        if (logoutButton != null) {
            logoutButton.setOnAction(e -> handleLogout());
        }

        if (quickProfileButton != null) {
            quickProfileButton.setOnAction(e -> {
                setActiveButton(profileButton);
                showProfilePage();
            });
        }

        if (quickSettingsButton != null) {
            quickSettingsButton.setOnAction(e -> showComingSoon("Settings"));
        }

        if (quickHelpButton != null) {
            quickHelpButton.setOnAction(e -> showComingSoon("Help"));
        }
    }

    private void setupNotificationHandler() {
        if (notificationsButton != null) {
            notificationsButton.setOnAction(e -> {
                if (profileCompletionNotificationShown) {
                    showNotificationPopup();
                } else {
                    showComingSoon("Notifications");
                }
            });
        }
    }

    private void showNotificationPopup() {
        if (notificationPopup.isShowing()) {
            notificationPopup.hide();
            return;
        }

        VBox popupContent = new VBox(10);
        popupContent.setStyle("-fx-background-color: white; -fx-background-radius: 10; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 10, 0, 0, 5); -fx-padding: 15; -fx-min-width: 300; -fx-max-width: 350;");

        Label title = new Label("📋 Profil incomplet");
        title.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #1b5e20; -fx-padding: 0 0 5 0;");

        popupContent.getChildren().add(title);

        if (missingFields != null && !missingFields.isEmpty()) {
            for (String field : missingFields) {
                HBox item = new HBox(10);
                item.setAlignment(Pos.CENTER_LEFT);
                item.setStyle("-fx-padding: 8; -fx-background-color: #f9f9f9; -fx-background-radius: 5; -fx-cursor: hand;");

                Label icon = new Label("•");
                icon.setStyle("-fx-text-fill: #c62828; -fx-font-size: 16px;");

                Label text = new Label(field);
                text.setStyle("-fx-font-size: 13px;");

                Label arrow = new Label("→");
                arrow.setStyle("-fx-text-fill: #1b5e20; -fx-font-size: 14px;");

                Region spacer = new Region();
                HBox.setHgrow(spacer, Priority.ALWAYS);

                item.getChildren().addAll(icon, text, spacer, arrow);

                item.setOnMouseEntered(ev -> item.setStyle("-fx-background-color: #e8f5e9; -fx-padding: 8; -fx-background-radius: 5; -fx-cursor: hand;"));
                item.setOnMouseExited(ev -> item.setStyle("-fx-background-color: #f9f9f9; -fx-padding: 8; -fx-background-radius: 5; -fx-cursor: hand;"));

                item.setOnMouseClicked(ev -> {
                    notificationPopup.hide();
                    setActiveButton(profileButton);
                    showProfilePage();
                });

                popupContent.getChildren().add(item);
            }

            Button completeButton = new Button("Compléter le profil");
            completeButton.setStyle("-fx-background-color: #1b5e20; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 10; -fx-background-radius: 20; -fx-cursor: hand; -fx-min-width: 280;");
            completeButton.setOnAction(e -> {
                notificationPopup.hide();
                setActiveButton(profileButton);
                showProfilePage();
            });

            popupContent.getChildren().add(completeButton);
        }

        notificationPopup.getContent().clear();
        notificationPopup.getContent().add(popupContent);

        // Position the popup under the notification button
        double x = notificationsButton.localToScreen(notificationsButton.getBoundsInLocal()).getMinX() - 150;
        double y = notificationsButton.localToScreen(notificationsButton.getBoundsInLocal()).getMaxY() + 5;

        notificationPopup.show(notificationsButton.getScene().getWindow(), x, y);
    }

    private void setActiveButton(Button activeButton) {
        Button[] buttons = {profileButton, eventButton, financialButton,
                equipmentButton, maintenanceButton, animalButton};

        for (Button btn : buttons) {
            if (btn != null) {
                btn.getStyleClass().remove("user-nav-button-active");
                btn.getStyleClass().add("user-nav-button");
            }
        }

        if (activeButton != null) {
            activeButton.getStyleClass().remove("user-nav-button");
            activeButton.getStyleClass().add("user-nav-button-active");
        }
    }

    private void showProfilePage() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/ProfilePage.fxml"));
            Parent page = loader.load();

            ProfilePageController controller = loader.getController();
            controller.setUserData(loggedInUser);
            controller.setUserHomeController(this);

            contentArea.getChildren().setAll(page);

        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "Cannot load profile page: " + e.getMessage());
        }
    }

    private void loadPage(String fxmlPath, String pageName) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent page = loader.load();
            contentArea.getChildren().setAll(page);
            System.out.println("Loaded " + pageName + " page");
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "Cannot load " + pageName + " page: " + e.getMessage());
        }
    }

    public void setLoggedInUser(Utilisateur user) {
        this.loggedInUser = user;

        updateSidebarProfile();
        checkForMissingInformation();
    }

    public void refreshSidebarProfile(Utilisateur updatedUser) {
        this.loggedInUser = updatedUser;
        updateSidebarProfile();
        checkForMissingInformation();
    }

    private void updateSidebarProfile() {
        if (loggedInUser == null) return;

        if (sidebarNameLabel != null) {
            sidebarNameLabel.setText(loggedInUser.getNom() + " " + loggedInUser.getPrenom());
        }

        if (userMenuLabel != null) {
            userMenuLabel.setText(loggedInUser.getNom());
        }

        if (welcomeLabel != null) {
            welcomeLabel.setText("Welcome back, " + loggedInUser.getNom() + "!");
        }

        String roleText;
        switch (loggedInUser.getRole()) {
            case 1: roleText = "Agriculteur"; break;
            case 2: roleText = "Technicien"; break;
            case 3: roleText = "Fournisseur"; break;
            default: roleText = "Farmer";
        }

        if (sidebarRoleLabel != null) {
            sidebarRoleLabel.setText(roleText);
        }

        if (loggedInUser.getImage() != null && loggedInUser.getImage().length > 0) {
            try {
                Image img = new Image(new ByteArrayInputStream(loggedInUser.getImage()));
                if (profileImageView != null) {
                    profileImageView.setImage(img);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void checkForMissingInformation() {
        if (loggedInUser == null) return;

        missingFields.clear();

        if (loggedInUser.getDateNaissance() == null ||
                loggedInUser.getDateNaissance().equals(LocalDate.of(2000, 1, 1))) {
            missingFields.add("Date de naissance");
        }

        if (loggedInUser.getGenre() == null ||
                loggedInUser.getGenre().equals("Non spécifié") ||
                loggedInUser.getGenre().isEmpty()) {
            missingFields.add("Genre");
        }

        if (loggedInUser.getAdresse() == null || loggedInUser.getAdresse().isEmpty()) {
            missingFields.add("Adresse");
        }

        if (loggedInUser.getPhone() == 0) {
            missingFields.add("Numéro de téléphone");
        }

        if (!missingFields.isEmpty()) {
            showProfileCompletionNotificationWithDetails(missingFields);
        } else {
            hideProfileCompletionNotification();
        }
    }

    public void showProfileCompletionNotification() {
        profileCompletionNotificationShown = true;
        if (count != null) {
            count.setText("1");
            count.setVisible(true);
        }

        if (notificationsButton != null) {
            ScaleTransition st = new ScaleTransition(Duration.millis(500), notificationsButton);
            st.setFromX(1.0);
            st.setFromY(1.0);
            st.setToX(1.2);
            st.setToY(1.2);
            st.setCycleCount(3);
            st.setAutoReverse(true);
            st.play();
        }
    }

    public void showProfileCompletionNotificationWithDetails(List<String> missingFields) {
        this.missingFields = missingFields;
        profileCompletionNotificationShown = true;

        if (count != null) {
            count.setText(String.valueOf(missingFields.size()));
            count.setVisible(true);
        }

        if (notificationsButton != null) {
            ScaleTransition st = new ScaleTransition(Duration.millis(500), notificationsButton);
            st.setFromX(1.0);
            st.setFromY(1.0);
            st.setToX(1.2);
            st.setToY(1.2);
            st.setCycleCount(3);
            st.setAutoReverse(true);
            st.play();
        }
    }

    public void hideProfileCompletionNotification() {
        profileCompletionNotificationShown = false;
        if (count != null) {
            count.setVisible(false);
        }
        if (notificationPopup != null && notificationPopup.isShowing()) {
            notificationPopup.hide();
        }
    }

    private void showComingSoon(String feature) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Coming Soon");
        alert.setHeaderText(feature);
        alert.setContentText("This feature will be available soon!");
        alert.showAndWait();
    }

    private void handleLogout() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Logout");
        confirm.setHeaderText("Confirm Logout");
        confirm.setContentText("Are you sure you want to logout?");

        if (confirm.showAndWait().filter(r -> r == ButtonType.OK).isPresent()) {
            try {
                Parent root = FXMLLoader.load(getClass().getResource("/fxml/signin.fxml"));
                Stage stage = (Stage) logoutButton.getScene().getWindow();
                stage.setScene(new Scene(root));
                stage.setTitle("Sign In");
                stage.show();
            } catch (Exception e) {
                e.printStackTrace();
                showAlert("Error", "Failed to logout: " + e.getMessage());
            }
        }
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}