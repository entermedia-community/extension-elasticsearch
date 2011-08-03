package org.entermedia.elasticsearch.searchers;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequest;
import org.elasticsearch.action.admin.indices.create.CreateIndexResponse;
import org.elasticsearch.action.admin.indices.mapping.put.PutMappingRequest;
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
import org.elasticsearch.indices.IndexAlreadyExistsException;
import org.elasticsearch.search.sort.FieldSortBuilder;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.entermedia.elasticsearch.ClientPool;
import org.entermedia.elasticsearch.ElasticHitTracker;
import org.entermedia.elasticsearch.ElasticSearchQuery;
import org.entermedia.locks.LockManager;
import org.openedit.Data;
import org.openedit.data.BaseSearcher;
import org.openedit.data.PropertyDetail;
import org.openedit.data.PropertyDetails;
import org.openedit.util.DateStorageUtil;

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
	protected LockManager fieldLockManager;

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
		connect();

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
			SearchRequestBuilder search = getClient().prepareSearch(toId(getCatalogId()));
			search.setSearchType(SearchType.DFS_QUERY_THEN_FETCH);
			search.setTypes(getSearchType());
			XContentQueryBuilder terms = buildTerms(inQuery);
			search.setQuery(terms);
			addSorts(inQuery, search);
			search.setFrom(0).setSize(60).setExplain(true);

			SearchResponse results = search.execute().actionGet();
			ElasticHitTracker hits = new ElasticHitTracker(results);
			hits.setIndexId(getIndexId());
			hits.setSearchQuery(inQuery);

			String json = new String(terms.buildAsBytes(), "UTF-8");
			long end = System.currentTimeMillis() - start;

			log.info(hits.size() + " hits query: " + json + " sort by " + inQuery.getSorts() + " in " + (double) end / 1000D + " seconds] on " + getCatalogId() + "/" + getSearchType());

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
	}

	@SuppressWarnings("rawtypes")
	protected void connect() 
	{
		if( !isConnected())
		{
			AdminClient admin = getClientPool().getClient().admin();
			boolean reindex = false;
			try
			{
				//ActionFuture<IndicesStatusResponse> future = admin.indices().status(Requests.indicesStatusRequest(toId(getCatalogId())));
				
				CreateIndexRequest newindex = new CreateIndexRequest(toId(getCatalogId()));
				XContentBuilder source = buildMapping();
				newindex.mapping(getSearchType(), source);
				
				CreateIndexResponse res = admin.indices().create(newindex).actionGet();
				if( res.acknowledged() )
				{
					reindex  = true;
				}
			}
			catch( IndexAlreadyExistsException exists)
			{
				//silent error
			}
			catch( Exception ex)
			{
				log.error("index could not be created ", ex);
			}

//			PutMappingRequest  req = Requests.putMappingRequest( toId( getCatalogId() ) );
//			req.source(jsonproperties);
//			getClient().admin().indices().putMapping(req); //does this only apply to one index?

			if(reindex)
			{
				try
				{
					reIndexAll();
				}
				catch( Exception ex)
				{
					log.error("Problem with reindex", ex);
				}
				
			}
			fieldConnected =  true;
		}
	}

	protected XContentBuilder buildMapping() throws Exception
	{
		XContentBuilder jsonBuilder =  XContentFactory.jsonBuilder(); 
		XContentBuilder jsonproperties = jsonBuilder.startObject().startObject(getSearchType());
		jsonproperties = jsonproperties.startObject("properties");
		List props = getPropertyDetails().findIndexProperties();
		if( props.size() == 0)
		{
			throw new OpenEditException("No fields defined for " + getSearchType() );
		}
		for (Iterator i = props.iterator() ; i.hasNext();)
		{
			PropertyDetail detail = (PropertyDetail) i.next();
			jsonproperties  = jsonproperties.startObject(detail.getId());
			
			if( detail.isDate())
			{
				jsonproperties = jsonproperties.field("type", "date");
				jsonproperties = jsonproperties.field("format", "yyyy-MM-dd HH:mm:ss Z");
			}
			else if ( detail.isBoolean())
			{
				jsonproperties = jsonproperties.field("type", "boolean");					
			}
			else if ( detail.isDataType("number"))
			{
				jsonproperties = jsonproperties.field("type", "long");					
			}
			else
			{
				String indextype = detail.get("indextype");
				if( indextype == null)
				{	
					indextype = "not_analyzed";
				}
				jsonproperties = jsonproperties.field("index", indextype);
				jsonproperties = jsonproperties.field("type", "string");					
			}
			if( detail.isStored())
			{
				jsonproperties = jsonproperties.field("store", "yes");
			}
			jsonproperties = jsonproperties.endObject();
		}
		jsonproperties = jsonproperties.endObject();
		jsonBuilder = jsonproperties.endObject();
		log.info("Mapping: " + jsonBuilder.string());
		return jsonproperties;

	}
	
	protected XContentQueryBuilder buildTerms(SearchQuery inQuery)
	{
		if( inQuery.getTerms().size() == 1)
		{
			Term term = (Term)inQuery.getTerms().iterator().next();
			String value = term.getValue();
			XContentQueryBuilder find =buildTerm(term.getDetail(),value);
			return find;
		}
		
		//TODO: Deal with subqueries, or, and, not 
		BoolQueryBuilder bool = QueryBuilders.boolQuery();
		for (Iterator iterator = inQuery.getTerms().iterator(); iterator.hasNext();)
		{
			Term term = (Term) iterator.next();
			if ("orsGroup".equals(term.getOperation()))
			{
				if (term.getValues() != null)
				{
					BoolQueryBuilder or = QueryBuilders.boolQuery();
					for (int i = 0; i < term.getValues().length; i++)
					{
						Object val = term.getValues()[i];
						or.should(buildTerm(term.getDetail(),val));
					}
					bool.must(or);
				}
			}
			else
			{
				String value = term.getValue();
				XContentQueryBuilder find =buildTerm(term.getDetail(),value);
				bool.must(find);
			}
		}
		return bool;
	}

	protected XContentQueryBuilder buildTerm(PropertyDetail inDetail, Object inValue)
	{
		//Check for quick date object
		XContentQueryBuilder find = null;
		if( inValue instanceof Date)
		{
			find = QueryBuilders.termQuery(inDetail.getId(), (Date)inValue);
		}
		else
		{
			String valueof = String.valueOf(inValue);
			
			if( valueof.contains("*"))
			{
				find = QueryBuilders.wildcardQuery(inDetail.getId(), valueof);
			}
			else if( inDetail.isBoolean())
			{
				find = QueryBuilders.termQuery(inDetail.getId(), Boolean.parseBoolean(valueof));
			}
			else if( inDetail.isDate())
			{
				Date date = DateStorageUtil.getStorageUtil().parseFromStorage(valueof);
				find = QueryBuilders.termQuery(inDetail.getId(), date);
			}
			else if( inDetail.isDataType("number") )
			{
				find = QueryBuilders.termQuery(inDetail.getId(), Long.parseLong(valueof));
			}
			else
			{
				find = QueryBuilders.termQuery(inDetail.getId(), valueof);
			}
		}
		return find;
	}

	protected void addSorts(SearchQuery inQuery, SearchRequestBuilder search)
	{
		if (inQuery.getSorts() == null)
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
			if (direction)
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
		saveData((Data) inData, inUser);
	}

	public void saveData(Data inData, User inUser)
	{
		//update the index
		List<Data> list = new ArrayList(1);
		list.add((Data) inData);
		saveAllData(list, inUser);
	}

	protected void updateIndex(Collection<Data> inBuffer, User inUser)
	{
		String catid = toId(getCatalogId());

		//		BulkRequestBuilder brb = getClient().prepareBulk();
		//		brb.add(Requests.indexRequest(indexName).type(getIndexType()).id(id).source(source));
		//    }
		//    if (brb.numberOfActions() > 0) brb.execute().actionGet(); 
		PropertyDetails details = getPropertyDetailsArchive().getPropertyDetailsCached(getSearchType());

		for (Data data : inBuffer)
		{
			try
			{
				IndexRequestBuilder builder = getClient().prepareIndex(catid, getSearchType(), data.getId());
				XContentBuilder content = XContentFactory.jsonBuilder().startObject();
				updateIndex(content, data, details);
				content.endObject();
				IndexResponse response = builder.setSource(content).setRefresh(true).execute().actionGet();
			}
			catch (Exception ex)
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
			PropertyDetail detail = (PropertyDetail) iterator.next();
			String value = inData.get(detail.getId());
			try
			{
				if( detail.isDate())
				{
					if( value == null)
					{
						inContent.field(detail.getId(), (Date)null);
					}
					else
					{
						//ie date = DateStorageUtil.getStorageUtil().parseFromStorage(value);
						inContent.field(detail.getId(), value);
					}
				}
				else if( detail.isBoolean())
				{
					if( value == null)
					{
						inContent.field(detail.getId(), Boolean.FALSE);
					}
					else
					{
						inContent.field(detail.getId(), Boolean.valueOf(value));
					}
				}
				else if( detail.isDataType("number"))
				{
					if( value == null)
					{
						inContent.field(detail.getId(), Long.valueOf(0));
					}
					else
					{
						inContent.field(detail.getId(), Long.valueOf(value));
					}
				}
				else
				{
					if( value == null)
					{
						//inContent.field(detail.getId(), ""); // this ok?
					}
					else
					{
						inContent.field(detail.getId(), value);
					}
				}
				//log.info("Saved" + detail.getId() + "=" + value );
			}
			catch (Exception ex)
			{
				throw new OpenEditException(ex);
			}
		}
	}

	public void deleteAll(User inUser)
	{
		log.info("Deleted old database");
		DeleteByQueryRequestBuilder delete = getClient().prepareDeleteByQuery(toId(getCatalogId()));

		delete.setQuery(new MatchAllQueryBuilder()).execute().actionGet();

	}

	public void delete(Data inData, User inUser)
	{
		String id = inData.getId();
		DeleteRequestBuilder delete = getClient().prepareDelete(toId(getCatalogId()), getSearchType(), id);
		delete.setOperationThreaded(false).setRefresh(true).execute().actionGet();
	}

	//Base class only updated the index in bulk
	public void saveAllData(Collection<Data> inAll, User inUser)
	{
		for(Data data:inAll)
		{
			if( data.getId() == null)
			{
				data.setId(nextId());
			}
		}
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
			Page prop = getPageManager().getPage("/WEB-INF/data/" + getCatalogId() + "/" + getSearchType() + "s/idcounter.properties");
			File file = new File(prop.getContentItem().getAbsolutePath());
			file.getParentFile().mkdirs();
			fieldIntCounter.setCounterFile(file);
		}
		return fieldIntCounter;
	}
	
	public LockManager getLockManager()
	{
		return fieldLockManager;
	}

	public void setLockManager(LockManager inLockManager)
	{
		fieldLockManager = inLockManager;
	}

}
