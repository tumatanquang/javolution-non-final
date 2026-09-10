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
import _templates.javax.realtime.MemoryArea;
import _templates.javolution.lang.Reusable;
import _templates.javolution.util.concurrent.locks.ReadWriteLock;
import _templates.javolution.util.concurrent.locks.ReentrantWriterPreferenceReadWriteLock;
import _templates.javolution.util.concurrent.locks.Sync;
/**
 * <p> This class represents a high-performance, open-addressing hash set of <code>float</code>
 *     primitive values with linear probing.</p>
 *
 * <p> Unlike standard <code>java.util.HashSet&lt;Float&gt;</code> or <code>FastSet&lt;Float&gt;</code>
 *     which allocate wrapping objects and entry nodes for every single value, {@link FastFloatSet}
 *     stores elements in a single contiguous primitive array without any boxing overhead.
 *     Memory consumption is reduced by 80-90% and cache locality is maximized.</p>
 *
 * <p> Standard mathematical set semantics (no duplicate elements, $\mathcal{O}(1)$ average
 *     time complexity for lookup, insertion, and deletion) are guaranteed.</p>
 *
 * @author <a href="mailto:jean-marie@dautelle.com">Jean-Marie Dautelle</a>
 * @version 6.1.0, August 22, 2026
 */
