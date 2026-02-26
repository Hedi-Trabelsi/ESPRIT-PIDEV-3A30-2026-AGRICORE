package Controller;

import Model.ActionLog;
import Model.EvennementAgricole;
import Model.Participant;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.concurrent.Worker;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.web.WebView;
import javafx.stage.Modality;
import javafx.stage.Stage;
import netscape.javascript.JSObject;
import services.EvennementService;
import services.ParticipantService;
import services.LogService;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

public class EventManagementController {

    @FXML
    private VBox mainContentVBox;

    private EvennementService evennementService;
    private ParticipantService participantService;
    private LogService logService;

    private String currentFilter = "TOUT";
    private final DateTimeFormatter dtfDisplay = DateTimeFormatter.ofPattern("dd/MM/yyyy hh:mm a");

    // Map bridge for JavaScript communication
    private MapBridge permanentBridge;

    public EventManagementController() {
        try {
            System.out.println("Initializing EventManagementController...");
            this.evennementService = new EvennementService();
            this.participantService = new ParticipantService();
            this.logService = new LogService();
            System.out.println("Services initialized successfully");
        } catch (SQLException e) {
            System.err.println("Failed to initialize services: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    public void initialize() {
        System.out.println("EventManagementController.initialize() called");
        if (mainContentVBox != null) {
            mainContentVBox.getStyleClass().add("event-main-content");
            System.out.println("mainContentVBox initialized");
        } else {
            System.err.println("mainContentVBox is NULL!");
        }
        showGestionEvenements();
    }

    private void logAction(String type, int targetId, String desc) {
        try {
            ActionLog log = new ActionLog(type, "evennement_agricole", targetId, desc);
            logService.create(log);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private int getRemainingPlaces(EvennementAgricole ev) {
        try {
            int booked = participantService.read().stream()
                    .filter(p -> p.getIdEvennement() == ev.getIdEvennement())
                    .mapToInt(Participant::getNbrPlaces)
                    .sum();
            return Math.max(0, ev.getCapaciteMax() - booked);
        } catch (SQLException e) {
            e.printStackTrace();
            return ev.getCapaciteMax();
        }
    }

    @FXML
    private void showGestionEvenements() {
        System.out.println("showGestionEvenements() called");
        resetMainView();

        // Create a wrapper VBox to hold everything
        VBox contentWrapper = new VBox(20);
        contentWrapper.setPadding(new Insets(20));
        contentWrapper.setAlignment(Pos.TOP_CENTER);

        // Header
        Label header = new Label("Gestion des Événements");
        header.setStyle("-fx-font-size: 28px; -fx-font-weight: bold; -fx-text-fill: #1a3c1a;");
        header.setPadding(new Insets(0, 0, 10, 0));

        // Action buttons
        HBox actionBar = new HBox(15);
        actionBar.setAlignment(Pos.CENTER_RIGHT);
        actionBar.setPadding(new Insets(0, 0, 10, 0));

        Button btnLogs = new Button("📜 Logs");
        btnLogs.setStyle("-fx-background-color: #555; -fx-text-fill: white; -fx-background-radius: 20; -fx-padding: 10 20; -fx-cursor: hand;");
        btnLogs.setOnAction(e -> showAuditLogs());

        Button btnAdd = new Button("+ Nouvel Événement");
        btnAdd.setStyle("-fx-background-color: #2d5a27; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 20; -fx-padding: 10 25; -fx-cursor: hand;");
        btnAdd.setOnAction(e -> renderForm(null));

        Button btnShowAll = new Button("📋 Tous les événements");
        btnShowAll.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-background-radius: 20; -fx-padding: 10 20; -fx-cursor: hand; -fx-margin-right: 10;");
        btnShowAll.setOnAction(e -> showAllEvenements());

        Button btnShowParticipants = new Button("👥 Tous les participants");
        btnShowParticipants.setStyle("-fx-background-color: #9b59b6; -fx-text-fill: white; -fx-background-radius: 20; -fx-padding: 10 20; -fx-cursor: hand;");
        btnShowParticipants.setOnAction(e -> showAllParticipants());

        actionBar.getChildren().addAll(btnShowAll, btnShowParticipants, btnLogs, btnAdd);

        // Search field
        TextField searchField = new TextField();
        searchField.setPromptText("🔍 Rechercher un événement...");
        searchField.setStyle("-fx-background-radius: 15; -fx-padding: 10 20; -fx-border-color: #ddd; -fx-border-radius: 15; -fx-font-size: 14px;");
        searchField.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(searchField, Priority.ALWAYS);

        // Filter buttons
        HBox filterBar = new HBox(15);
        filterBar.setAlignment(Pos.CENTER_LEFT);
        filterBar.setPadding(new Insets(10, 0, 20, 0));

        Button btnAll = createFilterButton("Tout", "TOUT");
        Button btnOngoing = createFilterButton("En cours", "EN_COURS");
        Button btnComing = createFilterButton("À venir", "COMING");
        Button btnHistory = createFilterButton("Historique", "HISTORIQUE");
        filterBar.getChildren().addAll(btnAll, btnOngoing, btnComing, btnHistory);

        // Events grid
        FlowPane flowPane = new FlowPane(20, 25);
        flowPane.setAlignment(Pos.TOP_CENTER);
        flowPane.setPadding(new Insets(10, 0, 30, 0));
        flowPane.setPrefWrapLength(900);

        // Add all to content wrapper
        contentWrapper.getChildren().addAll(header, actionBar, searchField, filterBar, flowPane);

        // Add content wrapper to main VBox
        mainContentVBox.getChildren().add(contentWrapper);

        // Load events
        loadEventsToGrid(flowPane, searchField, btnAll, btnOngoing, btnComing, btnHistory, filterBar);
    }

    private void loadEventsToGrid(FlowPane flowPane, TextField searchField, Button btnAll, Button btnOngoing,
                                  Button btnComing, Button btnHistory, HBox filterBar) {
        try {
            List<EvennementAgricole> allEvents = evennementService.read();
            System.out.println("Loaded " + allEvents.size() + " events from database");

            if (allEvents.isEmpty()) {
                Label emptyLabel = new Label("Aucun événement dans la base de données");
                emptyLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: #666; -fx-padding: 50;");
                flowPane.getChildren().add(emptyLabel);
                return;
            }

            Runnable refreshList = () -> {
                flowPane.getChildren().clear();
                String searchText = searchField.getText().toLowerCase();
                LocalDate today = LocalDate.now();

                List<EvennementAgricole> filteredEvents = allEvents.stream()
                        .filter(ev -> ev.getTitre().toLowerCase().contains(searchText) ||
                                (ev.getLieu() != null && ev.getLieu().toLowerCase().contains(searchText)))
                        .filter(ev -> {
                            LocalDate start = ev.getDateDebut().toLocalDate();
                            LocalDate end = ev.getDateFin().toLocalDate();
                            if (currentFilter.equals("HISTORIQUE")) return end.isBefore(today);
                            if (currentFilter.equals("EN_COURS")) return (start.isBefore(today) || start.isEqual(today)) && (end.isAfter(today) || end.isEqual(today));
                            if (currentFilter.equals("COMING")) return start.isAfter(today);
                            return true;
                        })
                        .collect(Collectors.toList());

                System.out.println("Showing " + filteredEvents.size() + " events after filtering");

                if (filteredEvents.isEmpty()) {
                    Label emptyLabel = new Label("Aucun événement trouvé");
                    emptyLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: #666; -fx-padding: 50;");
                    flowPane.getChildren().add(emptyLabel);
                } else {
                    for (EvennementAgricole ev : filteredEvents) {
                        VBox card = createEventCard(ev);
                        flowPane.getChildren().add(card);
                    }
                }
            };

            searchField.textProperty().addListener((obs, old, newVal) -> refreshList.run());
            btnAll.setOnAction(e -> {
                currentFilter = "TOUT";
                refreshList.run();
                updateTabStyles(filterBar);
            });
            btnOngoing.setOnAction(e -> {
                currentFilter = "EN_COURS";
                refreshList.run();
                updateTabStyles(filterBar);
            });
            btnComing.setOnAction(e -> {
                currentFilter = "COMING";
                refreshList.run();
                updateTabStyles(filterBar);
            });
            btnHistory.setOnAction(e -> {
                currentFilter = "HISTORIQUE";
                refreshList.run();
                updateTabStyles(filterBar);
            });

            refreshList.run();

        } catch (SQLException e) {
            System.err.println("SQL Error loading events: " + e.getMessage());
            e.printStackTrace();
            showAlert("Erreur", "Impossible de charger les événements: " + e.getMessage());

            Label errorLabel = new Label("Erreur de chargement: " + e.getMessage());
            errorLabel.setStyle("-fx-text-fill: red; -fx-padding: 20;");
            flowPane.getChildren().add(errorLabel);
        }
    }

    private void showAllEvenements() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/ShowEvennement.fxml"));
            Parent root = loader.load();

            Stage stage = new Stage();
            stage.setTitle("Tous les événements");
            stage.setScene(new Scene(root));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Erreur", "Impossible d'ouvrir la liste des événements: " + e.getMessage());
        }
    }

    private void showAllParticipants() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/ShowParticipant.fxml"));
            Parent root = loader.load();

            Stage stage = new Stage();
            stage.setTitle("Tous les participants");
            stage.setScene(new Scene(root));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Erreur", "Impossible d'ouvrir la liste des participants: " + e.getMessage());
        }
    }

