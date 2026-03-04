package services;

import java.sql.SQLException;
import java.util.List;

public interface IService<T> {

    int create(T t) throws SQLException;

    void update(T t) throws SQLException;

    boolean delete(int id) throws SQLException;

    List<T> read() throws SQLException;

    // This method might not be needed for all services
    // You can keep it as default or remove it
    default T findByEmail(String email) throws SQLException {
        return null; // Default implementation
    }


}