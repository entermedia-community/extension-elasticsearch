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
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.entermedia.locks.Lock;
import org.openedit.Data;
import org.openedit.data.PropertyDetails;
import org.openedit.data.PropertyDetailsArchive;
import org.openedit.users.UserSearcher;

import com.openedit.OpenEditException;
import com.openedit.hittracker.HitTracker;
import com.openedit.hittracker.SearchQuery;
import com.openedit.users.BaseUser;
import com.openedit.users.Group;
import com.openedit.users.User;
import com.openedit.users.UserManager;

/**
 *
 */
public class ElasticUserSearcher extends BaseElasticSearcher implements UserSearcher
{
	private static final Log log = LogFactory.getLog(ElasticUserSearcher.class);
	protected UserManager fieldUserManager;

	@Override
	public Data createNewData()
	{
		BaseUser user = new BaseUser();
		return user;
	}

	
	public UserManager getUserManager() {
		if (fieldUserManager == null) {
			fieldUserManager = (UserManager) getModuleManager().getBean(
					getCatalogId(), "userManager");

		}

		return fieldUserManager;
	}
	
	public void reIndexAll() throws OpenEditException
	{
		log.info("Reindex of customer users directory");
		try
		{
			PropertyDetails details = getPropertyDetailsArchive().getPropertyDetails(getSearchType());
			Collection usernames = getUserManager().listUserNames();
			if( usernames != null)
			{
				deleteAll(null);
				List users = new ArrayList();
				for (Iterator iterator = usernames.iterator(); iterator.hasNext();)
				{
					String userid = (String) iterator.next();
					User data = getUserManager().getUser(userid);
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

	//TODO: Replace with in-memory copy for performance reasons?
	public Object searchById(String inId)
	{
		
//		Lock lock = getLockManager().lock(getCatalogId(), "/WEB-INF/data/system/users/" + inId + ".xml","admin");
//		try
//		{
			return getUserManager().loadUser(inId);
//		}
//		finally
//		{
//			getLockManager().release(getCatalogId(), lock);
//		}

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
		User user =  getUserManager().getUserByEmail(inEmail);
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
		Lock lock = getLockManager().lock(getCatalogId(), "/WEB-INF/data/" + getCatalogId() + "/users/" + inData.getId() + ".xml","admin");
		try
		{
			getUserManager().saveUser((User)inData);
			super.saveData(inData, inUser); //update the index
		}
		finally
		{
			getLockManager().release(getCatalogId(), lock);
		}

	}
	
	public void delete(Data inData, User inUser)
	{
		Lock lock = getLockManager().lock(getCatalogId(), "/WEB-INF/data/" + getCatalogId() + "/users/" + inData.getId() + ".xml","admin");
		try
		{
			getUserManager().deleteUser((User)inData);
			super.delete(inData, inUser); //delete the index
		}
		finally
		{
			getLockManager().release(getCatalogId(), lock);
		}
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
