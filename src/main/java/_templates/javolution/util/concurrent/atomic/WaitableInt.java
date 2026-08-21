/*
 * File: WaitableInt.java
 *
 * Originally written by Doug Lea and released into the public domain.
 * This may be used for any purposes whatsoever without acknowledgment.
 * Thanks for the assistance and support of Sun Microsystems Labs,
 * and everyone contributing, testing, and using this code.
 *
 * History:
 * Date         Who            What
 * 23Jun1998    dl             Create public version
 * 13may2004    dl             Add notifying bit ops
 */
package _templates.javolution.util.concurrent.atomic;
/**
 * A class useful for offloading waiting and signalling operations
 * on single int variables.
 * <p>
 * <p>[<a href="//gee.cs.oswego.edu/dl/classes/EDU/oswego/cs/dl/util/concurrent/intro.html"> Introduction to this package. </a>]
 */
public class WaitableInt extends SynchronizedInt {
	/**
	 * Make a new WaitableInt with the given initial value,
	 * and using its own internal lock.
	 */
	public WaitableInt(int initialValue) {
		super(initialValue);
	}
	/**
	 * Make a new WaitableInt with the given initial value,
	 * and using the supplied lock.
	 */
	public WaitableInt(int initialValue, Object lock) {
		super(initialValue, lock);
	}
	public int set(int newValue) {
		synchronized(_lock) {
			_lock.notifyAll();
			return super.set(newValue);
		}
	}
	public boolean commit(int assumedValue, int newValue) {
		synchronized(_lock) {
			final boolean success = super.commit(assumedValue, newValue);
			if(success) {
				_lock.notifyAll();
			}
			return success;
		}
	}
	public int increment() {
		synchronized(_lock) {
			_lock.notifyAll();
			return super.increment();
		}
	}
	public int decrement() {
		synchronized(_lock) {
			_lock.notifyAll();
			return super.decrement();
		}
	}
	public int add(int amount) {
		synchronized(_lock) {
			_lock.notifyAll();
			return super.add(amount);
		}
	}
	public int subtract(int amount) {
		synchronized(_lock) {
			_lock.notifyAll();
			return super.subtract(amount);
		}
	}
	public int multiply(int factor) {
		synchronized(_lock) {
			_lock.notifyAll();
			return super.multiply(factor);
		}
	}
	public int divide(int factor) {
		synchronized(_lock) {
			_lock.notifyAll();
			return super.divide(factor);
		}
	}
	/**
	 * Set the value to its complement
	 * @return the new value
	 */
	public int complement() {
		synchronized(_lock) {
			_value = ~_value;
			_lock.notifyAll();
			return _value;
		}
	}
	/**
	 * Set value to value &amp; b.
	 * @return the new value
	 */
	public int and(int b) {
		synchronized(_lock) {
			_value = _value & b;
			_lock.notifyAll();
			return _value;
		}
	}
	/**
	 * Set value to value | b.
	 * @return the new value
	 */
	public int or(int b) {
		synchronized(_lock) {
			_value = _value | b;
			_lock.notifyAll();
			return _value;
		}
	}
	/**
	 * Set value to value ^ b.
	 * @return the new value
	 */
	public int xor(int b) {
		synchronized(_lock) {
			_value = _value ^ b;
			_lock.notifyAll();
			return _value;
		}
	}
	/**
	 * Wait until value equals c, then run action if nonnull.
	 * The action is run with the synchronization lock held.
	 */
	public void whenEqual(int c, Runnable action) throws InterruptedException {
		synchronized(_lock) {
			while(!(_value == c)) {
				_lock.wait();
			}
			if(action != null) {
				action.run();
			}
		}
	}
	/**
	 * wait until value not equal to c, then run action if nonnull.
	 * The action is run with the synchronization lock held.
	 */
	public void whenNotEqual(int c, Runnable action)
			throws InterruptedException {
		synchronized(_lock) {
			while(!(_value != c)) {
				_lock.wait();
			}
			if(action != null) {
				action.run();
			}
		}
	}
	/**
	 * wait until value less than or equal to c, then run action if nonnull.
	 * The action is run with the synchronization lock held.
	 */
	public void whenLessEqual(int c, Runnable action)
			throws InterruptedException {
		synchronized(_lock) {
			while(!(_value <= c)) {
				_lock.wait();
			}
			if(action != null) {
				action.run();
			}
		}
	}
	/**
	 * wait until value less than c, then run action if nonnull.
	 * The action is run with the synchronization lock held.
	 */
	public void whenLess(int c, Runnable action) throws InterruptedException {
		synchronized(_lock) {
			while(!(_value < c)) {
				_lock.wait();
			}
			if(action != null) {
				action.run();
			}
		}
	}
	/**
	 * wait until value greater than or equal to c, then run action if nonnull.
	 * The action is run with the synchronization lock held.
	 */
	public void whenGreaterEqual(int c, Runnable action)
			throws InterruptedException {
		synchronized(_lock) {
			while(!(_value >= c)) {
				_lock.wait();
			}
			if(action != null) {
				action.run();
			}
		}
	}
	/**
	 * wait until value greater than c, then run action if nonnull.
	 * The action is run with the synchronization lock held.
	 */
	public void whenGreater(int c, Runnable action)
			throws InterruptedException {
		synchronized(_lock) {
			while(!(_value > c)) {
				_lock.wait();
			}
			if(action != null) {
				action.run();
			}
		}
	}
}