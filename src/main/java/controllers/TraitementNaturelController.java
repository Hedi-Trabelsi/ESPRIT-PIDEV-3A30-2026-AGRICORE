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
import java.util.List;

public class TraitementNaturelController {

    @FXML private ComboBox<Animal>  comboAnimal;
    @FXML private ComboBox<String>  pathologieCombo;
    @FXML private ComboBox<String>  graviteCombo;
    @FXML private TextArea          symptomesTf;
    @FXML private CheckBox          checkBio;
    @FXML private CheckBox          checkUrgent;

    @FXML private VBox              resultBox;
    @FXML private Label             lblAnimalInfo;
    @FXML private Label             lblPathologie;
    @FXML private ProgressIndicator progressIndicator;
    @FXML private Button            btnAnalyser;
    @FXML private VBox              traitementsBox;
    @FXML private VBox              plantesBox;
    @FXML private VBox              preventionBox;
    @FXML private TextArea          txtConseils;

    private final AnimalService      animalService = new AnimalService();
    private final SuiviAnimalService suiviService  = new SuiviAnimalService();

    private static final String GROQ_API_KEY = "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";
    private static final String GROQ_URL     = "https://api.groq.com/openai/v1/chat/completions";
    private static final String GROQ_MODEL   = "llama3-70b-8192";

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
            comboAnimal.setOnAction(e -> autoRemplir());
        } catch (SQLException e) { showAlert(e.getMessage()); }

        pathologieCombo.setItems(FXCollections.observableArrayList(
                "Parasites intestinaux (vers)",
                "Infection respiratoire / Toux",
                "Diarrhee / Troubles digestifs",
                "Blessure / Plaie cutanee",
                "Inflammation / Arthrite",
                "Mammite / Infection mammaire",
                "Stress / Anxiete",
                "Manque appetit / Anorexie",
                "Probleme de peau / Dermatite",
                "Fatigue / Faiblesse generale",
                "Fievre legere",
                "Parasites externes (puces, tiques)",
                "Constipation",
                "Coliques legeres",
                "Autre (decrire dans symptomes)"
        ));

        graviteCombo.setItems(FXCollections.observableArrayList(
                "Legere - Symptomes discrets",
                "Moderee - Surveillance requise",
                "Severe - Intervention necessaire"
        ));
        graviteCombo.setValue("Legere - Symptomes discrets");
        checkBio.setSelected(true);
    }

    // ════════════════════════════════════════
    //  AUTO-REMPLIR
    // ════════════════════════════════════════
    private void autoRemplir() {
        Animal a = comboAnimal.getValue();
        if (a == null) return;
        try {
            List<SuiviAnimal> suivis = suiviService.readByAnimal(a.getIdAnimal());
            if (!suivis.isEmpty()) {
                SuiviAnimal dernier = suivis.get(suivis.size() - 1);
                if ("Malade".equals(dernier.getEtatSante()) ||
                        "Critique".equals(dernier.getEtatSante())) {
                    graviteCombo.setValue("Moderee - Surveillance requise");
                }
                if (dernier.getRemarque() != null && !dernier.getRemarque().isEmpty()) {
                    symptomesTf.setText(dernier.getRemarque());
                }
            }
        } catch (Exception ex) { ex.printStackTrace(); }
    }

    // ════════════════════════════════════════
    //  ANALYSER
    // ════════════════════════════════════════
    @FXML
    void analyser() {
        if (comboAnimal.getValue() == null)     { showAlert("Choisissez un animal !"); return; }
        if (pathologieCombo.getValue() == null) { showAlert("Choisissez une pathologie !"); return; }

        Animal animal     = comboAnimal.getValue();
        String pathologie = pathologieCombo.getValue();
        String gravite    = graviteCombo.getValue();
        String symptomes  = symptomesTf.getText().trim();
        boolean bio       = checkBio.isSelected();
        boolean urgent    = checkUrgent.isSelected();

        btnAnalyser.setDisable(true);
        progressIndicator.setVisible(true);
        resultBox.setVisible(false);

        String prompt = construirePrompt(animal, pathologie, gravite, symptomes, bio, urgent);

        new Thread(() -> {
            try {
                String reponseIA = appellerGroq(prompt);
                Platform.runLater(() -> {
                    afficherResultat(animal, pathologie, reponseIA);
                    progressIndicator.setVisible(false);
                    btnAnalyser.setDisable(false);
                    resultBox.setVisible(true);
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    showAlert("Erreur API Groq : " + e.getMessage());
                    progressIndicator.setVisible(false);
                    btnAnalyser.setDisable(false);
                });
            }
        }).start();
    }

    // ════════════════════════════════════════
    //  CONSTRUIRE PROMPT — SANS CARACTÈRES SPÉCIAUX
    // ════════════════════════════════════════
    private String construirePrompt(Animal animal, String pathologie,
                                    String gravite, String symptomes,
                                    boolean bio, boolean urgent) {
        // ✅ Nettoyer TOUS les textes pour éviter l'erreur 400
        String espece     = nettoyer(animal.getEspece());
        String race       = nettoyer(animal.getRace());
        String code       = nettoyer(animal.getCodeAnimal());
        String patho      = nettoyer(pathologie);
        String grav       = nettoyer(gravite);
        String symp       = nettoyer(symptomes);

        return "Tu es un expert veterinaire specialise en medecine naturelle animale. "
                + "Reponds UNIQUEMENT en francais. Sois precis et structure.\\n\\n"
                + "=== CAS CLINIQUE ===\\n"
                + "Animal : " + espece + " (race : " + race + ")\\n"
                + "Code : " + code + "\\n"
                + "Pathologie : " + patho + "\\n"
                + "Gravite : " + grav + "\\n"
                + (symp.isEmpty() ? "" : "Symptomes : " + symp + "\\n")
                + "Elevage bio : " + (bio ? "OUI" : "NON") + "\\n"
                + "Urgent : " + (urgent ? "OUI" : "NON") + "\\n\\n"
                + "=== DEMANDE ===\\n"
                + "Reponds avec EXACTEMENT ces 5 sections (titres en majuscules) :\\n\\n"
                + "## TRAITEMENTS NATURELS\\n"
                + "Liste 3 a 5 traitements naturels avec dosages precis.\\n\\n"
                + "## PLANTES ET REMEDES\\n"
                + "Liste les plantes medicinales et remedes naturels avec mode utilisation.\\n\\n"
                + "## PREVENTION\\n"
                + "Liste 3 mesures preventives pour eviter la recidive.\\n\\n"
                + "## CONSEILS PRATIQUES\\n"
                + "Conseils concrets pour l eleveur.\\n\\n"
                + "## AVERTISSEMENT\\n"
                + "Limites des traitements naturels et quand consulter un veterinaire.";
    }

    // ════════════════════════════════════════
    //  ✅ NETTOYER TEXTE — Supprimer accents et caractères spéciaux
    // ════════════════════════════════════════
    private String nettoyer(String texte) {
        if (texte == null) return "";
        return texte
                // Supprimer accents
                .replace("é", "e").replace("è", "e").replace("ê", "e").replace("ë", "e")
                .replace("à", "a").replace("â", "a").replace("ä", "a")
                .replace("î", "i").replace("ï", "i")
                .replace("ô", "o").replace("ö", "o")
                .replace("ù", "u").replace("û", "u").replace("ü", "u")
                .replace("ç", "c")
                .replace("É", "E").replace("È", "E").replace("Ê", "E")
                .replace("À", "A").replace("Â", "A")
                .replace("Î", "I").replace("Ô", "O").replace("Ù", "U")
                // Supprimer caractères JSON dangereux
                .replace("\"", "'")
                .replace("\\", " ")
                .replace("\n", " ")
                .replace("\r", " ")
                .replace("\t", " ")
                // Supprimer caractères de contrôle
                .replaceAll("[\\p{Cntrl}&&[^\n\t]]", "")
                .trim();
    }

    // ════════════════════════════════════════
    //  APPEL GROQ API — JSON construit manuellement
    // ════════════════════════════════════════
    private String appellerGroq(String prompt) throws Exception {

        // ✅ Corps JSON avec prompt déjà nettoyé (pas de replace ici)
        String jsonBody = "{"
                + "\"model\":\"" + GROQ_MODEL + "\","
                + "\"messages\":["
                + "{"
                + "\"role\":\"user\","
                + "\"content\":\"" + prompt + "\""
                + "}"
                + "],"
                + "\"max_tokens\":2000,"
                + "\"temperature\":0.3"
                + "}";

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(GROQ_URL))
                .header("Content-Type", "application/json; charset=utf-8")
                .header("Authorization", "Bearer " + GROQ_API_KEY)
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response = client.send(request,
                HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 400) {
            // Afficher le détail de l'erreur 400 pour debug
            System.out.println("Erreur 400 body : " + response.body());
            throw new Exception("Requete invalide (400). Details : " + response.body().substring(0, Math.min(200, response.body().length())));
        }
        if (response.statusCode() == 401)
            throw new Exception("Cle API invalide - Verifiez sur console.groq.com");
        if (response.statusCode() == 429)
            throw new Exception("Limite atteinte - Reessayez dans 1 minute");
        if (response.statusCode() != 200)
            throw new Exception("Erreur API : " + response.statusCode());

        String body = response.body();

        // Parser la réponse
        int start = body.indexOf("\"content\":\"") + 11;
        int end   = body.lastIndexOf("\"}]");
        if (start < 11 || end < 0) {
            // Essayer autre format
            start = body.indexOf("\"content\": \"") + 12;
            end   = body.lastIndexOf("\"}]");
        }
        if (start < 11 || end < 0)
            throw new Exception("Reponse API invalide - format inattendu");

        return body.substring(start, end)
                .replace("\\n", "\n")
                .replace("\\\"", "\"");
    }

    // ════════════════════════════════════════
    //  AFFICHER RÉSULTAT
    // ════════════════════════════════════════
    private void afficherResultat(Animal animal, String pathologie, String reponseIA) {
        lblAnimalInfo.setText("🐾 " + animal.getCodeAnimal()
                + " — " + animal.getEspece() + " / " + animal.getRace());
        lblPathologie.setText("🌿 " + pathologie);

        String[] sections = {
                "## TRAITEMENTS NATURELS",
                "## PLANTES ET REMEDES",
                "## PREVENTION",
                "## CONSEILS PRATIQUES",
                "## AVERTISSEMENT"
        };

        String traitements   = extraireSection(reponseIA, sections[0], sections[1]);
        String plantes       = extraireSection(reponseIA, sections[1], sections[2]);
        String prevention    = extraireSection(reponseIA, sections[2], sections[3]);
        String conseils      = extraireSection(reponseIA, sections[3], sections[4]);
        String avertissement = extraireSection(reponseIA, sections[4], null);

        remplirSection(traitementsBox, traitements, "#2e7d32");
        remplirSection(plantesBox,     plantes,     "#1565c0");
        remplirSection(preventionBox,  prevention,  "#6a1b9a");

        txtConseils.setText(
                "CONSEILS PRATIQUES :\n" + conseils.trim()
                        + "\n\nAVERTISSEMENT :\n" + avertissement.trim()
                        + "\n\n---\nCes recommandations sont generees par IA.\n"
                        + "Consultez toujours un veterinaire pour un diagnostic officiel.");
    }

    private String extraireSection(String texte, String debut, String fin) {
        int start = texte.indexOf(debut);
        if (start < 0) {
            // Essayer sans ##
            String debutSimple = debut.replace("## ", "");
            start = texte.indexOf(debutSimple);
            if (start < 0) return "Information non disponible.";
            start += debutSimple.length();
        } else {
            start += debut.length();
        }

        int end = fin != null ? texte.indexOf(fin, start) : texte.length();
        if (end < 0) end = texte.length();

        return texte.substring(start, end).trim();
    }

    private void remplirSection(VBox box, String contenu, String couleur) {
        box.getChildren().clear();
        if (contenu == null || contenu.isEmpty()) return;

        for (String ligne : contenu.split("\n")) {
            ligne = ligne.trim();
            if (ligne.isEmpty()) continue;

            Label lbl = new Label(ligne);
            lbl.setWrapText(true);
            lbl.setMaxWidth(Double.MAX_VALUE);

            if (ligne.startsWith("-") || ligne.startsWith("•") || ligne.startsWith("*")) {
                lbl.setStyle("-fx-text-fill:" + couleur + ";-fx-font-size:12px;-fx-padding:2 0 2 10;");
            } else if (ligne.matches("^\\d+\\..*")) {
                lbl.setStyle("-fx-text-fill:" + couleur + ";-fx-font-size:12px;"
                        + "-fx-font-weight:bold;-fx-padding:4 0 2 0;");
            } else {
                lbl.setStyle("-fx-text-fill:#333;-fx-font-size:12px;-fx-padding:2 0;");
            }
            box.getChildren().add(lbl);
        }
    }

    // ════════════════════════════════════════
    //  RESET + NAVIGATION
    // ════════════════════════════════════════
    @FXML void reset() {
        comboAnimal.setValue(null);
        pathologieCombo.setValue(null);
        graviteCombo.setValue("Legere - Symptomes discrets");
        symptomesTf.clear();
        checkBio.setSelected(true);
        checkUrgent.setSelected(false);
        resultBox.setVisible(false);
    }

    @FXML void navigateBack() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/ShowAnimals.fxml"));
            Stage stage = (Stage) btnAnalyser.getScene().getWindow();
            stage.getScene().setRoot(root);
        } catch (Exception e) { showAlert(e.getMessage()); }
    }

    private void showAlert(String msg) {
        new Alert(Alert.AlertType.ERROR, msg).showAndWait();
    }
}