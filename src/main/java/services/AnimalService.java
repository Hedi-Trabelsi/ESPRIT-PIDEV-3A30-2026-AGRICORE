package services;

import models.Animal;
import utils.MyDatabase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class AnimalService implements IService<Animal>{

    private Connection connection;

    public AnimalService(){
        connection = MyDatabase.getInstance().getConnection();
    }

    @Override
    public int create(Animal a) throws SQLException {

        String sql = "INSERT INTO animal (idAgriculteur, codeAnimal, espece, race, sexe, dateNaissance) VALUES (?, ?, ?, ?, ?, ?)";

        PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);

        ps.setInt(1, a.getIdAgriculteur());
        ps.setString(2, a.getCodeAnimal());
        ps.setString(3, a.getEspece());
        ps.setString(4, a.getRace());
        ps.setString(5, a.getSexe());
        ps.setDate(6, a.getDateNaissance());

        ps.executeUpdate();

        ResultSet rs = ps.getGeneratedKeys();
        if (rs.next()) return rs.getInt(1);

        return -1;
    }

    @Override
    public void update(Animal a) throws SQLException {

        String sql = "UPDATE animal SET codeAnimal=?, espece=?, race=?, sexe=? WHERE idAnimal=?";

        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setString(1, a.getCodeAnimal());
        ps.setString(2, a.getEspece());
        ps.setString(3, a.getRace());
        ps.setString(4, a.getSexe());
        ps.setInt(5, a.getIdAnimal());

        ps.executeUpdate();
    }

    @Override
    public void delete(int id) throws SQLException {

        String sql = "DELETE FROM animal WHERE idAnimal = ?";
        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setInt(1, id);
        ps.executeUpdate();
    }

    @Override
    public List<Animal> read() throws SQLException {

        String sql = "SELECT * FROM animal";
        Statement st = connection.createStatement();
        ResultSet rs = st.executeQuery(sql);

        List<Animal> list = new ArrayList<>();

        while(rs.next()){
            Animal a = new Animal();
            a.setIdAnimal(rs.getInt("idAnimal"));
            a.setCodeAnimal(rs.getString("codeAnimal"));
            a.setEspece(rs.getString("espece"));
            a.setRace(rs.getString("race"));
            a.setSexe(rs.getString("sexe"));
            a.setDateNaissance(rs.getDate("dateNaissance"));
            list.add(a);
        }

        return list;
    }
}