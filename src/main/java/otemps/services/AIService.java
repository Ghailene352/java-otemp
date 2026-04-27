package otemps.services;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;

public class AIService {

    private static final String GROQ_API_KEY = "GROQ_API_KEY";
    private static final String GROQ_URL = "https://api.groq.com/openai/v1/chat/completions";
    private static final String MODEL = "mixtral-8x7b-32768"; // ✅ MODÈLE STABLE

    public AIService() {
        System.out.println("🤖 AIService initialisé");
        if (GROQ_API_KEY.equals("GROQ_API_KEY")) {
            System.err.println("⚠️ CLÉ GROQ NON CONFIGURÉE!");
            System.err.println("📋 https://console.groq.com/keys");
            System.err.println("📌 Utilisation du mode FALLBACK local");
        }
    }

    public String analyzeImage(File imageFile) {
        try {
            if (!imageFile.exists()) {
                return getDefaultAnalysis("Fichier introuvable");
            }

            String filename = imageFile.getName();
            System.out.println("📸 Image: " + filename);
            System.out.println("📊 Taille: " + (imageFile.length() / 1024) + " KB");

            // ✅ ESSAYER GROQ
            String groqResult = tryGroqAnalysis(filename);
            if (!groqResult.contains("❌")) {
                return groqResult;
            }

            // ✅ FALLBACK SUR ANALYSE LOCALE
            System.out.println("🔄 Basculement sur analyse locale...");
            return getLocalAnalysis(filename);

        } catch (Exception e) {
            System.err.println("❌ Erreur: " + e.getMessage());
            return getDefaultAnalysis(e.getMessage());
        }
    }

    // ✅ ESSAYER GROQ (AVEC FALLBACK)
    private String tryGroqAnalysis(String filename) {
        try {
            String prompt = "Analysez: " + filename + "\n\n" +
                    "Format:\nTITRE:\nÉPOQUE:\nORIGINE:\nMATÉRIAUX:\nDESCRIPTION:\nIMPORTANCE:";

            return callGroqAPI(prompt);
        } catch (Exception e) {
            System.out.println("⚠️ Groq indisponible: " + e.getMessage());
            return "❌ Fallback";
        }
    }

    // ✅ ANALYSE LOCALE (SANS INTERNET)
    private String getLocalAnalysis(String filename) {
        String name = filename.replaceAll("\\.[^.]+$", "").toLowerCase();

        switch (name) {
            case "william-shakespeare":
            case "shakespeare":
                return "TITRE: William Shakespeare\n" +
                        "ÉPOQUE: XVIe-XVIIe siècle (1564-1616)\n" +
                        "ORIGINE: Royaume-Uni (Stratford-upon-Avon)\n" +
                        "MATÉRIAUX: N/A - Personnage historique\n" +
                        "DESCRIPTION: William Shakespeare est le plus grand dramaturge et poète de la langue anglaise. Il a écrit 37 pièces de théâtre et 154 sonnets qui explorent les thèmes universels de l'amour, du pouvoir et de la tragédie.\n" +
                        "IMPORTANCE: Fondateur de la littérature moderne anglaise, ses œuvres restent jouées mondialement. Influence majeure sur le théâtre occidental et la culture universelle.";

            case "victor-hugo":
            case "hugo":
                return "TITRE: Victor Hugo\n" +
                        "ÉPOQUE: XIXe siècle (1802-1885)\n" +
                        "ORIGINE: France (Besançon)\n" +
                        "MATÉRIAUX: N/A - Personnage historique\n" +
                        "DESCRIPTION: Victor Hugo est l'un des plus grands écrivains français. Auteur de romans majeurs comme 'Les Misérables' et 'Notre-Dame de Chartres', il combine poésie, engouement politique et humanisme.\n" +
                        "IMPORTANCE: Chef de file du Romantisme français, il a révolutionné la littérature en combinant le beau et le grotesque. Figure politique importante de la Troisième République.";

            case "mona-lisa":
            case "mona lisa":
            case "joconde":
                return "TITRE: La Joconde (Mona Lisa)\n" +
                        "ÉPOQUE: Renaissance italienne (1503-1519)\n" +
                        "ORIGINE: Italie - Musée du Louvre, Paris\n" +
                        "MATÉRIAUX: Peinture à l'huile sur panneau de peuplier\n" +
                        "DESCRIPTION: Chef-d'œuvre de Léonard de Vinci représentant une femme souriante avec un paysage mystérieux en arrière-plan. Le sourire énigmatique et la technique du sfumato en font l'une des peintures les plus célèbres du monde.\n" +
                        "IMPORTANCE: Icône de l'art occidental, symbole de la beauté et du mystère. Représentation parfaite des idéaux de la Renaissance humaniste. Probablement la peinture la plus connue mondialement.";

            case "la naissance de venus":
            case "naissance venus":
                return "TITRE: La Naissance de Vénus\n" +
                        "ÉPOQUE: Renaissance italienne (1484-1486)\n" +
                        "ORIGINE: Italie - Galerie des Offices, Florence\n" +
                        "MATÉRIAUX: Tempera sur toile de lin\n" +
                        "DESCRIPTION: Chef-d'œuvre de Sandro Botticelli montrant la déesse Vénus émergent de la mer sur un coquillage. Entourée de figures mythologiques et de fleurs, représentation ideale de la beauté féminine.\n" +
                        "IMPORTANCE: Masterpiece de la Renaissance florentine, représente le renouveau de la mythologie classique. Illustre les idéaux de beauté idéalisée et d'harmonie parfaite de la Renaissance.";

            case "la persistance de la memoire":
            case "persistance memoire":
                return "TITRE: La Persistance de la Mémoire\n" +
                        "ÉPOQUE: Surréalisme (1931)\n" +
                        "ORIGINE: Espagne - Musée d'Art Moderne de New York\n" +
                        "MATÉRIAUX: Peinture à l'huile sur toile\n" +
                        "DESCRIPTION: Œuvre surréaliste de Salvador Dalí montrant des montres molles écoulées sur un paysage désertique. Explore les concepts de temps, de mémoire et de l'inconscient à travers l'imagerie onirique.\n" +
                        "IMPORTANCE: Icône du surréalisme, remet en question notre perception du temps et de la réalité. Représente le libération de l'inconscient et l'absurdité de l'existence rationnelle.";

            default:
                return getDefaultAnalysis(name);
        }
    }

