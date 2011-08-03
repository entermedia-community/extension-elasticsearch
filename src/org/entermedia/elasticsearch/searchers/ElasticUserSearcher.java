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
import com.openedit.WebPageRequest;
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
	public HitTracker getAllHits(WebPageRequest inReq)
	{
		SearchQuery query = createSearchQuery();
		query.addMatches("enabled", "true");
		query.addMatches("enabled", "false");
		query.addSortBy("namesorted");
		query.setAndTogether(false);
		if( inReq == null)
		{
			return search(query);
		}
		else
		{
			return cachedSearch(inReq,query);
		}
		//return new ListHitTracker().setList(getCustomerArchive().)
	}

	public UserManager getUserManager()
	{
		return fieldUserManager;
	}

	public void setUserManager(UserManager inUserManager)
	{
		fieldUserManager = inUserManager;
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
	/*
	protected void updateIndex(Data inData, Document doc, PropertyDetails inDetails)
	{
		User user = (User)inData;
		doc.add(new Field("enabled", Boolean.toString(user.isEnabled()), Field.Store.YES, Field.Index.ANALYZED));
		StringBuffer groups = new StringBuffer();
		for (Iterator iterator = user.getGroups().iterator(); iterator.hasNext();)
		{
			Group group = (Group) iterator.next();
			groups.append(group.getId());
			groups.append(" ");
		}
		if( groups.length() > 0)
		{
			doc.add(new Field("groups", groups.toString(), Field.Store.NO, Field.Index.ANALYZED));
		}

		super.updateIndex(inData, doc, inDetails);
	}
	*/

	//TODO: Replace with search?
	public Object searchById(String inId)
	{
		return getUserManager().getUser(inId);
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
		return getUserManager().getUserByEmail(inEmail);
	}

	public HitTracker getUsersInGroup(Group inGroup)
	{
		SearchQuery query = createSearchQuery();
		if( inGroup == null)
		{
			throw new OpenEditException("No group found");
		}
		query.addMatches("group",inGroup.getId());
		query.setSortBy("namesorted");
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
		Lock lock = getLockManager().lock(getCatalogId(), "/WEB-INF/data/system/users/" + inData.getId() + ".xml","admin");
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
		Lock lock = getLockManager().lock(getCatalogId(), "/WEB-INF/data/system/users/" + inData.getId() + ".xml","admin");
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
					inContent.array("groups", groups);
				}
			}
		}
		catch ( Exception ex)
		{
			throw new OpenEditException(ex);	
		}
	}

	
	public void setCatalogId(String inCatalogId)
	{
		//This can be removed in the future once we track down a singleton bug
		if( inCatalogId != null && !inCatalogId.equals("system"))
		{
			OpenEditException ex = new OpenEditException("Invalid catalogid catalogid=" + inCatalogId );
			ex.printStackTrace();
			log.error( ex);
			
			//throw ex;
		}
		
		super.setCatalogId("system");
		if(fieldPropertyDetailsArchive != null){
		   fieldPropertyDetailsArchive.setCatalogId("system");
		   
		}
	}
	public PropertyDetailsArchive getPropertyDetailsArchive()
	{
		if (fieldPropertyDetailsArchive == null)
		{
			fieldPropertyDetailsArchive = (PropertyDetailsArchive) getSearcherManager().getModuleManager().getBean("system", "propertyDetailsArchive");
		}
		fieldPropertyDetailsArchive.setCatalogId("system");
		return fieldPropertyDetailsArchive;
	}
}
