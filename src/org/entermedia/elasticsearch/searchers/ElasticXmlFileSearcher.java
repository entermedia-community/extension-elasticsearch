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
import org.openedit.entermedia.SourcePathCreator;
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
	protected Log log = LogFactory.getLog(ElasticXmlFileSearcher.class);
	protected XmlArchive fieldXmlArchive;
	protected DataArchive fieldDataArchive; //lazy loaded
	protected String fieldPrefix;
	protected String fieldDataFileName;
protected SourcePathCreator fieldSourcePathCreator;
	
	public SourcePathCreator getSourcePathCreator()
	{
		return fieldSourcePathCreator;
	}
	public void setSourcePathCreator(SourcePathCreator inSourcePathCreator)
	{
		fieldSourcePathCreator = inSourcePathCreator;
	}
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
		return (Data)getModuleManager().getBean( getNewDataName());
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
			processor.setIncludeExtensions("xml");
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

	public void saveData(Data inData, User inUser)
	{
		//update the index
		PropertyDetails details = getPropertyDetailsArchive().getPropertyDetailsCached(getSearchType());

		Lock lock = null;
		try
		{
			lock = getLockManager().lock(getCatalogId(), getPathToData() + "/" + inData.getSourcePath(),"admin");
			
			
			updateElasticIndex(details, inData);
			//TODO - we might need the sourcepath saved in the below case.
			if( inData.getSourcePath() == null)
			{
				String sourcepath = getSourcePathCreator().createSourcePath(inData, inData.getId() );
				inData.setSourcePath(sourcepath);
			}
			
			
			getDataArchive().saveData(inData, inUser);
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
	protected DataArchive getDataArchive()
	{
		if (fieldDataArchive == null)
		{
			XmlDataArchive archive = new XmlDataArchive();
			archive.setXmlArchive(getXmlArchive());
			archive.setDataFileName(getDataFileName());
			archive.setElementName(getSearchType());
			archive.setPathToData(getPathToData());
			fieldDataArchive = archive;
		}

		return fieldDataArchive;
	}
	public Object searchByField(String inField, String inValue)
	{
		if( inValue == null)
		{
			throw new OpenEditException("Can't search for null value on field " + inField);
		}
		Data newdata =  (Data) super.searchByField(inField, inValue);
		//load up a real object?
		String sourcepath = null;
		String id = null;
		
		if( newdata == null)
		{
			return null;	
		}
			
		
		if( newdata.getSourcePath() == null)
		{
				sourcepath = getSourcePathCreator().createSourcePath(newdata, newdata.getId() );
				
		}	else{		
		sourcepath = newdata.getSourcePath();
		}
		id = newdata.getId();
		
		String path = getPathToData() + "/" + sourcepath + "/" + getSearchType() + ".xml";
		XmlFile content = getDataArchive().getXmlArchive().getXml(path, getSearchType());
		//log.info( newdata.getProperties() );
		if( !content.isExist() )
		{
			throw new OpenEditException("Missing data file " + path);
		}
		Element element = content.getElementById(id);
		
		ElementData realdata = (ElementData)createNewData();
		realdata.setElement(element);
		realdata.setSourcePath(sourcepath);

		return realdata;
	}
}
