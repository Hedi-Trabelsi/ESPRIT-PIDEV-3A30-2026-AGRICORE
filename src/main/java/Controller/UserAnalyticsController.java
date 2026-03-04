package Controller;

import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.control.TableColumn;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.layout.VBox;
import javafx.scene.layout.AnchorPane;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import Model.Depense;
import Model.User;
import Model.Vente;
import services.DepenseService;
import services.VenteService;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

public class UserAnalyticsController {
    @FXML
    private Label userTitle;
    @FXML
    private Label dep30Label;
    @FXML
    private Label ven30Label;
    @FXML
    private Label benef30Label;
    @FXML
    private PieChart depenseByTypeChart;
    @FXML
    private BarChart<String, Number> monthlyBarChart;
    @FXML
    private LineChart<String, Number> forecastLineChart;
    @FXML
    private Label avgDepenseLabel;
    @FXML
    private Label trendLabel;
    @FXML
    private Label forecastLabel;
    @FXML
    private Label recommendationLabel;
    @FXML
    private TableView<AnomRow> anomTable;
    @FXML
    private TableColumn<AnomRow, String> anomDateCol;
    @FXML
    private TableColumn<AnomRow, String> anomTypeCol;
    @FXML
    private TableColumn<AnomRow, String> anomMontantCol;
    @FXML
    private TableColumn<AnomRow, String> anomZCol;
    @FXML
    private TableColumn<AnomRow, String> anomBoundsCol;
    @FXML
    private TableColumn<AnomRow, String> anomFlagCol;
    @FXML
    private Node sidebar;
    @FXML
    private ComboBox<String> anomSensitivity;
    @FXML
    private VBox anomFeed;
    @FXML
    private AnchorPane anomHost;

    private final DepenseService depenseService;

