package org.entermedia.elasticsearch.searchers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermedia.locks.Lock;
import org.openedit.Data;
import org.openedit.data.DataArchive;
import org.openedit.data.PropertyDetails;
import org.openedit.data.PropertyDetailsArchive;
import org.openedit.xml.ElementData;
import org.openedit.xml.XmlFile;
import org.openedit.xml.XmlSearcher;

import com.openedit.OpenEditException;
import com.openedit.hittracker.HitTracker;
import com.openedit.page.manage.PageManager;
import com.openedit.users.User;

public class ElasticListSearcher extends BaseElasticSearcher
{
	protected Log log = LogFactory.getLog(ElasticListSearcher.class);
	protected DataArchive fieldDataArchive; //lazy loaded
	protected String fieldPrefix;
	protected String fieldDataFileName;
	protected XmlFile fieldXmlFile;
	protected XmlSearcher fieldXmlSearcher;
	
	
	
	public XmlSearcher getXmlSearcher() {
		
		if(fieldXmlSearcher.getCatalogId() == null){
			fieldXmlSearcher.setCatalogId(getCatalogId());
			fieldXmlSearcher.setSearchType(getSearchType());
			PropertyDetailsArchive newarchive = getSearcherManager().getPropertyDetailsArchive(getCatalogId());
			fieldXmlSearcher.setPropertyDetailsArchive(newarchive);
		}
		return fieldXmlSearcher;
	}

	public void setXmlSearcher(XmlSearcher inXmlSearcher) {
		fieldXmlSearcher = inXmlSearcher;
	}

	public PageManager getPageManager()
	{
		return fieldPageManager;
	}

	public void setPageManager(PageManager pageManager)
	{
		fieldPageManager = pageManager;
	}

	
	
	public String getDataFileName()
	{
		if (fieldDataFileName == null)
		{
			fieldDataFileName = getSearchType() + ".xml";
		}
		return fieldDataFileName;
	}
	public void setDataFileName(String inName)
	{
		fieldDataFileName = inName;
	}
	
	
	public Data createNewData()
	{
		if( fieldNewDataName == null)
		{
			ElementData data = new ElementData();
			
			return data;
		}
		return (Data)getModuleManager().getBean(getCatalogId(), getNewDataName());
	}


	public void reIndexAll() throws OpenEditException
	{		
		if( isReIndexing())
		{
			return;
		}
		setReIndexing(true);
		try
		{
			//For now just add things to the index. It never deletes
			deleteAll(null); //This only deleted the index
			
			 
			
			HitTracker settings = getXmlSearcher().getAllHits();
			
				for (Iterator iterator = settings.iterator(); iterator.hasNext();) 
				{
					Data data = (Data)iterator.next();					
					Collection toindex = new ArrayList();
					toindex.add(data);
					updateIndex(toindex,null);		
				}
		
			
			flushChanges();			
		}
		finally
		{
			setReIndexing(false);
		}
	}

	
	

	public void delete(Data inData, User inUser)
	{
		if( inData == null || inData.getSourcePath() == null || inData.getId() == null )
		{
			throw new OpenEditException("Cannot delete null data.");
		}
		Lock lock = getLockManager().lock(getCatalogId(), getSearchType() + "/" + inData.getSourcePath(),"admin");
		try
		{
			getXmlSearcher().delete(inData, inUser);
			super.delete(inData, inUser);
		}
		finally
		{
			getLockManager().release(getCatalogId(), lock);
		}
		// Remove from Index
	}

	

	//This is the main APU for saving and updates to the index
	public void saveAllData(Collection<Data> inAll, User inUser)
	{
		PropertyDetails details = getPropertyDetailsArchive().getPropertyDetailsCached(getSearchType());

		for (Object object: inAll)
		{
			Data data = (Data)object;
			Lock lock = null;
			try
			{
				
				lock = getLockManager().lock(getCatalogId(), getSearchType() + "/" + data.getSourcePath(),"admin");
				updateElasticIndex(details, data);
				getXmlSearcher().saveAllData(inAll, inUser);
			}
			catch(Throwable ex)
			{
				log.error("problem saving " + data.getId() , ex);
				throw new OpenEditException(ex);
			}
			finally
			{
				getLockManager().release(getCatalogId(), lock);
			}
		}
	}

	public void saveData(Data inData, User inUser)
	{
		//update the index
		PropertyDetails details = getPropertyDetailsArchive().getPropertyDetailsCached(getSearchType());

		Lock lock = null;
		try
		{
			
			lock = getLockManager().lock(getCatalogId(), getSearchType(),"admin");
			updateElasticIndex(details, inData);
			getXmlSearcher().saveData(inData, inUser);
		}
		catch(Throwable ex)
		{
			log.error("problem saving " + inData.getId() , ex);
			throw new OpenEditException(ex);
		}
		finally
		{
			getLockManager().release(getCatalogId(), lock);
		}
	}
	
	
	
	
	
}
