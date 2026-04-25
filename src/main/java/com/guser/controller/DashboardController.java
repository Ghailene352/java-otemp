package com.guser.controller;

import com.guser.dao.UserDao;
import com.guser.model.User;
import com.guser.util.SceneManager;
import com.guser.util.SessionManager;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;

import java.sql.SQLException;

public class DashboardController {

    @FXML
    private Label totalUsersLabel;

    @FXML
    private Label verifiedUsersLabel;

    @FXML
    private Label todayUsersLabel;

    @FXML
    private Label welcomeLabel;

    @FXML
    private Label roleLabel;

    private final UserDao userDao = new UserDao();

    @FXML
    public void initialize() {
        User currentUser = SessionManager.getCurrentUser();
        if (currentUser != null) {
            welcomeLabel.setText("Bienvenue, " + currentUser.getNomComplet());
            roleLabel.setText("Role : " + currentUser.getRole());
        }
        refreshMetrics();
    }

    @FXML
    private void ouvrirUtilisateurs() {
        SceneManager.switchScene(totalUsersLabel, "/com/guser/user-view.fxml", "Gestion des utilisateurs");
    }

    @FXML
    private void deconnexion() {
        SessionManager.clear();
        SceneManager.switchScene(totalUsersLabel, "/com/guser/login-view.fxml", "Guser");
    }

    private void refreshMetrics() {
        try {
            totalUsersLabel.setText(String.valueOf(userDao.countAll()));
            verifiedUsersLabel.setText("-");
            todayUsersLabel.setText("-");
        } catch (SQLException exception) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Erreur");
            alert.setHeaderText(null);
            alert.setContentText("Impossible de charger les statistiques : " + exception.getMessage());
            alert.showAndWait();
        }
    }
}
