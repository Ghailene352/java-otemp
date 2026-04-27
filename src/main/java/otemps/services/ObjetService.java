package otemps.services;

import otemps.entites.Media;
import otemps.entites.Objet;
import otemps.main.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ObjetService {

    private CategorieService categorieService = new CategorieService();

    private Connection getConnection() throws SQLException {
        Connection conn = DatabaseConnection.getInstance().getCnx();
        if (conn == null || conn.isClosed()) {
            System.err.println("❌ La connexion était fermée, tentative de récupération...");
        }
        return conn;
    }

    /**
     * 📋 Récupère tous les objets avec leurs médias associés (Utilisé par la Galerie)
     */
    public List<Objet> afficher() {
        List<Objet> objets = new ArrayList<>();
        String sql = "SELECT o.*, m.id as media_id, m.lien_fichier, m.type_media " +
                "FROM objet o " +
                "LEFT JOIN media m ON o.id = m.objet_id " +
                "ORDER BY o.nom ASC";

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                int idObj = rs.getInt("id");

                // On cherche si l'objet est déjà créé dans notre liste
                Objet obj = objets.stream()
                        .filter(o -> o.getIdObjet() == idObj)
                        .findFirst()
                        .orElse(null);

                if (obj == null) {
                    obj = buildObjet(rs);
                    objets.add(obj);
                    System.out.println("✅ Objet chargé: " + obj.getNom());
                }

                // Ajout du média si présent
                ajouterMediaALObjet(rs, obj);
            }
        } catch (SQLException e) {
            System.err.println("❌ Erreur afficher: " + e.getMessage());
        }
        System.out.println("📊 Total objets chargés: " + objets.size());
        return objets;
    }

    /**
     * 🔍 Récupère les objets par catégorie (Utilisé par le tri)
     */
    public List<Objet> findByCategorie(int idCategorie) {
        List<Objet> objets = new ArrayList<>();
        String sql = "SELECT o.*, m.id as media_id, m.lien_fichier, m.type_media " +
                "FROM objet o " +
                "LEFT JOIN media m ON o.id = m.objet_id " +
                "WHERE o.categorie_id = ? " +
                "ORDER BY o.nom ASC";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, idCategorie);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                int idObj = rs.getInt("id");

                Objet obj = objets.stream()
                        .filter(o -> o.getIdObjet() == idObj)
                        .findFirst()
                        .orElse(null);

                if (obj == null) {
                    obj = buildObjet(rs);
                    objets.add(obj);
                }

                ajouterMediaALObjet(rs, obj);
            }
            System.out.println("🔍 findByCategorie: " + objets.size() + " objets trouvés pour catégorie ID=" + idCategorie);
        } catch (SQLException e) {
            System.err.println("❌ Erreur findByCategorie: " + e.getMessage());
        }
        return objets;
    }

    /**
     * 📌 Alias pour compatibilité avec le ShowController
     */
    public List<Objet> getByCategorie(int idCategorie) {
        return findByCategorie(idCategorie);
    }

    /**
     * 🔎 Recherche multi-critères
     */
    public List<Objet> search(String query) {
        List<Objet> objets = new ArrayList<>();
        String sql = "SELECT o.*, m.id as media_id, m.lien_fichier, m.type_media " +
                "FROM objet o " +
                "LEFT JOIN media m ON o.id = m.objet_id " +
                "WHERE o.nom LIKE ? " +
                "OR o.description_historique LIKE ? " +
                "OR o.epoque LIKE ? " +
                "OR o.origine LIKE ? " +
                "OR o.materiaux LIKE ? " +
                "ORDER BY o.nom ASC";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            String pattern = "%" + query + "%";
            pstmt.setString(1, pattern);
            pstmt.setString(2, pattern);
            pstmt.setString(3, pattern);
            pstmt.setString(4, pattern);
            pstmt.setString(5, pattern);

            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                int idObj = rs.getInt("id");

                Objet obj = objets.stream()
                        .filter(o -> o.getIdObjet() == idObj)
                        .findFirst()
                        .orElse(null);

                if (obj == null) {
                    obj = buildObjet(rs);
                    objets.add(obj);
                }

                ajouterMediaALObjet(rs, obj);
            }
            System.out.println("🔎 Recherche '" + query + "': " + objets.size() + " résultats");
        } catch (SQLException e) {
            System.err.println("❌ Erreur search: " + e.getMessage());
        }
        return objets;
    }

    /**
     * 📍 Filtrage par époque
     */
    public List<Objet> findByEpoque(String epoque) {
        List<Objet> objets = new ArrayList<>();
        String sql = "SELECT o.*, m.id as media_id, m.lien_fichier, m.type_media " +
                "FROM objet o " +
                "LEFT JOIN media m ON o.id = m.objet_id " +
                "WHERE o.epoque LIKE ? " +
                "ORDER BY o.nom ASC";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, "%" + epoque + "%");
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                int idObj = rs.getInt("id");

                Objet obj = objets.stream()
                        .filter(o -> o.getIdObjet() == idObj)
                        .findFirst()
                        .orElse(null);

                if (obj == null) {
                    obj = buildObjet(rs);
                    objets.add(obj);
                }

                ajouterMediaALObjet(rs, obj);
            }
            System.out.println("📅 findByEpoque '" + epoque + "': " + objets.size() + " objets");
        } catch (SQLException e) {
            System.err.println("❌ Erreur findByEpoque: " + e.getMessage());
        }
        return objets;
    }

    /**
     * 🌍 Filtrage par origine
     */
    public List<Objet> findByOrigine(String origine) {
        List<Objet> objets = new ArrayList<>();
        String sql = "SELECT o.*, m.id as media_id, m.lien_fichier, m.type_media " +
                "FROM objet o " +
                "LEFT JOIN media m ON o.id = m.objet_id " +
                "WHERE o.origine LIKE ? " +
                "ORDER BY o.nom ASC";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, "%" + origine + "%");
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                int idObj = rs.getInt("id");

                Objet obj = objets.stream()
                        .filter(o -> o.getIdObjet() == idObj)
                        .findFirst()
                        .orElse(null);

                if (obj == null) {
                    obj = buildObjet(rs);
                    objets.add(obj);
                }

                ajouterMediaALObjet(rs, obj);
            }
            System.out.println("🌍 findByOrigine '" + origine + "': " + objets.size() + " objets");
        } catch (SQLException e) {
            System.err.println("❌ Erreur findByOrigine: " + e.getMessage());
        }
        return objets;
    }

    /**
     * 🔍 Récupère un objet par ID avec tous ses médias
     */
    public Objet getById(int idObjet) {
        String sql = "SELECT o.*, m.id as media_id, m.lien_fichier, m.type_media " +
                "FROM objet o " +
                "LEFT JOIN media m ON o.id = m.objet_id " +
                "WHERE o.id = ?";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, idObjet);
            ResultSet rs = pstmt.executeQuery();

            Objet obj = null;

            while (rs.next()) {
                if (obj == null) {
                    obj = buildObjet(rs);
                    System.out.println("✅ Objet trouvé: " + obj.getNom());
                }
                ajouterMediaALObjet(rs, obj);
            }

            return obj;
        } catch (SQLException e) {
            System.err.println("❌ Erreur getById: " + e.getMessage());
        }
        return null;
    }

    /**
     * ➕ Ajouter un nouvel objet
     */
    public int ajouter(Objet objet) throws SQLException {
        String sql = "INSERT INTO objet (nom, description_historique, epoque, origine, materiaux, categorie_id) " +
                "VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setString(1, objet.getNom());
            pstmt.setString(2, objet.getDescription());
            pstmt.setString(3, objet.getEpoque());
            pstmt.setString(4, objet.getOrigine());
            pstmt.setString(5, objet.getMateriaux());
            pstmt.setInt(6, objet.getIdCategorie());

            pstmt.executeUpdate();

            ResultSet rs = pstmt.getGeneratedKeys();
            if (rs.next()) {
                int id = rs.getInt(1);
                System.out.println("✅ Objet créé avec ID: " + id);
                return id;
            }
        }
        return -1;
    }

    /**
     * ✏️ Modifier un objet existant
     */
    public void update(Objet objet) throws SQLException {
        String sql = "UPDATE objet SET nom=?, description_historique=?, epoque=?, origine=?, materiaux=?, categorie_id=? WHERE id=?";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, objet.getNom());
            pstmt.setString(2, objet.getDescription());
            pstmt.setString(3, objet.getEpoque());
            pstmt.setString(4, objet.getOrigine());
            pstmt.setString(5, objet.getMateriaux());
            pstmt.setInt(6, objet.getIdCategorie());
            pstmt.setInt(7, objet.getIdObjet());

            int rowsUpdated = pstmt.executeUpdate();
            if (rowsUpdated > 0) {
                System.out.println("✅ Objet modifié: " + objet.getNom());
            }
        }
    }

    /**
     * 🗑️ Supprimer un objet
     */
    public void delete(int idObjet) throws SQLException {
        String sql = "DELETE FROM objet WHERE id = ?";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, idObjet);
            int rowsDeleted = pstmt.executeUpdate();

            if (rowsDeleted > 0) {
                System.out.println("✅ Objet supprimé: ID=" + idObjet);
            }
        }
    }

    /**
     * 📊 Récupère le nombre total d'objets
     */
    public int getTotalCount() {
        String sql = "SELECT COUNT(*) as total FROM objet";

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            if (rs.next()) {
                int total = rs.getInt("total");
                System.out.println("📊 Total objets en base: " + total);
                return total;
            }
        } catch (SQLException e) {
            System.err.println("❌ Erreur getTotalCount: " + e.getMessage());
        }
        return 0;
    }

    /**
     * 📊 Récupère le nombre d'objets par catégorie
     */
    public int getCountByCategorie(int idCategorie) {
        String sql = "SELECT COUNT(*) as total FROM objet WHERE categorie_id = ?";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, idCategorie);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                int total = rs.getInt("total");
                System.out.println("📊 Objets catégorie ID=" + idCategorie + ": " + total);
                return total;
            }
        } catch (SQLException e) {
            System.err.println("❌ Erreur getCountByCategorie: " + e.getMessage());
        }
        return 0;
    }

    /**
     * 🏆 Récupère les objets les plus anciens (par époque)
     */
    public List<Objet> getOldestObjets(int limit) {
        List<Objet> objets = new ArrayList<>();
        String sql = "SELECT o.*, m.id as media_id, m.lien_fichier, m.type_media " +
                "FROM objet o " +
                "LEFT JOIN media m ON o.id = m.objet_id " +
                "ORDER BY o.epoque ASC " +
                "LIMIT ?";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, limit);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                int idObj = rs.getInt("id");

                Objet obj = objets.stream()
                        .filter(o -> o.getIdObjet() == idObj)
                        .findFirst()
                        .orElse(null);

                if (obj == null) {
                    obj = buildObjet(rs);
                    objets.add(obj);
                }

                ajouterMediaALObjet(rs, obj);
            }
            System.out.println("🏆 Top " + limit + " objets anciens chargés");
        } catch (SQLException e) {
            System.err.println("❌ Erreur getOldestObjets: " + e.getMessage());
        }
        return objets;
    }

    /**
     * 🎯 Récupère les objets les plus récemment ajoutés
     */
    public List<Objet> getLatestObjets(int limit) {
        List<Objet> objets = new ArrayList<>();
        String sql = "SELECT o.*, m.id as media_id, m.lien_fichier, m.type_media " +
                "FROM objet o " +
                "LEFT JOIN media m ON o.id = m.objet_id " +
                "ORDER BY o.id DESC " +
                "LIMIT ?";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, limit);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                int idObj = rs.getInt("id");

                Objet obj = objets.stream()
                        .filter(o -> o.getIdObjet() == idObj)
                        .findFirst()
                        .orElse(null);

                if (obj == null) {
                    obj = buildObjet(rs);
                    objets.add(obj);
                }

                ajouterMediaALObjet(rs, obj);
            }
            System.out.println("🎯 Top " + limit + " objets récents chargés");
        } catch (SQLException e) {
            System.err.println("❌ Erreur getLatestObjets: " + e.getMessage());
        }
        return objets;
    }

    /**
     * 📈 Obtient les stats par époque
     */
    public java.util.Map<String, Integer> getStatsByEpoque() {
        java.util.Map<String, Integer> stats = new java.util.HashMap<>();
        String sql = "SELECT epoque, COUNT(*) as count FROM objet GROUP BY epoque ORDER BY count DESC";

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                String epoque = rs.getString("epoque");
                int count = rs.getInt("count");
                stats.put(epoque, count);
            }
            System.out.println("📈 Stats par époque: " + stats.size() + " périodes");
        } catch (SQLException e) {
            System.err.println("❌ Erreur getStatsByEpoque: " + e.getMessage());
        }
        return stats;
    }

    /**
     * 📈 Obtient les stats par origine
     */
    public java.util.Map<String, Integer> getStatsByOrigine() {
        java.util.Map<String, Integer> stats = new java.util.HashMap<>();
        String sql = "SELECT origine, COUNT(*) as count FROM objet GROUP BY origine ORDER BY count DESC";

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                String origine = rs.getString("origine");
                int count = rs.getInt("count");
                stats.put(origine, count);
            }
            System.out.println("📈 Stats par origine: " + stats.size() + " régions");
        } catch (SQLException e) {
            System.err.println("❌ Erreur getStatsByOrigine: " + e.getMessage());
        }
        return stats;
    }

    // --- Méthodes utilitaires (Helpers) ---

    /**
     * 🔧 Construit un objet à partir d'un ResultSet
     */
    private Objet buildObjet(ResultSet rs) throws SQLException {
        Objet obj = new Objet();
        obj.setIdObjet(rs.getInt("id"));
        obj.setNom(rs.getString("nom"));
        obj.setDescription(rs.getString("description_historique"));
        obj.setEpoque(rs.getString("epoque"));
        obj.setOrigine(rs.getString("origine"));
        obj.setMateriaux(rs.getString("materiaux"));
        obj.setIdCategorie(rs.getInt("categorie_id"));
        return obj;
    }

    /**
     * 🔧 Construit un objet avec ses médias
     */
    private Objet buildObjetWithMedia(ResultSet rs) throws SQLException {
        Objet obj = buildObjet(rs);
        ajouterMediaALObjet(rs, obj);
        return obj;
    }

    /**
     * 🔧 Ajoute un média à un objet
     */
    private void ajouterMediaALObjet(ResultSet rs, Objet obj) throws SQLException {
        String lien = rs.getString("lien_fichier");
        if (lien != null && !lien.isEmpty()) {
            Media m = new Media();
            try {
                m.setIdMedia(rs.getInt("media_id"));
            } catch (Exception e) {
                // Ignorer l'erreur si media_id n'existe pas
            }
            m.setLienFichier(lien);
            obj.addMedia(m);
        }
    }
}