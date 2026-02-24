package Controller;

import Model.Utilisateur;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.ByteArrayInputStream;
import java.time.format.DateTimeFormatter;

public class ProfilePageController {

    // Profile Image
    @FXML private ImageView profileDetailImageView;

    // Profile Info Labels
    @FXML private Label profileFullNameLabel;
    @FXML private Label profileRoleLabel;
    @FXML private Label profileEmailDetailLabel;
    @FXML private Label profilePhoneDetailLabel;
    @FXML private Label profileAddressDetailLabel;
    @FXML private Label profileMemberSinceDetailLabel;

    // Statistics Labels
    @FXML private Label profileEventsCount;
    @FXML private Label profileAnimalsCount;
    @FXML private Label profileEquipmentCount;

    // Buttons
    @FXML private Button editProfileDetailButton;
    @FXML private Button changePasswordDetailButton;

    private Utilisateur loggedInUser;
    private UserHomeController userHomeController;

    @FXML
    public void initialize() {
        System.out.println("ProfilePageController initialized");
        setupButtons();
    }

    private void setupButtons() {
        if (editProfileDetailButton != null) {
            editProfileDetailButton.setOnAction(e -> {
                System.out.println("Edit Profile button clicked");
                if (loggedInUser != null) {
                    openEditProfile();
                } else {
                    System.out.println("Error: loggedInUser is null!");
                    showAlert("Erreur", "Utilisateur non connecté");
                }
            });
        }

        if (changePasswordDetailButton != null) {
            changePasswordDetailButton.setOnAction(e -> showComingSoon("Changer mot de passe"));
        }
    }

    public void setUserData(Utilisateur user) {
        System.out.println("Setting user data in ProfilePageController");
        this.loggedInUser = user;
        if (user != null) {
            System.out.println("User: " + user.getEmail() + " - " + user.getNom() + " " + user.getPrenom());
            populateProfileData();
        } else {
            System.out.println("ERROR: User is NULL in setUserData!");
        }
    }

    public void setUserHomeController(UserHomeController controller) {
        this.userHomeController = controller;
    }

    private void populateProfileData() {
        if (loggedInUser == null) {
            System.out.println("Cannot populate profile: user is null");
            return;
        }

        System.out.println("Populating profile data for: " + loggedInUser.getEmail());

        // Set full name
        if (profileFullNameLabel != null) {
            String fullName = loggedInUser.getNom() + " " + loggedInUser.getPrenom();
            profileFullNameLabel.setText(fullName);
            System.out.println("Set name: " + fullName);
        }

        // Set role
        if (profileRoleLabel != null) {
            String roleText;
            switch (loggedInUser.getRole()) {
                case 1: roleText = "Agriculteur"; break;
                case 2: roleText = "Technicien"; break;
                case 3: roleText = "Fournisseur"; break;
                default: roleText = "Membre";
            }
            profileRoleLabel.setText(roleText);
        }

        // Set email
        if (profileEmailDetailLabel != null) {
            profileEmailDetailLabel.setText(loggedInUser.getEmail());
        }

        // Set phone
        if (profilePhoneDetailLabel != null) {
            profilePhoneDetailLabel.setText(String.valueOf(loggedInUser.getPhone()));
        }

        // Set address
        if (profileAddressDetailLabel != null) {
            profileAddressDetailLabel.setText(loggedInUser.getAdresse() != null ?
                    loggedInUser.getAdresse() : "Adresse non spécifiée");
        }

        // Set member since
        if (profileMemberSinceDetailLabel != null) {
            if (loggedInUser.getDateNaissance() != null) {
                profileMemberSinceDetailLabel.setText("Membre depuis " +
                        loggedInUser.getDateNaissance().format(DateTimeFormatter.ofPattern("MMMM yyyy")));
            } else {
                profileMemberSinceDetailLabel.setText("Membre depuis 2026");
            }
        }

        // Set profile image
        if (loggedInUser.getImage() != null && loggedInUser.getImage().length > 0) {
            try {
                Image img = new Image(new ByteArrayInputStream(loggedInUser.getImage()));
                if (profileDetailImageView != null) {
                    profileDetailImageView.setImage(img);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // Load statistics
        loadStatistics();
    }

    private void loadStatistics() {
        if (profileEventsCount != null) profileEventsCount.setText("3");
        if (profileAnimalsCount != null) profileAnimalsCount.setText("12");
        if (profileEquipmentCount != null) profileEquipmentCount.setText("5");
    }

    private void openEditProfile() {
        try {
            System.out.println("Opening Edit Profile window for user: " + loggedInUser.getEmail());

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/EditUser.fxml"));
            Parent root = loader.load();

            EditUserController controller = loader.getController();

            // IMPORTANT: Pass the user to EditUserController
            controller.setUser(loggedInUser);

            // Set callback to refresh profile after update
            controller.setOnUserUpdated(() -> {
                System.out.println("User updated, refreshing profile...");
                // Refresh profile data
                populateProfileData();

                // Update sidebar in UserHomeController if available
                if (userHomeController != null) {
                    userHomeController.refreshSidebarProfile(loggedInUser);
                }
            });

            Stage stage = new Stage();
            stage.setTitle("Modifier le profil");
            stage.setScene(new Scene(root));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Erreur", "Impossible d'ouvrir la fenêtre de modification: " + e.getMessage());
        }
    }

    private void showComingSoon(String feature) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Bientôt disponible");
        alert.setHeaderText(feature);
        alert.setContentText("Cette fonctionnalité sera bientôt disponible!");
        alert.showAndWait();
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}