package com.guser.controller;

import com.guser.dao.UserDao;
import com.guser.model.User;
import com.guser.service.EmailService;
import com.guser.service.FaceIdService;
import com.guser.util.SceneManager;
import com.guser.util.SessionManager;
import jakarta.mail.MessagingException;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.regex.Pattern;

public class UserController {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final Pattern NAME_PATTERN = Pattern.compile("^[\\p{L}' -]+$");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    private static final Pattern PASSWORD_PATTERN = Pattern.compile("^(?=.*[A-Za-z])(?=.*\\d).{8,}$");

    @FXML
    private TextField nomField;

    @FXML
    private TextField prenomsField;

    @FXML
    private TextField emailField;

    @FXML
    private TextField telephoneField;

    @FXML
    private PasswordField motDePasseField;

    @FXML
    private ComboBox<String> roleBox;

    @FXML
    private TextField rechercheField;

    @FXML
    private Button triNomButton;

    @FXML
    private Label totalUsersLabel;

    @FXML
    private FlowPane userCardsContainer;

    private final UserDao userDao = new UserDao();
    private final EmailService emailService = new EmailService();
    private final FaceIdService faceIdService = new FaceIdService();
    private final ObservableList<User> users = FXCollections.observableArrayList();
    private User selectedUser;
    private boolean triNomCroissant = true;

    @FXML
    public void initialize() {
        if (SessionManager.getCurrentUser() == null || !"ADMIN".equalsIgnoreCase(SessionManager.getCurrentUser().getRole())) {
            Platform.runLater(() -> SceneManager.switchScene(totalUsersLabel, "/com/guser/login-view.fxml", "Guser | Connexion"));
            return;
        }
        roleBox.setItems(FXCollections.observableArrayList("ADMIN", "UTILISATEUR"));
        roleBox.setValue("UTILISATEUR");
        rechercheField.textProperty().addListener((observable, oldValue, newValue) -> renderUserCards());
        refreshUsers();
    }

    @FXML
    private void ajouterUtilisateur() {
        if (!validateFields(null)) {
            return;
        }

        try {
            String verificationCode = generateVerificationCode();
            emailService.sendVerificationCode(
                    emailField.getText().trim(),
                    nomField.getText().trim() + " " + prenomsField.getText().trim(),
                    verificationCode
            );

            if (!demanderValidationCode(verificationCode)) {
                showError("Code de verification incorrect. L'utilisateur n'a pas ete ajoute.");
                return;
            }

            userDao.insert(new User(
                    nomField.getText().trim(),
                    prenomsField.getText().trim(),
                    emailField.getText().trim(),
                    telephoneField.getText().trim(),
                    motDePasseField.getText(),
                    roleBox.getValue()
            ));
            refreshUsers();
            clearFields();
            showInfo("Utilisateur ajoute avec succes.");
        } catch (MessagingException exception) {
            showError("Impossible d'envoyer l'email de verification. Verifiez la configuration SMTP dans MailConfig.java.");
        } catch (SQLException exception) {
            showError("Erreur lors de l'ajout de l'utilisateur : " + exception.getMessage());
        }
    }

    @FXML
    private void modifierUtilisateur() {
        if (selectedUser == null) {
            showError("Selectionnez une carte utilisateur a modifier.");
            return;
        }

        if (!validateFields(selectedUser)) {
            return;
        }

        selectedUser.setNom(nomField.getText().trim());
        selectedUser.setPrenoms(prenomsField.getText().trim());
        selectedUser.setEmail(emailField.getText().trim());
        selectedUser.setTelephone(telephoneField.getText().trim());
        selectedUser.setMotDePasse(motDePasseField.getText());
        selectedUser.setRole(roleBox.getValue());

        try {
            userDao.update(selectedUser);
            refreshUsers();
            clearFields();
            showInfo("Utilisateur modifie avec succes.");
        } catch (SQLException exception) {
            showError("Erreur lors de la modification : " + exception.getMessage());
        }
    }

