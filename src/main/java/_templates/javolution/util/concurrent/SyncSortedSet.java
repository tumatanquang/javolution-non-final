/*
 * File: SyncSortedSet.java
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
import _templates.java.util.SortedSet;
import _templates.javolution.util.concurrent.locks.ReadWriteLock;
import _templates.javolution.util.concurrent.locks.Sync;
/**
 * SyncSortedSets wrap Sync-based control around java.util.SortedSets.
 * They support the following additional reader operations over
 * SyncCollection: comparator, subSet, headSet, tailSet, first, last.
 * <p>[ <a href="//gee.cs.oswego.edu/dl/classes/EDU/oswego/cs/dl/util/concurrent/intro.html"> Introduction to this package.</a> ]</p>
 * @see SyncCollection
 */
public class SyncSortedSet extends SyncSet implements SortedSet {
	/**
	 * Create a new SyncSortedSet protecting the given collection,
	 * and using the given sync to control both reader and writer methods.
	 * Common, reasonable choices for the sync argument include
	 * Mutex, ReentrantLock, and Semaphores initialized to 1.
	 */
	public SyncSortedSet(SortedSet set, Sync sync) {
		super(set, sync);
	}
	/**
	 * Create a new SyncSortedSet protecting the given set,
	 * and using the given ReadWriteLock to control reader and writer methods.
	 */
	public SyncSortedSet(SortedSet set, ReadWriteLock rwl) {
		super(set, rwl.readLock(), rwl.writeLock());
	}
	/**
	 * Create a new SyncSortedSet protecting the given set,
	 * and using the given pair of locks to control reader and writer methods.
	 */
	public SyncSortedSet(SortedSet set, Sync readLock, Sync writeLock) {
		super(set, readLock, writeLock);
	}
	SortedSet baseSortedSet() {
		return (SortedSet) _c;
	}
	public Comparator comparator() {
		final boolean wasInterrupted = beforeRead();
		try {
			return baseSortedSet().comparator();
		}
		finally {
			afterRead(wasInterrupted);
		}
	}
	public Object first() {
		final boolean wasInterrupted = beforeRead();
		try {
			return baseSortedSet().first();
		}
		finally {
			afterRead(wasInterrupted);
		}
	}
	public Object last() {
		final boolean wasInterrupted = beforeRead();
		try {
			return baseSortedSet().last();
		}
		finally {
			afterRead(wasInterrupted);
		}
	}
	public SortedSet subSet(Object fromElement, Object toElement) {
		final boolean wasInterrupted = beforeRead();
		try {
			return new SyncSortedSet(
					baseSortedSet().subSet(fromElement, toElement), _rd, _wr);
		}
		finally {
			afterRead(wasInterrupted);
		}
	}
	public SortedSet headSet(Object toElement) {
		final boolean wasInterrupted = beforeRead();
		try {
			return new SyncSortedSet(baseSortedSet().headSet(toElement), _rd,
					_wr);
		}
		finally {
			afterRead(wasInterrupted);
		}
	}
	public SortedSet tailSet(Object fromElement) {
		final boolean wasInterrupted = beforeRead();
		try {
			return new SyncSortedSet(baseSortedSet().tailSet(fromElement), _rd,
					_wr);
		}
		finally {
			afterRead(wasInterrupted);
		}
	}
}