package llm;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.AccessToken;
import java.io.IOException;
import java.util.Collections;
import java.net.URI;
import java.net.http.*;
import java.nio.charset.StandardCharsets;

public class LlamaInteraction {
    public static void main(String[] args) throws Exception {
        String PROJECT_ID = System.getenv("GEMINI_PROJECT");
        String REGION = "us-central1";
        String ENDPOINT = REGION + "-aiplatform.googleapis.com";
        String MODEL_NAME = "meta/llama-3.1-405b-instruct-maas";

        String apiUrl = String.format("https://%s/v1/projects/%s/locations/%s/endpoints/openapi/chat/completions",
                ENDPOINT, PROJECT_ID, REGION);
        String requestBody = String.format("{\"model\":\"%s\", \"stream\":false, \"messages\":[{\"role\": \"user\", \"content\": \"%s\"}]}",
                MODEL_NAME, "Summer travel plan to Paris"); // Example query

        String ACCESS_TOKEN = getAccessToken();
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl))
                .header("Authorization", "Bearer " + ACCESS_TOKEN)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody, StandardCharsets.UTF_8))
                .build();

        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            System.out.println("Status Code: " + response.statusCode());
            System.out.println("Response Body: " + response.body());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static String getAccessToken() throws IOException {
        GoogleCredentials credentials = GoogleCredentials.getApplicationDefault()
                .createScoped(Collections.singletonList("https://www.googleapis.com/auth/cloud-platform"));
        credentials.refreshIfExpired();
        AccessToken accessToken = credentials.getAccessToken();
        return accessToken.getTokenValue();
    }

}