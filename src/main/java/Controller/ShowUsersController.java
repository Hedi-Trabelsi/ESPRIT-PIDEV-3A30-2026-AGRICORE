package Controller;

import java.net.URL;
import java.sql.SQLException;
import java.util.List;
import java.util.ResourceBundle;

import Model.Utilisateur;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import Model.User;
import services.UserService;

public class ShowUsersController {

    private final UserService us;

    {
        try {
            us = new UserService();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @FXML private ResourceBundle resources;
    @FXML private URL location;

    @FXML private FlowPane  cardsPane;
    @FXML private TextField searchField;
    @FXML private Label     countLabel;
    @FXML private Label     visibleBadge;
    @FXML private Label     statActifs;
    @FXML private Label     statDepenses;
    @FXML private Label     statRoles;

    private List<Utilisateur> allUsers;

    /*
     * LIGHT PALETTE — 5 tones, unified
     *   page bg      #F4F7F5
     *   surface 1    #FFFFFF   (topbar, card hover)
     *   surface 2    #E8F0EB   (stats)
     *   border       #C9DDD2
     *   accent dark  #1A5C38
     *   accent mid   #2E8B57
     *   accent light #52B788
     *   muted text   #6B9A80
     *   ghost text   #9DBFAD
     */

    // Avatar gradients — vivid, stand out on light bg
    private static final String[] AV = {
            "linear-gradient(135deg,#1A6B45,#2E8B57)",
            "linear-gradient(135deg,#1D5A8E,#2980D4)",
            "linear-gradient(135deg,#7B2E6B,#C44FA0)",
            "linear-gradient(135deg,#8B5213,#D48A3A)",
            "linear-gradient(135deg,#2C5282,#4299E1)",
            "linear-gradient(135deg,#276749,#52B788)"
    };

    // Stripe: soft, semi-transparent to not overwhelm light cards
    private static final String[] STRIPES = {
            "linear-gradient(to right,#52B788,#A8DCBE,transparent)",
            "linear-gradient(to right,#2980D4,#90C3F8,transparent)",
            "linear-gradient(to right,#C44FA0,#EAA8D4,transparent)",
            "linear-gradient(to right,#D48A3A,#F0C080,transparent)",
            "linear-gradient(to right,#4299E1,#90CAF9,transparent)",
            "linear-gradient(to right,#2E8B57,#7ECBA0,transparent)"
    };

    // Role badge: [text, background, border] — all soft tones
    private static final String[][] ROLE = {
            {"#1A5C38", "#D4EDE0", "#52B788"},
            {"#1D4D8E", "#DBEAFE", "#93C5FD"},
            {"#7B2E6B", "#FAE8F5", "#DDA0CC"},
            {"#8B4513", "#FEF0DC", "#F5C888"},
            {"#2C5282", "#DBEAFE", "#93C5FD"},
            {"#1A5C38", "#D4EDE0", "#52B788"}
    };

    @FXML
    void initialize() {
        refreshCards();
        if (searchField != null)
            searchField.textProperty().addListener((obs, o, n) -> renderFiltered(n));
    }

    @FXML
    public void refreshCards() {
        try {
            allUsers = us.read();
            renderFiltered(searchField != null ? searchField.getText() : null);
            updateStats();
        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR, e.getMessage()).showAndWait();
        }
    }

    private void updateStats() {
        int n = allUsers.size();
        if (countLabel != null)
            countLabel.setText(n + " membre" + (n > 1 ? "s" : "") + " enregistré" + (n > 1 ? "s" : ""));
        if (statRoles  != null) statRoles.setText(String.valueOf(n));
        if (statActifs != null) statActifs.setText(String.valueOf(Math.max(1, n - 1)));
    }

    private void renderFiltered(String query) {
        cardsPane.getChildren().clear();
        String q = query == null ? "" : query.trim().toLowerCase();
        int idx = 0, visible = 0;
        for (Utilisateur u : allUsers) {
            String name = ((u.getPrenom() != null ? u.getPrenom() : "") + " "
                    + (u.getNom()    != null ? u.getNom()    : "")).toLowerCase();
            if (q.isEmpty() || name.contains(q)) {
                cardsPane.getChildren().add(buildCard(u, idx++));
                visible++;
            }
        }
        if (visibleBadge != null) visibleBadge.setText(visible + " affiché(s)");
        if (countLabel   != null && !q.isEmpty()) countLabel.setText(visible + " membre(s) trouvé(s)");
    }

    // ── Card builder ────────────────────────────────────────────
    private Node buildCard(Utilisateur user, int idx) {
        int ci = idx % AV.length;

        String fn = user.getPrenom() != null ? user.getPrenom().trim() : "";
        String ln = user.getNom()    != null ? user.getNom().trim()    : "";
        String initials = "";
        if (!fn.isEmpty()) initials += fn.substring(0,1).toUpperCase();
        if (!ln.isEmpty()) initials += ln.substring(0,1).toUpperCase();
        if (initials.isEmpty()) initials = "U";

        // Stripe — 3px top accent
        Region stripe = new Region();
        stripe.setPrefHeight(3);
        stripe.setMaxWidth(Double.MAX_VALUE);
        stripe.setStyle("-fx-background-color:" + STRIPES[ci] + "; -fx-opacity:0.75;");

        // Avatar
        StackPane avatar = new StackPane();
        avatar.setPrefSize(50,50); avatar.setMinSize(50,50); avatar.setMaxSize(50,50);
        avatar.setStyle(
                "-fx-background-color:" + AV[ci] + ";" +
                        "-fx-background-radius:13;" +
                        "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.14),7,0,0,2);"
        );
        Label initLbl = new Label(initials);
        initLbl.setStyle("-fx-text-fill:white; -fx-font-weight:bold; -fx-font-size:15px;");

        if (idx == 0) {
            StackPane dot = new StackPane();
            dot.setPrefSize(10,10);
            dot.setStyle(
                    "-fx-background-color:#2E8B57; -fx-background-radius:5;" +
                            "-fx-border-color:#FFFFFF; -fx-border-radius:5; -fx-border-width:2;"
            );
            StackPane.setAlignment(dot, Pos.BOTTOM_RIGHT);
            avatar.getChildren().addAll(initLbl, dot);
        } else {
            avatar.getChildren().add(initLbl);
        }

        // Name
        String fullName = (fn + " " + ln).trim();
        Label nameLbl = new Label(fullName.isEmpty() ? "Utilisateur" : fullName);
        nameLbl.setStyle("-fx-font-size:14px; -fx-font-weight:bold; -fx-text-fill:#1A5C38;");
        nameLbl.setMaxWidth(172);

        // Handle
        Label handleLbl = new Label("@" + fn.toLowerCase() + "." + ln.toLowerCase() + " · #00" + (idx+1));
        handleLbl.setStyle("-fx-font-size:10px; -fx-text-fill:#9DBFAD;");

        // Role badge
        String roleText = switch (user.getRole()) {
            case 0 -> "Admin";
            case 1 -> "Agriculteur";
            case 2 -> "Technicien";
            case 3 -> "Fournisseur";
            case 4 -> "Financier";
            default -> "Membre";
        };
        Label roleLbl = new Label(roleText);
        roleLbl.setStyle(
                "-fx-font-size:10px; -fx-font-weight:700;" +
                        "-fx-text-fill:" + ROLE[ci][0] + ";" +
                        "-fx-background-color:" + ROLE[ci][1] + "; -fx-background-radius:20;" +
                        "-fx-border-color:" + ROLE[ci][2] + "; -fx-border-radius:20; -fx-border-width:1;" +
                        "-fx-padding:2 9;"
        );

        VBox infoBox = new VBox(3, nameLbl, handleLbl, roleLbl);
        infoBox.setAlignment(Pos.CENTER_LEFT);

        HBox cardBody = new HBox(12, avatar, infoBox);
        cardBody.setAlignment(Pos.TOP_LEFT);
        cardBody.setPadding(new Insets(15,15,0,15));

        // Meta row
        HBox metaRow = new HBox(20,
                metaItem("—",    "DÉPENSES"),
                metaItem("—",    "VENTES"),
                metaItem("— DT", "TOTAL")
        );
        metaRow.setPadding(new Insets(11,15,11,15));
        metaRow.setStyle(
                "-fx-border-color:transparent transparent #D8EBE1 transparent;" +
                        "-fx-border-width:0 0 1 0;"
        );

        // Convert Utilisateur to finance User DTO for finance navigation
        System.out.println("[DEBUG] ShowUsers - Building card for user id=" + user.getId() + " name=" + user.getPrenom() + " " + user.getNom());
        User financeUser = new User(user.getId(), user.getPrenom(), user.getNom());

        // Action buttons — Calendrier excluded from this view
        Button bO = btn("🛠 Outils",   "primary", e -> openUserOperations(financeUser));
        Button bA = btn("📊 Analyses", "ghost",   e -> openUserAnalytics(financeUser));
        Button bD = btn("📋 Détails",  "ghost",   e -> openFinanceFor(financeUser));

        HBox actRow = new HBox(6, bO, bA, bD);
        actRow.setAlignment(Pos.CENTER_LEFT);
        actRow.setPadding(new Insets(11,15,15,15));

        // Assemble
        VBox card = new VBox(0, stripe, cardBody, metaRow, actRow);
        card.setPrefWidth(288);
        card.setStyle(cNormal());

        card.setOnMouseEntered(e -> {
            card.setStyle(cHover());
            stripe.setStyle("-fx-background-color:" + STRIPES[ci] + "; -fx-opacity:1.0;");
        });
        card.setOnMouseExited(e -> {
            card.setStyle(cNormal());
            stripe.setStyle("-fx-background-color:" + STRIPES[ci] + "; -fx-opacity:0.75;");
        });

        return card;
    }

    private VBox metaItem(String val, String lbl) {
        Label v = new Label(val);
        v.setStyle("-fx-font-size:14px; -fx-font-weight:bold; -fx-text-fill:#1A5C38;");
        Label l = new Label(lbl);
        l.setStyle("-fx-font-size:9px; -fx-text-fill:#9DBFAD; -fx-font-weight:700;");
        VBox b = new VBox(1, v, l);
        b.setAlignment(Pos.CENTER);
        return b;
    }

    private Button btn(String text, String type,
                       javafx.event.EventHandler<javafx.event.ActionEvent> handler) {
        Button b = new Button(text);
        b.setOnAction(handler);
        String n, h;
        switch (type) {
            case "primary" -> {
                n = "-fx-background-color:linear-gradient(to bottom,#2E8B57,#1A5C38);" +
                        "-fx-text-fill:white; -fx-font-size:11px; -fx-font-weight:700;" +
                        "-fx-background-radius:7; -fx-padding:6 11; -fx-cursor:hand;" +
                        "-fx-effect:dropshadow(gaussian,rgba(26,92,56,0.22),6,0,0,1);";
                h = "-fx-background-color:linear-gradient(to bottom,#3AAA6A,#2E8B57);" +
                        "-fx-text-fill:white; -fx-font-size:11px; -fx-font-weight:700;" +
                        "-fx-background-radius:7; -fx-padding:6 11; -fx-cursor:hand;" +
                        "-fx-translate-y:-1;" +
                        "-fx-effect:dropshadow(gaussian,rgba(46,139,87,0.30),9,0,0,2);";
            }
            case "ghost" -> {
                // soft green tint — clearly part of the same palette
                n = "-fx-background-color:#E8F0EB;" +
                        "-fx-text-fill:#1A5C38; -fx-font-size:11px; -fx-font-weight:600;" +
                        "-fx-border-color:#C9DDD2; -fx-border-radius:7;" +
                        "-fx-background-radius:7; -fx-padding:6 11; -fx-cursor:hand;";
                h = "-fx-background-color:#D4EDE0;" +
                        "-fx-text-fill:#1A5C38; -fx-font-size:11px; -fx-font-weight:700;" +
                        "-fx-border-color:#52B788; -fx-border-radius:7;" +
                        "-fx-background-radius:7; -fx-padding:6 11; -fx-cursor:hand;";
            }
            default -> { // outline
                n = "-fx-background-color:transparent;" +
                        "-fx-text-fill:#6B9A80; -fx-font-size:11px; -fx-font-weight:600;" +
                        "-fx-border-color:#C9DDD2; -fx-border-radius:7;" +
                        "-fx-background-radius:7; -fx-padding:6 11; -fx-cursor:hand;";
                h = "-fx-background-color:#E8F0EB;" +
                        "-fx-text-fill:#1A5C38; -fx-font-size:11px; -fx-font-weight:700;" +
                        "-fx-border-color:#52B788; -fx-border-radius:7;" +
                        "-fx-background-radius:7; -fx-padding:6 11; -fx-cursor:hand;";
            }
        }
        final String fn = n, fh = h;
        b.setStyle(fn);
        b.setOnMouseEntered(e -> b.setStyle(fh));
        b.setOnMouseExited(e  -> b.setStyle(fn));
        return b;
    }

    // Card states
    private String cNormal() {
        return "-fx-background-color:#FFFFFF;" +       // pure white card
                "-fx-background-radius:12;" +
                "-fx-border-color:#C9DDD2;" +           // soft sage border
                "-fx-border-radius:12; -fx-border-width:1;" +
                "-fx-effect:dropshadow(gaussian,rgba(26,92,56,0.09),10,0,0,3);" +
                "-fx-cursor:hand;";
    }
    private String cHover() {
        return "-fx-background-color:#FFFFFF;" +
                "-fx-background-radius:12;" +
                "-fx-border-color:#2E8B57;" +           // accent border on hover
                "-fx-border-radius:12; -fx-border-width:1.5;" +
                "-fx-effect:dropshadow(gaussian,rgba(46,139,87,0.20),16,0,0,5);" +
                "-fx-cursor:hand; -fx-translate-y:-2;";
    }

    // ── Navigation ──────────────────────────────────────────────
    private void openFinanceFor(User u) {
        nav("/fxml/FinanceTables.fxml", l -> { FinanceTablesController c = l.getController(); c.setUser(u); });
    }
    private void openUserOperations(User u) {
        nav("/fxml/UserOperations.fxml", l -> { UserOperationsController c = l.getController(); c.setUser(u); });
    }
    private void openUserCalendar(User u) {
        nav("/fxml/UserCalendar.fxml", l -> { UserCalendarController c = l.getController(); c.setUser(u); });
    }
    private void openUserAnalytics(User u) {
        nav("/fxml/UserAnalytics.fxml", l -> { UserAnalyticsController c = l.getController(); c.setUser(u); });
    }

    @FunctionalInterface interface LC { void accept(javafx.fxml.FXMLLoader l) throws Exception; }

    private void nav(String fxml, LC setup) {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource(fxml));
            javafx.scene.Parent root = loader.load();
            setup.accept(loader);
            NavigationUtil.loadInContentArea(cardsPane, root);
        } catch (Exception ex) {
            new Alert(Alert.AlertType.ERROR, ex.getMessage()).showAndWait();
        }
    }
}