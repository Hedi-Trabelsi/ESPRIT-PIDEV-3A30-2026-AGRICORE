package Model;

import java.time.LocalDate;

public class Maintenance {
    private int id;
    private String nom_maintenance; // Nouveau champ
    private String type;
    private LocalDate dateDeclaration;
    private String description;
    private String statut;
    private int idAgriculteur;
    private String priorite;
    private String lieu;
    private String equipement;

    // Constructeur vide
    public Maintenance() {
    }

    public Maintenance(int id, String nom_maintenance, String type, LocalDate dateDeclaration, String description, String statut, int idAgriculteur, String priorite, String lieu, String equipement) {
        this.id = id;
        this.nom_maintenance = nom_maintenance;
        this.type = type;
        this.dateDeclaration = dateDeclaration;
        this.description = description;
        this.statut = statut;
        this.idAgriculteur = idAgriculteur;
        this.priorite = priorite;
        this.lieu = lieu;
        this.equipement = equipement;
    }

    // Constructeur sans ID (pour l'insertion dans la BD)
    public Maintenance(String nom_maintenance, String type, LocalDate dateDeclaration, String description, String statut, int idAgriculteur, String priorite, String lieu, String equipement) {
        this.nom_maintenance = nom_maintenance;
        this.dateDeclaration = dateDeclaration;
        this.type = type;
        this.description = description;
        this.statut = statut;
        this.idAgriculteur = idAgriculteur;
        this.priorite = priorite;
        this.lieu = lieu;
        this.equipement = equipement;
    }

    // --- Getters et Setters ---

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getNom_maintenance() {
        return nom_maintenance;
    }

    public void setNom_maintenance(String nom_maintenance) {
        this.nom_maintenance = nom_maintenance;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public LocalDate getDateDeclaration() {
        return dateDeclaration;
    }

    public void setDateDeclaration(LocalDate dateDeclaration) {
        this.dateDeclaration = dateDeclaration;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getStatut() {
        return statut;
    }

    public void setStatut(String statut) {
        this.statut = statut;
    }

    public int getIdAgriculteur() {
        return idAgriculteur;
    }

    public void setIdAgriculteur(int idAgriculteur) {
        this.idAgriculteur = idAgriculteur;
    }

    public String getPriorite() {
        return priorite;
    }

    public void setPriorite(String priorite) {
        this.priorite = priorite;
    }

    public String getLieu() {
        return lieu;
    }

    public void setLieu(String lieu) {
        this.lieu = lieu;
    }

    public String getEquipement() {
        return equipement;
    }

    public void setEquipement(String equipement) {
        this.equipement = equipement;
    }

    @Override
    public String toString() {
        return "Maintenance{" +
                "id=" + id +
                ", nom_maintenance='" + nom_maintenance + '\'' +
                ", type='" + type + '\'' +
                ", dateDeclaration=" + dateDeclaration +
                ", description='" + description + '\'' +
                ", statut='" + statut + '\'' +
                ", idAgriculteur=" + idAgriculteur +
                ", priorite='" + priorite + '\'' +
                ", lieu='" + lieu + '\'' +
                ", equipement='" + equipement + '\'' +
                '}';
    }
}