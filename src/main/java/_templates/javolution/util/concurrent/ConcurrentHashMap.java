/*
 * File: ConcurrentHashMap
 *
 * Written by Doug Lea. Adapted and released, under explicit
 * permission, from JDK1.2 HashMap.java and Hashtable.java which
 * carries the following copyright:
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
 * 26nov2000    dl             Created, based on ConcurrentReaderHashMap
 * 12jan2001    dl             public release
 * 17nov2001    dl             Minor tunings
 * 24oct2003    dl             Segment implements Serializable
 */
package _templates.javolution.util.concurrent;
import java.io.IOException;
import java.util.Enumeration;
import java.util.NoSuchElementException;
import _templates.java.io.ObjectInputStream;
import _templates.java.io.ObjectOutputStream;
import _templates.java.io.Serializable;
import _templates.java.lang.Cloneable;
import _templates.java.util.AbstractCollection;
import _templates.java.util.AbstractMap;
import _templates.java.util.AbstractSet;
import _templates.java.util.Collection;
import _templates.java.util.Iterator;
import _templates.java.util.Map;
import _templates.java.util.Set;
/**
 * A version of Hashtable supporting
 * concurrency for both retrievals and updates:
 *
 * <dl>
 * <dt> Retrievals
 *
 * <dd> Retrievals may overlap updates.  (This is the same policy as
 * ConcurrentReaderHashMap.)  Successful retrievals using get(key) and
 * containsKey(key) usually run without locking. Unsuccessful
 * retrievals (i.e., when the key is not present) do involve brief
 * synchronization (locking).  Because retrieval operations can
 * ordinarily overlap with update operations (i.e., put, remove, and
 * their derivatives), retrievals can only be guaranteed to return the
 * results of the most recently <em>completed</em> operations holding
 * upon their onset. Retrieval operations may or may not return
 * results reflecting in-progress writing operations.  However, the
 * retrieval operations do always return consistent results -- either
 * those holding before any single modification or after it, but never
 * a nonsense result.  For aggregate operations such as putAll and
 * clear, concurrent reads may reflect insertion or removal of only
 * some entries.
 * <p>
 *
 * Iterators and Enumerations (i.e., those returned by
 * keySet().iterator(), entrySet().iterator(), values().iterator(),
 * keys(), and elements()) return elements reflecting the state of the
 * hash table at some point at or since the creation of the
 * iterator/enumeration.  They will return at most one instance of
 * each element (via next()/nextElement()), but might or might not
 * reflect puts and removes that have been processed since they were
 * created.  They do <em>not</em> throw ConcurrentModificationException.
 * However, these iterators are designed to be used by only one
 * thread at a time. Passing an iterator across multiple threads may
 * lead to unpredictable results if the table is being concurrently
 * modified.  <p>
 *
 *
 * <dt> Updates
 *
 * <dd> This class supports a hard-wired preset <em>concurrency
 * level</em> of 32. This allows a maximum of 32 put and/or remove
 * operations to proceed concurrently. This level is an upper bound on
 * concurrency, not a guarantee, since it interacts with how
 * well-strewn elements are across bins of the table. (The preset
 * value in part reflects the fact that even on large multiprocessors,
 * factors other than synchronization tend to be bottlenecks when more
 * than 32 threads concurrently attempt updates.)
 * Additionally, operations triggering internal resizing and clearing
 * do not execute concurrently with any operation.
 * <p>
 *
 * There is <em>NOT</em> any support for locking the entire table to
 * prevent updates. This makes it imposssible, for example, to
 * add an element only if it is not already present, since another
 * thread may be in the process of doing the same thing.
 * If you need such capabilities, consider instead using the
 * ConcurrentReaderHashMap class.
 *
 * </dl>
 *
 * Because of how concurrency control is split up, the
 * size() and isEmpty() methods require accumulations across 32
 * control segments, and so might be slightly slower than you expect.
 * <p>
 *
 * This class may be used as a direct replacement for
 * java.util.Hashtable in any application that does not rely
 * on the ability to lock the entire table to prevent updates.
 * As of this writing, it performs much faster than Hashtable in
 * typical multi-threaded applications with multiple readers and writers.
 * Like Hashtable but unlike java.util.HashMap,
 * this class does NOT allow <tt>null</tt> to be used as a key or
 * value.
 * <p>
 *
 * Implementation note: A slightly
 * faster implementation of this class will be possible once planned
 * Java Memory Model revisions are in place.
 *
 * <p>[ <a href="//gee.cs.oswego.edu/dl/classes/EDU/oswego/cs/dl/util/concurrent/intro.html"> Introduction to this package.</a> ]</p>
 */
