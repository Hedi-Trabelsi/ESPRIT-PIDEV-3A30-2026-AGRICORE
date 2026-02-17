package services;

import models.Participant;
import utils.MyDatabase;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class ParticipantService implements IService<Participant> {

    private Connection connection;

    public ParticipantService() {
        connection = MyDatabase.getInstance().getConnection();
    }

    @Override
    public int create(Participant p) throws SQLException {
        String query = "INSERT INTO participants (id_utilisateur, id_ev, date_inscription, statut_participation, montant_payee, confirmation) VALUES (?, ?, ?, ?, ?, ?)";
        PreparedStatement ps = connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
        ps.setInt(1, p.getIdUtilisateur());
        ps.setInt(2, p.getIdEvennement()); // maps to id_ev in DB
        ps.setDate(3, Date.valueOf(p.getDateInscription()));
        ps.setString(4, p.getStatutParticipation());
        ps.setString(5, p.getMontantPayee());
        ps.setString(6, p.getConfirmation());
        ps.executeUpdate();

        ResultSet rs = ps.getGeneratedKeys();
        if (rs.next()) {
            return rs.getInt(1); // generated id_participant
        }
        return 0;
    }

    @Override
    public void update(Participant p) throws SQLException {
        String query = "UPDATE participants SET id_utilisateur=?, id_ev=?, date_inscription=?, statut_participation=?, montant_payee=?, confirmation=? WHERE id_participant=?";
        PreparedStatement ps = connection.prepareStatement(query);
        ps.setInt(1, p.getIdUtilisateur());
        ps.setInt(2, p.getIdEvennement());
        ps.setDate(3, Date.valueOf(p.getDateInscription()));
        ps.setString(4, p.getStatutParticipation());
        ps.setString(5, p.getMontantPayee());
        ps.setString(6, p.getConfirmation());
        ps.setInt(7, p.getIdParticipant());
        ps.executeUpdate();
    }

    @Override
    public void delete(int id) throws SQLException {
        String query = "DELETE FROM participants WHERE id_participant=?";
        PreparedStatement ps = connection.prepareStatement(query);
        ps.setInt(1, id);
        ps.executeUpdate();
    }

    @Override
    public List<Participant> read() throws SQLException {
        List<Participant> list = new ArrayList<>();
        String query = "SELECT * FROM participants";
        Statement st = connection.createStatement();
        ResultSet rs = st.executeQuery(query);

        while (rs.next()) {
            Participant p = new Participant(
                    rs.getInt("id_participant"),
                    rs.getInt("id_utilisateur"),
                    rs.getInt("id_ev"),
                    rs.getDate("date_inscription").toLocalDate(),
                    rs.getString("statut_participation"),
                    rs.getString("montant_payee"),
                    rs.getString("confirmation")
            );
            list.add(p);
        }
        return list;
    }
}
