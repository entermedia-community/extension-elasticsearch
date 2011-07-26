curl -XDELETE http://localhost:9200/entermedia_catalogs_testcatalog

curl -XPUT http://localhost:9200/entermedia_catalogs_testcatalog -d '{"index":{"number_of_shards":1,"number_of_replicas":0}}'
