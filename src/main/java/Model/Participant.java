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
    private int nbrPlaces;
    private String nomParticipant;
    private String email;          // Added to fix SQLException
    private String entryCode;
    private int nbrPresents;
    private String confirmToken; // The pipe-delimited storage (e.g., "2|3|1")

    // Default Constructor
    public Participant() {}

    // Constructor for Creation (without ID) - 12 Parameters
    public Participant(int idUtilisateur, int idEvennement, LocalDate dateInscription,
                       String statutParticipation, String montantPayee, String confirmation,
                       int nbrPlaces, String nomParticipant, String email, String entryCode,
                       int nbrPresents, String confirmToken) {
        this.idUtilisateur = idUtilisateur;
        this.idEvennement = idEvennement;
        this.dateInscription = dateInscription;
        this.statutParticipation = statutParticipation;
        this.montantPayee = montantPayee;
        this.confirmation = confirmation;
        this.nbrPlaces = nbrPlaces;
        this.nomParticipant = nomParticipant;
        this.email = email;
        this.entryCode = entryCode;
        this.nbrPresents = nbrPresents;
        this.confirmToken = confirmToken;
    }

    // Full Constructor for Reading from DB - 13 Parameters
    public Participant(int idParticipant, int idUtilisateur, int idEvennement,
                       LocalDate dateInscription, String statutParticipation,
                       String montantPayee, String confirmation, int nbrPlaces,
                       String nomParticipant, String email, String entryCode,
                       int nbrPresents, String confirmToken) {
        this.idParticipant = idParticipant;
        this.idUtilisateur = idUtilisateur;
        this.idEvennement = idEvennement;
        this.dateInscription = dateInscription;
        this.statutParticipation = statutParticipation;
        this.montantPayee = montantPayee;
        this.confirmation = confirmation;
        this.nbrPlaces = nbrPlaces;
        this.nomParticipant = nomParticipant;
        this.email = email;
        this.entryCode = entryCode;
        this.nbrPresents = nbrPresents;
        this.confirmToken = confirmToken;
    }

    // --- CUSTOM LOGIC METHODS ---

    /**
     * Extracts the presence count for a specific day from the pipe-delimited token.
     * @param dayIndex 0 for Day 1, 1 for Day 2, etc.
     * @return The count of presents for that day.
     */
    public int getPresenceForDay(int dayIndex) {
        if (confirmToken == null || confirmToken.isEmpty()) return 0;
        String[] days = confirmToken.split("\\|");
        if (dayIndex >= 0 && dayIndex < days.length) {
            try {
                return Integer.parseInt(days[dayIndex]);
            } catch (NumberFormatException e) {
                return 0;
            }
        }
        return 0;
    }

    // --- GETTERS AND SETTERS ---

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

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getEntryCode() { return entryCode; }
    public void setEntryCode(String entryCode) { this.entryCode = entryCode; }

    public int getNbrPresents() { return nbrPresents; }
    public void setNbrPresents(int nbrPresents) { this.nbrPresents = nbrPresents; }

    public String getConfirmToken() { return confirmToken; }
    public void setConfirmToken(String confirmToken) { this.confirmToken = confirmToken; }
}