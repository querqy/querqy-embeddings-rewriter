package querqy.embeddings;

import org.apache.solr.search.SolrCache;

import java.util.Map;
import java.util.function.Supplier;

public interface EmbeddingModel {

    default void configure(final Map<String, Object> config, final EmbeddingCache<String> embeddingCache) {
    }


    Embedding getEmbedding(String text);
}
