package Controller;

import Model.Utilisateur;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;

import java.io.ByteArrayInputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class UserHomeController {

    @FXML private StackPane contentArea;

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
    @FXML private Button notificationsButton;

    private Utilisateur loggedInUser;

    @FXML
    public void initialize() {
        System.out.println("UserHomeController initialized");
        setupNavigation();
        // Load profile page by default
        showProfilePage();
    }

    private void setupNavigation() {
        // Set active button for Profile by default
        setActiveButton(profileButton);

        // Profile button
        if (profileButton != null) {
            profileButton.setOnAction(e -> {
                setActiveButton(profileButton);
                showProfilePage();
            });
        }

        // Event button
        if (eventButton != null) {
            eventButton.setOnAction(e -> {
                setActiveButton(eventButton);
                loadPage("/fxml/EventPage.fxml", "Event Management");
            });
        }

        // Financial button
        if (financialButton != null) {
            financialButton.setOnAction(e -> {
                setActiveButton(financialButton);
                loadPage("/fxml/FinancialPage.fxml", "Financial Management");
            });
        }

        // Equipment button
        if (equipmentButton != null) {
            equipmentButton.setOnAction(e -> {
                setActiveButton(equipmentButton);
                loadPage("/fxml/EquipmentPage.fxml", "Equipment Management");
            });
        }

        // Maintenance button
        if (maintenanceButton != null) {
            maintenanceButton.setOnAction(e -> {
                setActiveButton(maintenanceButton);
                loadPage("/fxml/MaintenancePage.fxml", "Maintenance");
            });
        }

        // Animal button
        if (animalButton != null) {
            animalButton.setOnAction(e -> {
                setActiveButton(animalButton);
                loadPage("/fxml/AnimalPage.fxml", "Animal Management");
            });
        }

        // Settings button
        if (settingsButton != null) {
            settingsButton.setOnAction(e -> showComingSoon("Settings"));
        }

        // Logout button
        if (logoutButton != null) {
            logoutButton.setOnAction(e -> handleLogout());
        }

        // Quick action buttons
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

        if (notificationsButton != null) {
            notificationsButton.setOnAction(e -> showComingSoon("Notifications"));
        }
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
            System.out.println("Loading profile page with user: " +
                    (loggedInUser != null ? loggedInUser.getEmail() : "NULL"));

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/ProfilePage.fxml"));
            Parent page = loader.load();

            // Get the controller and pass the user data
            ProfilePageController controller = loader.getController();
            controller.setUserData(loggedInUser);  // Make sure loggedInUser is not null
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
        System.out.println("UserHomeController.setLoggedInUser called with: " +
                (user != null ? user.getEmail() : "NULL"));

        this.loggedInUser = user;

        if (user == null) {
            System.out.println("ERROR: User is null in setLoggedInUser!");
            return;
        }

        // Update sidebar profile
        if (sidebarNameLabel != null) {
            sidebarNameLabel.setText(user.getNom() + " " + user.getPrenom());
        }

        if (userMenuLabel != null) {
            userMenuLabel.setText(user.getNom());
        }

        if (welcomeLabel != null) {
            welcomeLabel.setText("Welcome back, " + user.getNom() + "!");
        }

        // Set role based on user role
        String roleText;
        switch (user.getRole()) {
            case 1: roleText = "Agriculteur"; break;
            case 2: roleText = "Technicien"; break;
            case 3: roleText = "Fournisseur"; break;
            default: roleText = "Farmer";
        }

        if (sidebarRoleLabel != null) {
            sidebarRoleLabel.setText(roleText);
        }

        // Set profile image
        if (user.getImage() != null && user.getImage().length > 0) {
            try {
                Image img = new Image(new ByteArrayInputStream(user.getImage()));
                if (profileImageView != null) {
                    profileImageView.setImage(img);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // Now show profile page with the user data
        showProfilePage();
    }

    // ========== NEW METHOD FOR REFRESHING SIDEBAR ==========
    public void refreshSidebarProfile(Utilisateur updatedUser) {
        this.loggedInUser = updatedUser;

        if (sidebarNameLabel != null) {
            sidebarNameLabel.setText(updatedUser.getNom() + " " + updatedUser.getPrenom());
        }

        if (userMenuLabel != null) {
            userMenuLabel.setText(updatedUser.getNom());
        }

        // Set role based on user role
        String roleText;
        switch (updatedUser.getRole()) {
            case 1: roleText = "Agriculteur"; break;
            case 2: roleText = "Technicien"; break;
            case 3: roleText = "Fournisseur"; break;
            default: roleText = "Farmer";
        }

        if (sidebarRoleLabel != null) {
            sidebarRoleLabel.setText(roleText);
        }

        // Update profile image
        if (updatedUser.getImage() != null && updatedUser.getImage().length > 0) {
            try {
                Image img = new Image(new ByteArrayInputStream(updatedUser.getImage()));
                if (profileImageView != null) {
                    profileImageView.setImage(img);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
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