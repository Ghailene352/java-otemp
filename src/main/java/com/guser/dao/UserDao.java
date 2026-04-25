package com.guser.dao;

import com.guser.config.DatabaseConnection;
import com.guser.model.User;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class UserDao {

    private boolean schemaInitialized;

    public UserDao() {
    }

    public List<User> findAll() throws SQLException {
        ensureInitialized();
        String sql = """
                SELECT id, nom, prenoms, email, telephone, mot_de_passe, role, face_template, date_creation
                FROM utilisateurs
                ORDER BY id DESC
                """;
        List<User> users = new ArrayList<>();

        try (Connection connection = DatabaseConnection.getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {

            while (resultSet.next()) {
                users.add(mapUser(resultSet));
            }
        }

        return users;
    }

    public Optional<User> authenticate(String email, String motDePasse) throws SQLException {
        ensureInitialized();
        String sql = """
                SELECT id, nom, prenoms, email, telephone, mot_de_passe, role, face_template, date_creation
                FROM utilisateurs
                WHERE email = ? AND mot_de_passe = ?
                LIMIT 1
                """;

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, email);
            statement.setString(2, motDePasse);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? Optional.of(mapUser(resultSet)) : Optional.empty();
            }
        }
    }

    public void insert(User user) throws SQLException {
        ensureInitialized();
        String sql = """
                INSERT INTO utilisateurs(nom, prenoms, email, telephone, mot_de_passe, role)
                VALUES (?, ?, ?, ?, ?, ?)
                """;

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, user.getNom());
            statement.setString(2, user.getPrenoms());
            statement.setString(3, user.getEmail());
            statement.setString(4, user.getTelephone());
            statement.setString(5, user.getMotDePasse());
            statement.setString(6, user.getRole());
            statement.executeUpdate();
        }
    }

    public void update(User user) throws SQLException {
        ensureInitialized();
        String sql = """
                UPDATE utilisateurs
                SET nom = ?, prenoms = ?, email = ?, telephone = ?, mot_de_passe = ?, role = ?
                WHERE id = ?
                """;

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, user.getNom());
            statement.setString(2, user.getPrenoms());
            statement.setString(3, user.getEmail());
            statement.setString(4, user.getTelephone());
            statement.setString(5, user.getMotDePasse());
            statement.setString(6, user.getRole());
            statement.setInt(7, user.getId());
            statement.executeUpdate();
        }
    }

    public void delete(int userId) throws SQLException {
        ensureInitialized();
        String sql = "DELETE FROM utilisateurs WHERE id = ?";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, userId);
            statement.executeUpdate();
        }
    }

    public boolean existsByEmail(String email) throws SQLException {
        ensureInitialized();
        String sql = "SELECT COUNT(*) FROM utilisateurs WHERE email = ?";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, email);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() && resultSet.getInt(1) > 0;
            }
        }
    }

    public boolean existsByEmailExceptId(String email, int userId) throws SQLException {
        ensureInitialized();
        String sql = "SELECT COUNT(*) FROM utilisateurs WHERE email = ? AND id <> ?";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, email);
            statement.setInt(2, userId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() && resultSet.getInt(1) > 0;
            }
        }
    }

    public Optional<User> findByEmail(String email) throws SQLException {
        ensureInitialized();
        String sql = """
                SELECT id, nom, prenoms, email, telephone, mot_de_passe, role, face_template, date_creation
                FROM utilisateurs
                WHERE email = ?
                LIMIT 1
                """;

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, email);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? Optional.of(mapUser(resultSet)) : Optional.empty();
            }
        }
    }

    public void updatePassword(int userId, String newPassword) throws SQLException {
        ensureInitialized();
        String sql = "UPDATE utilisateurs SET mot_de_passe = ? WHERE id = ?";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, newPassword);
            statement.setInt(2, userId);
            statement.executeUpdate();
        }
    }

    public List<User> findAdminsWithFaceTemplate() throws SQLException {
        ensureInitialized();
        String sql = """
                SELECT id, nom, prenoms, email, telephone, mot_de_passe, role, face_template, date_creation
                FROM utilisateurs
                WHERE role = 'ADMIN'
                  AND face_template IS NOT NULL
                  AND face_template <> ''
                ORDER BY id DESC
                """;
        List<User> users = new ArrayList<>();

        try (Connection connection = DatabaseConnection.getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {
            while (resultSet.next()) {
                users.add(mapUser(resultSet));
            }
        }

        return users;
    }

    public void updateFaceTemplate(int userId, String faceTemplate) throws SQLException {
        ensureInitialized();
        String sql = "UPDATE utilisateurs SET face_template = ? WHERE id = ?";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            if (faceTemplate == null || faceTemplate.isBlank()) {
                statement.setNull(1, Types.LONGVARCHAR);
            } else {
                statement.setString(1, faceTemplate);
            }
            statement.setInt(2, userId);
            statement.executeUpdate();
        }
    }

    public void clearFaceTemplate(int userId) throws SQLException {
        updateFaceTemplate(userId, null);
    }

    public int countAll() throws SQLException {
        ensureInitialized();
        return countWithQuery("SELECT COUNT(*) FROM utilisateurs");
    }

    private void ensureInitialized() throws SQLException {
        if (schemaInitialized) {
            return;
        }
        ensureSchema();
        schemaInitialized = true;
    }

    private void ensureSchema() throws SQLException {
        try (Connection connection = DatabaseConnection.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute("""
                    CREATE TABLE IF NOT EXISTS utilisateurs (
                        id INT PRIMARY KEY AUTO_INCREMENT,
                        nom VARCHAR(100) NOT NULL,
                        prenoms VARCHAR(150) NOT NULL DEFAULT '',
                        email VARCHAR(150) NOT NULL UNIQUE,
                        telephone VARCHAR(30) NOT NULL,
                        mot_de_passe VARCHAR(255) NOT NULL,
                        role VARCHAR(40) NOT NULL DEFAULT 'UTILISATEUR',
                        face_template LONGTEXT NULL,
                        date_creation TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
                    )
                    """);

            DatabaseMetaData metaData = connection.getMetaData();
            if (!hasColumn(metaData, "prenoms")) {
                statement.execute("ALTER TABLE utilisateurs ADD COLUMN prenoms VARCHAR(150) NOT NULL DEFAULT '' AFTER nom");
            }
            if (!hasColumn(metaData, "mot_de_passe")) {
                statement.execute("ALTER TABLE utilisateurs ADD COLUMN mot_de_passe VARCHAR(255) NOT NULL DEFAULT '' AFTER telephone");
            }
            if (!hasColumn(metaData, "role")) {
                statement.execute("ALTER TABLE utilisateurs ADD COLUMN role VARCHAR(40) NOT NULL DEFAULT 'UTILISATEUR' AFTER mot_de_passe");
            }
            if (!hasColumn(metaData, "face_template")) {
                statement.execute("ALTER TABLE utilisateurs ADD COLUMN face_template LONGTEXT NULL AFTER role");
            }
            if (!hasColumn(metaData, "date_creation")) {
                statement.execute("ALTER TABLE utilisateurs ADD COLUMN date_creation TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP");
            }
            if (hasColumn(metaData, "email_verifie")) {
                statement.execute("ALTER TABLE utilisateurs DROP COLUMN email_verifie");
            }
            statement.execute("DROP TABLE IF EXISTS administrateurs");
        }
    }

    private boolean hasColumn(DatabaseMetaData metaData, String columnName) throws SQLException {
        try (ResultSet columns = metaData.getColumns(null, null, "utilisateurs", columnName)) {
            return columns.next();
        }
    }

    private User mapUser(ResultSet resultSet) throws SQLException {
        return new User(
                resultSet.getInt("id"),
                resultSet.getString("nom"),
                resultSet.getString("prenoms"),
                resultSet.getString("email"),
                resultSet.getString("telephone"),
                resultSet.getString("mot_de_passe"),
                resultSet.getString("role"),
                toLocalDateTime(resultSet.getTimestamp("date_creation")),
                resultSet.getString("face_template")
        );
    }

    private java.time.LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }

    private int countWithQuery(String sql) throws SQLException {
        try (Connection connection = DatabaseConnection.getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {
            return resultSet.next() ? resultSet.getInt(1) : 0;
        }
    }
}
