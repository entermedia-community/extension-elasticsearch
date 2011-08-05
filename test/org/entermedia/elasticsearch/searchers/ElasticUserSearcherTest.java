package org.entermedia.elasticsearch.searchers;

import java.util.Collection;

import org.junit.Test;
import org.openedit.Data;
import org.openedit.entermedia.BaseEnterMediaTest;

import com.openedit.hittracker.HitTracker;
import com.openedit.hittracker.SearchQuery;
import com.openedit.page.Page;
import com.openedit.page.manage.PageManager;
import com.openedit.users.BaseGroup;
import com.openedit.users.BaseUser;
import com.openedit.users.Group;
import com.openedit.users.User;

public class ElasticUserSearcherTest extends BaseEnterMediaTest
{
	@Test
	public void testVerifyConfiguration()
	{
		ElasticUserSearcher userSearcher = (ElasticUserSearcher) getMediaArchive().getSearcherManager().getSearcher("system", "user");
		assertNotNull("user searcher is NULL!", userSearcher);
		assertTrue("user searcher is elastic", userSearcher instanceof ElasticUserSearcher);
	}

	@Test
	public void testCreateNewData()
	{
		ElasticUserSearcher searcher = new ElasticUserSearcher();
		Data data = null;
		data = searcher.createNewData();
		assertNotNull("data is NULL!", data);
		
		BaseUser base = (BaseUser) data;
		assertTrue("not enabled!", base.isEnabled());
	}

	@Test
	public void testGetUsers() throws Exception
	{
//		Client client = new ClientPool().getClient();
//		Thread.sleep(5000);
//		client.close();
		
		ElasticUserSearcher userSearcher = (ElasticUserSearcher) getMediaArchive().getSearcherManager().getSearcher("system", "user");
		userSearcher.reIndexAll();
		
//		GetResponse response = userSearcher.getClient().prepareGet("entermedia_catalogs_testcatalog", "user", "admin")
//		        .execute()
//		        .actionGet();
//		
//		assertEquals("admin", response.getId());
//		
//		SearchQuery query = new SearchQuery();
//		query.setId("admin");
//		ElasticHitTracker hit = (ElasticHitTracker) userSearcher.search( query );
//		assertNotNull("hit is null", hit);
		
//		Object result = null;
//		result = hit.getById("admin");
//		assertNotNull("result is NULL", result); 
		
		User result = null;
		result = userSearcher.getUser("admin");
		assertNotNull("user is NULL, cannot find 'admin'!", result);
		
		SearchQuery q = userSearcher.createSearchQuery();
		q.addOrsGroup("id", "admin testuser");
		Collection col = userSearcher.search(q);
		assertTrue(col.size() > 0);
	}

	@Test
	public void testGetUserByEmail() throws Exception
	{
		ElasticUserSearcher userSearcher = (ElasticUserSearcher) getMediaArchive().getSearcherManager().getSearcher("system", "user");
		userSearcher.reIndexAll();
		
		Data hit = (Data) userSearcher.searchByField("screenname", "Admin");
		assertEquals("screen name ","Admin",hit.get("screenname") );
		assertNotNull("screen Name is null" , hit);

		 hit = (Data) userSearcher.searchByField("screenname", "ADMIN");
		assertNull("screen name is not null", hit);

		hit = (Data) userSearcher.searchByField("lastName", "Administrator");
		assertNotNull("Last Name is null" , hit);
		
		String email = "support.openedit";
		
//		GetResponse response = userSearcher.getClient().prepareGet("entermedia_catalogs_testcatalog", "user", "admin")
//		        .execute()
//		        .actionGet();
//		Map fields = response.getFields();
//		String result = (String) fields.get("email");
//		User user = userSearcher.getUserByEmail(email);
		//assertEquals(email,	result);
		hit = (Data) userSearcher.searchByField("email", email);
		assertNotNull(hit);
		
	}

	@Test
	public void testGetUsersInGroup()
	{
		ElasticUserSearcher userSearcher = (ElasticUserSearcher) getMediaArchive().getSearcherManager().getSearcher("system", "user");
		Group group = new BaseGroup();
		group.setId("administrators");
		HitTracker hit = userSearcher.getUsersInGroup(group);
		assertTrue("no results", hit.size()>0);
	}

	@Test
	public void testSaveUsers()
	{
		ElasticUserSearcher userSearcher = (ElasticUserSearcher) getMediaArchive().getSearcherManager().getSearcher("system", "user");
		
		BaseUser user = (BaseUser) userSearcher.createNewData();
		user.setId("1");
		user.setName("test");
		
		//List<User> users = new ArrayList<User>();
		//users.add(usr);
		
		//userSearcher.saveUsers(users, usr);
		User admin = userSearcher.getUser("admin");
		userSearcher.saveData(user, admin);
		
		PageManager pManager = userSearcher.getPageManager();
		Page page = pManager.getPage("WEB-INF/data/system/users/1.xml");
		assertTrue("user file not found!", page.exists());
		
		//do a search to verify the user is in the index
		User test = userSearcher.getUser("1");
		assertNotNull("user not in index", test);
		
		//now get rid of the user
		userSearcher.delete(user, admin);
		
		//make sure the user is gone from the index
		test = userSearcher.getUser("1");
		assertNull("user still in index", test);
	}

}
