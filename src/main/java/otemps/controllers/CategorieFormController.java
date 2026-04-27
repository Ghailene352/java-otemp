package otemps.controllers;

import otemps.entites.Categorie;
import otemps.services.CategorieService;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.sql.SQLException;

public class CategorieFormController {

    @FXML private Label lblTitre;
    @FXML private TextField tfNom;
    @FXML private TextArea taDescription;
    @FXML private Button btnEnregistrer;
    @FXML private Label lblMessage;

    private CategorieService categorieService = new CategorieService();
    private Categorie currentCategorie;

    public void loadCategorie(Categorie categorie) {
        this.currentCategorie = categorie;
        lblTitre.setText("Modifier une catégorie");
        tfNom.setText(categorie.getNomCategorie());
        taDescription.setText(categorie.getDescription());
    }

    @FXML
    public void handleEnregistrer() {
        if (tfNom.getText().isEmpty()) {
            showAlert("Erreur", "Le nom est obligatoire!");
            return;
        }

        try {
            if (currentCategorie == null) {
                // Ajouter
                Categorie newCat = new Categorie();
                newCat.setNomCategorie(tfNom.getText());
                newCat.setDescription(taDescription.getText());

                int id = categorieService.ajouter(newCat);
                if (id > 0) {
                    showAlert("Succès", "Catégorie ajoutée avec succès!");
                    lblMessage.setText("✓ Catégorie ajoutée");
                    lblMessage.setStyle("-fx-text-fill: #27ae60;");
                    clearForm();
                }
            } else {
                // Modifier
                currentCategorie.setNomCategorie(tfNom.getText());
                currentCategorie.setDescription(taDescription.getText());

                categorieService.update(currentCategorie);
                showAlert("Succès", "Catégorie modifiée avec succès!");
                lblMessage.setText("✓ Catégorie modifiée");
                lblMessage.setStyle("-fx-text-fill: #27ae60;");
            }
        } catch (SQLException e) {
            showAlert("Erreur", "Erreur: " + e.getMessage());
            System.out.println("Erreur enregistrement: " + e.getMessage());
        }
    }

    @FXML
    public void handleAnnuler() {
        Stage stage = (Stage) btnEnregistrer.getScene().getWindow();
        stage.close();
    }

    private void clearForm() {
        tfNom.clear();
        taDescription.clear();
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}