package Controller;

import Controller.FinanceTablesController;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import Model.User;
import javafx.stage.Stage;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class CombinedDashboardController {

    /* ── FXML injected ─────────────────────────────────────────── */

    // Compat labels (hidden in status bar)
    @FXML private Label      titleLabel;
    @FXML private Label      avatarInitials;
    @FXML private Label      userNameLabel;

    // Nav buttons
    @FXML private Button     btnDetails;
    @FXML private Button     btnAnalyse;
    @FXML private Button     btnCalendrier;

    // Full-width content hosts
    @FXML private AnchorPane financeHost;    // Détails
    @FXML private AnchorPane analyticsHost;  // Analyse
    @FXML private AnchorPane calendarHost;   // Calendrier

    // Status bar
    @FXML private Label      statusLabel;
    @FXML private Label      dateLabel;
    @FXML private Label      periodBadge;

    private User currentUser;

    /* ── Button style constants ────────────────────────────────── */

    private static final String BTN_ACTIVE =
            "-fx-background-color: #16503A;" +
                    "-fx-text-fill: white;" +
                    "-fx-font-size: 12px; -fx-font-weight: bold;" +
                    "-fx-background-radius: 10; -fx-border-radius: 10;" +
                    "-fx-border-color: transparent;" +
                    "-fx-padding: 8 20 8 20; -fx-cursor: hand;" +
                    "-fx-effect: dropshadow(gaussian, rgba(22,80,58,0.28), 8, 0, 0, 2);";

    private static final String BTN_IDLE =
            "-fx-background-color: white;" +
                    "-fx-text-fill: #16503A;" +
                    "-fx-font-size: 12px; -fx-font-weight: bold;" +
                    "-fx-background-radius: 10; -fx-border-radius: 10;" +
                    "-fx-border-color: #D3E4DA; -fx-border-width: 1;" +
                    "-fx-padding: 8 20 8 20; -fx-cursor: hand;" +
                    "-fx-effect: none;";

    /* ── Entry point ───────────────────────────────────────────── */

    public void setUser(User user) {
        this.currentUser = user;
        loadPanels();
        updateStatusBar();
    }

    /* ── Panel loading ─────────────────────────────────────────── */

    private void loadPanels() {
        loadInto(financeHost,   "/fxml/FinanceTables.fxml",  loader -> {
            FinanceTablesController c = loader.getController();
            c.setEmbedded(true);
            c.setUser(currentUser);
        });
        loadInto(analyticsHost, "/fxml/UserAnalytics.fxml", loader -> {
            UserAnalyticsController c = loader.getController();
            c.setEmbedded(true);
            c.setUser(currentUser);
        });
        if (calendarHost != null) {
            loadInto(calendarHost, "/fxml/UserCalendar.fxml", loader -> {
                UserCalendarController c = loader.getController();
                c.setUser(currentUser);
            });
        }
        showDetails(); // start on Détails tab
    }

    private void loadInto(AnchorPane host, String fxml, LoaderConsumer setup) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxml));
            Parent panel = loader.load();
            setup.accept(loader);
            AnchorPane.setTopAnchor(panel, 0.0);
            AnchorPane.setBottomAnchor(panel, 0.0);
            AnchorPane.setLeftAnchor(panel, 0.0);
            AnchorPane.setRightAnchor(panel, 0.0);
            host.getChildren().setAll(panel);
        } catch (Exception e) {
            host.getChildren().setAll(errorCard(fxml, e.getMessage()));
        }
    }

    /* ── View switching ────────────────────────────────────────── */

    @FXML
    public void showDetails() {
        show(financeHost,   btnDetails);
        hide(analyticsHost, btnAnalyse);
        hide(calendarHost,  btnCalendrier);
        if (periodBadge != null) periodBadge.setText("Détails");
        setStatus("Vue : Détails");
    }

    @FXML
    public void showAnalyse() {
        hide(financeHost,   btnDetails);
        show(analyticsHost, btnAnalyse);
        hide(calendarHost,  btnCalendrier);
        if (periodBadge != null) periodBadge.setText("Analyses");
        setStatus("Vue : Analyse");
    }
    @FXML
    public void openMarketPrices() {
        try {

            FXMLLoader loader = new FXMLLoader(
                    CombinedDashboardController.class.getResource("/fxml/MarketPrices.fxml")
            );

            Parent root = loader.load();

            Stage stage = new Stage();
            stage.setTitle("Market Prices");
            stage.setScene(new Scene(root));

            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void showCalendrier() {
        hide(financeHost,   btnDetails);
        hide(analyticsHost, btnAnalyse);
        show(calendarHost,  btnCalendrier);
        if (periodBadge != null) periodBadge.setText("Calendrier");
        setStatus("Vue : Calendrier");
    }

    private void show(AnchorPane pane, Button btn) {
        if (pane != null) { pane.setVisible(true);  pane.setManaged(true);  }
        if (btn  != null)   btn.setStyle(BTN_ACTIVE);
    }

    private void hide(AnchorPane pane, Button btn) {
        if (pane != null) { pane.setVisible(false); pane.setManaged(false); }
        if (btn  != null)   btn.setStyle(BTN_IDLE);
    }

    /* ── Status bar ────────────────────────────────────────────── */

    private void updateStatusBar() {
        try {
            String date = LocalDate.now()
                    .format(DateTimeFormatter.ofPattern("EEEE d MMMM yyyy", Locale.FRENCH));
            date = date.substring(0, 1).toUpperCase() + date.substring(1);
            if (dateLabel != null) dateLabel.setText(date);
        } catch (Exception ignored) {}

        if (currentUser != null && statusLabel != null) {
            String name = (nvl(currentUser.getPrenom()) + " " + nvl(currentUser.getNom())).trim();
            statusLabel.setText("Connecté : " + (name.isEmpty() ? "Utilisateur" : name));
        }
    }

    private void setStatus(String msg) {
        Platform.runLater(() -> { if (statusLabel != null) statusLabel.setText(msg); });
    }

    /* ── Logout ────────────────────────────────────────────────── */

    @FXML
    public void logout() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/fxml/signin.fxml"));
            financeHost.getScene().setRoot(root);
        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR, "Erreur logout : " + e.getMessage()).showAndWait();
        }
    }

    /* ── Helpers ───────────────────────────────────────────────── */

    private String nvl(String s) { return s != null ? s.trim() : ""; }

    private javafx.scene.Node errorCard(String label, String msg) {
        Label title  = new Label("⚠  Erreur — " + label);
        title.setStyle("-fx-font-size:12px;-fx-font-weight:bold;-fx-text-fill:#C0392B;");
        Label detail = new Label(msg != null ? msg : "Inconnue");
        detail.setStyle("-fx-font-size:11px;-fx-text-fill:#922B21;");
        detail.setWrapText(true);
        VBox box = new VBox(8, title, detail);
        box.setStyle(
                "-fx-background-color:#FEF0EE;-fx-background-radius:12;" +
                        "-fx-border-color:#F5C6C0;-fx-border-radius:12;-fx-border-width:1;" +
                        "-fx-padding:20;-fx-alignment:center-left;"
        );
        AnchorPane.setTopAnchor(box, 24.0);
        AnchorPane.setLeftAnchor(box, 24.0);
        AnchorPane.setRightAnchor(box, 24.0);
        return box;
    }

    @FunctionalInterface
    interface LoaderConsumer {
        void accept(FXMLLoader loader) throws Exception;
    }
}