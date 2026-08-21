/*
 * File: BoundedBuffer.java
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
 * 25aug1998    dl             added peek
 * 5May1999     dl             replace % with conditional (slightly faster)
 */
package _templates.javolution.util.concurrent;
/**
 * Efficient array-based bounded buffer class.
 * Adapted from CPJ, chapter 8, which describes design.
 * <p>[<a href="//gee.cs.oswego.edu/dl/classes/EDU/oswego/cs/dl/util/concurrent/intro.html"> Introduction to this package. </a>] <p>
 */
public class BoundedBuffer implements BoundedChannel {
	private final Object[] _array; // the elements
	private int _takePtr = 0; // circular indices
	private int _putPtr = 0;
	private int _usedSlots = 0; // length
	private int _emptySlots; // capacity - length
	/**
	 * Helper monitor to handle puts.
	 */
	private final Object _putMonitor = new Object();
	/**
	 * Create a BoundedBuffer with the given capacity.
	 * @throws IllegalArgumentException if capacity less or equal to zero
	 */
	public BoundedBuffer(int capacity) throws IllegalArgumentException {
		if(capacity <= 0)
			throw new IllegalArgumentException();
		_array = new Object[capacity];
		_emptySlots = capacity;
	}
	/**
	 * Create a buffer with the current default capacity
	 */
	public BoundedBuffer() {
		this(DefaultChannelCapacity.get());
	}
	/**
	 * Return the number of elements in the buffer.
	 * This is only a snapshot value, that may change
	 * immediately after returning.
	 */
	public synchronized int size() {
		return _usedSlots;
	}
	public int capacity() {
		return _array.length;
	}
	private void incEmptySlots() {
		synchronized(_putMonitor) {
			++_emptySlots;
			_putMonitor.notify();
		}
	}
	private synchronized void incUsedSlots() {
		++_usedSlots;
		notify();
	}
	private final void insert(Object x) { // mechanics of put
		--_emptySlots;
		_array[_putPtr] = x;
		if(++_putPtr >= _array.length) {
			_putPtr = 0;
		}
	}
	private final Object extract() { // mechanics of take
		--_usedSlots;
		final Object old = _array[_takePtr];
		_array[_takePtr] = null;
		if(++_takePtr >= _array.length) {
			_takePtr = 0;
		}
		return old;
	}
	public Object peek() {
		synchronized(this) {
			if(_usedSlots > 0)
				return _array[_takePtr];
			return null;
		}
	}
	public void put(Object x) throws InterruptedException {
		if(x == null)
			throw new IllegalArgumentException();
		/*@JVM-1.1+@
		if(Thread.interrupted())
			throw new InterruptedException();
		/**/
		synchronized(_putMonitor) {
			while(_emptySlots <= 0) {
				try {
					_putMonitor.wait();
				}
				catch(final InterruptedException ex) {
					_putMonitor.notify();
					throw ex;
				}
			}
			insert(x);
		}
		incUsedSlots();
	}
	public boolean offer(Object x, long msecs) throws InterruptedException {
		if(x == null)
			throw new IllegalArgumentException();
		/*@JVM-1.1+@
		if(Thread.interrupted())
			throw new InterruptedException();
		/**/
		synchronized(_putMonitor) {
			final long start = msecs <= 0 ? 0 : System.currentTimeMillis();
			long waitTime = msecs;
			while(_emptySlots <= 0) {
				if(waitTime <= 0)
					return false;
				try {
					_putMonitor.wait(waitTime);
				}
				catch(final InterruptedException ex) {
					_putMonitor.notify();
					throw ex;
				}
				waitTime = msecs - (System.currentTimeMillis() - start);
			}
			insert(x);
		}
		incUsedSlots();
		return true;
	}
	public Object take() throws InterruptedException {
		/*@JVM-1.1+@
		if(Thread.interrupted())
			throw new InterruptedException();
		/**/
		Object old = null;
		synchronized(this) {
			while(_usedSlots <= 0) {
				try {
					wait();
				}
				catch(final InterruptedException ex) {
					notify();
					throw ex;
				}
			}
			old = extract();
		}
		incEmptySlots();
		return old;
	}
	public Object poll(long msecs) throws InterruptedException {
		/*@JVM-1.1+@
		if(Thread.interrupted())
			throw new InterruptedException();
		/**/
		Object old = null;
		synchronized(this) {
			final long start = msecs <= 0 ? 0 : System.currentTimeMillis();
			long waitTime = msecs;
			while(_usedSlots <= 0) {
				if(waitTime <= 0)
					return null;
				try {
					wait(waitTime);
				}
				catch(final InterruptedException ex) {
					notify();
					throw ex;
				}
				waitTime = msecs - (System.currentTimeMillis() - start);
			}
			old = extract();
		}
		incEmptySlots();
		return old;
	}
}