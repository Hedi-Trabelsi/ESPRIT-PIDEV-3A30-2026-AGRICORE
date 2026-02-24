package controllers;

import entities.Equipement;
import entities.Panier;
import services.EquipementService;
import services.PanierService;
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

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.SQLException;
import java.util.List;
import java.util.ResourceBundle;

public class AgriculteurController implements Initializable {

    // Catalogue
    @FXML private FlowPane gridCatalogue;
    @FXML private VBox emptyCatalogue;
    @FXML private TextField fieldSearchCatalogue;

    // Panier grid
    @FXML private FlowPane gridPanier;
    @FXML private VBox emptyPanier;

    // Sidebar totaux
    @FXML private Label labelSousTotal, labelGrandTotal, labelCartBadge, labelTotalPanier;

    // Panneau récap
    @FXML private Label labelNbArticles, labelSousTotalDetail, labelGrandTotalDetail;

    // API 1 — RestCountries (dans le panier)
    @FXML private Label labelPaysNom, labelPaysRegion, labelPaysCapitale;
    @FXML private Label labelPaysDevise, labelPaysPopulation;

    // API 2 — ExchangeRate (total panier en devises)
    @FXML private Label labelTotalTND, labelTotalEUR, labelTotalUSD;

    // Views / tabs
    @FXML private VBox viewCatalogue, viewPanier;
    @FXML private HBox btnTabCatalogue, btnTabPanier;

    private EquipementService equipementService = new EquipementService();
    private PanierService panierService = new PanierService();
    private ObservableList<Equipement> catalogueList = FXCollections.observableArrayList();
    private ObservableList<Panier> panierList = FXCollections.observableArrayList();
    private List<Equipement> allEquipements;

    private static final int ID_AGRICULTEUR = 1;
    private static final String EXCHANGE_API_KEY = "0c3f3b97f846b6f5ced36eff";

