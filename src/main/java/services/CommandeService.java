package services;

import Model.Commande;
import Model.LigneCommande;
import utils.MyDatabase;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class CommandeService {

    private final Connection connection;

    public CommandeService() throws SQLException {
        connection = MyDatabase.getInstance().getConnection();
    }

    public int ajouter(Commande c) throws SQLException {
        String sql = "INSERT INTO commande (date_commande, total, agriculteur_id) VALUES (?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            LocalDateTime when = c.getDateCommande() != null ? c.getDateCommande() : LocalDateTime.now();
            ps.setTimestamp(1, Timestamp.valueOf(when));
            ps.setBigDecimal(2, c.getTotal() != null ? c.getTotal() : BigDecimal.ZERO);
            ps.setInt(3, c.getAgriculteurId());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    int id = keys.getInt(1);
                    c.setId(id);
                    return id;
                }
            }
        }
        return 0;
    }

    public List<Commande> findByAgriculteur(int agriculteurId) throws SQLException {
        String sql = "SELECT * FROM commande WHERE agriculteur_id = ? ORDER BY date_commande DESC";
        List<Commande> list = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, agriculteurId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        }
        return list;
    }

    public Commande findById(int id) throws SQLException {
        String sql = "SELECT * FROM commande WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Commande c = mapRow(rs);
                    c.setLignes(new LigneCommandeService(connection).findByCommande(c.getId()));
                    return c;
                }
            }
        }
        return null;
    }

    public List<Commande> findAll() throws SQLException {
        String sql = "SELECT * FROM commande ORDER BY date_commande DESC";
        List<Commande> list = new ArrayList<>();
        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) list.add(mapRow(rs));
        }
        return list;
    }

    public void supprimer(int id) throws SQLException {
        // ligne_commande has ON DELETE CASCADE, but be explicit for clarity.
        try (PreparedStatement ps = connection.prepareStatement("DELETE FROM ligne_commande WHERE commande_id = ?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
        try (PreparedStatement ps = connection.prepareStatement("DELETE FROM commande WHERE id = ?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    private static Commande mapRow(ResultSet rs) throws SQLException {
        Commande c = new Commande();
        c.setId(rs.getInt("id"));
        Timestamp ts = rs.getTimestamp("date_commande");
        if (ts != null) c.setDateCommande(ts.toLocalDateTime());
        c.setTotal(rs.getBigDecimal("total"));
        c.setAgriculteurId(rs.getInt("agriculteur_id"));
        return c;
    }
}
