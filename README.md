# Querqy Vector Embeddings Rewriter

## About
A [Querqy](https://github.com/querqy/querqy) search query rewriter that adds embeddings to the query. So far, it is 
only available for the Solr plugin version of Querqy, but it should shortly become available for the other Querqy
versions (OpenSearch, Elasticsearch, Querqy Unplugged).

## Embedding models
The idea is to map query strings to vector embeddings and add the embeddings to the search query when it is being
processed in the Querqy query rewrite chain and then turned into a Lucene dense vector query. 

Embeddings can be used either to replace the main query string - and thus do a knn retrieval for the vector of the main
query - or to inject the vector as a boost query, which boosts the knn documents by vector similarity (the score is 
additive to the main query).

The mapping from the query string to an embedding is defined in an `EmbeddingModel`. So far, this rewriter package comes
with the following EmbeddingModel implementations:

* ChorusEmbeddingModel: This model integrates with the [Chorus stack](https://github.com/querqy/chorus) (an open source example e-commerce search
  application). It retrieves the embeddings from an HTTP-based embeddings service that is part of Chorus. You might want to
  use this model as a starting point for implementing your own embeddings service.
* OpenAiEmbeddingModel: This model retrieves the embeddings from the OpenAI API. It cannot be used directly, as the
  number of vector dimensions returned by OpenAI would exceed the max. number of dimensions that Lucene can handle.
  Nevertheless, it will give you an idea how to integrate external APIs.
* DummyEmbeddingsModel: This model has a vocabulary of only 4 words w1, w2, w3, w4, and it is only used for development
  and testing.

You can use your own model by implementing the `EmbeddingModel` interface.

## Rewriter configuration


Like any other Querqy rewriter, the embeddings rewriter is configured using [Querqy's rewriter API](https://querqy.org/docs/querqy/rewriters.html). The 
configuration skeleton looks like this:

```
POST /solr/<core>/querqy/rewriter/emb?action=save
Content-Type: application/json
```
```json
{
   "class": "querqy.solr.embeddings.SolrEmbeddingsRewriterFactory",
   "config": {
     "model" : {
       "class": "<my.model.ClassName>",
       "cache": "<optional.cache-name>",
       "<model-param1>": "<value 1>"
     }
   }
}
```
The rewriter becomes available under the ID `emb` (see last part of the path in the URL). The name can be freely chosen.

The `config` part contains the definition of the embedding model, where `class` references the Java class 
that implements the `EmbeddingModel` interface.

The `cache` property references an optional Solr cache for embeddings. We recommend to configure this cache, especially
if getting the embeddings for a query string is an expensive operation in your `EmbeddingModel` implementation.
Usually, a single embedding is needed multiple times per search request and using this cache thus makes sense, even
if the  queries that were entered by the search user will not reoccur. The following example shows how a
cache  named `embeddings` would be configured in `solrconfig.xml`:

```xml

<query>
    <cache name="embeddings" class="solr.CaffeineCache"
           size="1024"
           initialSize="128"
    />
</query>

```

The rewriter configuration also allows to specify further, model-dependant settings.
The full configuration for the rewriter, using the Chorus embeddings model and the above cache, could then look like
this:

```
POST /solr/<core>/querqy/rewriter/emb?action=save
Content-Type: application/json
```
```json
{
  "class": "querqy.solr.embeddings.SolrEmbeddingsRewriterFactory",
  "config": {
    "model" : {
      "class": "querqy.embeddings.ChorusEmbeddingModel",
      "cache": "embeddings",
      "url": "http://embeddings:8000/minilm/text/",
      "normalize": true
    }
  }
}
```

The configuration adds two properties, `url` (the HTTP URL of the Chorus embeddings service) and `normalize`, which 
is passed on to the service to control whether vectors should be normalized to unit length. Whether you want
normalized vectors depends on the choice of the vector similarity that you configured for your dense vector field in
Solr (you will need normalize:true for `dot_product` and normalize:false for `euclidean` and you can use either in case
of `cosine`).

## Making queries

Provided that you have configured the Querqy query parser under the name `querqy` (see [here](https://querqy.org/docs/querqy/index.html#installation) for the setup) and
created the embeddings rewriter with ID `emb` (see above), you can start using the embeddings rewriter when you make
search queries to Solr. 

The rewriter can be used in two modes - one that uses the embedding to define the result set by
retrieving the top k results whose vectors are the nearest to the embedding of the query and scoring them by the vector
similarity to the query. The other mode uses keyword matching to retrieve the results but then boosts the top k nearest
vectors/documents by vector similarity.

Parameters when retrieving the top k results:

* q=\<query text\>
* &qf=\<search fields (ignored)\>
* &defType=querqy
* &querqy.rewriters=emb (use the emb rewriter in the rewrite chain)
* &querqy.emb.topK=100 (the k in knn: k=100 - get back top 100 results)
* &querqy.emb.mode=MAIN_QUERY (retrieve top k)
* &querqy.emb.f=vector (the field to which we indexed the vectors)

(Note that if you use a rewriter ID other than `emb`, you will have to replace the `emb` in the parameter
names, like in `querqy.emb.topK`)

Parameters when boosting the top k results:

* q=\<query text\>
* &qf=f1 f2 f3 (search fields)
* &defType=querqy
* &querqy.rewriters=emb (use the emb rewriter in the rewrite chain)
* &querqy.emb.topK=20 (the k in knn: boost the top 20 by vector similarity)
* &querqy.emb.boost=100 (multiply the vector similarity by 100 and add this product to the score)
* &querqy.emb.mode=BOOST (boost, don't use knn as the main query)
* &querqy.emb.f=vector (the field to which we indexed the vectors)






