package services;

import models.Participant;
import utils.MyDatabase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ParticipantService implements IService<Participant> {

    private final Connection connection;

    public ParticipantService() {
        connection = MyDatabase.getInstance().getConnection();
    }

    @Override
    public int create(Participant p) throws SQLException {
        String sql = "INSERT INTO participants " +
                "(id_utilisateur, id_ev, date_inscription, statut_participation, montant_payee, confirmation) " +
                "VALUES (?, ?, ?, ?, ?, ?)";
        PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
        ps.setInt(1, p.getIdUtilisateur());
        ps.setInt(2, p.getIdEvennement());
        ps.setDate(3, Date.valueOf(p.getDateInscription()));
        ps.setString(4, p.getStatutParticipation());
        ps.setString(5, p.getMontantPayee());
        ps.setString(6, p.getConfirmation());

        ps.executeUpdate();
        ResultSet rs = ps.getGeneratedKeys();
        if (rs.next()) {
            return rs.getInt(1);
        }
        return -1;
    }

    @Override
    public void update(Participant p) throws SQLException {
        String sql = "UPDATE participants SET id_utilisateur=?, id_ev=?, date_inscription=?, " +
                "statut_participation=?, montant_payee=?, confirmation=? WHERE id_participant=?";
        PreparedStatement ps = connection.prepareStatement(sql);
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
        String sql = "DELETE FROM participants WHERE id_participant=?";
        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setInt(1, id);
        ps.executeUpdate();
    }

    @Override
    public List<Participant> read() throws SQLException {
        String sql = "SELECT * FROM participants";
        Statement st = connection.createStatement();
        ResultSet rs = st.executeQuery(sql);

        List<Participant> list = new ArrayList<>();
        while (rs.next()) {
            Participant p = new Participant();
            p.setIdParticipant(rs.getInt("id_participant"));
            p.setIdUtilisateur(rs.getInt("id_utilisateur"));
            p.setIdEvennement(rs.getInt("id_ev"));
            p.setDateInscription(rs.getDate("date_inscription").toLocalDate());
            p.setStatutParticipation(rs.getString("statut_participation"));
            p.setMontantPayee(rs.getString("montant_payee"));
            p.setConfirmation(rs.getString("confirmation"));
            list.add(p);
        }
        return list;
    }
}
