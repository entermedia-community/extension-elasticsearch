package org.entermedia.elasticsearch.searchers;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dom4j.tree.BaseElement;
import org.elasticsearch.action.deletebyquery.DeleteByQueryRequestBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.TermQueryBuilder;

import com.openedit.Shutdownable;

public class LockSearcher extends BaseElasticSearcher implements Shutdownable
{
	private static final Log log = LogFactory.getLog(LockSearcher.class);

	protected boolean fieldClearIndexOnStart;

	public boolean isClearIndexOnStart()
	{
		return fieldClearIndexOnStart;
	}

	public void setClearIndexOnStart(boolean inClearIndexOnStart)
	{
		fieldClearIndexOnStart = inClearIndexOnStart;
	}
	
	@Override
	protected void connect()
	{
		if( !isConnected() )
		{
			super.connect();

			clearStaleLocks();

		}
		else
		{
			super.connect();
		}
		
	}

	//TODO: move this to the ClientPool shutdown ruitine
	public void clearStaleLocks()
	{
		String id = getElasticNodeManager().getLocalNodeId();
		log.info("Deleted nodeid=" + id + " records database " + getSearchType() );
		DeleteByQueryRequestBuilder delete = getClient().prepareDeleteByQuery(toId(getCatalogId()));
		delete.setTypes(getSearchType());
		TermQueryBuilder builder = QueryBuilders.termQuery("nodeid", id);
		delete.setQuery(builder).execute().actionGet();
		
	}
	
	public void shutdown()
	{
		if( isConnected() )
		{
			clearStaleLocks();
		}
		if (fieldElasticNodeManager != null)
		{
			fieldElasticNodeManager.shutdown();
			fieldConnected = false;
			fieldElasticNodeManager = null;
		}
	}
}
