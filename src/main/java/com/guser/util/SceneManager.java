package com.guser.util;

import com.guser.MainApp;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public final class SceneManager {

    private SceneManager() {
    }

    public static void switchScene(Node node, String fxmlPath, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(MainApp.class.getResource(fxmlPath));
            Parent root = loader.load();
            Scene scene = new Scene(root);
            scene.getStylesheets().add(MainApp.class.getResource("/com/guser/app.css").toExternalForm());

            Stage stage = (Stage) node.getScene().getWindow();
            stage.setTitle(title);
            stage.setScene(scene);
            stage.centerOnScreen();
        } catch (IOException exception) {
            throw new IllegalStateException("Impossible de charger la vue " + fxmlPath, exception);
        }
    }
}
