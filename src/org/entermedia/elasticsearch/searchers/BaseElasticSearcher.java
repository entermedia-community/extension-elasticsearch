package org.entermedia.elasticsearch.searchers;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthResponse;
import org.elasticsearch.action.admin.indices.create.CreateIndexResponse;
import org.elasticsearch.action.admin.indices.exists.IndicesExistsRequest;
import org.elasticsearch.action.admin.indices.exists.IndicesExistsResponse;
import org.elasticsearch.action.admin.indices.flush.FlushRequest;
import org.elasticsearch.action.admin.indices.flush.FlushResponse;
import org.elasticsearch.action.admin.indices.mapping.put.PutMappingRequest;
import org.elasticsearch.action.admin.indices.mapping.put.PutMappingResponse;
import org.elasticsearch.action.admin.indices.refresh.RefreshRequest;
import org.elasticsearch.action.admin.indices.refresh.RefreshResponse;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.client.AdminClient;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.Requests;
import org.elasticsearch.client.action.delete.DeleteRequestBuilder;
import org.elasticsearch.client.action.deletebyquery.DeleteByQueryRequestBuilder;
import org.elasticsearch.client.action.index.IndexRequestBuilder;
import org.elasticsearch.client.action.search.SearchRequestBuilder;
import org.elasticsearch.cluster.ClusterState;
import org.elasticsearch.cluster.metadata.IndexMetaData;
import org.elasticsearch.cluster.metadata.MappingMetaData;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.MatchAllQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.TextQueryBuilder;
import org.elasticsearch.search.sort.FieldSortBuilder;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.elasticsearch.transport.RemoteTransportException;
import org.entermedia.elasticsearch.ClientPool;
import org.entermedia.elasticsearch.ElasticHitTracker;
import org.entermedia.elasticsearch.ElasticSearchQuery;
import org.entermedia.locks.Lock;
import org.entermedia.locks.LockManager;
import org.openedit.Data;
import org.openedit.data.BaseData;
import org.openedit.data.BaseSearcher;
import org.openedit.data.PropertyDetail;
import org.openedit.data.PropertyDetails;
import org.openedit.util.DateStorageUtil;

import com.openedit.OpenEditException;
import com.openedit.Shutdownable;
import com.openedit.WebPageRequest;
import com.openedit.hittracker.HitTracker;
import com.openedit.hittracker.SearchQuery;
import com.openedit.hittracker.Term;
import com.openedit.page.Page;
import com.openedit.page.manage.PageManager;
import com.openedit.users.User;
import com.openedit.util.IntCounter;

public abstract class BaseElasticSearcher extends BaseSearcher implements Shutdownable
{
	private static final Log log = LogFactory.getLog(BaseElasticSearcher.class);
	protected ClientPool fieldClientPool;
	protected boolean fieldConnected;
	protected IntCounter fieldIntCounter;
	protected PageManager fieldPageManager;
	protected LockManager fieldLockManager;
	protected boolean fieldAutoIncrementId;
	protected boolean fieldReIndexing;
	
	public boolean isReIndexing()
	{
		return fieldReIndexing;
	}

	public void setReIndexing(boolean inReIndexing)
	{
		fieldReIndexing = inReIndexing;
	}

	public boolean isAutoIncrementId()
	{
		return fieldAutoIncrementId;
	}

