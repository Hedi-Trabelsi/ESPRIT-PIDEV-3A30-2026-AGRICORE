package controllers;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import models.User;
import models.Vente;
import services.VenteService;

import java.time.LocalDate;
import java.util.function.Consumer;

public class VenteFormController {
    @FXML
    private TextField prixField;
    @FXML
    private TextField quantiteField;
    @FXML
    private TextField produitField;
    @FXML
    private DatePicker dateField;
    @FXML
    private Label prixError;
    @FXML
    private Label quantiteError;
    @FXML
    private Label produitError;
    @FXML
    private Label dateError;
    @FXML
    private Label caValue;

    private final VenteService venteService = new VenteService();
    private User user;
    private Vente editing;
    private Consumer<Void> onSaved;
    @FXML
    private Button saveBtn;

    @FXML
    void initialize() {
        prixField.textProperty().addListener((o, a, b) -> recalcCA());
        quantiteField.textProperty().addListener((o, a, b) -> recalcCA());
        if (saveBtn != null) saveBtn.setDisable(true);
        prixField.textProperty().addListener((o,a,b)->validateLive());
        quantiteField.textProperty().addListener((o,a,b)->validateLive());
        produitField.textProperty().addListener((o,a,b)->validateLive());
        dateField.valueProperty().addListener((o,a,b)->validateLive());
    }

    public void setUser(User user) {
        this.user = user;
    }

    public void setEditing(Vente v) {
        this.editing = v;
        if (v != null) {
            prixField.setText(String.valueOf(v.getPrixUnitaire()));
            quantiteField.setText(String.valueOf(v.getQuantite()));
            produitField.setText(v.getProduit());
            dateField.setValue(v.getDate());
            recalcCA();
        }
    }

    public void setOnSaved(Consumer<Void> onSaved) {
        this.onSaved = onSaved;
    }

    @FXML
    void save() {
        clearErrors();
        boolean valid = true;
        double prix = 0.0;
        try {
            prix = Double.parseDouble(prixField.getText());
            if (prix <= 0) {
                prixError.setText("Prix > 0 requis");
                valid = false;
            }
        } catch (Exception e) {
            prixError.setText("Prix numérique requis");
            valid = false;
        }
        double qte = 0.0;
        try {
            qte = Double.parseDouble(quantiteField.getText());
            if (qte <= 0) {
                quantiteError.setText("Qté > 0 requise");
                valid = false;
            }
        } catch (Exception e) {
            quantiteError.setText("Qté numérique requise");
            valid = false;
        }
        String produit = produitField.getText();
        if (produit == null || produit.isBlank()) {
            produitError.setText("Produit requis");
            valid = false;
        }
        LocalDate date = dateField.getValue();
        if (date == null) {
            dateError.setText("Date requise");
            valid = false;
        }
        if (!valid) return;

        try {
            double ca = prix * qte;
            if (editing != null) {
                editing.setPrixUnitaire(prix);
                editing.setQuantite(qte);
                editing.setProduit(produit);
                editing.setDate(date);
                editing.setChiffreAffaires(ca);
                venteService.update(editing);
            } else {
                Vente v = new Vente(0, user.getId(), null, prix, qte, ca, date, produit, null);
                venteService.create(v);
            }
            if (onSaved != null) onSaved.accept(null);
            closeStage();
        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR, e.getMessage()).showAndWait();
        }
    }

    @FXML
    void cancel() {
        closeStage();
    }

    private void recalcCA() {
        try {
            double p = Double.parseDouble(prixField.getText());
            double q = Double.parseDouble(quantiteField.getText());
            caValue.setText(String.format("%.2f", p * q));
        } catch (Exception e) {
            caValue.setText("0.0");
        }
    }

    private void clearErrors() {
        prixError.setText(" ");
        quantiteError.setText(" ");
        produitError.setText(" ");
        dateError.setText(" ");
    }

    private void closeStage() {
        Stage s = (Stage) prixField.getScene().getWindow();
        s.close();
    }

    private void validateLive() {
        clearErrors();
        boolean valid = true;
        try {
            double p = Double.parseDouble(prixField.getText());
            if (p <= 0) {
                prixError.setText("Prix > 0 requis");
                valid = false;
            }
        } catch (Exception e) {
            prixError.setText("Prix numérique requis");
            valid = false;
        }
        try {
            double q = Double.parseDouble(quantiteField.getText());
            if (q <= 0) {
                quantiteError.setText("Qté > 0 requise");
                valid = false;
            }
        } catch (Exception e) {
            quantiteError.setText("Qté numérique requise");
            valid = false;
        }
        if (produitField.getText() == null || produitField.getText().isBlank()) {
            produitError.setText("Produit requis");
            valid = false;
        }
        if (dateField.getValue() == null) {
            dateError.setText("Date requise");
            valid = false;
        }
        if (saveBtn != null) saveBtn.setDisable(!valid);
    }
}
