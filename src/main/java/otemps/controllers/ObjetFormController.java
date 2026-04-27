package otemps.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;
import otemps.entites.Categorie;
import otemps.entites.Objet;
import otemps.services.CategorieService;
import otemps.services.ObjetService;

import java.sql.SQLException;

public class ObjetFormController {

    @FXML private Label lblTitre, lblMessage;
    @FXML private TextField tfNom, tfEpoque, tfOrigine, tfMateriaux;
    @FXML private ComboBox<Categorie> cbCategorie;
    @FXML private TextArea taDescription;
    @FXML private Button btnEnregistrer;

    private ObjetService objetService = new ObjetService();
    private CategorieService categorieService = new CategorieService();
    private Objet currentObjet = null;

    @FXML
    public void initialize() {
        setupProComboBox();
        loadCategories();
    }

    private void setupProComboBox() {
        // 1. Définir comment l'objet Categorie est converti en texte (pour le champ fermé)
        cbCategorie.setConverter(new StringConverter<Categorie>() {
            @Override
            public String toString(Categorie cat) {
                return (cat == null) ? "" : cat.getNomCategorie();
            }
            @Override
            public Categorie fromString(String s) { return null; }
        });


        cbCategorie.setCellFactory(lv -> new ListCell<Categorie>() {
            @Override
            protected void updateItem(Categorie item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    // On crée un petit design : Nom en gras + petite description si besoin
                    VBox container = new VBox(2);
                    Label nameLabel = new Label(item.getNomCategorie());
                    nameLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #2c3e50;");

                    container.getChildren().add(nameLabel);
                    setGraphic(container);
                }
            }
        });

        // Style CSS rapide pour la combo
        cbCategorie.setStyle("-fx-background-radius: 5; -fx-border-radius: 5;");
    }

    private void loadCategories() {
        cbCategorie.getItems().setAll(categorieService.afficher());
    }

    public void loadObjet(Objet objet) {
        this.currentObjet = objet;
        if (lblTitre != null) lblTitre.setText("Modifier l'objet");

        tfNom.setText(objet.getNom());
        tfEpoque.setText(objet.getEpoque());
        tfOrigine.setText(objet.getOrigine());
        tfMateriaux.setText(objet.getMateriaux());
        taDescription.setText(objet.getDescription());

        for (Categorie cat : cbCategorie.getItems()) {
            if (cat.getIdCategorie() == objet.getIdCategorie()) {
                cbCategorie.setValue(cat);
                break;
            }
        }
    }

    @FXML
    public void handleEnregistrer() {
        if (tfNom.getText().isEmpty() || cbCategorie.getValue() == null) {
            showAlert("Champs obligatoires", "Veuillez saisir au moins le nom et la catégorie.");
            return;
        }

        try {
            if (currentObjet == null) {
                Objet newObj = new Objet();
                fillData(newObj);
                objetService.ajouter(newObj);
                showNotification("✓ Objet ajouté avec succès");
                clearForm();
            } else {
                fillData(currentObjet);
                objetService.update(currentObjet);
                showNotification("✓ Objet mis à jour");
            }
        } catch (SQLException e) {
            showAlert("Erreur SQL", e.getMessage());
        }
    }

    private void fillData(Objet obj) {
        obj.setNom(tfNom.getText());
        obj.setEpoque(tfEpoque.getText());
        obj.setOrigine(tfOrigine.getText());
        obj.setMateriaux(tfMateriaux.getText());
        obj.setDescription(taDescription.getText());
        obj.setIdCategorie(cbCategorie.getValue().getIdCategorie());
    }

    private void showNotification(String msg) {
        lblMessage.setText(msg);
        lblMessage.setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold;");
    }

    @FXML
    public void handleAnnuler() {
        clearForm();
        lblMessage.setText("");
    }

    private void clearForm() {
        tfNom.clear();
        tfEpoque.clear();
        tfOrigine.clear();
        tfMateriaux.clear();
        taDescription.clear();
        cbCategorie.setValue(null);
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}