package controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.chart.*;
import javafx.event.ActionEvent;
import models.Maintenance;
import services.ServiceMaintenance;
import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

public class StatsController {
    @FXML private BarChart<String, Number> barChart;
    @FXML private PieChart pieChart;
    @FXML private PieChart statutPieChart;

    private final ServiceMaintenance sm = new ServiceMaintenance();

    public void initialize() {
        try {
            List<Maintenance> toutes = sm.afficher();

            // PRIORITÉS
            pieChart.getData().clear();
            Map<String, Long> priorityCounts = toutes.stream()
                    .collect(Collectors.groupingBy(m -> m.getPriorite().toLowerCase(), Collectors.counting()));
            List.of("urgente", "normale", "faible").forEach(p -> {
                long count = priorityCounts.getOrDefault(p, 0L);
                pieChart.getData().add(new PieChart.Data(p.toUpperCase() + " (" + count + ")", count));
            });

            //  STATUTS
            statutPieChart.getData().clear();
            Map<String, Long> statutCounts = toutes.stream()
                    .collect(Collectors.groupingBy(m -> m.getStatut().toLowerCase(), Collectors.counting()));
            List.of("en cours", "planifiee", "resolu", "en attente").forEach(s -> {
                long count = statutCounts.getOrDefault(s, 0L);
                statutPieChart.getData().add(new PieChart.Data(s.toUpperCase() + " (" + count + ")", count));
            });

            //  ACTIVITÉ PAR JOUR
            barChart.getData().clear();
            XYChart.Series<String, Number> series = new XYChart.Series<>();
            series.setName("Demandes");
            Map<LocalDate, Long> demandesParJour = toutes.stream()
                    .filter(m -> m.getDateDeclaration() != null)
                    .collect(Collectors.groupingBy(Maintenance::getDateDeclaration, TreeMap::new, Collectors.counting()));
            demandesParJour.forEach((date, count) -> {
                series.getData().add(new XYChart.Data<>(date.toString(), count));
            });
            barChart.getData().add(series);

            // COULEUR ICI : Appliquer le vert #7ca76f à toutes les barres
            for (XYChart.Data<String, Number> data : series.getData()) {
                data.getNode().setStyle("-fx-bar-fill: #7ca76f;");
            }
            barChart.getData().add(series);
            NumberAxis yAxis = (NumberAxis) barChart.getYAxis();
            yAxis.setTickUnit(1);
            yAxis.setMinorTickVisible(false);

            CategoryAxis xAxis = (CategoryAxis) barChart.getXAxis();
            xAxis.setTickLabelRotation(0);

            barChart.setCategoryGap(90.0);
            barChart.setBarGap(10.0);

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @FXML
    void goBack(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/interfaces/Dashboard.fxml"));
            barChart.getScene().setRoot(root);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}