/*
 * Javolution - Java(TM) Solution for Real-Time and Embedded Systems
 * Copyright (C) 2005 - Javolution (http://javolution.org/)
 * All rights reserved.
 *
 * Permission to use, copy, modify, and distribute this software is
 * freely granted, provided that this notice is preserved.
 */
package _templates.javolution.util;
import java.util.NoSuchElementException;
import _templates.java.io.Serializable;
import _templates.java.lang.IllegalStateException;
import _templates.java.lang.UnsupportedOperationException;
import _templates.java.util.Collection;
import _templates.java.util.Deque;
import _templates.java.util.Iterator;
import _templates.java.util.List;
import _templates.java.util.ListIterator;
import _templates.java.util.Queue;
import _templates.javax.realtime.MemoryArea;
import _templates.javolution.context.ObjectFactory;
import _templates.javolution.context.PersistentContext;
import _templates.javolution.lang.Reusable;
import _templates.javolution.util.concurrent.locks.ReadWriteLock;
import _templates.javolution.util.concurrent.locks.ReentrantWriterPreferenceReadWriteLock;
import _templates.javolution.util.concurrent.locks.Sync;
/**
 * <p> This class represents a linked list with real-time behavior;
 *     smooth capacity increase/decrease and no memory fragmentation.</p>
 *
 * <p> FastList has a much smaller memory footprint than
 *     <code>java.util.LinkedList</code>. Real-time applications may use
 *     {@link #subList} to view only a portion of the list and avoid creating
 *     new collections instances.</p>
 *
 * <p> {@link FastList} are {@link Reusable reusable}, they maintain
 *     internal pools of {@link Node nodes} objects. When a node is removed
 *     from its list, it is automatically restored to its pool.</p>
 *
 * <p> Custom list implementations may override the {@link #newNode} method
 *     in order to return their own {@link Node} implementation (with
 *     additional fields for example).</p>
 *
 * @author <a href="mailto:jean-marie@dautelle.com">Jean-Marie Dautelle</a>
 * @version 6.1.0, August 22, 2026
 */
