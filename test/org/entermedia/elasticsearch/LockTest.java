package org.entermedia.elasticsearch;

import org.entermedia.locks.Lock;
import org.entermedia.locks.LockManager;
import org.openedit.entermedia.BaseEnterMediaTest;


public class LockTest extends BaseEnterMediaTest
{
	public void testLocks()
	{
		LockManager manager = (LockManager)getFixture().getModuleManager().getBean("lockManager");
		Lock lock = manager.lock("entermedia/catalogs/testcatalog", "/entermedia/catalogs/testcatalog/assets/users/101/index.html", "admin");
		assertNotNull(lock);

		assertTrue(lock.isOwner("admin"));
		
		//clear
		
		manager.lockIfPossible(inCatId, inPath, inOwnerId)
		
		manager.release(inCatId, inPath, inOwnerId)
	}
	
	public void testAssetIsAlreadyLocked() throws Exception{
		LockManager manager = (LockManager)getFixture().getModuleManager().getBean("lockManager");
		Lock lock = manager.lock("entermedia/catalogs/testcatalog", "/entermedia/catalogs/testcatalog/assets/users/101/index.html", "admin");
		
		assertFalse("Student should not be able to get lock", lock.isOwner("student"));
	}
}
