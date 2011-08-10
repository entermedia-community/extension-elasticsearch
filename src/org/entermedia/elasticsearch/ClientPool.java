package org.entermedia.elasticsearch;

import java.util.Iterator;

import org.dom4j.Element;
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
			NodeBuilder nb = NodeBuilder.nodeBuilder();//.client(client)local(true);
			
			//Todo Change the port number and work path for this node
			
			//required
			/*
			String[] keys = new String[] {
					"cluster.name",
					"gateway.type",
					"gateway.fs.location",
					"path.data",
					"path.logs",
					"path.work",
					"network.host"
			};
			*/
			Page config = getNodeManager().getPageManager().getPage("/WEB-INF/node.xml");
			if( !config.exists() )
			{
				throw new OpenEditException("Missing " + config.getPath());
			}
			String abs = config.getContentItem().getAbsolutePath();
			for (Iterator iterator = getNodeManager().getLocalNode().getElement().elementIterator("property"); iterator.hasNext();)
			{
				Element	prop = (Element) iterator.next();
				String key = prop.attributeValue("id");
				String val = prop.getTextTrim();
				if( val.startsWith("."))
				{
					val = PathUtilities.resolveRelativePath(val, abs );
				}
				nb.settings().put(key, val);
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
