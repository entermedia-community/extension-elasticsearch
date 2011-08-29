package org.entermedia.elasticsearch.searchers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dom4j.Element;
import org.entermedia.locks.Lock;
import org.openedit.Data;
import org.openedit.data.DataArchive;
import org.openedit.data.PropertyDetails;
import org.openedit.data.XmlDataArchive;
import org.openedit.data.XmlFileSearcher;
import org.openedit.repository.ContentItem;
import org.openedit.xml.ElementData;
import org.openedit.xml.XmlArchive;
import org.openedit.xml.XmlFile;

import com.openedit.OpenEditException;
import com.openedit.page.manage.PageManager;
import com.openedit.users.User;
import com.openedit.util.PathProcessor;

public class ElasticXmlFileSearcher extends BaseElasticSearcher
{
	protected Log log = LogFactory.getLog(XmlFileSearcher.class);
	protected XmlArchive fieldXmlArchive;
	protected DataArchive fieldXmlDataArchive; //lazy loaded
	protected String fieldPrefix;
	protected String fieldDataFileName;

	public PageManager getPageManager()
	{
		return fieldPageManager;
	}

	public void setPageManager(PageManager pageManager)
	{
		fieldPageManager = pageManager;
	}

	public String getPathToData()
	{
		return "/WEB-INF/data/" + getCatalogId() + "/" + getPrefix();
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
	protected DataArchive getDataArchive()
	{
		if (fieldXmlDataArchive == null)
		{
			fieldXmlDataArchive = new XmlDataArchive();
			fieldXmlDataArchive.setXmlArchive(getXmlArchive());
			fieldXmlDataArchive.setDataFileName(getDataFileName());
			fieldXmlDataArchive.setElementName(getSearchType());
			fieldXmlDataArchive.setPathToData(getPathToData());
		}
		return fieldXmlDataArchive;
	}
	public XmlArchive getXmlArchive()
	{
		return fieldXmlArchive;
	}

	public void setXmlArchive(XmlArchive inXmlArchive)
	{
		fieldXmlArchive = inXmlArchive;
	}
	
	public Data createNewData()
	{
		if( fieldNewDataName == null)
		{
			ElementData data = new ElementData();
			
			return data;
		}
		return (Data)getModuleManager().getBean(getNewDataName());
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
			final List buffer = new ArrayList(100);
			PathProcessor processor = new PathProcessor()
			{
				public void processFile(ContentItem inContent, User inUser)
				{
					if (!inContent.getName().equals(getDataFileName()))
					{
						return;
					}
					String sourcepath = inContent.getPath();
					sourcepath = sourcepath.substring(getPathToData().length() + 1,
							sourcepath.length() - getDataFileName().length() - 1);
					hydrateData( inContent, sourcepath, buffer);
					incrementCount();
				}
			};
			processor.setRecursive(true);
			processor.setRootPath(getPathToData());
			processor.setPageManager(getPageManager());
			processor.setFilter("xml");
			processor.process();
			updateIndex(buffer,null);
			log.info("reindexed " + processor.getExecCount());
			flushChanges();			
		}
		finally
		{
			setReIndexing(false);
		}
	}

	protected void hydrateData(ContentItem inContent, String sourcepath, List buffer)
	{
		String path = inContent.getPath();
		//TODO: Create new api to load up assets
		XmlFile content = getDataArchive().getXmlArchive().getXml(path, getSearchType());

		// TODO Auto-generated method stub
		for (Iterator iterator = content.getElements().iterator(); iterator.hasNext();)
		{
			Element element = (Element) iterator.next();
			ElementData data = new ElementData();
			data.setElement(element);
			data.setSourcePath(sourcepath);
			buffer.add(data);
			if( buffer.size() > 99)
			{
				updateIndex(buffer,null);
			}
		}

	}
	public String getPrefix()
	{
		if(fieldPrefix == null)
		{
			fieldPrefix = getPageManager().getPage("/" + getCatalogId()).get("defaultdatafolder");
			if( fieldPrefix == null)
			{
				fieldPrefix = getSearchType();
			}
		}
		return fieldPrefix;
	}

	public void setPrefix(String prefix)
	{
		fieldPrefix = prefix;
	}

	public void delete(Data inData, User inUser)
	{
		if( inData == null || inData.getSourcePath() == null || inData.getId() == null )
		{
			throw new OpenEditException("Cannot delete null data.");
		}
		Lock lock = getLockManager().lock(getCatalogId(), getPathToData() + "/" + inData.getSourcePath(),"admin");
		try
		{
			getDataArchive().delete(inData,inUser);
		}
		finally
		{
			getLockManager().release(getCatalogId(), lock);
		}
		// Remove from Index
		super.delete(inData, inUser);
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
				lock = getLockManager().lock(getCatalogId(), getPathToData() + "/" + data.getSourcePath(),"admin");
				updateElasticIndex(details, data);
				getDataArchive().saveData(data, inUser);
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

	public Object searchByField(String inField, String inValue)
	{
		Object hit =  super.searchByField(inField, inValue);
		//load up a real object?
		String sourcepath = null;
		String id = null;
		
		if( hit instanceof Data)
		{
			Data newdata = (Data)hit;
			if( newdata.getSourcePath() == null)
			{
				log.info("Source path is null on search results "  +getSearchType() );
				return null;
			}
			sourcepath = newdata.getSourcePath();
			id = newdata.getId();
		}
		else
		{
//			Map newdata = (Map)hit;
//			sourcepath = (String)newdata.get("sourcepath");
//			id = (String)newdata.get("_id");
		}
		String path = getPathToData() + "/" + sourcepath + "/" + getSearchType() + ".xml";
		XmlFile content = getDataArchive().getXmlArchive().getXml(path, getSearchType());
		//log.info( newdata.getProperties() );
		if( !content.isExist() )
		{
			log.error("Missing data file " + path);
		}
		Element element = content.getElementById(id);
		
		ElementData realdata = (ElementData)createNewData();
		realdata.setElement(element);
		realdata.setSourcePath(sourcepath);

		return realdata;
	}
}
