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

import com.openedit.OpenEditException;
import com.openedit.users.User;

public class ElasticCategorySearcher extends BaseElasticSearcher 
{
	private static final Log log = LogFactory.getLog(ElasticCategorySearcher.class);

	public Data createNewData()
	{
		return new ElasticCategory(this);
	}
	protected Category refreshData(String inId, GetResponse response) 
	{
		ElasticCategory category = (ElasticCategory)createNewData();
		category.setProperties(response.getSource());
		
		return category;
	}

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
		//This is the one time we load up the categories from the XML file
	
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

	
}
