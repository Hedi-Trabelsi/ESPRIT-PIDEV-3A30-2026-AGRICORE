package Model;

import java.math.BigDecimal;

public class LigneCommande {

    private int id;
    private int commandeId;
    private int equipementId;
    private int quantite;
    private BigDecimal prixUnitaire;
    private BigDecimal totalLigne;

    public LigneCommande() {}

    public LigneCommande(int commandeId, int equipementId, int quantite,
                         BigDecimal prixUnitaire, BigDecimal totalLigne) {
        this.commandeId = commandeId;
        this.equipementId = equipementId;
        this.quantite = quantite;
        this.prixUnitaire = prixUnitaire;
        this.totalLigne = totalLigne;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getCommandeId() { return commandeId; }
    public void setCommandeId(int commandeId) { this.commandeId = commandeId; }

    public int getEquipementId() { return equipementId; }
    public void setEquipementId(int equipementId) { this.equipementId = equipementId; }

    public int getQuantite() { return quantite; }
    public void setQuantite(int quantite) { this.quantite = quantite; }

    public BigDecimal getPrixUnitaire() { return prixUnitaire; }
    public void setPrixUnitaire(BigDecimal prixUnitaire) { this.prixUnitaire = prixUnitaire; }

    public BigDecimal getTotalLigne() { return totalLigne; }
    public void setTotalLigne(BigDecimal totalLigne) { this.totalLigne = totalLigne; }
}
