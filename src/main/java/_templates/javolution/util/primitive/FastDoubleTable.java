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
 * <p> This class represents a high-performance, resizable table of <code>double</code>
 *     primitive values based on a two-tier segmented array architecture
 *     (similar to {@link _templates.javolution.util.FastTable FastTable}).</p>
 *
 * <p> Unlike traditional <code>ArrayList</code>-based implementations that allocate
 *     massive contiguous arrays and copy all elements during resizing,
 *     {@link FastDoubleTable} increases its capacity smoothly by allocating fixed-size
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
public class FastDoubleTable extends FastPrimitiveCollection
		implements Cloneable, RandomAccess, Serializable {
	private static final int B0 = 4; // Initial capacity in bits (16).
	private static final int C0 = 1 << B0; // Initial capacity (16).
	private static final int B1 = 10; // Low array maximum capacity in bits.
	private static final int C1 = 1 << B1; // Low array maximum capacity (1024).
	private static final int M1 = C1 - 1; // Mask (1023).
	private transient double[] _low;
	private transient double[][] _high;
	private transient int _capacity;
	public FastDoubleTable() {
		this(C0);
	}
	public FastDoubleTable(int capacity) {
		_capacity = C0;
		_low = new double[C0];
		_high = new double[1][];
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
	public FastDoubleTable(double[] values) {
		this(values.length);
		addAll(values);
	}
	private final void increaseCapacity() {
		MemoryArea.getMemoryArea(this).executeInArea(new Runnable() {
			public final void run() {
				if(_capacity < C1) {
					_capacity <<= 1;
					final double[] tmp = new double[_capacity];
					System.arraycopy(_low, 0, tmp, 0, size);
					_low = tmp;
					_high[0] = tmp;
				}
				else {
					final int b = _capacity >> B1, length = _high.length;
					if(b >= length) {
						final double[][] tmp = new double[length << 1][];
						System.arraycopy(_high, 0, tmp, 0, length);
						_high = tmp;
					}
					_high[b] = new double[C1];
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
				final double[] tmp = new double[newCap];
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
	public double get(int index) {
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
	public double set(int index, double value) {
		if(index < 0 || index >= size)
			throw new ArrayIndexOutOfBoundsException(
					"Index: " + index + ", Size: " + size);
		final double[] low = _high[index >> B1];
		final double previous = low[index & M1];
		low[index & M1] = value;
		return previous;
	}
	/**
	 * Appends the specified element to the end of this table.
	 *
	 * @param value element to be appended to this table.
	 * @return <code>true</code>.
	 */
	public boolean add(double value) {
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
	public void add(int index, double element) {
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
	public double remove(int index) {
		if(index < 0 || index >= size)
			throw new ArrayIndexOutOfBoundsException(
					"Index: " + index + ", Size: " + size);
		final double previous = get(index);
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
	public boolean removeElement(double value) {
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
	public double getFirst() {
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
	public double getLast() {
		if(size == 0)
			throw new NoSuchElementException();
		return get(size - 1);
	}
	/**
	 * Inserts the specified value at the beginning of this table.
	 *
	 * @param value the value to be inserted.
	 */
	public void addFirst(double value) {
		add(0, value);
	}
	/**
	 * Appends the specified value to the end of this table.
	 *
	 * @param value the value to be added.
	 */
	public void addLast(double value) {
		add(value);
	}
	/**
	 * Removes and returns the first value of this table.
	 *
	 * @return the first value of this table before this call.
	 * @throws NoSuchElementException if this table is empty.
	 */
	public double removeFirst() {
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
	public double removeLast() {
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
	public void push(double value) {
		addFirst(value);
	}
	/**
	 * Pops a value from the stack represented by this table. In other words,
	 * removes and returns the first value of this table.
	 *
	 * @return the value at the front of this table (which is the top of the stack).
	 * @throws NoSuchElementException if this table is empty.
	 */
	public double pop() {
		return removeFirst();
	}
	/**
	 * Retrieves, but does not remove, the first value of this table.
	 *
	 * @return the first value of this table.
	 * @throws NoSuchElementException if this table is empty.
	 */
	public double peek() {
		return getFirst();
	}
	/**
	 * Inserts the specified value at the end of this table.
	 *
	 * @param value the value to add.
	 * @return <code>true</code>.
	 */
	public boolean offer(double value) {
		return add(value);
	}
	/**
	 * Retrieves and removes the first value of this table.
	 *
	 * @return the first value of this table before this call.
	 * @throws NoSuchElementException if this table is empty.
	 */
	public double poll() {
		return removeFirst();
	}
	/**
	 * Retrieves, but does not remove, the first value of this table.
	 *
	 * @return the first value of this table.
	 * @throws NoSuchElementException if this table is empty.
	 */
	public double element() {
		return getFirst();
	}
	/**
	 * Appends all elements from the specified array to the end of this table.
	 *
	 * @param values array containing elements to be added.
	 * @return <code>true</code> if this table changed as a result of the call.
	 */
	public boolean addAll(double[] values) {
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
	public boolean addAll(int index, double[] values) {
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
	public boolean addAll(FastDoubleTable values) {
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
	public boolean addAll(int index, FastDoubleTable values) {
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
	 * @param value element whose presence in this table is to be tested.
	 * @return <code>true</code> if this table contains the specified element.
	 */
	public boolean contains(double value) {
		return indexOf(value) >= 0;
	}
	/**
	 * Returns the index of the first occurrence of the specified element in this table,
	 * or <code>-1</code> if this table does not contain the element.
	 *
	 * @param value element to search for.
	 * @return the index of the first occurrence of the element, or <code>-1</code>.
	 */
	public int indexOf(double value) {
		final long valueBits = Double.doubleToLongBits(value);
		for(int i = 0; i < size;) {
			final double[] low = _high[i >> B1];
			final int count = Math.min(low.length, size - i);
			for(int j = -1; ++j < count;) {
				if(Double.doubleToLongBits(low[j]) == valueBits)
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
	public int lastIndexOf(double value) {
		final long valueBits = Double.doubleToLongBits(value);
		for(int i = size; i > 0;) {
			final int block = i - 1 >> B1, start = block << B1;
			final double[] low = _high[block];
			for(int j = i - start; --j >= 0;) {
				if(Double.doubleToLongBits(low[j]) == valueBits)
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
	public double[] toArray() {
		final double[] a = new double[size];
		for(int i = 0; i < size;) {
			final double[] low = _high[i >> B1];
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
		final double piv = get(first + last >> 1);
		do {
			while(get(i) < piv) {
				++i;
			}
			while(get(j) > piv) {
				--j;
			}
			if(i <= j) {
				final double tmp = get(i);
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
	public FastPrimitiveCollection/*FastDoubleTable*/ unmodifiable() {
		return new Unmodifiable(this);
	}
	/**
	 * {@inheritDoc}
	 */
	public FastPrimitiveCollection/*FastDoubleTable*/ shared() {
		return new Shared(this);
	}
	/**
	 * {@inheritDoc}
	 */
	public FastPrimitiveIterator/*FastDoubleIterator*/ iterator() {
		return new DoubleIterator(this);
	}
	/**
	 * Returns a copy of this table.
	 *
	 * @return a clone of this instance.
	 * @throws CloneNotSupportedException if cloning is not supported.
	 */
	public Object/*FastDoubleTable*/ clone() throws CloneNotSupportedException {
		/*@JVM-1.1+@
		if(true) {
			final FastDoubleTable c = (FastDoubleTable) super.clone();
			c._capacity = C0;
			c._low = new double[C0];
			c._high = new double[1][];
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
			final double[] low = _high[i >> B1];
			final int count = Math.min(low.length, size - i);
			for(int j = -1; ++j < count;) {
				s.writeDouble(low[j]);
			}
			i += count;
		}
	}
	private final void readObject(ObjectInputStream s)
			throws IOException, ClassNotFoundException {
		s.defaultReadObject();
		final int count = s.readInt();
		_capacity = C0;
		_low = new double[C0];
		_high = new double[1][];
		_high[0] = _low;
		while(_capacity < count) {
			increaseCapacity();
		}
		size = count;
		for(int i = 0; i < size;) {
			final double[] low = _high[i >> B1];
			final int blockCount = Math.min(low.length, size - i);
			for(int j = -1; ++j < blockCount;) {
				low[j] = s.readDouble();
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
		if(!(o instanceof FastDoubleTable))
			return false;
		final FastDoubleTable that = (FastDoubleTable) o;
		final int length = size();
		if(length != that.size())
			return false;
		for(int i = -1; ++i < length;) {
			if(Double.doubleToLongBits(get(i)) != Double
					.doubleToLongBits(that.get(i)))
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
			final long bits = Double.doubleToLongBits(get(i));
			h = 31 * h + (int) (bits ^ bits >>> 32);
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
	private static final class DoubleIterator implements FastDoubleIterator {
		private final FastDoubleTable _table;
		private int _index;
		private int _last = -1;
		private DoubleIterator(FastDoubleTable table) {
			_table = table;
		}
		public final boolean hasNext() {
			return _index < _table.size;
		}
		public final double next() {
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
	private static final class Unmodifiable extends FastDoubleTable
			implements Cloneable, RandomAccess, Reusable, Serializable {
		private final FastDoubleTable _table;
		private Unmodifiable(FastDoubleTable table) {
			super(0);
			_table = table;
		}
		public final FastPrimitiveCollection/*FastDoubleTable*/ unmodifiable() {
			return this;
		}
		public final FastPrimitiveCollection/*FastDoubleTable*/ shared() {
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
		public final boolean contains(double value) {
			return _table.contains(value);
		}
		public final int indexOf(double value) {
			return _table.indexOf(value);
		}
		public final int lastIndexOf(double value) {
			return _table.lastIndexOf(value);
		}
		public final Object/*FastDoubleTable*/ clone()
				throws CloneNotSupportedException {
			return _table.clone();
		}
		public final double[] toArray() {
			return _table.toArray();
		}
		public final double get(int index) {
			return _table.get(index);
		}
		public final double set(int index, double value) {
			throw new UnsupportedOperationException("Unmodifiable");
		}
		public final boolean add(double value) {
			throw new UnsupportedOperationException("Unmodifiable");
		}
		public final void add(int index, double element) {
			throw new UnsupportedOperationException("Unmodifiable");
		}
		public final double remove(int index) {
			throw new UnsupportedOperationException("Unmodifiable");
		}
		public final boolean removeElement(double value) {
			throw new UnsupportedOperationException("Unmodifiable");
		}
		public final void clear() {
			throw new UnsupportedOperationException("Unmodifiable");
		}
		public final boolean addAll(double[] values) {
			throw new UnsupportedOperationException("Unmodifiable");
		}
		public final boolean addAll(int index, double[] values) {
			throw new UnsupportedOperationException("Unmodifiable");
		}
		public final boolean addAll(FastDoubleTable values) {
			throw new UnsupportedOperationException("Unmodifiable");
		}
		public final boolean addAll(int index, FastDoubleTable values) {
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
		public final double getFirst() {
			return _table.getFirst();
		}
		public final double getLast() {
			return _table.getLast();
		}
		public final double peek() {
			return _table.peek();
		}
		public final double element() {
			return _table.element();
		}
		public final void addFirst(double value) {
			throw new UnsupportedOperationException("Unmodifiable");
		}
		public final void addLast(double value) {
			throw new UnsupportedOperationException("Unmodifiable");
		}
		public final double removeFirst() {
			throw new UnsupportedOperationException("Unmodifiable");
		}
		public final double removeLast() {
			throw new UnsupportedOperationException("Unmodifiable");
		}
		public final void push(double value) {
			throw new UnsupportedOperationException("Unmodifiable");
		}
		public final double pop() {
			throw new UnsupportedOperationException("Unmodifiable");
		}
		public final boolean offer(double value) {
			throw new UnsupportedOperationException("Unmodifiable");
		}
		public final double poll() {
			throw new UnsupportedOperationException("Unmodifiable");
		}
		public final FastPrimitiveIterator/*FastDoubleIterator*/ iterator() {
			final FastDoubleIterator iterator = (FastDoubleIterator) _table
					.iterator();
			return new FastDoubleIterator() {
				public final boolean hasNext() {
					return iterator.hasNext();
				}
				public final double next() {
					return iterator.next();
				}
				public final void remove() {
					throw new UnsupportedOperationException("Unmodifiable");
				}
			};
		}
	}
	private static final class Shared extends FastDoubleTable
			implements Cloneable, RandomAccess, Reusable, Serializable {
		private final FastDoubleTable _table;
		private final ReadWriteLock _lock;
		private Shared(FastDoubleTable table) {
			super(0);
			_table = table;
			_lock = new ReentrantWriterPreferenceReadWriteLock();
		}
		public final FastPrimitiveCollection/*FastDoubleTable*/ unmodifiable() {
			return _table.unmodifiable();
		}
		public final FastPrimitiveCollection/*FastDoubleTable*/ shared() {
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
		public final boolean contains(double value) {
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
		public final int indexOf(double value) {
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
		public final int lastIndexOf(double value) {
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
		public final Object/*FastDoubleTable*/ clone()
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
		public final double[] toArray() {
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
		public final double get(int index) {
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
		public final double set(int index, double value) {
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
		public final boolean add(double value) {
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
		public final void add(int index, double element) {
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
		public final double remove(int index) {
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
		public final boolean removeElement(double value) {
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
		public final boolean addAll(double[] values) {
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
		public final boolean addAll(int index, double[] values) {
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
		public final boolean addAll(FastDoubleTable values) {
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
		public final boolean addAll(int index, FastDoubleTable values) {
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
		public final double getFirst() {
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
		public final double getLast() {
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
		public final double peek() {
			return getFirst();
		}
		public final double element() {
			return getFirst();
		}
		public final void addFirst(double value) {
			add(0, value);
		}
		public final void addLast(double value) {
			add(value);
		}
		public final double removeFirst() {
			return remove(0);
		}
		public final double removeLast() {
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
		public final void push(double value) {
			addFirst(value);
		}
		public final double pop() {
			return removeFirst();
		}
		public final boolean offer(double value) {
			return add(value);
		}
		public final double poll() {
			return removeFirst();
		}
		public final FastPrimitiveIterator/*FastDoubleIterator*/ iterator() {
			final FastDoubleIterator iterator = (FastDoubleIterator) _table
					.iterator();
			return new FastDoubleIterator() {
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
				public final double next() {
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