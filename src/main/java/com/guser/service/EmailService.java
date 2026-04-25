package com.guser.service;

import com.guser.config.MailConfig;
import com.guser.dao.UserDao;
import com.guser.model.User;
import jakarta.activation.DataHandler;
import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import jakarta.mail.util.ByteArrayDataSource;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.Properties;

public class EmailService {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private final UserDao userDao = new UserDao();

    public void sendVerificationCode(String recipient, String fullName, String code) throws MessagingException {
        sendBrandedEmail(
                recipient,
                fullName,
                "Verification de votre adresse email",
                "Verification de votre email",
                "Nous avons recu une demande de verification pour votre compte Guser.",
                "Code de verification",
                code,
                "Saisissez ce code dans l'application pour confirmer votre adresse email.",
                null,
                null
        );
    }

    public void sendPasswordResetCode(String recipient, String fullName, String code) throws MessagingException {
        sendBrandedEmail(
                recipient,
                fullName,
                "Recuperation de mot de passe",
                "Recuperation de mot de passe",
                "Une demande de reinitialisation de mot de passe a ete detectee pour votre compte.",
                "Code de recuperation",
                code,
                "Saisissez ce code dans l'application pour definir un nouveau mot de passe.",
                null,
                null
        );
    }

    public void sendTwoFactorCode(String recipient, String fullName, String code, LocalDateTime expiresAt)
            throws MessagingException {
        String expirationText = expiresAt == null ? "5 minutes" : DATE_TIME_FORMATTER.format(expiresAt);
        sendBrandedEmail(
                recipient,
                fullName,
                "Code de connexion 2FA",
                "Connexion a 2 facteurs",
                "Une tentative de connexion valide a ete detectee sur votre compte Guser.",
                "Code 2FA",
                code,
                "Ce code expire le " + expirationText + ". Ne le partagez avec personne.",
                null,
                null
        );
    }

    public void sendSuspiciousLoginCapture(String recipient, String fullName, File imageFile)
            throws MessagingException, IOException {
        sendBrandedEmail(
                recipient,
                fullName,
                "Alerte connexion Guser",
                "Alerte de securite",
                "Nous avons detecte 3 tentatives de connexion avec un mot de passe incorrect sur votre compte.",
                null,
                null,
                "La capture camera prise lors de la troisieme tentative est jointe a cet email.",
                imageFile,
                "capture-connexion.png"
        );
    }

