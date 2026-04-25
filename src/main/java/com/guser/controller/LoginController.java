package com.guser.controller;

import com.github.sarxos.webcam.Webcam;
import com.guser.dao.UserDao;
import com.guser.model.User;
import com.guser.service.CaptchaApiService;
import com.guser.service.EmailService;
import com.guser.service.FaceIdService;
import com.guser.util.SceneManager;
import com.guser.util.SessionManager;
import jakarta.mail.MessagingException;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.regex.Pattern;

public class LoginController {

    private static final int MAX_FAILED_PASSWORD_ATTEMPTS = 3;
    private static final int TWO_FACTOR_CODE_EXPIRATION_MINUTES = 5;
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    private static final Pattern PASSWORD_PATTERN = Pattern.compile("^(?=.*[A-Za-z])(?=.*\\d).{8,}$");

    @FXML
    private TextField emailField;

    @FXML
    private PasswordField motDePasseField;

    @FXML
    private TextField captchaField;

    @FXML
    private ImageView captchaImageView;

    @FXML
    private Label captchaFallbackLabel;

    private final UserDao userDao = new UserDao();
    private final EmailService emailService = new EmailService();
    private final CaptchaApiService captchaApiService = new CaptchaApiService();
    private final FaceIdService faceIdService = new FaceIdService();
    private final Random random = new Random();
    private final Map<String, Integer> failedAttemptsByEmail = new HashMap<>();
    private String expectedCaptcha;

    @FXML
    private void initialize() {
        refreshCaptcha();
    }

    @FXML
    private void seConnecter() {
        String email = emailField.getText().trim();
        String motDePasse = motDePasseField.getText();
        String captcha = captchaField.getText().trim();

        if (email.isBlank() || motDePasse.isBlank() || captcha.isBlank()) {
            showAlert(Alert.AlertType.ERROR, "Tous les champs sont obligatoires.");
            return;
        }
        if (!captcha.equalsIgnoreCase(expectedCaptcha)) {
            showAlert(Alert.AlertType.ERROR, "Captcha invalide.");
            refreshCaptcha();
            return;
        }

        try {
            Optional<User> user = userDao.authenticate(email, motDePasse);
            if (user.isEmpty()) {
                handleFailedPasswordAttempt(email);
                showAlert(Alert.AlertType.ERROR, "Email ou mot de passe incorrect.");
                refreshCaptcha();
                return;
            }

            if (!"ADMIN".equalsIgnoreCase(user.get().getRole())) {
                showAlert(Alert.AlertType.ERROR, "Acces refuse. Seuls les utilisateurs avec le role ADMIN peuvent se connecter.");
                return;
            }

            if (!validateTwoFactor(user.get())) {
                showAlert(Alert.AlertType.ERROR, "Code de verification 2FA incorrect ou expire.");
                refreshCaptcha();
                return;
            }

            SessionManager.setCurrentUser(user.get());
            resetFailedAttempts(email);
            SceneManager.switchScene(emailField, "/com/guser/user-view.fxml", "Guser | Utilisateurs");
        } catch (SQLException exception) {
            showAlert(Alert.AlertType.ERROR, "Connexion a la base impossible : " + exception.getMessage());
        } catch (MessagingException exception) {
            showAlert(Alert.AlertType.ERROR, "Impossible d'envoyer le code 2FA. Verifiez la configuration SMTP.");
        }
    }

    @FXML
    private void ouvrirMonTravail() {
        User currentUser = SessionManager.getCurrentUser();
        if (currentUser == null || !"ADMIN".equalsIgnoreCase(currentUser.getRole())) {
            showAlert(Alert.AlertType.ERROR, "Acces reserve a un administrateur connecte.");
            return;
        }

        SceneManager.switchScene(emailField, "/com/guser/user-view.fxml", "Guser | Utilisateurs");
    }

