package services;

import models.Tache;
import utils.MyDataBase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ServiceTache {

    private Connection connection;

    public ServiceTache() {
        connection = MyDataBase.getInstance().getMyConnection();
    }

    // Ajouter une tâche
    public void ajouter(Tache tache) throws SQLException {
        String sql = "INSERT INTO tache(date_prevue, description, cout_estimee, id_maintenance) VALUES (?, ?, ?, ?)";
        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setString(1, tache.getDate_prevue());
        ps.setString(2, tache.getDesciption());
        ps.setInt(3, tache.getCout_estimee());
        ps.setInt(4, tache.getId_maintenace());

        ps.executeUpdate();
    }

    // Modifier une tâche
    public void modifier(Tache tache) throws SQLException {
        String sql = "UPDATE tache SET date_prevue=?, description=?, cout_estimee=?, id_maintenance=? WHERE id_tache=?";
        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setString(1, tache.getDate_prevue());
        ps.setString(2, tache.getDesciption());
        ps.setInt(3, tache.getCout_estimee());
        ps.setInt(4, tache.getId_maintenace());
        ps.setInt(5, tache.getId_tache());

        ps.executeUpdate();
    }

    // Supprimer une tâche
    public void supprimer(int id) throws SQLException {
        String sql = "DELETE FROM tache WHERE id_tache=?";
        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setInt(1, id);
        ps.executeUpdate();
    }

    // Afficher toutes les tâches
    public List<Tache> afficher() throws SQLException {
        List<Tache> taches = new ArrayList<>();
        String sql = "SELECT * FROM tache";
        Statement statement = connection.createStatement();
        ResultSet rs = statement.executeQuery(sql);

        while (rs.next()) {
            Tache t = new Tache(
                    rs.getInt("id_tache"),
                    rs.getString("date_prevue"),
                    rs.getString("description"),
                    rs.getInt("cout_estimee"),
                    rs.getInt("id_maintenance")
            );
            taches.add(t);
        }

        return taches;
    }

    // Lister les tâches d'une maintenance spécifique
    public List<Tache> afficherParMaintenance(int idMaintenance) throws SQLException {
        List<Tache> taches = new ArrayList<>();
        String sql = "SELECT * FROM tache WHERE id_maintenance=?";
        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setInt(1, idMaintenance);
        ResultSet rs = ps.executeQuery();

        while (rs.next()) {
            Tache t = new Tache(
                    rs.getInt("id_tache"),
                    rs.getString("date_prevue"),
                    rs.getString("description"),
                    rs.getInt("cout_estimee"),
                    rs.getInt("id_maintenance")
            );
            taches.add(t);
        }

        return taches;

    }
    // Lister les tâches d'une maintenance spécifique
    public List<Tache> getTachesByMaintenance(int idMaintenance) throws SQLException {
        List<Tache> taches = new ArrayList<>();
        String sql = "SELECT * FROM tache WHERE id_maintenace = ?";
        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setInt(1, idMaintenance);
        ResultSet rs = ps.executeQuery();

        while (rs.next()) {
            Tache t = new Tache(
                    rs.getInt("id_tache"),
                    rs.getString("date_prevue"),
                    rs.getString("desciption"),
                    rs.getInt("cout_estimee"),
                    rs.getInt("id_maintenace")
            );
            taches.add(t);
        }
        return taches;
    }

}
