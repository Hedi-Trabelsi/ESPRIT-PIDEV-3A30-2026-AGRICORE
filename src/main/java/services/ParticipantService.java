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

    /**
     * Creates a new participant record.
     * Fixed the "Field 'email' doesn't have a default value" error by adding the email column.
     */
    public int create(Participant p) throws SQLException {
        String query = "INSERT INTO participants (id_utilisateur, id_ev, date_inscription, " +
                "statut_participation, montant_payee, confirmation, nbr_places, " +
                "nom_participant, email, entry_code, nbr_presents, confirm_token) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        PreparedStatement ps = connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
        ps.setInt(1, p.getIdUtilisateur());
        ps.setInt(2, p.getIdEvennement());
        ps.setDate(3, Date.valueOf(p.getDateInscription()));
        ps.setString(4, p.getStatutParticipation());
        ps.setString(5, p.getMontantPayee());
        ps.setString(6, p.getConfirmation());
        ps.setInt(7, p.getNbrPlaces());
        ps.setString(8, p.getNomParticipant());
        ps.setString(9, p.getEmail()); // Added email support
        ps.setString(10, p.getEntryCode());
        ps.setInt(11, p.getNbrPresents());
        ps.setString(12, p.getConfirmToken());

        ps.executeUpdate();

        ResultSet rs = ps.getGeneratedKeys();
        if (rs.next()) {
            return rs.getInt(1);
        }
        return 0;
    }

    /**
     * Updates an existing participant.
     * Includes support for the dynamic presence tracking (nbr_presents and confirm_token).
     */
    public void update(Participant p) throws SQLException {
        String req = "UPDATE participants SET "
                + "id_utilisateur = ?, "
                + "id_ev = ?, "
                + "date_inscription = ?, "
                + "statut_participation = ?, "
                + "montant_payee = ?, "
                + "confirmation = ?, "
                + "nbr_places = ?, "
                + "nom_participant = ?, "
                + "email = ?, "
                + "entry_code = ?, "
                + "nbr_presents = ?, "
                + "confirm_token = ? "
                + "WHERE id_participant = ?";

        PreparedStatement ps = connection.prepareStatement(req);
        ps.setInt(1, p.getIdUtilisateur());
        ps.setInt(2, p.getIdEvennement());
        ps.setDate(3, java.sql.Date.valueOf(p.getDateInscription()));
        ps.setString(4, p.getStatutParticipation());
        ps.setString(5, p.getMontantPayee());
        ps.setString(6, p.getConfirmation());
        ps.setInt(7, p.getNbrPlaces());
        ps.setString(8, p.getNomParticipant());
        ps.setString(9, p.getEmail());
        ps.setString(10, p.getEntryCode());
        ps.setInt(11, p.getNbrPresents());
        ps.setString(12, p.getConfirmToken());
        ps.setInt(13, p.getIdParticipant());

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
                    rs.getString("email"),
                    rs.getString("entry_code"),
                    rs.getInt("nbr_presents"),
                    rs.getString("confirm_token")
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

    public String getUserEmail(int userId) throws SQLException {
        String query = "SELECT email FROM utilisateurs WHERE id = ?";
        PreparedStatement ps = connection.prepareStatement(query);
        ps.setInt(1, userId);
        ResultSet rs = ps.executeQuery();
        if (rs.next()) {
            return rs.getString("email");
        }
        return "";
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

    public List<String> getParticipantNamesForEvent(int eventId) throws SQLException {
        List<String> names = new ArrayList<>();
        String query = "SELECT u.nom, u.prenom FROM participants p " +
                "JOIN utilisateurs u ON p.id_utilisateur = u.id " +
                "WHERE p.id_ev = ?";
        PreparedStatement ps = connection.prepareStatement(query);
        ps.setInt(1, eventId);
        ResultSet rs = ps.executeQuery();
        while (rs.next()) {
            names.add(rs.getString("prenom") + " " + rs.getString("nom"));
        }
        return names;
    }

    public String getAdminName() throws SQLException {
        String query = "SELECT nom, prenom FROM utilisateurs WHERE role = 0 LIMIT 1";
        Statement st = connection.createStatement();
        ResultSet rs = st.executeQuery(query);
        if (rs.next()) {
            return rs.getString("prenom") + " " + rs.getString("nom");
        }
        return "Administrateur";
    }
}