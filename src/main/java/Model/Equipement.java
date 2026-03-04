package Model;

public class Equipement {

    private int id_equipement;
    private String nom;
    private String type;
    private String prix;
    private int quantite;
    private int id_fournisseur;

    public Equipement() {}

    public Equipement(String nom, String type, String prix, int quantite, int id_fournisseur) {
        this.nom = nom;
        this.type = type;
        this.prix = prix;
        this.quantite = quantite;
        this.id_fournisseur = id_fournisseur;
    }

    public Equipement(int id, String nom, String type, String prix, int quantite, int id_fournisseur) {
        this.id_equipement = id;
        this.nom = nom;
        this.type = type;
        this.prix = prix;
        this.quantite = quantite;
        this.id_fournisseur = id_fournisseur;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getPrix() {
        return prix;
    }

    public void setPrix(String prix) {
        this.prix = prix;
    }

    public int getQuantite() {
        return quantite;
    }

    public void setQuantite(int quantite) {
        this.quantite = quantite;
    }

    public int getId_fournisseur() {
        return id_fournisseur;
    }

    public void setId_fournisseur(int id_fournisseur) {
        this.id_fournisseur = id_fournisseur;
    }

    public int getId_equipement() {
        return id_equipement;
    }

    public void setId_equipement(int id_equipement) {
        this.id_equipement = id_equipement;
    }

    @Override
    public String toString() {
        return "Equipement{" +
                "id_fournisseur=" + id_fournisseur +
                ", quantite=" + quantite +
                ", prix='" + prix + '\'' +
                ", type='" + type + '\'' +
                ", nom='" + nom + '\'' +
                '}';
    }
}
