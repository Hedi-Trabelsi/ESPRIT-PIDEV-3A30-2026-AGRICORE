package Controller;

import Model.Equipement;
import Model.Utilisateur;
import services.EquipementService;
import utils.ImageManager;

import javafx.animation.PauseTransition;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;

import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.awt.Desktop;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.ResourceBundle;

public class AjoutEquipementController implements Initializable {

    // ── Formulaire ───────────────────────────────────────────────
    @FXML private TextField fieldNom, fieldType, fieldPrix, fieldQuantite;
    @FXML private Label labelMessage, labelTitre, labelSousTitre;

    // ── Section image (injectée depuis FXML) ─────────────────────
    @FXML private VBox panneauImage;           // conteneur principal de la section image

    // ── Carte Actualités ─────────────────────────────────────────
    @FXML private Label labelNewsStatus;
    @FXML private Label labelNews1Titre, labelNews1Source, labelNews1Date;
    @FXML private Label labelNews2Titre, labelNews2Source, labelNews2Date;
    @FXML private Label labelNews3Titre, labelNews3Source, labelNews3Date;
    @FXML private VBox cardNews1, cardNews2, cardNews3;

    // ── Conversion ───────────────────────────────────────────────
    @FXML private Label labelPrixEUR, labelPrixUSD;
    @FXML private Button btnAction;

    // ── Image : composants créés programmatiquement ───────────────
    private File   fichierImageChoisi    = null;
    private String nomEquipementOriginal = null;  // pour modification : ancien nom
    private ImageView ivApercu;
    private Label     lblNomFichier;
    private Button    btnSupprimerImg;
    private Label     lblPlaceholder;

    // ── Données articles (stockées pour la popup) ────────────────
    private final String[][] articles = new String[3][6];

    // ── Services ─────────────────────────────────────────────────
    private EquipementService equipementService = new EquipementService();
    private static final int ID_FOURNISSEUR = Utilisateur.getId();
    private Equipement equipementToModify = null;

    private double tauxEUR = 0.2981;
    private double tauxUSD = 0.3213;
    private static final String EXCHANGE_API_KEY = "0c3f3b97f846b6f5ced36eff";

    private PauseTransition pauseNews;

    public AjoutEquipementController() throws SQLException {
    }

    // ═══════════════════════════════════════════════════════════════
    //  INITIALISATION
    // ═══════════════════════════════════════════════════════════════
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        for (int i = 0; i < 3; i++)
            articles[i] = new String[]{"—", "—", "—", "—", "", "—"};

        reinitialiserCarteNews();

        pauseNews = new PauseTransition(Duration.millis(800));
        pauseNews.setOnFinished(e -> lancerRechercheNews());

        fieldNom.textProperty().addListener((obs, o, n) -> pauseNews.playFromStart());
        fieldType.textProperty().addListener((obs, o, n) -> {
            if (fieldNom.getText().trim().isEmpty()) pauseNews.playFromStart();
        });

        ajouterHoverEffect(cardNews1, "#ffe0e0", "#fff8f8");
        ajouterHoverEffect(cardNews2, "#ffe0e0", "#fff8f8");
        ajouterHoverEffect(cardNews3, "#ffe0e0", "#fff8f8");

