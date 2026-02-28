package Controller;

import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;

public class NavigationUtil {
    public static void loadInContentArea(Node source, Parent view) {
        Parent root = source.getScene().getRoot();
        if (root instanceof BorderPane) {
            Node center = ((BorderPane) root).getCenter();
            if (center instanceof StackPane) {
                // UserHomePage flow: center is a StackPane
                ((StackPane) center).getChildren().setAll(view);
            } else {
                // Admin (HomeController) flow: center set directly
                ((BorderPane) root).setCenter(view);
            }
        } else {
            // Fallback
            source.getScene().setRoot(view);
        }
    }
}
