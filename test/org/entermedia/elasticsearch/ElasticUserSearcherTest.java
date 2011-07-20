package org.entermedia.elasticsearch;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.junit.Test;
import org.openedit.Data;
import org.openedit.data.Searcher;
import org.openedit.entermedia.BaseEnterMediaTest;

import com.openedit.hittracker.HitTracker;
import com.openedit.page.Page;
import com.openedit.page.manage.PageManager;
import com.openedit.users.BaseGroup;
import com.openedit.users.BaseUser;
import com.openedit.users.Group;
import com.openedit.users.User;
import com.openedit.users.UserManager;

public class ElasticUserSearcherTest extends BaseEnterMediaTest
{
	@Test
	public void testVerifyConfiguration()
	{
		Searcher userSearcher = getMediaArchive().getSearcherManager().getSearcher("entermedia/catalogs/testcatalog", "user");
		assertNotNull("user searcher is NULL!", userSearcher);
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
	public void testGetUser() throws Exception
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
		
	}

	@Test
	public void testGetUserByEmail() throws Exception
	{
		ElasticUserSearcher userSearcher = (ElasticUserSearcher) getMediaArchive().getSearcherManager().getSearcher("system", "user");
		userSearcher.reIndexAll();
		
		Data hit = (Data) userSearcher.searchByField("screenname", "admin");
		assertEquals("screen name ","Admin",hit.get("screenname") );
		assertNotNull("screen Name is null" , hit);

		 hit = (Data) userSearcher.searchByField("screenname", "ADMIN");
			assertNotNull("screen Name is null" , hit);

		hit = (Data) userSearcher.searchByField("lastName", "administrator");
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
		group.setId("photoeditors");
		HitTracker hit = userSearcher.getUsersInGroup(group);
		assertTrue("no results", hit.size()>0);
	}

	@Test
	public void testSaveUsers()
	{
		ElasticUserSearcher userSearcher = (ElasticUserSearcher) getMediaArchive().getSearcherManager().getSearcher("entermedia/catalogs/testcatalog", "user");
		BaseUser user = (BaseUser) userSearcher.createNewData();
		user.setId("1");
		
		UserManager mgr = getFixture().getUserManager();
		User usr = mgr.createUser("don5", null);
//		mgr.saveUser(usr);
		
		List<User> users = new ArrayList<User>();
		users.add(usr);
		
		userSearcher.saveUsers(users, usr);
		
		PageManager pManager = userSearcher.getPageManager();
		Page page = pManager.getPage("WEB-INF/data/system/users/don5.xml");
		assertTrue("user file not found!", page.exists());
		
		pManager.removePage(page);
	}

}
