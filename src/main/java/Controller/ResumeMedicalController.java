package Controller;

import javafx.application.Platform;
import javafx.collections.FXCollections;
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
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

public class ResumeMedicalController {

    // ── Entrées ──
    @FXML private ComboBox<Animal>  comboAnimal;
    @FXML private ComboBox<String>  periodeCombo;
    @FXML private ComboBox<String>  styleCombo;
    @FXML private CheckBox          checkConclusion;
    @FXML private CheckBox          checkPrognostic;
    @FXML private CheckBox          checkRecommandations;
    @FXML private Button            btnGenerer;
    @FXML private ProgressIndicator progressIndicator;
    @FXML private Label             lblStatut;

    // ── Résultats ──
    @FXML private VBox              resultBox;
    @FXML private Label             lblAnimalHeader;
    @FXML private Label             lblNbSuivis;
    @FXML private Label             lblPeriode;
    @FXML private Label             lblScoreSante;
    @FXML private VBox              statsBox;
    @FXML private TextArea          txtResume;

    private final AnimalService      animalService = new AnimalService();
    private final SuiviAnimalService suiviService  = new SuiviAnimalService();

    private static final String GROQ_API_KEY = System.getenv("GROQ_API_KEY") != null
            ? System.getenv("GROQ_API_KEY")
            : "VOTRE_CLE_GROQ_ICI";
    private static final String GROQ_URL     = "https://api.groq.com/openai/v1/chat/completions";
    private static final String GROQ_MODEL   = "llama-3.1-8b-instant";

    // ════════════════════════════════════════
    //  INITIALIZE
    // ════════════════════════════════════════
    @FXML
    void initialize() {
        resultBox.setVisible(false);
        progressIndicator.setVisible(false);

        // Charger animaux
        try {
            List<Animal> animaux = animalService.read();
            comboAnimal.setItems(FXCollections.observableArrayList(animaux));
            comboAnimal.setCellFactory(p -> new ListCell<>() {
                @Override protected void updateItem(Animal a, boolean empty) {
                    super.updateItem(a, empty);
                    setText(empty || a == null ? null
                            : a.getCodeAnimal() + " — " + a.getEspece() + " / " + a.getRace());
                }
            });
            comboAnimal.setButtonCell(new ListCell<>() {
                @Override protected void updateItem(Animal a, boolean empty) {
                    super.updateItem(a, empty);
                    setText(empty || a == null ? null
                            : a.getCodeAnimal() + " — " + a.getEspece());
                }
            });
        } catch (SQLException e) { showAlert(e.getMessage()); }

        periodeCombo.setItems(FXCollections.observableArrayList(
                "7 derniers jours",
                "30 derniers jours",
                "3 derniers mois",
                "6 derniers mois",
                "1 an",
                "Tout l'historique"));
        periodeCombo.setValue("30 derniers jours");

        styleCombo.setItems(FXCollections.observableArrayList(
                "Rapport veterinaire professionnel",
                "Resume simplifie pour eleveur",
                "Rapport scientifique detaille",
                "Fiche de suivi concise"));
        styleCombo.setValue("Rapport veterinaire professionnel");

        // Cocher tout par défaut
        checkConclusion.setSelected(true);
        checkPrognostic.setSelected(true);
        checkRecommandations.setSelected(true);
    }

