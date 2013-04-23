##curl -XPOST 'http://localhost:9200/media_catalogs_public/lock/_search' -d '

curl -XPOST 'http://localhost:9200/entermedia_catalogs_testcatalog/lock/_search' -d '
{"query" :
	{ 
		"match_all" : {}
	}
}' | python -mjson.tool
