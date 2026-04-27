package otemps.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import otemps.entites.Objet;
import otemps.services.ObjetService;

import java.io.IOException;
import java.util.List;

public class GalerieController {

    @FXML private FlowPane galeryContainer;
    @FXML private TextField searchField;

    private ObjetService objetService = new ObjetService();
    private final String PLACEHOLDER = "https://via.placeholder.com/250x200?text=Image+Indisponible";

    @FXML
    public void initialize() {
        loadGalery();
    }

    public void loadGalery() {
        if (galeryContainer == null) return;
        galeryContainer.getChildren().clear();

        List<Objet> objets = objetService.afficher();
        if (objets != null) {
            for (Objet objet : objets) {
                galeryContainer.getChildren().add(createObjetCard(objet));
            }
        }
    }

    private VBox createObjetCard(Objet objet) {
        VBox card = new VBox(10);
        card.setAlignment(javafx.geometry.Pos.CENTER);
        card.setStyle("-fx-border-color: #c5a059; -fx-border-radius: 10; -fx-padding: 15; " +
                "-fx-background-color: white; -fx-cursor: hand; " +
                "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 10, 0, 0, 5);");
        card.setMinWidth(260);
        card.setPrefHeight(320);

        // Image
        ImageView imageView = new ImageView();
        imageView.setFitHeight(180);
        imageView.setFitWidth(230);
        imageView.setPreserveRatio(true);

        try {
            if (objet.getMedias() != null && !objet.getMedias().isEmpty()) {
                String path = objet.getMedias().get(0).getLienFichier();
                Image img = path.startsWith("http") ? new Image(path, true) : new Image("file:" + path);
                imageView.setImage(img);
            } else {
                imageView.setImage(new Image(PLACEHOLDER));
            }
        } catch (Exception e) {
            imageView.setImage(new Image(PLACEHOLDER));
        }

        // Titre
        Label titleLabel = new Label(objet.getNom().toUpperCase());
        titleLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #2c2c2c; -fx-text-alignment: center;");
        titleLabel.setWrapText(true);

        // ============================================================
        // CLIC SUR LA CARTE -> DIRECTION PAGE DETAILS
        // ============================================================
        card.setOnMouseClicked(event -> {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/ShowView.fxml"));
                Parent root = loader.load();

                // Récupération du contrôleur ShowController
                ShowController controller = loader.getController();

                // On passe l'ID de l'objet sélectionné
                controller.loadObjet(objet.getIdObjet());

                // On change la racine de la scène actuelle
                Stage stage = (Stage) galeryContainer.getScene().getWindow();
                stage.getScene().setRoot(root);

            } catch (IOException e) {
                System.err.println("Erreur de navigation : " + e.getMessage());
                e.printStackTrace();
            }
        });

        card.getChildren().addAll(imageView, titleLabel);
        return card;
    }

    @FXML
    public void handleSearch() {
        String query = searchField.getText().toLowerCase().trim();
        galeryContainer.getChildren().clear();
        List<Objet> results = query.isEmpty() ? objetService.afficher() : objetService.search(query);
        if (results != null) {
            for (Objet o : results) {
                galeryContainer.getChildren().add(createObjetCard(o));
            }
        }
    }
}