package querqy.embeddings;

import java.util.Map;
import java.util.Random;
import java.util.stream.DoubleStream;

/**
 * This is just a dummy - it could later be used as a Wrapper around SBERT etc.
 */
public class EmbeddingModel {

    final static Map<String, float[]> EMBEDDINGS = Map.of(
            "w1", norm(new float[] {0.1f, -0.006f, -0.9f, 0.25f}),
            "w2", norm(new float[] {-0.1f, 0.006f, 0.9f, -0.25f}),
            "w3", norm(new float[] {0.75f, 0.006f, -0.03f, -0.25f}),
            "w4", norm(new float[] {-0.07f, 0.01f, 0.8f, -0.18f})
            );

    public float[] getEmbedding(final String text) {
        float[] emb = EMBEDDINGS.get(text);
        if (emb == null) {
            // use a stable (but meaningless) random vector if we don't have a pre-defined embedding
            final Random random = new Random(text.hashCode());
            emb = new float[4];
            for (int i = 0; i < emb.length; i++) {
                emb[i] = random.nextFloat();
            }
        }
        return emb;
    }

    /**
     * Normalise vector to unit length
     *
     * @param vec
     * @return
     */
    public static float[] norm(final float[] vec) {
        float sum = 0;
        for (final float val : vec) {
            sum += val*val;
        }
        float n = (float) Math.sqrt(sum);
        float[] uvec = new float[vec.length];
        for (int i = 0; i < vec.length; i++) {
            uvec[i] = vec[i] / n;
        }
        return uvec;
    }
}