        chargerTauxDeChange();
        construirePanneauImage();
    }

    // ═══════════════════════════════════════════════════════════════
    //  SECTION IMAGE — construction programmatique dans panneauImage
    // ═══════════════════════════════════════════════════════════════

    private void construirePanneauImage() {
        if (panneauImage == null) return;
        panneauImage.getChildren().clear();
        panneauImage.setSpacing(10);
        panneauImage.setAlignment(Pos.CENTER);

        // ── Aperçu ─────────────────────────────────────────────
        ivApercu = new ImageView();
        ivApercu.setFitWidth(240);
        ivApercu.setFitHeight(150);
        ivApercu.setPreserveRatio(true);
        ivApercu.setSmooth(true);
        ivApercu.setVisible(false);

        // ── Placeholder ────────────────────────────────────────
        lblPlaceholder = new Label("📷  Cliquez pour choisir une image");
        lblPlaceholder.setStyle(
            "-fx-font-size: 12px; -fx-text-fill: #7a9c70; -fx-padding: 30 0;");

        // ── Nom du fichier ─────────────────────────────────────
        lblNomFichier = new Label("");
        lblNomFichier.setStyle(
            "-fx-font-size: 10px; -fx-text-fill: #4a7c40; -fx-font-weight: bold;");
        lblNomFichier.setWrapText(true);
        lblNomFichier.setMaxWidth(240);
        lblNomFichier.setVisible(false);

        // ── Bouton Choisir ─────────────────────────────────────
        Button btnChoisir = new Button("🖼  Choisir une image");
        btnChoisir.setStyle(
            "-fx-background-color: #4a7c40; -fx-text-fill: white;" +
            "-fx-background-radius: 9; -fx-font-size: 12px; -fx-font-weight: bold;" +
            "-fx-cursor: hand; -fx-padding: 8 18;");
        btnChoisir.setOnMouseEntered(e -> btnChoisir.setStyle(
            "-fx-background-color: #3a6b2e; -fx-text-fill: white;" +
            "-fx-background-radius: 9; -fx-font-size: 12px; -fx-font-weight: bold;" +
            "-fx-cursor: hand; -fx-padding: 8 18;"));
        btnChoisir.setOnMouseExited(e -> btnChoisir.setStyle(
            "-fx-background-color: #4a7c40; -fx-text-fill: white;" +
            "-fx-background-radius: 9; -fx-font-size: 12px; -fx-font-weight: bold;" +
            "-fx-cursor: hand; -fx-padding: 8 18;"));
        btnChoisir.setOnAction(e -> ouvrirFileChooser());

        // ── Bouton Supprimer ───────────────────────────────────
        btnSupprimerImg = new Button("✕  Supprimer l'image");
        btnSupprimerImg.setStyle(
            "-fx-background-color: #fdf0f0; -fx-text-fill: #e74c3c;" +
            "-fx-background-radius: 9; -fx-font-size: 11px; -fx-cursor: hand; -fx-padding: 6 14;" +
            "-fx-border-color: #fac0c0; -fx-border-radius: 9;");
        btnSupprimerImg.setVisible(false);
        btnSupprimerImg.setOnAction(e -> supprimerImageLocale());

        HBox boutons = new HBox(10, btnChoisir, btnSupprimerImg);
        boutons.setAlignment(Pos.CENTER);

        panneauImage.getChildren().addAll(
            lblPlaceholder, ivApercu, lblNomFichier, boutons);
    }

    private void ouvrirFileChooser() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Choisir l'image de l'équipement");
        fc.getExtensionFilters().add(
            new FileChooser.ExtensionFilter(
                "Images", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.bmp", "*.webp"));

        Stage stage = (Stage) fieldNom.getScene().getWindow();
        File fichier = fc.showOpenDialog(stage);
        if (fichier == null) return;

        fichierImageChoisi = fichier;
        try {
            Image img = new Image(
                fichier.toURI().toString(), 240, 150, true, true);
            ivApercu.setImage(img);
            ivApercu.setVisible(true);
            lblPlaceholder.setVisible(false);
            lblNomFichier.setText("📁 " + fichier.getName());
            lblNomFichier.setVisible(true);
            btnSupprimerImg.setVisible(true);
            panneauImage.setStyle(
                "-fx-background-color: #eef7ec; -fx-background-radius: 12;" +
                "-fx-border-color: #4a7c40; -fx-border-radius: 12;" +
                "-fx-border-style: solid; -fx-padding: 12;");
        } catch (Exception ex) {
            showMessage("Impossible de charger l'image.", false);
        }
    }

    private void supprimerImageLocale() {
        fichierImageChoisi = null;
        ivApercu.setImage(null);
        ivApercu.setVisible(false);
        lblPlaceholder.setVisible(true);
        lblNomFichier.setVisible(false);
        btnSupprimerImg.setVisible(false);
        panneauImage.setStyle(
            "-fx-background-color: #f5f9f4; -fx-background-radius: 12;" +
            "-fx-border-color: #c8ddc5; -fx-border-radius: 12;" +
            "-fx-border-style: dashed; -fx-padding: 12;");
    }

    /** Charge l'aperçu d'une image déjà existante (mode modification) */
    private void chargerApercu(String nomEquipement) {
        String chemin = ImageManager.getImagePath(nomEquipement);
        if (chemin == null) return;
        try {
            Image img = new Image(
                new File(chemin).toURI().toString(), 240, 150, true, true);
            ivApercu.setImage(img);
            ivApercu.setVisible(true);
            lblPlaceholder.setVisible(false);
            lblNomFichier.setText("📁 " + new File(chemin).getName() + "  (image existante)");
            lblNomFichier.setVisible(true);
            btnSupprimerImg.setVisible(true);
            panneauImage.setStyle(
                "-fx-background-color: #eef7ec; -fx-background-radius: 12;" +
                "-fx-border-color: #4a7c40; -fx-border-radius: 12;" +
                "-fx-border-style: solid; -fx-padding: 12;");
        } catch (Exception ignored) {}
    }

    // ═══════════════════════════════════════════════════════════════
    //  AJOUT / MODIFICATION
    // ═══════════════════════════════════════════════════════════════

    public void setEquipementToModify(Equipement eq) {
        this.equipementToModify = eq;
        this.nomEquipementOriginal = eq.getNom();
        labelTitre.setText("Modifier l'Équipement");
        labelSousTitre.setText("Modifiez les informations de : " + eq.getNom());
        btnAction.setText("Enregistrer les modifications");
        btnAction.setStyle(
            "-fx-background-color: #f39c12; -fx-text-fill: white; " +
            "-fx-background-radius: 10; -fx-font-weight: bold; " +
            "-fx-font-size: 14px; -fx-cursor: hand; -fx-padding: 12 30;");
        fieldNom.setText(eq.getNom());
        fieldType.setText(eq.getType());
        fieldPrix.setText(eq.getPrix());
        fieldQuantite.setText(String.valueOf(eq.getQuantite()));
        mettreAJourConversion();
        lancerRechercheNews();
        // Charger l'image existante si elle existe
        chargerApercu(eq.getNom());
    }

    @FXML
    private void sauvegarder() {
        if (!validateForm()) return;

        String nomSaisi = fieldNom.getText().trim();

        try {
            // ── Gérer l'image ─────────────────────────────────────
            if (fichierImageChoisi != null) {
                // Si on renomme un équipement, supprimer l'ancienne clé image
                if (nomEquipementOriginal != null
                        && !nomEquipementOriginal.equals(nomSaisi)) {
                    ImageManager.supprimerImage(nomEquipementOriginal);
                }
                ImageManager.sauvegarderImage(nomSaisi, fichierImageChoisi);
            } else if (nomEquipementOriginal != null
                    && !nomEquipementOriginal.equals(nomSaisi)) {
                // Renommage sans nouvelle image → migrer l'ancienne image
                String ancienChemin = ImageManager.getImagePath(nomEquipementOriginal);
                if (ancienChemin != null) {
                    ImageManager.supprimerImage(nomEquipementOriginal);
                    ImageManager.sauvegarderImage(nomSaisi, new File(ancienChemin));
                }
            } else if (ivApercu != null && !ivApercu.isVisible()
                    && nomEquipementOriginal != null) {
                // L'utilisateur a cliqué "Supprimer l'image" → on efface
                ImageManager.supprimerImage(nomEquipementOriginal);
            }

            // ── Sauvegarder l'équipement ─────────────────────────
            if (equipementToModify != null) {
                equipementToModify.setNom(nomSaisi);
                equipementToModify.setType(fieldType.getText().trim());
                equipementToModify.setPrix(fieldPrix.getText().trim());
                equipementToModify.setQuantite(Integer.parseInt(
                    fieldQuantite.getText().trim()));
                equipementToModify.setId_fournisseur(ID_FOURNISSEUR);
                equipementService.modifier(equipementToModify);
                showMessage("Équipement modifié avec succès !", true);
                equipementToModify = null;
                nomEquipementOriginal = null;
                goToListe();
            } else {
                Equipement eq = new Equipement(
                    nomSaisi,
                    fieldType.getText().trim(),
                    fieldPrix.getText().trim(),
                    Integer.parseInt(fieldQuantite.getText().trim()),
                    ID_FOURNISSEUR
                );
                equipementService.ajouter(eq);
                showMessage("Équipement ajouté avec succès !", true);
                clearForm();
            }

        } catch (SQLException e) {
            showMessage("Erreur : " + e.getMessage(), false);
        }
    }

    @FXML
    private void clearForm() {
        fieldNom.clear(); fieldType.clear();
        fieldPrix.clear(); fieldQuantite.clear();
        equipementToModify = null;
        nomEquipementOriginal = null;
        fichierImageChoisi = null;
        labelMessage.setText("");
        labelPrixEUR.setText("Saisir un prix...");
        labelPrixUSD.setText("");
        labelTitre.setText("➕ Ajouter un Équipement");
        labelSousTitre.setText(
            "Remplissez le formulaire pour enregistrer un nouvel équipement agricole");
        btnAction.setText("✅ Ajouter l'équipement");
        btnAction.setStyle(
            "-fx-background-color: #4a7c40; -fx-text-fill: white; " +
            "-fx-background-radius: 10; -fx-font-weight: bold; " +
            "-fx-font-size: 14px; -fx-cursor: hand; -fx-padding: 12 30;");
        reinitialiserCarteNews();
        for (int i = 0; i < 3; i++)
            articles[i] = new String[]{"—", "—", "—", "—", "", "—"};
        construirePanneauImage();  // reset visuel image
    }

    @FXML private void goToListe() { navigateTo("/fxml/ListeEquipements.fxml"); }

    private void navigateTo(String fxml) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(fxml));
            NavigationUtil.loadInContentArea(fieldNom, root);
        } catch (Exception e) { e.printStackTrace(); }
    }

    // ═══════════════════════════════════════════════════════════════
    //  API 1 — GOOGLE NEWS RSS
    // ═══════════════════════════════════════════════════════════════

    private void lancerRechercheNews() {
        String termeNom  = fieldNom.getText().trim();
        String termeType = fieldType.getText().trim();
        final String terme = !termeNom.isEmpty() ? termeNom : termeType;
        if (terme.isEmpty()) { reinitialiserCarteNews(); return; }

        final String query = terme + " équipement agricole";
        afficherChargement(terme);

        new Thread(() -> {
            try {
                String encoded = URLEncoder.encode(query, StandardCharsets.UTF_8);
                String urlStr  = "https://news.google.com/rss/search?q="
                                 + encoded + "&hl=fr&gl=TN&ceid=TN:fr";
                String xml     = appelHTTP(urlStr);

                DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
                dbf.setFeature(
                    "http://xml.org/sax/features/external-general-entities", false);
                DocumentBuilder db = dbf.newDocumentBuilder();
                Document doc = db.parse(
                    new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
                NodeList items = doc.getElementsByTagName("item");
                int count = Math.min(items.getLength(), 3);

                for (int i = 0; i < 3; i++)
                    articles[i] = new String[]{
                        "—", "—", "—", "Aucune description disponible.", "", "—"};

                for (int i = 0; i < count; i++) {
                    Element item   = (Element) items.item(i);
                    String  titre  = texteTag(item, "title");
                    String  lien   = texteTag(item, "link");
                    String  desc   = texteTag(item, "description");
                    String  source = extraireSource(texteTag(item, "source"), titre);
                    String  date   = formaterDate(texteTag(item, "pubDate"));
                    String  categ  = texteTag(item, "category");

                    desc = desc.replaceAll("<[^>]+>", "").trim();
                    if (desc.isEmpty()) desc = "Cliquez pour lire l'article complet.";

                    String titrePur = titre.contains(" - ")
                        ? titre.substring(0, titre.lastIndexOf(" - ")).trim()
                        : titre;

                    articles[i] = new String[]{
                        titrePur, source, date, desc, lien,
                        categ.isEmpty() ? "Actualité" : categ
                    };
                }

                final int nb = count;
                final String tf = terme;
                javafx.application.Platform.runLater(() -> {
                    labelNewsStatus.setText(nb == 0
                        ? "Aucune actualité trouvée pour : " + tf
                        : nb + " article(s) — cliquez pour voir les détails");
                    for (int i = 0; i < 3; i++) {
                        String[] a = articles[i];
                        switch (i) {
                            case 0 -> {
                                labelNews1Titre.setText(tronquer(a[0], 80));
                                labelNews1Source.setText(a[1]);
                                labelNews1Date.setText(a[2]);
                            }
                            case 1 -> {
                                labelNews2Titre.setText(tronquer(a[0], 80));
                                labelNews2Source.setText(a[1]);
                                labelNews2Date.setText(a[2]);
                            }
                            case 2 -> {
                                labelNews3Titre.setText(tronquer(a[0], 80));
                                labelNews3Source.setText(a[1]);
                                labelNews3Date.setText(a[2]);
                            }
                        }
                    }
                });
            } catch (Exception ex) {
                ex.printStackTrace();
                javafx.application.Platform.runLater(() -> {
                    labelNewsStatus.setText("⚠ Erreur de connexion au flux d'actualités");
                    labelNews1Titre.setText("Impossible de récupérer les actualités.");
                    labelNews1Source.setText("Vérifiez votre connexion.");
                    labelNews1Date.setText("—");
                    labelNews2Titre.setText("—"); labelNews2Source.setText("—"); labelNews2Date.setText("—");
                    labelNews3Titre.setText("—"); labelNews3Source.setText("—"); labelNews3Date.setText("—");
                });
            }
        }).start();
    }

    @FXML private void ouvrirDetailNews1(MouseEvent e) { afficherPopupDetail(0); }
    @FXML private void ouvrirDetailNews2(MouseEvent e) { afficherPopupDetail(1); }
    @FXML private void ouvrirDetailNews3(MouseEvent e) { afficherPopupDetail(2); }

    private void afficherPopupDetail(int index) {
        String[] a = articles[index];
        if (a[0].equals("—") || a[0].startsWith("Chargement")) return;

        Stage popup = new Stage();
        popup.initModality(Modality.APPLICATION_MODAL);
        popup.setTitle("📰 Détail de l'article");
        popup.setResizable(false);

        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(18, 24, 18, 24));
        header.setStyle("-fx-background-color: #b71c1c;");
        Label iconHeader = new Label("📰");
        iconHeader.setStyle("-fx-font-size: 24px;");
        VBox titreHeader = new VBox(3);
        Label lblSource = new Label("📡 " + a[1] + "   •   🗓 " + a[2]);
        lblSource.setStyle("-fx-font-size: 11px; -fx-text-fill: rgba(255,255,255,0.75);");
        Label lblCateg = new Label("🏷 " + a[5]);
        lblCateg.setStyle("-fx-font-size: 10px; -fx-text-fill: rgba(255,255,255,0.55); " +
            "-fx-background-color: rgba(0,0,0,0.2); -fx-background-radius: 5; -fx-padding: 2 8;");
        titreHeader.getChildren().addAll(lblSource, lblCateg);
        Region spacer = new Region(); HBox.setHgrow(spacer, Priority.ALWAYS);
        Button btnFermer = new Button("✕");
        btnFermer.setStyle("-fx-background-color: rgba(255,255,255,0.2); -fx-text-fill: white; " +
            "-fx-background-radius: 20; -fx-font-size: 13px; -fx-cursor: hand; -fx-padding: 4 10;");
        btnFermer.setOnAction(e -> popup.close());
        header.getChildren().addAll(iconHeader, titreHeader, spacer, btnFermer);

        VBox titreBox = new VBox(6);
        titreBox.setPadding(new Insets(20, 24, 12, 24));
        titreBox.setStyle("-fx-background-color: #fff5f5;");
        Label lblTitre = new Label(a[0]);
        lblTitre.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #1a1a1a;");
        lblTitre.setWrapText(true); lblTitre.setMaxWidth(500);
        titreBox.getChildren().add(lblTitre);

        Separator sep = new Separator();
        VBox descBox = new VBox(10);
        descBox.setPadding(new Insets(16, 24, 16, 24));
        Label lblDescTitre = new Label("📄 Résumé de l'article");
        lblDescTitre.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #b71c1c;");
        Label lblDesc = new Label(a[3]);
        lblDesc.setStyle("-fx-font-size: 13px; -fx-text-fill: #333; -fx-line-spacing: 3;");
        lblDesc.setWrapText(true); lblDesc.setMaxWidth(500);
        descBox.getChildren().addAll(lblDescTitre, lblDesc);

        HBox footer = new HBox(12);
        footer.setPadding(new Insets(16, 24, 20, 24));
        footer.setAlignment(Pos.CENTER_RIGHT);
        footer.setStyle("-fx-background-color: #fafafa; -fx-border-color: #eee; -fx-border-width: 1 0 0 0;");
        Button btnClose = new Button("Fermer");
        btnClose.setStyle("-fx-background-color: #ecf0f1; -fx-text-fill: #555; " +
            "-fx-background-radius: 9; -fx-font-size: 13px; -fx-cursor: hand; -fx-padding: 10 22;");
        btnClose.setOnAction(e -> popup.close());
        if (!a[4].isEmpty()) {
            Button btnNav = new Button("🌐 Ouvrir dans le navigateur");
            btnNav.setStyle("-fx-background-color: #b71c1c; -fx-text-fill: white; " +
                "-fx-background-radius: 9; -fx-font-weight: bold; -fx-font-size: 13px; " +
                "-fx-cursor: hand; -fx-padding: 10 22;");
            btnNav.setOnAction(e -> {
                try { Desktop.getDesktop().browse(new URI(a[4])); } catch (Exception ex) { ex.printStackTrace(); }
            });
            footer.getChildren().addAll(btnClose, btnNav);
        } else {
            footer.getChildren().add(btnClose);
        }

        VBox root = new VBox();
        root.getChildren().addAll(header, titreBox, sep, descBox, footer);
        root.setStyle("-fx-background-color: white;");
        popup.setScene(new Scene(root, 560, 400));
        popup.show();
    }

    // ═══════════════════════════════════════════════════════════════
    //  API 2 — EXCHANGERATE
    // ═══════════════════════════════════════════════════════════════

    private void chargerTauxDeChange() {
        new Thread(() -> {
            try {
                String urlStr = "https://v6.exchangerate-api.com/v6/" + EXCHANGE_API_KEY + "/latest/TND";
                JSONObject rates = new JSONObject(appelHTTP(urlStr)).getJSONObject("conversion_rates");
                double eur = rates.getDouble("EUR");
                double usd = rates.getDouble("USD");
                javafx.application.Platform.runLater(() -> {
                    tauxEUR = eur; tauxUSD = usd;
                    mettreAJourConversion();
                });
            } catch (Exception e) { /* taux par défaut */ }
        }).start();
    }

    @FXML
    private void mettreAJourConversion() {
        String prixStr = fieldPrix.getText().trim().replace(",", ".");
        if (prixStr.isEmpty()) {
            labelPrixEUR.setText("Saisir un prix...");
            labelPrixUSD.setText("");
            return;
        }
        try {
            double prix = Double.parseDouble(prixStr);
            labelPrixEUR.setText(String.format("= %.2f EUR", prix * tauxEUR));
            labelPrixUSD.setText(String.format("= %.2f USD", prix * tauxUSD));
        } catch (NumberFormatException e) {
            labelPrixEUR.setText("Prix invalide");
            labelPrixUSD.setText("");
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  VALIDATION
    // ═══════════════════════════════════════════════════════════════

    private boolean validateForm() {
        if (fieldNom.getText().trim().isEmpty()
                || fieldType.getText().trim().isEmpty()
                || fieldPrix.getText().trim().isEmpty()
                || fieldQuantite.getText().trim().isEmpty()) {
            showMessage("Veuillez remplir tous les champs.", false);
            return false;
        }
        try {
            int q = Integer.parseInt(fieldQuantite.getText().trim());
            if (q < 0) {
                showMessage("La quantité ne peut pas être négative.", false);
                return false;
            }
        } catch (NumberFormatException e) {
            showMessage("La quantité doit être un entier valide.", false);
            return false;
        }
        try {
            double p = Double.parseDouble(
                fieldPrix.getText().trim().replace(",", "."));
            if (p < 0) {
                showMessage("Le prix ne peut pas être négatif.", false);
                return false;
            }
        } catch (NumberFormatException e) {
            showMessage("Le prix doit être un nombre valide.", false);
            return false;
        }
        return true;
    }

    private void showMessage(String msg, boolean success) {
        labelMessage.setText(msg);
        labelMessage.setStyle(
            "-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: "
            + (success ? "#27ae60" : "#e74c3c") + ";");
    }

    // ═══════════════════════════════════════════════════════════════
    //  UTILITAIRES
    // ═══════════════════════════════════════════════════════════════

    private void ajouterHoverEffect(VBox card, String colorHover, String colorNormal) {
        if (card == null) return;
        String baseStyle = card.getStyle();
        card.setOnMouseEntered(e -> card.setStyle(baseStyle
            + "-fx-background-color: " + colorHover + "; "
            + "-fx-effect: dropshadow(gaussian, rgba(183,28,28,0.25), 10, 0, 0, 3);"));
        card.setOnMouseExited(e -> card.setStyle(baseStyle));
    }

    private void reinitialiserCarteNews() {
        if (labelNewsStatus  != null)
            labelNewsStatus.setText("Saisissez un nom pour voir les news...");
        String dash = "—";
        if (labelNews1Titre  != null) labelNews1Titre.setText(dash);
        if (labelNews1Source != null) labelNews1Source.setText(dash);
        if (labelNews1Date   != null) labelNews1Date.setText(dash);
        if (labelNews2Titre  != null) labelNews2Titre.setText(dash);
        if (labelNews2Source != null) labelNews2Source.setText(dash);
        if (labelNews2Date   != null) labelNews2Date.setText(dash);
        if (labelNews3Titre  != null) labelNews3Titre.setText(dash);
        if (labelNews3Source != null) labelNews3Source.setText(dash);
        if (labelNews3Date   != null) labelNews3Date.setText(dash);
    }

    private String tronquer(String texte, int maxLen) {
        if (texte == null || texte.length() <= maxLen)
            return (texte == null) ? "—" : texte;
        int coupure = texte.lastIndexOf(' ', maxLen);
        return (coupure > 0
            ? texte.substring(0, coupure)
            : texte.substring(0, maxLen)) + "…";
    }

    private String texteTag(Element el, String tag) {
        NodeList nl = el.getElementsByTagName(tag);
        return (nl.getLength() == 0) ? "" : nl.item(0).getTextContent().trim();
    }

    private String extraireSource(String sourceTag, String titre) {
        if (!sourceTag.isEmpty()) return sourceTag;
        int sep = titre.lastIndexOf(" - ");
        return (sep > 0) ? titre.substring(sep + 3) : "Google News";
    }

    private String formaterDate(String pubDate) {
        if (pubDate.isEmpty()) return "—";
        try {
            ZonedDateTime zdt = ZonedDateTime.parse(
                pubDate, DateTimeFormatter.RFC_1123_DATE_TIME);
            return zdt.format(
                DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.FRENCH));
        } catch (DateTimeParseException e) {
            return pubDate.length() > 16 ? pubDate.substring(5, 16) : pubDate;
        }
    }

    private void afficherChargement(String terme) {
        javafx.application.Platform.runLater(() -> {
            labelNewsStatus.setText(
                "⏳ Recherche des actualités pour : " + terme + "...");
            labelNews1Titre.setText("Chargement en cours...");
            labelNews1Source.setText("—"); labelNews1Date.setText("—");
            labelNews2Titre.setText("—"); labelNews2Source.setText("—");
            labelNews2Date.setText("—");
            labelNews3Titre.setText("—"); labelNews3Source.setText("—");
            labelNews3Date.setText("—");
        });
    }

    private String appelHTTP(String urlStr) throws Exception {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("User-Agent",
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
        conn.setConnectTimeout(7000);
        conn.setReadTimeout(7000);
        BufferedReader reader = new BufferedReader(
            new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) sb.append(line);
        reader.close();
        return sb.toString();
    }
}
