package org.entermedia.elasticsearch;

import org.elasticsearch.client.Client;
import org.elasticsearch.node.NodeBuilder;
import org.openedit.entermedia.cluster.NodeManager;

import com.openedit.OpenEditException;
import com.openedit.page.Page;
import com.openedit.util.PathUtilities;

public class ClientPool
{
	protected Client fieldClient;
	protected NodeManager fieldNodeManager;
	
	public NodeManager getNodeManager()
	{
		return fieldNodeManager;
	}

	public void setNodeManager(NodeManager inNodeManager)
	{
		fieldNodeManager = inNodeManager;
	}

	public Client getClient()
	{
		if( fieldClient == null)
		{
			//Get this info from the NodeManager
			
			//Node node = NodeBuilder.nodeBuilder().local(true).node();
			NodeBuilder nb = NodeBuilder.nodeBuilder().local(true);
			
			//Todo Change the port number and work path for this node
			
			//required
			String[] keys = new String[] {
					"cluster.name",
					"gateway.type",
					"gateway.fs.location",
					"path.data",
					"path.logs",
					"path.work",
			};
			
			Page config = getNodeManager().getPageManager().getPage("/WEB-INF/node.xml");
			String abs = config.getContentItem().getAbsolutePath();
			for (int i = 0; i < keys.length; i++)
			{
				String val = getNodeManager().getLocalNode().get(keys[i]);
				if( val == null)
				{
					throw new OpenEditException(keys[i] + " is not set in node.xml");
				}
				if( val.startsWith("."))
				{
					val = PathUtilities.resolveRelativePath(val, abs );
				}
	            nb.settings().put(keys[i], val);
			}
			//extras
            //nb.settings().put("index.store.type", "mmapfs");
            //nb.settings().put("index.store.fs.mmapfs.enabled", "true");
            //nb.settings().put("index.merge.policy.merge_factor", "20");
           // nb.settings().put("discovery.zen.ping.unicast.hosts", "localhost:9300");
           // nb.settings().put("discovery.zen.ping.unicast.hosts", elasticSearchHostsList);

            fieldClient = nb.node().client();   //when this line executes, I get the error in the other node 
			
			//fieldClient = node.client();
		}
		return fieldClient;
	}

}
