package org.entermedia.elasticsearch.searchers;

import org.entermediadb.asset.Asset;
import org.entermediadb.asset.BaseEnterMediaTest;
import org.entermediadb.asset.EnterMedia;
import org.entermediadb.asset.search.BaseAssetSearcher;
import org.entermediadb.elasticsearch.searchers.ElasticAssetDataConnector;
import org.junit.Test;
import org.openedit.WebPageRequest;
import org.openedit.data.Searcher;
import org.openedit.hittracker.HitTracker;
import org.openedit.hittracker.SearchQuery;

public class ElasticAssetTest  extends BaseEnterMediaTest
{
	public void testVerifyConfiguration()
	{
		BaseAssetSearcher searcher = (BaseAssetSearcher)getMediaArchive().getSearcherManager().getSearcher("entermedia/catalogs/testcatalog", "asset");
		assertNotNull("asset searcher is NULL!", searcher);
		assertTrue( searcher.getDataConnector() instanceof ElasticAssetDataConnector );
	}

	@Test
	public void testCreateNewData()
	{
		Searcher searcher = getMediaArchive().getSearcherManager().getSearcher("entermedia/catalogs/testcatalog", "asset");
		Asset data = (Asset)searcher.createNewData();
		assertNotNull("data is NULL!", data);
		data.setName("newtestdata");
		data.setSourcePath("junk/testing");
		searcher.saveData(data, null);
		
		Asset one = (Asset)searcher.searchById(data.getId());
		assertNotNull(one);
		
		
	}

	public void testAssetLoad()
	{
		Searcher searcher = getMediaArchive().getSearcherManager().getSearcher("entermedia/catalogs/testcatalog", "asset");
		Asset one = (Asset)searcher.searchById("101");
		assertNotNull(one);
		assertNotNull(one.get("caption"));
	}
	public void testAssetSearch()
	{
		Searcher searcher = getMediaArchive().getSearcherManager().getSearcher("entermedia/catalogs/testcatalog", "asset");

		SearchQuery q = searcher.createSearchQuery();
		q.addMatches("caption","test101");
		WebPageRequest req = getFixture().createPageRequest("/entermedia/catalogs/testcatalog/index.html");
		HitTracker tracker = searcher.cachedSearch(req, q);
		assertTrue(tracker.size() > 0);

		q = searcher.createSearchQuery();
		q.addMatches("description","test101");

		tracker = searcher.cachedSearch(req, q);
		assertTrue(tracker.size() > 0);

		Asset one = (Asset)searcher.searchById("101");
		one.setProperty("owner", "admin");
		getMediaArchive().saveAsset(one,null);
		

		q = searcher.createSearchQuery();
		q.addMatches("description","test101");

		tracker = searcher.cachedSearch(req, q);
		assertTrue(tracker.size() > 0);

		
	}

	public EnterMedia getEnterMedia(String inApplicationId)
	{
		EnterMedia media = (EnterMedia)getStaticFixture().getModuleManager().getBean(inApplicationId, "enterMedia");
		media.setApplicationId(inApplicationId);
		return media;
	}
}
