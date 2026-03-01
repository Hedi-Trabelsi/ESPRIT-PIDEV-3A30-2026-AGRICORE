package controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import models.Depense;
import models.User;
import models.Vente;
import services.DepenseService;
import services.VenteService;

import java.time.format.DateTimeFormatter;
import java.util.List;

public class FinanceTablesController {

    // ── FXML injections ──────────────────────────────────────────
    @FXML private Label      titleLabel;
    @FXML private VBox       depenseFeed;
    @FXML private VBox       venteFeed;
    @FXML private AnchorPane calendarHost;
    @FXML private GridPane   agriGrid;          // Agriculture panel grid
    @FXML private Button     backBtn;
    @FXML private VBox       calendarPanel;

    // ── Services ─────────────────────────────────────────────────
    private final DepenseService            depenseService  = new DepenseService();
    private final VenteService              venteService    = new VenteService();
    private final AgriculturePanelController agriController = new AgriculturePanelController();
    private final DateTimeFormatter         fmt             = DateTimeFormatter.ISO_DATE;

    private User user;
    private boolean embedded = false;

    public void setEmbedded(boolean embedded) {
        this.embedded = embedded;
        if (embedded) {
            if (backBtn != null) {
                backBtn.setVisible(false);
                backBtn.setManaged(false);
            }
            if (calendarPanel != null) {
                calendarPanel.setVisible(false);
                calendarPanel.setManaged(false);
            }
        }
    }

    // ────────────────────────────────────────────────────────────
    //  Entry point
    // ────────────────────────────────────────────────────────────
    public void setUser(User user) {
        this.user = user;

        if (titleLabel != null && user != null) {
            titleLabel.setText("Finances: " + user.getPrenom() + " " + user.getNom());
        }

        loadData();
        if (!embedded) {
            loadCalendar();
        }
        loadAgriPanel();   // ← new: starts async World Bank fetch
    }

    // ────────────────────────────────────────────────────────────
    //  Load depenses + ventes feeds
    // ────────────────────────────────────────────────────────────
    private void loadData() {
        try {
            List<Depense> ds = depenseService.readByUser(user.getId());
            renderDepenseFeed(ds);
            List<Vente> vs = venteService.readByUser(user.getId());
            renderVenteFeed(vs);
        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR, e.getMessage()).showAndWait();
        }
    }

    // ────────────────────────────────────────────────────────────
    //  Load embedded calendar
    // ────────────────────────────────────────────────────────────
    private void loadCalendar() {
        if (calendarHost == null) return;
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/UserCalendar.fxml"));
            Parent root = loader.load();

            UserCalendarController controller = loader.getController();
            controller.setUser(user);

            calendarHost.getChildren().setAll(root);
            AnchorPane.setTopAnchor(root,    0.0);
            AnchorPane.setBottomAnchor(root, 0.0);
            AnchorPane.setLeftAnchor(root,   0.0);
            AnchorPane.setRightAnchor(root,  0.0);
        } catch (Exception e) {
            // silently skip if calendar FXML is unavailable
        }
    }

    // ────────────────────────────────────────────────────────────
    //  Load agriculture panel (async World Bank API fetch)
    // ────────────────────────────────────────────────────────────
    private void loadAgriPanel() {
        if (agriGrid == null) return;
        agriController.attach(agriGrid);
    }

    // ────────────────────────────────────────────────────────────
    //  Feed renderers
    // ────────────────────────────────────────────────────────────
    private void renderDepenseFeed(List<Depense> depenses) {
        depenseFeed.getChildren().clear();
        if (depenses == null || depenses.isEmpty()) {
            Label empty = new Label("Aucune dépense enregistrée.");
            empty.getStyleClass().add("feed-empty");
            depenseFeed.getChildren().add(empty);
            return;
        }
        for (int i = 0; i < depenses.size(); i++) {
            depenseFeed.getChildren().add(
                    buildDepenseRow(depenses.get(i), i % 2 != 0));
        }
    }

    private void renderVenteFeed(List<Vente> ventes) {
        venteFeed.getChildren().clear();
        if (ventes == null || ventes.isEmpty()) {
            Label empty = new Label("Aucune vente enregistrée.");
            empty.getStyleClass().add("feed-empty");
            venteFeed.getChildren().add(empty);
            return;
        }
        for (int i = 0; i < ventes.size(); i++) {
            venteFeed.getChildren().add(
                    buildVenteRow(ventes.get(i), i % 2 != 0));
        }
    }

    // ────────────────────────────────────────────────────────────
    //  Row builders
    // ────────────────────────────────────────────────────────────
    private HBox buildDepenseRow(Depense d, boolean alt) {
        HBox row = new HBox();
        row.getStyleClass().add(alt ? "feed-row-alt" : "feed-row");
        row.setAlignment(Pos.CENTER_LEFT);

        Region spine = new Region();
        spine.getStyleClass().add("feed-spine-depense");

        VBox content = new VBox(3);
        content.getStyleClass().add("feed-row-content");
        HBox.setHgrow(content, Priority.ALWAYS);

        Label badge = new Label(d.getType() != null ? d.getType().name() : "AUTRE");
        badge.getStyleClass().add("feed-badge-depense");

        String dateStr = d.getDate() != null ? d.getDate().format(fmt) : "—";
        Label date = new Label("📅 " + dateStr);
        date.getStyleClass().add("feed-row-date");

        content.getChildren().addAll(badge, date);

        Label amount = new Label(String.format("− %.2f DT", d.getMontant()));
        amount.getStyleClass().add("feed-row-amount-depense");

        row.getChildren().addAll(spine, content, amount);
        return row;
    }

    private HBox buildVenteRow(Vente v, boolean alt) {
        HBox row = new HBox();
        row.getStyleClass().add(alt ? "feed-row-alt" : "feed-row");
        row.setAlignment(Pos.CENTER_LEFT);

        Region spine = new Region();
        spine.getStyleClass().add("feed-spine-vente");

        VBox content = new VBox(3);
        content.getStyleClass().add("feed-row-content");
        HBox.setHgrow(content, Priority.ALWAYS);

        HBox topRow = new HBox(6);
        topRow.setAlignment(Pos.CENTER_LEFT);

        Label badge = new Label(v.getProduit() != null ? v.getProduit() : "Produit");
        badge.getStyleClass().add("feed-badge-vente");

        Label meta = new Label(String.format("%.2f × %.0f u.",
                v.getPrixUnitaire(), v.getQuantite()));
        meta.getStyleClass().add("feed-row-date");

        topRow.getChildren().addAll(badge, meta);

        String dateStr = v.getDate() != null ? v.getDate().format(fmt) : "—";
        Label date = new Label("📅 " + dateStr);
        date.getStyleClass().add("feed-row-date");

        content.getChildren().addAll(topRow, date);

        Label amount = new Label(String.format("+ %.2f DT", v.getChiffreAffaires()));
        amount.getStyleClass().add("feed-row-amount-vente");

        row.getChildren().addAll(spine, content, amount);
        return row;
    }

    // ────────────────────────────────────────────────────────────
    //  Navigation
    // ────────────────────────────────────────────────────────────
    @FXML
    void goBack() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/ShowUsers.fxml"));
            Parent root = loader.load();
            titleLabel.getScene().setRoot(root);
        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR, e.getMessage()).showAndWait();
        }
    }
}
