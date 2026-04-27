package otemps.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;
import otemps.entites.Categorie;
import otemps.entites.Objet;
import otemps.services.CategorieService;
import otemps.services.ObjetService;

import java.io.IOException;
import java.util.List;

public class ObjetController {

    @FXML private TableView<Objet> tableObjets;
    @FXML private TableColumn<Objet, Integer> colId;
    @FXML private TableColumn<Objet, String> colNom;
    @FXML private TableColumn<Objet, String> colCategorie;
    @FXML private TableColumn<Objet, String> colEpoque;
    @FXML private TableColumn<Objet, String> colOrigine;
    @FXML private TextField tfRecherche;
    @FXML private ComboBox<Categorie> cbCategorie;
    @FXML private Label lblTotal;
    @FXML private Button btnAjouter;

    private ObjetService objetService = new ObjetService();
    private CategorieService categorieService = new CategorieService();

    @FXML
    public void initialize() {
        setupColumns();
        loadCategories();
        loadObjets();
    }

    private void setupColumns() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id")); // Vérifie si c'est "id" ou "idObjet" dans ton entité
        colNom.setCellValueFactory(new PropertyValueFactory<>("nom"));
        colEpoque.setCellValueFactory(new PropertyValueFactory<>("epoque"));
        colOrigine.setCellValueFactory(new PropertyValueFactory<>("origine"));
    }

    private void loadCategories() {
        List<Categorie> categories = categorieService.afficher();
        cbCategorie.getItems().addAll(categories);
    }

    private void loadObjets() {
        List<Objet> objets = objetService.afficher();
        tableObjets.getItems().clear();
        tableObjets.getItems().addAll(objets);
        lblTotal.setText("Total: " + objets.size() + " objets");
    }

    @FXML
    public void handleAjouter() {
        openFormWindow(null, "Ajouter un objet");
        loadObjets();
    }

    @FXML
    public void handleFiltrer() {
        String recherche = tfRecherche.getText().toLowerCase();
        List<Objet> results;

        if (!recherche.isEmpty()) {
            results = objetService.search(recherche);
        } else if (cbCategorie.getValue() != null) {
            // Utilise getId() ou getIdCategorie() selon ce qu'on a mis dans l'entité
            results = objetService.getByCategorie(cbCategorie.getValue().getIdCategorie());
        } else {
            results = objetService.afficher();
        }

        tableObjets.getItems().clear();
        tableObjets.getItems().addAll(results);
        lblTotal.setText("Total: " + results.size() + " objets");
    }

    @FXML
    public void handleRetour() {
        Stage stage = (Stage) btnAjouter.getScene().getWindow();
        stage.close();
    }

    private void openFormWindow(Objet objet, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/admin/ObjetFormView.fxml"));
            Parent root = loader.load();

            ObjetFormController controller = loader.getController();
            if (objet != null) {
          //      controller.loadbjet(objet);
            }

            Stage stage = new Stage();
            stage.setTitle(title);
            stage.setScene(new Scene(root, 900, 800));
            stage.showAndWait();
        } catch (IOException e) {
            System.out.println("Erreur: " + e.getMessage());
        }// catch (SQLException e) {
           // throw new RuntimeException(e);
        }
    }

