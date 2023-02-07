package querqy.embeddings;

import querqy.CompoundCharSequence;
import querqy.model.AbstractNodeVisitor;
import querqy.model.BoostQuery;
import querqy.model.Clause;
import querqy.model.ExpandedQuery;
import querqy.model.MatchAllQuery;
import querqy.model.Node;
import querqy.model.ParsedRawQuery;
import querqy.model.QuerqyQuery;
import querqy.model.Query;
import querqy.model.StringRawQuery;
import querqy.model.Term;
import querqy.rewrite.QueryRewriter;
import querqy.rewrite.RewriterOutput;
import querqy.rewrite.SearchEngineRequestAdapter;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class EmbeddingsRewriter extends AbstractNodeVisitor<Node> implements QueryRewriter {


    public enum EmbeddingQueryMode {
        BOOST, MAIN_QUERY;

        public static EmbeddingQueryMode fromString(final String str) {
            for (final EmbeddingQueryMode mode : values()) {
                if (mode.name().equalsIgnoreCase(str)) {
                    return mode;
                }
            }
            throw new IllegalArgumentException("No such EmbeddingQueryMode " + str);
        }
    }

    private final List<Term> terms = new ArrayList<>();

    private final EmbeddingQueryMode queryMode;

    private final EmbeddingModel embeddingModel;

    private final int topK;

    private final String vectorField;

    private final float boost;

    public EmbeddingsRewriter(final EmbeddingModel embeddingModel, final EmbeddingQueryMode queryMode, final int topK,
                              final String vectorField) {
        // FIXME: it would probably be better to have 2 different rewriters, one for boosting and one for selection
        this(embeddingModel, queryMode, topK, vectorField, 1f);
    }

    public EmbeddingsRewriter(final EmbeddingModel embeddingModel, final EmbeddingQueryMode queryMode, final int topK,
                              final String vectorField, final float boost) {
        this.queryMode = queryMode;
        this.embeddingModel = embeddingModel;
        this.topK = topK;
        this.vectorField = vectorField;
        this.boost = boost;
    }


    @Override
    public RewriterOutput rewrite(final ExpandedQuery query,
                                  final SearchEngineRequestAdapter searchEngineRequestAdapter) {
        return RewriterOutput.builder().expandedQuery(
            collectQueryString(query)
                    .map(embeddingModel::getEmbedding)
                    .map(embedding -> applyEmbedding(embedding, query))
                //.map(this::makeEmbeddingQueryString)
                //.map(embeddingQueryString -> applyVectorQuery(embeddingQueryString, query))
                .orElse(query))
                .build();

    }

    protected ExpandedQuery applyEmbedding(final Embedding embedding, final ExpandedQuery inputQuery) {


        switch (queryMode) {
            case BOOST:
                inputQuery.addBoostUpQuery(new BoostQuery(new StringRawQuery(null, makeEmbeddingQueryString(embedding),
                        Clause.Occur.SHOULD, true), boost));
                break;
            case MAIN_QUERY:
                // this is a workaround to avoid changing Querqy's query object model for now:
                // as we cant set a StringRawQuery as the userQuery, we use a match all for that, add a vector query
                // as a filter query (retrieve only knn) and a boost query (rank by distance)
                //inputQuery.setUserQuery(new MatchAllQuery());
                inputQuery.setUserQuery(new ParsedRawQuery<org.apache.lucene.search.Query>(null, Clause.Occur.MUST,
                        true, new Add1KnnVectorQuery(vectorField, embedding.asVector(), topK)));
                //new StringRawQuery(null, embeddingQueryString, Clause.Occur.MUST, true));
                // inputQuery.addBoostUpQuery(new BoostQuery(embeddingsQuery, boost));
                break;
            default:
                throw new IllegalStateException("Unknown query mode: " + queryMode);

        }

        return inputQuery;
    }

    protected String makeEmbeddingQueryString(final Embedding embedding) {
        return "{!func}sum(100,query({!knn f=" + vectorField + " topK=" + topK + " v='[" + embedding.asCommaSeparatedString() + "]'}))";
    }

    protected String embeddingToString(final float[] embedding) {
        final StringBuilder sb = new StringBuilder(embedding.length * 16);
        for (int i = 0; i < embedding.length; i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(embedding[i]);
        }
        return sb.toString();
    }

    protected ExpandedQuery applyVectorQuery(final String embeddingQueryString, final ExpandedQuery inputQuery) {


        final StringRawQuery embeddingsQuery = new StringRawQuery(null, embeddingQueryString, Clause.Occur.SHOULD, true);
        switch (queryMode) {
            case BOOST:
                inputQuery.addBoostUpQuery(new BoostQuery(embeddingsQuery, boost));
                break;
            case MAIN_QUERY:
                // this is a workaround to avoid changing Querqy's query object model for now:
                // as we cant set a StringRawQuery as the userQuery, we use a match all for that, add a vector query
                // as a filter query (retrieve only knn) and a boost query (rank by distance)
                //inputQuery.setUserQuery(new MatchAllQuery());
                inputQuery.setUserQuery(new StringRawQuery(null, embeddingQueryString, Clause.Occur.MUST, true));
                // inputQuery.addBoostUpQuery(new BoostQuery(embeddingsQuery, boost));
                break;
            default:
                throw new IllegalStateException("Unknown query mode: " + queryMode);

        }

        return inputQuery;
    }
    /**
     * Traverse the query graph, collect all the terms and join them into a string
     */
    protected Optional<String> collectQueryString(final ExpandedQuery query) {
        final QuerqyQuery<?> userQuery = query.getUserQuery();
        // make sure we get a query type that we can handle and not a match all query etc.
        if (userQuery instanceof Query) {
            terms.clear();
            this.visit((Query) userQuery);
            if (!terms.isEmpty()) {
                return Optional.of(new CompoundCharSequence(" ", terms).toString());
            }
        }
        return Optional.empty();
    }

    @Override
    public Node visit(final Term term) {
        terms.add(term);
        return null;
    }

}
