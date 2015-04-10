package org.entermedia.elasticsearch;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHitField;
import org.openedit.Data;
import org.openedit.MultiValued;
import org.openedit.data.BaseData;
import org.openedit.data.SaveableData;

public class SearchHitData extends BaseData implements Data, MultiValued, SaveableData
{
	protected SearchHit fieldSearchHit;

	public SearchHitData(SearchHit inDoc)
	{
		setSearchHit(inDoc);
		setId(getSearchHit().getId());
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
		String svalue = super.get(inId);
		if (svalue != null)
		{
			return svalue;
		}
		svalue = getFromDb(inId);
		return svalue;
	}

	protected String getFromDb(String inId)
	{
		if (inId.equals(".version"))
		{
			if (getSearchHit().getVersion() > -1)
			{
				return String.valueOf(getSearchHit().getVersion());
			}
			return null;
		}

		SearchHitField field = getSearchHit().field(inId);
		if (field != null)
		{
			Object value = field.getValue();
			if (value != null)
			{
				return String.valueOf(value);
			}
		}
		Object found = getSearchHit().getSource().get(inId);
		if (found != null)
		{
			String val = String.valueOf(found);
			return val;
		}
		return null;
	}

	public Iterator keys()
	{
		return getProperties().keySet().iterator();
	}

	public Map getProperties()
	{
		Map all = new HashMap();
		for (Iterator iterator = getSearchHit().getSource().keySet().iterator(); iterator.hasNext();)
		{
			String key = (String) iterator.next();
			String val = getFromDb(key);
			all.put(key, val);
		}
		String version = getFromDb(".version");
		if (version != null)
		{
			all.put(".version", version);
		}
		all.putAll(super.getProperties());

		return all;
	}

	public String toString()
	{
		if (getName() != null)
		{
			return getName();
		}
		else
		{
			return getId();
		}
	}
}
