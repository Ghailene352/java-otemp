package otemps.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import otemps.services.CategorieService;
import otemps.services.ObjetService;
import otemps.services.MediaService;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class AdminDashboardController {

    @FXML private Label lblCategoriesCount;
    @FXML private Label lblObjetsCount;
    @FXML private Label lblMediasCount;
    @FXML private Label lblDbStatus;
    @FXML private Label lblLastUpdate;
    @FXML private Label lblStatus;

    private CategorieService categorieService;
    private ObjetService objetService;
    private MediaService mediaService;

    @FXML
    public void initialize() {
        System.out.println("✓ Initialisation du Dashboard Admin");

        // Initialisation des services
        categorieService = new CategorieService();
        objetService = new ObjetService();
        mediaService = new MediaService();

        // Chargement des données
        loadStatistics();
        updateTimestamp();
    }

    /**
     * Charge les statistiques du système
     */
    private void loadStatistics() {
        try {
            // Récupérer les counts
            int countCategories = categorieService.getTotalCount();
            int countObjets = objetService.getTotalCount();
            int countMedias = mediaService.getTotalCount();

            // Mettre à jour les labels
            lblCategoriesCount.setText(String.valueOf(countCategories));
            lblObjetsCount.setText(String.valueOf(countObjets));
            lblMediasCount.setText(String.valueOf(countMedias));

            System.out.println("✓ Statistiques chargées:");
            System.out.println("  - Catégories: " + countCategories);
            System.out.println("  - Objets: " + countObjets);
            System.out.println("  - Médias: " + countMedias);

            // Statut de la base de données
            lblDbStatus.setText("✅ Connectée");
            lblStatus.setText("✅ Système opérationnel");

        } catch (Exception e) {
            System.err.println("✗ Erreur chargement statistiques: " + e.getMessage());
            lblDbStatus.setText("❌ Erreur de connexion");
            lblStatus.setText("⚠️ Problème système");
        }
    }

    /**
     * Met à jour le timestamp de la dernière mise à jour
     */
    private void updateTimestamp() {
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");
        lblLastUpdate.setText(now.format(formatter));
    }

    // ==================== GESTION DES CATÉGORIES ====================

    @FXML
    public void handleCategories() {
        System.out.println("→ Ouverture de la gestion des Catégories");
        openWindow("/fxml/categorie/CategorieView.fxml", "📚 Gestion des Catégories", 1000, 700);
    }

    @FXML
    public void handleAddCategorie() {
        System.out.println("→ Ajout d'une nouvelle catégorie");
        openWindow("/fxml/categorie/AddCategorieView.fxml", "➕ Nouvelle Catégorie", 600, 400);
    }

    // ==================== GESTION DES OBJETS ====================

    @FXML
    public void handleObjets() {
        System.out.println("→ Ouverture de la gestion des Objets");
        openWindow("/fxml/objet/ObjetView.fxml", "🎨 Gestion des Objets", 1200, 700);
    }

    @FXML
    public void handleAddObjet() {
        System.out.println("→ Ajout d'un nouvel objet");
        openWindow("/fxml/objet/AddObjetView.fxml", "➕ Nouvel Objet", 800, 600);
    }

    // ==================== GESTION DES MÉDIAS ====================

    @FXML
    public void handleMedias() {
        System.out.println("→ Ouverture de la gestion des Médias");
        openWindow("/fxml/media/MediaView.fxml", "🖼️ Gestion des Médias", 1100, 700);
    }

    @FXML
    public void handleAddMedia() {
        System.out.println("→ Ajout d'un nouveau média");
        openWindow("/fxml/media/AddMediaView.fxml", "➕ Nouvel Média", 700, 500);
    }

    // ==================== AUTRES ACTIONS ====================

    @FXML
    public void handleReports() {
        System.out.println("→ Ouverture des Rapports");
        openWindow("/fxml/reports/ReportsView.fxml", "📊 Rapports & Statistiques", 1200, 800);
    }

    @FXML
    public void handleRefresh() {
        System.out.println("🔄 Rafraîchissement des données");
        loadStatistics();
        updateTimestamp();
        System.out.println("✓ Données rafraîchies");
    }

    @FXML
    public void handleHome() {
        System.out.println("→ Retour à l'accueil");
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/HomeView.fxml")
            );
            Parent root = loader.load();

            // Obtenir la fenêtre actuelle et charger HomeView
            Stage stage = (Stage) lblStatus.getScene().getWindow();
            stage.close();

            // Ouvrir HomeView dans une nouvelle fenêtre
            Stage homeStage = new Stage();
            homeStage.setTitle("🏛️ OTEMPS - Galerie Royale");
            homeStage.setScene(new Scene(root, 1200, 800));
            homeStage.show();

            System.out.println("✓ Retour à HomeView");

        } catch (IOException e) {
            System.err.println("✗ Erreur retour accueil: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    public void handleLogout() {
        System.out.println("→ Déconnexion");
        Stage stage = (Stage) lblStatus.getScene().getWindow();
        stage.close();
        System.out.println("✓ Déconnecté - Application fermée");
    }

    // ==================== MÉTHODE UTILITAIRE ====================

    /**
     * Ouvre une fenêtre avec un fichier FXML
     */
    private void openWindow(String fxmlPath, String title, int width, int height) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));

            if (loader.getLocation() == null) {
                System.err.println("✗ Fichier FXML non trouvé: " + fxmlPath);
                return;
            }

            Parent root = loader.load();

            Stage stage = new Stage();
            stage.setTitle("🏛️ OTEMPS - " + title);
            stage.setScene(new Scene(root, width, height));
            stage.show();

            System.out.println("✓ Fenêtre ouverte: " + title);

        } catch (IOException e) {
            System.err.println("✗ Erreur ouverture fenêtre: " + e.getMessage());
            e.printStackTrace();
        } catch (NullPointerException e) {
            System.err.println("✗ Fichier FXML non trouvé: " + fxmlPath);
            System.err.println("  Créez le fichier à: src/main/resources" + fxmlPath);
        }
    }
}