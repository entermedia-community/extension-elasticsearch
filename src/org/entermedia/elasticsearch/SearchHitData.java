package org.entermedia.elasticsearch;

import java.util.Iterator;
import java.util.Map;

import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHitField;
import org.openedit.Data;

import com.openedit.OpenEditException;

public class SearchHitData implements Data
{
	protected SearchHit fieldSearchHit;
	
	public SearchHitData()
	{
	}
	public SearchHitData(SearchHit inDoc)
	{
		setSearchHit(inDoc);
	}
	public SearchHit getSearchHit()
	{
		return fieldSearchHit;
	}
	public void setSearchHit(SearchHit inSearchHit)
	{
		fieldSearchHit = inSearchHit;
	}
	public String get(String inId)
	{
		SearchHitField field = getSearchHit().field(inId);
		if( field != null)
		{
			Object value = field.getValue();
			if( value != null)
			{
				return String.valueOf(value);
			}
		}
		return null;
	}

	public String getId()
	{
		return get("id");
	}

	public String getName()
	{
		return get("name");
	}
	public void setName(String inName)
	{
		setProperty("name", inName);
	}
	public void setId(String inNewid)
	{
		throw new OpenEditException("Search results are not editable");
	}

	public void setProperty(String inId, String inValue)
	{
		throw new OpenEditException("Search results are not editable");
	}
	public Iterator keys()
	{
		//TODO: Use Strings
		return getSearchHit().getFields().keySet().iterator();
	}
	public String getSourcePath()
	{
		// TODO Auto-generated method stub
		return get("sourcepath");
	}
	public void setSourcePath(String inSourcepath)
	{
		throw new OpenEditException("Search results are not editable");
	}
	public Map getProperties() 
	{
		return null;
	}
	public String toString()
	{
		if(getName() != null){
		return getName();
		} else{
			return getId();
		}
	}

}
