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
 * <p> This class represents a high-performance, resizable table of <code>float</code>
 *     primitive values based on a two-tier segmented array architecture
 *     (similar to {@link _templates.javolution.util.FastTable FastTable}).</p>
 *
 * <p> Unlike traditional <code>ArrayList</code>-based implementations that allocate
 *     massive contiguous arrays and copy all elements during resizing,
 *     {@link FastFloatTable} increases its capacity smoothly by allocating fixed-size
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
public class FastFloatTable extends FastPrimitiveCollection
		implements Cloneable, RandomAccess, Serializable {
	private static final int B0 = 4; // Initial capacity in bits (16).
	private static final int C0 = 1 << B0; // Initial capacity (16).
	private static final int B1 = 10; // Low array maximum capacity in bits.
	private static final int C1 = 1 << B1; // Low array maximum capacity (1024).
	private static final int M1 = C1 - 1; // Mask (1023).
	private transient float[] _low;
	private transient float[][] _high;
	private transient int _capacity;
	public FastFloatTable() {
		this(C0);
	}
	public FastFloatTable(int capacity) {
		_capacity = C0;
		_low = new float[C0];
		_high = new float[1][];
		_high[0] = _low;
		while(capacity > _capacity) {
			increaseCapacity();
		}
	}
	public FastFloatTable(float[] values) {
		this(values == null ? C0 : values.length);
		if(values == null)
			throw new NullPointerException("Source array is null");
		addAll(values);
	}
	private final void increaseCapacity() {
		if(_capacity < C1) {
			_capacity <<= 1;
			final float[] tmp = new float[_capacity];
			System.arraycopy(_low, 0, tmp, 0, size);
			_low = tmp;
			_high[0] = tmp;
		}
		else {
			final int j = _capacity >> B1;
			if(j >= _high.length) {
				final float[][] tmp = new float[_high.length << 1][];
				System.arraycopy(_high, 0, tmp, 0, _high.length);
				_high = tmp;
			}
			_high[j] = new float[C1];
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
				final float[] tmp = new float[newCap];
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
	public float get(int index) {
		if(index < 0 || index >= size)
			throw new ArrayIndexOutOfBoundsException(
					"Index: " + index + ", Size: " + size);
		return index < C1 ? _low[index] : _high[index >> B1][index & M1];
	}
	public float set(int index, float value) {
		if(index < 0 || index >= size)
			throw new ArrayIndexOutOfBoundsException(
					"Index: " + index + ", Size: " + size);
		final float[] low = _high[index >> B1];
		final float previous = low[index & M1];
		low[index & M1] = value;
		return previous;
	}
	public boolean add(float value) {
		if(size >= _capacity) {
			increaseCapacity();
		}
		_high[size >> B1][size & M1] = value;
		++size;
		return true;
	}
	public void add(int index, float element) {
		if(index < 0 || index > size)
			throw new ArrayIndexOutOfBoundsException(
					"Index: " + index + ", Size: " + size);
		shiftRight(index, 1);
		_high[index >> B1][index & M1] = element;
		++size;
	}
	public float remove(int index) {
		if(index < 0 || index >= size)
			throw new ArrayIndexOutOfBoundsException(
					"Index: " + index + ", Size: " + size);
		final float previous = get(index);
		shiftLeft(index + 1, 1);
		--size;
		return previous;
	}
	public boolean removeElement(float value) {
		final int i = indexOf(value);
		if(i >= 0) {
			remove(i);
			return true;
		}
		return false;
	}
	public float getFirst() {
		if(size == 0)
			throw new NoSuchElementException();
		return _low[0];
	}
	public float getLast() {
		if(size == 0)
			throw new NoSuchElementException();
		return get(size - 1);
	}
	public void addFirst(float value) {
		add(0, value);
	}
	public void addLast(float value) {
		add(value);
	}
	public float removeFirst() {
		if(size == 0)
			throw new NoSuchElementException();
		return remove(0);
	}
	public float removeLast() {
		if(size == 0)
			throw new NoSuchElementException();
		return remove(size - 1);
	}
	public void push(float value) {
		addFirst(value);
	}
	public float pop() {
		return removeFirst();
	}
	public float peek() {
		return getFirst();
	}
	public boolean offer(float value) {
		return add(value);
	}
	public float poll() {
		return removeFirst();
	}
	public float element() {
		return getFirst();
	}
	public boolean addAll(float[] values) {
		return addAll(size, values);
	}
	public boolean addAll(int index, float[] values) {
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
	public boolean addAll(FastFloatTable values) {
		return addAll(size, values);
	}
	public boolean addAll(int index, FastFloatTable values) {
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
	public boolean contains(float value) {
		return indexOf(value) >= 0;
	}
	public int indexOf(float value) {
		final int valueBits = Float.floatToIntBits(value);
		for(int i = 0; i < size;) {
			final float[] low = _high[i >> B1];
			final int count = Math.min(low.length, size - i);
			for(int j = 0; j < count; ++j) {
				if(Float.floatToIntBits(low[j]) == valueBits)
					return i + j;
			}
			i += count;
		}
		return -1;
	}
	public int lastIndexOf(float value) {
		final int valueBits = Float.floatToIntBits(value);
		for(int i = size; i > 0;) {
			final int block = i - 1 >> B1;
			final float[] low = _high[block];
			final int start = block << B1;
			final int offset = i - start;
			for(int j = offset; --j >= 0;) {
				if(Float.floatToIntBits(low[j]) == valueBits)
					return start + j;
			}
			i = start;
		}
		return -1;
	}
	public float[] toArray() {
		final float[] a = new float[size];
		for(int i = 0; i < size;) {
			final float[] low = _high[i >> B1];
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
		final float piv = get(first + last >> 1);
		do {
			while(get(i) < piv) {
				++i;
			}
			while(get(j) > piv) {
				--j;
			}
			if(i <= j) {
				final float tmp = get(i);
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
	public FastPrimitiveCollection/*FastFloatTable*/ unmodifiable() {
		return new Unmodifiable(this);
	}
	public FastPrimitiveCollection/*FastFloatTable*/ shared() {
		return new Shared(this);
	}
	public FastPrimitiveIterator/*FastFloatIterator*/ iterator() {
		return new FloatIterator(this);
	}
	public Object/*FastFloatTable*/ clone() throws CloneNotSupportedException {
		/*@JVM-1.1+@
		if(true) {
			final FastFloatTable c = (FastFloatTable) super.clone();
			c._capacity = C0;
			c._low = new float[C0];
			c._high = new float[1][];
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
			final float[] low = _high[i >> B1];
			final int count = Math.min(low.length, size - i);
			for(int j = 0; j < count; ++j) {
				s.writeFloat(low[j]);
			}
			i += count;
		}
	}
	private final void readObject(ObjectInputStream s)
			throws IOException, ClassNotFoundException {
		s.defaultReadObject();
		final int count = s.readInt();
		_capacity = C0;
		_low = new float[C0];
		_high = new float[1][];
		_high[0] = _low;
		while(_capacity < count) {
			increaseCapacity();
		}
		size = count;
		for(int i = 0; i < size;) {
			final float[] low = _high[i >> B1];
			final int blockCount = Math.min(low.length, size - i);
			for(int j = 0; j < blockCount; ++j) {
				low[j] = s.readFloat();
			}
			i += blockCount;
		}
	}
	public boolean equals(Object o) {
		if(this == o)
			return true;
		if(!(o instanceof FastFloatTable))
			return false;
		final FastFloatTable that = (FastFloatTable) o;
		final int length = size();
		if(length != that.size())
			return false;
		for(int i = -1; ++i < length;) {
			if(Float.floatToIntBits(get(i)) != Float
					.floatToIntBits(that.get(i)))
				return false;
		}
		return true;
	}
	public int hashCode() {
		int h = 1;
		for(int i = -1; ++i < size;) {
			h = 31 * h + Float.floatToIntBits(get(i));
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
	private static final class FloatIterator implements FastFloatIterator {
		private final FastFloatTable _table;
		private int _index;
		private int _last = -1;
		private FloatIterator(FastFloatTable table) {
			_table = table;
		}
		public final boolean hasNext() {
			return _index < _table.size;
		}
		public final float next() {
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
	private static final class Unmodifiable extends FastFloatTable
			implements Cloneable, RandomAccess, Reusable, Serializable {
		private final FastFloatTable _table;
		private Unmodifiable(FastFloatTable table) {
			super(0);
			_table = table;
		}
		public final FastPrimitiveCollection/*FastFloatTable*/ unmodifiable() {
			return this;
		}
		public final FastPrimitiveCollection/*FastFloatTable*/ shared() {
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
		public final boolean contains(float value) {
			return _table.contains(value);
		}
		public final int indexOf(float value) {
			return _table.indexOf(value);
		}
		public final int lastIndexOf(float value) {
			return _table.lastIndexOf(value);
		}
		public final Object/*FastFloatTable*/ clone()
				throws CloneNotSupportedException {
			return _table.clone();
		}
		public final float[] toArray() {
			return _table.toArray();
		}
		public final float get(int index) {
			return _table.get(index);
		}
		public final float set(int index, float value) {
			throw new UnsupportedOperationException("Unmodifiable");
		}
		public final boolean add(float value) {
			throw new UnsupportedOperationException("Unmodifiable");
		}
		public final void add(int index, float element) {
			throw new UnsupportedOperationException("Unmodifiable");
		}
		public final float remove(int index) {
			throw new UnsupportedOperationException("Unmodifiable");
		}
		public final boolean removeElement(float value) {
			throw new UnsupportedOperationException("Unmodifiable");
		}
		public final void clear() {
			throw new UnsupportedOperationException("Unmodifiable");
		}
		public final boolean addAll(float[] values) {
			throw new UnsupportedOperationException("Unmodifiable");
		}
		public final boolean addAll(int index, float[] values) {
			throw new UnsupportedOperationException("Unmodifiable");
		}
		public final boolean addAll(FastFloatTable values) {
			throw new UnsupportedOperationException("Unmodifiable");
		}
		public final boolean addAll(int index, FastFloatTable values) {
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
		public final float getFirst() {
			return _table.getFirst();
		}
		public final float getLast() {
			return _table.getLast();
		}
		public final float peek() {
			return _table.peek();
		}
		public final float element() {
			return _table.element();
		}
		public final void addFirst(float value) {
			throw new UnsupportedOperationException("Unmodifiable");
		}
		public final void addLast(float value) {
			throw new UnsupportedOperationException("Unmodifiable");
		}
		public final float removeFirst() {
			throw new UnsupportedOperationException("Unmodifiable");
		}
		public final float removeLast() {
			throw new UnsupportedOperationException("Unmodifiable");
		}
		public final void push(float value) {
			throw new UnsupportedOperationException("Unmodifiable");
		}
		public final float pop() {
			throw new UnsupportedOperationException("Unmodifiable");
		}
		public final boolean offer(float value) {
			throw new UnsupportedOperationException("Unmodifiable");
		}
		public final float poll() {
			throw new UnsupportedOperationException("Unmodifiable");
		}
		public final FastPrimitiveIterator/*FastFloatIterator*/ iterator() {
			return new FastFloatIterator() {
				private final FastFloatIterator _it = (FastFloatIterator) _table
						.iterator();
				public final boolean hasNext() {
					return _it.hasNext();
				}
				public final float next() {
					return _it.next();
				}
				public final void remove() {
					throw new UnsupportedOperationException("Unmodifiable");
				}
			};
		}
	}
	private static final class Shared extends FastFloatTable
			implements Cloneable, RandomAccess, Reusable, Serializable {
		private final FastFloatTable _table;
		private final ReadWriteLock _lock;
		private Shared(FastFloatTable table) {
			super(0);
			_table = table;
			_lock = new ReentrantWriterPreferenceReadWriteLock();
		}
		public final FastPrimitiveCollection/*FastFloatTable*/ unmodifiable() {
			return _table.unmodifiable();
		}
		public final FastPrimitiveCollection/*FastFloatTable*/ shared() {
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
		public final boolean contains(float value) {
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
		public final int indexOf(float value) {
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
		public final int lastIndexOf(float value) {
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
		public final Object/*FastFloatTable*/ clone()
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
		public final float[] toArray() {
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
		public final float get(int index) {
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
		public final float set(int index, float value) {
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
		public final boolean add(float value) {
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
		public final void add(int index, float element) {
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
		public final float remove(int index) {
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
		public final boolean removeElement(float value) {
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
		public final boolean addAll(float[] values) {
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
		public final boolean addAll(int index, float[] values) {
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
		public final boolean addAll(FastFloatTable values) {
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
		public final boolean addAll(int index, FastFloatTable values) {
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
		public final float getFirst() {
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
		public final float getLast() {
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
		public final float peek() {
			return getFirst();
		}
		public final float element() {
			return getFirst();
		}
		public final void addFirst(float value) {
			add(0, value);
		}
		public final void addLast(float value) {
			add(value);
		}
		public final float removeFirst() {
			return remove(0);
		}
		public final float removeLast() {
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
		public final void push(float value) {
			addFirst(value);
		}
		public final float pop() {
			return removeFirst();
		}
		public final boolean offer(float value) {
			return add(value);
		}
		public final float poll() {
			return removeFirst();
		}
		public final FastPrimitiveIterator/*FastFloatIterator*/ iterator() {
			final FastFloatIterator iterator = (FastFloatIterator) _table
					.iterator();
			return new FastFloatIterator() {
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
				public final float next() {
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
