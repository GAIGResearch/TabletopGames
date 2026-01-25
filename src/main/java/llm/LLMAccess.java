package llm;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;
import dev.langchain4j.model.anthropic.AnthropicChatModel;
import dev.langchain4j.model.anthropic.AnthropicChatModelName;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.mistralai.MistralAiChatModelName;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiChatModelName;
import dev.langchain4j.model.openai.OpenAiTokenCountEstimator;
import dev.langchain4j.model.vertexai.VertexAiGeminiChatModel;
import dev.langchain4j.model.mistralai.MistralAiChatModel;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import utilities.JSONUtils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

public class LLMAccess {

    static VertexAiGeminiChatModel[] geminiModel = new VertexAiGeminiChatModel[3];
    static MistralAiChatModel[] mistralModel = new MistralAiChatModel[3];
    static OpenAiChatModel[] openaiModel = new OpenAiChatModel[3];
    static AnthropicChatModel[] anthropicModel = new AnthropicChatModel[3];

    static OpenAiTokenCountEstimator tokenizer = new OpenAiTokenCountEstimator("o200k_base");

    String mistralToken = System.getenv("MISTRAL_TOKEN");
    String geminiProject = System.getenv("GEMINI_PROJECT");
    String openaiToken = System.getenv("OPENAI_TOKEN");
    String anthropicToken = System.getenv("ANTHROPIC_TOKEN");

    File logFile;
    FileWriter logWriter;

    String geminiLocation = "europe-west9";
    // String llamaLocationLarge = "us-east5";  // Required for Llama 4 Maverick
    String llamaLocationLarge = "us-central1";
    String llamaLocationSmall = "us-central1";

    LLM_MODEL modelType;
    LLM_SIZE modelSize;

    long inputTokens = 0;
    long outputTokens = 0;

    public enum LLM_MODEL {
        GEMINI,
        MISTRAL,
        OPENAI,
        ANTHROPIC,
        LLAMA
    }

    public enum LLM_SIZE {
        SMALL,
        LARGE,
        REASONING
    }


