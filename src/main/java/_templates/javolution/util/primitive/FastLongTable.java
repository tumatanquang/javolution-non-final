/*
 * Javolution - Java(TM) Solution for Real-Time and Embedded Systems
 * Copyright (C) 2005 - Javolution (http://javolution.org/)
 * All rights reserved.
 *
 * Permission to use, copy, modify, and distribute this software is
 * freely granted, provided that this notice is preserved.
 */
package _templates.javolution.util.primitive;
import java.io.IOException;
import _templates.java.io.ObjectInputStream;
import _templates.java.io.ObjectOutputStream;
import _templates.java.io.Serializable;
import _templates.java.lang.CloneNotSupportedException;
import _templates.java.lang.Cloneable;
import _templates.java.lang.IllegalStateException;
import _templates.java.lang.UnsupportedOperationException;
import _templates.java.util.NoSuchElementException;
import _templates.java.util.RandomAccess;
import _templates.javax.realtime.MemoryArea;
import _templates.javolution.lang.MathLib;
import _templates.javolution.lang.Reusable;
import _templates.javolution.util.concurrent.locks.ReadWriteLock;
import _templates.javolution.util.concurrent.locks.ReentrantWriterPreferenceReadWriteLock;
import _templates.javolution.util.concurrent.locks.Sync;
/**
 * <p> This class represents a high-performance, resizable table of <code>long</code>
 *     primitive values based on a two-tier segmented array architecture
 *     (similar to {@link _templates.javolution.util.FastTable FastTable}).</p>
 *
 * <p> Unlike traditional <code>ArrayList</code>-based implementations that allocate
 *     massive contiguous arrays and copy all elements during resizing,
 *     {@link FastLongTable} increases its capacity smoothly by allocating fixed-size
 *     blocks of 1024 elements without copying existing blocks. This ensures
 *     deterministic $\mathcal{O}(1)$ execution time for append operations and
 *     eliminates heap memory fragmentation (Zero-GC Fragmentation).</p>
 *
 * <p> Memory reclamation is instant: calling {@link #trimToSize()} deallocates
 *     unused 1024-element blocks immediately for garbage collection.</p>
 *
 * @author <a href="mailto:jean-marie@dautelle.com">Jean-Marie Dautelle</a>
 * @version 6.1.0, August 22, 2026
 */
