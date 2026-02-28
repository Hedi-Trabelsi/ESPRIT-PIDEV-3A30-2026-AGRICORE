package Controller;

import Model.Equipement;
import Model.Panier;
import services.PanierService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import org.json.JSONArray;
import org.json.JSONObject;

import java.awt.Desktop;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.sql.SQLException;
import java.util.ResourceBundle;

public class DetailEquipementController implements Initializable {

    // ── Seul élément injecté depuis le FXML ─────────────────────
    @FXML private VBox rootContainer;

    // ── Widgets créés entièrement en Java (jamais null) ──────────
    private Label             labelNom, labelBandType, labelStock;
    private Label             labelPrix, labelPrixEUR, labelPrixUSD, labelQuantite;
    private HBox              topBand;
    private TextArea          textAreaIA;
    private Label             labelIAStatus, labelArticlesStatus;
    private ProgressIndicator progressIA, progressArticles;
    private Button            btnRegenIA;
    private VBox              articlesContainer;

    // ── Data ─────────────────────────────────────────────────────
    private Equipement            equipement;
    private AgriculteurController parentController;
    private double tauxEUR = 0.2981;
    private double tauxUSD = 0.3213;

    private final PanierService panierService = new PanierService();

    // ── API Keys ─────────────────────────────────────────────────
    // Groq AI (gratuit) → https://console.groq.com
    private static final String GROQ_API_KEY  = "gsk_VOTRE_CLE_GROQ_ICI";
    private static final String GROQ_API_URL  = "https://api.groq.com/openai/v1/chat/completions";
    // GNews (gratuit 10 req/jour) → https://gnews.io
    private static final String GNEWS_API_KEY = "VOTRE_CLE_GNEWS_ICI";

    @Override
    public void initialize(URL url, ResourceBundle rb) { /* tout construit dans setEquipement */ }

    /**
     * Point d'entrée depuis AgriculteurController.
     */
    public void setEquipement(Equipement eq, double tauxEUR, double tauxUSD,
                              AgriculteurController parent) {
        this.equipement       = eq;
        this.tauxEUR          = tauxEUR;
        this.tauxUSD          = tauxUSD;
        this.parentController = parent;
        construireInterface();
        lancerIAAsync();
        lancerArticlesAsync();
    }

    // ═══════════════════════════════════════════════════════════════
    //  CONSTRUCTION COMPLÈTE EN JAVA
    // ═══════════════════════════════════════════════════════════════

    private void construireInterface() {
        rootContainer.getChildren().clear();
        rootContainer.setStyle("-fx-background-color: #f0f5ef;");
        rootContainer.setSpacing(0);

        String color = typeColor(equipement.getType());
        double prix  = parsePrix(equipement.getPrix());

        // ── TOPBAR ──────────────────────────────────────────────
        HBox topbar = new HBox(12);
        topbar.setAlignment(Pos.CENTER_LEFT);
        topbar.setStyle("-fx-background-color: linear-gradient(to right, #2d5a25, #4a7c40);");
        topbar.setPadding(new Insets(14, 24, 14, 20));

        Button btnBack = new Button("← Retour au catalogue");
        btnBack.setStyle(styleBtnBack(false));
        btnBack.setOnAction(e -> goBack());
        btnBack.setOnMouseEntered(e -> btnBack.setStyle(styleBtnBack(true)));
        btnBack.setOnMouseExited(e  -> btnBack.setStyle(styleBtnBack(false)));

        Label appTitle = new Label("🌾  AgroManager");
        appTitle.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: white;");
        Region spacerTop = new Region(); HBox.setHgrow(spacerTop, Priority.ALWAYS);
        Label pageTitle = new Label("Détail Équipement");
        pageTitle.setStyle("-fx-font-size: 12px; -fx-text-fill: rgba(255,255,255,0.7);");

        topbar.getChildren().addAll(btnBack, appTitle, spacerTop, pageTitle);

        // ── HERO CARD ───────────────────────────────────────────
        VBox heroCard = new VBox(0);
        heroCard.setStyle("-fx-background-color: white;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 12, 0, 0, 3);");

        topBand = new HBox(); topBand.setPrefHeight(10);
        topBand.setStyle("-fx-background-color: " + color + ";");

        HBox heroBody = new HBox(24);
        heroBody.setAlignment(Pos.CENTER_LEFT);
        heroBody.setPadding(new Insets(22, 30, 22, 28));

        // Colonne gauche — infos + bouton ajouter
        VBox infoLeft = new VBox(10); HBox.setHgrow(infoLeft, Priority.ALWAYS);

        labelNom = new Label(typeEmoji(equipement.getType()) + "  " + equipement.getNom());
        labelNom.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; " +
            "-fx-text-fill: #1e3a1a; -fx-wrap-text: true;");

        labelBandType = new Label(equipement.getType());
        labelBandType.setStyle("-fx-background-color: " + color + "22; -fx-text-fill: " + color + ";" +
            "-fx-background-radius: 20; -fx-padding: 3 12; " +
            "-fx-font-size: 11px; -fx-font-weight: bold;");

        String stockText  = equipement.getQuantite() == 0 ? "⚠   Rupture de stock"
                          : equipement.getQuantite() < 3
                            ? "⚡  Stock faible (" + equipement.getQuantite() + " restants)"
                            : "✅  En stock (" + equipement.getQuantite() + " disponibles)";
        String stockColor = equipement.getQuantite() == 0 ? "#e74c3c"
                          : equipement.getQuantite() < 3  ? "#f39c12" : "#27ae60";
        labelStock = new Label(stockText);
        labelStock.setStyle("-fx-text-fill: " + stockColor + "; -fx-font-weight: bold; -fx-font-size: 12px;");

