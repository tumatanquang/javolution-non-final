/*
 * Javolution - Java(TM) Solution for Real-Time and Embedded Systems
 * Copyright (C) 2005 - Javolution (http://javolution.org/)
 * All rights reserved.
 *
 * Permission to use, copy, modify, and distribute this software is
 * freely granted, provided that this notice is preserved.
 */
package _templates.javolution.util.primitive;
import _templates.java.io.Serializable;
import _templates.java.lang.Cloneable;
import _templates.javolution.lang.Reusable;
/**
 * <p> This class represents the abstract base class for all
 *     primitive collections.</p>
 *
 * @author <a href="mailto:jean-marie@dautelle.com">Jean-Marie Dautelle</a>
 * @version 6.0.0, August 18, 2026
 */
public abstract class FastPrimitiveCollection
		implements Cloneable, Reusable, Serializable {
	/**
	 * The current number of elements contained in this collection.
	 */
	int size;
	/**
	 * Returns the number of elements in this collection.
	 *
	 * @return the number of elements in this collection.
	 */
	public int size() {
		return size;
	}
	/**
	 * Indicates if this collection contains no elements.
	 *
	 * @return <code>true</code> if this collection contains no elements;
	 *         <code>false</code> otherwise.
	 */
	public boolean isEmpty() {
		return size == 0;
	}
	/**
	 * Removes all elements from this collection.
	 */
	public void clear() {
		size = 0;
	}
	/**
	 * Resets this collection for object pooling / recycling by clearing its elements.
	 */
	public void reset() {
		size = 0;
	}
	/**
	 * Trims the capacity of this collection instance to be the collection's current size.
	 */
	public abstract void trimToSize();
	/**
	 * Increases the capacity of this collection instance, if necessary, to ensure
	 * that it can hold at least the number of elements specified by the
	 * minimum capacity argument.
	 *
	 * @param min the desired minimum capacity.
	 */
	public abstract void ensureCapacity(int min);
	/**
	 * Returns an unmodifiable view over this primitive collection.
	 *
	 * @return an unmodifiable view over this collection.
	 */
	public abstract FastPrimitiveCollection unmodifiable();
	/**
	 * Returns a thread-safe (read-write locked) view over this primitive collection.
	 *
	 * @return a shared thread-safe view over this collection.
	 */
	public abstract FastPrimitiveCollection shared();
	/**
	 * Returns a primitive iterator over the elements in this collection.
	 *
	 * @return a primitive iterator over this collection's elements.
	 */
	public abstract FastPrimitiveIterator iterator();
}
