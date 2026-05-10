package services;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;

public class ChatBotService {

    // Groq + Llama 3.3 — same provider/key/model as the Symfony chatbot
    // (see Symfony `.env.local` GROQ_API_KEY and `UtilisateurController::apiChat`).
    private static final String API_URL = "https://api.groq.com/openai/v1/chat/completions";
    private static final String API_KEY = "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";
    private static final String MODEL   = "llama-3.3-70b-versatile";

    private static final String SYSTEM_PROMPT = """
            Tu es l'assistant virtuel d'AGRICOR, une application de gestion agricole intelligente.
            Tu aides les utilisateurs a naviguer et comprendre les fonctionnalites de l'application.

            Fonctionnalites de l'application:
            - Gestion des Animaux: ajouter, modifier, supprimer des animaux, suivi medical, statistiques de sante, meteo et sante, recommandations alimentaires
            - Gestion Financiere: suivi des depenses et ventes, tableaux financiers, analyses et graphiques
            - Gestion des Equipements: inventaire des equipements agricoles, suivi d'utilisation
            - Gestion de la Maintenance: planifier et suivre les maintenances, notifications de maintenance
            - Gestion des Evenements: creation et suivi d'evenements agricoles
            - Profil Utilisateur: modification du profil, photo de profil, parametres du compte

            Reponds toujours en francais, de maniere concise et utile.
            Si l'utilisateur pose une question hors du contexte de l'application, redirige-le poliment.
            """;

    private final HttpClient httpClient;
    private final List<JSONObject> conversationHistory;

    // Rate limiting
    private static final long MIN_REQUEST_INTERVAL_MS = 2000; // 2 seconds minimum between requests
    private final AtomicLong lastRequestTime = new AtomicLong(0);

    // Simple cache for common greetings
    private String lastUserMessage = "";
    private String lastResponse = "";
    private long lastCacheTime = 0;
    private static final long CACHE_DURATION_MS = 8000; // Cache same message for 8 seconds

    public ChatBotService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15))
                .build();
        this.conversationHistory = new ArrayList<>();
    }

    public CompletableFuture<String> sendMessage(String userMessage) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Check cache first (for repeated messages)
                if (userMessage.equalsIgnoreCase(lastUserMessage) &&
                        System.currentTimeMillis() - lastCacheTime < CACHE_DURATION_MS) {
                    return lastResponse;
                }

                // Apply rate limiting
                applyRateLimit();

                // Add user message to history
                JSONObject userMsg = new JSONObject();
                userMsg.put("role", "user");
                userMsg.put("content", userMessage);
                conversationHistory.add(userMsg);

                // Build messages array
                JSONArray messages = new JSONArray();

                // System prompt
                JSONObject systemMsg = new JSONObject();
                systemMsg.put("role", "system");
                systemMsg.put("content", SYSTEM_PROMPT);
                messages.put(systemMsg);

                // Conversation history (keep last 6 messages for context)
                int start = Math.max(0, conversationHistory.size() - 6);
                for (int i = start; i < conversationHistory.size(); i++) {
                    messages.put(conversationHistory.get(i));
                }

                // Build request body — matches Symfony's apiChat payload shape
                JSONObject requestBody = new JSONObject();
                requestBody.put("model", MODEL);
                requestBody.put("messages", messages);
                requestBody.put("max_tokens", 300);
                requestBody.put("temperature", 0.7);

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(API_URL))
                        .header("Content-Type", "application/json")
                        .header("Authorization", "Bearer " + API_KEY)
                        .POST(HttpRequest.BodyPublishers.ofString(requestBody.toString()))
                        .timeout(Duration.ofSeconds(25))
                        .build();

                // Try with retry logic
                String response = sendWithRetry(request, 2);

                // Update cache
                lastUserMessage = userMessage;
                lastResponse = response;
                lastCacheTime = System.currentTimeMillis();

                return response;

            } catch (Exception e) {
                e.printStackTrace();
                return "Désolé, je rencontre des difficultés techniques. Veuillez réessayer dans quelques instants.";
            }
        });
    }

    private void applyRateLimit() {
        long now = System.currentTimeMillis();
        long lastTime = lastRequestTime.get();

        if (now - lastTime < MIN_REQUEST_INTERVAL_MS) {
            try {
                Thread.sleep(MIN_REQUEST_INTERVAL_MS - (now - lastTime));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        lastRequestTime.set(System.currentTimeMillis());
    }

    private String sendWithRetry(HttpRequest request, int maxRetries) {
        int retryCount = 0;
        int baseDelay = 2000; // Start with 2 seconds delay

        while (retryCount <= maxRetries) {
            try {
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 200) {
                    JSONObject responseJson = new JSONObject(response.body());

                    // Check if choices array exists and has elements
                    if (!responseJson.has("choices") || responseJson.getJSONArray("choices").isEmpty()) {
                        return "Désolé, je n'ai pas pu générer une réponse. Veuillez réessayer.";
                    }

                    String assistantMessage = responseJson
                            .getJSONArray("choices")
                            .getJSONObject(0)
                            .getJSONObject("message")
                            .getString("content");

                    // Add assistant response to history
                    JSONObject assistantMsg = new JSONObject();
                    assistantMsg.put("role", "assistant");
                    assistantMsg.put("content", assistantMessage);
                    conversationHistory.add(assistantMsg);

                    return assistantMessage;

                } else if (response.statusCode() == 429) {
                    // Rate limited - retry with exponential backoff
                    retryCount++;
                    if (retryCount <= maxRetries) {
                        int delay = baseDelay * (int) Math.pow(2, retryCount - 1);
                        try {
                            Thread.sleep(delay);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            return "Erreur lors de la tentative de reconnexion.";
                        }
                        continue;
                    } else {
                        return "Le service est temporairement surchargé. Veuillez patienter quelques instants avant de réessayer.";
                    }
                } else if (response.statusCode() == 401 || response.statusCode() == 403) {
                    return "Erreur d'authentification avec le service. Veuillez vérifier votre clé API.";
                } else if (response.statusCode() >= 500) {
                    retryCount++;
                    if (retryCount <= maxRetries) {
                        try {
                            Thread.sleep(baseDelay * retryCount);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                        continue;
                    } else {
                        return "Le service est actuellement indisponible. Veuillez réessayer plus tard.";
                    }
                } else {
                    return "Erreur du serveur (code " + response.statusCode() + "). Veuillez réessayer.";
                }

            } catch (Exception e) {
                retryCount++;
                if (retryCount <= maxRetries) {
                    try {
                        Thread.sleep(baseDelay * retryCount);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    }
                } else {
                    return "Erreur de connexion après plusieurs tentatives. Veuillez vérifier votre connexion internet.";
                }
            }
        }

        return "Désolé, je n'ai pas pu traiter votre demande. Veuillez réessayer.";
    }

    public void clearHistory() {
        conversationHistory.clear();
        lastUserMessage = "";
        lastResponse = "";
    }
}