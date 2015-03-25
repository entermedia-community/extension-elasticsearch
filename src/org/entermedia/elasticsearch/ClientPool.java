package org.entermedia.elasticsearch;

import java.io.File;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.dom4j.Element;
import org.elasticsearch.client.Client;
import org.elasticsearch.node.NodeBuilder;
import org.openedit.entermedia.cluster.NodeManager;

import com.openedit.OpenEditException;
import com.openedit.Shutdownable;
import com.openedit.page.Page;
import com.openedit.util.PathUtilities;
import com.openedit.util.Replacer;

public class ClientPool implements Shutdownable
{
	protected Client fieldClient;
	protected NodeManager fieldNodeManager;
	protected boolean fieldShutdown = false;

	
	
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
		if( fieldShutdown == false && fieldClient == null)
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
			File parent = new File(abs);
			Map params = new HashMap();
			params.put("webroot", parent.getParentFile().getParentFile().getAbsolutePath());
			params.put("nodeid", getNodeManager().getLocalNodeId());
			Replacer replace = new Replacer();
			
			for (Iterator iterator = getNodeManager().getLocalNode().getElement().elementIterator("property"); iterator.hasNext();)
			{
				Element	prop = (Element) iterator.next();
				String key = prop.attributeValue("id");
				String val = prop.getTextTrim();
				
				if( val.startsWith("."))
				{
					val = PathUtilities.resolveRelativePath(val, abs );
				}
				val = replace.replace(val, params);
				
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
	
	public void shutdown()
	{
		if(!fieldShutdown)
		{
			getClient().close();
		}
		fieldShutdown = true;
		
	}


}
