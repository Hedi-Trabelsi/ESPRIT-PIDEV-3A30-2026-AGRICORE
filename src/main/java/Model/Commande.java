package Model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Commande {

    private int id;
    private LocalDateTime dateCommande;
    private BigDecimal total;
    private int agriculteurId;
    private List<LigneCommande> lignes = new ArrayList<>();

    public Commande() {
        this.dateCommande = LocalDateTime.now();
        this.total = BigDecimal.ZERO;
    }

    public Commande(int agriculteurId, BigDecimal total) {
        this();
        this.agriculteurId = agriculteurId;
        this.total = total;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public LocalDateTime getDateCommande() { return dateCommande; }
    public void setDateCommande(LocalDateTime dateCommande) { this.dateCommande = dateCommande; }

    public BigDecimal getTotal() { return total; }
    public void setTotal(BigDecimal total) { this.total = total; }

    public int getAgriculteurId() { return agriculteurId; }
    public void setAgriculteurId(int agriculteurId) { this.agriculteurId = agriculteurId; }

    public List<LigneCommande> getLignes() { return lignes; }
    public void setLignes(List<LigneCommande> lignes) { this.lignes = lignes; }

    public void addLigne(LigneCommande ligne) {
        this.lignes.add(ligne);
        ligne.setCommandeId(this.id);
    }
}
