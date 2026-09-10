/*
 * File: SyncList.java
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
import _templates.java.util.Iterator;
import _templates.java.util.List;
import _templates.java.util.ListIterator;
import _templates.javolution.util.concurrent.locks.ReadWriteLock;
import _templates.javolution.util.concurrent.locks.Sync;
/**
 * SyncLists wrap Sync-based control around java.util.Lists.
 * They support the following additional reader operations over
 * SyncCollection: hashCode, equals, get, indexOf, lastIndexOf,
 * subList. They support additional writer operations remove(int),
 * set(int), add(int), addAll(int). The corresponding listIterators
 * and are similarly extended.
 * <p>[ <a href="//gee.cs.oswego.edu/dl/classes/EDU/oswego/cs/dl/util/concurrent/intro.html"> Introduction to this package.</a> ]</p>
 * @see SyncCollection
 */
public class SyncList extends SyncCollection implements List {
	/**
	 * Create a new SyncList protecting the given collection,
	 * and using the given sync to control both reader and writer methods.
	 * Common, reasonable choices for the sync argument include
	 * Mutex, ReentrantLock, and Semaphores initialized to 1.
	 */
	public SyncList(List list, Sync sync) {
		super(list, sync);
	}
	/**
	 * Create a new SyncList protecting the given list,
	 * and using the given ReadWriteLock to control reader and writer methods.
	 */
	public SyncList(List list, ReadWriteLock rwl) {
		super(list, rwl.readLock(), rwl.writeLock());
	}
	/**
	 * Create a new SyncList protecting the given list,
	 * and using the given pair of locks to control reader and writer methods.
	 */
	public SyncList(List list, Sync readLock, Sync writeLock) {
		super(list, readLock, writeLock);
	}
	List baseList() {
		return (List) _c;
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
	public Object get(int index) {
		final boolean wasInterrupted = beforeRead();
		try {
			return baseList().get(index);
		}
		finally {
			afterRead(wasInterrupted);
		}
	}
	public int indexOf(Object o) {
		final boolean wasInterrupted = beforeRead();
		try {
			return baseList().indexOf(o);
		}
		finally {
			afterRead(wasInterrupted);
		}
	}
	public int lastIndexOf(Object o) {
		final boolean wasInterrupted = beforeRead();
		try {
			return baseList().lastIndexOf(o);
		}
		finally {
			afterRead(wasInterrupted);
		}
	}
	public List subList(int fromIndex, int toIndex) {
		final boolean wasInterrupted = beforeRead();
		try {
			return new SyncList(baseList().subList(fromIndex, toIndex), _rd,
					_wr);
		}
		finally {
			afterRead(wasInterrupted);
		}
	}
	public Object set(int index, Object o) {
		try {
			_wr.acquire();
			try {
				return baseList().set(index, o);
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
	public Object remove(int index) {
		try {
			_wr.acquire();
			try {
				return baseList().remove(index);
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
	public void add(int index, Object o) {
		try {
			_wr.acquire();
			try {
				baseList().add(index, o);
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
	public boolean addAll(int index, Collection coll) {
		try {
			_wr.acquire();
			try {
				return baseList().addAll(index, coll);
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
	public ListIterator unprotectedListIterator() {
		final boolean wasInterrupted = beforeRead();
		try {
			return baseList().listIterator();
		}
		finally {
			afterRead(wasInterrupted);
		}
	}
	public ListIterator listIterator() {
		final boolean wasInterrupted = beforeRead();
		try {
			return new SyncCollectionListIterator(baseList().listIterator());
		}
		finally {
			afterRead(wasInterrupted);
		}
	}
	public ListIterator unprotectedListIterator(int index) {
		final boolean wasInterrupted = beforeRead();
		try {
			return baseList().listIterator(index);
		}
		finally {
			afterRead(wasInterrupted);
		}
	}
	public ListIterator listIterator(int index) {
		final boolean wasInterrupted = beforeRead();
		try {
			return new SyncCollectionListIterator(
					baseList().listIterator(index));
		}
		finally {
			afterRead(wasInterrupted);
		}
	}
	public class SyncCollectionListIterator extends SyncCollectionIterator
			implements ListIterator {
		SyncCollectionListIterator(Iterator baseIterator) {
			super(baseIterator);
		}
		ListIterator baseListIterator() {
			return (ListIterator) _baseIterator;
		}
		public boolean hasPrevious() {
			final boolean wasInterrupted = beforeRead();
			try {
				return baseListIterator().hasPrevious();
			}
			finally {
				afterRead(wasInterrupted);
			}
		}
		public Object previous() {
			final boolean wasInterrupted = beforeRead();
			try {
				return baseListIterator().previous();
			}
			finally {
				afterRead(wasInterrupted);
			}
		}
		public int nextIndex() {
			final boolean wasInterrupted = beforeRead();
			try {
				return baseListIterator().nextIndex();
			}
			finally {
				afterRead(wasInterrupted);
			}
		}
		public int previousIndex() {
			final boolean wasInterrupted = beforeRead();
			try {
				return baseListIterator().previousIndex();
			}
			finally {
				afterRead(wasInterrupted);
			}
		}
		public void set(Object o) {
			try {
				_wr.acquire();
				try {
					baseListIterator().set(o);
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
		public void add(Object o) {
			try {
				_wr.acquire();
				try {
					baseListIterator().add(o);
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
	}
}