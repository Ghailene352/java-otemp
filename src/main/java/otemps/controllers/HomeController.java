package otemps.controllers;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import otemps.entites.Objet;
import otemps.services.AIService;
import otemps.services.ObjetService;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class HomeController {

    // ✅ GALERIE ET FAVORIS
    @FXML private FlowPane galerieContainer;
    @FXML private FlowPane favorisContainer;
    @FXML private VBox favoritesSection;

    // ✅ RECHERCHE
    @FXML private TextField searchField;
    @FXML private Label statsLabel;
    @FXML private Label collectionCount;
    @FXML private Label favoritesCountLabel;

    // ✅ BOUTONS HEADER
    @FXML private Button toggleFavoritesButton;
    @FXML private Button toggleChatButton;

    // ✅ CHATBOT SIDEBAR
    @FXML private VBox chatSidebar;
    @FXML private ScrollPane chatScrollPane;
    @FXML private VBox chatContainer;
    @FXML private TextField messageInput;
    @FXML private Button uploadButton;
    @FXML private Button sendButton;
    @FXML private ImageView chatImageView;
    @FXML private Label imageInfoLabel;

    // ✅ SERVICES
    private ObjetService objetService = new ObjetService();
    private AIService aiService = new AIService();

    // ✅ DONNÉES
    private List<Objet> allObjets = new ArrayList<>();
    private Set<Integer> favorites = new HashSet<>();
    private File imageActuelle = null;

    // ✅ IMAGE PLACEHOLDER - CRÉÉE LOCALEMENT
    private Image placeholderImage;

    @FXML
    public void initialize() {
        System.out.println("✅ HomeController initialisé");

        // ✅ CRÉER L'IMAGE PLACEHOLDER LOCALEMENT
        createPlaceholderImage();

        // ✅ CHARGER LES OBJETS
        loadAllObjets();

        // ✅ LISTENER RECHERCHE
        if (searchField != null) {
            searchField.textProperty().addListener((obs, oldVal, newVal) -> handleSearch());
        }

        // ✅ LISTENERS CHATBOT
        if (uploadButton != null) {
            uploadButton.setOnAction(e -> handleUploadImage());
        }
        if (sendButton != null) {
            sendButton.setOnAction(e -> handleSendMessage());
        }
        if (messageInput != null) {
            messageInput.setOnAction(e -> handleSendMessage());
        }
    }

    // ============================
    // 🖼️ CRÉER IMAGE PLACEHOLDER
    // ============================

    private void createPlaceholderImage() {
        try {
            // ✅ CRÉER UNE IMAGE GRISE SIMPLE SANS INTERNET
            placeholderImage = new Image(getClass().getResourceAsStream("/images/placeholder.png"));
            if (placeholderImage.isError()) {
                System.out.println("⚠️ Image placeholder non trouvée, création d'une image vide");
                // Créer une image vide si le fichier n'existe pas
                placeholderImage = new Image("/images/placeholder.png", true);
            }
        } catch (Exception e) {
            System.out.println("⚠️ Création placeholder: " + e.getMessage());
        }
    }

    // ============================
    // 📦 CHARGEMENT DES OBJETS
    // ============================

    private void loadAllObjets() {
        new Thread(() -> {
            try {
                allObjets = objetService.afficher();
                if (allObjets == null) {
                    allObjets = new ArrayList<>();
                }

                Platform.runLater(() -> {
                    displayObjets(allObjets);
                    updateStats();
                    System.out.println("✅ " + allObjets.size() + " objets chargés");
                });
            } catch (Exception e) {
                System.err.println("❌ Erreur chargement objets: " + e.getMessage());
                e.printStackTrace();
            }
        }).start();
    }

    // ============================
    // 🤖 CHATBOT - UPLOAD IMAGE
    // ============================

    @FXML
    public void handleUploadImage() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("📸 Sélectionner une image d'objet patrimonial");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Images de haute qualité", "*.jpg", "*.jpeg", "*.png"),
                new FileChooser.ExtensionFilter("Tous les formats", "*.jpg", "*.jpeg", "*.png", "*.gif", "*.bmp"),
                new FileChooser.ExtensionFilter("Tous les fichiers", "*.*")
        );

        Stage stage = (Stage) uploadButton.getScene().getWindow();
        File selectedFile = fileChooser.showOpenDialog(stage);

        if (selectedFile != null) {
            imageActuelle = selectedFile;

            // ✅ AFFICHER L'APERÇU DE L'IMAGE
            try {
                Image image = new Image(selectedFile.toURI().toString());
                chatImageView.setImage(image);

                // ✅ AFFICHER LES INFO DE L'IMAGE
                String sizeKB = String.format("%.2f KB", selectedFile.length() / 1024.0);
                imageInfoLabel.setText("📷 " + selectedFile.getName() + " (" + sizeKB + ")");

                System.out.println("✅ Image chargée: " + selectedFile.getName());
                afficherImageDansChat(selectedFile);
                analyserImageAvecIA();

            } catch (Exception e) {
                imageInfoLabel.setText("❌ Erreur: " + e.getMessage());
                System.err.println("❌ Erreur aperçu image: " + e.getMessage());
            }
        }
    }

    private void afficherImageDansChat(File file) {
        HBox imageBox = new HBox();
        imageBox.setPadding(new Insets(10, 20, 10, 20));

        try {
            Image image = new Image(file.toURI().toString());
            ImageView imageView = new ImageView(image);
            imageView.setFitHeight(200);
            imageView.setFitWidth(300);
            imageView.setPreserveRatio(true);
            imageView.setStyle("-fx-border-color: #d4af37; " +
                    "-fx-border-width: 2; " +
                    "-fx-border-radius: 8;");

            imageBox.setStyle("-fx-alignment: center-right;");
            imageBox.getChildren().add(imageView);
            chatContainer.getChildren().add(imageBox);

            scrollChatToBottom();

        } catch (Exception e) {
            System.err.println("✗ Erreur chargement image chat: " + e.getMessage());
            afficherMessageBot("❌ Erreur lors du chargement de l'image");
        }
    }

    // ============================
    // 🤖 ANALYSE IMAGE AVEC IA
    // ============================

    private void analyserImageAvecIA() {
        afficherMessageBot("⏳ Analyse en cours avec Groq Vision...\n(Cela peut prendre quelques secondes)");

        new Thread(() -> {
            try {
                String result = aiService.analyzeImage(imageActuelle);

                Platform.runLater(() -> {
                    // ✅ SUPPRIMER LE MESSAGE D'ATTENTE
                    if (!chatContainer.getChildren().isEmpty()) {
                        chatContainer.getChildren().remove(chatContainer.getChildren().size() - 1);
                    }

                    // ✅ AFFICHER LE RÉSULTAT
                    if (result.contains("❌")) {
                        afficherMessageBot(result);
                    } else {
                        displayAnalysisResult(result);
                    }
                    scrollChatToBottom();
                });
            } catch (Exception e) {
                System.err.println("❌ Erreur analyse: " + e.getMessage());
                Platform.runLater(() -> {
                    afficherMessageBot("❌ Erreur d'analyse: " + e.getMessage());
                    scrollChatToBottom();
                });
            }
        }).start();
    }

    private void displayAnalysisResult(String response) {
        // ✅ PARSER LES CHAMPS
        VBox resultBox = new VBox(8);
        resultBox.setStyle("-fx-border-color: #d4af37; " +
                "-fx-border-width: 2; " +
                "-fx-padding: 12; " +
                "-fx-background-color: #1a1a1a; " +
                "-fx-border-radius: 8;");
        resultBox.setPadding(new Insets(10));

        String titre = extractField(response, "TITRE");
        String epoque = extractField(response, "ÉPOQUE");
        String origine = extractField(response, "ORIGINE");
        String materiaux = extractField(response, "MATÉRIAUX");
        String description = extractField(response, "DESCRIPTION");
        String importance = extractField(response, "IMPORTANCE");

        // ✅ TITRE
        Label titleLabel = new Label("🏛️ " + titre);
        titleLabel.setStyle("-fx-font-size: 14; -fx-font-weight: bold; -fx-text-fill: #d4af37;");
        titleLabel.setWrapText(true);

        // ✅ ÉPOQUE
        Label epoqueLabel = new Label("⏰ Époque: " + epoque);
        epoqueLabel.setStyle("-fx-font-size: 11; -fx-text-fill: #999;");
        epoqueLabel.setWrapText(true);

        // ✅ ORIGINE
        Label origineLabel = new Label("🌍 Origine: " + origine);
        origineLabel.setStyle("-fx-font-size: 11; -fx-text-fill: #999;");
        origineLabel.setWrapText(true);

        // ✅ MATÉRIAUX
        Label materiauLabel = new Label("🔨 Matériaux: " + materiaux);
        materiauLabel.setStyle("-fx-font-size: 11; -fx-text-fill: #999;");
        materiauLabel.setWrapText(true);

        // ✅ DESCRIPTION
        Label descLabel = new Label("📝 Description:\n" + description);
        descLabel.setStyle("-fx-font-size: 10; -fx-text-fill: #ddd;");
        descLabel.setWrapText(true);

        // ✅ IMPORTANCE
        Label importanceLabel = new Label("✨ Importance:\n" + importance);
        importanceLabel.setStyle("-fx-font-size: 10; -fx-text-fill: #d4af37;");
        importanceLabel.setWrapText(true);

        resultBox.getChildren().addAll(
                titleLabel,
                new Label(""),
                epoqueLabel,
                origineLabel,
                materiauLabel,
                descLabel,
                importanceLabel
        );

        HBox containerBox = new HBox();
        containerBox.setPadding(new Insets(10, 20, 10, 20));
        containerBox.setStyle("-fx-alignment: center-left;");
        containerBox.getChildren().add(resultBox);

        chatContainer.getChildren().add(containerBox);
        scrollChatToBottom();
    }

    private String extractField(String text, String fieldName) {
        try {
            String pattern = fieldName + ":";
            int startIdx = text.indexOf(pattern);

            if (startIdx == -1) {
                return "N/A";
            }

            startIdx += pattern.length();
            int endIdx = text.indexOf("\n", startIdx);

            if (endIdx == -1) {
                endIdx = text.length();
            }

            return text.substring(startIdx, endIdx).trim();
        } catch (Exception e) {
            return "N/A";
        }
    }

    // ============================
    // 💬 MESSAGES CHATBOT
    // ============================

    @FXML
    public void handleSendMessage() {
        String message = messageInput.getText().trim();

        if (message.isEmpty()) {
            return;
        }

        afficherMessageUtilisateur(message);
        messageInput.clear();

        if (imageActuelle != null) {
            analyserImageAvecIA();
        } else {
            afficherMessageBot("📤 Veuillez d'abord uploader une image!");
        }
    }

    private void afficherMessageUtilisateur(String message) {
        HBox messageBox = new HBox();
        messageBox.setPadding(new Insets(10, 20, 10, 20));

        Label label = new Label(message);
        label.setStyle("-fx-background-color: #d4af37; " +
                "-fx-text-fill: #1a1a1a; " +
                "-fx-padding: 10; " +
                "-fx-border-radius: 8; " +
                "-fx-font-size: 11;");
        label.setMaxWidth(350);
        label.setWrapText(true);

        messageBox.setStyle("-fx-alignment: center-right;");
        messageBox.getChildren().add(label);

        chatContainer.getChildren().add(messageBox);
        scrollChatToBottom();
    }

    private void afficherMessageBot(String message) {
        HBox messageBox = new HBox();
        messageBox.setPadding(new Insets(10, 20, 10, 20));

        Label label = new Label(message);
        label.setStyle("-fx-background-color: #0a0a0a; " +
                "-fx-text-fill: #999; " +
                "-fx-padding: 10; " +
                "-fx-border-radius: 8; " +
                "-fx-font-size: 11;");
        label.setMaxWidth(380);
        label.setWrapText(true);

        messageBox.setStyle("-fx-alignment: center-left;");
        messageBox.getChildren().add(label);

        chatContainer.getChildren().add(messageBox);
        scrollChatToBottom();
    }

    @FXML
    public void handleClearChat() {
        chatContainer.getChildren().clear();
        chatImageView.setImage(null);
        imageInfoLabel.setText("📷 Aucune image uploadée");
        imageActuelle = null;
        System.out.println("🗑️ Chat effacé");
    }

    private void scrollChatToBottom() {
        if (chatScrollPane != null) {
            chatScrollPane.setVvalue(1.0);
        }
    }

    // ============================
    // 🔍 RECHERCHE
    // ============================

    @FXML
    public void handleSearch() {
        if (allObjets == null || allObjets.isEmpty()) return;

        String query = searchField.getText().toLowerCase().trim();

        if (query.isEmpty()) {
            displayObjets(allObjets);
            return;
        }

        List<Objet> filtered = allObjets.stream()
                .filter(o -> o.getNom().toLowerCase().contains(query) ||
                        (o.getDescription() != null && o.getDescription().toLowerCase().contains(query)) ||
                        (o.getEpoque() != null && o.getEpoque().toLowerCase().contains(query)) ||
                        (o.getOrigine() != null && o.getOrigine().toLowerCase().contains(query)))
                .toList();

        displayObjets(filtered);
        System.out.println("🔍 Recherche: " + filtered.size() + " résultats");
    }

    private void displayObjets(List<Objet> objets) {
        Platform.runLater(() -> {
            galerieContainer.getChildren().clear();
            for (Objet obj : objets) {
                VBox card = createObjetCard(obj);
                galerieContainer.getChildren().add(card);
            }
        });
    }

    // ============================
    // ❤️ FAVORIS
    // ============================

    private void displayFavoris() {
        Platform.runLater(() -> {
            favorisContainer.getChildren().clear();

            if (allObjets == null || allObjets.isEmpty()) {
                Label emptyLabel = new Label("❌ Aucun favori");
                emptyLabel.setStyle("-fx-text-fill: #999; -fx-font-size: 14;");
                favorisContainer.getChildren().add(emptyLabel);
                return;
            }

            for (Objet obj : allObjets) {
                if (favorites.contains(obj.getIdObjet())) {
                    VBox card = createObjetCard(obj);
                    favorisContainer.getChildren().add(card);
                }
            }
            favoritesCountLabel.setText("(" + favorites.size() + " objets)");
        });
    }

    // ============================
    // 🎴 CRÉER UNE CARTE OBJET
    // ============================

    private VBox createObjetCard(Objet obj) {
        VBox card = new VBox(10);
        card.setStyle("-fx-background-color: #0d0d0d; -fx-padding: 15; -fx-border-color: #d4af37; -fx-border-width: 2; -fx-border-radius: 8; -fx-cursor: hand; -fx-min-width: 250; -fx-max-width: 250;");

        card.setOnMouseClicked(e -> openObjetDetails(obj.getIdObjet()));

        // ✅ IMAGE
        ImageView imageView = new ImageView();
        imageView.setFitHeight(200);
        imageView.setFitWidth(220);
        imageView.setPreserveRatio(false);
        imageView.setStyle("-fx-cursor: hand;");

        new Thread(() -> {
            Image img = fetchImage(obj);
            Platform.runLater(() -> imageView.setImage(img));
        }).start();

        // ✅ TITRE
        Label titre = new Label(obj.getNom());
        titre.setStyle("-fx-font-size: 14; -fx-font-weight: bold; -fx-text-fill: #d4af37; -fx-wrap-text: true;");

        // ✅ ÉPOQUE
        Label epoque = new Label("⏰ " + (obj.getEpoque() != null ? obj.getEpoque() : "N/A"));
        epoque.setStyle("-fx-font-size: 11; -fx-text-fill: #999;");

        // ✅ ORIGINE
        Label origine = new Label("🌍 " + (obj.getOrigine() != null ? obj.getOrigine() : "N/A"));
        origine.setStyle("-fx-font-size: 11; -fx-text-fill: #999;");

        // ✅ BOUTON VOIR DÉTAILS
        Button viewBtn = new Button("👁️ VOIR DÉTAILS");
        viewBtn.setStyle("-fx-background-color: #d4af37; -fx-text-fill: #0d0d0d; -fx-padding: 8 15; -fx-font-weight: bold; -fx-cursor: hand; -fx-background-radius: 5;");
        viewBtn.setOnAction(e -> {
            e.consume();
            openObjetDetails(obj.getIdObjet());
        });

        // ✅ BOUTON FAVORI
        Button favBtn = new Button(favorites.contains(obj.getIdObjet()) ? "❤️ FAVORI" : "🤍 AJOUTER");
        favBtn.setStyle("-fx-background-color: " + (favorites.contains(obj.getIdObjet()) ? "#ff6b6b" : "#666") + "; -fx-text-fill: white; -fx-padding: 8 15; -fx-font-weight: bold; -fx-cursor: hand; -fx-background-radius: 5;");
        favBtn.setOnAction(e -> {
            e.consume();
            toggleFavorite(obj.getIdObjet(), favBtn);
            displayFavoris();
        });

        card.getChildren().addAll(imageView, titre, epoque, origine, viewBtn, favBtn);
        return card;
    }

    private void toggleFavorite(int objId, Button btn) {
        if (favorites.contains(objId)) {
            favorites.remove(objId);
            btn.setText("🤍 AJOUTER");
            btn.setStyle("-fx-background-color: #666; -fx-text-fill: white; -fx-padding: 8 15; -fx-font-weight: bold; -fx-cursor: hand; -fx-background-radius: 5;");
            System.out.println("❌ Retiré des favoris");
        } else {
            favorites.add(objId);
            btn.setText("❤️ FAVORI");
            btn.setStyle("-fx-background-color: #ff6b6b; -fx-text-fill: white; -fx-padding: 8 15; -fx-font-weight: bold; -fx-cursor: hand; -fx-background-radius: 5;");
            System.out.println("✅ Ajouté aux favoris");
        }
        toggleFavoritesButton.setText("❤️ FAVORIS (" + favorites.size() + ")");
    }

    // ============================
    // 👁️ VOIR DÉTAILS
    // ============================

    private void openObjetDetails(int objId) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/ObjetDetailsView.fxml"));

            if (loader.getLocation() == null) {
                System.err.println("❌ Fichier ObjetDetailsView.fxml introuvable!");
                return;
            }

            Parent root = loader.load();
            ObjetDetailsController controller = loader.getController();
            if (controller != null) {
                controller.loadObjet(objId);
            }

            Stage stage = (Stage) galerieContainer.getScene().getWindow();
            stage.getScene().setRoot(root);
            System.out.println("✅ Chargement détails objet ID=" + objId);
        } catch (IOException e) {
            System.err.println("❌ Erreur: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ✅ FETCH IMAGE - GESTION D'ERREUR COMPLÈTE
    private Image fetchImage(Objet obj) {
        try {
            // ✅ VÉRIFIER SI L'OBJET A DES MÉDIAS
            if (obj.getMedias() != null && !obj.getMedias().isEmpty()) {
                String imageUrl = obj.getMedias().get(0).getLienFichier();

                if (imageUrl != null && !imageUrl.isEmpty()) {
                    // ✅ SI C'EST UNE URL HTTP
                    if (imageUrl.startsWith("http")) {
                        try {
                            URL url = new URL(imageUrl);
                            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                            conn.setRequestProperty("User-Agent", "Mozilla/5.0");
                            conn.setConnectTimeout(5000);
                            conn.setReadTimeout(5000);

                            int responseCode = conn.getResponseCode();
                            if (responseCode == 200) {
                                try (InputStream is = conn.getInputStream()) {
                                    return new Image(is);
                                }
                            } else {
                                System.out.println("⚠️ URL retourne " + responseCode + ": " + imageUrl);
                                return placeholderImage;
                            }
                        } catch (Exception e) {
                            System.out.println("⚠️ Erreur HTTP: " + e.getMessage());
                            return placeholderImage;
                        }
                    }
                    // ✅ SI C'EST UN CHEMIN LOCAL
                    else if (imageUrl.startsWith("/") || imageUrl.contains("\\")) {
                        try {
                            return new Image("file:" + imageUrl);
                        } catch (Exception e) {
                            System.out.println("⚠️ Erreur fichier local: " + e.getMessage());
                            return placeholderImage;
                        }
                    }
                }
            }

            // ✅ PAR DÉFAUT - RETOURNER L'IMAGE PLACEHOLDER
            System.out.println("⚠️ Pas d'image pour: " + obj.getNom());
            return placeholderImage;

        } catch (Exception e) {
            System.out.println("❌ Erreur fetchImage: " + e.getMessage());
            return placeholderImage;
        }
    }

    // ============================
    // 📊 STATS
    // ============================

    private void updateStats() {
        if (allObjets != null) {
            collectionCount.setText(allObjets.size() + " objet(s)");
            statsLabel.setText("📊 Objets en collection: " + allObjets.size());
        }
    }

    // ============================
    // 🎛️ CONTRÔLES UI
    // ============================

    @FXML
    public void handleToggleFavorites() {
        if (favoritesSection.isVisible()) {
            favoritesSection.setVisible(false);
            favoritesSection.setManaged(false);
            displayObjets(allObjets);
        } else {
            favoritesSection.setVisible(true);
            favoritesSection.setManaged(true);
            galerieContainer.getChildren().clear();
            displayFavoris();
        }
    }

    @FXML
    public void handleCloseFavorites() {
        favoritesSection.setVisible(false);
        favoritesSection.setManaged(false);
        displayObjets(allObjets);
    }

    @FXML public void handleGalerie() {
        favoritesSection.setVisible(false);
        favoritesSection.setManaged(false);
        displayObjets(allObjets);
    }

    @FXML public void handleToggleChat() {
        if (chatSidebar != null) {
            chatSidebar.setVisible(!chatSidebar.isVisible());
            chatSidebar.setManaged(chatSidebar.isVisible());
        }
    }

    @FXML public void handleCloseChatbot() {
        if (chatSidebar != null) {
            chatSidebar.setVisible(false);
            chatSidebar.setManaged(false);
        }
    }

    @FXML public void handleAdmin() {
        System.out.println("🔧 Admin");
    }

    @FXML public void handleReset() {
        searchField.clear();
        displayObjets(allObjets);
    }
}