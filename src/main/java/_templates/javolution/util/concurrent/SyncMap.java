/*
 * File: SyncMap.java
 *
 * Originally written by Doug Lea and released into the public domain.
 * This may be used for any purposes whatsoever without acknowledgment.
 * Thanks for the assistance and support of Sun Microsystems Labs,
 * and everyone contributing, testing, and using this code.
 *
 * History:
 * Date         Who            What
 * 1Aug1998     dl             Create public version
 */
package _templates.javolution.util.concurrent;
import _templates.java.lang.UnsupportedOperationException;
import _templates.java.util.Collection;
import _templates.java.util.Map;
import _templates.java.util.Set;
import _templates.javolution.util.concurrent.atomic.SynchronizedLong;
import _templates.javolution.util.concurrent.locks.ReadWriteLock;
import _templates.javolution.util.concurrent.locks.Sync;
/**
 * SyncMaps wrap Sync-based control around java.util.Maps.
 * They operate in the same way as SyncCollection.
 * <p>
 * Reader operations are
 * <ul>
 *  <li> size
 *  <li> isEmpty
 *  <li> get
 *  <li> containsKey
 *  <li> containsValue
 *  <li> keySet
 *  <li> entrySet
 *  <li> values
 * </ul>
 * Writer operations are:
 * <ul>
 *  <li> put
 *  <li> putAll
 *  <li> remove
 *  <li> clear
 * </ul>
 *
 * <p>[ <a href="//gee.cs.oswego.edu/dl/classes/EDU/oswego/cs/dl/util/concurrent/intro.html"> Introduction to this package.</a> ]</p>
 * @see SyncCollection
 */
public class SyncMap implements Map {
	final Map _c; // Backing Map
	final Sync _rd; //  sync for read-only methods
	final Sync _wr; //  sync for mutative methods
	private final SynchronizedLong _syncFailures = new SynchronizedLong(0);
	/**
	 * Create a new SyncMap protecting the given map,
	 * and using the given sync to control both reader and writer methods.
	 * Common, reasonable choices for the sync argument include
	 * Mutex, ReentrantLock, and Semaphores initialized to 1.
	 */
	public SyncMap(Map map, Sync sync) {
		this(map, sync, sync);
	}
	/**
	 * Create a new SyncMap protecting the given map,
	 * and using the given ReadWriteLock to control reader and writer methods.
	 */
	public SyncMap(Map map, ReadWriteLock rwl) {
		this(map, rwl.readLock(), rwl.writeLock());
	}
	/**
	 * Create a new SyncMap protecting the given map,
	 * and using the given pair of locks to control reader and writer methods.
	 */
	public SyncMap(Map map, Sync readLock, Sync writeLock) {
		_c = map;
		_rd = readLock;
		_wr = writeLock;
	}
	/**
	 * Return the Sync object managing read-only operations
	 */
	public Sync readerSync() {
		return _rd;
	}
	/**
	 * Return the Sync object managing mutative operations
	 */
	public Sync writerSync() {
		return _wr;
	}
	/**
	 * Return the number of synchronization failures for read-only operations
	 */
	public long syncFailures() {
		return _syncFailures.get();
	}
	/**
	 * Try to acquire sync before a reader operation; record failure
	 */
	boolean beforeRead() {
		try {
			_rd.acquire();
			return false;
		}
		catch(final InterruptedException ex) {
			_syncFailures.increment();
			return true;
		}
	}
	/**
	 * Clean up after a reader operation
	 */
	void afterRead(boolean wasInterrupted) {
		if(wasInterrupted) {
			Thread.currentThread().interrupt();
		}
		else {
			_rd.release();
		}
	}
	public int hashCode() {
		final boolean wasInterrupted = beforeRead();
		try {
			return _c.hashCode();
		}
		finally {
			afterRead(wasInterrupted);
		}
	}
	public boolean equals(Object o) {
		final boolean wasInterrupted = beforeRead();
		try {
			return _c.equals(o);
		}
		finally {
			afterRead(wasInterrupted);
		}
	}
	public int size() {
		final boolean wasInterrupted = beforeRead();
		try {
			return _c.size();
		}
		finally {
			afterRead(wasInterrupted);
		}
	}
	public boolean isEmpty() {
		final boolean wasInterrupted = beforeRead();
		try {
			return _c.isEmpty();
		}
		finally {
			afterRead(wasInterrupted);
		}
	}
	public boolean containsKey(Object o) {
		final boolean wasInterrupted = beforeRead();
		try {
			return _c.containsKey(o);
		}
		finally {
			afterRead(wasInterrupted);
		}
	}
	public boolean containsValue(Object o) {
		final boolean wasInterrupted = beforeRead();
		try {
			return _c.containsValue(o);
		}
		finally {
			afterRead(wasInterrupted);
		}
	}
	public Object get(Object key) {
		final boolean wasInterrupted = beforeRead();
		try {
			return _c.get(key);
		}
		finally {
			afterRead(wasInterrupted);
		}
	}
	public Object put(Object key, Object value) {
		try {
			_wr.acquire();
			try {
				return _c.put(key, value);
			}
			finally {
				_wr.release();
			}
		}
		catch(final InterruptedException ex) {
			Thread.currentThread().interrupt();
			throw new UnsupportedOperationException();
		}
	}
	public Object remove(Object key) {
		try {
			_wr.acquire();
			try {
				return _c.remove(key);
			}
			finally {
				_wr.release();
			}
		}
		catch(final InterruptedException ex) {
			Thread.currentThread().interrupt();
			throw new UnsupportedOperationException();
		}
	}
	public void putAll(Map coll) {
		try {
			_wr.acquire();
			try {
				_c.putAll(coll);
			}
			finally {
				_wr.release();
			}
		}
		catch(final InterruptedException ex) {
			Thread.currentThread().interrupt();
			throw new UnsupportedOperationException();
		}
	}
	public void clear() {
		try {
			_wr.acquire();
			try {
				_c.clear();
			}
			finally {
				_wr.release();
			}
		}
		catch(final InterruptedException ex) {
			Thread.currentThread().interrupt();
			throw new UnsupportedOperationException();
		}
	}
	private transient Set _keySet = null;
	private transient Set _entrySet = null;
	private transient Collection _values = null;
	public Set keySet() {
		final boolean wasInterrupted = beforeRead();
		try {
			if(_keySet == null) {
				_keySet = new SyncSet(_c.keySet(), _rd, _wr);
			}
			return _keySet;
		}
		finally {
			afterRead(wasInterrupted);
		}
	}
	public Set entrySet() {
		final boolean wasInterrupted = beforeRead();
		try {
			if(_entrySet == null) {
				_entrySet = new SyncSet(_c.entrySet(), _rd, _wr);
			}
			return _entrySet;
		}
		finally {
			afterRead(wasInterrupted);
		}
	}
	public Collection values() {
		final boolean wasInterrupted = beforeRead();
		try {
			if(_values == null) {
				_values = new SyncCollection(_c.values(), _rd, _wr);
			}
			return _values;
		}
		finally {
			afterRead(wasInterrupted);
		}
	}
}