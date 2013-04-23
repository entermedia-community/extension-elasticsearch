

curl -XPOST 'http://localhost:9200/media_catalogs_public/asset/_search' -d '
{
   "query" : {
         "term" : {
           "_id" : "TuVldyWrRtGMAAZYWkZ34w"
         }
   }
 }'  | python -mjson.tool
