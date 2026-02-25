package models;

import java.time.LocalDate;

public class Vente {
    private int idVente;
    private int userId;

    private double prixUnitaire;
    private double quantite;
    private double chiffreAffaires;
    private LocalDate date;
    private String produit;


    public Vente() {
    }

    public Vente(int idVente, int userId, String client, double prixUnitaire, double quantite, double chiffreAffaires, LocalDate date, String produit, Integer idRecolte) {
        this.idVente = idVente;
        this.userId = userId;
        this.prixUnitaire = prixUnitaire;
        this.quantite = quantite;
        this.chiffreAffaires = chiffreAffaires;
        this.date = date;
        this.produit = produit;
    }

    public int getIdVente() {
        return idVente;
    }

    public void setIdVente(int idVente) {
        this.idVente = idVente;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }





    public double getPrixUnitaire() {
        return prixUnitaire;
    }

    public void setPrixUnitaire(double prixUnitaire) {
        this.prixUnitaire = prixUnitaire;
    }

    public double getQuantite() {
        return quantite;
    }

    public void setQuantite(double quantite) {
        this.quantite = quantite;
    }

    public double getChiffreAffaires() {
        return chiffreAffaires;
    }

    public void setChiffreAffaires(double chiffreAffaires) {
        this.chiffreAffaires = chiffreAffaires;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public String getProduit() {
        return produit;
    }

    public void setProduit(String produit) {
        this.produit = produit;
    }


}
