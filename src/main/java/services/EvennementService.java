package services;

import models.EvennementAgricole;
import utils.MyDatabase;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class EvennementService implements IService<EvennementAgricole> {

    private Connection connection;

    public EvennementService() {
        connection = MyDatabase.getInstance().getConnection();
    }

    @Override
    public int create(EvennementAgricole e) throws SQLException {
        // La requête reste la même, mais les types de données envoyés changent
        String query = "INSERT INTO evennementagricole (titre, description, date_debut, date_fin, lieu, capacite_max, frais_inscription, statut) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        PreparedStatement ps = connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);

        ps.setString(1, e.getTitre());
        ps.setString(2, e.getDescription());

        // Utilisation de setTimestamp pour inclure Date + Heure
        ps.setTimestamp(3, Timestamp.valueOf(e.getDateDebut()));
        ps.setTimestamp(4, Timestamp.valueOf(e.getDateFin()));

        ps.setString(5, e.getLieu());
        ps.setInt(6, e.getCapaciteMax());
        ps.setInt(7, e.getFraisInscription());
        ps.setString(8, e.getStatut());

        ps.executeUpdate();

        ResultSet rs = ps.getGeneratedKeys();
        if (rs.next()) {
            return rs.getInt(1); // Retourne l'ID généré
        }
        return 0;
    }

    @Override
    public void update(EvennementAgricole e) throws SQLException {
        String query = "UPDATE evennementagricole SET titre=?, description=?, date_debut=?, date_fin=?, lieu=?, capacite_max=?, frais_inscription=?, statut=? WHERE id_ev=?";
        PreparedStatement ps = connection.prepareStatement(query);

        ps.setString(1, e.getTitre());
        ps.setString(2, e.getDescription());

        // Mise à jour avec Timestamp (Heure incluse)
        ps.setTimestamp(3, Timestamp.valueOf(e.getDateDebut()));
        ps.setTimestamp(4, Timestamp.valueOf(e.getDateFin()));

        ps.setString(5, e.getLieu());
        ps.setInt(6, e.getCapaciteMax());
        ps.setInt(7, e.getFraisInscription());
        ps.setString(8, e.getStatut());
        ps.setInt(9, e.getIdEvennement());

        ps.executeUpdate();
    }

    @Override
    public void delete(int id) throws SQLException {
        String query = "DELETE FROM evennementagricole WHERE id_ev=?";
        PreparedStatement ps = connection.prepareStatement(query);
        ps.setInt(1, id);
        ps.executeUpdate();
    }

    @Override
    public List<EvennementAgricole> read() throws SQLException {
        List<EvennementAgricole> list = new ArrayList<>();
        String query = "SELECT * FROM evennementagricole";
        Statement st = connection.createStatement();
        ResultSet rs = st.executeQuery(query);

        while (rs.next()) {
            // Conversion SQL Timestamp -> Java LocalDateTime
            LocalDateTime debut = rs.getTimestamp("date_debut").toLocalDateTime();
            LocalDateTime fin = rs.getTimestamp("date_fin").toLocalDateTime();

            EvennementAgricole e = new EvennementAgricole(
                    rs.getInt("id_ev"),
                    rs.getString("titre"),
                    rs.getString("description"),
                    debut,
                    fin,
                    rs.getString("lieu"),
                    rs.getInt("capacite_max"),
                    rs.getInt("frais_inscription"),
                    rs.getString("statut")
            );
            list.add(e);
        }
        return list;
    }
}