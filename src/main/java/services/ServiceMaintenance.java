package services;

import Model.Maintenance;
import utils.MyDataBase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ServiceMaintenance implements IServiceMaintenance<Maintenance>{
    private Connection connection;

    public ServiceMaintenance() {
        connection = MyDataBase.getInstance().getMyConnection();
    }

    @Override
    public void ajouter(Maintenance maintenance) throws SQLException {
        maintenance.setStatut("En attente");
        // Ajout de nom_maintenance dans la requête
        String sql = "INSERT INTO maintenance(nom_maintenance, type, date_declaration, description, statut, id_technicien, priorite, lieu, equipement) VALUES (?, ?, ?, ?, ?, NULL, ?, ?, ?)";

        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setString(1, maintenance.getNom_maintenance());
        ps.setString(2, maintenance.getType());
        ps.setDate(3, Date.valueOf(maintenance.getDateDeclaration()));
        ps.setString(4, maintenance.getDescription());
        ps.setString(5, maintenance.getStatut());
        ps.setString(6, maintenance.getPriorite());
        ps.setString(7, maintenance.getLieu());
        ps.setString(8, maintenance.getEquipement());

        ps.executeUpdate();
    }

    @Override
    public void modifier(Maintenance maintenance) throws SQLException {
        // Ajout de nom_maintenance=? dans le SET
        String sql = "UPDATE maintenance SET nom_maintenance=?, type=?, date_declaration=?, description=?, statut=?, priorite=?, lieu=?, equipement=? WHERE id_maintenance=?";
        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setString(1, maintenance.getNom_maintenance());
        ps.setString(2, maintenance.getType());
        ps.setDate(3, Date.valueOf(maintenance.getDateDeclaration()));
        ps.setString(4, maintenance.getDescription());
        ps.setString(5, maintenance.getStatut());
        ps.setString(6, maintenance.getPriorite());
        ps.setString(7, maintenance.getLieu());
        ps.setString(8, maintenance.getEquipement());
        ps.setInt(9, maintenance.getId());

        ps.executeUpdate();
    }

    @Override
    public void supprimer(int id) throws SQLException {
        String sql = "DELETE FROM maintenance WHERE id_maintenance=?";
        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setInt(1, id);
        ps.executeUpdate();
    }

    @Override
    public List<Maintenance> afficher() throws SQLException {
        List<Maintenance> maintenances = new ArrayList<>();
        String sql = "SELECT * FROM maintenance";
        Statement statement = connection.createStatement();
        ResultSet rs = statement.executeQuery(sql);

        while (rs.next()) {
            // Utilisation du nouveau constructeur incluant nom_maintenance
            Maintenance m = new Maintenance(
                    rs.getInt("id_maintenance"),
                    rs.getString("nom_maintenance"), // Nouveau champ
                    rs.getString("type"),
                    rs.getDate("date_declaration").toLocalDate(),
                    rs.getString("description"),
                    rs.getString("statut"),
                    rs.getInt("id_technicien"),
                    rs.getString("priorite"),
                    rs.getString("lieu"),
                    rs.getString("equipement")
            );
            maintenances.add(m);
        }
        return maintenances;
    }

    public Maintenance getMaintenanceById(int id) throws SQLException {
        String sql = "SELECT * FROM maintenance WHERE id_maintenance=?";
        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setInt(1, id);
        ResultSet rs = ps.executeQuery();
        if (rs.next()) {
            return new Maintenance(
                    rs.getInt("id_maintenance"),
                    rs.getString("nom_maintenance"), // Nouveau champ
                    rs.getString("type"),
                    rs.getDate("date_declaration").toLocalDate(),
                    rs.getString("description"),
                    rs.getString("statut"),
                    rs.getInt("id_technicien"),
                    rs.getString("priorite"),
                    rs.getString("lieu"),
                    rs.getString("equipement")
            );
        }
        return null;
    }
}