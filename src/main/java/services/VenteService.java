package services;

import models.Vente;
import utils.MyDatabase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class VenteService {
    private final Connection connection;

    public VenteService() {
        this.connection = MyDatabase.getInstance().getConnection();
    }

    public void create(Vente v) throws SQLException {
        String sql = "INSERT INTO vente (userId, prixUnitaire, quantite, chiffreAffaires, date, produit) VALUES (?,?,?,?,?,?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, v.getUserId());
            ps.setDouble(2, v.getPrixUnitaire());
            ps.setDouble(3, v.getQuantite());
            ps.setDouble(4, v.getChiffreAffaires());
            ps.setDate(5, v.getDate() != null ? Date.valueOf(v.getDate()) : null);
            ps.setString(6, v.getProduit());
            ps.executeUpdate();
        }
    }

    public List<Vente> readByUser(int userId) throws SQLException {
        String sql = "SELECT idVente, prixUnitaire, quantite, chiffreAffaires, date, produit FROM vente WHERE userId = ?";
        List<Vente> list = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Vente v = new Vente();
                    v.setIdVente(rs.getInt("idVente"));
                    v.setUserId(userId);
                    v.setPrixUnitaire(rs.getDouble("prixUnitaire"));
                    v.setQuantite(rs.getDouble("quantite"));
                    v.setChiffreAffaires(rs.getDouble("chiffreAffaires"));
                    Date d = rs.getDate("date");
                    if (d != null) v.setDate(d.toLocalDate());
                    v.setProduit(rs.getString("produit"));
                    list.add(v);
                }
            }
        }
        return list;
    }

    public void update(Vente v) throws SQLException {
        String sql = "UPDATE vente SET prixUnitaire = ?, quantite = ?, chiffreAffaires = ?, date = ?, produit = ? WHERE idVente = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setDouble(1, v.getPrixUnitaire());
            ps.setDouble(2, v.getQuantite());
            ps.setDouble(3, v.getChiffreAffaires());
            ps.setDate(4, v.getDate() != null ? Date.valueOf(v.getDate()) : null);
            ps.setString(5, v.getProduit());
            ps.setInt(6, v.getIdVente());
            ps.executeUpdate();
        }
    }

    public void delete(int idVente) throws SQLException {
        String sql = "DELETE FROM vente WHERE idVente = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, idVente);
            ps.executeUpdate();
        }
    }
}
