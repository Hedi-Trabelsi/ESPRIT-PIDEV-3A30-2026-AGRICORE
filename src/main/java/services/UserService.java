package services;

import Model.Utilisateur;
import utils.MyDatabase;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class UserService implements IService<Utilisateur> {

    private final Connection connection;

    public UserService() throws SQLException {
        connection = MyDatabase.getInstance().getConnection();
    }

    @Override
    public int create(Utilisateur u) throws SQLException {
        u.setProfileComplete(computeProfileComplete(u));

        String query = "INSERT INTO `user` (nom, prenom, date, adresse, role, numeroT, email, image, password, genre, profile_complete, banned) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement ps = connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, u.getNom());           // nom
            ps.setString(2, u.getPrenom());        // prenom

            // FIX: Handle null date
            LocalDate dateNaissance = u.getDateNaissance();
            if (dateNaissance != null) {
                ps.setDate(3, Date.valueOf(dateNaissance));
            } else {
                ps.setNull(3, Types.DATE); // Set NULL for date column
            }

            ps.setString(4, u.getAdresse());       // adresse
            ps.setInt(5, u.getRole());              // role
            ps.setInt(6, u.getPhone());             // numeroT
            ps.setString(7, u.getEmail());          // email

            // Handle image (column 8)
            if (u.getImage() != null) {
                ps.setBytes(8, u.getImage());
            } else {
                ps.setNull(8, Types.BLOB);
            }

            ps.setString(9, u.getPassword());       // password
            ps.setString(10, u.getGenre());         // genre
            ps.setBoolean(11, u.isProfileComplete());
            ps.setBoolean(12, u.isBanned());

            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }

        return 0;
    }

    @Override
    public void update(Utilisateur u) throws SQLException {
        u.setProfileComplete(computeProfileComplete(u));

        String query = "UPDATE `user` SET nom=?, prenom=?, date=?, adresse=?, role=?, numeroT=?, email=?, image=?, password=?, genre=?, profile_complete=?, banned=? WHERE id=?";

        try (PreparedStatement ps = connection.prepareStatement(query)) {

            ps.setString(1, u.getNom());            // nom
            ps.setString(2, u.getPrenom());         // prenom

            // FIX: Handle null date in update too
            LocalDate dateNaissance = u.getDateNaissance();
            if (dateNaissance != null) {
                ps.setDate(3, Date.valueOf(dateNaissance));
            } else {
                ps.setNull(3, Types.DATE);
            }

            ps.setString(4, u.getAdresse());        // adresse
            ps.setInt(5, u.getRole());               // role
            ps.setInt(6, u.getPhone());              // numeroT
            ps.setString(7, u.getEmail());           // email

            // Handle image (column 8)
            if (u.getImage() != null) {
                ps.setBytes(8, u.getImage());
            } else {
                ps.setNull(8, Types.BLOB);
            }

            ps.setString(9, u.getPassword());        // password
            ps.setString(10, u.getGenre());          // genre
            ps.setBoolean(11, u.isProfileComplete());
            ps.setBoolean(12, u.isBanned());
            ps.setInt(13, u.getId());                 // id (WHERE clause)

            ps.executeUpdate();
        }
    }

    @Override
    public boolean delete(int id) throws SQLException {
        // Delete child records first to avoid FK constraint errors
        try (PreparedStatement ps = connection.prepareStatement("DELETE FROM vente WHERE userId=?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
        try (PreparedStatement ps = connection.prepareStatement("DELETE FROM depense WHERE userId=?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
        String query = "DELETE FROM `user` WHERE id=?";
        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setInt(1, id);
            int rowsAffected = ps.executeUpdate();
            return rowsAffected > 0;
        }
    }

    @Override
    public List<Utilisateur> read() throws SQLException {
        List<Utilisateur> list = new ArrayList<>();
        String query = "SELECT * FROM `user`";

        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery(query)) {

            while (rs.next()) {
                list.add(mapRow(rs));
            }
        }

        return list;
    }

    @Override
    public Utilisateur findByEmail(String email) throws SQLException {
        String query = "SELECT * FROM `user` WHERE email = ?";
        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapRow(rs);
            }
        }
        return null;
    }

    public Utilisateur findById(int id) throws SQLException {
        String query = "SELECT * FROM `user` WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapRow(rs);
            }
        }
        return null;
    }

    private static Utilisateur mapRow(ResultSet rs) throws SQLException {
        Date sqlDate = rs.getDate("date");
        LocalDate dateNaissance = sqlDate != null ? sqlDate.toLocalDate() : null;

        Utilisateur u = new Utilisateur(
                rs.getString("nom"),
                rs.getString("prenom"),
                dateNaissance,
                rs.getString("genre"),
                rs.getString("adresse"),
                rs.getInt("numeroT"),
                rs.getInt("role"),
                rs.getString("email"),
                rs.getString("password"),
                rs.getBytes("image")
        );
        u.setId(rs.getInt("id"));

        // profile_complete and banned were added to the schema by the Symfony side;
        // getBoolean returns false when the column is NULL, which is the correct default.
        u.setProfileComplete(rs.getBoolean("profile_complete"));
        u.setBanned(rs.getBoolean("banned"));
        return u;
    }

    private static boolean computeProfileComplete(Utilisateur u) {
        return u.getDateNaissance() != null
                && u.getGenre() != null && !u.getGenre().isBlank()
                && u.getAdresse() != null && !u.getAdresse().isBlank()
                && u.getPhone() > 0;
    }
}