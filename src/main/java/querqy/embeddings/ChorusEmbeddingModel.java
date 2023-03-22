package querqy.embeddings;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import querqy.solr.utils.JsonUtil;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static java.nio.charset.StandardCharsets.UTF_8;

public class ChorusEmbeddingModel implements EmbeddingModel {

    private static final String CONTENT_TYPE_JSON = "application/json";

    private static final ObjectMapper objectMapper = new ObjectMapper();

    private URL url;

    private boolean normalize = true;

    private EmbeddingCache<String> embeddingsCache;

    @Override
    public void configure(final Map<String, Object> config, final EmbeddingCache<String> embeddingsCache) {
        try {
            this.url = new URL((String) config.get("url"));
            if (Boolean.TRUE.equals(config.get("test_on_configure"))) {
                getEmbedding("hello world");
            }
            this.normalize = !Boolean.FALSE.equals(config.get("normalize"));
            this.embeddingsCache = embeddingsCache;

        } catch (final MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Embedding getEmbedding(final String text) {

        try {

            final String json = toJsonString(text);
            final String cacheKey = url.toString() + "|" + json;
            Embedding embedding = embeddingsCache.getEmbedding(cacheKey);
            if (embedding != null) {
                return embedding;
            }

            final HttpURLConnection con = (HttpURLConnection)url.openConnection();
            con.setRequestMethod("POST");
            con.setRequestProperty("Content-Type", CONTENT_TYPE_JSON);
            con.setRequestProperty("Accept", CONTENT_TYPE_JSON);
            //con.setRequestProperty("Connection", "Close");
            //con.setConnectTimeout(10);
            //con.setReadTimeout(30);
            con.setDoOutput(true);


            try(final OutputStream os = con.getOutputStream()) {
                final byte[] input = json.getBytes(UTF_8);
                os.write(input, 0, input.length);
            }

            embedding = parseEmbeddingFromResponse(con.getInputStream());
            embeddingsCache.putEmbedding(cacheKey, embedding);
            return embedding;

        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }

    public Embedding parseEmbeddingFromResponse(InputStream is) {
        try {
            JsonNode responseTree = objectMapper.readTree(is);
            List<Double> embedding = objectMapper.convertValue(responseTree.path("embedding"), new TypeReference<>() {});
            return Embedding.of(embedding);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected String toJsonString(final String text) {
        return JsonUtil.toJson(
                // sorting the keys so that we get a stable key for the cache lookup. Maybe this
                // can be configured in Jackson?
                new TreeMap<String, Object>(Map.of("text", text,
                        "output_format", "float_list",
                        "separator", ",",
                        "normalize", normalize

                )));
    }

}
