package org.entermedia.elasticsearch.searchers;

import java.util.Collection;

import org.openedit.Data;

import com.openedit.OpenEditException;
import com.openedit.users.User;

public class IndexElasticSearcher extends BaseElasticSearcher
{
	public void reIndexAll() throws OpenEditException
	{
		//there is not reindex step since it is only in memory
		if( isReIndexing())
		{
			return;
		}
		try
		{
			setReIndexing(true);
			deleteAll(null); //This only deleted the index
		}
		finally
		{
			setReIndexing(false);
		}
			
		
	}

	public void saveAllData(Collection<Data> inAll, User inUser)
	{
//		for(Data data:inAll)
//		{
//			if( data.getId() == null)
//			{
//				data.setId(nextId());
//			}
//		}
		updateIndex(inAll, inUser);
	}
}
