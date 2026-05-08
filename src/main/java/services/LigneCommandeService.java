package services;

import Model.LigneCommande;
import utils.MyDatabase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class LigneCommandeService {

    private final Connection connection;

    public LigneCommandeService() throws SQLException {
        connection = MyDatabase.getInstance().getConnection();
    }

    /** Package-internal constructor used by CommandeService to avoid opening a second connection. */
    LigneCommandeService(Connection connection) {
        this.connection = connection;
    }

    public int ajouter(LigneCommande l) throws SQLException {
        String sql = "INSERT INTO ligne_commande (commande_id, equipement_id, quantite, prix_unitaire, total_ligne) "
                + "VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, l.getCommandeId());
            ps.setInt(2, l.getEquipementId());
            ps.setInt(3, l.getQuantite());
            ps.setBigDecimal(4, l.getPrixUnitaire());
            ps.setBigDecimal(5, l.getTotalLigne());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    int id = keys.getInt(1);
                    l.setId(id);
                    return id;
                }
            }
        }
        return 0;
    }

    public List<LigneCommande> findByCommande(int commandeId) throws SQLException {
        String sql = "SELECT * FROM ligne_commande WHERE commande_id = ?";
        List<LigneCommande> list = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, commandeId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        }
        return list;
    }

    public void supprimer(int id) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement("DELETE FROM ligne_commande WHERE id = ?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    private static LigneCommande mapRow(ResultSet rs) throws SQLException {
        LigneCommande l = new LigneCommande();
        l.setId(rs.getInt("id"));
        l.setCommandeId(rs.getInt("commande_id"));
        l.setEquipementId(rs.getInt("equipement_id"));
        l.setQuantite(rs.getInt("quantite"));
        l.setPrixUnitaire(rs.getBigDecimal("prix_unitaire"));
        l.setTotalLigne(rs.getBigDecimal("total_ligne"));
        return l;
    }
}
