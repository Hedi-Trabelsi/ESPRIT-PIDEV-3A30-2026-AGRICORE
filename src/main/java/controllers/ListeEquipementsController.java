package controllers;

import entities.Equipement;
import services.EquipementService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

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

    // Statistiques
    @FXML private VBox panelStats;
    @FXML private BarChart<String, Number> barChart;

    private EquipementService equipementService = new EquipementService();
    private ObservableList<Equipement> equipementList = FXCollections.observableArrayList();

    // Taux par défaut
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
            emptyPlaceholder.setVisible(true);
            emptyPlaceholder.setManaged(true);
            gridEquipements.setVisible(false);
            gridEquipements.setManaged(false);
            return;
        }

        emptyPlaceholder.setVisible(false);
        emptyPlaceholder.setManaged(false);
        gridEquipements.setVisible(true);
        gridEquipements.setManaged(true);

        for (Equipement eq : list) {
            gridEquipements.getChildren().add(createCard(eq));
        }
    }

    private VBox createCard(Equipement eq) {
        // === Card container ===
        VBox card = new VBox(0);
        card.setPrefWidth(285);
        card.setMaxWidth(285);
        card.setStyle(
            "-fx-background-color: white;" +
            "-fx-background-radius: 16;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 12, 0, 0, 3);" +
            "-fx-cursor: hand;"
        );

        // === Colored top band by type ===
        String bandColor = getBandColor(eq.getType());
        HBox topBand = new HBox();
        topBand.setPrefHeight(7);
        topBand.setStyle("-fx-background-color: " + bandColor + "; -fx-background-radius: 16 16 0 0;");

        // === Card body ===
        VBox body = new VBox(10);
        body.setPadding(new Insets(16, 18, 18, 18));

        // Type badge + icon
        HBox header = new HBox(8);
        header.setAlignment(Pos.CENTER_LEFT);

        Label iconLabel = new Label(getTypeEmoji(eq.getType()));
        iconLabel.setStyle("-fx-font-size: 22px;");

        VBox namePart = new VBox(2);
        namePart.setAlignment(Pos.CENTER_LEFT);
        Label nomLabel = new Label(eq.getNom());
        nomLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #1e3a1a; -fx-wrap-text: true;");
        nomLabel.setMaxWidth(195);

        Label typeBadge = new Label(eq.getType());
        typeBadge.setStyle(
            "-fx-background-color: " + bandColor + "22; " +
            "-fx-text-fill: " + bandColor + "; " +
            "-fx-background-radius: 20; -fx-padding: 2 8; " +
            "-fx-font-size: 10px; -fx-font-weight: bold;"
        );

        namePart.getChildren().addAll(nomLabel, typeBadge);
        header.getChildren().addAll(iconLabel, namePart);

        // Separator line
        Separator sep = new Separator();
        sep.setStyle("-fx-background-color: #eef5ec;");

        // Price section
        double prixNum = 0;
        try { prixNum = Double.parseDouble(eq.getPrix().replace(",", ".")); } catch (Exception ignored) {}

        HBox priceRow = new HBox(8);
        priceRow.setAlignment(Pos.CENTER_LEFT);

        VBox priceBlock = new VBox(2);
        Label prixTND = new Label(String.format("%.2f TND", prixNum));
        prixTND.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #2d5a25;");
        HBox conversions = new HBox(8);
        Label prixEUR = new Label(String.format("%.2f €", prixNum * tauxEUR));
        prixEUR.setStyle("-fx-font-size: 11px; -fx-text-fill: #2980b9; -fx-background-color: #e8f4fd; -fx-background-radius: 6; -fx-padding: 2 6;");
        Label prixUSD = new Label(String.format("$%.2f", prixNum * tauxUSD));
        prixUSD.setStyle("-fx-font-size: 11px; -fx-text-fill: #27ae60; -fx-background-color: #e8f8ef; -fx-background-radius: 6; -fx-padding: 2 6;");
        conversions.getChildren().addAll(prixEUR, prixUSD);
        priceBlock.getChildren().addAll(prixTND, conversions);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Quantity pill
        String qtyColor = eq.getQuantite() == 0 ? "#e74c3c" : eq.getQuantite() < 3 ? "#f39c12" : "#27ae60";
        Label qtyLabel = new Label("x" + eq.getQuantite());
        qtyLabel.setStyle(
            "-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: white;" +
            "-fx-background-color: " + qtyColor + "; " +
            "-fx-background-radius: 20; -fx-padding: 4 12;"
        );

        priceRow.getChildren().addAll(priceBlock, spacer, qtyLabel);

        // Stock label
        String stockText = eq.getQuantite() == 0 ? "⚠ Rupture de stock"
                         : eq.getQuantite() < 3 ? "⚡ Stock faible"
                         : "✅ En stock";
        String stockColor = eq.getQuantite() == 0 ? "#e74c3c" : eq.getQuantite() < 3 ? "#f39c12" : "#27ae60";
        Label stockLabel = new Label(stockText);
        stockLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: " + stockColor + "; -fx-font-weight: bold;");

        // Action buttons
        HBox actions = new HBox(8);
        actions.setAlignment(Pos.CENTER_RIGHT);
        Button btnModif = new Button("✏ Modifier");
        btnModif.setStyle(
            "-fx-background-color: #f39c12; -fx-text-fill: white;" +
            "-fx-background-radius: 8; -fx-font-size: 11px; -fx-font-weight: bold;" +
            "-fx-cursor: hand; -fx-padding: 7 14;"
        );
        Button btnSuppr = new Button("🗑 Supprimer");
        btnSuppr.setStyle(
            "-fx-background-color: #e74c3c; -fx-text-fill: white;" +
            "-fx-background-radius: 8; -fx-font-size: 11px; -fx-font-weight: bold;" +
            "-fx-cursor: hand; -fx-padding: 7 14;"
        );

        Equipement eqRef = eq;
        btnModif.setOnAction(e -> goToModifier(eqRef));
        btnSuppr.setOnAction(e -> supprimerEquipement(eqRef.getId_equipement()));

        // Hover effect
        card.setOnMouseEntered(e -> card.setStyle(
            "-fx-background-color: #fafffe;" +
            "-fx-background-radius: 16;" +
            "-fx-effect: dropshadow(gaussian, rgba(74,124,64,0.25), 18, 0, 0, 6);" +
            "-fx-cursor: hand;"
        ));
        card.setOnMouseExited(e -> card.setStyle(
            "-fx-background-color: white;" +
            "-fx-background-radius: 16;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 12, 0, 0, 3);" +
            "-fx-cursor: hand;"
        ));

        actions.getChildren().addAll(btnModif, btnSuppr);
        body.getChildren().addAll(header, sep, priceRow, stockLabel, actions);
        card.getChildren().addAll(topBand, body);
        return card;
    }

    private String getBandColor(String type) {
        if (type == null) return "#4a7c40";
        return switch (type.toLowerCase().trim()) {
            case "machinerie", "machine" -> "#2980b9";
            case "outil", "outils" -> "#f39c12";
            case "vehicule", "véhicule" -> "#8e44ad";
            case "irrigation" -> "#16a085";
            case "semence", "semences" -> "#27ae60";
            case "engrais" -> "#d35400";
            default -> "#4a7c40";
        };
    }

    private String getTypeEmoji(String type) {
        if (type == null) return "🔧";
        return switch (type.toLowerCase().trim()) {
            case "machinerie", "machine" -> "🚜";
            case "outil", "outils" -> "🔧";
            case "vehicule", "véhicule" -> "🚛";
            case "irrigation" -> "💧";
            case "semence", "semences" -> "🌱";
            case "engrais" -> "🌿";
            default -> "📦";
        };
    }

    // ==================== SUPPRESSION ====================

    private void supprimerEquipement(int id) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
            "Voulez-vous vraiment supprimer cet équipement ?",
            ButtonType.YES, ButtonType.NO);
        confirm.setTitle("Confirmation suppression");
        confirm.setHeaderText(null);
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.YES) {
                try {
                    equipementService.supprimer(id);
                    loadData();
                } catch (SQLException e) { showAlert("Erreur : " + e.getMessage()); }
            }
        });
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
                    tauxEUR = eur;
                    tauxUSD = usd;
                    labelTauxEUR.setText(String.format("1 TND = %.4f EUR", tauxEUR));
                    labelTauxUSD.setText(String.format("1 TND = %.4f USD", tauxUSD));
                    labelMajTaux.setText("Mis à jour");
                    renderGrid(equipementList); // refresh cards with new rates
                });
            } catch (Exception e) {
                javafx.application.Platform.runLater(() -> {
                    labelTauxEUR.setText("1 TND ≈ 0.298 EUR");
                    labelTauxUSD.setText("1 TND ≈ 0.321 USD");
                });
            }
        }).start();
    }

    // ==================== STATISTIQUES ====================

    @FXML
    private void afficherStats() {
        panelStats.setVisible(true);
        panelStats.setManaged(true);
        barChart.getData().clear();
        Map<String, Integer> statsMap = new LinkedHashMap<>();
        for (Equipement eq : equipementList) {
            statsMap.merge(eq.getType(), eq.getQuantite(), Integer::sum);
        }
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Quantité par type");
        for (Map.Entry<String, Integer> entry : statsMap.entrySet()) {
            series.getData().add(new XYChart.Data<>(entry.getKey(), entry.getValue()));
        }
        barChart.getData().add(series);
    }

    @FXML
    private void fermerStats() {
        panelStats.setVisible(false);
        panelStats.setManaged(false);
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
        try {
            genererPDF(file);
            showAlert("PDF exporté avec succès !\n" + file.getAbsolutePath());
        } catch (Exception e) {
            showAlert("Erreur export PDF : " + e.getMessage());
        }
    }

    private void genererPDF(File file) throws IOException {
        StringBuilder contenu = new StringBuilder();
        contenu.append("BT\n/F1 15 Tf\n40 800 Td\n");
        contenu.append("(AgroManager - Liste des Equipements) Tj\n");
        contenu.append("/F1 9 Tf\n0 -18 Td\n");
        contenu.append("(Date : ").append(new java.util.Date().toString().replace("(","").replace(")","")).append(") Tj\n");
        contenu.append("0 -6 Td\n");
        contenu.append("(Taux : 1 TND = ").append(String.format("%.4f EUR | 1 TND = %.4f USD", tauxEUR, tauxUSD)).append(") Tj\n");
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
            .append(String.format("%.2f TND = %.2f EUR = %.2f USD", totalTND, totalTND * tauxEUR, totalTND * tauxUSD)
                .replace("(","\\(").replace(")","\\)"))
            .append(") Tj\n");
        contenu.append("ET\n");

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
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/AjoutEquipement.fxml"));
            Parent root = loader.load();
            AjoutEquipementController controller = loader.getController();
            controller.setEquipementToModify(eq);
            Stage stage = (Stage) gridEquipements.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.centerOnScreen();
        } catch (Exception e) { e.printStackTrace(); }
    }

    @FXML private void goToAjout() { navigateTo("/AjoutEquipement.fxml"); }
    @FXML private void goToLogin() { navigateTo("/Login.fxml"); }

    private void navigateTo(String fxml) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(fxml));
            Stage stage = (Stage) gridEquipements.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.centerOnScreen();
        } catch (Exception e) { e.printStackTrace(); }
    }

    // ==================== UTILITAIRES ====================

    private String appelHTTP(String urlStr) throws Exception {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(6000);
        conn.setReadTimeout(6000);
        BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) sb.append(line);
        reader.close();
        return sb.toString();
    }

    private void showAlert(String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Information");
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}