    @FXML
    private void seConnecterParVisage() {
        String captcha = captchaField.getText().trim();
        if (captcha.isBlank()) {
            showAlert(Alert.AlertType.ERROR, "Saisissez d'abord le captcha.");
            return;
        }
        if (!captcha.equalsIgnoreCase(expectedCaptcha)) {
            showAlert(Alert.AlertType.ERROR, "Captcha invalide.");
            refreshCaptcha();
            return;
        }

        try {
            String liveTemplate = faceIdService.captureTemplate();
            List<User> candidates = userDao.findAdminsWithFaceTemplate();
            if (candidates.isEmpty()) {
                showAlert(Alert.AlertType.ERROR, "Aucun profil FaceID admin n'est configure.");
                refreshCaptcha();
                return;
            }

            User matchedUser = null;
            double bestScore = 0.0;
            for (User candidate : candidates) {
                double score = faceIdService.compareTemplates(liveTemplate, candidate.getFaceTemplate());
                if (score > bestScore) {
                    bestScore = score;
                    matchedUser = candidate;
                }
            }

            if (matchedUser == null || bestScore < faceIdService.getMatchThreshold()) {
                showAlert(Alert.AlertType.ERROR, "Visage non reconnu. Essayez de vous placer face a la camera.");
                refreshCaptcha();
                return;
            }

            SessionManager.setCurrentUser(matchedUser);
            resetFailedAttempts(matchedUser.getEmail());
            SceneManager.switchScene(emailField, "/com/guser/user-view.fxml", "Guser | Utilisateurs");
        } catch (IOException exception) {
            showAlert(Alert.AlertType.ERROR, "Capture FaceID impossible : " + exception.getMessage());
        } catch (SQLException exception) {
            showAlert(Alert.AlertType.ERROR, "Erreur FaceID base de donnees : " + exception.getMessage());
        }
    }

    @FXML
    private void motDePasseOublie() {
        String email = emailField.getText().trim();
        if (email.isBlank()) {
            showAlert(Alert.AlertType.ERROR, "Saisissez d'abord votre email.");
            return;
        }
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            showAlert(Alert.AlertType.ERROR, "Veuillez saisir une adresse email valide.");
            return;
        }

