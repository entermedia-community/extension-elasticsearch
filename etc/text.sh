curl -XPOST 'http://localhost:9200/entermedia_catalogs_testcatalog/asset/_search' -d '
"query" : 
{
        "text" : 
	{
          "description" : 
	  {
             "test101"
          }
        }
}' | python -mjson.tool

curl -XGET localhost:9200/entermedia_catalogs_testcatalog/asset/_search?q=message:teach  | python -mjson.tool

curl -XGET 'http://localhost:9200/entermedia_catalogs_testcatalog/asset/_mapping' | python -mjson.tool


