package controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import models.Depense;
import models.User;
import models.Vente;
import services.DepenseService;
import services.VenteService;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class UserCalendarController {

    // ── FXML injections — match fx:id in UserCalendar.fxml ──────
    @FXML private Label     titleLabel;
    @FXML private Label     subtitleLabel;
    @FXML private Label     monthLabel;
    @FXML private GridPane  calendarGrid;

    // Sidebar today card
    @FXML private Label     todayLabel;
    @FXML private Label     todayDayLabel;
    @FXML private Label     todayMonthLabel;

    // ── Services & state ────────────────────────────────────────
    private final DepenseService depenseService = new DepenseService();
    private final VenteService   venteService   = new VenteService();

    private User          user;
    private YearMonth     current;
    private List<Depense> depenses = Collections.emptyList();
    private List<Vente>   ventes   = Collections.emptyList();

    // ────────────────────────────────────────────────────────────
    //  Entry point — call this right after loading the FXML
    // ────────────────────────────────────────────────────────────
    public void setUser(User user) {
        this.user = user;

        // Page title
        if (titleLabel != null && user != null) {
            titleLabel.setText("Calendrier: "
                    + user.getFirstName() + " " + user.getLastName());
        }

        // Sidebar today card
        updateSidebarToday();

        current = YearMonth.now();
        loadData();
        renderMonth();
    }

    // ────────────────────────────────────────────────────────────
    //  Month navigation
    // ────────────────────────────────────────────────────────────
    @FXML
    void prevMonth() {
        current = current.minusMonths(1);
        loadData();
        renderMonth();
    }

    @FXML
    void nextMonth() {
        current = current.plusMonths(1);
        loadData();
        renderMonth();
    }

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

    // ────────────────────────────────────────────────────────────
    //  Sidebar — show real today's date
    // ────────────────────────────────────────────────────────────
    private void updateSidebarToday() {
        LocalDate today = LocalDate.now();
        if (todayLabel != null)
            todayLabel.setText(String.valueOf(today.getDayOfMonth()));
        if (todayDayLabel != null)
            todayDayLabel.setText(
                    today.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.FRENCH));
        if (todayMonthLabel != null)
            todayMonthLabel.setText(
                    today.getMonth().getDisplayName(TextStyle.FULL, Locale.FRENCH)
                            + " " + today.getYear());
    }

    // ────────────────────────────────────────────────────────────
    //  Load depenses + ventes for the current user
    // ────────────────────────────────────────────────────────────
    private void loadData() {
        try {
            depenses = depenseService.readByUser(user.getId());
            ventes   = venteService.readByUser(user.getId());
        } catch (Exception e) {
            depenses = Collections.emptyList();
            ventes   = Collections.emptyList();
        }
    }

    // ────────────────────────────────────────────────────────────
    //  Render the full 6×7 calendar grid
    // ────────────────────────────────────────────────────────────
    private void renderMonth() {
        if (calendarGrid == null) return;
        calendarGrid.getChildren().clear();

        // Month label in topbar  e.g. "FÉVRIER 2026"
        monthLabel.setText(
                current.getMonth()
                        .getDisplayName(TextStyle.FULL, Locale.FRENCH)
                        .toUpperCase()
                        + " " + current.getYear()
        );

        LocalDate today       = LocalDate.now();
        LocalDate firstDay    = current.atDay(1);
        int       startCol    = dayOfWeekToCol(firstDay.getDayOfWeek()); // 0=Mon
        int       daysInMonth = current.lengthOfMonth();

        // ── 1. Fill blank cells before the 1st (prev-month padding) ──
        for (int c = 0; c < startCol; c++) {
            addFillerCell(c, 0);
        }

        // ── 2. Fill actual days ──
        int row = 0, col = startCol;
        for (int d = 1; d <= daysInMonth; d++) {
            LocalDate date = current.atDay(d);
            addDayCell(date, today, col, row);
            col++;
            if (col > 6) { col = 0; row++; }
        }

        // ── 3. Fill blank cells after the last day (next-month padding) ──
        while (row < 6) {
            addFillerCell(col, row);
            col++;
            if (col > 6) { col = 0; row++; }
        }
    }

    // ────────────────────────────────────────────────────────────
    //  Add a real day cell at (col, row)
    // ────────────────────────────────────────────────────────────
    private void addDayCell(LocalDate date, LocalDate today, int col, int row) {
        boolean isToday   = date.equals(today);
        boolean isWeekend = date.getDayOfWeek() == DayOfWeek.SATURDAY
                || date.getDayOfWeek() == DayOfWeek.SUNDAY;

        // Day number
        Label dayNum = new Label(String.valueOf(date.getDayOfMonth()));
        if (isToday)
            dayNum.getStyleClass().add("cell-day-num-today");
        else if (isWeekend)
            dayNum.getStyleClass().add("cell-day-num-weekend");
        else
            dayNum.getStyleClass().add("cell-day-num");

        // Totals for this date
        double depTotal = depenses.stream()
                .filter(x -> date.equals(x.getDate()))
                .mapToDouble(Depense::getMontant).sum();
        double venTotal = ventes.stream()
                .filter(x -> date.equals(x.getDate()))
                .mapToDouble(Vente::getChiffreAffaires).sum();

        // Cell VBox
        VBox cell = new VBox(4.0);
        cell.setPadding(new Insets(5, 7, 4, 7));
        cell.setAlignment(Pos.TOP_LEFT);
        cell.setMaxWidth(Double.MAX_VALUE);
        cell.setMaxHeight(Double.MAX_VALUE);

        // CSS style classes
        cell.getStyleClass().add("day-cell");
        if (isToday)        cell.getStyleClass().add("day-cell-today");
        else if (isWeekend) cell.getStyleClass().add("day-cell-weekend");

        cell.getChildren().add(dayNum);

        // Dépense pill (red tint)
        if (depTotal > 0) {
            Label dep = new Label("◆ Dép: " + String.format("%.2f", depTotal));
            dep.getStyleClass().addAll("event-pill", "event-pill-red");
            dep.setMaxWidth(Double.MAX_VALUE);
            cell.getChildren().add(dep);
        }

        // Vente pill (green tint)
        if (venTotal > 0) {
            Label ven = new Label("◆ Ven: " + String.format("%.2f", venTotal));
            ven.getStyleClass().addAll("event-pill", "event-pill-green");
            ven.setMaxWidth(Double.MAX_VALUE);
            cell.getChildren().add(ven);
        }

        // Double-click → detail popup
        cell.setOnMouseClicked(e -> {
            if (e.getClickCount() >= 2) openDayDetails(date);
        });

        // ── CRITICAL: stretch cell to fill grid slot ──
        GridPane.setFillWidth(cell,  true);
        GridPane.setFillHeight(cell, true);
        VBox.setVgrow(cell, Priority.ALWAYS);

        calendarGrid.add(cell, col, row);
    }

    // ────────────────────────────────────────────────────────────
    //  Add an empty filler cell (prev/next-month padding)
    // ────────────────────────────────────────────────────────────
    private void addFillerCell(int col, int row) {
        VBox cell = new VBox();
        cell.setMaxWidth(Double.MAX_VALUE);
        cell.setMaxHeight(Double.MAX_VALUE);
        cell.getStyleClass().addAll("day-cell", "day-cell-other");
        GridPane.setFillWidth(cell,  true);
        GridPane.setFillHeight(cell, true);
        calendarGrid.add(cell, col, row);
    }

    // ────────────────────────────────────────────────────────────
    //  DayOfWeek → column index  (Monday = 0, Sunday = 6)
    // ────────────────────────────────────────────────────────────
    private int dayOfWeekToCol(DayOfWeek dow) {
        switch (dow) {
            case MONDAY:    return 0;
            case TUESDAY:   return 1;
            case WEDNESDAY: return 2;
            case THURSDAY:  return 3;
            case FRIDAY:    return 4;
            case SATURDAY:  return 5;
            default:        return 6; // SUNDAY
        }
    }

    // ────────────────────────────────────────────────────────────
    //  Detail popup on double-click — custom modern Stage
    // ────────────────────────────────────────────────────────────
    private void openDayDetails(LocalDate date) {
        List<Depense> ds = depenses.stream()
                .filter(x -> date.equals(x.getDate())).toList();
        List<Vente> vs = ventes.stream()
                .filter(x -> date.equals(x.getDate())).toList();

        // ── Formatted date string ──
        String dateStr = date.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.FRENCH)
                + " " + date.getDayOfMonth() + " "
                + date.getMonth().getDisplayName(TextStyle.FULL, Locale.FRENCH)
                + " " + date.getYear();

        // ══════════════════════════════════════
        //  HEADER — dark green gradient banner
        // ══════════════════════════════════════
        Label dayNumLbl = new Label(String.valueOf(date.getDayOfMonth()));
        dayNumLbl.setStyle(
                "-fx-font-family: 'Georgia';" +
                        "-fx-font-size: 42px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-text-fill: #3ecf77;"
        );

        Label dayNameLbl = new Label(
                date.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.FRENCH).toUpperCase()
        );
        dayNameLbl.setStyle(
                "-fx-font-size: 11px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-text-fill: rgba(255,255,255,0.60);" +
                        "-fx-letter-spacing: 2px;"
        );

        Label monthYearLbl = new Label(
                date.getMonth().getDisplayName(TextStyle.FULL, Locale.FRENCH) + " " + date.getYear()
        );
        monthYearLbl.setStyle(
                "-fx-font-family: 'Georgia';" +
                        "-fx-font-size: 14px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-text-fill: #ffffff;"
        );

        VBox dateBlock = new VBox(2, dayNameLbl, monthYearLbl);
        dateBlock.setAlignment(Pos.CENTER_LEFT);
        dateBlock.setPadding(new Insets(0, 0, 0, 16));

        // Close (×) button top-right
        Button closeBtn = new Button("✕");
        closeBtn.setStyle(
                "-fx-background-color: transparent;" +
                        "-fx-text-fill: rgba(255,255,255,0.55);" +
                        "-fx-font-size: 14px;" +
                        "-fx-cursor: hand;" +
                        "-fx-border-width: 0;" +
                        "-fx-padding: 4px 8px;"
        );

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox headerTop = new HBox(dayNumLbl, dateBlock, spacer, closeBtn);
        headerTop.setAlignment(Pos.CENTER_LEFT);
        headerTop.setPadding(new Insets(20, 20, 16, 20));
        headerTop.setStyle(
                "-fx-background-color: linear-gradient(to bottom right, #0b3320, #1d7a44);" +
                        "-fx-background-radius: 14px 14px 0px 0px;"
        );

        // ══════════════════════════════════════
        //  SUMMARY BAR — totals at a glance
        // ══════════════════════════════════════
        double totalDep = ds.stream().mapToDouble(Depense::getMontant).sum();
        double totalVen = vs.stream().mapToDouble(Vente::getChiffreAffaires).sum();

        VBox depSummary = makeSummaryCard(
                "Dépenses", String.format("%.2f DT", totalDep),
                "#fde8e8", "#7a1a1a", "#f5c0c0"
        );
        VBox venSummary = makeSummaryCard(
                "Ventes", String.format("%.2f DT", totalVen),
                "#d0f0de", "#0b3320", "#a8dfc0"
        );

        HBox summaryBar = new HBox(12, depSummary, venSummary);
        summaryBar.setPadding(new Insets(16, 20, 8, 20));
        depSummary.setMaxWidth(Double.MAX_VALUE);
        venSummary.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(depSummary, Priority.ALWAYS);
        HBox.setHgrow(venSummary, Priority.ALWAYS);

        // ══════════════════════════════════════
        //  TRANSACTION LIST
        // ══════════════════════════════════════
        VBox listContent = new VBox(6);
        listContent.setPadding(new Insets(0, 20, 16, 20));

        if (ds.isEmpty() && vs.isEmpty()) {
            Label empty = new Label("Aucune transaction ce jour.");
            empty.setStyle(
                    "-fx-text-fill: #7a9688;" +
                            "-fx-font-size: 12px;" +
                            "-fx-padding: 12px 0;"
            );
            listContent.getChildren().add(empty);
        } else {
            // ── Dépenses section ──
            if (!ds.isEmpty()) {
                listContent.getChildren().add(makeSectionLabel("DÉPENSES", "#c8444a"));
                for (Depense d : ds) {
                    String type = d.getType() != null ? d.getType().name() : "AUTRE";
                    listContent.getChildren().add(
                            makeTransactionRow("◆", type,
                                    String.format("%.2f DT", d.getMontant()),
                                    "#fde8e8", "#c8444a", "#7a1a1a")
                    );
                }
            }
            // ── Spacing between sections ──
            if (!ds.isEmpty() && !vs.isEmpty()) {
                Region sep = new Region();
                sep.setPrefHeight(8);
                listContent.getChildren().add(sep);
            }
            // ── Ventes section ──
            if (!vs.isEmpty()) {
                listContent.getChildren().add(makeSectionLabel("VENTES", "#1d7a44"));
                for (Vente v : vs) {
                    String prod = v.getProduit() != null ? v.getProduit() : "—";
                    listContent.getChildren().add(
                            makeTransactionRow("◆", prod,
                                    String.format("%.2f DT", v.getChiffreAffaires()),
                                    "#d0f0de", "#22a659", "#0b3320")
                    );
                }
            }
        }

        ScrollPane listScroll = new ScrollPane(listContent);
        listScroll.setFitToWidth(true);
        listScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        listScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        listScroll.setMaxHeight(280);
        listScroll.setStyle("-fx-background-color: transparent; -fx-background: transparent; -fx-border-color: transparent;");

        // ══════════════════════════════════════
        //  FOOTER — close button
        // ══════════════════════════════════════
        Button footerClose = new Button("Fermer");
        footerClose.setStyle(
                "-fx-background-color: linear-gradient(to bottom, #3ecf77, #22a659);" +
                        "-fx-text-fill: white;" +
                        "-fx-font-weight: bold;" +
                        "-fx-font-size: 12px;" +
                        "-fx-background-radius: 8px;" +
                        "-fx-padding: 8px 28px;" +
                        "-fx-cursor: hand;" +
                        "-fx-border-width: 0;" +
                        "-fx-effect: dropshadow(gaussian, rgba(34,166,89,0.40), 8, 0.1, 0, 2);"
        );

        HBox footer = new HBox(footerClose);
        footer.setAlignment(Pos.CENTER_RIGHT);
        footer.setPadding(new Insets(8, 20, 18, 20));
        footer.setStyle("-fx-border-color: #e0f2e9 transparent transparent transparent; -fx-border-width: 1px 0 0 0;");

        // ══════════════════════════════════════
        //  ASSEMBLE ROOT
        // ══════════════════════════════════════
        VBox root = new VBox(0, headerTop, summaryBar, listScroll, footer);
        root.setStyle(
                "-fx-background-color: #ffffff;" +
                        "-fx-background-radius: 14px;" +
                        "-fx-effect: dropshadow(gaussian, rgba(10,46,26,0.30), 28, 0.15, 0, 6);"
        );
        root.setPrefWidth(420);

        // ══════════════════════════════════════
        //  STAGE
        // ══════════════════════════════════════
        Stage popup = new Stage();
        popup.initModality(Modality.APPLICATION_MODAL);
        popup.initStyle(StageStyle.TRANSPARENT);
        popup.setTitle(dateStr);

        // Owner window so popup centres on calendar
        if (calendarGrid.getScene() != null && calendarGrid.getScene().getWindow() != null) {
            popup.initOwner(calendarGrid.getScene().getWindow());
        }

        Scene scene = new Scene(root);
        scene.setFill(null); // transparent background for rounded corners
        scene.getStylesheets().add(
                getClass().getResource("/calender.css").toExternalForm()
        );
        popup.setScene(scene);

        // Wire close buttons
        closeBtn.setOnAction(e -> popup.close());
        footerClose.setOnAction(e -> popup.close());

        // Hover effects on footer button
        footerClose.setOnMouseEntered(e -> footerClose.setStyle(
                "-fx-background-color: linear-gradient(to bottom, #52e08b, #2ebd6a);" +
                        "-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 12px;" +
                        "-fx-background-radius: 8px; -fx-padding: 8px 28px; -fx-cursor: hand;" +
                        "-fx-border-width: 0; -fx-effect: dropshadow(gaussian, rgba(34,166,89,0.55), 12, 0.2, 0, 3);"
        ));
        footerClose.setOnMouseExited(e -> footerClose.setStyle(
                "-fx-background-color: linear-gradient(to bottom, #3ecf77, #22a659);" +
                        "-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 12px;" +
                        "-fx-background-radius: 8px; -fx-padding: 8px 28px; -fx-cursor: hand;" +
                        "-fx-border-width: 0; -fx-effect: dropshadow(gaussian, rgba(34,166,89,0.40), 8, 0.1, 0, 2);"
        ));

        popup.showAndWait();
    }

    // ── Helper: summary card (total at top of popup) ────────────
    private VBox makeSummaryCard(String label, String value,
                                 String bg, String textColor, String borderColor) {
        Label lbl = new Label(label);
        lbl.setStyle(
                "-fx-font-size: 10px; -fx-font-weight: bold;" +
                        "-fx-text-fill: " + textColor + "; -fx-letter-spacing: 1px;"
        );
        Label val = new Label(value);
        val.setStyle(
                "-fx-font-family: 'Georgia'; -fx-font-size: 16px; -fx-font-weight: bold;" +
                        "-fx-text-fill: " + textColor + ";"
        );
        VBox card = new VBox(2, lbl, val);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(10, 14, 10, 14));
        card.setStyle(
                "-fx-background-color: " + bg + ";" +
                        "-fx-background-radius: 10px;" +
                        "-fx-border-color: " + borderColor + ";" +
                        "-fx-border-radius: 10px;" +
                        "-fx-border-width: 1px;"
        );
        return card;
    }

    // ── Helper: section label ("DÉPENSES" / "VENTES") ───────────
    private Label makeSectionLabel(String text, String color) {
        Label lbl = new Label(text);
        lbl.setStyle(
                "-fx-font-size: 10px; -fx-font-weight: bold;" +
                        "-fx-text-fill: " + color + "; -fx-letter-spacing: 1.5px;" +
                        "-fx-padding: 4px 0 2px 0;"
        );
        return lbl;
    }

    // ── Helper: single transaction row ──────────────────────────
    private HBox makeTransactionRow(String icon, String label, String amount,
                                    String bg, String accentColor, String textColor) {
        Label iconLbl = new Label(icon);
        iconLbl.setStyle("-fx-text-fill: " + accentColor + "; -fx-font-size: 9px;");
        iconLbl.setMinWidth(14);

        Label nameLbl = new Label(label);
        nameLbl.setStyle(
                "-fx-font-size: 12px; -fx-text-fill: " + textColor + ";"
        );
        nameLbl.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(nameLbl, Priority.ALWAYS);

        Label amtLbl = new Label(amount);
        amtLbl.setStyle(
                "-fx-font-family: 'Georgia'; -fx-font-size: 12px;" +
                        "-fx-font-weight: bold; -fx-text-fill: " + textColor + ";"
        );

        HBox row = new HBox(8, iconLbl, nameLbl, amtLbl);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(7, 12, 7, 12));
        row.setStyle(
                "-fx-background-color: " + bg + ";" +
                        "-fx-background-radius: 8px;"
        );
        return row;
    }
}