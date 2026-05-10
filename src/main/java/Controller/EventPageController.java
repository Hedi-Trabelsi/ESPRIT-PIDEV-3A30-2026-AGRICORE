package Controller;

import Model.EvennementAgricole;
import Model.Participant;
import javafx.animation.*;
import javafx.application.Platform;
import javafx.fxml.FXML;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Text;
import javafx.scene.web.WebView;
import javafx.stage.Modality;
import javafx.stage.Popup;
import javafx.stage.Stage;
import javafx.util.Duration;
import services.EvennementService;
import services.ParticipantService;
import services.MessageService;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

public class EventPageController {

    @FXML private VBox mainContentVBox;
    @FXML private StackPane notificationBell;
    @FXML private Label notificationBadge;
    @FXML private VBox calendarIcon;
    @FXML private TextField searchInput;
    @FXML private DatePicker datePicker;
    @FXML private Slider priceSlider;
    @FXML private Label priceValueLabel;
    @FXML private Button resetButton;
    @FXML private GridPane eventGrid;

    private EvennementService evService;
    private ParticipantService partService;
    private List<EvennementAgricole> allEvents = new ArrayList<>();
    private final Set<Integer> reservedEventIds = new HashSet<>();
    private final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd MMMM yyyy");
    private final DateTimeFormatter dtfWithTime = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private final DateTimeFormatter tf = DateTimeFormatter.ofPattern("HH:mm");
    private final int CURRENT_USER_ID = 1;
    private YearMonth currentYearMonth = YearMonth.now();
    private List<NotificationItem> activeNotifications = new ArrayList<>();