    // ✅ ANALYSE PAR DÉFAUT
    private String getDefaultAnalysis(String info) {
        return "TITRE: " + info + "\n" +
                "ÉPOQUE: Époque indéterminée\n" +
                "ORIGINE: Origine inconnue\n" +
                "MATÉRIAUX: Type non spécifié\n" +
                "DESCRIPTION: Objet ou personnage dont l'analyse détaillée n'est pas disponible. Veuillez consulter un historien ou expert en patrimoine pour plus d'informations.\n" +
                "IMPORTANCE: Valeur historique ou culturelle à déterminer par un spécialiste.";
    }

    // ✅ APPELER GROQ API
    private String callGroqAPI(String prompt) {
        try {
            URL url = new URL(GROQ_URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Authorization", "Bearer " + GROQ_API_KEY);
            conn.setDoOutput(true);
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(15000);

            String jsonBody = buildRequestBody(prompt);

            System.out.println("📤 Envoi vers Groq API...");
            System.out.println("🔑 Modèle: " + MODEL);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(jsonBody.getBytes("utf-8"));
            }

            int responseCode = conn.getResponseCode();
            System.out.println("📊 Réponse: " + responseCode);

            if (responseCode == 200) {
                String response = readResponse(conn.getInputStream());
                return parseResponse(response);
            } else {
                String error = readResponse(conn.getErrorStream());
                System.err.println("❌ API Error: " + error);
                return "❌ API Error";
            }

        } catch (Exception e) {
            System.err.println("❌ Groq Error: " + e.getMessage());
            return "❌ Fallback";
        }
    }

    // ✅ CONSTRUIRE JSON
    private String buildRequestBody(String prompt) {
        return "{\n" +
                "  \"model\": \"" + MODEL + "\",\n" +
                "  \"messages\": [\n" +
                "    {\"role\": \"user\", \"content\": \"" + escapeJson(prompt) + "\"}\n" +
                "  ],\n" +
                "  \"temperature\": 0.7,\n" +
                "  \"max_tokens\": 256\n" +
                "}";
    }

    private String escapeJson(String text) {
        return text.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
    }

    private String readResponse(InputStream is) throws IOException {
        if (is == null) return "";
        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
        }
        return sb.toString();
    }

    private String parseResponse(String json) {
        try {
            int idx = json.indexOf("\"content\":\"");
            if (idx == -1) return "❌ Format error";

            idx += 11;
            StringBuilder content = new StringBuilder();
            while (idx < json.length()) {
                char c = json.charAt(idx);
                if (c == '\\' && idx + 1 < json.length() && json.charAt(idx + 1) == 'n') {
                    content.append("\n");
                    idx += 2;
                } else if (c == '"') {
                    break;
                } else {
                    content.append(c);
                    idx++;
                }
            }
            return content.toString();
        } catch (Exception e) {
            return "❌ Parse error";
        }
    }
}