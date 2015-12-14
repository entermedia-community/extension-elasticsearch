package org.entermediadb.elasticsearch.searchers;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.elasticsearch.action.delete.DeleteRequestBuilder;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.entermediadb.asset.Asset;
import org.entermediadb.asset.AssetArchive;
import org.entermediadb.asset.Category;
import org.entermediadb.asset.MediaArchive;
import org.entermediadb.asset.search.AssetSecurityArchive;
import org.entermediadb.asset.search.DataConnector;
import org.entermediadb.data.DataArchive;
import org.entermediadb.elasticsearch.SearchHitData;
import org.openedit.Data;
import org.openedit.OpenEditException;
import org.openedit.data.PropertyDetails;
import org.openedit.hittracker.HitTracker;
import org.openedit.locks.Lock;
import org.openedit.repository.ContentItem;
import org.openedit.users.User;
import org.openedit.util.IntCounter;
import org.openedit.util.OutputFiller;
import org.openedit.util.PathProcessor;

public class ElasticAssetDataConnector extends ElasticXmlFileSearcher implements DataConnector
{
	protected AssetSecurityArchive fieldAssetSecurityArchive;
	protected MediaArchive fieldMediaArchive;
	protected IntCounter fieldIntCounter;
	protected OutputFiller filler = new OutputFiller();
	public Data createNewData()
	{
		return new Asset(getMediaArchive());
	}

	protected DataArchive getDataArchive()
	{
		if (fieldDataArchive == null)
		{
			DataArchive archive = (DataArchive)getModuleManager().getBean(getCatalogId(),"assetDataArchive");
			archive.setDataFileName(getDataFileName());
			archive.setElementName(getSearchType());
			archive.setPathToData(getPathToData());
			fieldDataArchive = archive;
		}

		return fieldDataArchive;
	}

	
	public void deleteFromIndex(String inId)
	{
		// TODO Auto-generated method stub
		DeleteRequestBuilder delete = getClient().prepareDelete(toId(getCatalogId()), getSearchType(), inId);
		delete.setRefresh(true).execute().actionGet();
		// delete()
	}

	public void deleteFromIndex(HitTracker inOld)
	{
		for (Iterator iterator = inOld.iterator(); iterator.hasNext();)
		{
			Data hit = (Data) iterator.next();
			deleteFromIndex(hit.getId());
		}

	}


	public void reIndexAll() throws OpenEditException
	{		
		if( isReIndexing())
		{
			return;  //TODO: Make a lock so that two servers startin up dont conflict?
		}
		setReIndexing(true);
		try
		{
			getMediaArchive().getAssetArchive().clearAssets();
			//For now just add things to the index. It never deletes
			if( fieldConnected )
			{
				//Someone is forcing a reindex
				deleteOldMapping();
				putMappings();
			}
			final List tosave = new ArrayList(500);
			
			PathProcessor processor = new PathProcessor()
			{
				public void processFile(ContentItem inContent, User inUser)
				{
					if (!inContent.getName().equals(getDataFileName()))
					{
						return;
					}
					String sourcepath = inContent.getPath();
					sourcepath = sourcepath.substring(getPathToData().length() + 1,
							sourcepath.length() - getDataFileName().length() - 1);
					Asset asset = getMediaArchive().getAssetArchive().getAssetBySourcePath(sourcepath);
					tosave.add(asset);
					if(tosave.size() == 500)
					{
						updateIndex(tosave,null);
						tosave.clear();
					}
					incrementCount();
				}
			};
			processor.setRecursive(true);
			processor.setRootPath(getPathToData());
			processor.setPageManager(getPageManager());
			processor.setIncludeExtensions("xml");
			processor.process();
			updateIndex(tosave,null);
			log.info("reindexed " + processor.getExecCount());
			flushChanges();			
		}
		catch(Exception e){
			throw new OpenEditException(e);
		}
		finally
		{
			setReIndexing(false);
		}
	}
	
	
	
