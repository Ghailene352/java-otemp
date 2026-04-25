package com.guser;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.TextArea;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class MainApp extends Application {

    @Override
    public void start(Stage stage) {
        try {
            FXMLLoader loader = new FXMLLoader(MainApp.class.getResource("/com/guser/login-view.fxml"));
            Scene scene = new Scene(loader.load());
            scene.getStylesheets().add(MainApp.class.getResource("/com/guser/app.css").toExternalForm());
            stage.setTitle("Guser | Connexion");
            stage.setScene(scene);
            stage.setResizable(true);
            stage.setMinWidth(980);
            stage.setMinHeight(680);
            stage.show();
        } catch (Exception exception) {
            stage.setTitle("Guser | Erreur");
            stage.setScene(new Scene(new VBox(), 640, 240));
            stage.show();

            String message = buildErrorMessage(exception);
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Erreur de demarrage");
            alert.setHeaderText("Impossible de lancer l'application");
            alert.setContentText(message);

            TextArea details = new TextArea(message);
            details.setEditable(false);
            details.setWrapText(true);
            details.setPrefColumnCount(60);
            details.setPrefRowCount(12);
            alert.getDialogPane().setExpandableContent(details);
            alert.showAndWait();
        }
    }

    private String buildErrorMessage(Exception exception) {
        Throwable current = exception;
        StringBuilder builder = new StringBuilder();

        while (current != null) {
            if (builder.length() > 0) {
                builder.append("\nCause: ");
            }
            builder.append(current.getClass().getSimpleName());
            if (current.getMessage() != null && !current.getMessage().isBlank()) {
                builder.append(" - ").append(current.getMessage());
            }
            current = current.getCause();
        }

        return builder.toString();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
