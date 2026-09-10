/*
 * Javolution - Java(TM) Solution for Real-Time and Embedded Systems
 * Copyright (C) 2005 - Javolution (http://javolution.org/)
 * All rights reserved.
 *
 * Permission to use, copy, modify, and distribute this software is
 * freely granted, provided that this notice is preserved.
 */
package _templates.javolution.util;
import _templates.java.lang.IllegalStateException;
import _templates.java.lang.UnsupportedOperationException;
import _templates.java.util.Collection;
import _templates.java.util.Deque;
import _templates.java.util.Iterator;
import _templates.java.util.List;
import _templates.java.util.ListIterator;
import _templates.java.util.NoSuchElementException;
import _templates.java.util.Queue;
import _templates.java.util.RandomAccess;
import _templates.javax.realtime.MemoryArea;
import _templates.javolution.context.ObjectFactory;
import _templates.javolution.context.PersistentContext;
import _templates.javolution.lang.MathLib;
import _templates.javolution.lang.Reusable;
import _templates.javolution.util.concurrent.locks.ReadWriteLock;
import _templates.javolution.util.concurrent.locks.ReentrantWriterPreferenceReadWriteLock;
import _templates.javolution.util.concurrent.locks.Sync;
/**
 * <p> This class represents a random access collection with real-time behavior
 *     (smooth capacity increase).</p>
 *     <img src="doc-files/list-add.png"/>
 *
 * <p> This class has the following advantages over the widely used
 *     <code>java.util.ArrayList</code>:
 *     <ul>
 *         <li>No large array allocation (for large collections multi-dimensional
 *             arrays are employed). The garbage collector is not stressed with
 *             large chunk of memory to allocate (likely to trigger a
 *             full garbage collection due to memory fragmentation).</li>
 *         <li>Support concurrent access/iteration/modification without synchronization
 *             if marked {@link FastCollection#shared shared}. </li>
 *     </ul></p>
 *
 * <p> Iterations over the {@link FastTable} values are faster when
 *     performed using the {@link #get} method rather than using collection
 *     records or iterators:[code]
 * final int n = table.size();
 * for(int i = 0; i < n; ++i) {
 *     table.get(i);
 * }[/code]</p>
 *
 * <p> {@link FastTable} supports {@link #sort sorting} in place (quick sort)
 *     using the {@link FastCollection#getValueComparator() value comparator}
 *     for the table (no object or array allocation when sorting).</p>
 *
 * <p> This table accepts <code>null</code> elements. Applications requiring
 *     non-null elements must validate them before insertion.</p>
 *
 * @author <a href="mailto:jean-marie@dautelle.com">Jean-Marie Dautelle</a>
 * @version 7.0.0, September 9, 2026
 */
