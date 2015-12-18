package org.entermedia.elasticsearch.searchers;

import org.entermedia.elasticsearch.ElasticNodeManager;
import org.junit.Test;
import org.openedit.entermedia.BaseEnterMediaTest;

public class KnapsackTest extends BaseEnterMediaTest
{
	@Test
	public void testSearchAssetSuggest()
	{
		BaseElasticSearcher searcher = (BaseElasticSearcher)getMediaArchive().getSearcherManager().getSearcher("entermedia/catalogs/testcatalog", "states" );
		ElasticNodeManager manager = searcher.getElasticNodeManager();
		manager.exportKnapsack("entermedia/catalogs/testcatalog");
	}

}
