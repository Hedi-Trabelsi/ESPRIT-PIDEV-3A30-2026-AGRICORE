package services;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class OpenAIService {

    private static final String API_KEY = "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";
    private static final String URL = "https://api.groq.com/openai/v1/chat/completions";

    public static String getAICompletion(String type, String priorite, String equipement, String descAgriculteur) throws Exception {


        String descNettoyee = (descAgriculteur != null) ? descAgriculteur.replace("\"", "\\\"").replace("\n", " ") : "";



        String prompt = String.format(
                "Tu es un expert en maintenance agricole. Analyse ces deux sources d'infos :\\n" +
                        "1. DONNÉES TECHNIQUES : Machine %s, Type d'entretien %s.\\n" +
                        "2. DESCRIPTION DE L'AGRICULTEUR : \\\"%s\\\"\\n\\n" +
                        "Utilise la description pour identifier la panne précise sur cette machine. " +
                        "Réponds par : CAUSE PROBABLE / GRAVITÉ / SOLUTION.",
                equipement, type, descNettoyee
        );

        // 3. Construction du JSON avec le modèle llama-3.3-70b-versatile
        String jsonBody = "{"
                + "\"model\": \"llama-3.3-70b-versatile\","
                + "\"messages\": [{\"role\": \"user\", \"content\": \"" + prompt + "\"}],"
                + "\"temperature\": 0.5"
                + "}";

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(URL))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + API_KEY)
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            String fullResponse = response.body();
            // Extraction simple du texte (on peut utiliser Jackson/Gson pour faire plus propre)
            try {
                int start = fullResponse.indexOf("\"content\":\"") + 11;
                int end = fullResponse.indexOf("\"", start);
                String result = fullResponse.substring(start, end);
                return result.replace("\\n", "\n").replace("\\\"", "\"");
            } catch (Exception e) {
                return "Diagnostic généré : " + fullResponse;
            }
        } else {
            throw new Exception("Erreur Groq : " + response.statusCode());
        }
    }
}