package querqy.embeddings;

import querqy.solr.utils.JsonUtil;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.nio.charset.StandardCharsets.UTF_8;

public class ChorusEmbeddingModel implements EmbeddingModel {

    private static final String CONTENT_TYPE_JSON = "application/json";

    private URL url;
     /*
     FIXME: This should really be a bounded LRU cache of embeddings, not an unbounded one.
     */
    private Map<String,Embedding> embeddings;
    private boolean normalize = true;

    @Override
    public void configure(final Map<String, Object> config) {
        try {
            this.embeddings = new HashMap<>();
            this.url = new URL((String) config.get("url"));
            if (Boolean.TRUE.equals(config.get("test_on_configure"))) {
                getEmbedding("hello world");
            }
            this.normalize = !Boolean.FALSE.equals(config.get("normalize"));

        } catch (final MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings (value="unchecked")
    @Override
    public Embedding getEmbedding(final String text) {
        Embedding embedding = embeddings.getOrDefault(text, null);
        if (embedding != null) return embedding;
        try {
            final String json = toJsonString(text);

            final HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("POST");
            con.setRequestProperty("Content-Type", CONTENT_TYPE_JSON);
            con.setRequestProperty("Accept", CONTENT_TYPE_JSON);
            con.setDoOutput(true);

            try (final OutputStream os = con.getOutputStream()) {
                final byte[] input = json.getBytes(UTF_8);
                os.write(input, 0, input.length);
            }
            embedding = Embedding.of((List<Double>) JsonUtil.readJson(con.getInputStream(), Map.class).get("embedding"));
            embeddings.put(text, embedding);
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    return embedding;
    }
    protected String toJsonString(final String text) {
        return JsonUtil.toJson(
                Map.of("text", text,
                        "output_format", "float_list",
                        "separator", ",",
                        "normalize", normalize

                ));
    }
}
