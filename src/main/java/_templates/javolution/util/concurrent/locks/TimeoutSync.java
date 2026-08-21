/*
 * File: TimeoutSync.java
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
package _templates.javolution.util.concurrent.locks;
import _templates.javolution.util.concurrent.TimeoutException;
/**
 * A TimeoutSync is an adaptor class that transforms all
 * calls to acquire to instead invoke attempt with a predetermined
 * timeout value.
 * <p>
 * <b>Sample Usage</b>. A TimeoutSync can be used to obtain
 * Timeouts for locks used in SyncCollections. For example:
 * <pre>
 * Mutex lock = new Mutex();
 * TimeoutSync timedLock = new TimeoutSync(lock, 1000); // 1 sec timeouts
 * Set set = new SyncSet(new HashSet(), timedlock);
 * try {
 *   set.add("hi");
 * }
 * // SyncSets translate timeouts and other lock failures
 * //   to unsupported operation exceptions,
 * catch(UnsupportedOperationException ex) {
 *    System.out.println("Lock failure");
 * }
 * </pre>
 *
 * <p>[ <a href="//gee.cs.oswego.edu/dl/classes/EDU/oswego/cs/dl/util/concurrent/intro.html"> Introduction to this package.</a> ]</p>
 * @see Sync
 */
public class TimeoutSync implements Sync {
	private final Sync _sync; // the adapted sync
	private final long _timeout; // timeout value
	/**
	 * Create a TimeoutSync using the given Sync object, and
	 * using the given timeout value for all calls to acquire.
	 */
	public TimeoutSync(Sync sync, long timeout) {
		_sync = sync;
		_timeout = timeout;
	}
	public void acquire() throws InterruptedException {
		if(!_sync.attempt(_timeout))
			throw new TimeoutException(_timeout);
	}
	public boolean attempt(long msecs) throws InterruptedException {
		return _sync.attempt(msecs);
	}
	public void release() {
		_sync.release();
	}
}