curl -XPOST localhost:9200/twitter -d '
{"index":
  { "number_of_shards": 1,
    "analysis": {
       "filter": {
                "snowball": {
                    "type" : "snowball",
                    "language" : "English"
                }
                 },
       "analyzer": { "a2" : {
                    "type":"custom",
                    "tokenizer": "standard",
                    "filter": ["lowercase", "snowball"]
                    }
                  }
     }
  }
}
}'

curl -XPUT localhost:9200/twitter/tweet/_mapping -d '{
    "tweet" : {
        "properties" : {
            "user": {"type":"string"},
            "message" : {"type" : "string", "analyzer":"a2"}
        }
    }}'

curl -XPUT http://localhost:9200/twitter/tweet/1 -d '{ "user": "kimchy", "message": "Trying out searching teaching, so far so good?" }'

curl -XGET localhost:9200/twitter/tweet/_search?q=message:search

