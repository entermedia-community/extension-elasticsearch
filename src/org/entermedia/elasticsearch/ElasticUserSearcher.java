package org.entermedia.elasticsearch;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.openedit.Data;
import org.openedit.users.UserSearcher;

import com.openedit.OpenEditException;
import com.openedit.hittracker.HitTracker;
import com.openedit.hittracker.SearchQuery;
import com.openedit.page.manage.PageManager;
import com.openedit.users.BaseUser;
import com.openedit.users.Group;
import com.openedit.users.User;
import com.openedit.users.UserManager;

public class ElasticUserSearcher extends BaseElasticSearcher implements UserSearcher
{
	protected UserManager fieldUserManager;
	protected PageManager pageManager;

//	@Override
//	public Object searchById(String inId)
//	{
//		return searchByField("user-name",inId);
//	}

	public PageManager getPageManager()
	{
		return pageManager;
	}

	public void setPageManager(PageManager inPageManager)
	{
		pageManager = inPageManager;
	}

	public UserManager getUserManager()
	{
		return fieldUserManager;
	}

	public void setUserManager(UserManager inUserManager)
	{
		fieldUserManager = inUserManager;
	}

	@Override
	public void reIndexAll() throws OpenEditException
	{
		Collection users = getUserManager().getUsers();
		updateIndex(users, null);
	}

	public User getUser(String inAccount)
	{
		return getUserManager().getUser(inAccount);
	}

	public Data createNewData()
	{
		return new BaseUser();
	}

	public User getUserByEmail(String inEmail)
	{
		return (User) searchByField("email", inEmail);
	}

	//this may not be correct syntax
	public HitTracker getUsersInGroup(Group inGroup)
	{
		SearchQuery query = createSearchQuery();
		query.addMatches("groupid", inGroup.getId());
		query.setSortBy("namesorted");
		HitTracker tracker = search(query);
		return tracker;
	}

	@Override
	public void saveUsers(List inUserstosave, User inUser)
	{
		for (Iterator iterator = inUserstosave.iterator(); iterator.hasNext();)
		{
			User user = (User) iterator.next();
			getUserManager().saveUser(user);

		}
		updateIndex(inUserstosave, null);
	}

}
