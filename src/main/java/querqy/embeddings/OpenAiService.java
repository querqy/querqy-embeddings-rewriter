package querqy.embeddings;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import okhttp3.ConnectionPool;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.jackson.JacksonConverterFactory;

public class OpenAiService {
  final OpenAiApi api;
  String API_TOKEN = "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";

  public OpenAiService(final String baseUrl , final Duration timeout) {
    ObjectMapper mapper = new ObjectMapper();
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES , false);
    mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    mapper.setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE);

    OkHttpClient client = new OkHttpClient.Builder()
        .addInterceptor(new Interceptor() {
          @Override
          public Response intercept(Chain chain) throws IOException {
            Request request = chain.request()
                .newBuilder()
                .header("Authorization" , "Bearer " + API_TOKEN)
                .build();
            return chain.proceed(request);
          }
        })
        .connectionPool(new ConnectionPool(5 , 1 , TimeUnit.SECONDS))
        .readTimeout(timeout.toMillis() , TimeUnit.MILLISECONDS)
        .build();

    Retrofit retrofit = new Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(client)
        .addConverterFactory(JacksonConverterFactory.create(mapper))
        .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
        .build();

    this.api = retrofit.create(OpenAiApi.class);
  }

  public EmbeddingResult createEmbeddings(EmbeddingRequest request) {
    return api.createEmbeddings(request).blockingGet();
  }
}
