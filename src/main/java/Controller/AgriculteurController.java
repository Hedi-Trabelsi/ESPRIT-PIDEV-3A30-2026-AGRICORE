package Controller;

import Model.Equipement;
import Model.Panier;
import services.EquipementService;
import services.PanierService;
import utils.ImageManager;

import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
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
import javafx.util.Duration;

import org.json.JSONObject;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.SQLException;
import java.util.List;
import java.util.ResourceBundle;

public class AgriculteurController implements Initializable {

    // ── Catalogue ────────────────────────────────────────────────
    @FXML private FlowPane  gridCatalogue;
    @FXML private VBox      emptyCatalogue;
    @FXML private TextField fieldSearchCatalogue;

    // ── Panier ───────────────────────────────────────────────────
    @FXML private FlowPane gridPanier;
    @FXML private VBox     emptyPanier;

    // ── Sidebar ──────────────────────────────────────────────────
    @FXML private Label labelSousTotal, labelGrandTotal, labelCartBadge, labelTotalPanier;
    @FXML private Label labelNbArticles, labelSousTotalDetail, labelGrandTotalDetail;

    // ── ExchangeRate totaux ───────────────────────────────────────
    @FXML private Label labelTotalTND, labelTotalEUR, labelTotalUSD;

    // ── Labels World Bank (masqués) ───────────────────────────────
    @FXML private Label labelAgriPIB, labelAgriTerres, labelAgriEmploi, labelAgriExport;

    // ── Tabs ─────────────────────────────────────────────────────
    @FXML private VBox viewCatalogue, viewPanier;
    @FXML private HBox btnTabCatalogue, btnTabPanier;

    // ── Services / data ──────────────────────────────────────────
    private final EquipementService    equipementService;

