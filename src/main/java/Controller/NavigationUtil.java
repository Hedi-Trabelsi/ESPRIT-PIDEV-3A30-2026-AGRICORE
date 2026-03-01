package Controller;

import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;

public class NavigationUtil {
    /**
     * Loads a view into the shell's content area (center of the main BorderPane).
     * Works for both UserHomePage (StackPane center) and HomeController (direct center).
     * Never replaces the scene root — the shell (sidebar + topbar) always stays.
     */
    public static void loadInContentArea(Node source, Parent view) {
        Parent root = source.getScene().getRoot();

        if (root instanceof BorderPane) {
            BorderPane shell = (BorderPane) root;
            Node center = shell.getCenter();

            if (center instanceof StackPane) {
                // UserHomePage flow: center is a StackPane (contentArea)
                ((StackPane) center).getChildren().setAll(view);
            } else {
                // Admin (HomeController) flow: wrap in ScrollPane for consistency
                ScrollPane scroll = new ScrollPane(view);
                scroll.setFitToWidth(true);
                scroll.setFitToHeight(true);
                scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
                scroll.setStyle("-fx-background-color: transparent;");
                shell.setCenter(scroll);
            }
        } else {
            // Search up the scene graph for a BorderPane shell
            Node current = source;
            while (current != null) {
                if (current.getParent() instanceof StackPane) {
                    StackPane stackPane = (StackPane) current.getParent();
                    if (stackPane.getParent() instanceof BorderPane) {
                        stackPane.getChildren().setAll(view);
                        return;
                    }
                }
                current = current.getParent();
            }
            System.err.println("NavigationUtil: Could not find shell container. Navigation may not work correctly.");
        }
    }
}
