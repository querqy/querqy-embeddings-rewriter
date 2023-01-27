package querqy.solr.embeddings;

import querqy.solr.RewriterConfigRequestBuilder;
import querqy.solr.SolrRewriterFactoryAdapter;

import java.util.Collections;
import java.util.Map;

public class EmbeddingsConfigRequestBuilder extends RewriterConfigRequestBuilder {
    public EmbeddingsConfigRequestBuilder() {
        super(SolrEmbeddingsRewriterFactory.class);
    }

    @Override
    public Map<String, Object> buildConfig() {
        // we can later put the path to the model file etc. into the config
        return Collections.emptyMap();
    }
}
