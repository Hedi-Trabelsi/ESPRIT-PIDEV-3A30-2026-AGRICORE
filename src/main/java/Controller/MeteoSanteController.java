package Controller;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import Model.Animal;
import Model.SuiviAnimal;
import services.AnimalService;
import services.SuiviAnimalService;

import java.net.URI;
import java.net.http.*;
import java.util.*;
import java.util.stream.Collectors;

public class MeteoSanteController {

    @FXML private TextField         villeTf;
    @FXML private Button            btnMeteo;
    @FXML private ProgressIndicator progressIndicator;
    @FXML private Label             lblVille;
    @FXML private Label             lblTempExt;
    @FXML private Label             lblHumidite;
    @FXML private Label             lblVent;
    @FXML private Label             lblDescription;
    @FXML private Label             lblIconeMeteo;
    @FXML private VBox              risquesBox;
    @FXML private VBox              conseilsBox;
    @FXML private VBox              alertesAnimauxBox;
    @FXML private VBox              meteoBox;

    private final AnimalService      animalService = new AnimalService();
    private final SuiviAnimalService suiviService  = new SuiviAnimalService();

    // ✅ Votre clé API OpenWeatherMap
    private static final String API_KEY  = "a9eb50d5aa78991d8326e3599e86fad9";
    private static final String BASE_URL = "https://api.openweathermap.org/data/2.5/weather";

    @FXML
    void initialize() {
        meteoBox.setVisible(false);
        progressIndicator.setVisible(false);
        villeTf.setText("Tunis");
    }

