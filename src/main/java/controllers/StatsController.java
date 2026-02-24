package controllers;

import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import models.Maintenance;
import models.Tache;
import services.ServiceMaintenance;
import services.ServiceTache;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class StatsController {
    @FXML private BarChart<String, Number> barChart;
    @FXML
    private PieChart pieChart;
    private final ServiceMaintenance sm = new ServiceMaintenance();
    private final ServiceTache st = new ServiceTache();

    public void initialize() {
        try {
            // 1. Charger le PieChart (Priorités)
            List<Maintenance> toutes = sm.afficher();
            Map<String, Long> counts = toutes.stream()
                    .collect(Collectors.groupingBy(Maintenance::getPriorite, Collectors.counting()));

            counts.forEach((prio, count) ->
                    pieChart.getData().add(new PieChart.Data(prio, count)));

            // 2. Charger le BarChart (Coûts par équipement)
            XYChart.Series<String, Number> series = new XYChart.Series<>();
            series.setName("Dépenses Réelles");

            // Logique : Pour chaque maintenance résolue, sommer le coût de ses tâches
            for (Maintenance m : toutes) {
                if ("resolu".equalsIgnoreCase(m.getStatut())) {
                    int coutTaches = st.getTachesByMaintenance(m.getId()).stream()
                            .mapToInt(Tache::getCout_estimee).sum();
                    series.getData().add(new XYChart.Data<>(m.getEquipement(), coutTaches));
                }
            }
            barChart.getData().add(series);

        } catch (SQLException e) { e.printStackTrace(); }
    }
}
