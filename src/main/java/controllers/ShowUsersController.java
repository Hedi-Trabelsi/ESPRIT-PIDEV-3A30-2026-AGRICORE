package controllers;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Text;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import models.User;
import services.UserService;

public class ShowUsersController {

    private final UserService us = new UserService();

    @FXML private ResourceBundle resources;
    @FXML private URL location;
    @FXML private FlowPane cardsPane;
    @FXML private TextField searchField;
    @FXML private Text countText;

    private List<User> allUsers;

    // ── Avatar gradients — rich but harmonious ──────────────────
    private static final String[] AVATAR_COLORS = {
            "linear-gradient(135deg,#1a6b45,#22a659)",  // emerald
            "linear-gradient(135deg,#1d5a8e,#3a8fd4)",  // ocean
            "linear-gradient(135deg,#7b3a6b,#c45fa0)",  // plum
            "linear-gradient(135deg,#8b5213,#d48a3a)",  // amber
            "linear-gradient(135deg,#2c5282,#4a9de1)",  // indigo
            "linear-gradient(135deg,#2d6a4f,#52b788)"   // forest
    };

    @FXML
    void initialize() {
        refreshCards();
        if (searchField != null)
            searchField.textProperty().addListener((obs, o, n) -> renderFiltered(n));
        wireFinanceNav();
    }