    @FXML
    private void supprimerUtilisateur() {
        if (selectedUser == null) {
            showError("Selectionnez une carte utilisateur a supprimer.");
            return;
        }

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmation");
        alert.setHeaderText(null);
        alert.setContentText("Supprimer l'utilisateur selectionne ?");
        Optional<ButtonType> result = alert.showAndWait();

        if (result.isEmpty() || result.get() != ButtonType.OK) {
            return;
        }

        try {
            userDao.delete(selectedUser.getId());
            refreshUsers();
            clearFields();
            showInfo("Utilisateur supprime avec succes.");
        } catch (SQLException exception) {
            showError("Erreur lors de la suppression : " + exception.getMessage());
        }
    }

    @FXML
    private void trierParNom() {
        triNomCroissant = !triNomCroissant;
        triNomButton.setText(triNomCroissant ? "Tri A-Z" : "Tri Z-A");
        renderUserCards();
    }

    @FXML
    private void deconnexion() {
        SessionManager.clear();
        SceneManager.switchScene(totalUsersLabel, "/com/guser/login-view.fxml", "Guser | Connexion");
    }

    @FXML
    private void enregistrerFaceId() {
        if (selectedUser == null) {
            showError("Selectionnez un utilisateur a enroler pour FaceID.");
            return;
        }
        if (!"ADMIN".equalsIgnoreCase(selectedUser.getRole())) {
            showError("FaceID de connexion directe est reserve aux comptes ADMIN.");
            return;
        }

        try {
            String template = faceIdService.captureTemplate();
            userDao.updateFaceTemplate(selectedUser.getId(), template);
            refreshUsers();
            showInfo("FaceID enregistre avec succes pour " + selectedUser.getNomComplet() + ".");
        } catch (IOException exception) {
            showError("Capture FaceID impossible : " + exception.getMessage());
        } catch (SQLException exception) {
            showError("Impossible d'enregistrer FaceID : " + exception.getMessage());
        }
    }

    @FXML
    private void supprimerFaceId() {
        if (selectedUser == null) {
            showError("Selectionnez un utilisateur dont FaceID doit etre supprime.");
            return;
        }

        try {
            userDao.clearFaceTemplate(selectedUser.getId());
            refreshUsers();
            showInfo("FaceID supprime pour " + selectedUser.getNomComplet() + ".");
        } catch (SQLException exception) {
            showError("Impossible de supprimer FaceID : " + exception.getMessage());
        }
    }

    private void refreshUsers() {
        try {
            users.setAll(userDao.findAll());
            totalUsersLabel.setText(users.size() + " utilisateurs");
            renderUserCards();
        } catch (SQLException exception) {
            showError("Connexion a la base impossible : " + exception.getMessage());
        }
    }

    private void renderUserCards() {
        userCardsContainer.getChildren().clear();
        List<User> visibleUsers = new ArrayList<>(users);
        String recherche = rechercheField.getText() == null ? "" : rechercheField.getText().trim().toLowerCase();
        visibleUsers.removeIf(user -> !recherche.isBlank() && !user.getNom().toLowerCase().contains(recherche));
        visibleUsers.sort(Comparator.comparing(user -> user.getNom().toLowerCase()));
        if (!triNomCroissant) {
            Collections.reverse(visibleUsers);
        }

        for (User user : visibleUsers) {
            userCardsContainer.getChildren().add(createUserCard(user));
        }
    }

