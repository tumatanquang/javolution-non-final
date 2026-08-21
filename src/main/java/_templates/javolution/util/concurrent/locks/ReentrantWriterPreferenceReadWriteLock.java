/*
 * File: ReentrantWriterPreferenceReadWriteLock.java
 *
 * Originally written by Doug Lea and released into the public domain.
 * This may be used for any purposes whatsoever without acknowledgment.
 * Thanks for the assistance and support of Sun Microsystems Labs,
 * and everyone contributing, testing, and using this code.
 *
 * History:
 * Date         Who            What
 * 26aug1998    dl             Create public version
 *  7sep2000    dl             Readers are now also reentrant
 * 19jan2001    dl             Allow read->write upgrades if the only reader
 * 10dec2002    dl             Throw IllegalStateException on extra release
 */
package _templates.javolution.util.concurrent.locks;
import _templates.java.lang.IllegalStateException;
import _templates.java.util.HashMap;
/**
 * A writer-preference ReadWriteLock that allows both readers and
 * writers to reacquire
 * read or write locks in the style of a ReentrantLock.
 * Readers are not allowed until all write locks held by
 * the writing thread have been released.
 * Among other applications, reentrancy can be useful when
 * write locks are held during calls or callbacks to methods that perform
 * reads under read locks.
 * <p>
 * <b>Sample usage</b>. Here is a code sketch showing how to exploit
 * reentrancy to perform lock downgrading after updating a cache:
 * <pre>
 * class CachedData {
 *   Object data;
 *   volatile boolean cacheValid;
 *   ReentrantWriterPreferenceReadWriteLock rwl = ...
 *
 *   void processCachedData() {
 *     rwl.readLock().acquire();
 *     if(!cacheValid) {
 *
 *        // upgrade lock:
 *        rwl.readLock().release();   // must release first to obtain writelock
 *        rwl.writeLock().acquire();
 *        if(!cacheValid) { // recheck
 *          data = ...
 *          cacheValid = true;
 *        }
 *        // downgrade lock
 *        rwl.readLock().acquire();  // reacquire read without giving up lock
 *        rwl.writeLock().release(); // release write, still hold read
 *     }
 *
 *     use(data);
 *     rwl.readLock().release();
 *   }
 * }
 * </pre>
 *
 *
 * <p>[ <a href="//gee.cs.oswego.edu/dl/classes/EDU/oswego/cs/dl/util/concurrent/intro.html"> Introduction to this package.</a> ]</p>
 * @see ReentrantLock
 */
public class ReentrantWriterPreferenceReadWriteLock
		extends WriterPreferenceReadWriteLock {
	/**
	 * cache/reuse the special Integer value one to speed up readlocks
	 */
	private static final Integer IONE = new Integer(1);
	/**
	 * Number of acquires on read lock by any reader thread
	 */
	private final HashMap _readers = new HashMap();
	/**
	 * Number of acquires on write lock by _activeWriter thread
	 */
	private long _writeHolds;
	boolean allowReader() {
		return _activeWriter == null && _waitingWriters == 0
				|| _activeWriter == Thread.currentThread();
	}
	synchronized boolean startRead() {
		final Thread t = Thread.currentThread();
		final Object c = _readers.get(t);
		if(c != null) { // already held -- just increment hold count
			_readers.put(t, new Integer(((Integer) c).intValue() + 1));
			++_activeReaders;
			return true;
		}
		else if(allowReader()) {
			_readers.put(t, IONE);
			++_activeReaders;
			return true;
		}
		else
			return false;
	}
	synchronized boolean startWrite() {
		if(_activeWriter == Thread.currentThread()) { // already held; re-acquire
			++_writeHolds;
			return true;
		}
		else if(_writeHolds == 0) {
			if(_activeReaders == 0 || _readers.size() == 1
					&& _readers.get(Thread.currentThread()) != null) {
				_activeWriter = Thread.currentThread();
				_writeHolds = 1;
				return true;
			}
			return false;
		}
		else
			return false;
	}
	synchronized Signaller endRead() {
		final Thread t = Thread.currentThread();
		final Object c = _readers.get(t);
		if(c == null)
			throw new IllegalStateException();
		--_activeReaders;
		if(c != IONE) { // more than one hold; decrement count
			final int h = ((Integer) c).intValue() - 1;
			final Integer ih = h == 1 ? IONE : new Integer(h);
			_readers.put(t, ih);
			return null;
		}
		_readers.remove(t);
		if(_writeHolds > 0)
			return null;
		else if(_activeReaders == 0 && _waitingWriters > 0)
			return _writerLock;
		else
			return null;
	}
	synchronized Signaller endWrite() {
		--_writeHolds;
		if(_writeHolds > 0)
			return null;
		_activeWriter = null;
		if(_waitingReaders > 0 && allowReader())
			return _readerLock;
		else if(_waitingWriters > 0)
			return _writerLock;
		else
			return null;
	}
}