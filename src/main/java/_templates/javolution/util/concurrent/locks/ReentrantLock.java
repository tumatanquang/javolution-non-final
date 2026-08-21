/*
 * File: ReentrantLock.java
 *
 * Originally written by Doug Lea and released into the public domain.
 * This may be used for any purposes whatsoever without acknowledgment.
 * Thanks for the assistance and support of Sun Microsystems Labs,
 * and everyone contributing, testing, and using this code.
 *
 * History:
 * Date         Who            What
 * 11Jun1998    dl             Create public version
 *  5Aug1998    dl             Replaced int counters with longs
 */
package _templates.javolution.util.concurrent.locks;
/**
 * A lock with the same semantics as builtin
 * Java synchronized locks: Once a thread has a lock, it
 * can re-obtain it any number of times without blocking.
 * The lock is made available to other threads when
 * as many releases as acquires have occurred.
 * <p>[ <a href="//gee.cs.oswego.edu/dl/classes/EDU/oswego/cs/dl/util/concurrent/intro.html"> Introduction to this package.</a> ]</p>
 */
public class ReentrantLock implements Sync {
	protected Thread _owner;
	protected long _holds;
	public void acquire() throws InterruptedException {
		/*@JVM-1.1+@
		if(Thread.interrupted())
			throw new InterruptedException();
		/**/
		final Thread caller = Thread.currentThread();
		synchronized(this) {
			if(caller == _owner) {
				++_holds;
			}
			else {
				try {
					while(_owner != null) {
						wait();
					}
					_owner = caller;
					_holds = 1;
				}
				catch(final InterruptedException ex) {
					notify();
					throw ex;
				}
			}
		}
	}
	public boolean attempt(long msecs) throws InterruptedException {
		/*@JVM-1.1+@
		if(Thread.interrupted())
			throw new InterruptedException();
		/**/
		final Thread caller = Thread.currentThread();
		synchronized(this) {
			if(caller == _owner) {
				++_holds;
				return true;
			}
			else if(_owner == null) {
				_owner = caller;
				_holds = 1;
				return true;
			}
			else if(msecs <= 0)
				return false;
			else {
				long waitTime = msecs;
				final long start = System.currentTimeMillis();
				try {
					for(;;) {
						wait(waitTime);
						if(caller == _owner) {
							++_holds;
							return true;
						}
						else if(_owner == null) {
							_owner = caller;
							_holds = 1;
							return true;
						}
						else {
							waitTime = msecs
									- (System.currentTimeMillis() - start);
							if(waitTime <= 0)
								return false;
						}
					}
				}
				catch(final InterruptedException ex) {
					notify();
					throw ex;
				}
			}
		}
	}
	/**
	 * Release the lock.
	 * @throws Error thrown if not current owner of lock
	 */
	public synchronized void release() {
		if(Thread.currentThread() != _owner)
			throw new Error("Illegal Lock usage");
		if(--_holds == 0) {
			_owner = null;
			notify();
		}
	}
	/**
	 * Release the lock N times. <code>release(n)</code> is
	 * equivalent in effect to:
	 * <pre>
	 *   for(int i = 0; i < n; ++i) release();
	 * </pre>
	 * <p>
	 * @throws Error thrown if not current owner of lock
	 * or has fewer than N holds on the lock
	 */
	public synchronized void release(long n) {
		if(Thread.currentThread() != _owner || n > _holds)
			throw new Error("Illegal Lock usage");
		_holds -= n;
		if(_holds == 0) {
			_owner = null;
			notify();
		}
	}
	/**
	 * Return the number of unreleased acquires performed
	 * by the current thread.
	 * Returns zero if current thread does not hold lock.
	 */
	public synchronized long holds() {
		if(Thread.currentThread() != _owner)
			return 0;
		return _holds;
	}
}