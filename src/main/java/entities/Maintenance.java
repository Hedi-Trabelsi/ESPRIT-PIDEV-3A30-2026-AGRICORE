package entities;

import java.time.LocalDate;

public class Maintenance {
    private int id;
    private String type;
    private LocalDate dateDeclaration;
    private  String description;
    private  String statut;
    private  int idTechnicien;

    public Maintenance() {
    }

    public Maintenance(int id, String type, LocalDate dateDeclaration, String description, String statut, int idTechnicien) {
        this.id = id;
        this.type = type;
        this.dateDeclaration = dateDeclaration;
        this.description = description;
        this.statut = statut;
        this.idTechnicien = idTechnicien;
    }

    public Maintenance(String type, LocalDate dateDeclaration, String description, String statutory, int idTechnicien) {
        this.type = type;

        this.dateDeclaration = dateDeclaration;
        this.description = description;
        this.statut = statutory;
        this.idTechnicien = idTechnicien;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
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

    public int getIdTechnicien() {
        return idTechnicien;
    }

    public void setIdTechnicien(int idTechnicien) {
        this.idTechnicien = idTechnicien;
    }

    @Override
    public String toString() {
        return "Maintenance{" +
                "id=" + id +
                ", type='" + type + '\'' +
                ", dateDeclaration=" + dateDeclaration +
                ", description='" + description + '\'' +
                ", statutory='" + statut + '\'' +
                ", idTechnicien=" + idTechnicien +
                '}';
    }
}
