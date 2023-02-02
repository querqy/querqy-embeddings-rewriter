package querqy.embeddings;

public class MathUtil {

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
