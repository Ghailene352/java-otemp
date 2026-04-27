package otemps.main;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnection {

    private Connection cnx;
    private static DatabaseConnection instance;

    // Paramètres de connexion
    private final String URL = "jdbc:mysql://127.0.0.1:3307/mon_musee";
    private final String USERNAME = "root";
    private final String PASSWORD = "";

    // Constructeur privé pour le Singleton
    private DatabaseConnection() {
        connect();
    }

    // Méthode interne pour (ré)établir la connexion
    private void connect() {
        try {
            if (cnx == null || cnx.isClosed()) {
                cnx = DriverManager.getConnection(URL, USERNAME, PASSWORD);
                System.out.println("Connexion établie avec succès !");
            }
        } catch (SQLException e) {
            System.err.println("Erreur de connexion : " + e.getMessage());
            // Ne pas jeter de RuntimeException ici pour éviter de crash l'app au démarrage
        }
    }

    public static DatabaseConnection getInstance() {
        if (instance == null) {
            instance = new DatabaseConnection();
        }
        return instance;
    }

    // Vérifie et renvoie la connexion
    public Connection getCnx() {
        try {
            // Si la connexion a été fermée par une autre classe, on la réouvre
            if (cnx == null || cnx.isClosed()) {
                connect();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return cnx;
    }
}