        // Bouton Ajouter au panier
        boolean dispo = equipement.getQuantite() > 0;
        Button btnAjouter = new Button(dispo ? "🛒  Ajouter au panier" : "❌  Indisponible");
        btnAjouter.setDisable(!dispo);
        btnAjouter.setStyle(dispo
            ? "-fx-background-color: #4a7c40; -fx-text-fill: white; -fx-background-radius: 10;" +
              "-fx-font-size: 13px; -fx-font-weight: bold; -fx-cursor: hand; -fx-padding: 10 22;"
            : "-fx-background-color: #ecf0f1; -fx-text-fill: #aaa; -fx-background-radius: 10;" +
              "-fx-font-size: 13px; -fx-font-weight: bold; -fx-padding: 10 22;");
        if (dispo) {
            btnAjouter.setOnAction(e -> ajouterAuPanier());
            btnAjouter.setOnMouseEntered(e -> btnAjouter.setStyle(
                "-fx-background-color: #3a6b2e; -fx-text-fill: white; -fx-background-radius: 10;" +
                "-fx-font-size: 13px; -fx-font-weight: bold; -fx-cursor: hand; -fx-padding: 10 22;" +
                "-fx-effect: dropshadow(gaussian, rgba(74,124,64,0.45), 8, 0, 0, 3);"));
            btnAjouter.setOnMouseExited(e -> btnAjouter.setStyle(
                "-fx-background-color: #4a7c40; -fx-text-fill: white; -fx-background-radius: 10;" +
                "-fx-font-size: 13px; -fx-font-weight: bold; -fx-cursor: hand; -fx-padding: 10 22;"));
        }

        infoLeft.getChildren().addAll(labelNom, labelBandType, labelStock, btnAjouter);

        // Colonne droite — prix
        VBox prixBox = new VBox(8);
        prixBox.setAlignment(Pos.CENTER_RIGHT);
        prixBox.setStyle("-fx-background-color: #f4fbf2; -fx-background-radius: 14; -fx-padding: 16 22;");

        labelPrix = new Label(String.format("%.2f TND", prix));
        labelPrix.setStyle("-fx-font-size: 28px; -fx-font-weight: bold; -fx-text-fill: #2d5a25;");

        labelPrixEUR = new Label(String.format("%.2f €", prix * tauxEUR));
        labelPrixEUR.setStyle("-fx-font-size: 13px; -fx-text-fill: #1565c0;" +
            "-fx-background-color: #e3f2fd; -fx-background-radius: 6; " +
            "-fx-padding: 3 9; -fx-font-weight: bold;");

        labelPrixUSD = new Label(String.format("$%.2f", prix * tauxUSD));
        labelPrixUSD.setStyle("-fx-font-size: 13px; -fx-text-fill: #e65100;" +
            "-fx-background-color: #fff8e1; -fx-background-radius: 6; " +
            "-fx-padding: 3 9; -fx-font-weight: bold;");

        HBox devises = new HBox(8, labelPrixEUR, labelPrixUSD);
        devises.setAlignment(Pos.CENTER_RIGHT);

        Label qtyCaption = new Label("Quantité disponible :");
        qtyCaption.setStyle("-fx-font-size: 11px; -fx-text-fill: #888;");

        String qtyColor = equipement.getQuantite() == 0 ? "#e74c3c"
                        : equipement.getQuantite() < 3  ? "#f39c12" : "#27ae60";
        labelQuantite = new Label("×" + equipement.getQuantite());
        labelQuantite.setStyle("-fx-background-color: " + qtyColor + "; -fx-text-fill: white;" +
            "-fx-background-radius: 20; -fx-padding: 5 16; " +
            "-fx-font-size: 15px; -fx-font-weight: bold;");

        prixBox.getChildren().addAll(labelPrix, devises, qtyCaption, labelQuantite);

        heroBody.getChildren().addAll(infoLeft, prixBox);
        heroCard.getChildren().addAll(topBand, heroBody);

        // ── CONTENU PRINCIPAL (IA + Articles côte à côte) ───────
        HBox mainContent = new HBox(16);
        mainContent.setAlignment(Pos.TOP_LEFT);
        mainContent.setPadding(new Insets(20));
        VBox.setVgrow(mainContent, Priority.ALWAYS);

        VBox iaPanel = construireIAPanel();
        HBox.setHgrow(iaPanel, Priority.ALWAYS);

        VBox articlesPanel = construireArticlesPanel();
        articlesPanel.setPrefWidth(380);
        articlesPanel.setMinWidth(300);

        mainContent.getChildren().addAll(iaPanel, articlesPanel);

        Region bottomSpacer = new Region();
        bottomSpacer.setPrefHeight(30);

