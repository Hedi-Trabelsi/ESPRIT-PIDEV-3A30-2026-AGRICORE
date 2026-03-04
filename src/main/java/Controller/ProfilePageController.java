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
        setupButtons();
    }

    private void setupButtons() {
        if (editProfileDetailButton != null) {
            editProfileDetailButton.setOnAction(e -> {
                if (loggedInUser != null) {
                    openEditProfile();
                } else {
                    showAlert("Erreur", "Utilisateur non connecté");
                }
            });
        }
    }

    public void setUserData(Utilisateur user) {
        this.loggedInUser = user;

        if (user != null) {
            populateProfileData();

            // Check for missing information and notify if needed
            List<String> missingInfo = getMissingInformation();
            if (!missingInfo.isEmpty() && userHomeController != null) {
                userHomeController.showProfileCompletionNotificationWithDetails(missingInfo);
            }
        }
    }

    public void setUserHomeController(UserHomeController controller) {
        this.userHomeController = controller;
    }

    private void populateProfileData() {
        if (loggedInUser == null) return;

        try {
            if (profileFullNameLabel != null) {
                profileFullNameLabel.setText(loggedInUser.getNom() + " " + loggedInUser.getPrenom());
            }

            if (profileRoleLabel != null) {
                String roleText;
                switch (loggedInUser.getRole()) {
                    case 1: roleText = "Agriculteur"; break;
                    case 2: roleText = "Technicien"; break;
                    case 3: roleText = "Fournisseur"; break;
                    case 4: roleText = "Financier"; break;
                    default: roleText = "Membre";
                }
                profileRoleLabel.setText(roleText);
            }

            if (profileEmailDetailLabel != null) {
                profileEmailDetailLabel.setText(loggedInUser.getEmail());
            }

            if (profilePhoneDetailLabel != null) {
                profilePhoneDetailLabel.setText(
                        loggedInUser.getPhone() != 0 ? String.valueOf(loggedInUser.getPhone()) : "Non spécifié");
            }

            if (profileAddressDetailLabel != null) {
                profileAddressDetailLabel.setText(
                        loggedInUser.getAdresse() != null && !loggedInUser.getAdresse().isEmpty()
                                ? loggedInUser.getAdresse() : "Non spécifiée");
            }

            if (profileBirthDateLabel != null) {
                LocalDate birthDate = loggedInUser.getDateNaissance();
                profileBirthDateLabel.setText(
                        birthDate != null ? birthDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "Non spécifiée");
            }

            if (profileMemberSinceDetailLabel != null) {
                LocalDate birthDate = loggedInUser.getDateNaissance();
                profileMemberSinceDetailLabel.setText(
                        birthDate != null ? "Membre depuis " + birthDate.getYear() : "Membre depuis 2026");
            }

            if (profileGenreLabel != null) {
                profileGenreLabel.setText(
                        loggedInUser.getGenre() != null && !loggedInUser.getGenre().isEmpty()
                                ? loggedInUser.getGenre() : "Non spécifié");
            }

            if (loggedInUser.getImage() != null && loggedInUser.getImage().length > 0) {
                try {
                    Image img = new Image(new ByteArrayInputStream(loggedInUser.getImage()));
                    if (profileDetailImageView != null) {
                        profileDetailImageView.setImage(img);
                    }
                } catch (Exception ignored) {}
            }

            loadStatistics();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadStatistics() {
        if (profileEventsCount != null) profileEventsCount.setText("—");
        if (profileAnimalsCount != null) profileAnimalsCount.setText("—");
        if (profileEquipmentCount != null) profileEquipmentCount.setText("—");
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
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/EditUser.fxml"));
            Parent root = loader.load();

            EditUserController controller = loader.getController();

            // Pass the user to EditUserController
            controller.setUser(loggedInUser);

            // Set callback to refresh profile after update
            controller.setOnUserUpdated(() -> {
                // Recharger l'utilisateur depuis la base de données
                try {
                    UserService userService = new UserService();
                    Utilisateur updatedUser = userService.findByEmail(loggedInUser.getEmail());
                    if (updatedUser != null) {
                        loggedInUser = updatedUser;
                        UserSession.setCurrentUser(updatedUser);
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