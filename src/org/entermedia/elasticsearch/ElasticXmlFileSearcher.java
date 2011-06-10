package org.entermedia.elasticsearch;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dom4j.Element;
import org.openedit.Data;
import org.openedit.data.XmlDataArchive;
import org.openedit.data.XmlFileSearcher;
import org.openedit.repository.ContentItem;
import org.openedit.xml.ElementData;
import org.openedit.xml.XmlArchive;
import org.openedit.xml.XmlFile;

import com.openedit.OpenEditException;
import com.openedit.page.manage.PageManager;
import com.openedit.users.User;
import com.openedit.util.IntCounter;
import com.openedit.util.PathProcessor;

public class ElasticXmlFileSearcher extends BaseElasticSearcher
{
	protected Log log = LogFactory.getLog(XmlFileSearcher.class);
	protected XmlArchive fieldXmlArchive;
	protected XmlDataArchive fieldXmlDataArchive;
	protected IntCounter fieldIntCounter;
	protected PageManager fieldPageManager;
	protected String fieldPrefix;

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
		return getSearchType() + ".xml";
	}
	protected XmlDataArchive getXmlDataArchive()
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


	public void reIndexAll() throws OpenEditException
	{		
		//For now just add things to the index. It never deletes
		
		final List buffer = new ArrayList(100);
		PathProcessor processor = new PathProcessor()
		{
			public void processFile(ContentItem inContent, User inUser)
			{
				if (!inContent.getName().equals(getSearchType() + ".xml"))
				{
					return;
				}
				String sourcepath = inContent.getPath();
				sourcepath = sourcepath.substring(getPathToData().length() + 1,
						sourcepath.length() - getDataFileName().length() - 1);
				String path = inContent.getPath();
				XmlFile content = getXmlArchive().getXml(path, getSearchType());
				for (Iterator iterator = content.getElements().iterator(); iterator.hasNext();)
				{
					Element element = (Element) iterator.next();
					ElementData data = (ElementData)createNewData();
					data.setElement(element);
					data.setSourcePath(sourcepath);
					buffer.add(data);
					if( buffer.size() > 99)
					{
						updateIndex(buffer,null);
					}
				}
			}
		};
		processor.setRecursive(true);
		processor.setRootPath(getPathToData());
		processor.setPageManager(getPageManager());
		processor.setFilter("xml");
		processor.process();
		updateIndex(buffer,null);
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
		getXmlDataArchive().delete(inData,inUser);
		// Remove from Index
		super.delete(inData, inUser);
	}

	
	public void saveAllData(List inAll, User inUser)
	{
		for (Object object: inAll)
		{
			Data data = (Data)object;
			if(data.getId() == null)
			{
				data.setId(nextId());
			}			
		}
		getXmlDataArchive().saveAllData(inAll, inUser);
		super.saveAllData(inAll,inUser);
	}
}
