package org.entermedia.elasticsearch.locks;

import java.util.ConcurrentModificationException;

import org.entermedia.locks.Lock;
import org.entermedia.locks.LockManager;
import org.openedit.entermedia.cluster.ClusterLockManager;
import org.openedit.entermedia.model.LockTest;

import com.openedit.OpenEditException;


public class ClusterLockTest extends LockTest
{
	//Had some problems with the very first lock not being saved ok
	public void testLock()
	{
		LockManager manager = (LockManager)getStaticFixture().getModuleManager().getBean("lockManager");
		
		String path = "/entermedia/catalogs/testcatalog/assets/users/101/index.html";
		String catid = "entermedia/catalogs/testcatalog";
		manager.releaseAll(catid, path);
		
		Lock lock = manager.lock(catid, path, "admin");
		assertNotNull(lock);

		manager.releaseAll(catid, path);
		lock = manager.loadLock(catid, path);
		assertFalse(lock.isLocked());

		//clear
		//manager.lockIfPossible(inCatId, inPath, inOwnerId)
		//manager.release(inCatId, inPath, inOwnerId)
	}

	public void testVersion() throws Exception
	{
		ClusterLockManager manager = (ClusterLockManager)getStaticFixture().getModuleManager().getBean("lockManager");
		String path = "/entermedia/catalogs/testcatalog/assets/users/101/index.html";
		String catid = "entermedia/catalogs/testcatalog";
		
		Lock lockfirst = manager.loadLock(catid, path);
		String version = lockfirst.get("version");
		assertNotNull(version);

		Lock locksecond = manager.loadLock(catid, path);
		locksecond.setOwnerId("fastdude");
		manager.getLockSearcher(catid).saveData(locksecond, null);

		String version2 = locksecond.get("version");
		assertNotNull(version2);
		
		lockfirst.setOwnerId("slowdude");
		boolean failed = false;
		try
		{
			manager.getLockSearcher(catid).saveData(lockfirst, null);
		}
		catch( OpenEditException ex)
		{
			if( ex.getCause() instanceof ConcurrentModificationException)
			{
				failed = true;
			}
		}
		assertTrue(failed);
	}
}
