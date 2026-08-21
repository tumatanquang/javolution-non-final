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
import _templates.javolution.lang.Reusable;
import _templates.javolution.util.concurrent.locks.ReadWriteLock;
import _templates.javolution.util.concurrent.locks.ReentrantWriterPreferenceReadWriteLock;
import _templates.javolution.util.concurrent.locks.Sync;
/**
 * <p> This class represents a high-performance, resizable table of <code>short</code>
 *     primitive values based on a two-tier segmented array architecture
 *     (similar to {@link _templates.javolution.util.FastTable FastTable}).</p>
 *
 * <p> Unlike traditional <code>ArrayList</code>-based implementations that allocate
 *     massive contiguous arrays and copy all elements during resizing,
 *     {@link FastShortTable} increases its capacity smoothly by allocating fixed-size
 *     blocks of 1024 elements without copying existing blocks. This ensures
 *     deterministic $\mathcal{O}(1)$ execution time for append operations and
 *     eliminates heap memory fragmentation (Zero-GC Fragmentation).</p>
 *
 * <p> Memory reclamation is instant: calling {@link #trimToSize()} deallocates
 *     unused 1024-element blocks immediately for garbage collection.</p>
 *
 * @author <a href="mailto:jean-marie@dautelle.com">Jean-Marie Dautelle</a>
 * @version 6.0.0, August 18, 2026
 */
