package controllers;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import models.Animal;
import models.SuiviAnimal;
import services.AnimalService;
import services.SuiviAnimalService;

import java.net.URI;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
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

    // ── Résultat ──
    @FXML private VBox              resultBox;
    @FXML private Label             lblAnimalHeader;
    @FXML private Label             lblNbSuivis;
    @FXML private Label             lblPeriode;
    @FXML private Label             lblScoreSante;
    @FXML private VBox              statsBox;
    @FXML private TextArea          txtResume;
    @FXML private ProgressIndicator progressIndicator;
    @FXML private Button            btnGenerer;
    @FXML private Label             lblStatut;

    private final AnimalService      animalService = new AnimalService();
    private final SuiviAnimalService suiviService  = new SuiviAnimalService();

    private static final String GROQ_API_KEY = "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";
    private static final String GROQ_URL     = "https://api.groq.com/openai/v1/chat/completions";
    private static final String GROQ_MODEL = "llama-3.1-8b-instant";

    private final SimpleDateFormat sdf     = new SimpleDateFormat("dd/MM/yyyy");
    private final SimpleDateFormat sdfFull = new SimpleDateFormat("dd/MM/yyyy HH:mm");

    // ════════════════════════════════════════
    //  INITIALIZE
    // ════════════════════════════════════════
    @FXML
    void initialize() {
        resultBox.setVisible(false);
        progressIndicator.setVisible(false);

        try {
            List<Animal> animaux = animalService.read();
            comboAnimal.setItems(FXCollections.observableArrayList(animaux));
            comboAnimal.setCellFactory(p -> new ListCell<>() {
                @Override protected void updateItem(Animal a, boolean empty) {
                    super.updateItem(a, empty);
                    setText(empty || a == null ? null
                            : a.getCodeAnimal() + " - " + a.getEspece() + " / " + a.getRace());
                }
            });
            comboAnimal.setButtonCell(new ListCell<>() {
                @Override protected void updateItem(Animal a, boolean empty) {
                    super.updateItem(a, empty);
                    setText(empty || a == null ? null
                            : a.getCodeAnimal() + " - " + a.getEspece());
                }
            });
        } catch (SQLException e) { showAlert(e.getMessage()); }

        periodeCombo.setItems(FXCollections.observableArrayList(
                "Tous les suivis",
                "30 derniers jours",
                "3 derniers mois",
                "6 derniers mois",
                "Dernière année"
        ));
        periodeCombo.setValue("Tous les suivis");

        styleCombo.setItems(FXCollections.observableArrayList(
                "Rapport vétérinaire officiel",
                "Résumé simplifié éleveur",
                "Fiche technique concise",
                "Rapport détaillé scientifique"
        ));
        styleCombo.setValue("Rapport vétérinaire officiel");

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

        try {
            List<SuiviAnimal> tousLesSuivis = suiviService.readByAnimal(animal.getIdAnimal());
            if (tousLesSuivis.isEmpty()) {
                showAlert("Aucun suivi enregistre pour cet animal !");
                return;
            }

            // Trier chronologiquement
            tousLesSuivis.sort(Comparator.comparing(SuiviAnimal::getDateSuivi));

            // Filtrer par période
            List<SuiviAnimal> suivis = filtrerParPeriode(tousLesSuivis, periodeCombo.getValue());
            if (suivis.isEmpty()) {
                showAlert("Aucun suivi dans la periode selectionnee !");
                return;
            }

            // Afficher statistiques rapides
            afficherStats(animal, suivis);

            // Lancer génération IA
            btnGenerer.setDisable(true);
            progressIndicator.setVisible(true);
            lblStatut.setText("⏳ Groq IA analyse l'historique medical...");
            txtResume.setText("");
            resultBox.setVisible(true);

            String prompt = construirePrompt(animal, suivis);

            new Thread(() -> {
                try {
                    String reponse = appellerGroq(prompt);
                    Platform.runLater(() -> {
                        txtResume.setText(reponse);
                        progressIndicator.setVisible(false);
                        btnGenerer.setDisable(false);
                        lblStatut.setText("✅ Resume genere avec succes !");
                        lblStatut.setStyle("-fx-text-fill:#2e7d32;-fx-font-weight:bold;");
                    });
                } catch (Exception e) {
                    Platform.runLater(() -> {
                        showAlert("Erreur Groq : " + e.getMessage());
                        progressIndicator.setVisible(false);
                        btnGenerer.setDisable(false);
                        lblStatut.setText("❌ Erreur de generation");
                        lblStatut.setStyle("-fx-text-fill:#c62828;");
                    });
                }
            }).start();

        } catch (SQLException e) { showAlert(e.getMessage()); }
    }

    // ════════════════════════════════════════
    //  AFFICHER STATISTIQUES RAPIDES
    // ════════════════════════════════════════
    private void afficherStats(Animal animal, List<SuiviAnimal> suivis) {
        SuiviAnimal premier = suivis.get(0);
        SuiviAnimal dernier = suivis.get(suivis.size() - 1);

        lblAnimalHeader.setText(animal.getCodeAnimal()
                + " — " + animal.getEspece() + " / " + animal.getRace()
                + " (" + animal.getSexe() + ")");

        lblNbSuivis.setText(suivis.size() + " suivi(s) analysé(s)");

        lblPeriode.setText(sdf.format(premier.getDateSuivi())
                + " → " + sdf.format(dernier.getDateSuivi()));

        // Score santé moyen
        int score = calculerScoreMoyen(suivis);
        String couleur = score >= 75 ? "#2e7d32" : score >= 50 ? "#f57c00" : "#c62828";
        String statut  = score >= 75 ? "BON" : score >= 50 ? "MOYEN" : "CRITIQUE";
        lblScoreSante.setText(score + "/100 — " + statut);
        lblScoreSante.setStyle("-fx-font-weight:bold;-fx-font-size:14px;-fx-text-fill:" + couleur);

        // Stats détaillées
        statsBox.getChildren().clear();
        double tempMoy  = suivis.stream().mapToDouble(SuiviAnimal::getTemperature).average().orElse(0);
        double tempMin  = suivis.stream().mapToDouble(SuiviAnimal::getTemperature).min().orElse(0);
        double tempMax  = suivis.stream().mapToDouble(SuiviAnimal::getTemperature).max().orElse(0);
        double poidsMoy = suivis.stream().mapToDouble(SuiviAnimal::getPoids).average().orElse(0);
        double poidsMin = suivis.stream().mapToDouble(SuiviAnimal::getPoids).min().orElse(0);
        double poidsMax = suivis.stream().mapToDouble(SuiviAnimal::getPoids).max().orElse(0);

        long nbBon      = suivis.stream().filter(s -> "Bon".equals(s.getEtatSante())).count();
        long nbMalade   = suivis.stream().filter(s -> "Malade".equals(s.getEtatSante())).count();
        long nbCritique = suivis.stream().filter(s -> "Critique".equals(s.getEtatSante())).count();

        ajouterStatLigne("🌡️ Température", String.format("moy: %.1f°C  |  min: %.1f°C  |  max: %.1f°C",
                tempMoy, tempMin, tempMax));
        ajouterStatLigne("⚖️ Poids", String.format("moy: %.1f kg  |  min: %.1f kg  |  max: %.1f kg",
                poidsMoy, poidsMin, poidsMax));
        ajouterStatLigne("🏥 États de santé", String.format("✅ Bon: %d  |  ⚠️ Malade: %d  |  🚨 Critique: %d",
                nbBon, nbMalade, nbCritique));
        ajouterStatLigne("📅 Dernier état", dernier.getEtatSante()
                + " | " + sdfFull.format(dernier.getDateSuivi()));
    }

    private void ajouterStatLigne(String label, String valeur) {
        HBox row = new HBox(12);
        row.setStyle("-fx-padding:6 0;");
        Label lbl = new Label(label);
        lbl.setStyle("-fx-font-weight:bold;-fx-text-fill:#555;-fx-min-width:140;-fx-font-size:12px;");
        Label val = new Label(valeur);
        val.setStyle("-fx-text-fill:#333;-fx-font-size:12px;");
        row.getChildren().addAll(lbl, val);
        statsBox.getChildren().add(row);
    }

    private int calculerScoreMoyen(List<SuiviAnimal> suivis) {
        return (int) suivis.stream().mapToInt(s -> {
            int score = 100;
            if (s.getTemperature() > 40.0 || s.getTemperature() < 37.5) score -= 30;
            else if (s.getTemperature() > 39.5) score -= 15;
            if ("Critique".equals(s.getEtatSante())) score -= 35;
            else if ("Malade".equals(s.getEtatSante())) score -= 15;
            if ("Faible".equals(s.getNiveauActivite())) score -= 10;
            return Math.max(0, score);
        }).average().orElse(0);
    }

    // ════════════════════════════════════════
    //  FILTRER PAR PÉRIODE
    // ════════════════════════════════════════
    private List<SuiviAnimal> filtrerParPeriode(List<SuiviAnimal> suivis, String periode) {
        if ("Tous les suivis".equals(periode)) return suivis;

        Calendar cal = Calendar.getInstance();
        switch (periode) {
            case "30 derniers jours"  -> cal.add(Calendar.DAY_OF_YEAR, -30);
            case "3 derniers mois"    -> cal.add(Calendar.MONTH, -3);
            case "6 derniers mois"    -> cal.add(Calendar.MONTH, -6);
            case "Dernière année"     -> cal.add(Calendar.YEAR, -1);
            default -> { return suivis; }
        }
        Date limite = cal.getTime();
        return suivis.stream()
                .filter(s -> s.getDateSuivi().after(limite))
                .collect(Collectors.toList());
    }

    // ════════════════════════════════════════
    //  CONSTRUIRE PROMPT MÉDICAL
    // ════════════════════════════════════════
    private String construirePrompt(Animal animal, List<SuiviAnimal> suivis) {
        SuiviAnimal premier = suivis.get(0);
        SuiviAnimal dernier = suivis.get(suivis.size() - 1);

        double tempMoy  = suivis.stream().mapToDouble(SuiviAnimal::getTemperature).average().orElse(0);
        double tempMin  = suivis.stream().mapToDouble(SuiviAnimal::getTemperature).min().orElse(0);
        double tempMax  = suivis.stream().mapToDouble(SuiviAnimal::getTemperature).max().orElse(0);
        double poidsMoy = suivis.stream().mapToDouble(SuiviAnimal::getPoids).average().orElse(0);
        double poidsMin = suivis.stream().mapToDouble(SuiviAnimal::getPoids).min().orElse(0);
        double poidsMax = suivis.stream().mapToDouble(SuiviAnimal::getPoids).max().orElse(0);
        double rythmeMoy = suivis.stream().mapToDouble(SuiviAnimal::getRythmeCardiaque).average().orElse(0);

        long nbBon      = suivis.stream().filter(s -> "Bon".equals(s.getEtatSante())).count();
        long nbMalade   = suivis.stream().filter(s -> "Malade".equals(s.getEtatSante())).count();
        long nbCritique = suivis.stream().filter(s -> "Critique".equals(s.getEtatSante())).count();

        // Tendance poids
        String tendancePoids = poidsMax - poidsMin < 1 ? "stable"
                : dernier.getPoids() > premier.getPoids() ? "en progression"
                : "en diminution";

        // Tendance température
        String tendanceTemp = dernier.getTemperature() > tempMoy + 0.5 ? "en hausse recente"
                : dernier.getTemperature() < tempMoy - 0.5 ? "en baisse recente"
                : "stable";

        // Remarques importantes (derniers 3 suivis)
        String remarques = suivis.stream()
                .filter(s -> s.getRemarque() != null && !s.getRemarque().trim().isEmpty())
                .map(s -> "- " + nettoyer(s.getRemarque()))
                .limit(5)
                .collect(Collectors.joining("\\n"));

        String style = nettoyer(styleCombo.getValue());
        boolean avecConclusion     = checkConclusion.isSelected();
        boolean avecPrognostic     = checkPrognostic.isSelected();
        boolean avecRecommandations = checkRecommandations.isSelected();

        StringBuilder sb = new StringBuilder();
        sb.append("Tu es un veterinaire expert. Redige un ").append(style);
        sb.append(" en francais pour l animal suivant.\\n");
        sb.append("Utilise un langage medical professionnel. Sois precis et structure.\\n\\n");

        sb.append("=== IDENTITE DE L ANIMAL ===\\n");
        sb.append("Code : ").append(nettoyer(animal.getCodeAnimal())).append("\\n");
        sb.append("Espece : ").append(nettoyer(animal.getEspece())).append("\\n");
        sb.append("Race : ").append(nettoyer(animal.getRace())).append("\\n");
        sb.append("Sexe : ").append(nettoyer(animal.getSexe())).append("\\n\\n");

        sb.append("=== HISTORIQUE MEDICAL (").append(suivis.size()).append(" suivis) ===\\n");
        sb.append("Periode : du ").append(sdf.format(premier.getDateSuivi()));
        sb.append(" au ").append(sdf.format(dernier.getDateSuivi())).append("\\n\\n");

        sb.append("Parametres vitaux observes :\\n");
        sb.append("- Temperature : moyenne ").append(String.format("%.1f", tempMoy));
        sb.append("C, min ").append(String.format("%.1f", tempMin));
        sb.append("C, max ").append(String.format("%.1f", tempMax)).append("C (tendance : ").append(tendanceTemp).append(")\\n");
        sb.append("- Poids : moyenne ").append(String.format("%.1f", poidsMoy));
        sb.append(" kg, min ").append(String.format("%.1f", poidsMin));
        sb.append(" kg, max ").append(String.format("%.1f", poidsMax)).append(" kg (tendance : ").append(tendancePoids).append(")\\n");
        sb.append("- Rythme cardiaque moyen : ").append(String.format("%.0f", rythmeMoy)).append(" bpm\\n\\n");

        sb.append("Distribution etats de sante :\\n");
        sb.append("- Bon : ").append(nbBon).append(" fois\\n");
        sb.append("- Malade : ").append(nbMalade).append(" fois\\n");
        sb.append("- Critique : ").append(nbCritique).append(" fois\\n\\n");

        sb.append("Etat actuel (dernier suivi le ").append(sdf.format(dernier.getDateSuivi())).append(") :\\n");
        sb.append("- Etat : ").append(nettoyer(dernier.getEtatSante())).append("\\n");
        sb.append("- Temperature : ").append(dernier.getTemperature()).append("C\\n");
        sb.append("- Poids : ").append(dernier.getPoids()).append(" kg\\n");
        sb.append("- Rythme cardiaque : ").append(dernier.getRythmeCardiaque()).append(" bpm\\n");
        sb.append("- Niveau activite : ").append(nettoyer(dernier.getNiveauActivite())).append("\\n");

        if (!remarques.isEmpty()) {
            sb.append("- Observations : ").append(remarques).append("\\n");
        }

        sb.append("\\n=== REDACTION DEMANDEE ===\\n");
        sb.append("Redige le rapport avec ces sections OBLIGATOIRES :\\n\\n");
        sb.append("## IDENTIFICATION DU PATIENT\\n");
        sb.append("Presentation complete de l animal.\\n\\n");
        sb.append("## HISTORIQUE CLINIQUE\\n");
        sb.append("Narration medicale de l evolution de l animal sur la periode. ");
        sb.append("Decris les tendances, les episodes notables, les variations significatives.\\n\\n");
        sb.append("## BILAN PARAMETRES VITAUX\\n");
        sb.append("Analyse detaillee des parametres : temperature, poids, rythme cardiaque.\\n\\n");

        if (avecConclusion) {
            sb.append("## CONCLUSION MEDICALE\\n");
            sb.append("Synthese de l etat de sante global de l animal.\\n\\n");
        }
        if (avecPrognostic) {
            sb.append("## PRONOSTIC\\n");
            sb.append("Evaluation du pronostic a court et moyen terme.\\n\\n");
        }
        if (avecRecommandations) {
            sb.append("## RECOMMANDATIONS\\n");
            sb.append("Actions medicales et de gestion recommandees.\\n");
        }

        return sb.toString();
    }

    // ════════════════════════════════════════
    //  APPEL GROQ API
    // ════════════════════════════════════════
    private String appellerGroq(String prompt) throws Exception {

        com.google.gson.JsonObject requestBody = new com.google.gson.JsonObject();
        requestBody.addProperty("model", GROQ_MODEL);

        com.google.gson.JsonArray messages = new com.google.gson.JsonArray();
        com.google.gson.JsonObject message = new com.google.gson.JsonObject();
        message.addProperty("role", "user");
        message.addProperty("content", prompt);
        messages.add(message);

        requestBody.add("messages", messages);
        requestBody.addProperty("max_tokens", 2500);
        requestBody.addProperty("temperature", 0.4);

        String jsonBody = new com.google.gson.Gson().toJson(requestBody);

        HttpClient client = HttpClient.newHttpClient();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(GROQ_URL))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + GROQ_API_KEY)
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        HttpResponse<String> response =
                client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new Exception("Erreur API " + response.statusCode() + "\n" + response.body());
        }

        // Parser réponse proprement
        com.google.gson.JsonObject responseJson =
                new com.google.gson.Gson().fromJson(response.body(),
                        com.google.gson.JsonObject.class);

        return responseJson
                .getAsJsonArray("choices")
                .get(0)
                .getAsJsonObject()
                .getAsJsonObject("message")
                .get("content")
                .getAsString();
    }

    // ════════════════════════════════════════
    //  COPIER RÉSUMÉ
    // ════════════════════════════════════════
    @FXML
    void copierResume() {
        if (txtResume.getText().isEmpty()) { showAlert("Aucun resume a copier !"); return; }
        javafx.scene.input.Clipboard clipboard = javafx.scene.input.Clipboard.getSystemClipboard();
        javafx.scene.input.ClipboardContent content = new javafx.scene.input.ClipboardContent();
        content.putString(txtResume.getText());
        clipboard.setContent(content);
        lblStatut.setText("✅ Copie dans le presse-papiers !");
        lblStatut.setStyle("-fx-text-fill:#2e7d32;-fx-font-weight:bold;");
    }

    // ════════════════════════════════════════
    //  RESET
    // ════════════════════════════════════════
    @FXML
    void reset() {
        comboAnimal.setValue(null);
        periodeCombo.setValue("Tous les suivis");
        styleCombo.setValue("Rapport veterinaire officiel");
        txtResume.clear();
        lblStatut.setText("");
        resultBox.setVisible(false);
    }

    // ════════════════════════════════════════
    //  UTILITAIRE — Nettoyer texte pour JSON
    // ════════════════════════════════════════
    private String nettoyer(String texte) {
        if (texte == null) return "N/A";
        return texte
                .replace("é","e").replace("è","e").replace("ê","e").replace("ë","e")
                .replace("à","a").replace("â","a").replace("ä","a")
                .replace("î","i").replace("ï","i")
                .replace("ô","o").replace("ö","o")
                .replace("ù","u").replace("û","u").replace("ü","u")
                .replace("ç","c")
                .replace("É","E").replace("È","E").replace("Ê","E")
                .replace("À","A").replace("Â","A").replace("Î","I")
                .replace("Ô","O").replace("Ù","U")
                .replace("\"","'").replace("\\", " ")
                .replace("\n"," ").replace("\r","").replace("\t"," ")
                .trim();
    }

    // ════════════════════════════════════════
    //  NAVIGATION
    // ════════════════════════════════════════
    @FXML
    void navigateBack() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/ShowAnimals.fxml"));
            Stage stage = (Stage) btnGenerer.getScene().getWindow();
            stage.getScene().setRoot(root);
        } catch (Exception e) { showAlert(e.getMessage()); }
    }

    private void showAlert(String msg) {
        new Alert(Alert.AlertType.ERROR, msg).showAndWait();
    }
}
