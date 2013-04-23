set -o verbose  #echo on
#set +o verbose #echo off

SERVER="http://localhost:9200/twitter"

curl -XDELETE  $SERVER | python -mjson.tool

curl -XPOST $SERVER -d '
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
}'  | python -mjson.tool

sleep 1

curl -XPUT $SERVER/tweet/_mapping -d '{
    "tweet" : {
        "properties" : {
            "message" : {"type" : "string", "analyzer":"a2","include_in_all":"true"},
            "user": {"type":"string"}
        }
    }}'  | python -mjson.tool

sleep 1

curl -XPUT $SERVER/tweet/1 -d '{ "user": "kimchy", "message": "Trying out searching teaching, so far so good?" }'  | python -mjson.tool

sleep 1

curl -XGET $SERVER/tweet/_search?q=message:teach  | python -mjson.tool

sleep 1


curl -XPOST $SERVER/tweet/_search -d '{
"query":
	{
	"wildcard" : {
   		"message" : "TR*"
		}
     }
}'  | python -mjson.tool




echo "Should have a hit"

