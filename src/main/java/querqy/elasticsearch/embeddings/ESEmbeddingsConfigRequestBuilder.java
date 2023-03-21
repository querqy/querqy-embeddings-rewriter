package querqy.elasticsearch.embeddings;

import java.util.HashMap;
import java.util.Map;

import querqy.elasticsearch.rewriterstore.RewriterConfigMapping;
import querqy.embeddings.EmbeddingModel;

public class ESEmbeddingsConfigRequestBuilder extends RewriterConfigMapping {

    private Map<String, Object> modelConfig;

    public ESEmbeddingsConfigRequestBuilder() {
        super(ElasticsearchEmbeddingsRewriterFactory.class);
    }

    @Override
    public String getRewriterClassNameProperty() {
        return null;
    }

    @Override
    public String getConfigStringProperty() {
        return null;
    }

    @Override
    public String getInfoLoggingProperty() {
        return null;
    }

    @Override
    public String getRewriterClassName(String s , Map<String, Object> map) {
        return null;
    }

    @Override
    public Map<String, Object> getInfoLoggingConfig(String s , Map<String, Object> map) {
        return null;
    }

    @Override
    public Map<String, Object> buildConfig() {
        if (modelConfig == null) {
            throw new IllegalStateException("Missing: model");
        }
        return Map.of(ElasticsearchEmbeddingsRewriterFactory.CONF_MODEL, modelConfig);
    }

    public ESEmbeddingsConfigRequestBuilder model(final Class<? extends EmbeddingModel> modelClass,
                                                  final Map<String, Object> config) {
        if (config != null && config.containsKey(ElasticsearchEmbeddingsRewriterFactory.CONF_CLASS)) {
            throw new IllegalArgumentException("Property " + ElasticsearchEmbeddingsRewriterFactory.CONF_CLASS +
                    " not allowed in config");
        }
        modelConfig = config != null ? new HashMap<>(config) : new HashMap<>();
        modelConfig.put(ElasticsearchEmbeddingsRewriterFactory.CONF_CLASS, modelClass.getName());
        return this;

    }
}