        try {
            Optional<User> user = userDao.findByEmail(email);
            if (user.isEmpty() || !"ADMIN".equalsIgnoreCase(user.get().getRole())) {
                showAlert(Alert.AlertType.ERROR, "Aucun compte administrateur trouve pour cet email.");
                return;
            }

            String code = generateCode();
            emailService.sendPasswordResetCode(user.get().getEmail(), user.get().getNomComplet(), code);

            if (!askCode(code)) {
                showAlert(Alert.AlertType.ERROR, "Code de recuperation incorrect.");
                return;
            }

            Optional<String> nouveauMotDePasse = askNewPassword();
            if (nouveauMotDePasse.isEmpty()) {
                return;
            }

            userDao.updatePassword(user.get().getId(), nouveauMotDePasse.get());
            showAlert(Alert.AlertType.INFORMATION, "Mot de passe mis a jour avec succes.");
        } catch (MessagingException exception) {
            showAlert(Alert.AlertType.ERROR, "Impossible d'envoyer le code de recuperation. Verifiez MailConfig.java.");
        } catch (SQLException exception) {
            showAlert(Alert.AlertType.ERROR, "Erreur lors de la recuperation : " + exception.getMessage());
        }
    }

    private boolean askCode(String expectedCode) {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Recuperation");
        dialog.setHeaderText("Saisissez le code recu par email");
        dialog.setContentText("Code :");
        Optional<String> result = dialog.showAndWait();
        return result.isPresent() && expectedCode.equals(result.get().trim());
    }

    private Optional<String> askNewPassword() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Nouveau mot de passe");
        dialog.setHeaderText("Definissez un nouveau mot de passe");
        dialog.setContentText("Mot de passe :");
        Optional<String> result = dialog.showAndWait().map(String::trim);

        if (result.isEmpty()) {
            return Optional.empty();
        }
        if (!PASSWORD_PATTERN.matcher(result.get()).matches()) {
            showAlert(Alert.AlertType.ERROR, "Le mot de passe doit contenir au moins 8 caracteres, avec des lettres et des chiffres.");
            return Optional.empty();
        }
        return result;
    }

    private String generateCode() {
        return String.format("%06d", random.nextInt(1_000_000));
    }

    @FXML
    private void regenererCaptcha() {
        refreshCaptcha();
    }

    private void refreshCaptcha() {
        CaptchaApiService.CaptchaChallenge challenge = captchaApiService.generateChallenge();
        expectedCaptcha = challenge.code();
        captchaField.clear();
        captchaImageView.setImage(challenge.image());
        boolean fallback = challenge.image() == null;
        captchaFallbackLabel.setManaged(fallback);
        captchaFallbackLabel.setVisible(fallback);
        if (fallback) {
            captchaFallbackLabel.setText(expectedCaptcha);
        }
    }

    private boolean validateTwoFactor(User user) throws MessagingException {
        String twoFactorCode = generateCode();
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(TWO_FACTOR_CODE_EXPIRATION_MINUTES);
        emailService.sendTwoFactorCode(user.getEmail(), user.getNomComplet(), twoFactorCode, expiresAt);
        return askTwoFactorCode(twoFactorCode, expiresAt);
    }

    private boolean askTwoFactorCode(String expectedCode, LocalDateTime expiresAt) {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Verification 2FA");
        dialog.setHeaderText("Un code a ete envoye a votre email");
        dialog.setContentText("Code :");
        Optional<String> result = dialog.showAndWait().map(String::trim);
        if (result.isEmpty()) {
            return false;
        }
        if (LocalDateTime.now().isAfter(expiresAt)) {
            showAlert(Alert.AlertType.ERROR, "Le code 2FA a expire. Recommencez la connexion.");
            return false;
        }
        return expectedCode.equals(result.get());
    }

    private void handleFailedPasswordAttempt(String email) throws SQLException {
        Optional<User> existingUser = userDao.findByEmail(email);
        if (existingUser.isEmpty()) {
            return;
        }

        String key = email.toLowerCase(Locale.ROOT);
        int failedAttempts = failedAttemptsByEmail.merge(key, 1, Integer::sum);
        if (failedAttempts < MAX_FAILED_PASSWORD_ATTEMPTS) {
            return;
        }

        failedAttemptsByEmail.put(key, 0);
        sendCaptureToUser(existingUser.get());
    }

    private void resetFailedAttempts(String email) {
        failedAttemptsByEmail.remove(email.toLowerCase(Locale.ROOT));
    }

    private void sendCaptureToUser(User user) {
        Path imagePath = null;
        try {
            imagePath = captureFromCamera();
            emailService.sendSuspiciousLoginCapture(user.getEmail(), user.getNomComplet(), imagePath.toFile());
            showAlert(Alert.AlertType.WARNING,
                    "3 tentatives detectees. Une capture camera a ete envoyee a l'email associe.");
        } catch (IOException | MessagingException exception) {
            showAlert(Alert.AlertType.ERROR,
                    "3 tentatives detectees, mais l'envoi de la capture a echoue : " + exception.getMessage());
        } finally {
            if (imagePath != null) {
                try {
                    Files.deleteIfExists(imagePath);
                } catch (IOException ignored) {
                    // Ignore cleanup issue.
                }
            }
        }
    }

    private Path captureFromCamera() throws IOException {
        Webcam webcam = Webcam.getDefault();
        if (webcam == null) {
            throw new IOException("Aucune camera detectee.");
        }

        try {
            webcam.open();
            BufferedImage image = webcam.getImage();
            if (image == null) {
                throw new IOException("Impossible de capturer une image.");
            }
            Path imagePath = Files.createTempFile("guser-capture-", ".png");
            ImageIO.write(image, "png", imagePath.toFile());
            return imagePath;
        } finally {
            if (webcam.isOpen()) {
                webcam.close();
            }
        }
    }

    private void showAlert(Alert.AlertType type, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(type == Alert.AlertType.ERROR ? "Erreur"
                : type == Alert.AlertType.WARNING ? "Alerte" : "Information");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
