package org.entermedia.elasticsearch;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
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
import org.elasticsearch.client.action.delete.DeleteRequestBuilder;
import org.elasticsearch.client.action.deletebyquery.DeleteByQueryRequestBuilder;
import org.elasticsearch.client.action.index.IndexRequestBuilder;
import org.elasticsearch.client.action.search.SearchRequestBuilder;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.index.query.xcontent.BoolQueryBuilder;
import org.elasticsearch.index.query.xcontent.MatchAllQueryBuilder;
import org.elasticsearch.index.query.xcontent.QueryBuilders;
import org.elasticsearch.index.query.xcontent.XContentQueryBuilder;
import org.elasticsearch.search.sort.FieldSortBuilder;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.openedit.Data;
import org.openedit.data.BaseSearcher;
import org.openedit.data.PropertyDetail;
import org.openedit.data.PropertyDetails;

import com.openedit.OpenEditException;
import com.openedit.hittracker.HitTracker;
import com.openedit.hittracker.SearchQuery;
import com.openedit.hittracker.Term;
import com.openedit.page.Page;
import com.openedit.page.manage.PageManager;
import com.openedit.users.User;
import com.openedit.util.IntCounter;

public abstract class BaseElasticSearcher extends BaseSearcher
{
	private static final Log log = LogFactory.getLog(BaseElasticSearcher.class);
	protected ClientPool fieldClientPool;
	protected boolean fieldConnected;
	protected IntCounter fieldIntCounter;
	protected PageManager fieldPageManager;
	
	public PageManager getPageManager()
	{
		return fieldPageManager;
	}


	public void setPageManager(PageManager inPageManager)
	{
		fieldPageManager = inPageManager;
	}


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
		ElasticSearchQuery query = new ElasticSearchQuery();
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
	        XContentQueryBuilder terms = buildTerms(inQuery);
	        search.setQuery(terms);
	       // addSorts(inQuery,search);
	        search.setFrom(0).setSize(60).setExplain(true);

			SearchResponse results = search.execute().actionGet();
			ElasticHitTracker hits = new ElasticHitTracker(results);
			hits.setIndexId(getIndexId());
			hits.setSearchQuery(inQuery);

			String json = new String(terms.buildAsBytes() , "UTF-8" );
			long end = System.currentTimeMillis() - start;

			log.info(hits.size() + " hits query: " + json + " sort by " + inQuery.getSorts() + " in " + (double) end / 1000D + " seconds] on " + getCatalogId() + "/" + getSearchType() );

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
					
					  // create properties for the mapped type
//	                XContentBuilder data = jsonBuilder().startObject()
//	                                .startObject("properties")
//	                                        .startObject(MEDIA_TITLE_FIELD)
//	                                                .field("type", "string")
//	                                                .field("index", "not_analyzed")
//	                                                .field("store", "yes")
//	                                        .endObject()
//	                                        .startObject(MEDIA_DESCRIPTION_FIELD)
//	                                                .field("type", "string")
//	                                                .field("index", "standard")
//	                                                .field("store", "yes")
//	                                        .endObject()
//	                                .endObject()
//	                        .endObject();
//
//	                CreateIndexResponse mapping = client
//	                                .admin()
//	                                .indices()
//	                                .create(createIndexRequest(TEST_INDEX).mapping(TEST_TYPE, data))
//	                                .actionGet();
//
//	                assertTrue(mapping.acknowledged()); 
					
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

	protected XContentQueryBuilder buildTerms(SearchQuery inQuery)
	{
		BoolQueryBuilder bool = QueryBuilders.boolQuery();
		for (Iterator iterator = inQuery.getTerms().iterator(); iterator.hasNext();)
		{
			Term term = (Term) iterator.next();
			if( "orsGroup".equals( term.getOperation() ) )
			{
				if( term.getValues() != null)
				{
					BoolQueryBuilder or = QueryBuilders.boolQuery();
					for (int i = 0; i < term.getValues().length; i++)
					{
						Object val = term.getValues()[i];
						or.should(QueryBuilders.termQuery(term.getDetail().getId(), val));					
					}
					bool.must(or);
				}
			}
			else
			{
				String value = term.getValue();
				if( value.contains("*"))
				{
					XContentQueryBuilder find = QueryBuilders.wildcardQuery(term.getDetail().getId(), value);				
					bool.must(find);
				}
				else
				{
					XContentQueryBuilder find = QueryBuilders.termQuery(term.getDetail().getId(), value);
					bool.must(find);
				}
			}
			//TODO: Deal with subqueries, or, and, not 
		}
		return bool;
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
		saveData((Data)inData,inUser);
	}
	public void saveData(Data inData, User inUser)
	{
		//update the index
		List<Data> list = new ArrayList(1);
		list.add((Data)inData);
		saveAllData(list,inUser);
	}
	protected void updateIndex(Collection<Data> inBuffer, User inUser)
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
					//log.info("Saved" + detail.getId() + "=" + value );
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
		log.info("Deleted old database");
		DeleteByQueryRequestBuilder delete = getClient().prepareDeleteByQuery(toId(getCatalogId()));

		delete.setQuery(new MatchAllQueryBuilder() ).execute()
        .actionGet();

	}

	
	public void delete(Data inData, User inUser)
	{
		String id = inData.getId();
		DeleteRequestBuilder delete = getClient().prepareDelete(toId(getCatalogId()), getSearchType(), id);
		delete.setOperationThreaded(false)
        .execute()
        .actionGet();
	}

	//Base class only updated the index in bulk
	public void saveAllData(Collection<Data> inAll, User inUser)
	{
		updateIndex(inAll, inUser);
	}
	public synchronized String nextId()
	{
		return String.valueOf(getIntCounter().incrementCount());
	}

	protected IntCounter getIntCounter()
	{
		if (fieldIntCounter == null)
		{
			fieldIntCounter = new IntCounter();
			fieldIntCounter.setLabelName(getSearchType() + "IdCount");
			Page prop = getPageManager().getPage("/WEB-INF/data/" + getCatalogId() +"/"+ getSearchType() + "s/idcounter.properties");
			File file = new File(prop.getContentItem().getAbsolutePath());
			file.getParentFile().mkdirs();
			fieldIntCounter.setCounterFile(file);
		}
		return fieldIntCounter;
	}


}
