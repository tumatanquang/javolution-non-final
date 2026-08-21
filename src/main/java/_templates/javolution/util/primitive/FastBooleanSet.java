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
 * <p> This class represents a high-performance set of <code>boolean</code>
 *     primitive values.</p>
 *
 * <p> Because a boolean set contains at most two distinct elements (<code>false</code> and
 *     <code>true</code>), this class provides instant $\mathcal{O}(1)$ deterministic operations
 *     with zero memory allocation.</p>
 *
 * @author <a href="mailto:jean-marie@dautelle.com">Jean-Marie Dautelle</a>
 * @version 6.1.0, August 22, 2026
 */
public class FastBooleanSet extends FastPrimitiveCollection
		implements Cloneable, Reusable, Serializable {
	/**
	 * Indicates if this set contains the <code>false</code> element.
	 */
	private transient boolean _containsFalse;
	/**
	 * Indicates if this set contains the <code>true</code> element.
	 */
	private transient boolean _containsTrue;
	/**
	 * Constructs an empty boolean set.
	 */
	public FastBooleanSet() {}
	/**
	 * Constructs a boolean set containing the elements of the specified array.
	 *
	 * @param values the array of initial values.
	 * @throws NullPointerException if <code>values</code> is <code>null</code>
	 */
	public FastBooleanSet(boolean[] values) {
		if(values == null)
			throw new NullPointerException("values is null");
		addAll(values);
	}
	/**
	 * Constructs a boolean set containing the elements of the specified set.
	 *
	 * @param values the set of initial values.
	 * @throws NullPointerException if <code>values</code> is <code>null</code>
	 */
	public FastBooleanSet(FastBooleanSet values) {
		if(values == null)
			throw new NullPointerException("values is null");
		addAll(values);
	}
	/**
	 * Adds the specified value to this set if it is not already present.
	 *
	 * @param value the value to be added.
	 * @return <code>true</code> if this set did not already contain the specified value.
	 */
	public boolean add(boolean value) {
		if(value) {
			if(_containsTrue)
				return false;
			_containsTrue = true;
			++size;
			return true;
		}
		if(_containsFalse)
			return false;
		_containsFalse = true;
		++size;
		return true;
	}
	/**
	 * Returns <code>true</code> if this set contains the specified value.
	 *
	 * @param value the value whose presence in this set is to be tested.
	 * @return <code>true</code> if this set contains the specified value.
	 */
	public boolean contains(boolean value) {
		return value ? _containsTrue : _containsFalse;
	}
	/**
	 * Removes the specified value from this set if it is present.
	 *
	 * @param value the value to be removed from this set, if present.
	 * @return <code>true</code> if this set contained the specified value.
	 */
	public boolean remove(boolean value) {
		if(value) {
			if(!_containsTrue)
				return false;
			_containsTrue = false;
			--size;
			return true;
		}
		if(!_containsFalse)
			return false;
		_containsFalse = false;
		--size;
		return true;
	}
	/**
	 * Removes the specified value from this set if it is present.
	 *
	 * @param value the value to be removed from this set, if present.
	 * @return <code>true</code> if this set contained the specified value.
	 */
	public boolean removeElement(boolean value) {
		return remove(value);
	}
	/**
	 * {@inheritDoc}
	 */
	public void clear() {
		_containsFalse = false;
		_containsTrue = false;
		size = 0;
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
	public void trimToSize() {}
	/**
	 * {@inheritDoc}
	 */
	public void ensureCapacity(int minCapacity) {}
	/**
	 * Adds all elements from the specified array to this set.
	 *
	 * @param values array containing elements to be added.
	 * @return <code>true</code> if this set changed as a result of the call.
	 */
	public boolean addAll(boolean[] values) {
		if(values == null || values.length == 0)
			return false;
		boolean modified = false;
		final int length = values.length;
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
	public boolean addAll(FastBooleanSet values) {
		if(values == null || values.size() == 0)
			return false;
		boolean modified = false;
		if(values.contains(false) && add(false)) {
			modified = true;
		}
		if(values.contains(true) && add(true)) {
			modified = true;
		}
		return modified;
	}
	/**
	 * Returns <code>true</code> if this set contains all elements of the specified array.
	 *
	 * @param values array of elements to be checked for containment in this set.
	 * @return <code>true</code> if this set contains all elements of the specified array.
	 */
	public boolean containsAll(boolean[] values) {
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
	public boolean containsAll(FastBooleanSet values) {
		if(values == null)
			return true;
		if(values.contains(false) && !_containsFalse)
			return false;
		if(values.contains(true) && !_containsTrue)
			return false;
		return true;
	}
	/**
	 * Removes from this set all of its elements that are contained in the specified array.
	 *
	 * @param values array containing elements to be removed from this set.
	 * @return <code>true</code> if this set changed as a result of the call.
	 */
	public boolean removeAll(boolean[] values) {
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
	public boolean removeAll(FastBooleanSet values) {
		if(values == null || values.size() == 0 || size == 0)
			return false;
		boolean modified = false;
		if(values.contains(false) && remove(false)) {
			modified = true;
		}
		if(values.contains(true) && remove(true)) {
			modified = true;
		}
		return modified;
	}
	/**
	 * Retains only the elements in this set that are contained in the specified set.
	 *
	 * @param values set containing elements to be retained in this set.
	 * @return <code>true</code> if this set changed as a result of the call.
	 */
	public boolean retainAll(FastBooleanSet values) {
		if(values == null || size == 0)
			return false;
		boolean modified = false;
		if(_containsFalse && !values.contains(false)) {
			_containsFalse = false;
			--size;
			modified = true;
		}
		if(_containsTrue && !values.contains(true)) {
			_containsTrue = false;
			--size;
			modified = true;
		}
		return modified;
	}
	/**
	 * Returns an array containing all of the elements in this set.
	 *
	 * @return an array containing all of the elements in this set.
	 */
	public boolean[] toArray() {
		final boolean[] result = new boolean[size];
		int count = 0;
		if(_containsFalse) {
			result[count++] = false;
		}
		if(_containsTrue) {
			result[count++] = true;
		}
		return result;
	}
	/**
	 * {@inheritDoc}
	 */
	public FastPrimitiveIterator/*FastBooleanIterator*/ iterator() {
		return new SetIterator(this);
	}
	/**
	 * {@inheritDoc}
	 */
	public FastPrimitiveCollection/*FastBooleanSet*/ unmodifiable() {
		return new Unmodifiable(this);
	}
	/**
	 * {@inheritDoc}
	 */
	public FastPrimitiveCollection/*FastBooleanSet*/ shared() {
		return new Shared(this);
	}
	/**
	 * Returns a copy of this set.
	 *
	 * @return a clone of this instance.
	 * @throws CloneNotSupportedException if cloning is not supported.
	 */
	public Object/*FastBooleanSet*/ clone() throws CloneNotSupportedException {
		/*@JVM-1.1+@
		if(true) {
			final FastBooleanSet c = (FastBooleanSet) super.clone();
			c._containsFalse = _containsFalse;
			c._containsTrue = _containsTrue;
			c.size = size;
			return c;
		}
		/**/
		throw new UnsupportedOperationException("J2ME Not Supported Yet");
	}
	private final void writeObject(ObjectOutputStream s) throws IOException {
		s.defaultWriteObject();
		s.writeInt(size);
		if(_containsFalse) {
			s.writeBoolean(false);
		}
		if(_containsTrue) {
			s.writeBoolean(true);
		}
	}
	private final void readObject(ObjectInputStream s)
			throws IOException, ClassNotFoundException {
		s.defaultReadObject();
		final int count = s.readInt();
		_containsFalse = false;
		_containsTrue = false;
		size = 0;
		for(int i = -1; ++i < count;) {
			add(s.readBoolean());
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
		if(!(o instanceof FastBooleanSet))
			return false;
		final FastBooleanSet that = (FastBooleanSet) o;
		return contains(false) == that.contains(false)
				&& contains(true) == that.contains(true);
	}
	/**
	 * Returns the hash code value for this set.
	 *
	 * @return the hash code value for this set.
	 */
	public int hashCode() {
		int h = 0;
		if(_containsFalse) {
			h += 1237;
		}
		if(_containsTrue) {
			h += 1231;
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
		if(size == 1)
			return _containsFalse ? "[false]" : "[true]";
		return "[false, true]";
	}
	private static final class SetIterator implements FastBooleanIterator {
		private final FastBooleanSet _set;
		private int _state; // 0 = start, 1 = after false, 2 = after true, 3 = end
		private int _last = -1; // 0 = false, 1 = true
		private SetIterator(FastBooleanSet set) {
			_set = set;
			_state = _set._containsFalse ? 0 : _set._containsTrue ? 1 : 2;
		}
		public final boolean hasNext() {
			if(_state == 0)
				return _set._containsFalse;
			if(_state == 1)
				return _set._containsTrue;
			return false;
		}
		public final boolean next() {
			if(!hasNext())
				throw new NoSuchElementException();
			if(_state == 0) {
				_last = 0;
				_state = _set._containsTrue ? 1 : 2;
				return false;
			}
			_last = 1;
			_state = 2;
			return true;
		}
		public final void remove() {
			if(_last < 0)
				throw new IllegalStateException();
			_set.remove(_last == 1);
			_last = -1;
		}
	}
	private static final class Unmodifiable extends FastBooleanSet
			implements Cloneable, Reusable, Serializable {
		private final FastBooleanSet _set;
		private Unmodifiable(FastBooleanSet set) {
			super();
			_set = set;
		}
		public final FastPrimitiveCollection/*FastBooleanSet*/ unmodifiable() {
			return this;
		}
		public final FastPrimitiveCollection/*FastBooleanSet*/ shared() {
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
		public final boolean contains(boolean value) {
			return _set.contains(value);
		}
		public final Object/*FastBooleanSet*/ clone()
				throws CloneNotSupportedException {
			return _set.clone();
		}
		public final boolean[] toArray() {
			return _set.toArray();
		}
		public final boolean add(boolean value) {
			throw new UnsupportedOperationException("Unmodifiable");
		}
		public final boolean remove(boolean value) {
			throw new UnsupportedOperationException("Unmodifiable");
		}
		public final boolean removeElement(boolean value) {
			throw new UnsupportedOperationException("Unmodifiable");
		}
		public final void clear() {
			throw new UnsupportedOperationException("Unmodifiable");
		}
		public final boolean addAll(boolean[] values) {
			throw new UnsupportedOperationException("Unmodifiable");
		}
		public final boolean addAll(FastBooleanSet values) {
			throw new UnsupportedOperationException("Unmodifiable");
		}
		public final boolean containsAll(boolean[] values) {
			return _set.containsAll(values);
		}
		public final boolean containsAll(FastBooleanSet values) {
			return _set.containsAll(values);
		}
		public final boolean removeAll(boolean[] values) {
			throw new UnsupportedOperationException("Unmodifiable");
		}
		public final boolean removeAll(FastBooleanSet values) {
			throw new UnsupportedOperationException("Unmodifiable");
		}
		public final boolean retainAll(FastBooleanSet values) {
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
		public final FastPrimitiveIterator/*FastBooleanIterator*/ iterator() {
			final FastBooleanIterator iterator = (FastBooleanIterator) _set
					.iterator();
			return new FastBooleanIterator() {
				public final boolean hasNext() {
					return iterator.hasNext();
				}
				public final boolean next() {
					return iterator.next();
				}
				public final void remove() {
					throw new UnsupportedOperationException("Unmodifiable");
				}
			};
		}
	}
	private static final class Shared extends FastBooleanSet
			implements Cloneable, Reusable, Serializable {
		private final FastBooleanSet _set;
		private final ReadWriteLock _lock;
		private Shared(FastBooleanSet set) {
			super();
			_set = set;
			_lock = new ReentrantWriterPreferenceReadWriteLock();
		}
		public final FastPrimitiveCollection/*FastBooleanSet*/ unmodifiable() {
			return _set.unmodifiable();
		}
		public final FastPrimitiveCollection/*FastBooleanSet*/ shared() {
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
		public final void trimToSize() {}
		public final void ensureCapacity(int min) {}
		public final boolean contains(boolean value) {
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
		public final Object/*FastBooleanSet*/ clone()
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
		public final boolean[] toArray() {
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
		public final boolean add(boolean value) {
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
		public final boolean remove(boolean value) {
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
		public final boolean removeElement(boolean value) {
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
		public final boolean addAll(boolean[] values) {
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
		public final boolean addAll(FastBooleanSet values) {
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
		public final boolean containsAll(boolean[] values) {
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
		public final boolean containsAll(FastBooleanSet values) {
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
		public final boolean removeAll(boolean[] values) {
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
		public final boolean removeAll(FastBooleanSet values) {
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
		public final boolean retainAll(FastBooleanSet values) {
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
		public final FastPrimitiveIterator/*FastBooleanIterator*/ iterator() {
			final FastBooleanIterator iterator = (FastBooleanIterator) _set
					.iterator();
			return new FastBooleanIterator() {
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
				public final boolean next() {
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