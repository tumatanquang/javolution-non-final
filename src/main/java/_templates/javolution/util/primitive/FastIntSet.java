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
 * <p> This class represents a high-performance, open-addressing hash set of <code>int</code>
 *     primitive values with linear probing.</p>
 *
 * <p> Unlike standard <code>java.util.HashSet&lt;Integer&gt;</code> or <code>FastSet&lt;Integer&gt;</code>
 *     which allocate wrapping objects and entry nodes for every single value, {@link FastIntSet}
 *     stores elements in a single contiguous primitive array without any boxing overhead.
 *     Memory consumption is reduced by 80-90% and cache locality is maximized.</p>
 *
 * <p> Standard mathematical set semantics (no duplicate elements, $\mathcal{O}(1)$ average
 *     time complexity for lookup, insertion, and deletion) are guaranteed.</p>
 *
 * @author <a href="mailto:jean-marie@dautelle.com">Jean-Marie Dautelle</a>
 * @version 6.1.0, August 22, 2026
 */
public class FastIntSet extends FastPrimitiveCollection
		implements Cloneable, Reusable, Serializable {
	private static final byte FREE = 0;
	private static final byte OCCUPIED = 1;
	private static final byte REMOVED = 2;
	private static final int DEFAULT_CAPACITY = 16;
	private static final float LOAD_FACTOR = 0.75f;
	/**
	 * Holds the table entries.
	 */
	private transient int[] _table;
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
	public FastIntSet() {
		this(DEFAULT_CAPACITY);
	}
	/**
	 * Constructs an empty set with the specified initial capacity.
	 *
	 * @param capacity the initial capacity.
	 */
	public FastIntSet(int capacity) {
		int cap = DEFAULT_CAPACITY;
		final int min = Math.max(DEFAULT_CAPACITY, capacity);
		while(cap < min) {
			cap <<= 1;
		}
		_table = new int[cap];
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
	public FastIntSet(int[] values) {
		this(values.length);
		addAll(values);
	}
	/**
	 * Constructs a set containing the elements of the specified set.
	 *
	 * @param values the set of initial values.
	 * @throws NullPointException if values is null
	 */
	public FastIntSet(FastIntSet values) {
		this(values.size());
		addAll(values);
	}
	private final int hash(int value) {
		int h = value * 0x9E3779B9;
		h ^= h >>> 16;
		return h & _mask;
	}
	private final void rehash(final int newCapacity) {
		MemoryArea.getMemoryArea(this).executeInArea(new Runnable() {
			public final void run() {
				final int[] oldTable = _table;
				final byte[] oldState = _state;
				final int oldCap = oldTable.length;
				_table = new int[newCapacity];
				_state = new byte[newCapacity];
				_mask = newCapacity - 1;
				_threshold = (int) (newCapacity * LOAD_FACTOR);
				_used = 0;
				for(int i = 0; i < oldCap; ++i) {
					if(oldState[i] == OCCUPIED) {
						final int val = oldTable[i];
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
	public boolean add(int value) {
		int idx = hash(value), firstRemoved = -1, free = -1;
		final int length = _table.length;
		for(int probes = -1; ++probes < length;) {
			final byte state = _state[idx];
			if(state == FREE) {
				free = idx;
				break;
			}
			if(state == OCCUPIED) {
				if(_table[idx] == value)
					return false;
			}
			else if(firstRemoved < 0) {
				firstRemoved = idx;
			}
			idx = idx + 1 & _mask;
		}
		final int targetIdx = firstRemoved >= 0 ? firstRemoved : free;
		if(targetIdx < 0) {
			rehash(size + 1 >= _threshold ? length << 1 : length);
			return add(value);
		}
		if(size + 1 >= _threshold) {
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
		++size;
		return true;
	}
	/**
	 * Returns <code>true</code> if this set contains the specified value.
	 *
	 * @param value the value whose presence in this set is to be tested.
	 * @return <code>true</code> if this set contains the specified value.
	 */
	public boolean contains(int value) {
		int idx = hash(value);
		final int length = _table.length;
		for(int probes = -1; ++probes < length;) {
			if(_state[idx] == FREE)
				return false;
			if(_state[idx] == OCCUPIED && _table[idx] == value)
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
	public boolean remove(int value) {
		int idx = hash(value);
		final int length = _table.length;
		for(int probes = -1; ++probes < length;) {
			if(_state[idx] == FREE)
				return false;
			if(_state[idx] == OCCUPIED && _table[idx] == value) {
				_state[idx] = REMOVED;
				--size;
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
	public boolean removeElement(int value) {
		return remove(value);
	}
	/**
	 * {@inheritDoc}
	 */
	public void clear() {
		final int length = _state.length;
		for(int i = -1; ++i < length;) {
			_state[i] = FREE;
		}
		size = 0;
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
		while(minCap * LOAD_FACTOR < size) {
			minCap <<= 1;
		}
		if(minCap < _table.length) {
			rehash(minCap);
		}
		else if(_used > size) {
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
	public boolean addAll(int[] values) {
		if(values == null || values.length == 0)
			return false;
		boolean modified = false;
		final int length = values.length;
		ensureCapacity(size + length);
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
	public boolean addAll(FastIntSet values) {
		if(values == null || values.size() == 0)
			return false;
		boolean modified = false;
		ensureCapacity(size + values.size());
		final FastIntIterator it = (FastIntIterator) values.iterator();
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
	public boolean containsAll(int[] values) {
		if(values == null)
			return true;
		final int length = values.length;
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
	public boolean containsAll(FastIntSet values) {
		if(values == null)
			return true;
		final FastIntIterator it = (FastIntIterator) values.iterator();
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
	public boolean removeAll(int[] values) {
		if(values == null || values.length == 0 || size == 0)
			return false;
		boolean modified = false;
		final int length = values.length;
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
	public boolean removeAll(FastIntSet values) {
		if(values == null || values.size() == 0 || size == 0)
			return false;
		boolean modified = false;
		final FastIntIterator it = (FastIntIterator) values.iterator();
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
	public boolean retainAll(FastIntSet values) {
		if(values == null || size == 0)
			return false;
		boolean modified = false;
		final int length = _table.length;
		for(int i = -1; ++i < length;) {
			if(_state[i] == OCCUPIED && !values.contains(_table[i])) {
				_state[i] = REMOVED;
				--size;
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
	public int[] toArray() {
		final int[] result = new int[size];
		int count = 0;
		final int length = _table.length;
		for(int i = -1; ++i < length;) {
			if(_state[i] == OCCUPIED) {
				result[count++] = _table[i];
			}
		}
		return result;
	}
	/**
	 * {@inheritDoc}
	 */
	public FastPrimitiveIterator/*FastIntIterator*/ iterator() {
		return new SetIterator(this);
	}
	/**
	 * {@inheritDoc}
	 */
	public FastPrimitiveCollection/*FastIntSet*/ unmodifiable() {
		return new Unmodifiable(this);
	}
	/**
	 * {@inheritDoc}
	 */
	public FastPrimitiveCollection/*FastIntSet*/ shared() {
		return new Shared(this);
	}
	/**
	 * Returns a copy of this set.
	 *
	 * @return a clone of this instance.
	 * @throws CloneNotSupportedException if cloning is not supported.
	 */
	public Object/*FastIntSet*/ clone() throws CloneNotSupportedException {
		/*@JVM-1.1+@
		if(true) {
			final FastIntSet c = (FastIntSet) super.clone();
			c._table = new int[_table.length];
			c._state = new byte[_state.length];
			System.arraycopy(_table, 0, c._table, 0, _table.length);
			System.arraycopy(_state, 0, c._state, 0, _state.length);
			c._mask = _mask;
			c._threshold = _threshold;
			c._used = _used;
			c.size = size;
			return c;
		}
		/**/
		throw new UnsupportedOperationException("J2ME Not Supported Yet");
	}
	private final void writeObject(ObjectOutputStream s) throws IOException {
		s.defaultWriteObject();
		s.writeInt(size);
		final int length = _table.length;
		for(int i = -1; ++i < length;) {
			if(_state[i] == OCCUPIED) {
				s.writeInt(_table[i]);
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
		_table = new int[cap];
		_state = new byte[cap];
		_mask = cap - 1;
		_threshold = (int) (cap * LOAD_FACTOR);
		size = 0;
		_used = 0;
		for(int i = -1; ++i < count;) {
			add(s.readInt());
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
		if(!(o instanceof FastIntSet))
			return false;
		final FastIntSet that = (FastIntSet) o;
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
		int h = 0;
		final int length = _table.length;
		for(int i = -1; ++i < length;) {
			if(_state[i] == OCCUPIED) {
				h += _table[i];
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
		if(size == 0)
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
	private static final class SetIterator implements FastIntIterator {
		private final FastIntSet _set;
		private int _cursor = 0;
		private int _lastValue;
		private boolean _canRemove;
		private SetIterator(FastIntSet set) {
			_set = set;
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
		public final int next() {
			if(!hasNext())
				throw new NoSuchElementException();
			final int val = _set._table[_cursor++];
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
	private static final class Unmodifiable extends FastIntSet
			implements Cloneable, Reusable, Serializable {
		private final FastIntSet _set;
		private Unmodifiable(FastIntSet set) {
			super(0);
			_set = set;
		}
		public final FastPrimitiveCollection/*FastIntSet*/ unmodifiable() {
			return this;
		}
		public final FastPrimitiveCollection/*FastIntSet*/ shared() {
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
		public final boolean contains(int value) {
			return _set.contains(value);
		}
		public final Object/*FastIntSet*/ clone()
				throws CloneNotSupportedException {
			return _set.clone();
		}
		public final int[] toArray() {
			return _set.toArray();
		}
		public final boolean add(int value) {
			throw new UnsupportedOperationException("Unmodifiable");
		}
		public final boolean remove(int value) {
			throw new UnsupportedOperationException("Unmodifiable");
		}
		public final boolean removeElement(int value) {
			throw new UnsupportedOperationException("Unmodifiable");
		}
		public final void clear() {
			throw new UnsupportedOperationException("Unmodifiable");
		}
		public final boolean addAll(int[] values) {
			throw new UnsupportedOperationException("Unmodifiable");
		}
		public final boolean addAll(FastIntSet values) {
			throw new UnsupportedOperationException("Unmodifiable");
		}
		public final boolean containsAll(int[] values) {
			return _set.containsAll(values);
		}
		public final boolean containsAll(FastIntSet values) {
			return _set.containsAll(values);
		}
		public final boolean removeAll(int[] values) {
			throw new UnsupportedOperationException("Unmodifiable");
		}
		public final boolean removeAll(FastIntSet values) {
			throw new UnsupportedOperationException("Unmodifiable");
		}
		public final boolean retainAll(FastIntSet values) {
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
		public final FastPrimitiveIterator/*FastIntIterator*/ iterator() {
			final FastIntIterator iterator = (FastIntIterator) _set.iterator();
			return new FastIntIterator() {
				public final boolean hasNext() {
					return iterator.hasNext();
				}
				public final int next() {
					return iterator.next();
				}
				public final void remove() {
					throw new UnsupportedOperationException("Unmodifiable");
				}
			};
		}
	}
	private static final class Shared extends FastIntSet
			implements Cloneable, Reusable, Serializable {
		private final FastIntSet _set;
		private final ReadWriteLock _lock;
		private Shared(FastIntSet set) {
			super(0);
			_set = set;
			_lock = new ReentrantWriterPreferenceReadWriteLock();
		}
		public final FastPrimitiveCollection/*FastIntSet*/ unmodifiable() {
			return _set.unmodifiable();
		}
		public final FastPrimitiveCollection/*FastIntSet*/ shared() {
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
		public final boolean contains(int value) {
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
		public final Object/*FastIntSet*/ clone()
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
		public final int[] toArray() {
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
		public final boolean add(int value) {
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
		public final boolean remove(int value) {
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
		public final boolean removeElement(int value) {
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
		public final boolean addAll(int[] values) {
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
		public final boolean addAll(FastIntSet values) {
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
		public final boolean containsAll(int[] values) {
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
		public final boolean containsAll(FastIntSet values) {
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
		public final boolean removeAll(int[] values) {
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
		public final boolean removeAll(FastIntSet values) {
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
		public final boolean retainAll(FastIntSet values) {
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
		public final FastPrimitiveIterator/*FastIntIterator*/ iterator() {
			final FastIntIterator iterator = (FastIntIterator) _set.iterator();
			return new FastIntIterator() {
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
				public final int next() {
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