package otemps.controllers;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.*;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Box;
import javafx.scene.transform.Rotate;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import otemps.entites.Categorie;
import otemps.entites.Media;
import otemps.entites.Objet;
import otemps.services.CategorieService;
import otemps.services.ObjetService;
import otemps.services.TranslationService;
import otemps.utils.PDFGenerator;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ObjetDetailsController {

    @FXML private Button btnBack, btnPrint, btnPDF, btnToggle3D;
    @FXML private Label titleLabel, mainTitleLabel, categorieLabel, epoqueLabel, origineLabel, materiauxLabel, descriptionLabel;
    @FXML private Label traductionStatus;
    @FXML private ImageView mainImageView;
    @FXML private HBox mediasContainer;
    @FXML private StackPane displayStack;
    @FXML private ComboBox<String> languageSelector;

    private ObjetService objetService = new ObjetService();
    private CategorieService categorieService = new CategorieService();
    private TranslationService translationService = new TranslationService();

    private int currentObjetId;
    private boolean is3DMode = false;
    private Objet currentObjet;
    private String descriptionSource;

    private double anchorX, anchorY;
    private double anchorAngleX = 0;
    private double anchorAngleY = 0;
    private final Rotate rotateX = new Rotate(0, Rotate.X_AXIS);
    private final Rotate rotateY = new Rotate(0, Rotate.Y_AXIS);

    private final String PLACEHOLDER_URL = "https://via.placeholder.com/700x400?text=Image+Indisponible";

    // ✅ MAPPING DES LANGUES
    private final Map<String, String> languageMap = new HashMap<>();

    @FXML
    public void initialize() {
        System.out.println("✅ ObjetDetailsController initialisé");

        // ✅ CONFIGURATION DES LANGUES
        languageMap.put("Français (Original)", "fr");
        languageMap.put("English", "en");
        languageMap.put("Español", "es");
        languageMap.put("Deutsch", "de");
        languageMap.put("Italiano", "it");

        if (languageSelector != null) {
            languageSelector.getItems().addAll(languageMap.keySet());
            languageSelector.getSelectionModel().selectFirst();
            System.out.println("✅ Langues chargées: " + languageMap.keySet());
        }
    }

    public void loadObjet(int idObjet) {
        this.currentObjetId = idObjet;
        new Thread(() -> {
            try {
                Objet objet = objetService.getById(idObjet);
                if (objet != null) {
                    this.currentObjet = objet;
                    this.descriptionSource = objet.getDescription();
                    Categorie cat = categorieService.getById(objet.getIdCategorie());
                    List<Media> medias = objet.getMedias();

                    Platform.runLater(() -> {
                        titleLabel.setText(objet.getNom());
                        mainTitleLabel.setText(objet.getNom());
                        descriptionLabel.setText(objet.getDescription() != null ? objet.getDescription() : "Aucune description");
                        epoqueLabel.setText(objet.getEpoque() != null ? objet.getEpoque() : "Non spécifiée");
                        origineLabel.setText(objet.getOrigine() != null ? objet.getOrigine() : "Non spécifiée");
                        materiauxLabel.setText(objet.getMateriaux() != null ? objet.getMateriaux() : "Non spécifiés");

                        if (cat != null) {
                            categorieLabel.setText(cat.getNomCategorie());
                            System.out.println("✅ Catégorie: " + cat.getNomCategorie());
                        } else {
                            categorieLabel.setText("Non spécifiée");
                        }

                        if (medias != null && !medias.isEmpty()) {
                            setMainImageAsync(medias.get(0).getLienFichier());
                            loadGallery(medias);
                        } else {
                            mainImageView.setImage(new Image(PLACEHOLDER_URL));
                        }
                    });
                }
            } catch (Exception e) {
                System.err.println("❌ Erreur loadObjet: " + e.getMessage());
                e.printStackTrace();
            }
        }).start();
    }

    @FXML
    public void handleToggle3D() {
        if (!is3DMode) {
            mainImageView.setVisible(false);
            btnToggle3D.setText("🖼️ RETOUR VUE PHOTO");
            setup3DScene(displayStack);
            is3DMode = true;
            System.out.println("✅ Mode 3D activé");
        } else {
            displayStack.getChildren().removeIf(node -> node instanceof SubScene);
            mainImageView.setVisible(true);
            btnToggle3D.setText("📦 VOIR CET OBJET EN 3D");
            is3DMode = false;
            System.out.println("✅ Mode photo activé");
        }
    }

    private void setup3DScene(StackPane container) {
        Image img = mainImageView.getImage();
        if (img == null) {
            System.err.println("❌ Image null pour 3D");
            return;
        }

        try {
            double imgWidth = 400;
            double imgHeight = (img.getHeight() / img.getWidth()) * imgWidth;

            Box canvas3D = new Box(imgWidth, imgHeight, 10);
            PhongMaterial material = new PhongMaterial();
            material.setDiffuseMap(img);
            material.setSpecularColor(Color.WHITE);
            canvas3D.setMaterial(material);

            Group root3D = new Group(canvas3D);
            root3D.getTransforms().addAll(rotateX, rotateY);

            container.setOnMousePressed(event -> {
                anchorX = event.getSceneX();
                anchorY = event.getSceneY();
                anchorAngleX = rotateX.getAngle();
                anchorAngleY = rotateY.getAngle();
            });

            container.setOnMouseDragged(event -> {
                rotateX.setAngle(anchorAngleX - (anchorY - event.getSceneY()));
                rotateY.setAngle(anchorAngleY + (anchorX - event.getSceneX()));
            });

            PointLight light = new PointLight(Color.WHITE);
            light.setTranslateZ(-500);
            light.setTranslateX(200);
            light.setTranslateY(-200);

            AmbientLight ambientLight = new AmbientLight(Color.color(0.9, 0.9, 0.9));
            root3D.getChildren().addAll(light, ambientLight);

            SubScene subScene = new SubScene(root3D, 800, 500, true, SceneAntialiasing.BALANCED);
            subScene.setFill(Color.web("#0d0d0d"));

            PerspectiveCamera camera = new PerspectiveCamera(true);
            camera.setNearClip(0.1);
            camera.setFarClip(10000.0);
            camera.setTranslateZ(-900);
            subScene.setCamera(camera);

            container.getChildren().add(subScene);
            System.out.println("✅ 3D créé avec succès - Image: " + imgWidth + "x" + imgHeight);
        } catch (Exception e) {
            System.err.println("❌ Erreur setup3D: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    public void handleExportPDF() {
        if (currentObjet == null) return;
        FileChooser fc = new FileChooser();
        fc.setTitle("Enregistrer la fiche de l'œuvre");
        fc.setInitialFileName(mainTitleLabel.getText() + ".pdf");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Fichier PDF", "*.pdf"));
        java.io.File file = fc.showSaveDialog(btnPDF.getScene().getWindow());
        if (file != null) {
            PDFGenerator.generatePDF(currentObjet, mainImageView.getImage(), file.getAbsolutePath());
            Alert alert = new Alert(AlertType.INFORMATION);
            alert.setTitle("✅ Exportation Réussie");
            alert.setContentText("Le PDF a été créé avec succès!");
            alert.showAndWait();
        }
    }

    private void setMainImageAsync(String url) {
        new Thread(() -> {
            Image img = fetchImage(url);
            Platform.runLater(() -> mainImageView.setImage(img));
        }).start();
    }

    private void loadGallery(List<Media> medias) {
        mediasContainer.getChildren().clear();
        for (Media m : medias) {
            ImageView thumb = new ImageView();
            thumb.setFitHeight(80);
            thumb.setFitWidth(80);
            thumb.setPreserveRatio(true);
            thumb.setStyle("-fx-cursor: hand; -fx-border-color: #d4af37; -fx-border-width: 2; -fx-border-radius: 5;");
            new Thread(() -> {
                Image img = fetchImage(m.getLienFichier());
                Platform.runLater(() -> thumb.setImage(img));
            }).start();
            thumb.setOnMouseClicked(e -> {
                mainImageView.setImage(thumb.getImage());
                if(is3DMode) handleToggle3D();
            });
            mediasContainer.getChildren().add(thumb);
        }
    }

    private Image fetchImage(String urlStr) {
        try {
            if (urlStr == null || urlStr.isEmpty()) return new Image(PLACEHOLDER_URL);

            if (urlStr.startsWith("http")) {
                URL url = new URL(urlStr);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestProperty("User-Agent", "Mozilla/5.0");
                try (InputStream is = conn.getInputStream()) {
                    return new Image(is);
                }
            }
            return new Image("file:" + urlStr);
        } catch (Exception e) {
            return new Image(PLACEHOLDER_URL);
        }
    }

    @FXML
    public void handleBack() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/fxml/HomeView.fxml"));
            Stage stage = (Stage) btnBack.getScene().getWindow();
            stage.getScene().setRoot(root);
        } catch (IOException e) {
            System.err.println("❌ Erreur handleBack: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    public void handlePrint() {
        System.out.println("🖨️ Impression de " + mainTitleLabel.getText());
    }

    // ✅ TRADUCTION
    @FXML
    public void onLanguageChange() {
        String selected = languageSelector.getValue();
        if (selected == null || descriptionSource == null) {
            System.err.println("❌ Langue ou description nulle");
            return;
        }

        if (selected.equals("Français (Original)")) {
            descriptionLabel.setText(descriptionSource);
            traductionStatus.setText("");
            System.out.println("🇫🇷 Français (Original)");
            return;
        }

        // ✅ AFFICHER STATUT
        traductionStatus.setText("⏳ Traduction en cours...");
        descriptionLabel.setStyle("-fx-opacity: 0.6;");

        new Thread(() -> {
            String langCode = languageMap.get(selected);
            System.out.println("🌐 Traduction vers: " + selected + " (" + langCode + ")");

            String result = translationService.translate(descriptionSource, "fr", langCode);

            Platform.runLater(() -> {
                if (result.contains("❌")) {
                    // ✅ FALLBACK AU TEXTE ORIGINAL
                    traductionStatus.setText("⚠️ Traduction échouée, affichage du texte original");
                    descriptionLabel.setText(descriptionSource);
                    descriptionLabel.setStyle("-fx-opacity: 1.0;");
                    System.err.println("❌ Traduction échouée: " + result);
                } else {
                    // ✅ AFFICHER LA TRADUCTION
                    descriptionLabel.setText(result);
                    traductionStatus.setText("✅ Traduction réussie (" + selected + ")");
                    descriptionLabel.setStyle("-fx-opacity: 1.0;");
                    System.out.println("✅ Traduction OK");
                }
            });
        }).start();
    }
}