package controllers;

import entities.Equipement;
import services.EquipementService;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.SQLException;
import java.util.ResourceBundle;

public class AjoutEquipementController implements Initializable {

    @FXML private TextField fieldNom, fieldType, fieldPrix, fieldQuantite;
    @FXML private Label labelMessage, labelTitre, labelSousTitre;
    @FXML private Label labelPaysNom, labelPaysDevise, labelPaysCapitale;
    @FXML private Label labelPaysRegion, labelPaysPopulation;
    @FXML private Label labelPrixEUR, labelPrixUSD;
    @FXML private Button btnAction;

    private EquipementService equipementService = new EquipementService();
    private static final int ID_FOURNISSEUR = 1;

    // null = mode AJOUT | non null = mode MODIFICATION
    private Equipement equipementToModify = null;

    // Taux par defaut (mis a jour par l'API 2)
    private double tauxEUR = 0.2981;
    private double tauxUSD = 0.3213;

    // Cle API ExchangeRate
    private static final String EXCHANGE_API_KEY = "0c3f3b97f846b6f5ced36eff";

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        chargerInfosPays("Tunisia");  // API 1 : RestCountries (sans cle)
        chargerTauxDeChange();         // API 2 : ExchangeRate
    }

    // ==================== API 1 : RESTCOUNTRIES ====================
    // Gratuite, sans cle, documentation : https://restcountries.com

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

                String region = country.optString("region", "N/A")
                    + " / " + country.optString("subregion", "");

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
                    labelPaysDevise.setText("Devise : " + dF);
                    labelPaysCapitale.setText("Capitale : " + cF);
                    labelPaysRegion.setText("Region : " + rF);
                    labelPaysPopulation.setText("Pop : " + pF);
                });

            } catch (Exception e) {
                javafx.application.Platform.runLater(() -> {
                    labelPaysNom.setText("Tunisie");
                    labelPaysDevise.setText("Devise : Dinar (TND)");
                    labelPaysCapitale.setText("Capitale : Tunis");
                    labelPaysRegion.setText("Region : Afrique du Nord");
                    labelPaysPopulation.setText("Pop : 11 818 619 hab.");
                });
            }
        }).start();
    }

    // ==================== API 2 : EXCHANGERATE ====================
    // Cle : 0c3f3b97f846b6f5ced36eff
    // URL : https://v6.exchangerate-api.com/v6/0c3f3b97f846b6f5ced36eff/latest/TND

    private void chargerTauxDeChange() {
        new Thread(() -> {
            try {
                String urlStr = "https://v6.exchangerate-api.com/v6/"
                    + EXCHANGE_API_KEY + "/latest/TND";
                String json = appelHTTP(urlStr);
                JSONObject obj = new JSONObject(json);
                JSONObject rates = obj.getJSONObject("conversion_rates");

                double eur = rates.getDouble("EUR");
                double usd = rates.getDouble("USD");

                javafx.application.Platform.runLater(() -> {
                    tauxEUR = eur;
                    tauxUSD = usd;
                    mettreAJourConversion(); // rafraichir si prix deja saisi
                });

            } catch (Exception e) {
                // garder les valeurs par defaut
            }
        }).start();
    }

    /**
     * Conversion en temps reel a chaque touche saisie dans le champ Prix
     * (Fonctionnalite avancee - mise a jour instantanee via API)
     */
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

    // ==================== AJOUT / MODIFICATION ====================

    /**
     * Appele depuis ListeEquipementsController.
     * Pre-remplit le formulaire en mode MODIFICATION.
     */
    public void setEquipementToModify(Equipement eq) {
        this.equipementToModify = eq;
        labelTitre.setText("Modifier l'Equipement");
        labelSousTitre.setText("Modifiez les informations de : " + eq.getNom());
        btnAction.setText("Enregistrer les modifications");
        btnAction.setStyle(
            "-fx-background-color: #f39c12; -fx-text-fill: white; " +
            "-fx-background-radius: 10; -fx-font-weight: bold; " +
            "-fx-font-size: 14px; -fx-cursor: hand; -fx-padding: 11 28;");
        fieldNom.setText(eq.getNom());
        fieldType.setText(eq.getType());
        fieldPrix.setText(eq.getPrix());
        fieldQuantite.setText(String.valueOf(eq.getQuantite()));
        mettreAJourConversion();
    }

    /**
     * Bouton principal : AJOUTER ou MODIFIER selon le mode actif.
     */
    @FXML
    private void sauvegarder() {
        if (!validateForm()) return;
        try {
            if (equipementToModify != null) {
                // ===== MODE MODIFICATION =====
                equipementToModify.setNom(fieldNom.getText().trim());
                equipementToModify.setType(fieldType.getText().trim());
                equipementToModify.setPrix(fieldPrix.getText().trim());
                equipementToModify.setQuantite(Integer.parseInt(fieldQuantite.getText().trim()));
                equipementToModify.setId_fournisseur(ID_FOURNISSEUR);
                equipementService.modifier(equipementToModify);
                showMessage("Equipement modifie avec succes !", true);
                equipementToModify = null;
                goToListe();
            } else {
                // ===== MODE AJOUT =====
                Equipement eq = new Equipement(
                    fieldNom.getText().trim(),
                    fieldType.getText().trim(),
                    fieldPrix.getText().trim(),
                    Integer.parseInt(fieldQuantite.getText().trim()),
                    ID_FOURNISSEUR
                );
                equipementService.ajouter(eq);
                showMessage("Equipement ajoute avec succes !", true);
                clearForm();
            }
        } catch (SQLException e) {
            showMessage("Erreur : " + e.getMessage(), false);
        }
    }

    @FXML
    private void clearForm() {
        fieldNom.clear();
        fieldType.clear();
        fieldPrix.clear();
        fieldQuantite.clear();
        equipementToModify = null;
        labelMessage.setText("");
        labelPrixEUR.setText("Saisir un prix...");
        labelPrixUSD.setText("");
        labelTitre.setText("Ajouter un Equipement");
        labelSousTitre.setText("Remplissez le formulaire pour ajouter un nouvel equipement agricole");
        btnAction.setText("+ Ajouter l'equipement");
        btnAction.setStyle(
            "-fx-background-color: #4a7c40; -fx-text-fill: white; " +
            "-fx-background-radius: 10; -fx-font-weight: bold; " +
            "-fx-font-size: 14px; -fx-cursor: hand; -fx-padding: 11 28;");
    }

    // ==================== NAVIGATION ====================

    @FXML private void goToListe() { navigateTo("/ListeEquipements.fxml"); }
    @FXML private void goToLogin() { navigateTo("/Login.fxml"); }

    private void navigateTo(String fxml) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(fxml));
            Stage stage = (Stage) fieldNom.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.centerOnScreen();
        } catch (Exception e) { e.printStackTrace(); }
    }

    // ==================== UTILITAIRES ====================

    private boolean validateForm() {
        if (fieldNom.getText().trim().isEmpty() || fieldType.getText().trim().isEmpty()
                || fieldPrix.getText().trim().isEmpty() || fieldQuantite.getText().trim().isEmpty()) {
            showMessage("Veuillez remplir tous les champs.", false);
            return false;
        }
        try {
            int q = Integer.parseInt(fieldQuantite.getText().trim());
            if (q < 0) {
                showMessage("La quantite ne peut pas etre negative.", false);
                return false;
            }
        } catch (NumberFormatException e) {
            showMessage("La quantite doit etre un entier valide.", false);
            return false;
        }
        try {
            double p = Double.parseDouble(fieldPrix.getText().trim().replace(",", "."));
            if (p < 0) {
                showMessage("Le prix ne peut pas etre negatif.", false);
                return false;
            }
        } catch (NumberFormatException e) {
            showMessage("Le prix doit etre un nombre valide.", false);
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

    private String appelHTTP(String urlStr) throws Exception {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(6000);
        conn.setReadTimeout(6000);
        BufferedReader reader = new BufferedReader(
            new InputStreamReader(conn.getInputStream()));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) sb.append(line);
        reader.close();
        return sb.toString();
    }
}
