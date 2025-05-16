package llm;

import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.vertexai.*;
import com.google.cloud.vertexai.api.*;
import com.google.cloud.vertexai.generativeai.GenerativeModel;
import dev.langchain4j.model.anthropic.AnthropicChatModel;
import dev.langchain4j.model.anthropic.AnthropicChatModelName;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.mistralai.MistralAiChatModelName;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiChatModelName;
import dev.langchain4j.model.openai.OpenAiTokenCountEstimator;
import dev.langchain4j.model.vertexai.VertexAiGeminiChatModel;
import dev.langchain4j.model.mistralai.MistralAiChatModel;

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

    static VertexAiGeminiChatModel[] geminiModel = new VertexAiGeminiChatModel[2];
    static MistralAiChatModel[] mistralModel = new MistralAiChatModel[2];
    static OpenAiChatModel[] openaiModel = new OpenAiChatModel[2];
    static AnthropicChatModel[] anthropicModel = new AnthropicChatModel[2];

    static OpenAiTokenCountEstimator tokenizer = new OpenAiTokenCountEstimator("o200k_base");

    String mistralToken = System.getenv("MISTRAL_TOKEN");
    String geminiProject = System.getenv("GEMINI_PROJECT");
    String openaiToken = System.getenv("OPENAI_TOKEN");
    String anthropicToken = System.getenv("ANTHROPIC_TOKEN");

    File logFile;
    FileWriter logWriter;

    String geminiLocation = "europe-west9";
    String llamaLocation = "us-central1";

    VertexAI vertexAI;

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
        LARGE
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
            } catch (Error e) {
                System.out.println("Error creating Gemini model: " + e.getMessage());
            }
        }

//        if (geminiProject != null && !geminiProject.isEmpty() && llamaLocation != null && !llamaLocation.isEmpty()) {
//            try {
//                llamaModel[1] = VertexAiGeminiChatModel.builder()
//                        .project(geminiProject)
//                        .location(llamaLocation)
//                        //      .temperature(1.0f)  // between 0 and 2; default 1.0 for pro-1.5
//                        //       .topK(40) // some models have a three-stage sampling process. topK; then topP; then temperature
//                        //       .topP(0.94f)  // 1.5 default is 0.64; the is the sum of probability of tokens to sample from
//                        //     .maxOutputTokens(1000)  // max replay size (max is 8192)
//                        // .modelName("gemini-1.5-pro")   // $1.25 per million characters input, $0.3125 per million output
//                        .modelName("meta/llama-4-maverick-17b-128e-instruct-maas") // $0.075 per million characters output, $0.01875 per million characters input
//                        .build();
//                llamaModel[0] = VertexAiGeminiChatModel.builder()
//                        .project(geminiProject)
//                        .location(llamaLocation)
//                        .modelName("meta/llama-3.1-405b-instruct-maas")
//                        .build();
//            } catch (Error e) {
//                System.out.println("Error creating Llama model: " + e.getMessage());
//            }
//        }

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
            String ENDPOINT = llamaLocation + "-aiplatform.googleapis.com";
            String MODEL_NAME = modelSize == LLM_SIZE.SMALL ? "meta/llama-3.1-70b-instruct-maas" : "meta/llama-4-maverick-17b-128e-instruct-maas";
            String apiUrl = String.format("https://%s/v1/projects/%s/locations/%s/endpoints/openapi/chat/completions",
                    ENDPOINT, geminiProject, llamaLocation);
            String requestBody = String.format("{\"model\":\"%s\", \"stream\":false, \"messages\":[{\"role\": \"user\", \"content\": \"%s\"}]}",
                    MODEL_NAME, query);
            String ACCESS_TOKEN = getGoogleAccessToken();
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl))
                    .header("Authorization", "Bearer " + ACCESS_TOKEN)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody, StandardCharsets.UTF_8))
                    .build();

            try {
                response = client.send(request, HttpResponse.BodyHandlers.ofString()).body();
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            ChatModel modelToUse = switch (modelType) {
                case MISTRAL -> modelSize == LLM_SIZE.SMALL ? mistralModel[0] : mistralModel[1];
                case GEMINI -> modelSize == LLM_SIZE.SMALL ? geminiModel[0] : geminiModel[1];
                case OPENAI -> modelSize == LLM_SIZE.SMALL ? openaiModel[0] : openaiModel[1];
                case ANTHROPIC -> modelSize == LLM_SIZE.SMALL ? anthropicModel[0] : anthropicModel[1];
                default -> throw new IllegalArgumentException("Unknown model type: " + modelType);
            };
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
