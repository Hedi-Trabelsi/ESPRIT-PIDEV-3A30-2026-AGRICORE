package services;

import models.Participant;
import utils.MyDatabase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ParticipantService {

    private Connection connection;

    public ParticipantService() {
        connection = MyDatabase.getInstance().getConnection();
    }

    public int create(Participant p) throws SQLException {
        // Updated id_evennement -> id_ev to match your DB
        String query = "INSERT INTO participants (id_utilisateur, id_ev, date_inscription, statut_participation, montant_payee, confirmation, nbr_places, nom_participant) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        PreparedStatement ps = connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
        ps.setInt(1, p.getIdUtilisateur());
        ps.setInt(2, p.getIdEvennement());
        ps.setDate(3, Date.valueOf(p.getDateInscription()));
        ps.setString(4, p.getStatutParticipation());
        ps.setString(5, p.getMontantPayee());
        ps.setString(6, p.getConfirmation());
        ps.setInt(7, p.getNbrPlaces());
        ps.setString(8, p.getNomParticipant());
        ps.executeUpdate();

        ResultSet rs = ps.getGeneratedKeys();
        if (rs.next()) {
            return rs.getInt(1);
        }
        return 0;
    }

    public void delete(int id) throws SQLException {
        String query = "DELETE FROM participants WHERE id_participant=?";
        PreparedStatement ps = connection.prepareStatement(query);
        ps.setInt(1, id);
        ps.executeUpdate();
    }

    public List<Participant> read() throws SQLException {
        List<Participant> list = new ArrayList<>();
        String query = "SELECT * FROM participants";
        Statement st = connection.createStatement();
        ResultSet rs = st.executeQuery(query);

        while (rs.next()) {
            Participant p = new Participant(
                    rs.getInt("id_participant"),
                    rs.getInt("id_utilisateur"),
                    rs.getInt("id_ev"), // FIXED: Using id_ev as per your DB
                    rs.getDate("date_inscription").toLocalDate(),
                    rs.getString("statut_participation"),
                    rs.getString("montant_payee"),
                    rs.getString("confirmation"),
                    rs.getInt("nbr_places"),
                    rs.getString("nom_participant")
            );
            list.add(p);
        }
        return list;
    }

    public String getUserRealName(int userId) throws SQLException {
        // Using 'utilisateurs' and 'id' as confirmed by your DESCRIBE command
        String query = "SELECT nom, prenom FROM utilisateurs WHERE id = ?";
        PreparedStatement ps = connection.prepareStatement(query);
        ps.setInt(1, userId);
        ResultSet rs = ps.executeQuery();
        if (rs.next()) {
            return rs.getString("prenom") + " " + rs.getString("nom");
        }
        return "Utilisateur " + userId;
    }
}