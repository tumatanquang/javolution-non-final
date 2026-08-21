/*
 * File: WaiterPreferenceSemaphore.java
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
 */
package _templates.javolution.util.concurrent;
/**
 * An implementation of counting Semaphores that
 *  enforces enough fairness for applications that
 *  need to avoid indefinite overtaking without
 *  necessarily requiring FIFO ordered access.
 *  Empirically, very little is paid for this property
 *  unless there is a lot of contention among threads
 *  or very unfair JVM scheduling.
 *  The acquire method waits even if there are permits
 *  available but have not yet been claimed by threads that have
 *  been notified but not yet resumed. This makes the semaphore
 *  almost as fair as the underlying Java primitives allow.
 *  So, if synch lock entry and notify are both fair
 *  so is the semaphore -- almost:  Rewaits stemming
 *  from timeouts in attempt, along with potentials for
 *  interrupted threads to be notified can compromise fairness,
 *  possibly allowing later-arriving threads to pass before
 *  later arriving ones. However, in no case can a newly
 *  entering thread obtain a permit if there are still others waiting.
 *  Also, signalling order need not coincide with
 *  resumption order. Later-arriving threads might get permits
 *  and continue before other resumable threads are actually resumed.
 *  However, all of these potential fairness breaches are
 *  very rare in practice unless the underlying JVM
 *  performs strictly LIFO notifications (which has, sadly enough,
 *  been known to occur) in which case you need to use
 *  a FIFOSemaphore to maintain a reasonable approximation
 *  of fairness.
 * <p>[ <a href="//gee.cs.oswego.edu/dl/classes/EDU/oswego/cs/dl/util/concurrent/intro.html"> Introduction to this package.</a> ]</p>
 */
public final class WaiterPreferenceSemaphore extends Semaphore {
	/**
	 * Create a Semaphore with the given initial number of permits.
	 */
	public WaiterPreferenceSemaphore(long initial) {
		super(initial);
	}
	/** Number of waiting threads */
	private long _waits = 0;
	public void acquire() throws InterruptedException {
		/*@JVM-1.1+@
		if(Thread.interrupted())
			throw new InterruptedException();
		/**/
		synchronized(this) {
			/*
			 * Only take if there are more permits than threads waiting
			 * for permits. This prevents infinite overtaking.
			 */
			if(_permits > _waits) {
				--_permits;
				return;
			}
			++_waits;
			try {
				for(;;) {
					wait();
					if(_permits > 0) {
						--_waits;
						--_permits;
						return;
					}
				}
			}
			catch(final InterruptedException ex) {
				--_waits;
				notify();
				throw ex;
			}
		}
	}
	public boolean attempt(long msecs) throws InterruptedException {
		/*@JVM-1.1+@
		if(Thread.interrupted())
			throw new InterruptedException();
		/**/
		synchronized(this) {
			if(_permits > _waits) {
				--_permits;
				return true;
			}
			else if(msecs <= 0)
				return false;
			else {
				++_waits;
				final long startTime = System.currentTimeMillis();
				long waitTime = msecs;
				try {
					for(;;) {
						wait(waitTime);
						if(_permits > 0) {
							--_waits;
							--_permits;
							return true;
						}
						// got a time-out or false-alarm notify
						waitTime = msecs
								- (System.currentTimeMillis() - startTime);
						if(waitTime <= 0) {
							--_waits;
							return false;
						}
					}
				}
				catch(final InterruptedException ex) {
					--_waits;
					notify();
					throw ex;
				}
			}
		}
	}
	public synchronized void release() {
		++_permits;
		notify();
	}
	/**
	 * Release N permits
	 */
	public synchronized void release(long n) {
		_permits += n;
		for(long i = 0; i < n; ++i) {
			notify();
		}
	}
}