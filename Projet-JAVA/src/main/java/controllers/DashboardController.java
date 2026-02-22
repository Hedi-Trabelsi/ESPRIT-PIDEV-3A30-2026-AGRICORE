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
import javafx.scene.shape.Circle;
import javafx.scene.text.Text;
import javafx.scene.web.WebView;
import javafx.util.Duration;
import models.EvennementAgricole;
import models.Participant;
import services.EvennementService;
import services.ParticipantService;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.YearMonth;
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
    private Slider priceSlider;
    private Label priceValueLabel;
    private GridPane eventGrid;

    private YearMonth currentYearMonth = YearMonth.now();
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

    // --- FULL CALENDAR VIEW (Shows Past and Future) ---
    private void showFullCalendarView() {
        mainContentVBox.getChildren().clear();
        mainContentVBox.setSpacing(15);
        mainContentVBox.setPadding(new Insets(20, 40, 20, 40));

        HBox header = new HBox(20);
        header.setAlignment(Pos.CENTER_LEFT);
        Button btnBack = new Button("← Retour");
        btnBack.setStyle("-fx-background-color: #1a3c1a; -fx-text-fill: white; -fx-background-radius: 20; -fx-cursor: hand; -fx-padding: 8 15;");
        btnBack.setOnAction(e -> showGestionEvenements());

        Label calTitle = new Label("Calendrier AgriCore");
        calTitle.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: #1a3c1a;");
        header.getChildren().addAll(btnBack, calTitle);

        VBox calendarCard = new VBox(15);
        calendarCard.setAlignment(Pos.TOP_CENTER);
        calendarCard.setMaxWidth(800);
        calendarCard.setPadding(new Insets(20));
        calendarCard.setStyle("-fx-background-color: white; -fx-background-radius: 20; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 10, 0, 0, 5);");

        HBox nav = new HBox(40);
        nav.setAlignment(Pos.CENTER);
        Button prev = new Button("◀");
        Button next = new Button("▶");
        String btnStyle = "-fx-background-color: #f4f4f4; -fx-text-fill: #1a3c1a; -fx-font-weight: bold; -fx-background-radius: 50; -fx-cursor: hand;";
        prev.setStyle(btnStyle); next.setStyle(btnStyle);
        prev.setPrefSize(40,40); next.setPrefSize(40,40);

        Label monthLabel = new Label(currentYearMonth.getMonth().name() + " " + currentYearMonth.getYear());
        monthLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #333;");

        prev.setOnAction(e -> { currentYearMonth = currentYearMonth.minusMonths(1); showFullCalendarView(); });
        next.setOnAction(e -> { currentYearMonth = currentYearMonth.plusMonths(1); showFullCalendarView(); });
        nav.getChildren().addAll(prev, monthLabel, next);

        GridPane grid = new GridPane();
        grid.setAlignment(Pos.CENTER);
        grid.setHgap(10);
        grid.setVgap(10);

        String[] days = {"LUN", "MAR", "MER", "JEU", "VEN", "SAM", "DIM"};
        for (int i = 0; i < 7; i++) {
            Label d = new Label(days[i]);
            d.setStyle("-fx-text-fill: #888; -fx-font-weight: bold; -fx-font-size: 12px;");
            grid.add(d, i, 0);
            GridPane.setHalignment(d, javafx.geometry.HPos.CENTER);
        }

        LocalDate first = currentYearMonth.atDay(1);
        int offset = first.getDayOfWeek().getValue() - 1;
        int daysInMonth = currentYearMonth.lengthOfMonth();

        for (int i = 0; i < daysInMonth; i++) {
            LocalDate date = first.plusDays(i);
            VBox dayBox = new VBox(5);
            dayBox.setAlignment(Pos.TOP_CENTER);
            dayBox.setPrefSize(90, 85);
            dayBox.setPadding(new Insets(5));
            dayBox.setStyle("-fx-background-color: #fafafa; -fx-background-radius: 12; -fx-border-color: #eee; -fx-border-radius: 12; -fx-border-width: 1;");

            Label num = new Label(String.valueOf(i + 1));
            num.setStyle("-fx-font-weight: bold; -fx-text-fill: #333; -fx-font-size: 14px;");
            dayBox.getChildren().add(num);

            List<EvennementAgricole> dayEvents = allEvents.stream()
                    .filter(ev -> ev.getDateDebut().isEqual(date))
                    .collect(Collectors.toList());

            for (EvennementAgricole ev : dayEvents) {
                Label evLbl = new Label(ev.getTitre());
                evLbl.setMaxWidth(80);
                evLbl.setEllipsisString("...");

                boolean isPast = date.isBefore(LocalDate.now());
                String activeColor = isPast ? "#9e9e9e" : "#2d5a27";

                String defaultEvStyle = "-fx-background-color: #f0f0f0; -fx-text-fill: " + (isPast ? "#888" : "#555") + "; -fx-font-size: 9px; -fx-background-radius: 4; -fx-padding: 2 4;";
                String highlightEvStyle = "-fx-background-color: " + activeColor + "; -fx-text-fill: white; -fx-font-size: 9px; -fx-background-radius: 4; -fx-padding: 2 4; -fx-font-weight: bold; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.2), 3, 0, 0, 1);";

                evLbl.setStyle(defaultEvStyle);
                evLbl.setCursor(Cursor.HAND);

                evLbl.setOnMouseEntered(e -> { evLbl.setStyle(highlightEvStyle); evLbl.setScaleX(1.1); });
                evLbl.setOnMouseExited(e -> { evLbl.setStyle(defaultEvStyle); evLbl.setScaleX(1.0); });

                evLbl.setOnMouseClicked(e -> showEventDetails(ev, getDynamicGradient(0)));
                dayBox.getChildren().add(evLbl);
            }

            if(date.isEqual(LocalDate.now())) {
                dayBox.setStyle("-fx-background-color: #e8f5e9; -fx-border-color: #2d5a27; -fx-background-radius: 12; -fx-border-radius: 12; -fx-border-width: 2;");
            }
            grid.add(dayBox, (i + offset) % 7, (i + offset) / 7 + 1);
        }

        calendarCard.getChildren().addAll(nav, grid);
        mainContentVBox.getChildren().addAll(header, calendarCard);
    }

    // --- MAIN CATALOGUE (FIXED: Past events are filtered out here) ---
    @FXML
    public void showGestionEvenements() {
        refreshData();
        mainContentVBox.getChildren().clear();
        mainContentVBox.setSpacing(20);
        mainContentVBox.setPadding(new Insets(20, 40, 20, 40));

        HBox titleRow = new HBox();
        titleRow.setAlignment(Pos.CENTER_LEFT);
        titleRow.setMaxWidth(1000);

        Label title = new Label("Catalogue AgriCore");
        title.setStyle("-fx-font-size: 32px; -fx-font-weight: bold; -fx-text-fill: #1a3c1a;");

        Region spacerT = new Region();
        HBox.setHgrow(spacerT, Priority.ALWAYS);

        VBox calIconBtn = new VBox(2);
        calIconBtn.setAlignment(Pos.CENTER);
        calIconBtn.setPrefSize(70, 60);
        calIconBtn.setCursor(Cursor.HAND);
        calIconBtn.setStyle("-fx-background-color: white; -fx-background-radius: 12; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.15), 8, 0, 0, 4);");

        Label iconEmoji = new Label("📅");
        iconEmoji.setStyle("-fx-font-size: 24px;");
        Label iconText = new Label("CALENDRIER");
        iconText.setStyle("-fx-font-size: 8px; -fx-font-weight: bold; -fx-text-fill: #1a3c1a;");

        calIconBtn.getChildren().addAll(iconEmoji, iconText);
        calIconBtn.setOnMouseClicked(e -> showFullCalendarView());

        titleRow.getChildren().addAll(title, spacerT, calIconBtn);

        HBox searchBar = new HBox(15);
        searchBar.setAlignment(Pos.CENTER);
        searchBar.setPadding(new Insets(10, 20, 10, 20));
        searchBar.setMaxWidth(1000);
        searchBar.setStyle("-fx-background-color: white; -fx-background-radius: 30; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 10, 0, 0, 5);");

        searchInput = new TextField();
        searchInput.setPromptText("🔍 Rechercher...");
        searchInput.setPrefWidth(200);
        searchInput.setStyle("-fx-text-fill: #333; -fx-background-color: transparent; -fx-border-color: #ddd; -fx-border-width: 0 1 0 0;");

        datePicker = new DatePicker();
        datePicker.setPromptText("Date");
        datePicker.setPrefWidth(130);

        VBox priceContainer = new VBox(2);
        priceContainer.setAlignment(Pos.CENTER);
        Label priceLabel = new Label("Budget Max");
        priceLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #888; -fx-font-weight: bold;");

        HBox sliderRow = new HBox(5);
        sliderRow.setAlignment(Pos.CENTER);
        priceSlider = new Slider(0, 500, 100);
        priceSlider.setPrefWidth(120);
        priceValueLabel = new Label("100 DT");
        priceValueLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #2d5a27; -fx-font-size: 12px;");

        sliderRow.getChildren().addAll(priceSlider, priceValueLabel);
        priceContainer.getChildren().addAll(priceLabel, sliderRow);

        Button btnReset = new Button("Réinitialiser");
        btnReset.setStyle("-fx-background-radius: 20; -fx-cursor: hand; -fx-background-color: #f4f4f4; -fx-text-fill: #555;");
        btnReset.setOnAction(e -> {
            searchInput.clear();
            datePicker.setValue(null);
            priceSlider.setValue(100);
            updateFilters();
        });

        searchBar.getChildren().addAll(searchInput, datePicker, new Separator(javafx.geometry.Orientation.VERTICAL), priceContainer, btnReset);

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
        priceSlider.valueProperty().addListener((obs, old, val) -> {
            priceValueLabel.setText(Math.round(val.doubleValue()) + " DT");
            updateFilters();
        });

        mainContentVBox.getChildren().addAll(titleRow, searchBar, eventGrid);
        updateFilters();
    }

    private void updateFilters() {
        eventGrid.getChildren().clear();
        String query = searchInput.getText().toLowerCase();
        LocalDate selectedDate = datePicker.getValue();
        double maxPrice = priceSlider.getValue();
        LocalDate today = LocalDate.now();

        List<EvennementAgricole> filtered = allEvents.stream()
                .filter(e -> {
                    // CRITICAL FIX: In the Catalogue, show ONLY future events.
                    if (e.getDateDebut().isBefore(today)) return false;

                    boolean matchesTitle = e.getTitre().toLowerCase().contains(query);
                    boolean matchesDate = (selectedDate == null) || e.getDateDebut().isEqual(selectedDate);
                    boolean matchesPrice = e.getFraisInscription() <= maxPrice;
                    return matchesTitle && matchesDate && matchesPrice;
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
        int rem = getRemainingPlaces(ev);
        boolean isPast = ev.getDateDebut().isBefore(LocalDate.now());

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

        Label remLbl = new Label("🎟️ " + rem + " places restantes");
        remLbl.setStyle("-fx-text-fill: " + (rem < 5 ? "#c62828" : "#2d5a27") + "; -fx-font-weight: bold;");

        info.getChildren().addAll(dateLbl, titleLbl, lieuLbl, remLbl);

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        HBox footer = new HBox(new Label(ev.getFraisInscription() + " DT") {{
            setStyle("-fx-font-size: 22px; -fx-font-weight: 900; -fx-text-fill: #1a3c1a;");
        }});
        footer.setPadding(new Insets(0, 20, 20, 20));

        card.getChildren().addAll(top, info, spacer, footer);
        container.getChildren().add(card);

        if (reservedEventIds.contains(ev.getIdEvennement())) {
            Label badge = new Label(isPast ? "PASSÉ" : "INSCRIT");
            badge.setStyle("-fx-background-color: " + (isPast ? "#555" : "#1a3c1a") + "; -fx-text-fill: white; -fx-font-size: 11px; -fx-font-weight: bold; -fx-padding: 5 12; -fx-background-radius: 0 25 0 15;");
            StackPane.setAlignment(badge, Pos.TOP_RIGHT);
            container.getChildren().add(badge);
            card.setOpacity(isPast ? 0.6 : 0.85);
        } else if (rem <= 0) {
            Label badge = new Label("COMPLET");
            badge.setStyle("-fx-background-color: #888; -fx-text-fill: white; -fx-font-size: 11px; -fx-font-weight: bold; -fx-padding: 5 12; -fx-background-radius: 0 25 0 15;");
            StackPane.setAlignment(badge, Pos.TOP_RIGHT);
            container.getChildren().add(badge);
            card.setDisable(true);
            card.setOpacity(0.6);
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

    private void showEventDetails(EvennementAgricole ev, String gradient) {
        mainContentVBox.getChildren().clear();
        int rem = getRemainingPlaces(ev);
        boolean isPast = ev.getDateDebut().isBefore(LocalDate.now());
        boolean res = reservedEventIds.contains(ev.getIdEvennement());

        VBox detailBox = new VBox(0);
        detailBox.setMaxWidth(750);
        detailBox.setStyle("-fx-background-color: white; -fx-background-radius: 30; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.2), 20, 0, 0, 10);");

        VBox header = new VBox(15);
        header.setPadding(new Insets(30));
        header.setAlignment(Pos.CENTER);
        header.setStyle("-fx-background-color: " + (isPast ? "#555" : gradient) + "; -fx-background-radius: 30 30 0 0;");

        Button btnBack = new Button("← Retour");
        btnBack.setStyle("-fx-background-color: rgba(255,255,255,0.2); -fx-text-fill: white; -fx-background-radius: 20; -fx-cursor: hand;");
        btnBack.setOnAction(e -> showGestionEvenements());

        Label title = new Label(ev.getTitre());
        title.setStyle("-fx-font-size: 28px; -fx-font-weight: bold; -fx-text-fill: white;");
        header.getChildren().addAll(btnBack, new Label(getDynamicIcon(ev.getTitre())) {{ setStyle("-fx-font-size: 50px;"); }}, title);

        GridPane body = new GridPane();
        body.setPadding(new Insets(30));
        body.setHgap(30);

        VBox leftSide = new VBox(15);
        Label descHeader = new Label("À propos");
        descHeader.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #1a3c1a;");
        Text description = new Text(ev.getDescription());
        description.setWrappingWidth(400);
        description.setStyle("-fx-fill: #444; -fx-font-size: 15px; -fx-line-spacing: 5px;");
        leftSide.getChildren().addAll(descHeader, description);

        VBox rightSide = new VBox(15);
        rightSide.setPadding(new Insets(20));
        rightSide.setStyle("-fx-background-color: #f8fbf8; -fx-background-radius: 20; -fx-border-color: #eef2ee; -fx-border-radius: 20;");
        rightSide.setMinWidth(240);

        StackPane mapFrame = new StackPane();
        mapFrame.setPrefSize(200, 200);
        WebView webMap = new WebView();
        webMap.setPrefSize(200, 200);
        webMap.setClip(new Circle(100, 100, 100));
        loadReadOnlyMap(webMap, ev.getLieu());
        mapFrame.getChildren().add(webMap);

        rightSide.getChildren().addAll(
                mapFrame,
                createInfoRow("📅 Date", ev.getDateDebut().format(dtf)),
                createInfoRow("📍 Lieu", ev.getLieu()),
                createInfoRow("💰 Frais", ev.getFraisInscription() + " DT"),
                createInfoRow("🎟️ Disponibilité", rem + " places")
        );

        Button actionBtn = new Button();
        actionBtn.setMaxWidth(Double.MAX_VALUE);
        actionBtn.setCursor(Cursor.HAND);

        if (isPast) {
            actionBtn.setText("Historique (Non modifiable)");
            actionBtn.setDisable(true);
            actionBtn.setStyle("-fx-background-color: #eee; -fx-text-fill: #888; -fx-padding: 12;");
        } else {
            actionBtn.setText(res ? "Annuler l'inscription" : "S'inscrire maintenant");
            actionBtn.setStyle(res ? "-fx-background-color: #ffebee; -fx-text-fill: #c62828; -fx-font-weight: bold; -fx-padding: 12; -fx-background-radius: 10;" :
                    "-fx-background-color: #1a3c1a; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 12; -fx-background-radius: 10;");
            actionBtn.setOnAction(e -> {
                if (res) triggerStatusAnimation(ev, false);
                else if (rem > 0) showRegistrationForm(ev);
                else showAlert("Complet", "Plus de places disponibles.");
            });
        }

        rightSide.getChildren().add(actionBtn);
        body.add(leftSide, 0, 0);
        body.add(rightSide, 1, 0);

        detailBox.getChildren().addAll(header, body);
        mainContentVBox.getChildren().add(detailBox);
    }

    private void showRegistrationForm(EvennementAgricole ev) {
        Pane root = (Pane) mainContentVBox.getScene().getRoot();
        StackPane glassPane = new StackPane();
        glassPane.setStyle("-fx-background-color: rgba(0,0,0,0.6);");
        glassPane.prefWidthProperty().bind(root.widthProperty());
        glassPane.prefHeightProperty().bind(root.heightProperty());

        VBox formCard = new VBox(15);
        formCard.setAlignment(Pos.CENTER_LEFT);
        formCard.setMaxSize(350, 400);
        formCard.setPadding(new Insets(25));
        formCard.setStyle("-fx-background-color: white; -fx-background-radius: 20; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.3), 20, 0, 0, 10);");

        Label formTitle = new Label("Confirmer l'inscription");
        formTitle.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #1a3c1a;");

        String userName = "";
        try { userName = partService.getUserRealName(CURRENT_USER_ID); } catch (SQLException e) { e.printStackTrace(); }

        TextField nameInput = new TextField(userName);
        nameInput.setStyle("-fx-background-radius: 10; -fx-padding: 10; -fx-border-color: #ddd; -fx-border-radius: 10;");

        int initialRemaining = getRemainingPlaces(ev);
        Spinner<Integer> placesSpinner = new Spinner<>(1, initialRemaining, 1);
        placesSpinner.setMaxWidth(Double.MAX_VALUE);
        placesSpinner.getStyleClass().add(Spinner.STYLE_CLASS_SPLIT_ARROWS_HORIZONTAL);

        Label totalLbl = new Label("Montant Total: " + ev.getFraisInscription() + " DT");
        totalLbl.setStyle("-fx-font-weight: bold; -fx-text-fill: #2d5a27; -fx-font-size: 14px;");

        placesSpinner.valueProperty().addListener((obs, old, val) -> {
            totalLbl.setText("Montant Total: " + (val * ev.getFraisInscription()) + " DT");
        });

        HBox btnRow = new HBox(10);
        btnRow.setAlignment(Pos.CENTER_RIGHT);
        Button btnCancel = new Button("Annuler");
        btnCancel.setStyle("-fx-background-radius: 10;");
        Button btnSubmit = new Button("Valider");
        btnSubmit.setStyle("-fx-background-color: #1a3c1a; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 10; -fx-cursor: hand;");

        btnCancel.setOnAction(e -> root.getChildren().remove(glassPane));
        btnSubmit.setOnAction(e -> {
            try {
                Participant p = new Participant(CURRENT_USER_ID, ev.getIdEvennement(), LocalDate.now(), "Confirmé", String.valueOf(placesSpinner.getValue() * ev.getFraisInscription()), "OUI", placesSpinner.getValue(), nameInput.getText());
                partService.create(p);
                reservedEventIds.add(ev.getIdEvennement());
                root.getChildren().remove(glassPane);
                triggerStatusAnimation(ev, true);
            } catch (SQLException ex) { ex.printStackTrace(); }
        });

        btnRow.getChildren().addAll(btnCancel, btnSubmit);
        formCard.getChildren().addAll(formTitle, new Label("Nom du participant"), nameInput, new Label("Nombre de places"), placesSpinner, totalLbl, btnRow);

        glassPane.getChildren().add(formCard);
        root.getChildren().add(glassPane);

        FadeTransition ft = new FadeTransition(Duration.millis(300), glassPane);
        ft.setFromValue(0); ft.setToValue(1); ft.play();
    }

    private void triggerStatusAnimation(EvennementAgricole ev, boolean adding) {
        if (!adding) {
            try {
                List<Participant> all = partService.read();
                for (Participant p : all) {
                    if (p.getIdEvennement() == ev.getIdEvennement() && p.getIdUtilisateur() == CURRENT_USER_ID) {
                        partService.delete(p.getIdParticipant());
                        break;
                    }
                }
                reservedEventIds.remove(ev.getIdEvennement());
            } catch (SQLException e) { e.printStackTrace(); return; }
        }

        Pane root = (Pane) mainContentVBox.getScene().getRoot();
        StackPane glassPane = new StackPane();
        glassPane.setStyle("-fx-background-color: rgba(0,0,0,0.4);");
        glassPane.prefWidthProperty().bind(root.widthProperty());
        glassPane.prefHeightProperty().bind(root.heightProperty());

        VBox overlayCard = new VBox(15);
        overlayCard.setAlignment(Pos.CENTER);
        overlayCard.setMaxSize(300, 250);
        overlayCard.setStyle("-fx-background-color: white; -fx-background-radius: 30; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.2), 20, 0, 0, 10);");

        Label icon = new Label(adding ? "✓" : "✕");
        icon.setStyle("-fx-font-size: 80px; -fx-text-fill: " + (adding ? "#2d5a27" : "#c62828") + ";");
        Label txt = new Label(adding ? "INSCRIPTION RÉUSSIE" : "INSCRIPTION ANNULÉE");
        txt.setStyle("-fx-font-weight: bold; -fx-text-fill: #333; -fx-font-size: 16px;");

        overlayCard.getChildren().addAll(icon, txt);
        glassPane.getChildren().add(overlayCard);
        root.getChildren().add(glassPane);

        FadeTransition ft = new FadeTransition(Duration.seconds(0.5), glassPane);
        ft.setFromValue(0.0); ft.setToValue(1.0);
        PauseTransition pause = new PauseTransition(Duration.seconds(0.8));
        FadeTransition ftOut = new FadeTransition(Duration.seconds(0.5), glassPane);
        ftOut.setFromValue(1.0); ftOut.setToValue(0.0);
        ftOut.setOnFinished(e -> {
            root.getChildren().remove(glassPane);
            showGestionEvenements();
        });

        new SequentialTransition(ft, pause, ftOut).play();
    }

    private int getRemainingPlaces(EvennementAgricole ev) {
        try {
            int occupied = partService.read().stream()
                    .filter(p -> p.getIdEvennement() == ev.getIdEvennement())
                    .mapToInt(Participant::getNbrPlaces)
                    .sum();
            return ev.getCapaciteMax() - occupied;
        } catch (SQLException e) { return 0; }
    }

    private void loadReadOnlyMap(WebView webView, String coords) {
        String lat = "36.8065", lng = "10.1815";
        if (coords != null && coords.contains(",")) {
            String[] parts = coords.split(",");
            lat = parts[0].trim(); lng = parts[1].trim();
        }
        String html = "<!DOCTYPE html><html><head><link rel=\"stylesheet\" href=\"https://unpkg.com/leaflet@1.9.4/dist/leaflet.css\" /><script src=\"https://unpkg.com/leaflet@1.9.4/dist/leaflet.js\"></script><style>body,html,#map{height:100%;margin:0;padding:0;background:#eee;}.leaflet-control-container{display:none;}</style></head><body><div id=\"map\"></div><script>var map = L.map('map',{dragging:false,zoomControl:false,scrollWheelZoom:false,doubleClickZoom:false}).setView(["+lat+","+lng+"],13);L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png').addTo(map);L.marker(["+lat+","+lng+"]).addTo(map);</script></body></html>";
        webView.getEngine().loadContent(html);
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

    private HBox createInfoRow(String label, String value) {
        VBox v = new VBox(2);
        Label lbl = new Label(label); lbl.setStyle("-fx-text-fill: #888; -fx-font-size: 12px;");
        Label val = new Label(value); val.setStyle("-fx-text-fill: #333; -fx-font-weight: bold; -fx-font-size: 14px;");
        v.getChildren().addAll(lbl, val);
        return new HBox(10, v);
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title); alert.setHeaderText(null); alert.setContentText(content); alert.showAndWait();
    }
}