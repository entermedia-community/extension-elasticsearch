package org.entermedia.elasticsearch;

import java.util.Date;

import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.action.index.IndexRequestBuilder;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.index.query.xcontent.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.openedit.Data;
import org.openedit.data.Searcher;
import org.openedit.entermedia.Asset;
import org.openedit.entermedia.BaseEnterMediaTest;
import org.openedit.entermedia.search.BaseAssetSearcher;

import com.openedit.hittracker.HitTracker;
import com.openedit.hittracker.SearchQuery;


public class SearcherTest extends BaseEnterMediaTest
{

public void xxtestBasicRead() throws Exception
{
		// on startup
		//Node node = NodeBuilder.nodeBuilder().client(true).node();
	
	Settings.Builder settings = ImmutableSettings.settingsBuilder().put("cluster.name", "entermedia");
	Client client = new TransportClient(settings)
	        .addTransportAddress(new InetSocketTransportAddress("localhost", 9300));

//	Client client = node.client();
	
	//	AdminClient admin = client.admin();
	//	ActionFuture<CreateIndexResponse> indexresponse = admin.indices().create(new CreateIndexRequest("test"));
	//	log(indexresponse.isDone() + " done ");
		
		IndexRequestBuilder builder = client.prepareIndex("media_catalogs_video", "asset", "102");
		IndexResponse response = builder.setSource(XContentFactory.jsonBuilder()
	                .startObject()
	                    .field("user", "simon2")
	                    .field("postDate", new Date())
	                    .field("message", "saved to database")
	                .endObject()
	              )
	    .setRefresh(true)
	    .execute()
	    .actionGet();
		// on shutdown
		SearchResponse results = client.prepareSearch("media_catalogs_video")
		        .setSearchType(SearchType.DFS_QUERY_THEN_FETCH)
		        .setQuery(QueryBuilders.termQuery("user", "simon2"))
		        .setFrom(0).setSize(60).setExplain(true)
//		        .addField("_all")
		        .execute()
		        .actionGet();
		SearchHit hit = results.getHits().iterator().next();
		
		//client.g
		Object message = hit.getSource().get("message");
		assertEquals("saved to database",String.valueOf(message) );
		client.close();
	}
	
	public void testAssetConnectorSearcher()
	{
		BaseAssetSearcher searcher = (BaseAssetSearcher)getMediaArchive().getSearcherManager().getSearcher("entermedia/catalogs/testcatalog", "asset" );
		Asset asset = (Asset)searcher.createNewData();
		asset.setId("99");
		asset.setProperty("caption", "tester1");
		asset.setName("aname");
		asset.addCategory(getMediaArchive().getCategory("index"));
		asset.setSourcePath("users/aname");
		searcher.saveData(asset, null);
		
		Asset found = (Asset)searcher.searchById("99");
		assertNotNull(found);
		
		SearchQuery q = searcher.createSearchQuery();
		q.addMatches("caption","tester1");
		q.addOrsGroup("category","none other hope index");
		
		HitTracker tracker = searcher.search(q);
		assertEquals( 1, tracker.size() );
		
		Searcher approvalsearch = getMediaArchive().getSearcherManager().getSearcher("entermedia/catalogs/testcatalog", "approvals");
		Data approval = approvalsearch.createNewData();
		approval.setId("123");
		approval.setProperty("notes", "A note");
		approval.setProperty("assetid", "99");
		approval.setSourcePath(found.getSourcePath());
		approvalsearch.saveData(approval, null);
		
	}
	
	
	public void XXtestSearcher()
	{
		ElasticXmlFileSearcher searcher = (ElasticXmlFileSearcher)getMediaArchive().getSearcherManager().getSearcher("entermedia/catalogs/testcatalog", "states" );

		Data asset = searcher.createNewData();
		asset.setName("Bermuda");
		asset.setId("102");
		asset.setSourcePath("states/102");
		searcher.saveData(asset, null);
		
		SearchQuery q = searcher.createSearchQuery();
		q.addExact("name", "Bermuda");
		HitTracker tracker = searcher.search(q);
		assertTrue(tracker.size() > 0);
	}
	
	
}