public class ConcurrentHashMap extends AbstractMap
		implements Map, Cloneable, Serializable {
	/*
	 * The basic strategy is an optimistic-style scheme based on
	 * the guarantee that the hash table and its lists are always
	 * kept in a consistent enough state to be read without locking:
	 *
	 * Read operations first proceed without locking, by traversing the
	 * apparently correct list of the apparently correct bin. If an
	 * entry is found, but not invalidated (value field null), it is
	 * returned. If not found, operations must recheck (after a memory
	 * barrier) to make sure they are using both the right list and
	 * the right table (which can change under resizes). If
	 * invalidated, reads must acquire main update lock to wait out
	 * the update, and then re-traverse.
	 *
	 * All list additions are at the front of each bin, making it easy
	 * to check changes, and also fast to traverse.  Entry next
	 * pointers are never assigned. Remove() builds new nodes when
	 * necessary to preserve this.
	 *
	 * Remove() (also clear()) invalidates removed nodes to alert read
	 * operations that they must wait out the full modifications.
	 *
	 * Locking for puts, removes (and, when necessary gets, etc)
	 * is controlled by Segments, each covering a portion of the
	 * table. During operations requiring global exclusivity (mainly
	 * resize and clear), ALL of these locks are acquired at once.
	 * Note that these segments are NOT contiguous -- they are based
	 * on the least 5 bits of hashcodes. This ensures that the same
	 * segment controls the same slots before and after resizing, which
	 * is necessary for supporting concurrent retrievals. This
	 * comes at the price of a mismatch of logical vs physical locality,
	 * but this seems not to be a performance problem in practice.
	 */
	/**
	 * The number of concurrency control segments.
	 * The value can be at most 32 since ints are used
	 * as bitsets over segments. Emprically, it doesn't
	 * seem to pay to decrease it either, so the value should be at least 32.
	 * In other words, do not redefine this :-)
	 */
	private static final int CONCURRENCY_LEVEL = 32;
	/**
	 * Mask value for indexing into segments
	 */
	private static final int SEGMENT_MASK = CONCURRENCY_LEVEL - 1;
	/**
	 * The minimum capacity, used if a lower value is implicitly specified
	 * by either of the constructors with arguments.
	 * MUST be a power of two.
	 */
	private static final int MINIMUM_CAPACITY = 32;
	/**
	 * The maximum capacity, used if a higher value is implicitly specified
	 * by either of the constructors with arguments.
	 * MUST be a power of two <= 1<<30.
	 */
	private static final int MAXIMUM_CAPACITY = 1 << 30;
	/**
	 * The default initial number of table slots for this table (32).
	 * Used when not otherwise specified in constructor.
	 */
	public static final int DEFAULT_INITIAL_CAPACITY = 32;
	/**
	 * The default load factor for this table (0.75)
	 * Used when not otherwise specified in constructor.
	 */
	public static final float DEFAULT_LOAD_FACTOR = 0.75f;
	/**
	 * Bookkeeping for each concurrency control segment.
	 * Each segment contains a local count of the number of
	 * elements in its region.
	 * However, the main use of a Segment is for its lock.
	 */
	private static final class Segment implements Serializable {
		/**
		 * The number of elements in this segment's region.
		 * It is always updated within synchronized blocks.
		 */
		private int _count;
		/**
		 * Get the count under synch.
		 */
		private synchronized int getCount() {
			return _count;
		}
		/**
		 * Force a synchronization
		 */
		private synchronized void synch() {}
	}
	/**
	 * The hash table data.
	 */
	private transient Entry[] _table;
	/**
	 * The array of concurrency control segments.
	 */
	private final Segment[] _segments = new Segment[CONCURRENCY_LEVEL];
	/**
	 * The load factor for the hash table.
	 *
	 * @serial
	 */
	private final float _loadFactor;
	/**
	 * Per-segment resize threshold.
	 *
	 * @serial
	 */
	private int _threshold;
	/**
	 * Number of segments voting for resize. The table is
	 * doubled when 1/4 of the segments reach threshold.
	 * Volatile but updated without synch since this is just a heuristic.
	 */
	private transient volatile int _votesForResize;
	/**
	 * Return the number of set bits in w.
	 * For a derivation of this algorithm, see
	 * "Algorithms and data structures with applications to
	 *  graphics and geometry", by Jurg Nievergelt and Klaus Hinrichs,
	 *  Prentice Hall, 1993.
	 * See also notes by Torsten Sillke at
	 * http://www.mathematik.uni-bielefeld.de/~sillke/PROBLEMS/bitcount
	 */
	private static int bitcount(int w) {
		w -= (0xAAAAAAAA & w) >>> 1;
		w = (w & 0x33333333) + (w >>> 2 & 0x33333333);
		w = w + (w >>> 4) & 0x0F0F0F0F;
		w += w >>> 8;
		w += w >>> 16;
		return w & 0xFF;
	}
	/**
	 * Returns the appropriate capacity (power of two) for the specified
	 * initial capacity argument.
	 */
	private static int p2capacity(int initialCapacity) {
		final int cap = initialCapacity;
		// Compute the appropriate capacity
		int result;
		if(cap > MAXIMUM_CAPACITY || cap < 0) {
			result = MAXIMUM_CAPACITY;
		}
		else {
			result = MINIMUM_CAPACITY;
			while(result < cap) {
				result <<= 1;
			}
		}
		return result;
	}
	/**
	 * Return hash code for Object x. Since we are using power-of-two
	 * tables, it is worth the effort to improve hashcode via
	 * the same multiplicative scheme as used in IdentityHashMap.
	 */
	private static int hash(Object x) {
		final int h = x.hashCode();
		// Multiply by 127 (quickly, via shifts), and mix in some high
		// bits to help guard against bunching of codes that are
		// consecutive or equally spaced.
		return (h << 7) - h + (h >>> 9) + (h >>> 17);
	}
	/**
	 * Check for equality of non-null references x and y.
	 */
	private static boolean eq(Object x, Object y) {
		return x == y || x.equals(y);
	}
	/**
	 * Create table array and set the per-segment threshold
	 */
	private final Entry[] newTable(int capacity) {
		_threshold = (int) (capacity * _loadFactor / CONCURRENCY_LEVEL) + 1;
		return new Entry[capacity];
	}
	/**
	 * Constructs a new, empty map with the specified initial
	 * capacity and the specified load factor.
	 *
	 * @param initialCapacity the initial capacity.
	 *  The actual initial capacity is rounded to the nearest power of two.
	 * @param loadFactor  the load factor threshold, used to control resizing.
	 *   This value is used in an approximate way: When at least
	 *   a quarter of the segments of the table reach per-segment threshold, or
	 *   one of the segments itself exceeds overall threshold,
	 *   the table is doubled.
	 *   This will on average cause resizing when the table-wide
	 *   load factor is slightly less than the threshold. If you'd like
	 *   to avoid resizing, you can set this to a ridiculously large
	 *   value.
	 * @throws IllegalArgumentException  if the load factor is nonpositive.
	 */
	public ConcurrentHashMap(int initialCapacity, float loadFactor) {
		if(loadFactor < 0)
			throw new IllegalArgumentException(
					"Illegal Load factor: " + loadFactor);
		_loadFactor = loadFactor;
		final int len = _segments.length;
		for(int i = 0; i < len; ++i) {
			_segments[i] = new Segment();
		}
		final int cap = p2capacity(initialCapacity);
		_table = newTable(cap);
	}
	/**
	 * Constructs a new, empty map with the specified initial
	 * capacity and default load factor.
	 *
	 * @param   initialCapacity   the initial capacity of the
	 *                            ConcurrentHashMap.
	 * @throws    IllegalArgumentException if the initial maximum number
	 *              of elements is less
	 *              than zero.
	 */
	public ConcurrentHashMap(int initialCapacity) {
		this(initialCapacity, DEFAULT_LOAD_FACTOR);
	}
	/**
	 * Constructs a new, empty map with a default initial capacity
	 * and default load factor.
	 */
	public ConcurrentHashMap() {
		this(DEFAULT_INITIAL_CAPACITY, DEFAULT_LOAD_FACTOR);
	}
	/**
	 * Constructs a new map with the same mappings as the given map.  The
	 * map is created with a capacity of twice the number of mappings in
	 * the given map or 32 (whichever is greater), and a default load factor.
	 */
	public ConcurrentHashMap(Map t) {
		this(Math.max((int) (t.size() / DEFAULT_LOAD_FACTOR) + 1,
				MINIMUM_CAPACITY), DEFAULT_LOAD_FACTOR);
		putAll(t);
	}
	/**
	 * Returns the number of key-value mappings in this map.
	 *
	 * @return the number of key-value mappings in this map.
	 */
	public int size() {
		int c = 0;
		final int len = _segments.length;
		for(int i = 0; i < len; ++i) {
			c += _segments[i].getCount();
		}
		return c;
	}
	/**
	 * Returns <tt>true</tt> if this map contains no key-value mappings.
	 *
	 * @return <tt>true</tt> if this map contains no key-value mappings.
	 */
	public boolean isEmpty() {
		final int len = _segments.length;
		for(int i = 0; i < len; ++i) {
			if(_segments[i].getCount() != 0)
				return false;
		}
		return true;
	}
	/**
	 * Returns the value to which the specified key is mapped in this table.
	 *
	 * @param   key   a key in the table.
	 * @return  the value to which the key is mapped in this table;
	 *          <code>null</code> if the key is not mapped to any value in
	 *          this table.
	 * @throws  NullPointerException  if the key is
	 *               <code>null</code>.
	 * @see     #put(Object, Object)
	 */
	public Object get(Object key) {
		final int hash = hash(key); // throws null pointer exception if key null
		// Try first without locking...
		Entry[] tab = _table;
		int index = hash & tab.length - 1;
		final Entry first = tab[index];
		Entry e;
		for(e = first; e != null; e = e._next) {
			if(e._hash == hash && eq(key, e._key)) {
				final Object value = e._value;
				if(value != null)
					return value;
				break;
			}
		}
		// Recheck under synch if key apparently not there or interference
		final Segment seg = _segments[hash & SEGMENT_MASK];
		synchronized(seg) {
			tab = _table;
			index = hash & tab.length - 1;
			final Entry newFirst = tab[index];
			if(e != null || first != newFirst) {
				for(e = newFirst; e != null; e = e._next) {
					if(e._hash == hash && eq(key, e._key))
						return e._value;
				}
			}
			return null;
		}
	}
	/**
	 * Tests if the specified object is a key in this table.
	 *
	 * @param   key   possible key.
	 * @return  <code>true</code> if and only if the specified object
	 *          is a key in this table, as determined by the
	 *          <tt>equals</tt> method; <code>false</code> otherwise.
	 * @throws  NullPointerException  if the key is
	 *               <code>null</code>.
	 * @see     #contains(Object)
	 */
	public boolean containsKey(Object key) {
		return get(key) != null;
	}
	/**
	 * Maps the specified <code>key</code> to the specified
	 * <code>value</code> in this table. Neither the key nor the
	 * value can be <code>null</code>. (Note that this policy is
	 * the same as for java.util.Hashtable, but unlike java.util.HashMap,
	 * which does accept nulls as valid keys and values.)<p>
	 *
	 * The value can be retrieved by calling the <code>get</code> method
	 * with a key that is equal to the original key.
	 *
	 * @param     key     the table key.
	 * @param     value   the value.
	 * @return    the previous value of the specified key in this table,
	 *            or <code>null</code> if it did not have one.
	 * @throws    NullPointerException  if the key or value is
	 *               <code>null</code>.
	 * @see     Object#equals(Object)
	 * @see     #get(Object)
	 */
	public Object put(Object key, Object value) {
		if(value == null)
			throw new NullPointerException();
		final int hash = hash(key);
		final Segment seg = _segments[hash & SEGMENT_MASK];
		int segcount;
		Entry[] tab;
		int votes;
		synchronized(seg) {
			tab = _table;
			final int index = hash & tab.length - 1;
			final Entry first = tab[index];
			for(Entry e = first; e != null; e = e._next) {
				if(e._hash == hash && eq(key, e._key)) {
					final Object oldValue = e._value;
					e._value = value;
					return oldValue;
				}
			}
			//  Add to front of list
			final Entry newEntry = new Entry(hash, key, value, first);
			tab[index] = newEntry;
			if((segcount = ++seg._count) < _threshold)
				return null;
			final int bit = 1 << (hash & SEGMENT_MASK);
			votes = _votesForResize;
			if((votes & bit) == 0) {
				votes = _votesForResize |= bit;
			}
		}
		// Attempt resize if 1/4 segs vote,
		// or if this seg itself reaches the overall threshold.
		// (The latter check is just a safeguard to avoid pathological cases.)
		if(bitcount(votes) >= CONCURRENCY_LEVEL / 4
				|| segcount > _threshold * CONCURRENCY_LEVEL) {
			resize(0, tab);
		}
		return null;
	}
	/**
	 * Gather all locks in order to call rehash, by
	 * recursing within synch blocks for each segment index.
	 * @param index the current segment. initially call value must be 0
	 * @param assumedTab the state of table on first call to resize. If
	 * this changes on any call, the attempt is aborted because the
	 * table has already been resized by another thread.
	 */
	private void resize(int index, Entry[] assumedTab) {
		final Segment seg = _segments[index];
		synchronized(seg) {
			if(assumedTab == _table) {
				final int next = index + 1;
				if(next < _segments.length) {
					resize(next, assumedTab);
				}
				else {
					rehash();
				}
			}
		}
	}
	/**
	 * Rehashes the contents of this map into a new table
	 * with a larger capacity.
	 */
	private void rehash() {
		_votesForResize = 0; // reset
		final Entry[] oldTable = _table;
		final int oldCapacity = oldTable.length;
		if(oldCapacity >= MAXIMUM_CAPACITY) {
			_threshold = Integer.MAX_VALUE; // avoid retriggering
			return;
		}
		final int newCapacity = oldCapacity << 1;
		final Entry[] newTable = newTable(newCapacity);
		final int mask = newCapacity - 1;
		/*
		 * Reclassify nodes in each list to new Map.  Because we are
		 * using power-of-two expansion, the elements from each bin
		 * must either stay at same index, or move to
		 * oldCapacity+index. We also eliminate unnecessary node
		 * creation by catching cases where old nodes can be reused
		 * because their next fields won't change. Statistically, at
		 * the default threshhold, only about one-sixth of them need
		 * cloning. (The nodes they replace will be garbage
		 * collectable as soon as they are no longer referenced by any
		 * reader thread that may be in the midst of traversing table
		 * right now.)
		 */
		for(int i = 0; i < oldCapacity; ++i) {
			// We need to guarantee that any existing reads of old Map can
			//  proceed. So we cannot yet null out each bin.
			final Entry e = oldTable[i];
			if(e != null) {
				final int idx = e._hash & mask;
				final Entry next = e._next;
				//  Single node on list
				if(next == null) {
					newTable[idx] = e;
				}
				else {
					// Reuse trailing consecutive sequence of all same bit
					Entry lastRun = e;
					int lastIdx = idx;
					for(Entry last = next; last != null; last = last._next) {
						final int k = last._hash & mask;
						if(k != lastIdx) {
							lastIdx = k;
							lastRun = last;
						}
					}
					newTable[lastIdx] = lastRun;
					// Clone all remaining nodes
					for(Entry p = e; p != lastRun; p = p._next) {
						final int k = p._hash & mask;
						newTable[k] = new Entry(p._hash, p._key, p._value,
								newTable[k]);
					}
				}
			}
		}
		_table = newTable;
	}
	/**
	 * Removes the key (and its corresponding value) from this
	 * table. This method does nothing if the key is not in the table.
	 *
	 * @param   key   the key that needs to be removed.
	 * @return  the value to which the key had been mapped in this table,
	 *          or <code>null</code> if the key did not have a mapping.
	 * @throws  NullPointerException  if the key is
	 *               <code>null</code>.
	 */
	public Object remove(Object key) {
		return remove(key, hash(key), null);
	}
	/**
	 * {@inheritDoc}
	 *
	 * @throws NullPointerException if the specified key is null
	 */
	public boolean remove(Object key, Object value) {
		return value != null && remove(key, hash(key), value) != null;
	}
	/**
	 * Removes the (key, value) pair from this
	 * table. This method does nothing if the key is not in the table,
	 * or if the key is associated with a different value. This method
	 * is needed by EntrySet.
	 *
	 * @param   key   the key that needs to be removed.
	 * @param   value   the associated value. If the value is null,
	 *                   it means "any value".
	 * @return  the value to which the key had been mapped in this table,
	 *          or <code>null</code> if the key did not have a mapping.
	 * @throws  NullPointerException  if the key is
	 *               <code>null</code>.
	 */
	private Object remove(Object key, int hash, Object value) {
		/*
		  Find the entry, then
		1. Set value field to null, to force get() to retry
		2. Rebuild the list without this entry.
		   All entries following removed node can stay in list, but
		   all preceeding ones need to be cloned.  Traversals rely
		   on this strategy to ensure that elements will not be
		  repeated during iteration.
		*/
		final Segment seg = _segments[hash & SEGMENT_MASK];
		synchronized(seg) {
			final Entry[] tab = _table;
			final int index = hash & tab.length - 1;
			final Entry first = tab[index];
			Entry prev = null;
			Entry e = first;
			for(;;) {
				if(e == null)
					return null;
				if(e._hash == hash && eq(key, e._key)) {
					break;
				}
				prev = e;
				e = e._next;
			}
			final Object oldValue = e._value;
			if(value != null && !value.equals(oldValue))
				return null;
			e._value = null;
			if(prev == null) {
				tab[index] = e._next;
			}
			else {
				prev._next = e._next;
			}
			--seg._count;
			return oldValue;
		}
	}
	/**
	 * Returns <tt>true</tt> if this map maps one or more keys to the
	 * specified value. Note: This method requires a full internal
	 * traversal of the hash table, and so is much slower than
	 * method <tt>containsKey</tt>.
	 *
	 * @param value value whose presence in this map is to be tested.
	 * @return <tt>true</tt> if this map maps one or more keys to the
	 * specified value.
	 * @throws  NullPointerException  if the value is <code>null</code>.
	 */
	public boolean containsValue(Object value) {
		if(value == null)
			throw new NullPointerException();
		final int length = _segments.length;
		for(int s = 0; s < length; ++s) {
			final Segment seg = _segments[s];
			Entry[] tab;
			synchronized(seg) {
				tab = _table;
			}
			final int len = tab.length;
			for(int i = s; i < len; i += length) {
				for(Entry e = tab[i]; e != null; e = e._next) {
					if(value.equals(e._value))
						return true;
				}
			}
		}
		return false;
	}
	/**
	 * Tests if some key maps into the specified value in this table.
	 * This operation is more expensive than the <code>containsKey</code>
	 * method.<p>
	 *
	 * Note that this method is identical in functionality to containsValue,
	 * (which is part of the Map interface in the collections framework).
	 *
	 * @param      value   a value to search for.
	 * @return     <code>true</code> if and only if some key maps to the
	 *             <code>value</code> argument in this table as
	 *             determined by the <tt>equals</tt> method;
	 *             <code>false</code> otherwise.
	 * @throws  NullPointerException  if the value is <code>null</code>.
	 * @see        #containsKey(Object)
	 * @see        #containsValue(Object)
	 * @see	   Map
	 */
	public boolean contains(Object value) {
		return containsValue(value);
	}
	/**
	 * Copies all of the mappings from the specified map to this one.
	 *
	 * These mappings replace any mappings that this map had for any of the
	 * keys currently in the specified Map.
	 *
	 * @param t Mappings to be stored in this map.
	 */
	public void putAll(Map t) {
		final int n = t.size();
		if(n == 0)
			return;
		// Expand enough to hold at least n elements without resizing.
		// We can only resize table by factor of two at a time.
		// It is faster to rehash with fewer elements, so do it now.
		for(;;) {
			Entry[] tab;
			int max;
			synchronized(_segments[0]) { // must synch on some segment. pick 0.
				tab = _table;
				max = _threshold * CONCURRENCY_LEVEL;
			}
			if(n < max) {
				break;
			}
			resize(0, tab);
		}
		for(final Iterator it = t.entrySet().iterator(); it.hasNext();) {
			final Map.Entry entry = (Map.Entry) it.next();
			put(entry.getKey(), entry.getValue());
		}
	}
	/**
	 * Removes all mappings from this map.
	 */
	public void clear() {
		// We don't need all locks at once so long as locks
		//   are obtained in low to high order
		final int len = _segments.length;
		for(int s = 0; s < len; ++s) {
			final Segment seg = _segments[s];
			synchronized(seg) {
				final Entry[] tab = _table;
				final int length = tab.length;
				for(int i = s; i < length; i += len) {
					for(Entry e = tab[i]; e != null; e = e._next) {
						e._value = null;
					}
					tab[i] = null;
					seg._count = 0;
				}
			}
		}
	}
	/**
	 * Returns a shallow copy of this
	 * <tt>ConcurrentHashMap</tt> instance: the keys and
	 * values themselves are not cloned.
	 *
	 * @return a shallow copy of this map.
	 */
	public Object clone() {
		// We cannot call super.clone, since it would share final segments array,
		// and there's no way to reassign finals.
		return new ConcurrentHashMap(this);
	}
	// Views
	private transient Set keySet;
	private transient Set entrySet;
	private transient Collection values;
	/**
	 * Returns a set view of the keys contained in this map.  The set is
	 * backed by the map, so changes to the map are reflected in the set, and
	 * vice-versa.  The set supports element removal, which removes the
	 * corresponding mapping from this map, via the <tt>Iterator.remove</tt>,
	 * <tt>Set.remove</tt>, <tt>removeAll</tt>, <tt>retainAll</tt>, and
	 * <tt>clear</tt> operations.  It does not support the <tt>add</tt> or
	 * <tt>addAll</tt> operations.
	 *
	 * @return a set view of the keys contained in this map.
	 */
	public Set keySet() {
		final Set ks = keySet;
		return ks != null ? ks : (keySet = new KeySet());
	}
	/**
	 * Returns a {@link KeySetView} backing a new {@link ConcurrentHashMap}.
	 */
	public static KeySetView newKeySet() {
		return new KeySetView(new ConcurrentHashMap(), Boolean.TRUE);
	}
	/**
	 * Returns a {@link KeySetView} backing a new {@link ConcurrentHashMap} of the given capacity.
	 */
	public static KeySetView newKeySet(int initialCapacity) {
		return new KeySetView(new ConcurrentHashMap(initialCapacity),
				Boolean.TRUE);
	}
	/**
	 * Returns a {@link KeySetView} backing this map.
	 */
	public KeySetView keySet(Object mappedValue) {
		if(mappedValue == null)
			throw new NullPointerException();
		return new KeySetView(this, mappedValue);
	}
	private final class KeySet extends AbstractSet {
		public Iterator iterator() {
			return new KeyIterator();
		}
		public int size() {
			return ConcurrentHashMap.this.size();
		}
		public boolean contains(Object o) {
			return containsKey(o);
		}
		public boolean remove(Object o) {
			return ConcurrentHashMap.this.remove(o) != null;
		}
		public void clear() {
			ConcurrentHashMap.this.clear();
		}
	}
	/**
	 * Returns a collection view of the values contained in this map.  The
	 * collection is backed by the map, so changes to the map are reflected in
	 * the collection, and vice-versa.  The collection supports element
	 * removal, which removes the corresponding mapping from this map, via the
	 * <tt>Iterator.remove</tt>, <tt>Collection.remove</tt>,
	 * <tt>removeAll</tt>, <tt>retainAll</tt>, and <tt>clear</tt> operations.
	 * It does not support the <tt>add</tt> or <tt>addAll</tt> operations.
	 *
	 * @return a collection view of the values contained in this map.
	 */
	public Collection values() {
		final Collection vs = values;
		return vs != null ? vs : (values = new Values());
	}
	private final class Values extends AbstractCollection
			implements Collection {
		public Iterator iterator() {
			return new ValueIterator();
		}
		public int size() {
			return ConcurrentHashMap.this.size();
		}
		public boolean contains(Object o) {
			return containsValue(o);
		}
		public void clear() {
			ConcurrentHashMap.this.clear();
		}
	}
	/**
	 * Returns a collection view of the mappings contained in this map.  Each
	 * element in the returned collection is a <tt>Map.Entry</tt>.  The
	 * collection is backed by the map, so changes to the map are reflected in
	 * the collection, and vice-versa.  The collection supports element
	 * removal, which removes the corresponding mapping from the map, via the
	 * <tt>Iterator.remove</tt>, <tt>Collection.remove</tt>,
	 * <tt>removeAll</tt>, <tt>retainAll</tt>, and <tt>clear</tt> operations.
	 * It does not support the <tt>add</tt> or <tt>addAll</tt> operations.
	 *
	 * @return a collection view of the mappings contained in this map.
	 */
	public Set entrySet() {
		final Set es = entrySet;
		return es != null ? es : (entrySet = new EntrySet());
	}
	private final class EntrySet extends AbstractSet implements Set {
		public Iterator iterator() {
			return new HashIterator();
		}
		public boolean contains(Object o) {
			if(!(o instanceof Map.Entry))
				return false;
			final Map.Entry entry = (Map.Entry) o;
			final Object v = get(entry.getKey());
			return v != null && v.equals(entry.getValue());
		}
		public boolean remove(Object o) {
			if(!(o instanceof Map.Entry))
				return false;
			final Map.Entry e = (Map.Entry) o;
			final Object key = e.getKey();
			return ConcurrentHashMap.this.remove(key, hash(key),
					e.getValue()) != null;
		}
		public int size() {
			return ConcurrentHashMap.this.size();
		}
		public void clear() {
			ConcurrentHashMap.this.clear();
		}
	}
	/**
	 * Returns an enumeration of the keys in this table.
	 *
	 * @return  an enumeration of the keys in this table.
	 * @see     Enumeration
	 * @see     #elements()
	 * @see	#keySet()
	 * @see	Map
	 */
	public Enumeration keys() {
		return new KeyIterator();
	}
	/**
	 * Returns an enumeration of the values in this table.
	 * Use the Enumeration methods on the returned object to fetch the elements
	 * sequentially.
	 *
	 * @return  an enumeration of the values in this table.
	 * @see     java.util.Enumeration
	 * @see     #keys()
	 * @see	#values()
	 * @see	Map
	 */
	public Enumeration elements() {
		return new ValueIterator();
	}
	/**
	 * ConcurrentHashMap collision list entry.
	 */
	private static final class Entry implements Map.Entry {
		/*
		 * The use of volatile for value field ensures that
		 * we can detect status changes without synchronization.
		 * The other fields are never changed, and are
		 * marked as final.
		 */
		private final Object _key;
		private volatile Object _value;
		private final int _hash;
		private volatile Entry _next;
		private Entry(int hash, Object key, Object value, Entry next) {
			_value = value;
			_hash = hash;
			_key = key;
			_next = next;
		}
		// Map.Entry Ops
		public Object getKey() {
			return _key;
		}
		/**
		 * Get the value.  Note: In an entrySet or entrySet.iterator,
		 * unless you can guarantee lack of concurrent modification,
		 * <tt>getValue</tt> <em>might</em> return null, reflecting the
		 * fact that the entry has been concurrently removed. However,
		 * there are no assurances that concurrent removals will be
		 * reflected using this method.
		 *
		 * @return     the current value, or null if the entry has been
		 * detectably removed.
		 */
		public Object getValue() {
			return _value;
		}
		/**
		 * Set the value of this entry.  Note: In an entrySet or
		 * entrySet.iterator), unless you can guarantee lack of concurrent
		 * modification, <tt>setValue</tt> is not strictly guaranteed to
		 * actually replace the value field obtained via the <tt>get</tt>
		 * operation of the underlying hash table in multithreaded
		 * applications.  If iterator-wide synchronization is not used,
		 * and any other concurrent <tt>put</tt> or <tt>remove</tt>
		 * operations occur, sometimes even to <em>other</em> entries,
		 * then this change is not guaranteed to be reflected in the hash
		 * table. (It might, or it might not. There are no assurances
		 * either way.)
		 *
		 * @param      value   the new value.
		 * @return     the previous value, or null if entry has been detectably
		 * removed.
		 * @throws  NullPointerException  if the value is <code>null</code>.
		 *
		 */
		public Object setValue(Object value) {
			if(value == null)
				throw new NullPointerException();
			final Object oldValue = _value;
			_value = value;
			return oldValue;
		}
		public boolean equals(Object o) {
			if(!(o instanceof Map.Entry))
				return false;
			final Map.Entry e = (Map.Entry) o;
			return _key.equals(e.getKey()) && _value.equals(e.getValue());
		}
		public int hashCode() {
			return _key.hashCode() ^ _value.hashCode();
		}
		public String toString() {
			return _key + "=" + _value;
		}
	}
	private class HashIterator implements Iterator, Enumeration {
		private final Entry[] _tab; // snapshot of table
		private int _index; // current slot
		private Entry _entry; // current node of slot
		Object _currentKey; // key for current node
		Object _currentValue; // value for current node
		private Entry _lastReturned; // last node returned by next
		private HashIterator() {
			// force all segments to synch
			synchronized(_segments[0]) {
				_tab = _table;
			}
			final int len = _segments.length;
			for(int i = 1; i < len; ++i) {
				_segments[i].synch();
			}
			_index = _tab.length - 1;
		}
		public boolean hasMoreElements() {
			return hasNext();
		}
		public Object nextElement() {
			return next();
		}
		public boolean hasNext() {
			/*
			 * currentkey and currentValue are set here to ensure that next()
			 * returns normally if hasNext() returns true. This avoids
			 * surprises especially when final element is removed during
			 * traversal -- instead, we just ignore the removal during
			 * current traversal.
			 */
			for(;;) {
				if(_entry != null) {
					final Object v = _entry._value;
					if(v != null) {
						_currentKey = _entry._key;
						_currentValue = v;
						return true;
					}
					_entry = _entry._next;
				}
				while(_entry == null && _index >= 0) {
					_entry = _tab[_index--];
				}
				if(_entry == null) {
					_currentKey = _currentValue = null;
					return false;
				}
			}
		}
		Object returnValueOfNext() {
			return _entry;
		}
		public Object next() {
			if(_currentKey == null && !hasNext())
				throw new NoSuchElementException();
			final Object result = returnValueOfNext();
			_lastReturned = _entry;
			_currentKey = _currentValue = null;
			_entry = _entry._next;
			return result;
		}
		public void remove() {
			if(_lastReturned == null)
				throw new IllegalStateException();
			ConcurrentHashMap.this.remove(_lastReturned._key);
			_lastReturned = null;
		}
	}
	private final class KeyIterator extends HashIterator {
		Object returnValueOfNext() {
			return _currentKey;
		}
	}
	private final class ValueIterator extends HashIterator {
		Object returnValueOfNext() {
			return _currentValue;
		}
	}
	/**
	 * Save the state of the <tt>ConcurrentHashMap</tt>
	 * instance to a stream (i.e.,
	 * serialize it).
	 *
	 * @serialData
	 * An estimate of the table size, followed by
	 * the key (Object) and value (Object)
	 * for each key-value mapping, followed by a null pair.
	 * The key-value mappings are emitted in no particular order.
	 */
	private void writeObject(ObjectOutputStream s) throws IOException {
		// Write out the loadfactor, and any hidden stuff
		s.defaultWriteObject();
		// Write out capacity estimate. It is OK if this
		// changes during the write, since it is only used by
		// readObject to set initial capacity, to avoid needless resizings.
		int cap;
		synchronized(_segments[0]) {
			cap = _table.length;
		}
		s.writeInt(cap);
		// Write out keys and values (alternating)
		final int len = _segments.length;
		for(int k = 0; k < len; ++k) {
			final Segment seg = _segments[k];
			Entry[] tab;
			synchronized(seg) {
				tab = _table;
			}
			final int length = tab.length;
			for(int i = k; i < length; i += len) {
				for(Entry e = tab[i]; e != null; e = e._next) {
					s.writeObject(e._key);
					s.writeObject(e._value);
				}
			}
		}
		s.writeObject(null);
		s.writeObject(null);
	}
	/**
	 * Reconstitute the <tt>ConcurrentHashMap</tt>
	 * instance from a stream (i.e.,
	 * deserialize it).
	 */
	private void readObject(ObjectInputStream s)
			throws IOException, ClassNotFoundException {
		// Read in the threshold, loadfactor, and any hidden stuff
		s.defaultReadObject();
		final int cap = s.readInt();
		_table = newTable(cap);
		final int len = _segments.length;
		for(int i = 0; i < len; ++i) {
			_segments[i] = new Segment();
		}
		// Read the keys and values, and put the mappings in the table
		for(;;) {
			final Object key = s.readObject();
			final Object value = s.readObject();
			if(key == null) {
				break;
			}
			put(key, value);
		}
	}
	/**
	 * A Set view of the keys of a ConcurrentHashMap, where additions are enabled
	 * by mapping to a common value.
	 */
	public static final class KeySetView extends AbstractSet
			implements Set, Serializable {
		private final ConcurrentHashMap _map;
		private final Object _value;
		private KeySetView(ConcurrentHashMap map, Object value) {
			_map = map;
			_value = value;
		}
		public Object getMappedValue() {
			return _value;
		}
		public ConcurrentHashMap getMap() {
			return _map;
		}
		public void clear() {
			_map.clear();
		}
		public int size() {
			return _map.size();
		}
		public boolean isEmpty() {
			return _map.isEmpty();
		}
		public boolean contains(Object o) {
			return _map.containsKey(o);
		}
		public boolean remove(Object o) {
			return _map.remove(o) != null;
		}
		public Iterator iterator() {
			return _map.keySet().iterator();
		}
		public boolean add(Object e) {
			return _map.put(e, _value) == null;
		}
		public boolean addAll(Collection c) {
			boolean added = false;
			for(final Iterator it = c.iterator(); it.hasNext();) {
				if(add(it.next())) {
					added = true;
				}
			}
			return added;
		}
	}
}