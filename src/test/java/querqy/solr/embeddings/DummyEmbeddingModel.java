package querqy.solr.embeddings;

import querqy.embeddings.Embedding;
import querqy.embeddings.EmbeddingModel;

import java.util.Map;
import java.util.Random;

import static querqy.embeddings.MathUtil.norm;

public class DummyEmbeddingModel implements EmbeddingModel {

    final static Map<String, float[]> EMBEDDINGS = Map.of(
            "w1", norm(new float[] {0.1f, -0.006f, -0.9f, 0.25f}),
            "w2", norm(new float[] {-0.1f, 0.006f, 0.9f, -0.25f}),
            "w3", norm(new float[] {0.75f, 0.006f, -0.03f, -0.25f}),
            "w4", norm(new float[] {-0.07f, 0.01f, 0.8f, -0.18f})
            );


    @Override
    public Embedding getEmbedding(final String text) {
        float[] emb = EMBEDDINGS.get(text);
        if (emb == null) {
            // use a stable (but meaningless) random vector if we don't have a pre-defined embedding
            final Random random = new Random(text.hashCode());
            emb = new float[4];
            for (int i = 0; i < emb.length; i++) {
                emb[i] = random.nextFloat();
            }
        }
        return Embedding.of(emb);
    }

}
