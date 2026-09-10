/*
 * File: QueuedSemaphore.java
 *
 * Originally written by Doug Lea and released into the public domain.
 * This may be used for any purposes whatsoever without acknowledgment.
 * Thanks for the assistance and support of Sun Microsystems Labs,
 * and everyone contributing, testing, and using this code.
 *
 * History:
 * Date         Who            What
 * 11Jun1998    dl             Create public version
 * 5Aug1998     dl             replaced int counters with longs
 * 24Aug1999    dl             release(n): screen arguments
 */
package _templates.javolution.util.concurrent;
/**
 * Abstract base class for semaphores relying on queued wait nodes.
 * <p>[ <a href="//gee.cs.oswego.edu/dl/classes/EDU/oswego/cs/dl/util/concurrent/intro.html"> Introduction to this package.</a> ]</p>
 */
public abstract class QueuedSemaphore extends Semaphore {
	private final WaitQueue _wq;
	QueuedSemaphore(WaitQueue q, long initialPermits) {
		super(initialPermits);
		_wq = q;
	}
	public void acquire() throws InterruptedException {
		/*@JVM-1.1+@
		if(Thread.interrupted())
			throw new InterruptedException();
		/**/
		if(precheck())
			return;
		final WaitQueue.WaitNode w = new WaitQueue.WaitNode();
		w.doWait(this);
	}
	public boolean attempt(long msecs) throws InterruptedException {
		/*@JVM-1.1+@
		if(Thread.interrupted())
			throw new InterruptedException();
		/**/
		if(precheck())
			return true;
		if(msecs <= 0)
			return false;
		final WaitQueue.WaitNode w = new WaitQueue.WaitNode();
		return w.doTimedWait(this, msecs);
	}
	private synchronized boolean precheck() {
		final boolean pass = _permits > 0;
		if(pass) {
			--_permits;
		}
		return pass;
	}
	private synchronized boolean recheck(WaitQueue.WaitNode w) {
		final boolean pass = _permits > 0;
		if(pass) {
			--_permits;
		}
		else {
			_wq.insert(w);
		}
		return pass;
	}
	private synchronized WaitQueue.WaitNode getSignallee() {
		final WaitQueue.WaitNode w = _wq.extract();
		if(w == null) {
			++_permits; // if none, inc permits for new arrivals
		}
		return w;
	}
	public void release() {
		for(;;) {
			final WaitQueue.WaitNode w = getSignallee();
			if(w == null)
				return; // no one to signal
			if(w.signal())
				return; // notify if still waiting, else skip
		}
	}
	/** Release N permits */
	public void release(long n) {
		if(n < 0)
			throw new IllegalArgumentException("Negative argument");
		for(long i = 0; i < n; ++i) {
			release();
		}
	}
	/**
	 * Base class for internal queue classes for semaphores, etc.
	 * Relies on subclasses to actually implement queue mechanics
	 */
	static abstract class WaitQueue {
		abstract void insert(WaitNode w);// assumed not to block
		abstract WaitNode extract(); // should return null if empty
		static final class WaitNode {
			boolean waiting = true;
			WaitNode next = null;
			synchronized boolean signal() {
				final boolean signalled = waiting;
				if(signalled) {
					waiting = false;
					notify();
				}
				return signalled;
			}
			synchronized boolean doTimedWait(QueuedSemaphore sem, long msecs)
					throws InterruptedException {
				if(sem.recheck(this) || !waiting)
					return true;
				else if(msecs <= 0) {
					waiting = false;
					return false;
				}
				else {
					long waitTime = msecs;
					final long start = System.currentTimeMillis();
					try {
						for(;;) {
							wait(waitTime);
							if(!waiting) // definitely signalled
								return true;
							waitTime = msecs
									- (System.currentTimeMillis() - start);
							if(waitTime <= 0) { //  timed out
								waiting = false;
								return false;
							}
						}
					}
					catch(final InterruptedException ex) {
						if(waiting) { // no notification
							waiting = false; // invalidate for the signaller
							throw ex;
						}
						// thread was interrupted after it was notified
						Thread.currentThread().interrupt();
						return true;
					}
				}
			}
			synchronized void doWait(QueuedSemaphore sem)
					throws InterruptedException {
				if(!sem.recheck(this)) {
					try {
						while(waiting) {
							wait();
						}
					}
					catch(final InterruptedException ex) {
						if(waiting) { // no notification
							waiting = false; // invalidate for the signaller
							throw ex;
						}
						// thread was interrupted after it was notified
						Thread.currentThread().interrupt();
						return;
					}
				}
			}
		}
	}
}