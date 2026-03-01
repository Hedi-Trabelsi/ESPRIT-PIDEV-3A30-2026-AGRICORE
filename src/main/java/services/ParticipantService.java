package services;

import Model.Participant;
import utils.MyDatabase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ParticipantService {

    private Connection connection;

    public ParticipantService() {
        try {
            connection = MyDatabase.getInstance().getConnection();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public int create(Participant p) throws SQLException {
        String query = "INSERT INTO participants (id_utilisateur, id_ev, date_inscription, statut_participation, montant_payee, confirmation, nbr_places, nom_participant, entry_code) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        PreparedStatement ps = connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
        ps.setInt(1, p.getIdUtilisateur());
        ps.setInt(2, p.getIdEvennement());
        ps.setDate(3, Date.valueOf(p.getDateInscription()));
        ps.setString(4, p.getStatutParticipation());
        ps.setString(5, p.getMontantPayee());
        ps.setString(6, p.getConfirmation());
        ps.setInt(7, p.getNbrPlaces());
        ps.setString(8, p.getNomParticipant());
        ps.setString(9, p.getEntryCode());
        ps.executeUpdate();

        ResultSet rs = ps.getGeneratedKeys();
        if (rs.next()) {
            return rs.getInt(1);
        }
        return 0;
    }

    /**
     * NOUVELLE MÉTHODE : Met à jour toutes les informations d'un participant
     * Utilisée pour confirmer la présence (statut_participation)
     */
    public void update(Participant p) throws SQLException {
        String query = "UPDATE participants SET statut_participation = ?, montant_payee = ?, confirmation = ?, nbr_places = ?, entry_code = ? WHERE id_participant = ?";
        PreparedStatement ps = connection.prepareStatement(query);
        ps.setString(1, p.getStatutParticipation());
        ps.setString(2, p.getMontantPayee());
        ps.setString(3, p.getConfirmation());
        ps.setInt(4, p.getNbrPlaces());
        ps.setString(5, p.getEntryCode());
        ps.setInt(6, p.getIdParticipant());
        ps.executeUpdate();
    }

    public void updateEntryCode(int userId, int eventId, String code) throws SQLException {
        String query = "UPDATE participants SET entry_code = ? WHERE id_utilisateur = ? AND id_ev = ?";
        PreparedStatement ps = connection.prepareStatement(query);
        ps.setString(1, code);
        ps.setInt(2, userId);
        ps.setInt(3, eventId);
        ps.executeUpdate();
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
                    rs.getInt("id_ev"),
                    rs.getDate("date_inscription").toLocalDate(),
                    rs.getString("statut_participation"),
                    rs.getString("montant_payee"),
                    rs.getString("confirmation"),
                    rs.getInt("nbr_places"),
                    rs.getString("nom_participant"),
                    rs.getString("entry_code")
            );
            list.add(p);
        }
        return list;
    }

    public String getUserRealName(int userId) throws SQLException {
        String query = "SELECT nom, prenom FROM utilisateurs WHERE id = ?";
        PreparedStatement ps = connection.prepareStatement(query);
        ps.setInt(1, userId);
        ResultSet rs = ps.executeQuery();
        if (rs.next()) {
            return rs.getString("prenom") + " " + rs.getString("nom");
        }
        return "Utilisateur " + userId;
    }

    public int getReservedCount(int idEvennement) throws SQLException {
        int totalReserved = 0;
        String query = "SELECT SUM(nbr_places) FROM participants WHERE id_ev = ?";
        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setInt(1, idEvennement);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    totalReserved = rs.getInt(1);
                }
            }
        }
        return totalReserved;
    }
}