    private Button createFilterButton(String text, String filterValue) {
        Button b = new Button(text);
        b.setUserData(filterValue);
        b.setCursor(javafx.scene.Cursor.HAND);
        b.setStyle("-fx-background-color: #eee; -fx-text-fill: #555; -fx-background-radius: 20; -fx-padding: 8 20;");
        return b;
    }

    private void updateTabStyles(HBox filterBar) {
        filterBar.getChildren().forEach(node -> {
            Button b = (Button) node;
            boolean isActive = b.getUserData().equals(currentFilter);
            if (isActive) {
                b.setStyle("-fx-background-color: #2d5a27; -fx-text-fill: white; -fx-background-radius: 20; -fx-padding: 8 20; -fx-font-weight: bold;");
            } else {
                b.setStyle("-fx-background-color: #eee; -fx-text-fill: #555; -fx-background-radius: 20; -fx-padding: 8 20;");
            }
        });
    }

    private VBox createEventCard(EvennementAgricole ev) {
        VBox card = new VBox(8);
        card.setPrefWidth(260);
        card.setPadding(new Insets(15));
        card.setAlignment(Pos.TOP_CENTER);

        int left = getRemainingPlaces(ev);
        LocalDate today = LocalDate.now();
        LocalDate start = ev.getDateDebut().toLocalDate();
        LocalDate end = ev.getDateFin().toLocalDate();
        boolean isHistory = end.isBefore(today);

        String statusText = "● ACTIF";
        String statusColor = "#27ae60", bgColor = "white", borderColor = "#eee";

        if (left == 0) {
            statusText = "● COMPLET";
            statusColor = "#f39c12";
        }
        else if (isHistory) {
            statusText = "● TERMINÉ";
            statusColor = "#e74c3c";
            bgColor = "#fff5f5";
            borderColor = "#ffcdd2";
        }
        else if (start.isBefore(today) || start.isEqual(today)) {
            statusText = "● EN COURS";
            statusColor = "#27ae60";
            bgColor = "#f0fff4";
            borderColor = "#c6f6d5";
        }

        card.setStyle("-fx-background-color: " + bgColor + "; -fx-background-radius: 15; -fx-border-color: " + borderColor + "; -fx-border-width: 1.5; -fx-border-radius: 15;");

        DropShadow shadow = new DropShadow(10, Color.rgb(0,0,0,0.08));
        card.setEffect(shadow);

        card.setOnMouseEntered(e -> {
            card.setScaleX(1.03);
            card.setScaleY(1.03);
            shadow.setRadius(20);
        });

        card.setOnMouseExited(e -> {
            card.setScaleX(1.0);
            card.setScaleY(1.0);
            shadow.setRadius(10);
        });

        Label statusBadge = new Label(statusText);
        statusBadge.setStyle("-fx-text-fill: " + statusColor + "; -fx-font-weight: bold; -fx-font-size: 10px;");
        statusBadge.setMaxWidth(Double.MAX_VALUE);
        statusBadge.setAlignment(Pos.CENTER_RIGHT);

        HBox icons = new HBox(15);
        icons.setAlignment(Pos.CENTER_RIGHT);

        Label peopleIcon = createIconLabel("👥", "#3498db", () -> showParticipantsForEvent(ev));
        icons.getChildren().add(peopleIcon);

        if (!isHistory) {
            Label trashIcon = createIconLabel("🗑", "#e74c3c", null);
            trashIcon.setOnMouseClicked(e -> {
                HBox confirm = new HBox(5);
                Button y = new Button("✔");
                y.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white;");
                y.setOnAction(a -> {
                    try {
                        boolean deleted = evennementService.delete(ev.getIdEvennement());
                        if (deleted) {
                            logAction("DELETE", ev.getIdEvennement(), "Événement supprimé: " + ev.getTitre());
                            showGestionEvenements();
                        } else {
                            showAlert("Erreur", "Impossible de supprimer l'événement");
                        }
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                });
                Button n = new Button("✖");
                n.setOnAction(a -> icons.getChildren().setAll(peopleIcon, trashIcon));
                confirm.getChildren().addAll(y, n);
                icons.getChildren().setAll(confirm);
            });
            icons.getChildren().add(trashIcon);
        }

        VBox clickArea = new VBox(5);
        clickArea.setAlignment(Pos.CENTER);
        clickArea.setCursor(javafx.scene.Cursor.HAND);
        clickArea.setOnMouseClicked(e -> showEventDetails(ev));

        Label titleLabel = new Label(ev.getTitre());
        titleLabel.setStyle("-fx-font-size: 17px; -fx-font-weight: bold; -fx-text-fill: #1a3c1a; -fx-wrap-text: true; -fx-alignment: center;");
        titleLabel.setMinHeight(45);

        Label locLabel = new Label("📍 " + (ev.getLieu() != null ? ev.getLieu() : "Lieu non spécifié"));
        locLabel.setStyle("-fx-text-fill: #555;");

        Label capacityLeft = new Label(left + " places restantes");
        capacityLeft.setStyle("-fx-text-fill: " + (left < 5 ? "#e74c3c" : "#2d5a27") + "; -fx-font-size: 11px; -fx-font-weight: bold;");

        clickArea.getChildren().addAll(titleLabel, locLabel, capacityLeft);
        card.getChildren().addAll(statusBadge, icons, clickArea, new Region());

        if (!isHistory) {
            Button btnEdit = new Button("Modifier");
            btnEdit.setStyle("-fx-background-color: transparent; -fx-border-color: #2d5a27; -fx-text-fill: #2d5a27; -fx-border-radius: 8; -fx-font-weight: bold; -fx-cursor: hand; -fx-max-width: infinity;");
            btnEdit.setOnAction(e -> openUpdateForm(ev));
            card.getChildren().add(btnEdit);
        } else {
            Label lockedLabel = new Label("Consultation Uniquement");
            lockedLabel.setStyle("-fx-text-fill: #888; -fx-font-size: 10px; -fx-font-style: italic;");
            card.getChildren().add(lockedLabel);
        }

        VBox.setVgrow(card.getChildren().get(3), Priority.ALWAYS);
        return card;
    }

    private void openUpdateForm(EvennementAgricole ev) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/UpdateEvenement.fxml"));
            Parent root = loader.load();

            UpdateEvenementController controller = loader.getController();

            Stage stage = new Stage();
            controller.setEvenement(ev, stage, null);
            controller.setOnUpdateCallback(this::showGestionEvenements);

            stage.setTitle("Modifier l'événement");
            stage.setScene(new Scene(root));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Erreur", "Impossible d'ouvrir le formulaire de modification: " + e.getMessage());
        }
    }

    private void renderForm(EvennementAgricole targetEv) {
        resetMainView();
        boolean isEdit = (targetEv != null);

        VBox formCard = new VBox(15);
        formCard.setPadding(new Insets(30));
        formCard.setMaxWidth(600);
        formCard.setStyle("-fx-background-color: white; -fx-background-radius: 20;");
        formCard.setEffect(new DropShadow(20, Color.rgb(0,0,0,0.15)));

        Label head = new Label(isEdit ? "Mise à jour" : "Nouvel Événement");
        head.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #1a3c1a;");

        VBox tBox = createValidatedField("Titre", isEdit ? targetEv.getTitre() : "", "TEXT");
        VBox lBox = createValidatedField("Lieu (Cliquez sur la carte)", isEdit ? targetEv.getLieu() : "", "TEXT");
        TextField lieuField = (TextField) lBox.getChildren().get(1);

        HBox mapCenterer = new HBox();
        mapCenterer.setAlignment(Pos.CENTER);
        StackPane mapStack = new StackPane();
        mapStack.setPrefSize(200, 200);
        mapStack.setMaxSize(200, 200);
        WebView mapWebView = new WebView();
        mapWebView.setPrefSize(200, 200);
        mapWebView.setClip(new Circle(100, 100, 100));
        loadCircularSyncedMap(mapWebView, lieuField);
        mapStack.getChildren().add(mapWebView);
        mapCenterer.getChildren().add(mapStack);

        VBox descBox = createValidatedField("Description", isEdit ? targetEv.getDescription() : "", "AREA");

        // Date and time pickers with AM/PM
        HBox startRow = createDateTimePickerBox("Début", isEdit ? targetEv.getDateDebut() : LocalDateTime.now().withMinute(0));
        HBox endRow = createDateTimePickerBox("Fin", isEdit ? targetEv.getDateFin() : LocalDateTime.now().plusDays(1).withMinute(0));

        HBox numRow = new HBox(15);
        VBox capBox = createValidatedField("Capacité", isEdit ? String.valueOf(targetEv.getCapaciteMax()) : "", "INT");
        VBox fraisBox = createValidatedField("Frais (DT)", isEdit ? String.valueOf(targetEv.getFraisInscription()) : "", "NUMBER");
        HBox.setHgrow(capBox, Priority.ALWAYS);
        HBox.setHgrow(fraisBox, Priority.ALWAYS);
        numRow.getChildren().addAll(capBox, fraisBox);

        Button btnSave = new Button(isEdit ? "Enregistrer les modifications" : "Créer l'Événement");
        btnSave.setMaxWidth(Double.MAX_VALUE);
        btnSave.setStyle("-fx-background-color: #2d5a27; -fx-text-fill: white; -fx-padding: 15; -fx-background-radius: 12; -fx-font-weight: bold; -fx-cursor: hand;");
        btnSave.setOnAction(e -> {
            try {
                EvennementAgricole ev = isEdit ? targetEv : new EvennementAgricole();
                ev.setTitre(((TextField)tBox.getChildren().get(1)).getText());
                ev.setLieu(lieuField.getText());
                ev.setDescription(((TextArea)descBox.getChildren().get(1)).getText());
                ev.setDateDebut(getDateTimeFromRow(startRow));
                ev.setDateFin(getDateTimeFromRow(endRow));
                ev.setCapaciteMax(Integer.parseInt(((TextField)capBox.getChildren().get(1)).getText()));
                ev.setFraisInscription((int) Double.parseDouble(((TextField)fraisBox.getChildren().get(1)).getText()));
                ev.setStatut("Actif");

                if (isEdit) {
                    evennementService.update(ev);
                    logAction("UPDATE", ev.getIdEvennement(), "Modification de: " + ev.getTitre());
                } else {
                    int newId = evennementService.create(ev);
                    if (newId > 0) {
                        ev.setIdEvennement(newId);
                        logAction("CREATE", newId, "Nouvel événement créé: " + ev.getTitre());
                    }
                }
                showGestionEvenements();
            } catch (Exception ex) {
                ex.printStackTrace();
                showAlert("Erreur", "Vérifiez vos données: " + ex.getMessage());
            }
        });

        Button btnBack = new Button("Annuler");
        btnBack.setStyle("-fx-background-color: transparent; -fx-text-fill: #888; -fx-cursor: hand;");
        btnBack.setOnAction(a -> showGestionEvenements());

        formCard.getChildren().addAll(head, tBox, lBox, mapCenterer, descBox, startRow, endRow, numRow, btnSave, btnBack);
        mainContentVBox.getChildren().add(formCard);
    }

    private HBox createDateTimePickerBox(String label, LocalDateTime current) {
        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER_LEFT);

        Label lbl = new Label(label);
        lbl.setPrefWidth(50);
        lbl.setStyle("-fx-font-weight: bold;");

        DatePicker dp = new DatePicker(current.toLocalDate());
        dp.setPrefWidth(120);

        // AM/PM Logic: Convert 24h to 12h for the ComboBoxes
        ComboBox<Integer> hours = new ComboBox<>(FXCollections.observableArrayList(1,2,3,4,5,6,7,8,9,10,11,12));
        int hour24 = current.getHour();
        hours.setValue(hour24 == 0 ? 12 : (hour24 > 12 ? hour24 - 12 : hour24));

        ComboBox<Integer> mins = new ComboBox<>(FXCollections.observableArrayList(0,15,30,45));
        mins.setValue((current.getMinute() / 15) * 15);

        ComboBox<String> amPm = new ComboBox<>(FXCollections.observableArrayList("AM", "PM"));
        amPm.setValue(hour24 >= 12 ? "PM" : "AM");

        row.getChildren().addAll(lbl, dp, hours, mins, amPm);
        return row;
    }

    private LocalDateTime getDateTimeFromRow(HBox row) {
        DatePicker dp = (DatePicker) row.getChildren().get(1);
        ComboBox<Integer> hCombo = (ComboBox<Integer>) row.getChildren().get(2);
        ComboBox<Integer> mCombo = (ComboBox<Integer>) row.getChildren().get(3);
        ComboBox<String> amPmCombo = (ComboBox<String>) row.getChildren().get(4);

        int hour = hCombo.getValue();
        // Convert back to 24h format for LocalDateTime
        if (amPmCombo.getValue().equals("PM") && hour < 12) hour += 12;
        if (amPmCombo.getValue().equals("AM") && hour == 12) hour = 0;

        return LocalDateTime.of(dp.getValue(), LocalTime.of(hour, mCombo.getValue()));
    }

    private void loadCircularSyncedMap(WebView webView, TextField targetField) {
        this.permanentBridge = new MapBridge(targetField);

        // HTML with Leaflet map and spinning earth animation
        String html = "<!DOCTYPE html><html><head>" +
                "<link rel=\"stylesheet\" href=\"https://unpkg.com/leaflet@1.9.4/dist/leaflet.css\" />" +
                "<script src=\"https://unpkg.com/leaflet@1.9.4/dist/leaflet.js\"></script>" +
                "<style>body,html{height:100%;margin:0;padding:0;overflow:hidden;background:transparent;}" +
                "#map{position:absolute;top:0;bottom:0;left:0;right:0;opacity:0;transition:opacity 1s ease-in;cursor:crosshair;}" +
                "#earth{position:absolute;width:100%;height:100%;background:url('https://upload.wikimedia.org/wikipedia/commons/thumb/2/23/Blue_Marble_2002.png/600px-Blue_Marble_2002.png');background-size:cover;border-radius:50%;animation:spin 12s linear infinite;z-index:10;}" +
                "@keyframes spin{from{background-position:0 0;}to{background-position:600px 0;}}</style></head>" +
                "<body><div id=\"earth\"></div><div id=\"map\"></div>" +
                "<script>" +
                "var map=L.map('map',{zoomControl:false,attributionControl:false}).setView([36.8065,10.1815],6);" +
                "L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png').addTo(map);" +
                "var marker;" +

                // Search function for address lookup
                "function searchAddress(address) {" +
                "  fetch('https://nominatim.openstreetmap.org/search?format=json&q=' + address)" +
                "    .then(response => response.json())" +
                "    .then(data => {" +
                "      if (data.length > 0) {" +
                "        var lat = data[0].lat; var lon = data[0].lon;" +
                "        var latlng = [lat, lon];" +
                "        map.flyTo(latlng, 13);" +
                "        if(marker) map.removeLayer(marker);" +
                "        marker = L.marker(latlng).addTo(map);" +
                "        window.javaApp.updateCoords(lat + ', ' + lon);" +
                "      }" +
                "    });" +
                "}" +

                "function sendToJava(coords){if(window.javaApp){window.javaApp.updateCoords(coords);}}" +
                "window.onload=function(){setTimeout(function(){document.getElementById('earth').style.opacity='0';setTimeout(function(){document.getElementById('earth').style.display='none';document.getElementById('map').style.opacity='1';map.invalidateSize();},1000);},2000);};" +
                "map.on('click',function(e){if(marker)map.removeLayer(marker);marker=L.marker(e.latlng).addTo(map);var c=e.latlng.lat.toFixed(6)+', '+e.latlng.lng.toFixed(6);sendToJava(c);});" +
                "</script></body></html>";

        webView.getEngine().getLoadWorker().stateProperty().addListener((obs, old, state) -> {
            if (state == Worker.State.SUCCEEDED) {
                JSObject win = (JSObject) webView.getEngine().executeScript("window");
                win.setMember("javaApp", permanentBridge);

                // Sync textfield to map
                targetField.focusedProperty().addListener((o, oldV, newV) -> {
                    if (!newV) { // On blur
                        String text = targetField.getText();
                        if (!text.contains(",")) { // If it's a name, not coords
                            webView.getEngine().executeScript("searchAddress('" + text.replace("'", "\\'") + "')");
                        }
                    }
                });

                targetField.setOnAction(e -> { // On Enter key
                    String text = targetField.getText();
                    if (!text.contains(",")) {
                        webView.getEngine().executeScript("searchAddress('" + text.replace("'", "\\'") + "')");
                    }
                });
            }
        });
        webView.getEngine().loadContent(html);
    }

    // Bridge class for Java-JavaScript communication
    public class MapBridge {
        private final TextField target;
        public MapBridge(TextField t) { this.target = t; }

        public void updateCoords(String coords) {
            Platform.runLater(() -> {
                if (target != null) {
                    target.setText(coords);
                    target.setStyle("-fx-background-radius: 10; -fx-border-color: #2ecc71; -fx-border-width: 2; -fx-padding: 10;");
                }
            });
        }
    }

    private VBox createValidatedField(String label, String val, String type) {
        VBox v = new VBox(5);

        Label lbl = new Label(label);
        lbl.setStyle("-fx-font-weight: bold; -fx-text-fill: #555;");

        TextInputControl input = type.equals("AREA") ? new TextArea(val) : new TextField(val);
        input.setPromptText("Saisissez " + label.toLowerCase());
        input.setStyle("-fx-background-radius: 10; -fx-border-color: #ddd; -fx-padding: 10;");

        Label err = new Label();
        err.setStyle("-fx-text-fill: #e74c3c; -fx-font-size: 11px;");
        err.setVisible(false);

        input.textProperty().addListener((obs, o, n) -> {
            if (n.trim().isEmpty()) {
                input.setStyle("-fx-background-radius: 10; -fx-border-color: #e74c3c; -fx-border-width: 1.5; -fx-padding: 10;");
                err.setText("Obligatoire");
                err.setVisible(true);
            }
            else if (type.equals("INT") && !n.matches("\\d*")) {
                input.setStyle("-fx-background-radius: 10; -fx-border-color: #e74c3c; -fx-border-width: 1.5; -fx-padding: 10;");
                err.setText("Entier requis");
                err.setVisible(true);
            }
            else {
                input.setStyle("-fx-background-radius: 10; -fx-border-color: #2ecc71; -fx-border-width: 1.5; -fx-padding: 10;");
                err.setVisible(false);
            }
        });

        v.getChildren().addAll(lbl, input, err);
        return v;
    }

    private Label createIconLabel(String icon, String color, Runnable action) {
        Label l = new Label(icon);
        l.setStyle("-fx-text-fill: " + color + "; -fx-font-size: 18px; -fx-cursor: hand;");
        l.setOnMouseEntered(e -> { l.setScaleX(1.3); l.setScaleY(1.3); });
        l.setOnMouseExited(e -> { l.setScaleX(1.0); l.setScaleY(1.0); });
        if (action != null) l.setOnMouseClicked(e -> action.run());
        return l;
    }

    private void resetMainView() {
        if (mainContentVBox != null) {
            mainContentVBox.getChildren().clear();
            mainContentVBox.setPadding(new Insets(20));
            mainContentVBox.setAlignment(Pos.TOP_CENTER);
            mainContentVBox.setSpacing(20);
            mainContentVBox.setStyle("-fx-background-color: transparent;");
        }
    }

    private void showEventDetails(EvennementAgricole ev) {
        resetMainView();
        mainContentVBox.setPadding(new Insets(30, 60, 30, 60));
        mainContentVBox.setAlignment(Pos.TOP_LEFT);

        Button btnBack = new Button("← Retour");
        btnBack.setStyle("-fx-background-color: transparent; -fx-text-fill: #2d5a27; -fx-font-weight: bold; -fx-cursor: hand;");
        btnBack.setOnAction(e -> showGestionEvenements());

        HBox wrapper = new HBox(40);
        wrapper.setPadding(new Insets(20, 0, 0, 0));

        VBox left = new VBox(25);
        HBox.setHgrow(left, Priority.ALWAYS);

        Label title = new Label(ev.getTitre());
        title.setStyle("-fx-font-size: 40px; -fx-font-weight: bold; -fx-text-fill: #1a3c1a;");
        title.setWrapText(true);

        int remaining = getRemainingPlaces(ev);
        HBox metaRow = new HBox(20);

        Label dateLabel = new Label("📅 " + ev.getDateDebut().format(dtfDisplay) + " au " + ev.getDateFin().format(dtfDisplay));
        dateLabel.setStyle("-fx-text-fill: #333;");

        Label capLabel = new Label("🎟 " + remaining + " Places");
        capLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: " + (remaining < 5 ? "#e74c3c" : "#2d5a27") + ";");

        metaRow.getChildren().addAll(dateLabel, capLabel);

        Text desc = new Text(ev.getDescription() != null ? ev.getDescription() : "Aucune description");
        desc.setWrappingWidth(500);
        desc.setStyle("-fx-font-size: 15px; -fx-fill: #555;");

        left.getChildren().addAll(title, metaRow, new Separator(), desc);

        VBox right = new VBox(20);
        right.setMinWidth(300);
        right.setAlignment(Pos.TOP_CENTER);
        right.setPadding(new Insets(25));
        right.setStyle("-fx-background-color: #f9f9f9; -fx-background-radius: 25;");

        // Map for location display
        StackPane mapContainer = new StackPane();
        mapContainer.setPrefSize(220, 220);
        WebView staticMap = new WebView();
        staticMap.setPrefSize(220, 220);
        staticMap.setClip(new Circle(110, 110, 110));
        loadReadOnlyMap(staticMap, ev.getLieu());
        mapContainer.getChildren().add(staticMap);

        Button btnManage = new Button("Participants");
        btnManage.setMaxWidth(Double.MAX_VALUE);
        btnManage.setStyle("-fx-background-color: #2d5a27; -fx-text-fill: white; -fx-padding: 12; -fx-background-radius: 10; -fx-font-weight: bold; -fx-cursor: hand;");
        btnManage.setOnAction(a -> showParticipantsForEvent(ev));

        Label locHeader = new Label("Localisation");
        locHeader.setStyle("-fx-font-weight: bold; -fx-text-fill: #333;");

        Label locCoords = new Label(ev.getLieu() != null ? ev.getLieu() : "Non spécifié");
        locCoords.setStyle("-fx-text-fill: #666; -fx-wrap-text: true;");

        Label priceLabel = new Label("💰 " + ev.getFraisInscription() + " DT");
        priceLabel.setStyle("-fx-text-fill: #2d5a27; -fx-font-weight: bold;");

        right.getChildren().addAll(mapContainer, locHeader, locCoords, new Separator(), priceLabel, btnManage);
        wrapper.getChildren().addAll(left, right);
        mainContentVBox.getChildren().addAll(btnBack, wrapper);
    }

    private void loadReadOnlyMap(WebView webView, String coords) {
        String lat = "36.8065", lng = "10.1815";
        try {
            if (coords != null && coords.contains(",")) {
                String[] p = coords.split(",");
                lat = p[0].trim();
                lng = p[1].trim();
            }
        } catch (Exception e) {}

        String html = "<!DOCTYPE html><html><head>" +
                "<link rel=\"stylesheet\" href=\"https://unpkg.com/leaflet@1.9.4/dist/leaflet.css\" />" +
                "<script src=\"https://unpkg.com/leaflet@1.9.4/dist/leaflet.js\" ></script>" +
                "<style>body,html,#map{height:100%;margin:0;padding:0;background:#eee;}.leaflet-control-container{display:none;}</style>" +
                "</head><body><div id=\"map\"></div>" +
                "<script>var map = L.map('map',{dragging:false,zoomControl:false,scrollWheelZoom:false,doubleClickZoom:false}).setView([" + lat + "," + lng + "],13);" +
                "L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png').addTo(map);" +
                "L.marker([" + lat + "," + lng + "]).addTo(map);</script></body></html>";

        webView.getEngine().loadContent(html);
    }

    private void showParticipantsForEvent(EvennementAgricole ev) {
        resetMainView();
        mainContentVBox.setPadding(new Insets(30));
        mainContentVBox.setStyle("-fx-background-color: #f4f7f4;");

        Button btnBack = new Button("←");
        btnBack.setStyle("-fx-background-color: #2d5a27; -fx-text-fill: white; -fx-background-radius: 50; -fx-font-weight: bold; -fx-cursor: hand; -fx-min-width: 40; -fx-min-height: 40;");
        btnBack.setOnAction(a -> showGestionEvenements());

        Label t = new Label("Participants - " + ev.getTitre());
        t.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #1a3c1a; -fx-padding: 10 0;");

        FlowPane pFlow = new FlowPane(25, 25);
        pFlow.setPadding(new Insets(30));
        pFlow.setAlignment(Pos.CENTER);

        try {
            List<Participant> list = participantService.read().stream()
                    .filter(p -> p.getIdEvennement() == ev.getIdEvennement())
                    .collect(Collectors.toList());

            if (list.isEmpty()) {
                Label emptyLabel = new Label("Aucun participant");
                emptyLabel.setStyle("-fx-text-fill: #666; -fx-font-size: 16px; -fx-padding: 50;");
                pFlow.getChildren().add(emptyLabel);
            } else {
                for (Participant p : list) {
                    VBox bubble = new VBox(12);
                    bubble.setAlignment(Pos.CENTER);
                    bubble.setPadding(new Insets(20));
                    bubble.setPrefSize(180, 220);
                    bubble.setStyle("-fx-background-color: white; -fx-background-radius: 25; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 15, 0, 0, 8);");
                    bubble.setOnMouseEntered(e -> bubble.setScaleX(1.05));
                    bubble.setOnMouseExited(e -> bubble.setScaleX(1.0));

                    String iconType = (p.getNbrPlaces() > 1) ? "👥" : "👤";
                    Label avatar = new Label(iconType);
                    avatar.setStyle("-fx-font-size: 45px; -fx-text-fill: #2d5a27;");

                    String participantName = p.getNomParticipant();
                    if (participantName == null || participantName.isEmpty()) {
                        participantName = "Participant";
                    }

                    Label name = new Label(participantName);
                    name.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #333; -fx-wrap-text: true; -fx-alignment: center;");

                    Label places = new Label(p.getNbrPlaces() + (p.getNbrPlaces() > 1 ? " Places" : " Place"));
                    places.setStyle("-fx-background-color: #e8f5e9; -fx-text-fill: #2d5a27; -fx-font-size: 11px; -fx-font-weight: bold; -fx-padding: 4 12; -fx-background-radius: 15;");

                    bubble.getChildren().addAll(avatar, name, places);
                    pFlow.getChildren().add(bubble);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Erreur", "Impossible de charger les participants: " + e.getMessage());
        }

        mainContentVBox.getChildren().addAll(btnBack, t, pFlow);
    }

    private void showAuditLogs() {
        resetMainView();
        mainContentVBox.setPadding(new Insets(30, 80, 30, 80));
        mainContentVBox.setStyle("-fx-background-color: #f8faf8;");

        Button btnBack = new Button("← Retour");
        btnBack.setStyle("-fx-background-color: transparent; -fx-text-fill: #2d5a27; -fx-font-weight: bold; -fx-cursor: hand;");
        btnBack.setOnAction(e -> showGestionEvenements());

        Label head = new Label("Journal d'activités");
        head.setStyle("-fx-font-size: 28px; -fx-font-weight: bold; -fx-text-fill: #1a3c1a; -fx-padding: 10 0 20 0;");

        VBox logList = new VBox(10);

        try {
            List<ActionLog> logs = logService.readAll();

            if (logs.isEmpty()) {
                Label emptyLogs = new Label("Aucun log trouvé");
                emptyLogs.setStyle("-fx-text-fill: #666; -fx-font-size: 16px; -fx-padding: 50;");
                logList.getChildren().add(emptyLogs);
            } else {
                for (ActionLog log : logs) {
                    HBox row = new HBox(20);
                    row.setAlignment(Pos.CENTER_LEFT);
                    row.setPadding(new Insets(15, 25, 15, 25));
                    row.setStyle("-fx-background-color: white; -fx-background-radius: 12; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.05), 5, 0, 0, 2);");

                    Label typeBadge = new Label(log.getActionType());
                    String color = log.getActionType().equals("DELETE") ? "#e74c3c" :
                            (log.getActionType().equals("CREATE") ? "#27ae60" : "#3498db");
                    typeBadge.setStyle("-fx-background-color: " + color + "; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 5 12; -fx-background-radius: 8; -fx-min-width: 80; -fx-alignment: center;");

                    Label desc = new Label(log.getDescription());
                    desc.setStyle("-fx-font-size: 14px; -fx-text-fill: #333;");

                    Region spacer = new Region();
                    HBox.setHgrow(spacer, Priority.ALWAYS);

                    Label time = new Label(log.getCreatedAt() != null ? log.getCreatedAt() : "");
                    time.setStyle("-fx-text-fill: #666; -fx-font-size: 12px;");

                    row.getChildren().addAll(typeBadge, desc, spacer, time);
                    logList.getChildren().add(row);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Erreur", "Impossible de charger les logs: " + e.getMessage());
        }

        ScrollPane scrollPane = new ScrollPane(logList);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent; -fx-border-color: transparent;");
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        mainContentVBox.getChildren().addAll(btnBack, head, scrollPane);
    }

    private void showAlert(String title, String content) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(content);
            alert.showAndWait();
        });
    }
}