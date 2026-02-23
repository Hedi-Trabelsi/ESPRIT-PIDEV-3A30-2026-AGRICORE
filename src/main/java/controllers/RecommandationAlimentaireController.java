package controllers;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import models.Animal;
import services.AnimalService;
import services.SuiviAnimalService;
import models.SuiviAnimal;

import java.sql.SQLException;
import java.util.*;

public class RecommandationAlimentaireController {

    // ── FXML Entrées ─────────────────────────────────
    @FXML private ComboBox<Animal>  comboAnimal;
    @FXML private TextField         ageTf;
    @FXML private TextField         poidsTf;
    @FXML private ComboBox<String>  etatCombo;
    @FXML private ComboBox<String>  niveauCombo;
    @FXML private ComboBox<String>  saisonCombo;
    @FXML private ComboBox<String>  objectifCombo;

    // ── FXML Résultat ─────────────────────────────────
    @FXML private VBox   resultBox;
    @FXML private Label  lblAnimalInfo;
    @FXML private Label  lblScoreNutri;
    @FXML private Label  lblStatutNutri;
    @FXML private VBox   alimentsBox;
    @FXML private VBox   supplementsBox;
    @FXML private VBox   interditsBox;
    @FXML private VBox   planBox;
    @FXML private TextArea txtRapport;

    private final AnimalService      animalService = new AnimalService();
    private final SuiviAnimalService suiviService  = new SuiviAnimalService();

    // ════════════════════════════════════════════════
    //  MODÈLE EXPERT — Profil nutritionnel par espèce
    // ════════════════════════════════════════════════
    private static class ProfilNutri {
        double besoinsEnergie;   // Mcal/jour
        double besoinsProteine;  // g/kg poids/jour
        double besoinsFibre;     // % de la ration
        double besoinsCalcium;   // g/jour
        double besoinsPhosphore; // g/jour
        String[] alimentsBase;
        String[] alimentsInterdits;

        ProfilNutri(double energie, double proteine, double fibre,
                    double calcium, double phosphore,
                    String[] base, String[] interdits) {
            this.besoinsEnergie   = energie;
            this.besoinsProteine  = proteine;
            this.besoinsFibre     = fibre;
            this.besoinsCalcium   = calcium;
            this.besoinsPhosphore = phosphore;
            this.alimentsBase     = base;
            this.alimentsInterdits = interdits;
        }
    }

