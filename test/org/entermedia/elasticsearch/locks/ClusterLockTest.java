package org.entermedia.elasticsearch.locks;

import java.util.ConcurrentModificationException;

import org.entermedia.elasticsearch.searchers.LockSearcher;
import org.entermedia.locks.Lock;
import org.openedit.entermedia.cluster.ClusterLockManager;
import org.openedit.entermedia.model.LockTest;


public class ClusterLockTest extends LockTest
{
	//Had some problems with the very first lock not being saved ok
	public void testLock()
	{
		String catid = "entermedia/catalogs/testcatalog";
		ClusterLockManager manager = (ClusterLockManager)getStaticFixture().getModuleManager().getBean(catid,"lockManager");
		
		String path = "/entermedia/catalogs/testcatalog/assets/users/101/index.html";
		
		manager.releaseAll(path);
		
		Lock lock = manager.lock(path, "admin");
		assertNotNull(lock);

		LockSearcher searcher = (LockSearcher)manager.getLockSearcher();
		searcher.clearStaleLocks();
		
		lock = manager.loadLock(path);
		assertFalse(lock.isLocked());

		//clear
		//manager.lockIfPossible(ininPath, inOwnerId)
		//manager.release(ininPath, inOwnerId)
	}

	public void testVersion() throws Exception
	{
		String catid = "entermedia/catalogs/testcatalog";
		ClusterLockManager manager = (ClusterLockManager)getStaticFixture().getModuleManager().getBean(catid,"lockManager");
		String path = "/entermedia/catalogs/testcatalog/assets/users/101/index.html";
		
		Lock lockfirst = manager.loadLock(path);
		String version = lockfirst.get("_version");
		assertNotNull(version);

		Lock locksecond = manager.loadLock(path);
		locksecond.setOwnerId("fastdude");
		manager.getLockSearcher().saveData(locksecond, null);

		String version2 = locksecond.get("_version");
		assertNotNull(version2);
		
		lockfirst.setOwnerId("slowdude");
		boolean failed = false;
		try
		{
			manager.getLockSearcher().saveData(lockfirst, null);
		}
		catch( ConcurrentModificationException ex)
		{
			failed = true;
		}
		assertTrue(failed);
	}
}
