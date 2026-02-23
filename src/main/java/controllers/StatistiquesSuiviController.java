package controllers;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import models.Animal;
import models.SuiviAnimal;
import services.AnimalService;
import services.SuiviAnimalService;

import java.io.IOException;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class StatistiquesSuiviController {

    // ── Cartes moyennes ──────────────────────────────────────────────
    @FXML private Label lblMoyTemp;
    @FXML private Label lblMoyPoids;
    @FXML private Label lblMoyRythme;

    // ── Comptage états ───────────────────────────────────────────────
    @FXML private Label lblBon;
    @FXML private Label lblMalade;
    @FXML private Label lblCritique;
    @FXML private PieChart pieEtat;

    // ── Graphique évolution ──────────────────────────────────────────
    @FXML private LineChart<String, Number> lineChart;
    @FXML private CategoryAxis xAxis;
    @FXML private NumberAxis yAxis;
    @FXML private ComboBox<String> comboGraphique;

    // ── Alertes ──────────────────────────────────────────────────────
    @FXML private VBox alertesBox;

    private final SuiviAnimalService suiviService  = new SuiviAnimalService();
    private final AnimalService      animalService = new AnimalService();

    private List<SuiviAnimal> suivis;
    private List<Animal>      animals;

    @FXML
    public void initialize() {
        try {
            suivis  = suiviService.read();
            animals = animalService.read();
        } catch (SQLException e) {
            new Alert(Alert.AlertType.ERROR, e.getMessage()).show();
            return;
        }

        afficherMoyennes();
        afficherComptageEtats();
        configurerGraphique();
        afficherAlertes();
    }

    // ════════════════════════════════════════════════════════
    //  1. MOYENNES
    // ════════════════════════════════════════════════════════
    private void afficherMoyennes() {
        if (suivis.isEmpty()) {
            lblMoyTemp.setText("—");
            lblMoyPoids.setText("—");
            lblMoyRythme.setText("—");
            return;
        }

        double moyTemp   = suivis.stream().mapToDouble(SuiviAnimal::getTemperature).average().orElse(0);
        double moyPoids  = suivis.stream().mapToDouble(SuiviAnimal::getPoids).average().orElse(0);
        double moyRythme = suivis.stream().mapToInt(SuiviAnimal::getRythmeCardiaque).average().orElse(0);

        lblMoyTemp.setText(String.format("%.1f °C", moyTemp));
        lblMoyPoids.setText(String.format("%.1f kg", moyPoids));
        lblMoyRythme.setText(String.format("%.0f bpm", moyRythme));
    }

    // ════════════════════════════════════════════════════════
    //  2. COMPTAGE PAR ÉTAT DE SANTÉ
    // ════════════════════════════════════════════════════════
    private void afficherComptageEtats() {
        Map<String, Long> comptage = suivis.stream()
                .collect(Collectors.groupingBy(
                        s -> s.getEtatSante() == null ? "Inconnu" : s.getEtatSante(),
                        Collectors.counting()
                ));

        long bon      = comptage.getOrDefault("Bon", 0L);
        long malade   = comptage.getOrDefault("Malade", 0L);
        long critique = comptage.getOrDefault("Critique", 0L);

        lblBon.setText(bon + " animal(s)");
        lblMalade.setText(malade + " animal(s)");
        lblCritique.setText(critique + " animal(s)");

        // PieChart
        pieEtat.getData().clear();
        if (bon > 0)      pieEtat.getData().add(new PieChart.Data("Bon ✅ (" + bon + ")", bon));
        if (malade > 0)   pieEtat.getData().add(new PieChart.Data("Malade ⚠️ (" + malade + ")", malade));
        if (critique > 0) pieEtat.getData().add(new PieChart.Data("Critique 🔴 (" + critique + ")", critique));

        // Colorier les tranches
        pieEtat.getData().forEach(data -> {
            String label = data.getName();
            String color = label.startsWith("Bon") ? "#2e7d32"
                    : label.startsWith("Malade")   ? "#f57c00"
                    : "#c62828";
            data.getNode().setStyle("-fx-pie-color: " + color + ";");
        });

        pieEtat.setLegendVisible(true);
        pieEtat.setLabelsVisible(true);
    }

    // ════════════════════════════════════════════════════════
    //  3. GRAPHIQUE D'ÉVOLUTION
    // ════════════════════════════════════════════════════════
    private void configurerGraphique() {
        comboGraphique.setItems(FXCollections.observableArrayList(
                "Température", "Poids", "Rythme Cardiaque"
        ));
        comboGraphique.setValue("Température");
        comboGraphique.setOnAction(e -> rafraichirGraphique());
        rafraichirGraphique();
    }

    private void rafraichirGraphique() {
        lineChart.getData().clear();

        if (suivis.isEmpty()) return;

        String choix = comboGraphique.getValue();
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM HH:mm");

        // Trier par date
        List<SuiviAnimal> tries = suivis.stream()
                .sorted((a, b) -> a.getDateSuivi().compareTo(b.getDateSuivi()))
                .collect(Collectors.toList());

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName(choix);

        for (SuiviAnimal s : tries) {
            String date = sdf.format(s.getDateSuivi());
            Number valeur = switch (choix) {
                case "Température"      -> s.getTemperature();
                case "Poids"            -> s.getPoids();
                case "Rythme Cardiaque" -> s.getRythmeCardiaque();
                default                 -> s.getTemperature();
            };
            series.getData().add(new XYChart.Data<>(date, valeur));
        }

        lineChart.getData().add(series);
        xAxis.setLabel("Date");
        yAxis.setLabel(choix);
    }

    // ════════════════════════════════════════════════════════
    //  4. ALERTES ANIMAUX EN MAUVAIS ÉTAT
    // ════════════════════════════════════════════════════════
    private void afficherAlertes() {
        alertesBox.getChildren().clear();

        List<SuiviAnimal> enAlerte = suivis.stream()
                .filter(s -> "Malade".equals(s.getEtatSante()) || "Critique".equals(s.getEtatSante()))
                .collect(Collectors.toList());

        if (enAlerte.isEmpty()) {
            Label ok = new Label("✅ Aucun animal en état critique ou malade.");
            ok.setStyle("-fx-text-fill: #2e7d32; -fx-font-size: 14px;");
            alertesBox.getChildren().add(ok);
            return;
        }

        for (SuiviAnimal s : enAlerte) {
            // Trouver le nom de l'animal
            String nomAnimal = animals.stream()
                    .filter(a -> a.getIdAnimal() == s.getIdAnimal())
                    .map(a -> a.getCodeAnimal() + " (" + a.getEspece() + ")")
                    .findFirst().orElse("Animal inconnu");

            boolean critique = "Critique".equals(s.getEtatSante());

            HBox carte = new HBox(15);
            carte.setStyle("""
                    -fx-background-color: %s;
                    -fx-padding: 12;
                    -fx-background-radius: 10;
                    -fx-border-color: %s;
                    -fx-border-radius: 10;
                    """.formatted(
                    critique ? "#ffebee" : "#fff3e0",
                    critique ? "#c62828" : "#f57c00"
            ));

            Label icone = new Label(critique ? "🔴" : "⚠️");
            icone.setStyle("-fx-font-size: 20px;");

            VBox info = new VBox(3);
            Label nom  = new Label(nomAnimal);
            nom.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: " +
                    (critique ? "#c62828" : "#e65100") + ";");

            Label etat = new Label("État : " + s.getEtatSante()
                    + "  |  Temp : " + s.getTemperature() + "°C"
                    + "  |  Poids : " + s.getPoids() + " kg");
            etat.setStyle("-fx-font-size: 12px; -fx-text-fill: #555;");

            String remarque = s.getRemarque() != null && !s.getRemarque().isEmpty()
                    ? s.getRemarque() : "Aucune remarque";
            Label rem = new Label("📝 " + remarque);
            rem.setStyle("-fx-font-size: 11px; -fx-text-fill: #777;");

            info.getChildren().addAll(nom, etat, rem);
            carte.getChildren().addAll(icone, info);
            alertesBox.getChildren().add(carte);
        }
    }

    @FXML
    private void navigateBack() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/ShowSuiviAnimal.fxml"));
            Stage stage = (Stage) alertesBox.getScene().getWindow();
            stage.setScene(new Scene(root));
        } catch (IOException e) {
            new Alert(Alert.AlertType.ERROR, e.getMessage()).show();
        }
    }
}