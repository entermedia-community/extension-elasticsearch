package org.entermedia.elasticsearch.searchers;

import java.io.File;
import java.io.IOException;
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
import org.entermedia.locks.Lock;
import org.openedit.Data;
import org.openedit.data.DataArchive;
import org.openedit.data.PropertyDetails;
import org.openedit.entermedia.Asset;
import org.openedit.entermedia.AssetArchive;
import org.openedit.entermedia.Category;
import org.openedit.entermedia.MediaArchive;
import org.openedit.entermedia.search.AssetSecurityArchive;
import org.openedit.entermedia.search.DataConnector;
import org.openedit.repository.ContentItem;

import com.openedit.OpenEditException;
import com.openedit.hittracker.HitTracker;
import com.openedit.users.User;
import com.openedit.util.IntCounter;
import com.openedit.util.PathProcessor;

public class ElasticAssetDataConnector extends ElasticXmlFileSearcher implements DataConnector
{
	protected AssetSecurityArchive fieldAssetSecurityArchive;
	protected MediaArchive fieldMediaArchive;
	protected IntCounter fieldIntCounter;
	
	public Data createNewData()
	{
		return new Asset();
	}

	public void deleteFromIndex(String inId)
	{
		// TODO Auto-generated method stub
		DeleteRequestBuilder delete = getClient().prepareDelete(toId(getCatalogId()), getSearchType(), inId);
		delete.setOperationThreaded(false).setRefresh(true).execute().actionGet();
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
			return;
		}
		setReIndexing(true);
		try
		{
			getMediaArchive().getAssetArchive().clearAssets();
			//For now just add things to the index. It never deletes

			final List buffer = new ArrayList(100);
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
					updateIndex(asset);
					incrementCount();
				}
			};
			processor.setRecursive(true);
			processor.setRootPath(getPathToData());
			processor.setPageManager(getPageManager());
			processor.setIncludeExtensions("xml");
			processor.process();
			//updateIndex(buffer,null);
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
				inContent.array("category", catids);
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
			inContent.array("category-exact",array );
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

		String[] dirs = asset.getSourcePath().split("/");

		for (int i = 0; i < dirs.length; i++)
		{
			fullDesc.append(dirs[i]);
			fullDesc.append(' ');
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
		allCatalogs.addAll(catalogs);
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

	/**
	 * @deprecated Need to simplify
	 */
	public void updateIndex(Collection<Data> all, boolean b)
	{
		updateIndex(all, null);
	}

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
		Lock lock = getLockManager().lock(getCatalogId(), loadCounterPath(), "admin");
		try
		{
			return String.valueOf(getIntCounter().incrementCount());
		}
		finally
		{
			getLockManager().release(getCatalogId(), lock);
		}
	}

	public Object searchByField(String inField, String inValue)
	{
		if (inField.equals("id") || inField.equals("_id"))
		{
			GetResponse response = getClient().prepareGet(toId(getCatalogId()), getSearchType(), inValue).execute().actionGet();
			return createAssetFromResponse(response.getId(),response.getSource());
			// if(!response.isExists())
			// {
			// return null;
			// }
			// String path = (String)response.getSource().get("sourcepath");

			// return getAssetArchive().getAssetBySourcePath(path);
		}
		if(inField.equals("sourcepath") || inField.equals("_sourcepath")){
			
			SearchRequestBuilder search = getClient().prepareSearch(toId(getCatalogId()));
		
			search.setSearchType(SearchType.DFS_QUERY_THEN_FETCH);
			search.setTypes(getSearchType());
			
			QueryBuilder b = QueryBuilders.termQuery("sourcepath", inValue);
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
		Asset asset = new Asset();
		if(inSource == null){
			return null;
		}
		
		
		asset.setId(inId);
		
		for (Iterator iterator = inSource.keySet().iterator(); iterator.hasNext();)
		{
			String key = (String) iterator.next();
			Object object = inSource.get(key);
			if("category".equals(key)){
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
			if(val != null){
			asset.setProperty(key, val);
			}
		}
		
		Object exactcats =  (Object)inSource.get("category-exact");
		List categories;
		
		if(exactcats instanceof List){
			categories = (List) exactcats;
		}
		else{
			categories = new ArrayList();
			categories.add(exactcats);
		}
		
		for (Iterator iterator = categories.iterator(); iterator.hasNext();)
		{
			String categoryid = (String) iterator.next();
			Category category = getMediaArchive().getCategory(categoryid);
			if(category != null){
				asset.addCategory(category);
			}
			
		}
		//TODO: get this from the index
		ContentItem originalPage = getPageManager().getRepository().getStub("/WEB-INF/data/" + getCatalogId() + "/originals/" + asset.getSourcePath());
		asset.setFolder(originalPage.isFolder());
		return asset;
	}
	

	protected AssetArchive getAssetArchive()
	{
		return getMediaArchive().getAssetArchive();
	}

	protected DataArchive getDataArchive()
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
