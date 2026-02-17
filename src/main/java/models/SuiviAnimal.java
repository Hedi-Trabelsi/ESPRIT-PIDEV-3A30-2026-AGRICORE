package models;

import java.sql.Timestamp;

public class SuiviAnimal {

    private int idSuivi;
    private int idAnimal;
    private Timestamp dateSuivi;
    private double temperature;
    private double poids;
    private int rythmeCardiaque;
    private String niveauActivite;
    private String etatSante;
    private String remarque;

    public SuiviAnimal(){}

    public SuiviAnimal(int idAnimal, Timestamp dateSuivi,
                       double temperature, double poids,
                       int rythmeCardiaque, String niveauActivite,
                       String etatSante, String remarque) {
        this.idAnimal = idAnimal;
        this.dateSuivi = dateSuivi;
        this.temperature = temperature;
        this.poids = poids;
        this.rythmeCardiaque = rythmeCardiaque;
        this.niveauActivite = niveauActivite;
        this.etatSante = etatSante;
        this.remarque = remarque;
    }

    // GETTERS & SETTERS

    public int getIdSuivi() {
        return idSuivi;
    }

    public void setIdSuivi(int idSuivi) {
        this.idSuivi = idSuivi;
    }

    public int getIdAnimal() {
        return idAnimal;
    }

    public void setIdAnimal(int idAnimal) {
        this.idAnimal = idAnimal;
    }

    public Timestamp getDateSuivi() {
        return dateSuivi;
    }

    public void setDateSuivi(Timestamp dateSuivi) {
        this.dateSuivi = dateSuivi;
    }

    public double getTemperature() {
        return temperature;
    }

    public void setTemperature(double temperature) {
        this.temperature = temperature;
    }

    public double getPoids() {
        return poids;
    }

    public void setPoids(double poids) {
        this.poids = poids;
    }

    public int getRythmeCardiaque() {
        return rythmeCardiaque;
    }

    public void setRythmeCardiaque(int rythmeCardiaque) {
        this.rythmeCardiaque = rythmeCardiaque;
    }

    public String getNiveauActivite() {
        return niveauActivite;
    }

    public void setNiveauActivite(String niveauActivite) {
        this.niveauActivite = niveauActivite;
    }

    public String getEtatSante() {
        return etatSante;
    }

    public void setEtatSante(String etatSante) {
        this.etatSante = etatSante;
    }

    public String getRemarque() {
        return remarque;
    }

    public void setRemarque(String remarque) {
        this.remarque = remarque;
    }
}