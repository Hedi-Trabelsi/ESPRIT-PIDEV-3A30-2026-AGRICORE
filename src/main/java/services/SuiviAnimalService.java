package services;

import Model.SuiviAnimal;
import utils.MyDatabase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class SuiviAnimalService implements IService<SuiviAnimal> {

    private Connection connection;

    public SuiviAnimalService() {
        try {
            connection = MyDatabase.getInstance().getConnection();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public int create(SuiviAnimal s) throws SQLException {
        // ✅ Ordre exact des colonnes de votre table MySQL
        String sql = "INSERT INTO suivi_animal " +
                "(idAnimal, dateSuivi, temperature, poids, rythmeCardiaque, etatSante, remarque, niveauActitive, niveauActivite) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

        PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
        ps.setInt(1, s.getIdAnimal());
        ps.setTimestamp(2, s.getDateSuivi());
        ps.setDouble(3, s.getTemperature());
        ps.setDouble(4, s.getPoids());
        ps.setInt(5, s.getRythmeCardiaque());
        ps.setString(6, s.getEtatSante() != null ? s.getEtatSante() : "Bon");
        ps.setString(7, s.getRemarque() != null ? s.getRemarque() : "");
        String niveau = s.getNiveauActivite() != null ? s.getNiveauActivite() : "Moyen";
        ps.setString(8, niveau);
        ps.setString(9, niveau);
        ps.executeUpdate();

        ResultSet rs = ps.getGeneratedKeys();
        if (rs.next()) return rs.getInt(1);
        return -1;
    }

    @Override
    public void update(SuiviAnimal s) throws SQLException {
        String sql = "UPDATE suivi_animal SET " +
                "temperature=?, poids=?, rythmeCardiaque=?, " +
                "etatSante=?, remarque=?, niveauActitive=?, niveauActivite=? " +
                "WHERE idSuivi=?";

        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setDouble(1, s.getTemperature());
        ps.setDouble(2, s.getPoids());
        ps.setInt(3, s.getRythmeCardiaque());
        ps.setString(4, s.getEtatSante() != null ? s.getEtatSante() : "Bon");
        ps.setString(5, s.getRemarque() != null ? s.getRemarque() : "");
        String niveau = s.getNiveauActivite() != null ? s.getNiveauActivite() : "Moyen";
        ps.setString(6, niveau);
        ps.setString(7, niveau);
        ps.setInt(8, s.getIdSuivi());
        ps.executeUpdate();
    }

    @Override
    public boolean delete(int id) throws SQLException {
        String sql = "DELETE FROM suivi_animal WHERE idSuivi=?";
        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setInt(1, id);
        return ps.executeUpdate() > 0;
    }

    @Override
    public List<SuiviAnimal> read() throws SQLException {
        String sql = "SELECT * FROM suivi_animal";
        Statement st = connection.createStatement();
        ResultSet rs = st.executeQuery(sql);

        List<SuiviAnimal> list = new ArrayList<>();
        while (rs.next()) {
            SuiviAnimal s = new SuiviAnimal();
            s.setIdSuivi(rs.getInt("idSuivi"));
            s.setIdAnimal(rs.getInt("idAnimal"));
            s.setDateSuivi(rs.getTimestamp("dateSuivi"));
            s.setTemperature(rs.getDouble("temperature"));
            s.setPoids(rs.getDouble("poids"));
            s.setRythmeCardiaque(rs.getInt("rythmeCardiaque"));
            s.setEtatSante(rs.getString("etatSante"));
            s.setRemarque(rs.getString("remarque"));
            s.setNiveauActivite(rs.getString("niveauActitive"));
            list.add(s);
        }
        return list;
    }

    public List<SuiviAnimal> readByAnimal(int idAnimal) throws SQLException {
        List<SuiviAnimal> filtered = new ArrayList<>();
        for (SuiviAnimal s : read()) {
            if (s.getIdAnimal() == idAnimal) filtered.add(s);
        }
        return filtered;
    }
}
