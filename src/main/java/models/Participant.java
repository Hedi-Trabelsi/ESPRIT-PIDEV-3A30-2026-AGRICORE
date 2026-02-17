package models;

import java.time.LocalDate;

public class Participant {
    private int idParticipant;
    private int idUtilisateur;
    private int idEvennement;
    private LocalDate dateInscription;
    private String statutParticipation;
    private String montantPayee;
    private String confirmation;

    public Participant() {}

    public Participant(int idParticipant, int idUtilisateur, int idEvennement,
                       LocalDate dateInscription, String statutParticipation,
                       String montantPayee, String confirmation) {
        this.idParticipant = idParticipant;
        this.idUtilisateur = idUtilisateur;
        this.idEvennement = idEvennement;
        this.dateInscription = dateInscription;
        this.statutParticipation = statutParticipation;
        this.montantPayee = montantPayee;
        this.confirmation = confirmation;
    }

    public Participant(int idUtilisateur, int idEvennement,
                       LocalDate dateInscription, String statutParticipation,
                       String montantPayee, String confirmation) {
        this.idUtilisateur = idUtilisateur;
        this.idEvennement = idEvennement;
        this.dateInscription = dateInscription;
        this.statutParticipation = statutParticipation;
        this.montantPayee = montantPayee;
        this.confirmation = confirmation;
    }

    // Getters et Setters
    public int getIdParticipant() { return idParticipant; }
    public void setIdParticipant(int idParticipant) { this.idParticipant = idParticipant; }

    public int getIdUtilisateur() { return idUtilisateur; }
    public void setIdUtilisateur(int idUtilisateur) { this.idUtilisateur = idUtilisateur; }

    public int getIdEvennement() { return idEvennement; }
    public void setIdEvennement(int idEvennement) { this.idEvennement = idEvennement; }

    public LocalDate getDateInscription() { return dateInscription; }
    public void setDateInscription(LocalDate dateInscription) { this.dateInscription = dateInscription; }

    public String getStatutParticipation() { return statutParticipation; }
    public void setStatutParticipation(String statutParticipation) { this.statutParticipation = statutParticipation; }

    public String getMontantPayee() { return montantPayee; }
    public void setMontantPayee(String montantPayee) { this.montantPayee = montantPayee; }

    public String getConfirmation() { return confirmation; }
    public void setConfirmation(String confirmation) { this.confirmation = confirmation; }

    @Override
    public String toString() {
        return "Participant{" +
                "idParticipant=" + idParticipant +
                ", idUtilisateur=" + idUtilisateur +
                ", idEvennement=" + idEvennement +
                ", dateInscription=" + dateInscription +
                ", statutParticipation='" + statutParticipation + '\'' +
                ", montantPayee='" + montantPayee + '\'' +
                ", confirmation='" + confirmation + '\'' +
                '}';
    }
}
