package ma.emsi.othmanemoussawi.tp4othmanemoussawi.llm;

import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.google.genai.GoogleGenAiChatModel;
import dev.langchain4j.rag.DefaultRetrievalAugmentor;
import dev.langchain4j.rag.RetrievalAugmentor;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.query.router.LanguageModelQueryRouter;
import dev.langchain4j.rag.query.router.QueryRouter;
import dev.langchain4j.service.AiServices;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import ma.emsi.othmanemoussawi.tp4othmanemoussawi.rag.MagasinEmbeddings;
import ma.emsi.othmanemoussawi.tp4othmanemoussawi.rag.QueryRouterAvecOuSansRag;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Classe métier qui gère l'interface avec le LLM (Gemini) en utilisant LangChain4j.
 * Les réponses utilisent le RAG avec 2 fichiers PDF.
 */
@Dependent
public class LlmClient implements Serializable {

    /**
     * Rôle choisi par l'utilisateur pour l'assistant IA.
     */
    private String systemRole;
    /**
     * Assistant implémenté par LangChain4j (AiServices).
     */
    private Assistant assistant;
    /**
     * Mémoire de l'assistant pour garder l'historique de la conversation.
     */
    private ChatMemory chatMemory;

    /**
     * Les embeddings des PDF, calculés une seule fois au démarrage de l'application.
     */
    @Inject
    private MagasinEmbeddings magasinEmbeddings;

    @PostConstruct
    private void init() {
        // Récupère la clé secrète pour travailler avec l'API du LLM
        String llmKey = System.getenv("GEMINI_KEY");

        ChatModel model = GoogleGenAiChatModel.builder()
                .apiKey(llmKey)
                .modelName("gemini-3.5-flash-lite")
                .temperature(0.3)
                .logRequestsAndResponses(true)
                .build();

        // Test 3 : routage. Le LM choisit la source à utiliser à partir de ces descriptions.
        Map<ContentRetriever, String> descriptions = new HashMap<>();
        descriptions.put(magasinEmbeddings.getContentRetrieverIa(),
                "Support de cours sur l'intelligence artificielle : le RAG (Retrieval Augmented Generation), "
                        + "le fine-tuning, les embeddings et LangChain4j");
        descriptions.put(magasinEmbeddings.getContentRetrieverCuisine(),
                "Document sur la cuisine marocaine : plats traditionnels (tajine, couscous, harira, pastilla), "
                        + "recettes, thé à la menthe et pâtisseries");
        QueryRouter routeurEntreLesPdf = new LanguageModelQueryRouter(model, descriptions);

        // Test 4 : pas de RAG si la question ne concerne aucun des 2 PDF (par exemple "Bonjour").
        QueryRouter queryRouter = new QueryRouterAvecOuSansRag(model, routeurEntreLesPdf);

        RetrievalAugmentor retrievalAugmentor = DefaultRetrievalAugmentor.builder()
                .queryRouter(queryRouter)
                .build();

        this.chatMemory = MessageWindowChatMemory.withMaxMessages(20);
        this.assistant = AiServices.builder(Assistant.class)
                .chatModel(model)
                .chatMemory(chatMemory)
                .retrievalAugmentor(retrievalAugmentor)
                .build();
    }

    /**
     * Met le rôle système. Vide la mémoire avant d'y ajouter le rôle système.
     *
     * @param systemRole le rôle système.
     */
    public void setSystemRole(String systemRole) {
        this.systemRole = systemRole;
        this.chatMemory.clear();
        this.chatMemory.add(SystemMessage.from(systemRole));
    }

    /**
     * Envoie une question au LLM et retourne sa réponse.
     *
     * @param question la question.
     * @return la réponse du LLM.
     */
    public String envoyerRequete(String question) {
        return this.assistant.chat(question);
    }
}