    {
        try {
            equipementService = new EquipementService();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private final PanierService        panierService     = new PanierService();
    private ObservableList<Equipement> catalogueList     = FXCollections.observableArrayList();
    private ObservableList<Panier>     panierList        = FXCollections.observableArrayList();
    private List<Equipement>           allEquipements;

    static final int    ID_AGRICULTEUR   = 1;
    private static final String EXCHANGE_API_KEY = "0c3f3b97f846b6f5ced36eff";

    double tauxEUR = 0.2981;
    double tauxUSD = 0.3213;

    // ═══════════════════════════════════════════════════════════════
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        loadCatalogue();
        loadPanier();
        chargerTauxDeChange();
        if (labelAgriPIB    != null) { labelAgriPIB.setText("—");    labelAgriPIB.setVisible(false);    }
        if (labelAgriTerres != null) { labelAgriTerres.setText("—"); labelAgriTerres.setVisible(false); }
        if (labelAgriEmploi != null) { labelAgriEmploi.setText("—"); labelAgriEmploi.setVisible(false); }
        if (labelAgriExport != null) { labelAgriExport.setText("—"); labelAgriExport.setVisible(false); }
    }

    // ═══════════════════════════════════════════════════════════════
    //  CATALOGUE
    // ═══════════════════════════════════════════════════════════════

    private void loadCatalogue() {
        try {
            allEquipements = equipementService.afficher();
            catalogueList.setAll(allEquipements);
            renderCatalogueGrid(catalogueList);
        } catch (SQLException e) {
            showAlert("Erreur", "Impossible de charger le catalogue : " + e.getMessage());
        }
    }

    @FXML
    private void filterCatalogue() {
        String s = fieldSearchCatalogue.getText().toLowerCase().trim();
        List<Equipement> filtered = s.isEmpty() ? catalogueList
            : catalogueList.filtered(eq ->
                eq.getNom().toLowerCase().contains(s) ||
                eq.getType().toLowerCase().contains(s));
        renderCatalogueGrid(filtered);
    }

    private void renderCatalogueGrid(List<Equipement> list) {
        gridCatalogue.getChildren().clear();
        boolean empty = list.isEmpty();
        emptyCatalogue.setVisible(empty);  emptyCatalogue.setManaged(empty);
        gridCatalogue.setVisible(!empty);  gridCatalogue.setManaged(!empty);
        if (!empty)
            for (Equipement eq : list)
                gridCatalogue.getChildren().add(createCatalogueCard(eq));
    }

    /**
     * Carte catalogue avec IMAGE en haut de la carte.
     * Clic sur la carte → page DetailEquipement.
     */
    private VBox createCatalogueCard(Equipement eq) {
        VBox card = new VBox(0);
        card.setPrefWidth(270); card.setMaxWidth(270);
        card.setStyle(styleCard(false));
        card.setOnMouseEntered(e -> card.setStyle(styleCard(true)));
        card.setOnMouseExited(e  -> card.setStyle(styleCard(false)));
        card.setOnMouseClicked(e -> ouvrirDetail(eq));

        String color = typeColor(eq.getType());

        // ── Bande couleur ────────────────────────────────────────
        HBox band = new HBox(); band.setPrefHeight(7);
        band.setStyle("-fx-background-color: " + color + "; -fx-background-radius: 16 16 0 0;");

        // ── IMAGE ou placeholder ──────────────────────────────────
        StackPane imagePane = ImageManager.creerVignetteImage(
            eq.getNom(), 270, 130, typeEmoji(eq.getType()), color + "18");

        // ── Corps ─────────────────────────────────────────────────
        VBox body = new VBox(10);
        body.setPadding(new Insets(13, 18, 16, 18));

        HBox header = new HBox(8); header.setAlignment(Pos.CENTER_LEFT);
        Label iconLbl = new Label(typeEmoji(eq.getType()));
        iconLbl.setStyle("-fx-font-size: 22px;");
        VBox namePart = new VBox(3);
        Label nomLbl = new Label(eq.getNom());
        nomLbl.setStyle("-fx-font-size: 13px; -fx-font-weight: bold;" +
            "-fx-text-fill: #1e3a1a; -fx-wrap-text: true;");
        nomLbl.setMaxWidth(185);
        Label badgeLbl = new Label(eq.getType());
        badgeLbl.setStyle("-fx-background-color: " + color + "22; -fx-text-fill: " + color + ";" +
            "-fx-background-radius: 20; -fx-padding: 2 8; -fx-font-size: 10px; -fx-font-weight: bold;");
        namePart.getChildren().addAll(nomLbl, badgeLbl);
        header.getChildren().addAll(iconLbl, namePart);

        Separator sep = new Separator();
        sep.setStyle("-fx-background-color: #eef5ec;");

        double prixNum = parsePrix(eq.getPrix());
        Label prixLbl = new Label(String.format("%.2f TND", prixNum));
        prixLbl.setStyle("-fx-font-size: 17px; -fx-font-weight: bold; -fx-text-fill: #2d5a25;");

        HBox devises = new HBox(8);
        Label eurLbl = new Label(String.format("%.2f €", prixNum * tauxEUR));
        eurLbl.setStyle("-fx-font-size: 11px; -fx-text-fill: #1565c0;" +
            "-fx-background-color: #e3f2fd; -fx-background-radius: 6; -fx-padding: 2 7; -fx-font-weight: bold;");
        Label usdLbl = new Label(String.format("$%.2f", prixNum * tauxUSD));
        usdLbl.setStyle("-fx-font-size: 11px; -fx-text-fill: #e65100;" +
            "-fx-background-color: #fff8e1; -fx-background-radius: 6; -fx-padding: 2 7; -fx-font-weight: bold;");
        devises.getChildren().addAll(eurLbl, usdLbl);

        Label stockLbl = makeStockLabel(eq.getQuantite());
        Label hint = new Label("🔍  Cliquer pour détail, IA & articles");
        hint.setStyle("-fx-font-size: 9px; -fx-text-fill: #aaa; -fx-font-style: italic;");

        body.getChildren().addAll(header, sep, prixLbl, devises, stockLbl, hint);

        // ── Assemblage : bande + image + corps ─────────────────────
        card.getChildren().addAll(band, imagePane, body);
        return card;
    }

    // ═══════════════════════════════════════════════════════════════
    //  PANIER
    // ═══════════════════════════════════════════════════════════════

    void loadPanier() {
        try {
            List<Panier> list = panierService.afficher();
            panierList.setAll(list.stream()
                .filter(p -> p.getId_agriculteur() == ID_AGRICULTEUR).toList());
            renderPanierGrid(panierList);
            updatePanierSummary();
        } catch (SQLException e) {
            showAlert("Erreur", "Impossible de charger le panier : " + e.getMessage());
        }
    }

    private void renderPanierGrid(List<Panier> list) {
        gridPanier.getChildren().clear();
        boolean empty = list.isEmpty();
        emptyPanier.setVisible(empty);  emptyPanier.setManaged(empty);
        gridPanier.setVisible(!empty);  gridPanier.setManaged(!empty);
        if (!empty)
            for (Panier p : list)
                gridPanier.getChildren().add(createPanierCard(p));
    }

    /**
     * Carte panier avec IMAGE en haut.
     */
    private VBox createPanierCard(Panier p) {
        Equipement eq = getEquipementById(p.getId_equipement());
        String nom    = eq != null ? eq.getNom()  : "Équipement #" + p.getId_equipement();
        String type   = eq != null ? eq.getType() : "";
        String color  = typeColor(type);

        VBox card = new VBox(0);
        card.setPrefWidth(300); card.setMaxWidth(300);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 16;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 12, 0, 0, 3);");

        HBox band = new HBox(); band.setPrefHeight(7);
        band.setStyle("-fx-background-color: " + color + "; -fx-background-radius: 16 16 0 0;");

        // ── IMAGE ou placeholder ──────────────────────────────────
        StackPane imagePane = ImageManager.creerVignetteImage(
            nom, 300, 120, typeEmoji(type), color + "18");

        // ── Corps ─────────────────────────────────────────────────
        VBox body = new VBox(12);
        body.setPadding(new Insets(16, 18, 18, 18));

        HBox header = new HBox(10); header.setAlignment(Pos.CENTER_LEFT);
        Label iconLbl = new Label(typeEmoji(type)); iconLbl.setStyle("-fx-font-size: 26px;");
        VBox namePart = new VBox(3);
        Label nomLbl = new Label(nom);
        nomLbl.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;" +
            "-fx-text-fill: #1e3a1a; -fx-wrap-text: true;");
        nomLbl.setMaxWidth(210);
        Label badgeLbl = new Label(type.isEmpty() ? "Équipement" : type);
        badgeLbl.setStyle("-fx-background-color: " + color + "22; -fx-text-fill: " + color + ";" +
            "-fx-background-radius: 20; -fx-padding: 2 8; -fx-font-size: 10px; -fx-font-weight: bold;");
        namePart.getChildren().addAll(nomLbl, badgeLbl);
        header.getChildren().addAll(iconLbl, namePart);

