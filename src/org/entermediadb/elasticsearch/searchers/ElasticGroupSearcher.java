/*
 * Created on Oct 19, 2004
 */
package org.entermedia.elasticsearch.searchers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermedia.locks.Lock;
import org.openedit.Data;
import org.openedit.data.PropertyDetails;
import org.openedit.users.GroupSearcher;

import com.openedit.OpenEditException;
import com.openedit.users.Group;
import com.openedit.users.User;
import com.openedit.users.filesystem.XmlUserArchive;

/**
 * @author cburkey
 * 
 */
public class ElasticGroupSearcher extends BaseElasticSearcher implements
		GroupSearcher
{
	private static final Log log = LogFactory.getLog(ElasticGroupSearcher.class);
	protected XmlUserArchive fieldXmlUserArchive;
	
	public XmlUserArchive getXmlUserArchive() {
		if (fieldXmlUserArchive == null) {
			fieldXmlUserArchive = (XmlUserArchive) getModuleManager().getBean(
					getCatalogId(), "xmlUserArchive");

		}

		return fieldXmlUserArchive;
	}
	
	
	public Object searchById(String inId) 
	{
		return getGroup(inId);
	}
	public void reIndexAll()
	{
		log.info("Reindex of customer groups directory");
		try
		{
			if( fieldConnected )
			{
				deleteOldMapping();
				putMappings();
			}
			Collection ids = getXmlUserArchive().listGroupIds();
			if( ids != null)
			{
				List groups = new ArrayList();
				PropertyDetails details = getPropertyDetailsArchive().getPropertyDetails(getSearchType());
				for (Iterator iterator = ids.iterator(); iterator.hasNext();)
				{
					String id = (String) iterator.next();
					Group group = getXmlUserArchive().getGroup(id);
					groups.add(group);
					if( groups.size() > 50)
					{
						updateIndex(groups, null);
					}
				}
				updateIndex(groups, null);
			}
		} 
		catch (Exception e)
		{
			throw new OpenEditException(e);
		}
	}
	public void restoreSettings()
	{
		getPropertyDetailsArchive().clearCustomSettings(getSearchType());
		reIndexAll();
	}

	public Group getGroup(String inGroupId)
	{
		Group group = getXmlUserArchive().getGroup(inGroupId);
		if (group == null)
		{
			log.error("Index is out of date, group " + inGroupId
					+ " has since been deleted");
		} 
		return group;
	}

	public Data createNewData()
	{
		return getXmlUserArchive().createGroup();
	}

	public void saveData(Data inData, User inUser)
	{
		getXmlUserArchive().saveGroup((Group) inData);
		//super.saveData(inData, inUser); //update the index
		PropertyDetails details = getPropertyDetailsArchive().getPropertyDetailsCached(getSearchType());
		updateElasticIndex(details, inData);	}

	public void delete(Data inData, User inUser)
	{
		getXmlUserArchive().deleteGroup((Group) inData);
		super.delete(inData, inUser); //update the index
	}

}
