/*
 * File: FIFOSemaphore.java
 *
 * Originally written by Doug Lea and released into the public domain.
 * This may be used for any purposes whatsoever without acknowledgment.
 * Thanks for the assistance and support of Sun Microsystems Labs,
 * and everyone contributing, testing, and using this code.
 *
 * History:
 * Date         Who            What
 * 11Jun1998    dl             Create public version
 */
package _templates.javolution.util.concurrent;
/**
 * A First-in/First-out implementation of a Semaphore.
 * Waiting requests will be satisified in
 * the order that the processing of those requests got to a certain point.
 * If this sounds vague it is meant to be. FIFO implies a
 * logical timestamping at some point in the processing of the
 * request. To simplify things we don't actually timestamp but
 * simply store things in a FIFO queue. Thus the order in which
 * requests enter the queue will be the order in which they come
 * out.  This order need not have any relationship to the order in
 * which requests were made, nor the order in which requests
 * actually return to the caller. These depend on Java thread
 * scheduling which is not guaranteed to be predictable (although
 * JVMs tend not to go out of their way to be unfair).
 * <p>[ <a href="//gee.cs.oswego.edu/dl/classes/EDU/oswego/cs/dl/util/concurrent/intro.html"> Introduction to this package.</a> ]</p>
 */
public class FIFOSemaphore extends QueuedSemaphore {
	/**
	 * Create a Semaphore with the given initial number of permits.
	 * Using a seed of one makes the semaphore act as a mutual exclusion lock.
	 * Negative seeds are also allowed, in which case no acquires will proceed
	 * until the number of releases has pushed the number of permits past 0.
	 */
	public FIFOSemaphore(long initialPermits) {
		super(new FIFOWaitQueue(), initialPermits);
	}
	/**
	 * Simple linked list queue used in FIFOSemaphore.
	 * Methods are not synchronized; they depend on synch of callers
	 */
	static final class FIFOWaitQueue extends WaitQueue {
		private WaitNode _head = null;
		private WaitNode _tail = null;
		void insert(WaitNode w) {
			if(_tail == null) {
				_head = _tail = w;
			}
			else {
				_tail.next = w;
				_tail = w;
			}
		}
		WaitNode extract() {
			if(_head == null)
				return null;
			final WaitNode w = _head;
			_head = w.next;
			if(_head == null) {
				_tail = null;
			}
			w.next = null;
			return w;
		}
	}
}