	public void setAutoIncrementId(boolean inAutoIncrementId)
	{
		fieldAutoIncrementId = inAutoIncrementId;
	}

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
		if( isReIndexing() )
		{
			int timeout = 0;
			while(isReIndexing())
			{
				try
				{
					Thread.sleep(250);
				}
				catch( InterruptedException ex)
				{
					log.error(ex);
				}
				timeout++;
				if( timeout > 100)
				{
					throw new OpenEditException("timeout on search while reindexing" + getSearchType() );
				}
			}
		}
		String json = null;
		try
		{
			if( !( inQuery instanceof ElasticSearchQuery) )
			{
					throw new OpenEditException("Elastic search requires elastic query");
			}
			long start = System.currentTimeMillis();
			SearchRequestBuilder search = getClient().prepareSearch(toId(getCatalogId()));
			search.setSearchType(SearchType.DFS_QUERY_THEN_FETCH);
			search.setTypes(getSearchType());

			QueryBuilder terms = buildTerms(inQuery);
			
			search.setQuery(terms);
			addSorts(inQuery, search);

			json = search.toString();

			ElasticHitTracker hits = new ElasticHitTracker(search);
			hits.setIndexId(getIndexId());
			hits.setSearchQuery(inQuery);

			long end = System.currentTimeMillis() - start;

			log.info(hits.size() + " hits query: " + toId(getCatalogId()) + "/" + getSearchType() + "/_search' -d '" + json + "' sort by " + inQuery.getSorts() + " in " + (double) end / 1000D + " seconds]");

			return hits;
		}
		catch (Exception ex)
		{
			if( json != null)
			{
				log.error("Could not query: " + toId(getCatalogId()) + "/" + getSearchType() + "/_search' -d '" + json + "' sort by " + inQuery.getSorts() , ex);
			}

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
			synchronized (this)
			{
				if( isConnected() )
				{
					return;
				}
				AdminClient admin = getClientPool().getClient().admin();
				boolean reindex = false;
				try
				{
					String indexid = toId(getCatalogId());
					String cluster = indexid;

					ClusterHealthResponse health = admin.cluster().prepareHealth().setWaitForYellowStatus().execute().actionGet(); //yellow works when you only have one node
					if( health.timedOut() )
					{
						throw new OpenEditException("Could not get yellow status");
					}
					
					IndicesExistsRequest existsreq = Requests.indicesExistsRequest(cluster);
					IndicesExistsResponse res = admin.indices().exists(existsreq).actionGet();
					
					if( !res.exists() )
					{
						try
						{
							XContentBuilder jsonBuilder =  XContentFactory.jsonBuilder(); 
							
							CreateIndexResponse newindexres = admin.indices().prepareCreate(cluster)
				            .setSettings(ImmutableSettings.settingsBuilder().loadFromSource(jsonBuilder
				                .startObject()
				                    .startObject("analysis")
				                        .startObject("filter")
				                            .startObject("snowball")
				                                .field("type", "snowball")
				                                .field("language", "English")
				                            .endObject()
				                        .endObject()
				                        .startObject("analyzer")
				                            .startObject("lowersnowball")
				                                .field("type", "custom")
				                                .field("tokenizer", "standard")
	//			                                .field("tokenizer", "keyword")
				                                .field("filter", new String[]{"lowercase","snowball"})
	//			                                .field("filter", new String[]{"lowercase"})
				                            .endObject()
				                        .endObject()			                        
				                    .endObject()
				                .endObject().string()))
				            .execute().actionGet();
							
							if( newindexres.acknowledged() )
							{
								log.info("index created " + cluster);
							}
						}
						catch( RemoteTransportException exists)
						{
							//silent error
							log.debug("Index already exists " +  cluster);
						}
					}

					ClusterState cs = admin.cluster().prepareState().setFilterIndices(indexid).execute().actionGet().getState(); 
					IndexMetaData data = cs.getMetaData().index(indexid);
					boolean runmapping = true;
					if( data != null)
					{
						if( data.getMappings() != null )
						{
							MappingMetaData fields = data.getMappings().get(getSearchType());
							if( fields != null && fields.source() != null)
							{
								runmapping = false;
							}
						}
					}
					if( runmapping )
					{
						XContentBuilder source = buildMapping();
						log.info(cluster + "/" + getSearchType() + "/_mapping' -d '" + source.string() + "'");
						PutMappingRequest  req = Requests.putMappingRequest( cluster ).type(getSearchType());
						req.source(source);
						PutMappingResponse pres = admin.indices().putMapping(req).actionGet(); 
						if( pres.acknowledged() )
						{
							log.info("mapping applied " + getSearchType());
							reindex = true;
						}
					}
					RefreshRequest  req = Requests.refreshRequest( indexid );
					RefreshResponse rres = admin.indices().refresh(req).actionGet();
					if( rres.getFailedShards() > 0 )
					{
						log.error("Could not refresh shards");
					}
					
					log.info("Node is ready for " + getSearchType() );
				}
				catch( Exception ex)
				{
					log.error("index could not be created ", ex);
				}
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
		//https://github.com/elasticsearch/elasticsearch/pull/606
		//https://gist.github.com/870714
		/*
		index.analysis.analyzer.lowercase_keyword.type=custom
		index.analysis.analyzer.lowercase_keyword.filter.0=lowercase
		index.analysis.analyzer.lowercase_keyword.tokenizer=keyword 
		*/	
		
		//jsonproperties  = jsonproperties.startObject("_all");
		//jsonproperties = jsonproperties.field("store", "false");
		//jsonproperties = jsonproperties.field("analyzer", "lowersnowball");					
		//jsonproperties = jsonproperties.field("index_analyzer", "lowersnowball");					
		//jsonproperties = jsonproperties.field("search_analyzer", "lowersnowball");			//lower case does not seem to work		
		//jsonproperties = jsonproperties.field("index", "analyzed");
		//jsonproperties = jsonproperties.field("type", "string");					
		//jsonproperties = jsonproperties.endObject();
		
		for (Iterator i = props.iterator() ; i.hasNext();)
		{
			PropertyDetail detail = (PropertyDetail) i.next();

			if("_id".equals( detail.getId() ) || "id".equals( detail.getId() ) )
			{
				jsonproperties  = jsonproperties.startObject("_id");
				jsonproperties = jsonproperties.field("index", "not_analyzed");
				jsonproperties = jsonproperties.field("type", "string");					
				jsonproperties = jsonproperties.endObject();
				continue;
			}
			jsonproperties  = jsonproperties.startObject(detail.getId());
			if("description".equals( detail.getId() ) )
			{
				String analyzer = "lowersnowball";
				jsonproperties = jsonproperties.field("analyzer", analyzer);
				jsonproperties = jsonproperties.field("type", "string");					
				jsonproperties = jsonproperties.field("index", "analyzed");
				jsonproperties = jsonproperties.field("store", "no");
				jsonproperties = jsonproperties.field("include_in_all", "false");
				jsonproperties = jsonproperties.endObject();

				continue;
			}

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
			else
			{
				jsonproperties = jsonproperties.field("store", "no");
			}
//this does not work yet			
//			if( detail.isKeyword())
//			{
//				jsonproperties = jsonproperties.field("include_in_all", "true");
//			}
//			else
//			{
				jsonproperties = jsonproperties.field("include_in_all", "false");
//			}

			jsonproperties = jsonproperties.endObject();
		}
		jsonproperties = jsonproperties.endObject();
		jsonBuilder = jsonproperties.endObject();
		return jsonproperties;

	}
	
	protected QueryBuilder buildTerms(SearchQuery inQuery)
	{
		
		if( inQuery.getTerms().size() == 1 && inQuery.getChildren().size() == 0)
		{
			Term term = (Term)inQuery.getTerms().iterator().next();

			if ("orsGroup".equals(term.getOperation()))
			{
				return addOrsGroup(term);
			}

			String value = term.getValue();
			if( value.equals("*"))
			{
				return QueryBuilders.matchAllQuery();
			}
			QueryBuilder find =buildTerm(term.getDetail(),term,value);
			return find;
		}

		BoolQueryBuilder bool = QueryBuilders.boolQuery();

		if( inQuery.isAndTogether() )
		{
			//TODO: Deal with subqueries, or, and, not 
			for (Iterator iterator = inQuery.getTerms().iterator(); iterator.hasNext();)
			{
				Term term = (Term) iterator.next();
				if ("orsGroup".equals(term.getOperation()))
				{
					BoolQueryBuilder or = addOrsGroup(term);
					bool.must(or);
				}
				else
				{
					String value = term.getValue();
					QueryBuilder find =buildTerm(term.getDetail(),term, value);
					if( find != null)
					{
						bool.must(find);
					}
				}
			}
		}
		else
		{
			for (Iterator iterator = inQuery.getTerms().iterator(); iterator.hasNext();)
			{
				Term term = (Term) iterator.next();
				if ("orsGroup".equals(term.getOperation()))
				{
					BoolQueryBuilder or = addOrsGroup(term);
					bool.should(or);
				}
				else
				{
					String value = term.getValue();
					QueryBuilder find =buildTerm(term.getDetail(),term,  value);
					if( find != null)
					{
						bool.should(find);
					}
				}
			}
		}

		if( inQuery.getChildren().size() > 0)
		{
			for (Iterator iterator = inQuery.getChildren().iterator(); iterator.hasNext();)
			{
				SearchQuery query = (SearchQuery) iterator.next();
				QueryBuilder builder = buildTerms(query);
				if( inQuery.isAndTogether() )
				{
					bool.must(builder);
				}
				else
				{
					bool.should(builder);
				}
			}
		}
		return bool;

	
	}

	protected BoolQueryBuilder addOrsGroup(Term term)
	{
		if (term.getValues() != null)
		{
			BoolQueryBuilder or = QueryBuilders.boolQuery();
			for (int i = 0; i < term.getValues().length; i++)
			{
				Object val = term.getValues()[i];
				QueryBuilder aterm = buildTerm(term.getDetail(),term, val);
				if( aterm != null)
				{
					or.should(aterm);
				}
			}
			return or;
		}
		return null;
	}

	protected QueryBuilder buildTerm(PropertyDetail inDetail, Term inTerm, Object inValue)
	{
		//Check for quick date object
		QueryBuilder find = null;
		if( inValue instanceof Date)
		{
			find = QueryBuilders.termQuery(inDetail.getId(), (Date)inValue);
		}
		else
		{
			String valueof = String.valueOf(inValue);
			
			String fieldid = inDetail.getId();
//			if( fieldid.equals("description"))
//			{
//				//fieldid = "_all";
//				//valueof  = valueof.toLowerCase();
//				find = QueryBuilders.textQuery(fieldid, valueof);
//				return find;
//			}
			if(fieldid.equals("id"))
			{
			//	valueof  = valueof.toLowerCase();				
				find = QueryBuilders.termQuery("_id", valueof);
				return find;
			}
			
			if( valueof.equals("*"))
			{
				find = QueryBuilders.matchAllQuery();
			}
			else if ( valueof.endsWith("*"))
			{
				valueof = valueof.substring(0,valueof.length()-1);
				TextQueryBuilder text = QueryBuilders.textPhrasePrefixQuery(fieldid, valueof);
				text.maxExpansions(10);
				find = text;
			}
			else if( valueof.contains("*"))
			{
				find = QueryBuilders.wildcardQuery(fieldid, valueof);
			}
			else if( "startswith".equals( inTerm.getOperation() ) )
			{
				TextQueryBuilder text = QueryBuilders.textPhrasePrefixQuery(fieldid, valueof);
				text.maxExpansions(10);
				find = text;
			}
			else if( inDetail.isBoolean())
			{
				find = QueryBuilders.termQuery(fieldid, Boolean.parseBoolean(valueof));
			}
			else if( inDetail.isDate())
			{
				if( "beforedate".equals(inTerm.getOperation()))
				{
					Date date = DateStorageUtil.getStorageUtil().parseFromStorage(valueof);
					
					find = QueryBuilders.rangeQuery(inDetail.getId())
		                .from(new Date(0))
		                .to(date);	
				}
				else
				{
					Date date = DateStorageUtil.getStorageUtil().parseFromStorage(valueof);
					find = QueryBuilders.termQuery(fieldid, date);
				}
			}
			else if( inDetail.isDataType("number") )
			{
				find = QueryBuilders.termQuery(fieldid, Long.parseLong(valueof));
			}
			else if( fieldid.equals("description"))
			{
				find = QueryBuilders.textQuery(fieldid, valueof);				
			}
			else
			{
				find = QueryBuilders.termQuery(fieldid, valueof);
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
			updateElasticIndex(details, data);
		}
		log.info("Saved "  + inBuffer.size() + " records into " + toId(getCatalogId()) + "/" + getSearchType() );
		inBuffer.clear();
	}

	protected void updateElasticIndex(PropertyDetails details, Data data)
	{
		try
		{
			String catid = toId(getCatalogId() );
			IndexRequestBuilder builder = null;
			if( data.getId() == null)
			{
				builder = getClient().prepareIndex(catid, getSearchType());
			}
			else
			{
				builder = getClient().prepareIndex(catid, getSearchType(), data.getId());
			}
			XContentBuilder content = XContentFactory.jsonBuilder().startObject();
			updateIndex(content, data, details);
			content.endObject();
			log.info(content.string());
			IndexResponse response = builder.setSource(content).setRefresh(true).execute().actionGet();
			if( response.getId() != null)
			{
				data.setId(response.getId());
			}
		}
		catch (Exception ex)
		{
			if( ex instanceof OpenEditException)
			{
				throw (OpenEditException)ex;
			}
			throw new OpenEditException(ex);
		}
	}

	protected void updateIndex(XContentBuilder inContent, Data inData, PropertyDetails inDetails)
	{
		for (Iterator iterator = inDetails.findIndexProperties().iterator(); iterator.hasNext();)
		{
			PropertyDetail detail = (PropertyDetail) iterator.next();
			String value = inData.get(detail.getId());
			if( value != null && value.length() == 0)
			{
				value = null;
			}
			try
			{
				if( "_id".equals( detail.getId() ) || "id".equals(detail.getId() ) )
				{
//					if( value != null)
//					{
//						inContent.field("_id", value);
//						continue;
//					}
					continue;
				}

				if( detail.isDate())
				{
					if( value != null)
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
				else if( detail.getId().equals("description") )
				{
					StringBuffer desc = new StringBuffer();
					populateKeywords(desc, inData, inDetails);
					if( desc.length() > 0)
					{
						inContent.field(detail.getId(), desc.toString());
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
		log.info("Deleted all records database " + getSearchType() );
		DeleteByQueryRequestBuilder delete = getClient().prepareDeleteByQuery(toId(getCatalogId()));
		delete.setTypes(getSearchType());
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
		updateIndex(inAll, inUser);
	}

	public synchronized String nextId()
	{
		Lock lock = getLockManager().lock(getCatalogId(), loadCounterPath(), "admin");
		try
		{
			return String.valueOf(getIntCounter().incrementCount());
		}
		finally
		{
			getLockManager().release(getCatalogId(), lock);
		}
//		throw new OpenEditException("Should not call next ID");
	}

	protected IntCounter getIntCounter()
	{
		if (fieldIntCounter == null)
		{
			fieldIntCounter = new IntCounter();
			//fieldIntCounter.setLabelName(getSearchType() + "IdCount");
			Page prop = getPageManager().getPage(loadCounterPath());
			File file = new File(prop.getContentItem().getAbsolutePath());
			file.getParentFile().mkdirs();
			fieldIntCounter.setCounterFile(file);
		}
		return fieldIntCounter;
	}

	protected String loadCounterPath()
	{
		return "/WEB-INF/data/" + getCatalogId() + "/" + getSearchType() + "s/idcounter.properties";
	}
	
	public LockManager getLockManager()
	{
		return fieldLockManager;
	}

	public void setLockManager(LockManager inLockManager)
	{
		fieldLockManager = inLockManager;
	}

	public void shutdown()
	{
		if( fieldClientPool != null)
		{
			getClient().close();
		}
	}
	
	public boolean hasChanged(HitTracker inTracker)
	{
		return true;
	}

	public HitTracker checkCurrent(WebPageRequest inReq, HitTracker inTracker) throws OpenEditException
	{
		return inTracker;
	}
	
	protected boolean flushChanges()
	{
		FlushRequest  req = Requests.flushRequest( toId(getCatalogId()) );
		FlushResponse res = getClient().admin().indices().flush(req).actionGet();
		if( res.successfulShards() > 0)
		{
			return true;
		}
		return false;
	}
	public Object searchByField(String inField, String inValue)
	{
		if( inField.equals("id") || inField.equals("_id"))
		{
			GetResponse response = getClient().prepareGet(toId(getCatalogId()), getSearchType(), inValue).execute().actionGet();
			Data data = new BaseData(response.getSource());
			data.setId(inValue);
			return data;
		}
		return super.searchByField(inField, inValue);
	}
	
	protected void populateKeywords(StringBuffer inFullDesc, Data inData, PropertyDetails inDetails)
	{
		for (Iterator iter = inDetails.findKeywordProperties().iterator(); iter.hasNext();)
		{
			PropertyDetail det = (PropertyDetail) iter.next();
			if (det.isList())
			{
				String prop = inData.get(det.getId());
				if (prop != null)
				{
					Data data = (Data)getSearcherManager().getData(det.getListCatalogId(), det.getListId(), prop);
					if( data != null && data.getName() != null)
					{
						inFullDesc.append(data.getName());
						inFullDesc.append(' ');
					}
				}
			}
			else
			{
				String val = inData.get(det.getId());
				if( val != null)
				{
					inFullDesc.append(val);
					inFullDesc.append(' ');
				}
			}
		}
	}

}