    // ════════════════════════════════════════
    //  GÉNÉRER RÉSUMÉ
    // ════════════════════════════════════════
    @FXML
    void genererResume() {
        if (comboAnimal.getValue() == null) { showAlert("Choisissez un animal !"); return; }

        Animal animal = comboAnimal.getValue();
        String periode = periodeCombo.getValue();

        btnGenerer.setDisable(true);
        progressIndicator.setVisible(true);
        lblStatut.setText("Analyse des donnees et generation IA...");
        resultBox.setVisible(false);

        new Thread(() -> {
            try {
                // 1. Charger les suivis
                List<SuiviAnimal> tousLesSuivis = suiviService.readByAnimal(animal.getIdAnimal());

                // 2. Filtrer selon la période
                List<SuiviAnimal> suivisFiltres = filtrerParPeriode(tousLesSuivis, periode);

                if (suivisFiltres.isEmpty()) {
                    Platform.runLater(() -> {
                        showAlert("Aucun suivi trouve pour cet animal sur la periode selectionnee !");
                        progressIndicator.setVisible(false);
                        btnGenerer.setDisable(false);
                        lblStatut.setText("");
                    });
                    return;
                }

                // 3. Calculer les statistiques
                StatsMedicales stats = calculerStats(suivisFiltres);

                // 4. Construire le prompt
                String prompt = construirePromptResume(animal, suivisFiltres, stats, periode);

                // 5. Appeler Groq
                String reponseIA = appellerGroq(prompt);

                // 6. Afficher
                Platform.runLater(() -> {
                    afficherResultats(animal, suivisFiltres, stats, reponseIA, periode);
                    progressIndicator.setVisible(false);
                    btnGenerer.setDisable(false);
                    lblStatut.setText("✅ Resume genere avec succes !");
                    lblStatut.setStyle("-fx-text-fill:#2e7d32;-fx-font-weight:bold;");
                    resultBox.setVisible(true);
                });

            } catch (Exception e) {
                Platform.runLater(() -> {
                    showAlert("Erreur : " + e.getMessage());
                    progressIndicator.setVisible(false);
                    btnGenerer.setDisable(false);
                    lblStatut.setText("Erreur");
                    lblStatut.setStyle("-fx-text-fill:#c62828;");
                });
            }
        }).start();
    }

    // ════════════════════════════════════════
    //  FILTRER PAR PÉRIODE
    // ════════════════════════════════════════
    private List<SuiviAnimal> filtrerParPeriode(List<SuiviAnimal> suivis, String periode) {
        if ("Tout l'historique".equals(periode)) return suivis;

        int jours = switch (periode) {
            case "7 derniers jours"  -> 7;
            case "30 derniers jours" -> 30;
            case "3 derniers mois"   -> 90;
            case "6 derniers mois"   -> 180;
            case "1 an"              -> 365;
            default                  -> 30;
        };

        long cutoff = System.currentTimeMillis() - (long) jours * 24 * 60 * 60 * 1000;
        return suivis.stream()
                .filter(s -> s.getDateSuivi() != null && s.getDateSuivi().getTime() >= cutoff)
                .sorted(Comparator.comparing(SuiviAnimal::getDateSuivi))
                .collect(Collectors.toList());
    }

    // ════════════════════════════════════════
    //  CALCULER STATISTIQUES
    // ════════════════════════════════════════
    private static class StatsMedicales {
        double moyTemp, minTemp, maxTemp;
        double moyPoids, minPoids, maxPoids;
        double moyRythme, minRythme, maxRythme;
        long nbBon, nbMalade, nbCritique;
        String tendancePoids; // "stable", "hausse", "baisse"
        String etatDominant;
        int scoreSante; // 0-100
    }

