package Controller;

import Model.EvennementAgricole;
import Model.Participant;
import javafx.animation.*;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
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

        List<EvennementAgricole> myActiveEvents = allEvents.stream()
                .filter(ev -> reservedEventIds.contains(ev.getIdEvennement()))
                .filter(ev -> ev.getDateFin().isAfter(now))
                .collect(Collectors.toList());

        for (int i = 0; i < myActiveEvents.size(); i++) {
            EvennementAgricole evA = myActiveEvents.get(i);

            long hoursLeft = ChronoUnit.HOURS.between(now, evA.getDateDebut());
            if (hoursLeft >= 0 && hoursLeft <= 24) {
                activeNotifications.add(new NotificationItem("Rappel Événement",
                        "'" + evA.getTitre() + "' commence à " + evA.getDateDebut().format(tf), "REMINDER"));
            }

            for (int j = i + 1; j < myActiveEvents.size(); j++) {
                EvennementAgricole evB = myActiveEvents.get(j);
                if (evA.getDateDebut().toLocalDate().isEqual(evB.getDateDebut().toLocalDate())) {
                    boolean isOverlapping = evA.getDateDebut().isBefore(evB.getDateFin()) &&
                            evB.getDateDebut().isBefore(evA.getDateFin());
                    if (isOverlapping) {
                        activeNotifications.add(new NotificationItem("Conflit d'horaire",
                                "Conflit entre '" + evA.getTitre() + "' et '" + evB.getTitre() + "'.",
                                "CONFLICT"));
                    }
                }
            }
        }

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

        VBox container = new VBox();
        container.setPrefWidth(320);
        container.getStyleClass().add("notification-popup");

        HBox header = new HBox();
        header.setPadding(new Insets(15));
        header.getStyleClass().add("notification-header");
        Label title = new Label("Centre de Notifications");
        title.getStyleClass().add("notification-title");
        header.getChildren().add(title);
        container.getChildren().add(header);

        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefHeight(350);
        scrollPane.getStyleClass().add("notification-scroll");

        VBox list = new VBox();
        list.setSpacing(1);

        if (activeNotifications.isEmpty()) {
            VBox empty = createEmptyNotificationView();
            list.getChildren().add(empty);
        } else {
            for (NotificationItem item : activeNotifications) {
                VBox card = createNotificationCard(item, popup);
                list.getChildren().add(card);
            }
        }

        scrollPane.setContent(list);
        container.getChildren().add(scrollPane);

        popup.getContent().add(container);
        Bounds bounds = anchor.localToScreen(anchor.getBoundsInLocal());
        popup.show(anchor.getScene().getWindow(), bounds.getMinX() - 280, bounds.getMaxY() + 10);
    }

    private VBox createEmptyNotificationView() {
        VBox empty = new VBox(10);
        empty.setAlignment(Pos.CENTER);
        empty.setPadding(new Insets(40));
        Label emptyIcon = new Label("∅");
        emptyIcon.getStyleClass().add("notification-empty-icon");
        Label emptyText = new Label("Aucune notification pour le moment");
        emptyText.getStyleClass().add("notification-empty-text");
        empty.getChildren().addAll(emptyIcon, emptyText);
        return empty;
    }

    private VBox createNotificationCard(NotificationItem item, Popup popup) {
        VBox card = new VBox(5);
        card.setPadding(new Insets(12));
        card.getStyleClass().add("notification-card");

        Label rowTitle = new Label(item.title);
        rowTitle.getStyleClass().add(item.type.equals("CONFLICT") ? "notification-title-conflict" : "notification-title-reminder");

        Label rowMsg = new Label(item.message);
        rowMsg.setWrapText(true);
        rowMsg.getStyleClass().add("notification-message");

        card.getChildren().addAll(rowTitle, rowMsg);

        if (item.type.equals("CONFLICT")) {
            addAIConflictResolver(card, item, popup);
        }

        card.setOnMouseEntered(e -> card.setStyle("-fx-background-color: #f4fbf4;"));
        card.setOnMouseExited(e -> card.setStyle("-fx-background-color: white;"));

        return card;
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
        // Calendar view implementation (simplified for now)
        showAlert("Calendrier", "Fonctionnalité de calendrier à venir");
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

        // Filter events - REMOVED the past date filter to show ALL events
        List<EvennementAgricole> filtered = allEvents.stream()
                .filter(e -> {
                    // Apply search filter
                    boolean matchesTitle = query.isEmpty() || e.getTitre().toLowerCase().contains(query);

                    // Apply date filter
                    boolean matchesDate = (selectedDate == null) ||
                            e.getDateDebut().toLocalDate().isEqual(selectedDate);

                    // Apply price filter
                    boolean matchesPrice = e.getFraisInscription() <= maxPrice;

                    boolean keep = matchesTitle && matchesDate && matchesPrice;

                    if (!keep && !query.isEmpty()) {
                        System.out.println("  Filtered out: " + e.getTitre() +
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

    private void showEventDetails(EvennementAgricole ev, String gradient) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/EventDetails.fxml"));
            Parent root = loader.load();

            // You'll need to create an EventDetailsController and pass the event
            // EventDetailsController controller = loader.getController();
            // controller.setEvent(ev);

            Stage stage = new Stage();
            stage.setTitle("Détails de l'événement");
            stage.setScene(new Scene(root));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
            // Fallback to simple alert if details page not available
            showAlert("Détails de l'événement",
                    "Titre: " + ev.getTitre() + "\n" +
                            "Date: " + ev.getDateDebut().format(dtfWithTime) + "\n" +
                            "Lieu: " + ev.getLieu() + "\n" +
                            "Prix: " + ev.getFraisInscription() + " DT\n" +
                            "Description: " + ev.getDescription());
        }
    }

    private void showRegistrationForm(EvennementAgricole ev) {
        // Registration form implementation
    }

    private void triggerStatusAnimation(EvennementAgricole ev, boolean adding) {
        // Status animation implementation
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

    private HBox createInfoRow(String label, String value) {
        VBox v = new VBox(2);
        Label lbl = new Label(label);
        lbl.getStyleClass().add("info-row-label");
        Label val = new Label(value);
        val.getStyleClass().add("info-row-value");
        v.getChildren().addAll(lbl, val);
        return new HBox(10, v);
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}