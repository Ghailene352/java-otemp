package otemps.controllers;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import otemps.services.AIService;

import java.io.File;

public class ChatbotController {

    @FXML private VBox chatContainer;
    @FXML private TextField messageInput;
    @FXML private Button sendButton;
    @FXML private Button uploadButton;
    @FXML private ScrollPane scrollPane;

    private AIService aiService;
    private File imageActuelle;

    @FXML
    public void initialize() {
        System.out.println("✓ Chatbot initialisé");
        aiService = new AIService();

        sendButton.setOnAction(e -> handleSendMessage());
        uploadButton.setOnAction(e -> handleUploadImage());
        messageInput.setOnAction(e -> handleSendMessage());

        afficherMessageBienvenue();
    }

    private void afficherMessageBienvenue() {
        afficherMessageBot("👋 Bienvenue dans le Chatbot OTEMPS!\n\n" +
                "📤 COMMENT ÇA MARCHE:\n" +
                "1️⃣ Cliquez '📤 Upload Image'\n" +
                "2️⃣ Sélectionnez une photo d'objet patrimonial\n" +
                "3️⃣ L'IA génère automatiquement:\n" +
                "   ✅ Titre de l'objet\n" +
                "   ✅ Époque historique\n" +
                "   ✅ Origine géographique\n" +
                "   ✅ Matériaux utilisés\n" +
                "   ✅ Description détaillée\n" +
                "   ✅ Importance historique\n\n" +
                "🤖 Powered by Groq Vision (gratuit et rapide)\n" +
                "🔐 Vos données restent privées\n" +
                "⚡ Analyse en temps réel");
    }

    @FXML
    private void handleUploadImage() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Sélectionner une image d'objet patrimonial");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Images", "*.jpg", "*.jpeg", "*.png", "*.gif", "*.bmp"),
                new FileChooser.ExtensionFilter("Tous les fichiers", "*.*")
        );

        Stage stage = (Stage) uploadButton.getScene().getWindow();
        File selectedFile = fileChooser.showOpenDialog(stage);

        if (selectedFile != null) {
            imageActuelle = selectedFile;
            afficherImageDansChat(selectedFile);
            analyserImageAvecIA();
        }
    }

    private void analyserImageAvecIA() {
        afficherMessageBot("⏳ Analyse de l'image en cours avec Groq Vision...\n" +
                "(Cela peut prendre quelques secondes)\n\n" +
                "🔄 Veuillez patienter...");

        new Thread(() -> {
            try {
                String result = aiService.analyzeImage(imageActuelle);

                Platform.runLater(() -> {
                    afficherMessageBot(result);
                    scrollPane.setVvalue(1.0);
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    afficherMessageBot("❌ Erreur lors de l'analyse: " + e.getMessage());
                    scrollPane.setVvalue(1.0);
                });
            }
        }).start();
    }

    private void afficherImageDansChat(File file) {
        HBox imageBox = new HBox();
        imageBox.setPadding(new Insets(10, 20, 10, 20));

        try {
            Image image = new Image(file.toURI().toString());
            ImageView imageView = new ImageView(image);
            imageView.setFitHeight(250);
            imageView.setFitWidth(350);
            imageView.setPreserveRatio(true);
            imageView.setStyle("-fx-border-color: #d4af37; " +
                    "-fx-border-width: 3; " +
                    "-fx-border-radius: 8;");

            imageBox.setStyle("-fx-alignment: center-right;");
            imageBox.getChildren().add(imageView);
            chatContainer.getChildren().add(imageBox);

            scrollPane.setVvalue(1.0);

        } catch (Exception e) {
            System.err.println("✗ Erreur chargement image: " + e.getMessage());
            afficherMessageBot("❌ Erreur lors du chargement de l'image");
        }
    }

    @FXML
    private void handleSendMessage() {
        String message = messageInput.getText().trim();
        if (message.isEmpty()) return;

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
                "-fx-text-fill: white; " +
                "-fx-padding: 12; " +
                "-fx-border-radius: 8; " +
                "-fx-wrap-text: true; " +
                "-fx-font-size: 11;");
        label.setMaxWidth(400);
        label.setWrapText(true);

        messageBox.setStyle("-fx-alignment: center-right;");
        messageBox.getChildren().add(label);

        chatContainer.getChildren().add(messageBox);
        scrollPane.setVvalue(1.0);
    }

    private void afficherMessageBot(String message) {
        HBox messageBox = new HBox();
        messageBox.setPadding(new Insets(10, 20, 10, 20));

        Label label = new Label(message);
        label.setStyle("-fx-background-color: #f0f0f0; " +
                "-fx-text-fill: #1a1a1a; " +
                "-fx-padding: 12; " +
                "-fx-border-radius: 8; " +
                "-fx-wrap-text: true; " +
                "-fx-font-size: 11; " +
                "-fx-line-spacing: 2;");
        label.setMaxWidth(500);
        label.setWrapText(true);

        messageBox.setStyle("-fx-alignment: center-left;");
        messageBox.getChildren().add(label);

        chatContainer.getChildren().add(messageBox);
        scrollPane.setVvalue(1.0);
    }
}