package services;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class OpenAIService {

    private static final String API_KEY = "";
    private static final String URL = "https://api.groq.com/openai/v1/chat/completions";

    public static String getAICompletion(String type, String priorite, String equipement, String descAgriculteur) throws Exception {

        // 1. Nettoyage de la description (pour éviter l'erreur 400)
        String descNettoyee = (descAgriculteur != null) ? descAgriculteur.replace("\"", "\\\"").replace("\n", " ") : "";

        // 2. ICI ON MET LE PROMPT INTELLIGENT
        // Il combine les choix du technicien ET les mots de l'agriculteur
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

        // 4. Envoi de la requête
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