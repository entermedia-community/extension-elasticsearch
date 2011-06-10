package org.entermedia.elasticsearch;

import org.elasticsearch.client.Client;
import org.elasticsearch.node.NodeBuilder;

public class ClientPool
{
	protected Client fieldClient;
	
	public Client getClient()
	{
		if( fieldClient == null)
		{
			//Node node = NodeBuilder.nodeBuilder().local(true).node();
			NodeBuilder nb = NodeBuilder.nodeBuilder().local(true);
			
			//Todo Change the port number and work path for this node
            nb.settings().put("cluster.name", "entermedia");
            nb.settings().put("gateway.type", "fs");
            nb.settings().put("gateway.fs.location", "/tmp/elasticsearch/gateway"); //This is a shared location
            //nb.settings().put("index.store.type", "mmapfs");
            //nb.settings().put("index.store.fs.mmapfs.enabled", "true");
            //nb.settings().put("index.merge.policy.merge_factor", "20");
            nb.settings().put("path.data", "/tmp/elasticsearch2/data");
            nb.settings().put("path.logs", "/tmp/elasticsearch2/logs");
            nb.settings().put("path.work", "/tmp/elasticsearch2/work");
           // nb.settings().put("discovery.zen.ping.unicast.hosts", "localhost:9300");
           // nb.settings().put("discovery.zen.ping.unicast.hosts", elasticSearchHostsList);

            fieldClient = nb.node().client();   //when this line executes, I get the error in the other node 
			
			//fieldClient = node.client();
		}
		return fieldClient;
	}

}
