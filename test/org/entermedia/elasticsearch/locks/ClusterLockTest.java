package org.entermedia.elasticsearch.locks;

import java.util.Collection;
import java.util.Date;
import java.util.Iterator;

import org.entermedia.locks.Lock;
import org.entermedia.locks.LockManager;
import org.openedit.Data;
import org.openedit.data.Searcher;
import org.openedit.data.SearcherManager;
import org.openedit.entermedia.BaseEnterMediaTest;
import org.openedit.entermedia.model.LockTest;
import org.openedit.util.DateStorageUtil;


public class ClusterLockTest extends LockTest
{
	
	public void testLockOrder()
	{
		//create a bunch of locks out of order make sure the come back in the correct order
		LockManager manager = (LockManager)getFixture().getModuleManager().getBean("lockManager");
		SearcherManager searcherManager = (SearcherManager)getFixture().getModuleManager().getBean("searcherManager");
		
		String path = "/entermedia/catalogs/testcatalog/assets/users/101/index.html";
		String cat = "entermedia/catalogs/testcatalog";
		int numLocks = 3;
		manager.releaseAll(cat, path);
		
		Searcher searcher = searcherManager.getSearcher(cat, "lock");
		for (int i = 0; i < numLocks; i++)
		{
			Lock lockrequest = (Lock) searcher.createNewData();
			lockrequest.setPath(path);
			lockrequest.setOwnerId("admin");
			Date current = new Date();
			Date locktime = new Date(current.getTime() - i * 100000000);
			lockrequest.setDate(locktime);
			//lockrequest.setProperty("date", DateStorageUtil.getStorageUtil().formatForStorage(locktime));
			searcher.saveData(lockrequest, null);
		}
		
		Collection locks = manager.getLocksByDate(cat, path);
		assertNotNull(locks);
		
		//make sure pastlock is the first one
		Data prevLock = null;
		for (Iterator iterator = locks.iterator(); iterator.hasNext();)
		{
			Data lock = (Data) iterator.next();
			if(prevLock != null)
			{
				Date prevDate = DateStorageUtil.getStorageUtil().parseFromStorage(prevLock.get("date"));
				Date currentDate = DateStorageUtil.getStorageUtil().parseFromStorage(lock.get("date"));
				assertTrue(prevDate.getTime() < currentDate.getTime());
			}
			prevLock = lock;
		}
	}
		
}