    private VBox createUserCard(User user) {
        VBox card = new VBox(12);
        card.getStyleClass().add("user-card");
        if (selectedUser != null && selectedUser.getId() == user.getId()) {
            card.getStyleClass().add("user-card-selected");
        }
        card.setPadding(new Insets(18));
        card.setPrefWidth(290);

        Label nameLabel = new Label(user.getNomComplet());
        nameLabel.getStyleClass().add("event-card-title");

        Label roleBadge = new Label(user.getRole());
        roleBadge.getStyleClass().addAll("role-badge", "ADMIN".equalsIgnoreCase(user.getRole()) ? "role-admin" : "role-user");

        HBox header = new HBox(10, nameLabel, spacer(), roleBadge);

        Label emailLabel = createMetaLabel("Email : " + user.getEmail());
        Label phoneLabel = createMetaLabel("Telephone : " + user.getTelephone());
        Label roleLabel = createMetaLabel("Role : " + user.getRole());
        Label faceIdLabel = createMetaLabel("FaceID : " + (user.hasFaceTemplate() ? "Configure" : "Non configure"));
        Label dateLabel = createMetaLabel("Cree le : " + formatCreationDate(user));

        card.getChildren().addAll(header, emailLabel, phoneLabel, roleLabel, faceIdLabel, dateLabel);
        card.setOnMouseClicked(event -> selectUser(user));
        return card;
    }

    private Region spacer() {
        Region region = new Region();
        HBox.setHgrow(region, Priority.ALWAYS);
        return region;
    }

    private Label createMetaLabel(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("event-card-subtitle");
        label.setWrapText(true);
        return label;
    }

    private String formatCreationDate(User user) {
        return user.getDateCreation() == null ? "-" : DATE_FORMATTER.format(user.getDateCreation());
    }

    private void selectUser(User user) {
        selectedUser = user;
        nomField.setText(user.getNom());
        prenomsField.setText(user.getPrenoms());
        emailField.setText(user.getEmail());
        telephoneField.setText(user.getTelephone());
        motDePasseField.setText(user.getMotDePasse());
        roleBox.setValue(user.getRole());
        renderUserCards();
    }

    private boolean validateFields(User editingUser) {
        if (nomField.getText().isBlank()
                || prenomsField.getText().isBlank()
                || emailField.getText().isBlank()
                || telephoneField.getText().isBlank()
                || motDePasseField.getText().isBlank()
                || roleBox.getValue() == null || roleBox.getValue().isBlank()) {
            showError("Tous les champs sont obligatoires.");
            return false;
        }

        if (!NAME_PATTERN.matcher(nomField.getText().trim()).matches()) {
            showError("Le nom ne doit pas contenir de chiffres.");
            return false;
        }

        if (!NAME_PATTERN.matcher(prenomsField.getText().trim()).matches()) {
            showError("Le prenom ne doit pas contenir de chiffres.");
            return false;
        }

        if (!EMAIL_PATTERN.matcher(emailField.getText().trim()).matches()) {
            showError("L'email doit contenir un @ et un domaine valide.");
            return false;
        }

        try {
            String email = emailField.getText().trim();
            boolean emailExiste = editingUser == null
                    ? userDao.existsByEmail(email)
                    : userDao.existsByEmailExceptId(email, editingUser.getId());
            if (emailExiste) {
                showError("Cet email existe deja. Veuillez saisir un email unique.");
                return false;
            }
        } catch (SQLException exception) {
            showError("Erreur lors de la verification de l'unicite de l'email : " + exception.getMessage());
            return false;
        }

        if (!PASSWORD_PATTERN.matcher(motDePasseField.getText()).matches()) {
            showError("Le mot de passe doit contenir au moins 8 caracteres, avec des lettres et des chiffres.");
            return false;
        }

        return true;
    }

    private boolean demanderValidationCode(String expectedCode) {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Verification email");
        dialog.setHeaderText("Saisissez le code recu par email");
        dialog.setContentText("Code :");
        Optional<String> result = dialog.showAndWait();
        return result.isPresent() && expectedCode.equals(result.get().trim());
    }

    private String generateVerificationCode() {
        return String.format("%06d", new Random().nextInt(1_000_000));
    }

    private void clearFields() {
        selectedUser = null;
        nomField.clear();
        prenomsField.clear();
        emailField.clear();
        telephoneField.clear();
        motDePasseField.clear();
        roleBox.setValue("UTILISATEUR");
        renderUserCards();
    }

    private void showInfo(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Information");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Erreur");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
