/*
 * File: SynchronizedRef.java
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
package _templates.javolution.util.concurrent.atomic;
/**
 * A simple class maintaining a single reference variable that
 * is always accessed and updated under synchronization.
 * <p>
 * <p>[<a href="//gee.cs.oswego.edu/dl/classes/EDU/oswego/cs/dl/util/concurrent/intro.html"> Introduction to this package. </a>]
 */
public class SynchronizedRef extends SynchronizedVariable {
	/**
	 * The maintained reference
	 */
	Object _value;
	/**
	 * Create a SynchronizedRef initially holding the given reference
	 * and using its own internal lock.
	 */
	public SynchronizedRef(Object initialValue) {
		super();
		_value = initialValue;
	}
	/**
	 * Make a new SynchronizedRef with the given initial value,
	 * and using the supplied lock.
	 */
	public SynchronizedRef(Object initialValue, Object lock) {
		super(lock);
		_value = initialValue;
	}
	/**
	 * Return the current value
	 */
	public final Object get() {
		synchronized(_lock) {
			return _value;
		}
	}
	/**
	 * Set to newValue.
	 * @return the old value
	 */
	public Object set(Object newValue) {
		synchronized(_lock) {
			final Object old = _value;
			_value = newValue;
			return old;
		}
	}
	/**
	 * Set value to newValue only if it is currently assumedValue.
	 * @return true if successful
	 */
	public boolean commit(Object assumedValue, Object newValue) {
		synchronized(_lock) {
			final boolean success = assumedValue == _value;
			if(success) {
				_value = newValue;
			}
			return success;
		}
	}
	/**
	 * Atomically swap values with another SynchronizedRef.
	 * Uses identityHashCode to avoid deadlock when
	 * two SynchronizedRefs attempt to simultaneously swap with each other.
	 * (Note: Ordering via identyHashCode is not strictly guaranteed
	 * by the language specification to return unique, orderable
	 * values, but in practice JVMs rely on them being unique.)
	 * @return the new value
	 */
	public Object swap(SynchronizedRef other) {
		if(other == this)
			return get();
		SynchronizedRef fst = this;
		SynchronizedRef snd = other;
		if(System.identityHashCode(fst) > System.identityHashCode(snd)) {
			fst = other;
			snd = this;
		}
		synchronized(fst._lock) {
			synchronized(snd._lock) {
				fst.set(snd.set(fst.get()));
				return get();
			}
		}
	}
}