package services;

import entities.Panier;
import utils.MyDatabase;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class PanierService {

    Connection connection;

    public PanierService() {
        connection = MyDatabase.getInstance().getConnection();
    }

    public void ajouter(Panier p) throws SQLException {
        String sql = "INSERT INTO panier (id_equipement,quantite,total,id_agriculteur) VALUES (?,?,?,?)";
        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setInt(1, p.getId_equipement());
        ps.setInt(2, p.getQuantite());
        ps.setString(3, p.getTotal());
        ps.setInt(4, p.getId_agriculteur());
        ps.executeUpdate();
    }

    public List<Panier> afficher() throws SQLException {
        List<Panier> list = new ArrayList<>();
        ResultSet rs = connection.createStatement().executeQuery("SELECT * FROM panier");

        while (rs.next()) {
            Panier p = new Panier();
            p.setId_panier(rs.getInt("id_panier"));
            p.setId_equipement(rs.getInt("id_equipement"));
            p.setQuantite(rs.getInt("quantite"));
            p.setTotal(rs.getString("total"));
            p.setId_agriculteur(rs.getInt("id_agriculteur"));
            list.add(p);
        }
        return list;
    }

    public void supprimer(int id) throws SQLException {
        PreparedStatement ps = connection.prepareStatement("DELETE FROM panier WHERE id_panier=?");
        ps.setInt(1, id);
        ps.executeUpdate();
    }
}