        Separator sep = new Separator(); sep.setStyle("-fx-background-color: #eef5ec;");

        HBox qtyRow = new HBox(10); qtyRow.setAlignment(Pos.CENTER_LEFT);
        Label boxIcon = new Label("📦"); boxIcon.setStyle("-fx-font-size: 14px;");
        Label qtyCaption = new Label("Quantité commandée :");
        qtyCaption.setStyle("-fx-font-size: 12px; -fx-text-fill: #5a7a50;");
        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
        Label qtyVal = new Label("×" + p.getQuantite());
        qtyVal.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: white;" +
            "-fx-background-color: #4a7c40; -fx-background-radius: 20; -fx-padding: 4 12;");
        qtyRow.getChildren().addAll(boxIcon, qtyCaption, sp, qtyVal);

        double tnd = parsePrix(p.getTotal());
        HBox totalBox = new HBox(10); totalBox.setAlignment(Pos.CENTER_LEFT);
        totalBox.setStyle("-fx-background-color: #e8f5e4; -fx-background-radius: 8; -fx-padding: 9 12;");
        Label totalCaption = new Label("Total :");
        totalCaption.setStyle("-fx-font-size: 11px; -fx-text-fill: #5a7a50;");
        Region sp2 = new Region(); HBox.setHgrow(sp2, Priority.ALWAYS);
        Label totalVal = new Label(String.format("%.2f TND", tnd));
        totalVal.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #2d5a25;");
        totalBox.getChildren().addAll(totalCaption, sp2, totalVal);

        HBox devises = new HBox(8); devises.setAlignment(Pos.CENTER_RIGHT);
        Label eurLbl = new Label(String.format("%.2f €", tnd * tauxEUR));
        eurLbl.setStyle("-fx-font-size: 11px; -fx-text-fill: #1565c0;" +
            "-fx-background-color: #e3f2fd; -fx-background-radius: 6; -fx-padding: 3 8; -fx-font-weight: bold;");
        Label usdLbl = new Label(String.format("$%.2f", tnd * tauxUSD));
        usdLbl.setStyle("-fx-font-size: 11px; -fx-text-fill: #e65100;" +
            "-fx-background-color: #fff8e1; -fx-background-radius: 6; -fx-padding: 3 8; -fx-font-weight: bold;");
        devises.getChildren().addAll(eurLbl, usdLbl);

        Button btnRetirer = new Button("🗑  Retirer du panier");
        btnRetirer.setMaxWidth(Double.MAX_VALUE);
        btnRetirer.setStyle("-fx-background-color: #fdf0f0; -fx-text-fill: #e74c3c;" +
            "-fx-background-radius: 9; -fx-font-size: 12px; -fx-font-weight: bold;" +
            "-fx-cursor: hand; -fx-padding: 9 0; -fx-border-color: #fac0c0; -fx-border-radius: 9;");
        btnRetirer.setOnAction(e -> retirerDuPanier(p.getId_panier()));
        btnRetirer.setOnMouseEntered(e -> btnRetirer.setStyle(
            "-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-background-radius: 9;" +
            "-fx-font-size: 12px; -fx-font-weight: bold; -fx-cursor: hand; -fx-padding: 9 0;"));
        btnRetirer.setOnMouseExited(e -> btnRetirer.setStyle(
            "-fx-background-color: #fdf0f0; -fx-text-fill: #e74c3c; -fx-background-radius: 9;" +
            "-fx-font-size: 12px; -fx-font-weight: bold; -fx-cursor: hand; -fx-padding: 9 0;" +
            "-fx-border-color: #fac0c0; -fx-border-radius: 9;"));

        body.getChildren().addAll(header, sep, qtyRow, totalBox, devises, btnRetirer);

        // ── Assemblage : bande + image + corps ─────────────────────
        card.getChildren().addAll(band, imagePane, body);
        return card;
    }

    private void retirerDuPanier(int id) {
        try { panierService.supprimer(id); loadPanier(); }
        catch (SQLException e) { showAlert("Erreur", "Impossible de retirer : " + e.getMessage()); }
    }

    @FXML
    private void confirmerCommande() {
        if (panierList.isEmpty()) {
            showAlert("Panier vide", "Ajoutez des équipements avant de commander.");
            return;
        }
        double total = calcTotal();

        Stage dlg = new Stage();
        dlg.initModality(javafx.stage.Modality.APPLICATION_MODAL);
        dlg.initOwner(gridPanier.getScene().getWindow());
        dlg.initStyle(javafx.stage.StageStyle.TRANSPARENT);
        dlg.setResizable(false);

        VBox root = new VBox(0);
        root.setStyle("-fx-background-color: white; -fx-background-radius: 18;" +
            "-fx-effect: dropshadow(gaussian,rgba(0,0,0,0.38),28,0,0,8);");
        root.setPrefWidth(440);

        VBox hdr = new VBox(5);
        hdr.setStyle("-fx-background-color: linear-gradient(to right,#1e3a1a,#4a7c40);" +
            "-fx-background-radius: 18 18 0 0; -fx-padding: 20 26 16 26;");
        Label htitle = new Label("✅  Confirmer la commande");
        htitle.setStyle("-fx-font-size:16px;-fx-font-weight:bold;-fx-text-fill:white;");
        Label hsub = new Label(panierList.size() + " article(s)  —  PDF généré automatiquement");
        hsub.setStyle("-fx-font-size:11px;-fx-text-fill:rgba(255,255,255,0.65);");
        hdr.getChildren().addAll(htitle, hsub);

        VBox body = new VBox(12);
        body.setPadding(new Insets(20, 26, 8, 26));

        HBox rowTND = new HBox(10); rowTND.setAlignment(Pos.CENTER_LEFT);
        rowTND.setStyle("-fx-background-color:#e8f5e4;-fx-background-radius:8;-fx-padding:10 14;");
        Label lTND = new Label("🇹🇳  TOTAL TND");
        lTND.setStyle("-fx-font-size:13px;-fx-font-weight:bold;-fx-text-fill:#1e3a1a;");
        Region s1 = new Region(); HBox.setHgrow(s1, Priority.ALWAYS);
        Label vTND = new Label(String.format("%.2f TND", total));
        vTND.setStyle("-fx-font-size:16px;-fx-font-weight:bold;-fx-text-fill:#2d5a25;");
        rowTND.getChildren().addAll(lTND, s1, vTND);

        HBox rowDevises = new HBox(10);
        HBox rowEUR = new HBox(8); rowEUR.setAlignment(Pos.CENTER_LEFT); HBox.setHgrow(rowEUR, Priority.ALWAYS);
        rowEUR.setStyle("-fx-background-color:#e3f2fd;-fx-background-radius:8;-fx-padding:8 12;");
        Label lEUR = new Label("🇪🇺 EUR"); lEUR.setStyle("-fx-font-size:11px;-fx-font-weight:bold;-fx-text-fill:#1565c0;");
        Region s2=new Region(); HBox.setHgrow(s2, Priority.ALWAYS);
        Label vEUR = new Label(String.format("%.2f €", total * tauxEUR));
        vEUR.setStyle("-fx-font-size:13px;-fx-font-weight:bold;-fx-text-fill:#1565c0;");
        rowEUR.getChildren().addAll(lEUR,s2,vEUR);
        HBox rowUSD = new HBox(8); rowUSD.setAlignment(Pos.CENTER_LEFT); HBox.setHgrow(rowUSD, Priority.ALWAYS);
        rowUSD.setStyle("-fx-background-color:#fff8e1;-fx-background-radius:8;-fx-padding:8 12;");
        Label lUSD = new Label("🇺🇸 USD"); lUSD.setStyle("-fx-font-size:11px;-fx-font-weight:bold;-fx-text-fill:#e65100;");
        Region s3=new Region(); HBox.setHgrow(s3, Priority.ALWAYS);
        Label vUSD = new Label(String.format("$%.2f", total * tauxUSD));
        vUSD.setStyle("-fx-font-size:13px;-fx-font-weight:bold;-fx-text-fill:#e65100;");
        rowUSD.getChildren().addAll(lUSD,s3,vUSD);
        rowDevises.getChildren().addAll(rowEUR, rowUSD);

        Label pdfNote = new Label("📄  Un bon de commande PDF sera enregistré dans vos Documents.");
        pdfNote.setStyle("-fx-font-size:11px;-fx-text-fill:#888;-fx-font-style:italic;");
        pdfNote.setWrapText(true);
        body.getChildren().addAll(rowTND, rowDevises, pdfNote);

        HBox actions = new HBox(10);
        actions.setPadding(new Insets(16, 26, 22, 26));
        actions.setAlignment(Pos.CENTER_RIGHT);
        Button btnNo = new Button("Annuler");
        btnNo.setStyle("-fx-background-color:#ececec;-fx-text-fill:#666;-fx-background-radius:10;" +
            "-fx-font-size:13px;-fx-cursor:hand;-fx-padding:10 22;");
        btnNo.setOnAction(e -> dlg.close());
        Button btnYes = new Button("✅  Confirmer & Télécharger PDF");
        btnYes.setStyle("-fx-background-color:linear-gradient(to right,#1e3a1a,#4a7c40);" +
            "-fx-text-fill:white;-fx-background-radius:10;-fx-font-size:13px;" +
            "-fx-font-weight:bold;-fx-cursor:hand;-fx-padding:10 22;");
        btnYes.setOnAction(e -> { dlg.close(); genererPDF(); });
        actions.getChildren().addAll(btnNo, btnYes);

        root.getChildren().addAll(hdr, body, actions);
        Scene sc = new Scene(root);
        sc.setFill(javafx.scene.paint.Color.TRANSPARENT);
        dlg.setScene(sc);
        dlg.showAndWait();
    }

    // ═══════════════════════════════════════════════════════════════
    //  GÉNÉRATION PDF (inchangée)
    // ═══════════════════════════════════════════════════════════════

    private void genererPDF() {
        new Thread(() -> {
            try {
                String numero  = new java.text.SimpleDateFormat("yyyyMMdd-HHmm").format(new java.util.Date());
                String numFull = "AGM-" + numero;
                String date    = new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm").format(new java.util.Date());
                String docDir  = System.getProperty("user.home") + File.separator + "Documents";
                new File(docDir).mkdirs();
                String outPath = docDir + File.separator + "Commande_" + numFull + ".pdf";
                java.awt.Color cVert    = new java.awt.Color(0x1e, 0x3a, 0x1a);
                java.awt.Color cVertMed = new java.awt.Color(0x2d, 0x5a, 0x25);
                java.awt.Color cVertCl  = new java.awt.Color(0x4a, 0x7c, 0x40);
                java.awt.Color cVertPale= new java.awt.Color(0xe8, 0xf5, 0xe4);
                java.awt.Color cBlanc   = java.awt.Color.WHITE;
                java.awt.Color cGrisCl  = new java.awt.Color(0xf3, 0xf3, 0xf3);
                java.awt.Color cGrisTxt = new java.awt.Color(0x44, 0x44, 0x44);
                java.awt.Color cBleu    = new java.awt.Color(0x15, 0x65, 0xc0);
                java.awt.Color cOrange  = new java.awt.Color(0xe6, 0x51, 0x00);
                org.apache.pdfbox.pdmodel.PDDocument doc =
                    new org.apache.pdfbox.pdmodel.PDDocument();
                org.apache.pdfbox.pdmodel.PDPage page =
                    new org.apache.pdfbox.pdmodel.PDPage(
                        org.apache.pdfbox.pdmodel.common.PDRectangle.A4);
                doc.addPage(page);
                float PW = page.getMediaBox().getWidth();
                float PH = page.getMediaBox().getHeight();
                float mx = 36f;
                org.apache.pdfbox.pdmodel.font.PDType1Font fontReg =
                    new org.apache.pdfbox.pdmodel.font.PDType1Font(
                        org.apache.pdfbox.pdmodel.font.Standard14Fonts.FontName.HELVETICA);
                org.apache.pdfbox.pdmodel.font.PDType1Font fontBold =
                    new org.apache.pdfbox.pdmodel.font.PDType1Font(
                        org.apache.pdfbox.pdmodel.font.Standard14Fonts.FontName.HELVETICA_BOLD);
                org.apache.pdfbox.pdmodel.PDPageContentStream cs =
                    new org.apache.pdfbox.pdmodel.PDPageContentStream(doc, page);
                fillRect(cs, cVert, 0, PH - 68f, PW, 68f);
                float titleW = tw(fontBold, 24f, "AgriCore");
                text(cs, fontBold, 24f, java.awt.Color.WHITE, (PW - titleW) / 2f, PH - 30f, "AgriCore");
                float subW = tw(fontReg, 10f, "Gestion des equipements agricoles");
                text(cs, fontReg, 10f, new java.awt.Color(180,230,160), (PW - subW) / 2f, PH - 48f,
                    "Gestion des equipements agricoles");
                float y = PH - 82f;
                fillRect(cs, cVertPale, mx, y - 44f, PW - 2*mx, 44f);
                fillRect(cs, cVertCl, mx, y - 44f, 4f, 44f);
                text(cs, fontBold, 13f, cVert, mx + 12f, y - 17f, "BON DE COMMANDE  N  " + numFull);
                text(cs, fontReg, 8f, cGrisTxt, mx + 12f, y - 31f,
                    "Date : " + date + "   |   Statut : Confirmee   |   Agriculteur ID : " + ID_AGRICULTEUR);
                y -= 58f;
                float[] colX = {mx, mx+20f, mx+192f, mx+262f, mx+312f, mx+372f, mx+432f};
                String[] heads = {"#","Equipement","Type","Prix unit.","Qte","Total TND","Total EUR"};
                fillRect(cs, cVert, mx, y - 22f, PW - 2*mx, 22f);
                for (int i = 0; i < heads.length; i++) {
                    float cw = (i < colX.length-1) ? colX[i+1]-colX[i] : PW-mx-colX[i];
                    float tx = colX[i] + centeredX(fontBold, 9f, heads[i], cw);
                    text(cs, fontBold, 9f, java.awt.Color.WHITE, tx, y - 14f, heads[i]);
                }
                y -= 24f;
                double sousTotal = 0;
                List<Panier> snap = new java.util.ArrayList<>(panierList);
                for (int i = 0; i < snap.size(); i++) {
                    Panier p   = snap.get(i);
                    Equipement eq = getEquipementById(p.getId_equipement());
                    String nom  = eq != null ? eq.getNom()  : "Equipement #" + p.getId_equipement();
                    String type = eq != null ? eq.getType() : "";
                    double prix   = eq != null ? parsePrix(eq.getPrix()) : 0;
                    double ligTot = parsePrix(p.getTotal());
                    sousTotal += ligTot;
                    java.awt.Color bg = (i % 2 == 0) ? cBlanc : cGrisCl;
                    fillRect(cs, bg, mx, y - 19f, PW - 2*mx, 19f);
                    strokeLine(cs, new java.awt.Color(220,240,215), 0.4f, mx, y - 19f, PW - mx, y - 19f);
                    String nom28  = nom.length()  > 28 ? nom.substring(0,27)  + "." : nom;
                    String type12 = type.length() > 12 ? type.substring(0,11) + "." : type;
                    String[] vals = { String.valueOf(i+1), nom28, type12,
                        String.format("%.2f", prix), "x" + p.getQuantite(),
                        String.format("%.2f", ligTot), String.format("%.2f", ligTot * tauxEUR) };
                    for (int j = 0; j < vals.length; j++) {
                        float cw = (j < colX.length-1) ? colX[j+1]-colX[j] : PW-mx-colX[j];
                        float tx = colX[j] + centeredX(fontReg, 8f, vals[j], cw);
                        text(cs, fontReg, 8f, cGrisTxt, tx, y - 13f, vals[j]);
                    }
                    y -= 19f;
                    if (y < 80f) break;
                }
                y -= 12f;
                strokeLine(cs, new java.awt.Color(180,220,170), 0.8f, mx, y, PW - mx, y);
                y -= 18f;
                float tx0 = PW - mx - 160f;
                float boxW = 160f;
                float th = 60f;
                fillRect(cs, cVertPale, tx0, y - th, boxW, th);
                strokeRect(cs, new java.awt.Color(180,220,170), 0.7f, tx0, y - th, boxW, th);
                text(cs, fontBold, 8.5f, cVert, tx0+8f, y-12f, "TOTAL TND");
                text(cs, fontBold, 13f, cVertMed, tx0+8f, y-28f, String.format("%.2f TND", sousTotal));
                String usdStr = String.format("$%.2f", sousTotal * tauxUSD);
                String eurStr = String.format("%.2f EUR", sousTotal * tauxEUR);
                text(cs, fontReg, 7.5f, cBleu,   tx0+8f, y-th+10f, usdStr);
                text(cs, fontReg, 7.5f, cOrange, tx0+boxW-tw(fontReg,7.5f,eurStr)-6f, y-th+10f, eurStr);
                fillRect(cs, cVert, 0, 0, PW, 38f);
                String[] fi = {"Paiement securise","Livraison Tunisie 3-5j","Retours gratuits 30j"};
                float fStep = (PW - 2*mx) / fi.length;
                for (int i = 0; i < fi.length; i++) {
                    float fx = mx + i*fStep + (fStep - tw(fontReg,8f,fi[i]))/2f;
                    text(cs, fontReg, 8f, new java.awt.Color(180,230,160), fx, 14f, fi[i]);
                }
                String note = "Document genere par AgriCore  |  " + date + "  |  Ce document fait foi de commande.";
                text(cs, fontReg, 7f, new java.awt.Color(150,150,150), (PW - tw(fontReg,7f,note))/2f, 41f, note);
                cs.close(); doc.save(outPath); doc.close();
                final String path = outPath; final String num = numFull;
                javafx.application.Platform.runLater(() -> {
                    afficherToastPDF(path, num);
                    try { if (java.awt.Desktop.isDesktopSupported()) java.awt.Desktop.getDesktop().open(new File(path)); }
                    catch (Exception ignored) {}
                });
            } catch (Exception ex) {
                ex.printStackTrace();
                javafx.application.Platform.runLater(() -> showAlert("Erreur PDF", ex.getMessage()));
            }
        }).start();
    }

    private void fillRect(org.apache.pdfbox.pdmodel.PDPageContentStream cs,
                          java.awt.Color c, float x, float y, float w, float h) throws IOException {
        cs.setNonStrokingColor(c); cs.addRect(x, y, w, h); cs.fill();
    }
    private void strokeRect(org.apache.pdfbox.pdmodel.PDPageContentStream cs,
                            java.awt.Color c, float lw, float x, float y, float w, float h) throws IOException {
        cs.setStrokingColor(c); cs.setLineWidth(lw); cs.addRect(x, y, w, h); cs.stroke();
    }
    private void strokeLine(org.apache.pdfbox.pdmodel.PDPageContentStream cs,
                            java.awt.Color c, float lw, float x1, float y1, float x2, float y2) throws IOException {
        cs.setStrokingColor(c); cs.setLineWidth(lw); cs.moveTo(x1,y1); cs.lineTo(x2,y2); cs.stroke();
    }
    private void text(org.apache.pdfbox.pdmodel.PDPageContentStream cs,
                      org.apache.pdfbox.pdmodel.font.PDType1Font font,
                      float size, java.awt.Color c, float x, float y, String txt) throws IOException {
        cs.beginText(); cs.setNonStrokingColor(c); cs.setFont(font, size);
        cs.newLineAtOffset(x, y); cs.showText(txt); cs.endText();
    }
    private float tw(org.apache.pdfbox.pdmodel.font.PDType1Font font, float size, String txt) {
        try { return font.getStringWidth(txt) / 1000f * size; } catch (Exception e) { return 0f; }
    }
    private float centeredX(org.apache.pdfbox.pdmodel.font.PDType1Font font, float size, String txt, float colW) {
        return Math.max(0f, (colW - tw(font, size, txt)) / 2f);
    }
    private void afficherToastPDF(String pdfPath, String numero) {
        Stage toast = new Stage();
        toast.initModality(javafx.stage.Modality.NONE);
        toast.initStyle(javafx.stage.StageStyle.TRANSPARENT);
        toast.setAlwaysOnTop(true);
        HBox box = new HBox(12); box.setAlignment(Pos.CENTER_LEFT);
        box.setStyle("-fx-background-color:#1a3a16;-fx-background-radius:14;-fx-padding:14 20;" +
            "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.55),22,0,0,6);");
        Label icon = new Label("✅"); icon.setStyle("-fx-font-size:22px;");
        VBox msg = new VBox(3);
        Label l1 = new Label("Commande confirmee !"); l1.setStyle("-fx-font-size:13px;-fx-font-weight:bold;-fx-text-fill:#7ddc6e;");
        Label l2 = new Label("PDF : Commande_" + numero + ".pdf"); l2.setStyle("-fx-font-size:10px;-fx-text-fill:rgba(255,255,255,0.6);");
        Button openBtn = new Button("Ouvrir");
        openBtn.setStyle("-fx-background-color:rgba(255,255,255,0.12);-fx-text-fill:white;" +
            "-fx-background-radius:6;-fx-font-size:10px;-fx-cursor:hand;-fx-padding:2 10;");
        openBtn.setOnAction(e -> { try { java.awt.Desktop.getDesktop().open(new File(pdfPath)); } catch (Exception ignored) {} });
        msg.getChildren().addAll(l1, l2, openBtn);
        Button closeBtn = new Button("x");
        closeBtn.setStyle("-fx-background-color:transparent;-fx-text-fill:rgba(255,255,255,0.4);-fx-font-size:11px;-fx-cursor:hand;");
        closeBtn.setOnAction(e -> toast.close());
        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
        box.getChildren().addAll(icon, msg, sp, closeBtn);
        Scene sc = new Scene(box);
        sc.setFill(javafx.scene.paint.Color.TRANSPARENT);
        toast.setScene(sc);
        javafx.stage.Window owner = gridPanier.getScene().getWindow();
        toast.setX(owner.getX() + 32); toast.setY(owner.getY() + owner.getHeight() - 120);
        toast.show();
        PauseTransition pt = new PauseTransition(Duration.seconds(6));
        pt.setOnFinished(e -> {
            FadeTransition ft = new FadeTransition(Duration.millis(700), box);
            ft.setFromValue(1.0); ft.setToValue(0.0); ft.setOnFinished(f -> toast.close()); ft.play();
        });
        pt.play();
    }

    // ═══════════════════════════════════════════════════════════════
    //  NAVIGATION VERS DÉTAIL
    // ═══════════════════════════════════════════════════════════════

    void ouvrirDetail(Equipement eq) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/DetailEquipement.fxml"));
            Parent root = loader.load();
            DetailEquipementController ctrl = loader.getController();
            ctrl.setEquipement(eq, tauxEUR, tauxUSD, this);
            NavigationUtil.loadInContentArea(gridCatalogue, root);
        } catch (Exception ex) { ex.printStackTrace(); }
    }

    // ═══════════════════════════════════════════════════════════════
    //  TABS
    // ═══════════════════════════════════════════════════════════════

    @FXML
    private void showCatalogue() {
        viewCatalogue.setVisible(true);  viewCatalogue.setManaged(true);
        viewPanier.setVisible(false);    viewPanier.setManaged(false);
        btnTabCatalogue.setStyle("-fx-background-color: #4a7c40; -fx-background-radius: 10; -fx-padding: 9 18; -fx-cursor: hand;");
        btnTabPanier.setStyle("-fx-background-color: #f0f7ee; -fx-background-radius: 10; -fx-padding: 9 18; -fx-cursor: hand; -fx-border-color: #c8ddc5; -fx-border-radius: 10;");
    }

    @FXML
    private void showPanier() {
        loadPanier();
        viewPanier.setVisible(true);     viewPanier.setManaged(true);
        viewCatalogue.setVisible(false); viewCatalogue.setManaged(false);
        btnTabPanier.setStyle("-fx-background-color: #4a7c40; -fx-background-radius: 10; -fx-padding: 9 18; -fx-cursor: hand;");
        btnTabCatalogue.setStyle("-fx-background-color: #f0f7ee; -fx-background-radius: 10; -fx-padding: 9 18; -fx-cursor: hand; -fx-border-color: #c8ddc5; -fx-border-radius: 10;");
    }


    // ═══════════════════════════════════════════════════════════════
    //  API EXCHANGERATE
    // ═══════════════════════════════════════════════════════════════

    private void chargerTauxDeChange() {
        new Thread(() -> {
            try {
                String json = appelHTTP("https://v6.exchangerate-api.com/v6/" + EXCHANGE_API_KEY + "/latest/TND");
                JSONObject rates = new JSONObject(json).getJSONObject("conversion_rates");
                double eur = rates.getDouble("EUR"); double usd = rates.getDouble("USD");
                javafx.application.Platform.runLater(() -> {
                    tauxEUR = eur; tauxUSD = usd;
                    renderCatalogueGrid(catalogueList);
                    renderPanierGrid(panierList);
                    updatePanierSummary();
                });
            } catch (Exception ignored) { }
        }).start();
    }

    // ═══════════════════════════════════════════════════════════════
    //  HELPERS
    // ═══════════════════════════════════════════════════════════════

    private Equipement getEquipementById(int id) {
        if (allEquipements == null) return null;
        return allEquipements.stream()
            .filter(e -> e.getId_equipement() == id).findFirst().orElse(null);
    }

    private Label makeStockLabel(int qty) {
        String text  = qty == 0 ? "⚠ Rupture de stock"
                     : qty < 3  ? "⚡ Stock faible (" + qty + ")" : "✅ " + qty + " disponibles";
        String color = qty == 0 ? "#e74c3c" : qty < 3 ? "#f39c12" : "#27ae60";
        Label l = new Label(text);
        l.setStyle("-fx-font-size: 10px; -fx-text-fill: " + color + "; -fx-font-weight: bold;");
        return l;
    }

    private void updatePanierSummary() {
        double total = calcTotal(); int nb = panierList.size();
        if (labelSousTotal       != null) labelSousTotal.setText("Sous-total : " + String.format("%.2f", total) + " TND");
        if (labelGrandTotal      != null) labelGrandTotal.setText("TOTAL : " + String.format("%.2f", total) + " TND");
        if (labelCartBadge       != null) labelCartBadge.setText(String.valueOf(nb));
        if (labelTotalPanier     != null) labelTotalPanier.setText(nb == 0 ? "" : nb + " art. — " + String.format("%.2f TND", total));
        if (labelNbArticles      != null) labelNbArticles.setText(nb + " article(s)");
        if (labelSousTotalDetail != null) labelSousTotalDetail.setText(String.format("%.2f TND", total));
        if (labelGrandTotalDetail!= null) labelGrandTotalDetail.setText(String.format("%.2f TND", total));
        if (labelTotalTND        != null) labelTotalTND.setText(String.format("%.2f TND", total));
        if (labelTotalEUR        != null) labelTotalEUR.setText(String.format("%.2f EUR", total * tauxEUR));
        if (labelTotalUSD        != null) labelTotalUSD.setText(String.format("%.2f USD", total * tauxUSD));
    }

    private double calcTotal() {
        return panierList.stream().mapToDouble(p -> parsePrix(p.getTotal())).sum();
    }

    private String styleCard(boolean hover) {
        return hover
            ? "-fx-background-color: #fafffe; -fx-background-radius: 16;" +
              "-fx-effect: dropshadow(gaussian, rgba(74,124,64,0.30), 20, 0, 0, 7); -fx-cursor: hand;"
            : "-fx-background-color: white; -fx-background-radius: 16;" +
              "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 12, 0, 0, 3); -fx-cursor: hand;";
    }

    String typeColor(String type) {
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

    String typeEmoji(String type) {
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

    double parsePrix(String s) {
        try { return Double.parseDouble(s.replace(",", ".")); }
        catch (Exception e) { return 0; }
    }

    private String appelHTTP(String urlStr) throws Exception {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(6000); conn.setReadTimeout(6000);
        BufferedReader r = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        StringBuilder sb = new StringBuilder(); String line;
        while ((line = r.readLine()) != null) sb.append(line);
        r.close(); return sb.toString();
    }

    private void showAlert(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle(title); a.setHeaderText(null); a.setContentText(msg); a.showAndWait();
    }
}
