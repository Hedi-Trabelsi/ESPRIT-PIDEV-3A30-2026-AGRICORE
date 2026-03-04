package services;

import Model.Equipement;
import utils.MyDatabase;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class EquipementService {

    Connection connection;

    public EquipementService() throws SQLException {
        connection = MyDatabase.getInstance().getConnection();
    }

    public void ajouter(Equipement e) throws SQLException {
        String sql = "INSERT INTO equipements (nom,type,prix,quantite,id_fournisseur) VALUES (?,?,?,?,?)";
        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setString(1, e.getNom());
        ps.setString(2, e.getType());
        ps.setString(3, e.getPrix());
        ps.setInt(4, e.getQuantite());
        ps.setInt(5, e.getId_fournisseur());
        ps.executeUpdate();
    }

    public List<Equipement> afficher() throws SQLException {
        List<Equipement> list = new ArrayList<>();
        String sql = "SELECT * FROM equipements";
        ResultSet rs = connection.createStatement().executeQuery(sql);

        while (rs.next()) {
            list.add(new Equipement(
                    rs.getInt("id_equipement"),
                    rs.getString("nom"),
                    rs.getString("type"),
                    rs.getString("prix"),
                    rs.getInt("quantite"),
                    rs.getInt("id_fournisseur")
            ));
        }
        return list;
    }

    public void modifier(Equipement e) throws SQLException {
        String sql = "UPDATE equipements SET nom=?, type=?, prix=?, quantite=?, id_fournisseur=? WHERE id_equipement=?";
        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setString(1, e.getNom());
        ps.setString(2, e.getType());
        ps.setString(3, e.getPrix());
        ps.setInt(4, e.getQuantite());
        ps.setInt(5, e.getId_fournisseur());
        ps.setInt(6, e.getId_equipement());
        ps.executeUpdate();
    }

    public void supprimer(int id) throws SQLException {
        String sql = "DELETE FROM equipements WHERE id_equipement=?";
        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setInt(1, id);
        ps.executeUpdate();
    }
}