    public void refreshCards() {
        try {
            allUsers = us.read();
            renderFiltered(searchField != null ? searchField.getText() : null);
        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR, e.getMessage()).showAndWait();
        }
    }

    private void updateCount(int n) {
        if (countText != null)
            countText.setText(n + " membre" + (n > 1 ? "s" : "") + " enregistré" + (n > 1 ? "s" : ""));
    }

    private void renderFiltered(String query) {
        cardsPane.getChildren().clear();
        String q = query == null ? "" : query.trim().toLowerCase();
        int idx = 0, visible = 0;
        for (User u : allUsers) {
            String name = ((u.getPrenom() != null ? u.getPrenom() : "") + " "
                    + (u.getNom()    != null ? u.getNom()    : "")).toLowerCase();
            if (q.isEmpty() || name.contains(q)) {
                cardsPane.getChildren().add(buildCard(u, idx++));
                visible++;
            }
        }
        updateCount(visible);
    }

    // ── Card builder ────────────────────────────────────────────
    private Node buildCard(User user, int idx) {

        // Initials
        String fn = user.getPrenom() != null ? user.getPrenom().trim() : "";
        String ln = user.getNom()    != null ? user.getNom().trim()    : "";
        String initials = "";
        if (!fn.isEmpty()) initials += fn.substring(0, 1).toUpperCase();
        if (!ln.isEmpty()) initials += ln.substring(0, 1).toUpperCase();
        if (initials.isEmpty()) initials = "U";

        // ── Avatar ──
        StackPane avatar = new StackPane();
        avatar.setPrefSize(50, 50);
        avatar.setMinSize(50, 50);
        avatar.setMaxSize(50, 50);
        avatar.setStyle(
                "-fx-background-color: " + AVATAR_COLORS[idx % AVATAR_COLORS.length] + ";" +
                        "-fx-background-radius: 14px;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.14), 8, 0, 0, 2);"
        );
        Label initLbl = new Label(initials);
        initLbl.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 15px;");
        avatar.getChildren().add(initLbl);

        // ── Name ──
        String fullName = (fn + " " + ln).trim();
        Label nameLbl = new Label(fullName.isEmpty() ? "Utilisateur" : fullName);
        nameLbl.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #0b3320;");
        nameLbl.setMaxWidth(170);

        // ── Handle ──
        Label handleLbl = new Label("#00" + (idx + 1) + "  ·  " + fn.toLowerCase() + "." + ln.toLowerCase());
        handleLbl.setStyle("-fx-font-size: 10px; -fx-text-fill: #7a9e87;");

        // ── Role badge ──
        Label roleLbl = new Label("🌾  Agriculteur");
        roleLbl.setStyle(
                "-fx-font-size: 10px; -fx-font-weight: 700;" +
                        "-fx-text-fill: #1d6b3e;" +
                        "-fx-background-color: #cdeedd;" +
                        "-fx-background-radius: 20px;" +
                        "-fx-border-color: #9dd4b8;" +
                        "-fx-border-radius: 20px;" +
                        "-fx-border-width: 1px;" +
                        "-fx-padding: 2 10;"
        );

        VBox infoBox = new VBox(3, nameLbl, handleLbl, roleLbl);
        infoBox.setAlignment(Pos.CENTER_LEFT);

        HBox header = new HBox(12, avatar, infoBox);
        header.setAlignment(Pos.CENTER_LEFT);

        // ── Divider ──
        Region divider = new Region();
        divider.setPrefHeight(1);
        divider.setMaxWidth(Double.MAX_VALUE);
        divider.setStyle("-fx-background-color: #ddeee5;");

        // ── Footer ──
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Label hintLbl = new Label("Ouvrir le dossier");
        hintLbl.setStyle("-fx-font-size: 10px; -fx-text-fill: #8bbba0; -fx-font-weight: 600;");
        Label arrowLbl = new Label("→");
        arrowLbl.setStyle("-fx-font-size: 12px; -fx-text-fill: #22a659; -fx-font-weight: bold;");

        HBox footer = new HBox(spacer, hintLbl, arrowLbl);
        footer.setAlignment(Pos.CENTER_RIGHT);
        footer.setSpacing(6);

        // ── Assemble card ──
        VBox card = new VBox(12, header, divider, footer);
        card.setPadding(new Insets(16));
        card.setPrefWidth(280);
        card.setMinHeight(138);
        card.setStyle(cardNormal());

        card.setOnMouseEntered(e -> card.setStyle(cardHover()));
        card.setOnMouseExited(e  -> card.setStyle(cardNormal()));
        card.setOnMouseClicked(e -> openFinanceFor(user));

        return card;
    }

    // normal state: slightly off-white with sage border — blends into #eef5f0 bg
    private String cardNormal() {
        return  "-fx-background-color: #f9fdfb;" +
                "-fx-background-radius: 14px;" +
                "-fx-border-color: #c0d9cb;" +
                "-fx-border-radius: 14px;" +
                "-fx-border-width: 1px;" +
                "-fx-cursor: hand;" +
                "-fx-effect: dropshadow(gaussian, rgba(11,51,32,0.07), 10, 0, 0, 3);";
    }

    // hover: clean white card pops forward with green accent
    private String cardHover() {
        return  "-fx-background-color: #ffffff;" +
                "-fx-background-radius: 14px;" +
                "-fx-border-color: #22a659;" +
                "-fx-border-radius: 14px;" +
                "-fx-border-width: 1.5px;" +
                "-fx-cursor: hand;" +
                "-fx-effect: dropshadow(gaussian, rgba(22,166,89,0.18), 16, 0, 0, 5);";
    }

    // ── Navigation ──────────────────────────────────────────────

    private void wireFinanceNav() {
        Platform.runLater(() -> {
            try {
                javafx.scene.control.Button btn =
                        (javafx.scene.control.Button) cardsPane.getScene().getRoot().lookup("#financeBtn");
                if (btn != null) btn.setOnAction(e -> openFinanceChooser());
            } catch (Exception ignored) {}
        });
    }

    private void openFinanceChooser() {
        try {
            List<User> users = us.read();
            if (users.isEmpty()) { showAlert("Financière", "Aucun utilisateur disponible"); return; }
            ChoiceDialog<User> dlg = new ChoiceDialog<>(users.get(0), users);
            dlg.setTitle("Choisir un utilisateur");
            dlg.setHeaderText("Financière — Sélection utilisateur");
            dlg.setContentText("Utilisateur:");
            dlg.showAndWait().ifPresent(this::openFinanceFor);
        } catch (Exception e) { showAlert("Financière", e.getMessage()); }
    }

    private void openFinanceFor(User user) {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                    getClass().getResource("/FinanceTables.fxml"));
            javafx.scene.Parent root = loader.load();
            FinanceTablesController c = loader.getController();
            c.setUser(user);
            cardsPane.getScene().setRoot(root);
        } catch (Exception e) { showAlert("Navigation finance", e.getMessage()); }
    }

    private void openUserOperations(User user) {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                    getClass().getResource("/UserOperations.fxml"));
            javafx.scene.Node root = loader.load();
            UserOperationsController c = loader.getController();
            c.setUser(user);
            cardsPane.getScene().setRoot((javafx.scene.Parent) root);
        } catch (Exception e) { showAlert("Navigation error", e.getMessage()); }
    }

    private void openUserCalendar(User user) {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                    getClass().getResource("/UserCalendar.fxml"));
            javafx.scene.Parent root = loader.load();
            UserCalendarController c = loader.getController();
            c.setUser(user);
            cardsPane.getScene().setRoot(root);
        } catch (Exception e) { showAlert("Navigation calendrier", e.getMessage()); }
    }

    private void openUserAnalytics(User user) {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                    getClass().getResource("/UserAnalytics.fxml"));
            javafx.scene.Parent root = loader.load();
            UserAnalyticsController c = loader.getController();
            c.setUser(user);
            cardsPane.getScene().setRoot(root);
        } catch (Exception e) { showAlert("Navigation analytics", e.getMessage()); }
    }

    private void showAlert(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle(title); a.setContentText(msg); a.showAndWait();
    }
}