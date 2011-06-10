package org.entermedia.elasticsearch;

import org.openedit.Data;
import org.openedit.entermedia.BaseEnterMediaTest;

import com.openedit.hittracker.HitTracker;
import com.openedit.hittracker.SearchQuery;


public class SearcherTest extends BaseEnterMediaTest
{
	public void testSearcher()
	{
		ElasticXmlFileSearcher searcher = (ElasticXmlFileSearcher)getMediaArchive().getSearcherManager().getSearcher("entermedia/catalogs/testcatalog", "asset" );

		Data asset = searcher.createNewData();
		asset.setName("test");
		asset.setId("102");
		asset.setSourcePath("nytrip/june");
		searcher.saveData(asset, null);
		
		SearchQuery q = searcher.createSearchQuery();
		q.addExact("name", "test");
		HitTracker tracker = searcher.search(q);
		assertTrue(tracker.size() > 0);
	}
}
