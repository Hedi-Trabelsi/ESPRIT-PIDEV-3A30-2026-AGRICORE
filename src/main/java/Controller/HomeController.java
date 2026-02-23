package Controller;

import Model.Utilisateur;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import services.UserService;

import java.io.ByteArrayInputStream;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Optional;

public class HomeController {

    // Profile Section
    @FXML private Label nameLabel, emailLabel, roleLabel, userMenuLabel, welcomeLabel;
    @FXML private ImageView profileImageView;

    // Navigation Buttons
    @FXML private Button dashboardButton, manageUsersButton, financialButton,
            animalButton, equipmentButton, eventButton, maintenanceButton,
            logoutButton, settingsButton;

    // Main content area
    @FXML private BorderPane mainBorderPane;

    private Utilisateur loggedInUser;
    private UserService userService;

    @FXML
    public void initialize() {
        try {
            userService = new UserService();
            setupNavigation();
        } catch (SQLException e) {
            showAlert("Database Error", "Failed to connect to database: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void setupNavigation() {
        // Set active button for User Management by default
        setActiveButton(manageUsersButton);

        // Load User Management by default
        loadUserManagement();

        // Navigation handlers
        dashboardButton.setOnAction(e -> {
            setActiveButton(dashboardButton);
            loadDashboard();
        });

        manageUsersButton.setOnAction(e -> {
            setActiveButton(manageUsersButton);
            loadUserManagement();
        });

        financialButton.setOnAction(e -> {
            setActiveButton(financialButton);
            loadFinancialManagement();
        });

        animalButton.setOnAction(e -> {
            setActiveButton(animalButton);
            loadAnimalManagement();
        });

        equipmentButton.setOnAction(e -> {
            setActiveButton(equipmentButton);
            loadEquipmentManagement();
        });

        eventButton.setOnAction(e -> {
            setActiveButton(eventButton);
            loadEventManagement();
        });

        maintenanceButton.setOnAction(e -> {
            setActiveButton(maintenanceButton);
            loadMaintenanceManagement();
        });

        logoutButton.setOnAction(e -> handleLogout());
        settingsButton.setOnAction(e -> showComingSoon("Settings"));
    }

    private void setActiveButton(Button activeButton) {
        // Remove active class from all buttons
        Button[] buttons = {dashboardButton, manageUsersButton, financialButton,
                animalButton, equipmentButton, eventButton, maintenanceButton};

        for (Button btn : buttons) {
            if (btn != null) {
                btn.getStyleClass().remove("nav-button-active");
                btn.getStyleClass().add("nav-button");
            }
        }

        // Add active class to selected button
        if (activeButton != null) {
            activeButton.getStyleClass().remove("nav-button");
            activeButton.getStyleClass().add("nav-button-active");
        }
    }

    private void loadDashboard() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Dashboard.fxml"));
            Parent dashboardView = loader.load();
            mainBorderPane.setCenter(dashboardView);
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "Cannot load Dashboard page!", Alert.AlertType.ERROR);
        }
    }

    private void loadUserManagement() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/UserManagement.fxml"));
            Parent userView = loader.load();

            UserManagementController controller = loader.getController();
            controller.setLoggedInUser(loggedInUser);
            controller.setHomeController(this);

            mainBorderPane.setCenter(userView);

            // Force a small delay to ensure the view is properly rendered
            Platform.runLater(() -> {
                // The controller already loads users in setLoggedInUser
            });
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "Cannot load User Management page!", Alert.AlertType.ERROR);
        }
    }

    private void loadFinancialManagement() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/FinancialManagement.fxml"));
            Parent financialView = loader.load();
            mainBorderPane.setCenter(financialView);
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "Cannot load Financial Management page!", Alert.AlertType.ERROR);
        }
    }

    private void loadAnimalManagement() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/AnimalManagement.fxml"));
            Parent animalView = loader.load();
            mainBorderPane.setCenter(animalView);
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "Cannot load Animal Management page!", Alert.AlertType.ERROR);
        }
    }

    private void loadEquipmentManagement() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/EquipmentManagement.fxml"));
            Parent equipmentView = loader.load();
            mainBorderPane.setCenter(equipmentView);
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "Cannot load Equipment Management page!", Alert.AlertType.ERROR);
        }
    }

    private void loadEventManagement() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/EventManagement.fxml"));
            Parent eventView = loader.load();
            mainBorderPane.setCenter(eventView);
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "Cannot load Event Management page!", Alert.AlertType.ERROR);
        }
    }

    private void loadMaintenanceManagement() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/MaintenanceManagement.fxml"));
            Parent maintenanceView = loader.load();
            mainBorderPane.setCenter(maintenanceView);
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "Cannot load Maintenance page!", Alert.AlertType.ERROR);
        }
    }

    // ========== NEW METHODS FOR EDIT USER CONTROLLER ==========

    /**
     * Get the currently logged in user
     * @return the logged in user
     */
    public Utilisateur getLoggedInUser() {
        return loggedInUser;
    }

    /**
     * Update the logged in user's information in the sidebar
     * @param updatedUser the updated user object
     */
    public void updateLoggedInUser(Utilisateur updatedUser) {
        this.loggedInUser = updatedUser;

        // Update profile info in sidebar
        if (nameLabel != null) {
            nameLabel.setText(updatedUser.getNom() + " " + updatedUser.getPrenom());
        }
        if (emailLabel != null) {
            emailLabel.setText(updatedUser.getEmail());
        }
        if (userMenuLabel != null) {
            userMenuLabel.setText(updatedUser.getNom());
        }
        if (welcomeLabel != null) {
            welcomeLabel.setText("Welcome back, " + updatedUser.getNom() + "!");
        }
        if (roleLabel != null) {
            roleLabel.setText("Role: " + getRoleText(updatedUser.getRole()));
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

    // ===========================================================

    public void setLoggedInUser(Utilisateur user) {
        this.loggedInUser = user;

        // Update profile info
        nameLabel.setText(user.getNom() + " " + user.getPrenom());
        emailLabel.setText(user.getEmail());
        userMenuLabel.setText(user.getNom());
        welcomeLabel.setText("Welcome back, " + user.getNom() + "!");
        roleLabel.setText("Role: " + getRoleText(user.getRole()));

        // Set profile image
        if (user.getImage() != null && user.getImage().length > 0) {
            try {
                Image img = new Image(new ByteArrayInputStream(user.getImage()));
                profileImageView.setImage(img);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // Load User Management by default
        loadUserManagement();
    }

    private String getRoleText(int roleValue) {
        return switch (roleValue) {
            case 0 -> "Admin";
            case 1 -> "Agriculteur";
            case 2 -> "Technicien";
            case 3 -> "Fournisseur";
            default -> "Unknown";
        };
    }

    @FXML
    private void handleLogout() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Logout");
        confirm.setHeaderText("Confirm Logout");
        confirm.setContentText("Are you sure you want to logout?");
        confirm.initOwner(logoutButton.getScene().getWindow());

        if (confirm.showAndWait().filter(r -> r == ButtonType.OK).isPresent()) {
            try {
                Parent root = FXMLLoader.load(getClass().getResource("/fxml/signin.fxml"));
                Stage stage = (Stage) logoutButton.getScene().getWindow();
                stage.setScene(new Scene(root));
                stage.setTitle("Sign In");
                stage.show();
            } catch (Exception e) {
                e.printStackTrace();
                showAlert("Error", "Failed to logout: " + e.getMessage(), Alert.AlertType.ERROR);
            }
        }
    }

    private void showComingSoon(String feature) {
        showAlert(feature, "This feature is under development and will be available soon!",
                Alert.AlertType.INFORMATION);
    }

    private void showAlert(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.initOwner(mainBorderPane != null ? mainBorderPane.getScene().getWindow() : null);
        alert.showAndWait();
    }
}