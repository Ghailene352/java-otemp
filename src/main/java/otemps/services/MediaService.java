package otemps.services;

import otemps.entites.Media;
import otemps.main.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MediaService implements IService<Media> {

    private final Connection cnx = DatabaseConnection.getInstance().getCnx();

    @Override
    public int ajouter(Media media) throws SQLException {
        String req = "INSERT INTO `media` (`lienFichier`, `type`, `idObjet`) VALUES (?, ?, ?)";

        try (PreparedStatement ps = cnx.prepareStatement(req, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, media.getLienFichier());
            ps.setString(2, media.getType());
            ps.setInt(3, media.getIdObjet());

            int rowsInserted = ps.executeUpdate();

            if (rowsInserted > 0) {
                ResultSet rs = ps.getGeneratedKeys();
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
            return -1;
        }
    }

    @Override
    public void delete(int id) throws SQLException {
        String req = "DELETE FROM `media` WHERE idMedia = ?";
        try (PreparedStatement ps = cnx.prepareStatement(req)) {
            ps.setInt(1, id);
            ps.executeUpdate();
            System.out.println("Media supprimé !");
        }
    }

    @Override
    public List<Media> afficher() {
        List<Media> medias = new ArrayList<>();
        String req = "SELECT * FROM `media`";

        try (Statement statement = cnx.createStatement();
             ResultSet rs = statement.executeQuery(req)) {

            while (rs.next()) {
                Media media = new Media(
                        rs.getInt("idMedia"),
                        rs.getString("lienFichier"),
                        rs.getString("type"),
                        rs.getInt("idObjet")
                );
                medias.add(media);
            }
        } catch (SQLException e) {
            System.out.println("Erreur : " + e.getMessage());
        }
        return medias;
    }

    @Override
    public void update(Media media) throws SQLException {
        String req = "UPDATE `media` SET `lienFichier`=?, `type`=?, `idObjet`=? WHERE `idMedia`=?";

        try (PreparedStatement ps = cnx.prepareStatement(req)) {
            ps.setString(1, media.getLienFichier());
            ps.setString(2, media.getType());
            ps.setInt(3, media.getIdObjet());
            ps.setInt(4, media.getIdMedia());
            ps.executeUpdate();
        } catch (SQLException e) {
            System.out.println("Erreur update : " + e.getMessage());
            throw e;
        }
    }

    public Media getById(int id) {
        String query = "SELECT * FROM `media` WHERE idMedia = ?";
        try (PreparedStatement ps = cnx.prepareStatement(query)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return new Media(
                        rs.getInt("idMedia"),
                        rs.getString("lienFichier"),
                        rs.getString("type"),
                        rs.getInt("idObjet")
                );
            }
        } catch (SQLException e) {
            System.out.println("Erreur : " + e.getMessage());
        }
        return null;
    }

    public List<Media> getByObjet(int idObjet) {
        List<Media> medias = new ArrayList<>();
        String req = "SELECT * FROM `media` WHERE idObjet = ?";

        try (PreparedStatement ps = cnx.prepareStatement(req)) {
            ps.setInt(1, idObjet);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                Media media = new Media(
                        rs.getInt("idMedia"),
                        rs.getString("lienFichier"),
                        rs.getString("type"),
                        rs.getInt("idObjet")
                );
                medias.add(media);
            }
        } catch (SQLException e) {
            System.out.println("Erreur : " + e.getMessage());
        }
        return medias;
    }
    public int getTotalCount() {
        String sql = "SELECT COUNT(*) as total FROM media";

        try (Connection conn = DatabaseConnection.getInstance().getCnx();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            if (rs.next()) {
                return rs.getInt("total");
            }
        } catch (SQLException e) {
            System.out.println("Erreur getTotalCount Media: " + e.getMessage());
        }

        return 0;
    }

    public int getCountByObjet(int idObjet) {
        String sql = "SELECT COUNT(*) as total FROM media WHERE id_objet = ?";

        try (Connection conn = DatabaseConnection.getInstance().getCnx();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, idObjet);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return rs.getInt("total");
            }
        } catch (SQLException e) {
            System.out.println("Erreur getCountByObjet: " + e.getMessage());
        }

        return 0;
    }

}
