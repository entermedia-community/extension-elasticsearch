curl -XDELETE localhost:9200/twitter  | python -mjson.tool

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
}'  | python -mjson.tool

sleep 1

curl -XPUT localhost:9200/twitter/tweet/_mapping -d '{
    "tweet" : {
        "properties" : {
            "message" : {"type" : "string", "analyzer":"a2","include_in_all":"true"},
            "user": {"type":"string"}
        }
    }}'  | python -mjson.tool

sleep 1

curl -XPUT http://localhost:9200/twitter/tweet/1 -d '{ "user": "kimchy", "message": "Trying out searching teaching, so far so good?" }'  | python -mjson.tool

sleep 1

curl -XGET localhost:9200/twitter/tweet/_search?q=message:teach  | python -mjson.tool


sleep 1

curl -XGET localhost:9200/twitter/tweet/_search?q=_all:teach  | python -mjson.tool

echo "Should have a hit"
RN] [Professor Power] [system][0] failed to obtain snapshot lock, ignoring snapshot
org.elasticsearch.ElasticSearchIllegalStateException: failed to obtain snapshot lock [NativeFSLock@/export/home/apps/hbs/local/localhost/WEB-INF/data/elastic/gateway/entermediaprod/indices/system/0/snapshot.lock]        at org.elasticsearch.index.gateway.fs.FsIndexShardGateway.obtainSnapshotLock(FsIndexShardGateway.java:65)
        at org.elasticsearch.index.gateway.IndexShardGatewayService.snapshot(IndexShardGatewayService.java:258)
        at org.elasticsearch.index.gateway.IndexShardGatewayService$SnapshotRunnable.run(IndexShardGatewayService.java:365)
        at java.util.concurrent.ThreadPoolExecutor$Worker.runTask(ThreadPoolExecutor.java:886)
        at java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:908)
        at java.lang.Thread.run(Thread.java:662)
