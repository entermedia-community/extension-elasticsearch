/*
 * Created on Oct 19, 2004
 */
package org.entermediadb.elasticsearch.searchers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dom4j.tree.BaseElement;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.openedit.Data;
import org.openedit.OpenEditException;
import org.openedit.data.PropertyDetails;
import org.openedit.hittracker.HitTracker;
import org.openedit.hittracker.SearchQuery;
import org.openedit.locks.Lock;
import org.openedit.users.BaseUser;
import org.openedit.users.Group;
import org.openedit.users.User;
import org.openedit.users.UserSearcher;
import org.openedit.users.filesystem.XmlUserArchive;

/**
 *
 */
public class ElasticUserSearcher extends BaseElasticSearcher implements UserSearcher
{
	private static final Log log = LogFactory.getLog(ElasticUserSearcher.class);
	protected XmlUserArchive fieldXmlUserArchive;

	@Override
	public Data createNewData()
	{
		BaseUser user = new BaseUser();
		return user;
	}

	
	public XmlUserArchive getXmlUserArchive() {
		if (fieldXmlUserArchive == null) {
			fieldXmlUserArchive = (XmlUserArchive) getModuleManager().getBean(
					getCatalogId(), "xmlUserArchive");

		}

		return fieldXmlUserArchive;
	}
	
	public void reIndexAll() throws OpenEditException
	{
		log.info("Reindex of customer users directory");
		try
		{
			if( fieldConnected )
			{
				deleteOldMapping();
				putMappings();

			}
			PropertyDetails details = getPropertyDetailsArchive().getPropertyDetails(getSearchType());
			Collection usernames = getXmlUserArchive().listUserNames();
			if( usernames != null)
			{
				List users = new ArrayList();
				for (Iterator iterator = usernames.iterator(); iterator.hasNext();)
				{
					String userid = (String) iterator.next();
					User data = getXmlUserArchive().getUser(userid);
					users.add(data);
					if( users.size() > 50)
					{
						updateIndex(users, null);
					}
				}	
				updateIndex(users, null);
			}
		}
		catch (Exception e)
		{
			// TODO Auto-generated catch block
			throw new OpenEditException(e);
		}

	}
	public void restoreSettings()
	{
		getPropertyDetailsArchive().clearCustomSettings(getSearchType());
		reIndexAll();
	}

	
	//TODO: Replace with in-memory copy for performance reasons?
	public Object searchById(String inId)
	{		
		return getXmlUserArchive().loadUser(inId);
	}

	/* (non-Javadoc)
	 * @see org.openedit.users.UserSearcherI#getUser(java.lang.String)
	 */
	public User getUser(String inAccount)
	{
		User user = (User)searchById(inAccount);
		return user;
	}

	/**
	 * @deprecate use standard field search API
	 */
	public User getUserByEmail(String inEmail)
	{
		User user =  getXmlUserArchive().getUserByEmail(inEmail);
		if( user != null)
		{
			return getUser(user.getId());
		}
		return null;
	}

	public HitTracker getUsersInGroup(Group inGroup)
	{
		SearchQuery query = createSearchQuery();
		if( inGroup == null)
		{
			throw new OpenEditException("No group found");
		}
		query.addMatches("group",inGroup.getId());
		//query.setSortBy("idsorted");
		HitTracker tracker = search(query);
		return tracker;
	}

	public void saveUsers(List userstosave, User inUser) 
	{
		for (Iterator iterator = userstosave.iterator(); iterator.hasNext();) {
			User user = (User) iterator.next();
			saveData(user, inUser);
		}
	}

	public void saveData(Data inData, User inUser)
	{
		getXmlUserArchive().saveUser((User)inData);
		super.saveData(inData, inUser); //update the index
	}
	
	public void delete(Data inData, User inUser)
	{
		getXmlUserArchive().deleteUser((User)inData);
		super.delete(inData, inUser); //delete the index
	}
	
	protected void updateIndex(XContentBuilder inContent, Data inData, PropertyDetails inDetails)
	{
		super.updateIndex(inContent, inData, inDetails);
		User user = (User)inData;
		try
		{
			inContent.field("enabled", user.isEnabled() );
			if( user.getGroups().size() > 0)
			{
				String[] groups = new String[user.getGroups().size()];
				int i = 0;
				for (Iterator iterator = user.getGroups().iterator(); iterator.hasNext();)
				{
					Group group = (Group) iterator.next();
					groups[i++] = group.getId();
					inContent.array("group", groups);
				}
			}
		}
		catch ( Exception ex)
		{
			throw new OpenEditException(ex);	
		}
	}

	
	

}
