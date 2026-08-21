/*
 * Javolution - Java(TM) Solution for Real-Time and Embedded Systems
 * Copyright (C) 2005 - Javolution (http://javolution.org/)
 * All rights reserved.
 *
 * Permission to use, copy, modify, and distribute this software is
 * freely granted, provided that this notice is preserved.
 */
package _templates.javolution.util.primitive;
/**
 * A base interface for primitive iterators.
 *
 * @version 5.7.8, July 27, 2026
 */
public interface FastPrimitiveIterator {
	/**
	 * Returns <code>true</code> if the iteration has more elements.
	 *
	 * @return <code>true</code> if the iteration has more elements;
	 *         <code>false</code> otherwise.
	 */
	boolean hasNext();
	/**
	 * Removes from the underlying collection the last element returned
	 * by this iterator (optional operation).
	 */
	void remove();
}