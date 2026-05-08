package services;

import Model.Commande;
import Model.Equipement;
import Model.LigneCommande;
import Model.Panier;
import utils.MyDatabase;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class PanierService {

    Connection connection;

    public PanierService() {
        try {
            connection = MyDatabase.getInstance().getConnection();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void ajouter(Panier p) throws SQLException {
        String sql = "INSERT INTO panier (id_equipement,quantite,total,id_agriculteur) VALUES (?,?,?,?)";
        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setInt(1, p.getId_equipement());
        ps.setInt(2, p.getQuantite());
        ps.setString(3, p.getTotal());
        ps.setInt(4, p.getId_agriculteur());
        ps.executeUpdate();
    }

    public void ajouterOuIncrementer(int agriculteurId, int equipementId, int quantite) throws SQLException {
        if (quantite <= 0) return;

        EquipementService es = new EquipementService();
        Equipement eq = es.findById(equipementId);
        if (eq == null || !eq.isActive() || eq.getQuantite() <= 0) return;

        Panier existing = findOneByAgriculteurAndEquipement(agriculteurId, equipementId);
        int newQuantity = quantite;
        if (existing != null) {
            newQuantity += existing.getQuantite();
        }
        newQuantity = Math.min(newQuantity, eq.getQuantite());

        BigDecimal lineTotal = parsePrix(eq.getPrix())
                .multiply(BigDecimal.valueOf(newQuantity))
                .setScale(2, RoundingMode.HALF_UP);

        if (existing == null) {
            ajouter(new Panier(equipementId, newQuantity, lineTotal.toPlainString(), agriculteurId));
            return;
        }

        try (PreparedStatement ps = connection.prepareStatement(
                "UPDATE panier SET quantite = ?, total = ? WHERE id_panier = ?")) {
            ps.setInt(1, newQuantity);
            ps.setString(2, lineTotal.toPlainString());
            ps.setInt(3, existing.getId_panier());
            ps.executeUpdate();
        }
    }

    public List<Panier> afficher() throws SQLException {
        List<Panier> list = new ArrayList<>();
        ResultSet rs = connection.createStatement().executeQuery("SELECT * FROM panier");

        while (rs.next()) {
            Panier p = new Panier();
            p.setId_panier(rs.getInt("id_panier"));
            p.setId_equipement(rs.getInt("id_equipement"));
            p.setQuantite(rs.getInt("quantite"));
            p.setTotal(rs.getString("total"));
            p.setId_agriculteur(rs.getInt("id_agriculteur"));
            list.add(p);
        }
        return list;
    }

    public void supprimer(int id) throws SQLException {
        PreparedStatement ps = connection.prepareStatement("DELETE FROM panier WHERE id_panier=?");
        ps.setInt(1, id);
        ps.executeUpdate();
    }

    public void supprimerParEquipement(int agriculteurId, int equipementId) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(
                "DELETE FROM panier WHERE id_agriculteur = ? AND id_equipement = ?")) {
            ps.setInt(1, agriculteurId);
            ps.setInt(2, equipementId);
            ps.executeUpdate();
        }
    }

    public Panier findOneByAgriculteurAndEquipement(int agriculteurId, int equipementId) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT * FROM panier WHERE id_agriculteur = ? AND id_equipement = ? LIMIT 1")) {
            ps.setInt(1, agriculteurId);
            ps.setInt(2, equipementId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapPanier(rs);
                }
            }
        }
        return null;
    }

    public List<Panier> findByAgriculteur(int agriculteurId) throws SQLException {
        List<Panier> list = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT * FROM panier WHERE id_agriculteur = ?")) {
            ps.setInt(1, agriculteurId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapPanier(rs));
                }
            }
        }
        return list;
    }

    public void viderPanier(int agriculteurId) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(
                "DELETE FROM panier WHERE id_agriculteur = ?")) {
            ps.setInt(1, agriculteurId);
            ps.executeUpdate();
        }
    }

    /**
     * Convertit le panier d'un agriculteur en une `commande` + lignes
     * `ligne_commande`, décrémente le stock des équipements concernés et
     * vide le panier. Reproduit le flow `FrontEquipementController::confirmOrder`
     * du projet Symfony.
     *
     * @return l'identifiant de la commande créée, ou 0 si le panier est vide.
     */
    public int confirmerCommande(int agriculteurId) throws SQLException {
        List<Panier> panier = findByAgriculteur(agriculteurId);
        if (panier.isEmpty()) return 0;

        EquipementService es = new EquipementService();
        CommandeService cs = new CommandeService();
        LigneCommandeService ls = new LigneCommandeService();

        BigDecimal total = BigDecimal.ZERO;
        List<LigneCommande> lignes = new ArrayList<>();

        for (Panier item : panier) {
            Equipement eq = es.findById(item.getId_equipement());
            if (eq == null) continue;
            if (!eq.isActive() || eq.getQuantite() < item.getQuantite()) {
                throw new SQLException("Stock insuffisant pour " + eq.getNom());
            }
            BigDecimal prixUnit = parsePrix(eq.getPrix());
            BigDecimal totalLigne = prixUnit.multiply(BigDecimal.valueOf(item.getQuantite()))
                    .setScale(2, RoundingMode.HALF_UP);
            lignes.add(new LigneCommande(0, eq.getId_equipement(), item.getQuantite(),
                    prixUnit, totalLigne));
            total = total.add(totalLigne);
        }

        if (lignes.isEmpty()) return 0;

        Commande commande = new Commande(agriculteurId, total.setScale(2, RoundingMode.HALF_UP));
        int commandeId = cs.ajouter(commande);

        for (LigneCommande l : lignes) {
            l.setCommandeId(commandeId);
            ls.ajouter(l);
            es.decrementerStock(l.getEquipementId(), l.getQuantite());
        }

        viderPanier(agriculteurId);
        return commandeId;
    }

    private static BigDecimal parsePrix(String prix) {
        if (prix == null || prix.isBlank()) return BigDecimal.ZERO;
        try {
            return new BigDecimal(prix.replace(",", ".").trim());
        } catch (NumberFormatException ex) {
            return BigDecimal.ZERO;
        }
    }

    private static Panier mapPanier(ResultSet rs) throws SQLException {
        Panier p = new Panier();
        p.setId_panier(rs.getInt("id_panier"));
        p.setId_equipement(rs.getInt("id_equipement"));
        p.setQuantite(rs.getInt("quantite"));
        p.setTotal(rs.getString("total"));
        p.setId_agriculteur(rs.getInt("id_agriculteur"));
        return p;
    }
}
