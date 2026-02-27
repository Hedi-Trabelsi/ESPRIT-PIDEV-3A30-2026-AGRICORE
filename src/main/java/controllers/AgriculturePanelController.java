package controllers;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import services.AgricultureService;
import services.AgricultureService.IndicatorResult;

import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Builds and manages the 🌍 Agriculture panel cards.
 * Call  attach(GridPane agriGrid)  after the FXML is loaded,
 * and it will fetch data on a background thread then populate the grid.
 */
public class AgriculturePanelController {

    private final AgricultureService service = new AgricultureService();
    private GridPane grid;

    // ── Attach to the FXML GridPane and start loading ────────────
    public void attach(GridPane agriGrid) {
        this.grid = agriGrid;
        showLoading();
        fetchAsync();
    }

    // ── Show a loading indicator while waiting for the API ───────
    private void showLoading() {
        Label lbl = new Label("⏳  Chargement des données World Bank…");
        lbl.getStyleClass().add("agri-loading");
        grid.getChildren().clear();
        grid.add(lbl, 0, 0, 2, 1); // span 2 columns
    }

    // ── Fetch on background thread, then update UI on FX thread ──
    private void fetchAsync() {
        ExecutorService exec = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "agri-fetch");
            t.setDaemon(true);
            return t;
        });

        exec.submit(() -> {
            Map<String, IndicatorResult> data = service.fetchIndicators();
            Platform.runLater(() -> populateGrid(data));
        });

        exec.shutdown();
    }

    // ── Build a card for each indicator ─────────────────────────
    private void populateGrid(Map<String, IndicatorResult> data) {
        grid.getChildren().clear();

        int col = 0, row = 0;
        for (Map.Entry<String, IndicatorResult> entry : data.entrySet()) {
            VBox card = buildCard(entry.getKey(), entry.getValue());

            GridPane.setFillWidth(card, true);
            GridPane.setFillHeight(card, true);
            card.setMaxWidth(Double.MAX_VALUE);
            card.setMaxHeight(Double.MAX_VALUE);

            grid.add(card, col, row);

            col++;
            if (col > 2) { col = 0; row++; } // 3 columns
        }
    }

    // ── Single indicator card ────────────────────────────────────
    private VBox buildCard(String labelText, IndicatorResult result) {

        // Icon circle — smaller
        Label iconLbl = new Label(result.icon);
        iconLbl.setStyle(
                "-fx-font-size: 14px;" +
                        "-fx-min-width: 28px; -fx-min-height: 28px;" +
                        "-fx-max-width: 28px; -fx-max-height: 28px;" +
                        "-fx-background-color: #d0f0de;" +
                        "-fx-background-radius: 50%;" +
                        "-fx-alignment: CENTER;"
        );
        iconLbl.setAlignment(Pos.CENTER);

        // Label
        Label nameLbl = new Label(labelText);
        nameLbl.getStyleClass().add("agri-card-label");
        nameLbl.setWrapText(true);

        // Value
        Label valueLbl = new Label(result.formatted());
        valueLbl.getStyleClass().add("agri-card-value");

        // Unit + year
        String unitText = result.value != null
                ? result.unit + (result.year != null && !result.year.isEmpty()
                ? "  ·  " + result.year : "")
                : "";
        Label unitLbl = new Label(unitText);
        unitLbl.getStyleClass().add("agri-card-unit");

        VBox valStack = new VBox(1, valueLbl, unitLbl);
        valStack.setAlignment(Pos.BOTTOM_LEFT);
        VBox.setVgrow(valStack, Priority.ALWAYS);

        HBox topRow = new HBox(6, iconLbl, nameLbl);
        topRow.setAlignment(Pos.CENTER_LEFT);

        VBox card = new VBox(5, topRow, valStack);
        card.getStyleClass().add("agri-card");
        card.setPadding(new Insets(8, 10, 8, 10));
        card.setAlignment(Pos.TOP_LEFT);

        if (result.value == null) {
            valueLbl.setStyle(
                    "-fx-font-family: 'Georgia'; -fx-font-size: 18px;" +
                            "-fx-font-weight: bold; -fx-text-fill: #b0c8ba;"
            );
        }

        return card;
    }
}