    /**
     * Constructor for LLMAccess
     *
     * @param modelType   - the model to use as the default (this can be overridden in the getResponse method)
     * @param modelSize   - the size of the model to use (this can be overridden in the getResponse method)
     * @param logFileName - the name of the log file to write to. this will include all the messages sent to any LLM
     */
    public LLMAccess(LLM_MODEL modelType, LLM_SIZE modelSize, String logFileName) {
        if (logFileName != null && !logFileName.isEmpty()) {
            logFile = new File(logFileName);
            try {
                logWriter = new FileWriter(logFile);
            } catch (Exception e) {
                System.out.println("Error creating log file: " + e.getMessage());
            }
        }
        this.modelType = modelType;
        this.modelSize = modelSize;
        if (geminiProject != null && !geminiProject.isEmpty()) {
            try {
                geminiModel[1] = VertexAiGeminiChatModel.builder()
                        .project(geminiProject)
                        .location(geminiLocation)
                        //      .temperature(1.0f)  // between 0 and 2; default 1.0 for pro-1.5
                        //       .topK(40) // some models have a three-stage sampling process. topK; then topP; then temperature
                        //       .topP(0.94f)  // 1.5 default is 0.64; the is the sum of probability of tokens to sample from
                        //     .maxOutputTokens(1000)  // max replay size (max is 8192)
                        // .modelName("gemini-1.5-pro")   // $1.25 per million characters input, $0.3125 per million output
                        .modelName("gemini-2.0-flash") // $0.075 per million characters output, $0.01875 per million characters input
                        .build();
                geminiModel[0] = VertexAiGeminiChatModel.builder()
                        .project(geminiProject)
                        .location(geminiLocation)
                        .modelName("gemini-2.0-flash-lite")
                        .build();
                geminiModel[2] = VertexAiGeminiChatModel.builder()
                        .project(geminiProject)
                        .location(geminiLocation)
                        .modelName("gemini-2.5-flash-preview-05-20")
                        .build();
            } catch (Error e) {
                System.out.println("Error creating Gemini model: " + e.getMessage());
            }
        }

        if (mistralToken != null && !mistralToken.isEmpty()) {
            mistralModel[0] = MistralAiChatModel.builder()
                    .modelName(MistralAiChatModelName.MISTRAL_SMALL_LATEST)
                    .apiKey(mistralToken)
                    .build();
            mistralModel[1] = MistralAiChatModel.builder()
                    .modelName(MistralAiChatModelName.MISTRAL_LARGE_LATEST)
                    .apiKey(mistralToken)
                    .build();
            // $2 per million input tokens, $6 per million output tokens
        }

        if (openaiToken != null && !openaiToken.isEmpty()) {
            openaiModel[0] = OpenAiChatModel.builder()
                    .modelName(OpenAiChatModelName.GPT_4_O_MINI) // $0.15 per million input tokens, $0.6 per million output tokens
                    .apiKey(openaiToken)
                    .build();
            openaiModel[1] = OpenAiChatModel.builder()
                    .modelName(OpenAiChatModelName.GPT_4_O) // $5 per million input tokens, $15 per million output tokens
                    .apiKey(openaiToken)
                    .build();
            openaiModel[2] = OpenAiChatModel.builder()
                    .modelName(OpenAiChatModelName.O1_MINI) // $6 per million input tokens, $18 per million output tokens
                    .apiKey(openaiToken)
                    .build();
        }

        if (anthropicToken != null && !anthropicToken.isEmpty()) {
            anthropicModel[0] = AnthropicChatModel.builder()
                    .modelName(AnthropicChatModelName.CLAUDE_3_5_HAIKU_20241022) // $0.80 per million input tokens, $4 per million output tokens
                    .apiKey(anthropicToken)
                    .maxTokens(4096)
                    .build();
            anthropicModel[1] = AnthropicChatModel.builder()
                    .modelName(AnthropicChatModelName.CLAUDE_3_5_SONNET_20241022) // $3 per million input tokens, $15 per million output tokens
                    .apiKey(anthropicToken)
                    .maxTokens(8192)
                    .build();
        }

    }

    /**
     * Gets some text from the specified model
     *
     * @param query     the prompt sent to the model
     * @param modelType the LLM model to use
     * @return The full text returned by the model; or an empty string if no valid model
     */
    public String getResponse(String query, LLM_MODEL modelType, LLM_SIZE modelSize) {
        String response = "";
        inputTokens += tokenizer.estimateTokenCountInText(query);

        if (modelType == LLM_MODEL.LLAMA) {
            // do this the hardcore way
            response = getResponseWithLowLevelHttp(query, modelSize);
        } else {
            ChatModel modelToUse = switch (modelType) {
                case MISTRAL -> modelSize == LLM_SIZE.SMALL ? mistralModel[0] : mistralModel[1];
                case GEMINI -> modelSize == LLM_SIZE.SMALL ? geminiModel[0] : geminiModel[1];
                case OPENAI -> modelSize == LLM_SIZE.SMALL ? openaiModel[0] : openaiModel[1];
                case ANTHROPIC -> modelSize == LLM_SIZE.SMALL ? anthropicModel[0] : anthropicModel[1];
                default -> throw new IllegalArgumentException("Unknown model type: " + modelType);
            };
            if (modelSize == LLM_SIZE.REASONING) {
                if (modelType == LLM_MODEL.OPENAI)
                    modelToUse = openaiModel[2];
                else if (modelType == LLM_MODEL.GEMINI)
                    modelToUse = geminiModel[2];
                else
                    throw new IllegalArgumentException("Reasoning model not available for " + modelType);
            }
            if (modelToUse != null) {
                try {
                    response = modelToUse.chat(query);
                } catch (Exception e) {
                    System.out.println("Error getting response from model: " + e.getMessage());
                }
            } else {
                System.out.println("No valid model available for " + modelType + " " + modelSize);
                return "No reply available";
            }
        }
        // Write to file (if log file is specified)
        if (logWriter != null) {
            String output = String.format("\nModel: %s\nQuery: %s\nResponse: %s\n", modelType, query, response);
            try {
                logWriter.write(output);
                logWriter.flush();
            } catch (Exception e) {
                System.out.println("Error writing to log file: " + e.getMessage());
            }
        }
        outputTokens += tokenizer.estimateTokenCountInText(response);
        return response;
    }

