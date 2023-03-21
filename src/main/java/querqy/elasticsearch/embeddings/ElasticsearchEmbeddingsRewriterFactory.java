package querqy.elasticsearch.embeddings;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.elasticsearch.ElasticsearchException;
import querqy.embeddings.EmbeddingModel;
import querqy.embeddings.EmbeddingsRewriterFactory;
import querqy.rewrite.RewriterFactory;
import querqy.elasticsearch.ESRewriterFactory;

public class ElasticsearchEmbeddingsRewriterFactory extends ESRewriterFactory {

    public static final String CONF_MODEL = "model";
    public static final String CONF_CLASS = "class";

    private EmbeddingModel model;

    public ElasticsearchEmbeddingsRewriterFactory(final String rewriterId) {
        super(rewriterId);
    }

    @Override
    public void configure(final Map<String, Object> config) {
        final Map<String, Object> modelConfig = (Map<String, Object>) config.get(CONF_MODEL);
        if (modelConfig == null) {
            throw new IllegalArgumentException("Missing config property" + CONF_MODEL);
        }
        final EmbeddingModel embeddingModel = getInstanceFromArg(modelConfig, CONF_CLASS, null);
        if (embeddingModel == null) {
            throw new IllegalArgumentException("Missing " + CONF_MODEL + "/" + CONF_CLASS + "  property");
        }

        //embeddingModel.configure(modelConfig, cache);

        this.model = embeddingModel;


    }

    @Override
    public List<String> validateConfiguration(final Map<String, Object> config) {
        try {
            // TODO: provide some more meaningful error details (for now we just try to configure ourselves and
            //  see what happens)
            configure(config);
        } catch (final Exception e) {
            return Collections.singletonList("Cannot configure this EmbeddingsRewriterFactory because: " +
                    e.getMessage());

        }
        return Collections.emptyList(); // it worked, no error message
    }

    @Override
    public RewriterFactory createRewriterFactory(org.elasticsearch.index.shard.IndexShard indexShard)
        throws ElasticsearchException {
        return new EmbeddingsRewriterFactory(getRewriterId(), model);
    }

    // TODO: copied from query-solr ConfigUtils. Make it public there!
    static <V> V getInstanceFromArg(final Map<String, Object> config, final String propertyName, final V defaultValue) {

        final String classField = (String) config.get(propertyName);
        if (classField == null) {
            return defaultValue;
        }

        final String className = classField.trim();
        if (className.isEmpty()) {
            return defaultValue;
        }

        try {
            return (V) Class.forName(className).newInstance();
        } catch (final ClassNotFoundException | InstantiationException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }

    }
}


