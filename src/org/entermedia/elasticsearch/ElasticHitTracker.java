package org.entermedia.elasticsearch;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.search.SearchHit;
import org.openedit.Data;

import com.openedit.OpenEditException;
import com.openedit.hittracker.HitTracker;

public class ElasticHitTracker extends HitTracker
{
	private static final Log log = LogFactory.getLog(ElasticHitTracker.class);

	protected SearchRequestBuilder fieldSearcheRequestBuilder;
	protected Map fieldChunks;
	protected int fieldHitsPerChunk = 40;
	public SearchRequestBuilder getSearcheRequestBuilder()
	{
		return fieldSearcheRequestBuilder;
	}
	public void setSearcheRequestBuilder(SearchRequestBuilder inSearcheRequestBuilder)
	{
		fieldSearcheRequestBuilder = inSearcheRequestBuilder;
	}

	
	public ElasticHitTracker()
	{
		
	}
	public ElasticHitTracker(SearchRequestBuilder	builder)
	{
		setSearcheRequestBuilder(builder);
	}
	public SearchResponse getSearchResponse(int inChunk)
	{
		Integer chunk = Integer.valueOf(inChunk);
		SearchResponse response = (SearchResponse)getChunks().get(chunk);
		if( response == null)
		{
			int start = (inChunk) * fieldHitsPerChunk;
			int end = (inChunk+1) * fieldHitsPerChunk;
			
			getSearcheRequestBuilder().setFrom(start).setSize(end).setExplain(false);
			response = getSearcheRequestBuilder().execute().actionGet();
			if( getChunks().size() > 30)
			{
				SearchResponse first = getChunks().get(0);
				getChunks().clear();
				getChunks().put(0,first);
				log.info("Reloaded chunks " + start + " to " + end);
			}
			getChunks().put(chunk,response);
		}
		return response;
	}

	protected Map<Integer,SearchResponse> getChunks()
	{
		if (fieldChunks == null)
		{
			fieldChunks = new HashMap();
		}
		return fieldChunks;
	}

	@Override
	public Data get(int inCount)
	{
		//Get the relative location based on the page we are on
		
		//ie 50 / 40 = 1
		int chunk = inCount / fieldHitsPerChunk;
		
		// 50 - (1 * 40) = 10 relative
		int indexlocation = inCount - ( chunk * fieldHitsPerChunk );

		//get the chunk 1
		SearchHit[] hits = getSearchResponse(chunk).getHits().getHits();
		if( indexlocation >= hits.length)
		{
			//we dont support getting results beyond what we have loaded. 
			//User should call setPage(page+1) first
			throw new OpenEditException("row request falls beyond one page of results");
		}
		SearchHit hit = hits[indexlocation];
		SearchHitData data = new SearchHitData(hit);
		return data;
	}
	
	@Override
	public Iterator iterator()
	{
		return new ElasticHitIterator(this);
	}

	@Override
	public boolean contains(Object inHit)
	{
		throw new OpenEditException("Not implemented");
	}

	public int size()
	{
		return (int)getSearchResponse(0).getHits().getTotalHits();
	}


}
