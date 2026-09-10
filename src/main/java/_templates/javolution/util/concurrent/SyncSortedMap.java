/*
 * File: SyncSortedMap.java
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
import _templates.java.util.Comparator;
import _templates.java.util.SortedMap;
import _templates.javolution.util.concurrent.locks.ReadWriteLock;
import _templates.javolution.util.concurrent.locks.Sync;
/**
 * SyncSortedMaps wrap Sync-based control around java.util.SortedMaps.
 * They support the following additional reader operations over
 * SyncMap: comparator, subMap, headMap, tailMap, firstKey, lastKey.
 * <p>[ <a href="//gee.cs.oswego.edu/dl/classes/EDU/oswego/cs/dl/util/concurrent/intro.html"> Introduction to this package.</a> ]</p>
 * @see SyncCollection
 */
public class SyncSortedMap extends SyncMap implements SortedMap {
	/**
	 * Create a new SyncSortedMap protecting the given map,
	 * and using the given sync to control both reader and writer methods.
	 * Common, reasonable choices for the sync argument include
	 * Mutex, ReentrantLock, and Semaphores initialized to 1.
	 */
	public SyncSortedMap(SortedMap map, Sync sync) {
		this(map, sync, sync);
	}
	/**
	 * Create a new SyncSortedMap protecting the given map,
	 * and using the given ReadWriteLock to control reader and writer methods.
	 */
	public SyncSortedMap(SortedMap map, ReadWriteLock rwl) {
		super(map, rwl.readLock(), rwl.writeLock());
	}
	/**
	 * Create a new SyncSortedMap protecting the given map,
	 * and using the given pair of locks to control reader and writer methods.
	 */
	public SyncSortedMap(SortedMap map, Sync readLock, Sync writeLock) {
		super(map, readLock, writeLock);
	}
	SortedMap baseSortedMap() {
		return (SortedMap) _c;
	}
	public Comparator comparator() {
		final boolean wasInterrupted = beforeRead();
		try {
			return baseSortedMap().comparator();
		}
		finally {
			afterRead(wasInterrupted);
		}
	}
	public Object firstKey() {
		final boolean wasInterrupted = beforeRead();
		try {
			return baseSortedMap().firstKey();
		}
		finally {
			afterRead(wasInterrupted);
		}
	}
	public Object lastKey() {
		final boolean wasInterrupted = beforeRead();
		try {
			return baseSortedMap().lastKey();
		}
		finally {
			afterRead(wasInterrupted);
		}
	}
	public SortedMap subMap(Object fromElement, Object toElement) {
		final boolean wasInterrupted = beforeRead();
		try {
			return new SyncSortedMap(
					baseSortedMap().subMap(fromElement, toElement), _rd, _wr);
		}
		finally {
			afterRead(wasInterrupted);
		}
	}
	public SortedMap headMap(Object toElement) {
		final boolean wasInterrupted = beforeRead();
		try {
			return new SyncSortedMap(baseSortedMap().headMap(toElement), _rd,
					_wr);
		}
		finally {
			afterRead(wasInterrupted);
		}
	}
	public SortedMap tailMap(Object fromElement) {
		final boolean wasInterrupted = beforeRead();
		try {
			return new SyncSortedMap(baseSortedMap().tailMap(fromElement), _rd,
					_wr);
		}
		finally {
			afterRead(wasInterrupted);
		}
	}
}