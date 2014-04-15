package org.entermedia.elasticsearch.searchers;

import java.util.Date;

import org.openedit.Data;
import org.openedit.entermedia.autocomplete.AutoCompleteSearcher;
import org.openedit.util.DateStorageUtil;

import com.openedit.OpenEditException;
import com.openedit.WebPageRequest;
import com.openedit.hittracker.HitTracker;
import com.openedit.hittracker.SearchQuery;
import com.openedit.hittracker.Term;

public class ElasticAutoCompleteSearcher extends BaseElasticSearcher implements AutoCompleteSearcher
{

	
//	@Override
//	public HitTracker cachedSearch(WebPageRequest inPageRequest, SearchQuery inQuery) throws OpenEditException
//	{
//		//TODO: This is hackish, change to more generic paramters
//		Term term = inQuery.getTermByDetailId("synonymsenc");
//		term.setId("id");
//		return super.cachedSearch(inPageRequest, inQuery);
//	}
	
	@Override
	public void reIndexAll() throws OpenEditException
	{
	}

	/**
	 * TODO: Sort by timestamp, clean out old ones when they are deleted, stemming
	 */
	@Override
	public void updateHits(HitTracker inTracker, String inWord)
	{
		if( inTracker.size() > 0)
		{
			Data word = createNewData();
			//word.setId(inWord);
			word.setProperty("synonyms",inWord );
			word.setProperty("hitcount", String.valueOf( inTracker.size() ) );
			word.setProperty("timestamp", DateStorageUtil.getStorageUtil().formatForStorage(new Date()) );
			word.setProperty("synonymsenc",inWord ); //This can be removed if you change the search term to match
			
			saveData(word, null);
		}	
	}

}