    private ProfilNutri getProfilEspece(String espece) {
        return switch (espece.toLowerCase().trim()) {
            case "vache", "bovin", "bovins" -> new ProfilNutri(
                    24.0, 12.0, 35.0, 50.0, 30.0,
                    new String[]{"Foin de graminées", "Ensilage de maïs", "Pulpe de betterave",
                                 "Tourteau de soja", "Orge concassé", "Mélasse"},
                    new String[]{"Avocat", "Oignons", "Pommes de terre crues",
                                 "Champignons sauvages", "Plantes toxiques (if, laurier)"}
            );
            case "cheval", "jument", "equin", "poney" -> new ProfilNutri(
                    16.5, 8.0, 50.0, 30.0, 18.0,
                    new String[]{"Foin de prairie", "Avoine", "Orge", "Son de blé",
                                 "Carottes", "Pommes", "Luzerne"},
                    new String[]{"Pain en grande quantité", "Sucre raffiné", "Herbe fraîche excessive",
                                 "Fèves", "Chou", "Tomates", "Pelures de pommes de terre"}
            );
            case "mouton", "brebis", "ovin", "agneau" -> new ProfilNutri(
                    8.5, 10.0, 40.0, 4.0, 3.0,
                    new String[]{"Foin de bonne qualité", "Pâturage naturel", "Luzerne",
                                 "Orge", "Maïs", "Tourteau de colza"},
                    new String[]{"Cuivre en excès", "Choux", "Betteraves en excès",
                                 "Plantes riches en oxalates", "Rhubarbe"}
            );
            case "chèvre", "caprin", "bouc" -> new ProfilNutri(
                    9.0, 11.0, 38.0, 5.0, 3.5,
                    new String[]{"Foin varié", "Feuilles d'arbres", "Luzerne",
                                 "Son de blé", "Maïs", "Légumes variés"},
                    new String[]{"Ail en excès", "Poireaux", "Azalée",
                                 "Laurier-rose", "Rhododendron", "Plantes toxiques"}
            );
            case "porc", "truie", "porcin", "cochon" -> new ProfilNutri(
                    12.0, 15.0, 10.0, 8.0, 6.0,
                    new String[]{"Céréales (maïs, orge, blé)", "Tourteau de soja",
                                 "Pommes de terre cuites", "Légumes cuits", "Son de blé"},
                    new String[]{"Viande crue", "Sel en excès", "Sucreries",
                                 "Aliments moisis", "Avocat", "Macadamia"}
            );
            case "poulet", "poule", "volaille", "coq" -> new ProfilNutri(
                    0.3, 18.0, 5.0, 3.5, 2.5,
                    new String[]{"Maïs concassé", "Tourteau de soja", "Blé",
                                 "Coquilles d'huîtres (calcium)", "Vers de terre",
                                 "Légumes verts hachés"},
                    new String[]{"Avocat", "Chocolat", "Caféine", "Sel",
                                 "Oignons crus", "Pommes de terre vertes", "Haricots crus"}
            );
            case "lapin" -> new ProfilNutri(
                    0.2, 12.0, 45.0, 1.0, 0.8,
                    new String[]{"Foin de timothy", "Granulés spécifiques lapins",
                                 "Légumes verts frais", "Carottes", "Herbes fraîches",
                                 "Feuilles de pissenlit"},
                    new String[]{"Laitue iceberg", "Oignons", "Rhubarbe",
                                 "Pommes de terre", "Maïs", "Sucre", "Chocolat"}
            );
            default -> new ProfilNutri(
                    10.0, 12.0, 25.0, 5.0, 3.0,
                    new String[]{"Fourrage adapté", "Compléments minéraux",
                                 "Eau fraîche en permanence"},
                    new String[]{"Aliments moisis", "Plantes non identifiées"}
            );
        };
    }

    // ════════════════════════════════════════
    //  INITIALIZE
    // ════════════════════════════════════════
    @FXML
    void initialize() {
        resultBox.setVisible(false);

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

        etatCombo.setItems(FXCollections.observableArrayList("Bon", "Malade", "Critique", "Convalescent", "En gestation", "En lactation"));
        niveauCombo.setItems(FXCollections.observableArrayList("Faible", "Moyen", "Élevé"));
        saisonCombo.setItems(FXCollections.observableArrayList("Printemps", "Été", "Automne", "Hiver"));
        objectifCombo.setItems(FXCollections.observableArrayList(
                "Maintien du poids", "Prise de poids", "Perte de poids",
                "Production laitière", "Performance sportive", "Convalescence", "Croissance"));

        // Saison auto selon mois actuel
        int mois = java.time.LocalDate.now().getMonthValue();
        saisonCombo.setValue(mois <= 2 || mois == 12 ? "Hiver" :
                             mois <= 5 ? "Printemps" :
                             mois <= 8 ? "Été" : "Automne");
    }

    // ════════════════════════════════════════
    //  AUTO-REMPLIR DEPUIS LE DERNIER SUIVI
    // ════════════════════════════════════════
    private void autoRemplir() {
        Animal a = comboAnimal.getValue();
        if (a == null) return;
        try {
            List<SuiviAnimal> suivis = suiviService.readByAnimal(a.getIdAnimal());
            if (!suivis.isEmpty()) {
                SuiviAnimal dernier = suivis.get(suivis.size() - 1);
                poidsTf.setText(String.valueOf(dernier.getPoids()));
                etatCombo.setValue(dernier.getEtatSante());
                niveauCombo.setValue(dernier.getNiveauActivite());
            }
        } catch (Exception ex) { ex.printStackTrace(); }
    }

