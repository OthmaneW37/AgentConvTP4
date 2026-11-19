package ma.emsi.othmanemoussawi.tp4othmanemoussawi.rag;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.input.Prompt;
import dev.langchain4j.model.input.PromptTemplate;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.query.Query;
import dev.langchain4j.rag.query.router.QueryRouter;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

/**
 * Test 4 : pas de RAG si la question ne porte sur aucun des documents.
 * Le LM est d'abord interrogé pour savoir si le RAG est utile ; si c'est le cas,
 * le choix de la source est délégué à un autre QueryRouter (le routage du test 3).
 */
public class QueryRouterAvecOuSansRag implements QueryRouter {

    private static final PromptTemplate PROMPT_TEMPLATE = PromptTemplate.from(
            "Est-ce que la requête '{{requete}}' porte sur un de ces sujets : "
                    + "le RAG (Retrieval Augmented Generation), le fine-tuning des modèles de langage, "
                    + "ou la cuisine marocaine ? "
                    + "Réponds seulement par 'oui', 'non' ou 'peut-être'.");

    private final ChatModel chatModel;
    private final QueryRouter routeurSiRag;

    /**
     * @param chatModel    le LM qui décide s'il faut faire du RAG.
     * @param routeurSiRag le QueryRouter utilisé quand il faut faire du RAG.
     */
    public QueryRouterAvecOuSansRag(ChatModel chatModel, QueryRouter routeurSiRag) {
        this.chatModel = chatModel;
        this.routeurSiRag = routeurSiRag;
    }

    @Override
    public Collection<ContentRetriever> route(Query query) {
        Prompt prompt = PROMPT_TEMPLATE.apply(Map.of("requete", query.text()));
        String reponse = chatModel.chat(prompt.text());
        if (reponse.toLowerCase().contains("non")) {
            // Pas de RAG.
            return Collections.emptyList();
        }
        return routeurSiRag.route(query);
    }
}
