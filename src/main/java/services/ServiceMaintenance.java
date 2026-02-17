package services;

import models.Maintenance;
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
        String sql = "INSERT INTO maintenance(type,date_declaration, description, statut,id_technicien,priorite,lieu,equipement) VALUES ('"
                + maintenance.getType() + "', '"
                + maintenance.getDateDeclaration() + "', '"
                + maintenance.getDescription() + "', '"
                + maintenance.getStatut() + "', NULL, '"
                + maintenance.getPriorite() + "', '"
                + maintenance.getLieu() + "', '"
                + maintenance.getEquipement() + "')";

        Statement statement = connection.createStatement();
        statement.executeUpdate(sql);
    }


    @Override
    public void modifier(Maintenance maintenance) throws SQLException {
        String sql = "UPDATE maintenance SET type=?, date_declaration=?, description=?,statut=?, priorite=?,lieu=?, equipement=? WHERE id_maintenance=?";
        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setString(1, maintenance.getType());
        ps.setDate(2, Date.valueOf(maintenance.getDateDeclaration()));
        ps.setString(3, maintenance.getDescription());
        ps.setString(4, maintenance.getStatut());

        ps.setString(5, maintenance.getPriorite());
        ps.setString(6, maintenance.getLieu());
        ps.setString(7, maintenance.getEquipement());
        ps.setInt(8, maintenance.getId());

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
            // On utilise le constructeur complet comme pour ServicePersonne
            Maintenance m = new Maintenance(
                    rs.getInt("id_maintenance"),
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
