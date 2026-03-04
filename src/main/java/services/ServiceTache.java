package services;

import Model.Tache;
import utils.MyDatabase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ServiceTache {

    private Connection connection;

    public ServiceTache() throws SQLException {
        connection = MyDatabase.getInstance().getConnection();
    }

    // Ajouter une tâche avec nomTache et evaluation (par défaut 0)
    public void ajouter(Tache tache) throws SQLException {
        // La requête avec 7 colonnes
        String sql = "INSERT INTO tache(nomTache, date_prevue, description, cout_estimee, id_maintenance, evaluation, id_technicien) VALUES (?, ?, ?, ?, ?, ?, ?)";

        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setString(1, tache.getNomTache());
        ps.setString(2, tache.getDate_prevue());
        ps.setString(3, tache.getDesciption());
        ps.setInt(4, tache.getCout_estimee());
        ps.setInt(5, tache.getId_maintenace());
        ps.setInt(6, tache.getEvaluation()); // Utilise la valeur de l'objet (qui est 0 par défaut)
        ps.setInt(7, tache.getId_technicien()); // Ton ID récupéré du contrôleur

        ps.executeUpdate();
    }

    // Modifier une tâche (inclut evaluation au cas où)
    public void modifier(Tache tache) throws SQLException {
        String sql = "UPDATE tache SET nomTache=?, date_prevue=?, description=?, cout_estimee=?, id_maintenance=?, evaluation=? WHERE id_tache=?";
        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setString(1, tache.getNomTache());
        ps.setString(2, tache.getDate_prevue());
        ps.setString(3, tache.getDesciption());
        ps.setInt(4, tache.getCout_estimee());
        ps.setInt(5, tache.getId_maintenace());
        ps.setInt(6, tache.getEvaluation());
        ps.setInt(7, tache.getId_tache());

        ps.executeUpdate();
    }

    // --- NOUVELLE MÉTHODE : Mettre à jour uniquement le vote ---
    public void voterTache(int idTache, int valeurVote) throws SQLException {
        String sql = "UPDATE tache SET evaluation = ? WHERE id_tache = ?";
        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setInt(1, valeurVote);
        ps.setInt(2, idTache);
        ps.executeUpdate();
    }

    // Supprimer une tâche
    public void supprimer(int id) throws SQLException {
        String sql = "DELETE FROM tache WHERE id_tache=?";
        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setInt(1, id);
        ps.executeUpdate();
    }

    // Afficher toutes les tâches avec evaluation
    public List<Tache> afficher() throws SQLException {
        List<Tache> taches = new ArrayList<>();
        String sql = "SELECT * FROM tache";
        Statement statement = connection.createStatement();
        ResultSet rs = statement.executeQuery(sql);

        while (rs.next()) {
            Tache t = new Tache(
                    rs.getInt("id_tache"),
                    rs.getString("nomTache"),
                    rs.getString("date_prevue"),
                    rs.getString("description"),
                    rs.getInt("cout_estimee"),
                    rs.getInt("id_maintenance"),
                    rs.getInt("evaluation"),
                    rs.getInt("id_technicien") // <--- On ajoute la lecture ici
            );
            taches.add(t);
        }
        return taches;
    }

    // Lister les tâches d'une maintenance spécifique avec evaluation
    // 3. GET BY MAINTENANCE : On met aussi à jour ici
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
                    rs.getInt("id_maintenance"),
                    rs.getInt("evaluation"),
                    rs.getInt("id_technicien") // <--- On ajoute la lecture ici
            );
            taches.add(t);
        }
        return taches;
    }
}