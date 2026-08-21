/*
 * Javolution - Java(TM) Solution for Real-Time and Embedded Systems
 * Copyright (C) 2006 - Javolution (http://javolution.org/)
 * All rights reserved.
 *
 * Permission to use, copy, modify, and distribute this software is
 * freely granted, provided that this notice is preserved.
 */
package _templates.javolution.util;
import _templates.java.util.Iterator;
import _templates.java.util.Set;
import _templates.javolution.context.ObjectFactory;
import _templates.javolution.lang.Reusable;
/**
 * <p> This class represents a set collection backed by a {@link FastMap};
 *     smooth capacity increase and no rehashing ever performed.</p>
 *
 * <p> {@link FastSet}, as for any {@link FastCollection} sub-class, supports
 *     thread-safe fast iterations without using iterators. Since the
 *     underlying records are {@link FastMap.Entry} instances, values can be
 *     accessed directly without calling {@link #valueOf}:[code]
 * for(FastMap.Entry e = set.head(), end = set.tail(); (e = (FastMap.Entry) e.getNext()) != end;) {
 *     Object value = e.getKey(); // Direct access, no valueOf() needed.
 * }[/code]</p>
 *
 * <p> The generic {@link FastCollection.Record} approach is also supported:[code]
 * for(FastSet.Record r = set.head(), end = set.tail(); (r = r.getNext()) != end;) {
 *     Object value = set.valueOf(r);
 * }[/code]</p>
 *
 * @author  <a href="mailto:jean-marie@dautelle.com">Jean-Marie Dautelle</a>
 * @version 5.7.5, June 28, 2026
 */
public class FastSet/*<E>*/ extends FastCollection
		/*<E>*/ implements Set/*<E>*/, Reusable {
	/**
	 * Holds the set factory.
	 */
	private static final ObjectFactory FACTORY = new ObjectFactory() {
		public Object create() {
			return new FastSet();
		}
	};
	/**
	 * Holds the backing map.
	 */
	private transient FastMap _map;
	/**
	 * Creates a set of small initial capacity.
	 */
	public FastSet() {
		this(new FastMap());
	}
	/**
	 * Creates a persistent set associated to the specified unique identifier
	 * (convenience method).
	 *
	 * @param id the unique identifier for this map.
	 * @throws IllegalArgumentException if the identifier is not unique.
	 * @see _templates.javolution.context.PersistentContext.Reference
	 */
	public FastSet(String id) {
		this(new FastMap(id));
	}
	/**
	 * Creates a set of specified initial capacity; unless the set size
	 * reaches the specified capacity, operations on this set will not allocate
	 * memory (no lazy object creation).
	 *
	 * @param capacity the initial capacity.
	 */
	public FastSet(int capacity) {
		this(new FastMap(capacity));
	}
	/**
	 * Creates a set containing the specified elements, in the order they
	 * are returned by the set iterator.
	 *
	 * @param elements the elements to be placed into this fast set.
	 */
	public FastSet(Set/*<? extends E>*/ elements) {
		this(new FastMap(elements.size()));
		addAll(elements);
	}
	/**
	 * Creates a set implemented using the specified map.
	 *
	 * @param map the backing map.
	 */
	public FastSet(FastMap map) {
		_map = map;
	}
	/**
	 * Returns a new, preallocated or {@link #recycle recycled} set instance
	 * (on the stack when executing in a {@link _templates.javolution.context.StackContext
	 * StackContext}).
	 *
	 * @return a new, preallocated or recycled set instance.
	 */
	public static FastSet newInstance() {
		return (FastSet) FACTORY.object();
	}
	/**
	 * Recycles a set {@link #newInstance() instance} immediately
	 * (on the stack when executing in a {@link _templates.javolution.context.StackContext
	 * StackContext}).
	 */
	public static void recycle(FastSet instance) {
		FACTORY.recycle(instance);
	}
	/**
	 * Returns the number of elements in this set (its cardinality).
	 *
	 * @return the number of elements in this set (its cardinality).
	 */
	public int size() {
		return _map.size();
	}
	/**
	 * Adds the specified value to this set if it is not already present.
	 *
	 * @param value the value to be added to this set.
	 * @return <code>true</code> if this set did not already contain the
	 *         specified element.
	 * @throws NullPointerException if the value is <code>null</code>.
	 */
	public boolean add(Object/*{E}*/ value) {
		return _map.put(value, value) == null;
	}
	/**
	 * Returns an iterator over the elements in this set
	 * (allocated on the stack when executed in a
	 * {@link _templates.javolution.context.StackContext StackContext}).
	 *
	 * @return an iterator over this set values.
	 */
	public Iterator/*<E>*/ iterator() {
		return _map.keySet().iterator();
	}
	// Overrides to return a set (JDK1.5+).
	public FastCollection/*FastSet<E>*/ unmodifiable() {
		return (FastCollection/*FastSet<E>*/) super.unmodifiable();
	}
	// Overrides to return a set (JDK1.5+).
	public FastCollection/*FastSet<E>*/ shared() {
		_map.shared();
		return this;
	}
	// Overrides (optimization).
	public void clear() {
		_map.clear();
	}
	// Overrides (optimization).
	public boolean contains(Object o) {
		return _map.containsKey(o);
	}
	// Overrides (optimization).
	public boolean remove(Object o) {
		return _map.remove(o) != null;
	}
	/**
	 * Sets the comparator to use for value equality.
	 *
	 * @param comparator the value comparator.
	 * @return <code>this</code>
	 */
	public FastSet/*<E>*/ setValueComparator(
			FastComparator/*<? super E>*/ comparator) {
		_map.setKeyComparator(comparator);
		return this;
	}
	// Overrides.
	public FastComparator/*<? super E>*/ getValueComparator() {
		return _map.getKeyComparator();
	}
	// Implements Reusable.
	public void reset() {
		_map.reset();
	}
	// Implements FastCollection abstract method.
	public Record/*FastMap.Entry<E,E>*/ head() {
		return _map.head();
	}
	// Implements FastCollection abstract method.
	public Record/*FastMap.Entry<E,E>*/ tail() {
		return _map.tail();
	}
	// Implements FastCollection abstract method.
	public Object/*{E}*/ valueOf(Record record) {
		return (Object/*{E}*/) ((FastMap.Entry) record).getKey();
	}
	// Implements FastCollection abstract method.
	public void delete(Record record) {
		_map.remove(((FastMap.Entry) record).getKey());
	}
}