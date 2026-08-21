/*
 * File: WaitFreeQueue.java
 *
 * Originally written by Doug Lea and released into the public domain.
 * This may be used for any purposes whatsoever without acknowledgment.
 * Thanks for the assistance and support of Sun Microsystems Labs,
 * and everyone contributing, testing, and using this code.
 *
 * History:
 * Date         Who            What
 * 16Jun1998    dl             Create public version
 * 5Aug1998     dl             replaced int counters with longs
 * 17nov2001    dl             Simplify given Bill Pugh's observation
 * that         counted        pointers are unnecessary.
 */
package _templates.javolution.util.concurrent;
/**
 * A wait-free linked list based queue implementation.
 * <p>
 *
 * While this class conforms to the full Channel interface, only the
 * <code>put</code> and <code>poll</code> methods are useful in most
 * applications. Because the queue does not support blocking
 * operations, <code>take</code> relies on spin-loops, which can be
 * extremely wasteful.  <p>
 *
 * This class is adapted from the algorithm described in <a
 * href="//www.cs.rochester.edu/u/michael/PODC96.html"> Simple,
 * Fast, and Practical Non-Blocking and Blocking Concurrent Queue
 * Algorithms</a> by Maged M. Michael and Michael L. Scott.  This
 * implementation is not strictly wait-free since it relies on locking
 * for basic atomicity and visibility requirements.  Locks can impose
 * unbounded waits, although this should not be a major practical
 * concern here since each lock is held for the duration of only a few
 * statements. (However, the overhead of using so many locks can make
 * it less attractive than other Channel implementations on JVMs where
 * locking operations are very slow.)  <p>
 *
 * @see BoundedLinkedQueue
 * @see LinkedQueue
 *
 * <p>[ <a href="//gee.cs.oswego.edu/dl/classes/EDU/oswego/cs/dl/util/concurrent/intro.html"> Introduction to this package.</a> ]</p>
 */
public class WaitFreeQueue implements Channel {
	/*
		This is a straightforward adaptation of Michael & Scott
		algorithm, with CAS's simulated via per-field locks,
		and without version numbers for pointers since, under
		Java Garbage Collection, you can never see the "wrong"
		node with the same address as the one you think you have.
	*/
	/** List nodes for Queue */
	private static final class Node {
		final Object value;
		volatile Node next;
		/**
		 * Make a new node with indicated item, and null link
		 */
		Node(Object x) {
			value = x;
		}
		/**
		 * Simulate a CAS operation for 'next' field
		 */
		synchronized boolean CASNext(Node oldNext, Node newNext) {
			if(next == oldNext) {
				next = newNext;
				return true;
			}
			return false;
		}
	}
	/**
	 * Head of list is always a dummy node
	 */
	private volatile Node head = new Node(null);
	/**
	 * Pointer to last node on list
	 */
	private volatile Node tail = head;
	/**
	 * Lock for simulating CAS for tail field
	 */
	private final Object tailLock = new Object();
	/**
	 * Simulate CAS for head field, using 'this' lock
	 */
	private synchronized boolean CASHead(Node oldHead, Node newHead) {
		if(head == oldHead) {
			head = newHead;
			return true;
		}
		return false;
	}
	/** Simulate CAS for tail field */
	private boolean CASTail(Node oldTail, Node newTail) {
		synchronized(tailLock) {
			if(tail == oldTail) {
				tail = newTail;
				return true;
			}
			return false;
		}
	}
	public void put(Object x) throws InterruptedException {
		if(x == null)
			throw new IllegalArgumentException();
		/*@JVM-1.1+@
		if(Thread.interrupted())
			throw new InterruptedException();
		/**/
		final Node n = new Node(x);
		for(;;) {
			final Node t = tail;
			// Try to link new node to end of list.
			if(t.CASNext(null, n)) {
				// Must now change tail field.
				// This CAS might fail, but if so, it will be fixed by others.
				CASTail(t, n);
				return;
			}
			// If cannot link, help out a previous failed attempt to move tail
			CASTail(t, t.next);
		}
	}
	public boolean offer(Object x, long msecs) throws InterruptedException {
		put(x);
		return true;
	}
	/**
	 * Main dequeue algorithm, called by poll, take.
	 */
	Object extract() throws InterruptedException {
		for(;;) {
			final Node h = head;
			final Node first = h.next;
			if(first == null)
				return null;
			final Object result = first.value;
			if(CASHead(h, first))
				return result;
		}
	}
	public Object peek() {
		final Node first = head.next;
		if(first == null)
			return null;
		// Note: This synch unnecessary after JSR-133.
		// It exists only to guarantee visibility of returned object,
		// No other synch is needed, but "old" memory model requires one.
		synchronized(this) {
			return first.value;
		}
	}
	/**
	 * Spin until poll returns a non-null value.
	 * You probably don't want to call this method.
	 * A Thread.sleep(0) is performed on each iteration
	 * as a heuristic to reduce contention. If you would
	 * rather use, for example, an exponential backoff,
	 * you could manually set this up using poll.
	 */
	public Object take() throws InterruptedException {
		/*@JVM-1.1+@
		if(Thread.interrupted())
			throw new InterruptedException();
		/**/
		for(;;) {
			final Object x = extract();
			if(x != null)
				return x;
			Thread.sleep(0);
		}
	}
	/**
	 * Spin until poll returns a non-null value or time elapses.
	 * if msecs is positive, a Thread.sleep(0) is performed on each iteration
	 * as a heuristic to reduce contention.
	 */
	public Object poll(long msecs) throws InterruptedException {
		/*@JVM-1.1+@
		if(Thread.interrupted())
			throw new InterruptedException();
		/**/
		if(msecs <= 0)
			return extract();
		final long startTime = System.currentTimeMillis();
		for(;;) {
			final Object x = extract();
			if(x != null)
				return x;
			else if(System.currentTimeMillis() - startTime >= msecs)
				return null;
			else {
				Thread.sleep(0);
			}
		}
	}
}