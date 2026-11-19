package ma.emsi.othmanemoussawi.tp4othmanemoussawi.rag;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentParser;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.loader.ClassPathDocumentLoader;
import dev.langchain4j.data.document.parser.apache.tika.ApacheTikaDocumentParser;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.onnx.allminilml6v2.AllMiniLmL6V2EmbeddingModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Initialized;
import jakarta.enterprise.event.Observes;

import java.util.List;

/**
 * Phase 1 du RAG, faite une seule fois pour toute l'application :
 * les 2 fichiers PDF sont découpés en segments et leurs embeddings sont enregistrés
 * dans 2 magasins d'embeddings différents.
 */
@ApplicationScoped
public class MagasinEmbeddings {

    private EmbeddingModel embeddingModel;
    private ContentRetriever contentRetrieverIa;
    private ContentRetriever contentRetrieverCuisine;

    /**
     * Force la création du bean (et donc l'ingestion des PDF) au démarrage de l'application,
     * plutôt qu'à la première question posée.
     */
    public void init(@Observes @Initialized(ApplicationScoped.class) Object event) {
        // Rien à faire : l'appel de cette méthode suffit pour que @PostConstruct soit exécuté.
    }

    @PostConstruct
    private void ingererDocuments() {
        this.embeddingModel = new AllMiniLmL6V2EmbeddingModel();
        this.contentRetrieverIa = creerContentRetriever(ingerer("rag.pdf"));
        this.contentRetrieverCuisine = creerContentRetriever(ingerer("cuisine.pdf"));
    }

    /**
     * Parsing, découpage, calcul des embeddings et enregistrement d'un fichier PDF.
     *
     * @param cheminRessource chemin du fichier, relatif à src/main/resources.
     */
    private EmbeddingStore<TextSegment> ingerer(String cheminRessource) {
        DocumentParser documentParser = new ApacheTikaDocumentParser();
        // Le class loader de l'application Web, qui connaît WEB-INF/classes.
        Document document = ClassPathDocumentLoader.loadDocument(cheminRessource, documentParser,
                MagasinEmbeddings.class.getClassLoader());
        DocumentSplitter splitter = DocumentSplitters.recursive(300, 30);
        List<TextSegment> segments = splitter.split(document);
        List<Embedding> embeddings = embeddingModel.embedAll(segments).content();
        EmbeddingStore<TextSegment> embeddingStore = new InMemoryEmbeddingStore<>();
        embeddingStore.addAll(embeddings, segments);
        return embeddingStore;
    }

    private ContentRetriever creerContentRetriever(EmbeddingStore<TextSegment> embeddingStore) {
        return EmbeddingStoreContentRetriever.builder()
                .embeddingStore(embeddingStore)
                .embeddingModel(embeddingModel)
                .maxResults(2)
                .minScore(0.5)
                .build();
    }

    /**
     * @return le ContentRetriever pour le support de cours sur le RAG et le fine-tuning.
     */
    public ContentRetriever getContentRetrieverIa() {
        return contentRetrieverIa;
    }

    /**
     * @return le ContentRetriever pour le document sur la cuisine marocaine.
     */
    public ContentRetriever getContentRetrieverCuisine() {
        return contentRetrieverCuisine;
    }
}
