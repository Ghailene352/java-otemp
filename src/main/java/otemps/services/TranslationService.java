package otemps.services;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class TranslationService {

    private static final String MYMEMORY_API = "https://api.mymemory.translated.net/get?q={text}&langpair={from}|{to}";
    private static final String LIBRETRANSLATE_API = "https://libretranslate.de/translate";

    public TranslationService() {
        System.out.println("🌐 TranslationService initialisé (Gratuit - Sans clé)");
    }

    /**
     * ✅ TRADUIRE UN TEXTE
     * @param text Texte à traduire
     * @param fromLang Code langue source (ex: "fr")
     * @param toLang Code langue cible (ex: "en")
     * @return Texte traduit
     */
    public String translate(String text, String fromLang, String toLang) {
        try {
            if (text == null || text.isEmpty()) {
                return "❌ Texte vide";
            }

            if (fromLang.equals(toLang)) {
                return text;
            }

            System.out.println("🌐 Traduction: " + fromLang + " → " + toLang);

            // ✅ ESSAYER MyMemory EN PREMIER
            String result = translateWithMyMemory(text, fromLang, toLang);
            if (!result.contains("❌")) {
                System.out.println("✅ Traduction MyMemory réussie");
                return result;
            }

            // ✅ FALLBACK: LibreTranslate
            System.out.println("🔄 Fallback vers LibreTranslate...");
            result = translateWithLibreTranslate(text, fromLang, toLang);
            if (!result.contains("❌")) {
                System.out.println("✅ Traduction LibreTranslate réussie");
                return result;
            }

            // ✅ FALLBACK: Texte original
            System.out.println("⚠️ Traduction échouée, texte original retourné");
            return text;

        } catch (Exception e) {
            System.err.println("❌ Erreur traduction: " + e.getMessage());
            return text;
        }
    }

    /**
     * ✅ TRADUCTION AVEC MYMEMORY (GRATUIT - SANS CLÉ)
     */
    private String translateWithMyMemory(String text, String fromLang, String toLang) {
        try {
            String encodedText = URLEncoder.encode(text, StandardCharsets.UTF_8);
            String url = "https://api.mymemory.translated.net/get?q=" + encodedText + "&langpair=" + fromLang + "|" + toLang;

            System.out.println("📡 Appel MyMemory API...");

            HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("User-Agent", "Mozilla/5.0");
            conn.setConnectTimeout(8000);
            conn.setReadTimeout(8000);

            int code = conn.getResponseCode();
            System.out.println("📊 MyMemory Code: " + code);

            if (code == 200) {
                String response = readResponse(conn.getInputStream());
                String result = parseMyMemoryResponse(response);
                if (!result.contains("❌")) {
                    return result;
                }
            }

            return "❌ MyMemory Error: " + code;

        } catch (Exception e) {
            System.err.println("❌ MyMemory Exception: " + e.getMessage());
            return "❌ " + e.getMessage();
        }
    }

    /**
     * ✅ TRADUCTION AVEC LIBRETRANSLATE (GRATUIT - SANS CLÉ)
     */
    private String translateWithLibreTranslate(String text, String fromLang, String toLang) {
        try {
            System.out.println("📡 Appel LibreTranslate API...");

            String jsonBody = "{\"q\":\"" + escapeJson(text) + "\",\"source\":\"" + fromLang + "\",\"target\":\"" + toLang + "\"}";

            HttpURLConnection conn = (HttpURLConnection) new URL(LIBRETRANSLATE_API).openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);
            conn.setConnectTimeout(8000);
            conn.setReadTimeout(8000);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(jsonBody.getBytes(StandardCharsets.UTF_8));
            }

            int code = conn.getResponseCode();
            System.out.println("📊 LibreTranslate Code: " + code);

            if (code == 200) {
                String response = readResponse(conn.getInputStream());
                String result = parseLibreTranslateResponse(response);
                if (!result.contains("❌")) {
                    return result;
                }
            }

            return "❌ LibreTranslate Error: " + code;

        } catch (Exception e) {
            System.err.println("❌ LibreTranslate Exception: " + e.getMessage());
            return "❌ " + e.getMessage();
        }
    }

    /**
     * ✅ PARSER RÉPONSE MyMemory
     */
    private String parseMyMemoryResponse(String jsonResponse) {
        try {
            int textStart = jsonResponse.indexOf("\"translatedText\":\"");
            if (textStart == -1) {
                return "❌ Format invalide";
            }

            textStart += "\"translatedText\":\"".length();
            int textEnd = jsonResponse.indexOf("\"", textStart);

            if (textEnd == -1) {
                return "❌ Parsing error";
            }

            String translated = jsonResponse.substring(textStart, textEnd);
            translated = unescapeJson(translated);

            return translated;

        } catch (Exception e) {
            System.err.println("❌ Parsing MyMemory error: " + e.getMessage());
            return "❌ " + e.getMessage();
        }
    }

    /**
     * ✅ PARSER RÉPONSE LibreTranslate
     */
    private String parseLibreTranslateResponse(String jsonResponse) {
        try {
            int textStart = jsonResponse.indexOf("\"translatedText\":\"");
            if (textStart == -1) {
                return "❌ Format invalide";
            }

            textStart += "\"translatedText\":\"".length();
            int textEnd = jsonResponse.indexOf("\"", textStart);

            if (textEnd == -1) {
                return "❌ Parsing error";
            }

            String translated = jsonResponse.substring(textStart, textEnd);
            translated = unescapeJson(translated);

            return translated;

        } catch (Exception e) {
            System.err.println("❌ Parsing LibreTranslate error: " + e.getMessage());
            return "❌ " + e.getMessage();
        }
    }

    /**
     * ✅ LIRE RÉPONSE HTTP
     */
    private String readResponse(InputStream is) throws IOException {
        StringBuilder response = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
        }
        return response.toString();
    }

    /**
     * ✅ ÉCHAPPER JSON
     */
    private String escapeJson(String text) {
        return text.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    /**
     * ✅ DÉS-ÉCHAPPER JSON
     */
    private String unescapeJson(String text) {
        return text.replace("\\n", "\n")
                .replace("\\r", "\r")
                .replace("\\t", "\t")
                .replace("\\\"", "\"")
                .replace("\\\\", "\\");
    }
}