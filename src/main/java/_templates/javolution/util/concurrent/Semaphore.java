/*
 * File: Semaphore.java
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
import _templates.javolution.util.concurrent.locks.Sync;
/**
 * Base class for counting semaphores.
 * Conceptually, a semaphore maintains a set of permits.
 * Each acquire() blocks if necessary
 * until a permit is available, and then takes it.
 * Each release adds a permit. However, no actual permit objects
 * are used; the Semaphore just keeps a count of the number
 * available and acts accordingly.
 * <p>
 * A semaphore initialized to 1 can serve as a mutual exclusion
 * lock.
 * <p>
 * Different implementation subclasses may provide different
 * ordering guarantees (or lack thereof) surrounding which
 * threads will be resumed upon a signal.
 * <p>
 * The default implementation makes NO
 * guarantees about the order in which threads will
 * acquire permits. It is often faster than other implementations.
 * <p>
 * <b>Sample usage.</b> Here is a class that uses a semaphore to
 * help manage access to a pool of items.
 * <pre>
 * class Pool {
 *   static final MAX_AVAILABLE = 100;
 *   private final Semaphore available = new Semaphore(MAX_AVAILABLE);
 *
 *   public Object getItem() throws InterruptedException { // no synch
 *     available.acquire();
 *     return getNextAvailableItem();
 *   }
 *
 *   public void putItem(Object x) { // no synch
 *     if(markAsUnused(x))
 *       available.release();
 *   }
 *
 *   // Not a particularly efficient data structure; just for demo
 *
 *   protected Object[] items = ... whatever kinds of items being managed
 *   protected boolean[] used = new boolean[MAX_AVAILABLE];
 *
 *   protected synchronized Object getNextAvailableItem() {
 *     for(int i = 0; i < MAX_AVAILABLE; ++i) {
 *       if(!used[i]) {
 *          used[i] = true;
 *          return items[i];
 *       }
 *     }
 *     return null; // not reached
 *   }
 *
 *   protected synchronized boolean markAsUnused(Object item) {
 *     for(int i = 0; i < MAX_AVAILABLE; ++i) {
 *       if(item == items[i]) {
 *          if(used[i]) {
 *            used[i] = false;
 *            return true;
 *          }
 *          else
 *            return false;
 *       }
 *     }
 *     return false;
 *   }
 *
 * }
 * </pre>
 * <p>[ <a href="//gee.cs.oswego.edu/dl/classes/EDU/oswego/cs/dl/util/concurrent/intro.html"> Introduction to this package.</a> ]</p>
 */
public class Semaphore implements Sync {
	/**
	 * current number of available permits
	 */
	long _permits;
	/**
	 * Create a Semaphore with the given initial number of permits.
	 * Using a seed of one makes the semaphore act as a mutual exclusion lock.
	 * Negative seeds are also allowed, in which case no acquires will proceed
	 * until the number of releases has pushed the number of permits past 0.
	 */
	public Semaphore(long initialPermits) {
		_permits = initialPermits;
	}
	/**
	 * Wait until a permit is available, and take one
	 */
	public void acquire() throws InterruptedException {
		/*@JVM-1.1+@
		if(Thread.interrupted())
			throw new InterruptedException();
		/**/
		synchronized(this) {
			try {
				while(_permits <= 0) {
					wait();
				}
				--_permits;
			}
			catch(final InterruptedException ex) {
				notify();
				throw ex;
			}
		}
	}
	/**
	 * Wait at most msecs millisconds for a permit.
	 */
	public boolean attempt(long msecs) throws InterruptedException {
		/*@JVM-1.1+@
		if(Thread.interrupted())
			throw new InterruptedException();
		/**/
		synchronized(this) {
			if(_permits > 0) {
				--_permits;
				return true;
			}
			else if(msecs <= 0)
				return false;
			else {
				try {
					final long startTime = System.currentTimeMillis();
					long waitTime = msecs;
					for(;;) {
						wait(waitTime);
						if(_permits > 0) {
							--_permits;
							return true;
						}
						waitTime = msecs
								- (System.currentTimeMillis() - startTime);
						if(waitTime <= 0)
							return false;
					}
				}
				catch(final InterruptedException ex) {
					notify();
					throw ex;
				}
			}
		}
	}
	/** Release a permit */
	public synchronized void release() {
		++_permits;
		notify();
	}
	/**
	 * Release N permits. <code>release(n)</code> is
	 * equivalent in effect to:
	 * <pre>
	 *   for(int i = 0; i < n; ++i) release();
	 * </pre>
	 * <p>
	 * But may be more efficient in some semaphore implementations.
	 * @throws IllegalArgumentException if n is negative.
	 */
	public synchronized void release(long n) {
		if(n < 0)
			throw new IllegalArgumentException("Negative argument");
		_permits += n;
		for(long i = 0; i < n; ++i) {
			notify();
		}
	}
	/**
	 * Return the current number of available permits.
	 * Returns an accurate, but possibly unstable value,
	 * that may change immediately after returning.
	 */
	public synchronized long permits() {
		return _permits;
	}
	/**
	 * Acquires the given number of permits, blocking until all are available.
	 */
	public void acquire(int permits) throws InterruptedException {
		if(permits < 0)
			throw new IllegalArgumentException("Negative permits");
		/*@JVM-1.1+@
		if(Thread.interrupted())
			throw new InterruptedException();
		/**/
		synchronized(this) {
			try {
				while(_permits < permits) {
					wait();
				}
				_permits -= permits;
			}
			catch(final InterruptedException ex) {
				notify();
				throw ex;
			}
		}
	}
	/**
	 * Acquires a permit, blocking until one is available, without being interrupted.
	 */
	public void acquireUninterruptibly() {
		synchronized(this) {
			while(_permits <= 0) {
				try {
					wait();
				}
				catch(final InterruptedException ex) {
					// Ignore and retry
				}
			}
			--_permits;
		}
	}
	/**
	 * Acquires the given number of permits, blocking until all are available, without being interrupted.
	 */
	public void acquireUninterruptibly(int permits) {
		if(permits < 0)
			throw new IllegalArgumentException("Negative permits");
		synchronized(this) {
			while(_permits < permits) {
				try {
					wait();
				}
				catch(final InterruptedException ex) {
					// Ignore and retry
				}
			}
			_permits -= permits;
		}
	}
	/**
	 * Acquires a permit only if one is available at the time of invocation.
	 */
	public boolean tryAcquire() {
		synchronized(this) {
			if(_permits > 0) {
				--_permits;
				return true;
			}
			return false;
		}
	}
	/**
	 * Acquires the given number of permits only if all are available at the time of invocation.
	 */
	public boolean tryAcquire(int permits) {
		if(permits < 0)
			throw new IllegalArgumentException("Negative permits");
		synchronized(this) {
			if(_permits >= permits) {
				_permits -= permits;
				return true;
			}
			return false;
		}
	}
	/**
	 * Acquires and returns all permits that are immediately available.
	 */
	public synchronized int drainPermits() {
		final int old = (int) _permits;
		_permits = 0;
		return old;
	}
}