package Controller;

import Model.Utilisateur;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Modality;
import javafx.stage.Stage;
import services.UserService;

import java.io.ByteArrayInputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class ProfilePageController {

    // Profile Image
    @FXML private ImageView profileDetailImageView;

    // Profile Info Labels
    @FXML private Label profileFullNameLabel;
    @FXML private Label profileRoleLabel;
    @FXML private Label profileEmailDetailLabel;
    @FXML private Label profilePhoneDetailLabel;
    @FXML private Label profileAddressDetailLabel;
    @FXML private Label profileBirthDateLabel;
    @FXML private Label profileMemberSinceDetailLabel;
    @FXML private Label profileGenreLabel;

    // Statistics Labels
    @FXML private Label profileEventsCount;
    @FXML private Label profileAnimalsCount;
    @FXML private Label profileEquipmentCount;

    // Buttons
    @FXML private Button editProfileDetailButton;

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
    }

    public void setUserData(Utilisateur user) {
        System.out.println("=== ProfilePageController.setUserData ===");
        System.out.println("User received: " + (user != null ? user.getEmail() : "null"));

        this.loggedInUser = user;

        if (user != null) {
            System.out.println("User details:");
            System.out.println("  - Nom: " + user.getNom());
            System.out.println("  - Prenom: " + user.getPrenom());
            System.out.println("  - Email: " + user.getEmail());
            System.out.println("  - DateNaissance: " + user.getDateNaissance());
            System.out.println("  - Genre: " + user.getGenre());
            System.out.println("  - Phone: " + user.getPhone());
            System.out.println("  - Adresse: " + user.getAdresse());

            populateProfileData();

            // Check for missing information and notify if needed
            List<String> missingInfo = getMissingInformation();
            System.out.println("Missing information: " + missingInfo.size() + " fields");
            if (!missingInfo.isEmpty() && userHomeController != null) {
                userHomeController.showProfileCompletionNotificationWithDetails(missingInfo);
            }
        } else {
            System.out.println("ERROR: User is NULL in setUserData!");
        }
    }

    public void setUserHomeController(UserHomeController controller) {
        this.userHomeController = controller;
    }

    private void populateProfileData() {
        System.out.println("=== populateProfileData called ===");

        if (loggedInUser == null) {
            System.out.println("ERROR: Cannot populate profile: user is null");
            return;
        }

        System.out.println("Populating profile data for: " + loggedInUser.getEmail());

        try {
            // Set full name
            if (profileFullNameLabel != null) {
                String fullName = loggedInUser.getNom() + " " + loggedInUser.getPrenom();
                profileFullNameLabel.setText(fullName);
                System.out.println("Name set to: " + fullName);
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
                System.out.println("Role set to: " + roleText);
            }

            // Set email
            if (profileEmailDetailLabel != null) {
                profileEmailDetailLabel.setText(loggedInUser.getEmail());
                System.out.println("Email set to: " + loggedInUser.getEmail());
            }

            // Set phone
            if (profilePhoneDetailLabel != null) {
                if (loggedInUser.getPhone() != 0) {
                    profilePhoneDetailLabel.setText(String.valueOf(loggedInUser.getPhone()));
                    System.out.println("Phone set to: " + loggedInUser.getPhone());
                } else {
                    profilePhoneDetailLabel.setText("Non spécifié");
                    System.out.println("Phone is 0, set to 'Non spécifié'");
                }
            }

            // Set address
            if (profileAddressDetailLabel != null) {
                if (loggedInUser.getAdresse() != null && !loggedInUser.getAdresse().isEmpty()) {
                    profileAddressDetailLabel.setText(loggedInUser.getAdresse());
                    System.out.println("Address set to: " + loggedInUser.getAdresse());
                } else {
                    profileAddressDetailLabel.setText("Non spécifiée");
                    System.out.println("Address is empty, set to 'Non spécifiée'");
                }
            }

            // Set birth date - SAFE with null check
            if (profileBirthDateLabel != null) {
                LocalDate birthDate = loggedInUser.getDateNaissance();
                if (birthDate != null) {
                    profileBirthDateLabel.setText(birthDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
                    System.out.println("Birth date set to: " + birthDate);
                } else {
                    profileBirthDateLabel.setText("Non spécifiée");
                    System.out.println("Birth date is null, set to 'Non spécifiée'");
                }
            }

            // Set member since - SAFE with null check
            if (profileMemberSinceDetailLabel != null) {
                LocalDate birthDate = loggedInUser.getDateNaissance();
                if (birthDate != null) {
                    profileMemberSinceDetailLabel.setText("Membre depuis " + birthDate.getYear());
                    System.out.println("Member since set to: " + birthDate.getYear());
                } else {
                    profileMemberSinceDetailLabel.setText("Membre depuis 2026");
                    System.out.println("Birth date null, using default 'Membre depuis 2026'");
                }
            }

            // Set genre
            if (profileGenreLabel != null) {
                if (loggedInUser.getGenre() != null && !loggedInUser.getGenre().isEmpty()) {
                    profileGenreLabel.setText(loggedInUser.getGenre());
                    System.out.println("Genre set to: " + loggedInUser.getGenre());
                } else {
                    profileGenreLabel.setText("Non spécifié");
                    System.out.println("Genre is null/empty, set to 'Non spécifié'");
                }
            }

            // Set profile image
            if (loggedInUser.getImage() != null && loggedInUser.getImage().length > 0) {
                try {
                    Image img = new Image(new ByteArrayInputStream(loggedInUser.getImage()));
                    if (profileDetailImageView != null) {
                        profileDetailImageView.setImage(img);
                    }
                    System.out.println("Profile image set successfully");
                } catch (Exception e) {
                    System.err.println("Error setting profile image: " + e.getMessage());
                }
            } else {
                System.out.println("No profile image available");
            }

            // Load statistics
            loadStatistics();

            System.out.println("=== populateProfileData completed successfully ===");

        } catch (Exception e) {
            System.err.println("ERROR in populateProfileData: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void loadStatistics() {
        try {
            if (profileEventsCount != null) profileEventsCount.setText("3");
            if (profileAnimalsCount != null) profileAnimalsCount.setText("12");
            if (profileEquipmentCount != null) profileEquipmentCount.setText("5");
            System.out.println("Statistics loaded");
        } catch (Exception e) {
            System.err.println("Error loading statistics: " + e.getMessage());
        }
    }

    /**
     * Check which information is missing and return a list of missing fields
     */
    public List<String> getMissingInformation() {
        List<String> missingFields = new ArrayList<>();

        if (loggedInUser == null) return missingFields;

        if (loggedInUser.getDateNaissance() == null) {
            missingFields.add("Date de naissance");
        }

        if (loggedInUser.getGenre() == null || loggedInUser.getGenre().equals("Non spécifié") || loggedInUser.getGenre().isEmpty()) {
            missingFields.add("Genre");
        }

        if (loggedInUser.getAdresse() == null || loggedInUser.getAdresse().isEmpty()) {
            missingFields.add("Adresse");
        }

        if (loggedInUser.getPhone() == 0) {
            missingFields.add("Numéro de téléphone");
        }

        return missingFields;
    }

    // Ajoutez cette méthode dans ProfilePageController.java pour rafraîchir les données après modification

    private void openEditProfile() {
        try {
            System.out.println("Opening Edit Profile window for user: " + loggedInUser.getEmail());

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/EditUser.fxml"));
            Parent root = loader.load();

            EditUserController controller = loader.getController();

            // Pass the user to EditUserController
            controller.setUser(loggedInUser);

            // Set callback to refresh profile after update
            controller.setOnUserUpdated(() -> {
                System.out.println("User updated, refreshing profile...");

                // Recharger l'utilisateur depuis la base de données
                try {
                    UserService userService = new UserService();
                    Utilisateur updatedUser = userService.findByEmail(loggedInUser.getEmail());
                    if (updatedUser != null) {
                        loggedInUser = updatedUser;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

                // Refresh profile data
                populateProfileData();

                // Update sidebar in UserHomeController if available
                if (userHomeController != null) {
                    userHomeController.refreshSidebarProfile(loggedInUser);

                    // Check if there's still missing information after update
                    List<String> missingInfo = getMissingInformation();
                    if (!missingInfo.isEmpty()) {
                        userHomeController.showProfileCompletionNotificationWithDetails(missingInfo);
                    } else {
                        userHomeController.hideProfileCompletionNotification();
                    }
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

    /**
     * Refresh the profile data (called from UserHomeController after update)
     */
    public void refreshProfile() {
        populateProfileData();
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}