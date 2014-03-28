package org.entermedia.elasticsearch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.FilterBuilder;
import org.elasticsearch.index.query.IdsQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.facet.terms.TermsFacet;
import org.elasticsearch.search.facet.terms.TermsFacet.Entry;
import org.openedit.Data;
import org.openedit.data.PropertyDetail;

import com.openedit.OpenEditException;
import com.openedit.hittracker.FilterNode;
import com.openedit.hittracker.HitTracker;

public class ElasticHitTracker extends HitTracker
{
	private static final Log log = LogFactory.getLog(ElasticHitTracker.class);

	protected SearchRequestBuilder fieldSearcheRequestBuilder;
	protected FilterBuilder fieldFilterBuilder;

	protected Map fieldChunks;
	public FilterBuilder getFilterBuilder()
	{
		return fieldFilterBuilder;
	}

	public void setFilterBuilder(FilterBuilder inFilterBuilder)
	{
		fieldFilterBuilder = inFilterBuilder;
	}

	protected int fieldHitsPerChunk = 40;
	protected QueryBuilder terms;
	protected List fieldFilterOptions;

	public QueryBuilder getTerms()
	{
		return terms;
	}

	public void setTerms(QueryBuilder inTerms)
	{
		terms = inTerms;
	}

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

	public ElasticHitTracker(SearchRequestBuilder builder, QueryBuilder inTerms)
	{
		setTerms(inTerms);
		;
		setSearcheRequestBuilder(builder);
	}

	public void setShowOnlySelected(boolean inShowOnlySelected)
	{
		fieldShowOnlySelected = inShowOnlySelected;

		if (isShowOnlySelected() && fieldSelections != null && fieldSelections.size() > 0)
		{

			BoolQueryBuilder bool = QueryBuilders.boolQuery();
			bool.must(getTerms());
			String[] fieldSelected = fieldSelections.toArray(new String[fieldSelections.size()]);
			IdsQueryBuilder b = QueryBuilders.idsQuery();
			b.addIds(fieldSelected);
			bool.must(b);
			getSearcheRequestBuilder().setQuery(bool);
		}

		else
		{

			getSearcheRequestBuilder().setQuery(getTerms());

		}
		getChunks().clear();
	}

	public SearchResponse getSearchResponse(int inChunk)
	{
		Integer chunk = Integer.valueOf(inChunk);
		SearchResponse response = (SearchResponse) getChunks().get(chunk);
		if (response == null)
		{
			int start = (inChunk) * fieldHitsPerChunk;
			int end = (inChunk + 1) * fieldHitsPerChunk;

			getSearcheRequestBuilder().setFrom(start).setSize(end).setExplain(false);
			if(getFilterBuilder() != null){
				getSearcheRequestBuilder().setPostFilter(getFilterBuilder());
			}
			response = getSearcheRequestBuilder().execute().actionGet();
			if (response.getFacets() != null && fieldFilterOptions == null)
			{
				Map facets = response.getFacets().facetsAsMap();
				if (facets != null)
				{
					loadFacets(facets);
				}
			}
			if (getChunks().size() > 30)
			{
				SearchResponse first = getChunks().get(0);
				getChunks().clear();
				getChunks().put(0, first);
				log.info("Reloaded chunks " + start + " to " + end);
			}
			getChunks().put(chunk, response);
		}
		return response;
	}

	private void loadFacets(Map inFacets)
	{
		ArrayList facets = new ArrayList();
		for (Iterator iterator = inFacets.keySet().iterator(); iterator.hasNext();)
		{
			String key = (String) iterator.next();
			TermsFacet f = (TermsFacet) inFacets.get(key);
			if (f.getEntries().size() > 0)
			{
				FilterNode parent = new FilterNode();
				parent.setId(f.getName());
				parent.setName(f.getName());
				PropertyDetail detail = getSearcher().getDetail(f.getName());
				if (detail != null)
				{
					parent.setName(detail.getText());
				}

				for (Iterator iterator2 = f.getEntries().iterator(); iterator2.hasNext();)
				{
					Entry entry = (Entry) iterator2.next();
					int count = entry.getCount();
					String term = entry.getTerm().string();
					FilterNode child = new FilterNode();
					child.setId(term);
					if (detail.isList())
					{
						Data data = getSearcher().getSearcherManager().getData(getCatalogId(), detail.getListId(), term);
						if(data != null){
						child.setName(data.getName());
						} else{
							child.setName(term);
						}
					}
					else
					{
						child.setName(term);
					}

					child.setProperty("count", String.valueOf(entry.getCount()));
					parent.addChild(child);
				}

				facets.add(parent);
			}
		}
		setFilterOptions(facets);
		log.info("found some facets");

	}

	protected Map<Integer, SearchResponse> getChunks()
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
		// Get the relative location based on the page we are on

		// ie 50 / 40 = 1
		int chunk = inCount / fieldHitsPerChunk;

		// 50 - (1 * 40) = 10 relative
		int indexlocation = inCount - (chunk * fieldHitsPerChunk);

		// get the chunk 1
		SearchHit[] hits = getSearchResponse(chunk).getHits().getHits();
		if (indexlocation >= hits.length)
		{
			// we dont support getting results beyond what we have loaded.
			// User should call setPage(page+1) first
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

		if (!isAllSelected() && isShowOnlySelected() && (fieldSelections == null || fieldSelections.size() == 0))
		{
			return 0;
		}

		return (int) getSearchResponse(0).getHits().getTotalHits();
	}

	@Override
	public List<FilterNode> getFilterOptions()
	{
		return fieldFilterOptions;
	}

	public void setFilterOptions(List inFilterOptions)
	{
		fieldFilterOptions = inFilterOptions;
	}

}
