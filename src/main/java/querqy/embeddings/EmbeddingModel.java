package querqy.embeddings;

import java.util.Map;

public interface EmbeddingModel {

    default void configure(Map<String, Object> config) {}


    Embedding getEmbedding(String text);
}