    // ════════════════════════════════════════
    //  GÉNÉRER RECOMMANDATION
    // ════════════════════════════════════════
    @FXML
    void genererRecommandation() {

        // ── Validations ──
        if (comboAnimal.getValue() == null) { showAlert("Choisissez un animal !"); return; }
        if (poidsTf.getText().trim().isEmpty()) { showAlert("Entrez le poids !"); return; }
        if (ageTf.getText().trim().isEmpty())   { showAlert("Entrez l'âge !"); return; }
        if (etatCombo.getValue() == null)       { showAlert("Choisissez l'état de santé !"); return; }
        if (objectifCombo.getValue() == null)   { showAlert("Choisissez un objectif !"); return; }

        double poids; int age;
        try { poids = Double.parseDouble(poidsTf.getText().trim().replace(",", ".")); }
        catch (NumberFormatException e) { showAlert("Poids invalide !"); return; }
        try { age = Integer.parseInt(ageTf.getText().trim()); }
        catch (NumberFormatException e) { showAlert("Âge invalide !"); return; }

        Animal animal   = comboAnimal.getValue();
        String espece   = animal.getEspece();
        String race     = animal.getRace();
        String etat     = etatCombo.getValue();
        String niveau   = niveauCombo.getValue() != null ? niveauCombo.getValue() : "Moyen";
        String saison   = saisonCombo.getValue() != null ? saisonCombo.getValue() : "Printemps";
        String objectif = objectifCombo.getValue();

        ProfilNutri profil = getProfilEspece(espece);

        // ════════════════════════════════════════
        //  MOTEUR DE RECOMMANDATION IA
        // ════════════════════════════════════════
        List<String> aliments     = new ArrayList<>();
        List<String> supplements  = new ArrayList<>();
        List<String> interdits    = new ArrayList<>(Arrays.asList(profil.alimentsInterdits));
        List<String> plan         = new ArrayList<>();
        StringBuilder rapport     = new StringBuilder();

        // Score nutritionnel de départ
        int scoreNutri = 70;

        // ── PHASE 1 : Aliments de base selon espèce ──
        aliments.addAll(Arrays.asList(profil.alimentsBase));

        // ── PHASE 2 : Ajustements selon ÉTAT DE SANTÉ ──
        switch (etat) {
            case "Malade" -> {
                supplements.add("💊 Électrolytes — Réhydratation (eau + sel + sucre)");
                supplements.add("🌿 Probiotiques — Restaurer la flore intestinale");
                supplements.add("🍋 Vitamine C — Renforcer l'immunité");
                plan.add("🔴 Réduire les rations de 30% pendant la maladie");
                plan.add("💧 Eau fraîche disponible en permanence (augmenter de 50%)");
                plan.add("📅 Repas fractionnés : 4-5 petits repas par jour");
                scoreNutri -= 15;
            }
            case "Critique" -> {
                supplements.add("🚨 Nutrition parentérale si refus de manger");
                supplements.add("💉 Vitamines B12 injectables — Énergie immédiate");
                supplements.add("⚡ Glucose — Apport énergétique d'urgence");
                plan.add("🚨 Consultation vétérinaire URGENTE avant toute alimentation");
                plan.add("🍽️ Alimentation liquide ou semi-liquide uniquement");
                scoreNutri -= 30;
            }
            case "Convalescent" -> {
                supplements.add("🌿 Probiotiques — Récupération digestive");
                supplements.add("💊 Multivitamines — Récupération générale");
                supplements.add("🥩 Protéines supplémentaires — Reconstruction musculaire");
                plan.add("📈 Augmenter progressivement les rations (+10%/semaine)");
                plan.add("🍽️ Aliments hautement digestibles en priorité");
                scoreNutri += 5;
            }
            case "En gestation" -> {
                supplements.add("🧪 Acide folique — Développement fœtal");
                supplements.add("🦴 Calcium + Phosphore — Squelette du fœtus");
                supplements.add("🛡️ Vitamine E + Sélénium — Prévention problèmes de mise bas");
                plan.add("📈 Augmenter les rations de 20-30% au dernier tiers de gestation");
                plan.add("🚫 Éviter le surpoids — Risque de difficultés à la mise bas");
                plan.add("💊 Supplémenter en iode et zinc");
                scoreNutri += 10;
            }
            case "En lactation" -> {
                supplements.add("🥛 Calcium — Production laitière (besoins x2)");
                supplements.add("⚡ Énergie supplémentaire — Céréales + corps gras");
                supplements.add("🌿 Magnésium — Prévenir la tétanie d'herbage");
                plan.add("📈 Augmenter les rations de 40-50% selon production");
                plan.add("💧 Eau : besoins multipliés par 3 pendant la lactation");
                scoreNutri += 15;
            }
        }

        // ── PHASE 3 : Ajustements selon OBJECTIF ──
        switch (objectif) {
            case "Prise de poids" -> {
                supplements.add("🌽 Énergie dense — Maïs + orge en supplément");
                supplements.add("💪 Acides aminés essentiels — Lysine + méthionine");
                plan.add("📈 Augmenter ration de 15-20% progressivement");
                plan.add("🕐 3 repas par jour minimum");
                plan.add("📊 Peser l'animal chaque semaine pour suivre la progression");
                scoreNutri += 10;
            }
            case "Perte de poids" -> {
                plan.add("📉 Réduire les céréales de 30%");
                plan.add("🌿 Augmenter le fourrage grossier (foin, paille)");
                plan.add("🏃 Augmenter l'activité physique progressivement");
                plan.add("📊 Peser chaque semaine — objectif : -1% du poids/semaine");
                interdits.add("Aliments riches en sucres (mélasse, betterave en excès)");
                scoreNutri -= 5;
            }
            case "Production laitière" -> {
                supplements.add("⚡ Énergie — Maïs + son de blé + mélasse");
                supplements.add("🥩 Protéines — Tourteau de soja ou colza");
                supplements.add("🦴 Minéraux lait — Calcium + Phosphore + Magnésium");
                plan.add("💧 Eau à volonté — 1L d'eau produit 1L de lait");
                plan.add("🍽️ Ration fractionnée : 3 repas + accès permanent au foin");
            }
            case "Performance sportive" -> {
                supplements.add("⚡ Glucides complexes — Avoine + orge");
                supplements.add("💪 Électrolytes — Avant et après l'effort");
                supplements.add("🛡️ Antioxydants — Vitamine E + Sélénium");
                plan.add("🕐 Repas 2h avant l'exercice minimum");
                plan.add("💧 Hydratation intensive avant/pendant/après effort");
            }
            case "Croissance" -> {
                supplements.add("🥩 Protéines élevées — 18-20% de la ration");
                supplements.add("🦴 Calcium + Phosphore + Vitamine D — Ossification");
                supplements.add("🌿 Vitamine A — Croissance et immunité");
                plan.add("🍽️ 4 repas par jour pour les jeunes animaux");
                plan.add("📊 Contrôle mensuel du poids et de la taille");
            }
        }

        // ── PHASE 4 : Ajustements selon ÂGE ──
        if (age <= 1) {
            supplements.add("🍼 Colostrum ou lait maternel si < 3 mois");
            supplements.add("🌿 Prébiotiques — Développement microbiote");
            plan.add("👶 Jeune animal : augmenter fréquence des repas");
        } else if (age >= 8) {
            supplements.add("🦴 Calcium + Vitamine D — Prévenir l'arthrose");
            supplements.add("🛡️ Antioxydants — Ralentir le vieillissement cellulaire");
            plan.add("👴 Animal âgé : aliments plus digestibles, moins de fibres dures");
            plan.add("🦷 Vérifier l'état dentaire régulièrement");
        }

        // ── PHASE 5 : Ajustements selon SAISON ──
        switch (saison) {
            case "Été" -> {
                supplements.add("💧 Électrolytes — Compenser la sudation excessive");
                supplements.add("🧂 Sel minéral — Besoins augmentés par la chaleur");
                plan.add("☀️ Été : donner les repas le matin tôt et le soir tard");
                plan.add("💧 Doubler les points d'eau et l'accès à l'eau fraîche");
                plan.add("🌿 Favoriser les aliments frais et aqueux");
            }
            case "Hiver" -> {
                supplements.add("⚡ Énergie supplémentaire — +15% de céréales");
                supplements.add("☀️ Vitamine D — Manque d'ensoleillement");
                plan.add("❄️ Hiver : augmenter les rations de 10-20%");
                plan.add("🌾 Favoriser le foin sec, réduire l'herbe fraîche");
            }
            case "Printemps" -> {
                plan.add("🌱 Printemps : introduction progressive de l'herbe fraîche");
                plan.add("⚠️ Risque de diarrhée — Limiter herbe à 2h/jour les premières semaines");
                supplements.add("🌿 Magnésium — Prévenir la tétanie d'herbage");
            }
            case "Automne" -> {
                supplements.add("🛡️ Vitamines A+D+E — Préparer l'hiver");
                plan.add("🍂 Automne : constituer les réserves corporelles pour l'hiver");
                plan.add("📈 Légère augmentation des rations énergétiques");
            }
        }

        // ── PHASE 6 : Ajustements selon NIVEAU D'ACTIVITÉ ──
        switch (niveau) {
            case "Élevé" -> {
                supplements.add("⚡ Glucides rapides — Maïs, avoine pour l'énergie");
                supplements.add("💧 Électrolytes — Récupération après effort");
                plan.add("🏃 Activité élevée : +25% d'apport énergétique");
            }
            case "Faible" -> {
                plan.add("😴 Activité faible : réduire les glucides de 20%");
                plan.add("🌿 Privilégier fourrages grossiers pour occuper l'animal");
                interdits.add("Céréales en excès (risque d'obésité)");
            }
        }

        // ── PHASE 7 : Ajustements selon RACE ──
        String raceLow = race.toLowerCase();
        if (raceLow.contains("holstein") || raceLow.contains("frisonne")) {
            supplements.add("🥛 Ration laitière haute — Race grande productrice");
            plan.add("🏆 Holstein : besoins énergétiques parmi les plus élevés des bovins");
        } else if (raceLow.contains("angus") || raceLow.contains("charolais")) {
            supplements.add("💪 Protéines — Race à viande, développement musculaire");
        } else if (raceLow.contains("merinos")) {
            supplements.add("🧶 Soufre + Zinc — Qualité de la laine");
        }

        // ── SCORE FINAL ──
        scoreNutri = Math.max(0, Math.min(100, scoreNutri));
        String statutNutri, couleurScore;
        if      (scoreNutri >= 80) { statutNutri = "✅ EXCELLENT";    couleurScore = "#2e7d32"; }
        else if (scoreNutri >= 60) { statutNutri = "⚠️ SATISFAISANT"; couleurScore = "#f57c00"; }
        else if (scoreNutri >= 40) { statutNutri = "🔴 INSUFFISANT";  couleurScore = "#e65100"; }
        else                       { statutNutri = "🚨 CRITIQUE";     couleurScore = "#c62828"; }

        // ── RAPPORT TEXTE ──
        rapport.append("═══ RAPPORT NUTRITIONNEL COMPLET ═══\n\n");
        rapport.append("🐾 Animal   : ").append(animal.getCodeAnimal())
               .append(" (").append(espece).append(" - ").append(race).append(")\n");
        rapport.append("⚖️ Poids    : ").append(poids).append(" kg\n");
        rapport.append("📅 Âge     : ").append(age).append(" an(s)\n");
        rapport.append("🏥 État    : ").append(etat).append("\n");
        rapport.append("🎯 Objectif : ").append(objectif).append("\n");
        rapport.append("🌤️ Saison  : ").append(saison).append("\n\n");

        rapport.append("📊 BESOINS JOURNALIERS ESTIMÉS :\n");
        double facteurActivite = "Élevé".equals(niveau) ? 1.25 : "Faible".equals(niveau) ? 0.85 : 1.0;
        rapport.append("   • Énergie    : ").append(String.format("%.1f", profil.besoinsEnergie * facteurActivite)).append(" Mcal/jour\n");
        rapport.append("   • Protéines  : ").append(String.format("%.1f", profil.besoinsProteine * poids / 1000)).append(" kg/jour\n");
        rapport.append("   • Fibres     : ").append(profil.besoinsFibre).append("% de la ration\n");
        rapport.append("   • Calcium    : ").append(profil.besoinsCalcium).append(" g/jour\n");
        rapport.append("   • Phosphore  : ").append(profil.besoinsPhosphore).append(" g/jour\n\n");

        rapport.append("💧 EAU : ").append(String.format("%.0f", poids * 0.1)).append(" - ")
               .append(String.format("%.0f", poids * 0.15)).append(" litres/jour minimum\n\n");

        rapport.append("⚠️ Cette recommandation est générée par règles expertes.\n");
        rapport.append("Consultez un vétérinaire ou zootechnicien pour validation.");

        // ════════════════════════════════════════
        //  AFFICHER LES RÉSULTATS
        // ════════════════════════════════════════
        lblAnimalInfo.setText("🐾 " + animal.getCodeAnimal() + " — " + espece + " / " + race
                + " | " + poids + "kg | " + age + " ans | " + etat);

        lblScoreNutri.setText(scoreNutri + " / 100");
        lblScoreNutri.setStyle("-fx-font-size:32px;-fx-font-weight:bold;-fx-text-fill:" + couleurScore + ";");

        lblStatutNutri.setText(statutNutri);
        lblStatutNutri.setStyle("-fx-font-size:15px;-fx-font-weight:bold;-fx-text-fill:" + couleurScore + ";");

        // Aliments recommandés
        alimentsBox.getChildren().clear();
        for (String aliment : aliments) {
            HBox row = createRow("✅ " + aliment, "#2e7d32");
            alimentsBox.getChildren().add(row);
        }

        // Suppléments
        supplementsBox.getChildren().clear();
        if (supplements.isEmpty()) {
            supplementsBox.getChildren().add(createRow("✅ Aucun supplément nécessaire", "#2e7d32"));
        } else {
            for (String s : supplements) {
                supplementsBox.getChildren().add(createRow(s, "#1565c0"));
            }
        }

        // Aliments interdits
        interditsBox.getChildren().clear();
        for (String interdit : interdits) {
            interditsBox.getChildren().add(createRow("❌ " + interdit, "#c62828"));
        }

        // Plan alimentaire
        planBox.getChildren().clear();
        for (String p : plan) {
            planBox.getChildren().add(createRow(p, "#555"));
        }

        txtRapport.setText(rapport.toString());
        resultBox.setVisible(true);
    }

    private HBox createRow(String texte, String couleur) {
        Label lbl = new Label(texte);
        lbl.setStyle("-fx-text-fill:" + couleur + ";-fx-font-size:12px;-fx-font-weight:bold;");
        lbl.setWrapText(true);
        HBox hbox = new HBox(lbl);
        hbox.setStyle("-fx-padding:4 0;");
        return hbox;
    }

    // ════════════════════════════════════════
    //  RESET
    // ════════════════════════════════════════
    @FXML
    void reset() {
        comboAnimal.setValue(null);
        ageTf.clear(); poidsTf.clear();
        etatCombo.setValue(null);
        niveauCombo.setValue(null);
        objectifCombo.setValue(null);
        resultBox.setVisible(false);
    }

    // ════════════════════════════════════════
    //  NAVIGATION
    // ════════════════════════════════════════
    @FXML
    void navigateBack() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/ShowAnimals.fxml"));
            Stage stage = (Stage) ageTf.getScene().getWindow();
            stage.getScene().setRoot(root);
        } catch (Exception e) { showAlert(e.getMessage()); }
    }

    private void showAlert(String msg) {
        new Alert(Alert.AlertType.ERROR, msg).showAndWait();
    }
}
