curl -XPOST 'http://localhost:9200/media_catalogs_public/asset/_search' -d '{
   "query" : {
     "bool" : {
       "must" : [ {
         "term" : {
           "category" : "index"
         }
       }, {
         "bool" : {
           "should" : [ {
             "term" : {
               "viewasset" : "true"
             }
           }, {
             "term" : {
               "viewasset" : "sgroupadministrator"
             }
           }, {
             "term" : {
               "viewasset" : "profileassetadmin"
             }
           }, {
             "term" : {
               "viewasset" : "photoeditors"
             }
           }, {
             "term" : {
               "viewasset" : "gs"
             }
           }, {
             "term" : {
               "viewasset" : "video"
             }
           }, {
             "term" : {
               "viewasset" : "administrators"
             }
           }, {
             "term" : {
               "viewasset" : "visualdata"
             }
           }, {
             "term" : {
               "viewasset" : "developers"
             }
           }, {
             "term" : {
               "viewasset" : "admin"
             }
           } ]
         }
       } ]
     }
   },
   "sort" : [ {
     "assetaddeddate" : {
       "order" : "desc"
     }
   } ]
 }' | python -mjson.tool

