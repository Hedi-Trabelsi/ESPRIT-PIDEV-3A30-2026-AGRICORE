package services;

import models.SuiviAnimal;
import utils.MyDatabase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class SuiviAnimalService implements IService<SuiviAnimal>{

    private Connection connection;

    public SuiviAnimalService(){
        connection = MyDatabase.getInstance().getConnection();
    }

    @Override
    public int create(SuiviAnimal s) throws SQLException {

        String sql = "INSERT INTO suivi_animal (idAnimal, dateSuivi, temperature, poids, rythmeCardiaque, niveauActitive, etatSante, remarque) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        PreparedStatement ps = connection.prepareStatement(sql);

        ps.setInt(1, s.getIdAnimal());
        ps.setTimestamp(2, s.getDateSuivi());
        ps.setDouble(3, s.getTemperature());
        ps.setDouble(4, s.getPoids());
        ps.setInt(5, s.getRythmeCardiaque());
        ps.setString(6, s.getNiveauActivite());
        ps.setString(7, s.getEtatSante());
        ps.setString(8, s.getRemarque());

        ps.executeUpdate();

        return 1;
    }

    @Override
    public void update(SuiviAnimal s) throws SQLException {}

    @Override
    public void delete(int id) throws SQLException {
        String sql = "DELETE FROM suivi_animal WHERE idSuivi = ?";
        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setInt(1, id);
        ps.executeUpdate();
    }

    @Override
    public List<SuiviAnimal> read() throws SQLException {

        String sql = "SELECT * FROM suivi_animal";
        Statement st = connection.createStatement();
        ResultSet rs = st.executeQuery(sql);

        List<SuiviAnimal> list = new ArrayList<>();

        while(rs.next()){
            SuiviAnimal s = new SuiviAnimal();
            s.setIdSuivi(rs.getInt("idSuivi"));
            s.setTemperature(rs.getDouble("temperature"));
            s.setPoids(rs.getDouble("poids"));
            list.add(s);
        }

        return list;
    }
}