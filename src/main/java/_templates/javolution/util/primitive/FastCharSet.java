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
 * <p> This class represents a high-performance, open-addressing hash set of <code>char</code>
 *     primitive values with linear probing.</p>
 *
 * <p> Unlike standard <code>java.util.HashSet&lt;Character&gt;</code> or <code>FastSet&lt;Character&gt;</code>
 *     which allocate wrapping objects and entry nodes for every single value, {@link FastCharSet}
 *     stores elements in a single contiguous primitive array without any boxing overhead.
 *     Memory consumption is reduced by 80-90% and cache locality is maximized.</p>
 *
 * <p> Standard mathematical set semantics (no duplicate elements, $\mathcal{O}(1)$ average
 *     time complexity for lookup, insertion, and deletion) are guaranteed.</p>
 *
 * @author <a href="mailto:jean-marie@dautelle.com">Jean-Marie Dautelle</a>
 * @version 6.0.0, August 18, 2026
 */
public class FastCharSet extends FastPrimitiveCollection
		implements Cloneable, Reusable, Serializable {
	private static final byte FREE = 0;
	private static final byte OCCUPIED = 1;
	private static final byte REMOVED = 2;
	private static final int DEFAULT_CAPACITY = 16;
	private static final float LOAD_FACTOR = 0.75f;
	private transient char[] _table;
	private transient byte[] _state;
	private transient int _mask;
	private transient int _threshold;
	private transient int _used;
	public FastCharSet() {
		this(DEFAULT_CAPACITY);
	}
	public FastCharSet(int capacity) {
		int cap = DEFAULT_CAPACITY;
		final int min = Math.max(DEFAULT_CAPACITY, capacity);
		while(cap < min) {
			cap <<= 1;
		}
		_table = new char[cap];
		_state = new byte[cap];
		_mask = cap - 1;
		_threshold = (int) (cap * LOAD_FACTOR);
	}
	public FastCharSet(char[] values) {
		this(values != null ? values.length : DEFAULT_CAPACITY);
		if(values != null) {
			addAll(values);
		}
	}
	public FastCharSet(FastCharSet values) {
		this(values != null ? values.size() : DEFAULT_CAPACITY);
		if(values != null) {
			addAll(values);
		}
	}
	private final int hash(char value) {
		int h = value * 0x9E3779B9;
		h ^= h >>> 16;
		return h & _mask;
	}
	private final void rehash(int newCapacity) {
		final char[] oldTable = _table;
		final byte[] oldState = _state;
		final int oldCap = oldTable.length;
		_table = new char[newCapacity];
		_state = new byte[newCapacity];
		_mask = newCapacity - 1;
		_threshold = (int) (newCapacity * LOAD_FACTOR);
		_used = 0;
		for(int i = 0; i < oldCap; ++i) {
			if(oldState[i] == OCCUPIED) {
				final char val = oldTable[i];
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
	public boolean add(char value) {
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
	public boolean contains(char value) {
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
	public boolean remove(char value) {
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
	public boolean removeElement(char value) {
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
	public boolean addAll(char[] values) {
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
	public boolean addAll(FastCharSet values) {
		if(values == null || values.size() == 0)
			return false;
		boolean modified = false;
		ensureCapacity(size + values.size());
		final FastCharIterator it = (FastCharIterator) values.iterator();
		while(it.hasNext()) {
			if(add(it.next())) {
				modified = true;
			}
		}
		return modified;
	}
	public boolean containsAll(char[] values) {
		if(values == null)
			return true;
		for(int i = 0; i < values.length; ++i) {
			if(!contains(values[i]))
				return false;
		}
		return true;
	}
	public boolean containsAll(FastCharSet values) {
		if(values == null)
			return true;
		final FastCharIterator it = (FastCharIterator) values.iterator();
		while(it.hasNext()) {
			if(!contains(it.next()))
				return false;
		}
		return true;
	}
	public boolean removeAll(char[] values) {
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
	public boolean removeAll(FastCharSet values) {
		if(values == null || values.size() == 0 || size == 0)
			return false;
		boolean modified = false;
		final FastCharIterator it = (FastCharIterator) values.iterator();
		while(it.hasNext()) {
			if(remove(it.next())) {
				modified = true;
			}
		}
		return modified;
	}
	public boolean retainAll(FastCharSet values) {
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
	public char[] toArray() {
		final char[] result = new char[size];
		int count = 0;
		for(int i = 0; i < _table.length; ++i) {
			if(_state[i] == OCCUPIED) {
				result[count++] = _table[i];
			}
		}
		return result;
	}
	public FastPrimitiveIterator/*FastCharIterator*/ iterator() {
		return new SetIterator(this);
	}
	public FastPrimitiveCollection/*FastCharSet*/ unmodifiable() {
		return new Unmodifiable(this);
	}
	public FastPrimitiveCollection/*FastCharSet*/ shared() {
		return new Shared(this);
	}
	public Object/*FastCharSet*/ clone() throws CloneNotSupportedException {
		/*@JVM-1.1+@
		if(true) {
			final FastCharSet c = (FastCharSet) super.clone();
			c._table = new char[_table.length];
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
				s.writeChar(_table[i]);
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
		_table = new char[cap];
		_state = new byte[cap];
		_mask = cap - 1;
		_threshold = (int) (cap * LOAD_FACTOR);
		size = 0;
		_used = 0;
		for(int i = 0; i < count; ++i) {
			add(s.readChar());
		}
	}
	public boolean equals(Object o) {
		if(this == o)
			return true;
		if(!(o instanceof FastCharSet))
			return false;
		final FastCharSet that = (FastCharSet) o;
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
	private static final class SetIterator implements FastCharIterator {
		private final FastCharSet _set;
		private int _cursor = 0;
		private char _lastValue;
		private boolean _canRemove;
		private SetIterator(FastCharSet set) {
			_set = set;
			advance();
		}
		private final void advance() {
			while(_cursor < _set._table.length
					&& _set._state[_cursor] != OCCUPIED) {
				++_cursor;
			}
		}
		public boolean hasNext() {
			return _cursor < _set._table.length;
		}
		public char next() {
			if(!hasNext())
				throw new NoSuchElementException();
			final char val = _set._table[_cursor++];
			_lastValue = val;
			_canRemove = true;
			advance();
			return val;
		}
		public void remove() {
			if(!_canRemove)
				throw new IllegalStateException();
			_set.remove(_lastValue);
			_canRemove = false;
		}
	}
	private static final class Unmodifiable extends FastCharSet
			implements Cloneable, Reusable, Serializable {
		private final FastCharSet _set;
		private Unmodifiable(FastCharSet set) {
			super(0);
			_set = set;
		}
		public final FastPrimitiveCollection/*FastCharSet*/ unmodifiable() {
			return this;
		}
		public final FastPrimitiveCollection/*FastCharSet*/ shared() {
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
		public final boolean contains(char value) {
			return _set.contains(value);
		}
		public final Object/*FastCharSet*/ clone()
				throws CloneNotSupportedException {
			return _set.clone();
		}
		public final char[] toArray() {
			return _set.toArray();
		}
		public final boolean add(char value) {
			throw new UnsupportedOperationException("Unmodifiable");
		}
		public final boolean remove(char value) {
			throw new UnsupportedOperationException("Unmodifiable");
		}
		public final boolean removeElement(char value) {
			throw new UnsupportedOperationException("Unmodifiable");
		}
		public final void clear() {
			throw new UnsupportedOperationException("Unmodifiable");
		}
		public final boolean addAll(char[] values) {
			throw new UnsupportedOperationException("Unmodifiable");
		}
		public final boolean addAll(FastCharSet values) {
			throw new UnsupportedOperationException("Unmodifiable");
		}
		public final boolean containsAll(char[] values) {
			return _set.containsAll(values);
		}
		public final boolean containsAll(FastCharSet values) {
			return _set.containsAll(values);
		}
		public final boolean removeAll(char[] values) {
			throw new UnsupportedOperationException("Unmodifiable");
		}
		public final boolean removeAll(FastCharSet values) {
			throw new UnsupportedOperationException("Unmodifiable");
		}
		public final boolean retainAll(FastCharSet values) {
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
		public final FastPrimitiveIterator/*FastCharIterator*/ iterator() {
			return new FastCharIterator() {
				private final FastCharIterator _it = (FastCharIterator) _set
						.iterator();
				public final boolean hasNext() {
					return _it.hasNext();
				}
				public final char next() {
					return _it.next();
				}
				public final void remove() {
					throw new UnsupportedOperationException("Unmodifiable");
				}
			};
		}
	}
	private static final class Shared extends FastCharSet
			implements Cloneable, Reusable, Serializable {
		private final FastCharSet _set;
		private final ReadWriteLock _lock;
		private Shared(FastCharSet set) {
			super(0);
			_set = set;
			_lock = new ReentrantWriterPreferenceReadWriteLock();
		}
		public final FastPrimitiveCollection/*FastCharSet*/ unmodifiable() {
			return _set.unmodifiable();
		}
		public final FastPrimitiveCollection/*FastCharSet*/ shared() {
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
		public final boolean contains(char value) {
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
		public final Object/*FastCharSet*/ clone()
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
		public final char[] toArray() {
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
		public final boolean add(char value) {
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
		public final boolean remove(char value) {
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
		public final boolean removeElement(char value) {
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
		public final boolean addAll(char[] values) {
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
		public final boolean addAll(FastCharSet values) {
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
		public final boolean containsAll(char[] values) {
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
		public final boolean containsAll(FastCharSet values) {
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
		public final boolean removeAll(char[] values) {
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
		public final boolean removeAll(FastCharSet values) {
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
		public final boolean retainAll(FastCharSet values) {
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
		public final FastPrimitiveIterator/*FastCharIterator*/ iterator() {
			final FastCharIterator iterator = (FastCharIterator) _set
					.iterator();
			return new FastCharIterator() {
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
				public final char next() {
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
