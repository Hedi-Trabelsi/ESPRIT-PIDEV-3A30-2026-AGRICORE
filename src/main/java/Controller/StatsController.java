package Controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.chart.*;
import javafx.event.ActionEvent;
import Model.Maintenance;
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

    private final ServiceMaintenance sm;

    {
        try {
            sm = new ServiceMaintenance();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

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
            List.of("en cours", "planifie", "resolu", "en attente").forEach(s -> {
                long count = statutCounts.getOrDefault(s, 0L);
                statutPieChart.getData().add(new PieChart.Data(s.toUpperCase() + " (" + count + ")", count));
            });

            //  ACTIVITÉ PAR JOUR
            // 1. Nettoyage du graphique
            barChart.getData().clear();

// 2. Création de la série et remplissage des données
            XYChart.Series<String, Number> series = new XYChart.Series<>();
            series.setName("Demandes");

            Map<LocalDate, Long> demandesParJour = toutes.stream()
                    .filter(m -> m.getDateDeclaration() != null)
                    .collect(Collectors.groupingBy(Maintenance::getDateDeclaration, TreeMap::new, Collectors.counting()));

            demandesParJour.forEach((date, count) -> {
                series.getData().add(new XYChart.Data<>(date.toString(), count));
            });

// 3. AJOUT UNIQUE au graphique (On ne le fait qu'une seule fois !)
            barChart.getData().add(series);

// 4. APPLICATION DE LA COULEUR
// On vérifie que le Node existe avant d'appliquer le style
            for (XYChart.Data<String, Number> data : series.getData()) {
                if (data.getNode() != null) {
                    data.getNode().setStyle("-fx-bar-fill: #f8fafc;");
                }
            }

// 5. CONFIGURATION DES AXES
            NumberAxis yAxis = (NumberAxis) barChart.getYAxis();
            yAxis.setTickUnit(1);
            yAxis.setMinorTickVisible(false);
            yAxis.setAutoRanging(true); // Permet d'ajuster l'échelle automatiquement

            CategoryAxis xAxis = (CategoryAxis) barChart.getXAxis();
            xAxis.setTickLabelRotation(0);

// 6. RÉGLAGES VISUELS
            barChart.setCategoryGap(50.0); // Réduit un peu pour que les barres soient visibles
            barChart.setBarGap(10.0);

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @FXML
    void goBack(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/fxml/Dashboard.fxml"));
            NavigationUtil.loadInContentArea(barChart, root);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}