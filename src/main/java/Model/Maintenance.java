package Model;



import java.time.LocalDate;

public class Maintenance {
    private int id;
    private String type;
    private LocalDate dateDeclaration;
    private  String description;
    private  String statut;
    private  int idTechnicien;
    private String priorite;
    private String lieu;
    private String equipement;


    public Maintenance() {
    }

    public Maintenance(int id, String type, LocalDate dateDeclaration, String description, String statut, int idTechnicien, String priorite, String lieu, String equipement) {
        this.id = id;
        this.type = type;
        this.dateDeclaration = dateDeclaration;
        this.description = description;
        this.statut = statut;
        this.idTechnicien = idTechnicien;
        this.priorite = priorite;
        this.lieu = lieu;
        this.equipement = equipement;

    }


    public Maintenance(LocalDate dateDeclaration, String type, String description, String statut, int idTechnicien, String priorite, String lieu, String equipement ) {
        this.dateDeclaration = dateDeclaration;

        this.type = type;
        this.description = description;
        this.statut = statut;
        this.idTechnicien = idTechnicien;
        this.priorite = priorite;
        this.lieu = lieu;
        this.equipement = equipement;

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
                ", type='" + type + '\'' +
                ", dateDeclaration=" + dateDeclaration +
                ", description='" + description + '\'' +
                ", statut='" + statut + '\'' +
                ", idTechnicien=" + idTechnicien +
                ", priorite='" + priorite + '\'' +
                ", lieu='" + lieu + '\'' +
                ", equipement='" + equipement + '\'' +
                '}';
    }
}
