package Controller;

import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import Model.Depense;
import Model.User;
import Model.AnomalyResult;
import services.DepenseService;
import services.ai.AnomalyDetectionService;
import services.ai.RobustAnomalyDetectionService;

import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class AnomalyPageController {

    // ── FXML fields ──────────────────────────────────────────────────────────
    @FXML private Label titleLabel;
    @FXML private VBox  anomFeed;
    @FXML private BarChart<String, Number> zscoreChart;

    @FXML private Label totalAnomaliesLabel;
    @FXML private Label maxZLabel;
    @FXML private Label totalMontantLabel;
    @FXML private Label tauxLabel;

    private final DepenseService          depenseService;

    {
        try {
            depenseService = new DepenseService();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private final AnomalyDetectionService ads            = new RobustAnomalyDetectionService();
    private final DateTimeFormatter       fmt            = DateTimeFormatter.ofPattern("dd/MM");
    private User user;

    private static final double ANOMALY_THRESHOLD = 2.5;
    private static final double WARNING_THRESHOLD = 1.5;

    // ── Entry point ──────────────────────────────────────────────────────────

    public void setUser(User user) {
        this.user = user;
        if (titleLabel != null && user != null)
            titleLabel.setText("Anomalies dépenses: " + user.getPrenom() + " " + user.getNom());
        refresh();
    }

    // ── Refresh ──────────────────────────────────────────────────────────────

    @FXML
    void refresh() {
        if (user == null) return;
        try {
            List<Depense> ds = depenseService.readByUser(user.getId());

            List<Row> rows = new ArrayList<>();
            for (Depense d : ds) {
                AnomalyResult res = ads.analyzeDepense(user.getId(), ds, d);
                String bounds = "";
                if (res.getLowerBound() != null && res.getUpperBound() != null)
                    bounds = String.format("[%.0f–%.0f]", res.getLowerBound(), res.getUpperBound());
                rows.add(new Row(
                        d.getDate() != null ? d.getDate().format(fmt) : "—",
                        d.getType() != null ? d.getType().name() : "AUTRE",
                        d.getMontant(),
                        res.getScore(),
                        bounds,
                        res.isAnomaly()
                ));
            }

            // Sort: highest severity first, then by z-score desc
            rows.sort((a, b) -> {
                int sa = severity(a.zscore), sb = severity(b.zscore);
                if (sa != sb) return Integer.compare(sb, sa);
                return Double.compare(b.zscore, a.zscore);
            });

            updateKpis(rows);
            renderChart(rows);
            renderList(rows);

        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR, e.getMessage()).showAndWait();
        }
    }

    // ── KPI strip ────────────────────────────────────────────────────────────

    private void updateKpis(List<Row> rows) {
        long count   = rows.stream().filter(r -> r.anomaly).count();
        double maxZ  = rows.stream().mapToDouble(r -> r.zscore).max().orElse(0.0);
        double total = rows.stream().filter(r -> r.anomaly).mapToDouble(r -> r.montant).sum();
        double taux  = rows.isEmpty() ? 0.0 : (count * 100.0 / rows.size());

        if (totalAnomaliesLabel != null)
            totalAnomaliesLabel.setText(String.valueOf(count));
        if (maxZLabel != null)
            maxZLabel.setText(String.format("%.2f", maxZ));
        if (totalMontantLabel != null)
            totalMontantLabel.setText(String.format("%.2f DT", total));
        if (tauxLabel != null)
            tauxLabel.setText(String.format("%.0f%%", taux));
    }

    // ── Bar chart ────────────────────────────────────────────────────────────

    private void renderChart(List<Row> rows) {
        if (zscoreChart == null) return;
        zscoreChart.getData().clear();

        XYChart.Series<String, Number> seriesRed   = new XYChart.Series<>();
        XYChart.Series<String, Number> seriesAmber = new XYChart.Series<>();
        XYChart.Series<String, Number> seriesGreen = new XYChart.Series<>();
        seriesRed.setName("Anomalie");
        seriesAmber.setName("Borderline");
        seriesGreen.setName("Normal");

        // Top 20 by z-score for readability
        List<Row> chartRows = rows.stream()
                .sorted((a, b) -> Double.compare(b.zscore, a.zscore))
                .limit(20)
                .toList();

        int idx = 1;
        for (Row r : chartRows) {
            String label = r.type.length() > 6 ? r.type.substring(0, 6) : r.type;
            String key   = label + " " + idx++;
            double z     = Math.abs(r.zscore);

            if (z >= ANOMALY_THRESHOLD) {
                XYChart.Data<String, Number> d = new XYChart.Data<>(key, z);
                seriesRed.getData().add(d);
                d.nodeProperty().addListener((obs, o, node) -> {
                    if (node != null) node.setStyle("-fx-bar-fill: #ef4444;");
                });
            } else if (z >= WARNING_THRESHOLD) {
                XYChart.Data<String, Number> d = new XYChart.Data<>(key, z);
                seriesAmber.getData().add(d);
                d.nodeProperty().addListener((obs, o, node) -> {
                    if (node != null) node.setStyle("-fx-bar-fill: #f59e0b;");
                });
            } else {
                XYChart.Data<String, Number> d = new XYChart.Data<>(key, z);
                seriesGreen.getData().add(d);
                d.nodeProperty().addListener((obs, o, node) -> {
                    if (node != null) node.setStyle("-fx-bar-fill: #22c55e;");
                });
            }
        }

        zscoreChart.getData().addAll(seriesRed, seriesAmber, seriesGreen);
    }

    // ── List renderer ─────────────────────────────────────────────────────────

    private void renderList(List<Row> rows) {
        if (anomFeed == null) return;
        anomFeed.getChildren().clear();

        if (rows.isEmpty()) {
            Label empty = new Label("✓  Aucune transaction à afficher.");
            empty.getStyleClass().add("feed-empty");
            anomFeed.getChildren().add(empty);
            return;
        }

        for (int i = 0; i < rows.size(); i++) {
            anomFeed.getChildren().add(buildListRow(rows.get(i), i));
        }
    }

    // ── List row builder ──────────────────────────────────────────────────────

    private HBox buildListRow(Row r, int idx) {
        boolean isAnomaly = r.zscore >= ANOMALY_THRESHOLD;
        boolean isWarning = !isAnomaly && r.zscore >= WARNING_THRESHOLD;

        HBox row = new HBox();
        String rowStyle = isAnomaly ? (idx % 2 == 0 ? "anom-row" : "anom-row-alt")
                : "anom-row-normal";
        row.getStyleClass().add(rowStyle);
        row.setAlignment(Pos.CENTER_LEFT);

        // Type
        Label type = new Label(r.type);
        type.getStyleClass().add("anom-cell-type");

        // Date
        Label date = new Label(r.date);
        date.getStyleClass().add("anom-cell-date");

        // Montant
        Label montant = new Label(String.format("%.2f DT", r.montant));
        montant.getStyleClass().add(isAnomaly ? "anom-cell-montant-flag" : "anom-cell-montant");

        // Z-score badge
        Label zBadge = new Label(String.format("%.2f", r.zscore));
        if (isAnomaly)      zBadge.getStyleClass().add("z-badge-red");
        else if (isWarning) zBadge.getStyleClass().add("z-badge-amber");
        else                zBadge.getStyleClass().add("z-badge-green");

        // Interval
        Label interval = new Label(r.bounds.isBlank() ? "—" : r.bounds);
        interval.getStyleClass().add("anom-cell-interval");
        HBox.setHgrow(interval, Priority.ALWAYS);

        row.getChildren().addAll(type, date, montant, zBadge, interval);
        return row;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private int severity(double z) {
        if (z >= ANOMALY_THRESHOLD) return 2;
        if (z >= WARNING_THRESHOLD) return 1;
        return 0;
    }

    // ── Row model ─────────────────────────────────────────────────────────────

    private static class Row {
        final String  date;
        final String  type;
        final double  montant;
        final double  zscore;
        final String  bounds;
        final boolean anomaly;

        Row(String date, String type, double montant,
            double zscore, String bounds, boolean anomaly) {
            this.date    = date;
            this.type    = type;
            this.montant = montant;
            this.zscore  = zscore;
            this.bounds  = bounds;
            this.anomaly = anomaly;
        }
    }
}