public class FastFloatSet extends FastPrimitiveCollection
		implements Cloneable, Reusable, Serializable {
	private static final byte FREE = 0;
	private static final byte OCCUPIED = 1;
	private static final byte REMOVED = 2;
	private static final int DEFAULT_CAPACITY = 16;
	private static final float LOAD_FACTOR = 0.75f;
	/**
	 * Holds the table entries.
	 */
	private transient float[] _table;
	/**
	 * Holds the state of each slot in the table (FREE, OCCUPIED, or REMOVED).
	 */
	private transient byte[] _state;
	/**
	 * Holds the bitmask for hash modulo calculations.
	 */
	private transient int _mask;
	/**
	 * Holds the resize threshold.
	 */
	private transient int _threshold;
	/**
	 * Holds the count of used slots (OCCUPIED + REMOVED).
	 */
	private transient int _used;
	/**
	 * Constructs an empty set with default initial capacity (16).
	 */
	public FastFloatSet() {
		this(DEFAULT_CAPACITY);
	}
	/**
	 * Constructs an empty set with the specified initial capacity.
	 *
	 * @param capacity the initial capacity.
	 */
	public FastFloatSet(int capacity) {
		int cap = DEFAULT_CAPACITY;
		final int min = Math.max(DEFAULT_CAPACITY, capacity);
		while(cap < min) {
			cap <<= 1;
		}
		_table = new float[cap];
		_state = new byte[cap];
		_mask = cap - 1;
		_threshold = (int) (cap * LOAD_FACTOR);
	}
	/**
	 * Constructs a set containing the elements of the specified array.
	 *
	 * @param values the array of initial values.
	 * @throws NullPointException if values is null
	 */
	public FastFloatSet(float[] values) {
		this(values.length);
		addAll(values);
	}
	/**
	 * Constructs a set containing the elements of the specified set.
	 *
	 * @param values the set of initial values.
	 * @throws NullPointException if values is null
	 */
	public FastFloatSet(FastFloatSet values) {
		this(values.size());
		addAll(values);
	}
	private final int hash(float value) {
		final int bits = Float.floatToIntBits(value);
		int h = bits * 0x9E3779B9;
		h ^= h >>> 16;
		return h & _mask;
	}
	private static final boolean equals(float v1, float v2) {
		return Float.floatToIntBits(v1) == Float.floatToIntBits(v2);
	}
	private final void rehash(final int newCapacity) {
		MemoryArea.getMemoryArea(this).executeInArea(new Runnable() {
			public final void run() {
				final float[] oldTable = _table;
				final byte[] oldState = _state;
				final int oldCap = oldTable.length;
				_table = new float[newCapacity];
				_state = new byte[newCapacity];
				_mask = newCapacity - 1;
				_threshold = (int) (newCapacity * LOAD_FACTOR);
				_used = 0;
				for(int i = 0; i < oldCap; ++i) {
					if(oldState[i] == OCCUPIED) {
						final float val = oldTable[i];
						int idx = hash(val);
						while(_state[idx] != FREE) {
							idx = idx + 1 & _mask;
						}
						_table[idx] = val;
						_state[idx] = OCCUPIED;
						++_used;
					}
				}
			}
		});
	}
	/**
	 * Adds the specified value to this set if it is not already present.
	 *
	 * @param value the value to be added.
	 * @return <code>true</code> if this set did not already contain the specified value.
	 */
	public boolean add(float value) {
		final int length = _table.length;
		int idx = hash(value), firstRemoved = -1, free = -1;
		for(int probes = -1; ++probes < length;) {
			final byte state = _state[idx];
			if(state == FREE) {
				free = idx;
				break;
			}
			if(state == OCCUPIED) {
				if(equals(_table[idx], value))
					return false;
			}
			else if(firstRemoved < 0) {
				firstRemoved = idx;
			}
			idx = idx + 1 & _mask;
		}
		final int targetIdx = firstRemoved >= 0 ? firstRemoved : free;
		if(targetIdx < 0) {
			rehash(_size + 1 >= _threshold ? length << 1 : length);
			return add(value);
		}
		if(_size + 1 >= _threshold) {
			rehash(length << 1);
			return add(value);
		}
		if(_used + (_state[targetIdx] == FREE ? 1 : 0) >= _threshold) {
			rehash(length);
			return add(value);
		}
		_table[targetIdx] = value;
		if(_state[targetIdx] == FREE) {
			++_used;
		}
		_state[targetIdx] = OCCUPIED;
		++_size;
		return true;
	}
	/**
	 * Returns <code>true</code> if this set contains the specified value.
	 *
	 * @param value the value resource whose presence in this set is to be tested.
	 * @return <code>true</code> if this set contains the specified value.
	 */
	public boolean contains(float value) {
		if(_size == 0)
			return false;
		final int length = _table.length;
		int idx = hash(value);
		for(int probes = -1; ++probes < length;) {
			if(_state[idx] == FREE)
				return false;
			if(_state[idx] == OCCUPIED && equals(_table[idx], value))
				return true;
			idx = idx + 1 & _mask;
		}
		return false;
	}
	/**
	 * Removes the specified value from this set if it is present.
	 *
	 * @param value the value to be removed from this set, if present.
	 * @return <code>true</code> if this set contained the specified value.
	 */
	public boolean remove(float value) {
		if(_size == 0)
			return false;
		final int length = _table.length;
		int idx = hash(value);
		for(int probes = -1; ++probes < length;) {
			if(_state[idx] == FREE)
				return false;
			if(_state[idx] == OCCUPIED && equals(_table[idx], value)) {
				_state[idx] = REMOVED;
				--_size;
				return true;
			}
			idx = idx + 1 & _mask;
		}
		return false;
	}
	/**
	 * Removes the specified value from this set if it is present.
	 *
	 * @param value the value to be removed from this set, if present.
	 * @return <code>true</code> if this set contained the specified value.
	 */
	public boolean removeElement(float value) {
		return remove(value);
	}
	/**
	 * {@inheritDoc}
	 */
	public void clear() {
		if(_used == 0)
			return;
		final int length = _state.length;
		for(int i = -1; ++i < length;) {
			_state[i] = FREE;
		}
		_size = 0;
		_used = 0;
	}
	/**
	 * {@inheritDoc}
	 */
	public void reset() {
		clear();
	}
	/**
	 * {@inheritDoc}
	 */
	public void trimToSize() {
		int minCap = DEFAULT_CAPACITY;
		while(minCap * LOAD_FACTOR < _size) {
			minCap <<= 1;
		}
		if(minCap < _table.length) {
			rehash(minCap);
		}
		else if(_used > _size) {
			rehash(_table.length);
		}
	}
	/**
	 * {@inheritDoc}
	 */
	public void ensureCapacity(int minCapacity) {
		if(minCapacity > _threshold) {
			int newCap = _table.length;
			while(newCap * LOAD_FACTOR < minCapacity) {
				newCap <<= 1;
			}
			rehash(newCap);
		}
	}
	/**
	 * Adds all elements from the specified array to this set.
	 *
	 * @param values array containing elements to be added.
	 * @return <code>true</code> if this set changed as a result of the call.
	 */
	public boolean addAll(float[] values) {
		final int length;
		if(values == null || (length = values.length) == 0)
			return false;
		boolean modified = false;
		ensureCapacity(_size + length);
		for(int i = -1; ++i < length;) {
			if(add(values[i])) {
				modified = true;
			}
		}
		return modified;
	}
	/**
	 * Adds all elements from the specified set to this set.
	 *
	 * @param values set containing elements to be added.
	 * @return <code>true</code> if this set changed as a result of the call.
	 */
	public boolean addAll(FastFloatSet values) {
		if(values == null || values.isEmpty())
			return false;
		boolean modified = false;
		ensureCapacity(_size + values.size());
		final FastFloatIterator it = (FastFloatIterator) values.iterator();
		while(it.hasNext()) {
			if(add(it.next())) {
				modified = true;
			}
		}
		return modified;
	}
	/**
	 * Returns <code>true</code> if this set contains all elements of the specified array.
	 *
	 * @param values array of elements to be checked for containment in this set.
	 * @return <code>true</code> if this set contains all elements of the specified array.
	 */
	public boolean containsAll(float[] values) {
		final int length;
		if(values == null || (length = values.length) == 0)
			return true;
		if(_size == 0)
			return false;
		for(int i = -1; ++i < length;) {
			if(!contains(values[i]))
				return false;
		}
		return true;
	}
	/**
	 * Returns <code>true</code> if this set contains all elements of the specified set.
	 *
	 * @param values set of elements to be checked for containment in this set.
	 * @return <code>true</code> if this set contains all elements of the specified set.
	 */
	public boolean containsAll(FastFloatSet values) {
		if(values == null || values.isEmpty())
			return true;
		if(_size == 0)
			return false;
		final FastFloatIterator it = (FastFloatIterator) values.iterator();
		while(it.hasNext()) {
			if(!contains(it.next()))
				return false;
		}
		return true;
	}
	/**
	 * Removes from this set all of its elements that are contained in the specified array.
	 *
	 * @param values array containing elements to be removed from this set.
	 * @return <code>true</code> if this set changed as a result of the call.
	 */
	public boolean removeAll(float[] values) {
		final int length;
		if(values == null || (length = values.length) == 0 || _size == 0)
			return false;
		boolean modified = false;
		for(int i = -1; ++i < length;) {
			if(remove(values[i])) {
				modified = true;
			}
		}
		return modified;
	}
	/**
	 * Removes from this set all of its elements that are contained in the specified set.
	 *
	 * @param values set containing elements to be removed from this set.
	 * @return <code>true</code> if this set changed as a result of the call.
	 */
	public boolean removeAll(FastFloatSet values) {
		if(values == null || values.isEmpty() || _size == 0)
			return false;
		boolean modified = false;
		final FastFloatIterator it = (FastFloatIterator) values.iterator();
		while(it.hasNext()) {
			if(remove(it.next())) {
				modified = true;
			}
		}
		return modified;
	}
	/**
	 * Retains only the elements in this set that are contained in the specified set.
	 *
	 * @param values set containing elements to be retained in this set.
	 * @return <code>true</code> if this set changed as a result of the call.
	 */
	public boolean retainAll(FastFloatSet values) {
		if(values == null || _size == 0)
			return false;
		final int length = _table.length;
		boolean modified = false;
		for(int i = -1; ++i < length;) {
			if(_state[i] == OCCUPIED && !values.contains(_table[i])) {
				_state[i] = REMOVED;
				--_size;
				modified = true;
			}
		}
		return modified;
	}
	/**
	 * Returns an array containing all of the elements in this set.
	 *
	 * @return an array containing all of the elements in this set.
	 */
	public float[] toArray() {
		final float[] result = new float[_size];
		if(_size == 0)
			return result;
		final int length = _table.length;
		for(int i = -1, j = -1; ++i < length;) {
			if(_state[i] == OCCUPIED) {
				result[++j] = _table[i];
			}
		}
		return result;
	}
	/**
	 * {@inheritDoc}
	 */
	public FastPrimitiveIterator/*FastFloatIterator*/ iterator() {
		return new SetIterator(this);
	}
	/**
	 * {@inheritDoc}
	 */
	public FastPrimitiveCollection/*FastFloatSet*/ unmodifiable() {
		return new Unmodifiable(this);
	}
	/**
	 * {@inheritDoc}
	 */
	public FastPrimitiveCollection/*FastFloatSet*/ shared() {
		return new Shared(this);
	}
	/**
	 * Returns a copy of this set.
	 *
	 * @return a clone of this instance.
	 * @throws CloneNotSupportedException if cloning is not supported.
	 */
	public Object/*FastFloatSet*/ clone() throws CloneNotSupportedException {
		/*@JVM-1.1+@
		if(true) {
			final FastFloatSet c = (FastFloatSet) super.clone();
			c._table = new float[_table.length];
			c._state = new byte[_state.length];
			System.arraycopy(_table, 0, c._table, 0, _table.length);
			System.arraycopy(_state, 0, c._state, 0, _state.length);
			c._mask = _mask;
			c._threshold = _threshold;
			c._used = _used;
			c._size = _size;
			return c;
		}
		/**/
		throw new UnsupportedOperationException("J2ME Not Supported Yet");
	}
	private final void writeObject(ObjectOutputStream s) throws IOException {
		s.defaultWriteObject();
		s.writeInt(_size);
		final int length = _table.length;
		for(int i = -1; ++i < length;) {
			if(_state[i] == OCCUPIED) {
				s.writeFloat(_table[i]);
			}
		}
	}
	private final void readObject(ObjectInputStream s)
			throws IOException, ClassNotFoundException {
		s.defaultReadObject();
		final int count = s.readInt();
		int cap = DEFAULT_CAPACITY;
		while(cap < count) {
			cap <<= 1;
		}
		_table = new float[cap];
		_state = new byte[cap];
		_mask = cap - 1;
		_threshold = (int) (cap * LOAD_FACTOR);
		_size = 0;
		_used = 0;
		for(int i = -1; ++i < count;) {
			add(s.readFloat());
		}
	}
	/**
	 * Compares the specified object with this set for equality.
	 *
	 * @param o the object to be compared for equality with this set.
	 * @return <code>true</code> if the specified object is equal to this set.
	 */
	public boolean equals(Object o) {
		if(this == o)
			return true;
		if(!(o instanceof FastFloatSet))
			return false;
		final FastFloatSet that = (FastFloatSet) o;
		if(size() != that.size())
			return false;
		return containsAll(that);
	}
	/**
	 * Returns the hash code value for this set.
	 *
	 * @return the hash code value for this set.
	 */
	public int hashCode() {
		if(_size == 0)
			return 0;
		int h = 0;
		final int length = _table.length;
		for(int i = -1; ++i < length;) {
			if(_state[i] == OCCUPIED) {
				h += Float.floatToIntBits(_table[i]);
			}
		}
		return h;
	}
	/**
	 * Returns a string representation of this set.
	 *
	 * @return a string representation of this set.
	 */
	public String toString() {
		if(_size == 0)
			return "[]";
		final StringBuffer/*StringBuilder*/ sb = new StringBuffer/*StringBuilder*/();
		sb.append('[');
		boolean first = true;
		final int length = _table.length;
		for(int i = -1; ++i < length;) {
			if(_state[i] == OCCUPIED) {
				if(!first) {
					sb.append(',').append(' ');
				}
				sb.append(_table[i]);
				first = false;
			}
		}
		return sb.append(']').toString();
	}
	private static final class SetIterator implements FastFloatIterator {
		private final FastFloatSet _set;
		private int _cursor = 0;
		private float _lastValue;
		private boolean _canRemove;
		private SetIterator(FastFloatSet set) {
			_set = set;
			if(set._size == 0) {
				_cursor = set._table.length;
				return;
			}
			advance();
		}
		private final void advance() {
			for(final int length = _set._table.length; _cursor < length
					&& _set._state[_cursor] != OCCUPIED;) {
				++_cursor;
			}
		}
		public final boolean hasNext() {
			return _cursor < _set._table.length;
		}
		public final float next() {
			if(!hasNext())
				throw new NoSuchElementException();
			final float val = _set._table[_cursor++];
			_lastValue = val;
			_canRemove = true;
			advance();
			return val;
		}
		public final void remove() {
			if(!_canRemove)
				throw new IllegalStateException();
			_set.remove(_lastValue);
			_canRemove = false;
		}
	}
	private static final class Unmodifiable extends FastFloatSet
			implements Cloneable, Reusable, Serializable {
		private final FastFloatSet _set;
		private Unmodifiable(FastFloatSet set) {
			super(0);
			_set = set;
		}
		public final FastPrimitiveCollection/*FastFloatSet*/ unmodifiable() {
			return this;
		}
		public final FastPrimitiveCollection/*FastFloatSet*/ shared() {
			throw new UnsupportedOperationException("Unmodifiable");
		}
		public final int size() {
			return _set.size();
		}
		public final boolean isEmpty() {
			return _set.isEmpty();
		}
		public final void trimToSize() {
			throw new UnsupportedOperationException("Unmodifiable");
		}
		public final void ensureCapacity(int min) {
			throw new UnsupportedOperationException("Unmodifiable");
		}
		public final boolean contains(float value) {
			return _set.contains(value);
		}
		public final Object/*FastFloatSet*/ clone()
				throws CloneNotSupportedException {
			return _set.clone();
		}
		public final float[] toArray() {
			return _set.toArray();
		}
		public final boolean add(float value) {
			throw new UnsupportedOperationException("Unmodifiable");
		}
		public final boolean remove(float value) {
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
		public final boolean addAll(FastFloatSet values) {
			throw new UnsupportedOperationException("Unmodifiable");
		}
		public final boolean containsAll(float[] values) {
			return _set.containsAll(values);
		}
		public final boolean containsAll(FastFloatSet values) {
			return _set.containsAll(values);
		}
		public final boolean removeAll(float[] values) {
			throw new UnsupportedOperationException("Unmodifiable");
		}
		public final boolean removeAll(FastFloatSet values) {
			throw new UnsupportedOperationException("Unmodifiable");
		}
		public final boolean retainAll(FastFloatSet values) {
			throw new UnsupportedOperationException("Unmodifiable");
		}
		public final boolean equals(Object o) {
			return _set.equals(o);
		}
		public final int hashCode() {
			return _set.hashCode();
		}
		public final String toString() {
			return _set.toString();
		}
		public final void reset() {
			throw new UnsupportedOperationException("Unmodifiable");
		}
		public final FastPrimitiveIterator/*FastFloatIterator*/ iterator() {
			final FastFloatIterator iterator = (FastFloatIterator) _set
					.iterator();
			return new FastFloatIterator() {
				public final boolean hasNext() {
					return iterator.hasNext();
				}
				public final float next() {
					return iterator.next();
				}
				public final void remove() {
					throw new UnsupportedOperationException("Unmodifiable");
				}
			};
		}
	}
	private static final class Shared extends FastFloatSet
			implements Cloneable, Reusable, Serializable {
		private final FastFloatSet _set;
		private final ReadWriteLock _lock;
		private Shared(FastFloatSet set) {
			super(0);
			_set = set;
			_lock = new ReentrantWriterPreferenceReadWriteLock();
		}
		public final FastPrimitiveCollection/*FastFloatSet*/ unmodifiable() {
			return _set.unmodifiable();
		}
		public final FastPrimitiveCollection/*FastFloatSet*/ shared() {
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
							return _set.size();
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
							return _set.isEmpty();
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
							_set.trimToSize();
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
							_set.ensureCapacity(min);
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
							return _set.contains(value);
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
		public final Object/*FastFloatSet*/ clone()
				throws CloneNotSupportedException {
			boolean wasInterrupted = /*@JVM-1.1+@ true ? Thread.interrupted() : /**/false;
			try {
				final Sync r = _lock.readLock();
				for(;;) {
					try {
						r.acquire();
						try {
							return _set.clone();
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
							return _set.toArray();
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
		public final boolean add(float value) {
			boolean wasInterrupted = /*@JVM-1.1+@ true ? Thread.interrupted() : /**/false;
			try {
				final Sync w = _lock.writeLock();
				for(;;) {
					try {
						w.acquire();
						try {
							return _set.add(value);
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
		public final boolean remove(float value) {
			boolean wasInterrupted = /*@JVM-1.1+@ true ? Thread.interrupted() : /**/false;
			try {
				final Sync w = _lock.writeLock();
				for(;;) {
					try {
						w.acquire();
						try {
							return _set.remove(value);
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
			return remove(value);
		}
		public final void clear() {
			boolean wasInterrupted = /*@JVM-1.1+@ true ? Thread.interrupted() : /**/false;
			try {
				final Sync w = _lock.writeLock();
				for(;;) {
					try {
						w.acquire();
						try {
							_set.clear();
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
							return _set.addAll(values);
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
		public final boolean addAll(FastFloatSet values) {
			boolean wasInterrupted = /*@JVM-1.1+@ true ? Thread.interrupted() : /**/false;
			try {
				final Sync w = _lock.writeLock();
				for(;;) {
					try {
						w.acquire();
						try {
							return _set.addAll(values);
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
		public final boolean containsAll(float[] values) {
			boolean wasInterrupted = /*@JVM-1.1+@ true ? Thread.interrupted() : /**/false;
			try {
				final Sync r = _lock.readLock();
				for(;;) {
					try {
						r.acquire();
						try {
							return _set.containsAll(values);
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
		public final boolean containsAll(FastFloatSet values) {
			boolean wasInterrupted = /*@JVM-1.1+@ true ? Thread.interrupted() : /**/false;
			try {
				final Sync r = _lock.readLock();
				for(;;) {
					try {
						r.acquire();
						try {
							return _set.containsAll(values);
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
		public final boolean removeAll(float[] values) {
			boolean wasInterrupted = /*@JVM-1.1+@ true ? Thread.interrupted() : /**/false;
			try {
				final Sync w = _lock.writeLock();
				for(;;) {
					try {
						w.acquire();
						try {
							return _set.removeAll(values);
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
		public final boolean removeAll(FastFloatSet values) {
			boolean wasInterrupted = /*@JVM-1.1+@ true ? Thread.interrupted() : /**/false;
			try {
				final Sync w = _lock.writeLock();
				for(;;) {
					try {
						w.acquire();
						try {
							return _set.removeAll(values);
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
		public final boolean retainAll(FastFloatSet values) {
			boolean wasInterrupted = /*@JVM-1.1+@ true ? Thread.interrupted() : /**/false;
			try {
				final Sync w = _lock.writeLock();
				for(;;) {
					try {
						w.acquire();
						try {
							return _set.retainAll(values);
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
							return _set.equals(o);
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
							return _set.hashCode();
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
							return _set.toString();
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
							_set.reset();
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
		public final FastPrimitiveIterator/*FastFloatIterator*/ iterator() {
			final FastFloatIterator iterator = (FastFloatIterator) _set
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