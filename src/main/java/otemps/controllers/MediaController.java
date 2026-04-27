package otemps.controllers;

import otemps.entites.Media;
import otemps.entites.Objet;
import otemps.services.MediaService;
import otemps.services.ObjetService;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.List;

public class MediaController {

    @FXML private TableView<Media> tableMedias;
    @FXML private TableColumn<Media, Integer> colId;
    @FXML private TableColumn<Media, String> colObjet;
    @FXML private TableColumn<Media, String> colType;
    @FXML private TableColumn<Media, String> colFichier;
    @FXML private ComboBox<Objet> cbObjet;
    @FXML private Label lblTotal;
    @FXML private Button btnUpload;

    private MediaService mediaService = new MediaService();
    private ObjetService objetService = new ObjetService();

    @FXML
    public void initialize() {
        setupColumns();
        loadObjets();
        loadMedias();
    }

    private void setupColumns() {
        colId.setCellValueFactory(new PropertyValueFactory<>("idMedia"));
        colType.setCellValueFactory(new PropertyValueFactory<>("type"));
        colFichier.setCellValueFactory(new PropertyValueFactory<>("lienFichier"));
    }

    private void loadObjets() {
        List<Objet> objets = objetService.afficher();
        cbObjet.getItems().addAll(objets);
    }

    private void loadMedias() {
        List<Media> medias = mediaService.afficher();
        tableMedias.getItems().clear();
        tableMedias.getItems().addAll(medias);
        lblTotal.setText("Total: " + medias.size() + " médias");
    }

    @FXML
    public void handleUpload() {
        openFormWindow(null, "Ajouter un média");
        loadMedias();
    }

    @FXML
    public void handleFiltrer() {
        if (cbObjet.getValue() != null) {
            List<Media> medias = mediaService.getByObjet(cbObjet.getValue().getIdObjet());
            tableMedias.getItems().clear();
            tableMedias.getItems().addAll(medias);
            lblTotal.setText("Total: " + medias.size() + " médias");
        }
    }

    @FXML
    public void handleRetour() {
        Stage stage = (Stage) btnUpload.getScene().getWindow();
        stage.close();
    }

    private void openFormWindow(Media media, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/admin/MediaFormView.fxml"));
            Parent root = loader.load();

            Stage stage = new Stage();
            stage.setTitle(title);
            stage.setScene(new Scene(root, 600, 400));
            stage.showAndWait();
        } catch (IOException e) {
            System.out.println("Erreur: " + e.getMessage());
        }
    }
}