/*
 * File: CopyOnWriteArrayList.java
 *
 * Written by Doug Lea. Adapted and released, under explicit
 * permission, from JDK1.2 ArrayList.java
 * which carries the following copyright:
 *
 *    * Copyright 1997 by Sun Microsystems, Inc.,
 *    * 901 San Antonio Road, Palo Alto, California, 94303, U.S.A.
 *    * All rights reserved.
 *    *
 *    * This software is the confidential and proprietary information
 *    * of Sun Microsystems, Inc. ("Confidential Information").  You
 *    * shall not disclose such Confidential Information and shall use
 *    * it only in accordance with the terms of the license agreement
 *    * you entered into with Sun.
 *
 * History:
 * Date         Who            What
 * 21Jun1998    dl             Create public version
 *  9Oct1999    dl             faster equals
 * 29jun2001    dl             Serialization methods now private
 */
package _templates.javolution.util.concurrent;
import java.io.IOException;
import java.util.NoSuchElementException;
import _templates.java.io.ObjectInputStream;
import _templates.java.io.ObjectOutputStream;
import _templates.java.io.Serializable;
import _templates.java.lang.CloneNotSupportedException;
import _templates.java.lang.Cloneable;
/*@JVM-1.1+@import java.lang.reflect.Array;/**/
import _templates.java.lang.UnsupportedOperationException;
import _templates.java.util.AbstractList;
import _templates.java.util.Collection;
import _templates.java.util.ConcurrentModificationException;
import _templates.java.util.Iterator;
import _templates.java.util.List;
import _templates.java.util.ListIterator;
/**
 * This class implements a variant of java.util.ArrayList in which
 * all mutative operations (add, set, and so on) are implemented
 * by making a fresh copy of the underlying array.
 *
 * This is ordinarily too costly, but it becomes attractive when traversal
 * operations vastly overwhelm mutations, and, especially,
 * when you cannot or don't want to
 * synchronize traversals, yet need to preclude interference
 * among concurrent threads.
 * The iterator method uses a reference to the
 * state of the array at the point that the
 * iterator was created. This array never changes during
 * the lifetime of the iterator, so interference is impossible.
 * (The iterator will not traverse elements added or changed
 * since the iterator was created, but usually this is a desirable
 * feature.)
 *
 * As much code and documentation as possible was shamelessly copied from
 * java.util.ArrayList (Thanks, Josh!), with the intent of preserving
 * all semantics of ArrayList except for the copy-on-write
 * property.
 * (The java.util collection code could not be subclassed here since all of the existing
 * collection classes assume elementwise mutability.)
 *
 * Because of the copy-on-write policy, some one-by-one
 * mutative operations
 * in the java.util.Arrays and java.util.Collections classes
 * are so time/space intensive as to never
 * be worth calling (except perhaps as benchmarks for garbage collectors :-).
 *
 * Three methods are supported in addition to
 * those described in List and ArrayList. The addIfAbsent
 * and addAllAbsent methods provide Set semantics for add,
 * and are used in CopyOnWriteArraySet. However, they
 * can also be used directly from this List version.
 * The copyIn method (and
 * a constructor that invokes it) allow
 * you to copy in an initial array to use. This method can
 * be useful when you first want to perform many operations
 * on a plain array, and then make a copy available for
 * use through the collection API.
 *
 * Due to their strict read-only nature,
 * element-changing operations on iterators
 * (remove, set, and add) are not supported. These
 * are the only methods throwing UnsupportedOperationException.
 *
 * <p>[ <a href="//gee.cs.oswego.edu/dl/classes/EDU/oswego/cs/dl/util/concurrent/intro.html"> Introduction to this package.</a> ]</p>
 * @see CopyOnWriteArraySet
 */
