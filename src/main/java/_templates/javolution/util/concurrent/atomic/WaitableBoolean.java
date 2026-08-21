/*
 * File: WaitableBoolean.java
 *
 * Originally written by Doug Lea and released into the public domain.
 * This may be used for any purposes whatsoever without acknowledgment.
 * Thanks for the assistance and support of Sun Microsystems Labs,
 * and everyone contributing, testing, and using this code.
 *
 * History:
 * Date         Who            What
 * 19Jun1998    dl             Create public version
 */
package _templates.javolution.util.concurrent.atomic;
/**
 * A class useful for offloading synch for boolean instance variables.
 *
 * <p>[<a href="//gee.cs.oswego.edu/dl/classes/EDU/oswego/cs/dl/util/concurrent/intro.html"> Introduction to this package. </a>]
 */
public class WaitableBoolean extends SynchronizedBoolean {
	/**
	 * Make a new WaitableBoolean with the given initial value
	 */
	public WaitableBoolean(boolean initialValue) {
		super(initialValue);
	}
	/**
	 * Make a new WaitableBoolean with the given initial value,
	 * and using the supplied lock.
	 */
	public WaitableBoolean(boolean initialValue, Object lock) {
		super(initialValue, lock);
	}
	public boolean set(boolean newValue) {
		synchronized(_lock) {
			_lock.notifyAll();
			return super.set(newValue);
		}
	}
	public boolean commit(boolean assumedValue, boolean newValue) {
		synchronized(_lock) {
			final boolean success = super.commit(assumedValue, newValue);
			if(success) {
				_lock.notifyAll();
			}
			return success;
		}
	}
	public boolean complement() {
		synchronized(_lock) {
			_lock.notifyAll();
			return super.complement();
		}
	}
	public boolean and(boolean b) {
		synchronized(_lock) {
			_lock.notifyAll();
			return super.and(b);
		}
	}
	public boolean or(boolean b) {
		synchronized(_lock) {
			_lock.notifyAll();
			return super.or(b);
		}
	}
	public boolean xor(boolean b) {
		synchronized(_lock) {
			_lock.notifyAll();
			return super.xor(b);
		}
	}
	/**
	 * Wait until value is false, then run action if nonnull.
	 * The action is run with the synchronization lock held.
	 */
	public void whenFalse(Runnable action) throws InterruptedException {
		synchronized(_lock) {
			while(_value) {
				_lock.wait();
			}
			if(action != null) {
				action.run();
			}
		}
	}
	/**
	 * wait until value is true, then run action if nonnull.
	 * The action is run with the synchronization lock held.
	 */
	public void whenTrue(Runnable action) throws InterruptedException {
		synchronized(_lock) {
			while(!_value) {
				_lock.wait();
			}
			if(action != null) {
				action.run();
			}
		}
	}
	/**
	 * Wait until value equals c, then run action if nonnull.
	 * The action is run with the synchronization lock held.
	 */
	public void whenEqual(boolean c, Runnable action)
			throws InterruptedException {
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
	public void whenNotEqual(boolean c, Runnable action)
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
}