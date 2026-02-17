package models;

import java.sql.Date;

public class Animal {

    private int idAnimal;
    private int idAgriculteur;
    private String codeAnimal;
    private String espece;
    private String race;
    private String sexe;
    private Date dateNaissance;

    public Animal() {}

    public Animal(int idAgriculteur, String codeAnimal, String espece,
                  String race, String sexe, Date dateNaissance) {
        this.idAgriculteur = idAgriculteur;
        this.codeAnimal = codeAnimal;
        this.espece = espece;
        this.race = race;
        this.sexe = sexe;
        this.dateNaissance = dateNaissance;
    }

    // GETTERS & SETTERS

    public int getIdAnimal() {
        return idAnimal;
    }

    public void setIdAnimal(int idAnimal) {
        this.idAnimal = idAnimal;
    }

    public int getIdAgriculteur() {
        return idAgriculteur;
    }

    public void setIdAgriculteur(int idAgriculteur) {
        this.idAgriculteur = idAgriculteur;
    }

    public String getCodeAnimal() {
        return codeAnimal;
    }

    public void setCodeAnimal(String codeAnimal) {
        this.codeAnimal = codeAnimal;
    }

    public String getEspece() {
        return espece;
    }

    public void setEspece(String espece) {
        this.espece = espece;
    }

    public String getRace() {
        return race;
    }

    public void setRace(String race) {
        this.race = race;
    }

    public String getSexe() {
        return sexe;
    }

    public void setSexe(String sexe) {
        this.sexe = sexe;
    }

    public Date getDateNaissance() {
        return dateNaissance;
    }

    public void setDateNaissance(Date dateNaissance) {
        this.dateNaissance = dateNaissance;
    }
}