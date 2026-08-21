/*
 * File: LinkedQueue.java
 *
 * Originally written by Doug Lea and released into the public domain.
 * This may be used for any purposes whatsoever without acknowledgment.
 * Thanks for the assistance and support of Sun Microsystems Labs,
 * and everyone contributing, testing, and using this code.
 *
 * History:
 * Date         Who            What
 * 11Jun1998    dl             Create public version
 * 25aug1998    dl             added peek
 * 10dec1998    dl             added isEmpty
 * 10oct1999    dl             lock on node object to ensure visibility
 */
package _templates.javolution.util.concurrent;
/**
 * A linked list based channel implementation.
 * The algorithm avoids contention between puts
 * and takes when the queue is not empty.
 * Normally a put and a take can proceed simultaneously.
 * (Although it does not allow multiple concurrent puts or takes.)
 * This class tends to perform more efficently than
 * other Channel implementations in producer/consumer
 * applications.
 * <p>[ <a href="//gee.cs.oswego.edu/dl/classes/EDU/oswego/cs/dl/util/concurrent/intro.html"> Introduction to this package.</a> ]</p>
 */
public class LinkedQueue implements Channel {
	/**
	 * Dummy header node of list. The first actual node, if it exists, is always
	 * at _head.next. After each take, the old first node becomes the head.
	 */
	private LinkedNode _head;
	/**
	 * Helper monitor for managing access to last node.
	 */
	private final Object _putLock = new Object();
	/**
	 * The last node of list. Put() appends to list, so modifies last_
	 */
	private LinkedNode _last;
	/**
	 * The number of threads waiting for a take.
	 * Notifications are provided in put only if greater than zero.
	 * The bookkeeping is worth it here since in reasonably balanced
	 * usages, the notifications will hardly ever be necessary, so
	 * the call overhead to notify can be eliminated.
	 */
	protected int _waitingForTake;
	public LinkedQueue() {
		_head = new LinkedNode(null);
		_last = _head;
	}
	/**
	 * Main mechanics for put/offer
	 */
	private void insert(Object x) {
		synchronized(_putLock) {
			final LinkedNode p = new LinkedNode(x);
			synchronized(_last) {
				_last.next = p;
				_last = p;
			}
			if(_waitingForTake > 0) {
				_putLock.notify();
			}
		}
	}
	/**
	 * Main mechanics for take/poll
	 */
	private synchronized Object extract() {
		synchronized(_head) {
			Object x = null;
			final LinkedNode first = _head.next;
			if(first != null) {
				x = first.value;
				first.value = null;
				_head = first;
			}
			return x;
		}
	}
	public void put(Object x) throws InterruptedException {
		if(x == null)
			throw new IllegalArgumentException();
		/*@JVM-1.1+@
		if(Thread.interrupted())
			throw new InterruptedException();
		/**/
		insert(x);
	}
	public boolean offer(Object x, long msecs) throws InterruptedException {
		if(x == null)
			throw new IllegalArgumentException();
		/*@JVM-1.1+@
		if(Thread.interrupted())
			throw new InterruptedException();
		/**/
		insert(x);
		return true;
	}
	public Object take() throws InterruptedException {
		/*@JVM-1.1+@
		if(Thread.interrupted())
			throw new InterruptedException();
		/**/
		// try to extract. If fail, then enter wait-based retry loop
		Object x = extract();
		if(x != null)
			return x;
		synchronized(_putLock) {
			try {
				++_waitingForTake;
				for(;;) {
					x = extract();
					if(x != null) {
						--_waitingForTake;
						return x;
					}
					_putLock.wait();
				}
			}
			catch(final InterruptedException ex) {
				--_waitingForTake;
				_putLock.notify();
				throw ex;
			}
		}
	}
	public Object peek() {
		synchronized(_head) {
			final LinkedNode first = _head.next;
			if(first != null)
				return first.value;
			return null;
		}
	}
	public boolean isEmpty() {
		synchronized(_head) {
			return _head.next == null;
		}
	}
	public Object poll(long msecs) throws InterruptedException {
		/*@JVM-1.1+@
		if(Thread.interrupted())
			throw new InterruptedException();
		/**/
		Object x = extract();
		if(x != null)
			return x;
		synchronized(_putLock) {
			try {
				long waitTime = msecs;
				final long start = msecs <= 0 ? 0 : System.currentTimeMillis();
				++_waitingForTake;
				for(;;) {
					x = extract();
					if(x != null || waitTime <= 0) {
						--_waitingForTake;
						return x;
					}
					_putLock.wait(waitTime);
					waitTime = msecs - (System.currentTimeMillis() - start);
				}
			}
			catch(final InterruptedException ex) {
				--_waitingForTake;
				_putLock.notify();
				throw ex;
			}
		}
	}
}