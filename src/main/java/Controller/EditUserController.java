package Controller;

import Model.Utilisateur;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.mindrot.jbcrypt.BCrypt;
import services.UserService;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.time.LocalDate;

public class EditUserController {

    @FXML private TextField nomField, prenomField, emailField, adresseField, phoneField;
    @FXML private PasswordField passwordField;
    @FXML private ComboBox<String> genreBox, roleBox;
    @FXML private DatePicker datePicker;
    @FXML private ImageView profileImageView;
    @FXML private Button uploadImageButton, saveButton, cancelButton;
    @FXML private Label errorLabel;

    private Utilisateur currentUser;
    private UserService userService;
    private byte[] profileImageBytes;
    private Runnable onUserUpdated;
    private boolean isImageChanged = false;
    private HomeController homeController;

    @FXML
    public void initialize() {
        try {
            userService = new UserService();
            setupComboBoxes();
            setupButtonHandlers();
            errorLabel.setVisible(false);
        } catch (Exception e) {
            showError("Failed to initialize: " + e.getMessage());
        }
    }

    private void setupComboBoxes() {
        genreBox.getItems().addAll("Male", "Female");
        roleBox.getItems().addAll("Agriculteur", "Technicien", "Fournisseur");
    }

    private void setupButtonHandlers() {
        uploadImageButton.setOnAction(e -> handleImageUpload());
        saveButton.setOnAction(e -> handleSave());
        cancelButton.setOnAction(e -> closeWindow());
    }

    public void setUser(Utilisateur user) {
        this.currentUser = user;
        populateFields();
    }

    public void setHomeController(HomeController controller) {
        this.homeController = controller;
    }

    public void setOnUserUpdated(Runnable callback) {
        this.onUserUpdated = callback;
    }

    private void populateFields() {
        nomField.setText(currentUser.getNom());
        prenomField.setText(currentUser.getPrenom());
        emailField.setText(currentUser.getEmail());
        genreBox.setValue(currentUser.getGenre());
        adresseField.setText(currentUser.getAdresse());
        phoneField.setText(String.valueOf(currentUser.getPhone()));
        datePicker.setValue(currentUser.getDateNaissance());

        // Set role
        roleBox.setValue(switch (currentUser.getRole()) {
            case 0 -> "Admin";
            case 1 -> "Agriculteur";
            case 2 -> "Technicien";
            case 3 -> "Fournisseur";
            case 4 -> "Financier";
            default -> "Agriculteur";
        });

        // Set image
        if (currentUser.getImage() != null) {
            try {
                profileImageBytes = currentUser.getImage();
                Image img = new Image(new ByteArrayInputStream(profileImageBytes));
                profileImageView.setImage(img);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void handleImageUpload() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choisir une photo de profil");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.gif")
        );

        File file = fileChooser.showOpenDialog(uploadImageButton.getScene().getWindow());
        if (file == null) return;

        try {
            // Read the image
            BufferedImage originalImage = ImageIO.read(file);

            // Resize the image to max 200x200 pixels
            BufferedImage resizedImage = resizeImage(originalImage, 200, 200);

            // Compress and convert to bytes
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(resizedImage, "jpg", baos);
            profileImageBytes = baos.toByteArray();

            // Display the resized image
            Image img = new Image(new ByteArrayInputStream(profileImageBytes));
            profileImageView.setImage(img);
            isImageChanged = true;

            showSuccess("Image uploaded and optimized successfully!");

        } catch (Exception e) {
            e.printStackTrace();
            showError("Failed to load image: " + e.getMessage());
        }
    }

    /**
     * Resize image to fit within maxWidth and maxHeight while maintaining aspect ratio
     */
    private BufferedImage resizeImage(BufferedImage originalImage, int maxWidth, int maxHeight) {
        int originalWidth = originalImage.getWidth();
        int originalHeight = originalImage.getHeight();

        // Calculate new dimensions
        double widthRatio = (double) maxWidth / originalWidth;
        double heightRatio = (double) maxHeight / originalHeight;
        double ratio = Math.min(widthRatio, heightRatio);

        int newWidth = (int) (originalWidth * ratio);
        int newHeight = (int) (originalHeight * ratio);

        // Create resized image
        BufferedImage resizedImage = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);
        java.awt.Graphics2D g = resizedImage.createGraphics();
        g.drawImage(originalImage, 0, 0, newWidth, newHeight, null);
        g.dispose();

        return resizedImage;
    }

    private void handleSave() {
        errorLabel.setVisible(false);

        try {
            // Validate inputs
            if (!validateInputs()) return;

            // Parse phone
            int phone;
            try {
                phone = Integer.parseInt(phoneField.getText().trim());
            } catch (NumberFormatException e) {
                showError("Invalid phone number!");
                return;
            }

            // Update user object
            updateUserFromFields(phone);

            // Save to database
            userService.update(currentUser);

            showSuccess("User updated successfully!");

            // Notify parent and close
            if (onUserUpdated != null) {
                onUserUpdated.run(); // This will refresh the table
            }

            // If this is the currently logged in user, update the profile in HomeController
            if (homeController != null && homeController.getLoggedInUser() != null &&
                    homeController.getLoggedInUser().getId() == currentUser.getId()) {
                Platform.runLater(() -> {
                    homeController.updateLoggedInUser(currentUser);
                });
            }

            // Close after short delay
            new Thread(() -> {
                try {
                    Thread.sleep(1000);
                    javafx.application.Platform.runLater(this::closeWindow);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }).start();

        } catch (Exception e) {
            e.printStackTrace();
            showError("Error updating user: " + e.getMessage());
        }
    }

    private boolean validateInputs() {
        if (nomField.getText().trim().isEmpty() ||
                prenomField.getText().trim().isEmpty() ||
                emailField.getText().trim().isEmpty() ||
                genreBox.getValue() == null ||
                roleBox.getValue() == null ||
                datePicker.getValue() == null) {
            showError("Please fill all required fields!");
            return false;
        }
        return true;
    }

    private void updateUserFromFields(int phone) {
        currentUser.setNom(nomField.getText().trim());
        currentUser.setPrenom(prenomField.getText().trim());
        currentUser.setEmail(emailField.getText().trim());
        currentUser.setGenre(genreBox.getValue());
        currentUser.setAdresse(adresseField.getText().trim());
        currentUser.setPhone(phone);
        currentUser.setDateNaissance(datePicker.getValue());

        // Set role
        currentUser.setRole(switch (roleBox.getValue()) {
            case "Admin" -> 0;
            case "Agriculteur" -> 1;
            case "Technicien" -> 2;
            case "Fournisseur" -> 3;
            case "Financier" -> 4;
            default -> 1;
        });

        // Update image if changed
        if (isImageChanged && profileImageBytes != null) {
            currentUser.setImage(profileImageBytes);
        }

        // Update password if provided
        if (!passwordField.getText().isEmpty()) {
            currentUser.setPassword(BCrypt.hashpw(passwordField.getText(), BCrypt.gensalt()));
        }
    }

    private void closeWindow() {
        ((Stage) cancelButton.getScene().getWindow()).close();
    }

    private void showError(String message) {
        errorLabel.getStyleClass().removeAll("success-label");
        errorLabel.getStyleClass().add("error-label");
        errorLabel.setText(message);
        errorLabel.setVisible(true);
    }

    private void showSuccess(String message) {
        errorLabel.getStyleClass().removeAll("error-label");
        errorLabel.getStyleClass().add("success-label");
        errorLabel.setText(message);
        errorLabel.setVisible(true);
    }
}