public class FastShortTable extends FastPrimitiveCollection
		implements Cloneable, RandomAccess, Serializable {
	private static final int B0 = 4; // Initial capacity in bits (16).
	private static final int C0 = 1 << B0; // Initial capacity (16).
	private static final int B1 = 10; // Low array maximum capacity in bits.
	private static final int C1 = 1 << B1; // Low array maximum capacity (1024).
	private static final int M1 = C1 - 1; // Mask (1023).
	private transient short[] _low;
	private transient short[][] _high;
	private transient int _capacity;
	public FastShortTable() {
		this(C0);
	}
	public FastShortTable(int capacity) {
		_capacity = C0;
		_low = new short[C0];
		_high = new short[1][];
		_high[0] = _low;
		while(capacity > _capacity) {
			increaseCapacity();
		}
	}
	public FastShortTable(short[] values) {
		this(values == null ? C0 : values.length);
		if(values == null)
			throw new NullPointerException("Source array is null");
		addAll(values);
	}
	private final void increaseCapacity() {
		if(_capacity < C1) {
			_capacity <<= 1;
			final short[] tmp = new short[_capacity];
			System.arraycopy(_low, 0, tmp, 0, size);
			_low = tmp;
			_high[0] = tmp;
		}
		else {
			final int j = _capacity >> B1;
			if(j >= _high.length) {
				final short[][] tmp = new short[_high.length << 1][];
				System.arraycopy(_high, 0, tmp, 0, _high.length);
				_high = tmp;
			}
			_high[j] = new short[C1];
			_capacity += C1;
		}
	}
	public void ensureCapacity(int min) {
		while(min > _capacity) {
			increaseCapacity();
		}
	}
	public void trimToSize() {
		while(_capacity > C1 && _capacity - size >= C1) {
			_high[(_capacity >> B1) - 1] = null;
			_capacity -= C1;
		}
		/*@JVM-1.5+@
		if(_capacity <= C1 && size < _capacity) {
			final int newCap = Math.max(C0, Integer.highestOneBit(size - 1) << 1);
			if(newCap < _capacity) {
				final short[] tmp = new short[newCap];
				System.arraycopy(_low, 0, tmp, 0, size);
				_low = tmp;
				_high[0] = tmp;
				_capacity = newCap;
			}
		}
		/**/
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
	private final void shiftLeft(int index, int shift) {
		for(int i = index; i < size;) {
			final int srcBlock = i >> B1;
			final int destIdx = i - shift;
			final int destBlock = destIdx >> B1;
			if(srcBlock == destBlock) {
				final int blockEnd = Math.min(srcBlock + 1 << B1, size);
				final int count = blockEnd - i;
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
	public short get(int index) {
		if(index < 0 || index >= size)
			throw new ArrayIndexOutOfBoundsException(
					"Index: " + index + ", Size: " + size);
		return index < C1 ? _low[index] : _high[index >> B1][index & M1];
	}
	public short set(int index, short value) {
		if(index < 0 || index >= size)
			throw new ArrayIndexOutOfBoundsException(
					"Index: " + index + ", Size: " + size);
		final short[] low = _high[index >> B1];
		final short previous = low[index & M1];
		low[index & M1] = value;
		return previous;
	}
	public boolean add(short value) {
		if(size >= _capacity) {
			increaseCapacity();
		}
		_high[size >> B1][size & M1] = value;
		++size;
		return true;
	}
	public void add(int index, short element) {
		if(index < 0 || index > size)
			throw new ArrayIndexOutOfBoundsException(
					"Index: " + index + ", Size: " + size);
		shiftRight(index, 1);
		_high[index >> B1][index & M1] = element;
		++size;
	}
	public short remove(int index) {
		if(index < 0 || index >= size)
			throw new ArrayIndexOutOfBoundsException(
					"Index: " + index + ", Size: " + size);
		final short previous = get(index);
		shiftLeft(index + 1, 1);
		--size;
		return previous;
	}
	public boolean removeElement(short value) {
		final int i = indexOf(value);
		if(i >= 0) {
			remove(i);
			return true;
		}
		return false;
	}
	public short getFirst() {
		if(size == 0)
			throw new NoSuchElementException();
		return _low[0];
	}
	public short getLast() {
		if(size == 0)
			throw new NoSuchElementException();
		return get(size - 1);
	}
	public void addFirst(short value) {
		add(0, value);
	}
	public void addLast(short value) {
		add(value);
	}
	public short removeFirst() {
		if(size == 0)
			throw new NoSuchElementException();
		return remove(0);
	}
	public short removeLast() {
		if(size == 0)
			throw new NoSuchElementException();
		return remove(size - 1);
	}
	public void push(short value) {
		addFirst(value);
	}
	public short pop() {
		return removeFirst();
	}
	public short peek() {
		return getFirst();
	}
	public boolean offer(short value) {
		return add(value);
	}
	public short poll() {
		return removeFirst();
	}
	public short element() {
		return getFirst();
	}
	public boolean addAll(short[] values) {
		return addAll(size, values);
	}
	public boolean addAll(int index, short[] values) {
		if(index < 0 || index > size)
			throw new ArrayIndexOutOfBoundsException(
					"Index: " + index + ", Size: " + size);
		if(values == null || values.length == 0)
			return false;
		final int shift = values.length;
		shiftRight(index, shift);
		for(int i = 0; i < shift; ++i) {
			final int idx = index + i;
			_high[idx >> B1][idx & M1] = values[i];
		}
		size += shift;
		return true;
	}
	public boolean addAll(FastShortTable values) {
		return addAll(size, values);
	}
	public boolean addAll(int index, FastShortTable values) {
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
		for(int i = 0; i < shift; ++i) {
			final int idx = index + i;
			final int sourceIndex = sameBacking && i >= index ? i + shift : i;
			_high[idx >> B1][idx & M1] = values.get(sourceIndex);
		}
		return true;
	}
	public boolean contains(short value) {
		return indexOf(value) >= 0;
	}
	public int indexOf(short value) {
		for(int i = 0; i < size;) {
			final short[] low = _high[i >> B1];
			final int count = Math.min(low.length, size - i);
			for(int j = 0; j < count; ++j) {
				if(low[j] == value)
					return i + j;
			}
			i += count;
		}
		return -1;
	}
	public int lastIndexOf(short value) {
		for(int i = size; i > 0;) {
			final int block = i - 1 >> B1;
			final short[] low = _high[block];
			final int start = block << B1;
			final int offset = i - start;
			for(int j = offset; --j >= 0;) {
				if(low[j] == value)
					return start + j;
			}
			i = start;
		}
		return -1;
	}
	public short[] toArray() {
		final short[] a = new short[size];
		for(int i = 0; i < size;) {
			final short[] low = _high[i >> B1];
			final int count = Math.min(low.length, size - i);
			System.arraycopy(low, 0, a, i, count);
			i += count;
		}
		return a;
	}
	public void sort() {
		if(size > 1) {
			quicksort(0, size - 1);
		}
	}
	private final void quicksort(int first, int last) {
		int i = first;
		int j = last;
		final short piv = get(first + last >> 1);
		do {
			while(get(i) < piv) {
				++i;
			}
			while(get(j) > piv) {
				--j;
			}
			if(i <= j) {
				final short tmp = get(i);
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
	public FastPrimitiveCollection/*FastShortTable*/ unmodifiable() {
		return new Unmodifiable(this);
	}
	public FastPrimitiveCollection/*FastShortTable*/ shared() {
		return new Shared(this);
	}
	public FastPrimitiveIterator/*FastShortIterator*/ iterator() {
		return new ShortIterator(this);
	}
	public Object/*FastShortTable*/ clone() throws CloneNotSupportedException {
		/*@JVM-1.1+@
		if(true) {
			final FastShortTable c = (FastShortTable) super.clone();
			c._capacity = C0;
			c._low = new short[C0];
			c._high = new short[1][];
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
			final short[] low = _high[i >> B1];
			final int count = Math.min(low.length, size - i);
			for(int j = 0; j < count; ++j) {
				s.writeShort(low[j]);
			}
			i += count;
		}
	}
	private final void readObject(ObjectInputStream s)
			throws IOException, ClassNotFoundException {
		s.defaultReadObject();
		final int count = s.readInt();
		_capacity = C0;
		_low = new short[C0];
		_high = new short[1][];
		_high[0] = _low;
		while(_capacity < count) {
			increaseCapacity();
		}
		size = count;
		for(int i = 0; i < size;) {
			final short[] low = _high[i >> B1];
			final int blockCount = Math.min(low.length, size - i);
			for(int j = 0; j < blockCount; ++j) {
				low[j] = s.readShort();
			}
			i += blockCount;
		}
	}
	public boolean equals(Object o) {
		if(this == o)
			return true;
		if(!(o instanceof FastShortTable))
			return false;
		final FastShortTable that = (FastShortTable) o;
		final int length = size();
		if(length != that.size())
			return false;
		for(int i = -1; ++i < length;) {
			if(get(i) != that.get(i))
				return false;
		}
		return true;
	}
	public int hashCode() {
		int h = 1;
		for(int i = -1; ++i < size;) {
			h = 31 * h + get(i);
		}
		return h;
	}
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
	private static final class ShortIterator implements FastShortIterator {
		private final FastShortTable _table;
		private int _index;
		private int _last = -1;
		private ShortIterator(FastShortTable table) {
			_table = table;
		}
		public final boolean hasNext() {
			return _index < _table.size;
		}
		public final short next() {
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
	private static final class Unmodifiable extends FastShortTable
			implements Cloneable, RandomAccess, Reusable, Serializable {
		private final FastShortTable _table;
		private Unmodifiable(FastShortTable table) {
			super(0);
			_table = table;
		}
		public final FastPrimitiveCollection/*FastShortTable*/ unmodifiable() {
			return this;
		}
		public final FastPrimitiveCollection/*FastShortTable*/ shared() {
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
		public final boolean contains(short value) {
			return _table.contains(value);
		}
		public final int indexOf(short value) {
			return _table.indexOf(value);
		}
		public final int lastIndexOf(short value) {
			return _table.lastIndexOf(value);
		}
		public final Object/*FastShortTable*/ clone()
				throws CloneNotSupportedException {
			return _table.clone();
		}
		public final short[] toArray() {
			return _table.toArray();
		}
		public final short get(int index) {
			return _table.get(index);
		}
		public final short set(int index, short value) {
			throw new UnsupportedOperationException("Unmodifiable");
		}
		public final boolean add(short value) {
			throw new UnsupportedOperationException("Unmodifiable");
		}
		public final void add(int index, short element) {
			throw new UnsupportedOperationException("Unmodifiable");
		}
		public final short remove(int index) {
			throw new UnsupportedOperationException("Unmodifiable");
		}
		public final boolean removeElement(short value) {
			throw new UnsupportedOperationException("Unmodifiable");
		}
		public final void clear() {
			throw new UnsupportedOperationException("Unmodifiable");
		}
		public final boolean addAll(short[] values) {
			throw new UnsupportedOperationException("Unmodifiable");
		}
		public final boolean addAll(int index, short[] values) {
			throw new UnsupportedOperationException("Unmodifiable");
		}
		public final boolean addAll(FastShortTable values) {
			throw new UnsupportedOperationException("Unmodifiable");
		}
		public final boolean addAll(int index, FastShortTable values) {
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
		public final short getFirst() {
			return _table.getFirst();
		}
		public final short getLast() {
			return _table.getLast();
		}
		public final short peek() {
			return _table.peek();
		}
		public final short element() {
			return _table.element();
		}
		public final void addFirst(short value) {
			throw new UnsupportedOperationException("Unmodifiable");
		}
		public final void addLast(short value) {
			throw new UnsupportedOperationException("Unmodifiable");
		}
		public final short removeFirst() {
			throw new UnsupportedOperationException("Unmodifiable");
		}
		public final short removeLast() {
			throw new UnsupportedOperationException("Unmodifiable");
		}
		public final void push(short value) {
			throw new UnsupportedOperationException("Unmodifiable");
		}
		public final short pop() {
			throw new UnsupportedOperationException("Unmodifiable");
		}
		public final boolean offer(short value) {
			throw new UnsupportedOperationException("Unmodifiable");
		}
		public final short poll() {
			throw new UnsupportedOperationException("Unmodifiable");
		}
		public final FastPrimitiveIterator/*FastShortIterator*/ iterator() {
			return new FastShortIterator() {
				private final FastShortIterator _it = (FastShortIterator) _table
						.iterator();
				public final boolean hasNext() {
					return _it.hasNext();
				}
				public final short next() {
					return _it.next();
				}
				public final void remove() {
					throw new UnsupportedOperationException("Unmodifiable");
				}
			};
		}
	}
	private static final class Shared extends FastShortTable
			implements Cloneable, RandomAccess, Reusable, Serializable {
		private final FastShortTable _table;
		private final ReadWriteLock _lock;
		private Shared(FastShortTable table) {
			super(0);
			_table = table;
			_lock = new ReentrantWriterPreferenceReadWriteLock();
		}
		public final FastPrimitiveCollection/*FastShortTable*/ unmodifiable() {
			return _table.unmodifiable();
		}
		public final FastPrimitiveCollection/*FastShortTable*/ shared() {
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
		public final boolean contains(short value) {
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
		public final int indexOf(short value) {
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
		public final int lastIndexOf(short value) {
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
		public final Object/*FastShortTable*/ clone()
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
		public final short[] toArray() {
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
		public final short get(int index) {
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
		public final short set(int index, short value) {
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
		public final boolean add(short value) {
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
		public final void add(int index, short element) {
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
		public final short remove(int index) {
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
		public final boolean removeElement(short value) {
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
		public final boolean addAll(short[] values) {
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
		public final boolean addAll(int index, short[] values) {
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
		public final boolean addAll(FastShortTable values) {
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
		public final boolean addAll(int index, FastShortTable values) {
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
		public final short getFirst() {
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
		public final short getLast() {
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
		public final short peek() {
			return getFirst();
		}
		public final short element() {
			return getFirst();
		}
		public final void addFirst(short value) {
			add(0, value);
		}
		public final void addLast(short value) {
			add(value);
		}
		public final short removeFirst() {
			return remove(0);
		}
		public final short removeLast() {
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
		public final void push(short value) {
			addFirst(value);
		}
		public final short pop() {
			return removeFirst();
		}
		public final boolean offer(short value) {
			return add(value);
		}
		public final short poll() {
			return removeFirst();
		}
		public final FastPrimitiveIterator/*FastShortIterator*/ iterator() {
			final FastShortIterator iterator = (FastShortIterator) _table
					.iterator();
			return new FastShortIterator() {
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
				public final short next() {
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
