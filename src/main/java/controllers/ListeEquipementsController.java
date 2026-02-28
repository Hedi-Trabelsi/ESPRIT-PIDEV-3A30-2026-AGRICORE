package controllers;

import entities.Equipement;
import services.EquipementService;
import utils.ImageManager;

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
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.Modality;
import javafx.stage.StageStyle;

import org.json.JSONObject;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.SQLException;
import java.util.*;

public class ListeEquipementsController implements Initializable {

    // Grid view
    @FXML private FlowPane gridEquipements;
    @FXML private VBox emptyPlaceholder;
    @FXML private TextField fieldSearch;
    @FXML private Label labelEquipCount;

    // API 2 : Taux de change (dans la topbar)
    @FXML private Label labelTauxEUR, labelTauxUSD, labelMajTaux;

    // Stats
    @FXML private VBox panelStats;
    @FXML private javafx.scene.chart.BarChart<String, Number> barChart;
    @FXML private javafx.scene.chart.PieChart pieChart;
    @FXML private Label statTotalEquip, statTotalValeur, statStockFaible, statRupture;
    @FXML private VBox statsCardsRow, statsSummaryBox;

    private EquipementService equipementService = new EquipementService();
    private ObservableList<Equipement> equipementList = FXCollections.observableArrayList();

