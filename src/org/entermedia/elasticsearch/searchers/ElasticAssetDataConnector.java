package org.entermedia.elasticsearch.searchers;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.client.action.delete.DeleteRequestBuilder;
import org.elasticsearch.client.action.search.SearchRequestBuilder;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.openedit.Data;
import org.openedit.data.BaseData;
import org.openedit.data.DataArchive;
import org.openedit.data.PropertyDetail;
import org.openedit.data.PropertyDetails;
import org.openedit.entermedia.Asset;
import org.openedit.entermedia.Category;
import org.openedit.entermedia.MediaArchive;
import org.openedit.entermedia.search.AssetSecurityArchive;
import org.openedit.entermedia.search.DataConnector;
import org.openedit.repository.ContentItem;

import com.openedit.OpenEditException;
import com.openedit.hittracker.HitTracker;
import com.openedit.page.Page;
import com.openedit.util.IntCounter;

public class ElasticAssetDataConnector extends ElasticXmlFileSearcher implements DataConnector
{
	protected AssetSecurityArchive fieldAssetSecurityArchive;
	protected MediaArchive fieldMediaArchive;
	public Data createNewData()
	{
		return new Asset();
	}
	public void deleteFromIndex(String inId)
	{
		// TODO Auto-generated method stub
		DeleteRequestBuilder delete = getClient().prepareDelete(toId(getCatalogId()), getSearchType(), inId);
		delete.setOperationThreaded(false)
        .execute()
        .actionGet();
		//delete()
	}

	public void deleteFromIndex(HitTracker inOld)
	{
		for (Iterator iterator = inOld.iterator(); iterator.hasNext();)
		{
			Data hit = (Data) iterator.next();
			deleteFromIndex(hit.getId());
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
		//updateIndex(one,null);
	}

	protected void updateIndex(XContentBuilder inContent, Data inData, PropertyDetails inDetails)
	{
		Asset asset = (Asset)inData;

		try
		{
			String fileformat = asset.getFileFormat();
			if(fileformat != null)
			{
				inContent.field("fileformat",fileformat);
			}
			
			if (asset.getCatalogId() == null)
			{
				asset.setCatalogId(getCatalogId());
			}
			inContent.field("catalogid", asset.getCatalogId() );
	
			Set categories = buildCategorySet(asset);
			String desc = populateDescription(asset, inDetails, categories);
			inContent.field("description", desc );

			String[] catids = new String[categories.size()];
			int i = 0;
			for (Iterator iterator = categories.iterator(); iterator.hasNext();)
			{
				Category cat = (Category) iterator.next();
				catids[i++] = cat.getId();				
			}
			if( i > 0)
			{
				inContent.array("category",catids);
			}
	//		Searcher searcher = getSearcherManager().getSearcher(asset.getCatalogId(),"assetalbums");
	//		SearchQuery query = searcher.createSearchQuery();
	//		query.addMatches("assetid", asset.getId());
	//		HitTracker tracker = searcher.search(query);
	//		populateJoinData("album", doc, tracker, "albumid", true);
			
			// populateSecurity(doc, asset, catalogs);
			populatePermission(inContent, asset, "viewasset");

			super.updateIndex(inContent, inData, inDetails);
//			for (Iterator iterator = inDetails.findIndexProperties().iterator(); iterator.hasNext();)
//			{
//				PropertyDetail detail = (PropertyDetail)iterator.next();
//				String value = inData.get(detail.getId());
//				if( value != null)
//				{
//					//TODO: Deal with data types and move to indexer object
//					inContent.field(detail.getId(),value);
//					//log.info("Saved" + detail.getId() + "=" + value );
//				}
//			}
		}
		catch( Exception ex)
		{
			throw new OpenEditException(ex);
		}
	}
	
	protected void hydrateData(ContentItem inContent, String sourcepath, List buffer)
	{
		Asset data = getMediaArchive().getAssetBySourcePath(sourcepath);
		if( data == null)
		{
			return;
		}
		buffer.add(data);
		if( buffer.size() > 99)
		{
			updateIndex(buffer,null);
		}
	}
	
	protected void populatePermission(XContentBuilder inContent, Asset inAsset, String inPermission) throws IOException
	{
		List add = getAssetSecurityArchive().getAccessList(getMediaArchive(), inAsset);
		if (add.size() == 0)
		{
			add.add("true");
		}
		inContent.array(inPermission, add.toArray() );

	}

	protected String populateDescription(Asset asset, PropertyDetails inDetails, Set inCategories)
	{
		// Low level reading in of text
		StringBuffer fullDesc = new StringBuffer();
//		fullDesc.append(asset.getName());
//		fullDesc.append(' ');
//		
//		fullDesc.append(asset.getFileFormat());
//		fullDesc.append(' ');
		
//		fullDesc.append(asset.getId());
//		fullDesc.append(' ');
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
		
		String result = fullDesc.toString();//fixInvalidCharacters(fullDesc.toString());
		return result;
	}
	protected String asTokens(Collection inList)
	{
		StringBuffer buffer = new StringBuffer();
		for (Iterator iter = inList.iterator(); iter.hasNext();)
		{
			String desc = (String) iter.next();
			buffer.append(desc);
			if( iter.hasNext())
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

	protected DataArchive getDataArchive()
	{
		if (fieldXmlDataArchive == null)
		{
			fieldXmlDataArchive = getMediaArchive().getAssetArchive();
			fieldXmlDataArchive.setXmlArchive(getXmlArchive());
			fieldXmlDataArchive.setDataFileName(getDataFileName());
			fieldXmlDataArchive.setElementName(getSearchType());
			fieldXmlDataArchive.setPathToData(getPathToData());
		}
		return fieldXmlDataArchive;
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

	
	public Object searchByField(String inField, String inValue)
	{
		if( inField.equals("id") || inField.equals("_id"))
		{
			GetResponse response = getClient().prepareGet(toId(getCatalogId()), getSearchType(), inValue).execute().actionGet();
			if(!response.exists())
			{
				return null;
			}
			String path = (String)response.getSource().get("sourcepath");
			return getMediaArchive().getAssetArchive().getAssetBySourcePath(path);
		}
		return super.searchByField(inField, inValue);
	}
}
