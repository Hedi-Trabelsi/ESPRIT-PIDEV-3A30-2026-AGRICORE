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
import javafx.scene.control.ScrollPane;
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
            logoutButton;

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

    private void setContent(Parent view) {
        ScrollPane scroll = new ScrollPane(view);
        scroll.setFitToWidth(true);
        scroll.setFitToHeight(false);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scroll.setStyle("-fx-background-color: transparent;");
        mainBorderPane.setCenter(scroll);
    }

    private void loadDashboard() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Dashboard.fxml"));
            Parent dashboardView = loader.load();
            setContent(dashboardView);
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

            setContent(userView);
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "Cannot load User Management page!", Alert.AlertType.ERROR);
        }
    }

    private void loadFinancialManagement() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/ShowUsers.fxml"));
            Parent financialView = loader.load();
            setContent(financialView);
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "Cannot load Financial Management page!", Alert.AlertType.ERROR);
        }
    }

    private void loadAnimalManagement() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/AdminAnimaux.fxml"));
            Parent animalView = loader.load();
            setContent(animalView);
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "Cannot load Animal Management page!", Alert.AlertType.ERROR);
        }
    }

    private void loadEquipmentManagement() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/ListeEquipements.fxml"));
            Parent equipmentView = loader.load();
            setContent(equipmentView);
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "Cannot load Equipment Management page!", Alert.AlertType.ERROR);
        }
    }

    private void loadEventManagement() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/EventManagement.fxml"));
            Parent eventView = loader.load();

            // If your EventManagementController needs the logged in user, uncomment these lines:
            // EventManagementController controller = loader.getController();
            // controller.setLoggedInUser(loggedInUser);

            setContent(eventView);
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "Cannot load Event Management page!", Alert.AlertType.ERROR);
        }
    }

    // Add this method to your existing HomeController.java
    private void loadMaintenanceManagement() {
        try {
            System.out.println("Loading maintenance management...");

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Dashboard.fxml"));
            Parent maintenanceView = loader.load();
            setContent(maintenanceView);

        } catch (Exception e) {
            System.err.println("Error loading maintenance page: " + e.getMessage());
            e.printStackTrace();
            showAlert("Error", "Cannot load Maintenance page: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    public Utilisateur getLoggedInUser() {
        return loggedInUser;
    }

    public void setLoggedInUser(Utilisateur user) {
        this.loggedInUser = user;
        UserSession.setCurrentUser(user);

        // Update profile info
        if (nameLabel != null) {
            nameLabel.setText(user.getNom() + " " + user.getPrenom());
        }
        if (emailLabel != null) {
            emailLabel.setText(user.getEmail());
        }
        if (userMenuLabel != null) {
            userMenuLabel.setText(user.getNom());
        }
        if (welcomeLabel != null) {
            welcomeLabel.setText("Welcome back, " + user.getNom() + "!");
        }
        if (roleLabel != null) {
            roleLabel.setText("Role: " + getRoleText(user.getRole()));
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

        // Role-based default view
        if (user.getRole() == 3) {
            // Fournisseur: disable all sidebar buttons except Equipment
            Button[] restrictedButtons = {dashboardButton, manageUsersButton, financialButton,
                    animalButton, eventButton, maintenanceButton};
            for (Button btn : restrictedButtons) {
                if (btn != null) {
                    btn.setDisable(true);
                    btn.setOpacity(0.5);
                }
            }
            // Auto-load equipment page
            setActiveButton(equipmentButton);
            loadEquipmentManagement();
        } else if (user.getRole() == 4) {
            // Financier: disable all except Financial
            Button[] restrictedButtons = {dashboardButton, manageUsersButton, animalButton,
                    equipmentButton, eventButton, maintenanceButton};
            for (Button btn : restrictedButtons) {
                if (btn != null) {
                    btn.setDisable(true);
                    btn.setOpacity(0.5);
                }
            }
            setActiveButton(financialButton);
            loadFinancialManagement();
        } else {
            // Admin: load User Management by default
            loadUserManagement();
        }
    }

    public void updateLoggedInUser(Utilisateur updatedUser) {
        this.loggedInUser = updatedUser;
        UserSession.setCurrentUser(updatedUser);

        // Update profile info
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

    private String getRoleText(int roleValue) {
        return switch (roleValue) {
            case 0 -> "Admin";
            case 1 -> "Agriculteur";
            case 2 -> "Technicien";
            case 3 -> "Fournisseur";
            case 4 -> "Financier";
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

    private void showAlert(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.initOwner(mainBorderPane != null ? mainBorderPane.getScene().getWindow() : null);
        alert.showAndWait();
    }
}