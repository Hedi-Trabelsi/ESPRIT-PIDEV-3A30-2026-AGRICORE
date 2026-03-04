package services;

import Model.Depense;
import Model.TypeDepense;
import utils.MyDatabase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DepenseService {
    private final Connection connection;

    public DepenseService() throws SQLException {
        this.connection = MyDatabase.getInstance().getConnection();
    }

    public void create(Depense d) throws SQLException {
        System.out.println("[DEBUG] DepenseService.create - inserting depense for userId=" + d.getUserId());
        String sql = "INSERT INTO depense (userId, type, montant, date) VALUES (?,?,?,?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, d.getUserId());
            ps.setString(2, d.getType() != null ? d.getType().name() : null);
            ps.setDouble(3, d.getMontant());
            ps.setDate(4, d.getDate() != null ? Date.valueOf(d.getDate()) : null);
            ps.executeUpdate();
        }
    }

    public List<Depense> readByUser(int userId) throws SQLException {
        String sql = "SELECT idDepense, type, montant, date FROM depense WHERE userId = ?";
        List<Depense> list = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Depense d = new Depense();
                    d.setIdDepense(rs.getInt("idDepense"));
                    d.setUserId(userId);
                    String t = null;
                    try { t = rs.getString("type"); } catch (SQLException ignored) {}
                    if (t != null) {
                        try { d.setType(TypeDepense.valueOf(t)); } catch (IllegalArgumentException ignored) {}
                    }
                    d.setMontant(rs.getDouble("montant"));
                    Date date = rs.getDate("date");
                    if (date != null) d.setDate(date.toLocalDate());
                    list.add(d);
                }
            }
        }
        return list;
    }

    public void update(Depense d) throws SQLException {
        String sql = "UPDATE depense SET type = ?, montant = ?, date = ? WHERE idDepense = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, d.getType() != null ? d.getType().name() : null);
            ps.setDouble(2, d.getMontant());
            ps.setDate(3, d.getDate() != null ? Date.valueOf(d.getDate()) : null);
            ps.setInt(4, d.getIdDepense());
            ps.executeUpdate();
        }
    }

    public void delete(int idDepense) throws SQLException {
        String sql = "DELETE FROM depense WHERE idDepense = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, idDepense);
            ps.executeUpdate();
        }
    }
}