        rootContainer.getChildren().addAll(topbar, heroCard, mainContent, bottomSpacer);
    }

    // ── Panneau Description IA ─────────────────────────────────

    private VBox construireIAPanel() {
        VBox panel = new VBox(12);
        panel.setStyle("-fx-background-color: white; -fx-background-radius: 16; -fx-padding: 20;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.07), 12, 0, 0, 3);");

        Label iconeIA = new Label("🤖");
        iconeIA.setStyle("-fx-font-size: 20px;");
        Label titreIA = new Label("Description générée par l'IA");
        titreIA.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #2d5a25;");

        labelIAStatus = new Label("Chargement...");
        labelIAStatus.setStyle("-fx-font-size: 11px; -fx-text-fill: #8e44ad;");
        progressIA = new ProgressIndicator(); progressIA.setPrefSize(14, 14);
        HBox statusRow = new HBox(8, labelIAStatus, progressIA);
        statusRow.setAlignment(Pos.CENTER_LEFT);
        VBox titreBox = new VBox(2, titreIA, statusRow);

        btnRegenIA = new Button("↻  Régénérer");
        btnRegenIA.setStyle("-fx-background-color: linear-gradient(to right, #6c3483, #8e44ad);" +
            "-fx-text-fill: white; -fx-background-radius: 8; -fx-font-size: 11px;" +
            "-fx-font-weight: bold; -fx-cursor: hand; -fx-padding: 7 14;");
        btnRegenIA.setOnAction(e -> lancerIAAsync());

        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
        HBox header = new HBox(10, iconeIA, titreBox, sp, btnRegenIA);
        header.setAlignment(Pos.CENTER_LEFT);

        Separator sep = new Separator(); sep.setStyle("-fx-background-color: #e8d8f5;");

        textAreaIA = new TextArea();
        textAreaIA.setWrapText(true); textAreaIA.setEditable(false); textAreaIA.setPrefHeight(220);
        textAreaIA.setStyle("-fx-font-size: 13px; -fx-font-family: 'Segoe UI';" +
            "-fx-text-fill: #2c3e50; -fx-background-color: #faf6fe; -fx-background-radius: 10;" +
            "-fx-border-color: #e0cff5; -fx-border-radius: 10; -fx-padding: 12;" +
            "-fx-control-inner-background: #faf6fe;");

        Label badge = new Label("Powered by  Groq · Llama3-8b");
        badge.setStyle("-fx-font-size: 10px; -fx-text-fill: #aaa;" +
            "-fx-background-color: #f5f0fa; -fx-background-radius: 5; -fx-padding: 3 9;");
        HBox badgeRow = new HBox(badge); badgeRow.setAlignment(Pos.CENTER_RIGHT);

        panel.getChildren().addAll(header, sep, textAreaIA, badgeRow);
        return panel;
    }

    // ── Panneau Articles ───────────────────────────────────────

    private VBox construireArticlesPanel() {
        VBox panel = new VBox(12);
        panel.setStyle("-fx-background-color: white; -fx-background-radius: 16; -fx-padding: 20;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.07), 12, 0, 0, 3);");

        Label iconeArt = new Label("📰"); iconeArt.setStyle("-fx-font-size: 20px;");
        Label titreArt = new Label("Articles & Actualités");
        titreArt.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #2d5a25;");

        labelArticlesStatus = new Label("Chargement...");
        labelArticlesStatus.setStyle("-fx-font-size: 11px; -fx-text-fill: #4a7c40;");
        progressArticles = new ProgressIndicator(); progressArticles.setPrefSize(14, 14);
        HBox statusRow = new HBox(8, labelArticlesStatus, progressArticles);
        statusRow.setAlignment(Pos.CENTER_LEFT);

        VBox titreBox = new VBox(2, titreArt, statusRow);
        HBox header = new HBox(10, iconeArt, titreBox); header.setAlignment(Pos.CENTER_LEFT);

        Separator sep = new Separator(); sep.setStyle("-fx-background-color: #d4edcf;");

        articlesContainer = new VBox(10);
        ScrollPane scroll = new ScrollPane(articlesContainer);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: transparent; -fx-border-color: transparent;");
        scroll.setPrefHeight(320); VBox.setVgrow(scroll, Priority.ALWAYS);

        panel.getChildren().addAll(header, sep, scroll);
        return panel;
    }

    // ═══════════════════════════════════════════════════════════════
    //  AJOUTER AU PANIER — Dialog moderne + Toast notification
    // ═══════════════════════════════════════════════════════════════

    private void ajouterAuPanier() {
        double prixUnit = parsePrix(equipement.getPrix());
        int    maxQty   = equipement.getQuantite();

        // ── Stage transparent custom ───────────────────────────────
        Stage dlg = new Stage();
        dlg.initModality(javafx.stage.Modality.APPLICATION_MODAL);
        dlg.initOwner(rootContainer.getScene().getWindow());
        dlg.initStyle(javafx.stage.StageStyle.TRANSPARENT);
        dlg.setResizable(false);

        // ── Racine avec ombre ──────────────────────────────────────
        VBox root = new VBox(0);
        root.setStyle(
            "-fx-background-color: white;" +
            "-fx-background-radius: 18;" +
            "-fx-effect: dropshadow(gaussian,rgba(0,0,0,0.38),30,0,0,8);");
        root.setPrefWidth(400);

        // ── HEADER dégradé vert ────────────────────────────────────
        VBox header = new VBox(5);
        header.setStyle(
            "-fx-background-color: linear-gradient(to bottom right,#1e3a1a,#4a7c40);" +
            "-fx-background-radius: 18 18 0 0;" +
            "-fx-padding: 20 24 16 24;");

        HBox titleRow = new HBox(12);
        titleRow.setAlignment(Pos.CENTER_LEFT);

        // Emoji type
        Label emojiLbl = new Label(typeEmoji(equipement.getType()));
        emojiLbl.setStyle("-fx-font-size: 30px;");

        VBox titleBox = new VBox(3);
        Label titleLbl = new Label("Ajouter au panier");
        titleLbl.setStyle(
            "-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: white;");
        Label nomLbl = new Label(equipement.getNom());
        nomLbl.setStyle(
            "-fx-font-size: 12px; -fx-text-fill: rgba(255,255,255,0.70); -fx-wrap-text: true;");
        nomLbl.setMaxWidth(260);
        titleBox.getChildren().addAll(titleLbl, nomLbl);
        titleRow.getChildren().addAll(emojiLbl, titleBox);

        // Prix unitaire
        Label prixLbl = new Label(String.format("%.2f TND  /  unité", prixUnit));
        prixLbl.setStyle(
            "-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #a8d8a0;");

        header.getChildren().addAll(titleRow, prixLbl);

        // ── BODY ───────────────────────────────────────────────────
        VBox body = new VBox(14);
        body.setPadding(new Insets(20, 24, 8, 24));

        // Badge stock
        String sc = maxQty == 0 ? "#e74c3c" : maxQty < 3 ? "#f39c12" : "#27ae60";
        String si = maxQty == 0 ? "❌" : maxQty < 3 ? "⚡" : "✅";
        String st = maxQty == 0 ? "Rupture de stock"
                  : maxQty < 3  ? "Stock faible : " + maxQty + " unité(s)"
                  : "En stock : " + maxQty + " unité(s) disponibles";
        HBox stockBadge = new HBox(8);
        stockBadge.setAlignment(Pos.CENTER_LEFT);
        stockBadge.setStyle(
            "-fx-background-color: " + sc + "18;" +
            "-fx-background-radius: 10; -fx-padding: 9 14;");
        Label siLbl = new Label(si); siLbl.setStyle("-fx-font-size: 14px;");
        Label stLbl = new Label(st);
        stLbl.setStyle(
            "-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: " + sc + ";");
        stockBadge.getChildren().addAll(siLbl, stLbl);

        // Label
        Label qtyLabel = new Label("Choisissez la quantité");
        qtyLabel.setStyle(
            "-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #4a7c40;");

        // Sélecteur  −  [valeur]  +
        HBox selector = new HBox(0);
        selector.setAlignment(Pos.CENTER_LEFT);
        selector.setStyle(
            "-fx-background-color: #f5f9f4;" +
            "-fx-background-radius: 12;" +
            "-fx-border-color: #c8ddc5;" +
            "-fx-border-radius: 12;" +
            "-fx-pref-width: 170;");

        Button btnM = new Button("−");
        btnM.setStyle(
            "-fx-background-color: transparent; -fx-text-fill: #4a7c40;" +
            "-fx-font-size: 22px; -fx-font-weight: bold; -fx-cursor: hand;" +
            "-fx-padding: 4 16; -fx-background-radius: 12 0 0 12;");
        btnM.setFocusTraversable(false);

        Spinner<Integer> spinner =
            new Spinner<>(1, Math.max(1, maxQty), 1);
        spinner.setEditable(false);
        spinner.setPrefWidth(74);
        spinner.setStyle(
            "-fx-background-color: transparent; -fx-border-color: transparent;");
        spinner.getEditor().setStyle(
            "-fx-background-color: transparent; -fx-font-size: 17px;" +
            "-fx-font-weight: bold; -fx-text-fill: #1e3a1a;" +
            "-fx-alignment: CENTER;");

        Button btnP = new Button("+");
        btnP.setStyle(
            "-fx-background-color: transparent; -fx-text-fill: #4a7c40;" +
            "-fx-font-size: 22px; -fx-font-weight: bold; -fx-cursor: hand;" +
            "-fx-padding: 4 16; -fx-background-radius: 0 12 12 0;");
        btnP.setFocusTraversable(false);

        btnM.setOnAction(e -> {
            int v = spinner.getValue(); if (v > 1) spinner.getValueFactory().setValue(v - 1);
        });
        btnP.setOnAction(e -> {
            int v = spinner.getValue(); if (v < maxQty) spinner.getValueFactory().setValue(v + 1);
        });
        selector.getChildren().addAll(btnM, spinner, btnP);

        // Total dynamique
        Label totalLbl = new Label(String.format("Total :   %.2f TND", prixUnit));
        totalLbl.setStyle(
            "-fx-font-size: 19px; -fx-font-weight: bold; -fx-text-fill: #2d5a25;" +
            "-fx-background-color: #e8f5e4; -fx-background-radius: 12;" +
            "-fx-padding: 11 18;");
        totalLbl.setMaxWidth(Double.MAX_VALUE);
        totalLbl.setAlignment(Pos.CENTER_RIGHT);
        spinner.valueProperty().addListener((obs, o, n) ->
            totalLbl.setText(String.format("Total :   %.2f TND", prixUnit * n)));

        body.getChildren().addAll(stockBadge, qtyLabel, selector, totalLbl);

        // ── ACTIONS ────────────────────────────────────────────────
        HBox actions = new HBox(10);
        actions.setPadding(new Insets(14, 24, 22, 24));
        actions.setAlignment(Pos.CENTER_RIGHT);

        Button btnCancel = new Button("Annuler");
        btnCancel.setStyle(
            "-fx-background-color: #ececec; -fx-text-fill: #666;" +
            "-fx-background-radius: 10; -fx-font-size: 13px;" +
            "-fx-cursor: hand; -fx-padding: 10 24;");
        btnCancel.setOnAction(e -> dlg.close());

        Button btnOk = new Button("🛒  Ajouter");
        btnOk.setDisable(maxQty == 0);
        btnOk.setStyle(
            "-fx-background-color: linear-gradient(to right,#1e3a1a,#4a7c40);" +
            "-fx-text-fill: white; -fx-background-radius: 10;" +
            "-fx-font-size: 13px; -fx-font-weight: bold;" +
            "-fx-cursor: hand; -fx-padding: 10 28;");
        btnOk.setOnMouseEntered(e -> {
            if (!btnOk.isDisabled()) btnOk.setStyle(
                "-fx-background-color: #3a6b2e; -fx-text-fill: white;" +
                "-fx-background-radius: 10; -fx-font-size: 13px; -fx-font-weight: bold;" +
                "-fx-cursor: hand; -fx-padding: 10 28;" +
                "-fx-effect: dropshadow(gaussian,rgba(74,124,64,0.5),10,0,0,3);");
        });
        btnOk.setOnMouseExited(e -> {
            if (!btnOk.isDisabled()) btnOk.setStyle(
                "-fx-background-color: linear-gradient(to right,#1e3a1a,#4a7c40);" +
                "-fx-text-fill: white; -fx-background-radius: 10;" +
                "-fx-font-size: 13px; -fx-font-weight: bold;" +
                "-fx-cursor: hand; -fx-padding: 10 28;");
        });
        btnOk.setOnAction(e -> {
            try {
                int qty    = spinner.getValue();
                double tot = prixUnit * qty;
                Panier panier = new Panier(
                    equipement.getId_equipement(), qty,
                    String.format("%.2f", tot),
                    AgriculteurController.ID_AGRICULTEUR);
                panierService.ajouter(panier);
                if (parentController != null) parentController.loadPanier();
                dlg.close();
                afficherToast(equipement.getNom(), qty, tot);
            } catch (SQLException ex) {
                showAlert("Erreur BD", ex.getMessage());
            }
        });

        actions.getChildren().addAll(btnCancel, btnOk);
        root.getChildren().addAll(header, body, actions);

        Scene scene = new Scene(root);
        scene.setFill(javafx.scene.paint.Color.TRANSPARENT);
        dlg.setScene(scene);
        dlg.showAndWait();
    }

    // ── Toast non-bloquant ──────────────────────────────────────────
    private void afficherToast(String nom, int qty, double total) {
        Stage toast = new Stage();
        toast.initOwner(rootContainer.getScene().getWindow());
        toast.initModality(javafx.stage.Modality.NONE);
        toast.initStyle(javafx.stage.StageStyle.TRANSPARENT);
        toast.setAlwaysOnTop(true);

        HBox box = new HBox(12);
        box.setAlignment(Pos.CENTER_LEFT);
        box.setStyle(
            "-fx-background-color: #1a3a16; -fx-background-radius: 14;" +
            "-fx-padding: 13 20;" +
            "-fx-effect: dropshadow(gaussian,rgba(0,0,0,0.55),22,0,0,6);");

        Label icon = new Label("🛒"); icon.setStyle("-fx-font-size: 20px;");
        VBox msg = new VBox(2);
        Label l1 = new Label("Ajouté au panier !");
        l1.setStyle("-fx-font-size:13px;-fx-font-weight:bold;-fx-text-fill:#7ddc6e;");
        Label l2 = new Label(nom + "  ×" + qty + "   —   " + String.format("%.2f TND", total));
        l2.setStyle("-fx-font-size:11px;-fx-text-fill:rgba(255,255,255,0.65);");
        msg.getChildren().addAll(l1, l2);

        Button closeBtn = new Button("✕");
        closeBtn.setStyle(
            "-fx-background-color:transparent;-fx-text-fill:rgba(255,255,255,0.45);" +
            "-fx-font-size:11px;-fx-cursor:hand;-fx-padding:0;");
        closeBtn.setOnAction(e -> toast.close());

        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
        box.getChildren().addAll(icon, msg, sp, closeBtn);

        Scene s = new Scene(box);
        s.setFill(javafx.scene.paint.Color.TRANSPARENT);
        toast.setScene(s);

        javafx.stage.Window owner = rootContainer.getScene().getWindow();
        toast.setX(owner.getX() + 30);
        toast.setY(owner.getY() + owner.getHeight() - 100);
        toast.show();

        javafx.animation.PauseTransition pt =
            new javafx.animation.PauseTransition(javafx.util.Duration.seconds(3.5));
        pt.setOnFinished(ev -> {
            javafx.animation.FadeTransition ft =
                new javafx.animation.FadeTransition(javafx.util.Duration.millis(600), box);
            ft.setFromValue(1.0); ft.setToValue(0.0);
            ft.setOnFinished(f -> toast.close());
            ft.play();
        });
        pt.play();
    }

    // ═══════════════════════════════════════════════════════════════
    //  IA — GROQ API
    // ═══════════════════════════════════════════════════════════════

    private void lancerIAAsync() {
        textAreaIA.setText("");
        labelIAStatus.setText("⏳  Génération par l'IA en cours...");
        labelIAStatus.setStyle("-fx-text-fill: #8e44ad; -fx-font-size: 11px;");
        progressIA.setVisible(true); btnRegenIA.setDisable(true);

        new Thread(() -> {
            String desc = genererDescriptionGroq();
            Platform.runLater(() -> {
                textAreaIA.setText(desc);
                labelIAStatus.setText("✅  Généré par Groq · Llama3");
                labelIAStatus.setStyle("-fx-text-fill: #27ae60; -fx-font-size: 11px;");
                progressIA.setVisible(false); btnRegenIA.setDisable(false);
            });
        }).start();
    }

    private String genererDescriptionGroq() {
        try {
            String prompt = String.format(
                "Tu es un expert en équipements agricoles. Génère une description commerciale " +
                "professionnelle et détaillée (5-6 phrases) pour cet équipement agricole tunisien :\n" +
                "- Nom : %s\n- Type : %s\n- Prix : %s TND\n- Quantité disponible : %d unités\n\n" +
                "Structure ta réponse en 3 parties :\n" +
                "1. Description générale et utilisation principale\n" +
                "2. Avantages pour l'agriculteur tunisien et conditions d'utilisation\n" +
                "3. Recommandation d'achat et rapport qualité/prix\n" +
                "Répondre en français uniquement.",
                equipement.getNom(), equipement.getType(),
                equipement.getPrix(), equipement.getQuantite());

            JSONObject body = new JSONObject();
            body.put("model", "llama3-8b-8192"); body.put("max_tokens", 500);
            JSONArray msgs = new JSONArray();
            JSONObject msg = new JSONObject();
            msg.put("role", "user"); msg.put("content", prompt);
            msgs.put(msg); body.put("messages", msgs);

            URL url = new URL(GROQ_API_URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Authorization", "Bearer " + GROQ_API_KEY);
            conn.setDoOutput(true);
            conn.setConnectTimeout(12000); conn.setReadTimeout(18000);
            try (OutputStream os = conn.getOutputStream())
                { os.write(body.toString().getBytes("UTF-8")); }

            BufferedReader reader = new BufferedReader(
                new InputStreamReader(conn.getInputStream(), "UTF-8"));
            StringBuilder sb = new StringBuilder(); String line;
            while ((line = reader.readLine()) != null) sb.append(line);
            reader.close();

            return new JSONObject(sb.toString())
                .getJSONArray("choices").getJSONObject(0)
                .getJSONObject("message").getString("content").trim();

        } catch (Exception e) { return genererDescriptionLocale(); }
    }

    private String genererDescriptionLocale() {
        double prix = parsePrix(equipement.getPrix());
        String usage = switch (equipement.getType().toLowerCase().trim()) {
            case "machinerie", "machine" ->
                "Cet équipement de machinerie lourde est indispensable pour mécaniser les travaux agricoles intensifs. " +
                "Il permet de réduire considérablement le temps de travail sur de grandes superficies.";
            case "outil", "outils" ->
                "Cet outil agricole de précision est conçu pour les travaux manuels ou semi-mécanisés. " +
                "Robuste et ergonomique, sa durabilité en fait un investissement rentable.";
            case "vehicule", "véhicule" ->
                "Ce véhicule agricole assure le transport et la logistique de l'exploitation. " +
                "Adapté aux terrains difficiles, il facilite le déplacement des récoltes.";
            case "irrigation" ->
                "Ce système d'irrigation garantit un apport en eau régulier et contrôlé. " +
                "Particulièrement adapté au climat semi-aride tunisien.";
            case "semence", "semences" ->
                "Ces semences sélectionnées offrent un taux de germination élevé. " +
                "Certifiées, elles garantissent une bonne récolte dès la première saison.";
            case "engrais" ->
                "Cet engrais enrichit le sol en nutriments essentiels. " +
                "Sa formulation équilibrée améliore la fertilité à long terme.";
            default ->
                "Cet équipement polyvalent est adapté aux exploitations tunisiennes modernes. " +
                "Sa conception robuste garantit une longue durée de vie.";
        };
        String stock = equipement.getQuantite() > 5 ? "📦 Stock disponible immédiatement."
                     : equipement.getQuantite() > 0 ? "⚡ Stock limité — commandez rapidement !"
                     : "❌ Actuellement en rupture de stock.";
        return String.format("🌾  %s — %s\n\n%s\n\n💡 Prix : %.2f TND — %s",
            equipement.getNom(), equipement.getType(), usage, prix, stock);
    }

    // ═══════════════════════════════════════════════════════════════
    //  ARTICLES — GNews API + clic → site web
    // ═══════════════════════════════════════════════════════════════

    private void lancerArticlesAsync() {
        labelArticlesStatus.setText("⏳  Recherche d'articles...");
        progressArticles.setVisible(true);
        articlesContainer.getChildren().clear();
        new Thread(this::chargerArticles).start();
    }

    private void chargerArticles() {
        try {
            // Recherche combinant le NOM de l'équipement + le TYPE pour des résultats pertinents
            String searchQuery = equipement.getNom() + " " + equipement.getType() + " agriculture";
            String query = URLEncoder.encode(searchQuery, "UTF-8");
            String json = appelHTTP("https://gnews.io/api/v4/search?q=" + query +
                "&lang=fr&max=4&token=" + GNEWS_API_KEY);
            JSONArray articles = new JSONObject(json).getJSONArray("articles");

            // Si 0 résultat avec le nom seul, on retente avec juste le type
            if (articles.length() == 0) {
                String fallbackQuery = URLEncoder.encode(
                    equipement.getType() + " agriculture Tunisie", "UTF-8");
                json = appelHTTP("https://gnews.io/api/v4/search?q=" + fallbackQuery +
                    "&lang=fr&max=4&token=" + GNEWS_API_KEY);
                articles = new JSONObject(json).getJSONArray("articles");
            }

            final JSONArray finalArticles = articles;
            Platform.runLater(() -> {
                progressArticles.setVisible(false);
                if (finalArticles.length() == 0) {
                    labelArticlesStatus.setText("📰  Articles suggérés");
                    afficherFallback(); return;
                }
                labelArticlesStatus.setText("📰  " + finalArticles.length() + " article(s) liés à « "
                    + equipement.getNom() + " »");
                articlesContainer.getChildren().clear();
                for (int i = 0; i < finalArticles.length(); i++) {
                    try {
                        JSONObject a   = finalArticles.getJSONObject(i);
                        String titre   = a.optString("title", "Sans titre");
                        String desc    = a.optString("description", "");
                        String source  = a.optJSONObject("source") != null
                            ? a.getJSONObject("source").optString("name", "") : "";
                        // Formatage de la date en "Mis à jour le JJ/MM/AAAA"
                        String rawDate = a.optString("publishedAt", "");
                        String dateFormatted = formaterDate(rawDate);
                        String aUrl    = a.optString("url", "");
                        articlesContainer.getChildren().add(
                            creerCarteArticle(titre, desc, source, dateFormatted, aUrl));
                    } catch (Exception ignored) {}
                }
            });
        } catch (Exception e) {
            Platform.runLater(() -> {
                progressArticles.setVisible(false);
                labelArticlesStatus.setText("📰  Articles suggérés pour « " + equipement.getNom() + " »");
                afficherFallback();
            });
        }
    }

    /**
     * Formate une date ISO 8601 (ex: "2024-11-15T10:30:00Z")
     * en "Mis à jour le 15/11/2024"
     */
    private String formaterDate(String rawDate) {
        if (rawDate == null || rawDate.isEmpty()) return "";
        try {
            // Format: 2024-11-15T10:30:00Z
            String[] parts = rawDate.split("T")[0].split("-");
            if (parts.length == 3) {
                return "Mis à jour le " + parts[2] + "/" + parts[1] + "/" + parts[0];
            }
        } catch (Exception ignored) {}
        return rawDate.replace("T", " ").replace("Z", "");
    }

    private void afficherFallback() {
        articlesContainer.getChildren().clear();
        String nom  = equipement.getNom();
        String type = equipement.getType();

        // Encodage du nom pour les URLs de recherche
        String nomEncode  = "";
        String typeEncode = "";
        try {
            nomEncode  = URLEncoder.encode(nom,  "UTF-8");
            typeEncode = URLEncoder.encode(type + " agriculture Tunisie", "UTF-8");
        } catch (Exception ignored) {}

        // Articles fallback avec vraies URLs de recherche Google — clic ouvre le navigateur
        String[][] arts = {
            {
                "Rechercher \"" + nom + "\" sur Google",
                "Cliquez pour trouver des informations, prix et fournisseurs pour : " + nom + ".",
                "Google Search",
                "Mis a jour maintenant",
                "https://www.google.com/search?q=" + nomEncode
            },
            {
                nom + " - actualites agriculture Tunisie",
                "Dernieres informations sur l'utilisation de " + nom + " dans le secteur agricole tunisien.",
                "Google Actualites",
                "Mis a jour maintenant",
                "https://news.google.com/search?q=" + nomEncode + "&hl=fr"
            },
            {
                "Equipements " + type + " - ONAGRI",
                "Portail officiel tunisien pour les statistiques et actualites sur les equipements agricoles de type " + type + ".",
                "ONAGRI",
                "Mis a jour regulierement",
                "https://www.onagri.nat.tn"
            },
            {
                "Subventions et aides pour " + type + " en Tunisie",
                "Le CRDA et le ministere de l'Agriculture proposent des financements avantageux. Cliquez pour en savoir plus.",
                "Ministere Agriculture TN",
                "Mis a jour regulierement",
                "https://www.google.com/search?q=" + typeEncode + "+subvention+CRDA"
            }
        };
        for (String[] a : arts)
            articlesContainer.getChildren().add(creerCarteArticle(a[0], a[1], a[2], a[3], a[4]));
    }

    /**
     * Carte article — clic sur toute la carte OU sur "Lire l'article" → ouvre le navigateur.
     */
    private VBox creerCarteArticle(String titre, String description,
                                   String source, String date, String url) {
        VBox card = new VBox(8);

        boolean hasUrl = url != null && !url.isEmpty();

        card.setStyle(styleArticle(false, hasUrl));
        if (hasUrl) {
            card.setOnMouseEntered(e -> card.setStyle(styleArticle(true, true)));
            card.setOnMouseExited(e  -> card.setStyle(styleArticle(false, true)));
            // Clic sur la carte → ouvre le site dans le navigateur par défaut
            card.setOnMouseClicked(e -> ouvrirLien(url));
        }

        Label iconNews = new Label("📰"); iconNews.setStyle("-fx-font-size: 16px;");

        Label lblTitre = new Label(titre);
        lblTitre.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; " +
            "-fx-text-fill: " + (hasUrl ? "#1565c0" : "#1e3a1a") + "; -fx-wrap-text: true;");
        lblTitre.setMaxWidth(300);

        HBox meta = new HBox(10);
        if (!source.isEmpty()) {
            Label lblSrc = new Label("🔗 " + source);
            lblSrc.setStyle("-fx-font-size: 10px; -fx-text-fill: #4a7c40;" +
                "-fx-background-color: #e8f5e4; -fx-background-radius: 5; " +
                "-fx-padding: 2 7; -fx-font-weight: bold;");
            meta.getChildren().add(lblSrc);
        }
        if (!date.isEmpty()) {
            // Affiche la date telle quelle (déjà formatée "Mis à jour le ...")
            Label lblDate = new Label("🕐 " + date);
            lblDate.setStyle("-fx-font-size: 10px; -fx-text-fill: #888;" +
                "-fx-background-color: #f8f8f8; -fx-background-radius: 5; -fx-padding: 2 6;");
            meta.getChildren().add(lblDate);
        }
        if (hasUrl) {
            Label lblWeb = new Label("🌐 Cliquer pour lire");
            lblWeb.setStyle("-fx-font-size: 10px; -fx-text-fill: #2980b9; -fx-font-style: italic;");
            meta.getChildren().add(lblWeb);
        }

        VBox titreBox = new VBox(4, lblTitre, meta);
        HBox header = new HBox(8, iconNews, titreBox);
        header.setAlignment(Pos.TOP_LEFT);

        VBox content = new VBox(6);
        if (!description.isEmpty() && !description.equals("null")) {
            Label lblDesc = new Label(description.length() > 200
                ? description.substring(0, 200) + "…" : description);
            lblDesc.setStyle("-fx-font-size: 12px; -fx-text-fill: #5a7a50; -fx-wrap-text: true;");
            lblDesc.setMaxWidth(320);
            content.getChildren().add(lblDesc);
        }

        card.getChildren().addAll(header, content);
        return card;
    }

    private String styleArticle(boolean hover, boolean hasUrl) {
        String border = hover ? "#4a7c40" : "#e8f2e6";
        String bg     = hover ? "#f4fbf2" : "white";
        String shadow = hover
            ? "-fx-effect: dropshadow(gaussian, rgba(74,124,64,0.2), 12, 0, 0, 3);"
            : "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.07), 10, 0, 0, 2);";
        String cursor = hasUrl ? "-fx-cursor: hand;" : "";
        return "-fx-background-color: " + bg + "; -fx-background-radius: 12; -fx-padding: 14 16;" +
            shadow + "-fx-border-color: " + border + "; -fx-border-radius: 12;" + cursor;
    }

    /** Ouvre une URL dans le navigateur système par défaut */
    private void ouvrirLien(String url) {
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(new URI(url));
            } else {
                // Fallback Linux/headless
                new ProcessBuilder("xdg-open", url).start();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            showAlert("Navigateur", "Impossible d'ouvrir : " + url);
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  NAVIGATION
    // ═══════════════════════════════════════════════════════════════

    private void goBack() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/fxml/Agriculteur.fxml"));
            NavigationUtil.loadInContentArea(rootContainer, root);
        } catch (Exception e) { e.printStackTrace(); }
    }

    // ═══════════════════════════════════════════════════════════════
    //  UTILITAIRES
    // ═══════════════════════════════════════════════════════════════

    private String styleBtnBack(boolean hover) {
        String bg = hover ? "rgba(255,255,255,0.28)" : "rgba(255,255,255,0.15)";
        return "-fx-background-color: " + bg + "; -fx-text-fill: white;" +
            "-fx-background-radius: 8; -fx-font-size: 12px; " +
            "-fx-font-weight: bold; -fx-cursor: hand; -fx-padding: 7 14;";
    }

    private String typeColor(String type) {
        if (type == null) return "#4a7c40";
        return switch (type.toLowerCase().trim()) {
            case "machinerie", "machine" -> "#2980b9";
            case "outil", "outils"       -> "#f39c12";
            case "vehicule", "véhicule"  -> "#8e44ad";
            case "irrigation"            -> "#16a085";
            case "semence", "semences"   -> "#27ae60";
            case "engrais"               -> "#d35400";
            default                      -> "#4a7c40";
        };
    }

    private String typeEmoji(String type) {
        if (type == null) return "📦";
        return switch (type.toLowerCase().trim()) {
            case "machinerie", "machine" -> "🚜";
            case "outil", "outils"       -> "🔧";
            case "vehicule", "véhicule"  -> "🚛";
            case "irrigation"            -> "💧";
            case "semence", "semences"   -> "🌱";
            case "engrais"               -> "🌿";
            default                      -> "📦";
        };
    }

    private double parsePrix(String s) {
        try { return Double.parseDouble(s.replace(",", ".")); }
        catch (Exception e) { return 0; }
    }

    private String appelHTTP(String urlStr) throws Exception {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(8000); conn.setReadTimeout(10000);
        conn.setRequestProperty("User-Agent", "AgroManager/1.0");
        BufferedReader r = new BufferedReader(
            new InputStreamReader(conn.getInputStream(), "UTF-8"));
        StringBuilder sb = new StringBuilder(); String line;
        while ((line = r.readLine()) != null) sb.append(line);
        r.close(); return sb.toString();
    }

    private void showAlert(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle(title); a.setHeaderText(null); a.setContentText(msg); a.showAndWait();
    }
}