public class CopyOnWriteArrayList implements List, Cloneable, Serializable {
	/**
	 * The held array. Directly access only within synchronized methods
	 */
	private transient Object[] _array;
	/**
	 * Accessor to the array intended to be called from
	 * within unsynchronized read-only methods
	 */
	private synchronized Object[] array() {
		return _array;
	}
	/**
	 * Constructs an empty list
	 *
	 */
	public CopyOnWriteArrayList() {
		_array = new Object[0];
	}
	/**
	 * Constructs an list containing the elements of the specified
	 * Collection, in the order they are returned by the Collection's
	 * iterator.
	 */
	public CopyOnWriteArrayList(Collection c) {
		_array = new Object[c.size()];
		int size = 0;
		for(final Iterator i = c.iterator(); i.hasNext();) {
			_array[size++] = i.next();
		}
	}
	/**
	 * Create a new CopyOnWriteArrayList holding a copy of given array
	 *
	 * @param toCopyIn the array. A copy of this array is used as the
	 * internal array.
	 */
	public CopyOnWriteArrayList(Object[] toCopyIn) {
		copyIn(toCopyIn, 0, toCopyIn.length);
	}
	/**
	 * Replace the held array with a copy of the <code>n</code>
	 * elements of the provided array, starting at position <code>first</code>.
	 * To copy an entire array, call with arguments (array, 0, array.length).
	 *
	 * @param toCopyIn the array. A copy of the indicated elements of
	 * this array is used as the internal array.
	 * @param first The index of first position of the array to
	 * start copying from.
	 * @param n the number of elements to copy. This will be the new size of
	 * the list.
	 */
	public synchronized void copyIn(Object[] toCopyIn, int first, int n) {
		_array = new Object[n];
		System.arraycopy(toCopyIn, first, _array, 0, n);
	}
	/**
	 * Returns the number of components in this list.
	 *
	 * @return the number of components in this list.
	 */
	public int size() {
		return array().length;
	}
	/**
	 * Tests if this list has no components.
	 *
	 * @return <code>true</code> if this list has no components;
	 *         <code>false</code> otherwise.
	 */
	public boolean isEmpty() {
		return size() == 0;
	}
	/**
	 * Returns true if this list contains the specified element.
	 *
	 * @param elem element whose presence in this List is to be tested.
	 */
	public boolean contains(Object elem) {
		final Object[] elementData = array();
		final int len = elementData.length;
		return indexOf(elem, elementData, len) >= 0;
	}
	/**
	 * Searches for the first occurence of the given argument, testing
	 * for equality using the <code>equals</code> method.
	 *
	 * @param elem an object.
	 * @return the index of the first occurrence of the argument in this
	 *         list; returns <code>-1</code> if the object is not found.
	 * @see Object#equals(Object)
	 */
	public int indexOf(Object elem) {
		final Object[] elementData = array();
		final int len = elementData.length;
		return indexOf(elem, elementData, len);
	}
	/**
	 * static version allows repeated call without needed
	 * to grab lock for array each time
	 */
	private static int indexOf(Object elem, Object[] elementData, int len) {
		if(elem == null) {
			for(int i = 0; i < len; ++i) {
				if(elementData[i] == null)
					return i;
			}
		}
		else {
			for(int i = 0; i < len; ++i) {
				if(elem.equals(elementData[i]))
					return i;
			}
		}
		return -1;
	}
	/**
	 * Searches for the first occurence of the given argument, beginning
	 * the search at <code>index</code>, and testing for equality using
	 * the <code>equals</code> method.
	 *
	 * @param elem an object.
	 * @param index the index to start searching from.
	 * @return the index of the first occurrence of the object argument in
	 *         this List at position <code>index</code> or later in the
	 *         List; returns <code>-1</code> if the object is not found.
	 * @see Object#equals(Object)
	 */
	// needed in order to compile on 1.2b3
	public int indexOf(Object elem, int index) {
		final Object[] elementData = array();
		final int elementCount = elementData.length;
		if(elem == null) {
			for(int i = index; i < elementCount; ++i) {
				if(elementData[i] == null)
					return i;
			}
		}
		else {
			for(int i = index; i < elementCount; ++i) {
				if(elem.equals(elementData[i]))
					return i;
			}
		}
		return -1;
	}
	/**
	 * Returns the index of the last occurrence of the specified object in
	 * this list.
	 *
	 * @param elem the desired component.
	 * @return the index of the last occurrence of the specified object in this list;
	 *         <code>-1</code> if the object is not found.
	 */
	public int lastIndexOf(Object elem) {
		final Object[] elementData = array();
		final int len = elementData.length;
		return lastIndexOf(elem, elementData, len);
	}
	private static int lastIndexOf(Object elem, Object[] elementData, int len) {
		if(elem == null) {
			for(int i = len - 1; i >= 0; --i) {
				if(elementData[i] == null)
					return i;
			}
		}
		else {
			for(int i = len - 1; i >= 0; --i) {
				if(elem.equals(elementData[i]))
					return i;
			}
		}
		return -1;
	}
	/**
	 * Searches backwards for the specified object, starting from the
	 * specified index, and returns an index to it.
	 *
	 * @param elem the desired component.
	 * @param index the index to start searching from.
	 * @return the index of the last occurrence of the specified object in this
	 *         List at position less than index in the List;
	 *         <code>-1<code> if the object is not found.
	 */
	public int lastIndexOf(Object elem, int index) {
		// needed in order to compile on 1.2b3
		final Object[] elementData = array();
		if(elem == null) {
			for(int i = index; i >= 0; --i) {
				if(elementData[i] == null)
					return i;
			}
		}
		else {
			for(int i = index; i >= 0; --i) {
				if(elem.equals(elementData[i]))
					return i;
			}
		}
		return -1;
	}
	/**
	 * Returns a shallow copy of this list. (The elements themselves are not copied.)
	 *
	 * @return a clone of this list.
	 */
	public Object/*CopyOnWriteArrayList*/ clone()
			throws CloneNotSupportedException {
		/*@JVM-1.1+@
		if(true) {
			final Object[] elementData = array();
			final CopyOnWriteArrayList v = (CopyOnWriteArrayList) super.clone();
			v._array = new Object[elementData.length];
			System.arraycopy(elementData, 0, v._array, 0, elementData.length);
			return v;
		}
		/**/
		throw new UnsupportedOperationException("J2ME Not Supported Yet");
	}
	/**
	 * Returns an array containing all of the elements in this list
	 * in the correct order.
	 */
	public Object[] toArray() {
		final Object[] elementData = array();
		final Object[] result = new Object[elementData.length];
		System.arraycopy(elementData, 0, result, 0, elementData.length);
		return result;
	}
	/**
	 * Returns an array containing all of the elements in this list in the
	 * correct order. The runtime type of the returned array is that of the
	 * specified array. If the list fits in the specified array, it is
	 * returned therein. Otherwise, a new array is allocated with the runtime
	 * type of the specified array and the size of this list.
	 *
	 * If the list fits in the specified array with room to spare
	 * (i.e., the array has more elements than the list),
	 * the element in the array immediately following the end of the
	 * collection is set to null. This is useful in determining the length
	 * of the list <em>only</em> if the caller knows that the list
	 * does not contain any null elements.
	 *
	 * @param a the array into which the elements of the list are to
	 *		    be stored, if it is big enough; otherwise, a new array of the
	 * 		    same runtime type is allocated for this purpose.
	 * @return an array containing the elements of the list.
	 * @throws ArrayStoreException the runtime type of a is not a supertype
	 *         of the runtime type of every element in this list.
	 */
	public Object[] toArray(Object[] a) {
		final Object[] elementData = array();
		/*@JVM-1.1+@resizeArray:/**/
		if(a.length < elementData.length) {
			if(true) {
				/*@JVM-1.1+@
				a = (Object[]) Array.newInstance(a.getClass().getComponentType(), elementData.length);
				break resizeArray;
				/**/
			}
			throw new IllegalArgumentException("a.length < elementData.length");
		}
		System.arraycopy(elementData, 0, a, 0, elementData.length);
		if(a.length > elementData.length) {
			a[elementData.length] = null;
		}
		return a;
	}
	// Positional Access Operations
	/**
	 * Returns the element at the specified position in this list.
	 *
	 * @param index index of element to return.
	 * @throws IndexOutOfBoundsException index is out of range (index &lt; 0 || index &gt;= size()).
	 */
	public Object get(int index) {
		final Object[] elementData = array();
		rangeCheck(index, elementData.length);
		return elementData[index];
	}
	/**
	 * Replaces the element at the specified position in this list with
	 * the specified element.
	 *
	 * @param index index of element to replace.
	 * @param element element to be stored at the specified position.
	 * @return the element previously at the specified position.
	 * @throws IndexOutOfBoundsException index out of range (index &lt; 0 || index &gt;= size()).
	 */
	public synchronized Object set(int index, Object element) {
		final int len = _array.length;
		rangeCheck(index, len);
		final Object oldValue = _array[index];
		final boolean same = oldValue == element
				|| element != null && element.equals(oldValue);
		if(!same) {
			final Object[] newArray = new Object[len];
			System.arraycopy(_array, 0, newArray, 0, len);
			newArray[index] = element;
			_array = newArray;
		}
		return oldValue;
	}
	/**
	 * Appends the specified element to the end of this list.
	 *
	 * @param element element to be appended to this list.
	 * @return true (as per the general contract of Collection.add).
	 */
	public synchronized boolean add(Object element) {
		final int len = _array.length;
		final Object[] newArray = new Object[len + 1];
		System.arraycopy(_array, 0, newArray, 0, len);
		newArray[len] = element;
		_array = newArray;
		return true;
	}
	/**
	 * Inserts the specified element at the specified position in this
	 * list. Shifts the element currently at that position (if any) and
	 * any subsequent elements to the right (adds one to their indices).
	 *
	 * @param index index at which the specified element is to be inserted.
	 * @param element element to be inserted.
	 * @throws IndexOutOfBoundsException index is out of range (index &lt; 0 || index &gt; size()).
	 */
	public synchronized void add(int index, Object element) {
		final int len = _array.length;
		if(index > len || index < 0)
			throw new IndexOutOfBoundsException(
					"Index: " + index + ", Size: " + len);
		final Object[] newArray = new Object[len + 1];
		System.arraycopy(_array, 0, newArray, 0, index);
		newArray[index] = element;
		System.arraycopy(_array, index, newArray, index + 1, len - index);
		_array = newArray;
	}
	/**
	 * Removes the element at the specified position in this list.
	 * Shifts any subsequent elements to the left (subtracts one from their
	 * indices). Returns the element that was removed from the list.
	 *
	 * @throws IndexOutOfBoundsException index out of range (index &lt; 0 || index &gt;= size()).
	 * @param index the index of the element to removed.
	 */
	public synchronized Object remove(int index) {
		final int len = _array.length;
		rangeCheck(index, len);
		final Object oldValue = _array[index];
		final Object[] newArray = new Object[len - 1];
		System.arraycopy(_array, 0, newArray, 0, index);
		final int numMoved = len - index - 1;
		if(numMoved > 0) {
			System.arraycopy(_array, index + 1, newArray, index, numMoved);
		}
		_array = newArray;
		return oldValue;
	}
	/**
	 * Removes a single instance of the specified element from this Collection,
	 * if it is present (optional operation). More formally, removes an
	 * element <code>e</code> such that <code>(o == null ? e == null :
	 * o.equals(e))</code>, if the Collection contains one or more such
	 * elements. Returns true if the Collection contained the specified
	 * element (or equivalently, if the Collection changed as a result of the
	 * call).
	 *
	 * @param element element to be removed from this Collection, if present.
	 * @return true if the Collection changed as a result of the call.
	 */
	public synchronized boolean remove(Object element) {
		final int len = _array.length;
		if(len == 0)
			return false;
		// Copy while searching for element to remove
		// This wins in the normal case of element being present
		final int newlen = len - 1;
		final Object[] newArray = new Object[newlen];
		for(int i = 0; i < newlen; ++i) {
			if(element == _array[i]
					|| element != null && element.equals(_array[i])) {
				// found one; copy remaining and exit
				for(int k = i + 1; k < len; ++k) {
					newArray[k - 1] = _array[k];
				}
				_array = newArray;
				return true;
			}
			newArray[i] = _array[i];
		}
		// special handling for last cell
		if(element == _array[newlen]
				|| element != null && element.equals(_array[newlen])) {
			_array = newArray;
			return true;
		}
		return false; // throw away copy
	}
	/**
	 * Removes from this List all of the elements whose index is between
	 * fromIndex, inclusive and toIndex, exclusive. Shifts any succeeding
	 * elements to the left (reduces their index).
	 * This call shortens the List by (toIndex - fromIndex) elements. (If
	 * toIndex == fromIndex, this operation has no effect.)
	 *
	 * @param fromIndex index of first element to be removed.
	 * @param toIndex index after last element to be removed.
	 * @throws IndexOutOfBoundsException fromIndex or toIndex out of range
	 *                                  (fromIndex &lt; 0 || fromIndex &gt;= size() ||
	 *                                  toIndex &gt; size() || toIndex &lt; fromIndex).
	 */
	public synchronized void removeRange(int fromIndex, int toIndex) {
		final int len = _array.length;
		if(fromIndex < 0 || fromIndex >= len || toIndex > len
				|| toIndex < fromIndex)
			throw new IndexOutOfBoundsException();
		final int numMoved = len - toIndex,
				newlen = len - (toIndex - fromIndex);
		final Object[] newArray = new Object[newlen];
		System.arraycopy(_array, 0, newArray, 0, fromIndex);
		System.arraycopy(_array, toIndex, newArray, fromIndex, numMoved);
		_array = newArray;
	}
	/**
	 * Append the element if not present.
	 * This operation can be used to obtain Set semantics
	 * for lists.
	 * @param element element to be added to this Collection, if absent.
	 * @return true if added
	 */
	public synchronized boolean addIfAbsent(Object element) {
		// Copy while checking if already present.
		// This wins in the most common case where it is not present
		final int len = _array.length;
		final Object[] newArray = new Object[len + 1];
		for(int i = 0; i < len; ++i) {
			if(element == _array[i]
					|| element != null && element.equals(_array[i]))
				return false; // exit, throwing away copy
			newArray[i] = _array[i];
		}
		newArray[len] = element;
		_array = newArray;
		return true;
	}
	/**
	 * Returns true if this Collection contains all of the elements in the
	 * specified Collection.
	 *
	 * This implementation iterates over the specified Collection, checking
	 * each element returned by the Iterator in turn to see if it's
	 * contained in this Collection. If all elements are so contained
	 * <code>true</code> is returned, otherwise <code>false</code>.
	 */
	public boolean containsAll(Collection c) {
		final Object[] elementData = array();
		final int len = elementData.length;
		for(final Iterator e = c.iterator(); e.hasNext();) {
			if(indexOf(e.next(), elementData, len) < 0)
				return false;
		}
		return true;
	}
	/**
	 * Removes from this Collection all of its elements that are contained in
	 * the specified Collection. This is a particularly expensive operation
	 * in this class because of the need for an internal temporary array.
	 *
	 * @return true if this Collection changed as a result of the call.
	 */
	public synchronized boolean removeAll(Collection c) {
		final Object[] elementData = _array;
		final int len = elementData.length;
		if(len == 0)
			return false;
		// temp array holds those elements we know we want to keep
		final Object[] temp = new Object[len];
		int newlen = 0;
		for(int i = 0; i < len; ++i) {
			final Object element = elementData[i];
			if(!c.contains(element)) {
				temp[newlen++] = element;
			}
		}
		if(newlen == len)
			return false;
		// copy temp as new array
		final Object[] newArray = new Object[newlen];
		System.arraycopy(temp, 0, newArray, 0, newlen);
		_array = newArray;
		return true;
	}
	/**
	 * Retains only the elements in this Collection that are contained in the
	 * specified Collection (optional operation). In other words, removes from
	 * this Collection all of its elements that are not contained in the
	 * specified Collection.
	 *
	 * @return true if this Collection changed as a result of the call.
	 */
	public synchronized boolean retainAll(Collection c) {
		final Object[] elementData = _array;
		final int len = elementData.length;
		if(len == 0)
			return false;
		final Object[] temp = new Object[len];
		int newlen = 0;
		for(int i = 0; i < len; ++i) {
			final Object element = elementData[i];
			if(c.contains(element)) {
				temp[newlen++] = element;
			}
		}
		if(newlen == len)
			return false;
		final Object[] newArray = new Object[newlen];
		System.arraycopy(temp, 0, newArray, 0, newlen);
		_array = newArray;
		return true;
	}
	/**
	 * Appends all of the elements in the specified Collection that
	 * are not already contained in this list, to the end of
	 * this list, in the order that they are returned by the
	 * specified Collection's Iterator.
	 *
	 * @param c elements to be added into this list.
	 * @return the number of elements added
	 */
	public synchronized int addAllAbsent(Collection c) {
		final int numNew = c.size();
		if(numNew == 0)
			return 0;
		final Object[] elementData = _array;
		final int len = elementData.length;
		final Object[] temp = new Object[numNew];
		int added = 0;
		for(final Iterator e = c.iterator(); e.hasNext();) {
			final Object element = e.next();
			if(indexOf(element, elementData, len) < 0
					&& indexOf(element, temp, added) < 0) {
				temp[added++] = element;
			}
		}
		if(added == 0)
			return 0;
		final Object[] newArray = new Object[len + added];
		System.arraycopy(elementData, 0, newArray, 0, len);
		System.arraycopy(temp, 0, newArray, len, added);
		_array = newArray;
		return added;
	}
	/**
	 * Removes all of the elements from this list.
	 */
	public synchronized void clear() {
		_array = new Object[0];
	}
	/**
	 * Appends all of the elements in the specified Collection to
	 * the end of this list, in the order that they are returned
	 * by the specified Collection's Iterator.
	 *
	 * @param c elements to be inserted into this list.
	 */
	public synchronized boolean addAll(Collection c) {
		final int numNew = c.size();
		if(numNew == 0)
			return false;
		int len = _array.length;
		final Object[] newArray = new Object[len + numNew];
		System.arraycopy(_array, 0, newArray, 0, len);
		final Iterator e = c.iterator();
		for(int i = 0; i < numNew; ++i) {
			newArray[len++] = e.next();
		}
		_array = newArray;
		return true;
	}
	/**
	 * Inserts all of the elements in the specified Collection into this
	 * list, starting at the specified position. Shifts the element
	 * currently at that position (if any) and any subsequent elements to
	 * the right (increases their indices). The new elements will appear
	 * in the list in the order that they are returned by the
	 * specified Collection's iterator.
	 *
	 * @param index index at which to insert first element
	 *		        from the specified collection.
	 * @param c elements to be inserted into this list.
	 * @throws IndexOutOfBoundsException index out of range (index &lt; 0 || index &gt; size()).
	 */
	public synchronized boolean addAll(int index, Collection c) {
		final int len = _array.length;
		if(index > len || index < 0)
			throw new IndexOutOfBoundsException(
					"Index: " + index + ", Size: " + len);
		final int numNew = c.size();
		if(numNew == 0)
			return false;
		final Object[] newArray = new Object[len + numNew];
		System.arraycopy(_array, 0, newArray, 0, len);
		final int numMoved = len - index;
		if(numMoved > 0) {
			System.arraycopy(_array, index, newArray, index + numNew, numMoved);
		}
		final Iterator e = c.iterator();
		for(int i = 0; i < numNew; ++i) {
			newArray[index++] = e.next();
		}
		_array = newArray;
		return true;
	}
	/**
	 * Check if the given index is in range.
	 * If not, throw an appropriate runtime exception.
	 */
	private static void rangeCheck(int index, int length) {
		if(index >= length || index < 0)
			throw new IndexOutOfBoundsException(
					"Index: " + index + ", Size: " + length);
	}
	/**
	 * Save the state of the list to a stream (i.e., serialize it).
	 *
	 * @serialData The length of the array backing the list is emitted
	 *		       (int), followed by all of its elements (each an Object)
	 *		       in the proper order.
	 */
	private void writeObject(ObjectOutputStream s) throws IOException {
		// Write out element count, and any hidden stuff
		s.defaultWriteObject();
		final Object[] elementData = array();
		// Write out array length
		s.writeInt(elementData.length);
		// Write out all elements in the proper order.
		final int len = elementData.length;
		for(int i = 0; i < len; ++i) {
			s.writeObject(elementData[i]);
		}
	}
	/**
	 * Reconstitute the list from a stream (i.e., deserialize it).
	 */
	private synchronized void readObject(ObjectInputStream s)
			throws IOException, ClassNotFoundException {
		// Read in size, and any hidden stuff
		s.defaultReadObject();
		// Read in array length and allocate array
		final int arrayLength = s.readInt();
		final Object[] elementData = new Object[arrayLength];
		// Read in all elements in the proper order.
		final int len = elementData.length;
		for(int i = 0; i < len; ++i) {
			elementData[i] = s.readObject();
		}
		_array = elementData;
	}
	/**
	 * Returns a string representation of this Collection, containing
	 * the String representation of each element.
	 */
	public String toString() {
		final StringBuffer buf = new StringBuffer();
		final Iterator e = iterator();
		buf.append('[');
		final int maxIndex = size() - 1;
		for(int i = 0; i <= maxIndex; ++i) {
			buf.append(String.valueOf(e.next()));
			if(i < maxIndex) {
				buf.append(", ");
			}
		}
		buf.append(']');
		return buf.toString();
	}
	/**
	 * Compares the specified Object with this List for equality. Returns true
	 * if and only if the specified Object is also a List, both Lists have the
	 * same size, and all corresponding pairs of elements in the two Lists are
	 * <em>equal</em>.  (Two elements <code>e1</code> and <code>e2</code> are
	 * <em>equal</em> if <code>(e1 == null ? e2 == null : e1.equals(e2))</code>.)
	 * In other words, two Lists are defined to be equal if they contain the
	 * same elements in the same order.
	 *
	 * This implementation first checks if the specified object is this
	 * List. If so, it returns true; if not, it checks if the specified
	 * object is a List. If not, it returns false; if so, it iterates over
	 * both lists, comparing corresponding pairs of elements. If any
	 * comparison returns false, this method returns false. If either
	 * Iterator runs out of elements before before the other it returns false
	 * (as the Lists are of unequal length); otherwise it returns true when
	 * the iterations complete.
	 *
	 * @param o the Object to be compared for equality with this List.
	 * @return true if the specified Object is equal to this List.
	 */
	public boolean equals(Object o) {
		if(o == this)
			return true;
		if(!(o instanceof List))
			return false;
		final List l2 = (List) o;
		if(size() != l2.size())
			return false;
		final ListIterator e1 = listIterator(), e2 = l2.listIterator();
		while(e1.hasNext()) {
			final Object o1 = e1.next(), o2 = e2.next();
			if(!(o1 == null ? o2 == null : o1.equals(o2)))
				return false;
		}
		return true;
	}
	/**
	 * Returns the hash code value for this List.
	 *
	 * This implementation uses exactly the code that is used to define
	 * the List hash function in the documentation for List.hashCode.
	 */
	public int hashCode() {
		int hashCode = 1;
		for(final Iterator i = iterator(); i.hasNext();) {
			final Object obj = i.next();
			hashCode = 31 * hashCode + (obj == null ? 0 : obj.hashCode());
		}
		return hashCode;
	}
	/**
	 * Returns an Iterator over the elements contained in this collection.
	 * The iterator provides a snapshot of the state of the list
	 * when the iterator was constructed. No synchronization is
	 * needed while traversing the iterator. The iterator does
	 * <em>NOT</em> support the <code>remove</code> method.
	 */
	public Iterator iterator() {
		return new COWIterator(array(), 0);
	}
	/**
	 * Returns an Iterator of the elements in this List (in proper sequence).
	 * The iterator provides a snapshot of the state of the list
	 * when the iterator was constructed. No synchronization is
	 * needed while traversing the iterator. The iterator does
	 * <em>NOT</em> support the <code>remove</code>, <code>set</code>,
	 * or <code>add</code> methods.
	 */
	public ListIterator listIterator() {
		return new COWIterator(array(), 0);
	}
	/**
	 * Returns a ListIterator of the elements in this List (in proper
	 * sequence), starting at the specified position in the List. The
	 * specified index indicates the first element that would be returned by
	 * an initial call to nextElement. An initial call to previousElement
	 * would return the element with the specified index minus one.
	 * The ListIterator returned by this implementation will throw
	 * an UnsupportedOperationException in its remove, set and
	 * add methods.
	 *
	 * @param index index of first element to be returned from the
	 *		        ListIterator (by a call to getNext).
	 * @throws IndexOutOfBoundsException index is out of range (index &lt; 0 || index &gt; size()).
	 */
	public ListIterator listIterator(int index) {
		final Object[] elementData = array();
		final int len = elementData.length;
		if(index < 0 || index > len)
			throw new IndexOutOfBoundsException("Index: " + index);
		return new COWIterator(array(), index);
	}
	private static final class COWIterator implements ListIterator {
		/**
		 * Snapshot of the array
		 */
		private final Object[] _array;
		/**
		 * Index of element to be returned by subsequent call to next.
		 */
		private int _cursor;
		private COWIterator(Object[] elementArray, int initialCursor) {
			_array = elementArray;
			_cursor = initialCursor;
		}
		public boolean hasNext() {
			return _cursor < _array.length;
		}
		public boolean hasPrevious() {
			return _cursor > 0;
		}
		public Object next() {
			try {
				return _array[_cursor++];
			}
			catch(final IndexOutOfBoundsException ex) {
				throw new NoSuchElementException();
			}
		}
		public Object previous() {
			try {
				return _array[--_cursor];
			}
			catch(final IndexOutOfBoundsException e) {
				throw new NoSuchElementException();
			}
		}
		public int nextIndex() {
			return _cursor;
		}
		public int previousIndex() {
			return _cursor - 1;
		}
		/**
		 * Not supported. Always throws UnsupportedOperationException.
		 * @throws UnsupportedOperationException remove is not supported by this Iterator.
		 */
		public void remove() {
			throw new UnsupportedOperationException();
		}
		/**
		 * Not supported. Always throws UnsupportedOperationException.
		 * @throws UnsupportedOperationException set is not supported by this Iterator.
		 */
		public void set(Object o) {
			throw new UnsupportedOperationException();
		}
		/**
		 * Not supported. Always throws UnsupportedOperationException.
		 * @throws UnsupportedOperationException add is not supported by this Iterator.
		 */
		public void add(Object o) {
			throw new UnsupportedOperationException();
		}
	}
	/**
	 * Returns a view of the portion of this List between fromIndex,
	 * inclusive, and toIndex, exclusive. The returned List is backed by this
	 * List, so changes in the returned List are reflected in this List, and
	 * vice-versa. While mutative operations are supported, they are probably
	 * not very useful for CopyOnWriteArrays.
	 *
	 * The semantics of the List returned by this method become undefined if
	 * the backing list (i.e., this List) is <i>structurally modified</i> in
	 * any way other than via the returned List. (Structural modifications are
	 * those that change the size of the List, or otherwise perturb it in such
	 * a fashion that iterations in progress may yield incorrect results.)
	 *
	 * @param fromIndex low endpoint (inclusive) of the subList.
	 * @param toIndex high endpoint (exclusive) of the subList.
	 * @return a view of the specified range within this List.
	 * @throws IndexOutOfBoundsException Illegal endpoint index value
	 *     (fromIndex &lt; 0 || toIndex &gt; size || fromIndex &gt; toIndex).
	 */
	public synchronized List subList(int fromIndex, int toIndex) {
		// synchronized since sublist ctor depends on it.
		final int len = _array.length;
		if(fromIndex < 0 || toIndex > len || fromIndex > toIndex)
			throw new IndexOutOfBoundsException();
		return new COWSubList(this, fromIndex, toIndex);
	}
	private static final class COWSubList extends AbstractList {
		/*
		 * This is currently a bit sleazy. The class
		 * extends AbstractList merely for convenience,
		 * to avoid having to define addAll, etc. This
		 * doesn't hurt, but is stupid and wasteful.
		 * This class does not need or use modCount mechanics
		 * in AbstractList, but does need to check for
		 * concurrent modification using similar mechanics.
		 * On each operation, the array that we expect
		 * the backing list to use is checked and updated.
		 * Since we do this for all of the base operations
		 * invoked by those defined in AbstractList, all is well.
		 *
		 * It's not clear whether this is worth cleaning up.
		 * The kinds of list operations inherited from
		 * AbstractList are are already so slow on COW sublists
		 * that adding a bit more space/time doesn't seem
		 * even noticeable.
		 */
		private final CopyOnWriteArrayList _list;
		private final int _offset;
		private int _size;
		private Object[] _expectedArray;
		private COWSubList(CopyOnWriteArrayList list, int fromIndex,
				int toIndex) {
			_list = list;
			_expectedArray = _list.array();
			_offset = fromIndex;
			_size = toIndex - fromIndex;
		}
		// only call this holding l's lock
		private void checkForComodification() {
			if(_list._array != _expectedArray)
				throw new ConcurrentModificationException();
		}
		// only call this holding l's lock
		private void rangeCheck(int index) {
			if(index < 0 || index >= _size)
				throw new IndexOutOfBoundsException(
						"Index: " + index + ",Size: " + _size);
		}
		public Object set(int index, Object element) {
			synchronized(_list) {
				rangeCheck(index);
				checkForComodification();
				final Object x = _list.set(index + _offset, element);
				_expectedArray = _list._array;
				return x;
			}
		}
		public Object get(int index) {
			synchronized(_list) {
				rangeCheck(index);
				checkForComodification();
				return _list.get(index + _offset);
			}
		}
		public int size() {
			synchronized(_list) {
				checkForComodification();
				return _size;
			}
		}
		public void add(int index, Object element) {
			synchronized(_list) {
				checkForComodification();
				if(index < 0 || index > _size)
					throw new IndexOutOfBoundsException();
				_list.add(index + _offset, element);
				_expectedArray = _list._array;
				_size++;
			}
		}
		public Object remove(int index) {
			synchronized(_list) {
				rangeCheck(index);
				checkForComodification();
				final Object result = _list.remove(index + _offset);
				_expectedArray = _list._array;
				_size--;
				return result;
			}
		}
		public Iterator iterator() {
			synchronized(_list) {
				checkForComodification();
				return new COWSubListIterator(0);
			}
		}
		public ListIterator listIterator(int index) {
			synchronized(_list) {
				checkForComodification();
				if(index < 0 || index > _size)
					throw new IndexOutOfBoundsException(
							"index: " + index + ", size: " + _size);
				return new COWSubListIterator(index);
			}
		}
		private final class COWSubListIterator implements ListIterator {
			private final ListIterator _i;
			private COWSubListIterator(int index) {
				_i = _list.listIterator(index + _offset);
			}
			public boolean hasNext() {
				return nextIndex() < _size;
			}
			public Object next() {
				if(hasNext())
					return _i.next();
				throw new NoSuchElementException();
			}
			public boolean hasPrevious() {
				return previousIndex() >= 0;
			}
			public Object previous() {
				if(hasPrevious())
					return _i.previous();
				throw new NoSuchElementException();
			}
			public int nextIndex() {
				return _i.nextIndex() - _offset;
			}
			public int previousIndex() {
				return _i.previousIndex() - _offset;
			}
			public void remove() {
				throw new UnsupportedOperationException();
			}
			public void set(Object o) {
				throw new UnsupportedOperationException();
			}
			public void add(Object o) {
				throw new UnsupportedOperationException();
			}
		}
		public List subList(int fromIndex, int toIndex) {
			synchronized(_list) {
				checkForComodification();
				if(fromIndex < 0 || toIndex > _size)
					throw new IndexOutOfBoundsException();
				return new COWSubList(_list, fromIndex + _offset,
						toIndex + _offset);
			}
		}
	}
}