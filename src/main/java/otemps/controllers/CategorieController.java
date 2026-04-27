package otemps.controllers;

import otemps.entites.Categorie;
import otemps.services.CategorieService;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.List;

public class CategorieController {

    @FXML private TableView<Categorie> tableCategories;
    @FXML private TableColumn<Categorie, Integer> colId;
    @FXML private TableColumn<Categorie, String> colNom;
    @FXML private TableColumn<Categorie, String> colDescription;
    @FXML private Label lblTotal;
    @FXML private Button btnAjouter;

    private CategorieService categorieService = new CategorieService();

    @FXML
    public void initialize() {
        setupColumns();
        loadCategories();
    }

    private void setupColumns() {
        colId.setCellValueFactory(new PropertyValueFactory<>("idCategorie"));
        colNom.setCellValueFactory(new PropertyValueFactory<>("nomCategorie"));
        colDescription.setCellValueFactory(new PropertyValueFactory<>("description"));
    }

    private void loadCategories() {
        List<Categorie> categories = categorieService.afficher();
        tableCategories.getItems().clear();
        tableCategories.getItems().addAll(categories);
        lblTotal.setText("Total: " + categories.size() + " catégories");
    }

    @FXML
    public void handleAjouter() {
        openFormWindow(null, "Ajouter une catégorie");
        loadCategories();
    }

    @FXML
    public void handleRetour() {
        Stage stage = (Stage) btnAjouter.getScene().getWindow();
        stage.close();
    }

    private void openFormWindow(Categorie categorie, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/admin/CategorieFormView.fxml"));
            Parent root = loader.load();

            CategorieFormController controller = loader.getController();
            if (categorie != null) {
                controller.loadCategorie(categorie);
            }

            Stage stage = new Stage();
            stage.setTitle(title);
            stage.setScene(new Scene(root, 800, 500));
            stage.showAndWait();
        } catch (IOException e) {
            System.out.println("Erreur: " + e.getMessage());
        }
    }
}