package querqy.embeddings;

import java.util.List;

import junit.framework.TestCase;
import org.junit.Assert;
import org.junit.Test;

public class EmbeddingModelTest extends TestCase {

  @Test
  public void testGetOpenAiEncodings() {
    String token_to_encode = "bluetooth speaker";
    EmbeddingModel embeddingModel = new EmbeddingModel();
    List<Double> embeddings = embeddingModel.getOpenAiEncodings(token_to_encode);
    System.out.println(embeddings);
    Assert.assertFalse(embeddings.isEmpty());
  }
}