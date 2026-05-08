package services;

import Model.Equipement;
import utils.MyDatabase;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class EquipementService {

    Connection connection;

    public EquipementService() throws SQLException {
        connection = MyDatabase.getInstance().getConnection();
    }

    public int ajouter(Equipement e) throws SQLException {
        String sql = "INSERT INTO equipements (nom, type, prix, quantite, id_fournisseur, image_filename, is_active, updated_at, image) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, e.getNom());
            ps.setString(2, e.getType());
            ps.setString(3, e.getPrix());
            ps.setInt(4, e.getQuantite());
            ps.setInt(5, e.getId_fournisseur());
            if (e.getImageFilename() != null) ps.setString(6, e.getImageFilename());
            else ps.setNull(6, Types.VARCHAR);
            ps.setBoolean(7, e.isActive());
            LocalDateTime now = e.getUpdatedAt() != null ? e.getUpdatedAt() : LocalDateTime.now();
            ps.setTimestamp(8, Timestamp.valueOf(now));
            if (e.getImage() != null) ps.setBytes(9, e.getImage());
            else ps.setNull(9, Types.BLOB);

            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    int id = keys.getInt(1);
                    e.setId_equipement(id);
                    return id;
                }
            }
        }
        return 0;
    }

    public List<Equipement> afficher() throws SQLException {
        return query("SELECT * FROM equipements WHERE is_active = 1");
    }

    public List<Equipement> afficherTous() throws SQLException {
        return query("SELECT * FROM equipements");
    }

    private List<Equipement> query(String sql) throws SQLException {
        List<Equipement> list = new ArrayList<>();
        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) list.add(mapRow(rs));
        }
        return list;
    }

    public Equipement findById(int id) throws SQLException {
        String sql = "SELECT * FROM equipements WHERE id_equipement = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapRow(rs);
            }
        }
        return null;
    }

    public void modifier(Equipement e) throws SQLException {
        String sql = "UPDATE equipements SET nom=?, type=?, prix=?, quantite=?, id_fournisseur=?, image_filename=?, is_active=?, updated_at=?, image=? WHERE id_equipement=?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, e.getNom());
            ps.setString(2, e.getType());
            ps.setString(3, e.getPrix());
            ps.setInt(4, e.getQuantite());
            ps.setInt(5, e.getId_fournisseur());
            if (e.getImageFilename() != null) ps.setString(6, e.getImageFilename());
            else ps.setNull(6, Types.VARCHAR);
            ps.setBoolean(7, e.isActive());
            LocalDateTime now = e.getUpdatedAt() != null ? e.getUpdatedAt() : LocalDateTime.now();
            ps.setTimestamp(8, Timestamp.valueOf(now));
            if (e.getImage() != null) ps.setBytes(9, e.getImage());
            else ps.setNull(9, Types.BLOB);
            ps.setInt(10, e.getId_equipement());
            ps.executeUpdate();
        }
    }

    /** Soft-delete: marks the equipment inactive (mirrors Symfony's BackEquipementController::delete). */
    public void supprimer(int id) throws SQLException {
        String sql = "UPDATE equipements SET is_active = 0, updated_at = ? WHERE id_equipement = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now()));
            ps.setInt(2, id);
            ps.executeUpdate();
        }
    }

    /** Hard delete — kept for admin tools that explicitly need it. */
    public void supprimerDefinitivement(int id) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement("DELETE FROM equipements WHERE id_equipement = ?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    public void decrementerStock(int idEquipement, int quantite) throws SQLException {
        String sql = "UPDATE equipements SET quantite = GREATEST(quantite - ?, 0), updated_at = ? WHERE id_equipement = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, quantite);
            ps.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now()));
            ps.setInt(3, idEquipement);
            ps.executeUpdate();
        }
    }

    private static Equipement mapRow(ResultSet rs) throws SQLException {
        Equipement e = new Equipement(
                rs.getInt("id_equipement"),
                rs.getString("nom"),
                rs.getString("type"),
                rs.getString("prix"),
                rs.getInt("quantite"),
                rs.getInt("id_fournisseur")
        );
        e.setImageFilename(rs.getString("image_filename"));
        e.setActive(rs.getBoolean("is_active"));
        Timestamp ts = rs.getTimestamp("updated_at");
        if (ts != null) e.setUpdatedAt(ts.toLocalDateTime());
        try {
            byte[] bytes = rs.getBytes("image");
            if (bytes != null) e.setImage(bytes);
        } catch (SQLException ignored) {
            // `image` column not present yet (migration not run) — fall back to filename only.
        }
        return e;
    }
}
