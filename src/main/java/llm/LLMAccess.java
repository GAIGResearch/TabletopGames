package llm;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.mistralai.MistralAiChatModelName;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiChatModelName;
import dev.langchain4j.model.vertexai.VertexAiGeminiChatModel;
import dev.langchain4j.model.mistralai.MistralAiChatModel;

import java.io.File;
import java.io.FileWriter;

public class LLMAccess {

    static ChatLanguageModel geminiModel;
    static ChatLanguageModel mistralModel;
    static ChatLanguageModel openaiModel;

    String mistralToken = System.getenv("MISTRAL_TOKEN");
    String geminiProject = System.getenv("GEMINI_PROJECT");
    String openaiToken = System.getenv("OPENAI_TOKEN");

    File logFile;
    FileWriter logWriter;

    String geminiLocation = "europe-west2";

    LLM_MODEL modelType;

    public enum LLM_MODEL {
        GEMINI,
        MISTRAL,
        OPENAI
    }


    public LLMAccess(LLM_MODEL modelType, String logFileName) {
        if (logFileName != null && !logFileName.isEmpty()) {
            logFile = new File(logFileName);
            try {
                logWriter = new FileWriter(logFile);
            } catch (Exception e) {
                System.out.println("Error creating log file: " + e.getMessage());
            }
        }
        this.modelType = modelType;
        if (geminiProject != null && !geminiProject.isEmpty()) {
            try {
                geminiModel = VertexAiGeminiChatModel.builder()
                        .project(geminiProject)
                        .location(geminiLocation)
                        //      .temperature(1.0f)  // between 0 and 2; default 1.0 for pro-1.5
                        //       .topK(40) // some models have a three-stage sampling process. topK; then topP; then temperature
                        //       .topP(0.94f)  // 1.5 default is 0.64; the is the sum of probability of tokens to sample from
                        //     .maxOutputTokens(1000)  // max replay size (max is 8192)
                        // .modelName("gemini-1.5-pro")   // $1.25 per million characters input, $0.3125 per million output
                        .modelName("gemini-1.5-flash") // $0.075 per million characters output, $0.01875 per million characters input
                        .build();
            } catch (Error e) {
                System.out.println("Error creating Gemini model: " + e.getMessage());
            }
        }

        if (mistralToken != null && !mistralToken.isEmpty()) {
            mistralModel = MistralAiChatModel.builder()
                    .modelName(MistralAiChatModelName.MISTRAL_LARGE_LATEST)
                    .apiKey(mistralToken)
                    .build();
            // $2 per million input tokens, $6 per million output tokens
        }

        if (openaiToken != null && !openaiToken.isEmpty()) {
            openaiModel = OpenAiChatModel.builder()
                    .modelName(OpenAiChatModelName.GPT_4_O) // $5 per million input tokens, $15 per million output tokens
            //        .modelName(OpenAiChatModelName.GPT_4_O_MINI) // $0.15 per million input tokens, $0.6 per million output tokens
                    .apiKey(openaiToken)
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
    public String getResponse(String query, LLM_MODEL modelType) {
        String response = "";
        if (modelType == LLM_MODEL.MISTRAL && mistralModel != null) {
            response = mistralModel.generate(query);
        }
        if (modelType == LLM_MODEL.GEMINI && geminiModel != null) {
            response = geminiModel.generate(query);
        }
        if (modelType == LLM_MODEL.OPENAI && openaiModel != null) {
            response = openaiModel.generate(query);
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
        return getResponse(query, this.modelType);
    }

    public static void main(String[] args) {
        LLMAccess llm = new LLMAccess(LLM_MODEL.OPENAI, "");
        llm.getResponse("What is the average lifespan of a Spanish Armadillo?");
        llm.getResponse("What is the lifecycle of the European Firefly?", LLM_MODEL.OPENAI);
    }
}
