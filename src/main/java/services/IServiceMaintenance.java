package services;

import java.sql.SQLException;
import java.util.List;

public interface IServiceMaintenance<M>{
    void ajouter(M m) throws SQLException;
    void modifier(M m) throws SQLException;
    void supprimer(int id) throws SQLException;
    List<M> afficher() throws SQLException;
}
