package org.entermedia.elasticsearch;

import java.util.Iterator;

import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.search.SearchHit;
import org.openedit.Data;

import com.openedit.OpenEditException;
import com.openedit.hittracker.HitTracker;

public class ElasticHitTracker extends HitTracker
{
	protected SearchResponse fieldSearchResponse;
	
	public ElasticHitTracker()
	{
		
	}
	public ElasticHitTracker(SearchResponse response)
	{
		setSearchResponse(response);
	}
	public SearchResponse getSearchResponse()
	{
		return fieldSearchResponse;
	}

	public void setSearchResponse(SearchResponse inSearchResponse)
	{
		fieldSearchResponse = inSearchResponse;
	}

	@Override
	public Data get(int inCount)
	{
		SearchHit hit = getSearchResponse().getHits().getHits()[inCount];
		SearchHitData data = new SearchHitData(hit);
		return data;
	}

	@Override
	public Iterator iterator()
	{
		return new ElasticHitIterator(getSearchResponse().getHits().iterator());
	}

	@Override
	public boolean contains(Object inHit)
	{
		throw new OpenEditException("Not implemented");
	}

	public int size()
	{
		return (int)getSearchResponse().getHits().getTotalHits();
	}


}
