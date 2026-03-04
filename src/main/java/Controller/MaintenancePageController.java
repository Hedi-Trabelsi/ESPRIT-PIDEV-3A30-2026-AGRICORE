package Controller;

import Model.Utilisateur;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import java.io.IOException;

public class MaintenancePageController {

    @FXML private Button backButton;
    @FXML private StackPane contentArea;

    private Utilisateur loggedInUser;

    @FXML
    public void initialize() {
        System.out.println("MaintenancePageController initialized");

        if (backButton != null) {
            backButton.setOnAction(e -> {
                // Go back to previous page or close
                goBack();
            });
        }

        // Load the main maintenance list view
        loadMaintenanceListView();
    }

    public void setLoggedInUser(Utilisateur user) {
        this.loggedInUser = user;
        System.out.println("Logged in user set: " + (user != null ? user.getNom() : "null"));
    }

    private void loadMaintenanceListView() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/ShowMaintenance.fxml"));
            Parent listView = loader.load();

            // Pass the user to the list controller if needed
            Object controller = loader.getController();
            if (controller != null && loggedInUser != null) {
                try {
                    controller.getClass().getMethod("setLoggedInUser", Utilisateur.class)
                            .invoke(controller, loggedInUser);
                } catch (Exception e) {
                    // Method doesn't exist, ignore
                }
            }

            contentArea.getChildren().setAll(listView);

        } catch (IOException e) {
            e.printStackTrace();
            showError("Could not load maintenance list");
        }
    }

    public void loadAddMaintenanceView() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/AddMaintenance.fxml"));
            Parent addView = loader.load();

            // Pass the user to the add controller if needed
            Object controller = loader.getController();
            if (controller != null && loggedInUser != null) {
                try {
                    controller.getClass().getMethod("setLoggedInUser", Utilisateur.class)
                            .invoke(controller, loggedInUser);
                } catch (Exception e) {
                    // Method doesn't exist, ignore
                }
            }

            contentArea.getChildren().setAll(addView);

        } catch (IOException e) {
            e.printStackTrace();
            showError("Could not load add maintenance view");
        }
    }

    public void loadUpdateMaintenanceView(Model.Maintenance maintenance) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/UpdateMaintenance.fxml"));
            Parent updateView = loader.load();

            // Get the controller and set the maintenance
            UpdateMaintenanceController controller = loader.getController();
            if (controller != null) {
                controller.setMaintenance(maintenance);
            }

            contentArea.getChildren().setAll(updateView);

        } catch (IOException e) {
            e.printStackTrace();
            showError("Could not load update maintenance view");
        }
    }

    public void refreshAll() {
        loadMaintenanceListView();
    }

    private void goBack() {
        // If we're in a sub-view, go back to list
        if (contentArea.getChildren().size() > 0) {
            loadMaintenanceListView();
        } else {
            // Otherwise close the window if it's a separate stage
            try {
                Stage stage = (Stage) backButton.getScene().getWindow();
                stage.close();
            } catch (Exception e) {
                // Ignore
            }
        }
    }




    private void showError(String message) {
        javafx.scene.control.Label errorLabel = new javafx.scene.control.Label(message);
        errorLabel.setStyle("-fx-text-fill: red; -fx-font-size: 16px;");
        contentArea.getChildren().setAll(errorLabel);
    }
}