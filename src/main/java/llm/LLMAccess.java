package llm;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.vertexai.VertexAiGeminiChatModel;
import dev.langchain4j.model.mistralai.MistralAiChatModel;
import dev.langchain4j.model.mistralai.MistralAiChatModelName;

public class LLMAccess {

    static ChatLanguageModel geminiModel;
    static ChatLanguageModel mistralModel;
    String mistralToken = System.getenv("MISTRAL_TOKEN");
    String geminiProject = System.getenv("GEMINI_PROJECT");
    String geminiLocation = "europe-west2";

    LLM_MODEL modelType;

    public enum LLM_MODEL {
        GEMINI,
        MISTRAL
    }


    public LLMAccess(LLM_MODEL modelType) {
        this.modelType = modelType;
        if (!geminiProject.isEmpty()) {
            try {
                geminiModel = VertexAiGeminiChatModel.builder()
                        .project(geminiProject)
                        .location(geminiLocation)
                        //      .temperature(1.0f)  // between 0 and 2; default 1.0 for pro-1.5
                        //       .topK(40) // some models have a three-stage sampling process. topK; then topP; then temperature
                        //       .topP(0.94f)  // 1.5 default is 0.64; the is the sum of probability of tokens to sample from
                   //     .maxOutputTokens(1000)  // max replay size (max is 8192)
                        .modelName("gemini-1.5-flash-001")
                        .build();
            } catch (Error e) {
                System.out.println("Error creating Gemini model: " + e.getMessage());
            }
        }

        if (!mistralToken.isEmpty()) {
            mistralModel = MistralAiChatModel.builder()
                    .modelName(MistralAiChatModelName.MISTRAL_MEDIUM_LATEST)
                    .apiKey(mistralToken)
                    .build();
        }
    }

    /**
     * Gets some text from the specified model
     * @param query the prompt sent to the model
     * @param modelType the LLM model to use
     * @return The full text returned by the model; or an empty string if no valid model
     */
    public String getResponse(String query, LLM_MODEL modelType) {
        String response = "";
        if (modelType == LLM_MODEL.MISTRAL && mistralModel != null) {
            response = mistralModel.generate(query);
            String output = String.format("\nModel: %s\nQuery: %s\nResponse: %s\n", "Mistral", query, response);
            System.out.println(output);
        }
        if (modelType == LLM_MODEL.GEMINI && geminiModel != null) {
            response = geminiModel.generate(query);
            String output = String.format("\nModel: %s\nQuery: %s\nResponse: %s\n", "Gemini", query, response);
            System.out.println(output);
        }
        return response;
    }

    /**
     * This will use the default LLM model specified in the constructor
     * @param query the prompt sent to the model
     * @return The full text returned by the model; or an empty string if no valid model
     */
    public String getResponse(String query) {
        return getResponse(query, this.modelType);
    }

    public static void main(String[] args) {
        LLMAccess llm = new LLMAccess(LLM_MODEL.MISTRAL);
        llm.getResponse("What is the average lifespan of a Spanish Armadillo?");

        llm.getResponse("What is the lifecycle of the European Firefly?", LLM_MODEL.GEMINI);
    }
}
