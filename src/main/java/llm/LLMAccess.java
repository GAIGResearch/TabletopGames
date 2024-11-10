package llm;

import dev.langchain4j.model.anthropic.AnthropicChatModel;
import dev.langchain4j.model.anthropic.AnthropicChatModelName;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.mistralai.MistralAiChatModelName;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiChatModelName;
import dev.langchain4j.model.openai.OpenAiTokenizer;
import dev.langchain4j.model.vertexai.VertexAiGeminiChatModel;
import dev.langchain4j.model.mistralai.MistralAiChatModel;

import java.io.File;
import java.io.FileWriter;

public class LLMAccess {

    static ChatLanguageModel[] geminiModel = new ChatLanguageModel[2];
    static ChatLanguageModel[] mistralModel = new ChatLanguageModel[2];
    static ChatLanguageModel[] openaiModel = new ChatLanguageModel[2];
    static ChatLanguageModel[] anthropicModel = new ChatLanguageModel[2];
    static ChatLanguageModel[] llamaModel = new ChatLanguageModel[2];

    static OpenAiTokenizer tokenizer = new OpenAiTokenizer();

    String mistralToken = System.getenv("MISTRAL_TOKEN");
    String geminiProject = System.getenv("GEMINI_PROJECT");
    String openaiToken = System.getenv("OPENAI_TOKEN");
    String anthropicToken = System.getenv("ANTHROPIC_TOKEN");

    File logFile;
    FileWriter logWriter;

    String geminiLocation = "europe-west2";
    String llamaLocation = "us-central1";

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
                        .modelName("gemini-1.5-pro") // $0.075 per million characters output, $0.01875 per million characters input
                        .build();
                geminiModel[0] = VertexAiGeminiChatModel.builder()
                        .project(geminiProject)
                        .location(geminiLocation)
                        .modelName("gemini-1.5-flash")
                        .build();
            } catch (Error e) {
                System.out.println("Error creating Gemini model: " + e.getMessage());
            }


            try {
                llamaModel[1] = VertexAiGeminiChatModel.builder()
                        .project(geminiProject)
                        .location(llamaLocation)
                        //      .temperature(1.0f)  // between 0 and 2; default 1.0 for pro-1.5
                        //       .topK(40) // some models have a three-stage sampling process. topK; then topP; then temperature
                        //       .topP(0.94f)  // 1.5 default is 0.64; the is the sum of probability of tokens to sample from
                        //     .maxOutputTokens(1000)  // max replay size (max is 8192)
                        // .modelName("gemini-1.5-pro")   // $1.25 per million characters input, $0.3125 per million output
                        .modelName("llama3-405b-instruct-maas") // $0.075 per million characters output, $0.01875 per million characters input
                        .build();
                llamaModel[0] = VertexAiGeminiChatModel.builder()
                        .project(geminiProject)
                        .location(llamaLocation)
                        .modelName("llama3-70b-instruct-maas")
                        .build();
            } catch (Error e) {
                System.out.println("Error creating Llama model: " + e.getMessage());
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
        }

        if (anthropicToken != null && !anthropicToken.isEmpty()) {
            anthropicModel[0] = AnthropicChatModel.builder()
                    .modelName(AnthropicChatModelName.CLAUDE_3_HAIKU_20240307) // $0.25 per million input tokens, $1.25 per million output tokens
                    .apiKey(anthropicToken)
                    .maxTokens(4096)
                    .build();
            anthropicModel[1] = AnthropicChatModel.builder()
                    .modelName(AnthropicChatModelName.CLAUDE_3_5_SONNET_20240620) // $3 per million input tokens, $15 per million output tokens
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
        ChatLanguageModel modelToUse = switch(modelType) {
            case MISTRAL -> modelSize == LLM_SIZE.SMALL ? mistralModel[0] : mistralModel[1];
            case GEMINI -> modelSize == LLM_SIZE.SMALL ? geminiModel[0] : geminiModel[1];
            case OPENAI -> modelSize == LLM_SIZE.SMALL ? openaiModel[0] : openaiModel[1];
            case ANTHROPIC -> modelSize == LLM_SIZE.SMALL ? anthropicModel[0] : anthropicModel[1];
            case LLAMA -> modelSize == LLM_SIZE.SMALL ? llamaModel[0] : llamaModel[1];
        };
        if (modelToUse != null) {
            try {
                inputTokens += tokenizer.estimateTokenCountInText(query);
                response = modelToUse.generate(query);
                outputTokens += tokenizer.estimateTokenCountInText(response);
            } catch (Exception e) {
                System.out.println("Error getting response from model: " + e.getMessage());
            }
        } else {
            System.out.println("No valid model available for " + modelType + " " + modelSize);
            return "No reply available";
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
        LLMAccess llm = new LLMAccess(LLM_MODEL.OPENAI, LLM_SIZE.LARGE, "llm_log.txt");
        llm.getResponse("What is the average lifespan of a Spanish Armadillo?");
        llm.getResponse("What is the lifecycle of the European Firefly?", LLM_MODEL.OPENAI, LLM_SIZE.SMALL);
    }
}
