package core;


import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.huggingface.HuggingFaceEmbeddingModel;

import java.io.BufferedReader;
import java.io.FileReader;

import static java.time.Duration.ofSeconds;
public class LLMHelloWorld {

    public static void main(String[] args) {

        BufferedReader brTest = null;
        String keyText = null;
        try {
            brTest = new BufferedReader(new FileReader("./HF_KEY.txt"));
            keyText = brTest.readLine();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        EmbeddingModel embeddingModel = HuggingFaceEmbeddingModel.builder()
                .accessToken(keyText)
                .modelId("sentence-transformers/all-MiniLM-L6-v2")
//                .modelId("meta-llama/Llama-2-7b") //Waiting for approval.
                .waitForModel(true)
                .timeout(ofSeconds(60))
                .build();

        Embedding embedding = embeddingModel.embed("Hello, how are you?");
        System.out.println(embedding);
    }
}
