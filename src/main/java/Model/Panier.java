package Model;

public class Panier {

    private int id_panier;
    private int id_equipement;
    private int quantite;
    private String total;
    private int id_agriculteur;

    public Panier() {}

    public Panier(int id_equipement, int quantite, String total, int id_agriculteur) {
        this.id_equipement = id_equipement;
        this.quantite = quantite;
        this.total = total;
        this.id_agriculteur = id_agriculteur;
    }

    public int getId_panier() {
        return id_panier;
    }

    public void setId_panier(int id_panier) {
        this.id_panier = id_panier;
    }

    public int getId_equipement() {
        return id_equipement;
    }

    public void setId_equipement(int id_equipement) {
        this.id_equipement = id_equipement;
    }

    public int getQuantite() {
        return quantite;
    }

    public void setQuantite(int quantite) {
        this.quantite = quantite;
    }

    public String getTotal() {
        return total;
    }

    public void setTotal(String total) {
        this.total = total;
    }

    public int getId_agriculteur() {
        return id_agriculteur;
    }

    public void setId_agriculteur(int id_agriculteur) {
        this.id_agriculteur = id_agriculteur;
    }

    @Override
    public String toString() {
        return "Panier{" +
                "id_equipement=" + id_equipement +
                ", quantite=" + quantite +
                ", total='" + total + '\'' +
                ", id_agriculteur=" + id_agriculteur +
                '}';
    }
}
