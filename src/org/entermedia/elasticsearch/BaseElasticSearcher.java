package org.entermedia.elasticsearch;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.elasticsearch.action.ActionFuture;
import org.elasticsearch.action.admin.indices.status.IndicesStatusResponse;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.client.AdminClient;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.Requests;
import org.elasticsearch.client.action.index.IndexRequestBuilder;
import org.elasticsearch.client.action.search.SearchRequestBuilder;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.index.query.xcontent.BoolQueryBuilder;
import org.elasticsearch.index.query.xcontent.QueryBuilders;
import org.elasticsearch.index.query.xcontent.XContentQueryBuilder;
import org.elasticsearch.search.sort.FieldSortBuilder;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.openedit.Data;
import org.openedit.data.BaseSearcher;
import org.openedit.data.PropertyDetail;
import org.openedit.data.PropertyDetails;
import org.openedit.data.lucene.LuceneSearchQuery;

import com.openedit.OpenEditException;
import com.openedit.hittracker.HitTracker;
import com.openedit.hittracker.SearchQuery;
import com.openedit.hittracker.Term;
import com.openedit.users.User;

public abstract class BaseElasticSearcher extends BaseSearcher
{
	private static final Log log = LogFactory.getLog(BaseElasticSearcher.class);
	protected ClientPool fieldClientPool;
	protected boolean fieldConnected;
	
	public boolean isConnected()
	{
		return fieldConnected;
	}


	public void setConnected(boolean inConnected)
	{
		fieldConnected = inConnected;
	}


	public ClientPool getClientPool()
	{
		return fieldClientPool;
	}


	public void setClientPool(ClientPool inClientPool)
	{
		fieldClientPool = inClientPool;
	}


	public SearchQuery createSearchQuery()
	{
		LuceneSearchQuery query = new LuceneSearchQuery();
		query.setPropertyDetails(getPropertyDetails());
		query.setCatalogId(getCatalogId());
		query.setResultType(getSearchType()); //a default
		query.setSearcherManager(getSearcherManager());
		return query;
	}
	protected Client getClient()
	{
		return getClientPool().getClient();
	}
	protected String toId(String inId)
	{
		String id = inId.replace('/', '_');
		return id;
	}
	public HitTracker search(SearchQuery inQuery)
	{
		try
		{
			long start = System.currentTimeMillis();
			connect();
			SearchRequestBuilder search = getClient().prepareSearch(toId(getCatalogId()));
	        search.setSearchType(SearchType.DFS_QUERY_THEN_FETCH);
	        addTerms(inQuery,search);
	        addSorts(inQuery,search);
	        search.setFrom(0).setSize(60).setExplain(true);

			SearchResponse results = search.execute().actionGet();
			ElasticHitTracker hits = new ElasticHitTracker(results);
			hits.setIndexId(getIndexId());
			hits.setSearchQuery(inQuery);

			long end = System.currentTimeMillis() - start;

			log.info(hits.size() + " hits query: " + inQuery + " sort by " + inQuery.getSorts() + " in " + (double) end / 1000D + " seconds] on " + getCatalogId() + "/" + getSearchType() );

			return hits;
		}
		catch (Exception ex)
		{
			log.error(ex);
			if (ex instanceof OpenEditException)
			{
				throw (OpenEditException) ex;
			}
			throw new OpenEditException(ex);
		}

//		AdminClient admin = client.admin();
//		ActionFuture<CreateIndexResponse> indexresponse = admin.indices().create(new CreateIndexRequest("test"));
//		log(indexresponse.isDone() + " done ");
		
		//node.close();
	}

	protected void connect()
	{
		if( !isConnected())
		{
			AdminClient admin = getClient().admin();
			boolean failed = false;
			try
			{
				ActionFuture<IndicesStatusResponse> future = admin.indices().status(Requests.indicesStatusRequest(toId(getCatalogId())));
				if( future.actionGet().getTotalShards() == 0 )
				{
					failed = true;
				}				
			}
			catch( Exception ex)
			{
				log.error("index not ready " + ex);
				failed = true;
			}
			if( failed)
			{
				try
				{
					admin.indices().create(Requests.createIndexRequest(toId(getCatalogId()))).actionGet();
					reIndexAll();
				}
				catch( Exception ex)
				{
					log.error(ex);
				}
				
			}
			fieldConnected =  true;
		}
	}


