package utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class MyDatabase {

    private static MyDatabase instance;
    private Connection connection;

    private final String URL = "jdbc:mysql://localhost:3306/projetjava"; // your DB name
    private final String USER = "root"; // usually root
    private final String PASSWORD = ""; // empty by default in XAMPP

    private MyDatabase() throws SQLException {
        try {
            connection = DriverManager.getConnection(URL, USER, PASSWORD);
        } catch (SQLException ex) {
            throw new SQLException("Database Connection Failed: " + ex.getMessage());
        }
    }

    public Connection getConnection() {
        return connection;
    }

    public static MyDatabase getInstance() throws SQLException {
        if (instance == null) {
            instance = new MyDatabase();
        } else if (instance.getConnection().isClosed()) {
            instance = new MyDatabase();
        }
        return instance;
    }
}