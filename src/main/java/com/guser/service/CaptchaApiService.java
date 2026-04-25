package com.guser.service;

import javafx.scene.image.Image;

import java.io.InputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Random;

public class CaptchaApiService {

    private static final String CAPTCHA_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final String CAPTCHA_IMAGE_TEMPLATE = "https://dummyimage.com/220x80/10233d/e7edf4.png&text=%s";

    private final Random random = new Random();

    public CaptchaChallenge generateChallenge() {
        String code = generateCode();
        Image image = loadCaptchaImage(code);
        return new CaptchaChallenge(code, image);
    }

    private String generateCode() {
        StringBuilder code = new StringBuilder(6);
        for (int i = 0; i < 6; i++) {
            code.append(CAPTCHA_CHARS.charAt(random.nextInt(CAPTCHA_CHARS.length())));
        }
        return code.toString();
    }

    private Image loadCaptchaImage(String code) {
        String encoded = URLEncoder.encode(code, StandardCharsets.UTF_8);
        String url = String.format(CAPTCHA_IMAGE_TEMPLATE, encoded);
        try (InputStream inputStream = URI.create(url).toURL().openStream()) {
            return new Image(inputStream);
        } catch (Exception exception) {
            return null;
        }
    }

    public record CaptchaChallenge(String code, Image image) {
    }
}