    // Taux par défaut
    private double tauxEUR = 0.2981;
    private double tauxUSD = 0.3213;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        loadCatalogue();
        loadPanier();
        // Les APIs se chargent une seule fois au démarrage
        chargerInfosPays("Tunisia");
        chargerTauxDeChange();
    }

    // ======================== CATALOGUE ========================

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
        String search = fieldSearchCatalogue.getText().toLowerCase().trim();
        List<Equipement> filtered = search.isEmpty()
            ? catalogueList
            : catalogueList.filtered(eq ->
                eq.getNom().toLowerCase().contains(search) ||
                eq.getType().toLowerCase().contains(search));
        renderCatalogueGrid(filtered);
    }

    private void renderCatalogueGrid(List<Equipement> list) {
        gridCatalogue.getChildren().clear();
        if (list.isEmpty()) {
            emptyCatalogue.setVisible(true);  emptyCatalogue.setManaged(true);
            gridCatalogue.setVisible(false);  gridCatalogue.setManaged(false);
            return;
        }
        emptyCatalogue.setVisible(false); emptyCatalogue.setManaged(false);
        gridCatalogue.setVisible(true);   gridCatalogue.setManaged(true);
        for (Equipement eq : list) gridCatalogue.getChildren().add(createCatalogueCard(eq));
    }

    private VBox createCatalogueCard(Equipement eq) {
        VBox card = new VBox(0);
        card.setPrefWidth(270); card.setMaxWidth(270);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 16;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 12, 0, 0, 3); -fx-cursor: hand;");

        String bandColor = getTypeColor(eq.getType());
        HBox topBand = new HBox();
        topBand.setPrefHeight(7);
        topBand.setStyle("-fx-background-color: " + bandColor + "; -fx-background-radius: 16 16 0 0;");

        VBox body = new VBox(10);
        body.setPadding(new Insets(15, 18, 18, 18));

        HBox header = new HBox(8); header.setAlignment(Pos.CENTER_LEFT);
        Label iconLabel = new Label(getTypeEmoji(eq.getType()));
        iconLabel.setStyle("-fx-font-size: 24px;");
        VBox namePart = new VBox(3);
        Label nomLabel = new Label(eq.getNom());
        nomLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #1e3a1a; -fx-wrap-text: true;");
        nomLabel.setMaxWidth(185);
        Label typeBadge = new Label(eq.getType());
        typeBadge.setStyle("-fx-background-color: " + bandColor + "22; -fx-text-fill: " + bandColor + ";" +
            "-fx-background-radius: 20; -fx-padding: 2 8; -fx-font-size: 10px; -fx-font-weight: bold;");
        namePart.getChildren().addAll(nomLabel, typeBadge);
        header.getChildren().addAll(iconLabel, namePart);

        Separator sep = new Separator();
        sep.setStyle("-fx-background-color: #eef5ec;");

        double prixNum = 0;
        try { prixNum = Double.parseDouble(eq.getPrix().replace(",", ".")); } catch (Exception ignored) {}
        Label prixLabel = new Label(String.format("%.2f TND", prixNum));
        prixLabel.setStyle("-fx-font-size: 17px; -fx-font-weight: bold; -fx-text-fill: #2d5a25;");

        String stockText = eq.getQuantite() == 0 ? "⚠ Rupture de stock"
                         : eq.getQuantite() < 3  ? "⚡ Stock faible (" + eq.getQuantite() + ")"
                                                  : "✅ " + eq.getQuantite() + " disponibles";
        String stockColor = eq.getQuantite() == 0 ? "#e74c3c" : eq.getQuantite() < 3 ? "#f39c12" : "#27ae60";
        Label stockLabel = new Label(stockText);
        stockLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: " + stockColor + "; -fx-font-weight: bold;");

        Button btnAjouter = new Button(eq.getQuantite() == 0 ? "❌  Indisponible" : "🛒  Ajouter au panier");
        btnAjouter.setMaxWidth(Double.MAX_VALUE);
        btnAjouter.setDisable(eq.getQuantite() == 0);
        if (eq.getQuantite() == 0) {
            btnAjouter.setStyle("-fx-background-color: #ecf0f1; -fx-text-fill: #aaa;" +
                "-fx-background-radius: 9; -fx-font-size: 12px; -fx-font-weight: bold; -fx-padding: 9 0;");
        } else {
            btnAjouter.setStyle("-fx-background-color: #4a7c40; -fx-text-fill: white;" +
                "-fx-background-radius: 9; -fx-font-size: 12px; -fx-font-weight: bold; -fx-cursor: hand; -fx-padding: 9 0;");
            btnAjouter.setOnAction(e -> showAddToPanierDialog(eq));
            btnAjouter.setOnMouseEntered(e -> btnAjouter.setStyle("-fx-background-color: #3a6b2e; -fx-text-fill: white;" +
                "-fx-background-radius: 9; -fx-font-size: 12px; -fx-font-weight: bold; -fx-cursor: hand; -fx-padding: 9 0;" +
                "-fx-effect: dropshadow(gaussian, rgba(74,124,64,0.45), 8, 0, 0, 3);"));
            btnAjouter.setOnMouseExited(e -> btnAjouter.setStyle("-fx-background-color: #4a7c40; -fx-text-fill: white;" +
                "-fx-background-radius: 9; -fx-font-size: 12px; -fx-font-weight: bold; -fx-cursor: hand; -fx-padding: 9 0;"));
        }

        card.setOnMouseEntered(e -> card.setStyle("-fx-background-color: #fafffe; -fx-background-radius: 16;" +
            "-fx-effect: dropshadow(gaussian, rgba(74,124,64,0.25), 18, 0, 0, 6); -fx-cursor: hand;"));
        card.setOnMouseExited(e -> card.setStyle("-fx-background-color: white; -fx-background-radius: 16;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 12, 0, 0, 3); -fx-cursor: hand;"));

        body.getChildren().addAll(header, sep, prixLabel, stockLabel, btnAjouter);
        card.getChildren().addAll(topBand, body);
        return card;
    }

    private void showAddToPanierDialog(Equipement eq) {
        TextInputDialog dialog = new TextInputDialog("1");
        dialog.setTitle("Ajouter au panier");
        dialog.setHeaderText("📦 " + eq.getNom());
        dialog.setContentText("Quantité souhaitée (max: " + eq.getQuantite() + ") :");
        dialog.showAndWait().ifPresent(input -> {
            try {
                int qty = Integer.parseInt(input.trim());
                if (qty <= 0 || qty > eq.getQuantite()) {
                    showAlert("Erreur", "Quantité invalide. Maximum disponible : " + eq.getQuantite());
                    return;
                }
                double unitPrice = Double.parseDouble(eq.getPrix().replace(",", "."));
                double total = unitPrice * qty;
                Panier panier = new Panier(eq.getId_equipement(), qty, String.format("%.2f", total), ID_AGRICULTEUR);
                panierService.ajouter(panier);
                loadPanier();
                showAlert("Succès", "✅ \"" + eq.getNom() + "\" x" + qty + " ajouté au panier !");
            } catch (NumberFormatException ex) {
                showAlert("Erreur", "Veuillez entrer un nombre entier valide.");
            } catch (SQLException ex) {
                showAlert("Erreur BD", ex.getMessage());
            }
        });
    }

    // ======================== PANIER ========================

    private void loadPanier() {
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
        if (list.isEmpty()) {
            emptyPanier.setVisible(true);  emptyPanier.setManaged(true);
            gridPanier.setVisible(false);  gridPanier.setManaged(false);
            return;
        }
        emptyPanier.setVisible(false); emptyPanier.setManaged(false);
        gridPanier.setVisible(true);   gridPanier.setManaged(true);
        for (Panier p : list) gridPanier.getChildren().add(createPanierCard(p));
    }

    private VBox createPanierCard(Panier p) {
        String nom = getEquipementNameById(p.getId_equipement());
        Equipement eq = getEquipementById(p.getId_equipement());
        String type = eq != null ? eq.getType() : "";
        String bandColor = getTypeColor(type);

        VBox card = new VBox(0);
        card.setPrefWidth(300); card.setMaxWidth(300);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 16;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 12, 0, 0, 3);");

        HBox topBand = new HBox();
        topBand.setPrefHeight(7);
        topBand.setStyle("-fx-background-color: " + bandColor + "; -fx-background-radius: 16 16 0 0;");

        VBox body = new VBox(12);
        body.setPadding(new Insets(16, 18, 18, 18));

        HBox header = new HBox(10); header.setAlignment(Pos.CENTER_LEFT);
        Label icon = new Label(getTypeEmoji(type)); icon.setStyle("-fx-font-size: 26px;");
        VBox namePart = new VBox(3);
        Label nomLabel = new Label(nom);
        nomLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #1e3a1a; -fx-wrap-text: true;");
        nomLabel.setMaxWidth(210);
        Label typeBadge = new Label(type.isEmpty() ? "Équipement" : type);
        typeBadge.setStyle("-fx-background-color: " + bandColor + "22; -fx-text-fill: " + bandColor + ";" +
            "-fx-background-radius: 20; -fx-padding: 2 8; -fx-font-size: 10px; -fx-font-weight: bold;");
        namePart.getChildren().addAll(nomLabel, typeBadge);
        header.getChildren().addAll(icon, namePart);

        Separator sep = new Separator(); sep.setStyle("-fx-background-color: #eef5ec;");

        // Quantité
        HBox qtyRow = new HBox(10); qtyRow.setAlignment(Pos.CENTER_LEFT);
        Label qtyIcon = new Label("📦"); qtyIcon.setStyle("-fx-font-size: 14px;");
        Label qtyLabel = new Label("Quantité commandée :");
        qtyLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #5a7a50;");
        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
        Label qtyVal = new Label("×" + p.getQuantite());
        qtyVal.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: white;" +
            "-fx-background-color: #4a7c40; -fx-background-radius: 20; -fx-padding: 4 12;");
        qtyRow.getChildren().addAll(qtyIcon, qtyLabel, sp, qtyVal);

        // Total TND
        HBox totalTND = new HBox(10); totalTND.setAlignment(Pos.CENTER_LEFT);
        totalTND.setStyle("-fx-background-color: #e8f5e4; -fx-background-radius: 8; -fx-padding: 9 12;");
        Label totalLabel = new Label("Total (TND) :");
        totalLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #5a7a50;");
        Region sp2 = new Region(); HBox.setHgrow(sp2, Priority.ALWAYS);
        double tndVal = 0;
        try { tndVal = Double.parseDouble(p.getTotal().replace(",", ".")); } catch (Exception ignored) {}
        Label totalValLabel = new Label(String.format("%.2f TND", tndVal));
        totalValLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #2d5a25;");
        totalTND.getChildren().addAll(totalLabel, sp2, totalValLabel);

        // Total EUR + USD (conversions en temps réel)
        HBox conversions = new HBox(8); conversions.setAlignment(Pos.CENTER_RIGHT);
        Label eurLabel = new Label(String.format("%.2f €", tndVal * tauxEUR));
        eurLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #1565c0; -fx-background-color: #e3f2fd;" +
            "-fx-background-radius: 6; -fx-padding: 3 8; -fx-font-weight: bold;");
        Label usdLabel = new Label(String.format("$%.2f", tndVal * tauxUSD));
        usdLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #e65100; -fx-background-color: #fff8e1;" +
            "-fx-background-radius: 6; -fx-padding: 3 8; -fx-font-weight: bold;");
        conversions.getChildren().addAll(eurLabel, usdLabel);

        // Bouton retirer
        Button btnRetirer = new Button("🗑  Retirer du panier");
        btnRetirer.setMaxWidth(Double.MAX_VALUE);
        btnRetirer.setStyle("-fx-background-color: #fdf0f0; -fx-text-fill: #e74c3c;" +
            "-fx-background-radius: 9; -fx-font-size: 12px; -fx-font-weight: bold;" +
            "-fx-cursor: hand; -fx-padding: 9 0; -fx-border-color: #fac0c0; -fx-border-radius: 9;");
        btnRetirer.setOnAction(e -> retirerDuPanier(p.getId_panier()));
        btnRetirer.setOnMouseEntered(e -> btnRetirer.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white;" +
            "-fx-background-radius: 9; -fx-font-size: 12px; -fx-font-weight: bold; -fx-cursor: hand; -fx-padding: 9 0;"));
        btnRetirer.setOnMouseExited(e -> btnRetirer.setStyle("-fx-background-color: #fdf0f0; -fx-text-fill: #e74c3c;" +
            "-fx-background-radius: 9; -fx-font-size: 12px; -fx-font-weight: bold;" +
            "-fx-cursor: hand; -fx-padding: 9 0; -fx-border-color: #fac0c0; -fx-border-radius: 9;"));

        card.setOnMouseEntered(e -> card.setStyle("-fx-background-color: #fafffe; -fx-background-radius: 16;" +
            "-fx-effect: dropshadow(gaussian, rgba(74,124,64,0.25), 18, 0, 0, 6);"));
        card.setOnMouseExited(e -> card.setStyle("-fx-background-color: white; -fx-background-radius: 16;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 12, 0, 0, 3);"));

        body.getChildren().addAll(header, sep, qtyRow, totalTND, conversions, btnRetirer);
        card.getChildren().addAll(topBand, body);
        return card;
    }

    private void retirerDuPanier(int id) {
        try {
            panierService.supprimer(id);
            loadPanier();
        } catch (SQLException e) {
            showAlert("Erreur", "Impossible de retirer l'article : " + e.getMessage());
        }
    }

    @FXML
    private void confirmerCommande() {
        if (panierList.isEmpty()) {
            showAlert("Panier vide", "Votre panier est vide. Ajoutez des équipements avant de commander.");
            return;
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
            "Confirmer la commande de " + panierList.size() + " article(s) pour un total de "
            + String.format("%.2f", calcTotal()) + " TND ?",
            ButtonType.YES, ButtonType.NO);
        confirm.setTitle("Confirmation de commande");
        confirm.setHeaderText(null);
        confirm.showAndWait().ifPresent(r -> {
            if (r == ButtonType.YES)
                showAlert("Commande confirmée", "✅ Votre commande a été passée avec succès !\nNous vous contacterons bientôt.");
        });
    }

    private void updatePanierSummary() {
        double total = calcTotal();
        int nb = panierList.size();

        // Sidebar
        labelSousTotal.setText("Sous-total : " + String.format("%.2f", total) + " TND");
        labelGrandTotal.setText("TOTAL : " + String.format("%.2f", total) + " TND");
        labelCartBadge.setText(String.valueOf(nb));
        labelTotalPanier.setText(nb == 0 ? "" : nb + " art. — " + String.format("%.2f TND", total));

        // Panneau récap
        labelNbArticles.setText(nb + " article(s)");
        labelSousTotalDetail.setText(String.format("%.2f TND", total));
        labelGrandTotalDetail.setText(String.format("%.2f TND", total));

        // Carte devises (API 2)
        labelTotalTND.setText(String.format("%.2f TND", total));
        labelTotalEUR.setText(String.format("%.2f EUR", total * tauxEUR));
        labelTotalUSD.setText(String.format("%.2f USD", total * tauxUSD));
    }

    private double calcTotal() {
        return panierList.stream().mapToDouble(p -> {
            try { return Double.parseDouble(p.getTotal().replace(",", ".")); }
            catch (Exception e) { return 0; }
        }).sum();
    }

    // ======================== API 1 — RESTCOUNTRIES ========================

    private void chargerInfosPays(String pays) {
        new Thread(() -> {
            try {
                String urlStr = "https://restcountries.com/v3.1/name/" + pays + "?fullText=true";
                String json = appelHTTP(urlStr);
                JSONArray array = new JSONArray(json);
                JSONObject country = array.getJSONObject(0);

                String nom = country.getJSONObject("name").getString("common");
                String capitale = country.has("capital")
                    ? country.getJSONArray("capital").getString(0) : "N/A";
                String region = country.optString("subregion",
                    country.optString("region", "N/A"));
                long population = country.optLong("population", 0);
                String popStr = String.format("%,.0f hab.", (double) population);

                String devise = "N/A";
                if (country.has("currencies")) {
                    JSONObject currencies = country.getJSONObject("currencies");
                    String code = currencies.keys().next();
                    JSONObject cur = currencies.getJSONObject(code);
                    devise = cur.optString("name", code) + " (" + code + ")";
                }

                String dF = devise, cF = capitale, rF = region, pF = popStr, nF = nom;
                javafx.application.Platform.runLater(() -> {
                    labelPaysNom.setText(nF);
                    labelPaysDevise.setText(dF);
                    labelPaysCapitale.setText(cF);
                    labelPaysRegion.setText(rF);
                    labelPaysPopulation.setText(pF);
                });
            } catch (Exception e) {
                javafx.application.Platform.runLater(() -> {
                    labelPaysNom.setText("Tunisie");
                    labelPaysDevise.setText("Dinar Tunisien (TND)");
                    labelPaysCapitale.setText("Tunis");
                    labelPaysRegion.setText("Afrique du Nord");
                    labelPaysPopulation.setText("11 818 619 hab.");
                });
            }
        }).start();
    }

    // ======================== API 2 — EXCHANGERATE ========================

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
                    updatePanierSummary(); // rafraîchir les totaux avec les vrais taux
                    renderPanierGrid(panierList); // rafraîchir les cartes
                });
            } catch (Exception e) {
                // garder les taux par défaut
            }
        }).start();
    }

    // ======================== NAVIGATION ========================

    @FXML
    private void showCatalogue() {
        viewCatalogue.setVisible(true);  viewCatalogue.setManaged(true);
        viewPanier.setVisible(false);    viewPanier.setManaged(false);
        btnTabCatalogue.setStyle("-fx-background-color: white; -fx-background-radius: 10; -fx-padding: 11 15; -fx-cursor: hand;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 6, 0, 0, 2);");
        btnTabPanier.setStyle("-fx-background-color: rgba(255,255,255,0.1); -fx-background-radius: 10; -fx-padding: 11 15; -fx-cursor: hand;");
    }

    @FXML
    private void showPanier() {
        loadPanier();
        viewPanier.setVisible(true);     viewPanier.setManaged(true);
        viewCatalogue.setVisible(false); viewCatalogue.setManaged(false);
        btnTabPanier.setStyle("-fx-background-color: white; -fx-background-radius: 10; -fx-padding: 11 15; -fx-cursor: hand;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 6, 0, 0, 2);");
        btnTabCatalogue.setStyle("-fx-background-color: rgba(255,255,255,0.1); -fx-background-radius: 10; -fx-padding: 11 15; -fx-cursor: hand;");
    }

    @FXML
    private void goToLogin() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/Login.fxml"));
            Stage stage = (Stage) gridCatalogue.getScene().getWindow();
            stage.setScene(new Scene(root)); stage.centerOnScreen();
        } catch (Exception e) { e.printStackTrace(); }
    }

    // ======================== HELPERS ========================

    private String getEquipementNameById(int id) {
        if (allEquipements == null) return "Équipement #" + id;
        return allEquipements.stream().filter(e -> e.getId_equipement() == id)
            .findFirst().map(Equipement::getNom).orElse("Équipement #" + id);
    }

    private Equipement getEquipementById(int id) {
        if (allEquipements == null) return null;
        return allEquipements.stream().filter(e -> e.getId_equipement() == id)
            .findFirst().orElse(null);
    }

    private String getTypeColor(String type) {
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

    private void showAlert(String title, String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title); alert.setHeaderText(null);
        alert.setContentText(msg); alert.showAndWait();
    }
}
