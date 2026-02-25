package Model;

public class Tache {
    private int id_tache;
    private String nomTache; // Nouveau champ
    private String date_prevue;
    private String desciption; // Gardé tel quel selon ton code
    private int cout_estimee;
    private int id_maintenace;

    public Tache() {
    }

    // Constructeur complet (pour la lecture depuis la DB)
    public Tache(int id_tache, String nomTache, String date_prevue, String desciption, int cout_estimee, int id_maintenace) {
        this.id_tache = id_tache;
        this.nomTache = nomTache;
        this.date_prevue = date_prevue;
        this.desciption = desciption;
        this.cout_estimee = cout_estimee;
        this.id_maintenace = id_maintenace;
    }

    // Constructeur sans ID (pour l'ajout)
    public Tache(String nomTache, String date_prevue, String desciption, int cout_estimee, int id_maintenace) {
        this.nomTache = nomTache;
        this.date_prevue = date_prevue;
        this.desciption = desciption;
        this.cout_estimee = cout_estimee;
        this.id_maintenace = id_maintenace;
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

    @Override
    public String toString() {
        return "Tache{" +
                "id_tache=" + id_tache +
                ", nomTache='" + nomTache + '\'' +
                ", date_prevue='" + date_prevue + '\'' +
                ", desciption='" + desciption + '\'' +
                ", cout_estimee=" + cout_estimee +
                ", id_maintenace=" + id_maintenace +
                '}';
    }
}