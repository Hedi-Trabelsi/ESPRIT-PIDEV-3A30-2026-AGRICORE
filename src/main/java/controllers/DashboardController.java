package controllers;

import javafx.animation.*;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.util.Duration;
import models.EvennementAgricole;
import models.Participant;
import services.EvennementService;
import services.ParticipantService;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class DashboardController {

    @FXML
    private VBox mainContentVBox;

    private final EvennementService evService = new EvennementService();
    private final ParticipantService partService = new ParticipantService();
    private List<EvennementAgricole> allEvents = new ArrayList<>();
    private final Set<Integer> reservedEventIds = new HashSet<>();
    private final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd MMMM yyyy");

    private TextField searchInput;
    private DatePicker datePicker;
    private GridPane eventGrid;

    // Placeholder for logged-in user
    private final int CURRENT_USER_ID = 1;

    @FXML
    public void initialize() {
        mainContentVBox.setAlignment(Pos.TOP_CENTER);
        refreshData();
        showGestionEvenements();
    }

    private void refreshData() {
        try {
            allEvents = evService.read();

            // Load existing registrations from DB to sync the "INSCRIT" badges
            List<Participant> allParticipants = partService.read();
            reservedEventIds.clear();
            for (Participant p : allParticipants) {
                if (p.getIdUtilisateur() == CURRENT_USER_ID) {
                    reservedEventIds.add(p.getIdEvennement());
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private String getDynamicIcon(String title) {
        String t = title.toLowerCase();
        if (t.contains("formation")) return "🎓";
        if (t.contains("atelier") || t.contains("tech")) return "⚙️";
        if (t.contains("marche") || t.contains("vente")) return "🧺";
        if (t.contains("visite") || t.contains("ferme")) return "🚜";
        return "🌱";
    }

    private String getDynamicGradient(int index) {
        String[] gradients = {
                "linear-gradient(to bottom right, #7ca76f, #2d5a27)",
                "linear-gradient(to bottom right, #d4a373, #a98467)",
                "linear-gradient(to bottom right, #8ecae6, #219ebc)",
                "linear-gradient(to bottom right, #ffb703, #fb8500)"
        };
        return gradients[index % gradients.length];
    }

    // ===================== MAIN CATALOG VIEW =====================
    @FXML
    public void showGestionEvenements() {
        mainContentVBox.getChildren().clear();
        mainContentVBox.setSpacing(20);
        mainContentVBox.setPadding(new Insets(20, 40, 20, 40));

        Label title = new Label("Catalogue AgriCore");
        title.setStyle("-fx-font-size: 32px; -fx-font-weight: bold; -fx-text-fill: #1a3c1a;");

        HBox searchBar = new HBox(15);
        searchBar.setAlignment(Pos.CENTER);
        searchBar.setPadding(new Insets(10, 20, 10, 20));
        searchBar.setMaxWidth(750);
        searchBar.setStyle("-fx-background-color: white; -fx-background-radius: 30; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 10, 0, 0, 5);");

        searchInput = new TextField();
        searchInput.setPromptText("🔍 Rechercher un événement...");
        searchInput.setPrefWidth(300);
        searchInput.setStyle("-fx-text-fill: #333; -fx-background-color: transparent; -fx-border-color: #ddd; -fx-border-width: 0 1 0 0;");

        datePicker = new DatePicker();
        datePicker.setPromptText("Date précise");
        datePicker.setStyle("-fx-background-color: white;");

        Button btnReset = new Button("Réinitialiser");
        btnReset.setStyle("-fx-background-radius: 20; -fx-cursor: hand; -fx-background-color: #f4f4f4; -fx-text-fill: #555;");
        btnReset.setOnAction(e -> { searchInput.clear(); datePicker.setValue(null); updateFilters(); });

        searchBar.getChildren().addAll(searchInput, datePicker, btnReset);

        eventGrid = new GridPane();
        eventGrid.setHgap(25);
        eventGrid.setVgap(25);
        eventGrid.setAlignment(Pos.TOP_CENTER);
        eventGrid.setMaxWidth(800);

        ColumnConstraints col = new ColumnConstraints();
        col.setPercentWidth(50);
        eventGrid.getColumnConstraints().setAll(col, col);

        searchInput.textProperty().addListener((obs, old, val) -> updateFilters());
        datePicker.valueProperty().addListener((obs, old, val) -> updateFilters());

        mainContentVBox.getChildren().addAll(title, searchBar, eventGrid);
        updateFilters();
    }

    private void updateFilters() {
        eventGrid.getChildren().clear();
        String query = searchInput.getText().toLowerCase();
        LocalDate selectedDate = datePicker.getValue();

        List<EvennementAgricole> filtered = allEvents.stream()
                .filter(e -> {
                    boolean matchesTitle = e.getTitre().toLowerCase().contains(query);
                    boolean matchesDate = (selectedDate == null) || e.getDateDebut().isEqual(selectedDate);
                    return matchesTitle && matchesDate;
                }).collect(Collectors.toList());

        if (filtered.isEmpty()) {
            Label noRes = new Label("∅ Aucun événement trouvé.");
            noRes.setStyle("-fx-text-fill: #999; -fx-font-size: 16px; -fx-padding: 50 0 0 0;");
            eventGrid.add(noRes, 0, 0, 2, 1);
            GridPane.setHalignment(noRes, javafx.geometry.HPos.CENTER);
        } else {
            for (int i = 0; i < filtered.size(); i++) {
                eventGrid.add(createPerfectCard(filtered.get(i), i), i % 2, i / 2);
            }
        }
    }

    private StackPane createPerfectCard(EvennementAgricole ev, int index) {
        StackPane container = new StackPane();
        String gradient = getDynamicGradient(index);

        VBox card = new VBox(0);
        card.setPrefSize(340, 360);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 25;");
        card.setEffect(new DropShadow(15, Color.rgb(0,0,0,0.1)));
        card.setCursor(Cursor.HAND);

        VBox top = new VBox(new Label(getDynamicIcon(ev.getTitre())) {{ setStyle("-fx-font-size: 40px;"); }});
        top.setAlignment(Pos.CENTER);
        top.setPrefHeight(100);
        top.setStyle("-fx-background-color: " + gradient + "; -fx-background-radius: 25 25 0 0;");

        VBox info = new VBox(10);
        info.setPadding(new Insets(20));

        Label dateLbl = new Label("📅 " + ev.getDateDebut().format(dtf));
        dateLbl.setStyle("-fx-text-fill: #1a3c1a; -fx-font-weight: bold; -fx-font-size: 12px;");

        Label titleLbl = new Label(ev.getTitre());
        titleLbl.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #333;");
        titleLbl.setWrapText(true);
        titleLbl.setMinHeight(50);

        Label lieuLbl = new Label("📍 " + ev.getLieu());
        lieuLbl.setStyle("-fx-text-fill: #666; -fx-font-size: 14px;");

        info.getChildren().addAll(dateLbl, titleLbl, lieuLbl);

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        HBox footer = new HBox(new Label(ev.getFraisInscription() + " DT") {{
            setStyle("-fx-font-size: 22px; -fx-font-weight: 900; -fx-text-fill: #1a3c1a;");
        }});
        footer.setPadding(new Insets(0, 20, 20, 20));

        card.getChildren().addAll(top, info, spacer, footer);
        container.getChildren().add(card);

        if (reservedEventIds.contains(ev.getIdEvennement())) {
            Label badge = new Label("INSCRIT");
            badge.setStyle("-fx-background-color: #1a3c1a; -fx-text-fill: white; -fx-font-size: 11px; -fx-font-weight: bold; -fx-padding: 5 12; -fx-background-radius: 0 25 0 15;");
            StackPane.setAlignment(badge, Pos.TOP_RIGHT);
            container.getChildren().add(badge);
            card.setOpacity(0.75);
        }

        card.setOnMouseEntered(e -> {
            ScaleTransition st = new ScaleTransition(Duration.millis(200), card);
            st.setToX(1.03); st.setToY(1.03);
            st.play();
        });
        card.setOnMouseExited(e -> {
            ScaleTransition st = new ScaleTransition(Duration.millis(200), card);
            st.setToX(1.0); st.setToY(1.0);
            st.play();
        });

        card.setOnMouseClicked(e -> showEventDetails(ev, gradient));
        return container;
    }

    // ===================== DETAIL VIEW =====================
    private void showEventDetails(EvennementAgricole ev, String gradient) {
        mainContentVBox.getChildren().clear();

        VBox detailBox = new VBox(0);
        detailBox.setMaxWidth(750);
        detailBox.setStyle("-fx-background-color: white; -fx-background-radius: 30; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.2), 20, 0, 0, 10);");

        VBox header = new VBox(15);
        header.setPadding(new Insets(30));
        header.setAlignment(Pos.CENTER);
        header.setStyle("-fx-background-color: " + gradient + "; -fx-background-radius: 30 30 0 0;");

        Button btnBack = new Button("← Retour au catalogue");
        btnBack.setStyle("-fx-background-color: rgba(255,255,255,0.2); -fx-text-fill: white; -fx-background-radius: 20; -fx-cursor: hand;");
        btnBack.setOnAction(e -> showGestionEvenements());

        Label title = new Label(ev.getTitre());
        title.setStyle("-fx-font-size: 28px; -fx-font-weight: bold; -fx-text-fill: white;");
        header.getChildren().addAll(btnBack, new Label(getDynamicIcon(ev.getTitre())) {{ setStyle("-fx-font-size: 50px;"); }}, title);

        GridPane body = new GridPane();
        body.setPadding(new Insets(30));
        body.setHgap(30);

        VBox leftSide = new VBox(15);
        Label descHeader = new Label("À propos de l'événement");
        descHeader.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #1a3c1a;");
        Text description = new Text(ev.getDescription());
        description.setWrappingWidth(400);
        description.setStyle("-fx-fill: #444; -fx-font-size: 15px; -fx-line-spacing: 5px;");
        leftSide.getChildren().addAll(descHeader, description);

        VBox rightSide = new VBox(15);
        rightSide.setPadding(new Insets(20));
        rightSide.setStyle("-fx-background-color: #f8fbf8; -fx-background-radius: 20; -fx-border-color: #eef2ee; -fx-border-radius: 20;");
        rightSide.setMinWidth(240);

        rightSide.getChildren().addAll(
                createInfoRow("📅 Date", ev.getDateDebut().format(dtf)),
                createInfoRow("📍 Lieu", ev.getLieu()),
                createInfoRow("💰 Frais", ev.getFraisInscription() + " DT")
        );

        boolean res = reservedEventIds.contains(ev.getIdEvennement());
        Button actionBtn = new Button(res ? "Annuler l'inscription" : "S'inscrire maintenant");
        actionBtn.setMaxWidth(Double.MAX_VALUE);
        actionBtn.setCursor(Cursor.HAND);
        actionBtn.setStyle(res ? "-fx-background-color: #ffebee; -fx-text-fill: #c62828; -fx-font-weight: bold; -fx-padding: 12;" :
                "-fx-background-color: #1a3c1a; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 12;");

        actionBtn.setOnAction(e -> triggerStatusAnimation(ev, !res));
        rightSide.getChildren().add(actionBtn);

        body.add(leftSide, 0, 0);
        body.add(rightSide, 1, 0);

        detailBox.getChildren().addAll(header, body);
        mainContentVBox.getChildren().add(detailBox);
    }

    private HBox createInfoRow(String label, String value) {
        VBox v = new VBox(2);
        Label lbl = new Label(label);
        lbl.setStyle("-fx-text-fill: #888; -fx-font-size: 12px;");
        Label val = new Label(value);
        val.setStyle("-fx-text-fill: #333; -fx-font-weight: bold; -fx-font-size: 14px;");
        v.getChildren().addAll(lbl, val);
        return new HBox(10, v);
    }

    // ===================== DB SYNC + ANIMATION =====================
    private void triggerStatusAnimation(EvennementAgricole ev, boolean adding) {
        try {
            if (adding) {
                // DB CREATE
                Participant p = new Participant(
                        CURRENT_USER_ID,
                        ev.getIdEvennement(),
                        LocalDate.now(),
                        "Confirmé",
                        String.valueOf(ev.getFraisInscription()),
                        "OUI"
                );
                partService.create(p);
                reservedEventIds.add(ev.getIdEvennement());
            } else {
                // DB DELETE
                List<Participant> all = partService.read();
                for (Participant p : all) {
                    if (p.getIdEvennement() == ev.getIdEvennement() && p.getIdUtilisateur() == CURRENT_USER_ID) {
                        partService.delete(p.getIdParticipant());
                        break;
                    }
                }
                reservedEventIds.remove(ev.getIdEvennement());
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return;
        }

        // --- ANIMATION UI ---
        Pane root = (Pane) mainContentVBox.getScene().getRoot();
        StackPane glassPane = new StackPane();
        glassPane.setStyle("-fx-background-color: rgba(0,0,0,0.4);");
        glassPane.prefWidthProperty().bind(root.widthProperty());
        glassPane.prefHeightProperty().bind(root.heightProperty());

        VBox overlayCard = new VBox(15);
        overlayCard.setAlignment(Pos.CENTER);
        overlayCard.setMaxSize(300, 250);
        overlayCard.setStyle("-fx-background-color: white; -fx-background-radius: 30; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.4), 20, 0, 0, 10);");

        Label icon = new Label(adding ? "✓" : "✕");
        icon.setStyle("-fx-font-size: 100px; -fx-text-fill: " + (adding ? "#2d5a27" : "#c62828") + "; -fx-font-weight: bold;");

        Label msg = new Label(adding ? "Confirmé !" : "Annulé !");
        msg.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: #333;");

        overlayCard.getChildren().addAll(icon, msg);
        glassPane.getChildren().add(overlayCard);

        root.getChildren().add(glassPane);
        glassPane.toFront();

        overlayCard.setScaleX(0);
        overlayCard.setScaleY(0);
        glassPane.setOpacity(0);

        FadeTransition bgFade = new FadeTransition(Duration.millis(200), glassPane);
        bgFade.setToValue(1);

        ScaleTransition pop = new ScaleTransition(Duration.millis(400), overlayCard);
        pop.setToX(1.0);
        pop.setToY(1.0);
        pop.setInterpolator(Interpolator.SPLINE(0.25, 0.1, 0.25, 1.0));

        FadeTransition exitFade = new FadeTransition(Duration.millis(300), glassPane);
        exitFade.setDelay(Duration.seconds(0.8));
        exitFade.setToValue(0);

        SequentialTransition seq = new SequentialTransition(new ParallelTransition(bgFade, pop), new PauseTransition(Duration.seconds(0.5)), exitFade);

        seq.setOnFinished(e -> {
            root.getChildren().remove(glassPane);
            showGestionEvenements(); // Reload to refresh the "INSCRIT" badges
        });

        seq.play();
    }
}