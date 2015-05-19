package org.entermedia.elasticsearch.categories;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.entermedia.elasticsearch.searchers.BaseElasticSearcher;
import org.openedit.Data;
import org.openedit.data.PropertyDetails;
import org.openedit.entermedia.Category;
import org.openedit.entermedia.xmldb.CategorySearcher;

import com.openedit.OpenEditException;
import com.openedit.users.User;

public class ElasticCategorySearcher extends BaseElasticSearcher implements CategorySearcher//, Reloadable
{
	private static final Log log = LogFactory.getLog(ElasticCategorySearcher.class);
	//protected CategoryArchive fieldCategoryArchive;
	protected Category fieldRootCategory;
	
	public Data createNewData()
	{
		return new ElasticCategory(this);
	}
//	protected Category refreshData(String inId, GetResponse response) 
//	{
//		ElasticCategory category = (ElasticCategory)createNewData();
//		category.setProperties(response.getSource());
//		
//		return category;
//	}

	public List findChildren(Category inParent) 
	{
		Collection hits = fieldSearch("parentid", inParent.getId(),"name");
		List children = new ArrayList(hits.size());
		for (Iterator iterator = hits.iterator(); iterator.hasNext();) {
			Data data = (Data) iterator.next();
			ElasticCategory category = (ElasticCategory)createNewData();
			category.setId(data.getId());
			category.setProperties(data.getProperties());
			category.setParentCategory(inParent);
			children.add(category);
		}
		return children;
	}

	public void reIndexAll() throws OpenEditException 
	{
		
		//there is not reindex step since it is only in memory
		if (isReIndexing())
		{
			return;
		}
		try
		{
			setReIndexing(true);
			if( fieldConnected )
			{
				putMappings(); //We can only try to put mapping. If this failes then they will
				//need to export their data and factory reset the fields 
			}
			//deleteAll(null); //This only deleted the index
			//This is the one time we load up the categories from the XML file
//			Category parent = getCategoryArchive().getRootCategory();
//			List tosave = new ArrayList();
//			updateChildren(parent,tosave);
//			updateIndex(tosave,null);
		}
		finally
		{
			setReIndexing(false);
		}	
	}
	protected void updateChildren(Category inParent, List inTosave)
	{
		// TODO Auto-generated method stub
		inTosave.add(inParent);
		if( inTosave.size() == 100)
		{
			updateIndex(inTosave,null);
			inTosave.clear();
		}
		for (Iterator iterator = inParent.getChildren().iterator(); iterator.hasNext();)
		{
			Category child = (Category) iterator.next();
			updateChildren(child,inTosave);
		}
	}

	protected void updateIndex(XContentBuilder inContent, Data inData, PropertyDetails inDetails)
	{
		Category category = (Category)inData;
		Category parent  = category.getParentCategory();
		if( parent != null)
		{
			try
			{
				inContent.field("parentid", parent.getId());
			}
			catch (Exception ex)
			{
				throw new OpenEditException(ex);
			}
		}
		super.updateIndex(inContent,inData,inDetails);
	}
	@Override
	public Category getRootCategory()
	{
		if( fieldRootCategory == null)
		{
			fieldRootCategory = (Category)searchById("index");
			if( fieldRootCategory == null)
			{
				fieldRootCategory = (Category)createNewData();
				fieldRootCategory.setId("index");
				fieldRootCategory.setName("Index");
				saveData(fieldRootCategory, null);
				//We are going to create a database tool to import categories.xml
			}
		}	
		return fieldRootCategory;
	}
//	public CategoryArchive getCategoryArchive()
//	{
//		if(fieldCategoryArchive != null){
//			fieldCategoryArchive.setCatalogId(getCatalogId());
//		}
//		return fieldCategoryArchive;
//	}
//	public void setCategoryArchive(CategoryArchive inCategoryArchive)
//	{
//		fieldCategoryArchive = inCategoryArchive;
//		inCategoryArchive.setCatalogId(getCatalogId());
//	}
	@Override
	public Category getCategory(String inCatalog)
	{
		return (Category)searchById(inCatalog);
	}
	
	public Object searchByField(String inField, String inValue)
	{
		if( inField.equals("id") || inField.equals("_id"))
		{
			GetResponse response = getClient().prepareGet(toId(getCatalogId()), getSearchType(), inValue).execute().actionGet();
			if( response.isExists() )
			{
				ElasticCategory data = (ElasticCategory) createNewData();
				data.setProperties(response.getSource());
				//data.
				//copyData(data, typed);
				data.setId(inValue);
				if( response.getVersion() > -1)
				{
					data.setProperty(".version",String.valueOf(response.getVersion()) );
				}
				return data;
			}
			return null;
		}
		return super.searchByField(inField, inValue);
	}

	@Override
	public void saveCategory(Category inCategory)
	{
		saveData(inCategory, null);
	}
	
	public void saveData(Data inData, User inUser)
	{
		super.saveData(inData, inUser);
		getRootCategory().setProperty("dirty", true);
//		cat = (ElasticCategory)cat.getParentCategory();
//		if( cat == null)
//		{
//			cat.setChildren(null); //force a reload?
//		}
	}
	
	
	@Override
	public void delete(Data inData, User inUser) {
		// TODO Auto-generated method stub
		super.delete(inData, inUser);
		getRootCategory().setProperty("dirty", true);

		getRootCategory().refresh();
	}
	
	
}