public class FastLongTable extends FastPrimitiveCollection
		implements Cloneable, RandomAccess, Serializable {
	private static final int B0 = 4; // Initial capacity in bits (16).
	private static final int C0 = 1 << B0; // Initial capacity (16).
	private static final int B1 = 10; // Low array maximum capacity in bits.
	private static final int C1 = 1 << B1; // Low array maximum capacity (1024).
	private static final int M1 = C1 - 1; // Mask (1023).
	private transient long[] _low;
	private transient long[][] _high;
	private transient int _capacity;
	/**
	 * Constructs an empty table with default initial capacity (16).
	 */
	public FastLongTable() {
		this(C0);
	}
	/**
	 * Constructs an empty table with the specified initial capacity.
	 *
	 * @param capacity the initial capacity.
	 */
	public FastLongTable(int capacity) {
		_capacity = C0;
		_low = new long[C0];
		_high = new long[1][];
		_high[0] = _low;
		while(capacity > _capacity) {
			increaseCapacity();
		}
	}
	/**
	 * Constructs a table containing the elements of the specified array.
	 *
	 * @param values the array of initial values.
	 * @throws NullPointerException if the specified array is <code>null</code>.
	 */
	public FastLongTable(long[] values) {
		this(values.length);
		addAll(values);
	}
	private final void increaseCapacity() {
		MemoryArea.getMemoryArea(this).executeInArea(new Runnable() {
			public final void run() {
				if(_capacity < C1) {
					_capacity <<= 1;
					final long[] tmp = new long[_capacity];
					System.arraycopy(_low, 0, tmp, 0, size);
					_low = tmp;
					_high[0] = tmp;
				}
				else {
					final int b = _capacity >> B1, length = _high.length;
					if(b >= length) {
						final long[][] tmp = new long[length << 1][];
						System.arraycopy(_high, 0, tmp, 0, length);
						_high = tmp;
					}
					_high[b] = new long[C1];
					_capacity += C1;
				}
			}
		});
	}
	/**
	 * {@inheritDoc}
	 */
	public void ensureCapacity(int min) {
		while(min > _capacity) {
			increaseCapacity();
		}
	}
	/**
	 * {@inheritDoc}
	 */
	public void trimToSize() {
		while(_capacity > C1 && _capacity - size >= C1) {
			_high[(_capacity >> B1) - 1] = null;
			_capacity -= C1;
		}
		if(_capacity <= C1 && size < _capacity) {
			final int newCap = Math.max(C0, MathLib
					.highestOneBit/*Integer.highestOneBit*/(size - 1) << 1);
			if(newCap < _capacity) {
				final long[] tmp = new long[newCap];
				System.arraycopy(_low, 0, tmp, 0, size);
				_low = tmp;
				_high[0] = tmp;
				_capacity = newCap;
			}
		}
	}
	private final void shiftRight(int index, int shift) {
		while(size + shift > _capacity) {
			increaseCapacity();
		}
		for(int i = size - 1; i >= index;) {
			final int srcBlock = i >> B1, destIdx = i + shift,
					destBlock = destIdx >> B1;
			if(srcBlock == destBlock) {
				final int blockStart = Math.max(index, srcBlock << B1);
				System.arraycopy(_high[srcBlock], blockStart & M1,
						_high[destBlock], blockStart + shift & M1,
						i - blockStart + 1);
				i = blockStart - 1;
			}
			else {
				_high[destBlock][destIdx & M1] = _high[srcBlock][i & M1];
				--i;
			}
		}
	}
	private final void shiftLeft(int index, int shift) {
		for(int i = index; i < size;) {
			final int srcBlock = i >> B1, destIdx = i - shift,
					destBlock = destIdx >> B1;
			if(srcBlock == destBlock) {
				final int blockEnd = Math.min(srcBlock + 1 << B1, size);
				System.arraycopy(_high[srcBlock], i & M1, _high[destBlock],
						destIdx & M1, blockEnd - i);
				i = blockEnd;
			}
			else {
				_high[destBlock][destIdx & M1] = _high[srcBlock][i & M1];
				++i;
			}
		}
	}
	/**
	 * Returns the element at the specified position in this table.
	 *
	 * @param index index of the element to return.
	 * @return the element at the specified position.
	 * @throws ArrayIndexOutOfBoundsException if index is out of range.
	 */
	public long get(int index) {
		if(index < 0 || index >= size)
			throw new ArrayIndexOutOfBoundsException(
					"Index: " + index + ", Size: " + size);
		return index < C1 ? _low[index] : _high[index >> B1][index & M1];
	}
	/**
	 * Replaces the element at the specified position in this table with the specified element.
	 *
	 * @param index index of the element to replace.
	 * @param value element to be stored at the specified position.
	 * @return the element previously at the specified position.
	 * @throws ArrayIndexOutOfBoundsException if index is out of range.
	 */
	public long set(int index, long value) {
		if(index < 0 || index >= size)
			throw new ArrayIndexOutOfBoundsException(
					"Index: " + index + ", Size: " + size);
		final long[] low = _high[index >> B1];
		final long previous = low[index & M1];
		low[index & M1] = value;
		return previous;
	}
	/**
	 * Appends the specified element to the end of this table.
	 *
	 * @param value element to be appended to this table.
	 * @return <code>true</code>.
	 */
	public boolean add(long value) {
		if(size >= _capacity) {
			increaseCapacity();
		}
		_high[size >> B1][size & M1] = value;
		++size;
		return true;
	}
	/**
	 * Inserts the specified element at the specified position in this table.
	 *
	 * @param index index at which the specified element is to be inserted.
	 * @param element element to be inserted.
	 * @throws ArrayIndexOutOfBoundsException if index is out of range.
	 */
	public void add(int index, long element) {
		if(index < 0 || index > size)
			throw new ArrayIndexOutOfBoundsException(
					"Index: " + index + ", Size: " + size);
		shiftRight(index, 1);
		_high[index >> B1][index & M1] = element;
		++size;
	}
	/**
	 * Removes the element at the specified position in this table.
	 *
	 * @param index index of the element to be removed.
	 * @return the element that was removed from the table.
	 * @throws ArrayIndexOutOfBoundsException if index is out of range.
	 */
	public long remove(int index) {
		if(index < 0 || index >= size)
			throw new ArrayIndexOutOfBoundsException(
					"Index: " + index + ", Size: " + size);
		final long previous = get(index);
		shiftLeft(index + 1, 1);
		--size;
		return previous;
	}
	/**
	 * Removes the first occurrence of the specified element from this table.
	 *
	 * @param value element to be removed from this table, if present.
	 * @return <code>true</code> if this table contained the specified element.
	 */
	public boolean removeElement(long value) {
		final int i = indexOf(value);
		if(i >= 0) {
			remove(i);
			return true;
		}
		return false;
	}
	/**
	 * Returns the first value of this table.
	 *
	 * @return the first value in this table.
	 * @throws NoSuchElementException if this table is empty.
	 */
	public long getFirst() {
		if(size == 0)
			throw new NoSuchElementException();
		return _low[0];
	}
	/**
	 * Returns the last value of this table.
	 *
	 * @return the last value in this table.
	 * @throws NoSuchElementException if this table is empty.
	 */
	public long getLast() {
		if(size == 0)
			throw new NoSuchElementException();
		return get(size - 1);
	}
	/**
	 * Inserts the specified value at the beginning of this table.
	 *
	 * @param value the value to be inserted.
	 */
	public void addFirst(long value) {
		add(0, value);
	}
	/**
	 * Appends the specified value to the end of this table.
	 *
	 * @param value the value to be added.
	 */
	public void addLast(long value) {
		add(value);
	}
	/**
	 * Removes and returns the first value of this table.
	 *
	 * @return the first value of this table before this call.
	 * @throws NoSuchElementException if this table is empty.
	 */
	public long removeFirst() {
		if(size == 0)
			throw new NoSuchElementException();
		return remove(0);
	}
	/**
	 * Removes and returns the last value of this table.
	 *
	 * @return the last value of this table before this call.
	 * @throws NoSuchElementException if this table is empty.
	 */
	public long removeLast() {
		if(size == 0)
			throw new NoSuchElementException();
		return remove(size - 1);
	}
	/**
	 * Pushes a value onto the stack represented by this table (in other words,
	 * at the head of this table).
	 *
	 * @param value the value to push.
	 */
	public void push(long value) {
		addFirst(value);
	}
	/**
	 * Pops a value from the stack represented by this table. In other words,
	 * removes and returns the first value of this table.
	 *
	 * @return the value at the front of this table (which is the top of the stack).
	 * @throws NoSuchElementException if this table is empty.
	 */
	public long pop() {
		return removeFirst();
	}
	/**
	 * Retrieves, but does not remove, the first value of this table.
	 *
	 * @return the first value of this table.
	 * @throws NoSuchElementException if this table is empty.
	 */
	public long peek() {
		return getFirst();
	}
	/**
	 * Inserts the specified value at the end of this table.
	 *
	 * @param value the value to add.
	 * @return <code>true</code>.
	 */
	public boolean offer(long value) {
		return add(value);
	}
	/**
	 * Retrieves and removes the first value of this table.
	 *
	 * @return the first value of this table before this call.
	 * @throws NoSuchElementException if this table is empty.
	 */
	public long poll() {
		return removeFirst();
	}
	/**
	 * Retrieves, but does not remove, the first value of this table.
	 *
	 * @return the first value of this table.
	 * @throws NoSuchElementException if this table is empty.
	 */
	public long element() {
		return getFirst();
	}
	/**
	 * Appends all elements from the specified array to the end of this table.
	 *
	 * @param values array containing elements to be added.
	 * @return <code>true</code> if this table changed as a result of the call.
	 */
	public boolean addAll(long[] values) {
		return addAll(size, values);
	}
	/**
	 * Inserts all elements from the specified array into this table at the specified position.
	 *
	 * @param index index at which to insert the first element.
	 * @param values array containing elements to be added.
	 * @return <code>true</code> if this table changed as a result of the call.
	 * @throws ArrayIndexOutOfBoundsException if index is out of range.
	 */
	public boolean addAll(int index, long[] values) {
		if(index < 0 || index > size)
			throw new ArrayIndexOutOfBoundsException(
					"Index: " + index + ", Size: " + size);
		if(values == null || values.length == 0)
			return false;
		final int shift = values.length;
		shiftRight(index, shift);
		size += shift;
		for(int i = -1; ++i < shift;) {
			final int idx = index + i;
			_high[idx >> B1][idx & M1] = values[i];
		}
		return true;
	}
	/**
	 * Appends all elements from the specified table to the end of this table.
	 *
	 * @param values table containing elements to be added.
	 * @return <code>true</code> if this table changed as a result of the call.
	 */
	public boolean addAll(FastLongTable values) {
		return addAll(size, values);
	}
	/**
	 * Inserts all elements from the specified table into this table at the specified position.
	 *
	 * @param index index at which to insert the first element.
	 * @param values table containing elements to be added.
	 * @return <code>true</code> if this table changed as a result of the call.
	 * @throws ArrayIndexOutOfBoundsException if index is out of range.
	 */
	public boolean addAll(int index, FastLongTable values) {
		if(index < 0 || index > size)
			throw new ArrayIndexOutOfBoundsException(
					"Index: " + index + ", Size: " + size);
		if(values == null || values.size() == 0)
			return false;
		final boolean sameBacking = values == this
				|| values instanceof Unmodifiable
						&& ((Unmodifiable) values)._table == this
				|| values instanceof Shared && ((Shared) values)._table == this;
		final int shift = values.size();
		shiftRight(index, shift);
		size += shift;
		for(int i = -1; ++i < shift;) {
			final int idx = index + i,
					sourceIndex = sameBacking && i >= index ? i + shift : i;
			_high[idx >> B1][idx & M1] = values.get(sourceIndex);
		}
		return true;
	}
	/**
	 * Returns <code>true</code> if this table contains the specified element.
	 *
	 * @param value element practical existence in this table is to be tested.
	 * @return <code>true</code> if this table contains the specified element.
	 */
	public boolean contains(long value) {
		return indexOf(value) >= 0;
	}
	/**
	 * Returns the index of the first occurrence of the specified element in this table,
	 * or <code>-1</code> if this table does not contain the element.
	 *
	 * @param value element to search for.
	 * @return the index of the first occurrence of the element, or <code>-1</code>.
	 */
	public int indexOf(long value) {
		for(int i = 0; i < size;) {
			final long[] low = _high[i >> B1];
			final int count = Math.min(low.length, size - i);
			for(int j = -1; ++j < count;) {
				if(low[j] == value)
					return i + j;
			}
			i += count;
		}
		return -1;
	}
	/**
	 * Returns the index of the last occurrence of the specified element in this table,
	 * or <code>-1</code> if this table does not contain the element.
	 *
	 * @param value element to search for.
	 * @return the index of the last occurrence of the element, or <code>-1</code>.
	 */
	public int lastIndexOf(long value) {
		for(int i = size; i > 0;) {
			final int block = i - 1 >> B1, start = block << B1;
			final long[] low = _high[block];
			for(int j = i - start; --j >= 0;) {
				if(low[j] == value)
					return start + j;
			}
			i = start;
		}
		return -1;
	}
	/**
	 * Returns an array containing all of the elements in this table in proper sequence.
	 *
	 * @return an array containing all of the elements in this table.
	 */
	public long[] toArray() {
		final long[] a = new long[size];
		for(int i = 0; i < size;) {
			final long[] low = _high[i >> B1];
			final int count = Math.min(low.length, size - i);
			System.arraycopy(low, 0, a, i, count);
			i += count;
		}
		return a;
	}
	/**
	 * Sorts this table in ascending numerical order in-place (Quicksort).
	 */
	public void sort() {
		if(size > 1) {
			quicksort(0, size - 1);
		}
	}
	private final void quicksort(int first, int last) {
		int i = first, j = last;
		final long piv = get(first + last >> 1);
		do {
			while(get(i) < piv) {
				++i;
			}
			while(get(j) > piv) {
				--j;
			}
			if(i <= j) {
				final long tmp = get(i);
				set(i, get(j));
				set(j, tmp);
				++i;
				--j;
			}
		}
		while(i <= j);
		if(first < j) {
			quicksort(first, j);
		}
		if(i < last) {
			quicksort(i, last);
		}
	}
	/**
	 * {@inheritDoc}
	 */
	public FastPrimitiveCollection/*FastLongTable*/ unmodifiable() {
		return new Unmodifiable(this);
	}
	/**
	 * {@inheritDoc}
	 */
	public FastPrimitiveCollection/*FastLongTable*/ shared() {
		return new Shared(this);
	}
	/**
	 * {@inheritDoc}
	 */
	public FastPrimitiveIterator/*FastLongIterator*/ iterator() {
		return new LongIterator(this);
	}
	/**
	 * Returns a copy of this table.
	 *
	 * @return a clone of this instance.
	 * @throws CloneNotSupportedException if cloning is not supported.
	 */
	public Object/*FastLongTable*/ clone() throws CloneNotSupportedException {
		/*@JVM-1.1+@
		if(true) {
			final FastLongTable c = (FastLongTable) super.clone();
			c._capacity = C0;
			c._low = new long[C0];
			c._high = new long[1][];
			c._high[0] = c._low;
			c.size = 0;
			c.addAll(this);
			return c;
		}
		/**/
		throw new UnsupportedOperationException("J2ME Not Supported Yet");
	}
	private final void writeObject(ObjectOutputStream s) throws IOException {
		s.defaultWriteObject();
		s.writeInt(size);
		for(int i = 0; i < size;) {
			final long[] low = _high[i >> B1];
			final int count = Math.min(low.length, size - i);
			for(int j = -1; ++j < count;) {
				s.writeLong(low[j]);
			}
			i += count;
		}
	}
	private final void readObject(ObjectInputStream s)
			throws IOException, ClassNotFoundException {
		s.defaultReadObject();
		final int count = s.readInt();
		_capacity = C0;
		_low = new long[C0];
		_high = new long[1][];
		_high[0] = _low;
		while(_capacity < count) {
			increaseCapacity();
		}
		size = count;
		for(int i = 0; i < size;) {
			final long[] low = _high[i >> B1];
			final int blockCount = Math.min(low.length, size - i);
			for(int j = -1; ++j < blockCount;) {
				low[j] = s.readLong();
			}
			i += blockCount;
		}
	}
	/**
	 * Compares the specified object with this table for equality.
	 *
	 * @param o the object to be compared for equality with this table.
	 * @return <code>true</code> if the specified object is equal to this table.
	 */
	public boolean equals(Object o) {
		if(this == o)
			return true;
		if(!(o instanceof FastLongTable))
			return false;
		final FastLongTable that = (FastLongTable) o;
		final int length = size();
		if(length != that.size())
			return false;
		for(int i = -1; ++i < length;) {
			if(get(i) != that.get(i))
				return false;
		}
		return true;
	}
	/**
	 * Returns the hash code value for this table.
	 *
	 * @return the hash code value for this table.
	 */
	public int hashCode() {
		int h = 1;
		for(int i = -1; ++i < size;) {
			final long val = get(i);
			h = 31 * h + (int) (val ^ val >>> 32);
		}
		return h;
	}
	/**
	 * Returns a string representation of this table.
	 *
	 * @return a string representation of this table.
	 */
	public String toString() {
		if(size == 0)
			return "[]";
		final StringBuffer/*StringBuilder*/ sb = new StringBuffer/*StringBuilder*/();
		sb.append('[');
		for(int i = -1; ++i < size;) {
			if(i > 0) {
				sb.append(',').append(' ');
			}
			sb.append(get(i));
		}
		return sb.append(']').toString();
	}
	private static final class LongIterator implements FastLongIterator {
		private final FastLongTable _table;
		private int _index;
		private int _last = -1;
		private LongIterator(FastLongTable table) {
			_table = table;
		}
		public final boolean hasNext() {
			return _index < _table.size;
		}
		public final long next() {
			if(_index >= _table.size)
				throw new NoSuchElementException();
			_last = _index;
			return _table.get(_index++);
		}
		public final void remove() {
			if(_last < 0)
				throw new IllegalStateException();
			_table.remove(_last);
			_index = _last;
			_last = -1;
		}
	}
	private static final class Unmodifiable extends FastLongTable
			implements Cloneable, RandomAccess, Reusable, Serializable {
		private final FastLongTable _table;
		private Unmodifiable(FastLongTable table) {
			super(0);
			_table = table;
		}
		public final FastPrimitiveCollection/*FastLongTable*/ unmodifiable() {
			return this;
		}
		public final FastPrimitiveCollection/*FastLongTable*/ shared() {
			throw new UnsupportedOperationException("Unmodifiable");
		}
		public final int size() {
			return _table.size();
		}
		public final boolean isEmpty() {
			return _table.isEmpty();
		}
		public final void trimToSize() {
			throw new UnsupportedOperationException("Unmodifiable");
		}
		public final void ensureCapacity(int min) {
			throw new UnsupportedOperationException("Unmodifiable");
		}
		public final boolean contains(long value) {
			return _table.contains(value);
		}
		public final int indexOf(long value) {
			return _table.indexOf(value);
		}
		public final int lastIndexOf(long value) {
			return _table.lastIndexOf(value);
		}
		public final Object/*FastLongTable*/ clone()
				throws CloneNotSupportedException {
			return _table.clone();
		}
		public final long[] toArray() {
			return _table.toArray();
		}
		public final long get(int index) {
			return _table.get(index);
		}
		public final long set(int index, long value) {
			throw new UnsupportedOperationException("Unmodifiable");
		}
		public final boolean add(long value) {
			throw new UnsupportedOperationException("Unmodifiable");
		}
		public final void add(int index, long element) {
			throw new UnsupportedOperationException("Unmodifiable");
		}
		public final long remove(int index) {
			throw new UnsupportedOperationException("Unmodifiable");
		}
		public final boolean removeElement(long value) {
			throw new UnsupportedOperationException("Unmodifiable");
		}
		public final void clear() {
			throw new UnsupportedOperationException("Unmodifiable");
		}
		public final boolean addAll(long[] values) {
			throw new UnsupportedOperationException("Unmodifiable");
		}
		public final boolean addAll(int index, long[] values) {
			throw new UnsupportedOperationException("Unmodifiable");
		}
		public final boolean addAll(FastLongTable values) {
			throw new UnsupportedOperationException("Unmodifiable");
		}
		public final boolean addAll(int index, FastLongTable values) {
			throw new UnsupportedOperationException("Unmodifiable");
		}
		public final void sort() {
			throw new UnsupportedOperationException("Unmodifiable");
		}
		public final boolean equals(Object o) {
			return _table.equals(o);
		}
		public final int hashCode() {
			return _table.hashCode();
		}
		public final String toString() {
			return _table.toString();
		}
		public final void reset() {
			throw new UnsupportedOperationException("Unmodifiable");
		}
		public final long getFirst() {
			return _table.getFirst();
		}
		public final long getLast() {
			return _table.getLast();
		}
		public final long peek() {
			return _table.peek();
		}
		public final long element() {
			return _table.element();
		}
		public final void addFirst(long value) {
			throw new UnsupportedOperationException("Unmodifiable");
		}
		public final void addLast(long value) {
			throw new UnsupportedOperationException("Unmodifiable");
		}
		public final long removeFirst() {
			throw new UnsupportedOperationException("Unmodifiable");
		}
		public final long removeLast() {
			throw new UnsupportedOperationException("Unmodifiable");
		}
		public final void push(long value) {
			throw new UnsupportedOperationException("Unmodifiable");
		}
		public final long pop() {
			throw new UnsupportedOperationException("Unmodifiable");
		}
		public final boolean offer(long value) {
			throw new UnsupportedOperationException("Unmodifiable");
		}
		public final long poll() {
			throw new UnsupportedOperationException("Unmodifiable");
		}
		public final FastPrimitiveIterator/*FastLongIterator*/ iterator() {
			final FastLongIterator iterator = (FastLongIterator) _table
					.iterator();
			return new FastLongIterator() {
				public final boolean hasNext() {
					return iterator.hasNext();
				}
				public final long next() {
					return iterator.next();
				}
				public final void remove() {
					throw new UnsupportedOperationException("Unmodifiable");
				}
			};
		}
	}
	private static final class Shared extends FastLongTable
			implements Cloneable, RandomAccess, Reusable, Serializable {
		private final FastLongTable _table;
		private final ReadWriteLock _lock;
		private Shared(FastLongTable table) {
			super(0);
			_table = table;
			_lock = new ReentrantWriterPreferenceReadWriteLock();
		}
		public final FastPrimitiveCollection/*FastLongTable*/ unmodifiable() {
			return _table.unmodifiable();
		}
		public final FastPrimitiveCollection/*FastLongTable*/ shared() {
			return this;
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
		public final void ensureCapacity(int min) {
			boolean wasInterrupted = /*@JVM-1.1+@ true ? Thread.interrupted() : /**/false;
			try {
				final Sync w = _lock.writeLock();
				for(;;) {
					try {
						w.acquire();
						try {
							_table.ensureCapacity(min);
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
		public final boolean contains(long value) {
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
		public final int indexOf(long value) {
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
		public final int lastIndexOf(long value) {
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
		public final Object/*FastLongTable*/ clone()
				throws CloneNotSupportedException {
			boolean wasInterrupted = /*@JVM-1.1+@ true ? Thread.interrupted() : /**/false;
			try {
				final Sync r = _lock.readLock();
				for(;;) {
					try {
						r.acquire();
						try {
							return _table.clone();
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
		public final long[] toArray() {
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
		public final long get(int index) {
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
		public final long set(int index, long value) {
			boolean wasInterrupted = /*@JVM-1.1+@ true ? Thread.interrupted() : /**/false;
			try {
				final Sync w = _lock.writeLock();
				for(;;) {
					try {
						w.acquire();
						try {
							return _table.set(index, value);
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
		public final boolean add(long value) {
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
		public final void add(int index, long element) {
			boolean wasInterrupted = /*@JVM-1.1+@ true ? Thread.interrupted() : /**/false;
			try {
				final Sync w = _lock.writeLock();
				for(;;) {
					try {
						w.acquire();
						try {
							_table.add(index, element);
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
		public final long remove(int index) {
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
		public final boolean removeElement(long value) {
			boolean wasInterrupted = /*@JVM-1.1+@ true ? Thread.interrupted() : /**/false;
			try {
				final Sync w = _lock.writeLock();
				for(;;) {
					try {
						w.acquire();
						try {
							return _table.removeElement(value);
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
		public final boolean addAll(long[] values) {
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
		public final boolean addAll(int index, long[] values) {
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
		public final boolean addAll(FastLongTable values) {
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
		public final boolean addAll(int index, FastLongTable values) {
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
		public final void sort() {
			boolean wasInterrupted = /*@JVM-1.1+@ true ? Thread.interrupted() : /**/false;
			try {
				final Sync w = _lock.writeLock();
				for(;;) {
					try {
						w.acquire();
						try {
							_table.sort();
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
		public final boolean equals(Object o) {
			boolean wasInterrupted = /*@JVM-1.1+@ true ? Thread.interrupted() : /**/false;
			try {
				final Sync r = _lock.readLock();
				for(;;) {
					try {
						r.acquire();
						try {
							return _table.equals(o);
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
		public final int hashCode() {
			boolean wasInterrupted = /*@JVM-1.1+@ true ? Thread.interrupted() : /**/false;
			try {
				final Sync r = _lock.readLock();
				for(;;) {
					try {
						r.acquire();
						try {
							return _table.hashCode();
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
		public final String toString() {
			boolean wasInterrupted = /*@JVM-1.1+@ true ? Thread.interrupted() : /**/false;
			try {
				final Sync r = _lock.readLock();
				for(;;) {
					try {
						r.acquire();
						try {
							return _table.toString();
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
		public final void reset() {
			boolean wasInterrupted = /*@JVM-1.1+@ true ? Thread.interrupted() : /**/false;
			try {
				final Sync w = _lock.writeLock();
				for(;;) {
					try {
						w.acquire();
						try {
							_table.reset();
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
		public final long getFirst() {
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
		public final long getLast() {
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
		public final long peek() {
			return getFirst();
		}
		public final long element() {
			return getFirst();
		}
		public final void addFirst(long value) {
			add(0, value);
		}
		public final void addLast(long value) {
			add(value);
		}
		public final long removeFirst() {
			return remove(0);
		}
		public final long removeLast() {
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
		public final void push(long value) {
			addFirst(value);
		}
		public final long pop() {
			return removeFirst();
		}
		public final boolean offer(long value) {
			return add(value);
		}
		public final long poll() {
			return removeFirst();
		}
		public final FastPrimitiveIterator/*FastLongIterator*/ iterator() {
			final FastLongIterator iterator = (FastLongIterator) _table
					.iterator();
			return new FastLongIterator() {
				public final boolean hasNext() {
					boolean wasInterrupted = /*@JVM-1.1+@ true ? Thread.interrupted() : /**/false;
					try {
						final Sync r = _lock.readLock();
						for(;;) {
							try {
								r.acquire();
								try {
									return iterator.hasNext();
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
				public final long next() {
					boolean wasInterrupted = /*@JVM-1.1+@ true ? Thread.interrupted() : /**/false;
					try {
						final Sync r = _lock.readLock();
						for(;;) {
							try {
								r.acquire();
								try {
									return iterator.next();
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
				public final void remove() {
					boolean wasInterrupted = /*@JVM-1.1+@ true ? Thread.interrupted() : /**/false;
					try {
						final Sync w = _lock.writeLock();
						for(;;) {
							try {
								w.acquire();
								try {
									iterator.remove();
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
			};
		}
	}
}