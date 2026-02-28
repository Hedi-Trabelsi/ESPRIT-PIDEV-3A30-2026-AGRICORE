package services;

import models.User;
import utils.MyDatabase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UserService implements IService<User>{

    private Connection connection;

    public UserService(){
        connection = MyDatabase.getInstance().getConnection();
    }

    @Override
    public int create(User user) throws SQLException {
        String sql = "INSERT INTO user (nom, prenom, date, adresse, role, numeroT, email, image, password, genre) " +
                "VALUES (?,?,?,?,?,?,?,?,?,?)";
        PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
        ps.setString(1, user.getNom());
        ps.setString(2, user.getPrenom());
        ps.setDate(3, user.getDate() != null ? Date.valueOf(user.getDate()) : null);
        ps.setString(4, user.getAdresse());
        ps.setString(5, user.getRole());
        ps.setString(6, user.getNumeroT());
        ps.setString(7, user.getEmail());
        ps.setString(8, user.getImage());
        ps.setString(9, user.getPassword());
        ps.setString(10, user.getGenre());
        ps.executeUpdate();
        ResultSet rs = ps.getGeneratedKeys();
        if (rs.next()) {
            return rs.getInt(1);
        }
        return -1;
    }

    @Override
    public void update(User user) throws SQLException {
        String sql = "UPDATE user SET nom = ?, prenom = ?, date = ?, adresse = ?, role = ?, numeroT = ?, email = ?, image = ?, password = ?, genre = ? WHERE id = ?";
        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setString(1, user.getNom());
        ps.setString(2, user.getPrenom());
        ps.setDate(3, user.getDate() != null ? Date.valueOf(user.getDate()) : null);
        ps.setString(4, user.getAdresse());
        ps.setString(5, user.getRole());
        ps.setString(6, user.getNumeroT());
        ps.setString(7, user.getEmail());
        ps.setString(8, user.getImage());
        ps.setString(9, user.getPassword());
        ps.setString(10, user.getGenre());
        ps.setInt(11, user.getId());
        ps.executeUpdate();
    }

    @Override
    public void delete(int id) throws SQLException {
        String sql = "DELETE FROM user WHERE id = ?";
        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setInt(1, id);
        ps.executeUpdate();
    }

    @Override
    public List<User> read() throws SQLException {
        String sql = "SELECT * FROM user";
        Statement statement = connection.createStatement();
        ResultSet rs = statement.executeQuery(sql);
        List<User> people = new ArrayList<>();
        while (rs.next()){
            User p = new User();
            p.setId(rs.getInt("id"));
            try { p.setNom(rs.getString("nom")); } catch (SQLException ignored) {}
            try { p.setPrenom(rs.getString("prenom")); } catch (SQLException ignored) {}
            try {
                Date d = rs.getDate("date");
                if (d != null) p.setDate(d.toLocalDate());
            } catch (SQLException ignored) {}
            try { p.setAdresse(rs.getString("adresse")); } catch (SQLException ignored) {}
            try { p.setRole(rs.getString("role")); } catch (SQLException ignored) {}
            try { p.setNumeroT(rs.getString("numeroT")); } catch (SQLException ignored) {}
            try { p.setEmail(rs.getString("email")); } catch (SQLException ignored) {}
            try { p.setImage(rs.getString("image")); } catch (SQLException ignored) {}
            try { p.setPassword(rs.getString("password")); } catch (SQLException ignored) {}
            try { p.setGenre(rs.getString("genre")); } catch (SQLException ignored) {}
            people.add(p);
        }
        return people;
    }
}
