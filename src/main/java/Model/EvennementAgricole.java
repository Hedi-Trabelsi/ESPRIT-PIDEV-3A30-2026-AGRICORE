package Model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class EvennementAgricole {
    private int idEvennement;
    private String titre;
    private String description;
    private LocalDateTime dateDebut;
    private LocalDateTime dateFin;
    private String lieu;
    private int capaciteMax;
    private int fraisInscription;
    private String statut;

    // --- CONSTRUCTEURS ---

    public EvennementAgricole() {}

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

    // --- MÉTHODES DE COMPATIBILITÉ POUR LE CONTROLLER ---

    /**
     * Utilisé par le contrôleur pour afficher le badge de catégorie.
     * Retourne le statut ou une valeur par défaut.
     */
    public String getType() {
        return (statut != null) ? statut : "Général";
    }

    /**
     * Utilisé par le contrôleur pour l'affichage de la date.
     * Retourne la date de début formatée en String.
     */
    public String getDate() {
        if (dateDebut == null) return "Date non définie";
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        return dateDebut.format(formatter);
    }

    // --- GETTERS ET SETTERS ---

    public int getIdEvennement() { return idEvennement; }
    public void setIdEvennement(int idEvennement) { this.idEvennement = idEvennement; }

    public String getTitre() { return titre; }
    public void setTitre(String titre) { this.titre = titre; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public LocalDateTime getDateDebut() { return dateDebut; }
    public void setDateDebut(LocalDateTime dateDebut) { this.dateDebut = dateDebut; }

    public LocalDateTime getDateFin() { return dateFin; }
    public void setDateFin(LocalDateTime dateFin) { this.dateFin = dateFin; }

    public String getLieu() { return lieu; }
    public void setLieu(String lieu) { this.lieu = lieu; }

    public int getCapaciteMax() { return capaciteMax; }
    public void setCapaciteMax(int capaciteMax) { this.capaciteMax = capaciteMax; }

    public int getFraisInscription() { return fraisInscription; }
    public void setFraisInscription(int fraisInscription) { this.fraisInscription = fraisInscription; }

    public String getStatut() { return statut; }
    public void setStatut(String statut) { this.statut = statut; }

    // --- MÉTHODES UTILES ---

    @Override
    public String toString() {
        return "EvennementAgricole{" +
                "idEvennement=" + idEvennement +
                ", titre='" + titre + '\'' +
                ", dateDebut=" + dateDebut +
                ", lieu='" + lieu + '\'' +
                ", statut='" + statut + '\'' +
                '}';
    }
}