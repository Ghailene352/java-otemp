package com.guser.service;

import com.github.sarxos.webcam.Webcam;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

public class FaceIdService {

    // Noms des variables d'environnement (pas les valeurs)
    private static final String ENV_FACEPP_API_KEY    = "FACEPP_API_KEY";
    private static final String ENV_FACEPP_API_SECRET = "FACEPP_API_SECRET";
    private static final String ENV_FACEPP_BASE_URL   = "FACEPP_BASE_URL";

    // URL par défaut sans espace trailing
    private static final String DEFAULT_FACEPP_BASE_URL = "https://api-us.faceplusplus.com/facepp/v3";

    // Clés codées en dur comme fallback si les variables d'env sont absentes
    private static final String FALLBACK_API_KEY    = "yYxrqYkF7uY6FOxpuZ3VO_I_GiUub6LI";
    private static final String FALLBACK_API_SECRET = "GUm8-4XiyxOSnB2kag0wyjtSxVJsIth_";

    private static final double   MATCH_THRESHOLD  = 0.80;
    private static final Duration REQUEST_TIMEOUT  = Duration.ofSeconds(20);

    private final HttpClient   httpClient   = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    private final ObjectMapper objectMapper = new ObjectMapper();

    // -------------------------------------------------------------------------
    // API publique
    // -------------------------------------------------------------------------

    public String captureTemplate() throws IOException {
        BufferedImage frame      = captureFromCamera();
        BufferedImage square     = cropCenterSquare(frame);
        BufferedImage normalized = toRgb(square, 512, 512);
        String        imageBase64 = encodeAsPngBase64(normalized);
        return detectFaceToken(imageBase64);
    }

    public double compareTemplates(String liveTemplate, String enrolledTemplate) throws IOException {
        if (liveTemplate == null || enrolledTemplate == null
                || liveTemplate.isBlank() || enrolledTemplate.isBlank()) {
            return 0.0;
        }

        JsonNode response       = callCompareApi(liveTemplate, enrolledTemplate);
        JsonNode confidenceNode = response.get("confidence");
        if (confidenceNode == null || !confidenceNode.isNumber()) {
            return 0.0;
        }

        return clamp(confidenceNode.asDouble() / 100.0);
    }

    public double getMatchThreshold() {
        return MATCH_THRESHOLD;
    }

    // -------------------------------------------------------------------------
    // Capture caméra
    // -------------------------------------------------------------------------

    private BufferedImage captureFromCamera() throws IOException {
        Webcam webcam = Webcam.getDefault();
        if (webcam == null) {
            throw new IOException("Aucune camera detectee.");
        }
        try {
            webcam.open();
            BufferedImage image = webcam.getImage();
            if (image == null) {
                throw new IOException("Capture visage impossible.");
            }
            return image;
        } finally {
            if (webcam.isOpen()) {
                webcam.close();
            }
        }
    }

    // -------------------------------------------------------------------------
    // Traitement image
    // -------------------------------------------------------------------------