    // ════════════════════════════════════════
    //  RECHERCHER MÉTÉO
    // ════════════════════════════════════════
    @FXML
    void rechercherMeteo() {
        String ville = villeTf.getText().trim();
        if (ville.isEmpty()) { showAlert("Entrez une ville !"); return; }

        btnMeteo.setDisable(true);
        progressIndicator.setVisible(true);
        meteoBox.setVisible(false);

        new Thread(() -> {
            try {
                String url = BASE_URL + "?q=" + ville.replace(" ", "+")
                        + "&appid=" + API_KEY + "&units=metric&lang=fr";

                HttpClient client = HttpClient.newHttpClient();
                HttpRequest req   = HttpRequest.newBuilder()
                        .uri(URI.create(url)).GET().build();
                HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString());

                if (res.statusCode() == 401) {
                    Platform.runLater(() -> showAlert(
                            "Clé API pas encore activée.\nAttendez 10-60 min après inscription."));
                    resetUI(); return;
                }
                if (res.statusCode() == 404) {
                    Platform.runLater(() -> showAlert("Ville introuvable : " + ville));
                    resetUI(); return;
                }

                String body       = res.body();
                double tempExt    = parseDouble(body, "\"temp\":");
                double humidity   = parseDouble(body, "\"humidity\":");
                double windSpeed  = parseDouble(body, "\"speed\":");
                double feelsLike  = parseDouble(body, "\"feels_like\":");
                String desc       = parseString(body, "\"description\":\"");
                String cityName   = parseString(body, "\"name\":\"");

                List<Animal>      animaux = animalService.read();
                List<SuiviAnimal> suivis  = suiviService.read();

                Platform.runLater(() -> {
                    afficherMeteo(cityName, tempExt, humidity, windSpeed, feelsLike, desc);
                    analyserRisques(tempExt, humidity, windSpeed, animaux, suivis);
                    meteoBox.setVisible(true);
                    progressIndicator.setVisible(false);
                    btnMeteo.setDisable(false);
                });

            } catch (Exception e) {
                Platform.runLater(() -> {
                    showAlert("Erreur connexion : " + e.getMessage());
                    resetUI();
                });
            }
        }).start();
    }

    private void resetUI() {
        Platform.runLater(() -> {
            progressIndicator.setVisible(false);
            btnMeteo.setDisable(false);
        });
    }

    // ════════════════════════════════════════
    //  AFFICHER MÉTÉO
    // ════════════════════════════════════════
    private void afficherMeteo(String ville, double temp, double humidity,
                               double wind, double feelsLike, String desc) {
        lblVille.setText("📍 " + ville);
        lblTempExt.setText(temp + " °C");
        lblHumidite.setText(humidity + " %");
        lblVent.setText(wind + " km/h");
        lblDescription.setText("🌤️ " + desc + "  |  Ressenti : " + feelsLike + " °C");

        String icone, couleur;
        if      (temp >= 38) { icone = "🔥"; couleur = "#c62828"; }
        else if (temp >= 30) { icone = "☀️"; couleur = "#f57c00"; }
        else if (temp >= 20) { icone = "🌤️"; couleur = "#2e7d32"; }
        else if (temp >= 10) { icone = "🌥️"; couleur = "#1565c0"; }
        else if (temp >= 0)  { icone = "❄️"; couleur = "#0288d1"; }
        else                 { icone = "🥶"; couleur = "#283593"; }

        lblIconeMeteo.setText(icone);
        lblTempExt.setStyle("-fx-font-size:28px;-fx-font-weight:bold;-fx-text-fill:" + couleur + ";");
    }

    // ════════════════════════════════════════
    //  ANALYSER RISQUES
    // ════════════════════════════════════════
    private void analyserRisques(double temp, double humidity, double wind,
                                 List<Animal> animaux, List<SuiviAnimal> suivis) {
        risquesBox.getChildren().clear();
        conseilsBox.getChildren().clear();
        alertesAnimauxBox.getChildren().clear();

        List<String> risques  = new ArrayList<>();
        List<String> conseils = new ArrayList<>();

        Map<String, Long> parEspece = animaux.stream()
                .collect(Collectors.groupingBy(
                        a -> a.getEspece().toLowerCase(), Collectors.counting()));

        // ── CHALEUR EXTRÊME ──
        if (temp >= 38) {
            risques.add("🔴 VAGUE DE CHALEUR EXTRÊME (" + temp + "°C)");
            if (parEspece.containsKey("vache") || parEspece.containsKey("bovin")) {
                risques.add("🐄 Bovins : Coup de chaleur critique — Mortalité possible");
                conseils.add("💧 Bovins : Doubler l'abreuvement (80L/jour minimum)");
                conseils.add("🏠 Bovins : Mettre à l'ombre entre 11h-17h obligatoirement");
                conseils.add("🌬️ Bovins : Ventilation forcée dans les étables");
            }
            if (parEspece.containsKey("poulet") || parEspece.containsKey("volaille")) {
                risques.add("🐔 Volailles : DANGER MORTEL — Mortalité de masse possible");
                conseils.add("❄️ Volailles : Brumisation + ventilation maximale URGENTE");
                conseils.add("💧 Volailles : Eau froide renouvelée toutes les 2h");
            }
            if (parEspece.containsKey("mouton") || parEspece.containsKey("ovin")) {
                risques.add("🐑 Ovins : Hyperthermie et stress thermique");
                conseils.add("✂️ Ovins : Tonte immédiate si toison épaisse");
            }
            if (parEspece.containsKey("cheval") || parEspece.containsKey("equin")) {
                risques.add("🐎 Chevaux : Coup de chaleur — Surveiller transpiration");
                conseils.add("🚿 Chevaux : Douche froide matin et soir");
                conseils.add("🏇 Chevaux : Annuler tout exercice physique");
            }
            conseils.add("🕐 Tous : Éviter toute manipulation entre 11h et 17h");
        }

        // ── CHALEUR FORTE ──
        else if (temp >= 30) {
            risques.add("🟠 FORTE CHALEUR (" + temp + "°C)");
            if (parEspece.containsKey("vache") || parEspece.containsKey("bovin")) {
                risques.add("🐄 Bovins : Stress thermique — Baisse production laitière");
                conseils.add("💧 Bovins : Augmenter l'abreuvement de 30%");
            }
            if (parEspece.containsKey("poulet") || parEspece.containsKey("volaille")) {
                risques.add("🐔 Volailles : Stress thermique — Baisse de ponte");
                conseils.add("❄️ Volailles : Ventilation renforcée et brumisation");
            }
            conseils.add("🕐 Tous : Éviter les manipulations entre 11h et 16h");
        }

        // ── FROID EXTRÊME ──
        else if (temp <= -5) {
            risques.add("🔵 FROID EXTRÊME (" + temp + "°C)");
            if (parEspece.containsKey("vache") || parEspece.containsKey("bovin")) {
                risques.add("🐄 Bovins : Hypothermie — Veaux très vulnérables");
                conseils.add("🏠 Bovins : Rentrer les veaux en bâtiment chauffé");
                conseils.add("🥛 Bovins : Augmenter les rations alimentaires de 20%");
            }
            if (parEspece.containsKey("mouton") || parEspece.containsKey("ovin")) {
                risques.add("🐑 Ovins : Hypothermie des agneaux");
                conseils.add("🏠 Ovins : Abriter les brebis en gestation");
            }
            conseils.add("💧 Tous : Vérifier que les abreuvoirs ne sont pas gelés");
            conseils.add("🌾 Tous : Augmenter les rations de fourrage");
        }

        // ── FROID MODÉRÉ ──
        else if (temp <= 5) {
            risques.add("🔵 FROID MODÉRÉ (" + temp + "°C)");
            risques.add("🌬️ Risque de maladies respiratoires augmenté");
            conseils.add("🏠 Jeunes animaux : Abriter et surveiller la température");
            conseils.add("💊 Tous : Vérifier vaccinations contre maladies hivernales");
        }

        // ── HUMIDITÉ ÉLEVÉE ──
        if (humidity >= 80) {
            risques.add("💧 HUMIDITÉ ÉLEVÉE (" + humidity + "%) — Risque maladies fongiques");
            if (parEspece.containsKey("mouton") || parEspece.containsKey("ovin")) {
                risques.add("🐑 Ovins : Risque PIÉTIN très élevé (maladie des sabots)");
                conseils.add("🦶 Ovins : Bain de pieds préventif au sulfate de zinc");
                conseils.add("🌿 Ovins : Éviter les prairies détrempées");
            }
            if (parEspece.containsKey("poulet") || parEspece.containsKey("volaille")) {
                risques.add("🐔 Volailles : Risque coccidiose et maladies respiratoires");
                conseils.add("🏠 Volailles : Améliorer la ventilation des poulaillers");
            }
            conseils.add("🌾 Tous : Surveiller la qualité des fourrages (moisissures)");
        }

        // ── VENT FORT ──
        if (wind >= 50) {
            risques.add("💨 VENT FORT (" + wind + " km/h) — Risque stress et blessures");
            conseils.add("🏠 Tous : Abriter les animaux et sécuriser les structures");
            conseils.add("🔍 Tous : Inspecter les clôtures après tempête");
        }

        // ── CONDITIONS FAVORABLES ──
        if (temp >= 15 && temp <= 25 && humidity < 70 && wind < 30) {
            risques.add("✅ CONDITIONS MÉTÉO FAVORABLES POUR LES ANIMAUX");
            conseils.add("✅ Conditions idéales pour la santé du troupeau");
            conseils.add("🌿 Bonne période pour vaccinations et soins préventifs");
            conseils.add("🏃 Activité physique normale recommandée");
        }

        if (animaux.isEmpty())
            risques.add("ℹ️ Aucun animal enregistré dans le système");

        // Afficher risques
        for (String r : risques) {
            Label lbl = new Label(r);
            lbl.setWrapText(true);
            boolean rouge  = r.startsWith("🔴") || r.contains("MORTEL");
            boolean orange = r.startsWith("🟠") || r.startsWith("💧") || r.startsWith("💨");
            lbl.setStyle(rouge  ? "-fx-text-fill:#c62828;-fx-font-weight:bold;-fx-font-size:13px;" :
                    orange ? "-fx-text-fill:#e65100;-fx-font-weight:bold;-fx-font-size:13px;" :
                            "-fx-text-fill:#2e7d32;-fx-font-weight:bold;-fx-font-size:13px;");
            risquesBox.getChildren().add(lbl);
        }

        // Afficher conseils
        if (conseils.isEmpty()) conseils.add("✅ Maintenir la surveillance habituelle");
        for (String c : conseils) {
            Label lbl = new Label(c);
            lbl.setStyle("-fx-text-fill:#1565c0;-fx-font-size:12px;");
            lbl.setWrapText(true);
            conseilsBox.getChildren().add(lbl);
        }

        // Corrélation avec suivis
        analyserCorrelation(temp, humidity, animaux, suivis);
    }

    // ════════════════════════════════════════
    //  CORRÉLATION MÉTÉO ↔ SUIVIS
    // ════════════════════════════════════════
    private void analyserCorrelation(double tempExt, double humidity,
                                     List<Animal> animaux, List<SuiviAnimal> suivis) {
        if (suivis.isEmpty() || animaux.isEmpty()) {
            Label lbl = new Label("ℹ️ Aucun suivi pour la corrélation");
            lbl.setStyle("-fx-text-fill:#888;");
            alertesAnimauxBox.getChildren().add(lbl);
            return;
        }

        int count = 0;
        for (Animal animal : animaux) {
            Optional<SuiviAnimal> opt = suivis.stream()
                    .filter(s -> s.getIdAnimal() == animal.getIdAnimal())
                    .max(Comparator.comparing(SuiviAnimal::getDateSuivi));
            if (opt.isEmpty()) continue;

            SuiviAnimal s = opt.get();
            List<String> alertes = new ArrayList<>();

            if (tempExt >= 30 && s.getTemperature() > 39.5)
                alertes.add("🔴 Température corporelle élevée (" + s.getTemperature() + "°C) + Chaleur → CRITIQUE");
            if (tempExt <= 5 && s.getRythmeCardiaque() < 45)
                alertes.add("🔴 Rythme faible (" + s.getRythmeCardiaque() + " bpm) + Froid → Hypothermie");
            if (humidity >= 80 && "Malade".equals(s.getEtatSante()))
                alertes.add("⚠️ Animal MALADE + Humidité élevée → Aggravation probable");
            if (tempExt >= 30 && "Faible".equals(s.getNiveauActivite()))
                alertes.add("⚠️ Activité faible + Chaleur → Surveiller hydratation");
            if ("Critique".equals(s.getEtatSante()))
                alertes.add("🚨 Animal CRITIQUE — Météo défavorable aggrave le risque");

            if (!alertes.isEmpty()) {
                count++;
                VBox carte = new VBox(5);
                carte.setStyle("-fx-background-color:#fff8e1;-fx-padding:12;"
                        + "-fx-background-radius:10;-fx-border-color:#ffcc02;-fx-border-radius:10;");
                Label nom = new Label("🐾 " + animal.getCodeAnimal()
                        + " — " + animal.getEspece() + " (" + animal.getRace() + ")");
                nom.setStyle("-fx-font-weight:bold;-fx-font-size:13px;");
                carte.getChildren().add(nom);
                for (String a : alertes) {
                    Label l = new Label(a);
                    l.setStyle("-fx-font-size:12px;-fx-text-fill:#c62828;");
                    l.setWrapText(true);
                    carte.getChildren().add(l);
                }
                alertesAnimauxBox.getChildren().add(carte);
            }
        }

        if (count == 0) {
            Label ok = new Label("✅ Aucun animal à risque élevé selon la météo actuelle");
            ok.setStyle("-fx-text-fill:#2e7d32;-fx-font-size:13px;");
            alertesAnimauxBox.getChildren().add(ok);
        }
    }

    // ════════════════════════════════════════
    //  PARSEURS JSON
    // ════════════════════════════════════════
    private double parseDouble(String json, String key) {
        try {
            int idx = json.indexOf(key);
            if (idx < 0) return 0;
            int s = idx + key.length(), e = s;
            while (e < json.length() && (Character.isDigit(json.charAt(e))
                    || json.charAt(e) == '.' || json.charAt(e) == '-')) e++;
            return Double.parseDouble(json.substring(s, e));
        } catch (Exception ex) { return 0; }
    }

    private String parseString(String json, String key) {
        try {
            int idx = json.indexOf(key);
            if (idx < 0) return "";
            int s = idx + key.length();
            return json.substring(s, json.indexOf("\"", s));
        } catch (Exception ex) { return ""; }
    }

    // ════════════════════════════════════════
    //  NAVIGATION
    // ════════════════════════════════════════
    @FXML
    void navigateBack() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/fxml/ShowAnimals.fxml"));
            NavigationUtil.loadInContentArea(villeTf, root);
        } catch (Exception e) { showAlert(e.getMessage()); }
    }

    private void showAlert(String msg) {
        new Alert(Alert.AlertType.ERROR, msg).showAndWait();
    }
}