package models;

import java.time.LocalDate;

public class EvennementAgricole {
    private int idEvennement;
    private String titre;
    private String description;
    private LocalDate dateDebut;
    private LocalDate dateFin;
    private String lieu;
    private int capaciteMax;
    private int fraisInscription;
    private String statut;

    public EvennementAgricole() {}

    public EvennementAgricole(int idEvennement, String titre, String description,
                              LocalDate dateDebut, LocalDate dateFin,
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

    public EvennementAgricole(String titre, String description,
                              LocalDate dateDebut, LocalDate dateFin,
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

    // Getters et Setters
    public int getIdEvennement() { return idEvennement; }
    public void setIdEvennement(int idEvennement) { this.idEvennement = idEvennement; }

    public String getTitre() { return titre; }
    public void setTitre(String titre) { this.titre = titre; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public LocalDate getDateDebut() { return dateDebut; }
    public void setDateDebut(LocalDate dateDebut) { this.dateDebut = dateDebut; }

    public LocalDate getDateFin() { return dateFin; }
    public void setDateFin(LocalDate dateFin) { this.dateFin = dateFin; }

    public String getLieu() { return lieu; }
    public void setLieu(String lieu) { this.lieu = lieu; }

    public int getCapaciteMax() { return capaciteMax; }
    public void setCapaciteMax(int capaciteMax) { this.capaciteMax = capaciteMax; }

    public int getFraisInscription() { return fraisInscription; }
    public void setFraisInscription(int fraisInscription) { this.fraisInscription = fraisInscription; }

    public String getStatut() { return statut; }
    public void setStatut(String statut) { this.statut = statut; }

    @Override
    public String toString() {
        return "EvennementAgricole{" +
                "idEvennement=" + idEvennement +
                ", titre='" + titre + '\'' +
                ", description='" + description + '\'' +
                ", dateDebut=" + dateDebut +
                ", dateFin=" + dateFin +
                ", lieu='" + lieu + '\'' +
                ", capaciteMax=" + capaciteMax +
                ", fraisInscription=" + fraisInscription +
                ", statut='" + statut + '\'' +
                '}';
    }
}
