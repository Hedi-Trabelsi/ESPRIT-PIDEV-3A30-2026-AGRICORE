package Controller;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import Model.User;

public class UserCardController {
    @FXML
    private Label nameLabel;
    @FXML
    private Label ageLabel;

    private User user;
    private Runnable onOpenOperations;
    private Runnable onOpenAnalytics;

    public void setUser(User user) {
        this.user = user;
        if (nameLabel != null && ageLabel != null && user != null) {
            nameLabel.setText((user.getPrenom() != null ? user.getPrenom() : "") + " " + (user.getNom() != null ? user.getNom() : ""));
            ageLabel.setText(user.getEmail() != null ? user.getEmail() : "");
        }
    }

    public void setOnOpenOperations(Runnable onOpenOperations) {
        this.onOpenOperations = onOpenOperations;
    }

    public void setOnOpenAnalytics(Runnable onOpenAnalytics) {
        this.onOpenAnalytics = onOpenAnalytics;
    }

    @FXML
    void openOperations() {
        if (onOpenOperations != null) {
            onOpenOperations.run();
        }
    }

    @FXML
    void openAnalytics() {
        if (onOpenAnalytics != null) {
            onOpenAnalytics.run();
        }
    }
}
