package services;

import Model.Tache;
import utils.MyDataBase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ServiceTache {

    private Connection connection;

    public ServiceTache() {
        connection = MyDataBase.getInstance().getMyConnection();
    }

    // Ajouter une tâche avec nomTache
    public void ajouter(Tache tache) throws SQLException {
        String sql = "INSERT INTO tache(nomTache, date_prevue, description, cout_estimee, id_maintenance) VALUES (?, ?, ?, ?, ?)";
        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setString(1, tache.getNomTache());
        ps.setString(2, tache.getDate_prevue());
        ps.setString(3, tache.getDesciption());
        ps.setInt(4, tache.getCout_estimee());
        ps.setInt(5, tache.getId_maintenace());

        ps.executeUpdate();
    }

    // Modifier une tâche avec nomTache
    public void modifier(Tache tache) throws SQLException {
        String sql = "UPDATE tache SET nomTache=?, date_prevue=?, description=?, cout_estimee=?, id_maintenance=? WHERE id_tache=?";
        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setString(1, tache.getNomTache());
        ps.setString(2, tache.getDate_prevue());
        ps.setString(3, tache.getDesciption());
        ps.setInt(4, tache.getCout_estimee());
        ps.setInt(5, tache.getId_maintenace());
        ps.setInt(6, tache.getId_tache());

        ps.executeUpdate();
    }

    // Supprimer une tâche
    public void supprimer(int id) throws SQLException {
        String sql = "DELETE FROM tache WHERE id_tache=?";
        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setInt(1, id);
        ps.executeUpdate();
    }

    // Afficher toutes les tâches avec nomTache
    public List<Tache> afficher() throws SQLException {
        List<Tache> taches = new ArrayList<>();
        String sql = "SELECT * FROM tache";
        Statement statement = connection.createStatement();
        ResultSet rs = statement.executeQuery(sql);

        while (rs.next()) {
            Tache t = new Tache(
                    rs.getInt("id_tache"),
                    rs.getString("nomTache"), // Lecture du nouveau champ
                    rs.getString("date_prevue"),
                    rs.getString("description"),
                    rs.getInt("cout_estimee"),
                    rs.getInt("id_maintenance")
            );
            taches.add(t);
        }

        return taches;
    }

    // Lister les tâches d'une maintenance spécifique (Version corrigée)
    public List<Tache> getTachesByMaintenance(int idMaintenance) throws SQLException {
        List<Tache> taches = new ArrayList<>();
        String sql = "SELECT * FROM tache WHERE id_maintenance = ?";
        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setInt(1, idMaintenance);
        ResultSet rs = ps.executeQuery();

        while (rs.next()) {
            Tache t = new Tache(
                    rs.getInt("id_tache"),
                    rs.getString("nomTache"),
                    rs.getString("date_prevue"),
                    rs.getString("description"),
                    rs.getInt("cout_estimee"),
                    rs.getInt("id_maintenance")
            );
            taches.add(t);
        }
        return taches;
    }
}