package otemps.services;

import java.sql.SQLException;
import java.util.List;

public interface IService<T> {

    int ajouter(T entity) throws SQLException;

    void delete(int id) throws SQLException;


    List<T> afficher();

    void update(T entity) throws SQLException;

}
