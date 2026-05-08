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
import java.util.*;

public class OrdonnanceIAController {

    // ── Entrées ──
    @FXML private ComboBox<Animal>  comboAnimal;
    @FXML private TextField         poidsTf;
    @FXML private ComboBox<String>  pathologieCombo;
    @FXML private ComboBox<String>  graviteCombo;
    @FXML private TextArea          symptomesTa;
    @FXML private CheckBox          checkAllergies;
    @FXML private TextField         allergiesTf;
    @FXML private CheckBox          checkGestante;
    @FXML private CheckBox          checkLactante;
    @FXML private ComboBox<String>  ageCombo;

    // ── Résultat ──
    @FXML private VBox              resultBox;
    @FXML private Label             lblEnteteAnimal;
    @FXML private Label             lblEnteteDiag;
    @FXML private Label             lblEnteteDate;
    @FXML private VBox              medicamentsBox;
    @FXML private VBox              posologieBox;
    @FXML private TextArea          txtInstructions;
    @FXML private TextArea          txtMises;
    @FXML private Label             lblDuree;
    @FXML private Label             lblControle;
    @FXML private ProgressIndicator progressIndicator;
    @FXML private Button            btnGenerer;
    @FXML private Label             lblStatut;

    private final AnimalService      animalService = new AnimalService();
    private final SuiviAnimalService suiviService  = new SuiviAnimalService();

    private static final String GROQ_API_KEY = System.getenv("GROQ_API_KEY") != null
            ? System.getenv("GROQ_API_KEY")
            : "VOTRE_CLE_GROQ_ICI";
    private static final String GROQ_URL     = "https://api.groq.com/openai/v1/chat/completions";
    private static final String GROQ_MODEL   = "llama-3.1-8b-instant";

    @FXML
    void initialize() {
        resultBox.setVisible(false);
        progressIndicator.setVisible(false);
        allergiesTf.setDisable(true);
        checkAllergies.setOnAction(e ->
                allergiesTf.setDisable(!checkAllergies.isSelected()));

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
            comboAnimal.setOnAction(e -> autoRemplir());
        } catch (SQLException e) { showAlert(e.getMessage()); }

        pathologieCombo.setItems(FXCollections.observableArrayList(
                "Mammite", "Pneumonie / Infection respiratoire", "Diarrhee infectieuse",
                "Infection uterine (metrite)", "Fievre aphteuse",
                "Infection cutanee / Plaie infectee", "Parasitose intestinale",
                "Boiterie / Arthrite infectieuse", "Infection urinaire",
                "Conjonctivite infectieuse", "Septicemie", "Infection post-operatoire",
                "Endoparasitose", "Ectoparasitose (tiques, puces)",
                "Autre (decrire dans symptomes)"));

        graviteCombo.setItems(FXCollections.observableArrayList(
                "Legere — Traitement ambulatoire",
                "Moderee — Surveillance quotidienne",
                "Severe — Hospitalisation recommandee"));
        graviteCombo.setValue("Moderee — Surveillance quotidienne");