public class FastTable/*<E>*/ extends FastCollection/*<E>*/ implements
		List/*<E>*/, Queue/*<E>*/, Deque/*<E>*/, Reusable, RandomAccess {
	/**
	 * Holds the factory for this fast table.
	 */
	private static final ObjectFactory FACTORY = new ObjectFactory() {
		public Object create() {
			return new FastTable();
		}
	};
	// We do a full resize (and copy) only when the capacity is less than C1.
	// For large collections, multi-dimensional arrays are employed.
	private static final int B0 = 4; // Initial capacity in bits.
	private static final int C0 = 1 << B0; // Initial capacity (16)
	private static final int B1 = 10; // Low array maximum capacity in bits.
	private static final int C1 = 1 << B1; // Low array maximum capacity (1024).
	private static final int M1 = C1 - 1; // Mask.
	/**
	 * Resizes up to 1024 maximum (16, 32, 64, 128, 256, 512, 1024).
	 */
	private transient Object/*{E}*/[] _low;
	/**
	 * For larger capacity use multi-dimensional array.
	 */
	private transient Object/*{E}*/[][] _high;
	/**
	 * Holds the current capacity.
	 */
	private transient int _capacity;
	/**
	 * Holds the current size.
	 */
	private transient int _size;
	/**
	 * Holds the value comparator.
	 */
	private transient FastComparator/*<? super E>*/ _valueComparator;
	/**
	 * Creates a table of small initial capacity.
	 */
	public FastTable() {
		_valueComparator = FastComparator.DEFAULT;
		_capacity = C0;
		_low = (Object/*{E}*/[]) new Object[C0];
		_high = (Object/*{E}*/[][]) new Object[1][];
		_high[0] = _low;
	}
	/**
	 * Creates a table of specified initial capacity; unless the table size
	 * reaches the specified capacity, operations on this table will not
	 * allocate memory (no lazy object creation).
	 *
	 * @param capacity the initial capacity.
	 */
	public FastTable(int capacity) {
		this();
		while(capacity > _capacity) {
			increaseCapacity();
		}
	}
	/**
	 * Creates a persistent table associated to the specified unique identifier
	 * (convenience method).
	 *
	 * @param id the unique identifier for this map.
	 * @throws IllegalArgumentException if the identifier is not unique.
	 * @see _templates.javolution.context.PersistentContext.Reference
	 */
	public FastTable(String id) {
		this();
		new PersistentContext.Reference(id, this) {
			protected void notifyChange() {
				FastTable.this.clear();
				FastTable.this.addAll((FastList) this.get());
			}
		};
	}
	/**
	 * Creates a table containing the specified values, in the order they
	 * are returned by the collection's iterator.
	 *
	 * @param values the values to be placed into this table.
	 */
	public FastTable(Collection/*<? extends E>*/ values) {
		this(values.size());
		addAll(values);
	}
	/**
	 * No-op constructor for internal view classes ({@link Unmodifiable}, {@link Shared}).
	 * Skips all internal data structure initialization.
	 */
	private FastTable(Void unused) {
		// No initialization — view delegates everything to backing list.
	}
	/**
	 * Returns a new, preallocated or {@link #recycle recycled} table instance
	 * (on the stack when executing in a {@link _templates.javolution.context.StackContext
	 * StackContext}).
	 *
	 * @return a new, preallocated or recycled table instance.
	 */
	public static/*<E>*/FastTable/*<E>*/ newInstance() {
		return (FastTable/*<E>*/) FACTORY.object();
	}
	/**
	 * Recycles a table {@link #newInstance() instance} immediately
	 * (on the stack when executing in a {@link _templates.javolution.context.StackContext
	 * StackContext}).
	 */
	public static void recycle(FastTable instance) {
		FACTORY.recycle(instance);
	}
	/**
	 * Sets the size of this table. If the specified size is greater than
	 * the current size then <code>null</code> elements are added; otherwise
	 * the last elements are removed until the desired size is reached.
	 *
	 * @param size the new size.
	 * @throws IllegalArgumentException if <code>size</code> less than 0
	 */
	public void setSize(int size) {
		if(size < 0)
			throw new IllegalArgumentException("size must be >= 0");
		if(_size < size) {
			while(_size < size) { // Adds null elements.
				addLast(null);
			}
		}
		else {
			while(_size > size) { // Removes last elements.
				removeLast();
			}
		}
	}
	/**
	 * Returns the element at the specified index.
	 *
	 * @param index index of value to return.
	 * @return the value at the specified position in this list.
	 * @throws IndexOutOfBoundsException if <code>index < 0 || index >= size()</code>
	 */
	public Object/*{E}*/ get(int index) { // Short to be inlined.
		if(index < 0 || index >= _size)
			throw new IndexOutOfBoundsException(
					"index: " + index + ", size: " + _size);
		return index < C1 ? _low[index] : _high[index >> B1][index & M1];
	}
	/**
	 * Replaces the value at the specified position in this table with the
	 * specified value.
	 *
	 * @param index index of value to replace.
	 * @param value value to be stored at the specified position.
	 * @return previous value.
	 * @throws IndexOutOfBoundsException if <code>index < 0 || index >= size()</code>
	 */
	public Object/*{E}*/ set(int index, Object/*{E}*/ value) {
		if(index < 0 || index >= _size)
			throw new IndexOutOfBoundsException(
					"index: " + index + ", size: " + _size);
		final Object/*{E}*/[] low = _high[index >> B1];
		final Object/*{E}*/ previous = low[index & M1];
		low[index & M1] = value;
		return previous;
	}
	/**
	 * Appends the specified value to the end of this table.
	 *
	 * @param value the value to be appended to this table.
	 * @return <code>true</code> (as per the general contract of the
	 *         <code>Collection.add</code> method).
	 */
	public boolean add(Object/*{E}*/ value) {
		if(_size >= _capacity) {
			increaseCapacity();
		}
		_high[_size >> B1][_size & M1] = value;
		++_size;
		return true;
	}
	/**
	 * Returns the first value of this table.
	 *
	 * @return this table first value.
	 * @throws NoSuchElementException if this table is empty.
	 */
	public Object/*{E}*/ getFirst() {
		if(_size == 0)
			throw new NoSuchElementException();
		return _low[0];
	}
	/**
	 * Returns the last value of this table.
	 *
	 * @return this table last value.
	 * @throws NoSuchElementException if this table is empty.
	 */
	public Object/*{E}*/ getLast() {
		if(_size == 0)
			throw new NoSuchElementException();
		return get(_size - 1);
	}
	/**
	 * Appends the specified value to the end of this table <i>(fast)</i>.
	 *
	 * @param value the value to be added.
	 */
	public void addLast(Object/*{E}*/ value) {
		add(value);
	}
	/**
	 * Removes and returns the last value of this table <i>(fast)</i>.
	 *
	 * @return this table's last value before this call.
	 * @throws NoSuchElementException if this table is empty.
	 */
	public Object/*{E}*/ removeLast() {
		if(_size == 0)
			throw new NoSuchElementException();
		--_size; // No need for volatile, removal are not thread-safe.
		final Object/*{E}*/[] low = _high[_size >> B1];
		final Object/*{E}*/ previous = low[_size & M1];
		low[_size & M1] = null;
		return previous;
	}
	/**
	 * Inserts the specified value at the beginning of this table.
	 *
	 * @param value the value to be inserted.
	 */
	public void addFirst(Object/*{E}*/ value) {
		add(0, value);
	}
	/**
	 * Removes and returns the first value of this table.
	 *
	 * @return this table's first value before this call.
	 * @throws NoSuchElementException if this table is empty.
	 */
	public Object/*{E}*/ removeFirst() {
		if(_size == 0)
			throw new NoSuchElementException();
		return remove(0);
	}
	/**
	 * Inserts the specified value at the front of this table.
	 *
	 * @param value the value to add.
	 * @return <code>true</code>.
	 */
	public boolean offerFirst(Object/*{E}*/ value) {
		addFirst(value);
		return true;
	}
	/**
	 * Inserts the specified value at the end of this table.
	 *
	 * @param value the value to add.
	 * @return <code>true</code>.
	 */
	public boolean offerLast(Object/*{E}*/ value) {
		addLast(value);
		return true;
	}
	/**
	 * Retrieves and removes the first value of this table,
	 * or returns <code>null</code> if this table is empty.
	 *
	 * @return the first value of this table, or <code>null</code> if this table is empty.
	 */
	public Object/*{E}*/ pollFirst() {
		return _size == 0 ? null : removeFirst();
	}
	/**
	 * Retrieves and removes the last value of this table,
	 * or returns <code>null</code> if this table is empty.
	 *
	 * @return the last value of this table, or <code>null</code> if this table is empty.
	 */
	public Object/*{E}*/ pollLast() {
		return _size == 0 ? null : removeLast();
	}
	/**
	 * Retrieves, but does not remove, the first value of this table,
	 * or returns <code>null</code> if this table is empty.
	 *
	 * @return the first value of this table, or <code>null</code> if this table is empty.
	 */
	public Object/*{E}*/ peekFirst() {
		return _size == 0 ? null : (Object/*{E}*/) _low[0];
	}
	/**
	 * Retrieves, but does not remove, the last value of this table,
	 * or returns <code>null</code> if this table is empty.
	 *
	 * @return the last value of this table, or <code>null</code> if this table is empty.
	 */
	public Object/*{E}*/ peekLast() {
		return _size == 0 ? null : (Object/*{E}*/) get(_size - 1);
	}
	/**
	 * Removes the first occurrence of the specified value from this table.
	 *
	 * @param value the value to be removed from this table, if present.
	 * @return <code>true</code> if this table contained the specified value.
	 */
	public boolean removeFirstOccurrence(Object value) {
		return remove(value);
	}
	/**
	 * Removes the last occurrence of the specified value from this table.
	 *
	 * @param value the value to be removed from this table, if present.
	 * @return <code>true</code> if this table contained the specified value.
	 */
	public boolean removeLastOccurrence(Object value) {
		final int i = lastIndexOf(value);
		if(i >= 0) {
			remove(i);
			return true;
		}
		return false;
	}
	/**
	 * Inserts the specified value as the tail (last value) of this table.
	 *
	 * @param value the value to add.
	 * @return <code>true</code>.
	 */
	public boolean offer(Object/*{E}*/ value) {
		return add(value);
	}
	/**
	 * Retrieves and removes the head (first value) of this table.
	 *
	 * @return the head of this table.
	 * @throws NoSuchElementException if this table is empty.
	 */
	public Object/*{E}*/ remove() {
		return removeFirst();
	}
	/**
	 * Retrieves and removes the head (first value) of this table,
	 * or returns <code>null</code> if this table is empty.
	 *
	 * @return the head of this table, or <code>null</code> if this table is empty.
	 */
	public Object/*{E}*/ poll() {
		return pollFirst();
	}
	/**
	 * Retrieves, but does not remove, the head (first value) of this table.
	 *
	 * @return the head of this table.
	 * @throws NoSuchElementException if this table is empty.
	 */
	public Object/*{E}*/ element() {
		return getFirst();
	}
	/**
	 * Retrieves, but does not remove, the head (first value) of this table,
	 * or returns <code>null</code> if this table is empty.
	 *
	 * @return the head of this table, or <code>null</code> if this table is empty.
	 */
	public Object/*{E}*/ peek() {
		return peekFirst();
	}
	/**
	 * Pushes a value onto the stack represented by this table (in other words,
	 * at the head of this table).
	 *
	 * @param value the value to push.
	 */
	public void push(Object/*{E}*/ value) {
		addFirst(value);
	}
	/**
	 * Pops a value from the stack represented by this table. In other words,
	 * removes and returns the first value of this table.
	 *
	 * @return the value at the front of this table (which is the top of the stack).
	 * @throws NoSuchElementException if this table is empty.
	 */
	public Object/*{E}*/ pop() {
		return removeFirst();
	}
	/**
	 * Returns an iterator over the values in this table in reverse sequential order.
	 * The values will be returned in order from last (tail) to first (head).
	 *
	 * @return an iterator over the values in this table in reverse sequence.
	 */
	public Iterator/*<E>*/ descendingIterator() {
		return new Iterator/*<E>*/() {
			private int _cursor = _size - 1;
			private int _lastReturned = -1;
			public boolean hasNext() {
				return _cursor >= 0;
			}
			public Object/*{E}*/ next() {
				if(_cursor < 0)
					throw new NoSuchElementException();
				_lastReturned = _cursor;
				return get(_cursor--);
			}
			public void remove() {
				if(_lastReturned < 0)
					throw new IllegalStateException();
				FastTable.this.remove(_lastReturned);
				_lastReturned = -1;
			}
		};
	}
	private static final Object[] NULL_BLOCK = new Object[C1];
	// Overrides.
	public void clear() {
		for(int i = 0; i < _size; i += C1) {
			final int count = Math.min(_size - i, C1);
			final Object/*{E}*/[] low = _high[i >> B1];
			System.arraycopy(NULL_BLOCK, 0, low, 0, count);
		}
		_size = 0; // No need for volatile, removal are not thread-safe.
	}
	// Implements Reusable interface.
	public void reset() {
		clear();
		setValueComparator(FastComparator.DEFAULT);
	}
	/**
	 * Inserts all of the values in the specified collection into this
	 * table at the specified position. Shifts the value currently at that
	 * position (if any) and any subsequent values to the right
	 * (increases their indices).
	 * A <code>null</code> collection is treated as empty.
	 *
	 * <p>Note: If this method is used concurrent access must be synchronized
	 *          (the table is no more thread-safe).</p>
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
			throw new IndexOutOfBoundsException(
					"index: " + index + ", size: " + _size);
		final int shift;
		if(values == null || (shift = values.size()) == 0)
			return false;
		shiftRight(index, shift);
		final Iterator/*<? extends E>*/ valuesIterator = values.iterator();
		final int n = index + shift;
		for(int i = index; i < n; ++i) {
			_high[i >> B1][i & M1] = valuesIterator.next();
		}
		_size += shift; // Increases size last (thread-safe)
		return shift != 0;
	}
	public boolean addAll(Collection/*<? extends E>*/ values) {
		return addAll(_size, values);
	}
	/**
	 * Inserts the specified value at the specified position in this table.
	 * Shifts the value currently at that position
	 * (if any) and any subsequent values to the right (adds one to their
	 * indices).
	 *
	 * <p>Note: If this method is used concurrent access must be synchronized
	 *          (the table is no more thread-safe).</p>
	 *
	 * @param index the index at which the specified value is to be inserted.
	 * @param value the value to be inserted.
	 * @throws IndexOutOfBoundsException if <code>index < 0 || index > size()</code>
	 */
	public void add(int index, Object/*{E}*/ value) {
		if(index < 0 || index > _size)
			throw new IndexOutOfBoundsException(
					"index: " + index + ", size: " + _size);
		shiftRight(index, 1);
		_high[index >> B1][index & M1] = value;
		++_size;
	}
	/**
	 * Removes the value at the specified position from this table.
	 * Shifts any subsequent values to the left (subtracts one
	 * from their indices). Returns the value that was removed from the
	 * table.
	 *
	 * <p>Note: If this method is used concurrent access must be synchronized
	 *          (the table is no more thread-safe).</p>
	 *
	 * @param index the index of the value to removed.
	 * @return the value previously at the specified position.
	 * @throws IndexOutOfBoundsException if <code>(index < 0) ||
	 *         (index >= size())</code>
	 */
	public Object/*{E}*/ remove(int index) {
		final Object/*{E}*/ previous = get(index);
		shiftLeft(index + 1, 1);
		--_size; // No need for volatile, removal are not thread-safe.
		_high[_size >> B1][_size & M1] = null; // Deallocates for GC.
		return previous;
	}
	public boolean remove(Object value) {
		final int index = indexOf(value);
		if(index >= 0) {
			remove(index);
			return true;
		}
		return false;
	}
	/**
	 * Removes the values between <code>[fromIndex..toIndex]</code> from
	 * this table.
	 *
	 * <p>Note: If this method is used concurrent access must be synchronized
	 *          (the table is no more thread-safe).</p>
	 *
	 * @param fromIndex the beginning index, inclusive.
	 * @param toIndex the ending index, exclusive.
	 * @throws IndexOutOfBoundsException if <code>fromIndex < 0 || toIndex < 0 ||
	 *         fromIndex > toIndex || toIndex > size()</code>
	 */
	public void removeRange(int fromIndex, int toIndex) {
		if(fromIndex < 0 || toIndex < 0 || fromIndex > toIndex
				|| toIndex > _size)
			throw new IndexOutOfBoundsException("fromIndex: " + fromIndex
					+ ", toIndex: " + toIndex + ", size: " + _size);
		final int shift = toIndex - fromIndex;
		if(shift == 0)
			return;
		shiftLeft(toIndex, shift);
		_size -= shift; // No need for volatile, removal are not thread-safe.
		final int n = _size + shift;
		for(int i = _size; i < n; ++i) {
			_high[i >> B1][i & M1] = null; // Deallocates for GC.
		}
	}
	/**
	 * Returns the index in this table of the first occurrence of the specified
	 * value, or <code>-1</code> if this table does not contain this value.
	 *
	 * @param value the value to search for.
	 * @return the index in this table of the first occurrence of the specified
	 *         value, or <code>-1</code> if this table does not contain this value.
	 */
	public int indexOf(Object value) {
		final FastComparator comp = getValueComparator();
		for(int i = 0; i < _size;) {
			final Object/*{E}*/[] low = _high[i >> B1];
			final int count = Math.min(low.length, _size - i);
			for(int j = 0; j < count; ++j) {
				if(comp == FastComparator.DEFAULT ? defaultEquals(value, low[j])
						: comp.areEqual(value, low[j]))
					return i + j;
			}
			i += count;
		}
		return -1;
	}
	/**
	 * Returns the index in this table of the last occurrence of the specified
	 * value, or <code>-1</code> if this table does not contain this value.
	 *
	 * @param value the value to search for.
	 * @return the index in this table of the last occurrence of the specified
	 *         value, or <code>-1</code> if this table does not contain this value.
	 */
	public int lastIndexOf(Object value) {
		final FastComparator comp = getValueComparator();
		for(int i = _size - 1; i >= 0;) {
			final Object/*{E}*/[] low = _high[i >> B1];
			final int count = (i & M1) + 1;
			for(int j = count; --j >= 0;) {
				if(comp == FastComparator.DEFAULT ? defaultEquals(value, low[j])
						: comp.areEqual(value, low[j]))
					return i + j - count + 1;
			}
			i -= count;
		}
		return -1;
	}
	/**
	 * Returns an iterator over the elements in this list
	 * (allocated on the stack when executed in a
	 * {@link _templates.javolution.context.StackContext StackContext}).
	 *
	 * @return an iterator over this list values.
	 */
	public Iterator/*<E>*/ iterator() {
		return FastTableIterator.valueOf(this, 0, 0, _size);
	}
	/**
	 * Returns a list iterator over the elements in this list
	 * (allocated on the stack when executed in a
	 * {@link _templates.javolution.context.StackContext StackContext}).
	 *
	 * @return an iterator over this list values.
	 */
	public ListIterator/*<E>*/ listIterator() {
		return FastTableIterator.valueOf(this, 0, 0, _size);
	}
	/**
	 * Returns a list iterator from the specified position
	 * (allocated on the stack when executed in a
	 * {@link _templates.javolution.context.StackContext StackContext}).
	 *
	 * @param index the index of first value to be returned from the
	 *        list iterator (by a call to the <code>next</code> method).
	 * @return a list iterator of the values in this table
	 *         starting at the specified position in this list.
	 * @throws IndexOutOfBoundsException if <code>index < 0 || index > size()</code>
	 */
	public ListIterator/*<E>*/ listIterator(int index) {
		if(index < 0 || index > _size)
			throw new IndexOutOfBoundsException();
		return FastTableIterator.valueOf(this, index, 0, _size);
	}
	/**
	 * Returns a view of the portion of this list between the specified
	 * indexes (instance of {@link FastList} allocated from the "stack" when
	 * executing in a {@link _templates.javolution.context.StackContext StackContext}).
	 * If the specified indexes are equal, the returned list is empty.
	 * The returned list is backed by this list, so non-structural changes in
	 * the returned list are reflected in this list, and vice-versa.
	 *
	 * This method eliminates the need for explicit range operations (of
	 * the sort that commonly exist for arrays). Any operation that expects
	 * a list can be used as a range operation by passing a subList view
	 * instead of a whole list.  For example, the following idiom
	 * removes a range of values from a list: [code]
	 * list.subList(from, to).clear();[/code]
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
		return SubTable.valueOf(this, fromIndex, toIndex - fromIndex);
	}
	/**
	 * Reduces the capacity of this table to the current size (minimize
	 * storage space).
	 */
	public void trimToSize() {
		while(_capacity > C1 && _capacity - _size >= C1) {
			_high[(_capacity >> B1) - 1] = null;
			_capacity -= C1;
		}
		if(_capacity <= C1 && _size < _capacity) {
			final int newCap = Math.max(C0, MathLib
					.highestOneBit/*Integer.highestOneBit*/(_size - 1) << 1);
			if(newCap < _capacity) {
				final Object/*{E}*/[] tmp = (Object/*{E}*/[]) new Object[newCap];
				System.arraycopy(_low, 0, tmp, 0, _size);
				_low = tmp;
				_high[0] = tmp;
				_capacity = newCap;
			}
		}
	}
	/**
	 * Sorts this table in place (quick sort) using this table
	 * {@link FastCollection#getValueComparator() value comparator}
	 * (smallest first).
	 *
	 * @return <code>this</code>
	 */
	public FastTable/*<E>*/ sort() {
		if(_size > 1) {
			quicksort(0, _size - 1, getValueComparator());
		}
		return this;
	}
	// From Wikipedia Quick Sort - http://en.wikipedia.org/wiki/Quicksort
	private final void quicksort(int first, int last, FastComparator cmp) {
		while(first < last) {
			if(last - first < 16) {
				insertionSort(first, last, cmp);
				return;
			}
			final int pivIndex = partition(first, last, cmp);
			if(pivIndex - first < last - pivIndex) {
				quicksort(first, pivIndex - 1, cmp);
				first = pivIndex + 1;
			}
			else {
				quicksort(pivIndex + 1, last, cmp);
				last = pivIndex - 1;
			}
		}
	}
	private final void insertionSort(int first, int last, FastComparator cmp) {
		for(int i = first + 1; i <= last; ++i) {
			final Object/*{E}*/ key = get(i);
			int j = i - 1;
			while(j >= first && cmp.compare(get(j), key) > 0) {
				set(j + 1, get(j));
				--j;
			}
			set(j + 1, key);
		}
	}
	private final int partition(int f, int l, FastComparator cmp) {
		final int mid = f + l >>> 1;
		if(cmp.compare(get(f), get(mid)) > 0) {
			swap(f, mid);
		}
		if(cmp.compare(get(f), get(l)) > 0) {
			swap(f, l);
		}
		if(cmp.compare(get(mid), get(l)) > 0) {
			swap(mid, l);
		}
		swap(mid, f);
		final Object/*{E}*/ piv = get(f);
		int up = f, down = l;
		do {
			while(cmp.compare(get(up), piv) <= 0 && up < l) {
				++up;
			}
			while(cmp.compare(get(down), piv) > 0 && down > f) {
				--down;
			}
			if(up < down) { // Swaps.
				final Object/*{E}*/ temp = get(up);
				set(up, get(down));
				set(down, temp);
			}
		}
		while(down > up);
		set(f, get(down));
		set(down, piv);
		return down;
	}
	private final void swap(int i, int j) {
		final Object/*{E}*/ tmp = get(i);
		set(i, get(j));
		set(j, tmp);
	}
	/**
	 * Sets the comparator to use for value equality or comparison if the
	 * collection is ordered (see {@link #sort()}).
	 *
	 * @param comparator the value comparator.
	 * @return <code>this</code>
	 */
	public FastTable/*<E>*/ setValueComparator(
			FastComparator/*<? super E>*/ comparator) {
		_valueComparator = comparator;
		return this;
	}
	// Overrides.
	public FastComparator/*<? super E>*/ getValueComparator() {
		return _valueComparator;
	}
	// Implements FastAbstractList abstract method.
	public int size() {
		return _size;
	}
	// Implements FastAbstractList abstract method.
	public boolean isEmpty() {
		return _size == 0;
	}
	// Implements FastAbstractList abstract method.
	public Record head() {
		return Index.valueOf(-1);
	}
	// Implements FastAbstractList abstract method.
	public Record tail() {
		return Index.valueOf(_size);
	}
	// Implements FastAbstractList abstract method.
	public Object/*{E}*/ valueOf(Record record) {
		return get(((Index) record).intValue());
	}
	// Implements FastAbstractList abstract method.
	public void delete(Record record) {
		remove(((Index) record).intValue());
	}
	// Overrides to return a list (JDK1.5+).
	public FastCollection/*FastTable<E>*/ unmodifiable() {
		return new Unmodifiable/*<E>*/(this);
	}
	// Overrides to return a list (JDK1.5+).
	public FastCollection/*FastTable<E>*/ shared() {
		return new Shared/*<E>*/(this);
	}
	// Overrides (optimization).
	public boolean contains(Object value) {
		return indexOf(value) >= 0;
	}
	/**
	 * Increases the capacity of this table, if necessary, to ensure that it
	 * can hold at least the number of elements specified by the minimum
	 * capacity argument. This operation does not change the table size or
	 * elements. Negative minimum capacities have no effect.
	 *
	 * @param minCapacity the desired minimum capacity.
	 */
	public void ensureCapacity(int minCapacity) {
		while(minCapacity > _capacity) {
			increaseCapacity();
		}
	}
	/**
	 * Increases this table capacity.
	 */
	private final void increaseCapacity() {
		MemoryArea.getMemoryArea(this).executeInArea(new Runnable() {
			public final void run() {
				if(_capacity < C1) { // For small capacity, resize.
					_capacity <<= 1;
					final Object/*{E}*/[] tmp = (Object/*{E}*/[]) new Object[_capacity];
					System.arraycopy(_low, 0, tmp, 0, _size);
					_low = tmp;
					_high[0] = tmp;
				}
				else { // Add a new low block of 1024 elements.
					final int j = _capacity >> B1;
					if(j >= _high.length) { // Resizes _high.
						final Object/*{E}*/[][] tmp = (Object/*{E}*/[][]) new Object[_high.length << 1][];
						System.arraycopy(_high, 0, tmp, 0, _high.length);
						_high = tmp;
					}
					_high[j] = (Object/*{E}*/[]) new Object[C1];
					_capacity += C1;
				}
			}
		});
	}
	/**
	 * Returns the current capacity of this table.
	 *
	 * @return this table's capacity.
	 */
	public int getCapacity() {
		return _capacity;
	}
	// Shifts element from the specified index to the right (higher indexes).
	private final void shiftRight(int index, int shift) {
		while(_size + shift > _capacity) {
			increaseCapacity();
		}
		for(int i = _size - 1; i >= index;) {
			final int srcBlock = i >> B1, destIdx = i + shift,
					destBlock = destIdx >> B1;
			if(srcBlock == destBlock) {
				final int blockStart = Math.max(index, srcBlock << B1);
				final int count = i - blockStart + 1;
				System.arraycopy(_high[srcBlock], blockStart & M1,
						_high[destBlock], blockStart + shift & M1, count);
				i = blockStart - 1;
			}
			else {
				_high[destBlock][destIdx & M1] = _high[srcBlock][i & M1];
				--i;
			}
		}
	}
	// Shifts element from the specified index to the left (lower indexes).
	private final void shiftLeft(int index, int shift) {
		for(int i = index; i < _size;) {
			final int srcBlock = i >> B1, destIdx = i - shift,
					destBlock = destIdx >> B1;
			if(srcBlock == destBlock) {
				final int blockEnd = Math.min(srcBlock + 1 << B1, _size),
						count = blockEnd - i;
				System.arraycopy(_high[srcBlock], i & M1, _high[destBlock],
						destIdx & M1, count);
				i = blockEnd;
			}
			else {
				_high[destBlock][destIdx & M1] = _high[srcBlock][i & M1];
				++i;
			}
		}
	}
	// For inlining of default comparator.
	private static final boolean defaultEquals(Object o1, Object o2) {
		return o1 == null ? o2 == null : o1 == o2 || o1.equals(o2);
	}
	/**
	 * This inner class implements a sub-table.
	 */
	private static final class SubTable/*<E>*/ extends FastCollection
			/*<E>*/ implements List/*<E>*/, RandomAccess {
		private FastTable/*<E>*/ _table;
		private int _offset;
		private int _size;
		private static final ObjectFactory FACTORY = new ObjectFactory() {
			protected Object create() {
				return new SubTable();
			}
			protected void cleanup(Object obj) {
				final SubTable st = (SubTable) obj;
				st._table = null;
			}
		};
		public static SubTable valueOf(FastTable table, int offset, int size) {
			final SubTable subTable = (SubTable) FACTORY.object();
			subTable._table = table;
			subTable._offset = offset;
			subTable._size = size;
			return subTable;
		}
		public int size() {
			return _size;
		}
		public Record head() {
			return Index.valueOf(-1);
		}
		public Record tail() {
			return Index.valueOf(_size);
		}
		public Object/*{E}*/ valueOf(Record record) {
			return (Object/*{E}*/) _table
					.get(((Index) record).intValue() + _offset);
		}
		public void delete(Record record) {
			throw new UnsupportedOperationException(
					"Deletion not supported, thread-safe collections.");
		}
		public boolean addAll(int index, Collection values) {
			throw new UnsupportedOperationException(
					"Insertion not supported, thread-safe collections.");
		}
		public Object/*{E}*/ get(int index) {
			if(index < 0 || index >= _size)
				throw new IndexOutOfBoundsException(
						"index: " + index + ", size: " + _size);
			return (Object/*{E}*/) _table.get(index + _offset);
		}
		public Object/*{E}*/ set(int index, Object/*{E}*/ value) {
			if(index < 0 || index >= _size)
				throw new IndexOutOfBoundsException(
						"index: " + index + ", size: " + _size);
			return (Object/*{E}*/) _table.set(index + _offset, value);
		}
		public void add(int index, Object/*{E}*/ element) {
			throw new UnsupportedOperationException(
					"Insertion not supported, thread-safe collections.");
		}
		public Object/*{E}*/ remove(int index) {
			throw new UnsupportedOperationException(
					"Deletion not supported, thread-safe collections.");
		}
		public int indexOf(Object value) {
			final FastComparator comp = _table.getValueComparator();
			for(int i = -1; ++i < _size;) {
				if(comp.areEqual(value, _table.get(i + _offset)))
					return i;
			}
			return -1;
		}
		public int lastIndexOf(Object value) {
			final FastComparator comp = _table.getValueComparator();
			for(int i = _size; --i >= 0;) {
				if(comp.areEqual(value, _table.get(i + _offset)))
					return i;
			}
			return -1;
		}
		public ListIterator/*<E>*/ listIterator() {
			return listIterator(0);
		}
		public ListIterator/*<E>*/ listIterator(int index) {
			if(index < 0 || index > _size)
				throw new IndexOutOfBoundsException(
						"index: " + index + ", size: " + _size);
			return FastTableIterator.valueOf(_table, index + _offset, _offset,
					_offset + _size);
		}
		public List/*<E>*/ subList(int fromIndex, int toIndex) {
			if(fromIndex < 0 || toIndex > _size || fromIndex > toIndex)
				throw new IndexOutOfBoundsException("fromIndex: " + fromIndex
						+ ", toIndex: " + toIndex + ", size: " + _size);
			return SubTable.valueOf(_table, _offset + fromIndex,
					toIndex - fromIndex);
		}
	}
	/**
	 * This inner class implements a fast table iterator.
	 */
	private static final class FastTableIterator
			/*<E>*/ implements ListIterator/*<E>*/ {
		private FastTable/*<E>*/ _table;
		private int _currentIndex;
		private int _start; // Inclusive.
		private int _end; // Exclusive.
		private int _nextIndex;
		private Object[] _low;
		private Object[][] _high;
		private static final ObjectFactory FACTORY = new ObjectFactory() {
			protected Object create() {
				return new FastTableIterator();
			}
			protected void cleanup(Object obj) {
				final FastTableIterator i = (FastTableIterator) obj;
				i._table = null;
				i._low = null;
				i._high = null;
			}
		};
		public static FastTableIterator valueOf(FastTable table, int nextIndex,
				int start, int end) {
			final FastTableIterator iterator = (FastTableIterator) FACTORY
					.object();
			iterator._table = table;
			iterator._start = start;
			iterator._end = end;
			iterator._nextIndex = nextIndex;
			iterator._low = table._low;
			iterator._high = table._high;
			iterator._currentIndex = -1;
			return iterator;
		}
		public boolean hasNext() {
			return _nextIndex != _end;
		}
		public Object/*{E}*/ next() {
			if(_nextIndex == _end)
				throw new NoSuchElementException();
			final int i = _currentIndex = _nextIndex++;
			return (Object/*{E}*/) (i < C1 ? _low[i] : _high[i >> B1][i & M1]);
		}
		public int nextIndex() {
			return _nextIndex - _start;
		}
		public boolean hasPrevious() {
			return _nextIndex != _start;
		}
		public Object/*{E}*/ previous() {
			if(_nextIndex == _start)
				throw new NoSuchElementException();
			final int i = _currentIndex = --_nextIndex;
			return (Object/*{E}*/) (i < C1 ? _low[i] : _high[i >> B1][i & M1]);
		}
		public int previousIndex() {
			return _nextIndex - _start - 1;
		}
		public void add(Object/*{E}*/ value) {
			_table.add(_nextIndex++, value);
			++_end;
			_currentIndex = -1;
			_low = _table._low;
			_high = _table._high;
		}
		public void set(Object/*{E}*/ value) {
			if(_currentIndex < 0)
				throw new IllegalStateException();
			_table.set(_currentIndex, value);
		}
		public void remove() {
			if(_currentIndex < 0)
				throw new IllegalStateException();
			_table.remove(_currentIndex);
			--_end;
			if(_currentIndex < _nextIndex) {
				--_nextIndex;
			}
			_currentIndex = -1;
		}
	}
	/**
	 * An unmodifiable view over a {@code FastTable}.
	 */
	private static final class Unmodifiable/*<E>*/ extends FastTable
			/*<E>*/ implements List/*<E>*/, Reusable, RandomAccess {
		private final FastTable/*<E>*/ _table;
		private Unmodifiable(FastTable/*<E>*/ table) {
			super((Void) null);
			_table = table;
		}
		public final FastCollection/*FastTable<E>*/ unmodifiable() {
			return this;
		}
		public final FastCollection/*FastTable<E>*/ shared() {
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
			return _table.contains(value);
		}
		public final boolean containsAll(Collection/*<?>*/ values) {
			return _table.containsAll(values);
		}
		public final void delete(Record record) {
			throw new UnsupportedOperationException("Unmodifiable");
		}
		public final void ensureCapacity(int minCapacity) {
			throw new UnsupportedOperationException("Unmodifiable");
		}
		public final Object/*{E}*/ get(int index) {
			return _table.get(index);
		}
		public final int getCapacity() {
			return _table.getCapacity();
		}
		public final FastComparator/*<? super E>*/ getValueComparator() {
			return _table.getValueComparator();
		}
		public final Record head() {
			return _table.head();
		}
		public final int indexOf(Object value) {
			return _table.indexOf(value);
		}
		public final boolean isEmpty() {
			return _table.isEmpty();
		}
		public final int lastIndexOf(Object value) {
			return _table.lastIndexOf(value);
		}
		public final Iterator/*<E>*/ iterator() {
			return new Iterator() {
				private final Iterator/*<E>*/ i = _table.iterator();
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
		public final void setSize(int size) {
			throw new UnsupportedOperationException("Unmodifiable");
		}
		public final FastTable/*<E>*/ setValueComparator(
				FastComparator/*<? super E>*/ comparator) {
			throw new UnsupportedOperationException("Unmodifiable");
		}
		public final int size() {
			return _table.size();
		}
		public final List/*<E>*/ subList(int fromIndex, int toIndex) {
			throw new UnsupportedOperationException(
					"Sub-List not supported for unmodifiable collection");
		}
		public final Record tail() {
			return _table.tail();
		}
		public final Object[] toArray() {
			return _table.toArray();
		}
		public final Object/*{<T> T}*/[] toArray(Object/*{T}*/[] array) {
			return _table.toArray(array);
		}
		public final Object/*{E}*/ valueOf(Record record) {
			return _table.valueOf(record);
		}
		public final Object/*{E}*/ getFirst() {
			return _table.getFirst();
		}
		public final Object/*{E}*/ getLast() {
			return _table.getLast();
		}
		public final Object/*{E}*/ peekFirst() {
			return _table.peekFirst();
		}
		public final Object/*{E}*/ peekLast() {
			return _table.peekLast();
		}
		public final Object/*{E}*/ peek() {
			return _table.peek();
		}
		public final Object/*{E}*/ element() {
			return _table.element();
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
		public final FastTable/*<E>*/ sort() {
			throw new UnsupportedOperationException("Unmodifiable");
		}
		public final void trimToSize() {
			throw new UnsupportedOperationException("Unmodifiable");
		}
		public final Iterator/*<E>*/ descendingIterator() {
			return new Iterator/*<E>*/() {
				private final Iterator/*<E>*/ i = _table.descendingIterator();
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
	 * A shared view over a {@code FastTable} (reads-write locks).
	 */
	private static final class Shared/*<E>*/ extends FastTable
			/*<E>*/ implements List/*<E>*/, Reusable, RandomAccess {
		private final FastTable/*<E>*/ _table;
		private final ReadWriteLock _lock;
		private Shared(FastTable/*<E>*/ table) {
			super((Void) null);
			_table = table;
			_lock = new ReentrantWriterPreferenceReadWriteLock();
		}
		public final FastCollection/*FastTable<E>*/ unmodifiable() {
			return _table.unmodifiable();
		}
		public final FastCollection/*FastTable<E>*/ shared() {
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
							return _table.add(value);
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
							return _table.remove(value);
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
							_table.add(index, element);
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
							return _table.addAll(values);
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
							return _table.addAll(index, values);
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
							_table.clear();
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
							return _table.contains(value);
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
							return _table.containsAll(values);
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
							_table.delete(record);
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
		public final void ensureCapacity(int minCapacity) {
			boolean wasInterrupted = /*@JVM-1.1+@ true ? Thread.interrupted() : /**/false;
			try {
				final Sync w = _lock.writeLock();
				for(;;) {
					try {
						w.acquire();
						try {
							_table.ensureCapacity(minCapacity);
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
		public final Object/*{E}*/ get(int index) {
			boolean wasInterrupted = /*@JVM-1.1+@ true ? Thread.interrupted() : /**/false;
			try {
				final Sync r = _lock.readLock();
				for(;;) {
					try {
						r.acquire();
						try {
							return _table.get(index);
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
		public final int getCapacity() {
			boolean wasInterrupted = /*@JVM-1.1+@ true ? Thread.interrupted() : /**/false;
			try {
				final Sync r = _lock.readLock();
				for(;;) {
					try {
						r.acquire();
						try {
							return _table.getCapacity();
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
		public FastTable/*<E>*/ setValueComparator(
				FastComparator/*<? super E>*/ comparator) {
			boolean wasInterrupted = /*@JVM-1.1+@ true ? Thread.interrupted() : /**/false;
			try {
				final Sync w = _lock.writeLock();
				for(;;) {
					try {
						w.acquire();
						try {
							return _table.setValueComparator(comparator);
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
		// Overrides.
		public final FastComparator/*<? super E>*/ getValueComparator() {
			boolean wasInterrupted = /*@JVM-1.1+@ true ? Thread.interrupted() : /**/false;
			try {
				final Sync r = _lock.readLock();
				for(;;) {
					try {
						r.acquire();
						try {
							return _table.getValueComparator();
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
		public final Record head() {
			boolean wasInterrupted = /*@JVM-1.1+@ true ? Thread.interrupted() : /**/false;
			try {
				final Sync r = _lock.readLock();
				for(;;) {
					try {
						r.acquire();
						try {
							return _table.head();
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
							return _table.indexOf(value);
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
							return _table.isEmpty();
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
							return _table.lastIndexOf(value);
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
							final int size = _table.size();
							if(index < 0 || index > size)
								throw new IndexOutOfBoundsException(
										"index: " + index + ", size: " + size);
							return new FastTableIterator(index);
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
							return _table.remove(index);
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
							return _table.removeAll(values);
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
							_table.reset();
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
							return _table.retainAll(values);
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
							return _table.set(index, element);
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
		public final void setSize(int size) {
			boolean wasInterrupted = /*@JVM-1.1+@ true ? Thread.interrupted() : /**/false;
			try {
				final Sync w = _lock.writeLock();
				for(;;) {
					try {
						w.acquire();
						try {
							_table.setSize(size);
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
		public final int size() {
			boolean wasInterrupted = /*@JVM-1.1+@ true ? Thread.interrupted() : /**/false;
			try {
				final Sync r = _lock.readLock();
				for(;;) {
					try {
						r.acquire();
						try {
							return _table.size();
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
							return _table.subList(fromIndex, toIndex);
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
		public final Record tail() {
			boolean wasInterrupted = /*@JVM-1.1+@ true ? Thread.interrupted() : /**/false;
			try {
				final Sync r = _lock.readLock();
				for(;;) {
					try {
						r.acquire();
						try {
							return _table.tail();
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
							return _table.toArray();
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
							return _table.toArray(array);
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
							return _table.valueOf(record);
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
							return _table.getFirst();
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
							return _table.getLast();
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
							return _table.peekFirst();
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
							return _table.peekLast();
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
							_table.addFirst(value);
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
							_table.addLast(value);
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
							return _table.removeFirst();
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
							return _table.removeLast();
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
							return _table.pollFirst();
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
							return _table.pollLast();
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
							return _table.removeLastOccurrence(value);
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
		public final FastTable/*<E>*/ sort() {
			boolean wasInterrupted = /*@JVM-1.1+@ true ? Thread.interrupted() : /**/false;
			try {
				final Sync w = _lock.writeLock();
				for(;;) {
					try {
						w.acquire();
						try {
							_table.sort();
							return this;
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
		public final void trimToSize() {
			boolean wasInterrupted = /*@JVM-1.1+@ true ? Thread.interrupted() : /**/false;
			try {
				final Sync w = _lock.writeLock();
				for(;;) {
					try {
						w.acquire();
						try {
							_table.trimToSize();
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
		private final class FastTableIterator implements ListIterator/*<E>*/ {
			private int _nextIndex;
			private int _currentIndex; // -1 when no current element
			private FastTableIterator(int startIndex) {
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
								return _nextIndex < _table.size();
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
								if(_nextIndex >= _table.size())
									throw new NoSuchElementException();
								_currentIndex = _nextIndex;
								return _table.get(_nextIndex++);
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
										&& _nextIndex <= _table.size();
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
								if(idx >= _table.size())
									throw new NoSuchElementException();
								_currentIndex = idx;
								return _table.get(idx);
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
								_table.remove(_currentIndex);
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
								_table.set(_currentIndex, value);
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
								_table.add(_nextIndex++, value);
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