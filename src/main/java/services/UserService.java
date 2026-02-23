package services;

import Model.Utilisateur;
import utils.MyDatabase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UserService implements IService<Utilisateur> {

    private final Connection connection;

    public UserService() throws SQLException {
        connection = MyDatabase.getInstance().getConnection();
    }

    @Override
    public int create(Utilisateur u) throws SQLException {
        // Updated query to match the exact column order from your table
        String query = "INSERT INTO `user` (nom, prenom, date, adresse, role, numeroT, email, image, password, genre) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement ps = connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, u.getNom());           // nom
            ps.setString(2, u.getPrenom());        // prenom
            ps.setDate(3, Date.valueOf(u.getDateNaissance())); // date
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
        // Updated query to match the exact column order
        String query = "UPDATE `user` SET nom=?, prenom=?, date=?, adresse=?, role=?, numeroT=?, email=?, image=?, password=?, genre=? WHERE id=?";

        try (PreparedStatement ps = connection.prepareStatement(query)) {

            ps.setString(1, u.getNom());            // nom
            ps.setString(2, u.getPrenom());         // prenom
            ps.setDate(3, Date.valueOf(u.getDateNaissance()));  // date
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
            ps.setInt(11, u.getId());                 // id (WHERE clause)

            ps.executeUpdate();
        }
    }

    @Override
    public boolean delete(int id) throws SQLException {
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
                Utilisateur u = new Utilisateur(
                        rs.getString("nom"),
                        rs.getString("prenom"),
                        rs.getDate("date").toLocalDate(),
                        rs.getString("genre"),
                        rs.getString("adresse"),
                        rs.getInt("numeroT"),
                        rs.getInt("role"),
                        rs.getString("email"),
                        rs.getString("password"),
                        rs.getBytes("image")
                );
                u.setId(rs.getInt("id"));
                list.add(u);
            }
        }

        return list;
    }

    // Optional: Add a method to find user by email
    @Override
    public Utilisateur findByEmail(String email) throws SQLException {
        String query = "SELECT * FROM `user` WHERE email = ?";
        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Utilisateur u = new Utilisateur(
                            rs.getString("nom"),
                            rs.getString("prenom"),
                            rs.getDate("date").toLocalDate(),
                            rs.getString("genre"),
                            rs.getString("adresse"),
                            rs.getInt("numeroT"),
                            rs.getInt("role"),
                            rs.getString("email"),
                            rs.getString("password"),
                            rs.getBytes("image")
                    );
                    u.setId(rs.getInt("id"));
                    return u;
                }
            }
        }
        return null;
    }
}