    private Session createSession() {
        return Session.getInstance(buildProperties(), new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(MailConfig.getUsername(), MailConfig.getPassword());
            }
        });
    }

    private void sendBrandedEmail(
            String recipient,
            String fallbackName,
            String subject,
            String title,
            String intro,
            String codeLabel,
            String codeValue,
            String footerText,
            File attachment,
            String attachmentFileName
    ) throws MessagingException {
        try {
            Session session = createSession();
            MimeMessage message = new MimeMessage(session);
            message.setFrom(new InternetAddress(MailConfig.getFromAddress()));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipient));
            message.setSubject(subject);

            String recipientName = resolveRecipientName(recipient, fallbackName);
            byte[] logoBytes = loadLogoBytes();
            String html = buildEmailHtml(recipientName, title, intro, codeLabel, codeValue, footerText, logoBytes != null);

            MimeMultipart mixed = new MimeMultipart("mixed");
            MimeBodyPart contentPart = new MimeBodyPart();
            MimeMultipart related = new MimeMultipart("related");

            MimeBodyPart htmlPart = new MimeBodyPart();
            htmlPart.setContent(html, "text/html; charset=UTF-8");
            related.addBodyPart(htmlPart);

            if (logoBytes != null) {
                related.addBodyPart(createInlineLogoPart(logoBytes));
            }

            contentPart.setContent(related);
            mixed.addBodyPart(contentPart);

            if (attachment != null && attachment.exists()) {
                MimeBodyPart attachmentPart = new MimeBodyPart();
                attachmentPart.attachFile(attachment);
                attachmentPart.setFileName(attachmentFileName == null ? attachment.getName() : attachmentFileName);
                mixed.addBodyPart(attachmentPart);
            }

            message.setContent(mixed);
            Transport.send(message);
        } catch (IOException exception) {
            throw new MessagingException("Impossible de construire l'email HTML.", exception);
        }
    }

    private MimeBodyPart createInlineLogoPart(byte[] logoBytes) throws MessagingException {
        MimeBodyPart logoPart = new MimeBodyPart();
        logoPart.setDataHandler(new DataHandler(new ByteArrayDataSource(logoBytes, "image/png")));
        logoPart.setHeader("Content-ID", "<guser-logo>");
        logoPart.setDisposition(MimeBodyPart.INLINE);
        logoPart.setFileName("guser-logo.png");
        return logoPart;
    }

    private byte[] loadLogoBytes() {
        byte[] preferredLogo = loadResourceBytes("/com/guser/assets/logo-otemps.png");
        return preferredLogo != null ? preferredLogo : loadResourceBytes("/com/guser/assets/logo.png");
    }

    private byte[] loadResourceBytes(String resourcePath) {
        try (InputStream inputStream = getClass().getResourceAsStream(resourcePath)) {
            return inputStream == null ? null : inputStream.readAllBytes();
        } catch (IOException exception) {
            return null;
        }
    }

    private String resolveRecipientName(String recipient, String fallbackName) {
        try {
            Optional<User> user = userDao.findByEmail(recipient);
            if (user.isPresent()) {
                String fullName = user.get().getNomComplet();
                if (fullName != null && !fullName.isBlank()) {
                    return fullName.trim();
                }
            }
        } catch (SQLException ignored) {
            // Ignore lookup failure and fallback below.
        }

        if (fallbackName != null && !fallbackName.isBlank()) {
            return fallbackName.trim();
        }

        if (recipient != null && recipient.contains("@")) {
            String localPart = recipient.substring(0, recipient.indexOf('@')).replace('.', ' ');
            return localPart.isBlank() ? "Utilisateur" : capitalize(localPart);
        }
        return "Utilisateur";
    }

    private String capitalize(String input) {
        String[] words = input.trim().split("\\s+");
        StringBuilder builder = new StringBuilder();
        for (String word : words) {
            if (word.isBlank()) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append(' ');
            }
            builder.append(Character.toUpperCase(word.charAt(0)));
            if (word.length() > 1) {
                builder.append(word.substring(1).toLowerCase());
            }
        }
        return builder.length() == 0 ? "Utilisateur" : builder.toString();
    }

    private String buildEmailHtml(
            String recipientName,
            String title,
            String intro,
            String codeLabel,
            String codeValue,
            String footerText,
            boolean includeLogo
    ) {
        String safeName = escapeHtml(recipientName);
        String safeTitle = escapeHtml(title);
        String safeIntro = escapeHtml(intro);
        String safeFooter = escapeHtml(footerText);
        String logoBlock = includeLogo
                ? "<img src=\"cid:guser-logo\" alt=\"Guser\" style=\"height:64px; width:auto; display:block; margin:0 auto 10px auto;\"/>"
                : "<div style=\"font-size:22px; font-weight:800; color:#10233d; margin-bottom:10px;\">Guser</div>";
        String codeBlock = (codeLabel == null || codeLabel.isBlank() || codeValue == null || codeValue.isBlank())
                ? ""
                : """
                        <div style="margin:20px 0; padding:18px; border-radius:12px; background:#f3f7fc; border:1px solid #d9e6f2;">
                            <div style="font-size:12px; color:#5f7389; text-transform:uppercase; letter-spacing:1px;">%s</div>
                            <div style="font-size:28px; font-weight:800; color:#10233d; margin-top:6px; letter-spacing:4px;">%s</div>
                        </div>
                        """.formatted(escapeHtml(codeLabel), escapeHtml(codeValue));

        return """
                <html>
                <body style="margin:0; padding:24px; background:#eef3f8; font-family:Segoe UI, Arial, sans-serif; color:#17324d;">
                    <table role="presentation" width="100%%" cellspacing="0" cellpadding="0">
                        <tr>
                            <td align="center">
                                <table role="presentation" width="620" cellspacing="0" cellpadding="0" style="background:#ffffff; border-radius:16px; overflow:hidden; border:1px solid #dce5ef;">
                                    <tr>
                                        <td style="padding:26px 28px; background:linear-gradient(120deg, #10233d 0%%, #1b3b63 100%%); color:#ffffff; text-align:center;">
                                            %s
                                            <div style="font-size:20px; font-weight:800; letter-spacing:0.4px;">%s</div>
                                        </td>
                                    </tr>
                                    <tr>
                                        <td style="padding:26px 28px;">
                                            <p style="margin:0 0 14px 0; font-size:15px;">Bonjour <strong>%s</strong>,</p>
                                            <p style="margin:0 0 10px 0; font-size:14px; line-height:1.55;">%s</p>
                                            %s
                                            <p style="margin:0; font-size:14px; line-height:1.55;">%s</p>
                                        </td>
                                    </tr>
                                    <tr>
                                        <td style="padding:14px 28px 24px 28px; font-size:12px; color:#6b7f95;">
                                            Equipe Guser
                                        </td>
                                    </tr>
                                </table>
                            </td>
                        </tr>
                    </table>
                </body>
                </html>
                """.formatted(logoBlock, safeTitle, safeName, safeIntro, codeBlock, safeFooter);
    }

    private String escapeHtml(String text) {
        if (text == null) {
            return "";
        }
        return text
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    private Properties buildProperties() {
        Properties properties = new Properties();
        properties.put("mail.smtp.auth", "true");
        properties.put("mail.smtp.starttls.enable", "true");
        properties.put("mail.smtp.host", MailConfig.getSmtpHost());
        properties.put("mail.smtp.port", String.valueOf(MailConfig.getSmtpPort()));
        return properties;
    }
}
