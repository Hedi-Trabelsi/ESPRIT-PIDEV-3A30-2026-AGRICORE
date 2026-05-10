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
    @FXML private ComboBox<String> statusFilter;

    private List<Utilisateur> allUsers;
    /** "Tous" | "Actifs" | "Bannis" — drives the status filter. */
    private String statusMode = "Tous";

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

    // Avatar gradients — agricultural palette: forest, leaf, wheat, earth, moss, sage
    private static final String[] AV = {
            "linear-gradient(135deg,#1F4E3B,#4A8B6F)",
            "linear-gradient(135deg,#2E5E47,#7FB89A)",
            "linear-gradient(135deg,#8C6A47,#C9A96E)",
            "linear-gradient(135deg,#4A6B3E,#8FAE6E)",
            "linear-gradient(135deg,#3E8266,#7FB89A)",
            "linear-gradient(135deg,#5C7A56,#A8C8B8)"
    };

    // Stripe: soft, leaf-toned gradients
    private static final String[] STRIPES = {
            "linear-gradient(to right,#4A8B6F,#C7DDD0,transparent)",
            "linear-gradient(to right,#7FB89A,#EAF2EC,transparent)",
            "linear-gradient(to right,#C9A96E,#E8DCC4,transparent)",
            "linear-gradient(to right,#8FAE6E,#D4DCB4,transparent)",
            "linear-gradient(to right,#3E8266,#A8C8B8,transparent)",
            "linear-gradient(to right,#A8C8B8,#EAF2EC,transparent)"
    };

    // Role badge: [text, background, border] — muted earthy/leaf tones
    private static final String[][] ROLE = {
            {"#1F4E3B", "#EAF2EC", "#4A8B6F"},
            {"#2E5E47", "#EAF2EC", "#7FB89A"},
            {"#8C6A47", "#FAF6EC", "#C9A96E"},
            {"#4A6B3E", "#F0F4E8", "#8FAE6E"},
            {"#3E8266", "#EAF2EC", "#7FB89A"},
            {"#1F4E3B", "#EAF2EC", "#A8C8B8"}
    };

    @FXML
    void initialize() {
        if (statusFilter != null) {
            statusFilter.getItems().setAll("Tous", "Actifs", "Bannis");
            statusFilter.setValue("Tous");
            statusFilter.valueProperty().addListener((obs, o, n) -> {
                statusMode = n != null ? n : "Tous";
                renderFiltered(searchField != null ? searchField.getText() : null);
            });
        }
        refreshCards();
        if (searchField != null)
            searchField.textProperty().addListener((obs, o, n) -> renderFiltered(n));
    }

    private boolean matchesStatus(Utilisateur u) {
        return switch (statusMode) {
            case "Actifs" -> !u.isBanned();
            case "Bannis" -> u.isBanned();
            default -> true;
        };
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
        long banned = allUsers.stream().filter(Utilisateur::isBanned).count();
        long active = n - banned;
        if (countLabel != null)
            countLabel.setText(n + " membre" + (n > 1 ? "s" : "") + " enregistré" + (n > 1 ? "s" : ""));
        if (statRoles  != null) statRoles.setText(String.valueOf(n));
        if (statActifs != null) statActifs.setText(String.valueOf(active));
        if (statDepenses != null) statDepenses.setText(String.valueOf(banned));
    }

    private void renderFiltered(String query) {
        cardsPane.getChildren().clear();
        String q = query == null ? "" : query.trim().toLowerCase();
        int idx = 0, visible = 0;
        for (Utilisateur u : allUsers) {
            if (!matchesStatus(u)) continue;
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
        if (user.isBanned()) {
            nameLbl.setStyle("-fx-font-size:14px; -fx-font-weight:bold; -fx-text-fill:#A04040; -fx-strikethrough:true;");
        } else {
            nameLbl.setStyle("-fx-font-size:14px; -fx-font-weight:bold; -fx-text-fill:#1A5C38;");
        }
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
        if (user.isBanned()) roleText = roleText + " · BANNI";
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

        // Action buttons — Calendrier excluded from this view.
        // Three regular actions on row 1; the ban toggle gets its own full-width
        // row with strong styling so admins can't miss it.
        Button bO = btn("🛠 Outils",   "primary", e -> openUserOperations(financeUser));
        Button bA = btn("📊 Analyses", "ghost",   e -> openUserAnalytics(financeUser));
        Button bD = btn("📋 Détails",  "ghost",   e -> openFinanceFor(financeUser));

        HBox actRow = new HBox(6, bO, bA, bD);
        actRow.setAlignment(Pos.CENTER_LEFT);
        actRow.setPadding(new Insets(11,15,8,15));

        Button bBan = new Button(user.isBanned() ? "✓  Débannir le compte" : "🚫  Suspendre le compte");
        bBan.setMaxWidth(Double.MAX_VALUE);
        bBan.setStyle(banButtonStyle(user.isBanned(), false));
        bBan.setOnMouseEntered(e -> bBan.setStyle(banButtonStyle(user.isBanned(), true)));
        bBan.setOnMouseExited(e  -> bBan.setStyle(banButtonStyle(user.isBanned(), false)));
        bBan.setOnAction(e -> toggleBan(user));

        HBox banRow = new HBox(bBan);
        banRow.setPadding(new Insets(0,15,15,15));

        // Assemble
        VBox card = new VBox(0, stripe, cardBody, metaRow, actRow, banRow);
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

    /**
     * Visual style for the ban / unban full-width button.
     * Banned users → green "Débannir" CTA. Active users → red "Suspendre" CTA.
     */
    private String banButtonStyle(boolean isBanned, boolean hover) {
        String base = "-fx-text-fill: white;" +
                "-fx-font-size: 12.5px;" +
                "-fx-font-weight: 700;" +
                "-fx-background-radius: 8;" +
                "-fx-padding: 9 0;" +
                "-fx-cursor: hand;" +
                "-fx-border-width: 0;";
        if (isBanned) {
            return base + "-fx-background-color: linear-gradient(to bottom,"
                    + (hover ? "#3FA478,#2E7B58" : "#3E8266,#1F4E3B") + ");"
                    + "-fx-effect: dropshadow(gaussian, rgba(31,78,59,0.30), 10, 0.05, 0, 3);";
        }
        return base + "-fx-background-color: linear-gradient(to bottom,"
                + (hover ? "#D14538,#A63329" : "#B5413A,#8C2E27") + ");"
                + "-fx-effect: dropshadow(gaussian, rgba(181,65,58,0.32), 10, 0.05, 0, 3);";
    }

    // Card states — paper surface with subtle shadow, leaf border on hover
    private String cNormal() {
        return "-fx-background-color:#FDFDFB;" +
                "-fx-background-radius:16;" +
                "-fx-border-color:#DCE8E0;" +
                "-fx-border-radius:16; -fx-border-width:1;" +
                "-fx-effect:dropshadow(gaussian,rgba(14,44,32,0.08),18,0.05,0,6);" +
                "-fx-cursor:hand;";
    }
    private String cHover() {
        return "-fx-background-color:#FDFDFB;" +
                "-fx-background-radius:16;" +
                "-fx-border-color:#4A8B6F;" +
                "-fx-border-radius:16; -fx-border-width:1.5;" +
                "-fx-effect:dropshadow(gaussian,rgba(74,139,111,0.22),22,0.08,0,8);" +
                "-fx-cursor:hand; -fx-translate-y:-3;";
    }

    private void toggleBan(Utilisateur u) {
        boolean banning = !u.isBanned();
        String fullName = ((u.getPrenom() != null ? u.getPrenom() : "") + " "
                         + (u.getNom() != null ? u.getNom() : "")).trim();
        if (fullName.isEmpty()) fullName = "cet utilisateur";

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle(banning ? "Suspendre le compte" : "Réactiver le compte");
        confirm.setHeaderText((banning ? "Suspendre " : "Réactiver ") + fullName + " ?");
        confirm.setContentText(banning
                ? "L'utilisateur ne pourra plus se connecter (mot de passe ou Face ID) tant qu'il est suspendu. Vous pourrez le réactiver à tout moment."
                : "L'utilisateur pourra à nouveau se connecter et accéder à son compte.");
        ButtonType okBtn = new ButtonType(banning ? "🚫 Suspendre" : "✓ Réactiver", javafx.scene.control.ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelBtn = new ButtonType("Annuler", javafx.scene.control.ButtonBar.ButtonData.CANCEL_CLOSE);
        confirm.getButtonTypes().setAll(okBtn, cancelBtn);

        confirm.showAndWait().ifPresent(response -> {
            if (response != okBtn) return;
            try {
                u.setBanned(banning);
                us.update(u);
                refreshCards();
            } catch (Exception ex) {
                new Alert(Alert.AlertType.ERROR, "Échec de la mise à jour: " + ex.getMessage()).showAndWait();
            }
        });
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