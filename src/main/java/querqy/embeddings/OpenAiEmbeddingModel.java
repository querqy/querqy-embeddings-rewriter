package querqy.embeddings;

import lombok.Data;
import querqy.solr.utils.JsonUtil;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static java.nio.charset.StandardCharsets.UTF_8;
import static querqy.embeddings.MathUtil.norm;

public class OpenAiEmbeddingModel implements EmbeddingModel {
    private static final String CONTENT_TYPE_JSON = "application/json";

    private URL url;
    private String apiToken;

    private String model;


    @Override
    public void configure(final Map<String, Object> config) {
        try {
            this.url = new URL((String) config.get("url"));
            this.apiToken = getRequiredStringParam(config, "api_token");
            this.model = getRequiredStringParam(config, "open_ai_model");

            if (Boolean.TRUE.equals(config.get("test_on_configure"))) {
                getEmbedding("hello world");
            }

        } catch (final MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Embedding getEmbedding(final String text) {

        try {

            final String json = toJsonRequestBodyString(text);

            final HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("POST");
            con.setRequestProperty("Content-Type", CONTENT_TYPE_JSON);
            con.setRequestProperty("Accept", CONTENT_TYPE_JSON);
            con.setRequestProperty("Authorization" , "Bearer " + apiToken);
            con.setDoOutput(true);

            try(final OutputStream os = con.getOutputStream()) {
                final byte[] input = json.getBytes(UTF_8);
                os.write(input, 0, input.length);
            }

            final float[] emb = JsonUtil.readJson(con.getInputStream(), EmbeddingResult.class).getData()
                    .get(0).getEmbedding();

            return Embedding.of(norm(emb));


        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }

    private String toJsonRequestBodyString(final String text) {
        return JsonUtil.toJson(Map.of("model", model, "input", (Collections.singletonList(text))));

    }

    private String getRequiredStringParam(final Map<String, Object> config, final String paramName) {
        String result = ((String) config.get(paramName));
        if (result == null) {
            throw new IllegalArgumentException("Missing: " + paramName);
        }
        result = result.trim();
        if (result.isEmpty()) {
            throw new IllegalArgumentException("Missing: " + paramName);
        }

        return result;
    }

    /**
     * Represents an embedding returned by the embedding api
     *
     * https://beta.openai.com/docs/api-reference/classifications/create
     */
    @Data
    public static class OpenAiEmbedding {

        /**
         * The type of object returned, should be "embedding"
         */
        String object;

        /**
         * The embedding vector
         */
        float[] embedding;

        /**
         * The position of this embedding in the list
         */
        Integer index;
    }

    /**
     * An object containing a response from the answer api
     *
     * https://beta.openai.com/docs/api-reference/embeddings/create
     */
    @Data
    public static class EmbeddingResult {

        Object usage;

        /**
         * The GPT-3 model used for generating embeddings
         */
        String model;

        /**
         * The type of object returned, should be "list"
         */
        String object;

        /**
         * A list of the calculated embeddings
         */
        List<OpenAiEmbedding> data;
    }
}
