/*
 * File: BoundedLinkedQueue.java
 *
 * Originally written by Doug Lea and released into the public domain.
 * This may be used for any purposes whatsoever without acknowledgment.
 * Thanks for the assistance and support of Sun Microsystems Labs,
 * and everyone contributing, testing, and using this code.
 *
 * History:
 * Date         Who            What
 * 11Jun1998    dl             Create public version
 * 17Jul1998    dl             Simplified by eliminating wait counts
 * 25aug1998    dl             Added peek
 * 10oct1999    dl             lock on node object to ensure visibility
 * 27jan2000    dl             setCapacity forces immediate permit reconcile
 */
package _templates.javolution.util.concurrent;
/**
 * A bounded variant of LinkedQueue class. This class may be preferable to
 * BoundedBuffer because it allows a bit more concurency among puts and takes,
 * because it does not pre-allocate fixed storage for elements, and allows
 * capacity to be dynamically reset.
 * On the other hand, since it allocates a node object on each put,
 * it can be slow on systems with slow allocation and GC.
 * Also, it may be preferable to LinkedQueue when you need to limit
 * the capacity to prevent resource exhaustion. This protection
 * normally does not hurt much performance-wise: When the
 * queue is not empty or full, most puts and takes
 * are still usually able to execute concurrently.
 * @see LinkedQueue
 * @see BoundedBuffer
 * <p>[<a href="//gee.cs.oswego.edu/dl/classes/EDU/oswego/cs/dl/util/concurrent/intro.html"> Introduction to this package. </a>] <p>
 */
