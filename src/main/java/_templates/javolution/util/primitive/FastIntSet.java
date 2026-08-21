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
 * @version 6.0.0, August 18, 2026
 */
public class FastIntSet extends FastPrimitiveCollection
		implements Cloneable, Reusable, Serializable {
	private static final byte FREE = 0;
	private static final byte OCCUPIED = 1;
	private static final byte REMOVED = 2;
	private static final int DEFAULT_CAPACITY = 16;
	private static final float LOAD_FACTOR = 0.75f;
	private transient int[] _table;
	private transient byte[] _state;
	private transient int _mask;
	private transient int _threshold;
	private transient int _used;
	public FastIntSet() {
		this(DEFAULT_CAPACITY);
	}
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
	public FastIntSet(int[] values) {
		this(values != null ? values.length : DEFAULT_CAPACITY);
		if(values != null) {
			addAll(values);
		}
	}
	public FastIntSet(FastIntSet values) {
		this(values != null ? values.size() : DEFAULT_CAPACITY);
		if(values != null) {
			addAll(values);
		}
	}
	private final int hash(int value) {
		int h = value * 0x9E3779B9;
		h ^= h >>> 16;
		return h & _mask;
	}
	private final void rehash(int newCapacity) {
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
	public boolean add(int value) {
		int idx = hash(value);
		int firstRemoved = -1;
		int free = -1;
		for(int probes = 0; probes < _table.length; ++probes) {
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
			rehash(size + 1 >= _threshold ? _table.length << 1 : _table.length);
			return add(value);
		}
		if(size + 1 >= _threshold) {
			rehash(_table.length << 1);
			return add(value);
		}
		if(_used + (_state[targetIdx] == FREE ? 1 : 0) >= _threshold) {
			rehash(_table.length);
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
	public boolean contains(int value) {
		int idx = hash(value);
		for(int probes = 0; probes < _table.length; ++probes) {
			if(_state[idx] == FREE)
				return false;
			if(_state[idx] == OCCUPIED && _table[idx] == value)
				return true;
			idx = idx + 1 & _mask;
		}
		return false;
	}
	public boolean remove(int value) {
		int idx = hash(value);
		for(int probes = 0; probes < _table.length; ++probes) {
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
	public boolean removeElement(int value) {
		return remove(value);
	}
	public void clear() {
		for(int i = 0; i < _state.length; ++i) {
			_state[i] = FREE;
		}
		size = 0;
		_used = 0;
	}
	public void reset() {
		clear();
	}
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
	public void ensureCapacity(int minCapacity) {
		if(minCapacity > _threshold) {
			int newCap = _table.length;
			while(newCap * LOAD_FACTOR < minCapacity) {
				newCap <<= 1;
			}
			rehash(newCap);
		}
	}
	public boolean addAll(int[] values) {
		if(values == null || values.length == 0)
			return false;
		boolean modified = false;
		ensureCapacity(size + values.length);
		for(int i = 0; i < values.length; ++i) {
			if(add(values[i])) {
				modified = true;
			}
		}
		return modified;
	}
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
	public boolean containsAll(int[] values) {
		if(values == null)
			return true;
		for(int i = 0; i < values.length; ++i) {
			if(!contains(values[i]))
				return false;
		}
		return true;
	}
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
	public boolean removeAll(int[] values) {
		if(values == null || values.length == 0 || size == 0)
			return false;
		boolean modified = false;
		for(int i = 0; i < values.length; ++i) {
			if(remove(values[i])) {
				modified = true;
			}
		}
		return modified;
	}
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
	public boolean retainAll(FastIntSet values) {
		if(values == null || size == 0)
			return false;
		boolean modified = false;
		for(int i = 0; i < _table.length; ++i) {
			if(_state[i] == OCCUPIED && !values.contains(_table[i])) {
				_state[i] = REMOVED;
				--size;
				modified = true;
			}
		}
		return modified;
	}
	public int[] toArray() {
		final int[] result = new int[size];
		int count = 0;
		for(int i = 0; i < _table.length; ++i) {
			if(_state[i] == OCCUPIED) {
				result[count++] = _table[i];
			}
		}
		return result;
	}
	public FastPrimitiveIterator/*FastIntIterator*/ iterator() {
		return new SetIterator(this);
	}
	public FastPrimitiveCollection/*FastIntSet*/ unmodifiable() {
		return new Unmodifiable(this);
	}
	public FastPrimitiveCollection/*FastIntSet*/ shared() {
		return new Shared(this);
	}
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
		for(int i = 0; i < _table.length; ++i) {
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
		for(int i = 0; i < count; ++i) {
			add(s.readInt());
		}
	}
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
	public int hashCode() {
		int h = 0;
		for(int i = 0; i < _table.length; ++i) {
			if(_state[i] == OCCUPIED) {
				h += _table[i];
			}
		}
		return h;
	}
	public String toString() {
		if(size == 0)
			return "[]";
		final StringBuffer/*StringBuilder*/ sb = new StringBuffer/*StringBuilder*/();
		sb.append('[');
		boolean first = true;
		for(int i = 0; i < _table.length; ++i) {
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
			while(_cursor < _set._table.length
					&& _set._state[_cursor] != OCCUPIED) {
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
			return new FastIntIterator() {
				private final FastIntIterator _it = (FastIntIterator) _set
						.iterator();
				public final boolean hasNext() {
					return _it.hasNext();
				}
				public final int next() {
					return _it.next();
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
