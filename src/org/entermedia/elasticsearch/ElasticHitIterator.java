/*
Copyright (c) 2003 eInnovation Inc. All rights reserved

This library is free software; you can redistribute it and/or modify it under the terms
of the GNU Lesser General Public License as published by the Free Software Foundation;
either version 2.1 of the License, or (at your option) any later version.

This library is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
See the GNU Lesser General Public License for more details.
*/

package org.entermedia.elasticsearch;

import java.io.IOException;
import java.util.Iterator;

import org.elasticsearch.search.SearchHit;

import com.openedit.OpenEditRuntimeException;


/**
 * @author cburkey
 */
public class ElasticHitIterator implements Iterator
{
	protected Iterator fieldSearchHitIterator;
	
	public Iterator getSearchHitIterator()
	{
		return fieldSearchHitIterator;
	}
	public void setSearchHitIterator(Iterator inSearchHitIterator)
	{
		fieldSearchHitIterator = inSearchHitIterator;
	}
	public ElasticHitIterator(Iterator inIter)
	{
		setSearchHitIterator(inIter);
	}
	public ElasticHitIterator()
	{
	}

	/**
	 * @see java.util.Iterator#hasNext()
	 */
	public boolean hasNext()
	{
		return getSearchHitIterator().hasNext();
	}

	/**
	 * @see java.util.Iterator#next()
	 */
	public Object next()
	{
		SearchHit hit = (SearchHit)getSearchHitIterator().next();
		return new SearchHitData(hit);
	}

	/**
	 * @see java.util.Iterator#remove()
	 */
	public void remove()
	{
		getSearchHitIterator().remove();
	}
}
