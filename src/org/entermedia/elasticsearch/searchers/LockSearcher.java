package org.entermedia.elasticsearch.searchers;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.elasticsearch.action.deletebyquery.DeleteByQueryRequestBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.TermQueryBuilder;

import com.openedit.Shutdownable;

public class LockSearcher extends ElasticTransientSearcher implements Shutdownable
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

	public void clearStaleLocks()
	{
		String id = getClientPool().getNodeManager().getLocalNodeId();
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
		if (fieldClientPool != null)
		{
			fieldClientPool.shutdown();
			fieldConnected = false;
		}
	}
}
