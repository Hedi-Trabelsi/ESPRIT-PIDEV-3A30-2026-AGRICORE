package controllers;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.shape.Circle;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.util.StringConverter;
import models.User;
import services.UserService;

public class ShowUsersController {
    private final UserService us = new UserService();


    @FXML
    private ResourceBundle resources;

    @FXML
    private URL location;

    @FXML
    private FlowPane cardsPane;

    @FXML
    private TextField searchField;

    private java.util.List<User> allUsers;

    @FXML
    void initialize() {
        refreshCards();
        if (searchField != null) {
            searchField.textProperty().addListener((obs, o, n) -> renderFiltered(n));
        }
        wireFinanceNav();
    }

    public void refreshCards() {
        try {
            allUsers = us.read();
            renderFiltered(searchField != null ? searchField.getText() : null);
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText("Error loading users");
            alert.setContentText(e.getMessage());
            alert.showAndWait();
        }

    }

    private void renderFiltered(String query) {
        cardsPane.getChildren().clear();
        String q = query == null ? "" : query.trim().toLowerCase();
        for (User u : allUsers) {
            String name = ((u.getFirstName() != null ? u.getFirstName() : "") + " " + (u.getLastName() != null ? u.getLastName() : "")).toLowerCase();
            if (q.isEmpty() || name.contains(q)) {
                Node card = buildInlineCard(u);
                cardsPane.getChildren().add(card);
            }
        }
    }

    private Node buildInlineCard(User user) {
        AnchorPane root = new AnchorPane();
        root.setPrefWidth(280.0);
        root.setPrefHeight(160.0);
        root.getStyleClass().add("user-card");

        VBox box = new VBox(12.0);
        AnchorPane.setTopAnchor(box, 12.0);
        AnchorPane.setBottomAnchor(box, 12.0);
        AnchorPane.setLeftAnchor(box, 12.0);
        AnchorPane.setRightAnchor(box, 12.0);

        String fn = user.getFirstName() != null ? user.getFirstName().trim() : "";
        String ln = user.getLastName() != null ? user.getLastName().trim() : "";
        String initials = "";
        if (!fn.isEmpty()) initials += fn.substring(0, 1).toUpperCase();
        if (!ln.isEmpty()) initials += ln.substring(0, 1).toUpperCase();
        if (initials.isEmpty()) initials = "U";
        Circle bg = new Circle(28);
        bg.setFill(Color.web("#7ca76f"));
        Label initialsLabel = new Label(initials);
        initialsLabel.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 16px;");
        StackPane avatar = new StackPane(bg, initialsLabel);
        avatar.setPrefSize(56, 56);

        Label name = new Label((user.getFirstName() != null ? user.getFirstName() : "") + " " + (user.getLastName() != null ? user.getLastName() : ""));
        name.getStyleClass().add("user-card-title");
        Label age = new Label(user.getAge() > 0 ? "Âge: " + user.getAge() : "");

        VBox nameBox = new VBox(4.0);
        nameBox.getChildren().addAll(name, age);

        HBox header = new HBox(12.0);
        header.setAlignment(Pos.CENTER_LEFT);
        header.getChildren().addAll(avatar, nameBox);

        HBox actions = new HBox(8.0);
        actions.setAlignment(Pos.CENTER_LEFT);
        Button open = new Button("Ouvrir");
        Button analyse = new Button("Analyse");
        Button details = new Button("Détails");
        Button calendar = new Button("Calendrier");
        open.getStyleClass().add("accent-button");
        analyse.getStyleClass().add("ghost-button");
        details.getStyleClass().add("ghost-button");
        calendar.getStyleClass().add("ghost-button");
        open.setOnAction(e -> openUserOperations(user));
        analyse.setOnAction(e -> openUserAnalytics(user));
        details.setOnAction(e -> openFinanceFor(user));
        calendar.setOnAction(e -> openUserCalendar(user));
        actions.getChildren().addAll(open, analyse, details, calendar);

        box.getChildren().addAll(header, actions);
        root.getChildren().add(box);
        return root;
    }

    private void wireFinanceNav() {
        Platform.runLater(() -> {
            try {
                Button btn = (Button) cardsPane.getScene().getRoot().lookup("#financeBtn");
                if (btn != null) {
                    btn.setOnAction(e -> openFinanceChooser());
                }
            } catch (Exception ignored) {}
        });
    }

    private void openFinanceChooser() {
        try {
            List<User> users = us.read();
            if (users.isEmpty()) {
                showAlert("Financière", "Aucun utilisateur disponible");
                return;
            }
            ChoiceDialog<User> dlg = new ChoiceDialog<>(users.get(0), users);
            dlg.setTitle("Choisir un utilisateur");
            dlg.setHeaderText("Financière - Sélection utilisateur");
            dlg.setContentText("Utilisateur:");
            // Le ChoiceDialog utilisera User.toString(); afficher tel quel
            dlg.showAndWait().ifPresent(this::openFinanceFor);
        } catch (Exception e) {
            showAlert("Financière", e.getMessage());
        }
    }

    private String displayName(User u) {
        return (u.getFirstName() != null ? u.getFirstName() : "") + " " + (u.getLastName() != null ? u.getLastName() : "");
    }

    private void openFinanceFor(User user) {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/FinanceTables.fxml"));
            javafx.scene.Parent root = loader.load();
            controllers.FinanceTablesController controller = loader.getController();
            controller.setUser(user);
            cardsPane.getScene().setRoot(root);
        } catch (Exception e) {
            showAlert("Navigation finance", e.getMessage());
        }
    }

    private void openUserOperations(User user) {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/UserOperations.fxml"));
            Node root = loader.load();
            controllers.UserOperationsController controller = loader.getController();
            controller.setUser(user);
            cardsPane.getScene().setRoot((javafx.scene.Parent) root);
        } catch (Exception e) {
            showAlert("Navigation error", e.getMessage());
        }
    }

    private void openUserCalendar(User user) {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/UserCalendar.fxml"));
            javafx.scene.Parent root = loader.load();
            controllers.UserCalendarController controller = loader.getController();
            controller.setUser(user);
            cardsPane.getScene().setRoot(root);
        } catch (Exception e) {
            showAlert("Navigation calendrier", e.getMessage());
        }
    }

    private void openUserAnalytics(User user) {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/UserAnalytics.fxml"));
            javafx.scene.Parent root = loader.load();
            controllers.UserAnalyticsController controller = loader.getController();
            controller.setUser(user);
            cardsPane.getScene().setRoot(root);
        } catch (Exception e) {
            showAlert("Navigation error", e.getMessage());
        }
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setContentText(content);
        alert.showAndWait();
    }


}
