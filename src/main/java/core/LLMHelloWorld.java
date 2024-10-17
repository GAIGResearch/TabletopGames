package core;


import dev.langchain4j.chain.ConversationalChain;
import dev.langchain4j.chain.ConversationalRetrievalChain;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.huggingface.HuggingFaceChatModel;
import dev.langchain4j.model.huggingface.HuggingFaceEmbeddingModel;
import dev.langchain4j.model.input.Prompt;
import dev.langchain4j.model.input.PromptTemplate;
import dev.langchain4j.model.input.structured.StructuredPrompt;
import dev.langchain4j.model.input.structured.StructuredPromptProcessor;
import dev.langchain4j.retriever.EmbeddingStoreRetriever;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static dev.langchain4j.data.document.FileSystemDocumentLoader.loadDocument;
import static java.time.Duration.ofSeconds;
import static java.util.stream.Collectors.joining;
public class LLMHelloWorld {

    interface Assistant {

        String chat(String message);
    }

    public static void main(String[] args) {

        final String modelID = "timdettmers/guanaco-33b-merged";
//        final String modelID = "sentence-transformers/all-MiniLM-L6-v2";
//        final String modelID = "gpt2";
        BufferedReader brTest = null;
        String keyText = null;
        try {
            brTest = new BufferedReader(new FileReader("./HF_KEY.txt"));
            keyText = brTest.readLine();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        String question = "What do you know about The Resistance, the board game?";

        // Simple Q/A (works with guanaco)
        dev.langchain4j.model.huggingface.HuggingFaceChatModel chatModel = dev.langchain4j.model.huggingface.HuggingFaceChatModel.builder()
            .accessToken(keyText)
            .modelId(modelID)
            .waitForModel(true)
            .timeout(ofSeconds(60))
            .maxNewTokens(200)
            .temperature(0.1)
            .returnFullText(true)
            .build();

        AiMessage aiMessage = chatModel.sendUserMessage(question);
        String answer = aiMessage.text();
        System.out.println(answer);


        dev.langchain4j.model.huggingface.   HuggingFaceLanguageModel llm = dev.langchain4j.model.huggingface.HuggingFaceLanguageModel.builder()
                .accessToken(keyText)
                .modelId(modelID)
                .waitForModel(true)
                .timeout(ofSeconds(60))
                .maxNewTokens(1024)
                .temperature(0.1)
                .returnFullText(true)
                .build();


        String templateStr = "Answer the following question to the best of your ability:\n"
                + "\n"
                + "Question:\n"
                + "{{question}}\n";

        StructuredPromptProcessor processor = new StructuredPromptProcessor();




        PromptTemplate promptTemplate = PromptTemplate.from(templateStr);

        Map<String, Object> variables = new HashMap<>();
        variables.put("question", question);

        answer = llm.process(prompt.toUserMessage()); //llm.sendUserMessage(prompt.toUserMessage());

        AiMessage aiMessage = llm.sendUserMessage("What do you know about The Resistance, the board game?");
        String answer = aiMessage.text();
        System.out.println(answer);

        // FOR DOCUMENTS:
//        EmbeddingModel embeddingModel = HuggingFaceEmbeddingModel.builder()
//                .accessToken(keyText)
//                .modelId(modelID)
//                .waitForModel(true)
//                .timeout(ofSeconds(60))
//                .build();
//        EmbeddingStore<TextSegment> embeddingStore = new InMemoryEmbeddingStore<>();
//        EmbeddingStoreIngestor ingestor = EmbeddingStoreIngestor.builder()
//                .documentSplitter(DocumentSplitters.recursive(500))
//                .embeddingModel(embeddingModel)
//                .embeddingStore(embeddingStore)
//                .build();
//        Document document = loadDocument("infodoc.txt");
//        ingestor.ingest(document); //For supplying a document to the store

//        ConversationalRetrievalChain chain = ConversationalRetrievalChain.builder()
//                .chatLanguageModel(chatModel)
//                .retriever(EmbeddingStoreRetriever.from(embeddingStore, embeddingModel))
//                .build();

//        String answer = chain.execute("Hello, how are you?");
//        System.out.println(answer);


//        String question = "Hello, how are you?";
//        Embedding questionEmbedding = embeddingModel.embed(question);
//        int maxResults = 3;
//        double minScore = 0.8;
//        List<EmbeddingMatch<TextSegment>> relevantEmbeddings = embeddingStore.findRelevant(questionEmbedding, maxResults, minScore);
//
//        PromptTemplate promptTemplate = PromptTemplate.from(
//                "Answer the following question to the best of your ability:\n"
//                        + "\n"
//                        + "Question:\n"
//                        + "{{question}}\n");
//
//        Map<String, Object> variables = new HashMap<>();
//        variables.put("question", question);
//        Prompt prompt = promptTemplate.apply(variables);
//
//        AiMessage aiMessage = chatModel.sendUserMessage(prompt.toUserMessage());
//
//        // See an answer from the model
//        String answer = aiMessage.text();
//        System.out.println(answer);
    }
}