    /**
     * This will use the default LLM model specified in the constructor
     *
     * @param query the prompt sent to the model
     * @return The full text returned by the model; or an empty string if no valid model
     */
    public String getResponse(String query) {
        return getResponse(query, this.modelType, this.modelSize);
    }

    private String getResponseWithLowLevelHttp(String query, LLM_SIZE size) {
        String llamaLocation = size == LLM_SIZE.SMALL ? llamaLocationSmall : llamaLocationLarge;
        String ENDPOINT = llamaLocation + "-aiplatform.googleapis.com";

        // changed from Maverick to 3.3 for better comparability instead of 405B model
        //     String MODEL_NAME = size == LLM_SIZE.SMALL ? "meta/llama-3.1-70b-instruct-maas" : "meta/llama-4-maverick-17b-128e-instruct-maas";
        String MODEL_NAME = size == LLM_SIZE.SMALL ? "meta/llama-3.1-70b-instruct-maas" : "meta/llama-3.3-70b-instruct-maas";
        String apiUrl = String.format("https://%s/v1/projects/%s/locations/%s/endpoints/openapi/chat/completions",
                ENDPOINT, geminiProject, llamaLocation);

        ObjectMapper objectMapper = new ObjectMapper();
        String jsonContent;
        try {
            jsonContent = objectMapper.writeValueAsString(query); // Escapes special characters automatically
        } catch (IOException e) {
            System.out.println("Error converting query to JSON: " + e.getMessage());
            return "Error converting query to JSON";
        }
        String requestBody = String.format("{\"model\":\"%s\", \"stream\":false, \"messages\":[{\"role\": \"user\", \"content\": %s}]}",
                MODEL_NAME, jsonContent);

        String ACCESS_TOKEN = getGoogleAccessToken();
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl))
                .header("Authorization", "Bearer " + ACCESS_TOKEN)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody, StandardCharsets.UTF_8))
                .build();

        try {
            String rawStringResponse = client.send(request, HttpResponse.BodyHandlers.ofString()).body();
            if (!rawStringResponse.substring(0, 1).equals("{")) {
                System.out.println("Error in response:");
                System.out.println(rawStringResponse);
            } else {
                JSONObject json = JSONUtils.fromString(rawStringResponse);
                JSONArray choices = (JSONArray) json.get("choices");
                JSONObject choice = (JSONObject) choices.get(0);
                JSONObject message = (JSONObject) choice.get("message");
                return (String) message.get("content");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        throw new RuntimeException("Failed to get response from Llama model");
    }

    public static void main(String[] args) {
        LLMAccess llm = new LLMAccess(LLM_MODEL.LLAMA, LLM_SIZE.SMALL, "llm_log.txt");
        llm.getResponse("What is the average lifespan of a Spanish Armadillo?");
        llm.getResponse("What is the lifecycle of the European Firefly?", LLM_MODEL.OPENAI, LLM_SIZE.SMALL);
    }

    private static String getGoogleAccessToken() {
        try {
            GoogleCredentials credentials = GoogleCredentials.getApplicationDefault()
                    .createScoped(Collections.singletonList("https://www.googleapis.com/auth/cloud-platform"));
            credentials.refreshIfExpired();
            AccessToken accessToken = credentials.getAccessToken();
            return accessToken.getTokenValue();
        } catch (IOException e) {
            System.out.println("Error getting Google access token: " + e.getMessage());
            throw new RuntimeException("Failed to get Google access token", e);
        }
    }

}