public class FastList/*<E>*/ extends FastCollection
		/*<E>*/ implements List/*<E>*/, Queue/*<E>*/, Deque/*<E>*/, Reusable {
	/**
	* Default initial capacity.
	*/
	private static final int DEFAULT_CAPACITY = 4;
	/**
	 * Holds the main list factory.
	 */
	private static final ObjectFactory FACTORY = new ObjectFactory() {
		public Object create() {
			return new FastList();
		}
	};
	/**
	 * Holds the node marking the beginning of the list (not included).
	 */
	private transient Node/*<E>*/ _head;
	/**
	 * Holds the node marking the end of the list (not included).
	 */
	private transient Node/*<E>*/ _tail;
	/**
	 * Holds the value comparator.
	 */
	private transient FastComparator/*<? super E>*/ _valueComparator;
	/**
	 * Holds the current size.
	 */
	private transient int _size;
	/**
	 * Creates a list of small initial capacity.
	 */
	public FastList() {
		this(DEFAULT_CAPACITY);
	}
	/**
	 * Creates a persistent list associated to the specified unique identifier
	 * (convenience method).
	 *
	 * @param id the unique identifier for this map.
	 * @throws IllegalArgumentException if the identifier is not unique.
	 * @see _templates.javolution.context.PersistentContext.Reference
	 */
	public FastList(String id) {
		this();
		new PersistentContext.Reference(id, this) {
			protected void notifyChange() {
				FastList.this.clear();
				FastList.this.addAll((FastList) this.get());
			}
		};
	}
	/**
	 * Creates a list of specified initial capacity; unless the list size
	 * reaches the specified capacity, operations on this list will not allocate
	 * memory (no lazy object creation).
	 *
	 * @param capacity the initial capacity.
	 */
	public FastList(int capacity) {
		_valueComparator = FastComparator.DEFAULT;
		_head = newNode();
		_tail = newNode();
		_head._next = _tail;
		_tail._previous = _head;
		Node/*<E>*/ previous = _tail;
		for(int i = 0; i < capacity; ++i) {
			final Node/*<E>*/ newNode = newNode();
			newNode._previous = previous;
			previous._next = newNode;
			previous = newNode;
		}
	}
	/**
	 * Creates a list containing the specified values, in the order they
	 * are returned by the collection's iterator.
	 *
	 * @param values the values to be placed into this list.
	 */
	public FastList(Collection/*<? extends E>*/ values) {
		this(values.size());
		addAll(values);
	}
	/**
	 * No-op constructor for internal view classes ({@link Unmodifiable}, {@link Shared}).
	 * Skips all internal data structure initialization.
	 */
	private FastList(Void unused) {
		// No initialization — view delegates everything to backing list.
	}
	/**
	 * Returns a new, preallocated or {@link #recycle recycled} list instance
	 * (on the stack when executing in a {@link _templates.javolution.context.StackContext
	 * StackContext}).
	 *
	 * @return a new, preallocated or recycled list instance.
	 */
	public static/*<E>*/FastList/*<E>*/ newInstance() {
		return (FastList/*<E>*/) FACTORY.object();
	}
	/**
	 * Recycles a list {@link #newInstance() instance} immediately
	 * (on the stack when executing in a {@link _templates.javolution.context.StackContext
	 * StackContext}).
	 */
	public static void recycle(FastList instance) {
		FACTORY.recycle(instance);
	}
	/**
	 * Appends the specified value to the end of this list
	 * (equivalent to {@link #addLast}).
	 *
	 * @param value the value to be appended to this list.
	 * @return <code>true</code> (as per the general contract of the
	 *         <code>Collection.add</code> method).
	 */
	public boolean add(Object/*{E}*/ value) {
		addLast(value);
		return true;
	}
	/**
	 * Returns the value at the specified position in this list.
	 *
	 * @param index the index of value to return.
	 * @return the value at the specified position in this list.
	 * @throws IndexOutOfBoundsException if <code>index < 0 || index >= size()</code>
	 */
	public Object/*{E}*/ get(int index) {
		if(index < 0 || index >= _size)
			throw new IndexOutOfBoundsException("index: " + index);
		return nodeAt(index)._value;
	}
	/**
	 * Replaces the value at the specified position in this list with the
	 * specified value.
	 *
	 * @param index the index of value to replace.
	 * @param value the value to be stored at the specified position.
	 * @return the value previously at the specified position.
	 * @throws IndexOutOfBoundsException if <code>index < 0 || index >= size()</code>
	 */
	public Object/*{E}*/ set(int index, Object/*{E}*/ value) {
		if(index < 0 || index >= _size)
			throw new IndexOutOfBoundsException("index: " + index);
		final Node/*<E>*/ node = nodeAt(index);
		final Object/*{E}*/ previousValue = node._value;
		node._value = value;
		return previousValue;
	}
	/**
	 * Inserts the specified value at the specified position in this list.
	 * Shifts the value currently at that position
	 * (if any) and any subsequent values to the right (adds one to their
	 * indices).
	 *
	 * @param index the index at which the specified value is to be inserted.
	 * @param value the value to be inserted.
	 * @throws IndexOutOfBoundsException if <code>index < 0 || index > size()</code>
	 */
	public void add(int index, Object/*{E}*/ value) {
		if(index < 0 || index > _size)
			throw new IndexOutOfBoundsException("index: " + index);
		addBefore(nodeAt(index), value);
	}
	/**
	 * Inserts all of the values in the specified collection into this
	 * list at the specified position. Shifts the value currently at that
	 * position (if any) and any subsequent values to the right
	 * (increases their indices).
	 *
	 * @param index the index at which to insert first value from the specified
	 *        collection.
	 * @param values the values to be inserted into this list.
	 * @return <code>true</code> if this list changed as a result of the call;
	 *         <code>false</code> otherwise.
	 * @throws IndexOutOfBoundsException if <code>index < 0 || index > size()</code>
	 */
	public boolean addAll(int index, Collection/*<? extends E>*/ values) {
		if(index < 0 || index > _size)
			throw new IndexOutOfBoundsException("index: " + index);
		final Node indexNode = nodeAt(index);
		for(final Iterator /*<? extends E>*/ i = values.iterator(); i
				.hasNext();) {
			addBefore(indexNode, i.next());
		}
		return values.size() != 0;
	}
	public boolean addAll(Collection/*<? extends E>*/ values) {
		return addAll(_size, values);
	}
	/**
	 * Removes the value at the specified position in this list.
	 * Shifts any subsequent values to the left (subtracts one from their indices).
	 * Returns the value that was removed from the list.
	 *
	 * @param index the index of the value to removed.
	 * @return the value previously at the specified position.
	 * @throws IndexOutOfBoundsException if <code>index < 0 || index >= size()</code>
	 */
	public Object/*{E}*/ remove(int index) {
		if(index < 0 || index >= _size)
			throw new IndexOutOfBoundsException("index: " + index);
		final Node/*<E>*/ node = nodeAt(index);
		final Object/*{E}*/ previousValue = node._value;
		delete(node);
		return previousValue;
	}
	/**
	 * Returns the index in this list of the first occurrence of the specified
	 * value, or <code>-1</code> if this list does not contain this value.
	 *
	 * @param value the value to search for.
	 * @return the index in this list of the first occurrence of the specified
	 *         value, or <code>-1</code> if this list does not contain this value.
	 */
	public int indexOf(Object value) {
		final FastComparator comp = getValueComparator();
		int index = 0;
		for(Node n = _head, end = _tail; (n = n._next) != end; ++index) {
			if(comp == FastComparator.DEFAULT ? defaultEquals(value, n._value)
					: comp.areEqual(value, n._value))
				return index;
		}
		return -1;
	}
	/**
	 * Returns the index in this list of the last occurrence of the specified
	 * value, or <code>-1</code> if this list does not contain this value.
	 *
	 * @param value the value to search for.
	 * @return the index in this list of the last occurrence of the specified
	 *         value, or <code>-1</code> if this list does not contain this value.
	 */
	public int lastIndexOf(Object value) {
		final FastComparator comp = getValueComparator();
		int index = size() - 1;
		for(Node n = _tail, end = _head; (n = n._previous) != end; --index) {
			if(comp == FastComparator.DEFAULT ? defaultEquals(value, n._value)
					: comp.areEqual(value, n._value))
				return index;
		}
		return -1;
	}
	/**
	 * Returns a simple iterator over the elements in this list
	 * (allocated on the stack when executed in a
	 * {@link _templates.javolution.context.StackContext StackContext}).
	 *
	 * @return an iterator over this list values.
	 */
	public Iterator/*<E>*/ iterator() {
		return FastListIterator.valueOf(this, _head._next, 0, _size);
	}
	/**
	 * Returns a list iterator over the elements in this list
	 * (allocated on the stack when executed in a
	 * {@link _templates.javolution.context.StackContext StackContext}).
	 *
	 * @return an iterator over this list values.
	 */
	public ListIterator/*<E>*/ listIterator() {
		return FastListIterator.valueOf(this, _head._next, 0, _size);
	}
	/**
	 * Returns a list iterator from the specified position
	 * (allocated on the stack when executed in a
	 * {@link _templates.javolution.context.StackContext StackContext}).
	 *
	 * The specified index indicates the first value that would be returned by
	 * an initial call to the <code>next</code> method.  An initial call to
	 * the <code>previous</code> method would return the value with the
	 * specified index minus one.
	 *
	 * @param index index of first value to be returned from the
	 *        list iterator (by a call to the <code>next</code> method).
	 * @return a list iterator over the values in this list
	 *         starting at the specified position in this list.
	 * @throws IndexOutOfBoundsException if <code>index < 0 || index > size()</code>
	 */
	public ListIterator/*<E>*/ listIterator(int index) {
		if(index < 0 || index > _size)
			throw new IndexOutOfBoundsException("index: " + index);
		return FastListIterator.valueOf(this, nodeAt(index), index, _size);
	}
	/**
	 * Returns a view of the portion of this list between the specified
	 * indexes (allocated from the "stack" when executing in a
	 * {@link _templates.javolution.context.StackContext StackContext}).
	 * If the specified indexes are equal, the returned list is empty.
	 * The returned list is backed by this list, so non-structural changes in
	 * the returned list are reflected in this list, and vice-versa.
	 *
	 * This method eliminates the need for explicit range operations (of
	 * the sort that commonly exist for arrays). Any operation that expects
	 * a list can be used as a range operation by passing a subList view
	 * instead of a whole list.  For example, the following idiom
	 * removes a range of values from a list:
	 * <code>list.subList(from, to).clear();</code>
	 * Similar idioms may be constructed for <code>indexOf</code> and
	 * <code>lastIndexOf</code>, and all of the algorithms in the
	 * <code>Collections</code> class can be applied to a subList.
	 *
	 * The semantics of the list returned by this method become undefined if
	 * the backing list (i.e., this list) is <i>structurally modified</i> in
	 * any way other than via the returned list (structural modifications are
	 * those that change the size of this list, or otherwise perturb it in such
	 * a fashion that iterations in progress may yield incorrect results).
	 *
	 * @param fromIndex low endpoint (inclusive) of the subList.
	 * @param toIndex high endpoint (exclusive) of the subList.
	 * @return a view of the specified range within this list.
	 *
	 * @throws IndexOutOfBoundsException if <code>fromIndex < 0 ||
	 *         fromIndex > toIndex || toIndex > size()</code>
	 */
	public List/*<E>*/ subList(int fromIndex, int toIndex) {
		if(fromIndex < 0 || fromIndex > toIndex || toIndex > _size)
			throw new IndexOutOfBoundsException("fromIndex: " + fromIndex
					+ ", toIndex: " + toIndex + " for list of size: " + _size);
		return SubList.valueOf(this, nodeAt(fromIndex)._previous,
				nodeAt(toIndex), toIndex - fromIndex);
	}
	/**
	 * Returns the first value of this list.
	 *
	 * @return this list's first value.
	 * @throws NoSuchElementException if this list is empty.
	 */
	public Object/*{E}*/ getFirst() {
		final Node/*<E>*/ node = _head._next;
		if(node == _tail)
			throw new NoSuchElementException();
		return node._value;
	}
	/**
	 * Returns the last value of this list.
	 *
	 * @return this list's last value.
	 * @throws NoSuchElementException if this list is empty.
	 */
	public Object/*{E}*/ getLast() {
		final Node/*<E>*/ node = _tail._previous;
		if(node == _head)
			throw new NoSuchElementException();
		return node._value;
	}
	/**
	 * Inserts the specified value at the beginning of this list.
	 *
	 * @param value the value to be inserted.
	 */
	public void addFirst(Object/*{E}*/ value) {
		addBefore(_head._next, value);
	}
	/**
	 * Appends the specified value to the end of this list <i>(fast)</i>.
	 *
	 * @param value the value to be inserted.
	 */
	public void addLast(Object/*{E}*/ value) { // Optimized.
		if(_tail._next == null) {
			increaseCapacity();
		}
		_tail._value = value;
		_tail = _tail._next;
		++_size;
	}
	/**
	 * Removes and returns the first value of this list.
	 *
	 * @return this list's first value before this call.
	 * @throws NoSuchElementException if this list is empty.
	 */
	public Object/*{E}*/ removeFirst() {
		final Node/*<E>*/ first = _head._next;
		if(first == _tail)
			throw new NoSuchElementException();
		final Object/*{E}*/ previousValue = first._value;
		delete(first);
		return previousValue;
	}
	/**
	 * Removes and returns the last value of this list <i>(fast)</i>.
	 *
	 * @return this list's last value before this call.
	 * @throws NoSuchElementException if this list is empty.
	 */
	public Object/*{E}*/ removeLast() {
		if(_size == 0)
			throw new NoSuchElementException();
		--_size;
		final Node/*<E>*/ last = _tail._previous;
		final Object/*{E}*/ previousValue = last._value;
		_tail = last;
		last._value = null;
		return previousValue;
	}
	/**
	 * Inserts the specified value at the front of this list.
	 *
	 * @param value the value to add.
	 * @return <code>true</code>.
	 */
	public boolean offerFirst(Object/*{E}*/ value) {
		addFirst(value);
		return true;
	}
	/**
	 * Inserts the specified value at the end of this list.
	 *
	 * @param value the value to add.
	 * @return <code>true</code>.
	 */
	public boolean offerLast(Object/*{E}*/ value) {
		addLast(value);
		return true;
	}
	/**
	 * Retrieves and removes the first value of this list,
	 * or returns <code>null</code> if this list is empty.
	 *
	 * @return the first value of this list, or <code>null</code> if this list is empty.
	 */
	public Object/*{E}*/ pollFirst() {
		return _size == 0 ? null : removeFirst();
	}
	/**
	 * Retrieves and removes the last value of this list,
	 * or returns <code>null</code> if this list is empty.
	 *
	 * @return the last value of this list, or <code>null</code> if this list is empty.
	 */
	public Object/*{E}*/ pollLast() {
		return _size == 0 ? null : removeLast();
	}
	/**
	 * Retrieves, but does not remove, the first value of this list,
	 * or returns <code>null</code> if this list is empty.
	 *
	 * @return the first value of this list, or <code>null</code> if this list is empty.
	 */
	public Object/*{E}*/ peekFirst() {
		final Node/*<E>*/ node = _head._next;
		return node == _tail ? null : (Object/*{E}*/) node._value;
	}
	/**
	 * Retrieves, but does not remove, the last value of this list,
	 * or returns <code>null</code> if this list is empty.
	 *
	 * @return the last value of this list, or <code>null</code> if this list is empty.
	 */
	public Object/*{E}*/ peekLast() {
		final Node/*<E>*/ node = _tail._previous;
		return node == _head ? null : (Object/*{E}*/) node._value;
	}
	/**
	 * Removes the first occurrence of the specified value from this list.
	 *
	 * @param value the value to be removed from this list, if present.
	 * @return <code>true</code> if this list contained the specified value.
	 */
	public boolean removeFirstOccurrence(Object value) {
		return remove(value);
	}
	/**
	 * Removes the last occurrence of the specified value from this list.
	 *
	 * @param value the value to be removed from this list, if present.
	 * @return <code>true</code> if this list contained the specified value.
	 */
	public boolean removeLastOccurrence(Object value) {
		final FastComparator comp = getValueComparator();
		for(Node /*<E>*/ n = _tail._previous; n != _head; n = n._previous) {
			if(comp.areEqual(value, n._value)) {
				delete(n);
				return true;
			}
		}
		return false;
	}
	/**
	 * Inserts the specified value as the tail (last value) of this list.
	 *
	 * @param value the value to add.
	 * @return <code>true</code>.
	 */
	public boolean offer(Object/*{E}*/ value) {
		return add(value);
	}
	/**
	 * Retrieves and removes the head (first value) of this list.
	 *
	 * @return the head of this list.
	 * @throws NoSuchElementException if this list is empty.
	 */
	public Object/*{E}*/ remove() {
		return removeFirst();
	}
	/**
	 * Retrieves and removes the head (first value) of this list,
	 * or returns <code>null</code> if this list is empty.
	 *
	 * @return the head of this list, or <code>null</code> if this list is empty.
	 */
	public Object/*{E}*/ poll() {
		return pollFirst();
	}
	/**
	 * Retrieves, but does not remove, the head (first value) of this list.
	 *
	 * @return the head of this list.
	 * @throws NoSuchElementException if this list is empty.
	 */
	public Object/*{E}*/ element() {
		return getFirst();
	}
	/**
	 * Retrieves, but does not remove, the head (first value) of this list,
	 * or returns <code>null</code> if this list is empty.
	 *
	 * @return the head of this list, or <code>null</code> if this list is empty.
	 */
	public Object/*{E}*/ peek() {
		return peekFirst();
	}
	/**
	 * Pushes a value onto the stack represented by this list (in other words,
	 * at the head of this list).
	 *
	 * @param value the value to push.
	 */
	public void push(Object/*{E}*/ value) {
		addFirst(value);
	}
	/**
	 * Pops a value from the stack represented by this list. In other words,
	 * removes and returns the first value of this list.
	 *
	 * @return the value at the front of this list (which is the top of the stack).
	 * @throws NoSuchElementException if this list is empty.
	 */
	public Object/*{E}*/ pop() {
		return removeFirst();
	}
	/**
	 * Returns an iterator over the values in this list in reverse sequential order.
	 * The values will be returned in order from last (tail) to first (head).
	 *
	 * @return an iterator over the values in this list in reverse sequence.
	 */
	public Iterator/*<E>*/ descendingIterator() {
		return new Iterator/*<E>*/() {
			private Node/*<E>*/ _current = _tail._previous;
			private Node/*<E>*/ _lastReturned = null;
			public boolean hasNext() {
				return _current != _head;
			}
			public Object/*{E}*/ next() {
				if(_current == _head)
					throw new NoSuchElementException();
				_lastReturned = _current;
				_current = _current._previous;
				return (Object/*{E}*/) _lastReturned._value;
			}
			public void remove() {
				if(_lastReturned == null)
					throw new IllegalStateException();
				delete(_lastReturned);
				_lastReturned = null;
			}
		};
	}
	///////////////////////
	// Nodes operations. //
	///////////////////////
	/**
	 * Inserts the specified value before the specified Node.
	 *
	 * @param next the Node before which this value is inserted.
	 * @param value the value to be inserted.
	 */
	public void addBefore(Node/*<E>*/ next, Object/*{E}*/ value) {
		if(_tail._next == null) {
			increaseCapacity();// Increases capacity.
		}
		final Node newNode = _tail._next;
		// Detaches newNode.
		final Node tailNext = _tail._next = newNode._next;
		if(tailNext != null) {
			tailNext._previous = _tail;
		}
		// Inserts before next.
		final Node previous = next._previous;
		previous._next = newNode;
		next._previous = newNode;
		newNode._next = next;
		newNode._previous = previous;
		newNode._value = value;
		++_size;
	}
	/**
	 * Returns the node at the specified index. This method returns
	 * the {@link #headNode} node when [code]index < 0[/code] or
	 * the {@link #tailNode} node when [code]index >= size()[/code].
	 *
	 * @param index the index of the Node to return.
	 */
	private final Node/*<E>*/ nodeAt(int index) {
		// We cannot do backward search because of thread-safety.
		Node/*<E>*/ node = _head;
		for(int i = index; i >= 0; --i) {
			node = node._next;
		}
		return node;
	}
	// Implements FastCollection abstract method.
	public Record/*Node<E>*/ head() {
		return _head;
	}
	// Implements FastCollection abstract method.
	public Record/*Node<E>*/ tail() {
		return _tail;
	}
	// Implements FastCollection abstract method.
	public Object/*{E}*/ valueOf(Record record) {
		return ((Node/*<E>*/) record)._value;
	}
	// Implements FastCollection abstract method.
	public void delete(Record record) {
		final Node/*<E>*/ node = (Node/*<E>*/) record;
		--_size;
		node._value = null;
		// Detaches.
		node._previous._next = node._next;
		node._next._previous = node._previous;
		// Inserts after _tail.
		final Node/*<E>*/ next = _tail._next;
		node._previous = _tail;
		node._next = next;
		_tail._next = node;
		if(next != null) {
			next._previous = node;
		}
	}
	// Overrides (optimization).
	public boolean remove(Object value) {
		final int index = indexOf(value);
		if(index >= 0) {
			remove(index);
			return true;
		}
		return false;
	}
	// Overrides (optimization).
	public boolean contains(Object value) {
		return indexOf(value) >= 0;
	}
	///////////////////////
	// Contract Methods. //
	///////////////////////
	// Implements abstract method.
	public int size() {
		return _size;
	}
	// Implements abstract method.
	public boolean isEmpty() {
		return _size == 0;
	}
	// Overrides (optimization).
	public void clear() {
		_size = 0;
		for(Node /*<E>*/ n = _head, end = _tail; (n = n._next) != end;) {
			n._value = null;
		}
		_tail = _head._next;
	}
	/**
	 * Sets the comparator to use for value equality.
	 *
	 * @param comparator the value comparator.
	 * @return <code>this</code>
	 */
	public FastList/*<E>*/ setValueComparator(
			FastComparator/*<? super E>*/ comparator) {
		_valueComparator = comparator;
		return this;
	}
	// Overrides.
	public FastComparator/*<? super E>*/ getValueComparator() {
		return _valueComparator;
	}
	// Overrides to return a list (JDK1.5+).
	public FastCollection/*FastList<E>*/ unmodifiable() {
		return new Unmodifiable/*<E>*/(this);
	}
	// Overrides to return a list (JDK1.5+).
	public FastCollection/*FastList<E>*/ shared() {
		return new Shared/*<E>*/(this);
	}
	/**
	 * Returns a new node for this list; this method can be overriden by
	 * custom list implementation.
	 *
	 * @return a new node.
	 */
	protected Node/*<E>*/ newNode() {
		return new Node();
	}
	// Increases capacity (_tail._next == null)
	private final void increaseCapacity() {
		MemoryArea.getMemoryArea(this).executeInArea(new Runnable() {
			public final void run() {
				final Node/*<E>*/ newNode0 = newNode();
				_tail._next = newNode0;
				newNode0._previous = _tail;
				final Node/*<E>*/ newNode1 = newNode();
				newNode0._next = newNode1;
				newNode1._previous = newNode0;
				final Node/*<E>*/ newNode2 = newNode();
				newNode1._next = newNode2;
				newNode2._previous = newNode1;
				final Node/*<E>*/ newNode3 = newNode();
				newNode2._next = newNode3;
				newNode3._previous = newNode2;
			}
		});
	}
	// Implements Reusable interface.
	public void reset() {
		clear();
		setValueComparator(FastComparator.DEFAULT);
	}
	// For inlining of default comparator.
	private static final boolean defaultEquals(Object o1, Object o2) {
		return o1 == null ? o2 == null : o1 == o2 || o1.equals(o2);
	}
	/**
	 * This class represents a {@link FastList} node; it allows for direct
	 * iteration over the list {@link #getValue values}.
	 * Custom {@link FastList} may use a derived implementation.
	 * For example:[code]
	 * static class MyList<E,X> extends FastList<E> {
	 *     protected MyNode newNode() {
	 *         return new MyNode();
	 *     }
	 *     class MyNode extends Node<E> {
	 *         X xxx; // Additional node field (e.g. cross references).
	 *     }
	 * }[/code]
	 */
	public static final class Node/*<E>*/ implements Record, Serializable {
		/**
		 * Holds the next node.
		 */
		private Node/*<E>*/ _next;
		/**
		 * Holds the previous node.
		 */
		private Node/*<E>*/ _previous;
		/**
		 * Holds the node value.
		 */
		private Object/*{E}*/ _value;
		/**
		 * Default constructor.
		 */
		protected Node() {}
		/**
		 * Returns the value for this node.
		 *
		 * @return the node value.
		 */
		public final Object/*{E}*/ getValue() {
			return _value;
		}
		// Implements Record interface.
		public final Record/*Node<E>*/ getNext() {
			return _next;
		}
		// Implements Record interface.
		public final Record/*Node<E>*/ getPrevious() {
			return _previous;
		}
	}
	/**
	 * This inner class implements a sub-list.
	 */
	private static final class SubList/*<E>*/ extends FastCollection
			/*<E>*/ implements List/*<E>*/, Serializable {
		private static final ObjectFactory FACTORY = new ObjectFactory() {
			protected Object create() {
				return new SubList();
			}
			protected void cleanup(Object obj) {
				final SubList sl = (SubList) obj;
				sl._list = null;
				sl._head = null;
				sl._tail = null;
			}
		};
		private FastList/*<E>*/ _list;
		private Node/*<E>*/ _head;
		private Node/*<E>*/ _tail;
		private int _size;
		public static SubList valueOf(FastList list, Node head, Node tail,
				int size) {
			final SubList subList = (SubList) FACTORY.object();
			subList._list = list;
			subList._head = head;
			subList._tail = tail;
			subList._size = size;
			return subList;
		}
		public final int size() {
			return _size;
		}
		public final Record head() {
			return _head;
		}
		public final Record tail() {
			return _tail;
		}
		public final Object/*{E}*/ valueOf(Record record) {
			return (Object/*{E}*/) _list.valueOf(record);
		}
		public final void delete(Record record) {
			_list.delete(record);
		}
		public final boolean addAll(int index, Collection values) {
			if(index < 0 || index > _size)
				throw new IndexOutOfBoundsException("index: " + index);
			final Node/*<E>*/ indexNode = nodeAt(index);
			for(final Iterator /*<E>*/ i = values.iterator(); i.hasNext();) {
				_list.addBefore(indexNode, i.next());
			}
			return values.size() != 0;
		}
		public final Object/*{E}*/ get(int index) {
			if(index < 0 || index >= _size)
				throw new IndexOutOfBoundsException("index: " + index);
			return (Object/*{E}*/) nodeAt(index)._value;
		}
		public final Object/*{E}*/ set(int index, Object/*{E}*/ value) {
			if(index < 0 || index >= _size)
				throw new IndexOutOfBoundsException("index: " + index);
			final Node/*<E>*/ node = nodeAt(index);
			final Object/*{E}*/ previousValue = (Object/*{E}*/) node._value;
			node._value = value;
			return previousValue;
		}
		public final void add(int index, Object/*{E}*/ element) {
			if(index < 0 || index > _size)
				throw new IndexOutOfBoundsException("index: " + index);
			_list.addBefore(nodeAt(index), element);
		}
		public final Object/*{E}*/ remove(int index) {
			if(index < 0 || index >= _size)
				throw new IndexOutOfBoundsException("index: " + index);
			final Node/*<E>*/ node = nodeAt(index);
			final Object/*{E}*/ previousValue = (Object/*{E}*/) node._value;
			_list.delete(node);
			return previousValue;
		}
		public final int indexOf(Object value) {
			final FastComparator comp = _list.getValueComparator();
			int index = 0;
			for(Node /*<E>*/ n = _head, end = _tail; (n = n._next) != end; ++index) {
				if(comp.areEqual(value, n._value))
					return index;
			}
			return -1;
		}
		public final int lastIndexOf(Object value) {
			final FastComparator comp = getValueComparator();
			int index = size() - 1;
			for(Node /*<E>*/ n = _tail, end = _head; (n = n._previous) != end; --index) {
				if(comp.areEqual(value, n._value))
					return index;
			}
			return -1;
		}
		public final ListIterator/*<E>*/ listIterator() {
			return listIterator(0);
		}
		public final ListIterator/*<E>*/ listIterator(int index) {
			if(index >= 0 && index <= _size)
				return FastListIterator.valueOf(_list, nodeAt(index), index,
						_size);
			throw new IndexOutOfBoundsException(
					"index: " + index + " for list of size: " + _size);
		}
		public final List/*<E>*/ subList(int fromIndex, int toIndex) {
			if(fromIndex < 0 || toIndex > _size || fromIndex > toIndex)
				throw new IndexOutOfBoundsException(
						"fromIndex: " + fromIndex + ", toIndex: " + toIndex
								+ " for list of size: " + _size);
			final SubList subList = SubList.valueOf(_list,
					nodeAt(fromIndex)._previous, nodeAt(toIndex),
					toIndex - fromIndex);
			return subList;
		}
		private final Node/*<E>*/ nodeAt(int index) {
			if(index <= _size >> 1) { // Forward search.
				Node/*<E>*/ node = _head;
				for(int i = index; i-- >= 0;) {
					node = node._next;
				}
				return node;
			}
			// Backward search.
			Node/*<E>*/ node = _tail;
			for(int i = _size - index; i-- > 0;) {
				node = node._previous;
			}
			return node;
		}
	}
	/**
	 * This inner class implements a fast list iterator.
	 */
	private static final class FastListIterator
			/*<E>*/ implements ListIterator/*<E>*/ {
		private static final ObjectFactory FACTORY = new ObjectFactory() {
			protected Object create() {
				return new FastListIterator();
			}
			protected void cleanup(Object obj) {
				final FastListIterator i = (FastListIterator) obj;
				i._list = null;
				i._currentNode = null;
				i._nextNode = null;
			}
		};
		private FastList/*<E>*/ _list;
		private Node/*<E>*/ _nextNode;
		private Node/*<E>*/ _currentNode;
		private int _length;
		private int _nextIndex;
		public static FastListIterator valueOf(FastList list, Node nextNode,
				int nextIndex, int size) {
			final FastListIterator itr = (FastListIterator) FACTORY.object();
			itr._list = list;
			itr._nextNode = nextNode;
			itr._nextIndex = nextIndex;
			itr._length = size;
			return itr;
		}
		public final boolean hasNext() {
			return _nextIndex != _length;
		}
		public final Object/*{E}*/ next() {
			if(_nextIndex == _length)
				throw new NoSuchElementException();
			++_nextIndex;
			_currentNode = _nextNode;
			_nextNode = _nextNode._next;
			return (Object/*{E}*/) _currentNode._value;
		}
		public final int nextIndex() {
			return _nextIndex;
		}
		public final boolean hasPrevious() {
			return _nextIndex != 0;
		}
		public final Object/*{E}*/ previous() {
			if(_nextIndex == 0)
				throw new NoSuchElementException();
			--_nextIndex;
			_currentNode = _nextNode = _nextNode._previous;
			return (Object/*{E}*/) _currentNode._value;
		}
		public final int previousIndex() {
			return _nextIndex - 1;
		}
		public final void add(Object/*{E}*/ value) {
			_list.addBefore(_nextNode, value);
			_currentNode = null;
			++_length;
			++_nextIndex;
		}
		public final void set(Object/*{E}*/ value) {
			if(_currentNode == null)
				throw new IllegalStateException();
			_currentNode._value = value;
		}
		public final void remove() {
			if(_currentNode == null)
				throw new IllegalStateException();
			if(_nextNode == _currentNode) { // previous() has been called.
				_nextNode = _nextNode._next;
			}
			else {
				--_nextIndex;
			}
			_list.delete(_currentNode);
			_currentNode = null;
			--_length;
		}
	}
	/**
	 * An unmodifiable view over a {@code FastList}.
	 */
	private static final class Unmodifiable/*<E>*/ extends FastList
			/*<E>*/ implements List/*<E>*/, Reusable {
		private final FastList/*<E>*/ _list;
		private Unmodifiable(FastList/*<E>*/ list) {
			super((Void) null);
			_list = list;
		}
		public final FastCollection/*FastList<E>*/ unmodifiable() {
			return this;
		}
		public final FastCollection/*FastList<E>*/ shared() {
			throw new UnsupportedOperationException("Unmodifiable");
		}
		public final boolean add(Object/*{E}*/ element) {
			throw new UnsupportedOperationException("Unmodifiable");
		}
		public final void add(int index, Object/*{E}*/ element) {
			throw new UnsupportedOperationException("Unmodifiable");
		}
		public final boolean addAll(Collection/*<? extends E>*/ values) {
			throw new UnsupportedOperationException("Unmodifiable");
		}
		public final boolean addAll(int index,
				Collection/*<? extends E>*/ values) {
			throw new UnsupportedOperationException("Unmodifiable");
		}
		public final void clear() {
			throw new UnsupportedOperationException("Unmodifiable");
		}
		public final boolean contains(Object value) {
			return _list.contains(value);
		}
		public final boolean containsAll(Collection/*<?>*/ values) {
			return _list.containsAll(values);
		}
		public final void delete(Record record) {
			throw new UnsupportedOperationException("Unmodifiable");
		}
		public final Object/*{E}*/ get(int index) {
			return _list.get(index);
		}
		public final FastComparator/*<? super E>*/ getValueComparator() {
			return _list.getValueComparator();
		}
		public final Record/*Node<E>*/ head() {
			return _list.head();
		}
		public final int indexOf(Object value) {
			return _list.indexOf(value);
		}
		public final boolean isEmpty() {
			return _list.isEmpty();
		}
		public final int lastIndexOf(Object value) {
			return _list.lastIndexOf(value);
		}
		public final Iterator/*<E>*/ iterator() {
			return new Iterator() {
				private final Iterator/*<E>*/ i = _list.iterator();
				public boolean hasNext() {
					return i.hasNext();
				}
				public Object next() {
					return i.next();
				}
				public void remove() {
					throw new UnsupportedOperationException();
				}
			};
		}
		public final ListIterator/*<E>*/ listIterator() {
			throw new UnsupportedOperationException(
					"List iterator not supported for unmodifiable collection");
		}
		public final ListIterator/*<E>*/ listIterator(int index) {
			throw new UnsupportedOperationException(
					"List iterator not supported for unmodifiable collection");
		}
		public final Object/*{E}*/ remove(int index) {
			throw new UnsupportedOperationException("Unmodifiable");
		}
		public final boolean remove(Object value) {
			throw new UnsupportedOperationException("Unmodifiable");
		}
		public final boolean removeAll(Collection/*<?>*/ values) {
			throw new UnsupportedOperationException("Unmodifiable");
		}
		public final void reset() {
			throw new UnsupportedOperationException("Unmodifiable");
		}
		public final boolean retainAll(Collection/*<?>*/ values) {
			throw new UnsupportedOperationException("Unmodifiable");
		}
		public final Object/*{E}*/ set(int index, Object/*{E}*/ element) {
			throw new UnsupportedOperationException("Unmodifiable");
		}
		public final FastList/*<E>*/ setValueComparator(
				FastComparator/*<? super E>*/ comparator) {
			throw new UnsupportedOperationException("Unmodifiable");
		}
		public final int size() {
			return _list.size();
		}
		public final List/*<E>*/ subList(int fromIndex, int toIndex) {
			throw new UnsupportedOperationException(
					"Sub-List not supported for unmodifiable collection");
		}
		public final Record/*Node<E>*/ tail() {
			return _list.tail();
		}
		public final Object[] toArray() {
			return _list.toArray();
		}
		public final Object/*{<T> T}*/[] toArray(Object/*{T}*/[] array) {
			return _list.toArray(array);
		}
		public final Object/*{E}*/ valueOf(Record record) {
			return _list.valueOf(record);
		}
		public final Object/*{E}*/ getFirst() {
			return _list.getFirst();
		}
		public final Object/*{E}*/ getLast() {
			return _list.getLast();
		}
		public final Object/*{E}*/ peekFirst() {
			return _list.peekFirst();
		}
		public final Object/*{E}*/ peekLast() {
			return _list.peekLast();
		}
		public final Object/*{E}*/ peek() {
			return _list.peek();
		}
		public final Object/*{E}*/ element() {
			return _list.element();
		}
		public final void addFirst(Object/*{E}*/ value) {
			throw new UnsupportedOperationException("Unmodifiable");
		}
		public final void addLast(Object/*{E}*/ value) {
			throw new UnsupportedOperationException("Unmodifiable");
		}
		public final boolean offerFirst(Object/*{E}*/ value) {
			throw new UnsupportedOperationException("Unmodifiable");
		}
		public final boolean offerLast(Object/*{E}*/ value) {
			throw new UnsupportedOperationException("Unmodifiable");
		}
		public final Object/*{E}*/ removeFirst() {
			throw new UnsupportedOperationException("Unmodifiable");
		}
		public final Object/*{E}*/ removeLast() {
			throw new UnsupportedOperationException("Unmodifiable");
		}
		public final Object/*{E}*/ pollFirst() {
			throw new UnsupportedOperationException("Unmodifiable");
		}
		public final Object/*{E}*/ pollLast() {
			throw new UnsupportedOperationException("Unmodifiable");
		}
		public final boolean offer(Object/*{E}*/ value) {
			throw new UnsupportedOperationException("Unmodifiable");
		}
		public final Object/*{E}*/ remove() {
			throw new UnsupportedOperationException("Unmodifiable");
		}
		public final Object/*{E}*/ poll() {
			throw new UnsupportedOperationException("Unmodifiable");
		}
		public final void push(Object/*{E}*/ value) {
			throw new UnsupportedOperationException("Unmodifiable");
		}
		public final Object/*{E}*/ pop() {
			throw new UnsupportedOperationException("Unmodifiable");
		}
		public final boolean removeFirstOccurrence(Object value) {
			throw new UnsupportedOperationException("Unmodifiable");
		}
		public final boolean removeLastOccurrence(Object value) {
			throw new UnsupportedOperationException("Unmodifiable");
		}
		public final void addBefore(Node/*<E>*/ next, Object/*{E}*/ value) {
			throw new UnsupportedOperationException("Unmodifiable");
		}
		public final Iterator/*<E>*/ descendingIterator() {
			return new Iterator/*<E>*/() {
				private final Iterator/*<E>*/ i = _list.descendingIterator();
				public boolean hasNext() {
					return i.hasNext();
				}
				public Object/*{E}*/ next() {
					return i.next();
				}
				public void remove() {
					throw new UnsupportedOperationException();
				}
			};
		}
	}
	/**
	 * A shared view over a {@code FastList} (reads-write locks).
	 */
	private static final class Shared/*<E>*/ extends FastList
			/*<E>*/ implements List/*<E>*/, Reusable {
		private final FastList/*<E>*/ _list;
		private final ReadWriteLock _lock;
		private Shared(FastList/*<E>*/ list) {
			super((Void) null);
			_list = list;
			_lock = new ReentrantWriterPreferenceReadWriteLock();
		}
		public final FastCollection/*FastList<E>*/ unmodifiable() {
			return _list.unmodifiable();
		}
		public final FastCollection/*FastList<E>*/ shared() {
			return this;
		}
		public final boolean add(Object/*{E}*/ value) {
			boolean wasInterrupted = /*@JVM-1.1+@ true ? Thread.interrupted() : /**/false;
			try {
				final Sync w = _lock.writeLock();
				for(;;) {
					try {
						w.acquire();
						try {
							return _list.add(value);
						}
						finally {
							w.release();
						}
					}
					catch(final InterruptedException ex) {
						wasInterrupted = true;
					}
				}
			}
			finally {
				if(wasInterrupted) {
					Thread.currentThread().interrupt();
				}
			}
		}
		public final boolean remove(Object value) {
			boolean wasInterrupted = /*@JVM-1.1+@ true ? Thread.interrupted() : /**/false;
			try {
				final Sync w = _lock.writeLock();
				for(;;) {
					try {
						w.acquire();
						try {
							return _list.remove(value);
						}
						finally {
							w.release();
						}
					}
					catch(final InterruptedException ex) {
						wasInterrupted = true;
					}
				}
			}
			finally {
				if(wasInterrupted) {
					Thread.currentThread().interrupt();
				}
			}
		}
		public final void add(int index, Object/*{E}*/ element) {
			boolean wasInterrupted = /*@JVM-1.1+@ true ? Thread.interrupted() : /**/false;
			try {
				final Sync w = _lock.writeLock();
				for(;;) {
					try {
						w.acquire();
						try {
							_list.add(index, element);
						}
						finally {
							w.release();
						}
						break;
					}
					catch(final InterruptedException ex) {
						wasInterrupted = true;
					}
				}
			}
			finally {
				if(wasInterrupted) {
					Thread.currentThread().interrupt();
				}
			}
		}
		public final boolean addAll(Collection/*<? extends E>*/ values) {
			boolean wasInterrupted = /*@JVM-1.1+@ true ? Thread.interrupted() : /**/false;
			try {
				final Sync w = _lock.writeLock();
				for(;;) {
					try {
						w.acquire();
						try {
							return _list.addAll(values);
						}
						finally {
							w.release();
						}
					}
					catch(final InterruptedException ex) {
						wasInterrupted = true;
					}
				}
			}
			finally {
				if(wasInterrupted) {
					Thread.currentThread().interrupt();
				}
			}
		}
		public final boolean addAll(int index,
				Collection/*<? extends E>*/ values) {
			boolean wasInterrupted = /*@JVM-1.1+@ true ? Thread.interrupted() : /**/false;
			try {
				final Sync w = _lock.writeLock();
				for(;;) {
					try {
						w.acquire();
						try {
							return _list.addAll(index, values);
						}
						finally {
							w.release();
						}
					}
					catch(final InterruptedException ex) {
						wasInterrupted = true;
					}
				}
			}
			finally {
				if(wasInterrupted) {
					Thread.currentThread().interrupt();
				}
			}
		}
		public final void clear() {
			boolean wasInterrupted = /*@JVM-1.1+@ true ? Thread.interrupted() : /**/false;
			try {
				final Sync w = _lock.writeLock();
				for(;;) {
					try {
						w.acquire();
						try {
							_list.clear();
						}
						finally {
							w.release();
						}
						break;
					}
					catch(final InterruptedException ex) {
						wasInterrupted = true;
					}
				}
			}
			finally {
				if(wasInterrupted) {
					Thread.currentThread().interrupt();
				}
			}
		}
		public final boolean contains(Object value) {
			boolean wasInterrupted = /*@JVM-1.1+@ true ? Thread.interrupted() : /**/false;
			try {
				final Sync r = _lock.readLock();
				for(;;) {
					try {
						r.acquire();
						try {
							return _list.contains(value);
						}
						finally {
							r.release();
						}
					}
					catch(final InterruptedException ex) {
						wasInterrupted = true;
					}
				}
			}
			finally {
				if(wasInterrupted) {
					Thread.currentThread().interrupt();
				}
			}
		}
		public final boolean containsAll(Collection/*<?>*/ values) {
			boolean wasInterrupted = /*@JVM-1.1+@ true ? Thread.interrupted() : /**/false;
			try {
				final Sync r = _lock.readLock();
				for(;;) {
					try {
						r.acquire();
						try {
							return _list.containsAll(values);
						}
						finally {
							r.release();
						}
					}
					catch(final InterruptedException ex) {
						wasInterrupted = true;
					}
				}
			}
			finally {
				if(wasInterrupted) {
					Thread.currentThread().interrupt();
				}
			}
		}
		public final void delete(Record record) {
			boolean wasInterrupted = /*@JVM-1.1+@ true ? Thread.interrupted() : /**/false;
			try {
				final Sync w = _lock.writeLock();
				for(;;) {
					try {
						w.acquire();
						try {
							_list.delete(record);
						}
						finally {
							w.release();
						}
						break;
					}
					catch(final InterruptedException ex) {
						wasInterrupted = true;
					}
				}
			}
			finally {
				if(wasInterrupted) {
					Thread.currentThread().interrupt();
				}
			}
		}
		public final Object/*{E}*/ get(int index) {
			boolean wasInterrupted = /*@JVM-1.1+@ true ? Thread.interrupted() : /**/false;
			try {
				final Sync r = _lock.readLock();
				for(;;) {
					try {
						r.acquire();
						try {
							return _list.get(index);
						}
						finally {
							r.release();
						}
					}
					catch(final InterruptedException ex) {
						wasInterrupted = true;
					}
				}
			}
			finally {
				if(wasInterrupted) {
					Thread.currentThread().interrupt();
				}
			}
		}
		public final FastComparator/*<? super E>*/ getValueComparator() {
			boolean wasInterrupted = /*@JVM-1.1+@ true ? Thread.interrupted() : /**/false;
			try {
				final Sync r = _lock.readLock();
				for(;;) {
					try {
						r.acquire();
						try {
							return _list.getValueComparator();
						}
						finally {
							r.release();
						}
					}
					catch(final InterruptedException ex) {
						wasInterrupted = true;
					}
				}
			}
			finally {
				if(wasInterrupted) {
					Thread.currentThread().interrupt();
				}
			}
		}
		public final Record/*Node<E>*/ head() {
			boolean wasInterrupted = /*@JVM-1.1+@ true ? Thread.interrupted() : /**/false;
			try {
				final Sync r = _lock.readLock();
				for(;;) {
					try {
						r.acquire();
						try {
							return _list.head();
						}
						finally {
							r.release();
						}
					}
					catch(final InterruptedException ex) {
						wasInterrupted = true;
					}
				}
			}
			finally {
				if(wasInterrupted) {
					Thread.currentThread().interrupt();
				}
			}
		}
		public final int indexOf(Object value) {
			boolean wasInterrupted = /*@JVM-1.1+@ true ? Thread.interrupted() : /**/false;
			try {
				final Sync r = _lock.readLock();
				for(;;) {
					try {
						r.acquire();
						try {
							return _list.indexOf(value);
						}
						finally {
							r.release();
						}
					}
					catch(final InterruptedException ex) {
						wasInterrupted = true;
					}
				}
			}
			finally {
				if(wasInterrupted) {
					Thread.currentThread().interrupt();
				}
			}
		}
		public final boolean isEmpty() {
			boolean wasInterrupted = /*@JVM-1.1+@ true ? Thread.interrupted() : /**/false;
			try {
				final Sync r = _lock.readLock();
				for(;;) {
					try {
						r.acquire();
						try {
							return _list.isEmpty();
						}
						finally {
							r.release();
						}
					}
					catch(final InterruptedException ex) {
						wasInterrupted = true;
					}
				}
			}
			finally {
				if(wasInterrupted) {
					Thread.currentThread().interrupt();
				}
			}
		}
		public final int lastIndexOf(Object value) {
			boolean wasInterrupted = /*@JVM-1.1+@ true ? Thread.interrupted() : /**/false;
			try {
				final Sync r = _lock.readLock();
				for(;;) {
					try {
						r.acquire();
						try {
							return _list.lastIndexOf(value);
						}
						finally {
							r.release();
						}
					}
					catch(final InterruptedException ex) {
						wasInterrupted = true;
					}
				}
			}
			finally {
				if(wasInterrupted) {
					Thread.currentThread().interrupt();
				}
			}
		}
		public final Iterator/*<E>*/ iterator() {
			return listIterator(0);
		}
		public final ListIterator/*<E>*/ listIterator() {
			return listIterator(0);
		}
		public final ListIterator/*<E>*/ listIterator(int index) {
			boolean wasInterrupted = /*@JVM-1.1+@ true ? Thread.interrupted() : /**/false;
			try {
				final Sync r = _lock.readLock();
				for(;;) {
					try {
						r.acquire();
						try {
							final int sz = _list.size();
							if(index < 0 || index > sz)
								throw new IndexOutOfBoundsException(
										"index: " + index + ", size: " + sz);
							return new SharedListIterator(index);
						}
						finally {
							r.release();
						}
					}
					catch(final InterruptedException ex) {
						wasInterrupted = true;
					}
				}
			}
			finally {
				if(wasInterrupted) {
					Thread.currentThread().interrupt();
				}
			}
		}
		public final Object/*{E}*/ remove(int index) {
			boolean wasInterrupted = /*@JVM-1.1+@ true ? Thread.interrupted() : /**/false;
			try {
				final Sync w = _lock.writeLock();
				for(;;) {
					try {
						w.acquire();
						try {
							return _list.remove(index);
						}
						finally {
							w.release();
						}
					}
					catch(final InterruptedException ex) {
						wasInterrupted = true;
					}
				}
			}
			finally {
				if(wasInterrupted) {
					Thread.currentThread().interrupt();
				}
			}
		}
		public final boolean removeAll(Collection/*<?>*/ values) {
			boolean wasInterrupted = /*@JVM-1.1+@ true ? Thread.interrupted() : /**/false;
			try {
				final Sync w = _lock.writeLock();
				for(;;) {
					try {
						w.acquire();
						try {
							return _list.removeAll(values);
						}
						finally {
							w.release();
						}
					}
					catch(final InterruptedException ex) {
						wasInterrupted = true;
					}
				}
			}
			finally {
				if(wasInterrupted) {
					Thread.currentThread().interrupt();
				}
			}
		}
		public final void reset() {
			boolean wasInterrupted = /*@JVM-1.1+@ true ? Thread.interrupted() : /**/false;
			try {
				final Sync w = _lock.writeLock();
				for(;;) {
					try {
						w.acquire();
						try {
							_list.reset();
						}
						finally {
							w.release();
						}
						break;
					}
					catch(final InterruptedException ex) {
						wasInterrupted = true;
					}
				}
			}
			finally {
				if(wasInterrupted) {
					Thread.currentThread().interrupt();
				}
			}
		}
		public final boolean retainAll(Collection/*<?>*/ values) {
			boolean wasInterrupted = /*@JVM-1.1+@ true ? Thread.interrupted() : /**/false;
			try {
				final Sync w = _lock.writeLock();
				for(;;) {
					try {
						w.acquire();
						try {
							return _list.retainAll(values);
						}
						finally {
							w.release();
						}
					}
					catch(final InterruptedException ex) {
						wasInterrupted = true;
					}
				}
			}
			finally {
				if(wasInterrupted) {
					Thread.currentThread().interrupt();
				}
			}
		}
		public final Object/*{E}*/ set(int index, Object/*{E}*/ element) {
			boolean wasInterrupted = /*@JVM-1.1+@ true ? Thread.interrupted() : /**/false;
			try {
				final Sync w = _lock.writeLock();
				for(;;) {
					try {
						w.acquire();
						try {
							return _list.set(index, element);
						}
						finally {
							w.release();
						}
					}
					catch(final InterruptedException ex) {
						wasInterrupted = true;
					}
				}
			}
			finally {
				if(wasInterrupted) {
					Thread.currentThread().interrupt();
				}
			}
		}
		public final FastList/*<E>*/ setValueComparator(
				FastComparator/*<? super E>*/ comparator) {
			boolean wasInterrupted = /*@JVM-1.1+@ true ? Thread.interrupted() : /**/false;
			try {
				final Sync w = _lock.writeLock();
				for(;;) {
					try {
						w.acquire();
						try {
							return _list.setValueComparator(comparator);
						}
						finally {
							w.release();
						}
					}
					catch(final InterruptedException ex) {
						wasInterrupted = true;
					}
				}
			}
			finally {
				if(wasInterrupted) {
					Thread.currentThread().interrupt();
				}
			}
		}
		public final int size() {
			boolean wasInterrupted = /*@JVM-1.1+@ true ? Thread.interrupted() : /**/false;
			try {
				final Sync r = _lock.readLock();
				for(;;) {
					try {
						r.acquire();
						try {
							return _list.size();
						}
						finally {
							r.release();
						}
					}
					catch(final InterruptedException ex) {
						wasInterrupted = true;
					}
				}
			}
			finally {
				if(wasInterrupted) {
					Thread.currentThread().interrupt();
				}
			}
		}
		public final List/*<E>*/ subList(int fromIndex, int toIndex) {
			boolean wasInterrupted = /*@JVM-1.1+@ true ? Thread.interrupted() : /**/false;
			try {
				final Sync w = _lock.readLock();
				for(;;) {
					try {
						w.acquire();
						try {
							return _list.subList(fromIndex, toIndex);
						}
						finally {
							w.release();
						}
					}
					catch(final InterruptedException ex) {
						wasInterrupted = true;
					}
				}
			}
			finally {
				if(wasInterrupted) {
					Thread.currentThread().interrupt();
				}
			}
		}
		public final Record/*Node<E>*/ tail() {
			boolean wasInterrupted = /*@JVM-1.1+@ true ? Thread.interrupted() : /**/false;
			try {
				final Sync r = _lock.readLock();
				for(;;) {
					try {
						r.acquire();
						try {
							return _list.tail();
						}
						finally {
							r.release();
						}
					}
					catch(final InterruptedException ex) {
						wasInterrupted = true;
					}
				}
			}
			finally {
				if(wasInterrupted) {
					Thread.currentThread().interrupt();
				}
			}
		}
		public final Object[] toArray() {
			boolean wasInterrupted = /*@JVM-1.1+@ true ? Thread.interrupted() : /**/false;
			try {
				final Sync r = _lock.readLock();
				for(;;) {
					try {
						r.acquire();
						try {
							return _list.toArray();
						}
						finally {
							r.release();
						}
					}
					catch(final InterruptedException ex) {
						wasInterrupted = true;
					}
				}
			}
			finally {
				if(wasInterrupted) {
					Thread.currentThread().interrupt();
				}
			}
		}
		public final Object/*{<T> T}*/[] toArray(Object/*{T}*/[] array) {
			boolean wasInterrupted = /*@JVM-1.1+@ true ? Thread.interrupted() : /**/false;
			try {
				final Sync r = _lock.readLock();
				for(;;) {
					try {
						r.acquire();
						try {
							return _list.toArray(array);
						}
						finally {
							r.release();
						}
					}
					catch(final InterruptedException ex) {
						wasInterrupted = true;
					}
				}
			}
			finally {
				if(wasInterrupted) {
					Thread.currentThread().interrupt();
				}
			}
		}
		public final Object/*{E}*/ valueOf(Record record) {
			boolean wasInterrupted = /*@JVM-1.1+@ true ? Thread.interrupted() : /**/false;
			try {
				final Sync r = _lock.readLock();
				for(;;) {
					try {
						r.acquire();
						try {
							return _list.valueOf(record);
						}
						finally {
							r.release();
						}
					}
					catch(final InterruptedException ex) {
						wasInterrupted = true;
					}
				}
			}
			finally {
				if(wasInterrupted) {
					Thread.currentThread().interrupt();
				}
			}
		}
		public final Object/*{E}*/ getFirst() {
			boolean wasInterrupted = /*@JVM-1.1+@ true ? Thread.interrupted() : /**/false;
			try {
				final Sync r = _lock.readLock();
				for(;;) {
					try {
						r.acquire();
						try {
							return _list.getFirst();
						}
						finally {
							r.release();
						}
					}
					catch(final InterruptedException ex) {
						wasInterrupted = true;
					}
				}
			}
			finally {
				if(wasInterrupted) {
					Thread.currentThread().interrupt();
				}
			}
		}
		public final Object/*{E}*/ getLast() {
			boolean wasInterrupted = /*@JVM-1.1+@ true ? Thread.interrupted() : /**/false;
			try {
				final Sync r = _lock.readLock();
				for(;;) {
					try {
						r.acquire();
						try {
							return _list.getLast();
						}
						finally {
							r.release();
						}
					}
					catch(final InterruptedException ex) {
						wasInterrupted = true;
					}
				}
			}
			finally {
				if(wasInterrupted) {
					Thread.currentThread().interrupt();
				}
			}
		}
		public final Object/*{E}*/ peekFirst() {
			boolean wasInterrupted = /*@JVM-1.1+@ true ? Thread.interrupted() : /**/false;
			try {
				final Sync r = _lock.readLock();
				for(;;) {
					try {
						r.acquire();
						try {
							return _list.peekFirst();
						}
						finally {
							r.release();
						}
					}
					catch(final InterruptedException ex) {
						wasInterrupted = true;
					}
				}
			}
			finally {
				if(wasInterrupted) {
					Thread.currentThread().interrupt();
				}
			}
		}
		public final Object/*{E}*/ peekLast() {
			boolean wasInterrupted = /*@JVM-1.1+@ true ? Thread.interrupted() : /**/false;
			try {
				final Sync r = _lock.readLock();
				for(;;) {
					try {
						r.acquire();
						try {
							return _list.peekLast();
						}
						finally {
							r.release();
						}
					}
					catch(final InterruptedException ex) {
						wasInterrupted = true;
					}
				}
			}
			finally {
				if(wasInterrupted) {
					Thread.currentThread().interrupt();
				}
			}
		}
		public final Object/*{E}*/ peek() {
			return peekFirst();
		}
		public final Object/*{E}*/ element() {
			return getFirst();
		}
		public final void addFirst(Object/*{E}*/ value) {
			boolean wasInterrupted = /*@JVM-1.1+@ true ? Thread.interrupted() : /**/false;
			try {
				final Sync w = _lock.writeLock();
				for(;;) {
					try {
						w.acquire();
						try {
							_list.addFirst(value);
							return;
						}
						finally {
							w.release();
						}
					}
					catch(final InterruptedException ex) {
						wasInterrupted = true;
					}
				}
			}
			finally {
				if(wasInterrupted) {
					Thread.currentThread().interrupt();
				}
			}
		}
		public final void addLast(Object/*{E}*/ value) {
			boolean wasInterrupted = /*@JVM-1.1+@ true ? Thread.interrupted() : /**/false;
			try {
				final Sync w = _lock.writeLock();
				for(;;) {
					try {
						w.acquire();
						try {
							_list.addLast(value);
							return;
						}
						finally {
							w.release();
						}
					}
					catch(final InterruptedException ex) {
						wasInterrupted = true;
					}
				}
			}
			finally {
				if(wasInterrupted) {
					Thread.currentThread().interrupt();
				}
			}
		}
		public final boolean offerFirst(Object/*{E}*/ value) {
			addFirst(value);
			return true;
		}
		public final boolean offerLast(Object/*{E}*/ value) {
			addLast(value);
			return true;
		}
		public final Object/*{E}*/ removeFirst() {
			boolean wasInterrupted = /*@JVM-1.1+@ true ? Thread.interrupted() : /**/false;
			try {
				final Sync w = _lock.writeLock();
				for(;;) {
					try {
						w.acquire();
						try {
							return _list.removeFirst();
						}
						finally {
							w.release();
						}
					}
					catch(final InterruptedException ex) {
						wasInterrupted = true;
					}
				}
			}
			finally {
				if(wasInterrupted) {
					Thread.currentThread().interrupt();
				}
			}
		}
		public final Object/*{E}*/ removeLast() {
			boolean wasInterrupted = /*@JVM-1.1+@ true ? Thread.interrupted() : /**/false;
			try {
				final Sync w = _lock.writeLock();
				for(;;) {
					try {
						w.acquire();
						try {
							return _list.removeLast();
						}
						finally {
							w.release();
						}
					}
					catch(final InterruptedException ex) {
						wasInterrupted = true;
					}
				}
			}
			finally {
				if(wasInterrupted) {
					Thread.currentThread().interrupt();
				}
			}
		}
		public final Object/*{E}*/ pollFirst() {
			boolean wasInterrupted = /*@JVM-1.1+@ true ? Thread.interrupted() : /**/false;
			try {
				final Sync w = _lock.writeLock();
				for(;;) {
					try {
						w.acquire();
						try {
							return _list.pollFirst();
						}
						finally {
							w.release();
						}
					}
					catch(final InterruptedException ex) {
						wasInterrupted = true;
					}
				}
			}
			finally {
				if(wasInterrupted) {
					Thread.currentThread().interrupt();
				}
			}
		}
		public final Object/*{E}*/ pollLast() {
			boolean wasInterrupted = /*@JVM-1.1+@ true ? Thread.interrupted() : /**/false;
			try {
				final Sync w = _lock.writeLock();
				for(;;) {
					try {
						w.acquire();
						try {
							return _list.pollLast();
						}
						finally {
							w.release();
						}
					}
					catch(final InterruptedException ex) {
						wasInterrupted = true;
					}
				}
			}
			finally {
				if(wasInterrupted) {
					Thread.currentThread().interrupt();
				}
			}
		}
		public final boolean offer(Object/*{E}*/ value) {
			return add(value);
		}
		public final Object/*{E}*/ remove() {
			return removeFirst();
		}
		public final Object/*{E}*/ poll() {
			return pollFirst();
		}
		public final void push(Object/*{E}*/ value) {
			addFirst(value);
		}
		public final Object/*{E}*/ pop() {
			return removeFirst();
		}
		public final boolean removeFirstOccurrence(Object value) {
			return remove(value);
		}
		public final boolean removeLastOccurrence(Object value) {
			boolean wasInterrupted = /*@JVM-1.1+@ true ? Thread.interrupted() : /**/false;
			try {
				final Sync w = _lock.writeLock();
				for(;;) {
					try {
						w.acquire();
						try {
							return _list.removeLastOccurrence(value);
						}
						finally {
							w.release();
						}
					}
					catch(final InterruptedException ex) {
						wasInterrupted = true;
					}
				}
			}
			finally {
				if(wasInterrupted) {
					Thread.currentThread().interrupt();
				}
			}
		}
		public final void addBefore(Node/*<E>*/ next, Object/*{E}*/ value) {
			boolean wasInterrupted = /*@JVM-1.1+@ true ? Thread.interrupted() : /**/false;
			try {
				final Sync w = _lock.writeLock();
				for(;;) {
					try {
						w.acquire();
						try {
							_list.addBefore(next, value);
							return;
						}
						finally {
							w.release();
						}
					}
					catch(final InterruptedException ex) {
						wasInterrupted = true;
					}
				}
			}
			finally {
				if(wasInterrupted) {
					Thread.currentThread().interrupt();
				}
			}
		}
		public final Iterator/*<E>*/ descendingIterator() {
			return new Iterator/*<E>*/() {
				private final ListIterator/*<E>*/ _it = listIterator(size());
				public boolean hasNext() {
					return _it.hasPrevious();
				}
				public Object/*{E}*/ next() {
					return _it.previous();
				}
				public void remove() {
					_it.remove();
				}
			};
		}
		/**
		 * A thread-safe list iterator that performs per-operation locking.
		 * Read operations ({@code hasNext}, {@code next}, {@code hasPrevious},
		 * {@code previous}) acquire the read lock. Write operations
		 * ({@code remove}, {@code set}, {@code add}) acquire the write lock.
		 *
		 * <p>This iterator operates directly on the live collection and is
		 * <i>weakly consistent</i>: modifications by other threads between
		 * iterator method calls may affect iteration order and element visibility.
		 * Each individual operation is atomic.</p>
		 */
		private final class SharedListIterator implements ListIterator/*<E>*/ {
			private int _nextIndex;
			private int _currentIndex; // -1 when no current element
			private SharedListIterator(int startIndex) {
				_nextIndex = startIndex;
				_currentIndex = -1;
			}
			public boolean hasNext() {
				boolean wasInterrupted = /*@JVM-1.1+@ true ? Thread.interrupted() : /**/false;
				try {
					final Sync r = _lock.readLock();
					for(;;) {
						try {
							r.acquire();
							try {
								return _nextIndex < _list.size();
							}
							finally {
								r.release();
							}
						}
						catch(final InterruptedException ex) {
							wasInterrupted = true;
						}
					}
				}
				finally {
					if(wasInterrupted) {
						Thread.currentThread().interrupt();
					}
				}
			}
			public Object/*{E}*/ next() {
				boolean wasInterrupted = /*@JVM-1.1+@ true ? Thread.interrupted() : /**/false;
				try {
					final Sync r = _lock.readLock();
					for(;;) {
						try {
							r.acquire();
							try {
								if(_nextIndex >= _list.size())
									throw new NoSuchElementException();
								_currentIndex = _nextIndex;
								return _list.get(_nextIndex++);
							}
							finally {
								r.release();
							}
						}
						catch(final InterruptedException ex) {
							wasInterrupted = true;
						}
					}
				}
				finally {
					if(wasInterrupted) {
						Thread.currentThread().interrupt();
					}
				}
			}
			public boolean hasPrevious() {
				boolean wasInterrupted = /*@JVM-1.1+@ true ? Thread.interrupted() : /**/false;
				try {
					final Sync r = _lock.readLock();
					for(;;) {
						try {
							r.acquire();
							try {
								return _nextIndex > 0
										&& _nextIndex <= _list.size();
							}
							finally {
								r.release();
							}
						}
						catch(final InterruptedException ex) {
							wasInterrupted = true;
						}
					}
				}
				finally {
					if(wasInterrupted) {
						Thread.currentThread().interrupt();
					}
				}
			}
			public Object/*{E}*/ previous() {
				boolean wasInterrupted = /*@JVM-1.1+@ true ? Thread.interrupted() : /**/false;
				try {
					final Sync r = _lock.readLock();
					for(;;) {
						try {
							r.acquire();
							try {
								if(_nextIndex <= 0)
									throw new NoSuchElementException();
								final int idx = --_nextIndex;
								if(idx >= _list.size())
									throw new NoSuchElementException();
								_currentIndex = idx;
								return _list.get(idx);
							}
							finally {
								r.release();
							}
						}
						catch(final InterruptedException ex) {
							wasInterrupted = true;
						}
					}
				}
				finally {
					if(wasInterrupted) {
						Thread.currentThread().interrupt();
					}
				}
			}
			public int nextIndex() {
				return _nextIndex;
			}
			public int previousIndex() {
				return _nextIndex - 1;
			}
			public void remove() {
				boolean wasInterrupted = /*@JVM-1.1+@ true ? Thread.interrupted() : /**/false;
				try {
					final Sync w = _lock.writeLock();
					for(;;) {
						try {
							w.acquire();
							try {
								if(_currentIndex < 0)
									throw new IllegalStateException();
								_list.remove(_currentIndex);
								if(_currentIndex < _nextIndex) {
									--_nextIndex;
								}
								_currentIndex = -1;
							}
							finally {
								w.release();
							}
							break;
						}
						catch(final InterruptedException ex) {
							wasInterrupted = true;
						}
					}
				}
				finally {
					if(wasInterrupted) {
						Thread.currentThread().interrupt();
					}
				}
			}
			public void set(Object/*{E}*/ value) {
				boolean wasInterrupted = /*@JVM-1.1+@ true ? Thread.interrupted() : /**/false;
				try {
					final Sync w = _lock.writeLock();
					for(;;) {
						try {
							w.acquire();
							try {
								if(_currentIndex < 0)
									throw new IllegalStateException();
								_list.set(_currentIndex, value);
							}
							finally {
								w.release();
							}
							break;
						}
						catch(final InterruptedException ex) {
							wasInterrupted = true;
						}
					}
				}
				finally {
					if(wasInterrupted) {
						Thread.currentThread().interrupt();
					}
				}
			}
			public void add(Object/*{E}*/ value) {
				boolean wasInterrupted = /*@JVM-1.1+@ true ? Thread.interrupted() : /**/false;
				try {
					final Sync w = _lock.writeLock();
					for(;;) {
						try {
							w.acquire();
							try {
								_list.add(_nextIndex++, value);
								_currentIndex = -1;
							}
							finally {
								w.release();
							}
							break;
						}
						catch(final InterruptedException ex) {
							wasInterrupted = true;
						}
					}
				}
				finally {
					if(wasInterrupted) {
						Thread.currentThread().interrupt();
					}
				}
			}
		}
	}
}