    private BufferedImage cropCenterSquare(BufferedImage image) {
        int width  = image.getWidth();
        int height = image.getHeight();
        int size   = Math.min(width, height);
        int x      = (width  - size) / 2;
        int y      = (height - size) / 2;

        BufferedImage square   = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);
        Graphics2D    graphics = square.createGraphics();
        graphics.drawImage(image, 0, 0, size, size, x, y, x + size, y + size, null);
        graphics.dispose();
        return square;
    }

    private BufferedImage toRgb(BufferedImage image, int width, int height) {
        BufferedImage normalized = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D    graphics   = normalized.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        graphics.setRenderingHint(RenderingHints.KEY_RENDERING,
                RenderingHints.VALUE_RENDER_QUALITY);
        graphics.drawImage(image, 0, 0, width, height, null);
        graphics.dispose();
        return normalized;
    }

    private String encodeAsPngBase64(BufferedImage image) throws IOException {
        ByteArrayOutputStream output  = new ByteArrayOutputStream();
        boolean               written = ImageIO.write(image, "png", output);
        if (!written) {
            throw new IOException("Impossible de convertir l'image camera en PNG.");
        }
        return Base64.getEncoder().encodeToString(output.toByteArray());
    }

    // -------------------------------------------------------------------------
    // Appels API Face++
    // -------------------------------------------------------------------------

    private String detectFaceToken(String imageBase64) throws IOException {
        Map<String, String> payload = basePayload();
        payload.put("image_base64", imageBase64);

        JsonNode response = postForm("/detect", payload);
        JsonNode faces    = response.get("faces");
        if (faces == null || !faces.isArray() || faces.isEmpty()) {
            throw new IOException("Aucun visage detecte. Regardez la camera et reessayez.");
        }

        JsonNode faceToken = faces.get(0).get("face_token");
        if (faceToken == null || faceToken.asText().isBlank()) {
            throw new IOException("L'API FaceID n'a retourne aucun token de visage.");
        }

        return faceToken.asText();
    }

    private JsonNode callCompareApi(String liveToken, String enrolledToken) throws IOException {
        Map<String, String> payload = basePayload();
        payload.put("face_token1", liveToken);
        payload.put("face_token2", enrolledToken);
        return postForm("/compare", payload);
    }

    private JsonNode postForm(String path, Map<String, String> payload) throws IOException {
        String body = payload.entrySet()
                .stream()
                .map(e -> encode(e.getKey()) + "=" + encode(e.getValue()))
                .collect(Collectors.joining("&"));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(getBaseUrl() + path))
                .timeout(REQUEST_TIMEOUT)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> response;
        try {
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Appel FaceID interrompu.", e);
        }

        JsonNode json;
        try {
            json = objectMapper.readTree(response.body());
        } catch (Exception e) {
            throw new IOException("Reponse FaceID invalide.", e);
        }

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException(
                    "Erreur FaceID (" + response.statusCode() + ") : " + extractApiError(json));
        }

        JsonNode errorMessage = json.get("error_message");
        if (errorMessage != null && !errorMessage.asText("").isBlank()) {
            throw new IOException("FaceID : " + errorMessage.asText());
        }

        return json;
    }

    // -------------------------------------------------------------------------
    // Utilitaires
    // -------------------------------------------------------------------------

    /**
     * Construit le payload de base avec api_key et api_secret.
     * Priorité : variables d'environnement → constantes de fallback.
     */
    private Map<String, String> basePayload() {
        String apiKey    = System.getenv(ENV_FACEPP_API_KEY);
        String apiSecret = System.getenv(ENV_FACEPP_API_SECRET);

        // Fallback sur les constantes si les variables d'env sont absentes
        if (apiKey    == null || apiKey.isBlank())    apiKey    = FALLBACK_API_KEY;
        if (apiSecret == null || apiSecret.isBlank()) apiSecret = FALLBACK_API_SECRET;

        Map<String, String> payload = new HashMap<>();
        payload.put("api_key",    apiKey);
        payload.put("api_secret", apiSecret);
        return payload;
    }

    private String getBaseUrl() {
        String baseUrl = System.getenv(ENV_FACEPP_BASE_URL);
        if (baseUrl == null || baseUrl.isBlank()) {
            return DEFAULT_FACEPP_BASE_URL;
        }
        // Supprimer le slash final s'il est présent
        return baseUrl.strip().endsWith("/")
                ? baseUrl.strip().substring(0, baseUrl.strip().length() - 1)
                : baseUrl.strip();
    }

    private String extractApiError(JsonNode json) {
        if (json == null) return "reponse vide";
        String message   = json.path("error_message").asText("");
        String requestId = json.path("request_id").asText("");
        if (!message.isBlank() && !requestId.isBlank())
            return message + " (request_id=" + requestId + ")";
        if (!message.isBlank())   return message;
        if (!requestId.isBlank()) return "request_id=" + requestId;
        return "echec de l'appel API";
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private double clamp(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }
}