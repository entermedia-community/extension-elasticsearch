set -o verbose  #echo on
curl -XPOST 'http://localhost:9200/system/group/_search' -d '{
   "query" : {
     "bool" : {
       "must" : [ {
         "bool" : {
           "should" : [ {
             "term" : {
               "_id" : "GS"
             }
           }, {
             "term" : {
               "_id" : "video"
             }
           }, {
             "term" : {
               "_id" : "photoeditors"
             }
           }, {
             "term" : {
               "_id" : "administrators"
             }
           }, {
             "term" : {
               "_id" : "developers"
             }
           }, {
             "term" : {
               "_id" : "visualdata"
             }
           } ]
         }
       }, {
         "wildcard" : {
           "description" : {
             "wildcard" : "a*"
           }
         }
       } ]
     }
   }
}' | python -mjson.tool



curl -XPOST 'http://localhost:9200/system/group/_search' -d '{
"query":
	{
		"wildcard" : 
		{
   			"description" : "a*"
		}
	}
}' | python -mjson.tool


curl -XPOST 'http://localhost:9200/system/group/_search' -d '{
"query":
	{
		"match_all" : {}
	}
}' | python -mjson.tool