	protected void addTerms(SearchQuery inQuery, SearchRequestBuilder inSearch)
	{
		BoolQueryBuilder bool = QueryBuilders.boolQuery();
		
		for (Iterator iterator = inQuery.getTerms().iterator(); iterator.hasNext();)
		{
			Term term = (Term) iterator.next();
			if( term.getOperation() == "exact")
			{
				XContentQueryBuilder find = QueryBuilders.termQuery(term.getDetail().getId(), term.getValue());
				bool.must(find);
			}
			else
			{
				XContentQueryBuilder find = QueryBuilders.wildcardQuery(term.getDetail().getId(), term.getValue());				
				bool.must(find);
			}
			//TODO: Deal with subqueries, or, and, not 
		}
		inSearch.setQuery(bool);
	}


	protected void addSorts(SearchQuery inQuery,SearchRequestBuilder search)
	{
		if( inQuery.getSorts() == null)
		{
			return;
		}
		for (Iterator iterator = inQuery.getSorts().iterator(); iterator.hasNext();)
		{
			String field = (String) iterator.next();
			boolean direction = false;
			if (field.endsWith("Down"))
			{
				direction = true;
				field = field.substring(0, field.length() - 4);
			}
			else if (field.endsWith("Up"))
			{
				direction = false;
				field = field.substring(0, field.length() - 2);
			}
	        FieldSortBuilder sort = SortBuilders.fieldSort(field);
	        if( direction)
	        {
	        	sort.order(SortOrder.DESC);
	        }
	        else
	        {
	        	sort.order(SortOrder.ASC);
	        }
	        search.addSort(sort);
		}
	}
	public String getIndexId()
	{
		
		return "singleton";
	}

	
	public void clearIndex()
	{

	}

	public void saveData(Object inData, User inUser)
	{
		//update the index
		List<Data> list = new ArrayList(1);
		list.add((Data)inData);
		updateIndex(list,inUser);
	}
	protected void updateIndex(List<Data> inBuffer, User inUser)
	{
		String catid = toId(getCatalogId());
		
//		BulkRequestBuilder brb = getClient().prepareBulk();
//		brb.add(Requests.indexRequest(indexName).type(getIndexType()).id(id).source(source));
//    }
//    if (brb.numberOfActions() > 0) brb.execute().actionGet(); 
		PropertyDetails details = getPropertyDetailsArchive().getPropertyDetailsCached(getSearchType());

		for(Data data : inBuffer)
		{
			try
			{
				IndexRequestBuilder builder = getClient().prepareIndex(catid, getSearchType(), data.getId());
				XContentBuilder content = XContentFactory.jsonBuilder().startObject();
				updateIndex(content,data,details);
				content.endObject();
				IndexResponse response = builder.setSource(content)
			        .setRefresh(true)
			        .execute()
			        .actionGet();
			}
			catch( Exception ex)
			{
				throw new OpenEditException(ex);
			}
		}
		
		inBuffer.clear();
	}

	protected void updateIndex(XContentBuilder inContent, Data inData, PropertyDetails inDetails)
	{
		for (Iterator iterator = inDetails.findIndexProperties().iterator(); iterator.hasNext();)
		{
			PropertyDetail detail = (PropertyDetail)iterator.next();
			String value = inData.get(detail.getId());
			if( value != null)
			{
				try
				{
					
					//TODO: Deal with data types and move to indexer object
					inContent.field(detail.getId(),value);
				}
				catch( Exception ex)
				{
					throw new OpenEditException(ex);
				}
			}
		}
//		.field("user", "kimchy")
//        .field("postDate", new Date())
//        .field("message", "trying out Elastic Search");
		
	}


	public void deleteAll(User inUser)
	{
//		for (Iterator iterator = collection.iterator(); iterator.hasNext();)
//		{
//			type type = (type) iterator.next();
//			
//		}
		throw new OpenEditException("Not implemented");	
	}

	
	public void delete(Data inData, User inUser)
	{
		

	}

	
	public void saveAllData(List<Data> inAll, User inUser)
	{
		

	}


}