    public EventPageController() {
        try {
            this.evService = new EvennementService();
            this.partService = new ParticipantService();
            System.out.println("EventPageController initialized successfully");
        } catch (SQLException e) {
            System.err.println("Error initializing services: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static class NotificationItem {
        String title;
        String message;
        String type;
        NotificationItem(String t, String m, String ty) { title = t; message = m; type = ty; }
    }

    @FXML
    public void initialize() {
        System.out.println("EventPageController.initialize() called");
        setupEventHandlers();
        refreshData();
    }

    private void setupEventHandlers() {
        notificationBell.setOnMouseClicked(e -> showNotificationPopup(notificationBell));
        calendarIcon.setOnMouseClicked(e -> showFullCalendarView());

        searchInput.textProperty().addListener((obs, old, val) -> {
            System.out.println("Search text changed: " + val);
            updateFilters();
        });

        datePicker.valueProperty().addListener((obs, old, val) -> {
            System.out.println("Date changed: " + val);
            updateFilters();
        });

        priceSlider.valueProperty().addListener((obs, old, val) -> {
            priceValueLabel.setText(Math.round(val.doubleValue()) + " DT");
            updateFilters();
        });

        resetButton.setOnAction(e -> {
            searchInput.clear();
            datePicker.setValue(null);
            priceSlider.setValue(500);
        });
    }

    private void refreshData() {
        try {
            System.out.println("=== Refreshing Event Data ===");
            allEvents = evService.read();
            System.out.println("Total events loaded from database: " + allEvents.size());

            // Print all events for debugging
            for (EvennementAgricole ev : allEvents) {
                System.out.println("Event ID: " + ev.getIdEvennement() +
                        ", Title: '" + ev.getTitre() +
                        "', Date: " + ev.getDateDebut().format(dtfWithTime) +
                        ", Lieu: " + ev.getLieu() +
                        ", Prix: " + ev.getFraisInscription() + " DT" +
                        ", Capacité: " + ev.getCapaciteMax());
            }

            List<Participant> allParticipants = partService.read();
            System.out.println("Total participants loaded: " + allParticipants.size());

            reservedEventIds.clear();
            for (Participant p : allParticipants) {
                if (p.getIdUtilisateur() == CURRENT_USER_ID) {
                    reservedEventIds.add(p.getIdEvennement());
                    System.out.println("User is registered for event ID: " + p.getIdEvennement());
                }
            }

            updateNotificationList();
            updateFilters(); // This will display the events
            System.out.println("=== Refresh Complete ===\n");

        } catch (SQLException e) {
            System.err.println("Error refreshing data: " + e.getMessage());
            e.printStackTrace();
            showAlert("Erreur", "Impossible de charger les données: " + e.getMessage());
        }
    }

    private void updateNotificationList() {
        activeNotifications.clear();
        LocalDateTime now = LocalDateTime.now();

        // On ne prend que les événements réservés et à venir
        List<EvennementAgricole> myActiveEvents = allEvents.stream()
                .filter(ev -> reservedEventIds.contains(ev.getIdEvennement()))
                .filter(ev -> ev.getDateDebut().isAfter(now)) // <-- uniquement futurs événements
                .collect(Collectors.toList());

        for (int i = 0; i < myActiveEvents.size(); i++) {
            EvennementAgricole evA = myActiveEvents.get(i);

            // Rappel si l'événement commence dans les prochaines 24h
            long hoursLeft = ChronoUnit.HOURS.between(now, evA.getDateDebut());
            if (hoursLeft >= 0 && hoursLeft <= 24) {
                activeNotifications.add(new NotificationItem(
                        "Rappel Événement",
                        "'" + evA.getTitre() + "' commence à " + evA.getDateDebut().format(tf),
                        "REMINDER"
                ));
            }

            // Vérification des conflits horaires pour événements le même jour
            for (int j = i + 1; j < myActiveEvents.size(); j++) {
                EvennementAgricole evB = myActiveEvents.get(j);
                if (evA.getDateDebut().toLocalDate().isEqual(evB.getDateDebut().toLocalDate())) {
                    boolean isOverlapping = evA.getDateDebut().isBefore(evB.getDateFin()) &&
                            evB.getDateDebut().isBefore(evA.getDateFin());
                    if (isOverlapping) {
                        activeNotifications.add(new NotificationItem(
                                "Conflit d'horaire",
                                "Conflit entre '" + evA.getTitre() + "' et '" + evB.getTitre() + "'.",
                                "CONFLICT"
                        ));
                    }
                }
            }
        }

        // Mise à jour du badge de notifications
        updateNotificationBadge();
    }


    private void updateNotificationBadge() {
        if (!activeNotifications.isEmpty()) {
            notificationBadge.setText(String.valueOf(activeNotifications.size()));
            notificationBadge.setVisible(true);
        } else {
            notificationBadge.setVisible(false);
        }
    }

    private void showNotificationPopup(Region anchor) {
        Popup popup = new Popup();
        popup.setAutoHide(true);

        VBox container = new VBox(0);
        container.setPrefWidth(320);
        container.setStyle("-fx-background-color: white; -fx-background-radius: 12; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.3), 15, 0, 0, 8); -fx-border-color: #ddd; -fx-border-radius: 12;");

        // Header
        HBox header = new HBox();
        header.setPadding(new Insets(15));
        header.setStyle("-fx-background-color: #f8f9fa; -fx-background-radius: 12 12 0 0; -fx-border-color: #eee; -fx-border-width: 0 0 1 0;");
        Label title = new Label("Centre de Notifications");
        title.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #333;");
        header.getChildren().add(title);
        container.getChildren().add(header);

        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefHeight(350);
        scrollPane.setStyle("-fx-background-color: transparent; -fx-background: white;");

        VBox list = new VBox(1);
        if (activeNotifications.isEmpty()) {
            VBox empty = new VBox(10, new Label("∅"), new Label("Aucune notification pour le moment"));
            empty.setAlignment(Pos.CENTER);
            empty.setPadding(new Insets(40));
            empty.setStyle("-fx-text-fill: #999;");
            list.getChildren().add(empty);
        } else {
            for (NotificationItem item : activeNotifications) {
                VBox card = new VBox(5);
                card.setPadding(new Insets(12));
                card.setStyle("-fx-border-color: #f0f0f0; -fx-border-width: 0 0 1 0;");

                Label rowTitle = new Label(item.title);
                rowTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 12px; -fx-text-fill: " + (item.type.equals("CONFLICT") ? "#d32f2f" : "#2d5a27") + ";");

                Label rowMsg = new Label(item.message);
                rowMsg.setWrapText(true);
                rowMsg.setStyle("-fx-text-fill: #666; -fx-font-size: 11px;");

                card.getChildren().addAll(rowTitle, rowMsg);

                // --- CORRECTED AI CONFLICT RESOLVER ---
                if (item.type.equals("CONFLICT")) {
                    List<EvennementAgricole> conflicting = allEvents.stream()
                            .filter(ev -> item.message.contains("'" + ev.getTitre() + "'"))
                            .collect(Collectors.toList());

                    if (conflicting.size() >= 2) {
                        EvennementAgricole ev1 = conflicting.get(0);
                        EvennementAgricole ev2 = conflicting.get(1);

                        EvennementAgricole suggested;
                        String reasonText;

                        if (ev1.getFraisInscription() != ev2.getFraisInscription()) {
                            suggested = (ev1.getFraisInscription() < ev2.getFraisInscription()) ? ev1 : ev2;
                            reasonText = "est plus économique (" + suggested.getFraisInscription() + " DT).";
                        } else {
                            long dur1 = ChronoUnit.MINUTES.between(ev1.getDateDebut(), ev1.getDateFin());
                            long dur2 = ChronoUnit.MINUTES.between(ev2.getDateDebut(), ev2.getDateFin());
                            suggested = (dur1 >= dur2) ? ev1 : ev2;
                            reasonText = "offre une meilleure durée pour le même prix.";
                        }

                        EvennementAgricole toDrop = (suggested == ev1) ? ev2 : ev1;

                        VBox aiBox = new VBox(5);
                        aiBox.setPadding(new Insets(10));
                        aiBox.setStyle("-fx-background-color: #e3f2fd; -fx-background-radius: 8; -fx-border-color: #90caf9; -fx-border-width: 1;");

                        Label aiLabel = new Label("✨ IA Analyse de Valeur");
                        aiLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 10px; -fx-text-fill: #1565c0;");

                        Text aiText = new Text("Conseil IA : Gardez '" + suggested.getTitre() + "' car il " + reasonText);
                        aiText.setWrappingWidth(260);
                        aiText.setStyle("-fx-font-size: 10px; -fx-fill: #0d47a1;");

                        Button resolveNow = new Button("Gérer l'annulation de '" + toDrop.getTitre() + "'");
                        resolveNow.setStyle("-fx-background-color: #1976d2; -fx-text-fill: white; -fx-font-size: 9px; -fx-font-weight: bold; -fx-background-radius: 4; -fx-cursor: hand;");
                        resolveNow.setOnAction(e -> {
                            popup.hide();
                            showEventDetails(toDrop, getDynamicGradient(0));
                        });

                        aiBox.getChildren().addAll(aiLabel, aiText, resolveNow);
                        card.getChildren().add(aiBox);
                    }
                }

                card.setOnMouseEntered(e -> card.setStyle("-fx-background-color: #f4fbf4; -fx-border-color: #f0f0f0; -fx-border-width: 0 0 1 0;"));
                card.setOnMouseExited(e -> card.setStyle("-fx-background-color: white; -fx-border-color: #f0f0f0; -fx-border-width: 0 0 1 0;"));

                list.getChildren().add(card);
            }
        }
        scrollPane.setContent(list);
        container.getChildren().add(scrollPane);

        popup.getContent().add(container);
        javafx.geometry.Bounds bounds = anchor.localToScreen(anchor.getBoundsInLocal());
        popup.show(anchor.getScene().getWindow(), bounds.getMinX() - 280, bounds.getMaxY() + 10);
    }


    private void addAIConflictResolver(VBox card, NotificationItem item, Popup popup) {
        List<EvennementAgricole> conflicting = allEvents.stream()
                .filter(ev -> item.message.contains("'" + ev.getTitre() + "'"))
                .collect(Collectors.toList());

        if (conflicting.size() >= 2) {
            EvennementAgricole ev1 = conflicting.get(0);
            EvennementAgricole ev2 = conflicting.get(1);

            EvennementAgricole suggested;
            String reasonText;

            if (ev1.getFraisInscription() != ev2.getFraisInscription()) {
                suggested = (ev1.getFraisInscription() < ev2.getFraisInscription()) ? ev1 : ev2;
                reasonText = "est plus économique (" + suggested.getFraisInscription() + " DT).";
            } else {
                long dur1 = ChronoUnit.MINUTES.between(ev1.getDateDebut(), ev1.getDateFin());
                long dur2 = ChronoUnit.MINUTES.between(ev2.getDateDebut(), ev2.getDateFin());
                suggested = (dur1 >= dur2) ? ev1 : ev2;
                reasonText = "offre une meilleure durée pour le même prix.";
            }

            EvennementAgricole toDrop = (suggested == ev1) ? ev2 : ev1;

            VBox aiBox = new VBox(5);
            aiBox.setPadding(new Insets(10));
            aiBox.getStyleClass().add("ai-suggestion-box");

            Label aiLabel = new Label("✨ IA Analyse de Valeur");
            aiLabel.getStyleClass().add("ai-suggestion-label");

            Text aiText = new Text("Conseil IA : Gardez '" + suggested.getTitre() + "' car il " + reasonText);
            aiText.setWrappingWidth(260);
            aiText.getStyleClass().add("ai-suggestion-text");

            Button resolveNow = new Button("Gérer l'annulation de '" + toDrop.getTitre() + "'");
            resolveNow.getStyleClass().add("ai-resolve-button");
            resolveNow.setOnAction(e -> {
                popup.hide();
                showEventDetails(toDrop, getDynamicGradient(0));
            });

            aiBox.getChildren().addAll(aiLabel, aiText, resolveNow);
            card.getChildren().add(aiBox);
        }
    }

    private void showFullCalendarView() {
        mainContentVBox.getChildren().clear();
        mainContentVBox.setSpacing(15);
        mainContentVBox.setPadding(new Insets(20, 40, 20, 40));

        // --- Header ---
        HBox header = new HBox(20);
        header.setAlignment(Pos.CENTER_LEFT);
        Button btnBack = new Button("← Retour");
        btnBack.setStyle("-fx-background-color: #1a3c1a; -fx-text-fill: white; -fx-background-radius: 20; -fx-cursor: hand; -fx-padding: 8 15;");
        btnBack.setOnAction(e -> showGestionEvenements(null));

        Label calTitle = new Label("Calendrier AgriCore");
        calTitle.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: #1a3c1a;");
        header.getChildren().addAll(btnBack, calTitle);

        // --- Main Layout: Calendar (Left) | Events List (Right) ---
        HBox mainLayout = new HBox(30);
        mainLayout.setAlignment(Pos.TOP_CENTER);

        // 1. Calendar Card
        VBox calendarCard = new VBox(15);
        calendarCard.setPadding(new Insets(20));
        calendarCard.setStyle("-fx-background-color: white; -fx-background-radius: 20; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 10, 0, 0, 5);");

        // Nav Bar
        HBox nav = new HBox(40);
        nav.setAlignment(Pos.CENTER);
        Button prev = new Button("<");
        Button next = new Button(">");
        String btnStyle = "-fx-background-color: #f4f4f4; -fx-text-fill: #1a3c1a; -fx-font-weight: bold; -fx-background-radius: 50; -fx-cursor: hand;";
        prev.setStyle(btnStyle); next.setStyle(btnStyle);
        prev.setPrefSize(40,40); next.setPrefSize(40,40);

        Label monthLabel = new Label(currentYearMonth.getMonth().name() + " " + currentYearMonth.getYear());
        monthLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        prev.setOnAction(e -> { currentYearMonth = currentYearMonth.minusMonths(1); showFullCalendarView(); });
        next.setOnAction(e -> { currentYearMonth = currentYearMonth.plusMonths(1); showFullCalendarView(); });
        nav.getChildren().addAll(prev, monthLabel, next);

        GridPane grid = new GridPane();
        grid.setHgap(8); grid.setVgap(8);

        // Days of week headers
        String[] days = {"LUN", "MAR", "MER", "JEU", "VEN", "SAM", "DIM"};
        for (int i = 0; i < 7; i++) {
            Label d = new Label(days[i]);
            d.setStyle("-fx-text-fill: #888; -fx-font-weight: bold;");
            grid.add(d, i, 0);
            GridPane.setHalignment(d, javafx.geometry.HPos.CENTER);
        }

        // 2. Events Side Panel
        VBox eventSidePanel = new VBox(15);
        eventSidePanel.setPrefWidth(280);
        eventSidePanel.setPadding(new Insets(10));
        eventSidePanel.setStyle("-fx-background-color: #f9f9f9; -fx-background-radius: 20; -fx-border-color: #eee; -fx-border-radius: 20;");

        Label sideTitle = new Label("Événements du jour");
        sideTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 16px; -fx-text-fill: #1a3c1a;");
        VBox eventListContainer = new VBox(10); // This will hold the dynamic list
        eventSidePanel.getChildren().addAll(sideTitle, eventListContainer);

        // Populate Grid
        LocalDate first = currentYearMonth.atDay(1);
        int offset = first.getDayOfWeek().getValue() - 1;
        for (int i = 0; i < currentYearMonth.lengthOfMonth(); i++) {
            LocalDate date = first.plusDays(i);
            VBox dayBox = createDayBox(date, eventListContainer);
            grid.add(dayBox, (i + offset) % 7, (i + offset) / 7 + 1);
        }

        calendarCard.getChildren().addAll(nav, grid);
        mainLayout.getChildren().addAll(calendarCard, eventSidePanel);
        mainContentVBox.getChildren().addAll(header, mainLayout);
    }

    private VBox createDayBox(LocalDate date, VBox listContainer) {
        VBox box = new VBox(2);
        box.setAlignment(Pos.TOP_CENTER);
        box.setPrefSize(75, 75);
        box.setCursor(Cursor.HAND);
        box.setPadding(new Insets(5));

        // Default Styles
        String baseStyle = "-fx-background-radius: 12; -fx-border-radius: 12; -fx-border-width: 1; ";
        if (date.isEqual(LocalDate.now())) {
            box.setStyle(baseStyle + "-fx-background-color: #e8f5e9; -fx-border-color: #2d5a27; -fx-border-width: 2;");
        } else {
            box.setStyle(baseStyle + "-fx-background-color: #fafafa; -fx-border-color: #eee;");
        }

        // Day Number
        Label lbl = new Label(String.valueOf(date.getDayOfMonth()));
        lbl.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #333;");
        box.getChildren().add(lbl);

        // --- STAR INDICATOR LOGIC ---
        long eventCount = allEvents.stream()
                .filter(ev -> ev.getDateDebut().toLocalDate().isEqual(date))
                .count();

        if (eventCount > 0) {
            Label star = new Label("★"); // Gold star symbol
            star.setStyle("-fx-text-fill: #ffc107; -fx-font-size: 16px;");

            // Subtle glow effect for the star
            DropShadow glow = new DropShadow();
            glow.setColor(Color.rgb(255, 193, 7, 0.6));
            glow.setRadius(5);
            star.setEffect(glow);

            box.getChildren().add(star);

            // If there are multiple events, maybe show a small '+2' label
            if (eventCount > 1) {
                Label countLabel = new Label("+" + (eventCount - 1));
                countLabel.setStyle("-fx-font-size: 9px; -fx-text-fill: #2d5a27; -fx-font-weight: bold;");
                box.getChildren().add(countLabel);
            }
        }

        // Hover Effects
        box.setOnMouseEntered(e -> box.setStyle(box.getStyle() + "-fx-background-color: #f0f4f0; -fx-scale-x: 1.05; -fx-scale-y: 1.05;"));
        box.setOnMouseExited(e -> {
            // Reset scale and original color
            box.setScaleX(1.0); box.setScaleY(1.0);
            if (date.isEqual(LocalDate.now())) {
                box.setStyle(baseStyle + "-fx-background-color: #e8f5e9; -fx-border-color: #2d5a27; -fx-border-width: 2;");
            } else {
                box.setStyle(baseStyle + "-fx-background-color: #fafafa; -fx-border-color: #eee;");
            }
        });

        box.setOnMouseClicked(e -> showEventsForDate(date, listContainer));

        return box;
    }

    // Helper to refresh the side list
    private void showEventsForDate(LocalDate date, VBox listContainer) {
        listContainer.getChildren().clear();

        List<EvennementAgricole> dayEvents = allEvents.stream()
                .filter(ev -> ev.getDateDebut().toLocalDate().isEqual(date))
                .collect(Collectors.toList());

        if (dayEvents.isEmpty()) {
            listContainer.getChildren().add(new Label("Aucun événement prévu.") {{ setStyle("-fx-text-fill: #999; -fx-italic: true;"); }});
        } else {
            for (EvennementAgricole ev : dayEvents) {
                HBox card = new HBox(10);
                card.setPadding(new Insets(10));
                card.setStyle("-fx-background-color: white; -fx-background-radius: 10; -fx-effect: dropshadow(two-pass-box, rgba(0,0,0,0.05), 5, 0, 0, 2);");
                card.setCursor(Cursor.HAND);

                VBox txt = new VBox(2);
                Label title = new Label(ev.getTitre());
                title.setStyle("-fx-font-weight: bold; -fx-font-size: 13px;");
                Label time = new Label(ev.getDateDebut().format(tf) + " - " + ev.getLieu());
                time.setStyle("-fx-font-size: 11px; -fx-text-fill: #666;");

                txt.getChildren().addAll(title, time);
                card.getChildren().add(txt);

                card.setOnMouseClicked(e -> showEventDetails(ev, getDynamicGradient(0)));
                listContainer.getChildren().add(card);
            }
        }
    }


    private void updateFilters() {
        eventGrid.getChildren().clear();

        if (allEvents.isEmpty()) {
            System.out.println("No events to display");
            Label noEvents = new Label("Aucun événement dans la base de données");
            noEvents.getStyleClass().add("event-no-results");
            eventGrid.add(noEvents, 0, 0, 2, 1);
            GridPane.setHalignment(noEvents, javafx.geometry.HPos.CENTER);
            return;
        }

        String query = searchInput.getText().toLowerCase();
        LocalDate selectedDate = datePicker.getValue();
        double maxPrice = priceSlider.getValue();

        System.out.println("Filtering " + allEvents.size() + " events with:");
        System.out.println("  - Query: '" + query + "'");
        System.out.println("  - Selected Date: " + selectedDate);
        System.out.println("  - Max Price: " + maxPrice);

        // Filter events - only future events
        LocalDateTime now = LocalDateTime.now();
        List<EvennementAgricole> filtered = allEvents.stream()
                .filter(ev -> !ev.getDateDebut().isBefore(now)) // <-- filtre événements passés
                .filter(ev -> {
                    boolean matchesTitle = query.isEmpty() || ev.getTitre().toLowerCase().contains(query);
                    boolean matchesDate = (selectedDate == null) || ev.getDateDebut().toLocalDate().isEqual(selectedDate);
                    boolean matchesPrice = ev.getFraisInscription() <= maxPrice;
                    boolean keep = matchesTitle && matchesDate && matchesPrice;

                    if (!keep && !query.isEmpty()) {
                        System.out.println("  Filtered out: " + ev.getTitre() +
                                " (title:" + matchesTitle +
                                ", date:" + matchesDate +
                                ", price:" + matchesPrice + ")");
                    }
                    return keep;
                }).collect(Collectors.toList());

        System.out.println("Filtered events count: " + filtered.size());

        if (filtered.isEmpty()) {
            Label noRes = new Label("∅ Aucun événement trouvé avec ces filtres.");
            noRes.getStyleClass().add("event-no-results");
            eventGrid.add(noRes, 0, 0, 2, 1);
            GridPane.setHalignment(noRes, javafx.geometry.HPos.CENTER);
        } else {
            for (int i = 0; i < filtered.size(); i++) {
                StackPane card = createEventCard(filtered.get(i), i);
                eventGrid.add(card, i % 2, i / 2);
            }
        }
    }


    private StackPane createEventCard(EvennementAgricole ev, int index) {
        StackPane container = new StackPane();
        String gradient = getDynamicGradient(index);
        int rem = getRemainingPlaces(ev);
        boolean isPast = ev.getDateDebut().isBefore(LocalDateTime.now());

        VBox card = new VBox();
        card.setPrefSize(340, 360);
        card.getStyleClass().add("event-card");
        card.setEffect(new DropShadow(15, Color.rgb(0,0,0,0.1)));
        card.setCursor(Cursor.HAND);

        VBox top = new VBox();
        top.setAlignment(Pos.CENTER);
        top.setPrefHeight(100);
        top.setStyle("-fx-background-color: " + gradient + "; -fx-background-radius: 25 25 0 0;");
        top.getStyleClass().add("event-card-top");

        Label iconLabel = new Label(getDynamicIcon(ev.getTitre()));
        iconLabel.getStyleClass().add("event-card-icon");
        top.getChildren().add(iconLabel);

        VBox info = new VBox(10);
        info.setPadding(new Insets(20));
        info.getStyleClass().add("event-card-info");

        Label dateLbl = new Label(ev.getDateDebut().format(dtf));
        dateLbl.getStyleClass().add("event-card-date");

        Label titleLbl = new Label(ev.getTitre());
        titleLbl.setWrapText(true);
        titleLbl.setMinHeight(50);
        titleLbl.getStyleClass().add("event-card-title");

        Label lieuLbl = new Label(ev.getLieu() != null ? ev.getLieu() : "Lieu non spécifié");
        lieuLbl.getStyleClass().add("event-card-location");

        Label remLbl = new Label("Places: " + rem + " restantes");
        remLbl.getStyleClass().add(rem < 5 ? "event-card-places-low" : "event-card-places");

        info.getChildren().addAll(dateLbl, titleLbl, lieuLbl, remLbl);

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        HBox footer = new HBox();
        footer.setPadding(new Insets(0, 20, 20, 20));
        footer.setAlignment(Pos.CENTER_LEFT);
        Label priceFooter = new Label(ev.getFraisInscription() + " DT");
        priceFooter.getStyleClass().add("event-card-price");
        footer.getChildren().add(priceFooter);

        card.getChildren().addAll(top, info, spacer, footer);
        container.getChildren().add(card);

        // Add badges
        if (reservedEventIds.contains(ev.getIdEvennement())) {
            Label badge = new Label(isPast ? "PASSÉ" : "INSCRIT");
            badge.getStyleClass().add(isPast ? "event-badge-past" : "event-badge-registered");
            StackPane.setAlignment(badge, Pos.TOP_RIGHT);
            container.getChildren().add(badge);
            card.setOpacity(isPast ? 0.6 : 0.85);
        } else if (rem <= 0) {
            Label badge = new Label("COMPLET");
            badge.getStyleClass().add("event-badge-completed");
            StackPane.setAlignment(badge, Pos.TOP_RIGHT);
            container.getChildren().add(badge);
            card.setDisable(true);
            card.setOpacity(0.6);
        } else if (isPast) {
            Label badge = new Label("PASSÉ");
            badge.getStyleClass().add("event-badge-past");
            StackPane.setAlignment(badge, Pos.TOP_RIGHT);
            container.getChildren().add(badge);
            card.setOpacity(0.6);
        }

        addCardAnimations(card);
        card.setOnMouseClicked(e -> showEventDetails(ev, gradient));

        return container;
    }

    private void addCardAnimations(VBox card) {
        card.setOnMouseEntered(e -> {
            ScaleTransition st = new ScaleTransition(Duration.millis(200), card);
            st.setToX(1.03); st.setToY(1.03); st.play();
        });
        card.setOnMouseExited(e -> {
            ScaleTransition st = new ScaleTransition(Duration.millis(200), card);
            st.setToX(1.0); st.setToY(1.0); st.play();
        });
    }
    private void showCommunityChat(EvennementAgricole ev, String gradient) {
        mainContentVBox.getChildren().clear();

        ParticipantService ps = new ParticipantService();
        MessageService msgService = new MessageService();

        int currentUserId = Controller.UserSession.getUserId();
        String currentFullName = Controller.UserSession.getPrenom() + " " + Controller.UserSession.getNom();
        boolean isAdmin = (Controller.UserSession.getRole() == 1);

        final int[] lastMessageId = {-1};
        final ScheduledExecutorService[] schedulerRef = {null};

        VBox layout = new VBox(25);
        layout.setPadding(new Insets(30));
        layout.setStyle("-fx-background-color: #f4f7f6;");
        layout.setAlignment(Pos.TOP_CENTER);

        ScrollPane globalScroll = new ScrollPane(layout);
        globalScroll.setFitToWidth(true);
        globalScroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");

        // --- BARRE DE NAVIGATION ---
        HBox nav = new HBox(20);
        nav.setAlignment(Pos.CENTER_LEFT);
        nav.setPadding(new Insets(15, 25, 15, 25));
        nav.setStyle("-fx-background-color: white; -fx-background-radius: 20; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.05), 15, 0, 0, 5);");

        Button btnBack = new Button("←");
        btnBack.setStyle("-fx-background-color: #1a3c1a; -fx-text-fill: white; -fx-background-radius: 50; -fx-cursor: hand; -fx-font-weight: bold;");
        btnBack.setOnAction(e -> {
            if (schedulerRef[0] != null && !schedulerRef[0].isShutdown()) {
                schedulerRef[0].shutdownNow();
            }
            showEventDetails(ev, gradient);
        });

        VBox titleGroup = new VBox(2);
        Label headerTitle = new Label(ev.getTitre());
        headerTitle.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #1a3c1a;");
        Label subTitle = new Label(isAdmin ? "Mode Modérateur" : "Espace Communautaire");
        subTitle.setStyle("-fx-text-fill: #666; -fx-font-size: 13px;");
        titleGroup.getChildren().addAll(headerTitle, subTitle);
        nav.getChildren().addAll(btnBack, titleGroup);

        HBox mainContent = new HBox(30);
        mainContent.setAlignment(Pos.TOP_CENTER);

        // --- SIDEBAR ---
        VBox sideBar = new VBox(15);
        sideBar.setPrefWidth(250);
        sideBar.setPadding(new Insets(20));
        sideBar.setStyle("-fx-background-color: white; -fx-background-radius: 25; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.08), 20, 0, 0, 10);");

        Label lblMembres = new Label("MEMBRES ACTIFS");
        lblMembres.setStyle("-fx-font-weight: bold; -fx-text-fill: #888; -fx-font-size: 12px;");

        VBox participantListContainer = new VBox(10);
        try {
            participantListContainer.getChildren().add(
                    createMemberCard("👑 " + currentFullName, "Organisateur", "#FFF9C4", "#F57F17")
            );
            for (String pName : ps.getParticipantNamesForEvent(ev.getIdEvennement())) {
                participantListContainer.getChildren().add(
                        createMemberCard(pName, "Membre", "#F0F4F0", "#2D5A27")
                );
            }
        } catch (Exception e) {
            System.err.println("Erreur chargement membres: " + e.getMessage());
        }
        sideBar.getChildren().addAll(lblMembres, participantListContainer);

        // --- FENÊTRE DE CHAT ---
        VBox chatWindow = new VBox(0);
        chatWindow.setPrefWidth(650);
        chatWindow.setMaxHeight(600);
        chatWindow.setStyle("-fx-background-color: white; -fx-background-radius: 25; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.08), 20, 0, 0, 10);");

        VBox messageContainer = new VBox(15);
        messageContainer.setPadding(new Insets(20));

        ScrollPane scrollChat = new ScrollPane(messageContainer);
        scrollChat.setFitToWidth(true);
        scrollChat.setPrefHeight(450);
        scrollChat.setStyle("-fx-background: transparent; -fx-background-color: transparent;");

        // --- CHARGEMENT HISTORIQUE INITIAL ---
        // FIX: always compare by real user ID, never by sender_id == 0
        try {
            List<MessageService.ChatMessage> history = msgService.getGroupMessages(ev.getIdEvennement());
            for (MessageService.ChatMessage m : history) {
                boolean isMoi = (m.senderId == currentUserId);
                addSmartBubble(messageContainer, isMoi ? "Moi" : m.senderName, m.content, isMoi);
                if (m.id > lastMessageId[0]) lastMessageId[0] = m.id;
            }
            Platform.runLater(() -> scrollChat.setVvalue(1.0));
        } catch (SQLException e) {
            e.printStackTrace();
        }

        // --- REAL-TIME POLLING ---
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        schedulerRef[0] = scheduler;

        scheduler.scheduleAtFixedRate(() -> {
            try {
                List<MessageService.ChatMessage> newMessages = msgService.getNewMessages(ev.getIdEvennement(), lastMessageId[0]);
                if (newMessages == null || newMessages.isEmpty()) return;

                for (MessageService.ChatMessage m : newMessages) {
                    if (m.id > lastMessageId[0]) lastMessageId[0] = m.id;

                    // FIX: always use real user ID to determine ownership
                    boolean isMoi = (m.senderId == currentUserId);

                    // Skip own messages — already shown optimistically on send
                    if (isMoi) continue;

                    final String displayName = m.senderName;
                    final String displayContent = m.content;
                    Platform.runLater(() -> {
                        addSmartBubble(messageContainer, displayName, displayContent, false);
                        scrollChat.setVvalue(1.0);
                    });
                }
            } catch (SQLException e) {
                System.err.println("Polling error: " + e.getMessage());
            }
        }, 2, 2, TimeUnit.SECONDS);

        // --- ZONE DE SAISIE ---
        HBox inputBar = new HBox(15);
        inputBar.setPadding(new Insets(15, 20, 15, 20));
        inputBar.setStyle("-fx-background-color: #f9f9f9; -fx-background-radius: 0 0 25 25; -fx-border-color: #eee; -fx-border-width: 1 0 0 0;");
        inputBar.setAlignment(Pos.CENTER);

        TextField inputField = new TextField();
        inputField.setPromptText("Tapez votre message ici...");
        inputField.setStyle("-fx-background-radius: 25; -fx-background-color: white; -fx-padding: 10 15; -fx-border-color: #ddd; -fx-border-radius: 25;");
        HBox.setHgrow(inputField, Priority.ALWAYS);

        Button btnSend = new Button("Envoyer");
        btnSend.setStyle("-fx-background-color: #1a3c1a; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 25; -fx-padding: 10 20; -fx-cursor: hand;");

        btnSend.setOnAction(e -> {
            String text = inputField.getText().trim();
            if (text.isEmpty()) return;

            try {
                // FIX: always use the real currentUserId, never hardcode 0
                String senderName = isAdmin ? "👑 " + currentFullName : currentFullName;

                int newId = msgService.sendMessage(currentUserId, senderName, ev.getIdEvennement(), text);
                if (newId > lastMessageId[0]) lastMessageId[0] = newId;

                addSmartBubble(messageContainer, "Moi", text, true);
                inputField.clear();
                Platform.runLater(() -> scrollChat.setVvalue(1.0));
            } catch (SQLException ex) {
                System.err.println("Erreur d'envoi : " + ex.getMessage());
            }
        });

        inputField.setOnKeyPressed(e -> {
            if (e.getCode() == javafx.scene.input.KeyCode.ENTER) {
                btnSend.fire();
            }
        });

        inputBar.getChildren().addAll(inputField, btnSend);
        chatWindow.getChildren().addAll(scrollChat, inputBar);

        mainContent.getChildren().addAll(sideBar, chatWindow);
        layout.getChildren().addAll(nav, mainContent);

        mainContentVBox.getChildren().add(globalScroll);
    }
    private void showEventDetails(EvennementAgricole ev, String gradient) {
        mainContentVBox.getChildren().clear();

        VBox layout = new VBox(25);
        layout.setPadding(new Insets(30));
        layout.setStyle("-fx-background-color: #f4f7f6;");
        layout.setAlignment(Pos.TOP_CENTER);

        ScrollPane globalScroll = new ScrollPane(layout);
        globalScroll.setFitToWidth(true);
        globalScroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");

        // --- BARRE DE NAVIGATION ---
        HBox nav = new HBox(20);
        nav.setAlignment(Pos.CENTER_LEFT);
        nav.setPadding(new Insets(10, 20, 10, 20));
        nav.setStyle("-fx-background-color: white; -fx-background-radius: 20; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.05), 15, 0, 0, 5);");

        Button btnBack = new Button("←");
        btnBack.setStyle("-fx-background-color: #1a3c1a; -fx-text-fill: white; -fx-background-radius: 50; -fx-cursor: hand;");
        btnBack.setOnAction(e -> {
            // Logique pour revenir à la liste globale des événements
            // showAllEvents();
        });

        VBox titleGroup = new VBox(2);
        Label headerTitle = new Label("Détails de l'événement");
        headerTitle.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: #1a3c1a;");
        titleGroup.getChildren().addAll(headerTitle, new Label("Informations complètes"));
        nav.getChildren().addAll(btnBack, titleGroup);

        // --- CONTENU PRINCIPAL ---
        VBox detailsCard = new VBox(20);
        detailsCard.setPadding(new Insets(30));
        detailsCard.setMaxWidth(800);
        detailsCard.setStyle("-fx-background-color: white; -fx-background-radius: 25; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.08), 20, 0, 0, 10);");

        // Titre et Badge
        HBox topRow = new HBox(20);
        topRow.setAlignment(Pos.CENTER_LEFT);
        Label title = new Label(ev.getTitre());
        title.setStyle("-fx-font-size: 28px; -fx-font-weight: bold; -fx-text-fill: #2D5A27;");

        Label badge = new Label(ev.getType()); // Supposant que getType() existe
        badge.setStyle("-fx-background-color: #E8F5E9; -fx-text-fill: #2E7D32; -fx-padding: 5 15; -fx-background-radius: 15; -fx-font-weight: bold;");
        topRow.getChildren().addAll(title, badge);

        // Description
        Label descLabel = new Label("Description");
        descLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 16px;");
        Text descText = new Text(ev.getDescription());
        descText.setWrappingWidth(700);
        descText.setStyle("-fx-fill: #555; -fx-font-size: 14px;");

        // Infos Grille (Date / Lieu)
        GridPane infoGrid = new GridPane();
        infoGrid.setHgap(40);
        infoGrid.setVgap(15);

        infoGrid.add(createDetailItem("📅 Date", ev.getDate().toString()), 0, 0);
        infoGrid.add(createDetailItem("📍 Lieu", ev.getLieu()), 1, 0);

        // --- ACTIONS ---
        HBox actions = new HBox(15);
        actions.setPadding(new Insets(20, 0, 0, 0));

        Button btnChat = new Button("Rejoindre la discussion");
        btnChat.setStyle("-fx-background-color: #1a3c1a; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 30; -fx-padding: 12 30; -fx-cursor: hand;");
        btnChat.setOnAction(e -> showCommunityChat(ev, gradient));

        actions.getChildren().addAll(btnChat);

        detailsCard.getChildren().addAll(topRow, new Separator(), descLabel, descText, infoGrid, actions);
        layout.getChildren().addAll(nav, detailsCard);

        mainContentVBox.getChildren().add(globalScroll);
    }

    // Helper pour les petits blocs d'info
    private VBox createDetailItem(String label, String value) {
        VBox box = new VBox(5);
        Label lbl = new Label(label);
        lbl.setStyle("-fx-text-fill: #888; -fx-font-size: 12px; -fx-font-weight: bold;");
        Label val = new Label(value);
        val.setStyle("-fx-text-fill: #333; -fx-font-size: 15px; -fx-font-weight: bold;");
        box.getChildren().addAll(lbl, val);
        return box;
    }
    // Helper: Cartes des membres (Design SideBar)
    private HBox createMemberCard(String name, String role, String bgColor, String textColor) {
        HBox card = new HBox(10);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(10));
        card.setStyle("-fx-background-color: " + bgColor + "; -fx-background-radius: 15;");
        VBox info = new VBox(2);
        Label nameLbl = new Label(name);
        nameLbl.setStyle("-fx-font-weight: bold; -fx-text-fill: " + textColor + "; -fx-font-size: 13px;");
        Label roleLbl = new Label(role);
        roleLbl.setStyle("-fx-font-size: 10px; -fx-text-fill: #777;");
        info.getChildren().addAll(nameLbl, roleLbl);
        card.getChildren().add(info);
        return card;
    }

    // Helper: Bulles de discussion "Smart"
    private void addSmartBubble(VBox container, String user, String text, boolean isMoi) {
        VBox wrapper = new VBox(5);
        wrapper.setAlignment(isMoi ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);

        Label nameLbl = new Label(user);
        nameLbl.setStyle("-fx-font-size: 11px; -fx-text-fill: #999; -fx-padding: 0 5 0 5;");

        Label msgLbl = new Label(text);
        msgLbl.setWrapText(true);
        msgLbl.setMaxWidth(400);
        msgLbl.setPadding(new Insets(12, 18, 12, 18));

        if (isMoi) {
            msgLbl.setStyle(
                    "-fx-background-color: rgba(46, 204, 113, 0.25); " +
                            "-fx-text-fill: #1a3c1a; " +
                            "-fx-background-radius: 15 15 0 15;"
            );

        } else {
            msgLbl.setStyle("-fx-background-color: #F0F0F0; -fx-text-fill: #333; -fx-background-radius: 20 20 20 2;");
        }

        wrapper.getChildren().addAll(nameLbl, msgLbl);
        container.getChildren().add(wrapper);
    }



    // Helper for the "Bubble" design

    private void fetchReadableAddress(String coords, Label targetLabel) {
        new Thread(() -> {
            try {
                String[] parts = coords.split(",");
                if (parts.length < 2) {
                    Platform.runLater(() -> targetLabel.setText(coords));
                    return;
                }

                // Utilisation de Nominatim (OpenStreetMap)
                String urlString = String.format(
                        "https://nominatim.openstreetmap.org/reverse?format=json&lat=%s&lon=%s&zoom=14",
                        parts[0].trim(), parts[1].trim()
                );

                java.net.URL url = new java.net.URL(urlString);
                java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
                conn.setRequestProperty("User-Agent", "JavaFX-Agricultural-App");

                java.util.Scanner s = new java.util.Scanner(conn.getInputStream(), "UTF-8").useDelimiter("\\A");
                String response = s.hasNext() ? s.next() : "";

                if (response.contains("\"display_name\":\"")) {
                    String fullAddress = response.split("\"display_name\":\"")[1].split("\"")[0];
                    // On nettoie pour ne garder que "Ville, Quartier"
                    String[] addrParts = fullAddress.split(",");
                    String shortAddress = (addrParts.length > 2) ? addrParts[0] + ", " + addrParts[1] : addrParts[0];
                    Platform.runLater(() -> targetLabel.setText(shortAddress));
                } else {
                    Platform.runLater(() -> targetLabel.setText("Lieu non identifié"));
                }
            } catch (Exception e) {
                Platform.runLater(() -> targetLabel.setText("Adresse indisponible"));
            }
        }).start();
    }
    @FXML
    public void showGestionEvenements(YearMonth restoreMonth) {
        if (mainContentVBox == null) return;

        if (restoreMonth != null) currentYearMonth = restoreMonth;

        // Clear seulement le contenu nécessaire pour éviter de perdre le topRow et notifications
        mainContentVBox.getChildren().clear();
        mainContentVBox.setSpacing(30);
        mainContentVBox.setPadding(new Insets(40, 60, 40, 60));
        mainContentVBox.setAlignment(Pos.TOP_CENTER);
        mainContentVBox.setStyle("-fx-background-color: #e8f3e8;");

        // --- TITLE & TOP ICONS ROW ---
        HBox topRow = new HBox();
        topRow.setAlignment(Pos.CENTER_LEFT);
        topRow.setMaxWidth(1100);

        Label title = new Label("Catalogue AgriCore");
        title.setStyle("-fx-font-size: 36px; -fx-font-weight: bold; -fx-text-fill: #1a3c1a; -fx-font-family: 'Segoe UI', sans-serif;");

        Region spacerT = new Region();
        HBox.setHgrow(spacerT, Priority.ALWAYS);

        // Groupe Icônes (Cloche + Calendrier)
        HBox iconBox = new HBox(20);
        iconBox.setAlignment(Pos.CENTER_RIGHT);

        // Cloche
        StackPane bellStack = new StackPane();
        bellStack.setCursor(Cursor.HAND);
        Label bellIcon = new Label("🔔");
        bellIcon.setStyle("-fx-font-size: 24px; -fx-text-fill: #1a3c1a;");

        // Badge dynamique
        if (activeNotifications != null && !activeNotifications.isEmpty()) {
            Label badge = new Label(String.valueOf(activeNotifications.size()));
            badge.setStyle("-fx-background-color: #d32f2f; -fx-text-fill: white; -fx-font-size: 10px; -fx-padding: 2 6; -fx-background-radius: 10; -fx-translate-x: 10; -fx-translate-y: -10;");
            bellStack.getChildren().addAll(bellIcon, badge);
        } else {
            bellStack.getChildren().add(bellIcon);
        }

        bellStack.setOnMouseClicked(e -> showNotificationPopup(bellStack));

        // Calendrier
        StackPane calBtn = new StackPane();
        calBtn.setPadding(new Insets(15));
        calBtn.setCursor(Cursor.HAND);
        calBtn.setStyle("-fx-background-color: white; -fx-background-radius: 15; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 10, 0, 0, 5);");
        Label calIcon = new Label("📅");
        calIcon.setStyle("-fx-font-size: 24px;");
        calBtn.getChildren().add(calIcon);
        calBtn.setOnMouseClicked(e -> showFullCalendarView());

        iconBox.getChildren().addAll(bellStack, calBtn);
        topRow.getChildren().addAll(title, spacerT, iconBox);
        mainContentVBox.getChildren().add(topRow);

        // --- SEARCH BAR "PILL" ---
        HBox searchPill = new HBox(15);
        searchPill.setAlignment(Pos.CENTER_LEFT);
        searchPill.setPadding(new Insets(10, 25, 10, 25));
        searchPill.setMaxWidth(1100);
        searchPill.setStyle("-fx-background-color: white; -fx-background-radius: 50; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.08), 15, 0, 0, 8);");

        searchInput = new TextField();
        searchInput.setPromptText("Rechercher...");
        searchInput.setPrefWidth(400);
        searchInput.setStyle("-fx-background-color: transparent; -fx-prompt-text-fill: #aaa; -fx-font-size: 14px;");
        searchInput.textProperty().addListener((obs, old, val) -> updateFilters());

        Separator sep1 = new Separator(javafx.geometry.Orientation.VERTICAL);
        sep1.setPrefHeight(30);

        datePicker = new DatePicker();
        datePicker.setPromptText("Date");
        datePicker.setPrefWidth(150);
        datePicker.setStyle("-fx-background-color: transparent; -fx-border-color: #ddd; -fx-border-radius: 5;");
        datePicker.valueProperty().addListener((obs, old, val) -> updateFilters());

        Separator sep2 = new Separator(javafx.geometry.Orientation.VERTICAL);
        sep2.setPrefHeight(30);

        VBox budgetBox = new VBox(-2);
        budgetBox.setAlignment(Pos.CENTER);
        priceSlider = new Slider(0, 500, 100);
        priceSlider.setPrefWidth(120);
        Label budgetTitle = new Label("Budget Max");
        budgetTitle.setStyle("-fx-font-size: 10px; -fx-text-fill: #888; -fx-font-weight: bold;");
        priceValueLabel = new Label("100 DT");
        priceValueLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #2d5a27;");
        priceSlider.valueProperty().addListener((obs, old, val) -> {
            priceValueLabel.setText(Math.round(val.doubleValue()) + " DT");
            updateFilters();
        });
        budgetBox.getChildren().addAll(budgetTitle, priceSlider);

        HBox priceDisplay = new HBox(5, budgetBox, priceValueLabel);
        priceDisplay.setAlignment(Pos.CENTER);

        Separator sep3 = new Separator(javafx.geometry.Orientation.VERTICAL);
        sep3.setPrefHeight(30);

        resetButton = new Button("Réinitialiser");
        resetButton.setStyle("-fx-background-color: #f5f5f5; -fx-background-radius: 20; -fx-text-fill: #666; -fx-cursor: hand;");
        resetButton.setOnAction(e -> {
            searchInput.clear();
            datePicker.setValue(null);
            priceSlider.setValue(500);
        });

        searchPill.getChildren().addAll(searchInput, sep1, datePicker, sep2, priceDisplay, sep3, resetButton);
        mainContentVBox.getChildren().add(searchPill);

        // --- EVENT GRID ---
        eventGrid = new GridPane();
        eventGrid.setHgap(40);
        eventGrid.setVgap(40);
        eventGrid.setAlignment(Pos.TOP_LEFT);
        eventGrid.setMaxWidth(1100);

        ScrollPane scrollPane = new ScrollPane(eventGrid);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent;");

        mainContentVBox.getChildren().add(scrollPane);

        // --- Refresh ou update ---
        if (restoreMonth == null) refreshData();
        else updateFilters();
    }






    private void showRegistrationForm(EvennementAgricole ev) {
        StackPane container = (StackPane) mainContentVBox.getChildren().get(0);
        VBox detailCard = (VBox) container.lookup("#detailCard");

        if (detailCard != null) {
            detailCard.setEffect(new javafx.scene.effect.BoxBlur(10, 10, 3));
        }

        StackPane glassPane = new StackPane();
        glassPane.setStyle("-fx-background-color: transparent;");

        VBox formCard = new VBox(20);
        formCard.setAlignment(Pos.CENTER);
        formCard.setMaxSize(420, 650); // Increased height for the extra field
        formCard.setPadding(new Insets(30));
        formCard.setStyle("-fx-background-color: white; -fx-background-radius: 25; " +
                "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.3), 30, 0, 0, 10); " +
                "-fx-border-color: #e0e0e0; -fx-border-radius: 25;");

        StackPane.setAlignment(formCard, Pos.CENTER);

        Label formTitle = new Label("Finaliser l'inscription");
        formTitle.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: #1a3c1a;");

        int initialPlacesDispo = getRemainingPlaces(ev);
        if (initialPlacesDispo <= 0) {
            if (detailCard != null) detailCard.setEffect(null);
            showAlert("Événement Complet", "Désolé, plus de places disponibles.");
            return;
        }

        // --- 1. CHAMPS (NOM, EMAIL) ---
        VBox inputsBox = new VBox(15);

        // Nom Field
        VBox nameBox = new VBox(5);
        Label nameLabel = new Label("Nom du participant");
        nameLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #555;");
        TextField nameInput = new TextField();
        try { nameInput.setText(partService.getUserRealName(CURRENT_USER_ID)); } catch (Exception e) {}
        styleField(nameInput, "Nom complet");
        nameBox.getChildren().addAll(nameLabel, nameInput);

        // Email Field (Fix for SQLException)
        VBox emailBox = new VBox(5);
        Label emailLabel = new Label("Email de contact");
        emailLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #555;");
        TextField emailInput = new TextField();
        // Pre-fill email if your service supports it, otherwise prompt the user
        try { emailInput.setText(partService.getUserEmail(CURRENT_USER_ID)); } catch (Exception e) {}
        styleField(emailInput, "exemple@domaine.com");
        emailBox.getChildren().addAll(emailLabel, emailInput);

        inputsBox.getChildren().addAll(nameBox, emailBox);

        // --- 2. PLACES & TOTAL ---
        VBox placesBox = new VBox(10);
        placesBox.setAlignment(Pos.CENTER_LEFT);
        Label infoPlaces = new Label("Nombre de places à réserver :");
        infoPlaces.setStyle("-fx-font-weight: bold;");

        Spinner<Integer> placesSpinner = new Spinner<>(1, initialPlacesDispo, 1);
        placesSpinner.setMaxWidth(Double.MAX_VALUE);
        placesSpinner.setStyle("-fx-font-size: 16px; -fx-background-radius: 10;");

        Label remainingCountLbl = new Label("Places encore disponibles : " + (initialPlacesDispo - 1));
        remainingCountLbl.setStyle("-fx-text-fill: #e67e22; -fx-font-weight: bold; -fx-font-size: 13px;");

        Label totalLbl = new Label("Total : " + ev.getFraisInscription() + " DT");
        totalLbl.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: #2d5a27;");

        placesSpinner.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                totalLbl.setText("Total : " + (newVal * ev.getFraisInscription()) + " DT");
                int restantes = initialPlacesDispo - newVal;
                remainingCountLbl.setText("Places encore disponibles : " + restantes);
            }
        });

        placesBox.getChildren().addAll(infoPlaces, placesSpinner, remainingCountLbl);

        // --- 3. BOUTONS ---
        Button btnSubmit = new Button("Confirmer l'inscription 🎟");
        btnSubmit.setMaxWidth(Double.MAX_VALUE);
        btnSubmit.setCursor(Cursor.HAND);
        btnSubmit.setStyle("-fx-background-color: #1a3c1a; -fx-text-fill: white; -fx-font-weight: bold; " +
                "-fx-padding: 15; -fx-background-radius: 12;");

        btnSubmit.setOnAction(e -> {
            String name = nameInput.getText().trim();
            String email = emailInput.getText().trim();

            if (name.isEmpty() || email.isEmpty()) {
                if (name.isEmpty()) nameInput.setStyle("-fx-border-color: red; -fx-background-radius: 10; -fx-border-radius: 10;");
                if (email.isEmpty()) emailInput.setStyle("-fx-border-color: red; -fx-background-radius: 10; -fx-border-radius: 10;");
                return;
            }

            try {
                int nbr = placesSpinner.getValue();
                double total = nbr * ev.getFraisInscription();
                String code = String.valueOf((int)(Math.random() * 90000) + 10000);

                // Assuming your Participant constructor now accepts email as the 9th parameter
                Participant p = new Participant(
                        CURRENT_USER_ID,        // idUtilisateur
                        ev.getIdEvennement(),   // idEvennement
                        LocalDate.now(),        // dateInscription
                        "Confirmee",            // statutParticipation
                        String.valueOf(total),  // montantPayee
                        "confirmed",                  // confirmation

                        nbr,                    // nbrPlaces
                        name,                   // nomParticipant
                        email,                  // email (FIX: Field no longer null)
                        code,                   // entryCode
                        0,                      // nbrPresents
                        "0"                     // confirmToken
                );

                partService.create(p);
                reservedEventIds.add(ev.getIdEvennement());

                if (detailCard != null) detailCard.setEffect(null);
                container.getChildren().remove(glassPane);
                showQRCodePage(ev, nbr, total, name, code);

            } catch (Exception ex) {
                ex.printStackTrace();
                showAlert("Erreur", "Une erreur est survenue lors de l'inscription.");
            }
        });

        Button btnCancel = new Button("Annuler");
        btnCancel.setStyle("-fx-background-color: transparent; -fx-text-fill: #7f8c8d; -fx-cursor: hand; -fx-underline: true;");
        btnCancel.setOnAction(e -> playCancelAnimation(container, glassPane, detailCard));

        formCard.getChildren().addAll(formTitle, new Separator(), inputsBox, placesBox, totalLbl, btnSubmit, btnCancel);
        glassPane.getChildren().add(formCard);
        container.getChildren().add(glassPane);
    }

    // --- MÉTHODE ANIMATION ANNULATION ---
    private void playCancelAnimation(StackPane container, StackPane glassPane, VBox detailCard) {
        // Création de l'icône X mignonne
        Label xIcon = new Label("✕");
        xIcon.setStyle("-fx-font-size: 80px; -fx-text-fill: #ff6b6b; -fx-font-weight: bold; " +
                "-fx-background-color: white; -fx-background-radius: 100; " +
                "-fx-min-width: 120; -fx-min-height: 120; -fx-alignment: center; " +
                "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.2), 15, 0, 0, 5);");

        // On l'ajoute au milieu du container
        container.getChildren().add(xIcon);

        // Retrait immédiat du formulaire
        container.getChildren().remove(glassPane);

        // Animation de Zoom
        ScaleTransition scale = new ScaleTransition(Duration.millis(400), xIcon);
        scale.setFromX(0);
        scale.setFromY(0);
        scale.setToX(1.1);
        scale.setToY(1.1);

        // Animation de Disparition (Fade)
        FadeTransition fade = new FadeTransition(Duration.millis(400), xIcon);
        fade.setFromValue(1.0);
        fade.setToValue(0.0);
        fade.setDelay(Duration.millis(500)); // Attend un peu avant de s'effacer

        // Fin de l'animation
        fade.setOnFinished(event -> {
            container.getChildren().remove(xIcon); // Nettoyage
            if (detailCard != null) detailCard.setEffect(null); // Retrait du flou
        });

        scale.play();
        fade.play();
    }
    private void styleField(TextField f, String prompt) {
        f.setPromptText(prompt);
        f.setStyle("-fx-background-color: #f8f8f8; -fx-background-radius: 10; -fx-padding: 12; -fx-border-color: #eee; -fx-border-radius: 10;");
    }
    private void showQRCodePage(EvennementAgricole ev, int nbrPlaces, double montant, String name, String entryCode) {
        mainContentVBox.getChildren().clear();

        VBox ticket = new VBox(20);
        ticket.setAlignment(Pos.CENTER);
        ticket.setPadding(new Insets(40));
        ticket.setMaxWidth(400);
        ticket.setStyle("-fx-background-color: white; -fx-background-radius: 30; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.15), 20, 0, 0, 10);");

        Label title = new Label("VOTRE TICKET");
        title.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #1a3c1a;");

        // QR Code API
        String data = "AGR-" + entryCode + "-" + name.replace(" ", "");
        String qrUrl = "https://api.qrserver.com/v1/create-qr-code/?size=180x180&data=" + data;
        ImageView qrView = new ImageView(new Image(qrUrl, true));

        VBox details = new VBox(5);
        details.setAlignment(Pos.CENTER);
        Label evTitle = new Label(ev.getTitre());
        evTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 16px;");
        Label codeLabel = new Label(entryCode); // Une seule fois, en gros
        codeLabel.setStyle("-fx-font-size: 36px; -fx-font-weight: 900; -fx-text-fill: #2d5a27;");

        details.getChildren().addAll(new Label(name), evTitle, new Separator() {{ setMaxWidth(150); }}, new Label("CODE D'ACCÈS"), codeLabel);

        Button btnHome = new Button("Retour aux événements");
        btnHome.setStyle("-fx-background-color: transparent; -fx-text-fill: #1a3c1a; -fx-underline: true; -fx-cursor: hand;");
        btnHome.setOnAction(e -> showGestionEvenements(null));

        ticket.getChildren().addAll(title, qrView, details, btnHome);

        VBox wrapper = new VBox(ticket);
        wrapper.setAlignment(Pos.CENTER);
        mainContentVBox.getChildren().add(wrapper);
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
        Label txt = new Label(adding ? "SUCCÈS" : "ANNULÉ");
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
            showGestionEvenements(null);
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
        } catch (SQLException e) {
            System.err.println("Error calculating remaining places: " + e.getMessage());
            return ev.getCapaciteMax();
        }
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
        if (title == null) return "🌱";
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


    private HBox createInfoRow(String label, Object value) {
        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER_LEFT);
        Label lbl = new Label(label + " :");
        lbl.setStyle("-fx-font-weight: bold; -fx-text-fill: #555; -fx-min-width: 80;");

        if (value instanceof Node) {
            row.getChildren().addAll(lbl, (Node) value);
        } else {
            Label val = new Label(value.toString());
            val.setStyle("-fx-text-fill: #222;");
            row.getChildren().addAll(lbl, val);
        }
        return row;
    }
    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}