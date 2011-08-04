curl -XPOST 'http://localhost:9200/media_catalogs_public/asset/_search' -d '
{"query":
	{
	    "bool": {
		"must": [
		    {
		        "term": {
		            "category": "index"
		        }
		    }, 
            {
                "bool": {
 "should": [
                        {
                            "term": {
                                "viewasset": "true"
                            }
                        }, 
                        {
                            "term": {
                                "viewasset": ""
                            }
                        }, 
                        {
                            "term": {
                                "viewasset": "sgroupadministrator"
                            }
                        }, 
                        {
                            "term": {
                                "viewasset": "profileassetadmin"
                            }
                        }, 
                        {
                            "term": {
                                "viewasset": "visualdata"
                            }
                        }, 
                        {
                            "term": {
                                "viewasset": "video"
                            }
                        }, 
                        {
                            "term": {
                                "viewasset": "administrators"
                            }
                        }, 
                        {
                            "term": {
                                "viewasset": "developers"
                            }
                        }, 
                        {
                            "term": {
                                "viewasset": "GS"
                            }
                        }, 
                        {
                            "term": {
                                "viewasset": "photoeditors"
                            }
                        }, 
                        {
                            "term": {
                                "viewasset": "admin"
                            }
                        }



                    ]
}}
		]
	    }
	}
}' | python -mjson.tool
