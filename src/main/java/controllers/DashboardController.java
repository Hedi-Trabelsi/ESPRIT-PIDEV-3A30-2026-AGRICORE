package controllers;

import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import models.EvennementAgricole;
import models.Participant;
import services.EvennementService;
import services.ParticipantService;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class DashboardController {

    @FXML
    private VBox mainContentVBox;

    private final EvennementService evennementService = new EvennementService();
    private final ParticipantService participantService = new ParticipantService();

    @FXML
    public void initialize() {
        showGestionEvenements();
    }

    // ===================== 1. LIST VIEW =====================
    @FXML
    private void showGestionEvenements() {
        mainContentVBox.getChildren().clear();
        mainContentVBox.setAlignment(Pos.TOP_CENTER);

        HBox actionBar = new HBox();
        actionBar.setAlignment(Pos.CENTER_LEFT);
        actionBar.setPadding(new Insets(20, 40, 20, 40));

        Label header = new Label("AgriCore Management");
        header.setFont(Font.font("System", FontWeight.BOLD, 30));
        header.setStyle("-fx-text-fill: #1a3c1a;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button btnAdd = new Button("+ Nouvel Événement");
        btnAdd.setStyle("-fx-background-color: #2d5a27; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 20; -fx-padding: 10 25; -fx-cursor: hand;");
        btnAdd.setOnAction(e -> renderForm(null));

        actionBar.getChildren().addAll(header, spacer, btnAdd);
        mainContentVBox.getChildren().add(actionBar);

        FlowPane flowPane = new FlowPane(20, 25);
        flowPane.setAlignment(Pos.CENTER);
        flowPane.setPrefWrapLength(1000);

        try {
            List<EvennementAgricole> events = evennementService.read();
            for (EvennementAgricole ev : events) {
                flowPane.getChildren().add(createEventCard(ev));
            }
        } catch (SQLException e) { e.printStackTrace(); }

        mainContentVBox.getChildren().add(flowPane);
    }

    private VBox createEventCard(EvennementAgricole ev) {
        VBox card = new VBox(8);
        card.setPrefWidth(260);
        card.setPadding(new Insets(15));
        card.setAlignment(Pos.TOP_CENTER);

        LocalDate today = LocalDate.now();
        String statusText = "";
        String cardBaseStyle = "-fx-background-radius: 15; -fx-border-radius: 15; -fx-border-width: 1.5; ";
        String statusColor = "#777", bgColor = "white", borderColor = "transparent";

        if (ev.getDateFin() != null && ev.getDateFin().isBefore(today)) {
            statusText = "● TERMINÉ"; statusColor = "#e74c3c"; bgColor = "#fff5f5"; borderColor = "#ffcdd2";
        } else if ((ev.getDateDebut().isBefore(today) || ev.getDateDebut().isEqual(today)) &&
                (ev.getDateFin() == null || ev.getDateFin().isAfter(today) || ev.getDateFin().isEqual(today))) {
            statusText = "● EN COURS"; statusColor = "#27ae60"; bgColor = "#f0fff4"; borderColor = "#c6f6d5";
        }

        card.setStyle(cardBaseStyle + "-fx-background-color: " + bgColor + "; -fx-border-color: " + borderColor + ";");
        DropShadow shadow = new DropShadow(10, Color.rgb(0,0,0,0.08));
        card.setEffect(shadow);

        // --- INTERACTIVE CARD ZOOM ---
        card.setOnMouseEntered(e -> {
            card.setScaleX(1.03); card.setScaleY(1.03);
            shadow.setRadius(20); shadow.setColor(Color.rgb(0,0,0,0.12));
        });
        card.setOnMouseExited(e -> {
            card.setScaleX(1.0); card.setScaleY(1.0);
            shadow.setRadius(10); shadow.setColor(Color.rgb(0,0,0,0.08));
        });

        Label statusBadge = new Label(statusText);
        statusBadge.setStyle("-fx-text-fill: " + statusColor + "; -fx-font-weight: bold; -fx-font-size: 10px;");
        statusBadge.setMaxWidth(Double.MAX_VALUE);
        statusBadge.setAlignment(Pos.CENTER_RIGHT);

        HBox icons = new HBox(15,
                createIconLabel("👥", "#3498db", () -> showParticipantsForEvent(ev)),
                createIconLabel("🗑", "#e74c3c", () -> handleDeleteEvent(ev))
        );
        icons.setAlignment(Pos.CENTER_RIGHT);

        Label title = new Label(ev.getTitre());
        title.setFont(Font.font("System", FontWeight.BOLD, 17));
        title.setStyle("-fx-text-fill: #1a3c1a;");
        title.setWrapText(true); title.setMinHeight(45); title.setAlignment(Pos.CENTER);

        String dRange = ev.getDateDebut().format(DateTimeFormatter.ofPattern("dd MMM"));
        if(ev.getDateFin() != null) dRange += " - " + ev.getDateFin().format(DateTimeFormatter.ofPattern("dd MMM"));
        Label date = new Label("📅 " + dRange);
        date.setStyle("-fx-text-fill: #555; -fx-font-size: 13px;");

        Button btnEdit = new Button("Modifier");
        btnEdit.setMaxWidth(Double.MAX_VALUE);
        String btnBase = "-fx-background-radius: 8; -fx-border-radius: 8; -fx-border-width: 1; -fx-cursor: hand; -fx-font-weight: bold; ";
        String btnIdle = btnBase + "-fx-background-color: transparent; -fx-text-fill: #2d5a27; -fx-border-color: #2d5a27;";
        String btnHover = btnBase + "-fx-background-color: #2d5a27; -fx-text-fill: white; -fx-border-color: #2d5a27;";
        btnEdit.setStyle(btnIdle);
        btnEdit.setOnMouseEntered(e -> btnEdit.setStyle(btnHover));
        btnEdit.setOnMouseExited(e -> btnEdit.setStyle(btnIdle));
        btnEdit.setOnAction(e -> renderForm(ev));

        card.getChildren().addAll(statusBadge, icons, title, date, new Region(), btnEdit);
        VBox.setVgrow(card.getChildren().get(4), Priority.ALWAYS);
        return card;
    }

    // ===================== 2. INTERACTIVE STUDIO FORM =====================
    private void renderForm(EvennementAgricole targetEv) {
        mainContentVBox.getChildren().clear();
        boolean isEdit = (targetEv != null);

        HBox splitLayout = new HBox(40);
        splitLayout.setAlignment(Pos.TOP_CENTER);
        splitLayout.setPadding(new Insets(30));

        VBox formCard = new VBox(12);
        formCard.setPadding(new Insets(25));
        formCard.setMinWidth(460);
        formCard.setStyle("-fx-background-color: white; -fx-background-radius: 20;");
        formCard.setEffect(new DropShadow(15, Color.rgb(0,0,0,0.1)));

        Label head = new Label(isEdit ? "Mise à jour" : "Nouvel Événement");
        head.setFont(Font.font("System", FontWeight.BOLD, 24));

        VBox tBox = createValidatedField("Titre", isEdit ? targetEv.getTitre() : "", "TEXT");
        VBox lBox = createValidatedField("Lieu", isEdit ? targetEv.getLieu() : "", "TEXT");
        VBox descBox = createValidatedField("Description", isEdit ? targetEv.getDescription() : "", "AREA");

        HBox dateRow = new HBox(15);
        VBox startBox = createDatePickerBox("Date Début", isEdit ? targetEv.getDateDebut() : LocalDate.now());
        VBox endBox = createDatePickerBox("Date Fin", isEdit ? targetEv.getDateFin() : LocalDate.now().plusDays(1));
        HBox.setHgrow(startBox, Priority.ALWAYS); HBox.setHgrow(endBox, Priority.ALWAYS);
        dateRow.getChildren().addAll(startBox, endBox);

        HBox numRow = new HBox(15);
        VBox cBox = createValidatedField("Capacité", isEdit ? String.valueOf(targetEv.getCapaciteMax()) : "", "NUMBER");
        VBox fBox = createValidatedField("Frais (DT)", isEdit ? String.valueOf(targetEv.getFraisInscription()) : "", "NUMBER");
        HBox.setHgrow(cBox, Priority.ALWAYS); HBox.setHgrow(fBox, Priority.ALWAYS);
        numRow.getChildren().addAll(cBox, fBox);

        VBox previewPane = new VBox(15);
        previewPane.setAlignment(Pos.TOP_CENTER);
        previewPane.setPadding(new Insets(50, 0, 0, 0));
        VBox liveCard = createPreviewCard(isEdit ? targetEv.getTitre() : "Titre...", isEdit ? targetEv.getDateDebut() : LocalDate.now());
        previewPane.getChildren().addAll(new Label("APERÇU EN DIRECT"), liveCard);

        TextField titleField = (TextField) tBox.getChildren().get(1);
        titleField.textProperty().addListener((obs, old, newVal) -> ((Label) liveCard.getChildren().get(1)).setText(newVal.isEmpty() ? "Titre..." : newVal));

        Button btnSave = new Button(isEdit ? "Enregistrer" : "Créer l'Événement");
        btnSave.setMaxWidth(Double.MAX_VALUE);
        btnSave.setStyle("-fx-background-color: #2d5a27; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 15; -fx-background-radius: 12; -fx-cursor: hand;");

        btnSave.setOnAction(e -> {
            try {
                LocalDate s = ((DatePicker)startBox.getChildren().get(1)).getValue();
                LocalDate fn = ((DatePicker)endBox.getChildren().get(1)).getValue();
                if (fn.isBefore(s)) { showAlert("Erreur", "Dates invalides."); return; }

                EvennementAgricole ev = isEdit ? targetEv : new EvennementAgricole();
                ev.setTitre(titleField.getText().trim());
                ev.setLieu(((TextField) lBox.getChildren().get(1)).getText().trim());
                ev.setDescription(((TextArea) descBox.getChildren().get(1)).getText().trim());
                ev.setCapaciteMax(Integer.parseInt(((TextField) cBox.getChildren().get(1)).getText().replaceAll("[^0-9]", "")));
                ev.setFraisInscription(Integer.parseInt(((TextField) fBox.getChildren().get(1)).getText().replaceAll("[^0-9]", "")));
                ev.setDateDebut(s); ev.setDateFin(fn);
                ev.setStatut("Actif");

                if (isEdit) evennementService.update(ev);
                else evennementService.create(ev);
                showGestionEvenements();
            } catch (Exception ex) { showAlert("Champs Invalides", "Vérifiez vos données."); }
        });

        Button btnBack = new Button("Annuler");
        btnBack.setOnAction(e -> showGestionEvenements());
        btnBack.setStyle("-fx-background-color: transparent; -fx-text-fill: #999;");

        formCard.getChildren().addAll(head, tBox, lBox, descBox, dateRow, numRow, new Separator(), btnSave, btnBack);
        splitLayout.getChildren().addAll(formCard, previewPane);

        ScrollPane sp = new ScrollPane(splitLayout);
        sp.setFitToWidth(true);
        sp.setStyle("-fx-background-color: transparent; -fx-background: #f8fcf8; -fx-border-color: transparent;");
        mainContentVBox.getChildren().add(sp);
    }

    // ===================== HELPERS =====================
    private VBox createValidatedField(String label, String val, String type) {
        VBox v = new VBox(5);
        Label l = new Label(label);
        l.setStyle("-fx-font-weight: bold; -fx-text-fill: #555;");
        TextInputControl input = type.equals("AREA") ? new TextArea(val) : new TextField(val);
        input.setPromptText("Saisissez " + label.toLowerCase());
        input.setStyle("-fx-background-radius: 10; -fx-border-color: #ddd; -fx-padding: 10;");

        input.textProperty().addListener((obs, old, newVal) -> {
            boolean isInvalid = newVal.trim().isEmpty() || (type.equals("NUMBER") && !newVal.matches("\\d+"));
            input.setStyle("-fx-background-radius: 10; -fx-border-color: " + (isInvalid ? "#e74c3c" : "#2ecc71") + "; -fx-border-width: 1.5;");
        });
        v.getChildren().addAll(l, input);
        return v;
    }

    private VBox createDatePickerBox(String label, LocalDate date) {
        VBox v = new VBox(5);
        v.getChildren().addAll(new Label(label), new DatePicker(date));
        ((Label)v.getChildren().get(0)).setStyle("-fx-font-weight: bold;");
        ((DatePicker)v.getChildren().get(1)).setMaxWidth(Double.MAX_VALUE);
        return v;
    }

    private VBox createPreviewCard(String title, LocalDate date) {
        VBox card = new VBox(15);
        card.setAlignment(Pos.CENTER);
        card.setPrefSize(250, 200);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 20; -fx-border-color: #2d5a27; -fx-border-style: dashed; -fx-border-width: 2;");
        Label icon = new Label("🌱"); icon.setFont(Font.font(30));
        Label lblT = new Label(title); lblT.setFont(Font.font("System", FontWeight.BOLD, 15)); lblT.setWrapText(true);
        Label lblD = new Label("📅 " + date.format(DateTimeFormatter.ofPattern("dd MMM"))); lblD.setStyle("-fx-text-fill: #7ca76f;");
        card.getChildren().addAll(icon, lblT, lblD);
        return card;
    }

    // --- INTERACTIVE ICON HELPER ---
    private Label createIconLabel(String icon, String color, Runnable action) {
        Label l = new Label(icon);
        l.setStyle("-fx-text-fill: " + color + "; -fx-font-size: 18px; -fx-cursor: hand;");

        l.setOnMouseEntered(e -> { l.setScaleX(1.3); l.setScaleY(1.3); });
        l.setOnMouseExited(e -> { l.setScaleX(1.0); l.setScaleY(1.0); });

        l.setOnMouseClicked(e -> action.run());
        return l;
    }

    private void showParticipantsForEvent(EvennementAgricole ev) {
        mainContentVBox.getChildren().clear();
        Button b = new Button("← Retour"); b.setOnAction(e -> showGestionEvenements());
        mainContentVBox.getChildren().addAll(b, new Label("Participants : " + ev.getTitre()));
    }

    private void handleDeleteEvent(EvennementAgricole ev) {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION, "Supprimer ?", ButtonType.YES, ButtonType.NO);
        a.showAndWait().ifPresent(r -> {
            if (r == ButtonType.YES) {
                try {
                    evennementService.delete(ev.getIdEvennement());
                    showGestionEvenements();
                } catch (SQLException ex) { ex.printStackTrace(); }
            }
        });
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}