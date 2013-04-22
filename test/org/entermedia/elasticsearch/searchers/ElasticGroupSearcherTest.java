package org.entermedia.elasticsearch.searchers;

import org.entermedia.elasticsearch.BaseElasticTest;
import org.junit.Test;
import org.openedit.Data;

import com.openedit.hittracker.HitTracker;
import com.openedit.hittracker.SearchQuery;
import com.openedit.users.Group;
import com.openedit.users.UserManager;

public class ElasticGroupSearcherTest extends BaseElasticTest
{

	@Test
	public void testVerifyConfiguration()
	{
		ElasticGroupSearcher groupSearcher = (ElasticGroupSearcher) getMediaArchive().getSearcherManager().getSearcher("system", "group");
		assertNotNull("group searcher is NULL!", groupSearcher);
	}

	@Test
	public void testSearchById()
	{
		ElasticGroupSearcher groupSearcher = (ElasticGroupSearcher) getMediaArchive().getSearcherManager().getSearcher("system", "group");
		groupSearcher.reIndexAll();
		Object groupObj = groupSearcher.searchById("administrators");
		assertNotNull("Group is NULL", groupObj);

		Group group = (Group) groupObj;
		assertEquals("administrators", group.getId());
	}

	@Test
	public void testSaveDataObjectUser()
	{
		ElasticGroupSearcher groupSearcher = (ElasticGroupSearcher) getMediaArchive().getSearcherManager().getSearcher("system", "group");
		groupSearcher.reIndexAll();
		Data testgroup = groupSearcher.createNewData();
		testgroup.setName("Testing");
		testgroup.setId("testid");
		groupSearcher.saveData(testgroup, null);
		Group group = groupSearcher.getGroup("testid");
		assertNotNull("group is NULL", group);
	}

	@Test
	public void testGetUserManager()
	{
		ElasticGroupSearcher groupSearcher = (ElasticGroupSearcher) getMediaArchive().getSearcherManager().getSearcher("system", "group");
		UserManager mgr = null;
		mgr = groupSearcher.getUserManager();
		assertNotNull("UserManager is NULL", mgr);
	}

	@Test
	public void testReIndexAll()
	{
		ElasticGroupSearcher groupSearcher = (ElasticGroupSearcher) getMediaArchive().getSearcherManager().getSearcher("system", "group");
		groupSearcher.reIndexAll();
	}

	@Test
	public void testGetGroup()
	{
		ElasticGroupSearcher groupSearcher = (ElasticGroupSearcher) getMediaArchive().getSearcherManager().getSearcher("system", "group");
		Group group = groupSearcher.getGroup("users");
		assertNotNull("NULL group", group);
		
		SearchQuery q = groupSearcher.createSearchQuery();
		q.addStartsWith("description","tes");
	//	q.addMatches("description","tes");

		HitTracker tracker = groupSearcher.search(q);
		assertTrue(tracker.size() > 0);
		
		q = groupSearcher.createSearchQuery();
		q.addStartsWith("description","Tes");

		tracker = groupSearcher.search(q);
		assertTrue(tracker.size() > 0);

		
		q = groupSearcher.createSearchQuery();
		q.addMatches("description","NOTinDB");

		tracker = groupSearcher.search(q);
		assertTrue(tracker.size() == 0);

	}

	
}
