package services;

import models.User;
import utils.MyDatabase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UserService implements IService<User>{

    private Connection connection;

    public UserService(){
        connection = MyDatabase.getInstance().getConnection();
    }

    @Override
    public int create(User user) throws SQLException {
        String sql = "insert into user (firstName,lastName,age)"+
                "values('"+ user.getFirstName()+"','"+ user.getLastName()+"'" +
                ","+ user.getAge()+")";
        PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
        ps.executeUpdate();
        ResultSet rs = ps.getGeneratedKeys();
        if (rs.next()) {
            return rs.getInt(1);
        }
        return -1;
    }

    @Override
    public void update(User user) throws SQLException {
        String sql = "update user set firstName = ?, lastName = ?, age = ? where id = ?";
        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setString(1, user.getFirstName());
        ps.setString(2, user.getLastName());
        ps.setInt(3, user.getAge());
        ps.setInt(4, user.getId());
        ps.executeUpdate();
    }

    @Override
    public void delete(int id) throws SQLException {
        String sql = "DELETE FROM user WHERE id = ?";
        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setInt(1, id);
        ps.executeUpdate();
    }

    @Override
    public List<User> read() throws SQLException {
        String sql = "select * from user";
        Statement statement = connection.createStatement();
        ResultSet rs = statement.executeQuery(sql);
        List<User> people = new ArrayList<>();
        while (rs.next()){
            User p = new User();
            p.setId(rs.getInt("id"));
            p.setAge(rs.getInt("age"));
            p.setFirstName(rs.getString("firstName"));
            p.setLastName(rs.getString("lastName"));

            people.add(p);
        }
        return people;
    }
}
