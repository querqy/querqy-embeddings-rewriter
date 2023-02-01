package querqy.embeddings;

import io.reactivex.Single;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface OpenAiApi {
  @POST("/v1/embeddings")
  Single<EmbeddingResult> createEmbeddings(@Body EmbeddingRequest request);
}
