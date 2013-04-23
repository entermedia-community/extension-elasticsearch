curl -XPOST 'http://localhost:9200/entermedia_catalogs_testcatalog/conversiontask/_search' -d '{
   "query" : {
     "term" : {
       "_id" : "wcHedu5PSciNCaHRvlVp8A"
     }
   }
}'| python -mjson.tool

curl -XGET 'http://localhost:9200/entermedia_catalogs_testcatalog/lock/_mapping' | python -mjson.tool


