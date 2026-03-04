package Model;

public class Tache {
    private int id_tache;
    private String nomTache;
    private String date_prevue;
    private String desciption;
    private int cout_estimee;
    private int id_maintenace;
    private int evaluation; // Nouveau champ : 0 (neutre), 1 (like), -1 (dislike)
    private int id_technicien;
    public Tache() {
    }

    public Tache(int id_tache, String nomTache, String date_prevue, String desciption, int cout_estimee, int id_maintenace, int evaluation, int id_technicien) {
        this.id_tache = id_tache;
        this.nomTache = nomTache;
        this.date_prevue = date_prevue;
        this.desciption = desciption;
        this.cout_estimee = cout_estimee;
        this.id_maintenace = id_maintenace;
        this.evaluation = evaluation;
        this.id_technicien = id_technicien;
    }

    public Tache(String nomTache, String date_prevue, String desciption, int cout_estimee, int id_maintenace, int id_technicien) {
        this.nomTache = nomTache;
        this.date_prevue = date_prevue;
        this.desciption = desciption;
        this.cout_estimee = cout_estimee;
        this.id_maintenace = id_maintenace;
        this.id_technicien = id_technicien; // Ajout
        this.evaluation = 0;
    }

    public int getId_technicien() {
        return id_technicien;
    }

    public void setId_technicien(int id_technicien) {
        this.id_technicien = id_technicien;
    }
// --- GETTERS & SETTERS ---

    public int getId_tache() {
        return id_tache;
    }

    public void setId_tache(int id_tache) {
        this.id_tache = id_tache;
    }

    public String getNomTache() {
        return nomTache;
    }

    public void setNomTache(String nomTache) {
        this.nomTache = nomTache;
    }

    public String getDate_prevue() {
        return date_prevue;
    }

    public void setDate_prevue(String date_prevue) {
        this.date_prevue = date_prevue;
    }

    public String getDesciption() {
        return desciption;
    }

    public void setDesciption(String desciption) {
        this.desciption = desciption;
    }

    public int getCout_estimee() {
        return cout_estimee;
    }

    public void setCout_estimee(int cout_estimee) {
        this.cout_estimee = cout_estimee;
    }

    public int getId_maintenace() {
        return id_maintenace;
    }

    public void setId_maintenace(int id_maintenace) {
        this.id_maintenace = id_maintenace;
    }

    public int getEvaluation() {
        return evaluation;
    }

    public void setEvaluation(int evaluation) {
        this.evaluation = evaluation;
    }

    @Override
    public String toString() {
        return "Tache{" +
                "id_tache=" + id_tache +
                ", nomTache='" + nomTache + '\'' +
                ", date_prevue='" + date_prevue + '\'' +
                ", evaluation=" + evaluation +
                ", id_maintenace=" + id_maintenace +
                '}';
    }
}