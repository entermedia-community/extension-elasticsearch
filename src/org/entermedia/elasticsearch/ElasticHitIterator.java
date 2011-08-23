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
	protected ElasticHitTracker fieldElasticHitTracker;
	int fieldCurrentLocation = 0;
	
	public ElasticHitIterator(ElasticHitTracker inTracker)
	{
		setElasticHitTracker(inTracker);
	}

	protected ElasticHitTracker getElasticHitTracker()
	{
		return fieldElasticHitTracker;
	}

	protected void setElasticHitTracker(ElasticHitTracker inElasticHitTracker)
	{
		fieldElasticHitTracker = inElasticHitTracker;
	}

	/**
	 * @see java.util.Iterator#hasNext()
	 */
	public boolean hasNext()
	{
		if( fieldCurrentLocation + 1 >= getElasticHitTracker().size() )
		{
			return false;
		}
		return true;
	}

	/**
	 * @see java.util.Iterator#next()
	 */
	public Object next()
	{
		fieldCurrentLocation++;
		return getElasticHitTracker().get(fieldCurrentLocation);
	}

	/**
	 * @see java.util.Iterator#remove()
	 */
	public void remove()
	{
		throw new UnsupportedOperationException();
	}
}
