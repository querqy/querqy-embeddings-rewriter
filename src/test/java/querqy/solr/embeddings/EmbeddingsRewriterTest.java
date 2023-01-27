package querqy.solr.embeddings;

import junit.framework.TestCase;
import org.apache.solr.SolrTestCaseJ4;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.params.DisMaxParams;
import org.apache.solr.common.util.ContentStreamBase;
import org.apache.solr.core.SolrCore;
import org.apache.solr.request.LocalSolrQueryRequest;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.request.SolrRequestHandler;
import org.apache.solr.request.SolrRequestInfo;
import org.apache.solr.response.SolrQueryResponse;
import org.checkerframework.checker.units.qual.A;
import org.junit.BeforeClass;
import org.junit.Test;
import querqy.embeddings.EmbeddingModel;
import querqy.embeddings.EmbeddingsRewriter;
import querqy.embeddings.EmbeddingsRewriterFactory;
import querqy.solr.rewriter.commonrules.CommonRulesConfigRequestBuilder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static querqy.embeddings.EmbeddingModel.norm;
import static querqy.embeddings.EmbeddingsRewriterFactory.PARAM_BOOST;
import static querqy.embeddings.EmbeddingsRewriterFactory.PARAM_MODE;
import static querqy.embeddings.EmbeddingsRewriterFactory.PARAM_QUERQY_PREFIX;
import static querqy.embeddings.EmbeddingsRewriterFactory.PARAM_TOP_K;
import static querqy.embeddings.EmbeddingsRewriterFactory.PARAM_VECTOR_FIELD;
import static querqy.solr.QuerqyQParserPlugin.PARAM_REWRITERS;
import static querqy.solr.QuerqyRewriterRequestHandler.ActionParam.SAVE;

@SolrTestCaseJ4.SuppressSSL
public class EmbeddingsRewriterTest extends SolrTestCaseJ4 {
    static final String REWRITER_NAME = "emb";


    private static SolrInputDocument doc(final String id, final String f1, final String f2, final float[] vec) {
        final SolrInputDocument doc = new SolrInputDocument();
        doc.addField("id", id);
        doc.addField("f1", f1);
        doc.addField("f2", f2);
        final List<Float> val = new ArrayList<>(vec.length);
        for (final float v : norm(vec)) {
            val.add(v);
        }
        doc.addField("vector", val);
        return doc;
    }
    private static void addDocs() {
        // the first doc has the vector for w1, the second for w2 etc.
        // if we query with boost "w2", all docs will match but the second doc (id=2) should come back at the top
        // -> see testBoost
        assertU(adoc(doc("1", "a b c w1 w2 w3 w4", "d", new float[] {0.1f, -0.006f, -0.9f, 0.25f})));
        assertU(adoc(doc("2", "a b c w1 w2 w3 w4", "e", new float[] {-0.1f, 0.006f, 0.9f, -0.25f})));
        assertU(adoc(doc("3",  "a b c w1 w2 w3 w4", "f", new float[] {0.75f, 0.006f, -0.03f, -0.25f})));
        assertU(adoc(doc("4",  "gh i w1 w2 w3 w4", "j", new float[] {-0.07f, 0.01f, 0.8f, -0.18f})));

        assertU(commit());
    }
    @BeforeClass
    public static void beforeTests() throws Exception {
        initCore("solrconfig.xml", "schema.xml");
        withEmbeddingsRewriter(h.getCore(), REWRITER_NAME);
        addDocs();
    }

    public static void withEmbeddingsRewriter(final SolrCore core, final String rewriterId) {
        SolrRequestHandler handler = core.getRequestHandler("/querqy/rewriter/" + rewriterId);

        final LocalSolrQueryRequest req = new LocalSolrQueryRequest(core, SAVE.params());
        req.setContentStreams(Collections.singletonList(new ContentStreamBase.StringStream(
                new EmbeddingsConfigRequestBuilder().buildJson())));
        req.getContext().put("httpMethod", "POST");

        final SolrQueryResponse rsp = new SolrQueryResponse();
        SolrRequestInfo.setRequestInfo(new SolrRequestInfo(req, rsp));
        try {
            core.execute(handler, req, rsp);
        } finally {
            SolrRequestInfo.clearRequestInfo();
            req.close();
        }
    }

    @Test
    public void testBoost() {

        String q = "w2";

        SolrQueryRequest req = req("q", q,
                DisMaxParams.QF, "f1 f2 f3",
                //DisMaxParams.MM, "100%",
                "defType", "querqy",
                "debugQuery", "on",
                PARAM_REWRITERS, REWRITER_NAME,
                PARAM_QUERQY_PREFIX + REWRITER_NAME + PARAM_TOP_K,  "1",
                PARAM_QUERQY_PREFIX + REWRITER_NAME + PARAM_BOOST,  "100",
                PARAM_QUERQY_PREFIX + REWRITER_NAME + PARAM_MODE , EmbeddingsRewriter.EmbeddingQueryMode.BOOST.name(),
                PARAM_QUERQY_PREFIX + REWRITER_NAME + PARAM_VECTOR_FIELD , "vector"

        );

        assertQ("Boosting not working",
                req,
                "//result[@name='response' and @numFound='4']",
                "//doc[1]/str[@name='id' and text()='2']"
        );


        req.close();
    }

    @Test
    public void testMainQuery() {

        String q = "w4";

        SolrQueryRequest req = req("q", q,
                DisMaxParams.QF, "f1 f2 f3",
                "defType", "querqy",
                "debugQuery", "on",
                PARAM_REWRITERS, REWRITER_NAME,
                PARAM_QUERQY_PREFIX + REWRITER_NAME + PARAM_TOP_K,  "2",
                PARAM_QUERQY_PREFIX + REWRITER_NAME + PARAM_BOOST,  "100",
                PARAM_QUERQY_PREFIX + REWRITER_NAME + PARAM_MODE , EmbeddingsRewriter.EmbeddingQueryMode.MAIN_QUERY.name(),
                PARAM_QUERQY_PREFIX + REWRITER_NAME + PARAM_VECTOR_FIELD , "vector"

        );

        assertQ("Boosting not working",
                req,
                "//result[@name='response' and @numFound='2']",
                "//doc[1]/str[@name='id' and text()='4']",
                "//doc[2]/str[@name='id' and text()='2']"
        );


        req.close();
    }

}