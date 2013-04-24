curl -XPOST 'http://localhost:9200/elastic_catalog/settingsgroup/_search' -d '
{"query" :
	{ 
		"match_all" : {}
	}
}' | python -mjson.tool
