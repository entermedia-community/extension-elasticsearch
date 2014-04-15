package org.entermedia.elasticsearch;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHitField;
import org.openedit.Data;
import org.openedit.MultiValued;

import com.openedit.OpenEditException;

public class SearchHitData implements Data, MultiValued
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
		if( inId.equals("id") || inId.equals("_id"))
		{
			return getSearchHit().getId();
		}
		if( inId.equals(".version"))
		{
			if( getSearchHit().getVersion() > -1)
			{
				return String.valueOf( getSearchHit().getVersion() );
			}
			return null;
		}

		SearchHitField field = getSearchHit().field(inId);
		if( field != null)
		{
			Object value = field.getValue();
			if( value != null)
			{
				return String.valueOf(value);
			}
		}
		Object found = getSearchHit().getSource().get(inId);
		if( found != null)
		{
			String val = String.valueOf(found);
			return val;
		}
		return null;
	}

	public String getId()
	{
		String id = get("id");
		if( id == null)
		{
			id = getSearchHit().getId();
		}
		return id;
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
		Map all = new HashMap();
		for (Iterator iterator = getSearchHit().getSource().keySet().iterator(); iterator.hasNext();)
		{
			String key = (String) iterator.next();
			String val = get(key);
			all.put(key, val);
		}
		String version = get(".version");
		if( version != null)
		{
			all.put(".version",version);
		}
	
		return all;
	}
	public String toString()
	{
		if(getName() != null){
		return getName();
		} else{
			return getId();
		}
	}
	@Override
	public void setProperties(Map<String, String> inProperties) {
		// TODO Auto-generated method stub
		throw new OpenEditException("Search results are not editable");
	}
	
	
	//Is this correct?  Need to double check.  might be using the JSON array values.
	
	public Collection getValues(String inKey)
	{
		String val = get(inKey);
		
		if (val == null)
			return null;
		
		String[] vals = null;
		if( val.contains("|") )
		{
			vals = VALUEDELMITER.split(val);
		}
		else
		{
			vals = val.split("\\s+"); //legacy
		}

		Collection collection = Arrays.asList(vals);
		//if null check parent
		return collection;
	}
	
	@Override
	public void setValues(String inKey, Collection<String> inValues)
	{
		// TODO Auto-generated method stub
		
	}

}