    private StatsMedicales calculerStats(List<SuiviAnimal> suivis) {
        StatsMedicales s = new StatsMedicales();

        s.moyTemp   = suivis.stream().mapToDouble(SuiviAnimal::getTemperature).average().orElse(0);
        s.minTemp   = suivis.stream().mapToDouble(SuiviAnimal::getTemperature).min().orElse(0);
        s.maxTemp   = suivis.stream().mapToDouble(SuiviAnimal::getTemperature).max().orElse(0);

        s.moyPoids  = suivis.stream().mapToDouble(SuiviAnimal::getPoids).average().orElse(0);
        s.minPoids  = suivis.stream().mapToDouble(SuiviAnimal::getPoids).min().orElse(0);
        s.maxPoids  = suivis.stream().mapToDouble(SuiviAnimal::getPoids).max().orElse(0);

        s.moyRythme = suivis.stream().mapToInt(SuiviAnimal::getRythmeCardiaque).average().orElse(0);
        s.minRythme = suivis.stream().mapToInt(SuiviAnimal::getRythmeCardiaque).min().orElse(0);
        s.maxRythme = suivis.stream().mapToInt(SuiviAnimal::getRythmeCardiaque).max().orElse(0);

        s.nbBon      = suivis.stream().filter(x -> "Bon".equals(x.getEtatSante())).count();
        s.nbMalade   = suivis.stream().filter(x -> "Malade".equals(x.getEtatSante())).count();
        s.nbCritique = suivis.stream().filter(x -> "Critique".equals(x.getEtatSante())).count();

        // Tendance poids
        if (suivis.size() >= 2) {
            double premier = suivis.get(0).getPoids();
            double dernier = suivis.get(suivis.size() - 1).getPoids();
            double diff = dernier - premier;
            if (Math.abs(diff) < premier * 0.02) s.tendancePoids = "stable";
            else if (diff > 0) s.tendancePoids = "en hausse (+%.1f kg)".formatted(diff);
            else s.tendancePoids = "en baisse (%.1f kg)".formatted(diff);
        } else {
            s.tendancePoids = "stable";
        }

        // État dominant
        if (s.nbCritique > 0) s.etatDominant = "Critique";
        else if (s.nbMalade > s.nbBon) s.etatDominant = "Malade";
        else s.etatDominant = "Bon";

        // Score santé
        int score = 100;
        score -= (int)(s.nbCritique * 20);
        score -= (int)(s.nbMalade * 10);
        if (s.maxTemp > 40.5) score -= 10;
        if (s.minTemp < 37.0) score -= 10;
        s.scoreSante = Math.max(0, Math.min(100, score));

        return s;
    }

    // ════════════════════════════════════════
    //  CONSTRUIRE PROMPT RÉSUMÉ
    // ════════════════════════════════════════
    private String construirePromptResume(Animal animal, List<SuiviAnimal> suivis,
                                           StatsMedicales stats, String periode) {
        String style = nettoyer(styleCombo.getValue());
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");

        StringBuilder historique = new StringBuilder();
        int max = Math.min(suivis.size(), 10); // max 10 suivis dans le prompt
        for (int i = 0; i < max; i++) {
            SuiviAnimal sv = suivis.get(i);
            historique.append(sdf.format(sv.getDateSuivi()))
                    .append(" : Temp=").append(sv.getTemperature()).append("C")
                    .append(", Poids=").append(sv.getPoids()).append("kg")
                    .append(", Rythme=").append(sv.getRythmeCardiaque()).append("bpm")
                    .append(", Etat=").append(sv.getEtatSante())
                    .append(sv.getRemarque() != null && !sv.getRemarque().isEmpty()
                            ? ", Note=" + nettoyer(sv.getRemarque()) : "")
                    .append("\\n");
        }

        String sections = "";
        if (checkConclusion.isSelected())      sections += "- Conclusion medicale globale\\n";
        if (checkPrognostic.isSelected())       sections += "- Pronostic a court et moyen terme\\n";
        if (checkRecommandations.isSelected())  sections += "- Recommandations veterinaires pratiques\\n";

        return "Tu es un veterinaire expert. Redige un " + style + " en francais pour l animal suivant.\\n\\n"
                + "=== ANIMAL ===\\n"
                + "Code : " + nettoyer(animal.getCodeAnimal()) + "\\n"
                + "Espece : " + nettoyer(animal.getEspece()) + " | Race : " + nettoyer(animal.getRace())
                + " | Sexe : " + nettoyer(animal.getSexe()) + "\\n\\n"
                + "=== STATISTIQUES SUR " + nettoyer(periode) + " (" + suivis.size() + " suivis) ===\\n"
                + "Temperature : moy=" + String.format("%.1f", stats.moyTemp)
                + "C, min=" + stats.minTemp + "C, max=" + stats.maxTemp + "C\\n"
                + "Poids : moy=" + String.format("%.1f", stats.moyPoids)
                + "kg, tendance=" + stats.tendancePoids + "\\n"
                + "Rythme cardiaque : moy=" + String.format("%.0f", stats.moyRythme)
                + "bpm, min=" + stats.minRythme + ", max=" + stats.maxRythme + "\\n"
                + "Etats de sante : Bon=" + stats.nbBon
                + ", Malade=" + stats.nbMalade + ", Critique=" + stats.nbCritique + "\\n\\n"
                + "=== HISTORIQUE DES SUIVIS ===\\n"
                + historique + "\\n"
                + "=== RAPPORT DEMANDE ===\\n"
                + "Redige un rapport medical narratif complet incluant :\\n"
                + "## RESUME CLINIQUE\\n"
                + "Synthese narrative de l evolution de l animal sur la periode.\\n\\n"
                + "## ANALYSE DES CONSTANTES\\n"
                + "Analyse detaillee temperature, poids, rythme cardiaque avec interpretation clinique.\\n\\n"
                + "## EVOLUTION DE L ETAT DE SANTE\\n"
                + "Evolution chronologique et tendances observees.\\n\\n"
                + (sections.isEmpty() ? "" : "Inclure aussi :\\n" + sections)
                + "\\nSois precis, professionnel et utilise un vocabulaire veterinaire adapte.";
    }

