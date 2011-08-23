package org.entermedia.elasticsearch.searchers;

import java.util.Collection;
import java.util.Date;

import org.openedit.Data;
import org.openedit.entermedia.BaseEnterMediaTest;
import org.openedit.util.DateStorageUtil;

import com.openedit.hittracker.SearchQuery;

public class ConvertTest extends BaseEnterMediaTest
{

	public void testCreateAndSearch() throws Exception
	{
		
		ElasticXmlFileSearcher convertSearcher = (ElasticXmlFileSearcher) getMediaArchive().getSearcherManager().getSearcher("entermedia/catalogs/testcatalog", "conversiontask");

		Data newone = convertSearcher.createNewData();
		newone.setProperty("submitted", DateStorageUtil.getStorageUtil().formatForStorage(new Date()));
		newone.setProperty("status","new");
		newone.setSourcePath("testing/new");
		convertSearcher.saveData(newone,null);
		
				
		Data found = (Data)convertSearcher.searchById(newone.getId());
		assertNotNull(found);

		
		SearchQuery q = convertSearcher.createSearchQuery();
		q.addMatches("_id",found.getId());
		Collection hits  = convertSearcher.search(q);
		assertTrue(hits.size() > 0);

	}
}
