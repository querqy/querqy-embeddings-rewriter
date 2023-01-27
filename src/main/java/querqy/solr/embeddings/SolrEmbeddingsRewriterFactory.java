package querqy.solr.embeddings;

import querqy.embeddings.EmbeddingModel;
import querqy.embeddings.EmbeddingsRewriterFactory;
import querqy.rewrite.RewriterFactory;
import querqy.solr.SolrRewriterFactoryAdapter;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class SolrEmbeddingsRewriterFactory extends SolrRewriterFactoryAdapter {

    private EmbeddingModel model;

    public SolrEmbeddingsRewriterFactory(final String rewriterId) {
        super(rewriterId);
    }



    @Override
    public void configure(final Map<String, Object> config) {
        // The config would probably hold a pointer from where to load an embeddings model
        // This is just a dummy model that knows 4 words:
        this.model = new EmbeddingModel();


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
    public RewriterFactory getRewriterFactory() {
        return new EmbeddingsRewriterFactory(getRewriterId(), model);
    }


}


