package Model;

import java.time.LocalDate;

public class Depense {
    private int idDepense;
    private int userId;
    private TypeDepense type;
    private double montant;
    private String description;
    private LocalDate date;

    public Depense() {
    }

    public Depense(int idDepense, int userId, double montant, String description, LocalDate date) {
        this.idDepense = idDepense;
        this.userId = userId;
        this.montant = montant;
        this.description = description;
        this.date = date;
    }

    public int getIdDepense() {
        return idDepense;
    }

    public void setIdDepense(int idDepense) {
        this.idDepense = idDepense;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public TypeDepense getType() {
        return type;
    }

    public void setType(TypeDepense type) {
        this.type = type;
    }

    public double getMontant() {
        return montant;
    }

    public void setMontant(double montant) {
        this.montant = montant;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }
}