        ageCombo.setItems(FXCollections.observableArrayList(
                "Nouveau-ne (< 1 mois)", "Jeune (1-6 mois)",
                "Juvenil (6-12 mois)", "Adulte (1-7 ans)", "Senior (> 7 ans)"));
        ageCombo.setValue("Adulte (1-7 ans)");
    }

    private void autoRemplir() {
        Animal a = comboAnimal.getValue();
        if (a == null) return;
        try {
            List<SuiviAnimal> suivis = suiviService.readByAnimal(a.getIdAnimal());
            if (!suivis.isEmpty()) {
                suivis.sort(Comparator.comparing(SuiviAnimal::getDateSuivi).reversed());
                SuiviAnimal dernier = suivis.get(0);
                poidsTf.setText(String.valueOf(dernier.getPoids()));
                if (dernier.getRemarque() != null && !dernier.getRemarque().isEmpty())
                    symptomesTa.setText(dernier.getRemarque());
                if ("Malade".equals(dernier.getEtatSante()))
                    graviteCombo.setValue("Moderee — Surveillance quotidienne");
                else if ("Critique".equals(dernier.getEtatSante()))
                    graviteCombo.setValue("Severe — Hospitalisation recommandee");
            }
        } catch (Exception ex) { ex.printStackTrace(); }
    }

    @FXML
    void genererOrdonnance() {
        if (comboAnimal.getValue() == null)     { showAlert("Choisissez un animal !"); return; }
        if (pathologieCombo.getValue() == null) { showAlert("Choisissez une pathologie !"); return; }

        double poids = 0;
        try {
            if (!poidsTf.getText().trim().isEmpty())
                poids = Double.parseDouble(poidsTf.getText().trim().replace(",", "."));
        } catch (NumberFormatException e) { showAlert("Poids invalide !"); return; }
        if (poids <= 0) { showAlert("Entrez un poids valide pour calculer les dosages !"); return; }

        Animal animal = comboAnimal.getValue();
        final double poidsVal = poids;

        btnGenerer.setDisable(true);
        progressIndicator.setVisible(true);
        lblStatut.setText("Groq IA calcule les dosages...");
        resultBox.setVisible(false);

        String prompt = construirePrompt(animal, poidsVal);

        new Thread(() -> {
            try {
                String reponse = appellerGroq(prompt);
                Platform.runLater(() -> {
                    afficherOrdonnance(animal, poidsVal, reponse);
                    progressIndicator.setVisible(false);
                    btnGenerer.setDisable(false);
                    lblStatut.setText("✅ Ordonnance generee !");
                    lblStatut.setStyle("-fx-text-fill:#2e7d32;-fx-font-weight:bold;");
                    resultBox.setVisible(true);
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    showAlert("Erreur Groq : " + e.getMessage());
                    progressIndicator.setVisible(false);
                    btnGenerer.setDisable(false);
                    lblStatut.setText("Erreur");
                    lblStatut.setStyle("-fx-text-fill:#c62828;");
                });
            }
        }).start();
    }

    private String construirePrompt(Animal animal, double poids) {
        String espece    = nettoyer(animal.getEspece());
        String race      = nettoyer(animal.getRace());
        String code      = nettoyer(animal.getCodeAnimal());
        String sexe      = nettoyer(animal.getSexe());
        String patho     = nettoyer(pathologieCombo.getValue());
        String gravite   = nettoyer(graviteCombo.getValue());
        String symptomes = nettoyer(symptomesTa.getText());
        String age       = nettoyer(ageCombo.getValue());
        boolean gestante = checkGestante.isSelected();
        boolean lactante = checkLactante.isSelected();
        String allergies = checkAllergies.isSelected()
                ? nettoyer(allergiesTf.getText()) : "aucune connue";

        return "Tu es un veterinaire expert en pharmacologie animale."
                + " Redige une ordonnance veterinaire COMPLETE et PRECISE en francais.\\n\\n"
                + "=== PATIENT ===\\n"
                + "Code : " + code + "\\n"
                + "Espece : " + espece + " | Race : " + race + " | Sexe : " + sexe + "\\n"
                + "Poids : " + poids + " kg | Age : " + age + "\\n"
                + (gestante ? "GESTANTE : OUI — eviter medicaments teratogenes\\n" : "")
                + (lactante ? "LACTANTE : OUI — respecter temps d attente lait\\n" : "")
                + "Allergies connues : " + allergies + "\\n\\n"
                + "=== DIAGNOSTIC ===\\n"
                + "Pathologie : " + patho + "\\n"
                + "Gravite : " + gravite + "\\n"
                + (symptomes.isEmpty() ? "" : "Symptomes : " + symptomes + "\\n")
                + "\\n=== ORDONNANCE DEMANDEE ===\\n"
                + "Reponds STRICTEMENT avec ces sections :\\n\\n"
                + "## MEDICAMENTS\\n"
                + "Pour chaque medicament liste OBLIGATOIREMENT sur une ligne separee :\\n"
                + "NOM_MEDICAMENT | CLASSE | DOSE_mg_par_kg | DOSE_TOTALE_calculee_pour_" + (int)poids + "kg | FREQUENCE | VOIE_ADMINISTRATION\\n"
                + "Liste 3 a 5 medicaments avec dosages PRECIS calcules pour " + poids + " kg.\\n\\n"
                + "## POSOLOGIE DETAILLEE\\n"
                + "Pour chaque medicament : instructions completes d administration avec horaires.\\n\\n"
                + "## DUREE TRAITEMENT\\n"
                + "Duree exacte en jours et date de fin estimee.\\n\\n"
                + "## CONTROLE VETERINAIRE\\n"
                + "Quand revenir en consultation et signes d alarme a surveiller.\\n\\n"
                + "## INSTRUCTIONS ELEVEUR\\n"
                + "Instructions pratiques pour l eleveur : conservation, manipulation, precautions.\\n\\n"
                + "## MISES EN GARDE\\n"
                + "Contre-indications, effets secondaires possibles, temps d attente viande/lait si applicable.";
    }

    private String appellerGroq(String prompt) throws Exception {
        String jsonBody = """
                {"model": "%s","messages": [{"role": "user","content": "%s"}],"max_tokens": 2000,"temperature": 0.2}
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

    private void afficherOrdonnance(Animal animal, double poids, String reponse) {
        String date = new SimpleDateFormat("dd/MM/yyyy HH:mm").format(new Date());
        lblEnteteAnimal.setText(animal.getCodeAnimal()
                + " | " + animal.getEspece() + " " + animal.getRace()
                + " | " + poids + " kg | " + animal.getSexe());
        lblEnteteDiag.setText(pathologieCombo.getValue() + " — " + graviteCombo.getValue());
        lblEnteteDate.setText("Date : " + date);

        String medicaments  = extraireSection(reponse, "## MEDICAMENTS",          "## POSOLOGIE DETAILLEE");
        String posologie    = extraireSection(reponse, "## POSOLOGIE DETAILLEE",  "## DUREE TRAITEMENT");
        String duree        = extraireSection(reponse, "## DUREE TRAITEMENT",     "## CONTROLE VETERINAIRE");
        String controle     = extraireSection(reponse, "## CONTROLE VETERINAIRE", "## INSTRUCTIONS ELEVEUR");
        String instructions = extraireSection(reponse, "## INSTRUCTIONS ELEVEUR", "## MISES EN GARDE");
        String misesGarde   = extraireSection(reponse, "## MISES EN GARDE",       null);

        // Tableau médicaments
        medicamentsBox.getChildren().clear();
        medicamentsBox.getChildren().add(creerLigneTableau(
                "Medicament", "Classe", "Dose/kg", "Dose totale", "Frequence", "Voie", true, null));
        int idx = 0;
        for (String ligne : medicaments.split("\n")) {
            ligne = ligne.trim();
            if (ligne.isEmpty() || ligne.startsWith("#") || ligne.startsWith("-")) continue;
            if (ligne.contains("|")) {
                String[] parts = ligne.split("\\|");
                if (parts.length >= 4) {
                    String[] couleurs = {"#e8f5e9","#e3f2fd","#f3e5f5","#fff8e1","#fce4ec"};
                    medicamentsBox.getChildren().add(creerLigneTableau(
                            parts.length > 0 ? parts[0].trim() : "",
                            parts.length > 1 ? parts[1].trim() : "",
                            parts.length > 2 ? parts[2].trim() : "",
                            parts.length > 3 ? parts[3].trim() : "",
                            parts.length > 4 ? parts[4].trim() : "",
                            parts.length > 5 ? parts[5].trim() : "",
                            false, couleurs[idx % couleurs.length]));
                    idx++;
                }
            } else {
                Label lbl = new Label(ligne);
                lbl.setStyle("-fx-font-size:12px;-fx-text-fill:#555;-fx-padding:4 8;");
                lbl.setWrapText(true);
                medicamentsBox.getChildren().add(lbl);
            }
        }

        // Posologie
        posologieBox.getChildren().clear();
        for (String ligne : posologie.split("\n")) {
            ligne = ligne.trim();
            if (ligne.isEmpty()) continue;
            Label lbl = new Label(ligne);
            lbl.setStyle(ligne.matches("^\\d+.*") || ligne.startsWith("-")
                    ? "-fx-font-size:12px;-fx-text-fill:#333;-fx-padding:2 0 2 12;"
                    : "-fx-font-size:12px;-fx-text-fill:#1565c0;-fx-font-weight:bold;-fx-padding:6 0 2 0;");
            lbl.setWrapText(true);
            lbl.setMaxWidth(Double.MAX_VALUE);
            posologieBox.getChildren().add(lbl);
        }

        lblDuree.setText(duree.trim().replace("\n", " | "));
        lblControle.setText(controle.trim().replace("\n", " "));
        txtInstructions.setText(instructions.trim());
        txtMises.setText(misesGarde.trim());
    }

    private HBox creerLigneTableau(String col1, String col2, String col3,
                                    String col4, String col5, String col6,
                                    boolean header, String bgColor) {
        HBox row = new HBox(0);
        if (header)               row.setStyle("-fx-background-color:#1a237e;-fx-padding:0;");
        else if (bgColor != null) row.setStyle("-fx-background-color:" + bgColor + ";-fx-padding:0;");
        double[] widths = {170, 120, 80, 130, 100, 100};
        String[] vals   = {col1, col2, col3, col4, col5, col6};
        for (int i = 0; i < vals.length; i++) {
            Label lbl = new Label(vals[i] != null ? vals[i] : "");
            lbl.setPrefWidth(widths[i]);
            lbl.setWrapText(true);
            lbl.setStyle((header
                    ? "-fx-text-fill:white;-fx-font-weight:bold;-fx-font-size:11px;"
                    : "-fx-text-fill:#333;-fx-font-size:11px;")
                    + "-fx-padding:8 6;-fx-border-color:#e0e0e0;-fx-border-width:0 1 1 0;");
            row.getChildren().add(lbl);
        }
        return row;
    }

    @FXML
    void copierOrdonnance() {
        String texte = lblEnteteAnimal.getText() + "\n"
                + lblEnteteDiag.getText() + "\n"
                + lblEnteteDate.getText() + "\n\n"
                + "INSTRUCTIONS :\n" + txtInstructions.getText() + "\n\n"
                + "MISES EN GARDE :\n" + txtMises.getText();
        javafx.scene.input.Clipboard cb = javafx.scene.input.Clipboard.getSystemClipboard();
        javafx.scene.input.ClipboardContent cc = new javafx.scene.input.ClipboardContent();
        cc.putString(texte);
        cb.setContent(cc);
        lblStatut.setText("✅ Ordonnance copiee !");
        lblStatut.setStyle("-fx-text-fill:#2e7d32;-fx-font-weight:bold;");
    }

    private String extraireSection(String texte, String debut, String fin) {
        int start = texte.indexOf(debut);
        if (start < 0) return "";
        start += debut.length();
        int end = fin != null ? texte.indexOf(fin, start) : texte.length();
        if (end < 0) end = texte.length();
        return texte.substring(start, end).trim();
    }

    private String nettoyer(String texte) {
        if (texte == null) return "";
        return texte.replace("é","e").replace("è","e").replace("ê","e").replace("ë","e")
                .replace("à","a").replace("â","a").replace("ä","a")
                .replace("î","i").replace("ï","i").replace("ô","o").replace("ö","o")
                .replace("ù","u").replace("û","u").replace("ü","u").replace("ç","c")
                .replace("É","E").replace("È","E").replace("Ê","E")
                .replace("À","A").replace("Â","A").replace("Î","I")
                .replace("Ô","O").replace("Ù","U")
                .replace("\"","'").replace("\\", " ")
                .replace("\n"," ").replace("\r","").replace("\t"," ").trim();
    }

    @FXML void reset() {
        comboAnimal.setValue(null);
        poidsTf.clear(); symptomesTa.clear(); allergiesTf.clear();
        pathologieCombo.setValue(null);
        graviteCombo.setValue("Moderee — Surveillance quotidienne");
        ageCombo.setValue("Adulte (1-7 ans)");
        checkGestante.setSelected(false);
        checkLactante.setSelected(false);
        checkAllergies.setSelected(false);
        allergiesTf.setDisable(true);
        resultBox.setVisible(false);
        lblStatut.setText("");
    }

    @FXML void navigateBack() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/fxml/ShowAnimals.fxml"));
            NavigationUtil.loadInContentArea(btnGenerer, root);
        } catch (Exception e) { showAlert(e.getMessage()); }
    }

    private void showAlert(String msg) {
        new Alert(Alert.AlertType.ERROR, msg).showAndWait();
    }
}
