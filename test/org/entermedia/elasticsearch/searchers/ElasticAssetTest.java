package org.entermedia.elasticsearch;

import net.sourceforge.jtds.jdbc.cache.SQLCacheKey;

import org.entermedia.elasticsearch.searchers.ElasticAssetDataConnector;
import org.junit.Test;
import org.openedit.data.Searcher;
import org.openedit.entermedia.Asset;
import org.openedit.entermedia.BaseEnterMediaTest;
import org.openedit.entermedia.search.BaseAssetSearcher;

import com.openedit.WebPageRequest;
import com.openedit.hittracker.HitTracker;
import com.openedit.hittracker.SearchQuery;

public class ElasticAssetTest  extends BaseEnterMediaTest
{
	public void testVerifyConfiguration()
	{
		BaseAssetSearcher searcher = (BaseAssetSearcher)getMediaArchive().getSearcherManager().getSearcher("entermedia/catalogs/testcatalog", "user");
		assertNotNull("asset searcher is NULL!", searcher);
		assertTrue( searcher.getDataConnector() instanceof ElasticAssetDataConnector );
	}

	@Test
	public void testCreateNewData()
	{
		Searcher searcher = getMediaArchive().getSearcherManager().getSearcher("entermedia/catalogs/testcatalog", "asset");
		Asset data = (Asset)searcher.createNewData();
		assertNotNull("data is NULL!", data);
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
		
	}
	
}