    {
        try {
            depenseService = new DepenseService();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private final VenteService venteService;

    {
        try {
            venteService = new VenteService();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private User user;
    private List<Depense> depenses = new ArrayList<>();
    private List<Vente> ventes = new ArrayList<>();

    public void setEmbedded(boolean embedded) { }

    public void setUser(User user) {
        this.user = user;
        if (userTitle != null && user != null) {
            userTitle.setText("Analyse financière: " + user.getPrenom() + " " + user.getNom());
        }
        loadData();
        refreshAnalytics();
        refreshAnomalies();
        wireFinanceNav();
        if (anomSensitivity != null) {
            anomSensitivity.getItems().setAll("Faible", "Normale", "Forte");
            anomSensitivity.getSelectionModel().select("Normale");
            anomSensitivity.valueProperty().addListener((o,a,b)->refreshAnomalies());
        }
        if (anomHost != null) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/AnomalyPage.fxml"));
                Parent root = loader.load();
                Controller.AnomalyPageController controller = loader.getController();
                controller.setUser(user);
                anomHost.getChildren().setAll(root);
                AnchorPane.setTopAnchor(root, 0.0);
                AnchorPane.setBottomAnchor(root, 0.0);
                AnchorPane.setLeftAnchor(root, 0.0);
                AnchorPane.setRightAnchor(root, 0.0);
            } catch (Exception ignored) {}
        }
    }

    private void loadData() {
        try {
            depenses = depenseService.readByUser(user.getId());
            ventes = venteService.readByUser(user.getId());
        } catch (Exception e) {
            depenses = new ArrayList<>();
            ventes = new ArrayList<>();
        }
    }

    @FXML
    void refreshAnalytics() {
        double avg = depenses.stream().mapToDouble(Depense::getMontant).average().orElse(0.0);
        if (avgDepenseLabel != null) avgDepenseLabel.setText(String.format("%.2f", avg));

        Map<YearMonth, Double> monthly = new HashMap<>();
        for (Depense d : depenses) {
            if (d.getDate() == null) continue;
            YearMonth ym = YearMonth.from(d.getDate());
            monthly.put(ym, monthly.getOrDefault(ym, 0.0) + d.getMontant());
        }
        List<Map.Entry<YearMonth, Double>> sorted = monthly.entrySet().stream()
                .sorted(Map.Entry.comparingByKey(Comparator.reverseOrder()))
                .collect(Collectors.toList());
        double m0 = sorted.size() > 0 ? sorted.get(0).getValue() : 0.0;
        double m1 = sorted.size() > 1 ? sorted.get(1).getValue() : 0.0;
        double m2 = sorted.size() > 2 ? sorted.get(2).getValue() : 0.0;
        double wma = (3 * m0 + 2 * m1 + 1 * m2) / 6.0;
        double trend = m1 > 0 ? (m0 - m1) / m1 : 0.0;
        double trendClamped = Math.max(-0.5, Math.min(0.5, trend));
        double forecast = wma * (1 + trendClamped);
        if (trendLabel != null) trendLabel.setText(String.format("%.0f%%", trendClamped * 100));
        if (forecastLabel != null) forecastLabel.setText(String.format("%.2f", forecast));

        LocalDate now = LocalDate.now();
        LocalDate cutoff = now.minusDays(30);
        double dep30 = depenses.stream()
                .filter(d -> d.getDate() != null && !d.getDate().isBefore(cutoff))
                .mapToDouble(Depense::getMontant).sum();
        double ven30 = ventes.stream()
                .filter(v -> v.getDate() != null && !v.getDate().isBefore(cutoff))
                .mapToDouble(Vente::getChiffreAffaires).sum();
        if (dep30Label != null) dep30Label.setText(String.format("%.2f", dep30));
        if (ven30Label != null) ven30Label.setText(String.format("%.2f", ven30));
        if (benef30Label != null) benef30Label.setText(String.format("%.2f", (ven30 - dep30)));
        String reco;
        if (dep30 > ven30) {
            reco = "Trop de dépenses: réduis coûts variables ou décale dépenses non urgentes.";
        } else if (trend > 0.2) {
            reco = "Dépenses en hausse: négocie fournisseurs ou plafonne certains budgets.";
        } else {
            reco = "Situation stable: maintenir le suivi et optimiser les achats.";
        }
        if (recommendationLabel != null) recommendationLabel.setText(reco);

        if (depenseByTypeChart != null) {
            Map<String, Double> byType = new HashMap<>();
            for (Depense d : depenses) {
                String key = d.getType() != null ? d.getType().name() : "AUTRE";
                byType.put(key, byType.getOrDefault(key, 0.0) + d.getMontant());
            }
            depenseByTypeChart.getData().clear();
            for (Map.Entry<String, Double> e : byType.entrySet()) {
                depenseByTypeChart.getData().add(new PieChart.Data(e.getKey(), e.getValue()));
            }
        }

        if (monthlyBarChart != null) {
            Map<YearMonth, Double> depMonth = new HashMap<>();
            for (Depense d : depenses) {
                if (d.getDate() == null) continue;
                YearMonth ym = YearMonth.from(d.getDate());
                depMonth.put(ym, depMonth.getOrDefault(ym, 0.0) + d.getMontant());
            }
            Map<YearMonth, Double> venMonth = new HashMap<>();
            for (Vente v : ventes) {
                if (v.getDate() == null) continue;
                YearMonth ym = YearMonth.from(v.getDate());
                venMonth.put(ym, venMonth.getOrDefault(ym, 0.0) + v.getChiffreAffaires());
            }
            List<YearMonth> months = new ArrayList<>();
            months.addAll(depMonth.keySet());
            for (YearMonth ym : venMonth.keySet()) if (!months.contains(ym)) months.add(ym);
            months.sort(Comparator.naturalOrder());

            XYChart.Series<String, Number> depSeries = new XYChart.Series<>();
            depSeries.setName("Dépenses");
            XYChart.Series<String, Number> venSeries = new XYChart.Series<>();
            venSeries.setName("Ventes");
            for (YearMonth ym : months) {
                String label = ym.toString();
                depSeries.getData().add(new XYChart.Data<>(label, depMonth.getOrDefault(ym, 0.0)));
                venSeries.getData().add(new XYChart.Data<>(label, venMonth.getOrDefault(ym, 0.0)));
            }
            monthlyBarChart.getData().clear();
            monthlyBarChart.getData().addAll(depSeries, venSeries);
        }

        if (forecastLineChart != null) {
            services.ai.ForecastingService forecastingService = new services.ai.DefaultForecastingService();
            Model.ForecastResult fr = forecastingService.forecastUserSales(user.getId(), ventes, 6);
            XYChart.Series<String, Number> hist = new XYChart.Series<>();
            hist.setName("Ventes passées");
            for (Model.ForecastPoint p : fr.getHistory()) {
                hist.getData().add(new XYChart.Data<>(p.getPeriod().toString(), p.getValue()));
            }
            XYChart.Series<String, Number> pred = new XYChart.Series<>();
            pred.setName("Prévision");
            XYChart.Series<String, Number> lower = new XYChart.Series<>();
            lower.setName("Borne basse");
            XYChart.Series<String, Number> upper = new XYChart.Series<>();
            upper.setName("Borne haute");
            for (Model.ForecastPoint p : fr.getForecast()) {
                pred.getData().add(new XYChart.Data<>(p.getPeriod().toString(), p.getValue()));
                if (p.getLower() != null) lower.getData().add(new XYChart.Data<>(p.getPeriod().toString(), p.getLower()));
                if (p.getUpper() != null) upper.getData().add(new XYChart.Data<>(p.getPeriod().toString(), p.getUpper()));
            }
            forecastLineChart.getData().clear();
            forecastLineChart.getData().addAll(hist, pred, lower, upper);
            if (!fr.getAlerts().isEmpty()) {
                recommendationLabel.setText(recommendationLabel.getText() + " | " + String.join(" | ", fr.getAlerts()));
            }
        }
    }

    private void refreshAnomalies() {
        double thr = 3.5;
        if (anomSensitivity != null) {
            String sel = anomSensitivity.getSelectionModel().getSelectedItem();
            if ("Faible".equals(sel)) thr = 3.0;
            else if ("Forte".equals(sel)) thr = 4.0;
        }
        if (anomTable != null && anomDateCol != null && anomDateCol.getCellValueFactory() == null) {
            anomDateCol.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().date));
            anomTypeCol.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().type));
            anomMontantCol.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().montant));
            anomZCol.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().z));
            anomBoundsCol.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().bounds));
            anomFlagCol.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().flag));
        }
        services.ai.RobustAnomalyDetectionService svc = new services.ai.RobustAnomalyDetectionService(thr);
        java.time.format.DateTimeFormatter fmt = java.time.format.DateTimeFormatter.ISO_DATE;
        java.util.List<AnomRow> rows = new java.util.ArrayList<>();
        for (Model.Depense d : depenses) {
            var res = svc.analyzeDepense(user.getId(), depenses, d);
            String bounds = "";
            if (res.getLowerBound() != null && res.getUpperBound() != null) {
                bounds = String.format("[%.2f ; %.2f]", res.getLowerBound(), res.getUpperBound());
            }
            rows.add(new AnomRow(
                    d.getDate() != null ? d.getDate().format(fmt) : "",
                    d.getType() != null ? d.getType().name() : "",
                    String.format("%.2f", d.getMontant()),
                    String.format("%.2f", res.getScore()),
                    bounds,
                    res.isAnomaly() ? "Oui" : "Non"
            ));
        }
        if (anomTable != null) {
            anomTable.getItems().setAll(rows);
        }
        if (anomFeed != null) {
            anomFeed.getChildren().clear();
            for (AnomRow r : rows) {
                Label lbl = new Label((r.flag.equals("Oui") ? "⚠ " : "• ") + r.date + " · " + r.type + " · " + r.montant + " DT · z=" + r.z + " " + r.bounds);
                anomFeed.getChildren().add(lbl);
            }
        }
    }

    private void wireFinanceNav() {
        if (sidebar == null) return;
        javafx.scene.control.Button btn = (javafx.scene.control.Button) sidebar.lookup("#financeBtn");
        if (btn != null) {
            btn.setOnAction(e -> openFinancePage());
        }
    }

    private void openFinancePage() {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/fxml/FinanceTables.fxml"));
            javafx.scene.Parent root = loader.load();
            Controller.FinanceTablesController controller = loader.getController();
            controller.setUser(user);
            NavigationUtil.loadInContentArea(userTitle, root);
        } catch (Exception e) {
            // ignore
        }
    }

    @FXML
    void goBack() {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/fxml/ShowUsers.fxml"));
            javafx.scene.Parent root = loader.load();
            NavigationUtil.loadInContentArea(userTitle, root);
        } catch (Exception e) {
        }
    }

    public static class AnomRow {
        public final String date;
        public final String type;
        public final String montant;
        public final String z;
        public final String bounds;
        public final String flag;
        public AnomRow(String date, String type, String montant, String z, String bounds, String flag) {
            this.date = date;
            this.type = type;
            this.montant = montant;
            this.z = z;
            this.bounds = bounds;
            this.flag = flag;
        }
    }
}
