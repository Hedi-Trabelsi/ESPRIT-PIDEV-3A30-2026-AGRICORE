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
        maintenance.setStatut("en cours");
        String sql = "INSERT INTO maintenance(type,date_declaration, description, statut,id_technicien,priorite) VALUES ('"
                + maintenance.getType() + "', '"
                + maintenance.getDateDeclaration() + "', '"
                + maintenance.getDescription() + "', '"
                + maintenance.getStatut() + "', NULL, '"
                + maintenance.getPriorite() + "')";
        Statement statement = connection.createStatement();
        statement.executeUpdate(sql);
    }


    @Override
    public void modifier(Maintenance maintenance) throws SQLException {
        String sql = "UPDATE maintenance SET type=?,date_declaration=?, description=?,statut=?, id_technicien=?,priorite=? WHERE id_maintenance=?";
        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setString(1, maintenance.getType());
        ps.setDate(2, Date.valueOf(maintenance.getDateDeclaration()));
        ps.setString(3, maintenance.getDescription());
        ps.setString(4, maintenance.getStatut());
        ps.setInt(5, maintenance.getIdTechnicien());
        ps.setString(6, maintenance.getPriorite());
        ps.setInt(7, maintenance.getId());

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
                    rs.getInt("id"),
                    rs.getString("type"),
                    rs.getDate("dateDeclaration").toLocalDate(),
                    rs.getString("description"),
                    rs.getString("statutory"),
                    rs.getInt("idTechnicien"),
                    rs.getString("priorite")
            );
            maintenances.add(m);
        }

        return maintenances;
    }
}
