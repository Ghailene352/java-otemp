package otemps.services;

import otemps.entites.Categorie;
import otemps.main.DatabaseConnection;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CategorieService {

    private final Connection cnx = DatabaseConnection.getInstance().getCnx();

    public int ajouter(Categorie categorie) throws SQLException {
        // CORRECTION : id, nom_categorie, description_type
        String req = "INSERT INTO `categorie` (`nom_categorie`, `description_type`) VALUES (?, ?)";

        try (PreparedStatement ps = cnx.prepareStatement(req, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, categorie.getNomCategorie());
            ps.setString(2, categorie.getDescription());
            ps.executeUpdate();

            ResultSet rs = ps.getGeneratedKeys();
            if (rs.next()) return rs.getInt(1);
            return -1;
        }
    }

    public void delete(int id) throws SQLException {
        // CORRECTION : id
        String req = "DELETE FROM `categorie` WHERE `id` = ?";
        try (PreparedStatement ps = cnx.prepareStatement(req)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    public List<Categorie> afficher() {
        List<Categorie> categories = new ArrayList<>();
        String req = "SELECT * FROM `categorie`";
        try (Statement statement = cnx.createStatement();
             ResultSet rs = statement.executeQuery(req)) {
            while (rs.next()) {
                categories.add(new Categorie(
                        rs.getInt("id"),
                        rs.getString("nom_categorie"),
                        rs.getString("description_type")
                ));
            }
        } catch (SQLException e) {
            System.out.println("Erreur afficher : " + e.getMessage());
        }
        return categories;
    }

    public void update(Categorie categorie) throws SQLException {
        String req = "UPDATE `categorie` SET `nom_categorie`=?, `description_type`=? WHERE `id`=?";
        try (PreparedStatement ps = cnx.prepareStatement(req)) {
            ps.setString(1, categorie.getNomCategorie());
            ps.setString(2, categorie.getDescription());
            ps.setInt(3, categorie.getIdCategorie());
            ps.executeUpdate();
        }
    }

    public Categorie getById(int id) {
        String query = "SELECT * FROM `categorie` WHERE `id` = ?";
        try (PreparedStatement ps = cnx.prepareStatement(query)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return new Categorie(
                        rs.getInt("id"),
                        rs.getString("nom_categorie"),
                        rs.getString("description_type")
                );
            }
        } catch (SQLException e) {
            System.out.println("Erreur getById : " + e.getMessage());
        }
        return null;
    }
    public int getTotalCount() {
        String sql = "SELECT COUNT(*) as total FROM categorie";

        try (Connection conn = DatabaseConnection.getInstance().getCnx();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            if (rs.next()) {
                return rs.getInt("total");
            }
        } catch (SQLException e) {
            System.out.println("Erreur getTotalCount Categorie: " + e.getMessage());
        }

        return 0;
    }
}