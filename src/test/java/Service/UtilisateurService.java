package Service;

import Model.Utilisateur;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class UtilisateurService {

    private Connection connection;

    public UtilisateurService() {
        try {
            connection = DriverManager.getConnection(
                    "jdbc:mysql://localhost:3306/agricore?useSSL=false&serverTimezone=UTC",
                    "root",
                    ""
            );
            System.out.println("✅ Database connected successfully!");
        } catch (SQLException e) {
            System.out.println("❌ Database connection failed!");
            e.printStackTrace();
        }
    }

    public boolean isConnected() {
        try {
            return connection != null && !connection.isClosed();
        } catch (SQLException e) {
            return false;
        }
    }

    // ✅ ADD USER
    public void ajouter(Utilisateur u) throws SQLException {

        String sql = "INSERT INTO `user` (nom, prenom, date, adresse, numeroT, genre, role) VALUES (?, ?, ?, ?, ?, ?, ?)";

        PreparedStatement ps = connection.prepareStatement(sql);

        ps.setString(1, u.getNom());
        ps.setString(2, u.getPrenom());
        ps.setDate(3, Date.valueOf(u.getDateNaissance()));
        ps.setString(4, u.getAdresse());
        ps.setInt(5, u.getPhone());      // INT
        ps.setString(6, u.getGenre());
        ps.setInt(7, u.getRole());       // INT

        ps.executeUpdate();
    }

    // ✅ SHOW USERS
    public List<Utilisateur> afficher() throws SQLException {

        List<Utilisateur> list = new ArrayList<>();

        String sql = "SELECT * FROM `user`";

        Statement st = connection.createStatement();
        ResultSet rs = st.executeQuery(sql);

        while (rs.next()) {

            Utilisateur u = new Utilisateur();

            u.setId(rs.getInt("id"));
            u.setNom(rs.getString("nom"));
            u.setPrenom(rs.getString("prenom"));
            u.setDateNaissance(rs.getDate("date").toLocalDate());
            u.setAdresse(rs.getString("adresse"));
            u.setPhone(rs.getInt("numeroT"));
            u.setGenre(rs.getString("genre"));
            u.setRole(rs.getInt("role"));

            list.add(u);
        }

        return list;
    }

    // ✅ DELETE USER
    public void supprimer(int id) throws SQLException {

        String sql = "DELETE FROM `user` WHERE id = ?";

        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setInt(1, id);
        ps.executeUpdate();
    }
}