	/**
	 * @deprecated Need to simplify
	 */
	public void updateIndex(Data one)
	{
		List all = new ArrayList(1);
		all.add(one);
		updateIndex(all, null);
		// updateIndex(one,null);
	}

	protected void updateIndex(XContentBuilder inContent, Data inData, PropertyDetails inDetails)
	{
		Asset asset = (Asset) inData;

		try
		{
			String fileformat = asset.getFileFormat();
			if (fileformat != null)
			{
				inContent.field("fileformat", fileformat);
			}

			if (asset.getCatalogId() == null)
			{
				asset.setCatalogId(getCatalogId());
			}
			inContent.field("catalogid", asset.getCatalogId());

			Set categories = buildCategorySet(asset);
			String desc = populateDescription(asset, inDetails, categories);
			inContent.field("description", desc);

			String[] catids = new String[categories.size()];
			int i = 0;
			for (Iterator iterator = categories.iterator(); iterator.hasNext();)
			{
				Category cat = (Category) iterator.next();
				catids[i++] = cat.getId();
			}
			if (i > 0)
			{
				inContent.field("category", catids);
			}

		
			
			// Searcher searcher =
			// getSearcherManager().getSearcher(asset.getCatalogId(),"assetalbums");
			// SearchQuery query = searcher.createSearchQuery();
			// query.addMatches("assetid", asset.getId());
			// HitTracker tracker = searcher.search(query);
			// populateJoinData("album", doc, tracker, "albumid", true);

			// populateSecurity(doc, asset, catalogs);
			populatePermission(inContent, asset, "viewasset");

			super.updateIndex(inContent, inData, inDetails);
			// for (Iterator iterator =
			// inDetails.findIndexProperties().iterator(); iterator.hasNext();)
			// {
			// PropertyDetail detail = (PropertyDetail)iterator.next();
			// String value = inData.get(detail.getId());
			// if( value != null)
			// {
			// //TODO: Deal with data types and move to indexer object
			// inContent.field(detail.getId(),value);
			// //log.info("Saved" + detail.getId() + "=" + value );
			// }
			// }
			
			//This is for saving and loading.
			ArrayList <String>realcats = new ArrayList();
			for (Iterator iterator = asset.getCategories().iterator(); iterator.hasNext();)
			{
				Category cat = (Category)iterator.next();
				String catid = cat.getId();
				realcats.add(catid);
			}
			String[] array = new String[realcats.size()];
			array =  realcats.toArray(array);
			inContent.field("category-exact",array);
		}
		catch (Exception ex)
		{
			throw new OpenEditException(ex);
		}
	}
	/*
	protected void hydrateData(ContentItem inContent, String sourcepath, List buffer)
	{
		Asset data = getMediaArchive().getAssetBySourcePath(sourcepath);
		if (data == null)
		{
			return;
		}
		buffer.add(data);
		if (buffer.size() > 99)
		{
			updateIndex(buffer, null);
		}
	}
	*/
	protected void populatePermission(XContentBuilder inContent, Asset inAsset, String inPermission) throws IOException
	{
		List add = getAssetSecurityArchive().getAccessList(getMediaArchive(), inAsset);
		if (add.size() == 0)
		{
			add.add("true");
		}
		inContent.array(inPermission, add.toArray());

	}

