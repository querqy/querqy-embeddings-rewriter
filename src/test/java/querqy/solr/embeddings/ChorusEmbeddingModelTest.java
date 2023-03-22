package querqy.solr.embeddings;

import org.junit.Assert;
import org.junit.Test;
import querqy.embeddings.ChorusEmbeddingModel;
import querqy.embeddings.Embedding;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

public class ChorusEmbeddingModelTest {

    @Test
    public void testParseJson() {
        String embeddingJson = "{ \"embedding\": [0.3, 1, 5] }";
        Embedding e = new ChorusEmbeddingModel().parseEmbeddingFromResponse(new ByteArrayInputStream(embeddingJson.getBytes(StandardCharsets.UTF_8)));
        Assert.assertArrayEquals(e.asVector(), new float[] { 0.3f, 1f, 5f}, 0f);
    }
}
