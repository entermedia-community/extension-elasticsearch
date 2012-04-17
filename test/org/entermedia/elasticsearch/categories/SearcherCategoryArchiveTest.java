package org.entermedia.elasticsearch.categories;


import java.util.List;

import org.openedit.entermedia.BaseEnterMediaTest;
import org.openedit.entermedia.Category;
import org.openedit.entermedia.CategoryArchive;

public class SearcherCategoryArchiveTest  extends BaseEnterMediaTest
{
	public void testVerifyConfiguration()
	{
		CategoryArchive archive = getMediaArchive().getCategoryArchive();
		assertNotNull("asset searcher is NULL!", archive);
		assertTrue( archive instanceof SearcherCategoryArchive );
	}

	public void testLoadTree()
	{
		CategoryArchive archive = getMediaArchive().getCategoryArchive();
		archive.reloadCategories();
		Category root = archive.getRootCategory();
		assertNotNull("root is null",root);
		List children  = root.getChildren();
		//assertTrue("Empty list", children.size() > 0);
	}

	public void testEditTree()
	{
		CategoryArchive archive = getMediaArchive().getCategoryArchive();
//		List children  = parent.getChildren();
		//assertTrue("Empty list", children.size() > 0);
		
		Category child = archive.createCategoryTree("users/tester"); //this calls saveAll if needed
		Category users = archive.getCategory("users");
		assertNotNull("users is null",users);

		Category found = archive.getCategory(child.getId());
		assertNotNull("tester is null",found);
		
		archive.reloadCategories();
		Category found2 = archive.getCategory(child.getId());
		assertNotNull("tester is null",found2);
		
	}
}
