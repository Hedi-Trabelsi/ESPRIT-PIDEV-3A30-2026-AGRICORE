package Model;

import java.time.LocalDate;

public class Participant {
    private int idParticipant;
    private int idUtilisateur;
    private int idEvennement;
    private LocalDate dateInscription;
    private String statutParticipation;
    private String montantPayee;
    private String confirmation;
    // New Fields
    private int nbrPlaces;
    private String nomParticipant;

    public Participant() {}

    public Participant(int idUtilisateur, int idEvennement, LocalDate dateInscription,
                       String statutParticipation, String montantPayee, String confirmation,
                       int nbrPlaces, String nomParticipant) {
        this.idUtilisateur = idUtilisateur;
        this.idEvennement = idEvennement;
        this.dateInscription = dateInscription;
        this.statutParticipation = statutParticipation;
        this.montantPayee = montantPayee;
        this.confirmation = confirmation;
        this.nbrPlaces = nbrPlaces;
        this.nomParticipant = nomParticipant;
    }

    // Constructor for Reading from DB
    public Participant(int idParticipant, int idUtilisateur, int idEvennement,
                       LocalDate dateInscription, String statutParticipation,
                       String montantPayee, String confirmation, int nbrPlaces, String nomParticipant) {
        this.idParticipant = idParticipant;
        this.idUtilisateur = idUtilisateur;
        this.idEvennement = idEvennement;
        this.dateInscription = dateInscription;
        this.statutParticipation = statutParticipation;
        this.montantPayee = montantPayee;
        this.confirmation = confirmation;
        this.nbrPlaces = nbrPlaces;
        this.nomParticipant = nomParticipant;
    }

    // Getters and Setters
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
    public int getNbrPlaces() { return nbrPlaces; }
    public void setNbrPlaces(int nbrPlaces) { this.nbrPlaces = nbrPlaces; }
    public String getNomParticipant() { return nomParticipant; }
    public void setNomParticipant(String nomParticipant) { this.nomParticipant = nomParticipant; }
}