	protected String populateDescription(Asset asset, PropertyDetails inDetails, Set inCategories)
	{
		// Low level reading in of text
		StringBuffer fullDesc = new StringBuffer();
		// fullDesc.append(asset.getName());
		// fullDesc.append(' ');
		//
		// fullDesc.append(asset.getFileFormat());
		// fullDesc.append(' ');

		// fullDesc.append(asset.getId());
		// fullDesc.append(' ');
		
		String keywords = asTokens(asset.getKeywords());
		fullDesc.append(keywords);
		fullDesc.append(' ');

		populateKeywords(fullDesc, asset, inDetails);
		// add a bunch of stuff to the full text field
		for (Iterator iter = inCategories.iterator(); iter.hasNext();)
		{
			Category cat = (Category) iter.next();
			fullDesc.append(cat.getName());
			fullDesc.append(' ');
		}
		if( asset.getSourcePath() != null)
		{
			String[] dirs = asset.getSourcePath().split("/");
	
			for (int i = 0; i < dirs.length; i++)
			{
				fullDesc.append(dirs[i]);
				fullDesc.append(' ');
			}
			if( Boolean.parseBoolean(asset.get("hasfulltext")))
			{
				ContentItem item = getPageManager().getRepository().getStub("/WEB-INF/data/" + getCatalogId() +"/assets/" + asset.getSourcePath() + "/fulltext.txt");
				if( item.exists() )
				{
					Reader input = null;
					try
					{
						input= new InputStreamReader( item.getInputStream(), "UTF-8");
						StringWriter output = new StringWriter(); 
						filler.fill(input, output);
						fullDesc.append(output.toString());
					}
					catch( IOException ex)
					{
						log.error(ex);
					}
					finally
					{
						filler.close(input);
					}
				}
			}
		}
//		fullDesc.append(asset.get("fulltext")); //TODO: Is this set? Should we store this another way?
//
//		//TODO: Limit the size? Trim words?
		if( fullDesc.length() > 500000)
		{
			return fullDesc.substring(0,500000);
		}
		
		String result = fullDesc.toString();// fixInvalidCharacters(fullDesc.toString());
		return result;
	}

	protected String asTokens(Collection inList)
	{
		StringBuffer buffer = new StringBuffer();
		for (Iterator iter = inList.iterator(); iter.hasNext();)
		{
			String desc = (String) iter.next();
			buffer.append(desc);
			if (iter.hasNext())
			{
				buffer.append(' ');
			}
		}
		return buffer.toString();
	}

	protected Set buildCategorySet(Asset inAsset)
	{
		HashSet allCatalogs = new HashSet();
		Collection catalogs = inAsset.getCategories();
		//allCatalogs.addAll(catalogs);
		for (Iterator iter = catalogs.iterator(); iter.hasNext();)
		{
			Category catalog = (Category) iter.next();
			buildCategorySet(catalog, allCatalogs);
		}
		return allCatalogs;
	}

	protected void buildCategorySet(Category inCatalog, Set inCatalogSet)
	{
		inCatalogSet.add(inCatalog);
		Category parent = inCatalog.getParentCategory();
		if (parent != null)
		{
			buildCategorySet(parent, inCatalogSet);
		}
	}

//	/**
//	 * @deprecated Need to simplify
//	 */
//	public void updateIndex(Collection<Data> all, boolean b)
//	{
//		updateIndex(all, null);
//	}

	public AssetSecurityArchive getAssetSecurityArchive()
	{
		return fieldAssetSecurityArchive;
	}

	public void setAssetSecurityArchive(AssetSecurityArchive inAssetSecurityArchive)
	{
		fieldAssetSecurityArchive = inAssetSecurityArchive;
	}

	public MediaArchive getMediaArchive()
	{
		if (fieldMediaArchive == null)
		{
			fieldMediaArchive = (MediaArchive) getModuleManager().getBean(getCatalogId(), "mediaArchive");
		}

		return fieldMediaArchive;
	}

	public void flush()
	{
	}

	public void setRootDirectory(File inRoot)
	{
	}

	protected IntCounter getIntCounter()
	{
		if (fieldIntCounter == null)
		{
			fieldIntCounter = super.getIntCounter();
			fieldIntCounter.setLabelName(getSearchType() + "IdCount");
		}
		return fieldIntCounter;
	}
	public synchronized String nextId()
	{
		Lock lock = getLockManager().lock(loadCounterPath(), "admin");
		try
		{
			return String.valueOf(getIntCounter().incrementCount());
		}
		finally
		{
			getLockManager().release(lock);
		}
	}

