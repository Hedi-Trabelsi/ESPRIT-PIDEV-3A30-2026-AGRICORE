package models;

import java.time.LocalDateTime;

public class EvennementAgricole {
    private int idEvennement;
    private String titre;
    private String description;
    private LocalDateTime dateDebut; // Changé de LocalDate à LocalDateTime
    private LocalDateTime dateFin;   // Changé de LocalDate à LocalDateTime
    private String lieu;
    private int capaciteMax;
    private int fraisInscription;
    private String statut;

    // --- CONSTRUCTEURS ---

    /**
     * Constructeur par défaut
     */
    public EvennementAgricole() {}

    /**
     * Constructeur complet (utile pour la lecture depuis la base de données)
     */
    public EvennementAgricole(int idEvennement, String titre, String description,
                              LocalDateTime dateDebut, LocalDateTime dateFin,
                              String lieu, int capaciteMax, int fraisInscription, String statut) {
        this.idEvennement = idEvennement;
        this.titre = titre;
        this.description = description;
        this.dateDebut = dateDebut;
        this.dateFin = dateFin;
        this.lieu = lieu;
        this.capaciteMax = capaciteMax;
        this.fraisInscription = fraisInscription;
        this.statut = statut;
    }

    /**
     * Constructeur sans ID (utile pour la création d'un nouvel événement avant insertion SQL)
     */
    public EvennementAgricole(String titre, String description,
                              LocalDateTime dateDebut, LocalDateTime dateFin,
                              String lieu, int capaciteMax, int fraisInscription, String statut) {
        this.titre = titre;
        this.description = description;
        this.dateDebut = dateDebut;
        this.dateFin = dateFin;
        this.lieu = lieu;
        this.capaciteMax = capaciteMax;
        this.fraisInscription = fraisInscription;
        this.statut = statut;
    }

    // --- GETTERS ET SETTERS ---

    public int getIdEvennement() {
        return idEvennement;
    }

    public void setIdEvennement(int idEvennement) {
        this.idEvennement = idEvennement;
    }

    public String getTitre() {
        return titre;
    }

    public void setTitre(String titre) {
        this.titre = titre;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDateTime getDateDebut() {
        return dateDebut;
    }

    public void setDateDebut(LocalDateTime dateDebut) {
        this.dateDebut = dateDebut;
    }

    public LocalDateTime getDateFin() {
        return dateFin;
    }

    public void setDateFin(LocalDateTime dateFin) {
        this.dateFin = dateFin;
    }

    public String getLieu() {
        return lieu;
    }

    public void setLieu(String lieu) {
        this.lieu = lieu;
    }

    public int getCapaciteMax() {
        return capaciteMax;
    }

    public void setCapaciteMax(int capaciteMax) {
        this.capaciteMax = capaciteMax;
    }

    public int getFraisInscription() {
        return fraisInscription;
    }

    public void setFraisInscription(int fraisInscription) {
        this.fraisInscription = fraisInscription;
    }

    public String getStatut() {
        return statut;
    }

    public void setStatut(String statut) {
        this.statut = statut;
    }

    // --- MÉTHODES UTILES ---

    @Override
    public String toString() {
        return "EvennementAgricole{" +
                "idEvennement=" + idEvennement +
                ", titre='" + titre + '\'' +
                ", dateDebut=" + dateDebut +
                ", dateFin=" + dateFin +
                ", lieu='" + lieu + '\'' +
                ", frais=" + fraisInscription +
                ", statut='" + statut + '\'' +
                '}';
    }
}