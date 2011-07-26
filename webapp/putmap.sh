curl -XPUT 'http://localhost:9200/entermedia_catalogs_testcatalog/lock/_mapping' -d '
{
    "lock" : {
        "properties" : {
            "id" : {"type" : "string", "store" : "yes"},
	    "path" : {"type" : "string", "store" : "yes","index":"not_analyzed" }
        }
    }
}
'

#curl -XGET 'http://localhost:9200/entermedia_catalogs_testcatalog/lock/_mapping'
#curl -XGET 'http://localhost:9200/entermedia_catalogs_testcatalog/_search -d '{query": { "query_string": {
