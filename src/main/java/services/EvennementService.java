package services;

import models.EvennementAgricole;
import utils.MyDatabase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class EvennementService implements IService<EvennementAgricole> {

    private final Connection connection;

    public EvennementService() {
        connection = MyDatabase.getInstance().getConnection();
    }

    @Override
    public int create(EvennementAgricole e) throws SQLException {
        String sql = "INSERT INTO evennementagricole " +
                "(titre, description, date_debut, date_fin, lieu, capacite_max, frais_inscription, statut) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
        ps.setString(1, e.getTitre());
        ps.setString(2, e.getDescription());
        ps.setDate(3, Date.valueOf(e.getDateDebut()));
        ps.setDate(4, Date.valueOf(e.getDateFin()));
        ps.setString(5, e.getLieu());
        ps.setInt(6, e.getCapaciteMax());
        ps.setInt(7, e.getFraisInscription());
        ps.setString(8, e.getStatut());

        ps.executeUpdate();
        ResultSet rs = ps.getGeneratedKeys();
        if (rs.next()) {
            return rs.getInt(1); // returns id_ev
        }
        return -1;
    }

    @Override
    public void update(EvennementAgricole e) throws SQLException {
        String sql = "UPDATE evennementagricole SET titre=?, description=?, date_debut=?, date_fin=?, " +
                "lieu=?, capacite_max=?, frais_inscription=?, statut=? WHERE id_ev=?";
        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setString(1, e.getTitre());
        ps.setString(2, e.getDescription());
        ps.setDate(3, Date.valueOf(e.getDateDebut()));
        ps.setDate(4, Date.valueOf(e.getDateFin()));
        ps.setString(5, e.getLieu());
        ps.setInt(6, e.getCapaciteMax());
        ps.setInt(7, e.getFraisInscription());
        ps.setString(8, e.getStatut());
        ps.setInt(9, e.getIdEvennement());

        ps.executeUpdate();
    }

    @Override
    public void delete(int id) throws SQLException {
        String sql = "DELETE FROM evennementagricole WHERE id_ev=?";
        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setInt(1, id);
        ps.executeUpdate();
    }

    @Override
    public List<EvennementAgricole> read() throws SQLException {
        String sql = "SELECT * FROM evennementagricole";
        Statement st = connection.createStatement();
        ResultSet rs = st.executeQuery(sql);

        List<EvennementAgricole> list = new ArrayList<>();
        while (rs.next()) {
            EvennementAgricole e = new EvennementAgricole();
            e.setIdEvennement(rs.getInt("id_ev"));
            e.setTitre(rs.getString("titre"));
            e.setDescription(rs.getString("description"));
            e.setDateDebut(rs.getDate("date_debut").toLocalDate());
            e.setDateFin(rs.getDate("date_fin").toLocalDate());
            e.setLieu(rs.getString("lieu"));
            e.setCapaciteMax(rs.getInt("capacite_max"));
            e.setFraisInscription(rs.getInt("frais_inscription"));
            e.setStatut(rs.getString("statut"));
            list.add(e);
        }
        return list;
    }
}
