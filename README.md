# Querqy Vector Embeddings Rewriter
A Querqy rewriter that adds embeddings to the query.

**This is just a stub implementation so far.**

The idea is to map query strings to vector embeddings and add the embeddings to the query as part of the Querqy rewrite chain.

Embeddings can be used either to replace the main query string - and thus do a knn retrieval for the vector of the main query - or to inject the vector as a boost query,
which boosts the knn documents by vector similarity (the score is additive to the main query).

So far, this is a stub implementation uses a dummy `text -> embedding` mapping (`querqy.solr.embeddings.DummyEmbeddingModel`), which only knows the 4 words w1, 
w2, w3, w4.

The implementation only works with Solr so far. An example can be seen in `querqy.solr.embeddings.EmbeddingsRewriterTest`. The test uses Solr's testing
framework. As this might be a bit tricky to translate into API calls, this is how it translates into HTTP when
you work directly with Solr:

Method `beforeTests()` initialises a Solr core using *.xml configs stored under src/test/resources.

`withEmbeddingsRewriter` calls Querqy's rewriter API:

```
POST /solr/<core>/querqy/rewriter/emb?action=save
Content-Type: application/json
```
```json
{
   "class": "querqy.solr.embeddings.SolrEmbeddingsRewriterFactory",
   "config": {}
}
```
The rewriter becomes available under the ID `emb` (see URL). The name can be freely chosen. The test keeps it in the constant `REWRITER_NAME`.

`testBoost()` runs a boosting test. Query "w2" matches all docs (see method `addDocs()`) but the second doc (id=2) gets boosted to the top
as the `w2` query string is mapped to the same vector that we added to doc 2 and as the vector embedding is added to the query as a boost
query.

The code actually produces the following URL params: 
* q=w2
* &qf=f1 f2 f3
* &defType=querqy
* &querqy.rewriters=emb (use the emb rewriter in the rewrite chain)
* &querqy.emb.topK=1 (the k in knn: k=1)
* &querqy.emb.boost=100 (multiply the vector similarity by 100 and add this product to the score)
* &querqy.emb.mode=BOOST (boost, don't use knn as the main query)
* &querqy.emb.f=vector (the field to which we indexed the vectors)

`testMainQuery()` uses the knn query as the main query and ranks by similarity. We use query "w4" (doc 4 to end up at the top), the doc
with the nearest vector (doc 2) comes in second. As we set k=2, we only get back 2 results.

The code produces the following URL params: 
* q=w4
* &qf=f1 f2 f3
* &defType=querqy
* &querqy.rewriters=emb (use the emb rewriter in the rewrite chain)
* &querqy.emb.topK=2 (the k in knn: k=2 - get back 2 results)
* &querqy.emb.mode=MAIN_QUERY (retrieve top k, don't just add a boost)
* &querqy.emb.f=vector (the field to which we indexed the vectors)

If you want to load a different embeddings mappings model (=not just a dummy model), you will have to go to 
`querqy.solr.embeddings.SolrEmbeddingsRewriterFactory#configure` and load the model there, replacing the dummy `EmbeddingModel` class
(or add an abstraction). Note that loading the model is executed only once, i.e. when the rewriter is created (the POST above).
For the EmbeddingsRewriter, an instance is created per search request and the already loaded model is just passed to it in the constructor.

You will probably need to pass parameters to the rewriter factory that help configure the model. You can extend the configuration like this:

```POST `/solr/<core>/querqy/rewriter/emb?action=save
Content-Type: application/json```
```json
{
   "class": "querqy.solr.embeddings.SolrEmbeddingsRewriterFactory",
   "config": {
    "my_param1" : "my_value1",
    "my_param2" : 22
   }
}
```

and then pick up the params in `querqy.solr.embeddings.SolrEmbeddingsRewriterFactory#configure(Map<String, Object> config)`:
```java
  String paramValue1 = config.get("my_param1");
```