public class BoundedLinkedQueue implements BoundedChannel {
	/*
	 * It might be a bit nicer if this were declared as
	 * a subclass of LinkedQueue, or a sibling class of
	 * a common abstract class. It shares much of the
	 * basic design and bookkeeping fields. But too
	 * many details differ to make this worth doing.
	 */
	/**
	 * Dummy header node of list. The first actual node, if it exists, is always
	 * at _head.next. After each take, the old first node becomes the head.
	 */
	private LinkedNode _head;
	/**
	 * The last node of list. Put() appends to list, so modifies last_
	 */
	private LinkedNode _last;
	/**
	 * Helper monitor. Ensures that only one put at a time executes.
	 */
	private final Object _putGuard;
	/**
	 * Helper monitor. Protects and provides wait queue for takes
	 */
	private final Object _takeGuard;
	/**
	 * Number of elements allowed
	 */
	private int _capacity;
	/**
	 * One side of a split permit count.
	 * The counts represent permits to do a put. (The queue is full when zero).
	 * Invariant: putSidePutPermits_ + takeSidePutPermits_ = capacity_ - length.
	 * (The length is never separately recorded, so this cannot be
	 * checked explicitly.)
	 * To minimize contention between puts and takes, the
	 * put side uses up all of its permits before transfering them from
	 * the take side. The take side just increments the count upon each take.
	 * Thus, most puts and take can run independently of each other unless
	 * the queue is empty or full.
	 * Initial value is queue capacity.
	 */
	private int _putSidePutPermits;
	/**
	 * Number of takes since last reconcile
	 */
	private int _takeSidePutPermits;
	/**
	 * Create a queue with the given capacity
	 * @throws IllegalArgumentException if capacity less or equal to zero
	 */
	public BoundedLinkedQueue(int capacity) {
		if(capacity <= 0)
			throw new IllegalArgumentException();
		_putGuard = new Object();
		_takeGuard = new Object();
		_capacity = capacity;
		_putSidePutPermits = capacity;
		_head = new LinkedNode(null);
		_last = _head;
	}
	/**
	 * Create a queue with the current default capacity
	 */
	public BoundedLinkedQueue() {
		this(DefaultChannelCapacity.get());
	}
	/**
	 * Move put permits from take side to put side;
	 * return the number of put side permits that are available.
	 * Call only under synch on puGuard_ AND this.
	 */
	private final int reconcilePutPermits() {
		_putSidePutPermits += _takeSidePutPermits;
		_takeSidePutPermits = 0;
		return _putSidePutPermits;
	}
	/**
	 * Return the current capacity of this queue
	 */
	public synchronized int capacity() {
		return _capacity;
	}
	/**
	 * Return the number of elements in the queue.
	 * This is only a snapshot value, that may be in the midst
	 * of changing. The returned value will be unreliable in the presence of
	 * active puts and takes, and should only be used as a heuristic
	 * estimate, for example for resource monitoring purposes.
	 */
	public synchronized int size() {
		/*
		 * This should ideally synch on putGuard_, but
		 * doing so would cause it to block waiting for an in-progress
		 * put, which might be stuck. So we instead use whatever
		 * value of putSidePutPermits_ that we happen to read.
		 */
		return _capacity - (_takeSidePutPermits + _putSidePutPermits);
	}
	/**
	 * Reset the capacity of this queue.
	 * If the new capacity is less than the old capacity,
	 * existing elements are NOT removed, but
	 * incoming puts will not proceed until the number of elements
	 * is less than the new capacity.
	 * @throws IllegalArgumentException if capacity less or equal to zero
	 */
	public void setCapacity(int newCapacity) {
		if(newCapacity <= 0)
			throw new IllegalArgumentException();
		synchronized(_putGuard) {
			synchronized(this) {
				_takeSidePutPermits += newCapacity - _capacity;
				_capacity = newCapacity;
				// Force immediate reconcilation.
				reconcilePutPermits();
				notifyAll();
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
				++_takeSidePutPermits;
				notify();
			}
			return x;
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
	public Object take() throws InterruptedException {
		/*@JVM-1.1+@
		if(Thread.interrupted())
			throw new InterruptedException();
		/**/
		Object x = extract();
		if(x != null)
			return x;
		synchronized(_takeGuard) {
			try {
				for(;;) {
					x = extract();
					if(x != null)
						return x;
					_takeGuard.wait();
				}
			}
			catch(final InterruptedException ex) {
				_takeGuard.notify();
				throw ex;
			}
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
		synchronized(_takeGuard) {
			try {
				long waitTime = msecs;
				final long start = msecs <= 0 ? 0 : System.currentTimeMillis();
				for(;;) {
					x = extract();
					if(x != null || waitTime <= 0)
						return x;
					_takeGuard.wait(waitTime);
					waitTime = msecs - (System.currentTimeMillis() - start);
				}
			}
			catch(final InterruptedException ex) {
				_takeGuard.notify();
				throw ex;
			}
		}
	}
	/**
	 * Notify a waiting take if needed
	 */
	private final void allowTake() {
		synchronized(_takeGuard) {
			_takeGuard.notify();
		}
	}
	/**
	 * Create and insert a node.
	 * Call only under synch on putGuard_
	 */
	private void insert(Object x) {
		--_putSidePutPermits;
		final LinkedNode p = new LinkedNode(x);
		synchronized(_last) {
			_last.next = p;
			_last = p;
		}
	}
	/*
	 * put and offer(ms) differ only in policy before insert/allowTake
	 */
	public void put(Object x) throws InterruptedException {
		if(x == null)
			throw new IllegalArgumentException();
		/*@JVM-1.1+@
		if(Thread.interrupted())
			throw new InterruptedException();
		/**/
		synchronized(_putGuard) {
			if(_putSidePutPermits <= 0) { // wait for permit.
				synchronized(this) {
					if(reconcilePutPermits() <= 0) {
						try {
							for(;;) {
								wait();
								if(reconcilePutPermits() > 0) {
									break;
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
			insert(x);
		}
		// call outside of lock to loosen put/take coupling
		allowTake();
	}
	public boolean offer(Object x, long msecs) throws InterruptedException {
		if(x == null)
			throw new IllegalArgumentException();
		/*@JVM-1.1+@
		if(Thread.interrupted())
			throw new InterruptedException();
		/**/
		synchronized(_putGuard) {
			if(_putSidePutPermits <= 0) {
				synchronized(this) {
					if(reconcilePutPermits() <= 0) {
						if(msecs <= 0)
							return false;
						try {
							long waitTime = msecs;
							final long start = System.currentTimeMillis();
							for(;;) {
								wait(waitTime);
								if(reconcilePutPermits() > 0) {
									break;
								}
								waitTime = msecs
										- (System.currentTimeMillis() - start);
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
			insert(x);
		}
		allowTake();
		return true;
	}
	public boolean isEmpty() {
		synchronized(_head) {
			return _head.next == null;
		}
	}
}