	public Object searchByField(String inField, String inValue)
	{
		if (inField.equals("id") || inField.equals("_id"))
		{
			GetResponse response = getClient().prepareGet(toId(getCatalogId()), getSearchType(), inValue).execute().actionGet();
			if(!response.isExists())
			{
				return null;
			}
			Asset asset =  createAssetFromResponse(response.getId(),response.getSource());
			return asset;
			// String path = (String)response.getSource().get("sourcepath");

			// return getAssetArchive().getAssetBySourcePath(path);
		}
		if(inField.equals("sourcepath") || inField.equals("_sourcepath")){
			
			SearchRequestBuilder search = getClient().prepareSearch(toId(getCatalogId()));
		
			search.setSearchType(SearchType.DFS_QUERY_THEN_FETCH);
			search.setTypes(getSearchType());
			
			QueryBuilder b = QueryBuilders.matchQuery("sourcepath", inValue);
			search.setQuery(b);
			SearchResponse response = search.execute().actionGet();
			Iterator <SearchHit> responseiter = response.getHits().iterator();
			if(responseiter.hasNext()){
				SearchHit hit = responseiter.next();
				return createAssetFromResponse(hit.getId(),hit.getSource());
				
				
			}
			return null;
		}
		return super.searchByField(inField, inValue);
	}

	protected Asset createAssetFromResponse(String inId, Map inSource)
	{
		Asset asset = (Asset)createNewData();
		if(inSource == null){
			return null;
		}
		asset.setId(inId);
		
		for (Iterator iterator = inSource.keySet().iterator(); iterator.hasNext();)
		{
			String key = (String) iterator.next();
			Object object = inSource.get(key);
			if("category-exact".equals(key)){
				continue;
			}
			String val = null;
			if (object instanceof String) {
				val= (String) object;
			}
			if (object instanceof Date) {
				val= String.valueOf((Date) object);
			}
			if (object instanceof Boolean) {
				val= String.valueOf((Boolean) object);
			}
			if (object instanceof Integer) {
				val= String.valueOf((Integer) object);
			}
			if (object instanceof Float) {
				val= String.valueOf((Float) object);
			}
			if (object instanceof Collection) {
				Collection values = (Collection) object;
				asset.setValues(key, (Collection<String>) object);
			}
			else if(val != null)
			{
				asset.setProperty(key, val);
			}
		}
		Collection categories = (Collection)inSource.get("category-exact");
		if( categories != null)
		{
			for (Iterator iterator = categories.iterator(); iterator.hasNext();)
			{
				String categoryid = (String) iterator.next();
				Category category = getMediaArchive().getCategory(categoryid); //Cache this? Or lazy load em
				if(category != null){
					asset.addCategory(category);
				}
			}
		}	
		String isfolder = asset.get("isfolder");
		if( isfolder == null)
		{
			ContentItem originalPage = getPageManager().getRepository().getStub("/WEB-INF/data/" + getCatalogId() + "/originals/" + asset.getSourcePath());
			asset.setFolder(originalPage.isFolder());
		}	
		return asset;
	}
	
	//TODO: Make an ElasticAsset bean type that can be searched and saved
	@Override
	public Data loadData(Data inHit)
	{
		if( inHit instanceof Asset)
		{
			return inHit;
		}
		//Stuff might get out of date?
		if( inHit instanceof SearchHitData)
		{
			SearchHitData db = (SearchHitData)inHit;
			return createAssetFromResponse(inHit.getId(), db.getSearchHit().getSource() );
		}
		return (Data)searchById(inHit.getId());
	}
	protected AssetArchive getAssetArchive()
	{
		return getMediaArchive().getAssetArchive();
	}

	public Data getDataBySourcePath(String inSourcePath)
	{
		return (Data) searchByField("sourcepath", inSourcePath);
	}

	public Data getDataBySourcePath(String inSourcePath, boolean inAutocreate)
	{
		return (Data) searchByField("sourcepath", inSourcePath);

	}
	
	
	public String getPathToData()
	{
		return "/WEB-INF/data/" + getCatalogId() + "/assets";
	}
	
	
}