    // ════════════════════════════════════════
    //  APPEL GROQ
    // ════════════════════════════════════════
    private String appellerGroq(String prompt) throws Exception {
        String jsonBody = """
                {"model": "%s","messages": [{"role": "user","content": "%s"}],"max_tokens": 2500,"temperature": 0.3}
                """.formatted(GROQ_MODEL,
                prompt.replace("\\", "\\\\").replace("\"", "\\\"")
                        .replace("\n", "\\n").replace("\r", "").replace("\t", " "));

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(GROQ_URL))
                .header("Authorization", "Bearer " + GROQ_API_KEY)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200)
            throw new Exception("Erreur API " + response.statusCode()
                    + "\n" + response.body().substring(0, Math.min(300, response.body().length())));

        String body = response.body();
        int start = body.indexOf("\"content\":\"") + 11;
        if (start < 11) start = body.indexOf("\"content\": \"") + 12;
        int end = start;
        while (end < body.length()) {
            end = body.indexOf("\"", end);
            if (end < 0) break;
            int backslashes = 0;
            int pos = end - 1;
            while (pos >= 0 && body.charAt(pos) == '\\') { backslashes++; pos--; }
            if (backslashes % 2 == 0) break;
            end++;
        }
        if (start < 11 || end < 0 || end <= start)
            throw new Exception("Format reponse invalide");

        return body.substring(start, end)
                .replace("\\n", "\n").replace("\\\"", "\"").replace("\\\\", "\\").trim();
    }

    // ════════════════════════════════════════
    //  AFFICHER RÉSULTATS
    // ════════════════════════════════════════
    private void afficherResultats(Animal animal, List<SuiviAnimal> suivis,
                                    StatsMedicales stats, String reponseIA, String periode) {
        // En-tête animal
        lblAnimalHeader.setText(animal.getCodeAnimal()
                + " — " + animal.getEspece() + " / " + animal.getRace()
                + " | " + animal.getSexe());
        lblNbSuivis.setText(suivis.size() + " suivi(s) analyse(s)");
        lblPeriode.setText("Periode : " + periode);

        // Score santé
        String couleurScore;
        String labelScore;
        if      (stats.scoreSante >= 80) { couleurScore = "#2e7d32"; labelScore = "✅ " + stats.scoreSante + "/100"; }
        else if (stats.scoreSante >= 60) { couleurScore = "#f57c00"; labelScore = "⚠️ " + stats.scoreSante + "/100"; }
        else                             { couleurScore = "#c62828"; labelScore = "🔴 " + stats.scoreSante + "/100"; }
        lblScoreSante.setText(labelScore);
        lblScoreSante.setStyle("-fx-font-size:16px;-fx-font-weight:bold;-fx-text-fill:" + couleurScore + ";");

        // Stats box
        statsBox.getChildren().clear();
        statsBox.getChildren().addAll(
                creerStatLigne("🌡️ Température",
                        String.format("Moy: %.1f°C  |  Min: %.1f°C  |  Max: %.1f°C",
                                stats.moyTemp, stats.minTemp, stats.maxTemp),
                        stats.maxTemp > 40.5 ? "#c62828" : "#2e7d32"),
                creerStatLigne("⚖️ Poids",
                        String.format("Moy: %.1f kg  |  Tendance: %s", stats.moyPoids, stats.tendancePoids),
                        "#1565c0"),
                creerStatLigne("❤️ Rythme cardiaque",
                        String.format("Moy: %.0f bpm  |  Min: %.0f  |  Max: %.0f",
                                stats.moyRythme, stats.minRythme, stats.maxRythme),
                        "#6a1b9a"),
                creerStatLigne("🏥 États de santé",
                        String.format("Bon: %d  |  Malade: %d  |  Critique: %d",
                                stats.nbBon, stats.nbMalade, stats.nbCritique),
                        stats.nbCritique > 0 ? "#c62828" : stats.nbMalade > 0 ? "#f57c00" : "#2e7d32")
        );

        // Rapport IA
        txtResume.setText(reponseIA);
    }

    private HBox creerStatLigne(String label, String valeur, String couleur) {
        Label lblLabel = new Label(label);
        lblLabel.setStyle("-fx-font-weight:bold;-fx-font-size:12px;-fx-text-fill:#555;-fx-min-width:160;");
        Label lblVal = new Label(valeur);
        lblVal.setStyle("-fx-font-size:12px;-fx-text-fill:" + couleur + ";-fx-font-weight:bold;");
        HBox row = new HBox(10, lblLabel, lblVal);
        row.setStyle("-fx-padding:5 0;-fx-border-color:#f0f0f0;-fx-border-width:0 0 1 0;");
        return row;
    }

    // ════════════════════════════════════════
    //  COPIER
    // ════════════════════════════════════════
    @FXML
    void copierResume() {
        String texte = lblAnimalHeader.getText() + "\n"
                + lblNbSuivis.getText() + " | " + lblPeriode.getText() + "\n"
                + "Score Sante : " + lblScoreSante.getText() + "\n\n"
                + txtResume.getText();
        javafx.scene.input.Clipboard cb = javafx.scene.input.Clipboard.getSystemClipboard();
        javafx.scene.input.ClipboardContent cc = new javafx.scene.input.ClipboardContent();
        cc.putString(texte);
        cb.setContent(cc);
        lblStatut.setText("✅ Resume copie !");
        lblStatut.setStyle("-fx-text-fill:#2e7d32;-fx-font-weight:bold;");
    }

    // ════════════════════════════════════════
    //  RESET + NAVIGATION
    // ════════════════════════════════════════
    @FXML void reset() {
        comboAnimal.setValue(null);
        periodeCombo.setValue("30 derniers jours");
        styleCombo.setValue("Rapport veterinaire professionnel");
        checkConclusion.setSelected(true);
        checkPrognostic.setSelected(true);
        checkRecommandations.setSelected(true);
        resultBox.setVisible(false);
        lblStatut.setText("");
    }

    @FXML void navigateBack() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/fxml/ShowAnimals.fxml"));
            NavigationUtil.loadInContentArea(btnGenerer, root);
        } catch (Exception e) { showAlert(e.getMessage()); }
    }

    private String nettoyer(String texte) {
        if (texte == null) return "";
        return texte.replace("é","e").replace("è","e").replace("ê","e").replace("ë","e")
                .replace("à","a").replace("â","a").replace("î","i").replace("ï","i")
                .replace("ô","o").replace("ù","u").replace("û","u").replace("ç","c")
                .replace("É","E").replace("È","E").replace("À","A").replace("Î","I")
                .replace("\"","'").replace("\\", " ")
                .replace("\n"," ").replace("\r","").replace("\t"," ").trim();
    }

    private void showAlert(String msg) {
        new Alert(Alert.AlertType.ERROR, msg).showAndWait();
    }
}