    private double tauxEUR = 0.2981;
    private double tauxUSD = 0.3213;
    private static final String EXCHANGE_API_KEY = "0c3f3b97f846b6f5ced36eff";

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        loadData();
        chargerTauxDeChange();
    }

    // ==================== DONNÉES ====================

    private void loadData() {
        try {
            List<Equipement> list = equipementService.afficher();
            equipementList.setAll(list);
            labelEquipCount.setText("📦 " + equipementList.size() + " équipement(s)");
            renderGrid(equipementList);
        } catch (SQLException e) {
            showAlert("Erreur chargement : " + e.getMessage());
        }
    }

    @FXML
    private void filterTable() {
        String search = fieldSearch.getText().toLowerCase().trim();
        List<Equipement> filtered;
        if (search.isEmpty()) {
            filtered = equipementList;
        } else {
            filtered = equipementList.filtered(eq ->
                eq.getNom().toLowerCase().contains(search)
                || eq.getType().toLowerCase().contains(search)
            );
        }
        renderGrid(filtered);
    }

    // ==================== GRILLE ====================

    private void renderGrid(List<Equipement> list) {
        gridEquipements.getChildren().clear();
        if (list.isEmpty()) {
            emptyPlaceholder.setVisible(true);  emptyPlaceholder.setManaged(true);
            gridEquipements.setVisible(false);  gridEquipements.setManaged(false);
            return;
        }
        emptyPlaceholder.setVisible(false);  emptyPlaceholder.setManaged(false);
        gridEquipements.setVisible(true);    gridEquipements.setManaged(true);
        for (Equipement eq : list) {
            gridEquipements.getChildren().add(createCard(eq));
        }
    }

    // ==================== CARTE AVEC IMAGE ====================

    private VBox createCard(Equipement eq) {
        VBox card = new VBox(0);
        card.setPrefWidth(285);
        card.setMaxWidth(285);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 16;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 12, 0, 0, 3); -fx-cursor: hand;");

        String bandColor = getBandColor(eq.getType());

        // ── Bande couleur ──────────────────────────────────────────
        HBox topBand = new HBox();
        topBand.setPrefHeight(7);
        topBand.setStyle("-fx-background-color: " + bandColor +
            "; -fx-background-radius: 16 16 0 0;");

        // ── IMAGE ou placeholder emoji ─────────────────────────────
        // Utilise ImageManager : cherche l'image par le NOM de l'équipement
        StackPane imagePane = ImageManager.creerVignetteImage(
            eq.getNom(),        // clé = nom de l'équipement
            285, 140,           // largeur x hauteur de la vignette
            getTypeEmoji(eq.getType()),
            bandColor + "18"    // fond placeholder = couleur de bande très transparent
        );

        // ── Corps de la carte ──────────────────────────────────────
        VBox body = new VBox(10);
        body.setPadding(new Insets(14, 18, 18, 18));

        HBox header = new HBox(8);
        header.setAlignment(Pos.CENTER_LEFT);
        Label iconLabel = new Label(getTypeEmoji(eq.getType()));
        iconLabel.setStyle("-fx-font-size: 22px;");
        VBox namePart = new VBox(2);
        namePart.setAlignment(Pos.CENTER_LEFT);
        Label nomLabel = new Label(eq.getNom());
        nomLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;" +
            "-fx-text-fill: #1e3a1a; -fx-wrap-text: true;");
        nomLabel.setMaxWidth(195);
        Label typeBadge = new Label(eq.getType());
        typeBadge.setStyle("-fx-background-color: " + bandColor + "22;" +
            "-fx-text-fill: " + bandColor + "; -fx-background-radius: 20;" +
            "-fx-padding: 2 8; -fx-font-size: 10px; -fx-font-weight: bold;");
        namePart.getChildren().addAll(nomLabel, typeBadge);
        header.getChildren().addAll(iconLabel, namePart);

        Separator sep = new Separator();
        sep.setStyle("-fx-background-color: #eef5ec;");

        double prixNum = 0;
        try { prixNum = Double.parseDouble(eq.getPrix().replace(",", ".")); }
        catch (Exception ignored) {}

        HBox priceRow = new HBox(8);
        priceRow.setAlignment(Pos.CENTER_LEFT);
        VBox priceBlock = new VBox(2);
        Label prixTND = new Label(String.format("%.2f TND", prixNum));
        prixTND.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #2d5a25;");
        HBox conversions = new HBox(8);
        Label prixEUR = new Label(String.format("%.2f €", prixNum * tauxEUR));
        prixEUR.setStyle("-fx-font-size: 11px; -fx-text-fill: #2980b9;" +
            "-fx-background-color: #e8f4fd; -fx-background-radius: 6; -fx-padding: 2 6;");
        Label prixUSD = new Label(String.format("$%.2f", prixNum * tauxUSD));
        prixUSD.setStyle("-fx-font-size: 11px; -fx-text-fill: #27ae60;" +
            "-fx-background-color: #e8f8ef; -fx-background-radius: 6; -fx-padding: 2 6;");
        conversions.getChildren().addAll(prixEUR, prixUSD);
        priceBlock.getChildren().addAll(prixTND, conversions);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        String qtyColor = eq.getQuantite() == 0 ? "#e74c3c"
                        : eq.getQuantite() < 3  ? "#f39c12" : "#27ae60";
        Label qtyLabel = new Label("x" + eq.getQuantite());
        qtyLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: bold;" +
            "-fx-text-fill: white; -fx-background-color: " + qtyColor +
            "; -fx-background-radius: 20; -fx-padding: 4 12;");
        priceRow.getChildren().addAll(priceBlock, spacer, qtyLabel);

        String stockText = eq.getQuantite() == 0 ? "⚠ Rupture de stock"
                         : eq.getQuantite() < 3  ? "⚡ Stock faible" : "✅ En stock";
        String stockColor = eq.getQuantite() == 0 ? "#e74c3c"
                          : eq.getQuantite() < 3  ? "#f39c12" : "#27ae60";
        Label stockLabel = new Label(stockText);
        stockLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: " +
            stockColor + "; -fx-font-weight: bold;");

        HBox actions = new HBox(8);
        actions.setAlignment(Pos.CENTER_RIGHT);

        Button btnModif = new Button("✏ Modifier");
        btnModif.setStyle("-fx-background-color: #f39c12; -fx-text-fill: white;" +
            "-fx-background-radius: 8; -fx-font-size: 11px; -fx-font-weight: bold;" +
            "-fx-cursor: hand; -fx-padding: 7 14;");
        Button btnSuppr = new Button("🗑 Supprimer");
        btnSuppr.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white;" +
            "-fx-background-radius: 8; -fx-font-size: 11px; -fx-font-weight: bold;" +
            "-fx-cursor: hand; -fx-padding: 7 14;");

        Equipement eqRef = eq;
        btnModif.setOnAction(e -> goToModifier(eqRef));
        // Passe aussi le nom pour pouvoir supprimer l'image associée
        btnSuppr.setOnAction(e -> supprimerEquipement(eqRef.getId_equipement(), eqRef.getNom()));

        card.setOnMouseEntered(e -> card.setStyle(
            "-fx-background-color: #fafffe; -fx-background-radius: 16;" +
            "-fx-effect: dropshadow(gaussian, rgba(74,124,64,0.25), 18, 0, 0, 6);" +
            "-fx-cursor: hand;"));
        card.setOnMouseExited(e -> card.setStyle(
            "-fx-background-color: white; -fx-background-radius: 16;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 12, 0, 0, 3);" +
            "-fx-cursor: hand;"));

        actions.getChildren().addAll(btnModif, btnSuppr);
        body.getChildren().addAll(header, sep, priceRow, stockLabel, actions);
        // ── Assemblage : bande + image + corps ──────────────────────
        card.getChildren().addAll(topBand, imagePane, body);
        return card;
    }

    // ==================== STATS ====================

    private String getBandColor(String type) {
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

    private String getTypeEmoji(String type) {
        if (type == null) return "🔧";
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

    @FXML
    private void afficherStats() {
        panelStats.setVisible(true);
        panelStats.setManaged(true);
        panelStats.getChildren().clear();
        panelStats.setSpacing(14);
        panelStats.setPadding(new Insets(20));
        panelStats.setStyle("-fx-background-color: white; -fx-background-radius: 14;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 14, 0, 0, 4);");

        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);
        Label titre = new Label("📊  Quantité en stock par type");
        titre.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #2d5a25;");
        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
        Button btnFermer = new Button("✕  Fermer");
        btnFermer.setStyle("-fx-background-color: #f0f0f0; -fx-text-fill: #555;" +
            "-fx-background-radius: 8; -fx-font-size: 11px; -fx-cursor: hand; -fx-padding: 6 14;");
        btnFermer.setOnAction(e -> fermerStats());
        header.getChildren().addAll(titre, sp, btnFermer);

        Map<String, Integer> qtéParType = new LinkedHashMap<>();
        for (Equipement eq : equipementList) {
            String t = (eq.getType() == null || eq.getType().isBlank()) ? "Autre" : eq.getType();
            qtéParType.merge(t, eq.getQuantite(), Integer::sum);
        }
        int maxQte = qtéParType.values().stream().mapToInt(i -> i).max().orElse(1);

        VBox chartBox = new VBox(10);
        chartBox.setPadding(new Insets(10, 0, 0, 0));
        for (Map.Entry<String, Integer> entry : qtéParType.entrySet().stream()
                .sorted(Map.Entry.comparingByValue())
                .collect(java.util.stream.Collectors.toList())) {
            String type  = entry.getKey();
            int    qte   = entry.getValue();
            String color = getBandColor(type);
            double pct   = (double) qte / maxQte;
            HBox row = new HBox(12); row.setAlignment(Pos.CENTER_LEFT);
            Label labelType = new Label(getTypeEmoji(type) + "  " + type);
            labelType.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #333;");
            labelType.setPrefWidth(130); labelType.setMinWidth(130);
            HBox barreTrack = new HBox();
            barreTrack.setPrefHeight(26); barreTrack.setPrefWidth(340); barreTrack.setMaxWidth(340);
            barreTrack.setStyle("-fx-background-color: #f0f0f0; -fx-background-radius: 6;");
            HBox barre = new HBox();
            barre.setPrefHeight(26); barre.setPrefWidth(Math.max(6, pct * 340));
            barre.setStyle("-fx-background-color: " + color + "; -fx-background-radius: 6;");
            barreTrack.getChildren().add(barre);
            Label labelQte = new Label(qte + " unités");
            labelQte.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: " + color + ";");
            labelQte.setPrefWidth(80);
            row.getChildren().addAll(labelType, barreTrack, labelQte);
            chartBox.getChildren().add(row);
        }
        panelStats.getChildren().addAll(header, chartBox);
    }

    @FXML
    private void fermerStats() {
        panelStats.setVisible(false);
        panelStats.setManaged(false);
    }

    // ==================== SUPPRESSION ====================

    /**
     * Signature étendue : reçoit aussi le nom pour pouvoir supprimer l'image associée.
     */
    private void supprimerEquipement(int id, String nomEquipement) {
        Stage dlg = new Stage();
        dlg.initModality(Modality.APPLICATION_MODAL);
        dlg.initOwner(gridEquipements.getScene().getWindow());
        dlg.initStyle(StageStyle.TRANSPARENT);
        dlg.setResizable(false);

        VBox root = new VBox(0);
        root.setStyle("-fx-background-color: white; -fx-background-radius: 16;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.35), 30, 0, 0, 8);");
        root.setPrefWidth(380);

        VBox header = new VBox(6);
        header.setAlignment(Pos.CENTER);
        header.setStyle("-fx-background-color: linear-gradient(to bottom right, #c0392b, #e74c3c);" +
            "-fx-background-radius: 16 16 0 0; -fx-padding: 22 24 18 24;");
        Label icone = new Label("🗑"); icone.setStyle("-fx-font-size: 32px;");
        Label titreH = new Label("Confirmer la suppression");
        titreH.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: white;");
        header.getChildren().addAll(icone, titreH);

        VBox body = new VBox(10);
        body.setAlignment(Pos.CENTER);
        body.setStyle("-fx-padding: 22 28;");
        Label msg = new Label("Voulez-vous vraiment supprimer\ncet équipement ?");
        msg.setStyle("-fx-font-size: 13px; -fx-text-fill: #444; -fx-text-alignment: center;");
        msg.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
        Label warn = new Label("⚠  Cette action est irréversible.");
        warn.setStyle("-fx-font-size: 11px; -fx-text-fill: #e74c3c; -fx-font-weight: bold;");
        body.getChildren().addAll(msg, warn);

        HBox btnRow = new HBox(12);
        btnRow.setAlignment(Pos.CENTER);
        btnRow.setStyle("-fx-padding: 0 24 22 24;");

        Button btnNon = new Button("Annuler");
        btnNon.setPrefWidth(130);
        btnNon.setStyle("-fx-background-color: #f0f0f0; -fx-text-fill: #555;" +
            "-fx-background-radius: 10; -fx-font-size: 13px; -fx-font-weight: bold;" +
            "-fx-cursor: hand; -fx-padding: 10 0;");
        btnNon.setOnMouseEntered(e -> btnNon.setStyle("-fx-background-color: #e0e0e0;" +
            "-fx-text-fill: #333; -fx-background-radius: 10; -fx-font-size: 13px;" +
            "-fx-font-weight: bold; -fx-cursor: hand; -fx-padding: 10 0;"));
        btnNon.setOnMouseExited(e -> btnNon.setStyle("-fx-background-color: #f0f0f0;" +
            "-fx-text-fill: #555; -fx-background-radius: 10; -fx-font-size: 13px;" +
            "-fx-font-weight: bold; -fx-cursor: hand; -fx-padding: 10 0;"));
        btnNon.setOnAction(e -> dlg.close());

        Button btnOui = new Button("Supprimer");
        btnOui.setPrefWidth(130);
        btnOui.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white;" +
            "-fx-background-radius: 10; -fx-font-size: 13px; -fx-font-weight: bold;" +
            "-fx-cursor: hand; -fx-padding: 10 0;");
        btnOui.setOnMouseEntered(e -> btnOui.setStyle("-fx-background-color: #c0392b;" +
            "-fx-text-fill: white; -fx-background-radius: 10; -fx-font-size: 13px;" +
            "-fx-font-weight: bold; -fx-cursor: hand; -fx-padding: 10 0;" +
            "-fx-effect: dropshadow(gaussian, rgba(192,57,43,0.4), 8, 0, 0, 3);"));
        btnOui.setOnMouseExited(e -> btnOui.setStyle("-fx-background-color: #e74c3c;" +
            "-fx-text-fill: white; -fx-background-radius: 10; -fx-font-size: 13px;" +
            "-fx-font-weight: bold; -fx-cursor: hand; -fx-padding: 10 0;"));
        btnOui.setOnAction(e -> {
            dlg.close();
            try {
                equipementService.supprimer(id);
                // ── Supprimer aussi l'image associée ─────────────────
                ImageManager.supprimerImage(nomEquipement);
                loadData();
            } catch (SQLException ex) { showAlert("Erreur : " + ex.getMessage()); }
        });

        btnRow.getChildren().addAll(btnNon, btnOui);
        root.getChildren().addAll(header, body, btnRow);
        Scene scene = new Scene(root);
        scene.setFill(javafx.scene.paint.Color.TRANSPARENT);
        dlg.setScene(scene);
        dlg.showAndWait();
    }

    // ==================== API 2 : TAUX DE CHANGE ====================

    private void chargerTauxDeChange() {
        new Thread(() -> {
            try {
                String urlStr = "https://v6.exchangerate-api.com/v6/" + EXCHANGE_API_KEY + "/latest/TND";
                String json = appelHTTP(urlStr);
                JSONObject obj = new JSONObject(json);
                JSONObject rates = obj.getJSONObject("conversion_rates");
                double eur = rates.getDouble("EUR");
                double usd = rates.getDouble("USD");
                javafx.application.Platform.runLater(() -> {
                    tauxEUR = eur; tauxUSD = usd;
                    if (labelTauxEUR != null) labelTauxEUR.setText(
                        String.format("1 TND = %.4f EUR", tauxEUR));
                    if (labelTauxUSD != null) labelTauxUSD.setText(
                        String.format("1 TND = %.4f USD", tauxUSD));
                    if (labelMajTaux != null) labelMajTaux.setText("Mis à jour");
                    renderGrid(equipementList);
                });
            } catch (Exception e) {
                javafx.application.Platform.runLater(() -> {
                    if (labelTauxEUR != null) labelTauxEUR.setText("1 TND ≈ 0.298 EUR");
                    if (labelTauxUSD != null) labelTauxUSD.setText("1 TND ≈ 0.321 USD");
                });
            }
        }).start();
    }

    // ==================== EXPORT PDF ====================

    @FXML
    private void exporterPDF() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Enregistrer le PDF");
        fileChooser.setInitialFileName("equipements_" + System.currentTimeMillis() + ".pdf");
        fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));
        File file = fileChooser.showSaveDialog(
            (Stage) gridEquipements.getScene().getWindow());
        if (file == null) return;
        try { genererPDF(file); showAlert("PDF exporté avec succès !\n" + file.getAbsolutePath()); }
        catch (Exception e) { showAlert("Erreur export PDF : " + e.getMessage()); }
    }

    private void genererPDF(File file) throws IOException {
        StringBuilder contenu = new StringBuilder();
        contenu.append("BT\n/F1 15 Tf\n40 800 Td\n");
        contenu.append("(AgriCore - Liste des Equipements) Tj\n");
        contenu.append("/F1 9 Tf\n0 -18 Td\n");
        contenu.append("(Date : ").append(new java.util.Date().toString()
            .replace("(","").replace(")","")).append(") Tj\n");
        contenu.append("0 -10 Td\n(--------------------------------------------------------------------) Tj\n");
        contenu.append("0 -14 Td\n/F1 10 Tf\n");
        contenu.append("(Nom                   Type         TND        EUR        USD        Qte) Tj\n");
        contenu.append("0 -4 Td\n(--------------------------------------------------------------------) Tj\n");
        contenu.append("/F1 8 Tf\n");
        for (Equipement eq : equipementList) {
            String nom  = padRight(eq.getNom(), 22);
            String type = padRight(eq.getType(), 12);
            String tnd  = padRight(eq.getPrix(), 10);
            double p = 0;
            try { p = Double.parseDouble(eq.getPrix().replace(",", ".")); } catch (Exception ignored) {}
            String eur  = padRight(String.format("%.2f", p * tauxEUR), 10);
            String usd  = padRight(String.format("%.2f", p * tauxUSD), 10);
            String qte  = String.valueOf(eq.getQuantite());
            String ligne = (nom + type + tnd + eur + usd + qte)
                .replace("\\", "\\\\").replace("(", "\\(").replace(")", "\\)");
            contenu.append("0 -12 Td\n(").append(ligne).append(") Tj\n");
        }
        double totalTND = equipementList.stream().mapToDouble(eq -> {
            try { return Double.parseDouble(eq.getPrix().replace(",",".")) * eq.getQuantite(); }
            catch (Exception ex) { return 0; }
        }).sum();
        contenu.append("0 -18 Td\n(--------------------------------------------------------------------) Tj\n");
        contenu.append("0 -12 Td\n/F1 9 Tf\n");
        contenu.append("(Nb equipements : ").append(equipementList.size())
            .append("   Valeur totale : ")
            .append(String.format("%.2f TND = %.2f EUR = %.2f USD",
                totalTND, totalTND * tauxEUR, totalTND * tauxUSD)
                .replace("(","\\(").replace(")","\\)"))
            .append(") Tj\nET\n");
        String cs = contenu.toString();
        byte[] cb = cs.getBytes("ISO-8859-1");
        StringBuilder pdf = new StringBuilder();
        pdf.append("%PDF-1.4\n");
        List<Integer> offsets = new ArrayList<>();
        offsets.add(pdf.length());
        pdf.append("1 0 obj\n<< /Type /Catalog /Pages 2 0 R >>\nendobj\n");
        offsets.add(pdf.length());
        pdf.append("2 0 obj\n<< /Type /Pages /Kids [3 0 R] /Count 1 >>\nendobj\n");
        offsets.add(pdf.length());
        pdf.append("3 0 obj\n<< /Type /Page /Parent 2 0 R ")
           .append("/MediaBox [0 0 595 842] /Contents 4 0 R ")
           .append("/Resources << /Font << /F1 5 0 R >> >> >>\nendobj\n");
        offsets.add(pdf.length());
        pdf.append("4 0 obj\n<< /Length ").append(cb.length).append(" >>\nstream\n")
           .append(cs).append("\nendstream\nendobj\n");
        offsets.add(pdf.length());
        pdf.append("5 0 obj\n<< /Type /Font /Subtype /Type1 /BaseFont /Courier >>\nendobj\n");
        int xrefOff = pdf.length();
        pdf.append("xref\n0 6\n0000000000 65535 f \n");
        for (int off : offsets) pdf.append(String.format("%010d 00000 n \n", off));
        pdf.append("trailer\n<< /Size 6 /Root 1 0 R >>\n");
        pdf.append("startxref\n").append(xrefOff).append("\n%%EOF\n");
        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(pdf.toString().getBytes("ISO-8859-1"));
        }
    }

    private String padRight(String s, int n) {
        if (s == null) s = "";
        if (s.length() >= n) return s.substring(0, n);
        return s + " ".repeat(n - s.length());
    }

    // ==================== NAVIGATION ====================

    private void goToModifier(Equipement eq) {
        try {
            FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/AjoutEquipement.fxml"));
            Parent root = loader.load();
            AjoutEquipementController controller = loader.getController();
            controller.setEquipementToModify(eq);
            Stage stage = (Stage) gridEquipements.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.centerOnScreen();
        } catch (Exception e) { e.printStackTrace(); }
    }

    @FXML private void goToAjout()  { navigateTo("/AjoutEquipement.fxml"); }
    @FXML private void goToLogin()  { navigateTo("/Login.fxml"); }

    private void navigateTo(String fxml) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(fxml));
            Stage stage = (Stage) gridEquipements.getScene().getWindow();
            stage.setScene(new Scene(root)); stage.centerOnScreen();
        } catch (Exception e) { e.printStackTrace(); }
    }

    // ==================== UTILITAIRES ====================

    private String appelHTTP(String urlStr) throws Exception {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(6000); conn.setReadTimeout(6000);
        BufferedReader reader = new BufferedReader(
            new InputStreamReader(conn.getInputStream()));
        StringBuilder sb = new StringBuilder(); String line;
        while ((line = reader.readLine()) != null) sb.append(line);
        reader.close(); return sb.toString();
    }

    private void showAlert(String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Information");
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}
